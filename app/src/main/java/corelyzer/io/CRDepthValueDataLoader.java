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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.DataImportWizard;
import corelyzer.util.FileUtility;

public class CRDepthValueDataLoader {
	public static int[] calculateDataDimension(final File dvFile, String fs) {
		int count = 0;
		int numberOfColumns = 0;

		String extension = FileUtility.getExtension(dvFile);

		if (fs == null) {
			if (extension.equalsIgnoreCase("tsv")) {
				fs = "\t";
			} else if (extension.equalsIgnoreCase("csv")) {
				fs = ",";
			} else {
				return null;
			}
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(dvFile));

			String line;
			while ((line = br.readLine()) != null) {
				String[] toks = line.split(fs);

				if (toks.length > 1) {
					if (numberOfColumns == 0) {
						numberOfColumns = toks.length;
					}

					count++;
				}
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return new int[] { (count - 2), (numberOfColumns - 1) };
	}

	public static File convert(final File f, final int startingLine, final boolean hasUnitLine, String fs) {
		if (f == null) {
			return null;
		}

		String fullPath = f.getAbsolutePath();
		int dotPos = fullPath.lastIndexOf(".");

		String filePrefix = fullPath.substring(0, dotPos);
		File outputFile = new File(filePrefix + ".xml");

		String fileName = f.getName();
		dotPos = fileName.lastIndexOf(".");
		String prefix = fileName.substring(0, dotPos);

		String extension = FileUtility.getExtension(f);

		if (fs == null) {
			if (extension.equalsIgnoreCase("tsv")) {
				fs = "\t";
			} else if (extension.equalsIgnoreCase("csv")) {
				fs = ",";
			} else {
				return null;
			}
		}

		int[] dataDimension = calculateDataDimension(f, fs);
		if (dataDimension == null) {
			return null;
		}

		Vector<Integer> vals = new Vector<Integer>();
		for (int i = 1; i <= dataDimension[1]; i++) {
			vals.add(i);
		}

		int depthColumn = 0;
		int unitLabel = hasUnitLine ? startingLine + 1 : startingLine;

		int dataStart = hasUnitLine ? unitLabel + 1 : startingLine + 1;
		int dataEnd = dataStart + dataDimension[0];

		// public static void convert(File fin, File fout, String fs, String
		// prefix,
		// int start, int end, int label, int unit,
		// String name, int depth, Vector<Integer> vals,
		// DepthMode dm, boolean useCustomizedSectionName,
		// float ignoreValue)
		DataImportWizard.convert(f, outputFile, fs, prefix, dataStart, dataEnd, startingLine, unitLabel, null, depthColumn, vals,
				DataImportWizard.DepthMode.ACCUM_DEPTH, false, -999.2500f);

		return outputFile;
	}

	File dvFile = null;
	// lines starts from 0, startingLine is for "LABELs"
	int startingLine = 0;

	boolean hasUnitLine = true;

	String fileSeparator = null;

	public CRDepthValueDataLoader() {
		super();
	}

	public CRDepthValueDataLoader(final File f) {
		this(f, 0, true, null);
	}

	public CRDepthValueDataLoader(final File f, final int startLine, final boolean hasUnit, final String fs) {
		this();
		this.dvFile = f;
		this.startingLine = startLine;
		this.hasUnitLine = hasUnit;
		this.fileSeparator = fs;
	}

	public void load() {
		File f = convert(this.dvFile, this.startingLine, this.hasUnitLine, this.fileSeparator);
		if (f == null) {
			return;
		}

		CorelyzerApp app = CorelyzerApp.getApp();
		if (app == null) {
			return;
		}

		app.loadData(f);
	}
}
