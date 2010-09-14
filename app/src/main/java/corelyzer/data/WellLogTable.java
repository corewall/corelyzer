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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Data table structure to hold core logging data. Each table is corresponding
 * to a core section(/image). Each table contains Columns and each
 * corelyzer.data.Column has Cells.
 */
public class WellLogTable extends Table {

	int depthColumn;
	int depthUnit;
	int numFields;
	float depth_offset = -1.0f;
	float topDepth = -1.0f;

	public WellLogTable() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param root
	 *            XML root object from XML dataset file
	 * @param sectionName
	 *            name of the section which the table belongs to
	 */
	public WellLogTable(final Element root, final String sectionName) {
		this();

		if (sectionName == null || sectionName.equals("")) {
			return;
		}

		name = sectionName;
		// System.out.println("---- Section: " + this.name +
		// " start loading from XML");

		this.loadFromXML(root, sectionName);

		if (numCols == 0) {
			depthColumn = -1;
		} else {
			depthColumn = 0;
		}
	}

	public WellLogTable(final String sectionName, final int numOfFields) {
		this();

		if (sectionName == null || sectionName.equals("")) {
			return;
		}

		this.name = sectionName;
		this.numFields = numOfFields;
		this.numCols = numFields + 1; // + depth
	}

	public float getDepth_offset() {
		return depth_offset;
	}

	public int getDepthColumnIndex() {
		return this.depthColumn;
	}

	public int getDepthUnits() {
		return this.depthUnit;
	}

	public int getNumFields() {
		return this.numFields;
	}

	public float getTopDepth() {
		return topDepth;
	}

