#pragma once


#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#include <stdlib.h>
#include <string.h>
#endif

#include "db_int.h"
#include "OsAlloc.h"
#include "common/DbErr.h"

#ifdef DIAGNOSTIC
static void __os_guard __P((DB_ENV *));

union __db_allocinfo {
	size_t size;
	double align;
}; 
#endif 

class COsAlloc
{
public:
	static int os_umalloc(DB_ENV *dbenv, size_t size, void *storep)
	{
		int ret;
	
		/* Never allocate 0 bytes -- some C libraries don't like it. */
		if (size == 0)
			++size;
	
		if (dbenv == NULL || dbenv->db_malloc == NULL) {
			if (DB_GLOBAL(j_malloc) != NULL)
				*(void **)storep = DB_GLOBAL(j_malloc)(size);
			else
				*(void **)storep = malloc(size);
			if (*(void **)storep == NULL) {
				/*
				 *  Correct error return, see __os_malloc.
				 */
				if ((ret = COsErrno::os_get_errno_ret_zero()) == 0) {
					ret = ENOMEM;
					COsErrno::os_set_errno(ENOMEM);
				}
				CDbErr::db_err(dbenv,
				    "malloc: %s: %lu", strerror(ret), (u_long)size);
				return (ret);
			}
			return (0);
		}
	
		if ((*(void **)storep = dbenv->db_malloc(size)) == NULL) {
			CDbErr::db_err(dbenv, "User-specified malloc function returned NULL");
			return (ENOMEM);
		}
	
		return (0);
	}

	static int os_urealloc(DB_ENV *dbenv, size_t size, void *storep)
	{
		int ret;
		void *ptr;
	
		ptr = *(void **)storep;
	
		/* Never allocate 0 bytes -- some C libraries don't like it. */
		if (size == 0)
			++size;
	
		if (dbenv == NULL || dbenv->db_realloc == NULL) {
			if (ptr == NULL)
				return (os_umalloc(dbenv, size, storep));
	
			if (DB_GLOBAL(j_realloc) != NULL)
				*(void **)storep = DB_GLOBAL(j_realloc)(ptr, size);
			else
				*(void **)storep = realloc(ptr, size);
			if (*(void **)storep == NULL) {
				/*
				 * Correct errno, see __os_realloc.
				 */
				if ((ret = COsErrno::os_get_errno_ret_zero()) == 0) {
					ret = ENOMEM;
					COsErrno::os_set_errno(ENOMEM);
				}
				CDbErr::db_err(dbenv,
				    "realloc: %s: %lu", strerror(ret), (u_long)size);
				return (ret);
			}
			return (0);
		}
	
		if ((*(void **)storep = dbenv->db_realloc(ptr, size)) == NULL) {
			CDbErr::db_err(dbenv,
			    "User-specified realloc function returned NULL");
			return (ENOMEM);
		}
	
		return (0);
	}

	static void os_ufree(DB_ENV *dbenv, void *ptr)
	{
		if (dbenv != NULL && dbenv->db_free != NULL)
			dbenv->db_free(ptr);
		else if (DB_GLOBAL(j_free) != NULL)
			DB_GLOBAL(j_free)(ptr);
		else
			free(ptr);
	}

	static int os_strdup(DB_ENV *dbenv, const char *str, void *storep)
	{
		size_t size;
		int ret;
		void *p;
	
		*(void **)storep = NULL;
	
		size = strlen(str) + 1;
		if ((ret = os_malloc(dbenv, size, &p)) != 0)
			return (ret);
	
		memcpy(p, str, size);
	
		*(void **)storep = p;
		return (0);
	}

