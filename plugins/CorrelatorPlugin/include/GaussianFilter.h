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

#ifndef _CORE_GAUSSIAN_FILTER_H_
#define _CORE_GAUSSIAN_FILTER_H_

#include <Actor.h>

class GaussianFilter : public Actor
{
public:
	// Constructor
	GaussianFilter( int value = 9, int unit = POINTS );
		
	// Destructor
	virtual ~GaussianFilter( void );

public:
	virtual void	visitor( CoreObject* object ); 

public:
	void init( void );
	void setWidth( int width );
	int getWidth( void );
	
	void setUnit( int unit );
	int getUnit( void );

	void setDepth( int depth, int max );
	int getDepth( void );
	
	void setPlot( int plot );
	int getPlot( void );
	
protected:
	void visitorPoints( CoreObject* object ); 
	void visitorDepth( CoreObject* object ); 
	
	void getGaussWeights(int width, double* weight);
	
	void useDepth( void );
	void useMbsf( void );
		
	// set_functions and get_functions.
public:
	
protected:
	int m_plot;
	
	int m_width;
	int m_unit;
	int m_maxdepth;
	int m_depth;
	
	int m_usedepth;
	
	double* m_weight;
	double* m_datptr;
	double* m_sm_datptr;
	
private:
	
};
 
#endif
