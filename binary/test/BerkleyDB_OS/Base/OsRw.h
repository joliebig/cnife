#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#include <sys/stat.h>

#include <string.h>
#endif

#ifndef WIN32
#include <unistd.h>
#endif


#include "db_int.h"
#include "OsRw.h"

#ifdef HAVE_FILESYSTEM_NOTZERO
static int __os_zerofill __P((DB_ENV *, DB_FH *));
#endif
static int __os_physwrite __P((DB_ENV *, DB_FH *, void *, size_t, size_t *));


class COsRw
{
public:
	static int os_io(DB_ENV *dbenv, int op, DB_FH *fhp, db_pgno_t pgno, u_int32_t pagesize, u_int8_t *buf, size_t *niop)
	{
#if defined(HAVE_PREAD) && defined(HAVE_PWRITE)
		ssize_t nio;
#endif
		int ret;
	
		/* Check for illegal usage. */
		DB_ASSERT(F_ISSET(fhp, DB_FH_OPENED) && fhp->fd != -1);
	
#if defined(HAVE_PREAD) && defined(HAVE_PWRITE)
		switch (op) {
		case DB_IO_READ:
			if (DB_GLOBAL(j_read) != NULL)
				goto slow;
			nio = DB_GLOBAL(j_pread) != NULL ? DB_GLOBAL(j_pread)
				(fhp->fd, buf, pagesize, (off_t)pgno * pagesize) :
				pread(fhp->fd, buf, pagesize, (off_t)pgno * pagesize);
			break;
		case DB_IO_WRITE:
			if (DB_GLOBAL(j_write) != NULL)
				goto slow;
#ifdef HAVE_FILESYSTEM_NOTZERO
			if (__os_fs_notzero())
				goto slow;
#endif
			nio = DB_GLOBAL(j_pwrite) != NULL ? DB_GLOBAL(j_pwrite)
				(fhp->fd, buf, pagesize, (off_t)pgno * pagesize) :
				pwrite(fhp->fd, buf, pagesize, (off_t)pgno * pagesize);
			break;
		default:
			return (EINVAL);
		}
		if (nio == (ssize_t)pagesize) {
			*niop = pagesize;
			return (0);
		}
	slow:
#endif
		MUTEX_LOCK(dbenv, fhp->mtx_fh);
	
		if ((ret = COsSeek::os_seek(dbenv, fhp,
		    pagesize, pgno, 0, 0, DB_OS_SEEK_SET)) != 0)
			goto err;
		switch (op) {
		case DB_IO_READ:
			ret = os_read(dbenv, fhp, buf, pagesize, niop);
			break;
		case DB_IO_WRITE:
			ret = os_write(dbenv, fhp, buf, pagesize, niop);
			break;
		default:
			ret = EINVAL;
			break;
		}
	
	err:	MUTEX_UNLOCK(dbenv, fhp->mtx_fh);
	
		return (ret);
	
	}
	

	static int os_read(DB_ENV *dbenv, DB_FH *fhp, void *addr, size_t len, size_t *nrp)
	{
		size_t offset;
		ssize_t nr;
		int ret;
		u_int8_t *taddr;
	
		ret = 0;
	
		/* Check for illegal usage. */
		DB_ASSERT(F_ISSET(fhp, DB_FH_OPENED) && fhp->fd != -1);
	
		if (DB_GLOBAL(j_read) != NULL) {
			*nrp = len;
			if (DB_GLOBAL(j_read)(fhp->fd, addr, len) != (ssize_t)len) {
				ret = COsErrno::os_get_errno();
				CDbErr::db_err(dbenv, "read: %#lx, %lu: %s",
				    P_TO_ULONG(addr), (u_long)len, strerror(ret));
			}
			return (ret);
		}
	
		for (taddr = static_cast<u_int8_t*>(addr), offset = 0;
		    offset < len; taddr += nr, offset += (u_int32_t)nr) {
			RETRY_CHK(((nr = read(
			    fhp->fd, taddr, len - offset)) < 0 ? 1 : 0), ret);
			if (nr == 0 || ret != 0)
				break;
		}
		*nrp = (size_t)(taddr - (u_int8_t *)addr);
		if (ret != 0)
			CDbErr::db_err(dbenv, "read: %#lx, %lu: %s",
			    P_TO_ULONG(taddr), (u_long)len - offset, strerror(ret));
		return (ret);
	}
	

	static int os_write(DB_ENV *dbenv, DB_FH *fhp, void *addr, size_t len, size_t *nwp)
	{
		/* Check for illegal usage. */
		DB_ASSERT(F_ISSET(fhp, DB_FH_OPENED) && fhp->fd != -1);
	
#ifdef HAVE_FILESYSTEM_NOTZERO
		/* Zero-fill as necessary. */
		if (COsConfig::os_fs_notzero()) {
			int ret;
			if ((ret = os_zerofill(dbenv, fhp)) != 0)
				return (ret);
		}
#endif
		return (os_physwrite(dbenv, fhp, addr, len, nwp));
	}
	

