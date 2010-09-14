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

#ifndef CORELYZER_GL_CANVAS
#define CORELYZER_GL_CANVAS

#if defined(WIN32) || defined(_WIN32)
#include <windows.h>
#endif

#include <stdio.h>
#include <stdlib.h>


#ifndef __APPLE__
#include <GL/gl.h>
#include <GL/glu.h>
#include <GL/glext.h>

#if defined(WIN32) || defined(_WIN32)
//#include <GL/wgl.h>
#include <GL/glext.h>
#define glGetProcAddress( x ) wglGetProcAddress((const char*) (x))
#else
#include <GL/glx.h>
#include <GL/glxext.h>
#define glGetProcAddress( x ) (*glXGetProcAddressARB)((const GLubyte*) (x))
#endif

#else
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#endif

#include "camera.h"

#define CM_PER_INCH           2.54

#define GRID_BASIC		0
#define GRID_HORIZON	1
#define GRID_VERTICAL	2
#define GRID_POINT		3
#define GRID_CPOINT		4

// Measurement modes, for c->nummeasurepoint
#define MEASURE_NOT         -1
#define MEASURE_READY       0
#define MEASURE_ONE_POINT   1
#define MEASURE_TWO_POINT   2

// Clast operation modes
#define CLAST_INIT          0
#define CLAST_HAS_1ST_POINT 1
#define CLAST_RECT_DEFINED  2

// Canvas Modes
#define CANVAS_NORMAL       0
#define CANVAS_MEASURE      1
#define CANVAS_MARKER       2
#define CANVAS_CLAST        3
#define CANVAS_CUT          4

typedef struct Point_s {
    float x;
    float y;
} Point;

typedef struct Canvas_s {
    int    camera;
    float  x;
    float  y;
    float  w;  // current height
    float  h;  // current width
    float  w0; // original width
    float  h0; // original height
    float  sx;
    float  sy;
    float  coverage_x;
    float  coverage_y;
    float  half_coverage_x;
    float  half_coverage_y;
    float  dpi_x;
    float  dpi_y;
    int    tick_threshold;
    bool   valid;
    bool   horizontal;

    // bool   depth_scale;
    bool   bottomRow;
    bool   firstColumn;

    bool   cross_core_scale;
    double projMatrix[16];

	float  mouseX;
	float  mouseY;

	int    nummeasurepoint;
	float  measurepoint[4];
	float  mode;

    bool   grid;
	int    grid_type;
	int    grid_thickness;
	float  grid_size;
	float  r;		// grid line color
    float  g;
    float  b;

	int    clastMode;
    Point  clastPoint1;
    Point  clastPoint2;
} Canvas;

int   create_canvas      ( float x, float y, float w, float h, float dpix, 
                           float dpiy);
void  free_canvas        ( int canvas);
void  free_all_canvas	 ();
void  render_canvas      ( int id);
bool  is_canvas          ( int canvas);
int   num_canvases       ();
int   get_current_canvas ();

void  get_canvas_position    ( int id, float* x, float *y);
void  get_canvas_dimensions  ( int id, float* w, float* h);
void  get_canvas_dpi         ( int id, float* dpix, float* dpiy);
int   get_canvas_camera      ( int id );
char  get_canvas_flags       ( int id );
void  get_canvas_scale       ( int id, float* sx, float* sy);
float get_canvas_width       ( int id );
float get_canvas_orig_width  ( int id );
float get_canvas_height      ( int id );
float get_canvas_orig_height ( int id );

void  set_canvas_position    ( int id, float x, float y);
void  set_canvas_dimensions  ( int id, float w, float h);
void  set_canvas_dpi         ( int id, float dpix, float dpiy);
void  set_canvas_scale       ( int id, float sx, float sy);
void  set_canvas_mouse( int id, float mx, float my);
float get_canvas_mouseX(int id);
float get_canvas_mouseY(int id);
int   add_canvas_measurepoint( int id, float x, float y);
void  set_canvas_measurepoint( int id, float x1, float y1, float x2, float y2);
void  set_canvas_measurepointnumber(int id, int num);
void  set_canvas_mode(int id, int imode);

void  orient_canvas_vertically   (int id);
void  orient_canvas_horizontally (int id);

void  set_canvas_bottom_row(int id, bool flag);
void  set_canvas_first_column(int id, bool flag);

void  set_canvas_draw_cross_core_scale (int id, bool flag);

void  set_canvas_draw_grid (int id, bool flag);
void  set_canvas_grid_color (int id, float r, float g, float b);
void  set_canvas_grid_size (int id, float size);
void  set_canvas_grid_thickness (int id, int thick);
void  set_canvas_grid_type (int id, int type);

bool  is_s3tc_available();
void  render_measure_mode(Canvas* c);

void  render_clast_mode(Canvas* c);
void  set_clast_1st_point(int id, float x, float y);
void  set_clast_2nd_point(int id, float x, float y);
void  set_clast_mode(int id, int mode);

void render_cut_mode(Canvas* c);

void  set_bgcolor(const float *bgcolor);
float *get_bgcolor();

void  set_crosshair(bool b);
bool  has_crosshair();

bool  get_horizontal_depth();
void  set_horizontal_depth(bool b);

bool  is_show_origin();
void  set_show_origin(bool b);

void  set_crosshair_label(char *label);
void  set_canvas_rows_and_columns(int nrow, int ncols);

void  setTieDepth(bool isEnabled, float tieDepth);
#endif
