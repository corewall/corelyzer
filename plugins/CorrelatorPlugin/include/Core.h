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

#ifndef _CORE_CORE_H_
#define _CORE_CORE_H_

#include <string>
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

	virtual void	init( int type = ALL_TIE, bool fromFile = false );
	virtual void	reset( void );
	
	virtual void setRange( data_range range );	

	virtual void getTuple( std::string& data, int type );
	virtual void	copy( CoreObject* objectptr );
	
public:
	Value*	createValue( int index );
	Value*	createValue( int index, double depth );
	Value*	createInterpolatedValue( double depth );
	int	deleteInterpolatedValue( int number );
	
	Section*	createSection( int index );
	void	deleteSection( int index );
	void	deleteValue( int index );
	void	updateLight( void );

	void	copyData(CoreObject* objectptr );	
	
	void	reset( int type );
			
	int		validate( void );
		
#ifdef DEBUG	
	void debugPrintOut( void );
#endif

protected:
	int		calcStdDevMean( int off = 0 );
	int		findAveDepth( int off = 3 );

	int		checkDepthOrder( void );

	void 		cleanTies( int type = ALL_TIE, bool fromFile = false );

private:
	Value* createTypedValue(const int index);
	
	// set_functions and get_functions.
public:
	void	setNumber( int index );
	int		getNumber( void );

	Value*	getValue( int index );
	Value*	getValue( int section_id, double top );
	int		getNumOfValues( void );

	Section* getSection( int index );
	int		getNumOfSections( void );
	
	void	setType( int type );
	int		getType( void );
	
	char	getCoreType( void );
	
	Value*	getLast(void);
	Value*  getFirst(void);
		
	void	setDepthOffset( double offset, bool applied = false, bool fromfile = false );
	void	setDepthOffset( double offset, char valuetype, bool applied = false, bool fromfile = false );
	double	getDepthOffset( void );
	double	getRawDepthOffset( void );
	void	initDepthOffset( void );
	
	int 	getAffineStatus( void );
	int 	getRawAffineStatus( void );
	int		getAffineType( void );
	void	setAffineType( int type );
		
	void	setSmoothStatus( int status );
	int		getSmoothStatus( void );

	void	setStretch( double stretch );
	double	getStretch( void );
	
	void	setMudLine( double line );
	double	getMudLine( void );
	
	double	getStd( void );
	
	void	refGood( void );
	void	unrefGood( void );
	
	void	undo( bool fromfile = false );

	bool 	isShiftbyTie();
	void	setOffsetByAboveTie(double offset, char valuetype);
	
	int		addTie( Tie* object );
	int		deleteTie( int index );
	int		deleteTies( int type );
	Tie*	getTie( int index );	
	Tie*	getTie( int type, int index );		
	int		getNumOfTies( void );
	int		getNumOfTies( int type );
	void	disableTies( void );

	int		addStrat( Strat* object );
	int		getNumOfStrat( void );	
	int		deleteStrat( int index );
	int		deleteStrat( void );
	Strat*  getStrat( int index );
	
	virtual const char* getName( void );
	virtual const char* getSite( void );
	virtual const char* getLeg( void );
	
	int		getDataFormat( void );
	int		check(char* leg, char* site , char* holename =NULL, int corenumber =-1,  char* section = NULL);	
	
	void	setLogValue( Value* valueptr );
	Value*	getLogValue( void );
	
	void	setQuality( int quality );
	int		getQuality( void );

	void setComment(const std::string &comment);
	std::string getComment();

	void setAffineDatatype(const std::string &type);
	std::string getAffineDatatype();

protected:
	std::vector<Value*> m_values;
	std::vector<Tie*> m_ties;
	std::vector<Strat*> m_strats;
	std::vector<Section*> m_sections;
	
	struct data_affine m_affine[2]; // index 0 refers to file (aka "raw") data, 1 to in-memory data
	
	int m_type;
	int m_smoothStatus;	
	
	int m_goodRef;
	int	m_quality;
	bool m_cull_tabled;
	
	double m_stddev;
	double m_mean;
	
	bool m_shiftByaboveCore;

	double m_stretch;
	double m_mudline;
	
	Value* m_logValueptr;

	//???
	//coreptr->cum_dep_offset=0.0;
	//coreptr->did_offset=False;
			
private:
	int	m_number;
	std::string m_affineDatatype; // name of datatype used to create affine shift
	std::string m_comment;

};

#endif
