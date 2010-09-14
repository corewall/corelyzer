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

#if defined(WIN32) || defined(_WIN32)
#include <windows.h>
#include <io.h>   // For access().
#include <iostream>
#endif

#include <stdio.h>
#include <stdlib.h>

#include <tiff.h>
#include <tiffio.h>
#include <png.h>

// For checking file system
#include <sys/types.h>  // For stat().
#include <sys/stat.h>   // For stat().
#include <string>

extern "C" {

#if defined(WIN32) || defined(_WIN32)
#define XMD_H
#endif

#include <jpeglib.h>
#include <jerror.h>

// For JPEG2000
#include "openjpeg.h"

#define J2K_CFMT 0
#define JP2_CFMT 1
#define JPT_CFMT 2

#define PXM_DFMT 10
#define PGX_DFMT 11
#define BMP_DFMT 12
#define YUV_DFMT 13
#define TIF_DFMT 14

#ifndef WIN32
#define stricmp strcasecmp
#define strnicmp strncasecmp
#endif

// SHA-1 Hash function
#include "sha1.h"

using namespace std;

/**
  sample error callback expecting a FILE* client object
 */
void error_callback(const char *msg, void *client_data) {
    FILE *stream = (FILE*)client_data;
    fprintf(stream, "[JP2 - ERROR] %s", msg);
}
/**
  sample warning callback expecting a FILE* client object
 */
void warning_callback(const char *msg, void *client_data) {
    FILE *stream = (FILE*)client_data;
    fprintf(stream, "[JP2 - WARNING] %s", msg);
}
/**
  sample debug callback expecting no client object
 */
void info_callback(const char *msg, void *client_data) {
    (void)client_data;
    fprintf(stdout, "[JP2 - INFO] %s", msg);
}

int get_file_format(char *filename) {
    unsigned int i;
    static const char *extension[] = {"pgx", "pnm", "pgm", "ppm", "bmp","tif", "j2k", "jp2", "jpt", "j2c" };
    static const int format[] = { PGX_DFMT, PXM_DFMT, PXM_DFMT, PXM_DFMT, BMP_DFMT, TIF_DFMT, J2K_CFMT, JP2_CFMT, JPT_CFMT, J2K_CFMT };
    char * ext = strrchr(filename, '.');
    if (ext == NULL)
	return -1;
    ext++;
    if(ext) {
	for(i = 0; i < sizeof(format)/sizeof(*format); i++) {
	    if(strnicmp(ext, extension[i], 3) == 0) {
		return format[i];
	    }
	}
    }

    return -1;
}

void pokeOpjImg(opj_image_t * opjimage)
{
    int x0 = opjimage->x0;
    int y0 = opjimage->y0;
    int x1 = opjimage->x1;
    int y1 = opjimage->y1;

    int numcomps = opjimage->numcomps;

    printf("x0: %d, y0: %d, x1: %d, y1: %d, numcomps: %d\n",
	    x0, y0, x1, y1, numcomps);
}

};

#include <string>
#include <vector>
#include <math.h>
#include <setjmp.h>

#include "textureresource_ex.h"
#include "utility.h"
#include "cache.h"
#include "canvas.h"

#include <squish.h>
#include <util.h>

#ifdef USE_FASTDXT
#include "libdxt.h"
#endif

extern char* default_block_dir;

vector< MultiLevelTextureSetEX* > texsetvec;

// determine whether this is bigendian system
const int i = 1;
#define is_bigendian() ( (*(char*)&i) == 0 )

unsigned long swap_bytes (unsigned long nLongNumber)
{
   return (((nLongNumber&0x000000FF)<<24)+((nLongNumber&0x0000FF00)<<8)+
   ((nLongNumber&0x00FF0000)>>8)+((nLongNumber&0xFF000000)>>24));
}

void replace_windows_seperators(string &fname)
{
    int p;
    string sep("/");
    while( (p = fname.find("\\")) != fname.length() && p >= 0 )
    {
#ifdef DEBUG
        printf("Replacing \\ with / at %d\n", p);
#endif
        fname = fname.replace(p,1,sep);
    }
}

//====================================================================
void copy_pixels_to_rgba(char* in_pix, char* out_pix, int in_fmt, 
                         int w, int h )
{
    if( in_fmt == RGBA )
    {
        memcpy( out_pix, in_pix, w * h * 4);
        return;
    }

    if( in_fmt == BGRA )
    {
        memcpy( out_pix, in_pix, w * h * 4);
        for( int i = 0; i < w * h * 4; i += 4)
        {
            out_pix[i]   = in_pix[i+2];
            out_pix[i+2] = in_pix[i];
        }
        return;
    }

   
    memset( out_pix, 255, w * h * 4 );

    if( in_fmt == GREY )
    {
        for( int i = 0; i < w * h * 4; i += 4)
        {
            int ii;
            ii = i / 4;
            out_pix[i]   = in_pix[ii];
            out_pix[i+1] = in_pix[ii];
            out_pix[i+2] = in_pix[ii];
        }
        return;
    }

    if( in_fmt == BGR )
    {
        for( int i = 0; i < w * h * 4; i += 4)
        {
            int ii;
            ii = (i / 4) * 3;

            out_pix[i]   = in_pix[ii+2];
            out_pix[i+1] = in_pix[ii+1];
            out_pix[i+2] = in_pix[ii];

        }
        return;
    }

    // RGB
    for( int i = 0; i < w * h * 4; i += 4)
    {
        int ii;
        ii = (i / 4) * 3;
        
        out_pix[i]   = in_pix[ii];
        out_pix[i+1] = in_pix[ii+1];
        out_pix[i+2] = in_pix[ii+2];

    }


}

//====================================================================
// Given in and out buffers, scale a part of the buffer to fit into out buffer
//====================================================================
void get_sub_image_scaled( char* in_pix, char* out_pix, int x, int y,
                           int w, int h, float s, MultiLevelTextureSetEX* t)
{
    if( !in_pix || !out_pix || !t )
        return;
    if( s == 0 )
        return;
    if( x < 0 || x >= t->src_w )
        return;
    if( y < 0 || y >= t->src_h )
        return;

    int   copy_w, copy_h, skip_x, skip_y;
    copy_w = int( w * s );
    copy_h = int( h * s );
    skip_x = copy_w / w;
    skip_y = copy_h / h;

    memset( out_pix, 0, w * h * t->components );
    if( t->src_w < x + w )
        copy_w = t->src_w - x;
    if( t->src_h < y + h )
        copy_h = t->src_h - y;

    for( int i = 0; i < h; ++i)
    {
        for( int j = 0; j < w; ++j)
        {
            for( int k = 0; k < t->components; ++k)
            {
                if( y + (i * skip_y) < t->src_h && x + (j * skip_x) < t->src_w)
                {
                    out_pix[ (i * w + j) * t->components + k] =
                        in_pix[ ((y + (i * skip_y)) * t->src_w + 
                                 (x + (j * skip_x))) * t->components + k];
                }
                else
                {
                    out_pix[ (i * w + j) * t->components + k] = 0;
                }
            }
        }
    }

}

// Crossplatform way of check if a directory exists
bool isDirectoryExists(const char* aPath)
{
	if ( access( aPath, 0 ) == 0 )
    {
        struct stat status;
        stat( aPath, &status );

        if ( status.st_mode & S_IFDIR )
        {
            return true;
        }
        else
        {
			// It's actually a file!
            return false;
        }
    }
    else
    {
		// path does not exist
        return false;
    }
}

// Check whether texture block files already exhist.
// An integrity ticket file?
bool isTextureBlocksExist(const char* filename, const char *blockDir)
{
    if(!filename || !blockDir) return false;

    string filenameString(filename);
    string blockDirString(blockDir);

	replace_windows_seperators(filenameString);
    replace_windows_seperators(blockDirString);

#ifdef DEBUG
    // printf("- [iTBE] filename: %s\n", filenameString.c_str());
    // printf("- [iTBE] blockdir: %s\n", blockDirString.c_str());
#endif

    return isDirectoryExists(blockDirString.c_str());
}

