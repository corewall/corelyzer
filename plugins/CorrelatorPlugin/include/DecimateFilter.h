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

#ifndef _CORE_DECIMATE_FILTER_H_
#define _CORE_DECIMATE_FILTER_H_

#include <Actor.h>

class DecimateFilter : public Actor
{
public:
	// Constructor
	DecimateFilter( int number = 1 );
		
	// Destructor
	virtual ~DecimateFilter( void );

public:
	virtual void	visitor( CoreObject* object ); 
	void init( void );

	// set_functions and get_functions.
public:
	void setNumber( int number );
	int getNumber( void );
	
protected:
	int m_deciNumber;

private:
	
};
 
#endif
