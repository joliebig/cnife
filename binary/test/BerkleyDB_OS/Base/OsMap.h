#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#ifdef HAVE_MMAP
#include <sys/mman.h>
#endif

#ifdef HAVE_SHMGET
#include <sys/ipc.h>
#include <sys/shm.h>
#endif

#include <string.h>
#endif

#include "db_int.h"
#include "OsMap.h"
#include "OsOflags.h"

#ifdef HAVE_MMAP
static int __os_map __P((DB_ENV *, char *, DB_FH *, size_t, int, int, void **));
#endif
#ifndef HAVE_SHMGET
static int __db_nosystemmem __P((DB_ENV *));
#endif
#include <env/EnvFile.h>


class COsMap
{
public:
	static int os_r_sysattach(DB_ENV *dbenv, REGINFO *infop, REGION *rp)
	{
		if (F_ISSET(dbenv, DB_ENV_SYSTEM_MEM)) {
			/*
			 * If the region is in system memory on UNIX, we use shmget(2).
			 *
			 * !!!
			 * There exist spinlocks that don't work in shmget memory, e.g.,
			 * the HP/UX msemaphore interface.  If we don't have locks that
			 * will work in shmget memory, we better be private and not be
			 * threaded.  If we reach this point, we know we're public, so
			 * it's an error.
			 */
#if defined(HAVE_MUTEX_HPPA_MSEM_INIT)
			CDbErr::db_err(dbenv,
		    "architecture does not support locks inside system shared memory");
			return (EINVAL);
#endif
#if defined(HAVE_SHMGET)
			{
			key_t segid;
			int id, mode, ret;
	
			/*
			 * We could potentially create based on REGION_CREATE_OK, but
			 * that's dangerous -- we might get crammed in sideways if
			 * some of the expected regions exist but others do not.  Also,
			 * if the requested size differs from an existing region's
			 * actual size, then all sorts of nasty things can happen.
			 * Basing create solely on REGION_CREATE is much safer -- a
			 * recovery will get us straightened out.
			 */
			if (F_ISSET(infop, REGION_CREATE)) {
				/*
				 * The application must give us a base System V IPC key
				 * value.  Adjust that value based on the region's ID,
				 * and correct so the user's original value appears in
				 * the ipcs output.
				 */
				if (dbenv->shm_key == INVALID_REGION_SEGID) {
					CDbErr::db_err(dbenv,
				    "no base system shared memory ID specified");
					return (EINVAL);
				}
				segid = (key_t)(dbenv->shm_key + (infop->id - 1));
	
				/*
				 * If map to an existing region, assume the application
				 * crashed and we're restarting.  Delete the old region
				 * and re-try.  If that fails, return an error, the
				 * application will have to select a different segment
				 * ID or clean up some other way.
				 */
				if ((id = shmget(segid, 0, 0)) != -1) {
					(void)shmctl(id, IPC_RMID, NULL);
					if ((id = shmget(segid, 0, 0)) != -1) {
						CDbErr::db_err(dbenv,
			"shmget: key: %ld: shared system memory region already exists",
						    (long)segid);
						return (EAGAIN);
					}
				}
	
				/*
				 * Map the DbEnv::open method file mode permissions to
				 * shmget call permissions.
				 */
				mode = IPC_CREAT | COsOflags::db_shm_mode(dbenv);
				if ((id = shmget(segid, rp->size, mode)) == -1) {
					ret = COsErrno::os_get_errno();
					CDbErr::db_err(dbenv,
		"shmget: key: %ld: unable to create shared system memory region: %s",
					    (long)segid, strerror(ret));
					return (ret);
				}
				rp->segid = id;
			} else
				id = rp->segid;
	
			if ((infop->addr = shmat(id, NULL, 0)) == (void *)-1) {
				infop->addr = NULL;
				ret = COsErrno::os_get_errno();
				CDbErr::db_err(dbenv,
		"shmat: id %d: unable to attach to shared system memory region: %s",
				    id, strerror(ret));
				return (ret);
			}
	
			return (0);
			}
#else
			return (db_nosystemmem(dbenv));
#endif
		}
	
#ifdef HAVE_MMAP
		{
		DB_FH *fhp;
		int ret;
	
		fhp = NULL;
	
		/*
		 * Try to open/create the shared region file.  We DO NOT need to ensure
		 * that multiple threads/processes attempting to simultaneously create
		 * the region are properly ordered, our caller has already taken care
		 * of that.
		 */
		if ((ret = COsOpen::os_open(dbenv, infop->name,
		    DB_OSO_REGION |
		    (F_ISSET(infop, REGION_CREATE_OK) ? DB_OSO_CREATE : 0),
		    dbenv->db_mode, &fhp)) != 0)
			CDbErr::db_err(dbenv, "%s: %s", infop->name, CDbErr::db_strerror(ret));
	
		/*
		 * If we created the file, grow it to its full size before mapping
		 * it in.  We really want to avoid touching the buffer cache after
		 * mmap(2) is called, doing anything else confuses the hell out of
		 * systems without merged VM/buffer cache systems, or, more to the
		 * point, *badly* merged VM/buffer cache systems.
		 */
		if (ret == 0 && F_ISSET(infop, REGION_CREATE)) {
			if (F_ISSET(dbenv, DB_ENV_REGION_INIT))
				ret = CEnvFile::db_file_write(dbenv, "region file", fhp,
				    rp->size / MEGABYTE, rp->size % MEGABYTE, 0x00);
			else
				ret = CEnvFile::db_file_extend(dbenv, fhp, rp->size);
		}
	
		/* Map the file in. */
		if (ret == 0)
			ret = os_map(dbenv,
			    infop->name, fhp, rp->size, 1, 0, &infop->addr);
	
		if (fhp != NULL)
			(void)COsHandle::os_closehandle(dbenv, fhp);
	
		return (ret);
		}
#else
		COMPQUIET(infop, NULL);
		COMPQUIET(rp, NULL);
		CDbErr::db_err(dbenv,
		    "architecture lacks mmap(2), shared environments not possible");
		return (DB_OPNOTSUP);
#endif
	}

