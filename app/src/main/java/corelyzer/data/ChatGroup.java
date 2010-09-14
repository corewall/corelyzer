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

public class ChatGroup {
	public static final int DEFAULT = 0;
	public static final int UNDEFINED = 0;
	public static final int SEDIMENTOLOGY = 1;
	public static final int GEOPHYSICS = 2;
	public static final int BIOCHEMISTRY = 3;
	public static final int OPERATIONAL = 4;
	public static final int EDUCATIONAL = 5;
	public static final int LITHOLOGY = 6;
	public static final int PETROLOGY = 7;
	public static final int CLAST = 8;
	public static final int SAMPLE = 9;
	public static final int DIS = 10;
	public static final int TIE = 11;

	static Hashtable<Integer, String> mapping;

	static {
		mapping = new Hashtable<Integer, String>();
		mapping.put(0, "UNDEFINED");
		mapping.put(1, "SEDIMENTOLOGY");
		mapping.put(2, "GEOPHYSICS");
		mapping.put(3, "BIOCHEMISTRY");
		mapping.put(4, "OPERATIONAL");
		mapping.put(5, "EDUCATIONAL");
		mapping.put(6, "LITHOLOGY");
		mapping.put(7, "PETROLOGY");

		mapping.put(8, "CLAST");
		mapping.put(9, "SAMPLE");
		mapping.put(10, "DIS");
		mapping.put(11, "TIE");
	}

	public static int getGroupId(final String groupName) {
		if (!mapping.contains(groupName)) {
			return ChatGroup.DEFAULT;
		}

		Enumeration<Integer> e = mapping.keys();
		while (e.hasMoreElements()) {
			Integer aKey = e.nextElement();
			if (mapping.get(aKey).equals(groupName)) {
				return aKey;
			}
		}

		return ChatGroup.UNDEFINED;
	}

	public static String getGroupName(final int g) {
		return mapping.containsKey(g) ? mapping.get(g) : "NA";
	}

	public static int getNumberOfGroups() {
		return mapping.size();
	}
}
