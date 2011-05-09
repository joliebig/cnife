#pragma once
#include "db_config.h"

#ifndef NO_SYSTEM_INCLUDES
#include <sys/types.h>

#if HAVE_DIRENT_H
# include <dirent.h>
# define NAMLEN(dirent) strlen((dirent)->d_name)
#else
# define dirent direct
# define NAMLEN(dirent) (dirent)->d_namlen
# if HAVE_SYS_NDIR_H
#  include <sys/ndir.h>
# endif
# if HAVE_SYS_DIR_H
#  include <sys/dir.h>
# endif
# if HAVE_NDIR_H
#  include <ndir.h>
# endif
#endif

#endif

#include "db_int.h"
#include "OsDir.h"
#include "OsErrno.h"
#include "OsAlloc.h"



class COsDir
{
public:
	static int os_dirlist(DB_ENV *dbenv, const char *dir, char ***namesp, int *cntp)
	{
		struct dirent *dp;
		DIR *dirp;
		int arraysz, cnt, ret;
		char **names;
	
		if (DB_GLOBAL(j_dirlist) != NULL)
			return (DB_GLOBAL(j_dirlist)(dir, namesp, cntp));
	
#ifdef HAVE_VXWORKS
		if ((dirp = opendir((char *)dir)) == NULL)
#else
		if ((dirp = opendir(dir)) == NULL)
#endif
			return (COsErrno::os_get_errno());
		names = NULL;
		for (arraysz = cnt = 0; (dp = readdir(dirp)) != NULL; ++cnt) {
			if (cnt >= arraysz) {
				arraysz += 100;
				if ((ret = COsAlloc::os_realloc(dbenv,
				    (u_int)arraysz * sizeof(names[0]), &names)) != 0)
					goto nomem;
			}
			if ((ret = COsAlloc::os_strdup(dbenv, dp->d_name, &names[cnt])) != 0)
				goto nomem;
		}
		(void)closedir(dirp);
	
		*namesp = names;
		*cntp = cnt;
		return (0);
	
	nomem:	if (names != NULL)
			os_dirfree(dbenv, names, cnt);
		if (dirp != NULL)
			(void)closedir(dirp);
		return (ret);
	}

	static void os_dirfree(DB_ENV *dbenv, char **names, int cnt)
	{
		if (DB_GLOBAL(j_dirfree) != NULL)
			DB_GLOBAL(j_dirfree)(names, cnt);
		else {
			while (cnt > 0)
				COsAlloc::os_free(dbenv, names[--cnt]);
			COsAlloc::os_free(dbenv, names);
		}
	}
};

