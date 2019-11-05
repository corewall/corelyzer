//////////////////////////////////////////////////////////////////////////////////
// CorrelatorLib - Correlator Class Library :  
// It's rebult based on functions in Splicer and Sagan Tool.
//
// Copyright (C) 2007 Hyejung Hur,  
// Electronic Visualization Laboratory, University of Illinois at Chicago
//
// This library is free software; you can redistribute it and/or modify it 
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation; either Version 2.1 of the License, or 
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
// or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
// License for more details.
// 
// You should have received a copy of the GNU Lesser Public License along
// with this library; if not, write to the Free Software Foundation, Inc., 
// 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
// Direct questions, comments etc about CorrelaterLib to hjhur@evl.uic.edu
//

#ifndef _CORE_DATAPARSER_H_
#define _CORE_DATAPARSER_H_

#include <CoreTypes.h>
#include <Data.h>
#include <Hole.h>
#include <string>

struct age_model_st {
	double mbsf;
	double mcd;
	double eld; 
	double age;
	double sedrate;
	double agerate;
};

// delimiters
enum {
	SPACETAB = 0, // delimiter used for all internal Correlator files (and export, until now)
	COMMA = 1
};

std::string GetTypeStr(const int datatype, const char *annotation);

int FindFormat( const char* filename, FILE *fptr ); 
int FindCoreType( FILE *fptr );

void SetDelimiter(const int delim);

int ReadCoreFormat(FILE *fptr, Data* dataptr, int datatype, char* annotation);
int ReadSplice( FILE *fptr, Data* dataptr );

int ReadAffineTable( FILE *fptr, Data* dataptr );
int ReadIODPAffineTable(FILE *fptr, Data* dataptr);

int ReadSpliceTable( FILE *fptr, Data* dataptr, bool alternative = false, int core_type = -1, char* annotation = NULL );

int ReadEqLogDepthTable( FILE *fptr, Data* dataptr, const char* affinefilename );
int ReadStrat( FILE *fptr, Data* dataptr, int datatype );
int ReadStratTable( FILE *fptr, Data* datapt );
int ReadCullTable( FILE *fptr, int coretype, Data* dataptr, char* annotation );

int WriteAffineTable( FILE *fptr, Data* dataptr );
int WriteIODPAffineTable( FILE *fptr, Data* dataptr );
int WriteSpliceTable( FILE *fptr, Data* dataptr, const char* affinefilename= NULL);
int WriteEqLogDepthTable( FILE *fptr, Data* dataptr, const char* affinefilename );
int WriteStratTable( FILE *fptr, Data* dataptr );
int WriteSplice( FILE *fptr, Hole* dataptr, const char* leg, const char* site );
int WriteAgeSplice( FILE *fptr, FILE *temp_fptr, Hole* dataptr, const char* leg, const char* site );
int	WriteLog( char* read_filename, char* write_filename, int depth_idx, int data_idx );
	
// wonder about these functionility.....
int ReadLog( FILE *fptr, char* label, Data* dataptr, int selectedColumn, std::string& result );
int WriteLog( FILE *fptr, Data* dataptr, char* label );

int WriteCoreData( char* filename, Data* dataptr );
int WriteCoreHole( char* filename, Hole* holeptr );

int WriteAgeCoreData( char* agefilename, char* filename, Data* dataptr, int appliedflag );
int WriteAgeCoreHole( char* agefilename, char* filename, Hole* holeptr, int appliedflag );

int ReadXMLFile(const char* fileName, Data* dataptr);
int ChangeFormat(const char* infilename, const char* outfilename);

#endif
