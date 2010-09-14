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

#include <ft2build.h>
#include FT_FREETYPE_H
#include <vector>
#include <queue>

#include <stdio.h>
#include <stdlib.h>

#ifndef __APPLE__
#include <GL/gl.h>
#include <GL/glu.h>
#else
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#endif

#include "fontsys.h"
#include <freetype/ftglyph.h>

#define CHAR_HEIGHT       48
#define CHAR_FIXED_WIDTH  32
#define LINE_VERT_ADVANCE 52
#define ASCII_COUNT       256

struct CWFont {
    char* name;
    char* filename;
    GLuint listBase;
    GLuint numLists;
    GLuint *texids;
    unsigned int advanceX[ ASCII_COUNT ]; //to advance before the next char
    GLuint glyphMap[ ASCII_COUNT ]; //fast map of ascii chars to Display List 
};

#ifndef GL_CLAMP_TO_EDGE
#define GL_CLAMP_TO_EDGE 0x812F
#endif

std::vector< CWFont* > fontvec;
std::queue< int >      fontloadqueue;
bool                   initialized_sys = false;
FT_Library             fontlibrary;
GLuint                 unknown_char;
int                    current_font = -1;

//=======================================================================
int next_p2(int i)
{
	int val = 1;
	while( val < i ) val *= 2;
	return val;
}

//=======================================================================
void init_font_sys()
{
    unknown_char = glGenLists( 1 );
    glNewList( unknown_char, GL_COMPILE );
    glBindTexture(GL_TEXTURE_2D, 0);
    glBegin(GL_LINE_STRIP);
        glVertex2f(3,3);
        glVertex2f(3,45);
        glVertex2f(29,45);
        glVertex2f(29,3);
        glVertex2f(3,3);
    glEnd();
    glTranslatef(32,0,0);
    glEndList();

    initialized_sys = true;
}

//=======================================================================
void load_font(int font)
{
    if(!initialized_sys) init_font_sys();
    if(!is_font(font)) return;

    CWFont *cwf = fontvec[font];

    FT_Face  face;
    FT_Error err;
    err = FT_New_Face( fontlibrary, cwf->filename, 0, &face);

    if(err)
    {
        printf("Failed to load font file %s\n", cwf->filename);
        return;
    }

    err = FT_Set_Pixel_Sizes( face, 32,48);
    if(err)
    {
        printf("Failed to set face dimensions to 32x48\n");
        return;
    }

    std::vector< int > supported_chars;
    
    int i;
    for( i = 0; i < ASCII_COUNT; ++i)
    {
        int glyph_index = FT_Get_Char_Index(face,i);
        if( glyph_index)
            supported_chars.push_back(i);
    }

    cwf->numLists = supported_chars.size();
    cwf->listBase = glGenLists( cwf->numLists );
    cwf->texids = new GLuint[ cwf->numLists ];
    glEnable(GL_TEXTURE_2D);
    glGenTextures( cwf->numLists, cwf->texids );

    int list_index = 0;
    for( i = 0; i < ASCII_COUNT; ++i)
    {
        cwf->glyphMap[i] = unknown_char;
        cwf->advanceX[i] = 32;
        
        if(supported_chars[list_index] != i)
        {
            continue;
        }

        if( FT_Load_Glyph(face, FT_Get_Char_Index(face,i), FT_LOAD_DEFAULT) )
        {
            list_index++;
            continue;
        }

        FT_Glyph glyph;
        if( FT_Get_Glyph( face->glyph, &glyph ) )
        {
            list_index++;
            continue;
        }

        FT_Glyph_To_Bitmap( &glyph, FT_RENDER_MODE_NORMAL, 0, 1);
        FT_BitmapGlyph bmp_glyph = (FT_BitmapGlyph) glyph;
        FT_Bitmap& bmp = bmp_glyph->bitmap;

        // create texture
        int w, h;
        w = bmp.width;
        h = bmp.rows;
        w = next_p2(w);
        h = next_p2(h);

        GLubyte *data;
        data = new GLubyte[w * h];
        for( int j = 0; j < h; j++)
        {
            for(int k = 0; k < w; k++)
            {
                if( k < bmp.width && j < bmp.rows )
                    data[k + j * w] = bmp.buffer[ k + j * bmp.width];
                else
                    data[k + j * w] = 0;
            }
        }

        glBindTexture(GL_TEXTURE_2D, cwf->texids[list_index]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, w, h, 0, GL_ALPHA,
                     GL_UNSIGNED_BYTE, data);

        delete [] data;

        // create display list
        float x, y;
        x = float(bmp.width) / float(w);
        y = float(bmp.rows) / float(h);
        cwf->glyphMap[i] = cwf->listBase + list_index;
        cwf->advanceX[i] = (unsigned int)
            (((float)face->glyph->advance.x) / 64.0f);

        glNewList( cwf->glyphMap[i], GL_COMPILE);
        glBindTexture(GL_TEXTURE_2D, cwf->texids[list_index]);
        glTranslatef(bmp_glyph->left, bmp_glyph->top - bmp.rows, 0);
        glBegin(GL_QUADS);
        {
            glTexCoord2d(0,0); glVertex2f(0,bmp.rows);
            glTexCoord2d(0,y); glVertex2f(0,0);
            glTexCoord2d(x,y); glVertex2f(bmp.width, 0);
            glTexCoord2d(x,0); glVertex2f(bmp.width, bmp.rows);
        }
        glEnd();
        glTranslatef(cwf->advanceX[i] - bmp_glyph->left,
                     bmp.rows - bmp_glyph->top, 0);
        glEndList();

        list_index++;

        FT_Done_Glyph(glyph);
    }
}

