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

import corelyzer.data.lims.LIMSImageryDirectory;
import corelyzer.remoteControl.server.controller.ControlServerApplication;

public class LoadCoreAction extends AbstractAction {
	public LoadCoreAction(final String[] toks) {
		super(toks);
	}

	private Integer[] generateRippleOrderedSections(final int begin, final int end, final int init) {
		// System.out.println("- Generate ripple from: " + init + " between " +
		// begin + " to " + end);

		Vector<Integer> ret = new Vector<Integer>();
		ret.add(init);

		// ripple indices
		for (int i = begin; i <= Math.abs(end - begin); i++) {
			int before = init - i;
			int after = init + i;

			if (before >= begin && before <= end) {
				if (!ret.contains(before)) {
					ret.add(before);
				}
			}

			if (after >= begin && after <= end) {
				if (!ret.contains(after)) {
					ret.add(after);
				}
			}
		}

		return ret.toArray(new Integer[ret.size()]);
	}

	// Load the section having tie point first (ripple outward)
	private void loadCoreImages(final String leg, final String site, final String hole, final String core, final String type, final String sectionStart,
			final String sectionEnd, final String startSection, final String shiftDepth) {
		// Loading images
		ControlServerApplication remoteControlServer = ControlServerApplication.getControlServer();
		LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();

		// Build loading action commands for each section
		int begin = Integer.parseInt(sectionStart);
		int end = Integer.parseInt(sectionEnd);
		int init = Integer.parseInt(startSection);

		Integer[] rippleOrderedSections = this.generateRippleOrderedSections(begin, end, init);

		for (int section : rippleOrderedSections) {
			float depth = dir.getSectionDepth(leg, site, hole, core, type, String.valueOf(section));
			depth += Float.parseFloat(shiftDepth);

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
			loadCommand.add(String.valueOf(section)); // section
			loadCommand.add(String.valueOf(depth));
			loadCommand.add(String.valueOf(length));
			loadCommand.add(String.valueOf(dpi));
			loadCommand.add(url);

			String[] cmd = loadCommand.toArray(new String[loadCommand.size()]);
			LoadSectionAction loadAction = new LoadSectionAction(cmd);

			remoteControlServer.addATaskToExecutor(loadAction);
		}
		System.out.println("");
	}

	public void run() {
		// load_core <leg> <site> <hole> <core> <type> <startSection#>
		// <endSection#> <shift> <startDepth>
		if (cmds.length >= 8) {
			String leg = cmds[1].trim();
			String site = cmds[2].trim();
			String hole = cmds[3].trim();
			String core = cmds[4].trim();

			String type = cmds[5].trim();
			String sectionStart = cmds[6].trim();
			String sectionEnd = cmds[7].trim();

			String shiftSrc = "default(0.0)";
			String shiftDepth = "0.0";
			String startDepth = "-999";
			String startSectionID = "1";

			if (cmds.length > 8) {
				shiftDepth = cmds[8].trim();
				shiftSrc = "commandline";
			}

			if (cmds.length > 9) {
				startDepth = cmds[9].trim();
			}

			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				String[] coreInfo = dir.getCoreInfo(leg, site, hole, core, startDepth); // stop
																						// here:
																						// core
																						// catcher:
																						// CC

				// type
				if (type.equals("-")) {
					type = coreInfo[0];
				}

				// 1st section
				if (sectionStart.equals("-")) {
					sectionStart = coreInfo[1];
				}

				// last section
				if (sectionEnd.equals("-")) {
					sectionEnd = coreInfo[2];
				}

				// section ID in the depth(startDepth)
				// - for ripple-like sections loading
				startSectionID = coreInfo[3];

				// shift in affine table
				if (shiftDepth.equals("-")) {
					shiftDepth = "" + dir.getAffineTableShift(leg, site, hole, core);
					shiftSrc = "affine_table";
				}
			}

			System.out.println("- Load core - leg: " + leg + ", site: " + site + ", hole: " + hole + ", core: " + core + " with shift: " + shiftDepth
					+ " from: " + shiftSrc + ", loaded from section in depth: " + startDepth + " at section: " + startSectionID);

			this.loadCoreImages(leg, site, hole, core, type, sectionStart, sectionEnd, startSectionID, shiftDepth);
		}
	}
}
