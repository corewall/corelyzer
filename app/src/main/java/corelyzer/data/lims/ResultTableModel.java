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

import javax.swing.table.AbstractTableModel;

public class ResultTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6650987643329625744L;

	LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();

	Vector<String[]> rowVec;

	public ResultTableModel() {
		rowVec = new Vector<String[]>();
	}

	public void addRow(final String line) {
		// Rearrange token orders in
		String[] toks = line.split("\t");

		if (toks.length >= this.getColumnCount()) {
			Vector<String> newToks = new Vector<String>();

			newToks.add(toks[0]);
			newToks.add(toks[1]);
			newToks.add(toks[2]);
			newToks.add(toks[3]);
			newToks.add(toks[7]);
			newToks.add(toks[4]);
			newToks.add("0.0");
			newToks.add("0.0");
			newToks.add(toks[10]);
			newToks.add(toks[9]);
			newToks.add(toks[12]);
			newToks.add(toks[13]);

			this.rowVec.add(newToks.toArray(new String[newToks.size()]));
		}
	}

	public void clear() {
		this.rowVec.clear();
	}

	public int getColumnCount() {
		if (dir == null) {
			return 0;
		} else {
			return dir.getNumberOfFields();
		}
	}

	@Override
	public String getColumnName(final int col) {
		if (dir == null) {
			return "N/A";
		} else {
			return dir.getFieldName(col);
		}
	}

	public int getRowCount() {
		return rowVec.size();
	}

	public Object getValueAt(final int row, final int column) {
		String[] toks = this.rowVec.elementAt(row);

		if (toks != null) {
			if (column < toks.length) {
				return toks[column];
			} else {
				return "";
			}
		} else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(final int i, final int i1) {
		return false;
	}
}
