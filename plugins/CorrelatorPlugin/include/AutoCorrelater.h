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

#ifndef _CORE_AUTOCORRELATER_H_
#define _CORE_AUTOCORRELATER_H_

#include <vector>

#include <Correlater.h>
#include <Tie.h>
#include <Core.h>
#include <Hole.h>

class AutoCorrResult 
{
public:
	double m_coef;
	double m_squish;
	double m_offset;
	Hole* m_holeptrA;
	Hole* m_holeptrB;
	double m_rate;
	double m_b;
public:
	AutoCorrResult( void ) : m_coef(1.0f), m_squish(100.0f), m_offset(0.0f), m_rate(0), m_b(0), m_holeptrA(NULL), m_holeptrB(NULL) { };
	AutoCorrResult( Hole* a, Hole* b, double coef, double m_rate, double m_b, double squish, double offset ) : 
					m_coef(coef), m_squish(squish), m_offset(offset), m_rate(m_rate), m_b(m_b),  m_holeptrA(a), m_holeptrB(b) { };
	~AutoCorrResult( void ) { };
};

class AutoCorrelater 
{
public:
	// Constructor
	AutoCorrelater( void );
	AutoCorrelater( Correlater* ptr, int tiesinCore );
		
	// Destructor
	virtual ~AutoCorrelater( void );

public:
	virtual void	visitor( CoreObject* object ); 

public:
	void init( void );

	int correlate( void );
	int correlate( float squish, float offset, float& ret_rate, float& ret_b );
	
	int correlate_backup( void );
	
	void addHole( Hole* holeptr );
	AutoCorrResult* getBestCorrelate( void );
	
	int applyBest( void );
	int applyBestToAll( void );	
	int applyToAll(  double stretch, double mudline );	
	int applyToAll( void );
	int apply(  double stretch, double mudline );	
	
	int undo( void );
	int undoToAll( void );
	
protected:
	int updateTie(Tie* tieptr, Core* coreA, float relativeposA, Core* coreB, float relativeposB );	
	double evalCore( Core* coreA, Core* coreB, Value* valueA, Value* valueB, double squish, double offset, int indexgap, int logindexgap, double& bnum );
	
	int apply( double appliedOffset, double stretch, double mudline, bool doflag = true);
	int applyToAll( double appliedOffset, double stretch, double mudline, bool doflag = true );


	// set_functions and get_functions.
public:
	void	setSquishRange( double from, double to, double inc =0.0f );
	void	setOffsetRange( double from, double to, double inc =0.0f );

	void	setCorrelater( Correlater* ptr );
	int		setCorrelate( void );
	
	void	setFlip( int flip );
	void	setTiesNo( int tiesinCore );
	
	void	addHole( char holeName );
	void	clearHoles();
	void	setDepthStep( double depthstep );
	
	double	evalCore( Core* coreA, Core* coreB, double squish, double offset, double& rate, double& b );
	

protected:
	double m_range_squish[2];
	double m_squish_inc;
	double m_nsquish;
	double m_range_offset[2];
	double m_offset_inc;
	double m_noffset;
	double m_depth_step;

	int m_noTiesinCore;
	int m_flip;
	
	int m_coreStartNum;
	int m_coreMaxNum;

	Correlater* m_correlater;
	int	m_isReady;
	
	std::vector<char> m_holes;
	std::vector<AutoCorrResult> m_result; //??
	AutoCorrResult m_best_result;
	
	std::vector<Hole*> m_holelist;
	Hole* m_mainHoleptr;

	std::vector<Core*> m_corelist;
	Core* m_coreptr[2];
	std::vector<Tie*> m_ties;
	bool m_isSplice;

private:
	
};
 
#endif
