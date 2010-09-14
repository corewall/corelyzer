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
#endif

#include <stdio.h>
#include <stdlib.h>

#ifndef __APPLE__
#include <GL/gl.h>
#include <GL/glu.h>
#else
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#endif

#include <png.h>

void read_png(const char* filename, int* w, int* h, GLenum* format, 
              char*& pixels)
{
    if(!w || !h || !format || !filename) return;
    FILE* fptr = fopen(filename,"rb");
    if(!fptr)
    {
        printf("READ PNG: Can't open file %s\n", filename);
        return;
    }

    // png structures

    png_structp png_ptr;
    png_infop   info_ptr;
    
    png_ptr = png_create_read_struct( PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    if( !png_ptr ) return;

    info_ptr = png_create_info_struct( png_ptr );
    if( !info_ptr ) return;

    png_init_io(png_ptr, fptr);
    png_read_png( png_ptr, info_ptr,
                  PNG_TRANSFORM_STRIP_16 | PNG_TRANSFORM_PACKING, NULL);

    int components = 0;

    switch( png_get_color_type(png_ptr,info_ptr))
    {
    case PNG_COLOR_TYPE_GRAY:
        *format = GL_LUMINANCE;
        components = 1;
        break;
    case PNG_COLOR_TYPE_RGB:
        *format = GL_RGB;
        components = 3;
        break;
    case PNG_COLOR_TYPE_RGB_ALPHA:
        *format = GL_RGBA;
        components = 4;
        break;
    default:
        break;
    }

    if( !components )
    {
        printf("Unsupported PNG color mode\n");
        png_destroy_read_struct( &png_ptr, &info_ptr, NULL);
        fclose(fptr);
        return;
    }

    if( info_ptr->bit_depth != 8 )
    {
        printf("Unsupported PNG bit depth\n");
        png_destroy_read_struct( &png_ptr, &info_ptr, NULL);
        fclose(fptr);
        return;
    }

    // try to get rows

    png_bytep *b = png_get_rows( png_ptr, info_ptr );

    if( !b)
    {
        printf("Can't get pixels from PNG\n");
        png_destroy_read_struct( &png_ptr, &info_ptr, NULL);
        fclose(fptr);
        return;
    }

    *w = (int) png_get_image_width( png_ptr, info_ptr);
    *h = (int) png_get_image_height( png_ptr, info_ptr);

    pixels = new char[ (*w) * (*h) * components ];
    memset( pixels, 0, (*w) * (*h) * components );

    for( int y = 0; y < *h; ++y)
        for( int x = 0; x < *w; ++x)
            for( int c = 0; c < components; ++c)
            {
                pixels[ y * (*w) * components +  x * components + c] = 
                    (char) b[y][x * components + c];

            }

    png_destroy_read_struct( &png_ptr, &info_ptr, NULL);
    fclose(fptr);

    // done
}
