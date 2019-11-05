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

#ifndef _CORE_CULL_FILTER_H_
#define _CORE_CULL_FILTER_H_

#include <vector>
#include <string>
#include <Actor.h>
#include <Data.h>

class Equation
{
public:
	Equation( void ) { };
	~Equation( void ) { };
public:
	double	m_value[2];
	int		m_sign[2];
	int		m_join;
};

class HashInfo 
{
public:
	HashInfo( void ) { };
	~HashInfo( void ) { };
public:
	std::string m_leg;
	int m_subleg;
	std::string m_site;
	std::string m_hole;
	int m_core;
	char m_type;
	int m_value[2];
	char m_section[2];
	double m_top;
	bool m_badcore;
};

class CullFilter : public Actor
{
public:
	// Constructor
	CullFilter( double top = 5.0f );	
	CullFilter( char* filename );
		
	// Destructor
	virtual ~CullFilter( void );

public:
	virtual void	visitor( CoreObject* object ); 

public:
	void init( void );
	int create( char* filename );
	
	void destory( void );
	
	int openCullTable( char* filename, char* type );
	int closeCullTable( void );
	
	// set_functions and get_functions.
public:
	void setCullTop( int top );
	int	getCullTop( void );
	
	void setCullCoreNo( int number );
	int getCullCoreNo( void );
	
	void setCullSignal( int signal );
	int getCullSignal( void );
	
	void setMethod( int method );
	int getMethod( void );
					
	void setEquation( double value1, double value2, int sign1, int sign2, int join );
	bool isUserEquation( void );
	Equation* getEquation( void );
	
protected:
	int	evalEquation(double data, Equation* cull_equation);
	int	evalEquationAlt(double data, Equation* cull_equation);

protected:
	int m_method;
	double m_top;
	int m_byNumber;
	int m_count;
	int m_signalMax;
	std::vector<HashInfo*> m_hashtable;
	std::string m_appliedType;
	int m_nType;
		
	bool m_userEquation;

	Equation m_cull_equation;
	Equation m_gra;
	Equation m_pwave;
	Equation m_susceptibility;
	Equation m_naturalgamma;
	Equation m_reflectance;
	Equation m_otherodp;
private:
	FILE * m_fptr;
	
};
 
#endif
