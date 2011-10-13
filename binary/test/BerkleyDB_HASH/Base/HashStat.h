/*-
* See the file LICENSE for redistribution information.
*
* Copyright (c) 1996-2005
*	Sleepycat Software.  All rights reserved.
*
*/

#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <string.h>
#endif

#include "db_int.h"
#include "db_page.h"
#include "db_shash.h"
#include "BtreeMacros.h"
#include "hash.h"
#include "mp.h"
#include "HashStat.h"
#include "HashMeta.h"
#include "Hash.h"
#include "HashPage.h"
#include "MpFget.h"
#include "MpFput.h"
#include "DbMethod.h"
#include "DbPr.h"
#include "EnvStat.h"
#include "DbCam.h"
#include "DbReclaim.h"
#include "BTreeStatistic.h"
#include "BTreeTraverse.h"
#include "DbErr.h"
#include "DbLog2.h"


/* Hash statistics structure. */
struct __db_h_stat {
	u_int32_t hash_magic;		/* Magic number. */
	u_int32_t hash_version;		/* Version number. */
	u_int32_t hash_metaflags;	/* Metadata flags. */
	u_int32_t hash_nkeys;		/* Number of unique keys. */
	u_int32_t hash_ndata;		/* Number of data items. */
	u_int32_t hash_pagesize;	/* Page size. */
	u_int32_t hash_ffactor;		/* Fill factor specified at create. */
	u_int32_t hash_buckets;		/* Number of hash buckets. */
	u_int32_t hash_free;		/* Pages on the free list. */
	u_int32_t hash_bfree;		/* Bytes free on bucket pages. */
	u_int32_t hash_bigpages;	/* Number of big key/data pages. */
	u_int32_t hash_big_bfree;	/* Bytes free on big item pages. */
	u_int32_t hash_overflows;	/* Number of overflow pages. */
	u_int32_t hash_ovfl_free;	/* Bytes free on ovfl pages. */
	u_int32_t hash_dup;		/* Number of dup pages. */
	u_int32_t hash_dup_free;	/* Bytes free on duplicate pages. */
};


class CHashStat
{
public:
#ifndef HAVE_HASH
	static void ham_print_cursor(DBC *dbc)
	{
		(void)CHash::db_no_hash_am(dbc->dbp->dbenv);
	}

	static int ham_stat(DBC *dbc, void *spp, u_int32_t flags)
	{
		COMPQUIET(spp, NULL);
		COMPQUIET(flags, 0);
		return (CHash::db_no_hash_am(dbc->dbp->dbenv));
	}

	static int ham_stat_print(DBC *dbc, u_int32_t flags)
	{
		COMPQUIET(flags, 0);
		return (CHash::db_no_hash_am(dbc->dbp->dbenv));
	}


	static int ham_stat_callback(DB *dbp, PAGE *pagep, void *cookie, int *putp);
	static int ham_traverse(DBC *dbc, db_lockmode_t mode, int (*callback) __P((DB *, PAGE *, void *, int *)), void *cookie, int look_past_max);

#else	//#ifndef HAVE_HASH


#ifdef HAVE_STATISTICS
	/*
	* __ham_print_cursor --
	*	Display the current cursor.
	*
	* PUBLIC: void __ham_print_cursor __P((DBC *));
	*/
	static void ham_print_cursor(DBC *dbc)
	{
		static const FN fn[] = {
			{ H_CONTINUE,	"H_CONTINUE" },
			{ H_DELETED,	"H_DELETED" },
			{ H_DIRTY,	"H_DIRTY" },
			{ H_DUPONLY,	"H_DUPONLY" },
			{ H_EXPAND,	"H_EXPAND" },
			{ H_ISDUP,	"H_ISDUP" },
			{ H_NEXT_NODUP,	"H_NEXT_NODUP" },
			{ H_NOMORE,	"H_NOMORE" },
			{ H_OK,		"H_OK" },
			{ 0,		NULL }
		};
		DB_ENV *dbenv;
		HASH_CURSOR *cp;

		dbenv = dbc->dbp->dbenv;
		cp = (HASH_CURSOR *)dbc->internal;

		STAT_ULONG("Bucket traversing", cp->bucket);
		STAT_ULONG("Bucket locked", cp->lbucket);
		STAT_ULONG("Duplicate set offset", cp->dup_off);
		STAT_ULONG("Current duplicate length", cp->dup_len);
		STAT_ULONG("Total duplicate set length", cp->dup_tlen);
		STAT_ULONG("Bytes needed for add", cp->seek_size);
		STAT_ULONG("Page on which we can insert", cp->seek_found_page);
		STAT_ULONG("Order", cp->order);
		CDbPr::db_prflags(dbenv, NULL, cp->flags, fn, NULL, "\tInternal Flags");
	}

