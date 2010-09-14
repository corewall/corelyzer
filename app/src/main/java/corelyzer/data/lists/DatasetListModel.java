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

import corelyzer.data.Session;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.coregraph.CoreGraph;

public class DatasetListModel extends CRDefaultListModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4168130741757658372L;

	public DatasetListModel() {
		super();
	}

	public DatasetListModel(final CoreGraph aCg) {
		this();
		this.cg = aCg;
		cg.addListener(this);
	}

	@Override
	public void addAll(final Collection<?> c) {
		Session s = cg.getCurrentSession();

		if (s != null) {
			s.getDatasets().addAll((Collection<? extends WellLogDataSet>) c);
		}
	}

	@Override
	public void addElement(final Object o) {
		if (o instanceof WellLogDataSet) {
			WellLogDataSet d = (WellLogDataSet) o;
			Session s = cg.getCurrentSession();

			if (s != null) {
				s.addDataset(d);
			}
		}
	}

	@Override
	public Enumeration<?> elements() {
		Session s = cg.getCurrentSession();

		if (s != null) {
			return s.getDatasets().elements();
		} else {
			return null;
		}
	}

	// for plugin compatibility

	@Override
	public Object get(final int i) {
		Session s = cg.getCurrentSession();

		if (s != null) {
			return s.getDatasets().get(i);
		} else {
			return null;
		}
	}

	public Object getElementAt(final int i) {
		if (cg == null) {
			return null;
		}
		Session s = cg.getCurrentSession();

		if (s == null) {
			return null;
		} else {
			return s.getDataset(i);
		}
	}

	public int getSize() {
		if (cg == null || cg.getCurrentSessionIdx() < 0) {
			return 0;
		}

		Session s = cg.getSession(cg.getCurrentSessionIdx());

		if (s == null) {
			return 0;
		} else {
			return s.getNumberOfDatasets();
		}
	}

	@Override
	public void removeElement(final Object o) {
		if (o instanceof WellLogDataSet) {
			WellLogDataSet d = (WellLogDataSet) o;
			Session s = cg.getCurrentSession();

			if (s != null) {
				s.getDatasets().removeElement(d);
			}
		}
	}
}
