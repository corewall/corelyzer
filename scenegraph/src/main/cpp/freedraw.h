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

#ifndef CORELYZER_FREE_DRAW_H
#define CORELYZER_FREE_DRAW_H

#include "corelyzer_system.h"
#include <jni.h>

typedef struct PluginFreeDrawRectangle_s {
    int plugin; // holds the integer reference to the plugin that controls
                // this rectangle
    int track;
    int section;
    float w;
    float h;
    float x;
    float y;
    bool  scale_independent;
    bool  visible;
} PluginFreeDrawRectangle;


int   create_free_draw_rectangle (int plugin, int track, int section,
                                  float x, float y,
                                  float w, float h);
void  free_free_draw_rectangle   (int fdid);

bool  is_free_draw_rectangle (int fdid);
int   get_free_draw_plugin   (int fdid);
int   get_free_draw_track    (int fdid);
int   get_free_draw_section  (int fdid);
float get_free_draw_width    (int fdid);
float get_free_draw_height   (int fdid);
float get_free_draw_x        (int fdid);
float get_free_draw_y        (int fdid);
bool  is_free_draw_scale_independent (int fdid);
bool  is_free_draw_visible   (int fdid);

void  set_free_draw_width    (int fdid, float w);
void  set_free_draw_height   (int fdid, float h);
void  set_free_draw_x        (int fdid, float x);
void  set_free_draw_y        (int fdid, float y);
void  set_free_draw_scale_independence (int fdid, bool flag);
void  set_free_draw_visibility (int fidid, bool flag);

void  render_free_draw       (int fdid);


#endif
