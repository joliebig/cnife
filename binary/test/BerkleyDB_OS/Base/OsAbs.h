#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#endif

#include "db_int.h"


// extracted from os_abs.c


class COsAbs
{
public:
	static int os_abspath(const char *path)
	{
		return (path[0] == '/');
	}
};



