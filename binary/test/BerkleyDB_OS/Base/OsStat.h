#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>
#include <sys/stat.h>

#include <string.h>
#endif

#include "db_int.h"
#include "OsStat.h"
#include "OsErrno.h"



class COsStat
{
public:
	static int os_exists(const char *path, int *isdirp)
	{
		struct stat sb;
		int ret;
	
		if (DB_GLOBAL(j_exists) != NULL)
			return (DB_GLOBAL(j_exists)(path, isdirp));
	
#ifdef HAVE_VXWORKS
		RETRY_CHK((stat((char *)path, &sb)), ret);
#else
		RETRY_CHK((stat(path, &sb)), ret);
#endif
		if (ret != 0)
			return (ret);
	
#if !defined(S_ISDIR) || defined(STAT_MACROS_BROKEN)
#undef	S_ISDIR
#ifdef _S_IFDIR
#define	S_ISDIR(m)	(_S_IFDIR & (m))
#else
#define	S_ISDIR(m)	(((m) & 0170000) == 0040000)
#endif
#endif
		if (isdirp != NULL)
			*isdirp = S_ISDIR(sb.st_mode);
	
		return (0);
	}

	static int os_ioinfo(DB_ENV *dbenv, const char *path, DB_FH *fhp, u_int32_t *mbytesp, u_int32_t *bytesp, u_int32_t *iosizep)
	{
		struct stat sb;
		int ret;
	
		if (DB_GLOBAL(j_ioinfo) != NULL)
			return (DB_GLOBAL(j_ioinfo)(path,
			    fhp->fd, mbytesp, bytesp, iosizep));
	
		/* Check for illegal usage. */
		DB_ASSERT(F_ISSET(fhp, DB_FH_OPENED) && fhp->fd != -1);
	
		RETRY_CHK((fstat(fhp->fd, &sb)), ret);
		if (ret != 0) {
			CDbErr::db_err(dbenv, "fstat: %s", strerror(ret));
			return (ret);
		}
	
		/* Return the size of the file. */
		if (mbytesp != NULL)
			*mbytesp = (u_int32_t)(sb.st_size / MEGABYTE);
		if (bytesp != NULL)
			*bytesp = (u_int32_t)(sb.st_size % MEGABYTE);
	
		/*
		 * Return the underlying filesystem blocksize, if available.
		 *
		 * XXX
		 * Check for a 0 size -- the HP MPE/iX architecture has st_blksize,
		 * but it's always 0.
		 */
#ifdef HAVE_STRUCT_STAT_ST_BLKSIZE
		if (iosizep != NULL && (*iosizep = sb.st_blksize) == 0)
			*iosizep = DB_DEF_IOSIZE;
#else
		if (iosizep != NULL)
			*iosizep = DB_DEF_IOSIZE;
#endif
		return (0);
	}
};

