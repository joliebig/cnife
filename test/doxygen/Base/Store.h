/******************************************************************************
 *
 * $Id: $
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

#include <qglobal.h>
#include <stdio.h>

#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <assert.h>

#include "portable.h"
#include "StorageIntf.h"

#define BLOCK_SIZE         512 // should be >8 and a multiple of 8
#define BLOCK_POINTER_SIZE sizeof(portable_off_t)


#define ASSERTS_ENABLED

#ifdef ASSERTS_ENABLED
#define STORE_ASSERT(x) assert(x)
#else
#define STORE_ASSERT(x)
#endif


/*! @brief The Store is a file based memory manager.
 *
 *  You can open the store using open(). Then obtain a handle via alloc()
 *  followed by a sequence of write() commands to store information,
 *  and finalize it using end(). 
 *  
 *  Later on you locate the information
 *  with seek() using the handle obtained with alloc(), and then use a
 *  sequence of read() calls to read the information back. 
 *
 *  If no longer needed the storage space can be freed using release().
 *  
 *  The store will dynamically grow the file on disk if needed.
 */
class Store : public StorageIntf
{
  public:
    /*! Creates a store. */
	  Store()
	  {
		  m_file       = 0;
		  m_front      = 0;
		  m_head       = 0;
		  m_state      = Init;
		  m_reads      = 0;
		  m_writes     = 0;
	  }

    /*! Releases the store object. Will close the underlying file if opened. */
	~Store()
	{
		if (m_file)   fclose(m_file);

		// clean up free list
		while (m_head)
		{
			Node *node = m_head;
			m_head = node->next;
			delete node;
		}
	}

    /*! Opens the file underlying the store using \a name as the file name. 
     *  Returns 0 upon success, or -1 otherwise.
     */
	int open(const char *name)
	{
		int i;
		STORE_ASSERT(m_state==Init);
		if (m_file) return 0; // already open
		m_file = fopen(name,"w+b");
		if (m_file==0) return -1;

		// first block serves as header, so offset=0 can be used as the end of the list.
		for (i=0;i<BLOCK_SIZE/8;i++)
		{
			fputc('D',m_file);
			fputc('O',m_file);
			fputc('X',m_file);
			fputc('Y',m_file);
			fputc('G',m_file);
			fputc('E',m_file);
			fputc('N',m_file);
			fputc(0,m_file);
		}
		m_front  = BLOCK_SIZE;
		m_head   = 0;
		m_state  = Reading;
		return 0;
	}

    /*! Allocates a handle to write to and read from. */
	portable_off_t alloc()
	{
		STORE_ASSERT(m_state==Reading);
		m_state=Writing;
		portable_off_t pos;
		if (m_head==0) // allocate new block
		{
			//printf("alloc: new block\n");
			if (portable_fseek(m_file,0,SEEK_END)==-1) // go to end of the file
			{
				fprintf(stderr,"Store::alloc: Error seeking to end of file: %s\n",strerror(errno));
				exit(1);
			}
			pos = portable_ftell(m_file);
			STORE_ASSERT( (pos & (BLOCK_SIZE-1))==0 );
			m_front = pos + BLOCK_SIZE; // move front to end of this block
		}
		else // reuse freed block
		{
			//printf("alloc: reuse block: m_head=%d\n",(int)m_head);
			Node *node = m_head;
			pos = node->pos;
			// point head to next free item
			m_head = node->next;
			delete node;
			// move to start of the block
			if (portable_fseek(m_file,pos,SEEK_SET)==-1)
			{
				fprintf(stderr,"Store::alloc: Error seeking to position %d: %s\n",
					(int)pos,strerror(errno));
				exit(1);
			}
			STORE_ASSERT( (pos & (BLOCK_SIZE-1))==0 );
		}
		//printf("%x: Store::alloc\n",(int)pos);
		return pos;
	}

