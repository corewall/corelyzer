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

#ifndef _CORE_HOLE_H_
#define _CORE_HOLE_H_

#include <vector>
#include <string>
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

	virtual void	init( int type = ALL_TIE, bool fromFile = false );
	void	init( int type, int coretype, char* annotation, bool fromFile = false );
	virtual void	reset( void );
	
	virtual void setRange( data_range range );

	virtual void getTuple( std::string& data, int type );
	virtual void	copy(  CoreObject* objectptr );	
	void	copyData(CoreObject* objectptr );	
	
	void    setType( int type );
	int     getType( void );
	std::string getTypeStr();

public:
	Core*	createCore( int index );
	void	deleteCore( int index );
	
	void	reset( int type );
	int		validate( void );
		
#ifdef DEBUG	
	void debugPrintOut( void );
#endif
	
	// set_functions and get_functions.
public:
	int		getNumber( void );
	
	Core*	getCore( int index );
	Core*	getCoreByNo( int index );	
	Core*	getCore( double basedepth, double offset );
	int		getNumOfCores( void );
	
	void	setName( char* name );
	const char*	getName( void );
	
	void	setELDStatus( bool status );
	bool	getELDStatus( void );

	virtual const char*	getSite( void );
	virtual const char*	getLeg( void );
	
	void	setDataFormat( int format );

	int		getDataFormat( void );
	int		check(char* leg, char* site , char* holename =NULL, int corenumber =-1,  char* section = NULL);			
	
	void	setTopAvailable( bool status );
	bool	getTopAvailable( void );
	
protected:
	std::vector<Core*> m_cores;
	bool m_eldStatus;

private:
	int	m_number;
	std::string m_name;
 	int  	m_type;
	bool m_topAvailable;
	
	int m_dataFormat;	
	
};

#endif
