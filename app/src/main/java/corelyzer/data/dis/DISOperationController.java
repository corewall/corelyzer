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

	// Min/max values in all sections/tables
	static Vector<Float> allMinVals;

	static Vector<Float> allMaxVals;

	static Vector<Color> colors;

	static void activateGraphs(final TrackSceneNode tnode, final WellLogDataSet ds) {
		boolean justAppend = false;
		CoreSection sec;

		for (int i = 0; i < ds.getNumTables(); i++) {
			WellLogTable wt = ds.getTable(i);

			String tableName = wt.getName();
			sec = tnode.getCoreSection(tableName);

			if (sec == null) {
				// need to creat new section
				int secid = SceneGraph.addSectionToTrack(tnode.getId(), tnode.getNumCores());
				SceneGraph.setSectionName(tnode.getId(), secid, tableName);

				// property?
				sec = new CoreSection(tableName, secid);
				tnode.addCoreSection(sec);
				CoreGraph.getInstance().notifyListeners();

				// If the dataset table contains section depth offset, use it

				// Figure out the top depth of a section
				float sectionBeginDepth;
				float sectionTopDepth = wt.getTopDepth(); // in meters
				final float unitScale = UnitLength.getUnitScale( wt.getDepthUnits() );

				if (sectionTopDepth != -1) {
					sectionBeginDepth = sectionTopDepth;
				} else if (wt.getDepth_offset() != -1) {
					sectionBeginDepth = wt.getDepth_offset() * unitScale;
					;
				} else {
					justAppend = true;
					sectionBeginDepth = 0.0f;
				}

				SceneGraph.positionSection(tnode.getId(), secid, sectionBeginDepth * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0), 0);
			}

			// if we created new coresection and add new graph
			// send section to the end of the track
			if (justAppend) {
				SceneGraph.pushSectionToEnd(tnode.getId(), sec.getId());
				sec.setDepth(SceneGraph.getSectionDepth(tnode.getId(), sec.getId()));
			}

			// now we have the section, iterate through all fields in the
			// dataset
			for (int j = 0; j < wt.getNumFields(); j++) {
				// native first, java next for gid
				int gid = SceneGraph.addLineGraphToSection(tnode.getId(), sec.getId(), ds.getId(), i, j); // i:
																											// tableIndex,
																											// j:
																											// fieldIndex

				if (gid == -1) {
					System.out.println("- DIS gid is -1!!!");
					continue;
				}

				SceneGraph.setLineGraphRange(gid, allMinVals.elementAt(j), allMaxVals.elementAt(j));
				Color c = colors.elementAt(j);
				SceneGraph.setLineGraphColor(gid, (c.getRed() / 255.0f), (c.getGreen() / 255.0f), (c.getBlue() / 255.0f));

				SceneGraph.setLineGraphType(gid, 0); // 0: default as lines

				String fieldName = wt.getHeader(j + 1);
				SceneGraph.setLineGraphLabel(gid, fieldName);

				// java side
				CoreSectionGraph csg = new CoreSectionGraph(ds.getId(), i, j, gid, tnode);
				csg.setName(tableName);
				tnode.addCoreSectionGraph(csg, sec.getId(), gid);
			}
		}
	}

	public static void applySelectedDownholeLog(final Window parent, final String logFilePath) {
		File selectedFile = new File(logFilePath);

		if (selectedFile.exists()) {
			// Load the DIS data file. And use the 'Hole', 'Expedition' data in
			// the file to
			// create session and track names.
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
					t = new TrackSceneNode(trackId);
					t.setName("MSCL Graph");
				}

				app.loadData(selectedFile, true);

				/*
				 * WellLogDataSet ds = null; for(WellLogDataSet d :
				 * s.getDatasets()) { if(d.getId() == dsId) { ds = d; break; } }
				 * 
				 * if(dsId >= 0 && ds != null) { // Activate the graphs
				 * calcMinMaxColor(ds); activateGraphs(t, ds); } else {
				 * JOptionPane.showMessageDialog(parent,
				 * "Loading MSCL data failed."); }
				 */

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

	static void calcMinMaxColor(final WellLogDataSet ds) {
		// Allocate
		colors = new Vector<Color>();
		allMinVals = new Vector<Float>();
		allMaxVals = new Vector<Float>();

		if (ds == null) {
			return;
		}

		System.out.println("- Dataset id: " + ds.getId() + " has " + ds.getNumTables() + " tables.");
		WellLogTable t = ds.getTable(0);

		Random generator = new Random(System.currentTimeMillis());

		// collect fields' all min/max
		for (int i = 0; i < t.getNumFields(); i++) {
			float min = 0.0f;
			float max = 0.0f;

			for (int j = 0; j < ds.getNumTables(); j++) {
				WellLogTable table = ds.getTable(j);

				if (j == 0) {
					min = table.getColumnMin(i);
					max = table.getColumnMax(i);
				} else {
					min = min > table.getColumnMin(i) ? table.getColumnMin(i) : min;
					max = max < table.getColumnMax(i) ? table.getColumnMax(i) : max;
				}
			}

			allMinVals.add(min);
			allMaxVals.add(max);

			int r = (int) (100 + 0.49803 * generator.nextInt(256));
			int g = (int) (100 + 0.49803 * generator.nextInt(256));
			int b = (int) (100 + 0.49803 * generator.nextInt(256));

			colors.add(new Color(r, g, b));
		}
	}
}
