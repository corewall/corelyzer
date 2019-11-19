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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import corelyzer.util.StringUtility;

public class LIMSImageryDirectory {
	// Default values
	public final static float DEFAULT_DPI = 254.0f;
	public final static float DEFAULT_DEPTH = 0.0f;
	public final static float DEFAULT_LENGTH = 1.0f;

	// Section table column indices
	static String[] mapping = { "Leg", "Site", "Hole", "Core", "Type", "Section", "TopOffset (m)", "BottomOffset (m)", "Depth (m)", "Length", "DPI", "URL" };

	final static int LEG_INDEX = 0;
	final static int SITE_INDEX = 1;
	final static int HOLE_INDEX = 2;
	final static int CORE_INDEX = 3;
	final static int TYPE_INDEX = 4;
	final static int SECTION_INDEX = 5;
	final static int TOPOFFSET_INDEX = 6;
	final static int BOTTOMOFFSET_INDEX = 7;
	final static int DEPTH_INDEX = 8;
	final static int LENGTH_INDEX = 9;
	final static int DPI_INDEX = 10;
	final static int URL_INDEX = 11;

	// Affine table entries
	final static String[] affineTableMapping = { "Leg", "Site", "Hole", "Core", "Type", "Shift (m)", "Apply only?" };

	final static int OFFSET_INDEX = 5;
	final static int APPLY_INDEX = 6;

	// Splice table entries
	final static String[] spliceTableMapping = { "Leg1", "Site1", "Hole1", "Core1", "Type1", "Section1", "Top1", "Bottom1", "mbsf1", "mcd1", "TIE/APPEND",
			"Leg2", "Site2", "Hole2", "Core2", "Type2", "Section2", "Top2", "Bottom2", "mbsf2", "mcd2" };
	final static int LEG1_INDEX = 0;
	final static int SITE1_INDEX = 1;
	final static int HOLE1_INDEX = 2;
	final static int CORE1_INDEX = 3;
	final static int TYPE1_INDEX = 4;
	final static int SECT1_INDEX = 5;
	final static int TOP1_INDEX = 6;
	final static int BTM1_INDEX = 7;
	final static int MBSF1_INDEX = 8;
	final static int MCD1_INDEX = 9;

	final static int OP_INDEX = 10;

	final static int LEG2_INDEX = 11;
	final static int SITE2_INDEX = 12;
	final static int HOLE2_INDEX = 13;
	final static int CORE2_INDEX = 14;
	final static int TYPE2_INDEX = 15;
	final static int SECT2_INDEX = 16;
	final static int TOP2_INDEX = 17;
	final static int BTM2_INDEX = 18;
	final static int MBSF2_INDEX = 19;
	final static int MCD2_INDEX = 20;

	static LIMSImageryDirectory directory;

	public static LIMSImageryDirectory getDirectory() {
		if (directory == null) {
			directory = new LIMSImageryDirectory();
		}

		return directory;
	}

	public static String getImageName(final String leg, final String site, final String hole, final String core, final String type, final String section) {
		return site + hole.toLowerCase() + "_" + StringUtility.expandNums(core, 3) + type.toLowerCase() + "_" + StringUtility.expandNums(section, 2);
	}

	// Could be better DS
	// section list table
	Vector<String[]> allSections;

	String sectionListFileStr;

	// affine table
	Vector<String[]> affineTable;

	// splice table
	Vector<String[]> spliceTable;

	public LIMSImageryDirectory() {
		super();

		allSections = new Vector<String[]>();
		affineTable = new Vector<String[]>();
		spliceTable = new Vector<String[]>();

		directory = this;
	}

	public void addChronosOnlyTokensToSectionTable(final String[] toks) {
		allSections.add(toks);
	}

	// leg site hole core sectionNumber sectionID sectionType coreType
	// curatedLength linearLength
	// MBSF FORMAT DPI imageURL
	public void addChronosTokensToSectionTable(final String[] toks) {
		String[] inputTks = { toks[0].trim(), toks[1].trim(), toks[2].trim(), toks[3].trim(), toks[7].trim(), toks[4].trim(), "0.0", "0.0", toks[10].trim(),
				toks[9].trim(), toks[12].trim(), toks[13].trim() };

		allSections.add(inputTks);
	}

