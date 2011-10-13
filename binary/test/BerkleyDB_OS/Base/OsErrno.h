#pragma once
#include "db_config.h"

#include "db_int.h"
#include "OsErrno.h"


// extracted from os_errno.c
#ifndef WIN32
#include <common/DbErr.h>
#endif

class COsErrno
{
public:
	static int os_get_errno_ret_zero()
	{
		/* This routine must be able to return the same value repeatedly. */
		return (errno);
	}

	static int os_get_errno()
	{
		/*
		 * This routine must be able to return the same value repeatedly.
		 *
		 * We've seen cases where system calls failed but errno was never set.
		 * This version of COsErrno::os_get_errno() sets errno to EAGAIN if it's not
		 * already set, to work around that problem.  For obvious reasons, we
		 * can only call this function if we know an error has occurred, that
		 * is, we can't test errno for a non-zero value after this call.
		 */
		if (errno == 0)
			os_set_errno(EAGAIN);
	
		return (errno);
	}

	static void os_set_errno(int evalue)
	{
		/*
		 * This routine is called by the compatibility interfaces (DB 1.85,
		 * dbm and hsearch).  Force values > 0, that is, not one of DB 2.X
		 * and later's public error returns.  If something bad has happened,
		 * default to EFAULT -- a nasty return.  Otherwise, default to EINVAL.
		 * As the compatibility APIs aren't included on Windows, the Windows
		 * version of this routine doesn't need this behavior.
		 */
		errno =
		    evalue >= 0 ? evalue : (evalue == DB_RUNRECOVERY ? EFAULT : EINVAL);
	}
};


