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

#include <string>
#include <vector>

#include "dataset.h"
#include "graph.h"

//======================================================================
std::vector< Dataset* > datasetvec;

//======================================================================
SectionTable::SectionTable(const char *name)
{
	if ( name )
	{
		this->name = new char[strlen(name) + 1];
	    strcpy(this->name, name);
	}
	else
	{
		printf("Unexpected - no given name for new SectionTable, using dummy\n");
		const char *dummyName = "unnamed section table";
		this->name = new char[strlen(dummyName) + 1];
		strcpy(this->name, dummyName);
	}

    numberOfRows = 0;
    numberOfFields = 0;
	depthUnitScale = 0.0f;
    offset = 0.0f;
	
    depth = NULL;
	min = NULL;
	max = NULL;
	fieldNames = NULL;
	table = NULL;
}

SectionTable::~SectionTable()
{
	if ( name ) delete[] name;
	if ( depth ) delete[] depth;
	if ( min ) delete[] min;
	if ( max ) delete[] max;
	
	for ( int i = 0; i < numberOfFields; i++ )
	{
		if ( fieldNames[i] ) delete[] fieldNames[i];
		if ( table[i] ) delete[] table[i];
	}
}

//======================================================================
int create_dataset(const char* name)
{
    if (!name) return -1;

    // see if the set by the same name is already there
    unsigned int i;
    for (i = 0; i < datasetvec.size(); i++) 
    {
        if (datasetvec[i] == NULL )
            continue;

        if (!strcmp(datasetvec[i]->name, name))
            return i;
    }

    Dataset* d = new Dataset();
    d->name = new char[strlen(name) + 1];
    strcpy(d->name,name);
    d->url = NULL;

    for (i = 0; i < datasetvec.size(); i++)
    {
        if (datasetvec[i] == NULL)
        {
            datasetvec[i] = d;
            return i;
        }

    }

    datasetvec.push_back(d);
    return (datasetvec.size() - 1);
}

//======================================================================
void free_dataset(const int datasetId)
{
	Dataset* d = datasetvec[datasetId];
    if (!d) return;
	
	// remove graphs that use this dataset
	remove_dataset_graphs( datasetId );
	
	// free SectionTable memory
	std::vector< SectionTable* >::iterator stitr = d->sectionvec.begin();
	while ( stitr != d->sectionvec.end() )
	{
		if ( *stitr ) delete *stitr;
		stitr++;
	}

    d->sectionvec.clear();
    
    delete datasetvec[datasetId];
    datasetvec[datasetId] = NULL;
}

//======================================================================
void set_dataset_url(int set, const char* url)
{
    if(!is_dataset(set)) return;
    if(!url) return;
    if(datasetvec[set]->url)
        delete [] datasetvec[set]->url;

    datasetvec[set]->url = new char[strlen(url) + 1];
    strcpy(datasetvec[set]->url,url);
}

//======================================================================
char* get_dataset_url(int set)
{
    if(!is_dataset(set)) return NULL;
    return datasetvec[set]->url;
}

//======================================================================
char* get_dataset_name(int set)
{
    if(!is_dataset(set)) return NULL;
    return datasetvec[set]->name;
}

//======================================================================
int create_table(int set, const char* name)
{
    if (!is_dataset(set)) return -1;
    if (!name) return -1;

    SectionTable* t = new SectionTable( name );

    for (unsigned int i = 0; i < datasetvec[set]->sectionvec.size(); i++)
    {
        if (datasetvec[set]->sectionvec[i] == NULL)
        {
            datasetvec[set]->sectionvec[i] = t;
            return i;
        }
    }

    datasetvec[set]->sectionvec.push_back(t);
    return datasetvec[set]->sectionvec.size() - 1;
}

//======================================================================
int num_datasets()
{
    return datasetvec.size();
}

//======================================================================
int num_tables(int set)
{
    if(!is_dataset(set)) return -1;
    return datasetvec[set]->sectionvec.size();
}

//======================================================================
bool is_dataset(int set)
{
    if (set < 0) return false;
	const int dataSetVecSize = datasetvec.size();
	if (set >= dataSetVecSize) return false;
    return (datasetvec[set] != NULL) ? true : false;
}

