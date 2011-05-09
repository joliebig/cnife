#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#endif

#include "db_int.h"
#include "OsRoot.h"
#ifndef NO_SYSTEM_INCLUDES
#include <unistd.h>
#endif


class COsRoot
{
public:
	static int os_isroot()
	{
#ifdef HAVE_GETUID
		return (getuid() == 0);
#else
		return (0);
#endif
	}
};


