/******************************************************************************
 *
 * $Id:$
 *
 * Copyright (C) 1997-2008 by Dimitri van Heesch.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation under the terms of the GNU General Public License is hereby 
 * granted. No representations are made about the suitability of this software 
 * for any purpose. It is provided "as is" without express or implied warranty.
 * See the GNU General Public License for more details.
 *
 * Documents produced by Doxygen are derivative works derived from the
 * input used in their production; they are not affected by this license.
 *
 */

#pragma once
#include <stdio.h>
#include <assert.h>
#include <qglobal.h>

//#define CACHE_TEST
//#define CACHE_DEBUG
#define CACHE_STATS

/** @brief Cache for objects.
 *
 *  This cache is used to decide which objects should remain in
 *  memory. It uses a least recently used policy (LRU) to decide
 *  which object should make room for a new object when the cache
 *  is full. An object should be added using add(), and then use()
 *  should be called when the object is used.
 */
class ObjCache
{
  private:
    struct CacheNode
    {
      CacheNode() : next(-1), prev(-1), obj(0) {}
      int next;
      int prev;
      void *obj;
    };
    struct HashNode
    {
      HashNode() : head(-1), nextHash(-1), index(-1), obj(0) {}
      int head;
      int nextHash; 
      int index;
      void *obj;
    };

  public:
    /*! Creates the cache. The number of elements in the cache is 2 to 
     *  the power of \a logSize. 
     */
	  ObjCache(unsigned int logSize) 
		  : m_head(-1), m_tail(-1), //m_numEntries(0), 
		  m_size(1<<logSize), m_freeHashNodes(0), m_freeCacheNodes(0), m_lastHandle(-1)
	  {
		  int i;
		  m_cache = new CacheNode[m_size];
		  m_hash  = new HashNode[m_size];
		  // add all items to list of free buckets
		  for (i=0;i<m_size-1;i++)
		  {
			  m_hash[i].nextHash = i+1;
			  m_cache[i].next    = i+1;
		  }
	  }

    /*! Deletes the cache and free all internal data-structures used. */
	  ~ObjCache()
	  {
		  delete[] m_cache;
		  delete[] m_hash;
	  }

    /*! Adds \a obj to the cache. When victim is not null, this object is
     *  removed from the cache to make room for \a obj. 
     *  Returns a handle to the object, which can be used by the use()
     *  function, each time the object is used.
     */
	  int add(void *obj,void **victim)
	  {
		  *victim=0;

		  HashNode *hnode = hashFind(obj);
		  //printf("hnode=%p\n",hnode);
		  if (hnode) // move object to the front of the LRU list, since it is used
			  // most recently
		  {
			  //printf("moveToFront=%d\n",hnode->index);
			  moveToFront(hnode->index);
#ifdef CACHE_STATS
			  hits++;
#endif
		  }
		  else // object not in the cache.
		  {
			  void *lruObj=0;
			  if (m_freeCacheNodes!=-1) // cache not full -> add element to the cache
			  {
				  // remove element from free list
				  int index = m_freeCacheNodes;
				  m_freeCacheNodes = m_cache[index].next;

				  // add to head of the list
				  if (m_tail==-1)
				  {
					  m_tail = index;
				  }
				  m_cache[index].prev = -1;
				  m_cache[index].next = m_head;
				  if (m_head!=-1)
				  {
					  m_cache[m_head].prev = index;
				  }
				  m_head = index;
			  }
			  else // cache full -> replace element in the cache
			  {
				  //printf("Cache full!\n");
				  lruObj = m_cache[m_tail].obj;
				  hashRemove(lruObj);
				  moveToFront(m_tail); // m_tail indexes the emptied element, which becomes m_head
			  }
			  //printf("numEntries=%d size=%d\n",m_numEntries,m_size);
			  m_cache[m_head].obj = obj;
			  hnode = hashInsert(obj);
			  hnode->index = m_head;
			  *victim = lruObj;
#ifdef CACHE_STATS
			  misses++;
#endif
		  }
		  return m_head;
	  }

    /*! Indicates that this object is used. This will move the object
     *  to the front of the internal LRU list to make sure it is removed last.
     *  The parameter \a handle is returned when called add().
     */
    void use(int handle)
    {
      hits++;
      if (handle==m_lastHandle) return;
      m_lastHandle = handle;
      moveToFront(handle);
    }

    /*! Removes the item identified by \a handle from the cache.
     *  @see add()
     */
	void del(int handle)
	{
		assert(handle!=-1);
		assert(m_cache[handle].obj!=0);
		hashRemove(m_cache[handle].obj);
		moveToFront(handle);
		m_head = m_cache[handle].next;
		if (m_head==-1) 
			m_tail=-1;
		else 
			m_cache[m_head].prev=-1;
		m_cache[handle].obj=0;
		m_cache[handle].prev=-1;
		m_cache[handle].next = m_freeCacheNodes;
		m_freeCacheNodes = handle;
	}