    /*! Writes \a size bytes in array \a buf to the store. 
     *  First alloc() has to be called.
     *  \note The information can only be read after end() has been called.
     */
	int write(const char *buf,uint size)
	{
		STORE_ASSERT(m_state==Writing);
		//printf("%x: Store::write\n",(int)portable_ftell(m_file));
		do
		{
			portable_off_t curPos     = portable_ftell(m_file);
			int bytesInBlock = BLOCK_SIZE - BLOCK_POINTER_SIZE - (curPos & (BLOCK_SIZE-1));
			int bytesLeft    = bytesInBlock<(int)size ? (int)size-bytesInBlock : 0;
			int numBytes     = size - bytesLeft;
			STORE_ASSERT(bytesInBlock>=0);
			STORE_ASSERT(numBytes<=(int)(BLOCK_SIZE-BLOCK_POINTER_SIZE));
			if (numBytes>0)
			{
				if ((int)fwrite(buf,1,numBytes,m_file)!=numBytes)
				{
					fprintf(stderr,"Error writing: %s\n",strerror(errno));
					exit(1);
				}
				m_writes++;
			}
			if (bytesLeft>0) // still more bytes to write
			{
				STORE_ASSERT(((portable_ftell(m_file)+BLOCK_POINTER_SIZE)&(BLOCK_SIZE-1))==0);
				// allocate new block
				if (m_head==0) // no free blocks to reuse
				{
					//printf("%x: Store::write: new: pos=%x\n",(int)m_front,(int)portable_ftell(m_file));
					// write pointer to next block
					if (fwrite(&m_front,BLOCK_POINTER_SIZE,1,m_file)!=1)
					{
						fprintf(stderr,"Error writing to store: %s\n",strerror(errno));
						exit(1);
					}
					STORE_ASSERT(portable_ftell(m_file)==(curPos&~(BLOCK_SIZE-1))+BLOCK_SIZE);

					// move to next block
					if (portable_fseek(m_file,0,SEEK_END)==-1) // go to end of the file
					{
						fprintf(stderr,"Store::alloc: Error seeking to end of file: %s\n",strerror(errno));
						exit(1);
					}
					STORE_ASSERT(portable_ftell(m_file)==m_front);
					// move front to the next of the block
					m_front+=BLOCK_SIZE;
				}
				else // reuse block from the free list
				{
					// write pointer to next block
					if (fwrite(&m_head->pos,BLOCK_POINTER_SIZE,1,m_file)!=1)
					{
						fprintf(stderr,"Error writing to store: %s\n",strerror(errno));
						exit(1);
					}
					Node *node = m_head;
					portable_off_t pos = node->pos;
					// point head to next free item
					m_head = node->next;
					delete node;
					// move to start of the block
					if (portable_fseek(m_file,pos,SEEK_SET)==-1)
					{
						fprintf(stderr,"Store::write: Error seeking to position %d: %s\n",
							(int)pos,strerror(errno));
						exit(1);
					}
					//printf("%x: Store::write: reuse\n",(int)pos);
				}
			}
			size-=numBytes;
			buf+=numBytes;
		}
		while (size>0);
		return size;
	}


    /*! Ends the sequence of writes. 
     *  \note After this call, first alloc() has to be called
     *  before new writes can be done.
     */
	void end()
	{
		STORE_ASSERT(m_state==Writing);
		portable_off_t curPos     = portable_ftell(m_file);
		int bytesInBlock = BLOCK_SIZE - (curPos & (BLOCK_SIZE-1));
		//printf("%x: Store::end erasing %x bytes\n",(int)curPos&~(BLOCK_SIZE-1),bytesInBlock);
		//printf("end: bytesInBlock=%x\n",bytesInBlock);
		// zero out rest of the block
		int i;
		for (i=0;i<bytesInBlock;i++)
		{
			fputc(0,m_file);
		}
		m_state=Reading;
	}

    /*! Releases the memory corresponding to the handle returned with alloc() */
	void release(portable_off_t pos)
	{
		STORE_ASSERT(m_state==Reading);
		//printf("%x: Store::release\n",(int)pos);
		STORE_ASSERT(pos>0 && (pos & (BLOCK_SIZE-1))==0);
		// goto end of the block
		portable_off_t cur = pos, next;
		while (1)
		{
			// add new node to the free list
			Node *node = new Node;
			node->next = m_head;
			node->pos = cur;

			m_head = node;
			// goto the end of cur block
			if (portable_fseek(m_file,cur+BLOCK_SIZE-BLOCK_POINTER_SIZE,SEEK_SET)==-1)
			{
				fprintf(stderr,"Store::release: Error seeking to position %d: %s\n",
					(int)(cur+BLOCK_SIZE-BLOCK_POINTER_SIZE),strerror(errno));
				exit(1);
			}
			// read pointer to next block
			if (fread(&next,BLOCK_POINTER_SIZE,1,m_file)!=1)
			{
				fprintf(stderr,"Store::release: Error reading from store: %s\n",strerror(errno));
				exit(1);
			}
			if (next==0) break; // found end of list -> cur is last element
			STORE_ASSERT((next & (BLOCK_SIZE-1))==0);
			cur = next;
			//printf("%x: Store::release\n",(int)cur);
		}
	}