//=======================================================================
int queue_font_to_load(const char* ttfname)
{
    CWFont* cwf = new CWFont();
    cwf->filename = new char[strlen(ttfname) +1];
    strcpy(cwf->filename,ttfname);
    cwf->name = new char[strlen(ttfname) +1];
    strcpy(cwf->name,ttfname);
    fontvec.push_back(cwf);
    fontloadqueue.push(fontvec.size() -1);
    return fontvec.size() - 1;
}

//=======================================================================
void process_font_load_queue()
{
    FT_Init_FreeType( &fontlibrary );

    while( fontloadqueue.size() > 0)
    {
        load_font( fontloadqueue.front() );
	    fontloadqueue.pop();
    }

    FT_Done_FreeType( fontlibrary );
}

//=======================================================================
void set_current_font(int font)
{
    if( !is_font(font)) return;
    current_font = font;
}

//=======================================================================
bool is_font(int font)
{
    if( font < 0 || font > fontvec.size() - 1) return false;
    return (fontvec[font] != NULL);
}

//=======================================================================
int get_num_fonts()
{
    return fontvec.size();
}

//=======================================================================
int get_current_font()
{
    return current_font;
}

//=======================================================================
const char* get_font_name()
{
    if(!is_font(current_font)) return NULL;
    return fontvec[current_font]->name;
}

//=======================================================================
int get_char_escapement(char c)
{
    if(!is_font(current_font)) return 0;
    return fontvec[current_font]->advanceX[c];
}

//=======================================================================
void render_string(const char* str, int start, int end)
{

    // make sure we have loaded all the fonts we should have by now
    //printf("processing font system\n");
    process_font_load_queue();
    //printf("queued fonts processed\n");
    // continue on
    if(!is_font(current_font)) { return; }
    CWFont* cwf = fontvec[current_font];

    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glPushMatrix();
    {
        glScalef(1,-1,1);
        for(int i = start; i <= end; i++)
        {
            glCallLists( 1, GL_UNSIGNED_INT, &(cwf->glyphMap[str[i]]) );
        }
    }
    glPopMatrix();
    glDisable(GL_BLEND);
}

