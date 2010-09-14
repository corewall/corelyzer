/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
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

package corelyzer.data;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Static enumeration tags for markers in scene graph
 * <p/>
 * // Number of available core marker types
 * <p/>
 * static int NUM_CORE_MARKERS = 3;
 * <p/>
 * // Point marker type. Used for annotation, for example
 * <p/>
 * static int CORE_POINT_MARKER = 0;
 * <p/>
 * // Span marker type
 * <p/>
 * static int CORE_SPAN_MARKER = 1;
 * <p/>
 * // Outline marker type
 * <p/>
 * static int CORE_OUTLINE_MARKER = 2;
 */
public class MarkerType {

	/** Number of available core marker types */
	public static final int NUM_CORE_MARKERS = 3;
	public static final int CORE_DEFAULT_MARKER = 0;
	public static final int CORE_POINT_MARKER = 0;
	public static final int CORE_SPAN_MARKER = 1;
	public static final int CORE_OUTLINE_MARKER = 2;

	static Hashtable<Integer, String> mapping;

	static {
		mapping = new Hashtable<Integer, String>();
		mapping.put(0, "CORE_POINT_MARKER");
		mapping.put(1, "CORE_SPAN_MARKER");
		mapping.put(2, "CORE_OUTLINE_MARKER");
	}

	public static int getMarkerId(final String markerName) {
		if (!mapping.contains(markerName)) {
			return MarkerType.CORE_DEFAULT_MARKER;
		}

		Enumeration<Integer> e = mapping.keys();
		while (e.hasMoreElements()) {
			Integer aKey = e.nextElement();
			if (mapping.get(aKey).equals(markerName)) {
				return aKey;
			}
		}

		return MarkerType.CORE_DEFAULT_MARKER;
	}

	public static String getMarkerName(final int m) {
		return mapping.containsKey(m) ? mapping.get(m) : "NA";
	}
}
