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

#ifndef _CORE_FILTER_MANAGER_H_
#define _CORE_FILTER_MANAGER_H_

#include <vector>
#include <string>
#include <Data.h>
#include <CullFilter.h>
#include <DecimateFilter.h>
#include <GaussianFilter.h>

class FilterManager
{
public:
	// Constructor
	FilterManager( void );	
		
	// Destructor
	virtual ~FilterManager( void );

public:
	void init( void );
	void setData(Data* dataptr);
	void setLogData(Data* dataptr);
	void setSpliceHole(Hole* holeptr);
	void setSaganHole(Hole* holeptr);
	
	CullFilter* getCullFilter( const char* leg, const char* site, int type, char* annot );
	DecimateFilter* getDeciFilter( const char* leg, const char* site, int type, char* annot );
	GaussianFilter* getSmoothFilter( const char* leg, const char* site, int type, char* annot );
	void apply( void );
		
protected:
		
protected:
	std::vector<CullFilter*> m_cullFilters;
	std::vector<DecimateFilter*> m_deciFilter;
	std::vector<GaussianFilter*> m_smoothFilter;	
	
	Data* m_dataptr;
	Hole* m_spliceptr;
	Hole* m_saganptr;
	Data* m_logdataptr;	
	std::string m_leg;
	std::string m_site;
};
 
#endif
