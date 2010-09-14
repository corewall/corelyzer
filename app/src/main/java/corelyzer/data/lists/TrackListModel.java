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
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;

public class TrackListModel extends CRDefaultListModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6515577803471925162L;

	public TrackListModel() {
		super();
	}

	public TrackListModel(final CoreGraph aCg) {
		this();
		this.cg = aCg;
		cg.addListener(this);
	}

	@Override
	public void addAll(final Collection<?> c) {
		Session s = cg.getCurrentSession();

		if (s != null) {
			s.getTrackSceneNodes().addAll((Collection<? extends TrackSceneNode>) c);
		}
	}

	@Override
	public void addElement(final Object o) {
		if (o instanceof TrackSceneNode) {
			Session s = cg.getCurrentSession();

			if (s != null) {
				TrackSceneNode t = (TrackSceneNode) o;
				s.getTrackSceneNodes().addElement(t);
			}
		}
	}

	@Override
	public Enumeration<?> elements() {
		Session s = cg.getCurrentSession();

		if (s != null) {
			return s.getTrackSceneNodes().elements();
		} else {
			return null;
		}
	}

	// for plugin compatibility

	@Override
	public Object get(final int i) {
		return this.getElementAt(i);
	}

	public Object getElementAt(final int i) {
		if (cg == null) {
			return 0;
		}
		Session s = cg.getSession(cg.getCurrentSessionIdx());

		if (s == null) {
			return null;
		} else {
			return s.getTrackSceneNodeWithIndex(i);
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
			return s.getNumberOfTracks();
		}
	}

	@Override
	public void removeElement(final Object o) {
		if (o instanceof TrackSceneNode) {
			Session s = cg.getCurrentSession();

			if (s != null) {
				s.getTrackSceneNodes().removeElement(o);
			}
		}
	}
}
