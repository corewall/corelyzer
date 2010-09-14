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
package corelyzer.ui.annotation.propertyList;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.table.AbstractTableModel;

public class DefaultPropertyValuesTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1466432348641670604L;
	Hashtable<String, String> propertyValues;

	public DefaultPropertyValuesTableModel() {
		super();
	}

	public DefaultPropertyValuesTableModel(final Hashtable<String, String> srcHash) {
		this();

		if (srcHash != null) {
			this.propertyValues = srcHash;
		} else {
			this.propertyValues = new Hashtable<String, String>();
		}
	}

	public void clear() {
		this.propertyValues.clear();
	}

	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		if (propertyValues == null) {
			return 0;
		}

		return propertyValues.size();
	}

	public Object getValueAt(final int row, final int col) {
		if (row < 0 || row >= propertyValues.size() || col < 0 || col >= 2) {
			return "Invalid table indices: " + row + ", " + col;
		}

		int count = 0;
		Enumeration<String> keys = propertyValues.keys();
		while (keys.hasMoreElements()) {
			if (count != row) {
				keys.nextElement();
				count++;
			} else {
				String key = keys.nextElement();
				if (col == 0) {
					return key;
				} else {
					return propertyValues.get(key);
				}
			}
		}

		return "N/A";
	}

	@Override
	public boolean isCellEditable(final int row, final int col) {
		return col != 0;
	}

	public void setPropertyList(final Hashtable<String, String> srcHash) {
		propertyValues = null;
		propertyValues = srcHash;

		// set some default values if they're missing from srcHash
		String idString = srcHash.get("id");
		String nameString = srcHash.get("name");
		String analystString = srcHash.get("analyst");

		if (idString == null || idString.equals("")) {
			propertyValues.put("id", "N/A");
		}

		if (nameString == null || nameString.equals("")) {
			propertyValues.put("name", "N/A");
		}

		if (analystString == null || analystString.equals("")) {
			propertyValues.put("analyst", System.getProperty("user.name"));
		}
	}

	public void setPropertyValuePair(final String key, final String value) {
		this.propertyValues.put(key, value);
	}

	@Override
	public void setValueAt(final Object value, final int row, final int col) {
		if (row < 0 || row >= propertyValues.size() || col < 0 || col >= 2) {
			return;
		}

		int count = 0;
		Enumeration<String> keys = propertyValues.keys();
		while (keys.hasMoreElements()) {
			if (count != row) {
				keys.nextElement();
				count++;
			} else {
				String key = keys.nextElement();
				if (col == 1) {
					this.propertyValues.put(key, value.toString());
				}

				break;
			}
		}
	}
}