	static int os_physwrite(DB_ENV *dbenv, DB_FH *fhp, void *addr, size_t len, size_t *nwp)
	{
		size_t offset;
		ssize_t nw;
		int ret;
		u_int8_t *taddr;
	
		ret = 0;
	
#if defined(HAVE_FILESYSTEM_NOTZERO) && defined(DIAGNOSTIC)
		if (COsConfig::os_fs_notzero()) {
			struct stat sb;
			off_t cur_off;
	
			DB_ASSERT(fstat(fhp->fd, &sb) != -1 &&
			    (cur_off = lseek(fhp->fd, (off_t)0, SEEK_CUR)) != -1 &&
			    cur_off <= sb.st_size);
		}
#endif
	
		/*
		 * Make a last "panic" check.  Imagine a thread of control running in
		 * Berkeley DB, going to sleep.  Another thread of control decides to
		 * run recovery because the environment is broken.  The first thing
		 * recovery does is panic the existing environment, but we only check
		 * the panic flag when crossing the public API.  If the sleeping thread
		 * wakes up and writes something, we could have two threads of control
		 * writing the log files at the same time.  So, before writing, make a
		 * last panic check.  Obviously, there's still a window, but it's very,
		 * very small.
		 */
		PANIC_CHECK(dbenv);
	
		if (DB_GLOBAL(j_write) != NULL) {
			*nwp = len;
			if (DB_GLOBAL(j_write)(fhp->fd, addr, len) != (ssize_t)len) {
				ret = COsErrno::os_get_errno();
				CDbErr::db_err(dbenv, "write: %#lx, %lu: %s",
				    P_TO_ULONG(addr), (u_long)len, strerror(ret));
			}
			return (ret);
		}
	
		for (taddr = static_cast<u_int8_t*>(addr), offset = 0;
		    offset < len; taddr += nw, offset += (u_int32_t)nw) {
			RETRY_CHK(((nw = write(
			    fhp->fd, taddr, len - offset)) < 0 ? 1 : 0), ret);
			if (ret != 0)
				break;
		}
		*nwp = len;
		if (ret != 0)
			CDbErr::db_err(dbenv, "write: %#lx, %lu: %s",
			    P_TO_ULONG(taddr), (u_long)len - offset, strerror(ret));
		return (ret);
	}
	

#ifdef HAVE_FILESYSTEM_NOTZERO
	static int os_zerofill(DB_ENV *dbenv, DB_FH *fhp)
	{
		off_t stat_offset, write_offset;
		size_t blen, nw;
		u_int32_t bytes, mbytes;
		int group_sync, need_free, ret;
		u_int8_t buf[8 * 1024], *bp;
	
		/* Calculate the byte offset of the next write. */
		write_offset = (off_t)fhp->pgno * fhp->pgsize + fhp->offset;
	
		/* Stat the file. */
		if ((ret = COsStat::os_ioinfo(dbenv, NULL, fhp, &mbytes, &bytes, NULL)) != 0)
			return (ret);
		stat_offset = (off_t)mbytes * MEGABYTE + bytes;
	
		/* Check if the file is large enough. */
		if (stat_offset >= write_offset)
			return (0);
	
		/* Get a large buffer if we're writing lots of data. */
#undef	ZF_LARGE_WRITE
#define	ZF_LARGE_WRITE	(64 * 1024)
		if (write_offset - stat_offset > ZF_LARGE_WRITE) {
			if ((ret = COsAlloc::os_calloc(dbenv, 1, ZF_LARGE_WRITE, &bp)) != 0)
				    return (ret);
			blen = ZF_LARGE_WRITE;
			need_free = 1;
		} else {
			bp = buf;
			blen = sizeof(buf);
			need_free = 0;
			memset(buf, 0, sizeof(buf));
		}
	
		/* Seek to the current end of the file. */
		if ((ret = COsSeek::os_seek(
		    dbenv, fhp, MEGABYTE, mbytes, bytes, 0, DB_OS_SEEK_SET)) != 0)
			goto err;
	
		/*
		 * Hash is the only access method that allocates groups of pages.  Hash
		 * uses the existence of the last page in a group to signify the entire
		 * group is OK; so, write all the pages but the last one in the group,
		 * flush them to disk, then write the last one to disk and flush it.
		 */
		for (group_sync = 0; stat_offset < write_offset; group_sync = 1) {
			if (write_offset - stat_offset <= blen) {
				blen = (size_t)(write_offset - stat_offset);
				if (group_sync && (ret = COsFsync::os_fsync(dbenv, fhp)) != 0)
					goto err;
			}
			if ((ret = os_physwrite(dbenv, fhp, bp, blen, &nw)) != 0)
				goto err;
			stat_offset += blen;
		}
		if ((ret = COsFsync::os_fsync(dbenv, fhp)) != 0)
			goto err;
	
		/* Seek back to where we started. */
		mbytes = (u_int32_t)(write_offset / MEGABYTE);
		bytes = (u_int32_t)(write_offset % MEGABYTE);
		ret = COsSeek::os_seek(dbenv, fhp, MEGABYTE, mbytes, bytes, 0, DB_OS_SEEK_SET);
	
	err:	if (need_free)
			COsAlloc::os_free(dbenv, bp);
		return (ret);
	}
#endif
};

