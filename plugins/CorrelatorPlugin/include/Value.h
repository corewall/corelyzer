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

#ifndef _CORE_VALUE_H_
#define _CORE_VALUE_H_

#include <CoreObject.h>
 
class Value : public CoreObject
{
public:
	// Constructor
	Value( int index, CoreObject* parent );

	// Destructor
	virtual ~Value( void );
	
public:
	virtual void	accept( Actor* flt ); 
	virtual void	update( void );

	virtual void	init( int type = ALL_TIE );
	virtual void	reset( void );
	
	virtual void setRange( data_range range );	
	
	virtual void	copy( CoreObject* objectptr );

	void getTuple( std::string& data, int type ); 

#ifdef DEBUG	
	virtual void debugPrintOut( void );
#endif	
	
public:
	int		applyAffine( double offset, char type );
	
	// set_functions and get_functions.
public:
	int		getNumber( void );
	
	void	setSection( char* value );
	char*	getSection( void );
	
	void	setRawData( double value );
	double	getRawData( void );
	
	void	setData( double value );
	double	getData( void );
	
	void	setType( char type );
	char	getType( void );
	
	void	setDepth( double depth );
	double	getDepth( void );
	
	void	setRunNo( int num );
	int		getRunNo( void );
	
	void	setOtherOdp( double value );
	double	getOtherOdp( void );
	
	void	setMbsf( double mbsf );
	double	getMbsf( void );
	
	void	setELD( double eld );
	double	getELD( void );
	
	void	setMcd( double mcd );
	double	getMcd( void );
		
	int		getValueType( void );
	
	void	setQuality( int quality );
	int		getQuality( void );

	void	setSmoothStatus( int status );
	int		getSmoothStatus( void );
	
	void	setStretch( double stretch );
	double	getStretch( void );
	
	void	setStretch( double rate, double b );
	double  getRate( void );
	double	getB( void );
		
	int		getDataFormat( void );
	int		getCoreNumber( void );	

	int		check(int leg, int site , char holename =-1, int corenumber =-1,  char* section = NULL);			

protected:	
	char m_type;
	int m_affine_status;
	int m_corr_status;
	int m_manual_corr_status;
		
	int m_quality;
	int m_valuetype;
	
	int m_smoothStatus;
	
	double m_data[2];
	
	int m_runNumber;

	double m_otherodp;
	// depth data that entered first
	double m_depth;
	// depth data : meters below seafloor
	double m_mbsf;
	// depth data : meters composite / from splicer
	double m_mcd;
	// depth data : equivalent log depth from core-log correlation
	double m_eld;
	
	double m_stretch;	
	double m_rate;
	double m_b;
	char m_section[2];
		
private:
	int	m_number;	
			
};

#endif
