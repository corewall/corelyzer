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

#ifndef _CORE_CORRELATOR_H_
#define _CORE_CORRELATOR_H_

#include <vector>

#include <Data.h>
#include <Hole.h>
#include <Tie.h>
#include <Core.h>
#include <Value.h>

class Correlator 
{
public:
	// Constructor
	Correlator( void );
	Correlator( Data* dataptr );
		
	// Destructor
	virtual ~Correlator( void );

public:
	void init( int type = QUALITY );
	void initSplice( void );
	void resetELD( void );

	void assignData( Data* dataptr );
	
	double repickDepth( int coreid, double relativepos );
	
	double composite(char* holeA, int coreidA, double posA, char* holeB, int coreidB, double posB, int coretype, char* annot);
	double compositeBelow(char* holeA, int coreidA, double posA, char* holeB, int coreidB, double posB, int coretype, char* annot);
	int composite( char* holeA, int coreidA, double offset, int coretype, char* annot );	
	int compositeBelow( char* holeA, int coreidA, double offset, int coretype, char* annot );

	double project(char *hole, int core, int datatype, char* annot, float offset);

	int splice( char* hole, int coreid, int type, char* annot, bool append = false );
	int splice( Core* source, bool append = false );
	//int splice( int id, char holeA, int coreidA, double posA, char holeB, int coreidB, double posB, int type, bool append = false );
	int splice( int tie_id, int typeA, char* annotA, char* holeA, int coreidA, double posA, int typeB, char* annotB, char* holeB, int coreidB, double posB, bool append = false );
	
	int splice( int tie_id, Core* coreA, Core* coreB, double posA, double posB, bool append = false, bool appendbegin =false );

	// brg 5/19/2014: unused
	//int splice( int coreidA, double relativeposA, int coreidB, double relativeposB );
	//int splice( int tieindex, int coreidA, double relativeposA, int coreidB, double relativeposB );	

	int splice( void);
	
	int appendSplice( bool allflag );
	int appendSelectedSplice(int type, char* annot, char* hole, int coreid);
	int undoAppendSplice(void);
	int deleteSplice(int tieid);
	void deleteAllSplice(void);
	
	double getRate(double depth);

	void generateSpliceHole( Hole* newSpliceHole = NULL );
	void generateAltSpliceHole( std::string& data ); 
	void initiateAltSplice( void );

	void generateSaganHole( void );

	void generateSagan(void);
	
	void setAgeSeries(int idx, double depth, char* code);

	//void squish(char holeA, int coreidA, double posA, char holeB, int coreidB, double posB);
	void squish( char* hole, int coreid, double squish );
	void squish( int coreid, double squish );
	
	int sagan(int tieindex, char* holeA, int coreidA, double posA, char* holeB, int coreidB, double posB);
	int deleteSagan(int tieid);

	int evalCore(int coretypeA, char* annotA, char* holeA, int coreidA, double posA, int coretypeB, char* annotB, char* holeB, int coreidB, double posB);
	int evalSpliceCore(int coretypeB, char* annotB, char* holeB, int coreidB, double posB, double posA);	
	int evalCoreLog(char* holeA, int coreidA, double posA, double posB);	

	void interporlate( void );

	void createSpliceHole( void );
	
	int deleteTie( int tieindex );
	
	int undo(void);
	int undoAffineShift(char * hole, const int coreid);
	//int undoAbove(char* hole, int coreid);

	void update( void );

	void getTuple( std::string& data, int type = SPLICEDATA ); 
	void getTuple( std::string& data, int hole_no, int type);
	void getSpliceTuple( std::string& data ); 
	void getSaganTuple( std::string& data, int fileflag = false ); 

	void getCoefList( std::string& data );
	int getSectionAtDepth(char *hole, int core, int coretype, double depth);
	
	int setCoreQuality( char* hole, int core, int coretype, int quality, char* annot = NULL );
	
#ifdef DEBUG	
	void debugPrintOut( void );
#endif
	
public:
	Data*	getDataPtr( void );
	Hole*	getSpliceHole( void );
	Hole*	getHolePtr( int type );
	
	void	setSpliceType( int type );
	int		getSpliceType( void );
	
	void	setDepthStep( double step );
	double	getDepthStep( void );
	