    /*! Closes the store */
	void close()
	{
		if (m_file) fclose(m_file);
		m_file=0;
		m_state  = Init;
	}

    /*! Goes to the start of information corresponding to handle \a pos */
	void seek(portable_off_t pos)
	{
		STORE_ASSERT(m_state==Reading);
		//printf("%x: Store::seek\n",(int)pos);
		if (portable_fseek(m_file,pos,SEEK_SET)==-1)
		{
			fprintf(stderr,"Store::seek: Error seeking to position %d: %s\n",
				(int)pos,strerror(errno));
			exit(1);
		}
		STORE_ASSERT((pos&(BLOCK_SIZE-1))==0);
	}


    /*! Reads \a size bytes from the store into the array pointed to be \a buf.
     *  \note Before reading seek() has to be called to set the right start of the store.
     */
	int read(char *buf,uint size)
	{
		STORE_ASSERT(m_state==Reading);
		//printf("%x: Store::read total=%d\n",(int)portable_ftell(m_file),size);
		do
		{
			portable_off_t curPos     = portable_ftell(m_file);
			int bytesInBlock = BLOCK_SIZE - BLOCK_POINTER_SIZE - (curPos & (BLOCK_SIZE-1));
			int bytesLeft    = bytesInBlock<(int)size ? (int)size-bytesInBlock : 0;
			int numBytes     = size - bytesLeft;
			//printf("  Store::read: pos=%x num=%d left=%d\n",(int)curPos,numBytes,bytesLeft);

			if (numBytes>0)
			{
				//printf("%x: Store::read: %d out of %d bytes\n",(int)portable_ftell(m_file),numBytes,size);
				if ((int)fread(buf,1,numBytes,m_file)!=numBytes)
				{
					fprintf(stderr,"Error reading from store: %s\n",strerror(errno));
					exit(1);
				}
				m_reads++;
			}
			if (bytesLeft>0)
			{
				portable_off_t newPos;
				// read offset of the next block
				STORE_ASSERT(((portable_ftell(m_file)+BLOCK_POINTER_SIZE)&(BLOCK_SIZE-1))==0);
				if (fread((char *)&newPos,BLOCK_POINTER_SIZE,1,m_file)!=1)
				{
					fprintf(stderr,"Error reading from store: %s\n",strerror(errno));
					exit(1);
				}
				//printf("%x: Store::read: continue in next block, %d bytes to go\n",(int)newPos,bytesLeft);
				//printf("  Store::read: next block=%x\n",(int)newPos);
				STORE_ASSERT(newPos!=0);
				STORE_ASSERT((newPos&(BLOCK_SIZE-1))==0);
				curPos = newPos;
				// move to next block
				if (portable_fseek(m_file,curPos,SEEK_SET)==-1)
				{
					fprintf(stderr,"Store::read: Error seeking to position %d: %s\n",
						(int)curPos,strerror(errno));
					exit(1);
				}
			}

			size-=numBytes;
			buf+=numBytes;
		}
		while (size>0);
		return size;
	}

    void printStats()
	{
		printf("ObjStore: block size %d bytes, total size %ld blocks, wrote %d blocks, read %d blocks\n",
			BLOCK_SIZE,(long)(m_front/BLOCK_SIZE),m_reads,m_writes);
	}


  private:
    enum State
    {
      Init,
      Reading,
      Writing
    };
    struct Node
    {
      portable_off_t pos;
      struct Node *next;
    };
    void printFreeList()
	{
		printf("FreeList: ");
		portable_off_t pos = m_head->pos;
		while (pos)
		{
			printf("%x ",(int)pos);
			m_head = m_head->next;
		}
		printf("\n");
	}

    FILE *m_file;
    portable_off_t m_front;
    Node *m_head;
    State m_state;
    int m_reads;
    int m_writes;
};

