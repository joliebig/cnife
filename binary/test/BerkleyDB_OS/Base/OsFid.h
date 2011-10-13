#pragma once
#define _LARGEFILE_SOURCE 1
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#include <sys/stat.h>

#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#endif

#include "db_int.h"
#include "OsFid.h"
#include "OsErrno.h"
#include "OsId.h"
#ifndef WIN32
#include <common/DbErr.h>
#endif


class COsFid
{
public:
	static int os_fileid(DB_ENV *dbenv, const char *fname, int unique_okay, u_int8_t *fidp)
	{
		pid_t pid;
		db_threadid_t tid;
		struct stat sb;
		size_t i;
		int ret;
		u_int32_t tmp;
		u_int8_t *p;
	
		/* Clear the buffer. */
		memset(fidp, 0, DB_FILE_ID_LEN);
	
		/* On POSIX/UNIX, use a dev/inode pair. */
#ifdef HAVE_VXWORKS
		RETRY_CHK((stat((char *)fname, &sb)), ret);
#else
		#ifndef WIN32
			#if (__GNUC__ > 4 || (__GNUC__ == 4 && __GNUC_MINOR__ >= 1))
				RETRY_CHK((lstat(fname, &sb)), ret);
			#else
				RETRY_CHK((stat(fname, &sb)), ret);
			#endif
		#else
			RETRY_CHK((stat(fname, &sb)), ret);
		#endif
#endif
		if (ret != 0) {
			CDbErr::db_err(dbenv, "%s: %s", fname, strerror(ret));
			return (ret);
		}
	
		/*
		 * !!!
		 * Nothing is ever big enough -- on Sparc V9, st_ino, st_dev and the
		 * time_t types are all 8 bytes.  As DB_FILE_ID_LEN is only 20 bytes,
		 * we convert to a (potentially) smaller fixed-size type and use it.
		 *
		 * We don't worry about byte sexing or the actual variable sizes.
		 *
		 * When this routine is called from the DB access methods, it's only
		 * called once -- whatever ID is generated when a database is created
		 * is stored in the database file's metadata, and that is what is
		 * saved in the mpool region's information to uniquely identify the
		 * file.
		 *
		 * When called from the mpool layer this routine will be called each
		 * time a new thread of control wants to share the file, which makes
		 * things tougher.  As far as byte sexing goes, since the mpool region
		 * lives on a single host, there's no issue of that -- the entire
		 * region is byte sex dependent.  As far as variable sizes go, we make
		 * the simplifying assumption that 32-bit and 64-bit processes will
		 * get the same 32-bit values if we truncate any returned 64-bit value
		 * to a 32-bit value.  When we're called from the mpool layer, though,
		 * we need to be careful not to include anything that isn't
		 * reproducible for a given file, such as the timestamp or serial
		 * number.
		 */
		tmp = (u_int32_t)sb.st_ino;
		for (p = (u_int8_t *)&tmp, i = sizeof(u_int32_t); i > 0; --i)
			*fidp++ = *p++;
	
		tmp = (u_int32_t)sb.st_dev;
		for (p = (u_int8_t *)&tmp, i = sizeof(u_int32_t); i > 0; --i)
			*fidp++ = *p++;
	
		if (unique_okay) {
			static u_int32_t fid_serial = 0;
	
			/* Add in 32-bits of (hopefully) unique number. */
			COsId::os_unique_id(dbenv, &tmp);
			for (p = (u_int8_t *)&tmp, i = sizeof(u_int32_t); i > 0; --i)
				*fidp++ = *p++;
	
			/*
			 * Initialize/increment the serial number we use to help
			 * avoid fileid collisions.  Note we don't bother with
			 * locking; it's unpleasant to do from down in here, and
			 * if we race on this no real harm will be done, since the
			 * finished fileid has so many other components.
			 *
			 * We use the bottom 32-bits of the process ID, hoping they
			 * are more random than the top 32-bits (should we be on a
			 * machine with 64-bit process IDs).
			 *
			 * We increment by 100000 on each call as a simple way of
			 * randomizing; simply incrementing seems potentially less
			 * useful if pids are also simply incremented, since this
			 * is process-local and we may be one of a set of processes
			 * starting up.  100000 pushes us out of pid space on most
			 * 32-bit platforms, and has few interesting properties in
			 * base 2.
			 */
			if (fid_serial == 0) {
				dbenv->thread_id(dbenv, &pid, &tid);
				fid_serial = (u_int32_t)pid;
			} else
				fid_serial += 100000;
	
			for (p =
			    (u_int8_t *)&fid_serial, i = sizeof(u_int32_t); i > 0; --i)
				*fidp++ = *p++;
		}
	
		return (0);
	}
};


