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
package corelyzer.data.dis;

import java.awt.Color;
import java.awt.Window;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.table.TableModel;

import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionGraph;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.UnitLength;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.WellLogTable;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;
import corelyzer.io.CRDISDepthValueDataLoader;
import corelyzer.remoteControl.server.controller.ControlServerApplication;
import corelyzer.remoteControl.server.controller.actions.LoadDISSectionAction;
import corelyzer.ui.CorelyzerApp;

public class DISOperationController {
	static final float DEFAULT_DEPTH = 0.0f;
	static final float DEFAULT_LENGTH = 1.0f;
	static final float INVALID_DPI = 0.0f;

	public static void applySelectedDownholeLog(final Window parent, final String logFilePath) {
		File selectedFile = new File(logFilePath);

		if (selectedFile.exists()) {
			// Load the DIS data file. And use the 'Hole', 'Expedition' data in
			// the file to create session and track names.
			CRDISDepthValueDataLoader dvLoader = new CRDISDepthValueDataLoader(selectedFile);
			dvLoader.load();
		} else {
			JOptionPane.showMessageDialog(parent, "Selected file '" + logFilePath + "' doesn't exist");
		}
	}

	public static void applySelectedMSCLLog(final Window parent, final String msclFilePath) {
		File selectedFile = new File(msclFilePath);

		if (selectedFile.exists()) {
			CorelyzerApp app = CorelyzerApp.getApp();

			if (app != null) {
				// Create the MSCL Track in current session
				CoreGraph cg = CoreGraph.getInstance();

				if (cg == null) { // just load the mscl data
					app.loadData(selectedFile, true);
					return;
				}

				Session s = cg.getCurrentSession();
				if (s == null) {
					s = new Session("MSCL Data");
					cg.setCurrentSession(s);
				}

				TrackSceneNode t = s.getTrackSceneNodeWithName("MSCL Graph");
				if (t == null) {
					int trackId = app.createTrack("MSCL Graph");
					t = s.getTrackSceneNodeWithIndex(trackId);
				}

				final int datasetId = app.loadData(selectedFile, true);
				
				WellLogDataSet ds = null;
				for (WellLogDataSet d : s.getDatasets()) {
					if (d.getId() == datasetId) {
						ds = d;
						break;
					}
				}
				 
				if (ds != null) { // Activate the graphs (which also creates sections)
					CRDISDepthValueDataLoader.activateGraphs(t, ds);
				} else {
					JOptionPane.showMessageDialog(parent, "Loading MSCL data failed.");
				}

				app.updateGLWindows();
			} else {
				System.err.println("- CorelyzerApp not available.");
			}
		} else {
			JOptionPane.showMessageDialog(parent, "Selected file '" + msclFilePath + "' doesn't exist");
		}
	}

	public static void applySelectedSections(final Window parent, final ControlServerApplication app, final TableModel model, final int[] rows) {
		for (int row : rows) {
			String leg = (String) model.getValueAt(row, DISImageTableModel.LEG_INDEX);
			String site = (String) model.getValueAt(row, DISImageTableModel.SITE_INDEX);
			String hole = (String) model.getValueAt(row, DISImageTableModel.HOLE_INDEX);
			String core = (String) model.getValueAt(row, DISImageTableModel.CORE_INDEX);
			String section = (String) model.getValueAt(row, DISImageTableModel.SECTION_INDEX);
			String urlString = (String) model.getValueAt(row, DISImageTableModel.URL_INDEX);
			String depthString = (String) model.getValueAt(row, DISImageTableModel.DEPTH_INDEX);

			float depth;
			try {
				depth = Float.parseFloat(depthString);
			} catch (NumberFormatException e) {
				depth = DEFAULT_DEPTH;
			}

			String lengthString = (String) model.getValueAt(row, DISImageTableModel.LENGTH_INDEX);
			float length;
			try {
				length = Float.parseFloat(lengthString);
			} catch (NumberFormatException e) {
				length = DEFAULT_LENGTH;
			}

			try {
				new URL(urlString);
			} catch (MalformedURLException e) {
				JOptionPane.showMessageDialog(parent, "URL '" + urlString + "' is invalid.");
			}

			float topOffset = 0f;
			try {
				topOffset = Float.parseFloat((String) model.getValueAt(row, DISImageTableModel.TOP_OFFSET_INDEX));
			} catch (NumberFormatException e) {
				// ignore
			}

			float bottomOffset = 0f;
			try {
				bottomOffset = Float.parseFloat((String) model.getValueAt(row, DISImageTableModel.BOT_OFFSET_INDEX));
			} catch (NumberFormatException e) {
				// ignore
			}

			if (urlString.toLowerCase().contains("corecatcher")) {
			} else {
			}

			if (app != null) {
				app.addATaskToExecutor(new LoadDISSectionAction(leg, site, hole, core, section, urlString, depth, length, topOffset, bottomOffset));
			}
		}
	}
}