	/*
	* __ham_stat --
	*	Gather/print the hash statistics
	*
	* PUBLIC: int __ham_stat __P((DBC *, void *, u_int32_t));
	*/
	static int ham_stat(DBC *dbc, void *spp, u_int32_t flags)
	{
		DB *dbp;
		DB_ENV *dbenv;
		DB_HASH_STAT *sp;
		DB_MPOOLFILE *mpf;
		HASH_CURSOR *hcp;
		PAGE *h;
		db_pgno_t pgno;
		int ret;

		dbp = dbc->dbp;
		dbenv = dbp->dbenv;

		mpf = dbp->mpf;
		sp = NULL;

		hcp = (HASH_CURSOR *)dbc->internal;

		if ((ret = CHashMeta::ham_get_meta(dbc)) != 0)
			goto err;

		/* Allocate and clear the structure. */
		if ((ret = COsAlloc::os_umalloc(dbenv, sizeof(*sp), &sp)) != 0)
			goto err;
		memset(sp, 0, sizeof(*sp));
		/* Copy the fields that we have. */
		sp->hash_nkeys = hcp->hdr->dbmeta.key_count;
		sp->hash_ndata = hcp->hdr->dbmeta.record_count;
		sp->hash_pagesize = dbp->pgsize;
		sp->hash_buckets = hcp->hdr->max_bucket + 1;
		sp->hash_magic = hcp->hdr->dbmeta.magic;
		sp->hash_version = hcp->hdr->dbmeta.version;
		sp->hash_metaflags = hcp->hdr->dbmeta.flags;
		sp->hash_ffactor = hcp->hdr->ffactor;

		if (flags == DB_FAST_STAT || flags == DB_CACHED_COUNTS)
			goto done;

		/* Walk the free list, counting pages. */
		for (sp->hash_free = 0, pgno = hcp->hdr->dbmeta.free;
			pgno != PGNO_INVALID;) {
				++sp->hash_free;

				if ((ret = CMpFget::memp_fget(mpf, &pgno, 0, &h)) != 0)
					goto err;

				pgno = h->next_pgno;
				(void)CMpFput::memp_fput(mpf, h, 0);
		}

		/* Now traverse the rest of the table. */
		sp->hash_nkeys = 0;
		sp->hash_ndata = 0;
		if ((ret = ham_traverse(dbc,
			DB_LOCK_READ, ham_stat_callback, sp, 0)) != 0)
			goto err;

		if (!F_ISSET(dbp, DB_AM_RDONLY)) {
			if ((ret = CHashMeta::ham_dirty_meta(dbc)) != 0)
				goto err;
			hcp->hdr->dbmeta.key_count = sp->hash_nkeys;
			hcp->hdr->dbmeta.record_count = sp->hash_ndata;
		}

done:	if ((ret = CHashMeta::ham_release_meta(dbc)) != 0)
			goto err;

		*(DB_HASH_STAT **)spp = sp;
		return (0);

err:	if (sp != NULL)
			COsAlloc::os_ufree(dbenv, sp);

		if (hcp->hdr != NULL)
			(void)CHashMeta::ham_release_meta(dbc);

		return (ret);
	}

