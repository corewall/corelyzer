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

#ifndef _CORE_TIE_H_
#define _CORE_TIE_H_

#include <CoreTypes.h>
#include <CoreObject.h>

class Core;
class Value;

class TieInfo
{
public:
	char m_valuetype;
	char m_section[2];

	double m_stretch;
	double m_mbsf;
	double m_mcd;
	double m_depth;
	double m_eld;
	
	double m_top;
	double m_bottom;

	Core* m_coreptr;
	Value* m_valueptr;
	
public:
	TieInfo( void );
	~TieInfo( void ) { };	
};

class Tie : public CoreObject
{
public:
	// Constructor
	Tie( Core* tiedcore, char valuetype, char* section, data_range range, double mbsf, double mcd, int type = REAL_TIE, bool fromFile = false );
	Tie( int type = REAL_TIE, bool fromFile = false );

	// Destructor
	virtual ~Tie( void );
	
public:
	virtual void	accept( Actor* flt ); 
	virtual void	update( void );

	virtual void	init( void ) { };
	virtual void	reset( void ) { };

	virtual void setRange( data_range range ) { };

	virtual void getTuple( std::string& data, int type );

#ifdef DEBUG	
	void debugPrintOut( void );
#endif

public:			
	int		applyAffine( double offset, char type, int iscoreto = 1 );
	int		applyAffine( double offset );

	void	calcEquation( Tie* tieptr );
	int		calcCoefficient( int off = 0 );
	int		calcCoefficientUpdate( int off = 0 );
	
	int		calcOffset( void );

	int		correlate( void );

	// set_functions and get_functions.
public:
	void	setTieTo( Core* coreptr, char valuetype, char* section, data_range range, double mbsf =0.0f, double mcd =0.0f );
	void	setTied( Core * coreptr, char valuetype, char* section, data_range range, double mbsf =0.0f, double mcd =0.0f );

	void	setTieTo( Value* valueptr);
	void	setTied( Value* valueptr);

	void	setTied( double mcd );
	
	Core*	getTieTo( void );
	Core*	getTied( void );
	Value*	getTiedValue( void );
	Value*	getTieToValue( void );

	void	setDummy( void ) { m_dummy = true; }
	
	TieInfo* getInfoTieTo( void );
	TieInfo* getInfoTied( void );
	
	void	setType( int type );
	int		getType( void ) ;
	
	void	setSharedFlag( bool flag );
	bool	getSharedFlag( void );
	
	void	setApply( int applied );
	
	void	setStretch( double stretch, int iscoreto = 1 );
	
	void	setRate( double rate );
	double	getRate( void ); 
	void	setB( double b );
	double	getB( void );
	void	setRateandB( double rate, double b );
	
	double	getCoef( void ); 
	
	int	getDataFormat( void ) { return -1; };
	bool	isFromFile( void );

	void setOrder(int order);
	int getOrder(void);

	void setConstrained(bool mode);
	bool isConstrained(void);

	double getOffset(void);
	
	void setStrightFlag(bool flag);
	bool getStrightFlag(void);
	
	void setFirstShiftFlag(bool flag);
	bool isFirstShift(void);
	
	void setAppend(bool flag, bool all = false);
	bool isAppend(void);
	bool isAll(void);
	
	// brgtodo 6/25/2014: isActive() never called, enable() never called,  m_active only
	// referred to in one case (TIE_SHIFT)
	void disable(void) { m_active= false; }
	void enable(void) { m_active = true; }
	bool isActive(void) { return m_active; }
	
	void addRefCount(void) { m_ref_count++; }
	int getRefCount(void) { return m_ref_count; } 
		
protected:
	Value*	findValue( int iscoreto );
	Value* findValue( Core* coreptr, double depth );
	
	int findValueNumber( Core* coreptr, double top );
	
	double evalCore( Core* coreA, Core* coreB, Value* valueA, Value* valueB );
	Value* createInterpolatedValue(Core* coreptr, double mcd, Value* valueptr);
	
protected:
	int	m_tietype;
	int m_affine_status;
	int m_corr_status;
	int m_applied;
	bool m_constrained;
	bool m_fromFile;
	bool m_active;
	bool m_dummy;
	
	double m_b;
	double m_rate;
	int m_ref_count;
	
	double m_coef;
	int m_nx;
	int m_lead_lag;
	double m_offset;
	double m_leadlag_in_meter;
	double m_cumOffset;
	
	bool m_sharedFlag;
	bool m_straightFlag;
	bool m_isFirstShift;
	bool m_isAppend;
	bool m_isAll;
	
	TieInfo m_tiedcore;
	TieInfo m_tieTocore;
			
	int m_status;
	int m_order;
private:

};

#endif
