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

#ifndef _CORE_NATURALGAMMAVALUE_H_
#define _CORE_NATURALGAMMAVALUE_H_

#include <Value.h>
 
class NaturalGammaValue : public Value
{
public:
	// Constructor
	NaturalGammaValue( int index, CoreObject* parent );

	// Destructor
	virtual ~NaturalGammaValue( void );
	
public:
	virtual void	accept( Actor* flt ); 

	virtual void	reset( void );

	virtual void	copy( Value* valueptr );

#ifdef DEBUG	
	virtual void debugPrintOut( void );
#endif
	
public:
	
	// set_functions and get_functions.
public:
	void setTotalCount( double count );
	double getTotalCount( void );
	
	void setSampleTime( int time );
	int getSampleTime( void );
	
	void setSegCount( int index, double count );
	double getSegCount( int index );
	
protected:
	double m_totalCount;
	
	int m_sampleTime;
	double m_segCount[5];
	
private:
			
};

#endif
