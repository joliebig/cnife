
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#if defined(HAVE_PSTAT_GETDYNAMIC)
#include <sys/pstat.h>
#endif

#include <limits.h>			/* Needed for sysconf on Solaris. */
#endif

#include "db_int.h"
#include "OsSpin.h"
#include "OsSleep.h"

#if defined(HAVE_PSTAT_GETDYNAMIC)
static int __os_pstat_getdynamic __P((void));
#endif
#if defined(HAVE_SYSCONF) && defined(_SC_NPROCESSORS_ONLN)
static u_int32_t __os_sysconf __P((void));
#endif


class COsSpin
{
public:
#if defined(HAVE_PSTAT_GETDYNAMIC)
	static int os_pstat_getdynamic()
	{
		struct pst_dynamic psd;
	
		return (pstat_getdynamic(&psd,
		    sizeof(psd), (size_t)1, 0) == -1 ? 1 : psd.psd_proc_cnt);
	}

#endif

#if defined(HAVE_SYSCONF) && defined(_SC_NPROCESSORS_ONLN)
	static u_int32_t os_sysconf()
	{
		long nproc;
	
		nproc = sysconf(_SC_NPROCESSORS_ONLN);
		return ((u_int32_t)(nproc > 1 ? nproc : 1));
	}
#endif


	static u_int32_t os_spin(DB_ENV *dbenv)
	{
		u_int32_t tas_spins;
	
		COMPQUIET(dbenv, NULL);
	
		tas_spins = 1;
#if defined(HAVE_PSTAT_GETDYNAMIC)
		tas_spins = os_pstat_getdynamic();
#endif
#if defined(HAVE_SYSCONF) && defined(_SC_NPROCESSORS_ONLN)
		tas_spins = os_sysconf();
#endif
	
		/*
		 * Spin 50 times per processor, we have anecdotal evidence that this
		 * is a reasonable value.
		 */
		if (tas_spins != 1)
			tas_spins *= 50;
	
		return (tas_spins);
	}
	

	static void os_yield(DB_ENV *dbenv, u_long usecs)
	{
		if (DB_GLOBAL(j_yield) != NULL && DB_GLOBAL(j_yield)() == 0)
			return;
#ifdef HAVE_VXWORKS
		taskDelay(1);
#endif
		COsSleep::os_sleep(dbenv, 0, usecs);
	}
};