	// leg site hole core coreType Section TopOffset BottomOffset Depth Length
	// cropped_asman_id
	private void addLIMSTokensToSectionTable(final String[] toks) {
		// hack to allow SPACE in URL string
		String urlString = "";
		for (int i = 10; i < toks.length; i++) {
			String space = i > 10 ? " " : "";
			urlString = urlString + space + toks[i];
		}

		String[] inputTks = { toks[0].trim(), toks[1].trim(), toks[2].trim(), toks[3].trim(), toks[4].trim(), toks[5].trim(), toks[6].trim(), toks[7].trim(),
				toks[8].trim(), toks[9].trim(), "0.0", urlString };

		allSections.add(inputTks);
	}

	public void clearAffineTable() {
		if (affineTable != null) {
			affineTable.clear();
		}
	}

	public void clearSectionList() {
		if (allSections != null) {
			allSections.clear();
		}
	}

	public void clearSpliceTable() {
		if (spliceTable != null) {
			spliceTable.clear();
		}
	}

	public String[] findNextCoreInATie(final String leg, final String site, final String hole, final String core, final String section) {
		String[] ret = null;

		for (int i = 0; i < getNumberOfSpliceTableEntries(); i++) {
			if (getSpliceTableCell(i, OP_INDEX).equalsIgnoreCase("tie") && getSpliceTableCell(i, LEG1_INDEX).equalsIgnoreCase(leg.toLowerCase())
					&& getSpliceTableCell(i, SITE1_INDEX).equalsIgnoreCase(site.toLowerCase())
					&& getSpliceTableCell(i, HOLE1_INDEX).equalsIgnoreCase(hole.toLowerCase())
					&& getSpliceTableCell(i, CORE1_INDEX).equalsIgnoreCase(core.toLowerCase())
					&& getSpliceTableCell(i, SECT1_INDEX).equalsIgnoreCase(section.toLowerCase())) {
				ret = new String[2];
				ret[0] = getSpliceTableCell(i, HOLE2_INDEX);
				ret[1] = getSpliceTableCell(i, CORE2_INDEX);

				break;
			}
		}

		return ret;
	}

	public String getAffineTableCell(final int row, final int col) {
		String[] toks = affineTable.elementAt(row);

		if (toks == null) {
			return null;
		}

		if (toks[col] != null) {
			return toks[col].trim();
		} else {
			return null;
		}
	}

	public String getAffineTableFieldName(final int col) {
		if ((col >= 0) && (col < affineTableMapping.length)) {
			return affineTableMapping[col];
		} else {
			return "N/A";
		}
	}

	public float getAffineTableShift(final String leg, final String site, final String hole, final String core) {
		String ret = "0.0";
		String type = getCoreType(leg, site, hole, core);

		if (type != null) {
			for (String[] line : affineTable) {

				if (line.length >= 7) { // leg, site, hole, core, type, shift,
										// apply?
					if (leg.equalsIgnoreCase(line[0].trim()) && site.equalsIgnoreCase(line[1].trim()) && hole.equalsIgnoreCase(line[2].trim())
							&& core.equalsIgnoreCase(line[3].trim()) && type.equalsIgnoreCase(line[4].trim())) {
						ret = line[5].trim();
					} else {
					}
				} else {
				}
			}
		}

		float v = 0.0f;

		try {
			v = Float.parseFloat(ret);
		} catch (NumberFormatException e) {
			v = 0.0f;
		}

		return v;
	}

	public String getCell(final int row, final int column) {
		String[] toks = allSections.elementAt(row);
		return toks[column];
	}

	public String[] getCoreInfo(final String leg, final String site, final String hole, final String core) {
		return getCoreInfo(leg, site, hole, core, "-999");
	}

