//////////////////////////////////////////////////////////////////////////////////
// CorrelaterLib - Correlater Class Library :  
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

int FindFormat( const char* filename, FILE *fptr ); 
int FindCoreType( FILE *fptr );

int ReadMST95report( FILE *fptr, Data* dataptr, int datatype);
int ReadTKreport( FILE *fptr, Data* dataptr, int datatype );
int ReadOtherOdp( FILE *fptr, Data* dataptr, int datatype );
int ReadJanus( FILE *fptr, Data* dataptr, int datatype);
int ReadSplice( FILE *fptr, Data* dataptr );


int ReadAffineTable( FILE *fptr, Data* dataptr );
int ReadSpliceTable( FILE *fptr, Data* dataptr );
int ReadEqLogDepthTable( FILE *fptr, Data* dataptr, const char* affinefilename );
int ReadStratTable( FILE *fptr, Data* dataptr, int datatype );

int WriteAffineTable( FILE *fptr, Data* dataptr );
int WriteAffineTableinXML( FILE *fptr, Data* dataptr );
int WriteSpliceTable( FILE *fptr, Data* dataptr );
int WriteSpliceTableinXML( FILE *fptr, Data* dataptr );
int WriteEqLogDepthTable( FILE *fptr, Data* dataptr, const char* affinefilename );
int WriteStratTable( FILE *fptr, Data* dataptr );
int WriteSplice( FILE *fptr, Hole* dataptr, int leg, int site );

// wonder about these functionility.....
int ReadLog( FILE *fptr, char* label, Data* dataptr, int selectedColumn, std::string& result );
int WriteLog( FILE *fptr, char* label );

int WriteCoreData( char* filename, Data* dataptr );

int ReadXMLFile(const char* fileName, Data* dataptr);

#endif