	/*
	* __ham_stat_print --
	*	Display hash statistics.
	*
	* PUBLIC: int __ham_stat_print __P((DBC *, u_int32_t));
	*/
	static int ham_stat_print(DBC *dbc, u_int32_t flags)
	{
		static const FN fn[] = {
			{ DB_HASH_DUP,		"duplicates" },
			{ DB_HASH_SUBDB,	"multiple-databases" },
			{ DB_HASH_DUPSORT,	"sorted duplicates" },
			{ 0,			NULL }
		};
		DB *dbp;
		DB_ENV *dbenv;
		DB_HASH_STAT *sp;
		int lorder, ret;
		const char *s;

		dbp = dbc->dbp;
		dbenv = dbp->dbenv;

		if ((ret = CHashStat::ham_stat(dbc, &sp, 0)) != 0)
			return (ret);

		if (LF_ISSET(DB_STAT_ALL)) {
			CDbErr::db_msg(dbenv, "%s", DB_GLOBAL(db_line));
			CDbErr::db_msg(dbenv, "Default Hash database information:");
		}
		CDbErr::db_msg(dbenv, "%lx\tHash magic number", (u_long)sp->hash_magic);
		CDbErr::db_msg(dbenv,
			"%lu\tHash version number", (u_long)sp->hash_version);
		(void)CDbMethod::db_get_lorder(dbp, &lorder);
		switch (lorder) {
case 1234:
	s = "Little-endian";
	break;
case 4321:
	s = "Big-endian";
	break;
default:
	s = "Unrecognized byte order";
	break;
		}
		CDbErr::db_msg(dbenv, "%s\tByte order", s);
		CDbPr::db_prflags(dbenv, NULL, sp->hash_metaflags, fn, NULL, "\tFlags");
		CEnvStat::db_dl(dbenv,
			"Underlying database page size", (u_long)sp->hash_pagesize);
		CEnvStat::db_dl(dbenv, "Specified fill factor", (u_long)sp->hash_ffactor);
		CEnvStat::db_dl(dbenv,
			"Number of keys in the database", (u_long)sp->hash_nkeys);
		CEnvStat::db_dl(dbenv,
			"Number of data items in the database", (u_long)sp->hash_ndata);

		CEnvStat::db_dl(dbenv, "Number of hash buckets", (u_long)sp->hash_buckets);
		CEnvStat::db_dl_pct(dbenv, "Number of bytes free on bucket pages",
			(u_long)sp->hash_bfree, DB_PCT_PG(
			sp->hash_bfree, sp->hash_buckets, sp->hash_pagesize), "ff");

		CEnvStat::db_dl(dbenv,
			"Number of overflow pages", (u_long)sp->hash_bigpages);
		CEnvStat::db_dl_pct(dbenv, "Number of bytes free in overflow pages",
			(u_long)sp->hash_big_bfree, DB_PCT_PG(
			sp->hash_big_bfree, sp->hash_bigpages, sp->hash_pagesize), "ff");

		CEnvStat::db_dl(dbenv,
			"Number of bucket overflow pages", (u_long)sp->hash_overflows);
		CEnvStat::db_dl_pct(dbenv,
			"Number of bytes free in bucket overflow pages",
			(u_long)sp->hash_ovfl_free, DB_PCT_PG(
			sp->hash_ovfl_free, sp->hash_overflows, sp->hash_pagesize), "ff");

		CEnvStat::db_dl(dbenv, "Number of duplicate pages", (u_long)sp->hash_dup);
		CEnvStat::db_dl_pct(dbenv, "Number of bytes free in duplicate pages",
			(u_long)sp->hash_dup_free, DB_PCT_PG(
			sp->hash_dup_free, sp->hash_dup, sp->hash_pagesize), "ff");

		CEnvStat::db_dl(dbenv,
			"Number of pages on the free list", (u_long)sp->hash_free);

		COsAlloc::os_ufree(dbenv, sp);

		return (0);
	}


