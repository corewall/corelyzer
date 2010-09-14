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

import corelyzer.data.CoreSection;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;

public class SectionListModel extends CRDefaultListModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 767077564415369570L;

	public SectionListModel() {
		super();
	}

	public SectionListModel(final CoreGraph aCg) {
		this();
		this.cg = aCg;
		cg.addListener(this);
	}

	@Override
	public void addAll(final Collection<?> c) {
		TrackSceneNode t = cg.getCurrentTrack();

		if (t != null) {
			t.getCoreSections().addAll((Collection<? extends CoreSection>) c);
		}
	}

	@Override
	public void addElement(final Object o) {
		if (o instanceof CoreSection) {
			CoreSection c = (CoreSection) o;
			TrackSceneNode t = cg.getCurrentTrack();

			if (t != null) {
				t.addCoreSection(c);
			}
		}
	}

	@Override
	public Enumeration<?> elements() {
		TrackSceneNode t = cg.getCurrentTrack();

		if (t != null) {
			return t.getCoreSections().elements();
		} else {
			return null;
		}
	}

	// for plugin compatibility

	@Override
	public Object get(final int i) {
		TrackSceneNode t = cg.getCurrentTrack();

		if (t != null) {
			return t.getCoreSection(i);
		} else {
			return null;
		}
	}

	public Object getElementAt(final int i) {
		if (cg == null) {
			return 0;
		}

		Session s = cg.getSession(cg.getCurrentSessionIdx());
		if (s == null) {
			return null;
		}

		TrackSceneNode t = s.getTrackSceneNodeWithIndex(cg.getCurrentTrackIdx());
		if (t == null) {
			return null;
		}

		return t.getCoreSection(i);
	}

	public int getSize() {
		if (cg == null || cg.getCurrentSessionIdx() < 0 || cg.getCurrentTrackIdx() < 0) {
			return 0;
		}

		Session s = cg.getSession(cg.getCurrentSessionIdx());
		if (s == null) {
			return 0;
		}

		TrackSceneNode t = s.getTrackSceneNodeWithIndex(cg.getCurrentTrackIdx());
		if (t == null) {
			return 0;
		}

		return t.getNumCores();
	}

	@Override
	public void removeElement(final Object o) {
		if (o instanceof CoreSection) {
			TrackSceneNode t = cg.getCurrentTrack();

			if (t != null) {
				t.getCoreSections().removeElement(o);
			}
		}
	}
}