//======================================================================
bool is_table(int set, int table)
{
    if (!is_dataset(set)) return false;
    if (table < 0) return false;
	const int secVecSize = datasetvec[set]->sectionvec.size();
	if (table >= secVecSize) return false;
    return (datasetvec[set]->sectionvec[table] != NULL) ? true : false;
}

//======================================================================
int set_table_height(int set, int table, int height)
{
    if(!is_table(set,table)) return -1;
    if( height <= 0 ) return datasetvec[set]->sectionvec[table]->numberOfRows;

    SectionTable* t = datasetvec[set]->sectionvec[table];
    if( t->numberOfRows > 0 && t->depth) return t->numberOfRows;

    t->numberOfRows = height;
    t->depth        = new float[height];

    return t->numberOfRows;
}

//======================================================================
int set_table_field_count(int set, int table, int fields)
{
    // need to make sure it is a table.  Also the depth column should already
    // be made!!!!

    if (!is_table(set,table)) return -1;
    SectionTable* t = datasetvec[set]->sectionvec[table];

    if ( t->depth == NULL || t->numberOfRows <= 0) return -1;
    if ( fields <= 0) return datasetvec[set]->sectionvec[table]->numberOfFields;


    if ( t->numberOfFields > 0 && t->table) return t->numberOfFields;
    
    t->table = new Cell*[fields];
    int i;
    for ( i = 0; i < fields; i++)
    {
        t->table[i] = new Cell[t->numberOfRows];
    }

    t->min = new float[fields];
    t->max = new float[fields];

    t->numberOfFields = fields;
    t->fieldNames = new char*[fields];
    for( i = 0; i < fields; i++)
        t->fieldNames[i] = NULL;

    return t->numberOfFields;
}

//======================================================================
void set_table_field_name(int set, int table, int field, const char* name)
{
    if(!name) return;
    if(!is_table(set,table)) return;

    SectionTable* t = datasetvec[set]->sectionvec[table];
    if( field > t->numberOfFields - 1) return;
    if( t->fieldNames[field] )
        delete [] t->fieldNames[field];
    t->fieldNames[field] = new char[ strlen(name) + 1];
    strcpy(t->fieldNames[field], name);
}

//======================================================================
void set_table_row_depth(int set, int table, int row, float depth)
{
    if(row < 0) return;
    if(!is_table(set,table)) return;

    SectionTable* t = datasetvec[set]->sectionvec[table];

    if( row >= t->numberOfRows)     return;
    if( !t->depth )                 return;

    t->depth[row] = depth;
}

//======================================================================
void set_table_cell(int set, int table, int field, int row, float value)
{
    if(!is_table(set,table))
    {
#ifdef DEBUG
        printf("Not a table! set %d, table %d\n", set, table);
#endif
        return;
    }

    if(field < 0 || row < 0)
    {
#ifdef DEBUG
#endif
        return;
    }
    SectionTable* t = datasetvec[set]->sectionvec[table];
   
    if( field >= t->numberOfFields)
    {
#ifdef DEBUG
        printf("Field number too high %d against %d\n", field,
               t->numberOfFields);
#endif
        return;
    }

    if( row >= t->numberOfRows)
    {
#ifdef DEBUG
        printf("Row number too hight %d agains %d\n", row, t->numberOfRows);
#endif
        return;
    }

    if( !t->table || !t->table[field] )
    {
#ifdef DEBUG
        printf("NULL POINTER!!!\n");
#endif
        return;
    }

    t->table[field][row].value = value;

#ifdef DEBUG
    printf("Set field %d, row %d to value %f\n", field, row, value);
#endif

}

//======================================================================
void set_table_cell_valid(int set, int table, int field, int row, bool valid)
{
    if(field < 0 || row < 0) return;
    if(!is_table(set,table)) return;

    SectionTable* t = datasetvec[set]->sectionvec[table];

    if( field >= t->numberOfFields)     return;
    if( row >= t->numberOfRows)         return;
    if( !t->table || !t->table[field] ) return;

    t->table[field][row].valid = valid;
}

