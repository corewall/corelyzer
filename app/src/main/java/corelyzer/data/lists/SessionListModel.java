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
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.ui.CorelyzerApp;

public class SessionListModel extends CRDefaultListModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4954670778666871485L;

	public SessionListModel() {
		super();
	}

	public SessionListModel(final CoreGraph aCg) {
		this();
		this.cg = aCg;
		cg.addListener(this);
	}

	@Override
	public void addAll(final Collection<?> c) {
		cg.getSessions().addAll((Collection<? extends Session>) c);
	}

	@Override
	public void addElement(final Object o) {
		if (o instanceof Session) {
			cg.addSession((Session) o);
		}
	}

	@Override
	public Enumeration<?> elements() {
		return cg.getSessions().elements();
	}

	// for plugin compatibility

	@Override
	public Object get(final int i) {
		return cg.getSession(i);
	}

	public Object getElementAt(final int i) {
		if (cg == null) {
			return null;
		} else {
			return cg.getSession(i);
		}
	}

	public int getSize() {
		if (cg == null) {
			return 0;
		} else {
			return cg.getNumberOfSessions();
		}
	}

	@Override
	public void modified() {
		super.modified();
		CorelyzerApp app = CorelyzerApp.getApp();
		app.enableMenuItemsOnSessionChange();
	}

	@Override
	public void removeElement(final Object o) {
		if (o instanceof Session) {
			cg.removeSession((Session) o);
		}
	}
}
