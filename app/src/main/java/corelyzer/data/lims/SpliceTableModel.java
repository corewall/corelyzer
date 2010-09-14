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

import javax.swing.table.DefaultTableModel;

public class SpliceTableModel extends DefaultTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6825024903812631468L;
	LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();

	public SpliceTableModel() {
		super();
	}

	@Override
	public int getColumnCount() {
		if (dir == null) {
			return 0;
		} else {
			return dir.getNumberOfSpliceTableColumns();
		}
	}

	@Override
	public String getColumnName(final int col) {
		if (dir == null) {
			return "N/A";
		} else {
			return dir.getSpliceTableFieldName(col);
		}
	}

	@Override
	public int getRowCount() {
		if (dir == null) {
			return 0;
		} else {
			return dir.getNumberOfSpliceTableEntries();
		}
	}

	@Override
	public Object getValueAt(final int row, final int column) {
		if (dir == null) {
			return "";
		} else {
			String cell = dir.getSpliceTableCell(row, column);
			return cell == null ? "" : cell;
		}
	}

	@Override
	public boolean isCellEditable(final int i, final int i1) {
		return false;
	}
}