//=======================================================================
void render_string_label(const char* str, int start, int end)
{

    // make sure we have loaded all the fonts we should have by now
    //printf("processing font system\n");
    process_font_load_queue();
    //printf("queued fonts processed\n");
    // continue on
    if(!is_font(current_font)) { return; }
    CWFont* cwf = fontvec[current_font];
	
	// draw backgrond label: dark blue
	glColor4f(0,0,0.3, 0.5);
	// calculate size of quad
	float w = 0;;
	for (int i=start; i<=end; i++) {
		w += cwf->advanceX[str[i]];
	}
	w = w +6;
	float h = -48 +6;
	glBegin(GL_QUADS);
	{
		glVertex2f(-6, 6);	// left upper
		glVertex2f(-6, h);	// left lower
		glVertex2f(w, h);	// right lower
		glVertex2f(w, 6);	// right upper
	}
	glEnd();
	
	// draw string: white
	glColor3f(1,1,1);	
    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glPushMatrix();
    {
        glScalef(1,-1,1);
        for(int i = start; i <= end; i++)
        {
            glCallLists( 1, GL_UNSIGNED_INT, &(cwf->glyphMap[str[i]]) );
        }
    }
    glPopMatrix();
    glDisable(GL_BLEND);
}

//=======================================================================
void render_string_shadowed(const char* str, int start, int end, 
							float* color, float offset)
{

    // make sure we have loaded all the fonts we should have by now
    //printf("processing font system\n");
    process_font_load_queue();
    //printf("queued fonts processed\n");
    // continue on
    if(!is_font(current_font)) { return; }
    CWFont* cwf = fontvec[current_font];
	
	// draw backgrond shadow: almost black
	glEnable(GL_TEXTURE_2D);
	glEnable(GL_BLEND);
	glColor3f(0,0,0.1);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glPushMatrix();
    glTranslatef(offset,offset,0);
	{
        glScalef(1,-1,1);
        for(int i = start; i <= end; i++)
        {
            glCallLists( 1, GL_UNSIGNED_INT, &(cwf->glyphMap[str[i]]) );
        }
    }
	glPopMatrix();

	// draw string: white
	if (color != NULL)
		glColor3f(color[0], color[1], color[2]);
	else
		glColor3f(1,1,1);	

    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glPushMatrix();
    {
        glScalef(1,-1,1);
        for(int i = start; i <= end; i++)
        {
            glCallLists( 1, GL_UNSIGNED_INT, &(cwf->glyphMap[str[i]]) );
        }
    }
    glPopMatrix();
    glDisable(GL_BLEND);
}

//=======================================================================
void render_string_outlined(const char* str, int start, int end)
{
	// this is a bit expensive font drawing
	// Since we need to draw strings five times.

    // make sure we have loaded all the fonts we should have by now
    //printf("processing font system\n");
    process_font_load_queue();
    //printf("queued fonts processed\n");
    // continue on
    if(!is_font(current_font)) { return; }
    CWFont* cwf = fontvec[current_font];
	
	// draw outline: almost black. draw it with four directional offset
	glEnable(GL_TEXTURE_2D);
	glEnable(GL_BLEND);
	glColor3f(0,0,0.1);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glPushMatrix();
    glTranslatef(2,2,0);
	{
        glScalef(1,-1,1);
        for(int i = start; i <= end; i++)
        {
            glCallLists( 1, GL_UNSIGNED_INT, &(cwf->glyphMap[str[i]]) );
        }
    }
	glPopMatrix();
	glPushMatrix();
    glTranslatef(-2,-2,0);
	{
        glScalef(1,-1,1);
        for(int i = start; i <= end; i++)
        {
            glCallLists( 1, GL_UNSIGNED_INT, &(cwf->glyphMap[str[i]]) );
        }
    }
	glPopMatrix();
/*	glPushMatrix();
    glTranslatef(0,2,0);
	{
        glScalef(1,-1,1);
        for(int i = start; i <= end; i++)
        {
            glCallLists( 1, GL_UNSIGNED_INT, &(cwf->glyphMap[str[i]]) );
        }
    }
	glPopMatrix();
	glPushMatrix();
    glTranslatef(0,-2,0);
	{
        glScalef(1,-1,1);
        for(int i = start; i <= end; i++)
        {
            glCallLists( 1, GL_UNSIGNED_INT, &(cwf->glyphMap[str[i]]) );
        }
    }
	glPopMatrix();
*/
	// draw string: white
	glColor3f(1,1,1);	
    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glPushMatrix();
    {
        glScalef(1,-1,1);
        for(int i = start; i <= end; i++)
        {
            glCallLists( 1, GL_UNSIGNED_INT, &(cwf->glyphMap[str[i]]) );
        }
    }
    glPopMatrix();
    glDisable(GL_BLEND);
}