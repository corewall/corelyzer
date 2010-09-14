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
import corelyzer.data.lims.LIMSImageryDirectory;
import corelyzer.remoteControl.server.controller.ControlServerApplication;

public class SplitSectionAction extends AbstractAction {
	public SplitSectionAction(final String[] toks) {
		super(toks);
	}

	public void run() {
		int[] location; // trackId, sectionId

		int splitStartIndex, splitEndIndex;

		if (cmds.length == 9) { // using leg-site-hole-core-type-section naming
			String leg = cmds[1].trim();
			String site = cmds[2].trim();
			String hole = cmds[3].trim();
			String core = cmds[4].trim();
			String type = cmds[5].trim();
			String section = cmds[6].trim();

			splitStartIndex = 7;
			splitEndIndex = 8;

			// First lookup LIMS table, if not found, use Janus URL
			String url = null;
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				url = dir.getSectionURL(leg, site, hole, core, type, section);
			}

			// Fall back to Janus url
			/*
			 * fixme if(url == null) { url =
			 * JanusUtility.generateJanusImageAccessURL(leg, site, hole, core,
			 * type, section); }
			 */

			System.out.println("---> [SplitSection] Section URL is: " + url);

			location = CRExperimentController.getSectionLocationWithURL(url);
		} else if (cmds.length == 4) { // using section name only
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
		} else {
			System.err.println("---> [SplitSection] Some command " + "length that I do not understand");

			return;
		}

		if (location != null) {
			float splitStart = Float.parseFloat(cmds[splitStartIndex].trim());
			float splitEnd = Float.parseFloat(cmds[splitEndIndex].trim());

			CRExperimentController.splitSection(location, splitStart, splitEnd);
		} else {
			if (!isKeepRetry()) {
				return;
			}

			String mesg = "---> [SplitSectionAction] The section is not loaded. Load section and re-submit myself.";
			System.out.println(mesg);

			// chain-of-actions: submit loadSectionAction and then locateSection
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
					System.out.println("---> [SplitSectionAction] Invalid command in Re-initLoadSectionAction");
				}
			}
		}
	}
}
