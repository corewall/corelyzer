/******************************************************************************
 *
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
 * Questions or comments about CoreWall should be directed to 
 * cavern@evl.uic.edu
 *
 *****************************************************************************/
 
#ifndef __EXCEPTIONS_H__
#define __EXCEPTIONS_H__

#include <string>

namespace Exceptions
{
	enum{
		Unknown = 0,
		OutOfBound_AccessAttempt,
		Invalid_Image,
		Invalid_Xpos,
		Invalid_Ypos,
		FileNotFound,
		NullString,
		OutOfMemory
	};

};


class Exception
{
public:
	Exception(char* description,int code = Exceptions::Unknown);
	Exception(std::string description, int code = Exceptions::Unknown);
	std::string desc;
	int errCode;
};

#endif
