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
 
#ifndef __CORELYZER_CACHE_H__
#define __CORELYZER_CACHE_H__

#if defined(WIN32) || defined(_WIN32)
#include <windows.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#if defined(__APPLE__) || defined(MACOSX)
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#else
#include <GL/gl.h>
#include <GL/glu.h>
#include <GL/glext.h>
#endif


//#include <baseimghandler.h>
#include <textureresource_ex.h>


typedef struct TexCacheEntry_s {
    int set;
    int level;
    int col;
    int row;
    texBlock* blk;
} TexCacheEntry;

int  set_max_texmem_usage (int bytes);
int  get_max_texmem_usage ();
int  get_cur_texmem_usage ();

void clear_tex_cache      ();
void remove_tex_entry     (void* i);

bool is_tex_cache_entry   (void* i);
void tex_cache_miss       (int set, int level, int col, int row);


#endif