//====================================================================
// Given pixel buffer build texblocks for the MultiLevelTextureSetEX
//====================================================================
// 90% of time in the most detailed level tile generation!!!

#define SQUISH  (1)
#define FASTDXT (2)

void delete_texset(MultiLevelTextureSetEX *texset)
{
    if(texset) {
        // Delete stuff inside MultiTexSet*
        for(int i = 0; i < texset->levels; ++i) {
            delete [] texset->tex[i];
        }

        delete [] texset->src_name;
        delete [] texset->blkdir;

        delete [] texset->scales;
        delete [] texset->rows;
        delete [] texset->cols;
        delete [] texset->tex;
        delete [] texset->pyramid_level;        
    }
}

void build_tex_blocks(char* pixels, MultiLevelTextureSetEX* set, bool hasDir, int library)
{
#ifdef DEBUG
    printf("-- Building tex blocks with library (1. Squish, 2. FastDXT) %d\n", library);
#endif

    if( !set )
    {
#ifdef DEBUG
        printf("- [build_tex_blocks] No set, return.\n");
#endif

        return;
    }

    if( !pixels && !hasDir)
    {
#ifdef DEBUG
        printf("- [build_tex_blocks] No pixels, has no dir.\n");
#endif

        return;
    }

    if( set->levels < 1 )
        set->levels = 1;

    // max number of levels before all the pixels fit into 1 texBlock
    int lh, lv;
    lh = (int) ceil( float(set->src_w) / float(set->blksize));
    lv = (int) ceil( float(set->src_h) / float(set->blksize));
    float exponent; // blksize * 2 ^ exp can encompass image

    if( lh > lv )
        exponent = ceilf( log((float) lh) / log(2.0)) + 1;
    else
        exponent = ceilf( log((float) lv) / log(2.0)) + 1;

    if( exponent < set->levels ) set->levels = (int) exponent;

    set->scales = new float[ set->levels ];
    set->rows   = new int[ set->levels ];
    set->cols   = new int[ set->levels ];
    set->tex    = new texBlock*[ set->levels ];
    set->pyramid_level = new int[ set->levels];

    // make sure imgblock directory is there

    char cwd[1024];
    GETCWD( cwd, 1024 );

    // Check if the block dir exists
    FILE* fptr0 = fopen(set->blkdir, "r");
    if(!fptr0)
    {
#ifdef DEBUG
        printf("Couldn't change CWD to %s\n", set->blkdir);
        printf("Making directory %s\n", set->blkdir);
        printf("CWD is %s\n", cwd);
#endif
        MKDIR( set->blkdir );
    }
    else
    {
        fclose(fptr0);
    }

    char* tex_data = new char[ set->blksize * set->blksize * set->components ];

    // DXT3 related
    int dxt3_size = 0;

#ifdef USE_FASTDXT
        dxt3_size = set->blksize * set->blksize * set->components;
#else
        dxt3_size = squish::GetStorageRequirements( set->blksize,
                                                    set->blksize,
                                                    squish::kDxt3 );    
#endif

#ifdef DEBUG
    printf("blksize: %d, components: %d\n", set->blksize, set->components);
    printf("dxt_size: %d\n", dxt3_size);
#endif    

    char* tex_to_file = new char[ dxt3_size ];
    char* tex_rgba = new char[ set->blksize * set->blksize * 4 ];

    // end DXT3 related until later when writing to file

    float final_scale    = 1.0f / powf( 2.0f, exponent);
    float scale_interval = 1.0f;
    int   level_interval = 1;

    if( set->levels != 1)
    {
        scale_interval = float(1.0 - final_scale) / float( set->levels - 1);
        level_interval = (int) ceil( exponent / set->levels) + 1;
    }

    // make texture blocks and store them off into the appropriate 
    // directories

#ifdef DEBUG
    printf("Going to generate %d levels\n", set->levels);
    int blockCount = 0;

    // The highest level takes 90% of the time
    printf("-- [OPTIMIZE] Working on %d levels in build_tex_blocks()\n", set->levels);
#endif

    for( int k = 0; k < set->levels; ++k)
    {
        int level;
        if( k * level_interval < exponent )
            level = k * level_interval;
        else
            level = (int) exponent;

        // make level directory
        
        char level_dir[5];
        sprintf(level_dir, "%d", level);
        set->scales[k] = 1.0f / powf( 2.0f, level );
        set->pyramid_level[k] = level;

#ifdef DEBUG
        printf("\t### Next Scale %f, Image Size %.2f x %.2f ###\n",
               set->scales[k],
               float(set->src_w) * set->scales[k],
               float(set->src_h) * set->scales[k]);
#endif

        set->cols[k]   = (int) ceil( float( set->src_w * set->scales[k]) /
                                     float( set->blksize));
        set->rows[k]   = (int) ceil( float( set->src_h * set->scales[k]) /
                                     float( set->blksize));

        float lvl_blksize = set->blksize / set->scales[k];
        set->tex[k]       = new texBlock[ set->cols[k] * set->rows[k] ];

#ifdef DEBUG
        printf("-- [OPTIMIZE] Level %d, Rows %d, Cols %d ###\n", k, set->rows[k],
               set->cols[k]);
        printf("-- [OPTIMIZE] lvl_blksize: %.3f\n", lvl_blksize);
        printf("-- [OPTIMIZE] set->blksize: %d\n", set->blksize);
#endif

        // go through rows and columns

        for( int i = 0; i < set->rows[k]; ++i)
        {
            for( int j = 0; j < set->cols[k]; ++j)
            {
                float coverage_w, coverage_h;
                int id = i * set->cols[k] + j;

                set->tex[k][id].imgx        = int(j * (lvl_blksize));
                set->tex[k][id].imgy        = int(i * (lvl_blksize));
                set->tex[k][id].texW        = set->blksize;
                set->tex[k][id].texH        = set->blksize;
                set->tex[k][id].dataW       = int(lvl_blksize);
                set->tex[k][id].dataH       = int(lvl_blksize);
                set->tex[k][id].texData     = NULL;
                set->tex[k][id].texId       = 0;
                set->tex[k][id].components  = set->components;

                // make sure our dataW & dataH aren't incorrect
                
                int real_x, real_y;
                real_x = set->tex[k][id].imgx;
                real_y = set->tex[k][id].imgy;
                if( real_x + set->tex[k][id].dataW >= set->src_w)
                {
                    set->tex[k][id].dataW = int(set->src_w - real_x);
                }

                if( real_y + set->tex[k][id].dataH >= set->src_h)
                {
                    set->tex[k][id].dataH = int(set->src_h - real_y);
                }

#ifdef DEBUG
                printf("Tex Block (%d, %d) at (%d, %d) covering %d x %d\n",
                       j, i, real_x, real_y,
                       set->tex[k][id].dataW, set->tex[k][id].dataH);
#endif

                // write pixels out to file
                // named b<col>_<row>

                char buf[15];
                string blockfile_name;
                string basedir;
                blockfile_name = "";

#ifndef _WIN32
                if( set->blkdir[0] != '/')
                    blockfile_name = cwd;

		        blockfile_name += "/";
#else
		        if( set->blkdir[1] != ':') {
                    blockfile_name = cwd;
		            blockfile_name += "/";
		        }
#endif

                blockfile_name += set->blkdir;
                blockfile_name += "/";

                blockfile_name += level_dir;
                blockfile_name += "/";

                // Check if level dir exists
                FILE* fptr1 = fopen(blockfile_name.c_str(), "r");
                if(!fptr1)
                {
                    MKDIR( blockfile_name.c_str());
                }
                else
                {
                    fclose(fptr1);
                }
                basedir = blockfile_name;

                blockfile_name += "b";
                sprintf(buf,"%d",j);
                blockfile_name += buf;
                blockfile_name += "_";
                sprintf(buf,"%d",i);
                blockfile_name += buf;

                // make sure it isn't there already
                FILE* fptr = fopen(blockfile_name.c_str(),"rb");
                if( !fptr )
                {
#ifdef DEBUG
                    printf("Making blockfile %s\n", blockfile_name.c_str());
#endif

                    // get pixels scaled to right size

                    get_sub_image_scaled( pixels, tex_data, real_x, real_y,
                                          set->blksize, set->blksize, 
                                          1.0f / set->scales[k], set);

                    fptr = fopen( blockfile_name.c_str(), "wb");
                    if( fptr )
                    {
                        // if S3TC DXT3 available compress, otherwise
                        // store raw blocks
                        if( !is_s3tc_available() )
                        {
#ifdef DEBUG
                            printf("---> [INFO] S3TC DXT3 not available, using raw blocks\n");
#endif

                            fwrite(tex_data, sizeof(char),
                                   set->blksize * set->blksize * set->components,
                                   fptr);
                            fclose(fptr);
                        }
                        else
                        {
#ifdef DEBUG
                            printf("---> [INFO] Compress with S3TC DXT3 with Squish.\n");
                            // printf("DXT3 Compression block %d, %d of size: %d, %d\n",
                            //    i, j,
                            //    set->tex[k][id].texW, set->tex[k][id].texH);
#endif

                            double time1 = aTime();

                            copy_pixels_to_rgba(tex_data, tex_rgba,
                                                set->src_format,
                                                set->blksize,
                                                set->blksize);

                            // time-Copy:time-Compress(Squish):time-Fops = 1:9:2
                            /*
                            if(library == FASTDXT) // Use FastDXT library
                            {
                                int outNBytes = CompressDXT((byte *) tex_rgba, (byte *) tex_to_file,
                                                            set->blksize, set->blksize, FORMAT_DXT5, 2);

                                fwrite(&outNBytes, sizeof(int), 1, fptr);
                                fwrite(tex_to_file, sizeof(char), outNBytes, fptr);
                            }
                            else // Use Squish library
                            {
                                squish::CompressImage((squish::u8*)tex_rgba,
                                                      set->blksize,
                                                      set->blksize,tex_to_file,
                                                      squish::kDxt3 |
                                                      squish::kColourRangeFit);

                                fwrite(&dxt3_size,  sizeof(int),  1, fptr);
                                fwrite(tex_to_file, sizeof(char), dxt3_size, fptr);
                            }*/
#ifdef USE_FASTDXT
                                int outNBytes = CompressDXT((byte *) tex_rgba, (byte *) tex_to_file,
                                                            set->blksize, set->blksize, FORMAT_DXT5, 2);

                                fwrite(&outNBytes, sizeof(int), 1, fptr);
                                fwrite(tex_to_file, sizeof(char), outNBytes, fptr);
#else
                                squish::CompressImage((squish::u8*)tex_rgba,
                                                      set->blksize,
                                                      set->blksize,tex_to_file,
                                                      squish::kDxt3 |
                                                      squish::kColourRangeFit);

                                fwrite(&dxt3_size,  sizeof(int),  1, fptr);
                                fwrite(tex_to_file, sizeof(char), dxt3_size, fptr);                            
#endif
                            fclose(fptr);
                        }
                    }
                    else
                    {
                        printf("ERROR: Couldn't write to blockfile %s\n",
                               blockfile_name.c_str());
                    }
                }
                else
                {
                    fclose(fptr);
#ifdef DEBUG
                    printf("Blockfile %s already exists, using that file\n",
                           blockfile_name.c_str());
#endif
                }

                set->tex[k][id].blockfile = new char[blockfile_name.size() +1];
                strcpy( set->tex[k][id].blockfile, blockfile_name.c_str());
            }
        } // end looping through rows and columns

    } // end for each level

    if( tex_data )
        delete [] tex_data;
    if( tex_rgba )
        delete [] tex_rgba;
    if( tex_to_file )
        delete [] tex_to_file;
}

