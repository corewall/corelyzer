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
package corelyzer.data.lims;

import java.awt.Window;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.table.TableModel;

import corelyzer.remoteControl.server.controller.ControlServerApplication;
import corelyzer.remoteControl.server.controller.actions.CutIntervalToNewTrackAction;
import corelyzer.remoteControl.server.controller.actions.LoadSectionAction;

public class IODPOperationController {
	public static enum ShiftSource {
		FROM_SPLICE, FROM_AFFINE
	}

	private static LIMSImageryDirectory dir;
	private static ControlServerApplication app;

	private static String spliceTrackName;
	// For applying splice table
	static boolean applyAffineTableToOriginalSections = true;
	static ShiftSource affineShiftSource = ShiftSource.FROM_AFFINE;

	public static void applyAffineTable(final Window parent, final LIMSImageryDirectory dir, final ControlServerApplication app) {
		if (dir == null) {
			System.err.println("[IODPOPCtrl - affine] No LIMSdir, return");
			return;
		}

		if (app == null) {
			System.err.println("[IODPOPCtrl - affine] No exec server, return.");
			return;
		}

		// 1st: load related images
		// 2nd: lookup affine table and apply shifts

		// every core
		for (int i = 0; i < dir.getNumberOfAffineTableEntries(); i++) {
			// leg, site, hole, core, type, offset, apply;
			String[] toks = new String[dir.getNumberOfAffineTableColumns()];

			for (int j = 0; j < dir.getNumberOfAffineTableColumns(); j++) {
				toks[j] = dir.getAffineTableCell(i, j);
			}

			String leg = toks[LIMSImageryDirectory.LEG_INDEX].trim();
			String site = toks[LIMSImageryDirectory.SITE_INDEX].trim();
			String hole = toks[LIMSImageryDirectory.HOLE_INDEX].trim();
			String core = toks[LIMSImageryDirectory.CORE_INDEX].trim();
			String type = toks[LIMSImageryDirectory.TYPE_INDEX].trim();
			// String offset = toks[LIMSImageryDirectory.OFFSET_INDEX].trim();
			// not used: String apply =
			// toks[LIMSImageryDirectory.APPLY_INDEX].trim();

			String[] sectionIds = dir.getSectionInACore(leg, site, hole, core, type);

			// apply all sections in this core
			for (String section : sectionIds) {
				float length = dir.getSectionLength(leg, site, hole, core, type, section);
				float dpi = dir.getSectionDPI(leg, site, hole, core, type, section);
				String urlStr = dir.getSectionURL(leg, site, hole, core, type, section);
				float depth = dir.getSectionMCDDepth(leg, site, hole, core, type, section);

				// Submit to exec queue
				String[] cmd = { "load_section", leg, site, hole, core, type, section, String.valueOf(depth), String.valueOf(length), String.valueOf(dpi),
						urlStr };

				LoadSectionAction loadAction = new LoadSectionAction(cmd);
				app.addATaskToExecutor(loadAction);
			}
		}
	}

	public static void applySelectedSections(final Window parent, final LIMSImageryDirectory dir, final ControlServerApplication app, final TableModel model,
			final int[] rows) {
		for (int row : rows) {
			String leg = (String) model.getValueAt(row, LIMSImageryDirectory.LEG_INDEX);
			String site = (String) model.getValueAt(row, LIMSImageryDirectory.SITE_INDEX);
			String hole = (String) model.getValueAt(row, LIMSImageryDirectory.HOLE_INDEX);
			String core = (String) model.getValueAt(row, LIMSImageryDirectory.CORE_INDEX);
			String type = (String) model.getValueAt(row, LIMSImageryDirectory.TYPE_INDEX);
			String section = (String) model.getValueAt(row, LIMSImageryDirectory.SECTION_INDEX);

			float depth = dir.getSectionDepth(leg, site, hole, core, type, section);
			float length = dir.getSectionLength(leg, site, hole, core, type, section);
			float dpi = dir.getSectionDPI(leg, site, hole, core, type, section);
			String url = dir.getSectionURL(leg, site, hole, core, type, section);

			try {
				new URL(url);
			} catch (MalformedURLException e) {
				// might be relative path
				File f = new File(dir.getSectionListFileStr());
				File parentDir = f.getParentFile();
				File aFile = new File(parentDir, url);
				try {
					url = aFile.toURI().toURL().toString();
				} catch (MalformedURLException e1) {
					System.err.println("oh no! still no URL? " + aFile.getAbsolutePath());
					e1.printStackTrace();
				}
			}

			// compose load_section command and submit
			String mesg = "Loading\t" + url + "\tLENGTH: " + length + "\tDEPTH: " + depth;
			System.out.println(mesg);

			String[] cmd = { "load_section", leg, site, hole, core, type, section, String.valueOf(depth), String.valueOf(length), String.valueOf(dpi), url };

			if (app != null) {
				LoadSectionAction loadAction = new LoadSectionAction(cmd);
				app.addATaskToExecutor(loadAction);
			}
		}
	}

