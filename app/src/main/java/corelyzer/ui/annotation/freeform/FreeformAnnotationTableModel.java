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
package corelyzer.ui.annotation.freeform;

import java.util.Hashtable;
import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;

public class FreeformAnnotationTableModel extends AbstractTableModel {
	class MyTableListener implements TableModelListener {

		public void tableChanged(final TableModelEvent event) {
			Hashtable<String, String> attribs = annotations.elementAt(event.getFirstRow());

			int trackId = Integer.parseInt(attribs.get("trackId"));
			int sectionId = Integer.parseInt(attribs.get("sectionId"));
			int markerId = Integer.parseInt(attribs.get("markerId"));

			CorelyzerApp app = CorelyzerApp.getApp();
			if (app != null) {
				boolean vis = SceneGraph.getCoreSectionMarkerVisibility(trackId, sectionId, markerId);

				SceneGraph.setCoreSectionMarkerVisibility(trackId, sectionId, markerId, !vis);

				app.updateGLWindows();

				attribs.put("show", String.valueOf(!vis));
			}
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -1037523610037436551L;

	final String[] indexKeyMapping = { "show", "track", "section", "depth", "group", "url", // string
			"trackId", "sectionId", "markerId" // integers
	};
	final int columnCount = indexKeyMapping.length;

	Vector<Hashtable<String, String>> annotations;

	public FreeformAnnotationTableModel() {
		super();
		annotations = new Vector<Hashtable<String, String>>();
	}

	public void addAnnotation(final Hashtable<String, String> attribs) {
		annotations.addElement(attribs);
	}

	public void clear() {
		annotations.clear();
	}

	@Override
	public Class getColumnClass(final int col) {
		return getValueAt(0, col).getClass();
	}

	public int getColumnCount() {
		return columnCount;
	}

	public int getRowCount() {
		return annotations.size();
	}

	public Object getValueAt(final int row, final int column) {
		Hashtable<String, String> attribs = annotations.elementAt(row);

		if (column == 0) {
			return Boolean.parseBoolean(attribs.get("show"));
		} else {
			return attribs.get(indexKeyMapping[column]);
		}
	}

	@Override
	public boolean isCellEditable(final int row, final int col) {
		return col == 0;
	}

	@Override
	public void setValueAt(final Object value, final int row, final int col) {
		Hashtable<String, String> attribs = annotations.elementAt(row);

		if (attribs == null) {
			return;
		}

		if (col == 0) {
			TableModelListener listener = new MyTableListener();
			listener.tableChanged(new TableModelEvent(this, row, row, 0));

			fireTableCellUpdated(row, col);
		}
	}
}
