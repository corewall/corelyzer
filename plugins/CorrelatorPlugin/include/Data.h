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

#ifndef _CORE_DATA_H_
#define _CORE_DATA_H_

#include <vector>
#include <CoreObject.h>
#include <Hole.h>
 
class Data : public CoreObject
{
public:
	// Constructor
	Data( void );

	// Destructor
	virtual ~Data( void );
	
public:
	virtual void	accept( Actor* flt ); 
	virtual void	update( void );

	virtual void	init( int type = ALL_TIE, bool fromFile = false );
	void	init( int type, int coretype, char* annotation, bool fromFile = false );
	
	virtual void	reset( void );

	virtual void setRange( data_range range );
	
	virtual void getTuple( std::string& data, int type=DATA );

public:
	Hole*	createHole( int index, char* name, int type, char* annotation = NULL );
	Hole*	getHole( char* name );
	Hole*	getHole( char* name, int format, int datatype, char* annotation = NULL);
	Hole*	getHole( char* name, int datatype, char* annotation);
	Hole*	getHole( int index );
	Hole*	getHole( char* name, int index);
	void	remove( int datatype );

	void	reset( int type, int coretype, char* annotation );

	void	sort( void );

	int		validate( void );

	void	getTypeRange( int type, char* annot, double& min, double& max );

	
#ifdef DEBUG	
	void debugPrintOut( void );
#endif
	
	// set_functions and get_functions.
public:
	int		getNumOfHoles( void );
	int		getNumOfHoles( char* name );	
	
	void	setCorrStatus( int status );
	int		getCorrStatus( void );

	void	setAffineStatus( int status );
	int		getAffineStatus( void );
		
	void	setSite( char* site );
	virtual const char*	getSite( void );
	
	void	setLeg( char* leg );
	virtual const char*	getLeg( void );
	
	void	setSubLeg( int leg );
	int		getSubLeg( void );
	
	void	setType( int type );
	int		getType( void );
	
	int		getNumOfTypes(void);
	
	void	setFlip( int flip );
	int		isFlip( void );
	
	void	setDataFormat( int format );

	int		getDataFormat( void );
	int		check(char* leg, char* site , char* holename =NULL, int corenumber =-1,  char* section = NULL);

	void	setStretch( double stretch );
	double	getStretch( void );
	
	void	setMudLine( double line );
	double	getMudLine( void );

protected:
	bool checkType(int typeA, int typeB, const char* annotA, const char* annotB);
	
protected:	
	std::vector<Hole*> m_holes;
	int	m_type;
	int m_flip;
	int m_corr_status;
	int m_affine_status;
	//int m_mudline_status;
	
	double m_stretch;
	double m_mudline;
	
private:
	std::string m_site;		// site number
	std::string m_leg;		// leg  number	
	int m_subleg;
	
	int m_dataFormat;
};

#endif
