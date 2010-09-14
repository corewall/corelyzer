/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2007 Julian Yu-Chung Chen
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
package corelyzer.ui.annotation.clast;

import java.util.Hashtable;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class ClastTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6268163508829759650L;
	final String[] indexKeyMapping = { "trackname", "corename", "width", "height", "upperleft", "lowerright", "size", "shape", "lithology", "grainsize",
			"color", "texture", "minerals", "samplenumber", "note" };
	final int columnCount = indexKeyMapping.length;

	Vector<Hashtable<String, String>> allClasts;

	public ClastTableModel() {
		super();
		allClasts = new Vector<Hashtable<String, String>>();
	}

	public void addAClast(final Hashtable<String, String> attribs) {
		allClasts.addElement(attribs);
	}

	public void clear() {
		allClasts.clear();
	}

	public int getColumnCount() {
		return columnCount;
	}

	private String getCountsString(final Hashtable<String, String> aHash, final int column) {
		String retStr = "";

		switch (column) {
			case 6: // size
				retStr += aHash.get("granuleCount") + ":";
				retStr += aHash.get("pebbleCount") + ":";
				retStr += aHash.get("cobbleCount") + ":";
				retStr += aHash.get("boulderCount");
				break;

			case 7: // shape
				retStr += aHash.get("angularCount") + ":";
				retStr += aHash.get("sub-AngularCount") + ":";
				retStr += aHash.get("sub-RoundedCount") + ":";
				retStr += aHash.get("roundedCount");
				break;

			case 8: // lithology
				retStr += aHash.get("volcanicCount") + ":";
				retStr += aHash.get("granitoidCount") + ":";
				retStr += aHash.get("sedimentaryCount") + ":";
				retStr += aHash.get("metamorphicCount") + ":";
				retStr += aHash.get("quartzCount") + ":";
				retStr += aHash.get("doleriteCount") + ":";
				retStr += aHash.get("intraclastCount");
				break;

			case 9: // grainsize
				retStr += aHash.get("vfgCount") + ":";
				retStr += aHash.get("fgCount") + ":";
				retStr += aHash.get("mgCount") + ":";
				retStr += aHash.get("cgCount");
				break;

			default:
				retStr = "NA";
		}

		return retStr;
	}

	public int getRowCount() {
		return allClasts.size();
	}

	public Object getValueAt(final int row, final int column) {
		Hashtable<String, String> attribs = allClasts.elementAt(row);

		// size, shape, lithology, grainsize
		if (column > 5 && column < 10) {
			if (attribs.containsKey("mode") && attribs.get("mode").equalsIgnoreCase("multiple")) {
				return getCountsString(attribs, column);
			}
		}

		return attribs.get(indexKeyMapping[column]);
	}
}
