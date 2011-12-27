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

#ifndef _CORE_HOLE_H_
#define _CORE_HOLE_H_

#include <vector>
#include <CoreObject.h>
#include <Core.h>
 
class Hole : public CoreObject
{
public:
	// Constructor
	Hole( int index, CoreObject* parent );

	// Destructor
	virtual ~Hole( void );
	
public:
	virtual void	accept( Actor* flt ); 
	virtual void	update( void );

	virtual void	init( int type = ALL_TIE );
	virtual void	reset( void );
	
	virtual void setRange( data_range range );

	virtual void getTuple( std::string& data, int type );
	virtual void	copy(  CoreObject* objectptr );	

        void    setType( int type );
        int     getType( void );

public:
	Core*	createCore( int index );

	int		validate( void );
	
#ifdef DEBUG	
	void debugPrintOut( void );
#endif
	
	// set_functions and get_functions.
public:
	int		getNumber( void );
	
	Core*	getCore( int index );
	Core*	getCore( double basedepth, double offset );
	int		getNumOfCores( void );
	
	void	setName( char name );
	char	getName( void );
	
	void	setELDStatus( bool status );
	bool	getELDStatus( void );

	int	getSite( void );
	int	getLeg( void );
	
	void	setDataFormat( int format );

	int		getDataFormat( void );
	int		check(int leg, int site , char holename =-1, int corenumber =-1,  char* section = NULL);			

protected:
	std::vector<Core*> m_cores;
	bool m_eldStatus;

private:
	int	m_number;
	char 	m_name;
 	int  	m_type;
	
	int m_dataFormat;	
	
};

#endif
