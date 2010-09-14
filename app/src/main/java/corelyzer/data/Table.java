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

import java.util.Vector;

/**
 * Data table structure to hold core logging data. Each table is corresponding
 * to a core section(/image). Each table contains Columns and each
 * corelyzer.data.Column has Cells. A super class of real
 * corelyzer.data.WellLogTable class.
 */
public class Table {
	/**
	 * Name of the table
	 */
	String name;
	/**
	 * Number of table rows
	 */
	int numRows;
	/**
	 * Number of table columns
	 */
	int numCols;

	String sourceFile;
	/**
	 * corelyzer.data.Table headers
	 */
	Vector<String> headers;
	/**
	 * Columns of the table
	 */
	Vector<Column> columns;
	Column[] colData;

	public Table() {
		this.sourceFile = "";
		this.name = null;
		this.numRows = 0;
		this.numCols = 0;
		this.headers = new Vector<String>();
		this.columns = new Vector<Column>();
	}

	public int addHeader(final String aName) {
		headers.add(aName);
		return headers.indexOf(aName);
	}

	/**
	 * Clearing table data
	 */
	void clearData() {
		System.out.println("Clearing data of table at " + this.name);

		for (int i = 0; i < numCols; i++) {
			if (colData[i] != null) {
				colData[i] = null;
			}
		}

		colData = null;

		headers.removeAllElements();
		sourceFile = null;
	}

	public float getCell(final int row, final int col) {
		return this.colData[col].dataArray[row];
	}

	public int getCellType(final int c) {
		if (c < 0 || c > getNumColumns()) {
			return CellType.UNDEF;
		}
		return this.colData[c].datatype;
	}

	public float getColumnMax(final int field) {
		int maxIndex = colData[field + 1].maxIndex;
		return maxIndex < 0 ? 0.0f : colData[field + 1].dataArray[maxIndex];
	}

	public float getColumnMin(final int field) {
		int minIndex = colData[field + 1].minIndex;
		return minIndex < 0 ? 0.0f : colData[field + 1].dataArray[minIndex];
	}

	public float getDepth(final int row) {
		return this.colData[0].dataArray[row];
	}

	public String getHeader(final int i) {
		if (i < 0 || i >= headers.size()) {
			return null;
		}
		return this.headers.elementAt(i);
	}

	public String getName() {
		return this.name;
	}

	public int getNumColumns() {
		return this.numCols;
	}

	public int getNumRows() {
		return this.numRows;
	}

	public String getSourceFilename() {
		return this.sourceFile;
	}

	public boolean isCellValid(final int row, final int col) {
		return !(row < 0 || row > numRows) && colData[col].valid[row];
	}

	public void setNumColumns(final int n) {
		this.numCols = n;
	}
}
