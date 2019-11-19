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
package corelyzer.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import corelyzer.data.CRPreferences;
import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionImage;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lims.LIMSImageryDirectory;
import corelyzer.graphics.SceneGraph;
import corelyzer.handlers.ProgressHandler;
import corelyzer.helper.URLRetrieval;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.FileUtility;
import corelyzer.util.StringUtility;

public class CRExperimentController {
	static int steps = 10;

	/* Cut a portion of a section and put it into a new track */
	public static boolean DOWNCORE_DEPTH = false;

	public static boolean ABSOLUTE_DEPTH = true;

	public static int[] cutIntervalToNewTrack(final int[] location, final float intervalStart, final float intervalEnd, final boolean depthType,
			final String newTrackName) {
		return cutIntervalToNewTrack(location, intervalStart, intervalEnd, depthType, newTrackName, null);
	}

	public static int[] cutIntervalToNewTrack(final int[] location, float intervalStart, float intervalEnd, final boolean depthType, final String newTrackName,
			final String newSectionName) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return null;
		}

		int trackId = location[0];
		int sectionId = location[1];

		/*
		 * boolean orientation = SceneGraph.getSectionOrientation(trackId,
		 * sectionId); float dpix = SceneGraph.getSectionDPIX(trackId,
		 * sectionId);
		 * 
		 * int imageId = SceneGraph.getImageIdForSection(trackId, sectionId);
		 * float imageHeight = SceneGraph.getImageHeight(imageId); float
		 * imageWidth = SceneGraph.getImageWidth(imageId);
		 * 
		 * float width; if (orientation == SceneGraph.PORTRAIT) { // true:
		 * portrait width = (imageHeight / dpix) * 2.54f; } else { // false:
		 * landscape width = (imageWidth / dpix) * 2.54f; }
		 */

		// Ops to make duplicate CoreSection but with the same texture model
		CoreGraph cg = CoreGraph.getInstance();
		TrackSceneNode t = cg.getTrack(cg.getCurrentSessionIdx(), trackId);
		if (t == null) {
			System.err.println("---> NULL original TrackSceneNode, return");
			return null;
		}

		CoreSection cs = t.getCoreSectionByGID(sectionId);
		String origName = cs.getName();

		int newTrackId = SceneGraph.getTrackIDByName(cg.getCurrentSession().getName(), newTrackName);
		if (newTrackId == -1) {
			// create a new track using newTrackName
			newTrackId = view.createTrack(newTrackName);
		}

		TrackSceneNode newTrackNode = cg.getTrack(cg.getCurrentSessionIdx(), newTrackId);
		if (newTrackNode == null) {
			System.err.println("---> Null new TrackSceneNode, return");
			return null;
		}

		// C side
		// int newSectionId = SceneGraph.duplicateSection(trackId, sectionId);
		int newSectionId = SceneGraph.duplicateSectionToAnotherTrack(trackId, sectionId, newTrackId);
		if (newSectionId != -1) {
			// assign visibility

			// offset measures if it's absolute_depth (mbsf)
			if (depthType == ABSOLUTE_DEPTH) {
				float sectionTopDepth = SceneGraph.getSectionDepth(newTrackId, newSectionId) / 100.0f;
				intervalStart = intervalStart - sectionTopDepth;
				intervalEnd = intervalEnd - sectionTopDepth;
			}

			// format the interval numbers
			DecimalFormat df = new DecimalFormat("#,###,###,##0.00");
			String headStr = df.format(intervalStart);
			String tailStr = df.format(intervalEnd);

			String newName;
			if (newSectionName == null || newSectionName.equals("")) {
				newName = origName + "_" + headStr + "-" + tailStr;
			} else {
				newName = newSectionName;
			}

			SceneGraph.setSectionIntervalTop(newTrackId, newSectionId, intervalStart * 100);
			SceneGraph.setSectionIntervalBottom(newTrackId, newSectionId, intervalEnd * 100);
			SceneGraph.setSectionName(newTrackId, newSectionId, newName);

			// Java side with CoreGraph
			int imageId = SceneGraph.getImageIdForSection(newTrackId, newSectionId);

			CoreSection sec = new CoreSection(newName, newSectionId);
			newTrackNode.addCoreSection(sec);

			String imageFilePath = SceneGraph.getImageName(imageId);
			CoreSectionImage node = new CoreSectionImage(newTrackNode, imageFilePath, imageId, newName);
			newTrackNode.addChild(node, newSectionId, imageId);
			newTrackNode.Update();
			cg.notifyListeners();
		} else {
			System.err.println("---> [CRExpController] Cannot create a new section!");
		}

		// refresh screen
		CorelyzerApp app = CorelyzerApp.getApp();
		if (app != null) {
			app.updateGLWindows();
		}

		return new int[] { newTrackNode.getId(), newSectionId };
	}

	public static void deleteAll() {
		CorelyzerApp app = CorelyzerApp.getApp();
		if (app != null) {
			String mesg = "Do you want to remove all loaded images?";
			if (JOptionPane.showConfirmDialog(app.getMainFrame(), mesg) == JOptionPane.YES_OPTION) {
				CoreGraph cg = CoreGraph.getInstance();
				if (cg == null) {
					return;
				}

				Iterator<Session> iter = cg.getSessions().iterator();

				// noinspection WhileLoopReplaceableByForEach
				while (iter.hasNext()) {
					Session s = (Session) iter.next();
					deleteSession(s);
				}
			}
		}
	}

	public static void deleteDataset(final WellLogDataSet d) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		int datasetId = d.getId();

		SceneGraph.lock();
		{
			SceneGraph.deleteDataset(datasetId);
		}
		SceneGraph.unlock();

		view.updateGLWindows();
	}

	public static void deleteHole(final String leg, final String site, final String hole) {
		String sessionName = leg + "_" + site;

		CoreGraph cg = CoreGraph.getInstance();
		if (cg == null) {
			return;
		}

		Session s = cg.getSession(sessionName);
		if (s == null) {
			return;
		}

		Vector<TrackSceneNode> toBeDeleted = new Vector<TrackSceneNode>();
		for (TrackSceneNode t : s.getTrackSceneNodes()) {
			String trackName = t.getName();

			// matched hole
			if (trackName.startsWith(hole + "_") || trackName.equals(hole)) {
				// remove sections
				Vector<CoreSection> sectionToBeDeleted = new Vector<CoreSection>();
				for (CoreSection cs : t.getCoreSections()) {
					sectionToBeDeleted.add(cs);
				}

				for (CoreSection cs : sectionToBeDeleted) {
					int[] location = { t.getId(), cs.getId() };
					deleteSection(location);
				}

				toBeDeleted.add(t);
			}
		}

		// remove tracks
		for (TrackSceneNode t : toBeDeleted) {
			s.removeTrack(t);
			SceneGraph.deleteTrack(t.getId());
		}
	}

	public static void deleteLeg(final String leg) {
		CoreGraph cg = CoreGraph.getInstance();
		if (cg == null) {
			return;
		}

		Iterator<Session> iter = cg.getSessions().iterator();

		// noinspection WhileLoopReplaceableByForEach
		while (iter.hasNext()) {
			Session s = (Session) iter.next();
			if (s.getName().startsWith(leg + "_") || s.getName().equals(leg)) {
				deleteSession(s);
			}
		}
	}

	public static void deleteSection(final int[] location) {
		int trackId = location[0];
		int sectionId = location[1];

		// perform range test
		if (trackId < 0 || sectionId < 0) {
			return;
		}

		TrackSceneNode t = null;
		CoreGraph cg = CoreGraph.getInstance();
		for (Session s : cg.getSessions()) {
			t = s.getTrackSceneNodeWithTrackId(trackId);

			if (t != null) {
				break;
			}
		}

		if (t == null) {
			String mesg = "---> [CRExpController] " + "Cannot find the track with trackId: " + trackId;
			// CRNotificationPrompt.notifyGrowl("Status", "DeleteSection",
			// mesg);
			System.out.println(mesg);

			return;
		}

		// need to find index of section in ListModel
		CoreSection cs;
		cs = t.getCoreSectionByGID(sectionId);

		if (cs == null) {
			String mesg = "---> [CRExpController] " + "Cannot find the section with trackId: " + trackId + ", sectionId: " + sectionId;
			// CRNotificationPrompt.notifyGrowl("Status", "DeleteSection",
			// mesg);
			System.out.println(mesg);

			return;
		}

		// Java side remove
		t.removeCoreSection(cs);
		cg.notifyListeners();

		// Native side remove
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		SceneGraph.lock();
		{
			// for native code, use native section id
			SceneGraph.removeSectionImageFromTrack(trackId, sectionId);
		}
		SceneGraph.unlock();
		view.updateGLWindows();
	}

	public static void deleteSession(final Session s) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}
		CoreGraph cg = CoreGraph.getInstance();

		// remove s.tracks
		java.util.Iterator<TrackSceneNode> tIter = s.getTrackSceneNodes().iterator();

		// Native
		// noinspection WhileLoopReplaceableByForEach
		while (tIter.hasNext()) {
			TrackSceneNode t = tIter.next();

			deleteTrack(t);
		}
		// Java
		cg.removeTracks(s);

		// remove s.datasets
		java.util.Iterator<WellLogDataSet> dIter = s.getDatasets().iterator();

		// Native
		// noinspection WhileLoopReplaceableByForEach
		while (dIter.hasNext()) {
			WellLogDataSet d = dIter.next();

			deleteDataset(d);
		}

		// Java
		cg.removeDatasets(s);
		cg.removeSession(s);
	}

	public static void deleteTrack(final TrackSceneNode t) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		SceneGraph.lock();
		{
			t.removeAllCoreSection();
			SceneGraph.deleteTrack(t.getId());
		}
		SceneGraph.unlock();

		view.updateGLWindows();
	}

	/* locate the location (trackId and sectionId) of a section given a name */
	public static int[] getSectionLocationWithSectionName(final String aSectionName) {
		int[] location = null;

		CoreGraph cg = CoreGraph.getInstance();
		for (Session s : cg.getSessions()) {
			for (TrackSceneNode t : s.getTrackSceneNodes()) {
				for (CoreSection cs : t.getCoreSections()) {
					int trackId = t.getId();
					int sectionId = cs.getId();
					String sectionName = cs.getName();

					if (sectionName != null && sectionName.equals(aSectionName)) {
						location = new int[2];
						location[0] = trackId;
						location[1] = sectionId;

						return location;
					}
				}
			}
		}

		return location;
	}

	/* locate the location (trackId and sectionId) of a section given a URL */
	public static int[] getSectionLocationWithURL(final String aUrl) {
		int[] location = null;

		CoreGraph cg = CoreGraph.getInstance();
		for (Session s : cg.getSessions()) {
			for (TrackSceneNode t : s.getTrackSceneNodes()) {
				for (CoreSection cs : t.getCoreSections()) {
					int trackId = t.getId();
					int sectionId = cs.getId();

					int imageId = SceneGraph.getImageIdForSection(trackId, sectionId);
					String url = SceneGraph.getImageURL(imageId);

					// System.out.println("[DEBUG] " + trackId + ", "
					// + sectionId + ", " + imageId + ", " + url);

					if (url != null && url.equals(aUrl)) {
						location = new int[2];
						location[0] = trackId;
						location[1] = sectionId;

						return location;
					}
				}
			}
		}

		return location;
	}

	public static int[] getTrackLocationWithName(final String aName) {
		String currentSessionName = CoreGraph.getInstance().getCurrentSession().getName();
		int trackId = SceneGraph.getTrackIDByName(currentSessionName, aName);

		if (trackId == -1) {
			return null;
		} else {
			return new int[] { trackId, -1 };
		}
	}

	public static void jumpToDepth(final float depth) {
		float dpix = SceneGraph.getCanvasDPIX(0);
		float dpiy = SceneGraph.getCanvasDPIY(0);

		float centerX = SceneGraph.getSceneCenterX();
		float centerY = SceneGraph.getSceneCenterY();

		float trackPos, dpi;
		if (SceneGraph.getDepthOrientation() == SceneGraph.HORIZONTAL) {
			trackPos = centerY;
			dpi = dpiy;
		} else {
			trackPos = centerX;
			dpi = dpix;
		}

		trackPos = 2.54f * trackPos / (dpi * 100.0f); // in meter
		jumpToDepth(depth, trackPos);
	}

	public static void jumpToDepth(final float depth, final float trackPos) {
		jumpToDepth(depth, trackPos, false);
	}

	public static void jumpToDepth(final float depth, final float trackPos, final boolean toTop) {
		try {
			float dpix = SceneGraph.getCanvasDPIX(0);
			float dpiy = SceneGraph.getCanvasDPIY(0);

			float px, py;
			px = depth * 100.0f / 2.54f * dpix;
			py = trackPos * 100.0f / 2.54f * dpiy;

			if (toTop) {
				CorelyzerApp app = CorelyzerApp.getApp();
				CRPreferences p = app.preferences();

				int numOfCols = p.numberOfColumns;
				float dX = SceneGraph.getCanvasWidth(0) * numOfCols / 2;
				px = px + dX;
			}

			SceneGraph.lock();
			{
				if (SceneGraph.getDepthOrientation()) {
					SceneGraph.positionScene(px, py);
				} else {
					SceneGraph.positionScene(py, px);
				}
			}
			SceneGraph.unlock();
		} catch (NumberFormatException e) {
			log("err", "Depth number is not right.");
		}

		CorelyzerApp.getApp().updateGLWindows();
	}

	public static void loadSectionImageFromLIMS(final String aURL, final String localName, final String sessionName, final String trackName, final float depth,
			final float dpix, final float dpiy) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		URL url;
		try {
			url = new URL(aURL);
		} catch (MalformedURLException e) {
			String mesg = "Invalid URL: " + aURL;
			System.err.println(mesg);

			return;
		}

		updateProgress("Loading " + localName);

		String name;
		if (localName != null) {
			name = localName;
		} else {
			String[] toks = url.getPath().split("/");
			name = toks[toks.length - 1];
		}

		boolean hasLocal;
		String localPath;

		if (url.getProtocol().toLowerCase().startsWith("file")) { // local file
			hasLocal = true;
			localPath = url.getPath();
		} else { // remote file, need to download first
			String sp = System.getProperty("file.separator");
			String prefix = view.preferences().getLocalRepositoryPath();
			localPath = prefix + sp + sessionName + sp + "images" + sp + name;
			File localFile = new File(localPath);

			// check if already has the file in localPath
			if (localFile.exists()) {
				hasLocal = true;
			} else {
				FileUtility.createDirsIfNecessary(localFile.getParentFile());

				try {
					hasLocal = URLRetrieval.retrieveLocalCopy(aURL, localPath);
				} catch (IOException e) {
					hasLocal = false;
				}
			}
		}

		if (hasLocal) {
			int lastDotIndex = name.lastIndexOf(".");
			String sectionName = name.substring(0, lastDotIndex);
			// String mesg = "loadSection " + aURL + ", " + sessionName +
			// ", " + trackName +
			// ", " + sectionName;
			String mesg;

			// create "Session" and "Track" accordingly!
			CoreGraph cg = CoreGraph.getInstance();
			Session session = cg.getSession(sessionName);

			if (session == null) {
				session = new Session(sessionName);
				cg.addSession(session);
				cg.notifyListeners();
			}

			TrackSceneNode track = session.getTrackSceneNodeWithName(trackName);
			if (track == null) {
				int trackId = SceneGraph.addTrack(sessionName, trackName);
				track = new TrackSceneNode(trackId);
				track.setName(trackName);
				session.addTrack(track);
				cg.notifyListeners();
			}
			cg.setCurrentTrack(session, track);
			view.getSessionList().setSelectedValue(session, true);
			view.getTrackList().setSelectedValue(track, true);

			int sectionId = view.loadImage(new File(localPath), aURL, sectionName);
			if (sectionId == -1) {
				mesg = "Load local image: " + localPath + " failed";
				System.out.println(mesg);
				// CRNotificationPrompt.notifyGrowl("Execution", "Action",
				// mesg);
			} else {
				// IODP cores section images are taken from top to bottom
				// (portrait)
				SceneGraph.setSectionOrientation(track.getId(), sectionId, SceneGraph.PORTRAIT);

				SceneGraph.setSectionDPI(track.getId(), sectionId, dpix, dpiy);

				int[] location = { track.getId(), sectionId };
				positionSection(location, depth);
			}
		} else {
			String mesg = "Cannot download section image to local";
			System.err.println(mesg);
		}

		view.updateGLWindows();
		updateProgress(localName + " loaded.");
	}

	public static void loadSectionImageWithDPI(final String aURL, final String localName, final String sessionName, final String trackName, final float depth,
			final float dpi) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		URL url;
		try {
			url = new URL(aURL);
		} catch (MalformedURLException e) {
			String mesg = "Invalid URL: " + aURL;
			// CRNotificationPrompt.notifyGrowl("Execution", "Action", mesg);
			System.err.println(mesg);

			return;
		}

		updateProgress("Loading section image " + localName);

		String name;
		if (localName != null) {
			name = localName;
		} else {
			String[] toks = url.getPath().split("/");
			name = toks[toks.length - 1];
		}

		boolean hasLocal;
		String localPath;

		if (url.getProtocol().toLowerCase().startsWith("file")) { // local file
			hasLocal = true;
			localPath = url.getPath();
		} else { // remote file, need to download first
			String sp = System.getProperty("file.separator");
			String prefix = view.preferences().getLocalRepositoryPath();
			localPath = prefix + sp + sessionName + sp + "images" + sp + name;
			File localFile = new File(localPath);

			// check if already has the file in localPath
			if (localFile.exists()) {
				hasLocal = true;
			} else {
				FileUtility.createDirsIfNecessary(localFile.getParentFile());

				try {
					hasLocal = URLRetrieval.retrieveLocalCopy(aURL, localPath);
				} catch (IOException e) {
					hasLocal = false;
				}
			}
		}

		if (hasLocal) {
			// Check if a valid image file or bail...
			File imageFile = new File(localPath);

			try {
				ImageInputStream in = ImageIO.createImageInputStream(imageFile);
				Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
				boolean hasReader = readers.hasNext();

				if (!hasReader) {
					JOptionPane.showMessageDialog(view.getMainFrame(), "'" + localPath + "' is not a valid image file.");

					if (!imageFile.delete()) {
						System.out.println("- Delete image file '" + imageFile + "' failed.");
					}

					return;
				}
			} catch (IOException e) {
				e.printStackTrace();

				JOptionPane.showMessageDialog(view.getMainFrame(), "'" + localPath + "' is not a valid image file.");

				if (!imageFile.delete()) {
					System.out.println("- Delete image file '" + imageFile + "' failed.");
				}
			}

			int lastDotIndex = name.lastIndexOf(".");
			String sectionName = name.substring(0, lastDotIndex);

			// create "Session" and "Track" accordingly!
			CoreGraph cg = CoreGraph.getInstance();
			Session session = cg.getSession(sessionName);

			if (session == null) {
				session = new Session(sessionName);
				cg.addSession(session);
				cg.notifyListeners();
			}

			TrackSceneNode track = session.getTrackSceneNodeWithName(trackName);
			if (track == null) {
				int trackId = SceneGraph.addTrack(sessionName, trackName);
				track = new TrackSceneNode(trackId);
				track.setName(trackName);
				session.addTrack(track);
				cg.notifyListeners();
			}
			cg.setCurrentTrack(session, track);
			view.getSessionList().setSelectedValue(session, true);
			view.getTrackList().setSelectedValue(track, true);

			int sectionId = view.loadImage(new File(localPath), aURL, sectionName);
			if (sectionId == -1) {
				String mesg = "Load local image: " + localPath + " failed";
				System.out.println(mesg);
				// CRNotificationPrompt.notifyGrowl("Execution", "Action",
				// mesg);
			} else {
				SceneGraph.setSectionDPI(track.getId(), sectionId, dpi, dpi);
				SceneGraph.setSectionOrientation(track.getId(), sectionId, SceneGraph.PORTRAIT);

				int[] location = { track.getId(), sectionId };
				positionSection(location, depth);

				// optional locateSection(location);
			}
		} else {
			String mesg = "Cannot download section image to local";
			// CRNotificationPrompt.notifyGrowl("Execution", "LoadSection",
			// mesg);
			System.err.println(mesg);
		}

		view.updateGLWindows();
		updateProgress(localName + " loaded.");
	}

	public static void loadSectionImageWithLength(final String aURL, final String localName, final String sessionName, final String trackName,
			final float depth, final float length) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		URL url;
		try {
			url = new URL(aURL);
		} catch (MalformedURLException e) {
			String mesg = "Invalid URL: " + aURL;
			System.err.println(mesg);

			return;
		}

		updateProgress("Loading " + localName);

		String name;
		if (localName != null) {
			name = localName;
		} else {
			String[] toks = url.getPath().split("/");
			name = toks[toks.length - 1];
		}

		boolean hasLocal;
		String localPath;

		if (url.getProtocol().toLowerCase().startsWith("file")) { // local file
			hasLocal = true;
			localPath = url.getPath();
		} else { // remote file, need to download first
			String sp = System.getProperty("file.separator");
			String prefix = view.preferences().getLocalRepositoryPath();
			localPath = prefix + sp + sessionName + sp + "images" + sp + name;
			File localFile = new File(localPath);

			// check if already has the file in localPath
			if (localFile.exists()) {
				hasLocal = true;
			} else {
				FileUtility.createDirsIfNecessary(localFile.getParentFile());

				try {
					hasLocal = URLRetrieval.retrieveLocalCopy(aURL, localPath);
				} catch (IOException e) {
					hasLocal = false;
				}
			}
		}

		if (hasLocal) {
			// Check if a valid image file or bail...
			File imageFile = new File(localPath);

			try {
				ImageInputStream in = ImageIO.createImageInputStream(imageFile);
				Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
				boolean hasReader = readers.hasNext();

				if (!hasReader) {
					JOptionPane.showMessageDialog(view.getMainFrame(), "'" + localPath + "' is not a valid image file.");

					if (!imageFile.delete()) {
						System.out.println("- Delete image file '" + imageFile + "' failed.");
					}

					return;
				}
			} catch (IOException e) {
				e.printStackTrace();

				JOptionPane.showMessageDialog(view.getMainFrame(), "'" + localPath + "' is not a valid image file.");

				if (!imageFile.delete()) {
					System.out.println("- Delete image file '" + imageFile + "' failed.");
				}
			}

			int lastDotIndex = name.lastIndexOf(".");
			String sectionName = name.substring(0, lastDotIndex);
			// String mesg = "loadSection " + aURL + ", " + sessionName +
			// ", " + trackName +
			// ", " + sectionName;
			String mesg;

			// CRNotificationPrompt.notifyGrowl("Execution", "Action", mesg);
			// System.out.println(mesg);

			// create "Session" and "Track" accordingly!
			CoreGraph cg = CoreGraph.getInstance();
			Session session = cg.getSession(sessionName);

			if (session == null) {
				session = new Session(sessionName);
				cg.addSession(session);
				cg.notifyListeners();
			}

			TrackSceneNode track = session.getTrackSceneNodeWithName(trackName);
			if (track == null) {
				int trackId = SceneGraph.addTrack(sessionName, trackName);
				track = new TrackSceneNode(trackId);
				track.setName(trackName);
				session.addTrack(track);
				cg.notifyListeners();
			}
			cg.setCurrentTrack(session, track);
			view.getSessionList().setSelectedValue(session, true);
			view.getTrackList().setSelectedValue(track, true);

			int sectionId = view.loadImage(new File(localPath), aURL, sectionName);
			if (sectionId == -1) {
				mesg = "Load local image: " + localPath + " failed";
				System.out.println(mesg);
				// CRNotificationPrompt.notifyGrowl("Execution", "Action",
				// mesg);
			} else {
				// IODP cores section images are taken from top to bottom
				// (portrait)
				SceneGraph.setSectionOrientation(track.getId(), sectionId, SceneGraph.PORTRAIT);

				// Estimate DPI from image size and core length
				// - Fall back to default DPI if "length" is negative or no
				// image
				int imageId = SceneGraph.getImageIdForSection(track.getId(), sectionId);
				float dpi;
				if (imageId == -1 || length <= 0) {
					dpi = LIMSImageryDirectory.DEFAULT_DPI;
				} else {
					float imageHeight = SceneGraph.getImageHeight(imageId);
					dpi = imageHeight / (length * 100.0f / 2.54f);
				}

				SceneGraph.setSectionDPI(track.getId(), sectionId, dpi, dpi);

				int[] location = { track.getId(), sectionId };
				positionSection(location, depth);
			}

			view.updateGLWindows();
			updateProgress(localName + " loaded.");
		} else {
			view.updateGLWindows();
			updateProgress("Load " + localName + " failed.");

			String mesg = "Cannot download section image to local from URL:\n" + aURL;
			System.err.println(mesg);

			JOptionPane.showMessageDialog(view.getMainFrame(), mesg);
		}

	}

	public static void loadSectionImageWithURL(final String aURL, final String localName, final String sessionName, final String trackName, final float depth) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		URL url;
		try {
			url = new URL(aURL);
		} catch (MalformedURLException e) {
			String mesg = "Invalid URL: " + aURL;
			// CRNotificationPrompt.notifyGrowl("Execution", "Action", mesg);
			System.err.println(mesg);

			return;
		}

		updateProgress("Loading section image " + localName);

		String name;
		if (localName != null) {
			name = localName;
		} else {
			String[] toks = url.getPath().split("/");
			name = toks[toks.length - 1];
		}

		boolean hasLocal;
		String localPath;

		if (url.getProtocol().toLowerCase().startsWith("file")) { // local file
			hasLocal = true;
			localPath = url.getPath();
		} else { // remote file, need to download first
			String sp = System.getProperty("file.separator");
			String prefix = view.preferences().getLocalRepositoryPath();
			localPath = prefix + sp + sessionName + sp + "images" + sp + name;
			File localFile = new File(localPath);

			// check if already has the file in localPath
			if (localFile.exists()) {
				hasLocal = true;
			} else {
				FileUtility.createDirsIfNecessary(localFile.getParentFile());

				try {
					hasLocal = URLRetrieval.retrieveLocalCopy(aURL, localPath);
				} catch (IOException e) {
					hasLocal = false;
				}
			}
		}

		if (hasLocal) {
			int lastDotIndex = name.lastIndexOf(".");
			String sectionName = name.substring(0, lastDotIndex);

			// create "Session" and "Track" accordingly!
			CoreGraph cg = CoreGraph.getInstance();
			Session session = cg.getSession(sessionName);

			if (session == null) {
				session = new Session(sessionName);
				cg.addSession(session);
				cg.notifyListeners();
			}

			TrackSceneNode track = session.getTrackSceneNodeWithName(trackName);
			if (track == null) {
				int trackId = SceneGraph.addTrack(sessionName, trackName);
				track = new TrackSceneNode(trackId);
				track.setName(trackName);
				session.addTrack(track);
				cg.notifyListeners();
			}
			cg.setCurrentTrack(session, track);
			view.getSessionList().setSelectedValue(session, true);
			view.getTrackList().setSelectedValue(track, true);

			int sectionId = view.loadImage(new File(localPath), aURL, sectionName);
			if (sectionId == -1) {
				String mesg = "Load local image: " + localPath + " failed";
				System.out.println(mesg);
				// CRNotificationPrompt.notifyGrowl("Execution", "Action",
				// mesg);
			} else {
				int[] location = { track.getId(), sectionId };
				positionSection(location, depth);

				// optional locateSection(location);
			}
		} else {
			String mesg = "Cannot download section image to local";
			// CRNotificationPrompt.notifyGrowl("Execution", "LoadSection",
			// mesg);
			System.err.println(mesg);
		}

		view.updateGLWindows();
		updateProgress(localName + " loaded.");
	}

	public static void locateAnnotation(final int trackId, final int sectionId, final int markerId) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		// track offset
		float t_x = SceneGraph.getTrackXPos(trackId);
		float t_y = SceneGraph.getTrackYPos(trackId);

		// section offset
		float cs_x = SceneGraph.getSectionXPos(trackId, sectionId);
		float cs_y = SceneGraph.getSectionYPos(trackId, sectionId);

		// float scaleX = SceneGraph.getCanvasDPIX(0) /
		// SceneGraph.getSectionDPIX(trackId, sectionId);
		// float scaleY = SceneGraph.getCanvasDPIY(0) /
		// SceneGraph.getSectionDPIY(trackId, sectionId);

		float scale = 1.0f;

		int imageId = SceneGraph.getImageIdForSection(trackId, sectionId);

		if (imageId != -1) {
			// int imageWidth = SceneGraph.getImageWidth(imageId);
			// int imageHeight = SceneGraph.getImageHeight(imageId);

			/*
			 * if (view.preferences().isAutoZoom()) { // Considering tiled
			 * display int cols = view.preferences().numberOfColumns; int rows =
			 * view.preferences().numberOfRows;
			 * 
			 * if (imageWidth > imageHeight) { scale = (imageWidth * scaleX) /
			 * (SceneGraph.getCanvasWidth(0) * cols); } else { scale =
			 * (imageHeight * scaleY) / (SceneGraph.getCanvasHeight(0) * rows);
			 * } }
			 */
		}

		float markerOffsetX = SceneGraph.getCoreSectionMarkerXPos(trackId, sectionId, markerId);
		float markerOffsetY = SceneGraph.getCoreSectionMarkerYPos(trackId, sectionId, markerId);

		SceneGraph.lock();
		{
			SceneGraph.bringSectionToFront(trackId, sectionId);
			SceneGraph.highlightSection(trackId, sectionId, true);

			if (SceneGraph.getDepthOrientation()) {
				SceneGraph.positionScene((t_x + cs_x + markerOffsetX), (t_y + cs_y + markerOffsetY));
			} else {
				SceneGraph.positionScene(-(t_y + cs_y + markerOffsetY), (t_x + cs_x + markerOffsetX));
			}

			SceneGraph.scaleScene(scale);

			// set marker focus
			SceneGraph.setCoreSectionMarkerFocus(false);

			SceneGraph.focusedTrack = trackId;
			SceneGraph.focusedTrackSection = sectionId;
			SceneGraph.focusedMarker = markerId;

			SceneGraph.setCoreSectionMarkerFocus(true);
		}
		SceneGraph.unlock();

		CorelyzerApp.getApp().updateGLWindows();
	}

	// handy call to locate selected section
	public static void locateSection(final int trackId, final int sectionId) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		if (trackId == -1 || sectionId == -1) {
			return;
		}

		// track offset
		float t_x = SceneGraph.getTrackXPos(trackId);
		float t_y = SceneGraph.getTrackYPos(trackId);

		// section offset
		float cs_x = SceneGraph.getSectionXPos(trackId, sectionId);
		float cs_y = SceneGraph.getSectionYPos(trackId, sectionId);

		int imageId = SceneGraph.getImageIdForSection(trackId, sectionId);
		if (imageId != -1) {
			int imageWidth = SceneGraph.getImageWidth(imageId);
			int imageHeight = SceneGraph.getImageHeight(imageId);

			float scaleX = SceneGraph.getCanvasDPIX(0) / SceneGraph.getSectionDPIX(trackId, sectionId);
			float scaleY = SceneGraph.getCanvasDPIY(0) / SceneGraph.getSectionDPIY(trackId, sectionId);

			float cs_offset_x = scaleX * imageWidth / 2;
			float cs_offset_y = scaleY * imageHeight / 2;

			if (SceneGraph.getSectionOrientation(trackId, sectionId)) {
				// swap
				// float tmp = cs_offset_x;
				// cs_offset_x = cs_offset_y;
				cs_offset_y = cs_offset_x;
			}

			float vTop = SceneGraph.getSectionIntervalTop(trackId, sectionId);
			float vBottom = SceneGraph.getSectionIntervalBottom(trackId, sectionId);
			float vLength = Math.abs(vBottom - vTop) / 100.0f; // meter

			float dpix = SceneGraph.getCanvasDPIX(0);
			float dpiy = SceneGraph.getCanvasDPIY(0);

			float startDepth, endDepth;

			// in meter
			startDepth = 2.54f * (t_x + cs_x) / (100.0f * dpix) + vTop / 100.0f;
			endDepth = startDepth + vLength;

			float yPos = 2.54f * (t_y + cs_y + cs_offset_y) / (dpiy * 100.0f);

			SceneGraph.lock();
			{
				SceneGraph.bringSectionToFront(trackId, sectionId);
				SceneGraph.highlightSection(trackId, sectionId, true);
			}
			SceneGraph.unlock();

			showDepthRange(startDepth, endDepth, yPos);
		} else { // graph only CoreSection
			float depth = SceneGraph.getSectionDepth(trackId, sectionId);
			float length = SceneGraph.getSectionLength(trackId, sectionId);

			float startDepth = depth / 100.0f;
			float endDepth = (depth + length) / 100.0f;

			float dpiy = SceneGraph.getCanvasDPIY(0);
			float yPos = 2.54f * (t_y + cs_y) / (dpiy * 100.0f);

			SceneGraph.lock();
			{
				SceneGraph.bringSectionToFront(trackId, sectionId);
				SceneGraph.highlightSection(trackId, sectionId, true);
			}
			SceneGraph.unlock();

			showDepthRange(startDepth, endDepth, yPos);
		}
	}

	public static void locateSection(final int[] location) {
		if (location.length >= 2) {
			locateSection(location[0], location[1]);
		}
	}

	public static void locateTrack(final int[] location) {
		if (location == null || location.length < 2 || location[0] == -1) {
			return;
		}

		float posX;
		float posY;
		SceneGraph.lock();
		{
			if (SceneGraph.getDepthOrientation()) {
				posX = SceneGraph.getSceneCenterX();
				posY = SceneGraph.getTrackYPos(location[0]);
			} else {
				posX = -SceneGraph.getTrackYPos(location[0]);
				posY = SceneGraph.getSceneCenterY();
			}

			SceneGraph.positionScene(posX, posY);
		}
		SceneGraph.unlock();

		CorelyzerApp.getApp().updateGLWindows();
	}

	private static void log(final String output, final String message) {
		if (output.equalsIgnoreCase("out")) {
			System.out.println("---> [EXP] " + message);
		} else if (output.equalsIgnoreCase("err")) {
			System.err.println("---> [EXP] " + message);
		} else {
			System.out.println("---> [EXP] " + message);
		}
	}

	public static void moveScene(final float deltaX, final float deltaY) {
		float centerX = SceneGraph.getSceneCenterX();
		float centerY = SceneGraph.getSceneCenterY();

		float px = centerX + deltaX;
		float py = centerY + deltaY;

		SceneGraph.lock();
		{
			SceneGraph.positionScene(px, py);
		}
		SceneGraph.unlock();

		CorelyzerApp.getApp().updateGLWindows();
	}

	public static void navigate(final float[] waypoints) {
		navigate(waypoints, 5);
	}

	public static void navigate(final float[] waypoints, final int pauseSeconds) {
		Thread nav = new Thread(new Runnable() {
			public void run() {
				float last = 0.0f;

				for (int i = 0; i < waypoints.length; i++) {
					log("out", "Jump to " + waypoints[i] + " (m)");

					float step = (waypoints[i] - last) / steps;
					float current = last;

					for (int s = 0; s < steps; s++) {
						float to = current + (s + 1) * step;

						jumpToDepth(to, 0);

						try {
							Thread.sleep(1000 / steps);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					// pause between waypoints
					if (i != waypoints.length - 1) {
						try {
							// log("out", "Now pause.");
							Thread.sleep(1000 * pauseSeconds);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						last = waypoints[i];
					}
				}

				// log("out", "Navigation done");
			}
		});
		nav.start();
	}

	public static void navigate(final Float[] waypointsX, final Float[] waypointsY, final int pauseSeconds) {
		Thread nav = new Thread(new Runnable() {
			public void run() {
				float dpiX = SceneGraph.getCanvasDPIX(0);
				float dpiY = SceneGraph.getCanvasDPIY(0);

				float origX = SceneGraph.getSceneCenterX();
				float origY = SceneGraph.getSceneCenterY();

				if (!SceneGraph.getDepthOrientation()) {
					float t = origX;
					origX = -origY;
					origY = t;
				}

				float lastX = 2.54f * origX / (dpiX * 100.0f);
				float lastY = 2.54f * origY / (dpiY * 100.0f);

				for (int i = 0; i < waypointsX.length; i++) {
					log("out", "Jump to " + waypointsX[i] + " " + waypointsY[i] + " (m)");

					float stepX = (waypointsX[i] - lastX) / steps;
					float stepY = (waypointsY[i] - lastY) / steps;

					float currentX = lastX;
					float currentY = lastY;

					for (int s = 0; s < steps; s++) {
						float toX = currentX + (s + 1) * stepX;
						float toY = currentY + (s + 1) * stepY;

						jumpToDepth(toX, toY);

						try {
							Thread.sleep(1000 / steps);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					// pause between waypoints
					if (i != waypointsX.length - 1) {
						try {
							// log("out", "Now pause.");
							Thread.sleep(1000 * pauseSeconds);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						lastX = waypointsX[i];
						lastY = waypointsY[i];
					}
				}

				// log("out", "Navigation done");
			}
		});
		nav.start();
	}

	/* Position a section in specified depth */
	public static void positionSection(final int[] location, final float depth) {
		int trackId = location[0];
		int sectionId = location[1];

		float intervalTop = SceneGraph.getSectionIntervalTop(trackId, sectionId);

		float depthInPx = (depth * 100.0f - intervalTop) / 2.54f * SceneGraph.getCanvasDPIX(0);

		float yPosInPx = SceneGraph.getSectionYPos(trackId, sectionId);
		SceneGraph.positionSection(trackId, sectionId, depthInPx, yPosInPx);
		SceneGraph.bringSectionToFront(trackId, sectionId);

		CorelyzerApp.getApp().updateGLWindows();
	}

	public static void reset() {
		// Reset loaded sections to their depths according to LIMS* table lookup
		CorelyzerApp app = CorelyzerApp.getApp();

		if (app != null) {
			if (JOptionPane.showConfirmDialog(app.getMainFrame(), "Reset all depths?") != JOptionPane.YES_OPTION) {
				return;
			}

			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			CoreGraph cg = CoreGraph.getInstance();

			for (Session s : cg.getSessions()) {
				String sessionName = s.getName();
				String[] toks = sessionName.split("_");

				String leg = toks[0].trim();
				String site = toks[1].trim();

				for (TrackSceneNode t : s.getTrackSceneNodes()) {
					String trackName = t.getName();
					toks = trackName.split("_");

					String hole = toks[0].trim();
					String core = toks[1].trim();

					for (CoreSection cs : t.getCoreSections()) {
						// Using IODP Janus convention, eg. 1239c_004h_07
						String sectionName = cs.getName();

						// type
						int lastUnderscore = sectionName.lastIndexOf("_");
						String type = sectionName.substring(lastUnderscore - 1, lastUnderscore);

						// section
						String sectionStr = sectionName.substring(lastUnderscore + 1);
						String section = StringUtility.shrinkStringToANumber(sectionStr);

						float depth = dir.getSectionDepth(leg, site, hole, core, type, section);
						float depthInPixels = SceneGraph.getCanvasDPIX(0) * depth * 100.0f / 2.54f;

						String mesg = "leg: " + leg + ", site: " + site + ", hole: " + hole + ", core: " + core + ", type: " + type + ", section: " + section;

						float y_pos = SceneGraph.getSectionYPos(t.getId(), cs.getId());

						if (depth == LIMSImageryDirectory.DEFAULT_DEPTH) {
							System.out.println("[ResetAction]!Resetting '" + mesg + "' to depth: " + depth + " (LIMS_DEFAULT_DEPTH).");
						} else {
							System.out.println("[ResetAction] Resetting '" + mesg + "' to depth: " + depth);
							SceneGraph.positionSection(t.getId(), cs.getId(), depthInPixels, y_pos);
						}
					}
				}
			}

			SceneGraph.resetDefaultTrackYPos();
		}
	}

	public static void scaleSceneCenter(final float scale) {
		CorelyzerApp.getApp().scaleSceneCenter(scale);
	}

	/* Set section's visible interval, depth in meter unit */
	public static void setSectionVisibleRange(final int[] location, final float topDepth, final float bottomDepth) {
		int trackId = location[0];
		int sectionId = location[1];

		// in cm
		SceneGraph.setSectionIntervalTop(trackId, sectionId, topDepth * 100.0f);
		SceneGraph.setSectionIntervalBottom(trackId, sectionId, bottomDepth * 100.0f);

		CorelyzerApp.getApp().updateGLWindows();
	}

	public static void shiftSection(final int[] location, final float shiftDistance) {
		int trackId = location[0];
		int sectionId = location[1];

		float dX = shiftDistance * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0);
		SceneGraph.moveSection(trackId, sectionId, dX, 0.0f);

		CorelyzerApp.getApp().updateGLWindows();
	}

	public static void showDepthRange(final float topDepth, final float bottomDepth) {
		float dpix = SceneGraph.getCanvasDPIX(0);
		float dpiy = SceneGraph.getCanvasDPIY(0);

		float yPos;
		if (SceneGraph.getDepthOrientation()) { // horizontal depth. Unit in
												// meter
			yPos = 2.54f * SceneGraph.getSceneCenterY() / (dpiy * 100);
		} else { // vertical depth. Unit in meter
			yPos = 2.54f * SceneGraph.getSceneCenterX() / (dpix * 100);
		}

		showDepthRange(topDepth, bottomDepth, yPos);
	}

	public static void showDepthRange(final float topDepth, float bottomDepth, final float yPos) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		float depthRange = Math.abs(bottomDepth - topDepth);

		// avoid 0 divident
		if (depthRange == 0.0f) {
			bottomDepth += 0.1f;
			depthRange = 0.1f;
		}

		float depthCenter = (topDepth + bottomDepth) / 2;

		float depthCenterInPixels = depthCenter * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0);
		float depthRangeInPixels = depthRange * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0);
		float yPosInPixels = yPos * 100.0f / 2.54f * SceneGraph.getCanvasDPIY(0);

		// Considering tiled display
		int cols = view.preferences().numberOfColumns;
		int rows = view.preferences().numberOfRows;

		float scale;
		if (SceneGraph.getDepthOrientation()) { // horizontal depth
			scale = depthRangeInPixels / (SceneGraph.getCanvasWidth(0) * cols);
		} else { // vertical depth
			scale = depthRangeInPixels / (SceneGraph.getCanvasHeight(0) * rows);
		}

		SceneGraph.lock();
		{
			if (SceneGraph.getDepthOrientation()) {
				SceneGraph.positionScene(depthCenterInPixels, yPosInPixels);
			} else {
				SceneGraph.positionScene(yPosInPixels, depthCenterInPixels);
			}

			SceneGraph.scaleScene(scale);
		}
		SceneGraph.unlock();

		CorelyzerApp.getApp().updateGLWindows();
	}

	/*
	 * Split a section with cutting off an interval from intervalStart to
	 * intervalEnd Interval unit: meter Origin: top of the core section
	 */
	public static void splitSection(final int[] location, final float intervalStart, final float intervalEnd) {
		CorelyzerApp view = CorelyzerApp.getApp();
		if (view == null) {
			return;
		}

		int trackId = location[0];
		int sectionId = location[1];

		// original section properties
		boolean orientation = SceneGraph.getSectionOrientation(trackId, sectionId);
		float dpix = SceneGraph.getSectionDPIX(trackId, sectionId);

		int imageId = SceneGraph.getImageIdForSection(trackId, sectionId);
		float imageHeight = SceneGraph.getImageHeight(imageId);
		float imageWidth = SceneGraph.getImageWidth(imageId);

		float width;
		if (orientation == SceneGraph.PORTRAIT) { // true: portrait
			width = imageHeight / dpix * 2.54f;
		} else { // false: landscape
			width = imageWidth / dpix * 2.54f;
		}

		// this section remain the first half
		SceneGraph.setSectionIntervalTop(trackId, sectionId, 0);
		SceneGraph.setSectionIntervalBottom(trackId, sectionId, intervalStart * 100);

		// Ops to make duplicate CoreSection but with the same texture model
		CoreGraph cg = CoreGraph.getInstance();
		TrackSceneNode t = cg.getTrack(cg.getCurrentSessionIdx(), trackId);
		if (t == null) {
			System.err.println("---> NULL TrackSceneNode, return");
			return;
		}

		CoreSection cs = t.getCoreSectionByGID(sectionId);
		String origName = cs.getName();
		cs.setName(origName + "_0.0-" + intervalStart);
		t.removeCoreSection(cs);
		t.addCoreSection(cs);

		// C side
		int newSectionId = SceneGraph.duplicateSection(trackId, sectionId);

		if (newSectionId != -1) {
			// assign visibility
			SceneGraph.setSectionIntervalTop(trackId, newSectionId, intervalEnd * 100);
			SceneGraph.setSectionIntervalBottom(trackId, newSectionId, width);

			// Java side with CoreGraph
			imageId = SceneGraph.getImageIdForSection(trackId, newSectionId);

			String newName = origName + "_" + intervalEnd + "-" + width;

			CoreSection sec = new CoreSection(newName, newSectionId);
			t.addCoreSection(sec);

			String imageFilePath = SceneGraph.getImageName(imageId);
			CoreSectionImage node = new CoreSectionImage(t, imageFilePath, imageId, newName);
			t.addChild(node, newSectionId, imageId);
			t.Update();
			cg.notifyListeners();
		} else {
			System.err.println("---> [CRExpController] Cannot create a new section!");
		}

		// refresh screen
		CorelyzerApp app = CorelyzerApp.getApp();
		if (app != null) {
			app.updateGLWindows();
		}
	}

	public static void tour() {
		final CorelyzerApp app = CorelyzerApp.getApp();

		if (app == null) {
			log("err", "No Corelyzer Main app.");
			return;
		}

		// in meter
		final float[] waypoints = { 0.0f, -100.0f, 2000.0f, -10.0f, 20.0f, 0.0f };
		navigate(waypoints);
	}

	private static void updateProgress(final String mesg) {
		CorelyzerApp app = CorelyzerApp.getApp();
		if (app == null) {
			return;
		}

		if (app.isUsePluginUI()) {
			JFrame f = app.getMainFrame();

			if (f instanceof ProgressHandler) {
				JProgressBar pb = (JProgressBar) ((ProgressHandler) f).getProgressUI();

				pb.setString(mesg);
			}
		}
	}
}
