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
 
#ifndef TRACK_SCENE_H
#define TRACK_SCENE_H

//#include "renderable.h"


#include "trackscenenode.h"

//---------------------------------------------------------- 

typedef struct TrackScene_s {
    int selectedTrack;
    std::vector< TrackSceneNode* > trackvec;
    std::vector< int > zorder;
    std::vector< int > freedrawvec;

} TrackScene;

//---------------------------------------------------------- 
int get_render_mode();
void set_render_mode(int m);

int  create_track_scene ();
void free_track_scene   (int i);
bool is_track_scene     (int i);
bool is_track           (int scene, int trackid);

int  append_track       (int scene, const char* sessionName, const char* trackName);
void free_all_tracks    (int scene);
void free_track         (int scene, int trackid);
void free_track_section_model(int scene, int trackid, int sectionid);
int  get_track          (int scene, const char* name);
int  num_tracks         (int scene);
void highlight_track    (int scene, int trackid, bool highlight = true);
void bring_track_front  (int scene, int trackid);
void attach_free_draw_to_scene   (int scene, int fdid);
void detach_free_draw_from_scene (int scene, int fdid);

void render_track_scene (int scene, Canvas* c);

TrackScene*     get_scene             (int scene);
TrackSceneNode* get_scene_track       (int scene, int trackid);
int  get_scene_track_zorder_length    (int scene);
void get_scene_track_zorder           (int scene, int* order);
int  get_track_id                     (int scene, TrackSceneNode* t);

// Same functions as above... assuming that bind_scene has been called
void bind_scene         (int scene);
bool is_scene_bound     ();
bool is_track           (int trackid);
int  append_track       (const char* sessionName, const char* name);
void free_all_tracks    ();
void free_track         (int trackid);
int  get_track          (const char* name);
int  num_tracks         ();
void highlight_track    (int trackid, bool highlight = true);
void bring_track_front  (int trackid);
void attach_free_draw_to_scene   (int pfdrId);
void detach_free_draw_from_scene (int pfdrId);
void render_track_scene          (Canvas* c);

TrackScene*     get_scene           ();
TrackSceneNode* get_scene_track     (int trackid);
int  get_scene_track_zorder_length  ();
void get_scene_track_zorder         (int* order);
int  get_track_id                   (TrackSceneNode* t);
float get_scene_track_nextpos (int trackid);

void set_remote_controlled(bool b);
bool get_remote_controlled();

void reset_default_track_ypos();

#endif

