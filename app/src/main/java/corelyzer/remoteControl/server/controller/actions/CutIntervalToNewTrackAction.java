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

import java.net.MalformedURLException;
import java.net.URL;

import corelyzer.controller.CRExperimentController;
import corelyzer.data.ChatGroup;
import corelyzer.data.MarkerType;
import corelyzer.data.lims.IODPOperationController;
import corelyzer.data.lims.LIMSImageryDirectory;
import corelyzer.graphics.SceneGraph;
import corelyzer.remoteControl.server.controller.ControlServerApplication;

public class CutIntervalToNewTrackAction extends AbstractAction {
	public CutIntervalToNewTrackAction(final String[] toks) {
		super(toks);
	}

	private void createTieAnnotation(final int[] location, final String hole, final String core, final String section, final String nextHole,
			final String nextCore, final float tiePoint, final boolean isEnd) {
		// Track & section position
		int trackId = location[0];
		int sectionId = location[1];

		float trackX = SceneGraph.getTrackXPos(trackId);
		float trackY = SceneGraph.getTrackYPos(trackId);
		float sectionX = SceneGraph.getSectionXPos(trackId, sectionId);
		float sectionY = SceneGraph.getSectionYPos(trackId, sectionId);

		// position
		float canvas_dpix = SceneGraph.getCanvasDPIX(0);
		float canvas_dpiy = SceneGraph.getCanvasDPIY(0);

		// Annotation position
		float xpos, ypos;
		float markerPosX, markerPosY;
		float iconPosX, iconPosY;

		xpos = tiePoint * 100.0f * canvas_dpix / 2.54f;
		if (isEnd) {
			ypos = 0.0f;
		} else {
			ypos = canvas_dpiy * SceneGraph.getSectionWidth(trackId, sectionId) / 2.54f;
		}

		markerPosX = xpos;
		markerPosY = ypos + trackY + sectionY;

		if (isEnd) {
			iconPosX = markerPosX - trackX - sectionX;
			iconPosX = iconPosX - 36f * 1.5f / 2.0f; // centralized (half of
														// marker icon width in
														// scene space)
			iconPosY = ypos - 2.0f * canvas_dpiy;
		} else {
			iconPosX = markerPosX - trackX - sectionX;
			iconPosY = ypos + 2.0f * canvas_dpiy;
		}

		// Create annotation
		int markerId = SceneGraph.createCoreSectionMarker(trackId, sectionId, ChatGroup.TIE, MarkerType.CORE_POINT_MARKER, markerPosX, markerPosY);
		if (markerId < 0) {
			return;
		}

		// Label
		String text = "Splice Tie";
		SceneGraph.setCoreSectionMarkerText(trackId, sectionId, markerId, text);

		text = "<- " + hole + "_" + core + " | " + nextHole + "_" + nextCore + " ->";
		SceneGraph.setCoreSectionMarkerRelationText(trackId, sectionId, markerId, text);

		SceneGraph.setCoreSectionMarkerVertex(trackId, sectionId, markerId, iconPosX, iconPosY, 0.0f, 0.0f, 0.0f, 0.0f);

		// Annotation contents: from file url or text string?
		/*
		 * String url = toks[9].trim(); String local = "";
		 * 
		 * try { URL u = new URL(url);
		 * 
		 * if(u.getProtocol().equals("file")) { local = u.getFile(); } } catch
		 * (MalformedURLException e) { // not a valid url, put the reset string
		 * into a new annot file
		 * System.out.println("---> Creating a new annotation file.");
		 * 
		 * local = createAnAnnotationFile(titles, toks, trackId, sectionId,
		 * markerId); try { url = (new File(local)).toURL().toString(); } catch
		 * (MalformedURLException e1) { url = "file:////" + local; } }
		 * 
		 * SceneGraph.setCoreSectionMarkerURL( trackId, sectionId, markerId,
		 * url); SceneGraph.setCoreSectionMarkerLocal( trackId, sectionId,
		 * markerId, local);
		 */
	}

