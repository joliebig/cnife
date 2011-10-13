/*-
* See the file LICENSE for redistribution information.
*
* Copyright (c) 1999-2005
*	Sleepycat Software.  All rights reserved.
*
* $Id: HashMeta.cpp,v 1.5 2006/08/17 12:39:09 rosenmue Exp $
*/
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#endif

#include "db_int.h"
#include "db_page.h"
#include "db_shash.h"
#include "hash.h"
#include "lock.h"
#include "mp.h"
#include "HashMeta.h"
#include "DbMeta.h"
#include "MpFget.h"
#include "MpFput.h"
#include "Lock.h"

/*
* Acquire the meta-data page.
*
* PUBLIC: int __ham_get_meta __P((DBC *));
*/



class CHashMeta
{
#ifdef HAVE_HASH

public:
	static int ham_get_meta(DBC *dbc)
	{
		DB *dbp;
		DB_MPOOLFILE *mpf;
		HASH *hashp;
		HASH_CURSOR *hcp;
		int ret;

		dbp = dbc->dbp;
		mpf = dbp->mpf;
		hashp = static_cast<HASH*>(dbp->h_internal);
		hcp = (HASH_CURSOR *)dbc->internal;

		if ((ret = CDbMeta::db_lget(dbc, 0,
			hashp->meta_pgno, DB_LOCK_READ, 0, &hcp->hlock)) != 0)
			return (ret);

		if ((ret = CMpFget::memp_fget(mpf,
			&hashp->meta_pgno, DB_MPOOL_CREATE, &(hcp->hdr))) != 0)
			(void)__LPUT(dbc, hcp->hlock);

		return (ret);
	}

	/*
	* Release the meta-data page.
	*
	* PUBLIC: int __ham_release_meta __P((DBC *));
	*/

	static int ham_release_meta(DBC *dbc)
	{
		DB_MPOOLFILE *mpf;
		HASH_CURSOR *hcp;

		mpf = dbc->dbp->mpf;
		hcp = (HASH_CURSOR *)dbc->internal;

		if (hcp->hdr)
			(void)CMpFput::memp_fput(mpf, hcp->hdr,
			F_ISSET(hcp, H_DIRTY) ? DB_MPOOL_DIRTY : 0);
		hcp->hdr = NULL;
		F_CLR(hcp, H_DIRTY);

		return (__TLPUT(dbc, hcp->hlock));
	}

	/*
	* Mark the meta-data page dirty.
	*
	* PUBLIC: int __ham_dirty_meta __P((DBC *));
	*/

	static int ham_dirty_meta(DBC *dbc)
	{
		DB *dbp;
		HASH *hashp;
		HASH_CURSOR *hcp;
		int ret;

		dbp = dbc->dbp;
		hashp = static_cast<HASH*>(dbp->h_internal);
		hcp = (HASH_CURSOR *)dbc->internal;

		ret = 0;

		ret = CDbMeta::db_lget(dbc, LCK_COUPLE,
			hashp->meta_pgno, DB_LOCK_WRITE, 0, &hcp->hlock);

		if (ret == 0)
			F_SET(hcp, H_DIRTY);
		return (ret);
	}

#else	//#ifdef HAVE_HASH

	static int ham_get_meta(DBC *dbc);
	static int ham_release_meta(DBC *dbc);
	static int ham_dirty_meta(DBC *dbc);

#endif	//#ifdef HAVE_HASH #else

};
