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
package corelyzer.data;

import java.util.Vector;

public class Session {
	String name = "UnNamed Session";
	String DISId = "N/A_N/A";
	String stateFilename;

	Vector<TrackSceneNode> tracks;
	Vector<WellLogDataSet> datasets;

	boolean isShow = true;

	public Session() {
		tracks = new Vector<TrackSceneNode>();
		datasets = new Vector<WellLogDataSet>();
	}

	public Session(final String aName) {
		this();
		name = aName;
	}

	public void addDataset(final WellLogDataSet aDataset) {
		if (!datasets.contains(aDataset)) {
			datasets.add(aDataset);
		}
	}

	public void addTrack(final TrackSceneNode aTrack) {
		if (!tracks.contains(aTrack)) {
			tracks.add(aTrack);
		}
	}

	public WellLogDataSet getDataset(final int i) {
		// range check
		if (i < 0 || i >= this.datasets.size()) {
			return null;
		}

		return datasets.elementAt(i);
	}

	public WellLogDataSet getDataset(final String name) {
		if (name == null) {
			return null;
		}

		for (WellLogDataSet ds : this.datasets) {
			if (name.equals(ds.getSourceFilename())) {
				return ds;
			}
		}

		return null;
	}

	public Vector<WellLogDataSet> getDatasets() {
		return datasets;
	}

	public String getDISId() {
		return DISId;
	}

	public int getIndexOfDataset(final WellLogDataSet d) {
		return this.datasets.indexOf(d);
	}

	public int getIndexOfTrack(final TrackSceneNode t) {
		return this.tracks.indexOf(t);
	}

	// for session
	public String getName() {
		return name;
	}

	// for datasets
	public int getNumberOfDatasets() {
		return this.datasets.size();
	}

	// for track
	public int getNumberOfTracks() {
		return tracks.size();
	}

	public Vector<TrackSceneNode> getTrackSceneNodes() {
		return this.tracks;
	}

	public TrackSceneNode getTrackSceneNodeWithIndex(final int i) {
		// range check
		if (i < 0 || i >= tracks.size()) {
			return null;
		}

		return tracks.elementAt(i);
	}

	public TrackSceneNode getTrackSceneNodeWithName(final String name) {
		if (name == null) {
			return null;
		}

		for (TrackSceneNode t : tracks) {
			if (t.getName().equals(name)) {
				return t;
			}
		}

		return null;
	}

	public TrackSceneNode getTrackSceneNodeWithTrackId(final int trackId) {
		if (trackId < 0) {
			return null;
		}

		for (TrackSceneNode t : tracks) {
			if (trackId == t.getId()) {
				return t;
			}
		}

		return null;
	}

	public boolean hasDataset(final String name) {
		if (name == null) {
			return false;
		}

		for (WellLogDataSet ds : this.datasets) {
			if (name.equals(ds.getSourceFilename())) {
				return true;
			}
		}

		return false;
	}

	public boolean isShow() {
		return isShow;
	}

	public boolean removeDataset(final WellLogDataSet d) {
		// FIXME Some d cleanups?
		return this.datasets.remove(d);
	}

	public boolean removeTrack(final TrackSceneNode t) {
		t.removeAllCoreSection();
		return this.tracks.remove(t);
	}

	public void setDISId(final String DISId) {
		this.DISId = DISId;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setShow(final boolean show) {
		isShow = show;
	}

	@Override
	public String toString() {
		return name;
	}
}
