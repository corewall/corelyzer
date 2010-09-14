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
package corelyzer.data.lists;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

import corelyzer.data.Session;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.WellLogTable;
import corelyzer.data.coregraph.CoreGraph;

public class FieldListModel extends CRDefaultListModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8488705278455252869L;
	Vector<String> strvec = new Vector<String>();

	public FieldListModel() {
		super();
	}

	public FieldListModel(final CoreGraph aCg) {
		this();
		this.cg = aCg;
		cg.addListener(this);
	}

	@Override
	public void addAll(final Collection<?> c) {
		strvec.addAll((Collection<? extends String>) c);
	}

	@Override
	public void addElement(final Object o) {
		// do nothing
	}

	@Override
	public Enumeration<?> elements() {
		return this.strvec.elements();
	}

	// for plugin compatibility

	@Override
	public Object get(final int i) {
		return this.strvec.get(i);
	}

	public Object getElementAt(final int i) {
		// refresh();
		if (i < 0 || i >= strvec.size()) {
			return null;
		}

		return strvec.elementAt(i);
	}

	public int getSize() {
		if (cg.getCurrentSessionIdx() < 0 || cg.getCurrentDatasetIdx() < 0) {
			return 0;
		}

		this.refresh();

		return strvec.size();
	}

	private void refresh() {
		strvec.clear();

		if (cg == null || cg.getCurrentSessionIdx() < 0 || cg.getCurrentDatasetIdx() < 0) {
			return;
		}

		Session s = cg.getSession(cg.getCurrentSessionIdx());
		if (s == null || s.getNumberOfDatasets() <= 0) {
			return;
		}

		WellLogDataSet d = s.getDataset(cg.getCurrentDatasetIdx());
		if (d == null) {
			return;
		}

		// go through all the tables and
		// if there is a field not in the array
		// then add it to the array
		for (int i = 0; i < d.getNumTables(); i++) {
			WellLogTable table = d.getTable(i);

			for (int k = 0; k < table.getNumColumns(); k++) {
				if (k != table.getDepthColumnIndex()) {
					String temp = table.getHeader(k);
					if (temp == null) {
						continue;
					}

					boolean insertstr = true;
					for (int p = 0; insertstr && p < strvec.size(); p++) {
						if (strvec.isEmpty()) {
							insertstr = true;
						} else if (strvec.contains(temp)) {
							insertstr = false;
						}
					}// end-of-for-p

					if (insertstr) {
						strvec.addElement(temp);
					}
				}// end-of-if
			}// end-of-for-k
		}// end-of-for-i
	}

	@Override
	public void removeElement(final Object o) {
		if (o instanceof String) {
			this.strvec.removeElement(o);
		}
	}
}
