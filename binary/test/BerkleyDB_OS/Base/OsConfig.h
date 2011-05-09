#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#endif

#include "db_int.h"
#include "OsConfig.h"



// extracted from os_config.c


class COsConfig
{
public:
	static int os_fs_notzero()
	{
		/* Most filesystems zero out implicitly created pages. */
		return (0);
	}
	
	static int os_support_db_register()
	{
		return (1);
	}

	static int os_support_replication()
	{
		return (1);
	}
};