	static int ham_stat_callback(DB *dbp, PAGE *pagep, void *cookie, int *putp)
	{
		DB_HASH_STAT *sp;
#ifdef HAVE_BTREE
		DB_BTREE_STAT bstat;
#endif
		db_indx_t indx, len, off, tlen, top;
		u_int8_t *hk;
		int ret;

		*putp = 0;
		sp = static_cast<DB_HASH_STAT*>(cookie);

		switch (pagep->type) {
case P_INVALID:
	/*
	* Hash pages may be wholly zeroed;  this is not a bug.
	* Obviously such pages have no data, so we can just proceed.
	*/
	break;
case P_HASH:
	/*
	* We count the buckets and the overflow pages
	* separately and tally their bytes separately
	* as well.  We need to figure out if this page
	* is a bucket.
	*/
	if (PREV_PGNO(pagep) == PGNO_INVALID)
		sp->hash_bfree += P_FREESPACE(dbp, pagep);
	else {
		sp->hash_overflows++;
		sp->hash_ovfl_free += P_FREESPACE(dbp, pagep);
	}
	top = NUM_ENT(pagep);
	/* Correct for on-page duplicates and deleted items. */
	for (indx = 0; indx < top; indx += P_INDX) {
		switch (*H_PAIRDATA(dbp, pagep, indx)) {
case H_OFFDUP:
	break;
case H_OFFPAGE:
case H_KEYDATA:
	sp->hash_ndata++;
	break;
case H_DUPLICATE:
	tlen = LEN_HDATA(dbp, pagep, 0, indx);
	hk = H_PAIRDATA(dbp, pagep, indx);
	for (off = 0; off < tlen;
		off += len + 2 * sizeof(db_indx_t)) {
			sp->hash_ndata++;
			memcpy(&len,
				HKEYDATA_DATA(hk)
				+ off, sizeof(db_indx_t));
	}
	break;
default:
	return (CDbErr::db_pgfmt(dbp->dbenv, PGNO(pagep)));
		}
	}
	sp->hash_nkeys += H_NUMPAIRS(pagep);
	break;
#ifdef HAVE_BTREE
case P_IBTREE:
case P_IRECNO:
case P_LBTREE:
case P_LRECNO:
case P_LDUP:
	/*
	* These are all btree pages; get a correct
	* cookie and call them.  Then add appropriate
	* fields into our stat structure.
	*/
	memset(&bstat, 0, sizeof(bstat));
	if ((ret = CBTreeStatistic::bam_stat_callback(dbp, pagep, &bstat, putp)) != 0)
		return (ret);
	sp->hash_dup++;
	sp->hash_dup_free += bstat.bt_leaf_pgfree +
		bstat.bt_dup_pgfree + bstat.bt_int_pgfree;
	sp->hash_ndata += bstat.bt_ndata;
	break;
#endif
case P_OVERFLOW:
	sp->hash_bigpages++;
	sp->hash_big_bfree += P_OVFLSPACE(dbp, dbp->pgsize, pagep);
	break;
default:
	return (CDbErr::db_pgfmt(dbp->dbenv, PGNO(pagep)));
		}

		return (0);
	}


#else /* !HAVE_STATISTICS */

	int CHashStat::ham_stat(DBC *dbc, void *spp, u_int32_t flags)
	{
		COMPQUIET(spp, NULL);
		COMPQUIET(flags, 0);

		return (CEnvStat::db_stat_not_built(dbc->dbp->dbenv));
	}
#endif

