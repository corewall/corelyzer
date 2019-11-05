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

#ifndef _CORE_SECTION_H_
#define _CORE_SECTION_H_

#include <vector>
#include <Value.h>


class Section : public CoreObject
{
public:
	// Constructor
	Section( int index, CoreObject* parent );

	// Destructor
	virtual ~Section( void );
	
public:
	virtual void	accept( Actor* flt ) { }; 
	virtual void	update( void );
	virtual int		getDataFormat( void ) { return 1; };
	virtual void	getTuple( std::string& data, int type );

	// set_functions and get_functions.
public:
	int		getNumber( void );
	void    setStartValue(Value* valueptr);
	void    setEndValue(Value* valueptr);
						
protected:
	Value* m_startValueptr;
	Value* m_endValueptr;
		
private:
	int	m_number;
	
};

#endif
