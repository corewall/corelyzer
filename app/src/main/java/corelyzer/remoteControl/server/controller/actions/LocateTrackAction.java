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

import corelyzer.controller.CRExperimentController;

public class LocateTrackAction extends AbstractAction {
	// section id specification:
	// leg, site, hole, core, type, section

	public LocateTrackAction(final String[] toks) {
		super(toks);
	}

	public void run() {
		int[] location; // trackId, sectionId

		String trackName;
		if (cmds.length == 3) { // using leg-site-hole-core-section naming
			// String leg = cmds[1].trim();
			// String site = cmds[2].trim();
			String hole = cmds[1].trim();
			String core = cmds[2].trim();

			trackName = hole + "_" + core;
		} else if (cmds.length == 2) { // using track name only
			trackName = cmds[1].trim();
		} else {
			System.err.println("---> [LocateTrack] Some command " + "length that I do not understand");

			return;
		}
		location = CRExperimentController.getTrackLocationWithName(trackName);

		if (location != null) {
			CRExperimentController.locateTrack(location);
		} else {
			System.out.println("---> [LocateTrack] The track is not loaded.");

			String mesg = "Selected track is not loaded";
			// CRNotificationPrompt.notifyGrowl("Status", "Corelyzer Warning",
			// mesg);
			System.out.println(mesg);
		}
	}
}
