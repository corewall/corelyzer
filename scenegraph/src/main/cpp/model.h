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
 
#ifndef __IMAGE_QUADTREE_H__
#define __IMAGE_QUADTREE_H__

#include "canvas.h"

//-------------------------------------------------------------------
#define NW_CORNER 	0
#define SE_CORNER 	1
#define NE_CORNER 	2
#define SW_CORNER	3
#define UNDEF_CORNER    4

#define TEX_S           0
#define TEX_T           1

//-------------------------------------------------------------------
typedef struct ModelNode_s {
    float  x;
    float  y;
    float  w;
    float  h;
    int    col;
    int    row;
    float  tex_crd[2][2];    
} ModelNode;

//-------------------------------------------------------------------

typedef struct ModelGridsLOD_s {
    
    int  lod;                    // lod to render
    int* cols;
    int* rows;
    int  src;                    // source image
    ModelNode** grid;           // lod number of 1D arrays of 2D grids

} ModelGridsLOD;

//-------------------------------------------------------------------          

void create_model_grid  (int src, ModelGridsLOD*& m);
void free_model_grid    (ModelGridsLOD* m);

void set_model_grid_lod (ModelGridsLOD* m, int lod);
int  get_model_grid_lod (ModelGridsLOD* m);

void render_model_grid  (ModelGridsLOD* m, Canvas* c);

#endif

