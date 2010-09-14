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
package corelyzer.remoteControl.server.controller.actions;

import java.util.Vector;

import corelyzer.controller.CRExperimentController;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lims.LIMSImageryDirectory;
import corelyzer.graphics.SceneGraph;
import corelyzer.remoteControl.server.controller.ControlServerApplication;
import corelyzer.ui.CorelyzerApp;

public class TieUpdateAction extends AbstractAction {
	static boolean isLoadFirstCoreRequested = false;
	static boolean isLoadSecondCoreRequested = false;
	static boolean isLoadFirstSectionRequested = false;
	static boolean isLoadSecondSectionRequested = false;
	static boolean didZoom = false;

	static int[] trackNativeIDs = { -1, -1 };

	public static boolean isDidZoom() {
		return didZoom;
	}

	public static boolean isLoadFirstCoreRequested() {
		return isLoadFirstCoreRequested;
	}

	public static boolean isLoadFirstSectionRequested() {
		return isLoadFirstSectionRequested;
	}

	public static boolean isLoadSecondCoreRequested() {
		return isLoadSecondCoreRequested;
	}

	public static boolean isLoadSecondSectionRequested() {
		return isLoadSecondSectionRequested;
	}

	public static void resetNativeCoreIDs() {
		SceneGraph.setTrackHighlightColor(trackNativeIDs[0], 1.0f, 1.0f, 0.0f);
		SceneGraph.setTrackHighlightColor(trackNativeIDs[1], 1.0f, 1.0f, 0.0f);

		TieUpdateAction.trackNativeIDs[0] = -1;
		TieUpdateAction.trackNativeIDs[1] = -1;
	}

	public static void setDidZoom(final boolean didZoom) {
		TieUpdateAction.didZoom = didZoom;
	}

	public static void setLoadFirstCoreRequested(final boolean loadFirstCoreRequested) {
		isLoadFirstCoreRequested = loadFirstCoreRequested;
	}

	public static void setLoadFirstSectionRequested(final boolean loadFirstSectionRequested) {
		isLoadFirstSectionRequested = loadFirstSectionRequested;
	}

	public static void setLoadSecondCoreRequested(final boolean loadSecondCoreRequested) {
		isLoadSecondCoreRequested = loadSecondCoreRequested;
	}

	public static void setLoadSecondSectionRequested(final boolean loadSecondSectionRequested) {
		isLoadSecondSectionRequested = loadSecondSectionRequested;
	}

	public TieUpdateAction(final String[] toks) {
		super(toks);
		actionType = Type.VIEW; // so it won't be blocked by loading jobs
	}

	// Loading images
	private void loadCoreImages(final String leg, final String site, final String hole, final String core, final String offset, final float tieDepth) {
		ControlServerApplication remoteControlServer = ControlServerApplication.getControlServer();

		// Build loading action command
		Vector<String> loadCommand = new Vector<String>();
		loadCommand.add("load_core");
		loadCommand.add(leg);
		loadCommand.add(site);
		loadCommand.add(hole);
		loadCommand.add(core);
		loadCommand.add("-"); // type
		loadCommand.add("-"); // sectionStart
		loadCommand.add("-"); // sectionEnd
		loadCommand.add(offset);
		loadCommand.add(String.valueOf(tieDepth)); // starting depth - ripple
													// outward

		String[] cmd = loadCommand.toArray(new String[loadCommand.size()]);
		LoadCoreAction loadAction = new LoadCoreAction(cmd);
		remoteControlServer.addATaskToExecutor(loadAction);
	}

	// Load section of a core at the depth
	private void loadSectionAtDepth(final String leg, final String site, final String hole, final String core, final float tieDepth,
			final LIMSImageryDirectory dir) {
		String type = dir.getCoreType(leg, site, hole, core);
		String[] coreInfo = dir.getCoreInfo(leg, site, hole, core, String.valueOf(tieDepth));
		String section = coreInfo[3];

		float depth = dir.getSectionMCDDepth(leg, site, hole, core, type, section);

		float length = dir.getSectionLength(leg, site, hole, core, type, String.valueOf(section));
		float dpi = dir.getSectionDPI(leg, site, hole, core, type, String.valueOf(section));
		String url = dir.getSectionURL(leg, site, hole, core, type, String.valueOf(section));

		Vector<String> loadCommand = new Vector<String>();
		loadCommand.add("load_section");
		loadCommand.add(leg);
		loadCommand.add(site);
		loadCommand.add(hole);
		loadCommand.add(core);
		loadCommand.add(type);
		loadCommand.add(section);
		loadCommand.add(String.valueOf(depth));
		loadCommand.add(String.valueOf(length));
		loadCommand.add(String.valueOf(dpi));
		loadCommand.add(url);

		String[] cmd = loadCommand.toArray(new String[loadCommand.size()]);
		LoadSectionAction loadAction = new LoadSectionAction(cmd);

		// Loading images
		ControlServerApplication remoteControlServer = ControlServerApplication.getControlServer();

		remoteControlServer.addATaskToExecutor(loadAction);
	}

