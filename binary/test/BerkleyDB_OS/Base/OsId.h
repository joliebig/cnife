#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <stdlib.h>
#endif

#include "db_int.h"
#include "dbinc/mutex_int.h"
#include "OsId.h"

#ifndef NO_SYSTEM_INCLUDES
#include <unistd.h>
#endif




// extracted from os_id.c


class COsId
{
public:
	static void os_id(DB_ENV *dbenv, pid_t *pidp, db_threadid_t *tidp)
	{
		/*
		 * We can't depend on dbenv not being NULL, this routine is called
		 * from places where there's no DB_ENV handle.  It takes a DB_ENV
		 * handle as an arg because it's the default DB_ENV->thread_id function.
		 *
		 * We cache the pid in the DB_ENV handle, it's a fairly slow call on
		 * lots of systems.
		 */
		if (pidp != NULL) {
			if (dbenv == NULL) {
#if defined(HAVE_VXWORKS)
				*pidp = taskIdSelf();
#else
				*pidp = getpid();
#endif
			} else
				*pidp = dbenv->pid_cache;
		}
	
		if (tidp != NULL) {
#if defined(DB_WIN32)
			*tidp = GetCurrentThreadId();
#endif
#if not defined(DB_WIN32) && defined(HAVE_MUTEX_UI_THREADS)
			*tidp = thr_self();
#endif
#if not defined(DB_WIN32) && not defined(HAVE_MUTEX_UI_THREADS) && defined(HAVE_MUTEX_SOLARIS_LWP) || defined(HAVE_MUTEX_PTHREADS) || defined(HAVE_PTHREAD_SELF)
			*tidp = pthread_self();
#else
			/*
			 * Default to just getpid.
			 */
			*tidp = 0;
#endif
		}
	}
	static void os_unique_id(DB_ENV *dbenv, u_int32_t *idp)
	{
		static int first = 1;
		pid_t pid;
		db_threadid_t tid;
		u_int32_t id, sec, usec;
	
		*idp = 0;
	
		/*
		 * Our randomized value is comprised of our process ID, the current
		 * time of day and a couple of a stack addresses, all XOR'd together.
		 */
		os_id(dbenv, &pid, &tid);
		COsClock::os_clock(dbenv, &sec, &usec);
	
		id = (u_int32_t)pid ^ sec ^ usec ^ P_TO_UINT32(&pid);
	
		/*
		 * We could try and find a reasonable random-number generator, but
		 * that's not all that easy to do.  Seed and use srand()/rand(), if
		 * we can find them.
		 */
#if HAVE_SRAND
		if (first == 1)
			srand((u_int)id);
#endif
		first = 0;
	
#if HAVE_RAND
		id ^= (u_int)rand();
#endif
	
		*idp = id;
	}
};


