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

#ifndef _CORE_PWAVEVALUE_H_
#define _CORE_PWAVEVALUE_H_

#include <Value.h>
 
class PwaveValue : public Value
{
public:
	// Constructor
	PwaveValue( int index, CoreObject* parent );

	// Destructor
	virtual ~PwaveValue( void );
	
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
	void setVelocity( double velocity );
	double getVelocity( void );
	
	void setStrength( int strenth );
	int getStrength( void );
	
	void setTravelTime( int time );
	int getTravelTime( void );
	
	void setDisplacement( int value );
	int getDisplacement( void );
	
protected:
	double m_velocity;
	
	int m_sigStrength;
	int m_travelTime;
	int m_displacement;
	
private:
			
};

#endif