	/*
	* __ham_traverse
	*	 Traverse an entire hash table.  We use the callback so that we
	* can use this both for stat collection and for deallocation.
	*
	* PUBLIC: int __ham_traverse __P((DBC *, db_lockmode_t,
	* PUBLIC:     int (*)(DB *, PAGE *, void *, int *), void *, int));
	*/
	static int ham_traverse(DBC *dbc, db_lockmode_t mode, int (*callback) __P((DB *, PAGE *, void *, int *)), void *cookie, int look_past_max)
	{
#pragma message("CHashStat::ham_traverse")
		DB *dbp;
		DBC *opd;
		DB_MPOOLFILE *mpf;
		HASH_CURSOR *hcp;
		HKEYDATA *hk;
		db_pgno_t pgno, opgno;
		int did_put, i, ret, t_ret;
		u_int32_t bucket, spares_entry;

		dbp = dbc->dbp;
		opd = NULL;
		mpf = dbp->mpf;
		hcp = (HASH_CURSOR *)dbc->internal;
		ret = 0;

		/*
		* In a perfect world, we could simply read each page in the file
		* and look at its page type to tally the information necessary.
		* Unfortunately, the bucket locking that hash tables do to make
		* locking easy, makes this a pain in the butt.  We have to traverse
		* duplicate, overflow and big pages from the bucket so that we
		* don't access anything that isn't properly locked.
		*
		*/
		for (bucket = 0;; bucket++) {
			/*
			* We put the loop exit condition check here, because
			* it made for a really vile extended ?: that made SCO's
			* compiler drop core.
		 *
		 * If look_past_max is not set, we can stop at max_bucket;
		 * if it is set, we need to include pages that are part of
		 * the current doubling but beyond the highest bucket we've
		 * split into, as well as pages from a "future" doubling
		 * that may have been created within an aborted
		 * transaction.  To do this, keep looping (and incrementing
		 * bucket) until the corresponding spares array entries
		 * cease to be defined.
		 */
			if (look_past_max) {
				spares_entry = CDbLog2::db_log2(bucket + 1);
				if (spares_entry >= NCACHED ||
					hcp->hdr->spares[spares_entry] == 0)
					break;
			} else {
				if (bucket > hcp->hdr->max_bucket)
					break;
			}

			hcp->bucket = bucket;
			hcp->pgno = pgno = BUCKET_TO_PAGE(hcp, bucket);
			for (ret = CHashPage::ham_get_cpage(dbc, mode); ret == 0;
				ret = CHashPage::ham_next_cpage(dbc, pgno, 0)) {

					/*
					* If we are cleaning up pages past the max_bucket,
					* then they may be on the free list and have their
					* next pointers set, but they should be ignored.  In
					* fact, we really ought to just skip anybody who is
					* not a valid page.
					*/
					if (TYPE(hcp->page) == P_INVALID)
						break;
					pgno = NEXT_PGNO(hcp->page);

					/*
					* Go through each item on the page checking for
					* duplicates (in which case we have to count the
					* duplicate pages) or big key/data items (in which
					* case we have to count those pages).
					*/
					for (i = 0; i < NUM_ENT(hcp->page); i++) {
						hk = (HKEYDATA *)P_ENTRY(dbp, hcp->page, i);
						switch (HPAGE_PTYPE(hk)) {
#ifdef HAVE_BTREE
case H_OFFDUP:
	{
		memcpy(&opgno, HOFFDUP_PGNO(hk),
			sizeof(db_pgno_t));
		if ((ret = CDbCam::db_c_newopd(dbc,
			opgno, NULL, &opd)) != 0)
			return (ret);

		CBTreeTraverse traverse(*opd, callback);
		if ((ret = traverse.bam_traverse(DB_LOCK_READ, opgno, cookie)) != 0)
			goto err;
		if ((ret = CDbCam::db_c_close(opd)) != 0)
			return (ret);
		opd = NULL;
		break;
	}
#endif
case H_OFFPAGE:
	/*
	* We are about to get a big page
	* which will use the same spot that
	* the current page uses, so we need
	* to restore the current page before
	* looking at it again.
	*/
	memcpy(&opgno, HOFFPAGE_PGNO(hk),
		sizeof(db_pgno_t));
	if ((ret = CDbReclaim::db_traverse_big(dbp,
		opgno, callback, cookie)) != 0)
		goto err;
	break;
case H_KEYDATA:
case H_DUPLICATE:
	break;
default:
	DB_ASSERT(0);
	ret = EINVAL;
	goto err;

						}
					}

					/* Call the callback on main pages. */
					if ((ret = callback(dbp,
						static_cast<PAGE*>(hcp->page), cookie, &did_put)) != 0)
						goto err;

					if (did_put)
						hcp->page = NULL;
					if (pgno == PGNO_INVALID)
						break;
			}
			if (ret != 0)
				goto err;

			if (hcp->page != NULL) {
				if ((ret = CMpFput::memp_fput(mpf, hcp->page, 0)) != 0)
					return (ret);
				hcp->page = NULL;
			}

		}
err:	if (opd != NULL &&
			(t_ret = CDbCam::db_c_close(opd)) != 0 && ret == 0)
			ret = t_ret;
		return (ret);
	}

#endif	//#ifndef HAVE_HASH #else

};