	public String[] getCoreInfo(final String leg, final String site, final String hole, final String core, final String startDepthString) {
		String[] ret = { "NA", "0", "0", "0" }; // type, 1st section ID, last
												// section ID, sectionID at
												// 'startDepth'

		String type = getCoreType(leg, site, hole, core);
		ret[0] = type;

		int min = 0;
		int max = 0;
		int start = 0;
		String[] sections = getSectionInACore(leg, site, hole, core, type);

		float startDepth = Float.valueOf(startDepthString);

		for (int i = 0; i < sections.length; i++) {
			String section = sections[i];

			if (section.equalsIgnoreCase("cc")) {
				continue; // ignore core catcher
			}

			int id = Integer.parseInt(section);

			// Begin & end section IDs of the core
			if (i == 0) {
				min = id;
				max = id;
				start = id;
			}

			if (id >= max) {
				max = id;
			}

			if (id <= min) {
				min = id;
			}

			// Determine which section the 'startDepth' belongs to
			float depth = getSectionMCDDepth(leg, site, hole, core, type, section);
			float length = getSectionLength(leg, site, hole, core, type, section);

			if ((startDepth >= depth) && (startDepth < depth + length)) {
				start = id;
			}
		}

		ret[1] = String.valueOf(min);
		ret[2] = String.valueOf(max);
		ret[3] = String.valueOf(start);

		return ret;
	}

	public String[] getCoresInAHoleBelowDepth(final String leg, final String site, final String hole, final float depth) {
		Vector<String> coresBelow = new Vector<String>();

		for (String[] row : allSections) {
			if (row[LEG_INDEX].trim().equalsIgnoreCase(leg) && row[SITE_INDEX].trim().equalsIgnoreCase(site) && row[HOLE_INDEX].trim().equalsIgnoreCase(hole)
					&& row[SECTION_INDEX].trim().equals("1")) {
				String core = row[CORE_INDEX].trim();
				String type = row[TYPE_INDEX].trim();
				float topCoreDepth = getSectionDepth(leg, site, hole, core, type, "1");

				if (topCoreDepth > depth) {
					coresBelow.add(row[CORE_INDEX]);
				}
			}
		}

		Vector<String> uniq = unique(coresBelow);

		return uniq.toArray(new String[uniq.size()]);
	}

	public String getCoreType(final String leg, final String site, final String hole, final String core) {
		for (String[] row : allSections) {
			if (row[LEG_INDEX].trim().equalsIgnoreCase(leg) && row[SITE_INDEX].trim().equalsIgnoreCase(site) && row[HOLE_INDEX].trim().equalsIgnoreCase(hole)
					&& row[CORE_INDEX].trim().equals(core)) {
				return row[TYPE_INDEX].trim();
			}
		}

		return null;
	}

	public String getFieldName(final int col) {
		return mapping[col];
	}

	public int getNumberOfAffineTableColumns() {
		return affineTableMapping.length;
	}

	// affine table accessors
	public int getNumberOfAffineTableEntries() {
		return affineTable.size();
	}

	public int getNumberOfFields() {
		return mapping.length;
	}

	public int getNumberOfSections() {
		return allSections.size();
	}

	public int getNumberOfSpliceTableColumns() {
		return spliceTableMapping.length;
	}

	// splice table accessors
	public int getNumberOfSpliceTableEntries() {
		return spliceTable.size();
	}

	public float getSectionDepth(final String leg, final String site, final String hole, final String core, final String type, final String section) {
		for (String[] toks : allSections) {
			if (toks[LEG_INDEX].equalsIgnoreCase(leg) && toks[SITE_INDEX].equalsIgnoreCase(site) && toks[HOLE_INDEX].equalsIgnoreCase(hole)
					&& toks[CORE_INDEX].equalsIgnoreCase(core) && toks[TYPE_INDEX].equalsIgnoreCase(type) && toks[SECTION_INDEX].equalsIgnoreCase(section)) {
				try {
					return Float.parseFloat(toks[DEPTH_INDEX]);
				} catch (NumberFormatException e) {
					return DEFAULT_DEPTH;
				}
			}
		}

		return DEFAULT_DEPTH;
	}

