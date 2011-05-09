#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <string.h>
#endif

#include "db_int.h"
#include "OsRename.h"
#include "OsErrno.h"


class COsRename
{
public:
	static int os_rename(DB_ENV *dbenv, const char *old, const char *new__renamed, u_int32_t silent)
	{
		int ret;
	
		if (DB_GLOBAL(j_rename) != NULL)
			ret = DB_GLOBAL(j_rename)(old, new__renamed);
		else
			RETRY_CHK((rename(old, new__renamed)), ret);
	
		/*
		 * If "silent" is not set, then errors are OK and we should not output
		 * an error message.
		 */
		if (!silent && ret != 0)
			CDbErr::db_err(dbenv, "rename %s %s: %s", old, new__renamed, strerror(ret));
		return (ret);
	}
};