	/**
	 * Load table values from given XML root element parsed
	 * 
	 * @param section
	 *            XML section object from dataset data file
	 * @param sectionName
	 *            name of the section which the table belongs to
	 */
	void loadFromXML(final Element section, final String sectionName) {
		if (section == null) {
			return;
		}

		NodeList unitList = section.getElementsByTagName("depth_unit");
		if (unitList.getLength() == 0) {
			return;
		}

		// Show loading table progress
		/*
		 * ProgressDialog pdlg = new ProgressDialog(); pdlg.setStatusText("");
		 * pdlg.setLabelText("Loading section table"); pdlg.setVisible(true);
		 */
		Element unit = (Element) unitList.item(0);
		String unitval = unit.getTextContent();

		if (unitval.toLowerCase().equals("cm")) {
			this.depthUnit = UnitLength.CM;
		} else if (unitval.toLowerCase().equals("m")) {
			this.depthUnit = UnitLength.M;
		} else if (unitval.toLowerCase().equals("mm")) {
			this.depthUnit = UnitLength.MM;
		} else if (unitval.toLowerCase().equals("in")) {
			this.depthUnit = UnitLength.INCH;
		} else if (unitval.toLowerCase().equals("ft")) {
			this.depthUnit = UnitLength.FOOT;
		} else if (unitval.toLowerCase().equals("yd")) {
			this.depthUnit = UnitLength.YARD;
		} else {
			System.err.println("No depth unit, will use meter");
			this.depthUnit = UnitLength.M;
		}

		// look for a <top> tag
		NodeList topList = section.getElementsByTagName("top");
		if (topList.getLength() > 0) {
			try {
				topDepth = Float.parseFloat(((Element) topList.item(0)).getTextContent());
			} catch (NumberFormatException e) {
				topDepth = -1.0f;
			}
		}

		String offsetStr = section.getAttribute("offset");
		try {
			this.depth_offset = Float.parseFloat(offsetStr);
		} catch (NumberFormatException e) {
			// System.out.println("---> Depth_offset is null or not a number, "
			// +
			// "let it be 0.0. " + e);
			depth_offset = 0.0f;
		}

		// System.out.println("[INFO] Depth_Unit is: " + unitval + ", " +
		// this.depthUnit);

		NodeList fieldList = section.getElementsByTagName("field");
		if (fieldList.getLength() == 0) {
			return;
		}

		NodeList depthList = section.getElementsByTagName("depth");
		if (depthList.getLength() == 0) {
			return;
		}

		this.numFields = fieldList.getLength();
		this.numCols = this.numFields + 1; // extra column for depth values
		this.numRows = depthList.getLength();

		// System.out.println("NumFields, numCols, numRows: " +
		// numFields + ", " + numCols + ", " + numRows);

		// ----
		headers.addElement("section depth");

		// init corelyzer.data.Column colData[]
		this.colData = new Column[(fieldList.getLength() + 1)]; // TODO: numCols

		// colData[0] is always depth values
		colData[0] = new Column(depthList.getLength());
		colData[0].setDataType(CellType.FLOAT);

		this.columns.addElement(colData[0]); // FIXME why another vector?

		// int ids[] = new int[this.numFields];

		// init colData[] elements
		for (int f = 0; f < this.numFields; f++) {
			Element e = (Element) fieldList.item(f);

			// name
			String v = e.getAttribute("name");
			headers.addElement(v);
			// / System.out.println("---- (fNo., name) = " + f + ", " + v);

			// local id
			v = e.getAttribute("localid");
			// ids[f] = Integer.parseInt(v);

			// System.out.println("-- localid: " + v + ", " +
			// depthList.getLength());

			// colData[] elements, colData[0] is depth values
			colData[f + 1] = new Column(depthList.getLength());
			colData[f + 1].setDataType(CellType.FLOAT);
			columns.addElement(colData[f + 1]);
		}

		// --------------------------------------------------------
		// System.out.println("\n--- Loop through section childs " +
		// "to get floats");

		// Get to the first depth tag
		NodeList childList = section.getChildNodes();
		Element firstDepth = null;

		int child;
		for (child = 0; firstDepth == null && child < childList.getLength(); child++) {
			if (!(childList.item(child) instanceof Element)) {
				continue;
			}
			Element e = (Element) childList.item(child);
			String tagname = e.getTagName();

			if (tagname.equals("depth")) {
				firstDepth = e;
			}
		}

		child--;

		int depthentry = 0;
		int lastdepth = -1;

		// pdlg.setProgressMax(numRows);
		// pdlg.setProgress(0);

		for (; child < childList.getLength() && depthentry <= this.numRows; child++) {
			// / System.out.println("---- DepthEntry, LastDepth : " +
			// / depthentry + ", " + lastdepth);

			// String value;
			float data; // to hold value
			int id; // to hold sensor id

			if (!(childList.item(child) instanceof Element)) {
				continue;
			}
			Element e = (Element) childList.item(child);

			// init fields info in new row(depth)
			if (lastdepth != depthentry && depthentry != this.numRows) // ignore
																		// the
																		// extra
																		// row
			{
				colData[0].valid[depthentry] = true;

				for (int f = 0; f < fieldList.getLength() + 1; f++) {
					colData[f].valid[depthentry] = false;
				}
				lastdepth = depthentry;
			}

			String tag = e.getTagName();

			// new depth entry
			if (tag.equals("depth")) {
				String v = e.getTextContent();
				data = Float.parseFloat(v);

				// depth data, store in colData[0] only
				colData[0].dataArray[depthentry] = data;
				depthentry++;

				// float [] dataArray = colData[0].dataArray;
				// float [] min;
				// float [] max;
				// dataArray[depthentry] = data;
				// dataArray = null;

			} else if (tag.equals("sensor")) {
				if (depthentry == 0) {
					continue;
				}

				String v = e.getAttribute("id");
				id = Integer.parseInt(v.trim());
				if (id < 0 || id > colData.length - 1) {
					// System.out.println("---> Unknown sensor id: " + id +
					// ", will just ignore it");
					continue;
				}

				v = e.getTextContent();
				if (v.equals("")) {
					// System.out.println("No sensor value, ignore and continue ["
					// + v + "]");
					continue;
				}
				data = Float.parseFloat(v);

				float dataArray[] = colData[id + 1].dataArray;
				dataArray[depthentry - 1] = data;
				colData[id + 1].valid[depthentry - 1] = true;

				// ---- min, max eval
				int min, max;
				min = colData[id + 1].minIndex;
				max = colData[id + 1].maxIndex;

				/*
				 * System.out.println("---- " + getHeader(id+1) + ": " + min +
				 * "\t" + max + "\t" + dataArray[depthentry-1]);
				 * 
				 * if( min >= 0) { System.out.println("\t" + dataArray[min]); }
				 * else { System.out.println("\t?"); }
				 * 
				 * if( max >= 0 ) { System.out.println("\t" + dataArray[max]); }
				 * else { System.out.println("\t?"); }
				 */
				if (min < 0) {
					min = depthentry - 1;
				}

				if (max < 0) {
					max = depthentry - 1;
				}

				if (dataArray[min] > dataArray[depthentry - 1]) {
					min = depthentry - 1;
				}

				if (dataArray[max] < dataArray[depthentry - 1]) {
					max = depthentry - 1;
				}

				colData[id + 1].minIndex = min;
				colData[id + 1].maxIndex = max;
			}// end of else

			// pdlg.setProgress(depthentry);
		}// end of for-loop

		// TODO Try to depth-sort colData[]
		// FIXME current Column[] colData is bad for sorting...

		// ids = null;
		// pdlg.setProgress(this.numRows);

		// pdlg.keep_running = false;

		/*
		 * This might cause non-stop progress dialog in windows try {
		 * pdlg.join(); } catch (Exception e) { e.printStackTrace(); }
		 */

		// pdlg.dispose();
	}// end of #loadFromXML();

	public void setDepth_offset(final float depth_offset) {
		this.depth_offset = depth_offset;
	}

	public void setTopDepth(final float topDepth) {
		this.topDepth = topDepth;
	}
}// end of class corelyzer.data.WellLogDataSet
