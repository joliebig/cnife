#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <string.h>
#endif

#include "db_int.h"
#include "OsTruncate.h"
#include "OsErrno.h"

#ifndef WIN32
#include <unistd.h>
#endif

class COsTruncate
{
public:
	static int os_truncate(DB_ENV *dbenv, DB_FH *fhp, db_pgno_t pgno, u_int32_t pgsize)
	{
		off_t offset;
		int ret;
	
		/*
		 * Truncate a file so that "pgno" is discarded from the end of the
		 * file.
		 */
		offset = (off_t)pgsize * pgno;
	
		if (DB_GLOBAL(j_ftruncate) != NULL)
			ret = DB_GLOBAL(j_ftruncate)(fhp->fd, offset);
		else {
#ifdef HAVE_FTRUNCATE
			RETRY_CHK((ftruncate(fhp->fd, offset)), ret);
#else
			ret = DB_OPNOTSUP;
#endif
		}
	
		if (ret != 0)
			CDbErr::db_err(dbenv,
			    "ftruncate: %lu: %s", (u_long)offset, strerror(ret));
	
		return (ret);
	}
};


