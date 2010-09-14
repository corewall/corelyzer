/******************************************************************************
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen,
 * Sangyoon Lee, Electronic Visualization Laboratory, University of Illinois 
 * at Chicago
 *
 * This software is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along
 * with this software; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *****************************************************************************/
#ifndef __UTILITY_H__
#define __UTILITY_H__

#if defined(WIN32) || defined(_WIN32)
#include <windows.h>
#endif

#include <stdio.h>
#include <stdlib.h>

//------------------------------------------------
// Directory manipulation

#ifdef _WIN32

#include <direct.h>
#define CHDIR( s ) _chdir( (s) )
#define MKDIR( s ) _mkdir( (s) )
#define GETCWD( s, i ) _getcwd( (s) , (i) )

#else

#include <sys/stat.h>
#include <unistd.h>
#define CHDIR( s ) chdir( (s) )
#define MKDIR( s ) mkdir( (s) , S_IRWXU ) // default user access only
#define GETCWD( s, i ) getcwd( (s) , (i) )

#endif

//-----------------------------------------------

#ifdef _cplusplus
extern "C" {
#endif

char* ResampleBuffer(char* oldbuffer, int bufferwidth, int bufferheight,
                    int channels, float scale, char*& newbuffer,
                    int& newWidth, int& newHeight, bool highQuality);


#ifdef _cplusplus
}
#endif
#endif
