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

#ifndef _CORE_CORE_H_
#define _CORE_CORE_H_

#include <vector>
#include <CoreObject.h>

class Section;
class Tie;
class Value;
class Strat;
 
class Core : public CoreObject
{
public:
	// Constructor
	Core( int index, CoreObject* parent );

	// Destructor
	virtual ~Core( void );
	
public:
	virtual void	accept( Actor* flt ); 
	virtual void	update( void );

	virtual void	init( int type = ALL_TIE );
	virtual void	reset( void );
	
	virtual void setRange( data_range range );	

	virtual void getTuple( std::string& data, int type );
	virtual void	copy( CoreObject* objectptr );
	
public:
	Value*	createValue( int index );
	Section*	createSection( int index );
		
	int		validate( void );
	
#ifdef DEBUG	
	void debugPrintOut( void );
#endif

protected:
	int		calcStdDevMean( int off = 0 );
	int		findAveDepth( int off = 3 );

	int		checkDepthOrder( void );

	void 		cleanTies( int type = ALL_TIE );
	
	// set_functions and get_functions.
public:
	void	setNumber( int index );
	int		getNumber( void );

	Value*	getValue( int index );
	int		getNumOfValues( void );

	Section* getSection( int index );
	int		getNumOfSections( void );
	
	void	setType( int type );
	int		getType( void );
	
	char	getCoreType( void );
		
	void	setDepthOffset( double offset, bool applied = false, bool fromfile = false );
	void	setDepthOffset( double offset, char valuetype, bool applied = false, bool fromfile = false );
	double	getDepthOffset( void );
	double	getRawDepthOffset( void );
	void	initDepthOffset( void );
	
	int 	getAffineStatus( void );
	int 	getRawAffineStatus( void );
		
	void	setSmoothStatus( int status );
	int		getSmoothStatus( void );

	void	setStretch( double stretch );
	double	getStretch( void );
	
	void	setMudLine( double line );
	double	getMudLine( void );
	
	double	getStd( void );
	
	void	refGood( void );
	void	unrefGood( void );
	
	void	undo( void );

	bool 	isShiftbyTie();
	void	setOffsetByAboveTie(double offset, char valuetype);
	
	int		addTie( Tie* object );
	int		deleteTie( int index );
	Tie*	getTie( int index );	
	Tie*	getTie( int type, int index );		
	int		getNumOfTies( void );
	int		getNumOfTies( int type );

	int		addStrat( Strat* object );
	int		getNumOfStrat( void );	
	int		deleteStrat( int index );
	int		deleteStrat( void );
	Strat*  getStrat( int index );
	
	virtual char getName( void );
	virtual int	getSite( void );
	virtual int	getLeg( void );
	
	int		getDataFormat( void );
	int		check(int leg, int site , char holename =-1, int corenumber =-1,  char* section = NULL);			

protected:
	std::vector<Value*> m_values;
	std::vector<Tie*> m_ties;
	std::vector<Strat*> m_strats;
	std::vector<Section*> m_sections;
	
	struct data_affine m_affine[2];
	
	int m_type;
	int m_smoothStatus;	
	
	int m_goodRef;

	double m_stddev;
	double m_mean;
	
	bool m_shiftByaboveCore;

	double m_stretch;
	double m_mudline;
	//???
	//coreptr->cum_dep_offset=0.0;
	//coreptr->did_offset=False;
			
private:
	int	m_number;
	
};

#endif
