#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <fcntl.h>
#include <string.h>
#endif

#include "db_int.h"
#include "OsFlock.h"


// extracted from os_flock.c


class COsFlock
{
public:
	static int os_fdlock(DB_ENV *dbenv, DB_FH *fhp, off_t offset, int acquire, int nowait)
	{
		DB_ASSERT(F_ISSET(fhp, DB_FH_OPENED) && fhp->fd != -1);
	
#ifdef HAVE_FCNTL
		int ret;
		struct flock fl;
		fl.l_start = offset;
		fl.l_len = 1;
		fl.l_type = acquire ? F_WRLCK : F_UNLCK;
		fl.l_whence = SEEK_SET;
	
		RETRY_CHK_EINTR_ONLY(
		    (fcntl(fhp->fd, nowait ? F_SETLK : F_SETLKW, &fl)), ret);
	
		if (ret != 0 && ret != EACCES && ret != EAGAIN)
			CDbErr::db_err(dbenv, "fcntl: %s", strerror(ret));
		return (ret);
#else
		CDbErr::db_err(dbenv,
		    "advisory file locking unavailable: %s", strerror(DB_OPNOTSUP));
		return (DB_OPNOTSUP);
#endif
	}
};


