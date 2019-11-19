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
package corelyzer.data.coregraph;

import java.util.Vector;

import corelyzer.data.CoreSection;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.lists.CRDefaultListModel;

public class CoreGraph {
	private static CoreGraph cg;

	public static CoreGraph getInstance() {
		if (cg == null) {
			cg = new CoreGraph();
		}

		return cg;
	}

	Vector<Session> sessionVec;
	int currentSessionIdx = -1;
	int currentTrackIdx = -1;
	int[] currentSectionIndices = {};
	int currentDatasetIdx = -1;

	int currentFieldIdx = -1;

	Vector<CRDefaultListModel> listeners;

	private CoreGraph() {
		super();

		cg = this;
		sessionVec = new Vector<Session>();
		listeners = new Vector<CRDefaultListModel>();
	}

	public void addDataset(final Session s, final WellLogDataSet d) {
		s.addDataset(d);

		if (!sessionVec.contains(s)) {
			sessionVec.addElement(s);
		}

		currentSessionIdx = sessionVec.indexOf(s);
		currentDatasetIdx = s.getIndexOfDataset(d);

		notifyListeners();
	}

	public void addListener(final CRDefaultListModel l) {
		listeners.addElement(l);
	}

	public void addSession(final Session s) {
		sessionVec.addElement(s);
		currentSessionIdx = sessionVec.indexOf(s);
		currentTrackIdx = -1;
		currentDatasetIdx = -1;

		notifyListeners();
	}

	public void addTrack(final Session s, final TrackSceneNode t) {
		s.addTrack(t);

		if (!sessionVec.contains(s)) {
			sessionVec.addElement(s);
		}

		currentSessionIdx = sessionVec.indexOf(s);
		currentTrackIdx = s.getIndexOfTrack(t);

		notifyListeners();
	}

	public WellLogDataSet getCurrentDataset() {
		Session s = getCurrentSession();

		if (s != null) {
			return s.getDataset(currentDatasetIdx);
		} else {
			return null;
		}
	}

	public int getCurrentDatasetIdx() {
		return currentDatasetIdx;
	}

	public int getCurrentFieldIdx() {
		return currentFieldIdx;
	}

	public CoreSection getCurrentSection() {
		Session s = getCurrentSession();

		if (s != null) {
			TrackSceneNode t = s.getTrackSceneNodeWithIndex(currentTrackIdx);

			if (t != null) {
				return t.getCoreSection(this.getCurrentSectionIdx());
			}
		}

		return null;
	}

	// return first selected section
	public int getCurrentSectionIdx() {
		int result = -1;
		if (this.currentSectionIndices.length > 0)
			result = this.currentSectionIndices[0];
		return result;
	}
	
	public int[] getCurrentSectionIndices() { return this.currentSectionIndices; }

	public Session getCurrentSession() {
		if (currentSessionIdx < 0) {
			Session s = new Session("Default");
			this.addSession(s);
			this.currentSessionIdx = sessionVec.indexOf(s);
		}

		return sessionVec.elementAt(currentSessionIdx);
	}

	public int getCurrentSessionIdx() {
		return currentSessionIdx;
	}

	public TrackSceneNode getCurrentTrack() {
		Session s = getCurrentSession();

		if (s != null) {
			return s.getTrackSceneNodeWithIndex(currentTrackIdx);
		} else {
			return null;
		}
	}

	public int getCurrentTrackIdx() {
		return currentTrackIdx;
	}

	public WellLogDataSet getDataset(final int sessionId, final int datasetId) {
		Session s = getSession(sessionId);
		if (s == null) {
			return null;
		}

		return s.getDataset(datasetId);
	}

	public int getIndexOfSession(final Session s) {
		return sessionVec.indexOf(s);
	}

	public int getNumberOfSessions() {
		if (sessionVec == null) {
			return 0;
		}

		return sessionVec.size();
	}

	public CoreSection getSection(final int sessionId, final int trackId, final int sectionId) {
		TrackSceneNode t = getTrack(sessionId, trackId);

		if (t == null) {
			return null;
		}

		return t.getCoreSection(sectionId);
	}

	public Session getSession(final int i) {
		if (sessionVec == null) {
			return null;
		}

		if (i < 0 || i >= sessionVec.size()) {
			return null;
		} else {
			return sessionVec.elementAt(i);
		}
	}

	public Session getSession(final String aString) {
		for (Session s : sessionVec) {
			if (s.getName().equals(aString)) {
				return s;
			}
		}

		return null;
	}

	// accessor for graph components
	public Vector<Session> getSessions() {
		return this.sessionVec;
	}

