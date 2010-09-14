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

import java.io.File;
import java.util.Vector;

import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lims.LIMSImageryDirectory;
import corelyzer.graphics.SceneGraph;

public class AffineTableAction extends AbstractAction {
	public AffineTableAction(final String[] toks) {
		super(toks);
		this.setActionType(Type.VIEW);
	}

	public void run() {
		// Reset TieUpdateAction status
		TieUpdateAction.setLoadFirstCoreRequested(false);
		TieUpdateAction.setLoadSecondCoreRequested(false);
		TieUpdateAction.setLoadFirstSectionRequested(false);
		TieUpdateAction.setLoadSecondSectionRequested(false);
		TieUpdateAction.setDidZoom(false);
		TieUpdateAction.resetNativeCoreIDs();

		if (app != null) {
			SceneGraph.setTieDepth(false, 0.0f); // clear tieDepth (red) line
			app.updateGLWindows();

			String tempAffineTablePath = cmds[1];

			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				File f = new File(tempAffineTablePath);

				System.out.println("- Update affine table from file: " + tempAffineTablePath);

				// fixme hack: wait a sec for Correlator really writes the
				// affine_table file...
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				dir.clearAffineTable();
				dir.loadPlainTextAffineTable(f);

				updateCoreLocations(dir);
			}
		} else {
			System.out.println("[AffineTableAction] Reload affine_table and adjust loaded core in the scene");
		}
	}

	private void updateCoreLocations(final LIMSImageryDirectory dir) {
		CoreGraph cg = CoreGraph.getInstance();
		Vector<Session> sessions = cg.getSessions();

		for (Session s : sessions) {
			String sessionName = s.getName();
			String[] toks = sessionName.split("_");

			if (toks.length >= 2) {
				String leg = toks[0];
				String site = toks[1];

				Vector<TrackSceneNode> tracks = s.getTrackSceneNodes();
				for (TrackSceneNode t : tracks) {
					String trackName = t.getName();
					toks = trackName.split("_");

					if (toks.length >= 2) {
						String hole = toks[0];
						String core = toks[1];
						String type = dir.getCoreType(leg, site, hole, core);

						int nativeTrackID = t.getId();

						// debug: the shift value is not properly passed from
						// tmp affine table...
						// System.out.println("- Shift trackNativeID: " +
						// nativeTrackID +
						// " Leg: " + leg + ", site: " + site + ", hole: " +
						// hole + ", core: " + core +
						// ", shift: " + shift);

						// Reset track's xpos to 0 and apply the shift to all
						// sections in the track
						SceneGraph.setTrackXPos(nativeTrackID, 0.0f);

						String[] sections = dir.getSectionInACore(leg, site, hole, core, type);
						for (String section : sections) {
							// get nativeSectionID from source URL
							String url = dir.getSectionURL(leg, site, hole, core, type, section);
							int nativeSectionID = SceneGraph.getSectionIDFromURL(nativeTrackID, url);

							if (nativeSectionID >= 0) {
								float depth = dir.getSectionMCDDepth(leg, site, hole, core, type, section);

								float x = SceneGraph.getCanvasDPIX(0) * depth * 100.0f / 2.54f;
								float y = SceneGraph.getSectionYPos(nativeTrackID, nativeSectionID);

								SceneGraph.positionSection(nativeTrackID, nativeSectionID, x, y);
							}
						}

						if (app != null) {
							app.updateGLWindows();
						}
					}
				}
			}
		}
	}
}
