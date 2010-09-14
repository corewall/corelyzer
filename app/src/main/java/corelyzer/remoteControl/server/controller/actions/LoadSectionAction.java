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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import corelyzer.controller.CRExperimentController;
import corelyzer.data.lims.LIMSImageryDirectory;
import corelyzer.util.JanusUtility;

public class LoadSectionAction extends AbstractAction {

	public LoadSectionAction(final String[] toks) {
		super(toks);
	}

	// Load with IODP LIMS data inputs
	// load_section <leg> <site> <hole> <core> <type> <section> <depth> <length>
	// <dpi> <url>
	// 0 1 2 3 4 5 6 7 8 9 10
	private void loadWithIODPandDPI() { // cmdsurl.length = 11
		String sessionName, trackName;
		String localName;

		String leg = cmds[1].trim();
		String site = cmds[2].trim();
		String hole = cmds[3].trim();
		String core = cmds[4].trim();
		String type = cmds[5].trim();
		String section = cmds[6].trim();
		String depthStr = cmds[7].trim();
		String lengthStr = cmds[8].trim();
		String dpiStr = cmds[9].trim();
		String url = cmds[10].trim();

		// Naming convention
		sessionName = leg + "_" + site;
		trackName = hole + "_" + core;

		// Determine URL
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			// System.out.println("Not a valid URL: '" + url +
			// "', lookup local table");

			// Lookup LIMS table
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				url = dir.getSectionURL(leg, site, hole, core, type, section);

				// Try relative path if still not valid URL
				try {
					new URL(url);
				} catch (MalformedURLException e1) {
					// might be relative path
					File f = new File(dir.getSectionListFileStr());
					File parentDir = f.getParentFile();
					File aFile = new File(parentDir, url);
					try {
						url = aFile.toURI().toURL().toString();
					} catch (MalformedURLException e2) {
						System.err.println("oh no! still invalid URL? " + aFile.getAbsolutePath());
						e2.printStackTrace();
					}
				}

			} else {
				url = null;
			}

