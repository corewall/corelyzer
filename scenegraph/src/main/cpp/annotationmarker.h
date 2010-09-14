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

#ifndef CORELYZER_ANNOTATION_MARKER_H
#define CORELYZER_ANNOTATION_MARKER_H

#define DEFAULT_MARKER_SCALE (1.5f)
#define DEFAULT_MARKER_DPI_X (36.0f)
#define DEFAULT_MARKER_DPI_Y (36.0f)

// Types of markers
#define NUM_CORE_MARKERS    7

#define CORE_POINT_MARKER   0
#define CORE_SPAN_MARKER    1
#define CORE_OUTLINE_MARKER 2

#define CORE_CLAST_MARKER   3
#define CORE_SAMPLE_MARKER  4
#define CORE_PV_MARKER      5
#define CORE_FREEFORM_MARKER 6

#define HIT_ANNOTATION		0
#define HIT_POINT			1
#define HIT_SPANL			2
#define HIT_SPANR			3
#define HIT_OUTLINE			4
#define HIT_OUTLINETOP		5
#define HIT_OUTLINEBOTTOM	6
#define HIT_OUTLINELEFT		7
#define HIT_OUTLINERIGHT	8

#include "canvas.h"

typedef struct AnnotationMarkerType_s {
    int    tex;
    char*  typeName;
    bool   valid;
} AnnotationMarkerType;

typedef struct AnnotationMarker_s {
    float px;        // x position for annotation icon
    float py;        // y position for annotation icon
    float w;         // width 
    float h;         // height
    float ultex[2];  // upper left tex coord
    float lrtex[2];  // lower right tex coord
    int   type;      // which marker type will we use for this?
    char* url;       // annotation marker's associated URL
    char* local_file;// annotation marker's associated Local File
    int   group;     // annotation group
                     // eg, sedimentology, geophysics etc.

    bool  visibility;
	bool  focused;		// editing mode
	float depthX;		// original x depth of user marker
	float depthY;		// original y depth of user marker
	float markerVt[5];	// vertices storage for marker type (span, outline)
	float *hit0, *hit1;	// current hit handle of manipulator pointer

    char* label;
    char* rlabel;
} AnnotationMarker;

static float color_palette[8][3] = {
                              {1.0f, 0.0f, 0.0f}, {1.0f, 0.0f, 0.8f},
                              {0.8f, 0.0f, 1.0f}, {1.0f, 1.0f, 0.0f},
                              {0.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 1.0f},
                              {0.0f, 0.0f, 1.0f}, {1.0f, 0.0f, 1.0f}
                            };

int  register_marker_type           (const char* typeName);
void unregister_marker_type         (int type);

void alloc_marker_type_resource     (int type, const char* pngTextureFilename);
int  copy_marker_type               (int type, const char* newTypeName);
void free_marker_type_resource      (int type);
bool marker_type_resources_allocated(int type);
bool is_marker_type                 (int type);

void  render_marker  (Canvas *c, AnnotationMarker* m);

void  set_marker_label(AnnotationMarker* m, char* label);
char* get_marker_label(AnnotationMarker* m);

void  set_marker_relation_label(AnnotationMarker* m, const char *label);
char* get_marker_relation_label(AnnotationMarker* m);

void  set_marker_url (AnnotationMarker* m, char* url);
void  set_marker_local_file( AnnotationMarker* m, char* filename);
char* get_marker_url (AnnotationMarker* m);
char* get_marker_local_file( AnnotationMarker* m);
void  set_marker_group (AnnotationMarker* m, int groupid);
int   get_marker_group (AnnotationMarker* m);
void  set_marker_type (AnnotationMarker* m, int typeId);
int   get_marker_type (AnnotationMarker* m);
void  set_marker_focus (AnnotationMarker* m, bool flag );

void  set_marker_visibility (AnnotationMarker* m, bool vis);
bool  get_marker_visibility (AnnotationMarker* m);

void  set_marker_position (AnnotationMarker* m, float x, float y);
void  draw_cross_arrow (float x, float y, float size);
void  draw_circle (float x, float y, float radius);
void  draw_rectangle (float x1, float y1, float x2, float y2); 
void  draw_solid_square (float x, float y, float size);
void  set_marker_vertex (AnnotationMarker* m, float ax, float ay,
											  float v0, float v1,
											  float v2, float v3);

void  setMarkerScale(float s);
float getMarkerScale();

void  setMarkerAutoScale(bool b);
bool  isMarkerAutoScale();
#endif
