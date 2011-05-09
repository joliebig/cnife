#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <stdlib.h>
#include <string.h>
#endif

#include "db_int.h"
#include "OsSeek.h"

#ifndef WIN32
#include <unistd.h>
#endif

class COsSeek
{
public:
	static int os_seek(DB_ENV *dbenv, DB_FH *fhp, u_int32_t pgsize, db_pgno_t pageno, u_int32_t relative, int isrewind, DB_OS_SEEK db_whence)
	{
		off_t offset;
		int ret, whence;
	
		/* Check for illegal usage. */
		DB_ASSERT(F_ISSET(fhp, DB_FH_OPENED) && fhp->fd != -1);
	
		switch (db_whence) {
		case DB_OS_SEEK_CUR:
			whence = SEEK_CUR;
			break;
		case DB_OS_SEEK_END:
			whence = SEEK_END;
			break;
		case DB_OS_SEEK_SET:
			whence = SEEK_SET;
			break;
		default:
			return (EINVAL);
		}
	
		offset = (off_t)pgsize * pageno + relative;
		if (isrewind)
			offset = -offset;
	
		if (DB_GLOBAL(j_seek) != NULL)
			ret = DB_GLOBAL(j_seek)(fhp->fd, offset, whence);
		else
			RETRY_CHK((lseek(fhp->fd, offset, whence) == -1 ? 1 : 0), ret);
	
		if (ret == 0) {
			fhp->pgsize = pgsize;
			fhp->pgno = pageno;
			fhp->offset = relative;
		} else
			CDbErr::db_err(dbenv, "seek: %lu %d %d: %s",
			    (u_long)pgsize * pageno + relative,
			    isrewind, db_whence, strerror(ret));
	
		return (ret);
	}
};