    /*! Debug function. Prints the LRU list */
#ifdef CACHE_DEBUG
#define cache_debug_printf printf
	void printLRU()
	{
		cache_debug_printf("MRU->LRU: ");
		int index = m_head;
		while (index!=-1)
		{
			cache_debug_printf("%d=%p ",index,m_cache[index].obj);
			index = m_cache[index].next;
		}
		cache_debug_printf("\n");

		cache_debug_printf("LRU->MRU: ");
		index = m_tail;
		while (index!=-1)
		{
			cache_debug_printf("%d=%p ",index,m_cache[index].obj);
			index = m_cache[index].prev;
		}
		cache_debug_printf("\n");
	}
#endif
    /*! Print miss/hits statistics */
#ifdef CACHE_STATS
#define cache_stats_printf printf
	void printStats()
	{
		cache_stats_printf("ObjCache: hits=%d misses=%d hit ratio=%f\n",hits,misses,hits*100.0/(hits+misses));
	}
#endif

  private:
	  void moveToFront(int index)
	  {
		  int prev,next;
		  if (m_head!=index)
		  {
			  next = m_cache[index].next;
			  prev = m_cache[index].prev;

			  // de-chain node at index
			  m_cache[prev].next = next;
			  if (next!=-1) m_cache[next].prev = prev; else m_tail = prev;

			  // add to head
			  m_cache[index].prev  = -1;
			  m_cache[index].next  = m_head;
			  m_cache[m_head].prev = index;
			  m_head = index;
		  }
	  }
	  unsigned int hash(void *addr)
	  {
		  static bool isPtr64 = sizeof(addr)==8;
		  if (isPtr64)
		  {
			  uint64 key = (uint64)addr;
			  // Thomas Wang's 64 bit Mix Function
			  key += ~(key << 32);
			  key ^=  (key >> 22);
			  key += ~(key << 13);
			  key ^=  (key >> 8);
			  key +=  (key << 3);
			  key ^=  (key >> 15);
			  key += ~(key << 27);
			  key ^=  (key >> 31);
			  return key & (m_size-1);
		  }
		  else
		  {
			  // Thomas Wang's 32 bit Mix Function
			  unsigned long key = (unsigned long)addr;
			  key += ~(key << 15);
			  key ^=  (key >> 10);
			  key +=  (key << 3);
			  key ^=  (key >> 6);
			  key += ~(key << 11);
			  key ^=  (key >> 16);
			  return key & (m_size-1);
		  }
	  }
	  HashNode *hashFind(void *obj)
	  {
		  HashNode *node = 0;
		  int index = m_hash[hash(obj)].head;
		  //printf("hashFind: obj=%p index=%d\n",obj,index);
		  while (index!=-1 &&
			  m_hash[index].obj!=obj
			  ) // search for right object in the list
		  {
			  index = m_hash[index].nextHash;
		  }
		  // found the obj at index, so it is in the cache!
		  if (index!=-1)
		  {
			  node = &m_hash[index];
		  }
		  return node;
	  }
	  HashNode *hashInsert(void *obj)
	  {
		  int index = hash(obj);
		  //printf("Inserting %p index=%d\n",obj,index);

		  // remove element from empty list
		  int newElement = m_freeHashNodes;
		  assert(newElement!=-1);
		  m_freeHashNodes = m_hash[m_freeHashNodes].nextHash;

		  if (m_hash[index].head!=-1) // hash collision -> goto end of the list
		  {
			  index = m_hash[index].head;
			  while (m_hash[index].nextHash!=-1)
			  {
				  index = m_hash[index].nextHash;
			  }
			  // add to end of the list
			  m_hash[index].nextHash = newElement;
		  }
		  else // first element in the hash list
		  {
			  m_hash[index].head = newElement;
		  }
		  // add to the end of the list
		  m_hash[newElement].nextHash = -1;
		  m_hash[newElement].obj = obj;
		  return &m_hash[newElement];
	  }
	  void hashRemove(void *obj)
	  {
		  int index = hash(obj);

		  // find element
		  int curIndex = m_hash[index].head;
		  int prevIndex=-1;
		  while (m_hash[curIndex].obj!=obj)
		  {
			  prevIndex = curIndex;
			  curIndex = m_hash[curIndex].nextHash;     
		  }

		  if (prevIndex==-1) // remove from start
		  {
			  m_hash[index].head = m_hash[curIndex].nextHash;
		  }
		  else // remove in the middle
		  {
			  m_hash[prevIndex].nextHash = m_hash[curIndex].nextHash;
		  }

		  // add curIndex element to empty list
		  m_hash[curIndex].nextHash = m_freeHashNodes;
		  m_hash[curIndex].index = -1;
		  m_hash[curIndex].obj   = 0;
		  m_freeHashNodes = curIndex;
	  }

    CacheNode *m_cache;
    HashNode  *m_hash;
    int        m_head;
    int        m_tail;
    int        m_size;
    int        m_freeHashNodes;
    int        m_freeCacheNodes;
    int        m_lastHandle;

#ifdef CACHE_STATS
    static int misses;
    static int hits;
#endif
};

