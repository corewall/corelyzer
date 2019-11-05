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

#ifndef _CORE_TYPES_H_
#define _CORE_TYPES_H_
	
#include <stdio.h>
	
//#define DEBUG						1

#define NO							0
#define YES							1
#define ALL							2

// Data Format
#define	MST95REPORT					0
#define	TKREPORT					1
#define OSUSCAT						2
#define JANUS						3
#define JANUSCLEAN					4
#define JANUSORIG					5
#define ODPOTHER					6
#define	ODPOTHER1					7
#define ODPOTHER2					8
#define ODPOTHER3					9
#define ODPOTHER4					10
#define ODPOTHER5					11
#define AFFINE_TABLE				12
#define AFFINE_TABLE1				13
#define SPLICE_TABLE				14
#define STRAT_TABLE					15
#define EQLOGDEPTH_TABLE			16
#define AGE							17
#define BERGER_AGE					18
#define SPLICER_AGE					19
#define LOG							20
#define ANCILL_LOG					21
#define SPLICE						22
#define SPLICER_TABLE				23
#define LOGCORE_PAIRS				24
#define ODP_CDROM					25
#define XML_FILE					26
#define CORE_DATA					27
#define NOT_DEFINE_DATA				28
#define SPLICED_RECORD				29
#define LOGSMOOTH					30
#define SECTION						31
#define ELD_RECORD					38

#define	INTERNAL_REPORT				40
#define	INTERNAL_TYPE				41

// Core Data types
#define	GRA							30	
#define	PWAVE						31	
#define	SUSCEPTIBILITY				32
#define	NATURALGAMMA				33
#define	REFLECTANCE					34
#define OTHERTYPE					35
#define LOGTYPE						36
#define USERDEFINEDTYPE				37
#define INTERPOLATED_VALUE			38

// Old (prior to 1.9) Affine Types
// 'Y' describes cores shifted a different distance than the core above.
// Thus 'N' describes cores that are either unshifted, or shifted by the
// 'this and all below' method (all cores below the selected
// core have the same shift distance as the core above)
#define AFFINE_N					0
#define AFFINE_Y					1

// IODP Affine/Composite types
#define AFFINE_TIE					1
#define AFFINE_SET					2
#define AFFINE_ANCHOR				3

#define AFFINE_TIE_STR				"TIE"
#define AFFINE_SET_STR				"SET"
#define AFFINE_ANCHOR_STR			"ANCHOR"

// Strat types
#define DIATOMS						0
#define RADIOLARIA					1
#define FORAMINIFERA				2
#define NANNOFOSSILS				3
#define PALEOMAG					4

// Data quality flags 
#define	GOOD						0
#define	BAD							1
#define	BAD_TOP						1
#define	BAD_CULL1					2
#define	BAD_CULL2					4
#define	DECIMATED					8
#define BAD_PWAVE_SIGNAL			16
#define	BAD_SB_DEPTH				32
#define	BAD_CORE_NUM				64
#define BAD_CULLTABLE				128
#define	BAD_SMOOTH					256
#define BAD_CODE					-999.00

// Culling 
#define	LESS_THAN					0
#define	LESS_THAN_OR_EQUAL_TO		1
#define	GREATER_THAN				2
#define	GREATER_THAN_OR_EQUAL_TO	3
#define	EQUAL_TO					4
#define	OR							1
#define	ONLY						2	
#define	CULL_PARAMETERS				1
#define	CULL_TABLE					2
#define CULL_PARAMETER_AND_TABLE	3
// section cm depth to use for whether to cull a value using cull table
#define CULL_TABLE_DEPTH_CHECK		0.01 
#define CULL_BAD_CORE				4

// 
#define ROUND_CHECK					0.001  

// Tie types
#define COMPOSITED_TIE				0
#define REAL_TIE					1
#define INTERPOLATED_TIED			2
#define ALL_TIE						3
#define SAGAN_TIE					4
#define SAGAN_FIXED_TIE				5
#define ALT_REAL_TIE				6
#define DUMMY_TIE					7


#define QUALITY						6
#define STRETCH						7
#define QUALITY_SPLICE				8

// Buffer
#define	MAX_LINE					1024
#define TOKEN_LEN					128
#define MAX_TOKENS					1000

#define BADLOG						1
#define LOG_VALUE_CULLED			2
#define LOG_DECIMATED				4

// Max array size for 'dummy' cleaned up core data used for plotting and correlation 
// and max array size for the correlation values
#define MAX_CORE					200
#define MAX_PER_CORE				1000
#define MAX_CORR					1000

// splicing constrained
#define CONSTRAINED					1    
// splicing unconstrained
#define UNCONSTRAINED				2    

// Node type
#define	DATA						0
#define	HOLE						1
#define	CORE						2
#define	VALUE						3
#define TIE							4
#define STRAT						5
#define SPLICEDATA					6
#define SPLICESMOOTH				7
#define TIE_SHIFT					8

// smoothing
#define NONE						0
#define	GAUSSIAN					1
#define	LOWPASS						2

#define	DEPTH						0	// cm
#define	POINTS						1	

#define UNSMOOTH					0
#define SMOOTH						1
#define SMOOTH_BOTH					2
#define SM_OK						3
#define SM_TAIL						4
// don't use smooth if have less than this number in a core 
#define TOOFEW_SMOOTHED				5
#define SM_BAD_DATA					-1
#define SM_TOOFEWPTS				-2
#define SM_BAD_CALC					-3
#define BAD_PT						-999.0

#define DIATOMS						0
#define RADIOLARIA					1
#define FORAMINIFERA				2
#define NANNOFOSSILS				3
#define	PALEOMAG					4

#define TIE_OK						0
#define TIE_ERROR					-1

// error
#define TOO_MANY_CULLED				0.5		// fraction if exceed; too many culled
#define TOO_MANY_OUTOF_ORDER		100		// if this many are out of depth order stop checking


#define PLUSMINUS_STD				3.0		//number of std awy from mean to set variable axis min & max


// brgtodo 5/19/2014: Totally unclear on what most of the matrix array values are used for.
// Indices 0, 5, 10, 15 seem to be always set to 1.0. 11 contains the current offset. 12 appears
// to be used to back up the current offset(?).  Other than that, values are only copied from other
// matrices.
struct data_affine {
	double matrix[16];
	bool applied;
	char valuetype;
	int affinetype;
};

#define round(x) (x<0?ceil((x)-0.5):floor((x)+0.5))


#endif
