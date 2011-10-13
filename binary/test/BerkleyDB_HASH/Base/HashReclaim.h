/*-
* See the file LICENSE for redistribution information.
*
* Copyright (c) 1996-2005
*	Sleepycat Software.  All rights reserved.
*
* $Id: HashReclaim.cpp,v 1.4 2006/08/16 17:43:21 rosenmue Exp $
*/

#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#endif

#include "db_int.h"
#include "db_page.h"
#include "hash.h"
#include "HashReclaim.h"
#include "HashMeta.h"
#include "HashStat.h"
#include "DbReclaim.h"
#include "DbIface.h"
#include "DbCam.h"
#include "Hash.h"




class CHashReclaim
{
public:

#ifndef HAVE_HASH

	static int ham_reclaim(DB *dbp, DB_TXN *txn)
	{
		COMPQUIET(txn, NULL);
		return (CHash::db_no_hash_am(dbp->dbenv));
	}

	/*
	* __ham_truncate --
	*	Reclaim the pages from a subdatabase and return them to the
	* parent free list.
	*
	* PUBLIC: int __ham_truncate __P((DBC *, u_int32_t *));
	*/

	static int ham_truncate(DBC *dbc, u_int32_t *countp)
	{
		COMPQUIET(dbc, NULL);
		COMPQUIET(countp, NULL);
		return (CHash::db_no_hash_am(dbc->dbp->dbenv));
	}

#else	//#ifndef HAVE_HASH
	/*
	* __ham_reclaim --
	*	Reclaim the pages from a subdatabase and return them to the
	* parent free list.  For now, we link each freed page on the list
	* separately.  If people really store hash databases in subdatabases
	* and do a lot of creates and deletes, this is going to be a problem,
	* because hash needs chunks of contiguous storage.  We may eventually
	* need to go to a model where we maintain the free list with chunks of
	* contiguous pages as well.
	*
	* PUBLIC: int __ham_reclaim __P((DB *, DB_TXN *txn));
	*/
	static int ham_reclaim(DB *dbp, DB_TXN *txn)
	{
		DBC *dbc;
		HASH_CURSOR *hcp;
		int ret;

		/* Open up a cursor that we'll use for traversing. */
		if ((ret = CDbIface::db_cursor(dbp, txn, &dbc, 0)) != 0)
			return (ret);
		hcp = (HASH_CURSOR *)dbc->internal;

		if ((ret = CHashMeta::ham_get_meta(dbc)) != 0)
			goto err;

		if ((ret = CHashStat::ham_traverse(dbc,
			DB_LOCK_WRITE, CDbReclaim::db_reclaim_callback, dbc, 1)) != 0)
			goto err;
		if ((ret = CDbCam::db_c_close(dbc)) != 0)
			goto err;
		if ((ret = CHashMeta::ham_release_meta(dbc)) != 0)
			goto err;
		return (0);

err:	if (hcp->hdr != NULL)
			(void)CHashMeta::ham_release_meta(dbc);
		(void)CDbCam::db_c_close(dbc);
		return (ret);
	}

	/*
	* __ham_truncate --
	*	Reclaim the pages from a subdatabase and return them to the
	* parent free list.
	*
	* PUBLIC: int __ham_truncate __P((DBC *, u_int32_t *));
	*/
	static int ham_truncate(DBC *dbc, u_int32_t *countp)

	{
		db_trunc_param trunc;
		int ret, t_ret;

		if ((ret = CHashMeta::ham_get_meta(dbc)) != 0)
			return (ret);

		trunc.count = 0;
		trunc.dbc = dbc;

		ret = CHashStat::ham_traverse(dbc,
			DB_LOCK_WRITE, CDbReclaim::db_truncate_callback, &trunc, 1);

		if ((t_ret = CHashMeta::ham_release_meta(dbc)) != 0 && ret == 0)
			ret = t_ret;

		if (countp != NULL)
			*countp = trunc.count;
		return (ret);
	}

#endif	//#ifndef HAVE_HASH #else

};
