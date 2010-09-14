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
package corelyzer.sessionSharing.client.model;

import java.util.Hashtable;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class SessionSharingTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8503571641760488024L;

	final String[] indexKeyMapping = { "name", "author", "description", "lastUpdateDate", "subscription", "download" };

	final int columnCount = indexKeyMapping.length;
	Vector<Hashtable<String, String>> sessions;

	public SessionSharingTableModel() {
		super();
		sessions = new Vector<Hashtable<String, String>>();
	}

	public int getColumnCount() {
		return columnCount;
	}

	public String[] getIndexKeyMapping() {
		return indexKeyMapping;
	}

	public int getRowCount() {
		return sessions.size();
	}

	public Vector<Hashtable<String, String>> getSessions() {
		return sessions;
	}

	public Object getValueAt(final int row, final int column) {
		if (column == 4) {
			return "Subscribe";
		} else if (column == 5) {
			return "Download";
		} else {
			Hashtable<String, String> attribs = sessions.elementAt(row);
			return attribs.get(indexKeyMapping[column]);
		}
	}

	@Override
	public boolean isCellEditable(final int row, final int col) {
		return col == 0;
	}

	public void setSessions(final Vector<Hashtable<String, String>> sessions) {
		this.sessions = sessions;
	}
}
