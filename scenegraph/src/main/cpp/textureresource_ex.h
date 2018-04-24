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
 
#ifndef __TEXTURE_RESOURCE_EX_H__
#define __TEXTURE_RESOURCE_EX_H__

#if defined(WIN32) || defined(_WIN32)
#include <windows.h>
#endif

#include <stdio.h>
#include <stdlib.h>

#if defined(__APPLE__) || defined(MACOSX)
#include <OpenGL/gl.h>
#else
#include <GL/gl.h>
    #if defined(WIN32) || defined(_WIN32)
    #include <GL/glext.h>
//    #include <GL/wgl.h>
    #include <GL/glext.h>
    #else
    #include <GL/glext.h>
    #include <GL/glx.h>
    #include <GL/glxext.h>
    #endif
#endif

//----------------------------------------------------------------
struct texBlock
{
    int    imgx;
    int    imgy;
    int    texW;
    int    texH;
    char   components;
    int    dataW;
    int    dataH;
    GLuint texId;
    char*  blockfile;   // file on disk that has the pixels
    char*  texData;     // pointer to pixel data for loading data on a seperate
                        // thread.. most of the time it's NULL
};

//------------------------------------------------------------------
enum {
    RGB = 1,
    RGBA,
    BGR,
    BGRA,
    GREY,
};

// 4/5/2012 brg: clarification: a default block is a 512x512 *square of pixels*, not 512 bytes.
#define DEFAULT_BLK_SIZE 512
 
typedef struct MultiLevelTextureSetEX_s {
    texBlock** tex;          // 2D array of texBlocks, 1 array per level
    int*       cols;         // number of columns per array
    int*       rows;         // number of rows per array
    int        blksize;      // size of the texture blocks
    int        levels;       // number of levels
    float*     scales;       // scale of the image at each level
    int*       pyramid_level;// the level of our grids in a full mip-map 
                             // pyramid
    char*      blkdir;       // the directory to find the copies of the blocks
                             // on disk, used for paging

    int        references;   // number of references to the texture set

    int        src_w;        // original width of source image
    int        src_h;        // original height of source image
    float      src_dpi_x;    // original horizontal dpi of source image
    float      src_dpi_y;    // original vertical dpi of source image
    char*      src_name;     // source image name
    char*      src_url;      // source image url
    char       components;   // number of components/channels
    int        src_format;    // format of pixel (RGB, RGBA, BGR, etc.)

} MultiLevelTextureSetEX;


//----------------------------------------------------------------------

MultiLevelTextureSetEX*   create_texset_from_jpeg   (const char* file,int nlevels,
                                int blksize = DEFAULT_BLK_SIZE);
MultiLevelTextureSetEX*   create_texset_from_png    (const char* file,int nlevels,
                                int blksize = DEFAULT_BLK_SIZE);
MultiLevelTextureSetEX*   create_texset_from_bmp    (const char* file,int nlevels,
                                int blksize = DEFAULT_BLK_SIZE);
MultiLevelTextureSetEX*   create_texset_from_tiff   (const char* file,int nlevels,
                                int blksize = DEFAULT_BLK_SIZE);
#ifdef CORELYZER_JPEG2000_SUPPORT
MultiLevelTextureSetEX*   create_texset_from_jp2k   (const char* file,int nlevels,
                                int blksize = DEFAULT_BLK_SIZE);
#endif

void delete_texset(MultiLevelTextureSetEX *texset);

int insert_texset(MultiLevelTextureSetEX* set);

void  free_texset               (int set, bool del_disk_blocks = false);
void  free_all_texsets          (bool del_disk_blocks = false);

bool  is_texset                 (int set);

void  set_texset_url            (int set, char* url);
void  inc_texset_ref_count      (int set);
void  dec_texset_ref_count      (int set);
int   get_texset_ref_count      (int set);  

int   get_texset_num_components   (int set);
int   get_texset_pixel_format     (int set);
int   get_texset_num_levels       (int set);
int   get_texset_src_width        (int set);
int   get_texset_src_height       (int set);
int   get_texset_block_size       (int set);
float get_texset_src_dpi_x        (int set);
float get_texset_src_dpi_y        (int set); 
int   get_texset_num_cols         (int set, int level);
int   get_texset_num_rows         (int set, int level);
int   get_texset_level_in_pyramid (int set, int level);
float get_texset_scale            (int set, int level);
char* get_texset_name             (int set);
char* get_texset_url              (int set);

texBlock* get_tex_block         (int set, int level, int col, int row);
void      bind_texblock         (int set, int level, int col, int row);

int get_image_depth_pix( char *fileName, bool isVertical );

#endif
