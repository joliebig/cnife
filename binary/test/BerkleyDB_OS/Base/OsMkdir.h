#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#include <sys/stat.h>
#endif

#include "db_int.h"
#include "OsMkdir.h"


class COsMkdir
{
public:
	static int os_mkdir(DB_ENV *dbenv, const char *name, int mode)
	{
		int ret;
	
		COMPQUIET(dbenv, NULL);
	
		/* Make the directory, with paranoid permissions. */
#ifdef HAVE_VXWORKS
		RETRY_CHK((mkdir((char *)name)), ret);
#else
#ifdef DB_WIN32
		RETRY_CHK((_mkdir(name)), ret);
#else
		RETRY_CHK((mkdir(name, 0600)), ret);
#endif
		if (ret != 0)
			return (ret);
	
		/* Set the absolute permissions, if specified. */
#ifndef DB_WIN32
		if (mode != 0)
			RETRY_CHK((chmod(name, mode)), ret);
#endif
#endif
		return (ret);
	}
};