//======================================================================
void set_field_min_max(int set, int table, int field, float min, float max)
{
    if(!is_table(set,table)) return;
    if(field < 0) return;

    SectionTable* t = datasetvec[set]->sectionvec[table];
   
    if( field >= t->numberOfFields)     return;
    if( !t->table || !t->table[field] ) return;

    t->min[field] = min;
    t->max[field] = max;
}

//======================================================================
bool add_new_field_to_table(int set, int table, const char* name) 
{
	// 8/20/2012 brg: TODO deleting old tables and creating new ones every time
	// we add a field? std::vector would be a better choice.
	
	if(!is_table(set,table)) return false;
    if(!name) return false;

    SectionTable* t = datasetvec[set]->sectionvec[table];

	// allocate new arrays one component larger than original arrays
	// to create space for new field name and data
    char **newnames = new char*[ t->numberOfFields + 1 ];
    Cell **newtable = new Cell*[ t->numberOfFields + 1 ];

	// copy contents of original arrays into new arrays
    for ( int i = 0; i < t->numberOfFields; i++ )
    {
        newtable[i] = t->table[i];
        newnames[i] = t->fieldNames[i];
    }

	// add new field name and data to new arrays
    Cell* col = new Cell[t->numberOfRows];
    newtable[ t->numberOfFields + 1 ] = col;
    newnames[ t->numberOfFields + 1 ] = new char[ strlen(name) + 1 ];
    strcpy( newnames[ t->numberOfFields + 1 ], name );

	// free original arrays
    delete[] t->table;
    delete[] newnames;

	// update pointers to new arrays
    t->table = newtable;
    t->fieldNames = newnames;
    t->numberOfFields = t->numberOfFields + 1;

    return true;
}

//======================================================================
char* get_table_name( int set, int table)
{
    if(!is_table(set,table)) return NULL;
    return datasetvec[set]->sectionvec[table]->name;
}

//======================================================================
int get_table_height(int set, int table)
{
    if(!is_table(set,table)) return -1;
    return datasetvec[set]->sectionvec[table]->numberOfRows;
}

//======================================================================
int get_table_field_count(int set, int table)
{
    if(!is_table(set,table)) return -1;
    return datasetvec[set]->sectionvec[table]->numberOfFields;
}

//======================================================================
float get_table_row_depth(int set, int table, int row)
{
    if( row < 0 )            return 0.0f;
    if(!is_table(set,table)) return 0.0f;

    SectionTable *t = datasetvec[set]->sectionvec[table];

    if(row >= t->numberOfRows) return 0.0f;
    if(!t->depth)              return 0.0f;
    
    return t->depth[row];
}

//======================================================================
// 7/18/2012 brg: See comments on get_table_cell_fast()
float get_table_row_depth_fast(int set, int table, int row)
{
    SectionTable *t = datasetvec[set]->sectionvec[table];
    return t->depth[row];
}

//======================================================================
float get_table_cell(int set, int table, int field, int row)
{
    if(row < 0 || field < 0)
    {
#ifdef DEBUG
        printf("Invalid Cell Range %d, %d\n", field, row);
#endif
        return 0.0f;
    }

    if(!is_table(set,table))
    {
#ifdef DEBUG
        printf("Is not a table: set %d, table %d\n", set, table);
#endif
        return 0.0f;
    }

    SectionTable* t = datasetvec[set]->sectionvec[table];

    if( field >= t->numberOfFields )
    {
#ifdef DEBUG
        printf("Field number %d out of range %d\n", field,
               t->numberOfFields);
#endif
        return 0.0f;
    }

    if( row >= t->numberOfRows )
    {
#ifdef DEBUG
        printf("Row number %d out of range %d\n", row, t->numberOfRows);
#endif
       return 0.0f;
    }

    if( !t->table || !t->table[field])
    {
#ifdef DEBUG
        printf("NULL POINTER!!!\n");
#endif
        return 0.0f;
    } 

#ifdef DEBUG
    printf("Returning %f\n", t->table[field][row].value);
#endif
    return t->table[field][row].value;
}