	static int os_calloc(DB_ENV *dbenv, size_t num, size_t size, void *storep)
	{
		void *p;
		int ret;
	
		size *= num;
		if ((ret = os_malloc(dbenv, size, &p)) != 0)
			return (ret);
	
		memset(p, 0, size);
	
		*(void **)storep = p;
		return (0);
	}
	static int os_malloc(DB_ENV *dbenv, size_t size, void *storep)
	{
		int ret;
		void *p;
	
		*(void **)storep = NULL;
	
		/* Never allocate 0 bytes -- some C libraries don't like it. */
		if (size == 0)
			++size;
	
#ifdef DIAGNOSTIC
		/* Add room for size and a guard byte. */
		size += sizeof(union __db_allocinfo) + 1;
#endif
	
		if (DB_GLOBAL(j_malloc) != NULL)
			p = DB_GLOBAL(j_malloc)(size);
		else
			p = malloc(size);
		if (p == NULL) {
			/*
			 * Some C libraries don't correctly set errno when malloc(3)
			 * fails.  We'd like to 0 out errno before calling malloc,
			 * but it turns out that setting errno is quite expensive on
			 * Windows/NT in an MT environment.
			 */
			if ((ret = COsErrno::os_get_errno_ret_zero()) == 0) {
				ret = ENOMEM;
				COsErrno::os_set_errno(ENOMEM);
			}
			CDbErr::db_err(dbenv,
			    "malloc: %s: %lu", strerror(ret), (u_long)size);
			return (ret);
		}
	
#ifdef DIAGNOSTIC
		/* Overwrite memory. */
		memset(p, CLEAR_BYTE, size);
	
		/*
		 * Guard bytes: if #DIAGNOSTIC is defined, we allocate an additional
		 * byte after the memory and set it to a special value that we check
		 * for when the memory is free'd.
		 */
		((u_int8_t *)p)[size - 1] = CLEAR_BYTE;
	
		((union __db_allocinfo *)p)->size = size;
		p = &((union __db_allocinfo *)p)[1];
#endif
		*(void **)storep = p;
	
		return (0);
	}

	static int os_realloc(DB_ENV *dbenv, size_t size, void *storep)
	{
		int ret;
		void *p, *ptr;
	
		ptr = *(void **)storep;
	
		/* Never allocate 0 bytes -- some C libraries don't like it. */
		if (size == 0)
			++size;
	
		/* If we haven't yet allocated anything yet, simply call malloc. */
		if (ptr == NULL)
			return (os_malloc(dbenv, size, storep));
	
#ifdef DIAGNOSTIC
		/* Add room for size and a guard byte. */
		size += sizeof(union __db_allocinfo) + 1;
	
		/* Back up to the real beginning */
		ptr = &((union __db_allocinfo *)ptr)[-1];
	
		{
			size_t s;
	
			s = ((union __db_allocinfo *)ptr)->size;
			if (((u_int8_t *)ptr)[s - 1] != CLEAR_BYTE)
				 os_guard(dbenv);
		}
#endif
	
		/*
		 * Don't overwrite the original pointer, there are places in DB we
		 * try to continue after realloc fails.
		 */
		if (DB_GLOBAL(j_realloc) != NULL)
			p = DB_GLOBAL(j_realloc)(ptr, size);
		else
			p = realloc(ptr, size);
		if (p == NULL) {
			/*
			 * Some C libraries don't correctly set errno when malloc(3)
			 * fails.  We'd like to 0 out errno before calling malloc,
			 * but it turns out that setting errno is quite expensive on
			 * Windows/NT in an MT environment.
			 */
			if ((ret = COsErrno::os_get_errno_ret_zero()) == 0) {
				ret = ENOMEM;
				COsErrno::os_set_errno(ENOMEM);
			}
			CDbErr::db_err(dbenv,
			    "realloc: %s: %lu", strerror(ret), (u_long)size);
			return (ret);
		}
#ifdef DIAGNOSTIC
		((u_int8_t *)p)[size - 1] = CLEAR_BYTE;	/* Initialize guard byte. */
	
		((union __db_allocinfo *)p)->size = size;
		p = &((union __db_allocinfo *)p)[1];
#endif
	
		*(void **)storep = p;
	
		return (0);
	}

	static void os_free(DB_ENV *dbenv, void *ptr)
	{
#ifdef DIAGNOSTIC
		size_t size;
		/*
		 * Check that the guard byte (one past the end of the memory) is
		 * still CLEAR_BYTE.
		 */
		if (ptr == NULL)
			return;
	
		ptr = &((union __db_allocinfo *)ptr)[-1];
		size = ((union __db_allocinfo *)ptr)->size;
		if (((u_int8_t *)ptr)[size - 1] != CLEAR_BYTE)
			 os_guard(dbenv);
	
		/* Overwrite memory. */
		if (size != 0)
			memset(ptr, CLEAR_BYTE, size);
#endif
		COMPQUIET(dbenv, NULL);
	
		if (DB_GLOBAL(j_free) != NULL)
			DB_GLOBAL(j_free)(ptr);
		else
			free(ptr);
	}


	
#ifdef DIAGNOSTIC
	/*
	 * __os_guard --
	 *	Complain and abort.
	 */
	static void os_guard(DB_ENV *dbenv)
	{
		CDbErr::db_err(dbenv, "Guard byte incorrect during free");
		abort();
		/* NOTREACHED */
	}
#endif
	
	static void * ua_memcpy(void *dst, const void *src, size_t len)
	{
		return ((void *)memcpy(dst, src, len));
	}
};



