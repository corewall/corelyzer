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
package corelyzer.data.lims;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

public class LogDBQueryResultTableModel extends DefaultTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1621825273006397320L;

	final static String[] headers = { "Leg", "Site+Hole", "Tool", "Measurement", "Filename", "FileUrl", "Pass" };

	public static int LEG_INDEX = 0;
	public static int SITE_INDEX = 1;
	public static int TOOL_INDEX = 2;
	public static int MEASUREMENT_INDEX = 3;
	public static int FILENAME_INDEX = 4;
	public static int FILEURL_INDEX = 5;
	public static int PASS_INDEX = 6;

	Vector<String[]> rows;

	public LogDBQueryResultTableModel() {
		super();
		rows = new Vector<String[]>();
	}

	public void addRow(final String[] row) {
		this.rows.add(row);
	}

	public void clear() {
		this.rows.clear();
	}

	@Override
	public int getColumnCount() {
		return headers.length;
	}

	@Override
	public String getColumnName(final int col) {
		return headers[col];
	}

	@Override
	public int getRowCount() {
		if (rows != null) {
			return rows.size();
		} else {
			return 0;
		}
	}

	@Override
	public Object getValueAt(final int r, final int c) {
		String[] row = rows.get(r);

		if (row != null) {
			if (row.length > 0 && c < row.length) {
				return row[c];
			}
		}

		return null;
	}

	@Override
	public boolean isCellEditable(final int r, final int c) {
		return false;
	}
}
