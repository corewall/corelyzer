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
package corelyzer.data.dis;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class DISImageTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6194990614857295827L;
	Vector<String[]> data;
	static final String[] columns = { "Exp", "Site", "Hole", "Core", "Section", "Depth", "Length", "URL", "Top Offset", "Bottom Offset" };

	final static int LEG_INDEX = 0;
	final static int SITE_INDEX = 1;
	final static int HOLE_INDEX = 2;
	final static int CORE_INDEX = 3;
	final static int SECTION_INDEX = 4;
	final static int DEPTH_INDEX = 5;
	final static int LENGTH_INDEX = 6;
	final static int URL_INDEX = 7;
	final static int TOP_OFFSET_INDEX = 8;
	final static int BOT_OFFSET_INDEX = 9;

	public DISImageTableModel() {
		super();

		data = new Vector<String[]>();
	}

	public void addRow(final String expName, final String siteName, final String holeName, final String coreName, final String sectionName,
			final String sectionTopDepth, final String sectionLength, final String imageUrl, final String topOffset, final String baseOffset) {
		String[] row = { expName, siteName, holeName, coreName, sectionName, sectionTopDepth, sectionLength, imageUrl, topOffset, baseOffset };
		this.data.add(row);
	}

	public void clear() {
		data.clear();
	}

	public int getColumnCount() {
		return columns.length;
	}

	@Override
	public String getColumnName(final int col) {
		return columns[col];
	}

	public int getRowCount() {
		if (data == null) {
			return 0;
		} else {
			return data.size();
		}
	}

	public Object getValueAt(final int row, final int column) {
		if (data == null) {
			return "N/A";
		} else {
			return data.elementAt(row)[column];
		}
	}

	@Override
	public boolean isCellEditable(final int i, final int i1) {
		return false;
	}
}
