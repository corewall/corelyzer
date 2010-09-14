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

#ifndef CAMERA_H
#define CAMERA_H

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

#ifndef PI
#define PI 3.14159
#endif

#ifndef DEG2RAD
#define DEG2RAD( x ) ( PI * (x) / 180.0 )
#endif

#ifndef RAD2DEG
#define RAD2DEG( x ) ( 180.0 * (x) / PI ) 
#endif

typedef struct {
    GLfloat     m[16]; // gl matrix style... column, row order to orient
    GLfloat     pos[3];
    bool        valid;
} Camera;

int  create_camera ();
void free_camera   (int cam);
void free_all_camera ();
bool is_camera     (int cam);
int  num_cameras   ();

// -- camera manipulation
// -- absolute manipulations
void position_camera        ( int id, float  x, float  y, float  z);

void orient_camera          ( int id, float  p, float  y, float  r);
void orient_camera          ( int id, float* mat4_basis);

void get_camera_position    ( int id, float* x, float* y, float* z);
void get_camera_orientation ( int id, float* mat4_basis);

// -- relative manipulations
void turn_camera            ( int id, float  p, float  y, float  r); // radians
void translate_camera       ( int id, float  x, float  y, float  z);

void apply_camera_matrix    ( int id );
void unapply_camera_matrix  ( int id);

#endif

