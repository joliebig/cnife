
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <string.h>
#endif

#include "db_int.h"
#include "OsUnlink.h"
#include "env/EnvFile.h"

#ifndef WIN32
#include <unistd.h>
#endif

class COsUnlink
{
public:
	static int os_region_unlink(DB_ENV *dbenv, const char *path)
	{
#ifdef HAVE_QNX
		int ret;
		char *newname;
	
		if ((ret = __os_shmname(dbenv, path, &newname)) != 0)
			goto err;
	
		if ((ret = shm_unlink(newname)) != 0) {
			ret = COsErrno::os_get_errno();
			if (ret != ENOENT)
				CDbErr::db_err(dbenv, "shm_unlink: %s: %s",
				    newname, strerror(ret));
		}
	err:
		if (newname != NULL)
			COsAlloc::os_free(dbenv, newname);
		return (ret);
#else
		if (F_ISSET(dbenv, DB_ENV_OVERWRITE))
			(void)CEnvFile::db_file_multi_write(dbenv, path);
	
		return (os_unlink(dbenv, path));
#endif
	}

	static int os_unlink(DB_ENV *dbenv, const char *path)
	{
		int ret;
	
		if (DB_GLOBAL(j_unlink) != NULL)
			ret = DB_GLOBAL(j_unlink)(path);
		else
#ifdef HAVE_VXWORKS
		    RETRY_CHK((unlink((char *)path)), ret);
#else
		    RETRY_CHK((unlink(path)), ret);
#endif
		/*
		 * !!!
		 * The results of unlink are file system driver specific on VxWorks.
		 * In the case of removing a file that did not exist, some, at least,
		 * return an error, but with an errno of 0, not ENOENT.  We do not
		 * have to test for the explicitly, the RETRY_CHK macro resets "ret"
		 * to be the errno, and so we'll just slide right on through.
		 *
		 * XXX
		 * We shouldn't be testing for an errno of ENOENT here, but ENOENT
		 * signals that a file is missing, and we attempt to unlink things
		 * (such as v. 2.x environment regions, in DB_ENV->remove) that we
		 * are expecting not to be there.  Reporting errors in these cases
		 * is annoying.
		 */
		if (ret != 0 && ret != ENOENT)
			CDbErr::db_err(dbenv, "unlink: %s: %s", path, strerror(ret));
	
		return (ret);
	}
};