	public void run() {
		if (cmds.length == 13) {
			String leg1 = cmds[1].trim();
			String site1 = cmds[2].trim();
			String hole1 = cmds[3].trim();
			String core1 = cmds[4].trim();

			String leg2 = cmds[5].trim();
			String site2 = cmds[6].trim();
			String hole2 = cmds[7].trim();
			String core2 = cmds[8].trim();

			String tieDepth = cmds[11].trim();
			String shift = cmds[12].trim();

			// Show tie depth visual cue
			float tie0 = Float.parseFloat(tieDepth) + Float.parseFloat(shift);
			SceneGraph.setTieDepth(true, tie0);

			// Creat/Switch sessions and tracks first
			CoreGraph cg = CoreGraph.getInstance();

			// 1st
			String sessionName1 = leg1 + "_" + site1;
			String trackName1 = hole1 + "_" + core1;

			Session session = cg.getSession(sessionName1);
			if (session == null) {
				session = new Session(sessionName1);
				cg.addSession(session);
				cg.notifyListeners();
			}

			TrackSceneNode track1 = session.getTrackSceneNodeWithName(trackName1);
			if (track1 == null) {
				int trackId = SceneGraph.addTrack(sessionName1, trackName1);
				track1 = new TrackSceneNode(trackId);
				track1.setName(trackName1);
				session.addTrack(track1);
				cg.notifyListeners();
			}
			TieUpdateAction.trackNativeIDs[0] = track1.getId();

			// 2nd
			String sessionName2 = leg2 + "_" + site2;
			String trackName2 = hole2 + "_" + core2;

			session = cg.getSession(sessionName2);
			if (session == null) {
				session = new Session(sessionName2);
				cg.addSession(session);
				cg.notifyListeners();
			}

			TrackSceneNode track2 = session.getTrackSceneNodeWithName(trackName2);
			if (track2 == null) {
				int trackId = SceneGraph.addTrack(sessionName2, trackName2);
				track2 = new TrackSceneNode(trackId);
				track2.setName(trackName2);
				session.addTrack(track2);
				cg.notifyListeners();
			}
			TieUpdateAction.trackNativeIDs[1] = track2.getId();
			cg.setCurrentTrack(session, track2);

			// Update list UI
			CorelyzerApp view = CorelyzerApp.getApp();
			view.getSessionList().setSelectedValue(session, true);
			view.getTrackList().setSelectedValue(track2, true);

			// ----

			if (tieDepth.equals("-") && shift.equals("-")) { // Clear ties
				SceneGraph.setTieDepth(false, 0.0f);
				SceneGraph.setTrackXPos(trackNativeIDs[0], 0.0f);
				SceneGraph.setTrackXPos(trackNativeIDs[1], 0.0f);

				SceneGraph.setTrackHighlightColor(trackNativeIDs[0], 1.0f, 1.0f, 0.0f);
				SceneGraph.setTrackHighlightColor(trackNativeIDs[1], 1.0f, 1.0f, 0.0f);

				SceneGraph.setTrackHighlight(trackNativeIDs[0], false);
				SceneGraph.setTrackHighlight(trackNativeIDs[1], false);
			} else {
				// only allowing moving the 2nd core
				SceneGraph.setTrackMovable(trackNativeIDs[0], false);
				SceneGraph.setTrackHighlightColor(trackNativeIDs[0], 1.0f, 0.0f, 0.0f); // Red:
																						// un-movable
				SceneGraph.setTrackHighlight(trackNativeIDs[0], true);

				SceneGraph.setTrackMovable(trackNativeIDs[1], true);
				SceneGraph.setTrackHighlightColor(trackNativeIDs[1], 0.0f, 1.0f, 0.0f); // Green:
																						// movable
				SceneGraph.setTrackHighlight(trackNativeIDs[1], true);

				// Loading core images if haven't load all sections in a track
				LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();

				// Load 2 sections at the tie depths first...
				if (!isLoadFirstSectionRequested) {
					this.loadSectionAtDepth(leg1, site1, hole1, core1, tie0, dir);
					isLoadFirstSectionRequested = true;
				}

				if (!isLoadSecondSectionRequested) {
					this.loadSectionAtDepth(leg2, site2, hole2, core2, Float.parseFloat(tieDepth), dir);
					isLoadFirstSectionRequested = true;
				}

				// Load 2 cores (rest of the sections)
				String type1 = dir.getCoreType(leg1, site1, hole1, core1);
				int numberOfLoadedSections = track1.getNumCores();
				int numberOfSectionsInTheCore = dir.getSectionInACore(leg1, site1, hole1, core1, type1).length;

				if (numberOfLoadedSections < numberOfSectionsInTheCore) {
					// Submit load_core command for the first core
					// Load core2(fixed) and core1(pushed into execution queue)
					if (!isLoadFirstCoreRequested) {
						this.loadCoreImages(leg1, site1, hole1, core1, "-", tie0);
						isLoadFirstCoreRequested = true;
					}
				}

				String type2 = dir.getCoreType(leg2, site2, hole2, core2);
				numberOfLoadedSections = track2.getNumCores();
				numberOfSectionsInTheCore = dir.getSectionInACore(leg2, site2, hole2, core2, type2).length;
				if (numberOfLoadedSections < numberOfSectionsInTheCore) {
					if (!isLoadSecondCoreRequested) {
						this.loadCoreImages(leg2, site2, hole2, core2, "-", Float.parseFloat(tieDepth));
						isLoadSecondCoreRequested = true;
					}
				}

				// Actual core(track) shifting
				// Zoom in tieDepth in 1m range
				final float delta = 0.7f;
				int[] location = { track1.getId(), -1 };

				if (!TieUpdateAction.didZoom) {
					CRExperimentController.locateTrack(location);
					CRExperimentController.showDepthRange(tie0 - delta, tie0 + delta);

					TieUpdateAction.setDidZoom(true);
				}

				// Apply shift to the core
				float dpix = SceneGraph.getCanvasDPIX(0);
				float trackXPos = SceneGraph.getTrackXPos(trackNativeIDs[1]);
				float shiftOffset = dpix * Float.parseFloat(shift) * 100.0f / 2.54f;

				SceneGraph.moveTrack(trackNativeIDs[1], (shiftOffset - trackXPos), 0.0f);
			}

			view.updateGLWindows();
		}
	}
}