	void	setWinLen( double winlen );
	double	getWinLen( void );
	
	void	setLeadLagMeters( double lag );
	double	getLeadLagMeters( void );
	
	void	setLogData(Data* dataptr);
	Data*   getLogData( void );
	void	clearLogData( int flag = 0 );
	void	clearStratData( void );	

	double	getDataAtDepth( char* hole, int coreid, double depth, int coretype, char* annot );	
	
	Value*	findValue( Core* coreptr, double top, int idx =0 );
	
	void	setCoreOnly( bool flag );
	void	makeUpELD( void );
	void	setFloating( bool flag );
	
	void	applyFilter( Actor* filter );
	const char* getSaganCoreName(void);
	
	Value*	getTiePoint( void );
	void updateTies(void);
	
	double getData(Value* prev_valueptr, Value* next_valueptr, double depth);
	double getELDData(Value* prev_valueptr, Value* next_valueptr, double depth);
	double getMbsfData(Value* prev_valueptr, Value* next_valueptr, double depth);

	double getTop(Value* prev_valueptr, Value* next_valueptr, double depth);
	
protected:
	Tie* createTie( int type, Core* coreA, double posA, Core* coreB, double posB );
	//Tie* createTie( int type, int coreidA, double relativeposA, int coreidB, double relativeposB );
	//Tie* createTie( int type, int tieindex, int coreidA, double relativeposA, int coreidB, double relativeposB );	

	Core* findCore( int index );
	Core* findCoreinSplice( int index );
	Core* findCore( char* holeName, int coreId, int index =0 );
	Core* findCore( int type, const char* holeName, int coreId, const char* annot = NULL, int index =0 );
	Tie* findTie( int order, bool alternative = false );
	Value* findValueInHole( Hole* holeptr, double depth );
	
	Value*	findValueWithMcd( Core* coreptr, double top, int idx =0 );
	Value*	findValueWithMbsf( Core* coreptr, double top, int idx =0 );


	int evalCore(Core* coreptrA, double posA, Core* coreptrB, double posB);
	double evalCore( Core* coreA, Core* coreB, Value* valueA, Value* valueB, double endpoint, Value* valueEndA );
	double evalCore(Core* fixedCore, Value* fixedValue, double fixedStart, Core* coreptr, double startdepth, int numlags, double gab);

	Value* assignAboveValues( Core* coreptr, Core* source, Value* valueptr, bool isConstrained, double tie_depth, double offset=0.0f);
	Value* assignBelowValues( Core* coreptr, Core* source, Value* valueptr, Value* lastvalue, bool isConstrained, double tie_depth, double offset=0.0f);
	Value* assignBetweenValues( Core* coreptr, Core* source, Value* valueptrA, Value* valueptrB, Value* lastvalue, bool isConstrained, double tie_depth, double offset=0.0f);
	Value* assignBetweenValues( Core* source, Value* valueptr, double gap);

	int assignShiftToValues(double offset);
	int assignShiftToValues(Core* coreptr, double offset);	
 	int assignRateToValues(Hole* holeptr, Core* coreptrA, Core* coreptrB, Value* valueptrA, Value* valueptrB, double rate, double b);

	void updateSaganTies(void);
	
	void addtoSaganTotalTie(Tie* tieptr, double pos);

	
protected:
	std::vector<Hole*> m_splicerholes;
	std::vector<Tie*> m_splicerties;
	Hole* m_mainHole[3]; // brg 4/28/2014 hole 0 = splice hole? seems like hole 1 is also used as splice hole at times...
	Data m_correlatorData;
	Value* m_tiepoint;
	
	std::vector<Tie*> m_ties;
	std::vector<Tie*> m_saganties;
	std::vector<Tie*> m_sagantotalties;	
	std::vector<double> m_coeflist;
	
	Data* m_dataptr;
	Data* m_logdataptr;
	bool m_floating;
	
	std::string m_sagan_corename;
	int	m_spliceType;
	double m_depthstep;
	double m_winlen;
	double m_leadlagmeters;
	int m_spliceOrder;

	bool m_belowflag;
	bool m_isCoreOnly;
	Core* m_givenCore;
	

private:
	
};

#endif
