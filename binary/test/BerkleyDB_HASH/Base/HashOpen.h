/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 1996-2005
 *	Sleepycat Software.  All rights reserved.
 */
/*
 * Copyright (c) 1990, 1993, 1994
 *	Margo Seltzer.  All rights reserved.
 */
/*
 * Copyright (c) 1990, 1993, 1994
 *	The Regents of the University of California.  All rights reserved.
 *
 * This code is derived from software contributed to Berkeley by
 * Margo Seltzer.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */

#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <string.h>
#endif

#include "db_int.h"
#include "Crypto.h"
#include "db_page.h"
#include "hash.h"
#include "log.h"
#include "db_shash.h"
#include "lock.h"
#include "mp.h"
#include "BtreeMacros.h"
#include "fop.h"
#include "HashOpen.h"
#include "HashMeta.h"
#include "HashFunc.h"
#include "HashConv.h"
#include "HashAuto.h"
#include "MpFget.h"
#include "MpFput.h"
#include "DbIface.h"
#include "DbCam.h"
#include "DbMethod.h"
#include "Db.h"
#include "DbMeta.h"
#include "DbConv.h"
#include "BTreeCompare.h"
#include "FileOpBasic.h"
#include "Lock.h"
#include "DbErr.h"
#include "Hash.h"
#include "DbLog2.h"