void build_tex_blocks(char* pixels, MultiLevelTextureSetEX* set, bool hasDir)
{
#ifdef USE_FASTDXT
    build_tex_blocks(pixels, set, hasDir, FASTDXT);
#else
    build_tex_blocks(pixels, set, hasDir, SQUISH);
#endif
}

void build_tex_blocks(char* pixels, MultiLevelTextureSetEX* set)
{
    // Default library to use: Squish or FastDXT
#ifdef USE_FASTDXT
    build_tex_blocks(pixels, set, false, FASTDXT);
#else
    build_tex_blocks(pixels, set, SQUISH);
#endif    
}


//====================================================================
// Places the texture set into the vector, if it isn't already there
//====================================================================
int insert_texset(MultiLevelTextureSetEX* set)
{
    if(!set) return -1;

    // make sure it isn't already there
    
    int i;
    for( i = 0; i < texsetvec.size(); ++i)
    {
        if( texsetvec[i] == set)
            return i;
    }

    // find a null spot

    for( i = 0; i < texsetvec.size(); ++i)
    {
        if( texsetvec[i] == NULL )
        {
            texsetvec[i] = set;
            return i;
        }

    }

    // place at the end

    texsetvec.push_back(set);
    return texsetvec.size() - 1;
}

/* Private function to generate SHA1-prefix texture block directory string
   The caller is responsible to release the string after using it.
*/
char* generateBlockDirString(string basedir, string fname)
{
    int point = fname.rfind('/');
    string blkdir = fname.substr( point + 1 );
    blkdir = basedir + blkdir;

    char *output = new char[blkdir.length() + 1];
    strcpy(output, blkdir.c_str());

    return output;

    // FIXME: crash unknown in win32?!
    /*
    string blkdir(fname);

    string hash("");

    SHA1Context sha;
    SHA1Reset(&sha);
    SHA1Input(&sha, (const unsigned char *) blkdir.c_str(), blkdir.length());

    if (!SHA1Result(&sha))
    {
        fprintf(stderr, "ERROR-- could not compute message digest\n");
    }
    else
    {
        for(int i = 0; i < 5 ; i++)
        {
            char *p = new char[8];
            sprintf(p, "%X", sha.Message_Digest[i]);
            string piece(p);
            hash = hash + piece
            delete [] p;
        }
    }

    int point = blkdir.rfind('/');
    blkdir    = blkdir.substr( point + 1 );
    blkdir    = basedir + hash + "-" + blkdir;

    char *output = new char[blkdir.length() + 1];
    strcpy(output, blkdir.c_str());

    return output;
    */
}

