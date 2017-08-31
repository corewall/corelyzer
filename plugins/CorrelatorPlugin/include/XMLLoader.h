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

#ifndef _CORE_XML_LOADER_H_
#define _CORE_XML_LOADER_H_

#include <xercesc/sax/HandlerBase.hpp>
#include <xercesc/sax/AttributeList.hpp>
#include <stack>

#include <Data.h>
#include <Hole.h>
#include <Core.h>
#include <Tie.h>

using namespace xercesc;

enum XML_ELEMTYPE {
	XML_NONE         = 0, 
	XML_CORRELATOR   = 1, 
	XML_DATA         = 2,
	XML_HOLE         = 4,
	XML_CORE         = 5,
	XML_TIE			 = 6,
};


class CXMLElement
{
	// constuctors & destructor	
public:
	CXMLElement(char *name, AttributeList *attrib, XML_ELEMTYPE type, bool delobj = false);
	~CXMLElement();

	// functions
public:
	bool			getValue( char *name, unsigned long &value );
	bool			getValuei( char *name, int &value );
	bool			getValue( char *name, double &value );
	bool			getValue( char *name, char ** value );
	bool			getValueb( char *name, bool &value );

	// variables
public:
	char*			_strName;
	AttributeList*	_attributeList;
	XML_ELEMTYPE	_nType;
	void*			_pObject;
	char*			_strObjectName;
	
private:
	bool			_pDeleteObj;
};


class CXMLLoader : public HandlerBase
{
public:
					CXMLLoader(Data* dataptr);
	virtual			~CXMLLoader();
	
	// static function
public:
	static char		*binToChar( const XMLCh* const string );

	// virtual functions
public:
	virtual void	endDocument();
	virtual void	endElement(const XMLCh* const name);
	virtual void	startDocument();
	virtual void	startElement(const XMLCh* const name, AttributeList& attributes);
	virtual void	warning(const SAXParseException& exception);
	virtual void	error(const SAXParseException& exception);
	virtual void	fatalError(const SAXParseException& exception);

private :
	void	loadAffineCoreData(CXMLElement* pElem);
	void	loadSpliceCoreData(CXMLElement* pElem);
	void	loadELDCoreData(CXMLElement* pElem);	

		// variables
private :
	std::stack<CXMLElement*>	_stackEelementStack;
	unsigned long	_ulWarnings;
	unsigned long	_ulErrors;
	
	Data* _dataptr;
	Hole* _holeptr;
	Core* _coreptr;
	Tie* _tieptr;
	int _tieid;
	int _datatype;
	bool _found;
};

#endif