	// trackId is native trackId, not index
	public TrackSceneNode getTrack(final int sessionId, final int trackId) {
		Session s = getSession(sessionId);
		if (s == null) {
			return null;
		}

		return s.getTrackSceneNodeWithTrackId(trackId);
	}

	public void notifyListeners() {
		for (CRDefaultListModel l : listeners) {
			l.modified();
		}
	}

	public void removeDataset(Session s, final WellLogDataSet d) {
		if (s == null) {
			s = getCurrentSession();
		}

		if (s.removeDataset(d)) {
			this.currentDatasetIdx = -1;
			System.out.println("Dataset Removing: SUCCESS");
		} else {
			System.out.println("Dataset Removing: Oops");
		}

		notifyListeners();
	}

	public void removeDatasets(Session s) {
		if (s == null) {
			s = getCurrentSession();
		}

		this.currentDatasetIdx = -1;
		s.getDatasets().clear();

		notifyListeners();
	}

	public void removeSection(final TrackSceneNode t, final CoreSection sec) {
		if (t.getCoreSections().contains(sec)) {
			t.getCoreSections().removeElement(sec);
		}

		notifyListeners();
	}

	public void removeSession(final Session s) {
		if (this.sessionVec.contains(s)) {
			this.sessionVec.remove(s);

			this.setCurrentSessionIdx(sessionVec.size() - 1);
		}
	}

	public void removeTrack(Session s, final TrackSceneNode t) {
		if (s == null) {
			s = getCurrentSession();
		}

		if (s.removeTrack(t)) {
			this.currentTrackIdx = -1;
			System.out.println("Track Removing: SUCCESS");
		} else {
			System.out.println("Track Removing: Oops");
		}

		notifyListeners();
	}

	public void removeTracks(Session s) {
		if (s == null) {
			s = getCurrentSession();
		}

		this.currentTrackIdx = -1;
		s.getTrackSceneNodes().clear();

		notifyListeners();
	}

	public void setCurrentDatasetIdx(final int idx) {
		Session s = this.getCurrentSession();
		Vector<WellLogDataSet> datasets = s.getDatasets();

		if (idx >= datasets.size()) {
			return;
		}

		this.currentDatasetIdx = idx;
		notifyListeners();
	}

	public void setCurrentFieldIdx(final int idx) {
		this.currentFieldIdx = idx;
		notifyListeners();
	}

	public void setCurrentSectionIdx(final int idx) {
		if (idx != -1)
			this.currentSectionIndices = new int[]{ idx };
		else
			this.currentSectionIndices = new int[]{};
		notifyListeners();
	}
	
	public void setCurrentSectionIndices(final int[] indices) {
		this.currentSectionIndices = indices;
		notifyListeners();
	}

	public void setCurrentSession(final Session s) {
		if (!sessionVec.contains(s)) {
			return;
		}

		setCurrentSessionIdx(sessionVec.indexOf(s));
	}

	public void setCurrentSessionIdx(final int idx) {
		// range check
		if (idx < 0 || idx >= sessionVec.size()) {
			this.currentSessionIdx = -1;
			notifyListeners();

			return;
		}

		this.currentSessionIdx = idx;

		Session s = sessionVec.elementAt(currentSessionIdx);
		Vector<TrackSceneNode> tracks = s.getTrackSceneNodes();
		Vector<WellLogDataSet> datasets = s.getDatasets();

		if (!tracks.isEmpty()) {
			this.setCurrentTrackIdx(tracks.size() - 1);
		} else {
			this.setCurrentTrackIdx(-1);
		}

		if (!datasets.isEmpty()) {
			this.setCurrentDatasetIdx(datasets.size() - 1);
		} else {
			this.setCurrentDatasetIdx(-1);
		}

		notifyListeners();
	}

	public void setCurrentTrack(final Session s, final TrackSceneNode t) {
		setCurrentSession(s);

		Vector<TrackSceneNode> tracks = s.getTrackSceneNodes();
		if (!tracks.contains(t)) {
			return;
		}

		setCurrentTrackIdx(tracks.indexOf(t));
	}

	public void setCurrentTrackIdx(final int idx) {
		Session s = this.getCurrentSession();
		Vector<TrackSceneNode> tracks = s.getTrackSceneNodes();

		if (idx >= tracks.size()) {
			return;
		}

		this.currentTrackIdx = idx;

		if (idx != -1) {
			TrackSceneNode t = tracks.elementAt(idx);
			if (t.getNumCores() > 0) {
				this.setCurrentSectionIdx(t.getNumCores() - 1);
			} else {
				this.setCurrentSectionIdx(-1);
			}
		}

		notifyListeners();
	}
}
