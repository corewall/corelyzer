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

#ifndef _CORE_DATAMANAGER_H_
#define _CORE_DATAMANAGER_H_

#include <vector>
#include <string>
#include <Data.h>

class CullFilter;
class DecimateFilter;
class GaussianFilter;

struct DataInfo 
{
	Data* m_dataptr;
	std::vector<std::string> m_coreDataFiles;
	std::vector<int> m_coreDataFormat;
	std::vector<int> m_coreDataType;
	
	std::string m_appliedCullFilename;
	std::string m_appliedAffineFilename;
	std::string m_appliedSpliceFilename;
	std::string m_appliedLogFilename;
	std::string m_appliedEldFilename;
	std::vector<std::string> m_startDataFiles;
	std::vector<int> m_stratDataType;
};

class DataManager
{
public:
	// Constructor
	DataManager( void );

	// Destructor
	virtual ~DataManager( void );
	
public:
	virtual void init( void );
	
	int		registerPath( char* paths );
	void	printPaths( void );
	
	Data*	load( const char* filename, Data* dataptr = NULL, char* annotation = NULL );
	Data*	loadAltSplice( const char* filename, Data* dataptr, int type, char* annotation = NULL);
	void	loadCullTable( const char* filename, int coretype, Data* dataptr, char* annotation = NULL );
	
	int		changeFormat(const char* infilename, const char* outfilename);

	
	int		save( char* filename, Data* dataptr, int format );
	int		save( char* filename, Data* dataptr );
	int		saveHole( char* filename, Hole* dataptr );
	int		save( char* filename, Data* dataptr, char* affinefile );

	int		save( char* read_filename, char* write_filename, int depth_idx, int data_idx );

	int		saveTimeSeries( char* agefilename, char* filename, Data* dataptr );
	int		saveTimeSeriesHole( char* agefilename, char* filename, Hole* dataptr );
			
	int		save( char* filename, Hole* dataptr, const char* leg, const char* site, bool age = false );
	
	void	setCoreFormat( int format );
	int		getCoreFormat( void );
	void	setCoreType( int format );
	int		getCoreType( void );
	
	void	setStratType( int type );
	int		getStratType( void );
	
	void	setCullFile( char* filename );
	void	cleanSplice(void);
	void	cleanLog(void);
	void	cleanStrat(void);
	void	cleanCore(Data* dataptr, int datatype);
		
	Data*	getLogData( void );
	std::string* getLogInfo( void );
	void	setSelectedColumn( int column );

protected:	
	int			valifyData( char* filename );
	std::string	verifyFileReadable( const char* filename );

protected:	
	std::vector<std::string> m_paths;
	std::string m_files; 
	std::string m_logInfo;
	
	int	m_coreformat;
	int	m_coretype;
	int m_column;
	int m_stratType;
	
	Data* m_logdataptr;
	
	std::vector<DataInfo*> m_dataList;
	
private:

};

#endif