	public float getSectionDPI(final String leg, final String site, final String hole, final String core, final String type, final String section) {
		for (String[] toks : allSections) {
			if (toks != null) {
				if (toks[LEG_INDEX].equals(leg) && toks[SITE_INDEX].equals(site) && toks[HOLE_INDEX].equals(hole) && toks[CORE_INDEX].equals(core)
						&& toks[TYPE_INDEX].equals(type) && toks[SECTION_INDEX].equals(section)) {
					float dpi;

					try {
						dpi = Float.valueOf(toks[DPI_INDEX].trim());
					} catch (Exception e) { // arrayindexOBex or numberformatex
						dpi = 0.0f;
					}

					return dpi;
				}
			}
		}

		return 0.0f;
	}

	public String[] getSectionInACore(final String leg, final String site, final String hole, final String core, final String type) {
		Vector<String> sections = new Vector<String>();

		for (String[] row : allSections) {
			if (row[LEG_INDEX].trim().equalsIgnoreCase(leg) && row[SITE_INDEX].trim().equalsIgnoreCase(site) && row[HOLE_INDEX].trim().equalsIgnoreCase(hole)
					&& row[CORE_INDEX].trim().equalsIgnoreCase(core) && row[TYPE_INDEX].trim().equalsIgnoreCase(type)) {
				sections.add(row[SECTION_INDEX]);
			}
		}

		return sections.toArray(new String[sections.size()]);
	}

	public String getSectionInfoString(final String leg, final String site, final String hole, final String core, final String type, final String section) {
		return "Leg: " + leg + ", site: " + site + ", hole: " + hole + ", core: " + core + ", type: " + type + ", section: " + section;
	}

	public float getSectionLength(final String leg, final String site, final String hole, final String core, final String type, final String section) {
		for (String[] toks : allSections) {
			if (toks != null) {
				if (toks[LEG_INDEX].equals(leg) && toks[SITE_INDEX].equals(site) && toks[HOLE_INDEX].equals(hole) && toks[CORE_INDEX].equals(core)
						&& toks[TYPE_INDEX].equals(type) && toks[SECTION_INDEX].equals(section)) {
					float length;

					try {
						length = Float.valueOf(toks[LENGTH_INDEX].trim());
					} catch (Exception e) { // arrayindexOBex or numberformatex
						length = DEFAULT_LENGTH;
					}

					return length;
				}
			}
		}

		return DEFAULT_LENGTH;
	}

	public String getSectionListFileStr() {
		return sectionListFileStr;
	}

	public float getSectionMCDDepth(final String leg, final String site, final String hole, final String core, final String type, final String section) {
		float depth = 0.0f;

		// Get mbsf
		for (String[] toks : allSections) {
			if (toks[LEG_INDEX].equalsIgnoreCase(leg) && toks[SITE_INDEX].equalsIgnoreCase(site) && toks[HOLE_INDEX].equalsIgnoreCase(hole)
					&& toks[CORE_INDEX].equalsIgnoreCase(core) && toks[TYPE_INDEX].equalsIgnoreCase(type) && toks[SECTION_INDEX].equalsIgnoreCase(section)) {
				try {
					depth = Float.parseFloat(toks[DEPTH_INDEX]);
				} catch (NumberFormatException e) {
					depth = DEFAULT_DEPTH;
				}
			}
		}

		float affineShift = getAffineTableShift(leg, site, hole, core);

		return depth + affineShift;
	}

	public String getSectionURL(final String leg, final String site, final String hole, final String core, final String type, final String section) {
		for (String[] toks : allSections) {
			if (toks[LEG_INDEX].equals(leg) && toks[SITE_INDEX].equals(site) && toks[HOLE_INDEX].equals(hole) && toks[CORE_INDEX].equals(core)
					&& toks[TYPE_INDEX].equals(type) && toks[SECTION_INDEX].equals(section)) {
				// String mesg = "[] "
				// + getSectionInfoString(leg, site, hole, core, type, section)
				// + " has url: " + toks[URL_INDEX];
				// System.out.println(mesg);

				return toks[URL_INDEX];
			}
		}

		return null;
	}

	public String getSpliceTableCell(final int row, final int col) {
		String[] toks = spliceTable.elementAt(row);

		if (toks == null) {
			return null;
		}

		if (toks[col] != null) {
			return toks[col].trim();
		} else {
			return null;
		}
	}

