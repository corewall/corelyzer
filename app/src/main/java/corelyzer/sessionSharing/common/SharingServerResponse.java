/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2008 Julian Yu-Chung Chen
 * Electronic Visualization Laboratory, University of Illinois at Chicago
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
package corelyzer.sessionSharing.common;

public class SharingServerResponse {
	public static final byte CONNECTED = 0;
	public static final byte DISCONNECTED = 1;

	public static final byte PUBLISH_SUCCESS = 2;
	public static final byte PUBLISH_FAILED = 3;

	public static final byte LIST_SUCCESS = 4;
	public static final byte LIST_FAILED = 5;

	public static final byte DOWNLOAD_SUCCESS = 6;
	public static final byte DOWNLOAD_FAILED = 7;

	public static final byte SUBSCRIBE_FAILED = 9;
	public static final byte SUBSCRIBE_SUCCESS = 8;

	public static final byte SHUTDOWN_SUCCESS = 10;
	public static final byte SHUTDOWN_FAILED = 11;
}
