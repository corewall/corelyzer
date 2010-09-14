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
package corelyzer.ui.annotation.sampling;

import java.util.Hashtable;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class SampleRequestTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8433606289511350770L;
	final String[] indexKeyMapping = { "status", "requestid", "trackname", "corename", "interval", "sampleLocation", "sampleType" };
	final int columnCount = indexKeyMapping.length;

	Vector<Hashtable<String, String>> samples;

	public SampleRequestTableModel() {
		super();
		samples = new Vector<Hashtable<String, String>>();
	}

	public void addAClast(final Hashtable<String, String> attribs) {
		samples.addElement(attribs);
	}

	public void clear() {
		samples.clear();
	}

	public int getColumnCount() {
		return columnCount;
	}

	public int getRowCount() {
		return samples.size();
	}

	public Object getValueAt(final int row, final int column) {
		Hashtable<String, String> attribs = samples.elementAt(row);
		return attribs.get(indexKeyMapping[column]);
	}
}
