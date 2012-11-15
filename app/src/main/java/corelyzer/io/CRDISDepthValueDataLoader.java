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
package corelyzer.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import corelyzer.data.CRPreferences;
import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionGraph;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.UnitLength;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.WellLogTable;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.DataImportWizard;
import corelyzer.util.FileUtility;

/*
 Header example:
 HOLE:	M0027A
 EXPEDITION:	313
 TOP:	417.6758
 BOTTOM:	621.6758
 Depth	VP15
 m	     m/sec
 417.6758	1974.7400
 417.7758	2068.1600
 417.8758	1863.0500
 417.9758	1977.4500
 */
public class CRDISDepthValueDataLoader {
	File dvFile = null;

	// DIS data setups
	String expName = "";
	String siteName = "";
	String holeName = "";
	String coreName = "";

	String fileSeparator = "\t";

	int dataStartLine = 6;
	int dataEndLine = 6; // need to calculate

	int labelLine = 4;
	int unitLine = 5;

	int numOfColumns = 0;
	int depthColumn = 0;

	public CRDISDepthValueDataLoader() {
		super();
	}

	public CRDISDepthValueDataLoader(final File f) {
		this();
		this.dvFile = f;
	}

	public static void activateGraphs(final TrackSceneNode tnode, final WellLogDataSet ds) {
		boolean justAppend = false;
		CoreSection sec;
		
		Vector<Float> allMinVals = new Vector<Float>(), allMaxVals = new Vector<Float>();
		Vector<Color> colors = new Vector<Color>();
		calcMinMaxColor(ds, allMinVals, allMaxVals, colors);

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
				// newSecCreated = true;

				// If the dataset table contains section depth offset, use it
				final float unitScale = UnitLength.getUnitScale(wt.getDepthUnits());

				// Figure out the top depth of a section
				float sectionBeginDepth;
				float sectionTopDepth = wt.getTopDepth(); // in meters

				if (sectionTopDepth != -1) {
					sectionBeginDepth = sectionTopDepth;
				} else if (wt.getDepth_offset() != -1) {
					sectionBeginDepth = wt.getDepth_offset() * unitScale;
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

			// now we have the section, iterate through all fields in the dataset
			for (int j = 0; j < wt.getNumFields(); j++) {
				// native first, java next for gid
				int gid = SceneGraph.addLineGraphToSection(tnode.getId(), sec.getId(), ds.getId(), i, j);

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

	private static void calcMinMaxColor(
			final WellLogDataSet ds,
			Vector<Float> allMinVals,
			Vector<Float> allMaxVals,
			Vector<Color> colors )
	{
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

	public File convert(final File f) {
		if (f == null) {
			return null;
		}

		// Load file
		int count = 0;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(dvFile));

			String line;
			while ((line = br.readLine()) != null) {
				String[] toks = line.split(this.fileSeparator);

				if (count == 0) {
					String holeString = toks[1].trim();
					this.siteName = holeString.substring(0, holeString.length() - 1);
					this.holeName = holeString.substring(holeString.length() - 1);
				} else if (count == 1) {
					this.expName = toks[1].trim();
				} else if (count == 4) { // Label line
					this.numOfColumns = toks.length;
				}

				count++;
			}

			this.dataEndLine = count - 1;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Vector<Integer> vals = new Vector<Integer>();
		for (int i = 1; i <= this.numOfColumns - 1; i++) {
			vals.add(i);
		}

		// Get output filename and path
		String fileName = f.getName();
		int dotPos = fileName.lastIndexOf(".");
		fileName = fileName.substring(0, dotPos);

		String sessionName = this.expName + "_" + this.siteName;

		CRPreferences prefs = CorelyzerApp.getApp().preferences();
		String prefix = prefs.getLocalRepositoryPath();
		String sp = System.getProperty("file.separator");
		String outputFileString = prefix + sp + sessionName + sp + "datasets" + sp + fileName + ".xml";

		System.out.println("- Converting '" + f + "' to '" + outputFileString + "'");

		File outputFile = new File(outputFileString);
		FileUtility.createDirsIfNecessary(outputFile.getParentFile());

		String sectionPrefix;
		String[] toks = fileName.split("_");
		if (toks.length >= 3) {
			sectionPrefix = toks[toks.length - 1];
		} else {
			sectionPrefix = fileName;
		}

		// public static void convert(File fin, File fout, String fs, String
		// prefix,
		// int start, int end, int label, int unit,
		// String name, int depth, Vector<Integer> vals,
		// DepthMode dm, boolean useCustomizedSectionName,
		// float ignoreValue)

		/*
		 * System.out.println("Converting DIS data " + f + " using parameters: "
		 * + this.dataStartLine + ", " + this.dataEndLine + ", " +
		 * this.labelLine + ", " + this.unitLine + ", " + depthColumn + ", " +
		 * vals.size());
		 * 
		 * for(int colNum : vals) { System.out.println("col: " + colNum); }
		 */

//		DataImportWizard.convert(f, outputFile, this.fileSeparator, sectionPrefix, this.dataStartLine, this.dataEndLine, this.labelLine, this.unitLine, null,
//				this.depthColumn, vals, DataImportWizard.DepthMode.ACCUM_DEPTH, false, -999.2500f);
		DataImportWizard.sax_convert(f, outputFile, this.fileSeparator, sectionPrefix, this.dataStartLine, this.dataEndLine, this.labelLine, this.unitLine, null,
				this.depthColumn, vals, DataImportWizard.DepthMode.ACCUM_DEPTH, false, -999.2500f);

		return outputFile;
	}

	public void load() {
		File f = convert(this.dvFile);
		if (f == null) {
			return;
		}

		CorelyzerApp app = CorelyzerApp.getApp();
		if (app == null) {
			return;
		}

		// Put the loaded into proper session, track
		CoreGraph cg = CoreGraph.getInstance();
		String sessionName = this.expName + "_" + this.siteName;
		Session s = cg.getSession(sessionName);
		if (s == null) {
			s = new Session(sessionName);
			cg.addSession(s);
		}
		cg.setCurrentSession(s);
		cg.notifyListeners();

		// Track - holeName + prop
		String[] toks = dvFile.getName().split("_");

		String propName = toks[toks.length - 1];
		propName = propName.substring(0, propName.length() - 4);

		String trackName;
		trackName = this.holeName + "_" + this.coreName + "-" + propName;

		int tid = app.createTrack(trackName);
		TrackSceneNode tnode = s.getTrackSceneNodeWithTrackId(tid);

		int dsId = app.loadData(f, true); // use SAX, or not

		WellLogDataSet ds = null;
		for (WellLogDataSet d : s.getDatasets()) {
			if (d.getId() == dsId) {
				ds = d;
				break;
			}
		}

		/*
		 * int dsIdx = -1; for(int i = 0; i < s.getNumberOfDatasets(); ++i) {
		 * WellLogDataSet ds = s.getDataset(i);
		 * 
		 * if(ds.getId() == dsId) { dsIdx = i; break; } }
		 */

		if (ds != null) {
			// Activate ALL the graph automatically
			System.out.println("- DSId before activate: " + dsId);

			activateGraphs(tnode, ds);
		} else {
			System.out.println("- Cannot find dataset in current session.");
		}
	}
}
