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

#ifndef _CORE_ACTIOR_H_
#define _CORE_ACTIOR_H_

#include <CoreTypes.h>
#include <CoreObject.h>

class Actor
{
public:
	// Constructor
	Actor( void );
	
	// Destructor
	virtual ~Actor( void );

public:
	virtual void	visitor( CoreObject* object ) =0; 

	// set_functions and get_functions.
public:
	void	setDataType( int type );
	int		getDataType( void );
	
	void	setCoreType( int type );
	int		getCoreType( void );

	void	setAnnotation( std::string annot );
	const char*	getAnnotation( void );
	

	void setEnable( bool enable );
	bool getEnable( void );

			
protected:
	static int m_noOfObject;	
	int m_datatype;
	int m_coretype;
	
	bool m_enable;
	std::string m_annotation;
			
private:
	int m_id;
	
};
 
#endif
