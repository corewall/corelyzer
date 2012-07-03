/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen,
 * Sangyoon Lee, Electronic Visualization Laboratory, University of Illinois 
 * at Chicago
 *
 * This software is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along
 * with this software; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Questions or comments about CoreWall should be directed to 
 * cavern@evl.uic.edu
 *
 *****************************************************************************/

#include "canvas.h"
#include "cache.h"
#include "utility.h"
#include <vector>
#include <map>
#include <list>
#include <string>
#include <squish.h>

#include <pthread.h>

//====================================================================
#ifndef __APPLE__

extern PFNGLCOMPRESSEDTEXIMAGE2DARBPROC    glCompressedTexImage2D;
extern PFNGLCOMPRESSEDTEXSUBIMAGE2DARBPROC glCompressedTexSubImage2D;

#endif

//====================================================================
using namespace std;

struct ltvoidptr {
    bool operator()(const void* p1, const void* p2) {
        return (p1 < p2);
    }
};

list< TexCacheEntry* > cache_list;
typedef list< TexCacheEntry* >::iterator list_itr;

map< const void*, list_itr > cache_map;
typedef map< const void*, list_itr >::iterator table_itr;

//===================================================================
// brg 6/27/2012 bump max cache memory from 256MB to 768MB - most modern machines
// have at least 2GB of RAM.  Make this a user-configurable option?
int max_tex_mem = 1024 * 1024 * 768;
int cur_tex_mem = 0;         // Start off at 0 MB of tex memory usage


//===================================================================
int set_max_texmem_usage(int bytes)
{
    if( bytes < cur_tex_mem )
        max_tex_mem = cur_tex_mem;
    else
        max_tex_mem = bytes;

    return max_tex_mem;
}

//=======================================================================
int get_max_texmem_usage()
{
    return max_tex_mem;
}

//======================================================================
int get_cur_texmem_usage()
{
    return cur_tex_mem;
}

//=====================================================================
void clear_tex_cache()
{
    printf("clearing texture cache\n");
	map< const void*, list_itr >::iterator itr = cache_map.begin();
    table_itr ti = itr;
    for( ; ti != cache_map.end(); ti++)
    {
        list_itr li = ti->second;
        texBlock* tb = (*li)->blk;
        if( tb && glIsTexture(tb->texId) == GL_TRUE)
        {
            glDeleteTextures(1,&(tb->texId));
            cur_tex_mem -= tb->texW * tb->texH * tb->components;
        }

        delete (*li);
    }

    cache_list.clear();
    cache_map.clear();
	printf("end of clear_tex_cache\n");
}

//====================================================================
void remove_tex_entry(void* i)
{
    if(!is_tex_cache_entry(i)) return;

    // remove from the table

    table_itr ti  = cache_map.find(i);
    list_itr itr = cache_list.end();
    if( ti != cache_map.end())
    {
        itr = ti->second;
        cache_map.erase(i);
    }

    // free texture memory

    texBlock* k = (texBlock*) i;

    if(k)
    {
        if( glIsTexture(k->texId) == GL_TRUE)
        {
            glDeleteTextures(1,&(k->texId));
            cur_tex_mem -= k->texW * k->texH * k->components;
        }

    } // end if k is valid

    
    delete (*itr);
    cache_list.erase(itr);

}

//===================================================================
void tex_cache_hit(table_itr ti)
{
    list_itr li = ti->second;
    cache_list.splice( cache_list.begin(), cache_list, li);
}


//====================================================================
bool is_tex_cache_entry(void* i)
{
    table_itr ti = cache_map.find(i);

    if( ti == cache_map.end()) return false;
#ifdef DEBUG
    printf("Tex Cache: Cache Hit!\n");
#endif

    tex_cache_hit(ti);

    return true;
}

//===================================================================
struct tex_cache_locator {
    bool usePthread;
    int  set;
    int  level;
    int  col;
    int  row;
};