//======================================================================
// 7/18/2012 brg: get_table_cell() above does the same checks in
// is_table_cell_valid(), which is redundant in the case of render_graph()
// and slows things down unnecessarily. Adding this validation-free flavor
// of the routine.
float get_table_cell_fast(int set, int table, int field, int row)
{
    SectionTable* t = datasetvec[set]->sectionvec[table];
    return t->table[field][row].value;
}

//======================================================================
bool is_table_cell_valid(int set, int table, int field, int row)
{
    if(row < 0 || field < 0) return 0.0f;
    if(!is_table(set,table)) return 0.0f;
    
    SectionTable* t = datasetvec[set]->sectionvec[table];

    if( field >= t->numberOfFields )   return 0.0f;
    if( row >= t->numberOfRows )       return 0.0f;
    if( !t->table || !t->table[field]) return 0.0f;
    
    return t->table[field][row].valid;

}

//======================================================================
// 8/22/2012 brg: Assume the parameters we pass in are valid for
// performance's sake. Validation is important, but I think we're
// doing it to excess thoughout the graphing code at the expense of
// performance. TODO
bool is_table_cell_valid_fast(int set, int table, int field, int row)
{
	return datasetvec[set]->sectionvec[table]->table[field][row].valid;
}

//======================================================================
int get_dataset(const char* name)
{
    for (unsigned int i = 0; i < datasetvec.size(); i++)
    {
        if (datasetvec[i] == NULL) continue;
        if (!strcmp(datasetvec[i]->name, name)) return i;
    }

    return -1;
}

//======================================================================
Dataset* get_dataset(int set)
{
    if(!is_dataset(set)) return NULL;
    return datasetvec[set];
}

//======================================================================
int get_table(int set, const char* name)
{
    if (!is_dataset(set)) return -1;
    for (unsigned int i = 0; i < datasetvec[set]->sectionvec.size(); i++)
    {
        if(datasetvec[set]->sectionvec[i] == NULL) continue;
        if(!strcmp(datasetvec[set]->sectionvec[i]->name, name)) return i;
    }

    return -1;
}

//======================================================================
SectionTable* get_table(int set, int table)
{
    if(!is_table(set,table)) return NULL;
    return datasetvec[set]->sectionvec[table];
}

//======================================================================
char* get_field_name(int set, int table, int field)
{
    if(!is_table(set,table)) return NULL;
    SectionTable *t = datasetvec[set]->sectionvec[table];
    if( field < 0 || field > t->numberOfFields - 1) return NULL;

//    printf("Returning Field Name: %s\n", t->fieldNames[field]);

    return t->fieldNames[field];
}

float get_field_min(int set, int table, int field)
{
    if(!is_table(set, table)) return -1;
    if(field < 0) return -1;

    SectionTable* t = datasetvec[set]->sectionvec[table];

    if( field >= t->numberOfFields )    return -1;
    if( !t->table || !t->table[field] ) return -1;

    return t->min[field];
}

float get_field_max(int set, int table, int field)
{
    if(!is_table(set, table)) return -1;
    if(field < 0) return -1;

    SectionTable* t = datasetvec[set]->sectionvec[table];

    if( field >= t->numberOfFields )    return -1;
    if( !t->table || !t->table[field] ) return -1;

    return t->max[field];
}

float get_field_range(int set, int table, int field)
{
    float min = get_field_min(set, table, field);
    float max = get_field_max(set, table, field);
    return (max-min);
}

//======================================================================

void set_table_depthunitscale (int set, int table, float scale)
{  
    datasetvec[set]->sectionvec[table]->depthUnitScale = scale;
}

float get_table_depthunitscale (int set, int table)
{
    return datasetvec[set]->sectionvec[table]->depthUnitScale;
}

float get_table_offset(int set, int table)
{
    if(!is_table(set, table)) return 0.0f;

    SectionTable* t = datasetvec[set]->sectionvec[table];

    if(!t) return 0.0f;

    return t->offset;
}

void set_table_offset(int set, int table, float offset)
{
    if(!is_table(set, table)) return;

    SectionTable* t = datasetvec[set]->sectionvec[table];

    if(!t) return;

    t->offset = offset;
}