	static int os_r_sysdetach(DB_ENV *dbenv, REGINFO *infop, int destroy)
	{
		REGION *rp;
	
		rp = infop->rp;
	
		if (F_ISSET(dbenv, DB_ENV_SYSTEM_MEM)) {
#ifdef HAVE_SHMGET
			int ret, segid;
	
			/*
			 * We may be about to remove the memory referenced by rp,
			 * save the segment ID, and (optionally) wipe the original.
			 */
			segid = rp->segid;
			if (destroy)
				rp->segid = INVALID_REGION_SEGID;
	
			if (shmdt(infop->addr) != 0) {
				ret = COsErrno::os_get_errno();
				CDbErr::db_err(dbenv, "shmdt: %s", strerror(ret));
				return (ret);
			}
	
			if (destroy && shmctl(segid, IPC_RMID,
			    NULL) != 0 && (ret = COsErrno::os_get_errno()) != EINVAL) {
				CDbErr::db_err(dbenv,
		    "shmctl: id %d: unable to delete system shared memory region: %s",
				    segid, strerror(ret));
				return (ret);
			}
	
			return (0);
#else
			return (db_nosystemmem(dbenv));
#endif
		}
	
#ifdef HAVE_MMAP
#ifdef HAVE_MUNLOCK
		if (F_ISSET(dbenv, DB_ENV_LOCKDOWN))
			(void)munlock(infop->addr, rp->size);
#endif
		if (munmap(infop->addr, rp->size) != 0) {
			int ret;
	
			ret = COsErrno::os_get_errno();
			CDbErr::db_err(dbenv, "munmap: %s", strerror(ret));
			return (ret);
		}
	
		if (destroy && COsUnlink::os_region_unlink(dbenv, infop->name) != 0)
			return (COsErrno::os_get_errno());
	
		return (0);
#else
		COMPQUIET(destroy, 0);
		return (EINVAL);
#endif
	}
	

	static int os_mapfile(DB_ENV *dbenv, char *path, DB_FH *fhp, size_t len, int is_rdonly, void **addrp)
	{
#if defined(HAVE_MMAP) && !defined(HAVE_QNX)
		return (os_map(dbenv, path, fhp, len, 0, is_rdonly, addrp));
#else
		COMPQUIET(dbenv, NULL);
		COMPQUIET(path, NULL);
		COMPQUIET(fhp, NULL);
		COMPQUIET(is_rdonly, 0);
		COMPQUIET(len, 0);
		COMPQUIET(addrp, NULL);
		return (EINVAL);
#endif
	}

