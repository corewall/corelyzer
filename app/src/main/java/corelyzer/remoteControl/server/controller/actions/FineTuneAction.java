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

import java.io.BufferedWriter;
import java.util.Vector;

import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lims.LIMSImageryDirectory;
import corelyzer.graphics.SceneGraph;
import corelyzer.remoteControl.server.controller.ControlServerApplication;
import corelyzer.ui.CorelyzerApp;

/** Blocking operation to feed section shifting offset back to calling program */
public class FineTuneAction extends AbstractAction {
	BufferedWriter output;
	FineTuneDialog dialog;

	public FineTuneAction(final String[] toks) {
		super(toks);
	}

	public BufferedWriter getOutput() {
		return output;
	}

	private void loadCoreImages(final String leg, final String site, final String hole, final String core, final boolean isFixed) {
		// Loading images
		ControlServerApplication remoteControlServer = ControlServerApplication.getControlServer();
		LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();

		String[] coreInfo = dir.getCoreInfo(leg, site, hole, core);
		String type = coreInfo[0];
		String sectionStart = coreInfo[1];
		String sectionEnd = coreInfo[2];

		// Build loading action commands for each section
		int numStart1 = Integer.parseInt(sectionStart);
		int numEnd1 = Integer.parseInt(sectionEnd);
		for (int i = numStart1; i <= numEnd1; i++) {
			float depth = dir.getSectionDepth(leg, site, hole, core, type, String.valueOf(i));
			float length = dir.getSectionLength(leg, site, hole, core, type, String.valueOf(i));
			float dpi = dir.getSectionDPI(leg, site, hole, core, type, String.valueOf(i));
			String url = dir.getSectionURL(leg, site, hole, core, type, String.valueOf(i));

			if (i == numStart1) {
				if (isFixed) {
					this.dialog.setCoreAOrigDepth(depth);
				} else {
					this.dialog.setCoreBOrigDepth(depth);
				}
			}

			Vector<String> loadCommand = new Vector<String>();
			loadCommand.add("load_section");
			loadCommand.add(leg);
			loadCommand.add(site);
			loadCommand.add(hole);
			loadCommand.add(core);
			loadCommand.add(type);
			loadCommand.add(String.valueOf(i)); // section
			loadCommand.add(String.valueOf(depth));
			loadCommand.add(String.valueOf(length));
			loadCommand.add(String.valueOf(dpi));
			loadCommand.add(url);

			// String sectionName = LIMSImageryDirectory.getImageName(leg, site,
			// hole, core, type, String.valueOf(i));
			// System.out.println("[FineTune] Loading '" + sectionName +
			// "' at depth: " + depth);

			String[] cmd = loadCommand.toArray(new String[loadCommand.size()]);
			LoadSectionAction loadAction = new LoadSectionAction(cmd);
			if (dialog != null) {
				loadAction.setStatusLabel(dialog.getStatusLabel());
			}

			remoteControlServer.addATaskToExecutor(loadAction);
		}
	}

	public void run() {
		// require 2 "core-level" ID descriptions:
		// fine_tune leg1 site1 hole1 core1 type1 sectionStart1 sectionEnd1
		// 0 1 2 3 4 5 6 7
		// leg2 site2 hole2 core2 type2 sectionStart2 sectionEnd2
		// 8 9 10 11 12 13 14
		// tieDepth
		// 15
		if (cmds.length == 16) {
			// workaround here: since correlator is passing parameters in wrong
			// order
			String leg2 = cmds[1].trim();
			String site2 = cmds[2].trim();
			String hole2 = cmds[3].trim();
			String core2 = cmds[4].trim();
			// String type2 = cmds[5].trim();
			// String sectionStart2 = cmds[6].trim();
			// String sectionEnd2 = cmds[7].trim();

			String leg1 = cmds[8].trim();
			String site1 = cmds[9].trim();
			String hole1 = cmds[10].trim();
			String core1 = cmds[11].trim();
			// String type1 = cmds[12].trim();
			// String sectionStart1 = cmds[13].trim();
			// String sectionEnd1 = cmds[14].trim();

			String tieDepth = cmds[15].trim();

			// Creat/Switch sessions and tracks first
			CoreGraph cg = CoreGraph.getInstance();
			int[] trackNativeIDs = { -1, -1 };

			// 1st
			String sessionName1 = leg1 + "_" + site1;
			String trackName1 = hole1 + "_" + core1;

			Session session = cg.getSession(sessionName1);
			if (session == null) {
				session = new Session(sessionName1);
				cg.addSession(session);
				cg.notifyListeners();
			}

			TrackSceneNode track = session.getTrackSceneNodeWithName(trackName1);
			if (track == null) {
				int trackId = SceneGraph.addTrack(sessionName1, trackName1);
				track = new TrackSceneNode(trackId);
				track.setName(trackName1);
				session.addTrack(track);
				cg.notifyListeners();
			}
			trackNativeIDs[0] = track.getId();

			// 2nd
			String sessionName2 = leg2 + "_" + site2;
			String trackName2 = hole2 + "_" + core2;

			session = cg.getSession(sessionName2);
			if (session == null) {
				session = new Session(sessionName2);
				cg.addSession(session);
				cg.notifyListeners();
			}

			track = session.getTrackSceneNodeWithName(trackName2);
			if (track == null) {
				int trackId = SceneGraph.addTrack(sessionName2, trackName2);
				track = new TrackSceneNode(trackId);
				track.setName(trackName2);
				session.addTrack(track);
				cg.notifyListeners();
			}
			trackNativeIDs[1] = track.getId();
			cg.setCurrentTrack(session, track);

			// only allowing moving the 2nd core
			SceneGraph.setTrackMovable(trackNativeIDs[0], false);
			SceneGraph.setTrackHighlightColor(trackNativeIDs[0], 1.0f, 0.0f, 0.0f);

			SceneGraph.setTrackMovable(trackNativeIDs[1], true);
			SceneGraph.setTrackHighlightColor(trackNativeIDs[1], 0.0f, 1.0f, 0.0f);

			// Show tie depth visual cue
			SceneGraph.setTieDepth(true, Float.valueOf(tieDepth));

			// Update list UI
			CorelyzerApp view = CorelyzerApp.getApp();
			view.getSessionList().setSelectedValue(session, true);
			view.getTrackList().setSelectedValue(track, true);

			// Init and hand over to fine-tune panel for status tracking
			dialog = new FineTuneDialog();
			dialog.setOutputWriter(output);
			dialog.setCoreA(leg1, site1, hole1, core1, trackNativeIDs[0]);
			dialog.setCoreB(leg2, site2, hole2, core2, trackNativeIDs[1]);
			dialog.setTieDepth(Float.valueOf(tieDepth));
			dialog.pack();
			dialog.setAlwaysOnTop(true);
			dialog.setVisible(true);

			// Loading core images
			// Load core2(fixed) and core1(pushed into execution queue)
			this.loadCoreImages(leg2, site2, hole2, core2, false);
			this.loadCoreImages(leg1, site1, hole1, core1, true);
		}
	}

	public void setOutput(final BufferedWriter output) {
		this.output = output;
	}
}