void* tex_cache_miss_impl(void *threadarg)
{
    struct tex_cache_locator * locator = (struct tex_cache_locator *) threadarg;

    bool usePthread = locator->usePthread;
    int set   = locator->set;
    int level = locator->level;
    int col   = locator->col;
    int row   = locator->row;

    // need to make sure that it's a legitimate block

    if(usePthread)
    {
        printf("[Cache] Impl: set %d, level %d, col %d, row %d\n",
               set,level,col,row);
    }

    texBlock* tb = get_tex_block(set,level,col,row);
    if(!tb)
    {
        if(usePthread)
        {
            printf("TexBlock doesn't exist!\n");
            pthread_exit(NULL);
        }
        else
        {
            return NULL;
        }
    }

    // free up enough memory to store the block
    int mem_avail  = (max_tex_mem - cur_tex_mem);
	int size = 0;
#ifdef USE_FASTDXT
        size = tb->texW * tb->texH * tb->components;
#else
        size = squish::GetStorageRequirements( tb->texW,
                                                    tb->texH,
                                                    squish::kDxt3 );    
#endif
//    int size = squish::GetStorageRequirements(tb->texW, tb->texH, squish::kDxt3);
    int mem_req    = tb->texW * tb->texH * tb->components;
    GLuint replace_id = 0;

    if( is_s3tc_available())
        mem_req = size;

    if( mem_avail - mem_req < 0)
    {

#ifdef DEBUG
        printf("Not enough memory.  Need %d more\n", mem_req - mem_avail);
#endif

        list< TexCacheEntry* >::reverse_iterator ritr=cache_list.rbegin();
        while( ritr != cache_list.rend() && (mem_avail - mem_req < 0))
        {
            texBlock* k = (*ritr)->blk;

            // remove from the table

            table_itr ti  = cache_map.find( (void*) k);
            if( ti != cache_map.end())
            {
                cache_map.erase(ti);
            }

            // free texture memory

            if( k)
            {
                if( glIsTexture(k->texId) == GL_TRUE)
                {
                    // get the format
                    GLint fmt;
                    glGetTexLevelParameteriv( GL_TEXTURE_2D, 0,
                                               GL_TEXTURE_INTERNAL_FORMAT, 
                                               &fmt);

                    if( !is_s3tc_available())
                    {
                        glDeleteTextures(1,&(k->texId));
                        cur_tex_mem -= k->texW * k->texH * k->components;
                        mem_avail += k->texW * k->texH * k->components;
                    }
                    else
                    {
                        // don't delete the texture, just replace and move on
                        mem_avail += size;
                        replace_id = k->texId;
                        k->texId = 0;
                    }
                }
            } // end if k is valid

            delete (*ritr);
            ritr++;

            // remove from the list

            cache_list.pop_back();

        } // end trying to free up enough memory

    } // end if there isn't enough memory
    
    // we should have enough memory, open up the file and grab the pixels
#ifdef DEBUG
    printf("Generating Texture ID\n");
#endif
    
    // are we just replacing??? if we are using DXT3 then yes
    if( replace_id == 0)
        glGenTextures(1,&(tb->texId));
    else
        tb->texId = replace_id;

    if( !tb->texId )
    {
#ifdef DEBUG
        printf("Couldn't get a non-zero id!\n");
#endif

        if(usePthread)
        {
           pthread_exit(NULL);
        }
        else
        {
            return NULL;
        }
    }

#ifdef DEBUG
    printf("Grabbing pixels from file %s!\n", tb->blockfile);
#endif

    static char cwd[1024];
    static string basedir;
    static string blkfile;
    static int point;

#ifdef DEBUG
    static char blkdir[1024];
#endif

    GETCWD( cwd, 1024 );

    basedir = tb->blockfile;
    point   = basedir.rfind('/');
    blkfile = basedir.substr( point + 1 );
    basedir = basedir.substr( 0, point);

    FILE* fptr = fopen(tb->blockfile, "rb");

    if(!fptr)
    {
#ifdef DEBUG
        printf("%s doesn't exist!\n", blkfile.c_str());
#endif

        if(usePthread)
        {
           pthread_exit(NULL);
        }
        else
        {
            return NULL;
        }
    }
    
    char* pixels;

    if( !is_s3tc_available() )
    {
        pixels = new char[ tb->texW * tb->texH * tb->components ];
        fread( pixels, sizeof(char), tb->texW * tb->texH * tb->components, 
               fptr);
        fclose(fptr);
    }
    else
    {
        fread(&size,sizeof(int),1,fptr);
        pixels = new char[ size ];
        fread(pixels,sizeof(char),size,fptr);
        fclose(fptr);
    }

    // create the texture

    if(usePthread)
    {
        printf("[CACHE] Binding ID %d\n", tb->texId);
    }

    glBindTexture(GL_TEXTURE_2D, tb->texId);
    glTexEnvi( GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    GLenum src_fmt;
    int components;
    switch( get_texset_pixel_format(set) )
    {
    case RGB:  src_fmt = GL_RGB;       components = 3; break;
    case RGBA: src_fmt = GL_RGBA;      components = 4; break;
    case BGR:  src_fmt = GL_BGR_EXT;   components = 3; break;
    case BGRA: src_fmt = GL_BGRA_EXT;  components = 4; break;
    case GREY: src_fmt = GL_LUMINANCE; components = 1; break;
    }

#ifdef DEBUG
    printf("Making call to glTexImage2D\n");
#endif

    if(!is_s3tc_available())
    {
        glTexImage2D( GL_TEXTURE_2D, 0, src_fmt,
                      tb->texW, tb->texH, 0, src_fmt,
                      GL_UNSIGNED_BYTE, pixels );

        cur_tex_mem += mem_req; // no compression gain
    }
    else
    {
//        src_fmt = GL_COMPRESSED_RGBA_S3TC_DXT5_EXT; // Squish: DXT3, FastDXT: DXT5
		src_fmt = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
        if( replace_id == 0)
        {
            glCompressedTexImage2D( GL_TEXTURE_2D, 0, src_fmt,
                                    tb->texW, tb->texH, 0, size, pixels);
            cur_tex_mem += size; // approximate compression gain
        }
        else
        {
            glCompressedTexSubImage2D( GL_TEXTURE_2D, 0, 0, 0, 
                                       tb->texW, tb->texH,
                                       src_fmt, size, pixels);
        }

    }
        
/*
    gluBuild2DMipmaps(GL_TEXTURE_2D, components, tb->texW, tb->texH, src_fmt,
                      GL_UNSIGNED_BYTE, pixels);
*/
    delete [] pixels;

    // put into the map and list
    TexCacheEntry* tce;
    tce        = new TexCacheEntry();
    tce->blk   = tb;
    tce->set   = set;
    tce->col   = col;
    tce->row   = row;
    tce->level = level;

#ifdef DEBUG
    printf("Putting TexBlock into Cache\n");
#endif

    cache_list.push_front(tce);
    cache_map[ (void*) tb ] = cache_list.begin();

    if(usePthread)
    {
        printf("[Cache] End of tex_cache_miss_impl(): %d, %d, %d, %d\n", set, level, col, row);
    }

	return NULL;
}

void* testProc(void *threadarg)
{
    struct tex_cache_locator * locator = (struct tex_cache_locator *) threadarg;

    int set   = locator->set;
    int level = locator->level;
    int col   = locator->col;
    int row   = locator->row;

    printf("Do testProc: %d %d %d %d\n",  set, level, col, row);

    printf("End testProc: %d %d %d %d\n", set, level, col, row);

	return NULL;
}

// FIXME: locks on cache_list & cache_map
void tex_cache_miss(int set, int level, int col, int row)
{
    struct tex_cache_locator *locator = new (struct tex_cache_locator);
    
    locator->usePthread = false; // fixme
    locator->set   = set;
    locator->level = level;
    locator->col   = col;
    locator->row   = row;

    if(locator->usePthread)
    {
        // TODO Create a thread to do the cache miss work
        printf("[Cache miss] set: %d, level: %d, col: %d, row: %d\n", locator->set, locator->level, locator->col, locator->row);

        pthread_t pid;
        pthread_create(&pid, NULL, tex_cache_miss_impl, (void *) locator);
        pthread_create(&pid, NULL, testProc, (void *) locator);
        pthread_join(pid, NULL);

        printf("[Cache] End of tex_cache_miss(): %d, %d, %d, %d\n", locator->set, locator->level, locator->col, locator->row);            
    }
    else
    {
        tex_cache_miss_impl((void *) locator);
    }

    // todo: should be freed from pthread spawned
    delete locator;
}
