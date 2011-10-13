/*-
* See the file LICENSE for redistribution information.
*
* Copyright (c) 1999-2005
*	Sleepycat Software.  All rights reserved.
*
* $Id: HashMethod.cpp,v 1.4 2006/08/16 17:43:23 rosenmue Exp $
*/

#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#endif

#include "db_int.h"
#include "db_page.h"
#include "hash.h"
#include "HashMethod.h"
#include "DbMethod.h"
#include "EnvMethod.h"


class CHashMethod
{
public:

#ifndef HAVE_HASH
	static int ham_db_create(DB *dbp)
	{
		COMPQUIET(dbp, NULL);
		return (0);
	}


	static int ham_db_close(DB *dbp)
	{
		COMPQUIET(dbp, NULL);
		return (0);
	}

	static int ham_get_h_ffactor(DB *dbp, u_int32_t *h_ffactorp);
	static int ham_set_h_ffactor(DB *dbp, u_int32_t h_ffactor);
	static int ham_set_h_hash(DB *dbp, u_int32_t (*func) __P((DB *, const void *, u_int32_t)));
	static int ham_get_h_nelem(DB *dbp, u_int32_t *h_nelemp);
	static int ham_set_h_nelem(DB *dbp, u_int32_t h_nelem);

#else
	/*
	* __ham_db_create --
	*	Hash specific initialization of the DB structure.
	*
	* PUBLIC: int __ham_db_create __P((DB *));
	*/
	static int ham_db_create(DB *dbp)
	{
		HASH *hashp;
		int ret;

		if ((ret = COsAlloc::os_malloc(dbp->dbenv,
			sizeof(HASH), &dbp->h_internal)) != 0)
			return (ret);

		hashp = static_cast<HASH*>(dbp->h_internal);

		hashp->h_nelem = 0;			/* Defaults. */
		hashp->h_ffactor = 0;
		hashp->h_hash = NULL;

		dbp->get_h_ffactor = ham_get_h_ffactor;
		dbp->set_h_ffactor = ham_set_h_ffactor;
		dbp->set_h_hash = ham_set_h_hash;
		dbp->get_h_nelem = ham_get_h_nelem;
		dbp->set_h_nelem = ham_set_h_nelem;

		return (0);
	}

	/*
	* PUBLIC: int __ham_db_close __P((DB *));
	*/

	static int ham_db_close(DB *dbp)
	{
		if (dbp->h_internal == NULL)
			return (0);
		COsAlloc::os_free(dbp->dbenv, dbp->h_internal);
		dbp->h_internal = NULL;
		return (0);
	}



	/*
	* __ham_get_h_ffactor --
	*
	* PUBLIC: int __ham_get_h_ffactor __P((DB *, u_int32_t *));
	*/

	static int ham_get_h_ffactor(DB *dbp, u_int32_t *h_ffactorp)
	{
		HASH *hashp;

		hashp = static_cast<HASH*>(dbp->h_internal);
		*h_ffactorp = hashp->h_ffactor;
		return (0);
	}

	/*
	* __ham_set_h_ffactor --
	*	Set the fill factor.
	*/

	static int ham_set_h_ffactor(DB *dbp, u_int32_t h_ffactor)
	{
		HASH *hashp;

		DB_ILLEGAL_AFTER_OPEN(dbp, "DB->set_h_ffactor");
		DB_ILLEGAL_METHOD(dbp, DB_OK_HASH);

		hashp = static_cast<HASH*>(dbp->h_internal);
		hashp->h_ffactor = h_ffactor;
		return (0);
	}

	/*
	* __ham_set_h_hash --
	*	Set the hash function.
	*/

	static int ham_set_h_hash(DB *dbp, u_int32_t (*func) __P((DB *, const void *, u_int32_t)))
	{
		HASH *hashp;

		DB_ILLEGAL_AFTER_OPEN(dbp, "DB->set_h_hash");
		DB_ILLEGAL_METHOD(dbp, DB_OK_HASH);

		hashp = static_cast<HASH*>(dbp->h_internal);
		hashp->h_hash = func;
		return (0);
	}

	/*
	* __db_get_h_nelem --
	*
	* PUBLIC: int __ham_get_h_nelem __P((DB *, u_int32_t *));
	*/

	static int ham_get_h_nelem(DB *dbp, u_int32_t *h_nelemp)
	{
		HASH *hashp;

		DB_ILLEGAL_METHOD(dbp, DB_OK_HASH);

		hashp = static_cast<HASH*>(dbp->h_internal);
		*h_nelemp = hashp->h_nelem;
		return (0);
	}

	/*
	* __ham_set_h_nelem --
	*	Set the table size.
	*/

	static int ham_set_h_nelem(DB *dbp, u_int32_t h_nelem)
	{
		HASH *hashp;

		DB_ILLEGAL_AFTER_OPEN(dbp, "DB->set_h_nelem");
		DB_ILLEGAL_METHOD(dbp, DB_OK_HASH);

		hashp = static_cast<HASH*>(dbp->h_internal);
		hashp->h_nelem = h_nelem;
		return (0);
	}
#endif	//#ifndef HAVE_HASH #else

};

