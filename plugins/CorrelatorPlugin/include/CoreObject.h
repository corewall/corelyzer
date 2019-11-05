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

#ifndef _CORE_OBJECT_H_
#define _CORE_OBJECT_H_

#include "CoreTypes.h"
#include <string>

class Actor;

class data_range 
{
public:
	double top;
	double bottom;
	double mindepth;
	double maxdepth;
	double maxdepthdiff;
	double min;			// data
	double max;			// with data
	double realminvar;
	double realmaxvar;
	double avedepstep;
	double ratio;

	data_range(void);
	~data_range(void) {};
public:
	void init(void);
};

class CoreObject
{
public:
	// Constructor
	CoreObject( void );
	CoreObject( CoreObject* parent );
	
	// Destructor
	virtual ~CoreObject( void );

public:
	virtual void	accept( Actor* flt ) =0; 
	virtual void	update( void ) =0;
	
	virtual int		getDataFormat( void ) =0;
	virtual int		check(char* leg, char* site , char* holename =NULL, int corenumber =-1,  char* section = NULL)  { return 0; } ;

	virtual void setRange( data_range range ) { } ;

	virtual void getTuple( std::string& data, int type ) { };
	void copyRange( CoreObject* objectptr );	

	virtual const char*	getSite( void ) { return NULL; };
	virtual const char*	getLeg( void ) { return NULL; };
	virtual const char*	getName( void ) { return NULL; };			
	// set_functions and get_functions.
public:
	unsigned int	getId( void );
	void setId(unsigned int nId);
	void setRange( void );
	void initRange( void );

	data_range*	getRange( void );
	void	updateRange( void );

	double	getTop( void );
	double	getBottom( void );
	double getMin( void );
	double getMax( void );
	double	getMinDepth( void );
	double	getMaxDepth( void );

	void	setTop( double top );
		
	double	getRatio( void );
		
	void setUpdate( void );
	
	void setParent( CoreObject* parent );
	CoreObject* getParent( void );
	
	void setAnnotation( char* annotation );
	const char* getAnnotation( void );
	
protected:
	static unsigned int m_noOfObject;	
	
	CoreObject* m_parent;

	data_range m_range;
	double m_timestamp;		

	bool m_updated;
	bool m_dataValidation;
	
	std::string m_annotation;
	
	
private:
	unsigned int m_id;
	
};

inline data_range*	CoreObject::getRange( void ) 
{ 
	return &m_range; 
}

inline double	CoreObject::getTop( void ) 
{ 
	return m_range.top; 
}

inline double CoreObject::getBottom( void )  
{ 
	return m_range.bottom; 
}

inline double CoreObject::getMinDepth( void )
{ 
	return m_range.mindepth; 
}

inline double CoreObject::getMaxDepth( void )
{ 
	return m_range.maxdepth; 
}
	
inline double CoreObject::getMin( void ) 
{ 
	return m_range.min; 
}

inline double CoreObject::getMax( void ) 
{ 
	return m_range.max; 
}

inline double CoreObject::getRatio( void ) 
{ 
	return m_range.ratio; 
}
 
#endif
