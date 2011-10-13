#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <fcntl.h>			/* Required on some platforms. */
#include <string.h>
#endif

#include "db_int.h"
#include "OsFsync.h"
#include "OsErrno.h"

#ifndef WIN32
#include <unistd.h>
#endif

#ifdef	HAVE_VXWORKS
#include "ioLib.h"

#define	fsync(fd)	__vx_fsync(fd)
#endif

class COsFsync
{
public:
#ifdef	HAVE_VXWORKS
	
	static int vx_fsync(int fd)
	{
		int ret;
	
		/*
		 * The results of ioctl are driver dependent.  Some will return the
		 * number of bytes sync'ed.  Only if it returns 'ERROR' should we
		 * flag it.
		 */
		if ((ret = ioctl(fd, FIOSYNC, 0)) != ERROR)
			return (0);
		return (ret);
	}
#endif

#ifdef __hp3000s900
#define	fsync(fd)	__mpe_fsync(fd)
	static int mpe_fsync(int fd)
	{
		extern FCONTROL(short, short, void *);
	
		FCONTROL(_MPE_FILENO(fd), 2, NULL);	/* Flush the buffers */
		FCONTROL(_MPE_FILENO(fd), 6, NULL);	/* Write the EOF */
		return (0);
	}
#endif
	static int os_fsync(DB_ENV *dbenv, DB_FH *fhp)
	{
		int ret;
	
		/* Check for illegal usage. */
		DB_ASSERT(F_ISSET(fhp, DB_FH_OPENED) && fhp->fd != -1);
	
		/*
		 * Do nothing if the file descriptor has been marked as not requiring
		 * any sync to disk.
		 */
		if (F_ISSET(fhp, DB_FH_NOSYNC))
			return (0);
	
		if (DB_GLOBAL(j_fsync) != NULL)
			ret = DB_GLOBAL(j_fsync)(fhp->fd);
		else
#if defined(F_FULLFSYNC)
			RETRY_CHK((fcntl(fhp->fd, F_FULLFSYNC, 0)), ret);
#endif
#if not defined(F_FULLFSYNC) && defined(HAVE_FDATASYNC)
			RETRY_CHK((fdatasync(fhp->fd)), ret);
#else
			RETRY_CHK((fsync(fhp->fd)), ret);
#endif
	
		if (ret != 0)
			CDbErr::db_err(dbenv, "fsync %s", strerror(ret));
		return (ret);
	}
};