	public static void applySpliceTable(final Window parent, final LIMSImageryDirectory aDir, final ControlServerApplication anApp, final String newTrackName) {
		if (aDir == null) {
			System.err.println("[IODPOPCtrl - splice] No LIMSdir, return");
			return;
		}

		if (anApp == null) {
			System.err.println("[IODPOPCtrl - splice] No exec server, return.");
			return;
		}

		dir = aDir;
		app = anApp;
		spliceTrackName = newTrackName;

		for (int i = 0; i < dir.getNumberOfSpliceTableEntries(); i++) {
			// local vars
			String leg1 = dir.getSpliceTableCell(i, LIMSImageryDirectory.LEG1_INDEX);
			String site1 = dir.getSpliceTableCell(i, LIMSImageryDirectory.SITE1_INDEX);
			String hole1 = dir.getSpliceTableCell(i, LIMSImageryDirectory.HOLE1_INDEX);
			String core1 = dir.getSpliceTableCell(i, LIMSImageryDirectory.CORE1_INDEX);
			String type1 = dir.getSpliceTableCell(i, LIMSImageryDirectory.TYPE1_INDEX);
			String section1 = dir.getSpliceTableCell(i, LIMSImageryDirectory.SECT1_INDEX);
			// String top1 = dir.getSpliceTableCell(i,
			// LIMSImageryDirectory.TOP1_INDEX);
			// String bottom1 = dir.getSpliceTableCell(i,
			// LIMSImageryDirectory.BTM1_INDEX);
			String mbsf1 = dir.getSpliceTableCell(i, LIMSImageryDirectory.MBSF1_INDEX);
			String mcd1 = dir.getSpliceTableCell(i, LIMSImageryDirectory.MCD1_INDEX);

			String op = dir.getSpliceTableCell(i, LIMSImageryDirectory.OP_INDEX);

			String leg2 = dir.getSpliceTableCell(i, LIMSImageryDirectory.LEG2_INDEX);
			String site2 = dir.getSpliceTableCell(i, LIMSImageryDirectory.SITE2_INDEX);
			String hole2 = dir.getSpliceTableCell(i, LIMSImageryDirectory.HOLE2_INDEX);
			String core2 = dir.getSpliceTableCell(i, LIMSImageryDirectory.CORE2_INDEX);
			String type2 = dir.getSpliceTableCell(i, LIMSImageryDirectory.TYPE2_INDEX);
			String section2 = dir.getSpliceTableCell(i, LIMSImageryDirectory.SECT2_INDEX);
			// String top2 = dir.getSpliceTableCell(i,
			// LIMSImageryDirectory.TOP2_INDEX);
			// String bottom2 = dir.getSpliceTableCell(i,
			// LIMSImageryDirectory.BTM2_INDEX);
			String mbsf2 = dir.getSpliceTableCell(i, LIMSImageryDirectory.MBSF2_INDEX);
			String mcd2 = dir.getSpliceTableCell(i, LIMSImageryDirectory.MCD2_INDEX);

			if (i == 0) { // the first line
				float shift;
				if (getAffineShiftSource() == ShiftSource.FROM_AFFINE) {
					shift = dir.getAffineTableShift(leg1, site1, hole1, core1);
				} else {
					shift = Float.parseFloat(mcd1) - Float.parseFloat(mbsf1);
				}

				loadFromBeginOfTheCoreTillThisSection(leg1, site1, hole1, core1, type1, section1, shift);

				// boundaries coreDepth
				float startCoreDepth1 = 0.0f;
				float endCoreDepth1 = Float.valueOf(mbsf1) - dir.getSectionDepth(leg1, site1, hole1, core1, type1, section1);

				loadThisSection(leg1, site1, hole1, core1, type1, section1, startCoreDepth1, endCoreDepth1, shift);
			} else { // the rest lines
				String leg3 = dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.LEG2_INDEX);
				String site3 = dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.SITE2_INDEX);
				String hole3 = dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.HOLE2_INDEX);
				String core3 = dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.CORE2_INDEX);
				String type3 = dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.TYPE2_INDEX);
				String section3 = dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.SECT2_INDEX);
				// top3 = dir.getSpliceTableCell(i-1,
				// LIMSImageryDirectory.TOP2_INDEX);
				// bottom3 = dir.getSpliceTableCell(i-1,
				// LIMSImageryDirectory.BTM2_INDEX);
				String mbsf3 = dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.MBSF2_INDEX);
				dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.MCD2_INDEX);

				if (leg1.equals(leg3) && site1.equals(site3) && hole1.equals(hole3) && core1.equals(core3) && type1.equals(type3) && section1.equals(section3)) { // the
																																									// same
																																									// section

					float startDepth = Float.valueOf(mbsf3) - dir.getSectionDepth(leg1, site1, hole1, core1, type1, section1);
					float endDepth = Float.valueOf(mbsf1) - dir.getSectionDepth(leg1, site1, hole1, core1, type1, section1);

					float shift;
					if (getAffineShiftSource() == ShiftSource.FROM_AFFINE) {
						shift = dir.getAffineTableShift(leg3, site3, hole3, core3);
					} else {
						shift = Float.valueOf(dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.MCD2_INDEX))
								- Float.valueOf(dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.MBSF2_INDEX));
					}

					loadThisSection(leg1, site1, hole1, core1, type1, section1, startDepth, endDepth, shift);
				} else {
					// range head
					float headStartCoreDepth = Float.valueOf(mbsf3) - dir.getSectionDepth(leg3, site3, hole3, core3, type3, section3);
					float headEndCoreDepth = dir.getSectionLength(leg3, site3, hole3, core3, type3, section3);

					// affine shift
					float shift;
					if (getAffineShiftSource() == ShiftSource.FROM_AFFINE) {
						shift = dir.getAffineTableShift(leg3, site3, hole3, core3);
					} else {
						shift = Float.valueOf(dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.MCD2_INDEX))
								- Float.valueOf(dir.getSpliceTableCell(i - 1, LIMSImageryDirectory.MBSF2_INDEX));
					}

					loadThisSection(leg3, site3, hole3, core3, type3, section3, headStartCoreDepth, headEndCoreDepth, shift);

					// propagate gap value in range body
					if (getAffineShiftSource() == ShiftSource.FROM_AFFINE) {
						shift = dir.getAffineTableShift(leg1, site1, hole1, core1);
					} else {
						shift = Float.valueOf(mcd1) - Float.valueOf(mbsf1);
					}

					loadFromLastEndToThisStart(leg3, site3, hole3, core3, type3, section3, leg1, site1, hole1, core1, type1, section1, shift);

					// range end
					float tailStartCoreDepth = 0.0f;
					float tailEndCoreDepth = Float.valueOf(mbsf1) - dir.getSectionDepth(leg1, site1, hole1, core1, type1, section1);

					loadThisSection(leg1, site1, hole1, core1, type1, section1, tailStartCoreDepth, tailEndCoreDepth, shift);

					// Append the remain sections and cores of the same hole if
					// op == APPEND
					if (op.toLowerCase().equals("append")) {
						float lastSectionDepth = dir.getSectionDepth(leg1, site1, hole1, core1, type1, section1);

						// load remain sections of the same core
						for (String section : dir.getSectionInACore(leg1, site1, hole1, core1, type1)) {
							float depth = dir.getSectionDepth(leg1, site1, hole1, core1, type1, section);
							if (depth > lastSectionDepth) {
								float startCoreDepth = 0.0f;
								float endCoreDepth = dir.getSectionLength(leg1, site1, hole1, core1, type1, section);

								loadThisSection(leg1, site1, hole1, core1, type1, section, startCoreDepth, endCoreDepth, shift);
							}
						}

						// if the last line, load all remain cores of the same
						// hole
						if (i == dir.getNumberOfSpliceTableEntries() - 1) {
							loadRemainCoresBelowWithShift(leg1, site1, hole1, Float.valueOf(mbsf1), shift);
						}
					}
				}
			}

			if (i == dir.getNumberOfSpliceTableEntries() - 1) { // the last line
				if (op.toLowerCase().equals("tie")) {
					float startDepth = Float.valueOf(mbsf2) - dir.getSectionDepth(leg2, site2, hole2, core2, type2, section2);
					float endDepth = dir.getSectionLength(leg2, site2, hole2, core2, type2, section2);

					float shift;
					if (getAffineShiftSource() == ShiftSource.FROM_AFFINE) {
						shift = dir.getAffineTableShift(leg2, site2, hole2, core2);
					} else {
						shift = Float.valueOf(mcd2) - Float.valueOf(mbsf2);
					}

					loadThisSection(leg2, site2, hole2, core2, type2, section2, startDepth, endDepth, shift);
				}
			}
		}
	}

	public static ShiftSource getAffineShiftSource() {
		return affineShiftSource;
	}

	public static boolean isApplyAffineTableToOriginalSections() {
		return applyAffineTableToOriginalSections;
	}

	// Load all section of the core specified in the first entry
	private static void loadFromBeginOfTheCoreTillThisSection(final String leg1, final String site1, final String hole1, final String core1,
			final String type1, final String section1, final float shift) {
		float depth1 = dir.getSectionDepth(leg1, site1, hole1, core1, type1, section1);

		String[] sections = dir.getSectionInACore(leg1, site1, hole1, core1, type1);
		for (String s : sections) {
			float d = dir.getSectionDepth(leg1, site1, hole1, core1, type1, s.trim());
			if (d < depth1) {
				loadThisSection(leg1, site1, hole1, core1, type1, s, 0.0f, dir.getSectionLength(leg1, site1, hole1, core1, type1, s), shift);
			}
		}
	}

	private static void loadFromLastEndToThisStart(final String leg1, final String site1, final String hole1, final String core1, final String type1,
			final String section1, final String leg2, final String site2, final String hole2, final String core2, final String type2, final String section2,
			final float shift) {
		float startDepth = dir.getSectionDepth(leg1, site1, hole1, core1, type1, section1);
		float endDepth = dir.getSectionDepth(leg2, site2, hole2, core2, type2, section2);

		String[] sections = dir.getSectionInACore(leg1, site1, hole1, core1, type1);
		for (String s : sections) {
			float depth = dir.getSectionDepth(leg1, site1, hole1, core1, type1, s);

			if ((depth > startDepth) && (depth < endDepth)) {
				loadThisSection(leg1, site1, hole1, core1, type1, s, 0.0f, dir.getSectionLength(leg1, site1, hole1, core1, type1, s), shift);
			}
		}
	}

	private static void loadRemainCoresBelowWithShift(final String leg, final String site, final String hole, final float belowDepth, final float shift) {
		String[] cores = dir.getCoresInAHoleBelowDepth(leg, site, hole, belowDepth);

		// load cores with shift value
		for (String core : cores) {
			String type = dir.getCoreType(leg, site, hole, core);
			String[] sections = dir.getSectionInACore(leg, site, hole, core, type);

			for (String section : sections) {
				float length = dir.getSectionLength(leg, site, hole, core, type, section);
				loadThisSection(leg, site, hole, core, type, section, 0, length, shift);
			}
		}
	}

	private static void loadThisSection(final String leg, final String site, final String hole, final String core, final String type, final String section,
			final float startCoreDepth, final float endCoreDepth, final float shift) {

		// submit action
		String[] toks = { "cut_interval_to_new_track", leg, site, hole, core, type, section, String.valueOf(startCoreDepth), String.valueOf(endCoreDepth),
				spliceTrackName, String.valueOf(shift) };

		CutIntervalToNewTrackAction cutAction = new CutIntervalToNewTrackAction(toks);
		app.addATaskToExecutor(cutAction);
	}

	public static void setAffineShiftSource(final ShiftSource affineShiftSource) {
		IODPOperationController.affineShiftSource = affineShiftSource;
	}

	public static void setApplyAffineTableToOriginalSections(final boolean applyAffineTableToOriginalSections) {
		IODPOperationController.applyAffineTableToOriginalSections = applyAffineTableToOriginalSections;
	}
}