	static int os_unmapfile(DB_ENV *dbenv, void *addr, size_t len)
	{
		int ret;
	
		/* If the user replaced the map call, call through their interface. */
		if (DB_GLOBAL(j_unmap) != NULL)
			return (DB_GLOBAL(j_unmap)(addr, len));
	
#ifdef HAVE_MMAP
#ifdef HAVE_MUNLOCK
		if (F_ISSET(dbenv, DB_ENV_LOCKDOWN))
			RETRY_CHK((munlock(addr, len)), ret);
#else
		COMPQUIET(dbenv, NULL);
#endif
		RETRY_CHK((munmap(addr, len)), ret);
		return (ret);
#else
		COMPQUIET(dbenv, NULL);
	
		return (EINVAL);
#endif
	}
	

#ifdef HAVE_MMAP
	static int os_map(DB_ENV *dbenv, char *path, DB_FH *fhp, size_t len, int is_region, int is_rdonly, void **addrp)
	{
		void *p;
		int flags, prot, ret;
	
		/* If the user replaced the map call, call through their interface. */
		if (DB_GLOBAL(j_map) != NULL)
			return (DB_GLOBAL(j_map)
			    (path, len, is_region, is_rdonly, addrp));
	
		/* Check for illegal usage. */
		DB_ASSERT(F_ISSET(fhp, DB_FH_OPENED) && fhp->fd != -1);
	
		/*
		 * If it's read-only, it's private, and if it's not, it's shared.
		 * Don't bother with an additional parameter.
		 */
		flags = is_rdonly ? MAP_PRIVATE : MAP_SHARED;
	
#ifdef MAP_FILE
		/*
		 * Historically, MAP_FILE was required for mapping regular files,
		 * even though it was the default.  Some systems have it, some
		 * don't, some that have it set it to 0.
		 */
		flags |= MAP_FILE;
#endif
	
		/*
		 * I know of no systems that implement the flag to tell the system
		 * that the region contains semaphores, but it's not an unreasonable
		 * thing to do, and has been part of the design since forever.  I
		 * don't think anyone will object, but don't set it for read-only
		 * files, it doesn't make sense.
		 */
#ifdef MAP_HASSEMAPHORE
		if (is_region && !is_rdonly)
			flags |= MAP_HASSEMAPHORE;
#else
		COMPQUIET(is_region, 0);
#endif
	
		/*
		 * FreeBSD:
		 * Causes data dirtied via this VM map to be flushed to physical media
		 * only when necessary (usually by the pager) rather then gratuitously.
		 * Typically this prevents the update daemons from flushing pages
		 * dirtied through such maps and thus allows efficient sharing of
		 * memory across unassociated processes using a file-backed shared
		 * memory map.
		 */
#ifdef MAP_NOSYNC
		flags |= MAP_NOSYNC;
#endif
	
		prot = PROT_READ | (is_rdonly ? 0 : PROT_WRITE);
	
		/*
		 * XXX
		 * Work around a bug in the VMS V7.1 mmap() implementation.  To map
		 * a file into memory on VMS it needs to be opened in a certain way,
		 * originally.  To get the file opened in that certain way, the VMS
		 * mmap() closes the file and re-opens it.  When it does this, it
		 * doesn't flush any caches out to disk before closing.  The problem
		 * this causes us is that when the memory cache doesn't get written
		 * out, the file isn't big enough to match the memory chunk and the
		 * mmap() call fails.  This call to fsync() fixes the problem.  DEC
		 * thinks this isn't a bug because of language in XPG5 discussing user
		 * responsibility for on-disk and in-memory synchronization.
		 */
#ifdef VMS
		if (COsFsync::os_fsync(dbenv, fhp) == -1)
			return (COsErrno::os_get_errno());
#endif
	
		/* MAP_FAILED was not defined in early mmap implementations. */
#ifndef MAP_FAILED
#define	MAP_FAILED	-1
#endif
		if ((p = mmap(NULL,
		    len, prot, flags, fhp->fd, (off_t)0)) == (void *)MAP_FAILED) {
			ret = COsErrno::os_get_errno();
			CDbErr::db_err(dbenv, "mmap: %s", strerror(ret));
			return (ret);
		}
	
#ifdef HAVE_MLOCK
		/*
		 * If it's a region, we want to make sure that the memory isn't paged.
		 * For example, Solaris will page large mpools because it thinks that
		 * I/O buffer memory is more important than we are.  The mlock system
		 * call may or may not succeed (mlock is restricted to the super-user
		 * on some systems).  Currently, the only other use of mmap in DB is
		 * to map read-only databases -- we don't want them paged, either, so
		 * the call isn't conditional.
		 */
		if (F_ISSET(dbenv, DB_ENV_LOCKDOWN) && mlock(p, len) != 0) {
			ret = COsErrno::os_get_errno();
			(void)munmap(p, len);
			CDbErr::db_err(dbenv, "mlock: %s", strerror(ret));
			return (ret);
		}
#else
		COMPQUIET(dbenv, NULL);
#endif
	
		*addrp = p;
		return (0);
	}
#endif

#ifndef HAVE_SHMGET
	static int db_nosystemmem(DB_ENV *dbenv)
	{
		CDbErr::db_err(dbenv,
		    "architecture doesn't support environments in system memory");
		return (DB_OPNOTSUP);
	}
#endif
};

