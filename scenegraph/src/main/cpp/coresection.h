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
    
#ifndef CORE_IMAGE_H 
#define CORE_IMAGE_H 

#if defined(WIN32) || defined(_WIN32)
#include <windows.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include "model.h"
#include "annotationmarker.h"
#include "tie.h"

#include <vector>

// NOTE: If you add a new marker, make sure to increase the
// NUM_CORE_MARKERS, take away a marker and decrease.
// Also update the init_section_annotation_marker function in
// coresection.cpp

#define DEFAULT_SECTION_HEIGHT 10.0f	// 10 cm
#define PORTRAIT               true
#define LANDSCAPE              false

typedef struct CoreAnnotation_s {
    AnnotationMarker m;
    float sec_depth_value; // depth value from beginning of section in meters
} CoreAnnotation;

typedef struct CoreSection_s {
    ModelGridsLOD* g;         // model grid

    float px;             // horizontal position (pixel)
    float py;             // vertical position   (pixel)
    float dpi_x;          // horizontal dpi for scaling
    float dpi_y;          // vertical dpi for scaling
    int   src;            // source image in texture repository
    int   track;          // trackId this section belongs to
    int   section;

    bool  highlight;
    GLfloat *highlight_color;
    
    bool movable;		// if true, section can be moved along depth axis
	bool graphMovable;	// if true, section graphs can be moved along depth axis

    std::vector< CoreAnnotation* > annovec;
    std::vector<CoreSectionTie *> tievec;
    std::vector< int > graphvec;
    std::vector< int > freedrawvec;

    bool  draw_vert_line;
    float vert_line_x;

	char* name;				// section name
	float width;			// section width  (cm)
	float height;			// section height (cm)
	float depth;			// section depth  (cm)
	float graph_offset;		// offset of graph (pixel)

    float rotangle;         // angle at which the model should be rotated
    bool  orientation;      // is the model landscape(0) or portrait(1)

    // for partial section rendering, default is 0 - width (cm)
    float intervalTop;
    float intervalBottom;

    int parentTrack;
    int parentSection;
} CoreSection;

void create_section_model      (int track, int section, CoreSection*& ptr);
void add_section_image		   (int track, int section, int src, 
                                float rot_angle, CoreSection*& ptr);

void free_section_model        (CoreSection* ptr);
void render_section_model      (CoreSection* ptr, Canvas* c);
void render_highlight(CoreSection* ptr, Canvas* c);

void init_section_annotation_markers();
void free_section_annotation_markers();


// create an section annotation of a certain type, at a particular depth
// from the top of the section in meters
int   create_section_annotation(CoreSection* ptr, int group, int type, float x, float y);
void  free_section_annotation   (CoreSection* ptr, int annoId);
bool  is_section_annotation     (CoreSection* ptr, int annoId);
void  set_section_annotation_focus (CoreSection* ptr, int annoId, bool value);
void  set_section_name			(CoreSection* ptr, const char * name);

char* get_section_name				(CoreSection* ptr);
void  set_section_annotation_width  (CoreSection* ptr, int annoId, float w);
void  set_section_annotation_height (CoreSection* ptr, int annoId, float h);
void  set_section_draw_vert_line    (CoreSection* ptr, bool draw, float x);

float get_section_annotation_x (CoreSection* ptr, int annoId);
float get_section_annotation_y (CoreSection* ptr, int annoId);

float get_section_length(CoreSection* ptr);

void  attach_free_draw_to_section   ( CoreSection* ptr, int pfdrId);
void  detach_free_draw_from_section ( CoreSection* ptr, int pfdrId);

float get_section_annotation_icon_x(CoreSection* ptr, int annoId);
float get_section_annotation_icon_y(CoreSection* ptr, int annoId);
float get_section_annotation_vt0(CoreSection* ptr, int annoId);
float get_section_annotation_vt1(CoreSection* ptr, int annoId);
float get_section_annotation_vt2(CoreSection* ptr, int annoId);
float get_section_annotation_vt3(CoreSection* ptr, int annoId);

float get_section_graph_offset(CoreSection* ptr);

bool  is_show_section_text();
void  set_show_section_text(bool b);

int   get_section_parentTrackId(CoreSection * ptr);
int   get_section_parentSectionId(CoreSection * ptr);

void  set_section_parentTrackId(CoreSection * ptr, int trackId);
void  set_section_parentSectionId(CoreSection * ptr, int sectionId);

void  set_section_highlight_color(CoreSection * ptr, float r, float g, float b);
bool  doesCoreSectionHasDuplicateWithInfo(CoreSection *cs, float x, float y, float v0, float v1, float v2, float v3, const char* urlString, const char* localString);
#endif