class CHashOpen
{
public:
#ifndef HAVE_HASH
	static int db_no_hash_am(DB_ENV *dbenv);
	static int ham_open(DB *dbp, DB_TXN *txn, const char *name, db_pgno_t base_pgno, u_int32_t flags)
	{
		COMPQUIET(txn, NULL);
		COMPQUIET(name, NULL);
		COMPQUIET(base_pgno, 0);
		COMPQUIET(flags, 0);
		return (CHash::db_no_hash_am(dbp->dbenv));
	}
	static int ham_metachk(DB *dbp, const char *name, HMETA *hashm)
	{
		COMPQUIET(name, NULL);
		COMPQUIET(hashm, NULL);
		return (CHash::db_no_hash_am(dbp->dbenv));
	}
	static int ham_new_file(DB *dbp, DB_TXN *txn, DB_FH *fhp, const char *name)
	{
		COMPQUIET(txn, NULL);
		COMPQUIET(fhp, NULL);
		COMPQUIET(name, NULL);
		return (CHash::db_no_hash_am(dbp->dbenv));
	}
	static int ham_new_subdb(DB *mdbp, DB *dbp, DB_TXN *txn)
	{
		COMPQUIET(dbp, NULL);
		COMPQUIET(txn, NULL);
		return (CHash::db_no_hash_am(mdbp->dbenv));
	}
#else 
	static int ham_open(DB *dbp, DB_TXN *txn, const char *name, db_pgno_t base_pgno, u_int32_t flags)
	{
		DB_ENV *dbenv;
		DBC *dbc;
		HASH_CURSOR *hcp;
		HASH *hashp;
		int ret, t_ret;

		dbenv = dbp->dbenv;
		dbc = NULL;

		/*
		 * Get a cursor.  If DB_CREATE is specified, we may be creating
		 * pages, and to do that safely in CDB we need a write cursor.
		 * In STD_LOCKING mode, we'll synchronize using the meta page
		 * lock instead.
		 */
		if ((ret = CDbIface::db_cursor(dbp,
			txn, &dbc, LF_ISSET(DB_CREATE) && CDB_LOCKING(dbenv) ?
			DB_WRITECURSOR : 0)) != 0)
			return (ret);

		hcp = (HASH_CURSOR *)dbc->internal;
		hashp = static_cast<HASH*>(dbp->h_internal);
		hashp->meta_pgno = base_pgno;
		if ((ret = CHashMeta::ham_get_meta(dbc)) != 0)
			goto err1;

		/* Initialize the hdr structure.  */
		if (hcp->hdr->dbmeta.magic == DB_HASHMAGIC) {
			/* File exists, verify the data in the header. */
			if (hashp->h_hash == NULL)
				hashp->h_hash = hcp->hdr->dbmeta.version < 5
				? CHashFunc::ham_func4 : CHashFunc::ham_func5;
			if (!F_ISSET(dbp, DB_AM_RDONLY) && !IS_RECOVERING(dbenv) &&
				hashp->h_hash(dbp,
				CHARKEY, sizeof(CHARKEY)) != hcp->hdr->h_charkey) {
				CDbErr::db_err(dbp->dbenv,
					"hash: incompatible hash function");
				ret = EINVAL;
				goto err2;
			}
			hashp->h_nelem = hcp->hdr->nelem;
			if (F_ISSET(&hcp->hdr->dbmeta, DB_HASH_DUP))
				F_SET(dbp, DB_AM_DUP);
			if (F_ISSET(&hcp->hdr->dbmeta, DB_HASH_DUPSORT))
				F_SET(dbp, DB_AM_DUPSORT);
			if (F_ISSET(&hcp->hdr->dbmeta, DB_HASH_SUBDB))
				F_SET(dbp, DB_AM_SUBDB);

		} else if (!IS_RECOVERING(dbenv) && !F_ISSET(dbp, DB_AM_RECOVER)) {
			CDbErr::db_err(dbp->dbenv,
				"%s: Invalid hash meta page %d", name, base_pgno);
			ret = EINVAL;
		}

	err2:	/* Release the meta data page */
		if ((t_ret = CHashMeta::ham_release_meta(dbc)) != 0 && ret == 0)
			ret = t_ret;
	err1:	if ((t_ret  = CDbCam::db_c_close(dbc)) != 0 && ret == 0)
			ret = t_ret;

		return (ret);
	}
	static int ham_metachk(DB *dbp, const char *name, HMETA *hashm)
	{
		DB_ENV *dbenv;
		u_int32_t vers;
		int ret;

		dbenv = dbp->dbenv;

		/*
		 * At this point, all we know is that the magic number is for a Hash.
		 * Check the version, the database may be out of date.
		 */
		vers = hashm->dbmeta.version;
		if (F_ISSET(dbp, DB_AM_SWAP))
			M_32_SWAP(vers);
		switch (vers) {
		case 4:
		case 5:
		case 6:
			CDbErr::db_err(dbenv,
				"%s: hash version %lu requires a version upgrade",
				name, (u_long)vers);
			return (DB_OLD_VERSION);
		case 7:
		case 8:
			break;
		default:
			CDbErr::db_err(dbenv,
				"%s: unsupported hash version: %lu", name, (u_long)vers);
			return (EINVAL);
		}

		/* Swap the page if we need to. */
		if (F_ISSET(dbp, DB_AM_SWAP) && (ret = CHashConv::ham_mswap((PAGE *)hashm)) != 0)
			return (ret);

		/* Check the type. */
		if (dbp->type != DB_HASH && dbp->type != DB_UNKNOWN)
			return (EINVAL);
		dbp->type = DB_HASH;
		DB_ILLEGAL_METHOD(dbp, DB_OK_HASH);

		/*
		 * Check application info against metadata info, and set info, flags,
		 * and type based on metadata info.
		 */
		if ((ret = CDbErr::db_fchk(dbenv,
			"DB->open", hashm->dbmeta.flags,
			DB_HASH_DUP | DB_HASH_SUBDB | DB_HASH_DUPSORT)) != 0)
			return (ret);

		if (F_ISSET(&hashm->dbmeta, DB_HASH_DUP))
			F_SET(dbp, DB_AM_DUP);
		else
			if (F_ISSET(dbp, DB_AM_DUP)) {
				CDbErr::db_err(dbenv,
			"%s: DB_DUP specified to open method but not set in database",
					name);
				return (EINVAL);
			}

		if (F_ISSET(&hashm->dbmeta, DB_HASH_SUBDB))
			F_SET(dbp, DB_AM_SUBDB);
		else
			if (F_ISSET(dbp, DB_AM_SUBDB)) {
				CDbErr::db_err(dbenv,
			"%s: multiple databases specified but not supported in file",
				name);
				return (EINVAL);
			}

		if (F_ISSET(&hashm->dbmeta, DB_HASH_DUPSORT)) {
			if (dbp->dup_compare == NULL)
				dbp->dup_compare = CBTreeCompare::bam_defcmp;
		} else
			if (dbp->dup_compare != NULL) {
				CDbErr::db_err(dbenv,
			"%s: duplicate sort function specified but not set in database",
					name);
				return (EINVAL);
			}

		/* Set the page size. */
		dbp->pgsize = hashm->dbmeta.pagesize;

		/* Copy the file's ID. */
		memcpy(dbp->fileid, hashm->dbmeta.uid, DB_FILE_ID_LEN);

		return (0);
	}
	int CHashOpen::ham_new_file(DB *dbp, DB_TXN *txn, DB_FH *fhp, const char *name)
	{
		DB_ENV *dbenv;
		DB_LSN lsn;
		DB_MPOOLFILE *mpf;
		DB_PGINFO pginfo;
		DBT pdbt;
		HMETA *meta;
		PAGE *page;
		int ret;
		db_pgno_t lpgno;
		void *buf;

		dbenv = dbp->dbenv;
		mpf = dbp->mpf;
		meta = NULL;
		page = NULL;
		buf = NULL;

		if (F_ISSET(dbp, DB_AM_INMEM)) {
			/* Build meta-data page. */
			lpgno = PGNO_BASE_MD;
			if ((ret =
				CMpFget::memp_fget(mpf, &lpgno, DB_MPOOL_CREATE, &meta)) != 0)
				return (ret);
			LSN_NOT_LOGGED(lsn);
			lpgno = ham_init_meta(dbp, meta, PGNO_BASE_MD, &lsn);
			meta->dbmeta.last_pgno = lpgno;
			if ((ret = dbp->db_log_page(txn, &lsn, meta->dbmeta.pgno, (PAGE *)meta)) != 0)
				goto err;
			ret = CMpFput::memp_fput(mpf, meta, DB_MPOOL_DIRTY);
			meta = NULL;
			if (ret != 0)
				goto err;

			/* Allocate the final hash bucket. */
			if ((ret =
				CMpFget::memp_fget(mpf, &lpgno, DB_MPOOL_CREATE, &page)) != 0)
				goto err;
			P_INIT(page,
				dbp->pgsize, lpgno, PGNO_INVALID, PGNO_INVALID, 0, P_HASH);
			LSN_NOT_LOGGED(page->lsn);
			if ((ret = dbp->db_log_page(txn, &page->lsn, lpgno, page)) != 0)
				goto err;
			ret = CMpFput::memp_fput(mpf, page, DB_MPOOL_DIRTY);
			page = NULL;
			if (ret != 0)
				goto err;
		} else {
			memset(&pdbt, 0, sizeof(pdbt));

			/* Build meta-data page. */
			pginfo.db_pagesize = dbp->pgsize;
			pginfo.type = dbp->type;
			pginfo.flags =
				F_ISSET(dbp, (DB_AM_CHKSUM | DB_AM_ENCRYPT | DB_AM_SWAP));
			pdbt.data = &pginfo;
			pdbt.size = sizeof(pginfo);
			if ((ret = COsAlloc::os_calloc(dbp->dbenv, 1, dbp->pgsize, &buf)) != 0)
				return (ret);
			meta = (HMETA *)buf;
			LSN_NOT_LOGGED(lsn);
			lpgno = ham_init_meta(dbp, meta, PGNO_BASE_MD, &lsn);
			meta->dbmeta.last_pgno = lpgno;
			if ((ret = CDbConv::db_pgout(dbenv, PGNO_BASE_MD, meta, &pdbt)) != 0)
				goto err;
			if ((ret = CFileOpBasic::fop_write(dbenv, txn, name, DB_APP_DATA, fhp,
				dbp->pgsize, 0, 0, static_cast<u_int8_t*>(buf), dbp->pgsize, 1, F_ISSET(
				dbp, DB_AM_NOT_DURABLE) ? DB_LOG_NOT_DURABLE : 0)) != 0)
				goto err;
			meta = NULL;

			/* Allocate the final hash bucket. */
	#ifdef DIAGNOSTIC
			memset(buf, 0, dbp->pgsize);
	#endif
			page = (PAGE *)buf;
			P_INIT(page,
				dbp->pgsize, lpgno, PGNO_INVALID, PGNO_INVALID, 0, P_HASH);
			LSN_NOT_LOGGED(page->lsn);
			if ((ret = CDbConv::db_pgout(dbenv, lpgno, buf, &pdbt)) != 0)
				goto err;
			if ((ret = CFileOpBasic::fop_write(dbenv, txn, name, DB_APP_DATA, fhp,
				dbp->pgsize, lpgno, 0, static_cast<u_int8_t*>(buf), dbp->pgsize, 1, F_ISSET(
				dbp, DB_AM_NOT_DURABLE) ? DB_LOG_NOT_DURABLE : 0)) != 0)
				goto err;
			page = NULL;
		}

	err:	if (buf != NULL)
				COsAlloc::os_free(dbenv, buf);
			else {
				if (meta != NULL)
					(void)CMpFput::memp_fput(mpf, meta, 0);
				if (page != NULL)
					(void)CMpFput::memp_fput(mpf, page, 0);
			}
			return (ret);
	}
	int CHashOpen::ham_new_subdb(DB *mdbp, DB *dbp, DB_TXN *txn)
	{
		DBC *dbc;
		DB_ENV *dbenv;
		DB_LOCK metalock, mmlock;
		DB_LSN lsn;
		DB_MPOOLFILE *mpf;
		DBMETA *mmeta;
		HMETA *meta;
		PAGE *h;
		int i, ret, t_ret;
		db_pgno_t lpgno, mpgno;

		dbenv = mdbp->dbenv;
		mpf = mdbp->mpf;
		dbc = NULL;
		meta = NULL;
		mmeta = NULL;
		LOCK_INIT(metalock);
		LOCK_INIT(mmlock);

		if ((ret = CDbIface::db_cursor(mdbp, txn,
			&dbc, CDB_LOCKING(dbenv) ?  DB_WRITECURSOR : 0)) != 0)
			return (ret);

		/* Get and lock the new meta data page. */
		if ((ret = CDbMeta::db_lget(dbc,
			0, dbp->meta_pgno, DB_LOCK_WRITE, 0, &metalock)) != 0)
			goto err;
		if ((ret =
			CMpFget::memp_fget(mpf, &dbp->meta_pgno, DB_MPOOL_CREATE, &meta)) != 0)
			goto err;

		/* Initialize the new meta-data page. */
		lsn = meta->dbmeta.lsn;
		lpgno = ham_init_meta(dbp, meta, dbp->meta_pgno, &lsn);

		/*
		* We are about to allocate a set of contiguous buckets (lpgno
		* worth).  We need to get the master meta-data page to figure
		* out where these pages are and to allocate them.  So, lock and
		* get the master meta data page.
		*/
		mpgno = PGNO_BASE_MD;
		if ((ret = CDbMeta::db_lget(dbc, 0, mpgno, DB_LOCK_WRITE, 0, &mmlock)) != 0)
			goto err;
		if ((ret = CMpFget::memp_fget(mpf, &mpgno, 0, &mmeta)) != 0)
			goto err;

		/*
		* Now update the hash meta-data page to reflect where the first
		* set of buckets are actually located.
		*/
		meta->spares[0] = mmeta->last_pgno + 1;
		for (i = 0; i < NCACHED && meta->spares[i] != PGNO_INVALID; i++)
			meta->spares[i] = meta->spares[0];

		/* The new meta data page is now complete; log it. */
		if ((ret = mdbp->db_log_page(txn, &meta->dbmeta.lsn, dbp->meta_pgno, (PAGE *)meta)) != 0)
			goto err;

		/* Reflect the group allocation. */
		if (DBENV_LOGGING(dbenv))
			if ((ret = CHashAuto::ham_groupalloc_log(mdbp, txn,
				&LSN(mmeta), 0, &LSN(mmeta), meta->spares[0],
				meta->max_bucket + 1, mmeta->free, mmeta->last_pgno)) != 0)
				goto err;

		/* Release the new meta-data page. */
		if ((ret = CMpFput::memp_fput(mpf, meta, DB_MPOOL_DIRTY)) != 0)
			goto err;
		meta = NULL;

		lpgno += mmeta->last_pgno;

		/* Now allocate the final hash bucket. */
		if ((ret = CMpFget::memp_fget(mpf, &lpgno, DB_MPOOL_CREATE, &h)) != 0)
			goto err;

		mmeta->last_pgno = lpgno;
		P_INIT(h, dbp->pgsize, lpgno, PGNO_INVALID, PGNO_INVALID, 0, P_HASH);
		LSN(h) = LSN(mmeta);
		if ((ret = CMpFput::memp_fput(mpf, h, DB_MPOOL_DIRTY)) != 0)
			goto err;

		/* Now put the master-metadata page back. */
		if ((ret = CMpFput::memp_fput(mpf, mmeta, DB_MPOOL_DIRTY)) != 0)
			goto err;
		mmeta = NULL;

	err:
		if (mmeta != NULL)
			if ((t_ret = CMpFput::memp_fput(mpf, mmeta, 0)) != 0 && ret == 0)
				ret = t_ret;
		if ((t_ret = __LPUT(dbc, mmlock)) != 0 && ret == 0)
			ret = t_ret;
		if (meta != NULL)
			if ((t_ret = CMpFput::memp_fput(mpf, meta, 0)) != 0 && ret == 0)
				ret = t_ret;
		if ((t_ret = __LPUT(dbc, metalock)) != 0 && ret == 0)
			ret = t_ret;
		if (dbc != NULL)
			if ((t_ret = CDbCam::db_c_close(dbc)) != 0 && ret == 0)
				ret = t_ret;
		return (ret);
	}
#endif

private:

#ifdef HAVE_HASH
	static db_pgno_t ham_init_meta(DB *dbp, HMETA *meta, db_pgno_t pgno, DB_LSN *lsnp)
	{
		HASH *hashp;
		db_pgno_t nbuckets;
		u_int i, l2;

		hashp = static_cast<HASH*>(dbp->h_internal);
		if (hashp->h_hash == NULL)
			hashp->h_hash = DB_HASHVERSION < 5 ? CHashFunc::ham_func4 : CHashFunc::ham_func5;

		if (hashp->h_nelem != 0 && hashp->h_ffactor != 0) {
			hashp->h_nelem = (hashp->h_nelem - 1) / hashp->h_ffactor + 1;
			l2 = CDbLog2::db_log2(hashp->h_nelem > 2 ? hashp->h_nelem : 2);
		} else
			l2 = 1;
		nbuckets = (db_pgno_t)(1 << l2);

		memset(meta, 0, sizeof(HMETA));
		meta->dbmeta.lsn = *lsnp;
		meta->dbmeta.pgno = pgno;
		meta->dbmeta.magic = DB_HASHMAGIC;
		meta->dbmeta.version = DB_HASHVERSION;
		meta->dbmeta.pagesize = dbp->pgsize;
		if (F_ISSET(dbp, DB_AM_CHKSUM))
			FLD_SET(meta->dbmeta.metaflags, DBMETA_CHKSUM);
		if (F_ISSET(dbp, DB_AM_ENCRYPT)) {
			meta->dbmeta.encrypt_alg =
			   ((DB_CIPHER *)dbp->dbenv->crypto_handle)->alg;
			DB_ASSERT(meta->dbmeta.encrypt_alg != 0);
			meta->crypto_magic = meta->dbmeta.magic;
		}
		meta->dbmeta.type = P_HASHMETA;
		meta->dbmeta.free = PGNO_INVALID;
		meta->dbmeta.last_pgno = pgno;
		meta->max_bucket = nbuckets - 1;
		meta->high_mask = nbuckets - 1;
		meta->low_mask = (nbuckets >> 1) - 1;
		meta->ffactor = hashp->h_ffactor;
		meta->nelem = hashp->h_nelem;
		meta->h_charkey = hashp->h_hash(dbp, CHARKEY, sizeof(CHARKEY));
		memcpy(meta->dbmeta.uid, dbp->fileid, DB_FILE_ID_LEN);

		if (F_ISSET(dbp, DB_AM_DUP))
			F_SET(&meta->dbmeta, DB_HASH_DUP);
		if (F_ISSET(dbp, DB_AM_SUBDB))
			F_SET(&meta->dbmeta, DB_HASH_SUBDB);
		if (dbp->dup_compare != NULL)
			F_SET(&meta->dbmeta, DB_HASH_DUPSORT);

		/*
		 * Create the first and second buckets pages so that we have the
		 * page numbers for them and we can store that page number in the
		 * meta-data header (spares[0]).
		 */
		meta->spares[0] = pgno + 1;

		/* Fill in the last fields of the meta data page. */
		for (i = 1; i <= l2; i++)
			meta->spares[i] = meta->spares[0];
		for (; i < NCACHED; i++)
			meta->spares[i] = PGNO_INVALID;

		return (nbuckets);
	}

#endif
};