	public void run() {
		int[] location; // trackId, sectionId

		int splitStartIndex, splitEndIndex;
		int newTrackNameIndex;

		String leg = "";
		String site = "";
		String hole = "";
		String core = "";
		String type = "";
		String section = "";

		if (cmds.length >= 10) { // using leg-site-hole-core-type-section naming
			leg = cmds[1].trim();
			site = cmds[2].trim();
			hole = cmds[3].trim();
			core = cmds[4].trim();
			type = cmds[5].trim();
			section = cmds[6].trim();

			splitStartIndex = 7;
			splitEndIndex = 8;
			newTrackNameIndex = 9;

			// First lookup LIMS table, if not found, use Janus URL
			String url = null;
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				url = dir.getSectionURL(leg, site, hole, core, type, section);
			}

			location = CRExperimentController.getSectionLocationWithURL(url);
		} else if (cmds.length == 5) { // using section name only
			boolean isURL;
			try {
				new URL(cmds[1].trim());
				isURL = true;
			} catch (MalformedURLException e) {
				isURL = false;
			}

			if (isURL) {
				location = CRExperimentController.getSectionLocationWithURL(cmds[1].trim());
			} else {
				String sectionName = cmds[1].trim();
				location = CRExperimentController.getSectionLocationWithSectionName(sectionName);
			}

			splitStartIndex = 2;
			splitEndIndex = 3;
			newTrackNameIndex = 4;
		} else {
			System.err.println("---> [CutIntervalToNewTrack] Some command " + "length that I do not understand");

			return;
		}

		if (location != null) {
			float intervalStart = Float.parseFloat(cmds[splitStartIndex].trim());
			float intervalEnd = Float.parseFloat(cmds[splitEndIndex].trim());
			String newTrackName = cmds[newTrackNameIndex].trim();

			if (cmds.length >= 11) { // has shift info
				// Original section
				float shift = 0.0f;

				if (IODPOperationController.isApplyAffineTableToOriginalSections()) {

					if (IODPOperationController.getAffineShiftSource() == IODPOperationController.ShiftSource.FROM_SPLICE) {
						shift = Float.parseFloat(cmds[10]);
					} else if (IODPOperationController.getAffineShiftSource() == IODPOperationController.ShiftSource.FROM_AFFINE) {
						LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
						shift = dir.getAffineTableShift(leg, site, hole, core);
					}

					// CRExperimentController.shiftSection(location, shift);
					LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
					float depthPos;
					depthPos = dir.getSectionDepth(leg, site, hole, core, type, section);
					depthPos += shift;
					CRExperimentController.positionSection(location, depthPos);
				}

				// Spliced section
				int[] newLocation = CRExperimentController.cutIntervalToNewTrack(location, intervalStart, intervalEnd, CRExperimentController.DOWNCORE_DEPTH,
						newTrackName);

				// Add an ending annotation if the intervalEnd != sectionLength
				LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
				if (dir != null) {
					/*
					 * if(intervalStart != 0.0f) { float mbsf = shift +
					 * dir.getSectionDepth(leg, site, hole, core, type, section)
					 * + intervalStart; this.createTieAnnotation(newLocation,
					 * hole, core, section, mbsf, false); }
					 */

					if (intervalEnd != dir.getSectionLength(leg, site, hole, core, type, section)) {
						// Lookup splice table
						String[] nextCore = dir.findNextCoreInATie(leg, site, hole, core, section);

						if (nextCore != null) {
							float mbsf = shift + dir.getSectionDepth(leg, site, hole, core, type, section) + intervalEnd;
							this.createTieAnnotation(newLocation, hole, core, section, nextCore[0], nextCore[1], mbsf, true);
						}
					}
				}
			}
		} else {
			if (!isKeepRetry()) {
				return;
			}

			// Chain-of-actions: submit loadSectionAction and then
			// CutIntervalToNewTrackAction
			// LoadSectionAction and then re-submit myself(this)
			ControlServerApplication app = ControlServerApplication.getControlServer();

			if (app != null) {
				String[] toks = this.generateLoadSectionCommand();

				if (toks != null) {
					LoadSectionAction loadAction = new LoadSectionAction(toks);
					app.addATaskToExecutor(loadAction);

					// re-submit myself
					app.addATaskToExecutor(this);
				} else {
					System.out.println("---> [CutIntervalToNewTrack] Invalid command in Re-initLoadSectionAction");
				}
			}
		}
	}
}
