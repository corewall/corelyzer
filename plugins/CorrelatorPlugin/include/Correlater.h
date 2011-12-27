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

#ifndef _CORE_CORRELATER_H_
#define _CORE_CORRELATER_H_

#include <vector>

#include <Data.h>
#include <Hole.h>
#include <Tie.h>
#include <Core.h>
#include <Value.h>

class Correlater 
{
public:
	// Constructor
	Correlater( void );
	Correlater( Data* dataptr );
		
	// Destructor
	virtual ~Correlater( void );

public:
	void init( int type = ALL_TIE );

	void assignData( Data* dataptr );
	
	double repickDepth( int coreid, float relativepos );
	
	double composite(char holeA, int coreidA, float posA, char holeB, int coreidB, float posB);
	double compositeBelow(char holeA, int coreidA, float posA, char holeB, int coreidB, float posB);
	int composite( int coreidA, float relativeposA, int coreidB, float relativeposB );
	int composite( int tieindex, int coreidA, float relativeposA, int coreidB, float relativeposB );	

	int splice( char hole, int coreid );
	int splice( Core* source );
	int splice( int id, char holeA, int coreidA, float posA, char holeB, int coreidB, float posB );
	int splice( int id, Core* coreA, Core* coreB, float posA, float posB );
	int splice( int coreidA, float relativeposA, int coreidB, float relativeposB );
	int splice( int tieindex, int coreidA, float relativeposA, int coreidB, float relativeposB );	
	int deleteSplice(int tieid);
	void deleteAllSplice(void);

	void generateSpliceHole( void );
	void generateSaganHole( void );

	void generateSagan(void);

	//void squish(char holeA, int coreidA, float posA, char holeB, int coreidB, float posB);
	void squish( char hole, int coreid, double squish );
	void squish( int coreid, double squish );
	
	int sagan(int tieindex, char holeA, int coreidA, float posA, char holeB, int coreidB, float posB);
	int sagan( void );

	int evalCore(char holeA, int coreidA, float posA, char holeB, int coreidB, float posB);

	void interporlate( void );

	void createSpliceHole( void );
	
	int deleteTie( int tieindex );
	
	int undo(void);
	int undoAbove(char hole, int coreid);

	void update( void );

	void getTuple( std::string& data, int type = SPLICEDATA ); 
	void getSpliceTuple( std::string& data ); 
	void getCoefList( std::string& data );
	
#ifdef DEBUG	
	void debugPrintOut( void );
#endif
	
public:
	Data*	getDataPtr( void );
	Hole*	getSpliceHole( void );
	
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
	
	Value*	findValue( Core* coreptr, double top );
	void	setCoreOnly( bool flag );
	
protected:
	Tie* createTie( int type, Core* coreA, float posA, Core* coreB, float posB );
	Tie* createTie( int type, int coreidA, float relativeposA, int coreidB, float relativeposB );
	Tie* createTie( int type, int tieindex, int coreidA, float relativeposA, int coreidB, float relativeposB );	

	Core* findCore( int index );
	Core* findCoreinSplice( int index );
	Core* findCore( char holeName, int coreId );
	Tie* findTie( int order );


	double evalCore( Core* coreA, Core* coreB, Value* valueA, Value* valueB, double endpoint, Value* valueEndA );

	Value* assignAboveValues( Core* coreptr, Core* source, Value* valueptr, double offset=0.0f);
	Value* assignBelowValues( Core* coreptr, Core* source, Value* valueptr, Value* lastvalue, double offset=0.0f);
	Value* assignBetweenValues( Core* coreptr, Core* source, Value* valueptrA, Value* valueptrB, Value* lastvalue, double offset=0.0f);
	Value* assignBetweenValues( Core* source, Value* valueptr, double gap);

	int assignShiftToValues(double offset);
	int assignShiftToValues(Core* coreptr, double offset);	
	int assignRateToValues(Core* coreptrA, Core* coreptrB, Value* valueptrA, Value* valueptrB, double rate, double &b);

protected:
	std::vector<Hole*> m_splicerholes;
	std::vector<Tie*> m_splicerties;
	Hole* m_mainHole[2];
	Data m_correlatorData;
	
	std::vector<Tie*> m_ties;
	std::vector<Tie*> m_saganties;
	std::vector<double> m_coeflist;
	
	Data* m_dataptr;
	Data* m_logdataptr;
	
	int	m_spliceType;
	double m_depthstep;
	double m_winlen;
	double m_leadlagmeters;
	int m_spliceOrder;

	bool m_belowflag;
	bool m_isCoreOnly;
	

private:
	
};
 
#endif