//====================================================================
// Load JPEG file 
//====================================================================
MultiLevelTextureSetEX* create_texset_from_jpeg(const char* filename, int nlevels, int blksize)
{
    // For measure time
#ifdef OPT
    aInitialize();
#endif

    if(!filename) return NULL;

#ifdef DEBUG
    printf("Replacing \\ with / for string:\n%s\n",filename);
#endif

    string fname(filename);
    replace_windows_seperators(fname);

#ifdef DEBUG
    printf("Now string is: %s\n", fname.c_str());
#endif

    jpeg_decompress_struct cinfo;
    jpeg_error_mgr         jerr;

    FILE* fptr = fopen( fname.c_str(), "rb");
    if( fptr == NULL )
    {
        printf("Cannot open %s\n", filename);
        jpeg_destroy_decompress(&cinfo);
        return NULL; //-1;
    }

    // create texture set struct and init basic info
    // name, block directory, block sizes

#ifdef DEBUG
    printf("Opened file pointer.  Starting creation process\n");
#endif

    MultiLevelTextureSetEX* texset = new MultiLevelTextureSetEX();

    texset->tex        = NULL;
    texset->cols       = NULL;
    texset->rows       = NULL;
    texset->references = 0;
    texset->blksize    = blksize;

    texset->src_name = new char[ strlen(fname.c_str()) + 1];
    strcpy(texset->src_name, fname.c_str());

    // Generate a unique directory name to store the texture blocks generated.
    string basedir(default_block_dir);
    texset->blkdir = generateBlockDirString(basedir, fname);
    string blkdir(texset->blkdir);

#ifdef DEBUG
    printf("TexSet properties so far:\n");
    printf("Block Size %d\nName %s\nMipMap Dir %s\n", texset->blksize,
           texset->src_name, texset->blkdir);
#endif

#ifdef OPT
    double time1 = aTime();
#endif

    // use jpeg library to get to pixels and info
    cinfo.err = jpeg_std_error(&jerr);

    // setup decompression and I/O
    jpeg_create_decompress( &cinfo );

    jpeg_stdio_src( &cinfo, fptr);

    jpeg_read_header(&cinfo,TRUE);

    jpeg_start_decompress( &cinfo);

    //determine color space, only support RGB on jpegs

    texset->components = 0;
    switch( cinfo.out_color_space )
    {
        case JCS_RGB:
            texset->components  = 3;
            texset->src_format = RGB;
            break;

        case JCS_GRAYSCALE:
            texset->components = 1;
            texset->src_format = GREY;
            break;

        default:
            break;
    }

    if( !texset->components )
    {

#ifdef DEBUG
        printf("Unsupported JPEG color mode!\n");
#endif
        jpeg_finish_decompress( &cinfo );
        jpeg_destroy_decompress( &cinfo);
        fclose(fptr);

        delete [] texset->blkdir;
        delete [] texset->src_name;
        delete texset;
        return NULL; //-1;
    }

    // Image width and height

    texset->src_w = cinfo.output_width;
    texset->src_h = cinfo.output_height;

    // size of a row, and allocation of buffers for reading & storing pixels
    int row_stride = cinfo.output_width * cinfo.output_components;

    char* pixels = NULL;
    char* buffer = NULL;
    
    // Grab each scanline and store
    // Ignore scanning if textures are already created

#ifdef OPT
    double time2 = aTime();
#endif

    bool hasDir = isTextureBlocksExist(fname.c_str(), blkdir.c_str());

#ifdef DEBUG
    printf("- [TODO] Texture block dir exists: %d\n", hasDir);
#endif    

    if(!hasDir)
    {
        pixels = new char[ row_stride * texset->src_h ];
        buffer = new char[ row_stride ]; 

        while( cinfo.output_scanline < cinfo.output_height )
        {
            jpeg_read_scanlines( &cinfo, (JSAMPLE**) &buffer, 1);
            memcpy( pixels + ((cinfo.output_scanline - 1) * row_stride),
                    buffer, row_stride);
        }

        delete [] buffer;
    }

#ifdef OPT
    double time3 = aTime();
#endif

    // determine dpi

    switch( cinfo.density_unit )
    {
        case 0: // UNKNOWN
            texset->src_dpi_x = 150;
            texset->src_dpi_x = 150;
            break;
        case 1: // DPI
            texset->src_dpi_x = cinfo.X_density;
            texset->src_dpi_y = cinfo.Y_density;
            break;
        case 2: // DPCM, 2.54 cm / 1 inch
            texset->src_dpi_x = cinfo.X_density * 2.54f;
            texset->src_dpi_y = cinfo.Y_density * 2.54f;
            break;
        default:
            texset->src_dpi_x = 150;
            texset->src_dpi_y = 150;
            break;
    }

    // close up jpeg stuff

    if(!hasDir)
    {
        jpeg_finish_decompress( &cinfo );
    }

    jpeg_destroy_decompress( &cinfo );
    fclose(fptr);

#ifdef OPT
    double time4 = aTime();
#endif

    // build texblocks and insert tex set
    texset->levels = nlevels;

    // Start building and saving texture blocks
    build_tex_blocks( pixels, texset, hasDir );

    if(pixels != NULL)
    {
        delete [] pixels;    
    }

#ifdef OPT
    double time5 = aTime();
#endif

#ifdef OPT
    fprintf(stdout, "[JPEG] Prep jpeg\t%f\n", (time2-time1));
    fprintf(stdout, "[JPEG] JPEG scan\t%f\n", (time3-time2));
    fprintf(stdout, "[JPEG] Finishup\t%f\n----\n", (time4-time3));

    fprintf(stdout, "[JPEG] JPEG Part\t%f\n", (time4-time1));
    fprintf(stdout, "[JPEG] Block Part\t%f\n", (time5-time4));
#endif

    return texset;
    // return insert_texset(texset);
}

//====================================================================
// Load PNG file
//====================================================================

