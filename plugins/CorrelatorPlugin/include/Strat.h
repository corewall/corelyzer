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

#ifndef _CORE_STRAT_H_
#define _CORE_STRAT_H_

#include <string>

#include <CoreTypes.h>
#include <CoreObject.h>

class Core;

class Strat : public CoreObject
{
public:
	// Constructor
	Strat( char* name, char* code, double topage, double botage );

	// Destructor
	virtual ~Strat( void );
protected:
	Strat( void );
	
public:
	virtual void	accept( Actor* flt ); 
	virtual void	update( void );

	virtual void	init( void ) { };
	virtual void	reset( void ) { };

	virtual void setRange( data_range range ) { };

#ifdef DEBUG	
	void debugPrintOut( void );
#endif

	virtual void getTuple( std::string& data, int type );

public:
	Strat* clone( void );

protected:
	double	calcSedRate( Strat* nextStrat );
	
	// set_functions and get_functions.
public:
	const char*   getDatumName( void );
	const char*   getCode( void );
	double  getTopAge( void );
	double  getBotAge( void );

	int		applyTopAffine( double offset );
	int		applyBotAffine( double offset );

	void	setTopType( char type );
	void	setBotType( char type );
	char	getTopType( void );
	char	getBotType( void );

	void	setTopSection( char* section );
	void	setBotSection( char* section );
	char*	getTopSection( void );
	char*	getBotSection( void );

	void	setTopMbsf( double mbsf );
	void	setBotMbsf( double mbsf );
	double	getTopMbsf( void );
	double	getBotMbsf( void );

	void	setTopInterval( double interval );
	void	setBotInterval( double interval );
	double	getTopInterval( void );
	double	getBotInterval( void );
	
	void	setTopCoreId( int coreid );
	void	setBotCoreId( int coreid );
	int		getTopCoreId( void );
	int		getBotCoreId( void );
	int		getTopCoreNumber( void );
	int		getBotCoreNumber( void );	
	
	int		getDataFormat( void ) { return -1; };

	void	setDataType( int type );
	int		getDataType( void );
	
	void	setTopCore( Core* coreptr );
	void	setBotCore( Core* coreptr );	
	const char*	getHoleName( void );
	
	void	setOrder(int order);
	int		getOrder( void );

protected:		
	std::string m_name;
	std::string m_code;
	int m_order;
	int	m_dataType;
	double m_age[2];
	Core* m_core[2];
	double m_sedrate;
	
	int  m_connectedCoreId[2]; 
	
	char m_valuetype[2];
	char m_section[2][2];
	
	double m_interval[2];
	double m_mbsf[2];
	double m_rawmbsf[2];

	
private:

};

#endif
