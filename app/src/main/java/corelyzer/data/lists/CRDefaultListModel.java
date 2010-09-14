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

import javax.swing.AbstractListModel;

import corelyzer.data.coregraph.CoreGraph;

public abstract class CRDefaultListModel extends AbstractListModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 271427339521303894L;
	CoreGraph cg;

	public abstract void addAll(Collection<?> c);

	public abstract void addElement(Object o);

	public Object elementAt(final int i) {
		return this.getElementAt(i);
	}

	public abstract Enumeration<?> elements();

	// for plugin compatibility
	public abstract Object get(int i);

	public boolean isEmpty() {
		return getSize() == 0;
	}

	public void modified() {
		fireContentsChanged(this, 0, size());
	}

	public abstract void removeElement(Object o);

	public int size() {
		return this.getSize();
	}
}