MultiLevelTextureSetEX* create_texset_from_png(const char* filename, int nlevels, int blksize)
{
#ifdef OPT
    // For measure time
    aInitialize();
#endif    

    if(!filename) return NULL; // -1;
    string fname(filename);
    replace_windows_seperators(fname);

    FILE* fptr = fopen(fname.c_str(), "rb");
    if( !fptr )
    {
        printf("Cant open file %s\n", filename);
        return NULL; //-1;
    }

    // png structures

    png_structp png_ptr;
    png_infop   info_ptr;
    
    png_ptr = png_create_read_struct( PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    if(!png_ptr) return NULL; //-1;

    info_ptr = png_create_info_struct(png_ptr);
    if(!info_ptr) return NULL; //-1;


    // create texture set struct and init basic info
    // name, block directory, block sizes

    MultiLevelTextureSetEX* texset = new MultiLevelTextureSetEX();

    texset->tex        = NULL;
    texset->cols       = NULL;
    texset->rows       = NULL;
    texset->references = 0;
    texset->blksize    = blksize;

    texset->src_name = new char[ strlen(fname.c_str()) + 1];
    strcpy(texset->src_name, fname.c_str());

    // Generate a unique directory name to store the texture blocks generated.
    string basedir(default_block_dir);
    texset->blkdir = generateBlockDirString(basedir, fname);
    string blkdir(texset->blkdir);

    bool hasDir = isTextureBlocksExist(fname.c_str(), blkdir.c_str());

#ifdef OPT
    double time1 = aTime();
#endif

    // init the I/O

    png_init_io(png_ptr, fptr);

    double time2 = aTime();

    // Skip read image data if texture blocks already exist
    if(!hasDir)
    {
        // high level read: read all PNG into memory
        png_read_png( png_ptr, info_ptr,
                      PNG_TRANSFORM_STRIP_16 | PNG_TRANSFORM_PACKING, NULL);    
    }
    else
    {
        // low level, read info up to real image data
        png_read_info( png_ptr, info_ptr );
    }

#ifdef OPT
    double time3 = aTime();
#endif
                                    
    // make sure attributes ok (color mode & bit depth)

    texset->components = 0;
    switch( png_get_color_type(png_ptr,info_ptr))
    {
    case PNG_COLOR_TYPE_GRAY:
        texset->components = 1;
        texset->src_format = GREY;
        break;
    case PNG_COLOR_TYPE_RGB:
        texset->components = 3;
        texset->src_format = RGB;
        break;
    case PNG_COLOR_TYPE_RGB_ALPHA:
        texset->components = 4;
        texset->src_format = RGBA;
        break;
    default:
        break;
    }

    if( !texset->components )
    {
        printf("Unsupported PNG color mode\n");
        png_destroy_read_struct( &png_ptr, &info_ptr, NULL);
        fclose(fptr);
        delete [] texset->blkdir;
        delete [] texset->src_name;
        delete texset;
        return NULL ; //-1;
    }

    if( info_ptr->bit_depth != 8 )
    {
        printf("Unsupported PNG bit depth\n");
        png_destroy_read_struct( &png_ptr, &info_ptr, NULL);
        fclose(fptr);
        delete [] texset->blkdir;
        delete [] texset->src_name;
        delete texset;
        return NULL; //-1;
    }

    // get dimensions and dpi

    texset->src_w = (int) png_get_image_width(png_ptr, info_ptr);
    texset->src_h = (int) png_get_image_height(png_ptr, info_ptr);
    texset->src_dpi_x = ((float) png_get_x_pixels_per_meter(png_ptr,info_ptr)) 
        / 100.0 * 2.54f;
    texset->src_dpi_y = ((float) png_get_y_pixels_per_meter(png_ptr,info_ptr)) 
        / 100.0 * 2.54f;

    // copy pixels
    char* pixels = NULL;

    // Skip load image file if texture blocks already exist
    if(!hasDir)
    {
        // try to get rows    
        png_bytep *b = png_get_rows( png_ptr, info_ptr );

        if(!b)
        {
            printf("Can't get pixels from PNG\n");
            png_destroy_read_struct( &png_ptr, &info_ptr, NULL);
            fclose(fptr);
            delete [] texset->blkdir;
            delete [] texset->src_name;
            delete texset;
            return NULL; //-1;
        }

        pixels = new char[texset->components*texset->src_w*texset->src_h];
        memset(pixels, 0, texset->components*texset->src_w*texset->src_h);

        for( int y = 0; y < texset->src_h; ++y)
        {
            for( int x = 0; x < texset->src_w; ++x)
            {
                for( int c = 0; c < texset->components; ++c)
                {
                    pixels[ y * texset->src_w * texset->components +
                            x * texset->components + c] =
                        (char) b[y][x * texset->components +c];

                }
            }
        }
    }

    png_destroy_read_struct( &png_ptr, &info_ptr, NULL);
    fclose(fptr);

#ifdef OPT
    double time4 = aTime();
#endif    

    // build texblocks and insert tex set
    texset->levels = nlevels;
    build_tex_blocks( pixels, texset, hasDir );

    if(!pixels)
    {
        delete [] pixels;    
    }

    double time5 = aTime();

#ifdef OPT
    printf("[PNG] init\t%f\n",   (time2-time1));
    printf("[PNG] read\t%f\n",   (time3-time2));
    printf("[PNG] fill\t%f\n",   (time4-time3));
    printf("[PNG] TexBlk\t%f\n", (time5-time4));
#endif

    return texset;
    //return insert_texset( texset);
}

//====================================================================
// Load BMP file
//====================================================================
#ifndef _WINGDI_H
typedef unsigned char  BYTE;
typedef unsigned short WORD;
typedef unsigned long  DWORD;
#endif

bool IsBigEndian()
{
    short word = 0x0001;
    if( (*(char*)& word) != 0x01 )
        return true;
    return false;
}

WORD Flip(WORD in)
{
    return( (in >> 8) | (in << 8) );
}

DWORD Flip(DWORD in)
{
    return ( ((in & 0xFF000000) >> 24) | ((in & 0x000000FF) << 24) |
             ((in & 0x00FF0000) >> 8 ) | ((in & 0x0000FF00) << 8 ));
}

struct BMPHeader {
    WORD  sig;
    DWORD filesize;
    DWORD reserved;
    DWORD dataoffset;
};

struct BMPInfoHeader {
	DWORD infoheadersize;
	DWORD bmpwidth;
	DWORD bmpheight;
	WORD  planes;
	WORD  bpp;
	DWORD compression;
	DWORD compressedimgsize;
	DWORD hppm; //horizontal pixels/meter
	DWORD vppm; //vertical pixels/meter
	DWORD numcolorsused;
	DWORD numimportantcolors;
};

MultiLevelTextureSetEX* create_texset_from_bmp(const char* filename, int nlevels, int blksize)
{
#ifdef OPT
    aInitialize();
#endif

    if(!filename) return NULL; //-1;
    string fname(filename);
    replace_windows_seperators(fname);
    FILE* fptr = fopen(fname.c_str(),"rb");
    if(!fptr)
    {
        printf("Can't open file %s\n", filename);
        return NULL; //-1;
    }

#ifdef OPT
    double time1 = aTime();
#endif

    BMPHeader header;
    fread((char*) &(header.sig), sizeof(WORD), 1, fptr);
    if( IsBigEndian() && header.sig != 0x424D )
    {
        printf("BMP file signature wrong!\n");
        fclose(fptr);
        return NULL; //-1;
    }
    else if( !IsBigEndian() && header.sig != 0x4D42 )
    {
        printf("BMP file signature wrong!\n");
        fclose(fptr);
        return NULL; //-1;
    }

    fread(&(header.filesize),   sizeof(DWORD), 1, fptr);
    fread(&(header.reserved),   sizeof(DWORD), 1, fptr);
    fread(&(header.dataoffset), sizeof(DWORD), 1, fptr);

    if( IsBigEndian() )
    {
        header.sig        = Flip(header.sig);
        header.filesize   = Flip(header.filesize);
        header.reserved   = Flip(header.reserved);
        header.dataoffset = Flip(header.dataoffset);
    }

    BMPInfoHeader infoheader;
    fread(&(infoheader.infoheadersize),     sizeof(DWORD), 1, fptr);
    fread(&(infoheader.bmpwidth),           sizeof(DWORD), 1, fptr);
    fread(&(infoheader.bmpheight),          sizeof(DWORD), 1, fptr);
    fread(&(infoheader.planes),             sizeof(WORD),  1, fptr);
    fread(&(infoheader.bpp),                sizeof(WORD),  1, fptr);
    fread(&(infoheader.compression),        sizeof(DWORD), 1, fptr);
    fread(&(infoheader.compressedimgsize),  sizeof(DWORD), 1, fptr);
    fread(&(infoheader.hppm),               sizeof(DWORD), 1, fptr);
    fread(&(infoheader.vppm),               sizeof(DWORD), 1, fptr);
    fread(&(infoheader.numcolorsused),      sizeof(DWORD), 1, fptr);
    fread(&(infoheader.numimportantcolors), sizeof(DWORD), 1, fptr);

    if( IsBigEndian() )
    {
        infoheader.infoheadersize     = Flip( infoheader.infoheadersize );
        infoheader.bmpwidth           = Flip( infoheader.bmpwidth );
        infoheader.bmpheight          = Flip( infoheader.bmpheight );
        infoheader.planes             = Flip( infoheader.planes );
        infoheader.bpp                = Flip( infoheader.bpp );
        infoheader.compression        = Flip( infoheader.compression );
        infoheader.compressedimgsize  = Flip( infoheader.compressedimgsize );
        infoheader.hppm               = Flip( infoheader.hppm );
        infoheader.vppm               = Flip( infoheader.vppm );
        infoheader.numcolorsused      = Flip( infoheader.numcolorsused );
        infoheader.numimportantcolors = Flip( infoheader.numimportantcolors );
    }

    //for now, don't bother supporting RLE compression or less than 24 bpp

    if( infoheader.compression != 0 )
    {
        printf("Not supporting compressed bitmaps for now.\n");
        fclose(fptr);
        return NULL; //-1;
    }

    if( infoheader.bpp != 24 )
    {
        printf("Not supporting BMP files less than true color. %d\n", infoheader.bpp);
        fclose(fptr);
        return NULL; //-1;
    }

    // create texture set struct and init basic info
    // name, block directory, block sizes

    MultiLevelTextureSetEX* texset = new MultiLevelTextureSetEX();

    texset->tex        = NULL;
    texset->cols       = NULL;
    texset->rows       = NULL;
    texset->references = 0;
    texset->blksize    = blksize;

    texset->src_name = new char[ strlen(fname.c_str()) + 1];
    strcpy(texset->src_name, fname.c_str());

    // Generate a unique directory name to store the texture blocks generated.
    string basedir(default_block_dir);    
    texset->blkdir = generateBlockDirString(basedir, fname);
    string blkdir(texset->blkdir);

    // assign attributes

    texset->src_format = RGB;
    texset->components = 3;
    texset->src_dpi_x  = ((float)infoheader.hppm) * 2.54f / 100.0f;
    texset->src_dpi_y  = ((float)infoheader.vppm) * 2.54f / 100.0f;
    texset->src_w      = abs((int) infoheader.bmpwidth);
    texset->src_h      = abs((int) infoheader.bmpheight);

#ifdef OPT
    double time2 = aTime();
#endif

    bool hasDir = isTextureBlocksExist(fname.c_str(), blkdir.c_str());

    // get the pixels

    char* pixels = NULL;

    if(!hasDir)
    {
        pixels = new char[ texset->src_w * texset->src_h * texset->components ];
        memset( pixels,255, texset->src_w * texset->src_h * texset->components);
        char pad;
        int padcount = int( ceil(float( texset->src_w * 3) / 4.0f));
        padcount = (4 * padcount) - (texset->src_w * 3);

        fseek( fptr, header.dataoffset, SEEK_SET);

        // 
        // for( int r = 0; r < texset->src_h; r++) works for certain BMP files, but
        // I can't find them at the moment. The following code works for LacCore images
        //                                                                                 
        for( int r = texset->src_h - 1; r > -1; --r)
        {
            for(int c = 0; c < texset->src_w; c++)
            {
                fread( pixels + r * texset->src_w * 3 + c * 3 + 2, 1, 1, fptr);
                fread( pixels + r * texset->src_w * 3 + c * 3 + 1, 1, 1, fptr);
                fread( pixels + r * texset->src_w * 3 + c * 3    , 1, 1, fptr);
            }

            //bmp is padded to fit on 32 bit boundry
            for(int i = 0; i < padcount; i++)
                fread(&pad,1,1,fptr);
        }    
    }

    fclose(fptr);

    // build texblocks and insert tex set

#ifdef OPT
    double time3 = aTime();
#endif

    texset->levels = nlevels;
    build_tex_blocks( pixels, texset, hasDir );
    delete [] pixels;

#ifdef OPT
    double time4 = aTime();

    printf("[BMP] Header:\t%.3f\n", (time2 - time1));
    printf("[BMP] Pixels:\t%.3f\n", (time3 - time2));
    printf("[BMP] Blocks:\t%.3f\n", (time4 - time3));
#endif

    return texset;
    // return insert_texset( texset);
}

//====================================================================
// Load TIFF file
//====================================================================
MultiLevelTextureSetEX* create_texset_from_tiff(const char* filename, int nlevels, int blksize)
{
#ifdef OPT
    aInitialize();

    double time1 = aTime();
#endif

    if(!filename) return NULL; //-1;

    string fname(filename);
    replace_windows_seperators(fname);

    TIFF* tif;
    tif = TIFFOpen(fname.c_str(),"r");
    if( !tif )
    {
        printf("ERROR: Could not open tiff file %s\n", filename);
        return NULL; //-1;
    }

    MultiLevelTextureSetEX* texset = new MultiLevelTextureSetEX();
    texset->tex        = NULL;
    texset->cols       = NULL;
    texset->rows       = NULL;
    texset->references = 0;
    texset->blksize    = blksize;

    texset->src_name = new char[ strlen(fname.c_str()) + 1];
    strcpy(texset->src_name, fname.c_str());

    // Generate a unique directory name to store the texture blocks generated.
    string basedir(default_block_dir);    
    texset->blkdir = generateBlockDirString(basedir, fname);
    string blkdir(texset->blkdir);

    // get attributes

    uint32* pixels = NULL;
    uint32 w, h;


    // image dimensions

    TIFFGetField(tif,TIFFTAG_IMAGEWIDTH, &w);
    TIFFGetField(tif,TIFFTAG_IMAGELENGTH, &h);

    texset->src_w = (int) w;
    texset->src_h = (int) h;
    texset->components = 4;
    texset->src_format = RGBA;

    bool hasDir = isTextureBlocksExist(fname.c_str(), blkdir.c_str());

#ifdef OPT
    double time2 = aTime();
#endif    

    if(!hasDir)
    {
        // get the pixels

        pixels = (uint32*) _TIFFmalloc( w * h * sizeof(uint32));

        if( !pixels )
        {
            delete texset;
            TIFFClose(tif);
            return NULL; //-1;
        }

        unsigned int status = TIFFReadRGBAImage( tif, w, h, pixels, 0);

        if(status == 0) // false
        {
            TIFFClose(tif);
            _TIFFfree(pixels);
            return NULL; //-1;
        }

        // Take care of big-endian systems, eg. PPC
        if(is_bigendian())
        {
            int row;
            for (row = 0; row < h; ++row)
            {
                // Make sure our channels are in the right order
                uint32 i;
                uint32 rowStart = row * w;
                uint32 rowEnd   = rowStart + w;

                for (i = rowStart; i < rowEnd; i++)
                {
                    pixels[i] = swap_bytes(pixels[i]);
                }
            }
        }

        // Image is actually upside down and is ABGR not RGBA!!!

    #ifdef DEBUG
        printf("Flipping Horizontal!\n");
    #endif

        int halfh = texset->src_h / 2;
        int i, k;
        for( i = 0; i < halfh; ++i)
        {
            for( k = 0; k < texset->src_w; ++k)
            {
                uint32 t;
                t = pixels[ (i * texset->src_w) + k];
                pixels[ (i * texset->src_w) + k ] =
                    pixels[ ((texset->src_h - i - 1) * texset->src_w) + k];
                pixels[ ((texset->src_h - i - 1) * texset->src_w) + k] = t;

            }
        }

    } // !hasDir

#ifdef OPT
    double time3 = aTime();
#endif    

/*
#ifdef DEBUG
    printf("Converting pixels from ABGR to RGBA\n");
#endif
	
    for( i = 0; i < texset->src_h; ++i)
    {
        for( k = 0; k < texset->src_w; ++k)
        {
            char* channels = (char*) &(pixels[ (i * texset->src_w) + k]);
            char t = channels[0];
            channels[0] = channels[3];
            channels[3] = t;
            t = channels[1];
            channels[1] = channels[2];
            channels[2] = t;

        }
    }
*/

    // image resolution
    //uint32 frac_comp[2];
	float frac_comp;
    uint16 units;

    TIFFGetField(tif,TIFFTAG_RESOLUTIONUNIT, &units);
    
	/*
	TIFFGetField(tif,TIFFTAG_XRESOLUTION, &(frac_comp[0]));
	
	
    if( frac_comp[1] )
    {
        texset->src_dpi_x = float(frac_comp[0]) / float(frac_comp[1]);
        if( units == RESUNIT_CENTIMETER) // DPCM --> DPI
            texset->src_dpi_x *= 2.54f;
    }
    else
    {
        texset->src_dpi_x = 150;
    }

    TIFFGetField(tif,TIFFTAG_YRESOLUTION, &(frac_comp[0]));

    if( frac_comp[1] )
    {
        texset->src_dpi_y = float(frac_comp[0]) / float(frac_comp[1]);
        if( units == RESUNIT_CENTIMETER) // DPCM --> DPI
            texset->src_dpi_y *= 2.54f;
    }
    else
    {
        texset->src_dpi_y = 150;
    }    
	*/

	// get x resolution
	TIFFGetField(tif,TIFFTAG_XRESOLUTION, &frac_comp);
	texset->src_dpi_x = frac_comp;
	// get y resolution
	TIFFGetField(tif,TIFFTAG_YRESOLUTION, &frac_comp);
	texset->src_dpi_y = frac_comp;
	// check unit
	if( units == RESUNIT_CENTIMETER) // DPCM --> DPI
	{
		texset->src_dpi_x *= 2.54f;
		texset->src_dpi_y *= 2.54f;
	}
	
    TIFFClose(tif);

#ifdef OPT
    double time4 = aTime();
#endif

    // build texblocks and insert tex set

    texset->levels = nlevels;
    build_tex_blocks( (char*) pixels, texset, hasDir );

#ifdef OPT
    double time5 = aTime();
#endif

    if(pixels != NULL)
    {
        _TIFFfree(pixels);
    }

#ifdef DEBUG
    printf("Image DPI %f x %f\n", texset->src_dpi_x, texset->src_dpi_y);
#endif

#ifdef OPT
    double time6 = aTime();

    printf("[TIF] Header:\t%.3f\n", (time2-time1));
    printf("[TIF] Pixels:\t%.3f\n", (time3-time2));
    printf("[TIF] DPI:\t%.3f\n", (time4-time3));
    printf("[TIF] Block:\t%.3f\n", (time5-time4));
    printf("[TIF] TIFree:\t%.3f\n", (time6-time5));
#endif

    return texset;
    // return insert_texset( texset);
}

// TODO
MultiLevelTextureSetEX* create_texset_from_jp2k(const char* filename, int nlevels, int blksize)
{
    printf("Processing '%s'\n", filename);

    // FIXME
    aInitialize();

    if(!filename) return NULL;

    string fname(filename);
    replace_windows_seperators(fname);

    // FIXME
    double time1 = aTime();

    // JPEG2000 stuff
    opj_dparameters_t parameters;
    opj_event_mgr_t   event_mgr;
    opj_image_t *     image = NULL;

    opj_dinfo_t*      dinfo = NULL;
    opj_cio_t*        cio = NULL;

    // configure the event callbacks (not required)
    memset(&event_mgr, 0, sizeof(opj_event_mgr_t));
    event_mgr.error_handler = error_callback;
    event_mgr.warning_handler = warning_callback;
    event_mgr.info_handler = info_callback;

    /* set decoding parameters to default values */
    opj_set_default_decoder_parameters(&parameters);

    // get file parameters
    parameters.decod_format = get_file_format((char*) filename);
    if(parameters.decod_format == -1)
    {
        printf("decode_format is -1, bail.\n");
        return NULL;
    }

    // FIXME
    double time2 = aTime();
    printf("[J2P] Init:\t%.3f\n", (time2 - time1));

    // Estimate file size and read it to memory
    int file_length;
    unsigned char * src = NULL;
    FILE* fsrc = fopen(fname.c_str(), "rb");

    if(fsrc == NULL)
    {
        printf("Cannot open %s\n", filename);
        free(src);
        return NULL;
    }

    // create texture set struct and init basic info
    // name, block directory, block sizes
    MultiLevelTextureSetEX* texset = new MultiLevelTextureSetEX();

    texset->tex        = NULL;
    texset->cols       = NULL;
    texset->rows       = NULL;
    texset->references = 0;
    texset->blksize    = blksize;

    texset->src_name = new char[ strlen(fname.c_str()) + 1];
    strcpy(texset->src_name, fname.c_str());

    // Generate a unique directory name to store the texture blocks generated.
    string basedir(default_block_dir);
    texset->blkdir = generateBlockDirString(basedir, fname);
    string blkdir(texset->blkdir);

    bool hasDir = isTextureBlocksExist(fname.c_str(), blkdir.c_str());

    // FIXME
    double time3 = aTime();
    printf("[J2P] DS init: %.3f\n", (time3 - time2));    

    // get file size
    fseek(fsrc, 0, SEEK_END);
    file_length = ftell(fsrc);
    fseek(fsrc, 0, SEEK_SET);
    src = (unsigned char *) malloc(file_length);
    fread(src, 1, file_length, fsrc);
    fclose(fsrc);

    // FIXME
    double time4 = aTime();
    printf("[J2P] Est file length:\t%.3f\n", (time4 - time3));
    printf("Estimate file_length = %d\n", file_length);
    
    /* decode the code-stream */
    /* ---------------------- */
    printf("File in format: %d\n", parameters.decod_format);

    switch(parameters.decod_format) {
	case J2K_CFMT:
	    {
		/* JPEG-2000 codestream */
	    // FIXME
	    printf("[J2P] J2K_CFMT!\n");

		/* get a decoder handle */
		dinfo = opj_create_decompress(CODEC_J2K);

		/* catch events using our callbacks and give a local context */
		opj_set_event_mgr((opj_common_ptr)dinfo, &event_mgr, stderr);

		/* setup the decoder decoding parameters using user parameters */
		opj_setup_decoder(dinfo, &parameters);

		/* open a byte stream */
		cio = opj_cio_open((opj_common_ptr)dinfo, src, file_length);

		/* decode the stream and fill the image structure */
		image = opj_decode(dinfo, cio);
		if(!image) {
		    fprintf(stderr, "ERROR -> j2k_to_image: failed to decode image!\n");
		    opj_destroy_decompress(dinfo);
		    opj_cio_close(cio);
		    return NULL;
		}

		pokeOpjImg(image);
		//------------------------------------
	    texset->components = image->numcomps;
	    switch(image->color_space)
	    {
            case CLRSPC_SRGB:
                texset->src_format = RGB; break;

            case CLRSPC_GRAY:
                texset->src_format = GREY; break;            
        }

        texset->src_w = abs(image->x0 - image->x1);
        texset->src_h = abs(image->y0 - image->y1);

		//------------------------------------

		/* close the byte stream */
		opj_cio_close(cio);
	    }
	    break;

	case JP2_CFMT:
	    {
		/* JPEG 2000 compressed image data */
	    // FIXME
	    printf("[J2P] JP2_CFMT!\n");

		/* get a decoder handle */
		dinfo = opj_create_decompress(CODEC_JP2);

		/* catch events using our callbacks and give a local context */
		opj_set_event_mgr((opj_common_ptr)dinfo, &event_mgr, stderr);

		/* setup the decoder decoding parameters using the current image and user parameters */
		opj_setup_decoder(dinfo, &parameters);

		/* open a byte stream */
		cio = opj_cio_open((opj_common_ptr)dinfo, src, file_length);

		/* decode the stream and fill the image structure */
		// FIXME
		double b4De = aTime();

		image = opj_decode(dinfo, cio);
		if(!image) {
		    fprintf(stderr, "ERROR -> j2k_to_image: failed to decode image!\n");
		    opj_destroy_decompress(dinfo);
		    opj_cio_close(cio);
		    return NULL;
		}

        // FIXME
        double afDe = aTime();
        printf("[JP2] decode:\t%.3f\n", (afDe - b4De));

		//------------------------------------
        pokeOpjImg(image);

	    texset->components = image->numcomps;
	    switch(image->color_space)
	    {
            case CLRSPC_SRGB:
                texset->src_format = RGB; break;

            case CLRSPC_GRAY:
                texset->src_format = GREY; break;
        }

        texset->src_w = abs(image->x0 - image->x1);
        texset->src_h = abs(image->y0 - image->y1);

        texset->src_dpi_x = 254.0f; // fixme
        texset->src_dpi_y = 254.0f; // fixme

        texset->levels = nlevels;

        printf("Before building texture\n");

        char* pixels;
        pixels = (char *)image->comps[0].data;

        build_tex_blocks(pixels, texset);

        printf("After building texture\n");

		//------------------------------------

		/* close the byte stream */
		opj_cio_close(cio);

	    }
	    break;

	case JPT_CFMT:
	    {
		/* JPEG 2000, JPIP */
	    // FIXME
	    printf("[JPT_CFMT] J2K_CFMT!\n");

		/* get a decoder handle */
		dinfo = opj_create_decompress(CODEC_JPT);

		/* catch events using our callbacks and give a local context */
		opj_set_event_mgr((opj_common_ptr)dinfo, &event_mgr, stderr);

		/* setup the decoder decoding parameters using user parameters */
		opj_setup_decoder(dinfo, &parameters);

		/* open a byte stream */
		cio = opj_cio_open((opj_common_ptr)dinfo, src, file_length);

		/* decode the stream and fill the image structure */
		image = opj_decode(dinfo, cio);
		if(!image) {
		    fprintf(stderr, "ERROR -> j2k_to_image: failed to decode image!\n");
		    opj_destroy_decompress(dinfo);
		    opj_cio_close(cio);
		    return NULL;
		}

		pokeOpjImg(image);

		/* close the byte stream */
		opj_cio_close(cio);
	    }
	    break;

	default:
	    fprintf(stderr, "skipping file..\n");
    }

    // FIXME
    double time5 = aTime();
    printf("[J2P] Switch:\t%.3f\n", (time5 - time4));

    /* free the memory containing the code-stream */
    free(src);
    src = NULL;

    printf("[J2P] End of do JPEG2000\n");

    return texset;
    // return NULL;
}

//====================================================================
void free_texset(int set, bool del_disk_blocks)
{
    if(!is_texset(set)) return;
    MultiLevelTextureSetEX* t = texsetvec[set];
    texsetvec[set] = NULL;
    
    // delete texBlocks, and texture objects if they exist
#ifdef DEBUG
    printf("deleting texBlocks\n");
#endif

//    glBindTexture(GL_TEXTURE_2D, 0);

    if( t->tex && t->cols && t->rows)
    {
        for( int l = 0; l < t->levels; ++l)
        {
            if( !t->tex[l] ) continue;
            
            for( int r = 0; r < t->rows[l]; ++r)
            {
                for( int c = 0; c < t->cols[l]; ++c)
                {
                    int index = (r * t->cols[l]) + c;
                    printf("level %d, row %d, col %d, index %d\n", l, r, c,
                           index);

                    if( t->tex[l][index].texData)
                        delete [] t->tex[l][index].texData;
                    if( t->tex[l][index].blockfile)
                        delete [] t->tex[l][index].blockfile;

                    if( t->tex[l][index].texId && 
                        glIsTexture(t->tex[l][index].texId) )
                    {
#ifdef DEBUG
                        printf("Deleting GL texture object\n");
#endif
                        glDeleteTextures(1,&(t->tex[l][index].texId));
                    }               
                }
            }

#ifdef DEBUG
            printf("Deleting row\n");
#endif
            if( t->tex[l] )
                delete [] t->tex[l];
        }

#ifdef DEBUG
        printf("Deleting t->tex, t->rows, t->cols\n");
#endif
        delete [] t->tex;
        delete [] t->rows;
        delete [] t->cols;
    }

#ifdef DEBUG
    printf("Deleting strings and scales\n");
#endif

    if( t->src_name ) delete [] t->src_name;
    if( t->scales   ) delete [] t->scales;
    if( t->blkdir   ) delete [] t->blkdir;

#ifdef DEBUG
    printf("Deleting object\n");
#endif

    delete t;
    
    
}

//=========================================================================
void free_all_texsets( bool del_disk_blocks)
{
    for( int i = 0; i < texsetvec.size(); ++i)
    {
#ifdef DEBUG
        printf("freeing texset %d\n", i);
#endif
    free_texset( i, del_disk_blocks);
    }

    texsetvec.clear();
}

//=========================================================================
bool is_texset( int s )
{
    if( s < 0 || s >= texsetvec.size() ) return false;
    return (texsetvec[s] != NULL);
}

//========================================================================
void set_texset_url(int s, char* url)
{
    if(!is_texset(s) || !url) return;
    if( texsetvec[s]->src_url)
        delete [] texsetvec[s]->src_url;
    texsetvec[s]->src_url = new char[ strlen(url) + 1];
    strcpy(texsetvec[s]->src_url, url);
}

//========================================================================
void inc_texset_ref_count( int s)
{
    if(!is_texset(s)) return;
    texsetvec[s]->references += 1;
}

//========================================================================
void dec_texset_ref_count( int s)
{
    if(!is_texset(s)) return;
    texsetvec[s]->references -= 1;
}

//========================================================================
int get_texset_ref_count(int s)
{
    if(!is_texset(s)) return -1;
    return texsetvec[s]->references;
}

//========================================================================
int get_texset_num_components(int s)
{
    if(!is_texset(s)) return -1;
    return texsetvec[s]->components;
}

//========================================================================
int get_texset_pixel_format(int s)
{
    if(!is_texset(s)) return -1;
    return texsetvec[s]->src_format;
}

//========================================================================
int get_texset_num_levels(int s)
{
    if(!is_texset(s)) return -1;
    return texsetvec[s]->levels;
}

//========================================================================
int get_texset_src_width(int s)
{
    if(!is_texset(s)) return -1;
    return texsetvec[s]->src_w;
}

//========================================================================
int get_texset_src_height(int s)
{
    if(!is_texset(s)) return -1;
    return texsetvec[s]->src_h;
}

//========================================================================
int get_texset_block_size(int s)
{
    if(!is_texset(s)) return -1;
    return texsetvec[s]->blksize;
}

//========================================================================
float get_texset_src_dpi_x(int s)
{
    if(!is_texset(s)) return -1;
    return texsetvec[s]->src_dpi_x;
}

//========================================================================
float get_texset_src_dpi_y(int s)
{
    if(!is_texset(s)) return -1;
    return texsetvec[s]->src_dpi_y;
}

//========================================================================
int get_texset_num_cols(int s, int level)
{
    if(!is_texset(s)) return -1;
    if( level < 0 || level >= texsetvec[s]->levels) return -1;
    return texsetvec[s]->cols[level];
}
//========================================================================
int get_texset_num_rows(int s, int level)
{
    if(!is_texset(s)) return -1;
    if( level < 0 || level >= texsetvec[s]->levels) return -1;
    return texsetvec[s]->rows[level];
}

//========================================================================
int get_texset_level_in_pyramid(int s, int level)
{
    if(!is_texset(s)) return -1;
    if( level < 0 || level >= texsetvec[s]->levels) return -1;
    return texsetvec[s]->pyramid_level[level];
}

//========================================================================
float get_texset_scale(int s, int level)
{
    if(!is_texset(s)) return -1;
    if( level < 0 || level >= texsetvec[s]->levels) return -1;
    return texsetvec[s]->scales[level];
}

//========================================================================
char* get_texset_name(int s)
{
    if(!is_texset(s)) return NULL;
    return texsetvec[s]->src_name;
}

//========================================================================
char* get_texset_url(int s)
{
    if(!is_texset(s)) return NULL;
    return texsetvec[s]->src_url;
}

//========================================================================
texBlock* get_tex_block(int set, int level, int col, int row)
{
    if(!is_texset(set)) return NULL;
    MultiLevelTextureSetEX* t = texsetvec[set];
    if( level < 0 || level >= t->levels) return NULL;
    if( col < 0 || col >= t->cols[level]) return NULL;
    if( row < 0 || row >= t->rows[level]) return NULL;

    return &( t->tex[level][( row * t->cols[level]) + col]);
}

//=======================================================================
void bind_texblock( int set, int level, int col, int row)
{

#ifdef DEBUG
    printf("Trying to bind texblock in set %d, level %d, col %d, row %d\n",
        set,level,col,row);
#endif

    if(!is_texset(set)) return;

    MultiLevelTextureSetEX* t = texsetvec[set];

    if( level < 0 || level >= t->levels) return;
    if( col < 0 || col >= t->cols[level]) return;
    if( row < 0 || row >= t->rows[level]) return;
    
    int id = (row * t->cols[level]) + col;

    if( !is_tex_cache_entry( &(t->tex[level][id])) )
    {
#ifdef DEBUG
        printf("Tex Cache Miss\n");
#endif

        // TODO Spawn a thread to do IOs?
        tex_cache_miss(set,level,col,row);
    }

#ifdef DEBUG
    printf("Binding texture object %d\n", t->tex[level][id].texId);
#endif

    glBindTexture(GL_TEXTURE_2D, t->tex[level][id].texId);
}
