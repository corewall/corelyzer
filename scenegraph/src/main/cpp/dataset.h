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

#ifndef __DATASET_H__
#define __DATASET_H__

#if defined(WIN32) || defined(_WIN32)
#include <windows.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <vector>

//------------------------------------------------
// Data structure to hold datset
// Each Scene has multiple datasets(xml file),
// Each dataset has multiple SectionTables

struct Cell {
    bool  valid;
    float value;
	
	Cell() : valid(false), value(0.0f) { }
};

struct SectionTable {
	SectionTable(const char *name);
	~SectionTable();
    
	char* name;
    int numberOfRows;
    int numberOfFields;
    float depthUnitScale;
    float offset;

    float* depth;
    float* min;
    float* max;
    char** fieldNames;
    Cell **table;
};

typedef struct {
    char* name;
    char* url;
    std::vector< SectionTable* > sectionvec;
} Dataset;

//------------------------------------------------


int   create_dataset   (const char* name);
void  free_dataset(const int datasetId);
void  set_dataset_url  (int set, const char* url);
char* get_dataset_url  (int set);
char* get_dataset_name (int set);
int   create_table     (int set, const char* name);
int   num_datasets     ();
int   num_tables       (int set);
bool  is_dataset       (int set);
bool  is_table         (int set, int table);

int   set_table_height       (int set, int table, int height);
int   set_table_field_count  (int set, int table, int fields);
void  set_table_field_name   (int set, int table, int field, const char* name);
void  set_table_row_depth    (int set, int table, int row, float depth);
void  set_table_cell         (int set, int table, int field, int row, float v);
void  set_table_cell_valid   (int set, int table, int field, int row, bool v);
void  set_field_min_max      (int set, int table, int field, 
                              float min, float max);
void  set_table_offset       (int set, int table, float offset);
bool  add_new_field_to_table (int set, int table, const char* name);

char* get_table_name           (int set, int table);
int   get_table_height         (int set, int table);
int   get_table_field_count    (int set, int table);
float get_table_row_depth      (int set, int table, int row);
float get_table_row_depth_fast (int set, int table, int row);
float get_table_cell           (int set, int table, int field, int row);
float get_table_cell_fast      (int set, int table, int field, int row);
void  set_table_depthunitscale (int set, int table, float scale);
float get_table_depthunitscale (int set, int table);

char* get_field_name        (int set, int table, int field);
float get_field_min         (int set, int table, int field);
float get_field_max         (int set, int table, int field);
float get_field_range       (int set, int table, int field);
float get_table_offset      (int set, int table);
bool  is_table_cell_valid   (int set, int table, int field, int row);
    
int           get_dataset (const char* name);
Dataset*      get_dataset (int set);
int           get_table   (int set, const char* name);
SectionTable* get_table   (int set, int table);


#endif
