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
 
#ifndef GRAPH_H
#define GRAPH_H

#include "canvas.h"
#include "model.h"
#include "dataset.h"
#include "coresection.h"
#include "trackscenenode.h"

#include <stdio.h>
#include <stdlib.h>


#include <vector>

#define CM_PER_INCH       2.54
#define INCH_PER_CM       0.394

#define DEFAULT_GRAPH_HEIGHT      (1.0f) // Graph height in inch
#define DEFAULT_GRAPH_SCALE       (1.5f)
#define GRAPH_SECTION_GAP         1.0  // Graph and Image section gap in inch
#define GRAPH_FIELDS_GAP          0.5 

#define BORDER_LINE_WIDTH 2.0
#define BORDER_R          0.0
#define BORDER_G          0.0
#define BORDER_B          1.0

#define GRAPH_LINE		0
#define GRAPH_POINT		1
#define GRAPH_CPOINT	2
#define GRAPH_LINEPOINT 3

#define EXCLUDE_STYLE_CONTINUOUS 0
#define EXCLUDE_STYLE_SHOWGAPS   1

struct GraphPoint {
	float x;
	float y;
	bool exclude;
	
	GraphPoint() : x(0.0f), y(0.0f), exclude(false) { }
	GraphPoint( const float x, const float y, const bool exclude ) : x(x), y(y), exclude(exclude) { }
};

struct Graph {
    int   scene;
    int   track;
    int   section;

    int   dataset;
    int   table;
    int   field;

    // view
    float min;
    float max;

    // data
    float orig_min;
    float orig_max;
	
	// excluded value range
	float exclude_min;
	float exclude_max;
	int exclude_style; // 0 = continuous, 1 = show gaps

    float r;
    float g;
    float b;

    float w;	// width (pixel)
    float h;	// height (pixel)
    float y;	// y pos (pixel)
    
    int   slot;
	int	  type; // graph type: 0 = line, 1 = point, 2 = cross point, 3 = point and line

    char* label;
    bool  show;

	bool  graphonly;
};

enum GraphPointShape {
	GPS_ROUND = 1,
	GPS_TRIANGLE = 2,
	GPS_SQUARE = 3,
	GPS_DIAMOND = 4,
	GPS_X = 5
};

struct Box {
    // upper left object coordinate
    float x;
    float y;

    // width and height
    float w;
    float h;
};

struct GraphDebugInfo {
	bool useScaling;
	bool useLabels;
	bool useBorder;
	bool useScissoring;
	int scaleFactor;
};

void  render_graph  (ModelGridsLOD* m, float dpi_x, float dpi_y,
                     int trackId, int sectionId);
void  render_border (ModelGridsLOD* m, float dpi_x, float dpi_y);

void  render_graph  (Canvas* c, CoreSection* cs, int gid);
void  render_border (float width, float height);
                  
int   add_line_graph_to_section      (int track, int section,
                                      int dataset, int table, int field);

void  remove_dataset_graphs( const int dataset );
int   remove_line_graph_from_section (int track, int section,
                                      int dataset, int table, int field);
int   remove_line_graph_from_section (int gid);
int   locate_graph (int track, int section, int dataset, int table, int field);

bool  is_graph           (int gid);
int   get_data_set_index (int gid);
int   get_table_index    (int gid);
int   get_field_index    (int gid);
int   get_graph_slot     (int gid);
int   get_track_index    (int gid);
int   get_section_index  (int gid);
float get_min            (int gid);
float get_max            (int gid);
float get_graph_orig_max (int gid);
float get_graph_orig_min (int gid);
float get_graph_exclude_max(int gid);
float get_graph_exclude_min(int gid);
int	  get_graph_exclude_style(int gid);
float get_graph_height   (int gid); // returns height in inches
float get_line_graph_color_component (int gid, int component);
int   get_line_graph_type (int gid);

void  set_line_graph_color (int gid, float r, float g, float b);
void  set_line_graph_range (int gid, float min, float max);
void  set_line_graph_exclude_range(int gid, float min, float max);
void  set_line_graph_exclude_style(int gid, int style);
void  set_line_graph_label (int gid, char *label);
void  set_line_graph_type  (int gid, int type);

void render_graph_point( const GraphPoint &pt, const GraphPointShape ptStyle, const float scaling );


// function to position the graph vertically with respect to section
// 0th slot is right above section image, 1st is above the 0th
void  set_graph_slot  (int gid, int slot); 
void  set_graph_ypos  (int gid, float y);
bool  is_line_graph_shown  (int gid);
void  move_graph_to_top(int gid);

int   get_graph_from_section_slot (CoreSection* ptr, int slot);
Box*  get_graph_box(CoreSection* cs, int gid);
Box*  get_graph_box(Canvas* c, CoreSection* cs, int gid);
std::vector< int >  match_my_graph_id (int trackId, int sectionId);

int	find_graph_by_field( const int dataset, const int field );

int isOutside(float prevDepth, float depth, float startDepth, float endDepth);
bool ifCollapse();
void setCollapse(bool aBool);

void setGraphScale(float s);
float getGraphScale();
#endif