			if (url == null) {

				String mesg = "[LoadSection] Cannot find URL of section image:\nleg + " + cmds[1] + ", site: " + cmds[2] + ", hole: " + cmds[3] + ", core: "
						+ cmds[4] + ", type: " + cmds[5] + ", section: " + cmds[6];

				JOptionPane.showMessageDialog(app.getMainFrame(), mesg);
				return;
			}
		}

		if (url.toLowerCase().endsWith(".jpg") || url.toLowerCase().endsWith(".bmp") || url.toLowerCase().endsWith(".tif")
				|| url.toLowerCase().endsWith(".png")) {
			String[] toks = url.split("/");
			localName = toks[toks.length - 1];
		} else {
			localName = LIMSImageryDirectory.getImageName(leg, site, hole, core, type, section) + ".jpg";
		}

		// Determine Depth
		float depth;
		try {
			depth = Float.parseFloat(depthStr);
		} catch (NumberFormatException e) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				depth = dir.getSectionDepth(leg, site, hole, core, type, section);
			} else {
				depth = LIMSImageryDirectory.DEFAULT_DEPTH;
			}
		}

		// Determine DPI
		float dpi;
		try {
			dpi = Float.parseFloat(dpiStr);
		} catch (NumberFormatException e) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				dpi = dir.getSectionDPI(leg, site, hole, core, type, section);
			} else {
				dpi = 0.0f;
			}
		}

		// Determine Length
		float length;
		try {
			length = Float.parseFloat(lengthStr);
		} catch (NumberFormatException e) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				length = dir.getSectionLength(leg, site, hole, core, type, section);
			} else {
				length = LIMSImageryDirectory.DEFAULT_LENGTH;
			}
		}

		// Load (with status update)
		String mesg = "Status: Loading " + localName + " to track: " + trackName + " of session: " + sessionName;

		JLabel aLabel = getStatusLabel();
		if (aLabel != null) {
			aLabel.setText(mesg);
		}

		// Use DPI if value > 0.0f
		if (dpi > 0.0f) { // use dpi
			CRExperimentController.loadSectionImageWithDPI(url, localName, sessionName, trackName, depth, dpi);
		} else { // use length
			CRExperimentController.loadSectionImageWithLength(url, localName, sessionName, trackName, depth, length);
		}

		if (aLabel != null) {
			aLabel.setText("Status: " + localName + " loaded.");
		}
	}

	// Load with IODP LIMS data inputs
	// load_section <leg> <site> <hole> <core> <type> <section> <depth> <length>
	// <url>
	// 0 1 2 3 4 5 6 7 8 9
	private void loadWithIODPandLength() { // cmdsurl.length = 10
		String sessionName, trackName;
		String localName;

		String leg = cmds[1].trim();
		String site = cmds[2].trim();
		String hole = cmds[3].trim();
		String core = cmds[4].trim();
		String type = cmds[5].trim();
		String section = cmds[6].trim();
		String depthStr = cmds[7].trim();
		String lengthStr = cmds[8].trim();
		String url = cmds[9].trim();

		// Naming convention
		sessionName = leg + "_" + site;
		trackName = hole + "_" + core;

		// Determine URL
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			// System.out.println("Not a valid URL: '" + url +
			// "', lookup local table");

			// Lookup LIMS table
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				url = dir.getSectionURL(leg, site, hole, core, type, section);

				// Try relative path if still not valid URL
				try {
					new URL(url);
				} catch (MalformedURLException e1) {
					// might be relative path
					File f = new File(dir.getSectionListFileStr());
					File parentDir = f.getParentFile();
					File aFile = new File(parentDir, url);
					try {
						url = aFile.toURI().toURL().toString();
					} catch (MalformedURLException e2) {
						System.err.println("oh no! still invalid URL? " + aFile.getAbsolutePath());
						e2.printStackTrace();
					}
				}

			} else {
				url = null;
			}

			if (url == null) {

				String mesg = "[LoadSection] Cannot find URL of section image:\nleg + " + cmds[1] + ", site: " + cmds[2] + ", hole: " + cmds[3] + ", core: "
						+ cmds[4] + ", type: " + cmds[5] + ", section: " + cmds[6];

				JOptionPane.showMessageDialog(app.getMainFrame(), mesg);
				return;
			}
		}

		if (url.toLowerCase().endsWith(".jpg") || url.toLowerCase().endsWith(".bmp") || url.toLowerCase().endsWith(".tif")
				|| url.toLowerCase().endsWith(".png")) {
			String[] toks = url.split("/");
			localName = toks[toks.length - 1];
		} else {
			localName = LIMSImageryDirectory.getImageName(leg, site, hole, core, type, section) + ".jpg";
		}

		// Determine Depth
		float depth, length;
		try {
			depth = Float.parseFloat(depthStr);
		} catch (NumberFormatException e) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				depth = dir.getSectionDepth(leg, site, hole, core, type, section);
			} else {
				depth = LIMSImageryDirectory.DEFAULT_DEPTH;
			}
		}

		// Determine Length
		try {
			length = Float.parseFloat(lengthStr);
		} catch (NumberFormatException e) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir != null) {
				length = dir.getSectionLength(leg, site, hole, core, type, section);
			} else {
				length = LIMSImageryDirectory.DEFAULT_LENGTH; // -1, to use
																// default DPI
																// instead
			}
		}

		// Load (with status update)
		String mesg = "Status: Loading " + localName + " to track: " + trackName + " of session: " + sessionName;

		JLabel aLabel = getStatusLabel();
		if (aLabel != null) {
			aLabel.setText(mesg);
		}

		CRExperimentController.loadSectionImageWithLength(url, localName, sessionName, trackName, depth, length);

		if (aLabel != null) {
			aLabel.setText("Status: " + localName + " loaded.");
		}
	}

	// Load with IODP addressing(leg, site, hole, core), get more metadata from
	// ChronosJanus service(http://portal.chronos.org/)
	private void loadWithIODPFromChronosJanus() { // cmds.length = 7
		String sessionName, trackName;
		String url;
		String localName;

		String leg = cmds[1].trim();
		String site = cmds[2].trim();
		String hole = cmds[3].trim();
		String core = cmds[4].trim();
		String type = cmds[5].trim();
		String section = cmds[6].trim();

		float dpi = JanusUtility.DEFAULT_DPI;
		float depth = JanusUtility.DEFAULT_DEPTH;

		// Naming convention
		sessionName = leg + "_" + site;
		trackName = hole + "_" + core;

		localName = JanusUtility.generateJanusImageName(leg, site, hole, core, type, section);
		url = JanusUtility.generateJanusImageAccessURL(leg, site, hole, core, type, section);

		String sectionInfo;
		try {
			sectionInfo = JanusUtility.getJanusSectionInfo(url, sessionName, trackName);
		} catch (IOException e) {
			sectionInfo = null;
		}

		if (sectionInfo != null) {
			String[] secInfo = sectionInfo.split("\t");

			depth = Float.parseFloat(secInfo[10].trim());
			dpi = Float.parseFloat(secInfo[12].trim());
		}

		// load
		String mesg = "---> [loadWithIODPFromChronosJanus] Laoding " + url + " to Session: " + sessionName + ", trackName: " + trackName;
		System.out.println(mesg);

		CRExperimentController.loadSectionImageWithDPI(url, localName, sessionName, trackName, depth, dpi);
	}

	// Load with URL only
	private void loadWithURL(final float depth) { // cmds.length = 4
		String url = cmds[1].trim();
		String sessionName = cmds[2].trim();
		String trackName = cmds[3].trim();

		String[] toks = url.split("/");
		String localName = toks[toks.length - 1];

		// load
		String mesg = "---> [loadWithURL] Loading " + url + " to Session: " + sessionName + ", TrackName: " + trackName;
		System.out.println(mesg);

		CRExperimentController.loadSectionImageWithURL(url, localName, sessionName, trackName, depth);
	}

	public void run() {
		switch (cmds.length) {
			case 4: { // load_section <url> <session> <track>
				loadWithURL(0.0f);
				break;
			}

			case 5: { // load_section <url> <session> <track> <depth>
				float depth;
				try {
					depth = Float.parseFloat(cmds[4].trim());
				} catch (NumberFormatException e) {
					depth = 0.0f;
				}

				loadWithURL(depth);
				break;
			}

			case 7: { // load_section <leg> <site> <hole> <core> <type>
						// <section>
				loadWithIODPFromChronosJanus();
				break;
			}

			case 10: { // load_section <leg> <site> <hole> <core> <type>
						// <section> <depth> <length> <url>
				loadWithIODPandLength();
				break;
			}

			case 11: { // load_section <leg> <site> <hole> <core> <type>
						// <section> <depth> <length> <dpi> <url>
				loadWithIODPandDPI();
			}

			default:
				// Just ignore
		}
	}
}