	public String getSpliceTableFieldName(final int col) {
		if ((col >= 0) && (col < spliceTableMapping.length)) {
			return spliceTableMapping[col];
		} else {
			return "N/A";
		}
	}

	public void loadImagesTableFiles(final Window parent, final String[] fileStrs) {
		if (fileStrs != null) {
			for (String str : fileStrs) {
				boolean isLoaded = loadImageTable(str);
				if (!isLoaded) {
					JOptionPane.showMessageDialog(parent, "Unknown format: '" + str + "'");
				}
			}
		}
	}

	private boolean loadImageTable(final String fileStr) {
		File f = new File(fileStr);
		if (!f.exists()) {
			System.out.println("[LIMSDir] '" + f + "' does not exist, ignore.");
			return false;
		}

		System.out.println("- Loading listing file: '" + fileStr + "'");
		sectionListFileStr = fileStr;

		try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
			// ignore 1st line header/labels
			reader.readLine();

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				// Identify format and length of token array
				// LIMS: 11 tokens separated with SPACE
				// leg site hole core coreType Section TopOffset BottomOffset
				// Depth Length cropped_asman_id
				// Chronos: 14 tokens separated with TAB
				// leg site hole core sectionNumber sectionID sectionType
				// coreType curatedLength linearLength
				// MBSF FORMAT DPI imageURL

				String[] spaceTks = line.split(" ");
				String[] tabTks = line.split("\t");

				if (spaceTks.length >= 11) {
					addLIMSTokensToSectionTable(spaceTks);
				} else if (tabTks.length == 14) {
					addChronosTokensToSectionTable(tabTks);
				} else {
					// ignore
					return false;
				}
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();

			return false;
		}

