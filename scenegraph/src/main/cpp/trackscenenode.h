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
    
#ifndef TRACK_SCENE_NODE_H
#define TRACK_SCENE_NODE_H

#include "coresection.h"
#include "freedraw.h"
#include <vector>

typedef struct TrackSceneNode_s {
    std::vector< CoreSection* > modelvec;
    std::vector< int > zorder;
    std::vector< int > freedrawvec;

    int   selectedSection;

    char* sessionName;
    char* name;      // UTF-8 name

    bool  highlight;
    GLfloat *highlight_color;

    bool  show;
    float px;        // position x
    float py;        // position y
    float w;         
    float h;         
    float gw;        // graphs are in drawn at a negative position
    float gh;        // relative to the track scene node's position
                     // just keeping track of dimensions taken by graph area
	float nextPos;

	bool  movable;
} TrackSceneNode;


void create_track      (const char* sessionName, const char* trackName, TrackSceneNode*& ptr);
int  append_model      (TrackSceneNode* t, CoreSection* c);

void free_track        (TrackSceneNode* t);
void free_all_models   (TrackSceneNode* t);
void free_model        (TrackSceneNode* t, int section);
bool is_section_model  (TrackSceneNode* t, int section);
void highlight_model   (TrackSceneNode* t, int section, bool highlight = true);
void setShow(TrackSceneNode* t, bool isShow);
void bring_model_front (TrackSceneNode* t, int section);
void render_track        (TrackSceneNode* t, Canvas* c);

CoreSection* get_track_section       (TrackSceneNode* t, int section);
int  get_track_section_zorder_length (TrackSceneNode* t);
void get_track_section_zorder        (TrackSceneNode* t, int* order);
void attach_free_draw_to_track       (TrackSceneNode* t, int fdid);
void detach_free_draw_from_track     (TrackSceneNode* t, int fdid);
void push_section_to_end             (TrackSceneNode* t, int section);

void  set_track_highlight_color(TrackSceneNode* t, float r, float g, float b);
#endif

