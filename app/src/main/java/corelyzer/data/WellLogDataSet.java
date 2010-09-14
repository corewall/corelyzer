/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
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

package corelyzer.data;

import java.io.File;
import java.util.Vector;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Dataset structure to hold core logging data
 */
public class WellLogDataSet {
	/**
	 * Tables in this dataset
	 */
	Vector<WellLogTable> tables;

	/**
	 * Dataset filename in string
	 */
	String filename;

	/**
	 * The dataset's URL
	 */
	String urn;

	/**
	 * Dataset ID
	 */
	int id;

	public WellLogDataSet() {
		this.tables = new Vector<WellLogTable>();
	}

	/**
	 * Construct a dataset object from given dataset XML file
	 * 
	 * @param f
	 *            Filename of the dataset in XML format
	 */
	public WellLogDataSet(final String f) {
		this();

		if (f == null || f.equals("")) {
			return;
		}

		this.filename = f;
		Element root = null;

		try {
			DOMParser parser = new DOMParser();
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);
			parser.parse(this.filename);
			Document doc = parser.getDocument();

			root = doc.getDocumentElement();
		} catch (Exception e) {
			System.err.println("---> Parse dataset file error, " + "self insert WellLogTable(s) needed: " + e);
			return;
		}

		if (root != null) {
			NodeList sectionList = root.getElementsByTagName("section");
			int numsections = sectionList.getLength();

			// System.out.println("Number of Sections in the dataset file: " +
			// numsections);

			/*
			 * fixme with global progress bar
			 * System.out.print("Loading sections: "); ProgressDialog progress =
			 * new ProgressDialog();
			 * progress.setLabelText("Loading section table");
			 * progress.setProgress(0); progress.setProgressMax(numsections);
			 * progress.setStatusText(""); progress.setVisible(true);
			 */

			for (int i = 0; i < numsections; i++) {
				if (!(sectionList.item(i) instanceof Element)) {
					continue;
				}

				Element sec = (Element) sectionList.item(i);

				NodeList idList = sec.getElementsByTagName("id");

				if (idList.getLength() == 0) {
					continue;
				}

				Element id = (Element) idList.item(0);
				String idcontext = id.getTextContent();

				WellLogTable t = new WellLogTable(sec, idcontext);
				this.tables.addElement(t);

				// System.out.print("#");
				// progress.setProgress(i);
			}
			// progress.setIndeterminant();
			// progress.dispose();

			// System.out.println("\nDone. " + numsections +
			// " sections/tables loaded");
		}
	}

	public int addTable(final WellLogTable t) {
		tables.addElement(t);

		return tables.indexOf(t);
	}

	public int getId() {
		return this.id;
	}

	public int getNumTables() {
		return this.tables.size();
	}

	public String getSourceFilename() {
		return this.filename;
	}

	public WellLogTable getTable(final int i) {
		if (i < 0 || i >= tables.size()) {
			return null;
		}

		return this.tables.elementAt(i);
	}

	public WellLogTable getTable(final String name) {
		for (WellLogTable t : tables) {
			if (name.equals(t.getName())) {
				return t;
			}
		}

		return null;
	}

	public String getURN() {
		return this.urn;
	}

	public void setId(final int i) {
		this.id = i;
	}

	public void setURN(final String u) {
		this.urn = u;
	}

	/**
	 * Override toString(), use the filename as the name of the dataset
	 */

	@Override
	public String toString() {
		return new File(this.filename).getName();
	}
}