		return true;
	}

	public void loadPlainTextAffineTable(final File f) {
		if (!f.exists()) {
			return;
		}

		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;
			String[] toks;

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				toks = line.split("\t");
				affineTable.add(toks);
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// need "leg" parameter because the plain text splice file doesn't have
	// leg column
	public void loadPlainTextSpliceTableFile(final File f, final String leg) {
		if (!f.exists()) {
			System.out.println("[LIMSDir] Splice table file '" + f + "' does not exist, ignore.");
			return;
		}

		System.out.println("- Loading splice table file: '" + f.getAbsolutePath() + "'");
		spliceTable.clear();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;
			String[] toks;

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				toks = line.split("\t");

				if (leg != null) {
					String[] new_toks = new String[spliceTableMapping.length];
					new_toks[0] = leg;

					if (toks.length >= 10) {
						System.arraycopy(toks, 0, new_toks, 1, 10);

						if (toks.length > 10) {
							System.arraycopy(toks, 10, new_toks, 12, 9);
							new_toks[11] = leg;
						}
					} else {
						System.arraycopy(toks, 0, new_toks, 1, toks.length);
					}

					spliceTable.add(new_toks);
				} else {
					spliceTable.add(toks);
				}
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "ConstantConditions" })
	public boolean loadXMLAffineTable(final File f) {
		if (!f.exists()) {
			return false;
		}

		try {
			DOMParser parser = new DOMParser();
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);

			parser.parse(f.toURI().toURL().toString());

			Document doc = parser.getDocument();
			Element e = doc.getDocumentElement(); // <Correlator>

			NodeList dataList = e.getChildNodes();
			for (int i = 0; i < dataList.getLength(); i++) {
				if (!(dataList.item(i) instanceof Element)) {
					continue;
				}

				Element dataElement = (Element) dataList.item(i);
				String dataType = dataElement.getAttribute("affine table");
				if (!dataType.equalsIgnoreCase("affine table")) {
					continue;
				}

				String leg = dataElement.getAttribute("leg");
				String site = dataElement.getAttribute("site");

				NodeList holeList = dataElement.getChildNodes();
				for (int j = 0; j < holeList.getLength(); j++) {
					if (!(holeList.item(j) instanceof Element)) {
						continue;
					}

					Element holeElement = (Element) holeList.item(j);
					String hole = holeElement.getAttribute("value");

					NodeList coreList = holeElement.getChildNodes();
					for (int k = 0; k < coreList.getLength(); k++) {
						if (!(coreList.item(k) instanceof Element)) {
							continue;
						}

						Element coreElement = (Element) coreList.item(k);
						String core = coreElement.getAttribute("id");
						String type = coreElement.getAttribute("type");
						String applied = coreElement.getAttribute("applied");
						String offset = coreElement.getAttribute("offset");

						// add to local affine table
						String[] row = { leg, site, hole, core, type, offset, applied };
						affineTable.add(row);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@SuppressWarnings({ "ConstantConditions" })
	public boolean loadXMLSpliceTableFile(final File f) {
		if (!f.exists()) {
			return false;
		}

		try {
			DOMParser parser = new DOMParser();
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);

			parser.parse(f.toURI().toURL().toString());

			Document doc = parser.getDocument();
			Element e = doc.getDocumentElement(); // <Correlator>

			NodeList dataList = e.getChildNodes();
			for (int i = 0; i < dataList.getLength(); i++) {
				if (!(dataList.item(i) instanceof Element)) {
					continue;
				}

				Element dataElement = (Element) dataList.item(i);
				String dataType = dataElement.getAttribute("type");
				if (!dataType.equalsIgnoreCase("splice table")) {
					continue;
				}

				String leg = dataElement.getAttribute("leg");
				String site = dataElement.getAttribute("site");

				NodeList tieList = dataElement.getChildNodes();
				for (int j = 0; j < tieList.getLength(); j++) {
					if (!(tieList.item(j) instanceof Element)) {
						continue;
					}

					Element tieElement = (Element) tieList.item(j);
					Vector<String> row = new Vector<String>();

					NodeList coreList = tieElement.getChildNodes();
					for (int k = 0; k < coreList.getLength(); k++) {
						if (!(coreList.item(k) instanceof Element)) {
							continue;
						}

						Element coreElement = (Element) coreList.item(k);
						String core = coreElement.getAttribute("id");
						String hole = coreElement.getAttribute("hole");
						String type = coreElement.getAttribute("type");
						String section = coreElement.getAttribute("section");
						String top = coreElement.getAttribute("top");
						String bottom = coreElement.getAttribute("bottom");
						String mbsf = coreElement.getAttribute("mbsf");
						String mcd = coreElement.getAttribute("mcd");
						String tietype = coreElement.getAttribute("tietype");

						if ((tietype == null) || tietype.equals("")) {
							row.clear();

							row.add(leg);
							row.add(site);
							row.add(hole);
							row.add(core);
							row.add(type);
							row.add(section);
							row.add(top);
							row.add(bottom);
							row.add(mbsf);
							row.add(mcd);
						} else {
							if (tietype.toLowerCase().startsWith("tie")) {
								row.add("TIE");
							} else {
								row.add(tietype.toUpperCase());
							}

							if ((core == null) || core.equals("")) {
								row.add(""); // leg
								row.add(""); // site
							} else {
								row.add(leg);
								row.add(site);
							}
							row.add(hole);
							row.add(core);
							row.add(type);
							row.add(section);
							row.add(top);
							row.add(bottom);
							row.add(mbsf);
							row.add(mcd);

							String[] rowArray = row.toArray(new String[row.size()]);
							spliceTable.add(rowArray);
							row.clear();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void setSectionListFileStr(final String sectionListFileStr) {
		this.sectionListFileStr = sectionListFileStr;
	}

	private Vector<String> unique(final Vector<String> v) {
		Vector<String> tmpVector = new Vector<String>();

		if (v.isEmpty()) {
			return v;
		}

		for (int i = 0; i < v.size(); i++) {
			String tmpValue = v.elementAt(i);

			if (tmpValue != null) {
				if (tmpVector.isEmpty()) {
					tmpVector.addElement(tmpValue);
				}

				if (tmpVector.indexOf(tmpValue) == -1) {
					tmpVector.addElement(tmpValue);
				}
			}
		}

		return tmpVector;
	}
}
