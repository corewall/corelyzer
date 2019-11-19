/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 outputfile* Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
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
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corelyzer.data.ChatGroup;
import corelyzer.data.MarkerType;
import corelyzer.data.UnitLength;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.WellLogTable;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.FileUtility;
import corelyzer.util.PropertyListUtility;

public class OutputWriter {
	CorelyzerApp app;
	String projectname;
	String site;
	String hole;
	String section;
	String outputfile;
	String outputdir;

	int rulerStart;
	int graphStart;
	int imageStart;
	int markerStart;

	// canvas dpi to convert pixel to cm
	float canvas_dpix;
	float canvas_dpiy;

	// property of section
	float CM_TO_PIXEL; // scale from centimeter to screen pixel
	float section_depth;
	float image_width; // section image width in pixel
	float image_height; // section image height in pixel

	boolean image_orientation; // section image orientation

	float image_dpix;
	float image_dpiy;
	int image_id;

	int style;
	SimpleDateFormat defaultFormat;
	SimpleDateFormat userFormat;

	public OutputWriter() {
		this.app = CorelyzerApp.getApp();
		this.projectname = "Sample Annotation Output";
		this.site = "EVL";
		this.hole = "1";
		this.section = "1";

		this.rulerStart = 0;
		this.graphStart = 0;
		this.imageStart = 0;
		this.markerStart = 0;

		this.CM_TO_PIXEL = 4.7f; // 800 screen pixel for 170 cm height coreimage

		this.style = 0;
		this.defaultFormat = new SimpleDateFormat("MM/dd/yy 'at' hh:mm:ss z");
		this.userFormat = new SimpleDateFormat("MM/dd/yyy 'at' hh:mm:ss z");
	}

	public String extractAnnotaitonContent(final String content) {
		// extract body of annoation
		String ls = System.getProperty("line.separator");
		String result = "";

		// remove header and footer first
		int start, end;
		start = content.indexOf("<table");
		end = content.lastIndexOf("</table>") + 8;
		result = content.substring(start, end);

		// rearrang some styles: especially table format
		// replace table element: some trick
		start = result.indexOf("<table", 0);
		end = result.indexOf(">", start);
		String sub;

		while (end != -1 && start != -1) {
			sub = result.substring(start, end + 1);
			result = result.replaceFirst(sub, "<table class=\"ann_content\">");
			start = result.indexOf("<table", start + 1);
			end = result.indexOf(">", start);
		}

		result = result.replaceAll("<p style=\"margin-left: 5%; margin-right: 10%\">", "<p>");
		result = result.replaceAll("<p style=\"margin-right: 10%; margin-left: 5%\">", "<p>");
		result = result.replaceAll("      <tr>" + ls + "        <td>", "");
		result = result.replaceAll("<b>On", "<th><b>On");
		result = result.replaceAll("wrote: </b><br>", "wrote: </b><br></th>" + ls + "<tr><td>");
		result = result.replaceAll("    </table>" + ls + "    <hr>", "    </table>") + ls;
		result += "<br>" + ls;

		return result;
	}

	public String extractAnnotaitonContentNew(final String content) {
		/*
		 * this is sample table format <table class="ann_content"> <th
		 * class="indent"></th> <th class="time">02/19/07 - 10:42:29 CST</th>
		 * <th class="author">sjames</th> <tr> <td colspan="3"> <p class="c">
		 * Dark Spot. Diameter is 2.75 cm (diagonal) </p> </td> </tr> </table>
		 */

		// extract body of annoation
		String ls = System.getProperty("line.separator");

		// remove header and footer first
		int start, end;
		start = content.indexOf("    <table");
		end = content.lastIndexOf("    </table>") + 12;
		String result = content.substring(start, end);

		/*
		 * Expected result string <table bgcolor="#ffffff" border="0"
		 * width="100%"> <tr> <td> <b>On 02/19/07 at 10:42:29 CST sjames wrote:
		 * </b><br>
		 * 
		 * <p style="margin-right: 10%; margin-left: 5%"> Dark Spot. Diameter is
		 * 2.75 cm (digonal) </p> </td> </tr> </table> <hr>
		 * 
		 * 
		 * <table border="0" bgcolor="#ccccff" width="100%"> <tr> <td> <b>On
		 * 02/20/07 at 12:57:57 CST sjames wrote: </b><br>
		 * 
		 * <p style="margin-right: 10%; margin-left: 5%"> Yes, there it is. </p>
		 * </td> </tr> </table>
		 */
		// parse by line
		StringTokenizer lineTok = new StringTokenizer(result, ls, true);
		String readline;
		String authorStr = "";
		String timeStr = "";
		ParsePosition pos = new ParsePosition(0);
		Date dateFormat;

		result = "";
		while (lineTok.hasMoreTokens()) {
			readline = lineTok.nextToken();

			if (readline.contains("<table")) // a person's blob
			{
				// There's a border 3 table inside a blob, just append the whole
				// table
				if (readline.contains("<table border=\"3\">")) {
					result += "<!-- START: ANNOTATION_INNER_TABLE -->" + ls;

					while (!readline.contains("</table>")) {
						result += readline + ls;

						readline = lineTok.nextToken();
					}
					result += readline + ls;
					result += "<!-- END: ANNOTATION_INNER_TABLE -->" + ls;
				} else {
					// find author line
					readline = lineTok.nextToken();
					while (!readline.contains("<b>On")) {
						readline = lineTok.nextToken();
					}

					// date format conversion and find author
					start = readline.indexOf("<b>On") + 6;
					pos.setIndex(start);
					try {
						timeStr = readline.substring(start);
						dateFormat = this.defaultFormat.parse(readline, pos);
						start = pos.getIndex() + 1;
						end = readline.indexOf("wrote") - 1;
						authorStr = readline.substring(start, end);
						timeStr = this.userFormat.format(dateFormat);
					} catch (Exception ee) {
						System.err.println("Date format exception in annotation output");
					}

					result += "<table class=\"ann_content\">" + ls;
					result += "  <th class=\"indent\"></th>" + ls;
					result += "  <th class=\"time\">" + timeStr + "</th>" + ls;
					result += "  <th class=\"author\">" + authorStr + "</th>" + ls;
					result += "  <tr>" + ls;
					result += "    <td colspan=\"3\">" + ls;
				}
			} else if (readline.contains("<p style")) {
				result += "          <p class=\"c\">" + ls;
			} else if (readline.contains("<hr>")) {
				// do nothing here
			} else if (readline.contains("<img")) {
				// in case that image size is too big not to fix into td
				// make image to fixed size: handled by css
				// for full size image: add link to image
				start = readline.indexOf("src") + 5;
				end = readline.indexOf(">", start) - 1;
				String imageUrl = readline.substring(start, end);
				result += "          <a href=\"" + imageUrl + "\">" + ls;
				result += readline + ls;
				result += "          </a>" + ls;
			} else {
				// just add it to result
				if (!readline.trim().equals("")) {
					result += readline + ls;
				}
			}

		}
		result += "</td></tr></table>" + ls;
		result += "<br>" + ls;

		return result;
	}

	public void setDateFormat(final String format) {
		this.userFormat = new SimpleDateFormat(format);

	}

	public void setProjectDesc(final String project, final String site, final String h, final String s) {
		if (project == null || site == null) {
			return;
		}

		this.projectname = project;
		this.site = site;
		this.hole = h;
		this.section = s;
	}

	public String writeAnnotationHeader(final int trackid, final int secid, final int idx) {
		// Annotation Header table
		// inlcude id, depth, group, type
		// id is index of this annotation in the section

		/*
		 * this is sample table format <table class="ann_header" > <tr> <td
		 * width="60">ID</td> <td colspan="3" width="290">A_00 @ 18.196 CM</td>
		 * </tr> <tr> <td width="50">Group</td> <td
		 * width="125">SEDIMENTOLOGY</td> <td width="50">Type</td> <td
		 * width="125">POINT_MARKER</td> </tr> </table>
		 */

		String ls = System.getProperty("line.separator");
		String header = "";
		String depth = "";
		String group = "";
		String type = "";

		// collect marker information
		int value;
		value = SceneGraph.getCoreSectionMarkerGroup(trackid, secid, idx);
		group = ChatGroup.getGroupName(value);
		value = SceneGraph.getCoreSectionMarkerType(trackid, secid, idx);
		type = MarkerType.getMarkerName(value);
		type = type.substring(5);
		float fvalue;

		switch (value) {
			case 0: // point marker
				fvalue = SceneGraph.getCoreSectionMarkerXPos(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue);

				break;
			case 1: // span marker
				fvalue = SceneGraph.getCoreSectionMarkerV0(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue) + " - ";
				fvalue = SceneGraph.getCoreSectionMarkerV2(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue);
				break;
			case 2: // outline marker
				fvalue = SceneGraph.getCoreSectionMarkerV0(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue) + " - ";
				fvalue = SceneGraph.getCoreSectionMarkerV2(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue);

				break;
		}

		// write header string
		header += "<!-- Annotation " + idx + " -->" + ls;
		header += "<table class=\"ann_header\">" + ls;
		header += "\t<tr>" + ls;
		header += "\t\t<td width=\"60\">ID</td>" + ls;

		if (idx < 10) {
			header += "\t\t<td colspan=\"3\" width=\"290\">A_0" + idx;
		} else {
			header += "\t\t<td colspan=\"3\" width=\"290\">A_" + idx;
		}

		header += " @ " + depth + " CM</td>" + ls;
		header += "\t</tr>" + ls;
		header += "\t<tr>" + ls;
		header += "\t\t<td width=\"50\">Group</td>" + ls;
		header += "\t\t<td width=\"125\">" + group + "</td>" + ls;
		header += "\t\t<td width=\"50\">Type</td>" + ls;
		header += "\t\t<td width=\"125\">" + type + "</td>" + ls;
		header += "\t</tr>" + ls;
		header += "</table>" + ls;
		return header;
	}

	public String writeAnnotationHeaderNew(final int trackid, final int secid, final int idx, final String label) {
		// Annotation Header table
		// inlcude id, depth, group, type
		// id is index of this annotation in the section

		/*
		 * this is sample table format <table class="ann"> <tr><td> <table
		 * class="ann_header"> <tr> <td>A_00 @ 61.37 - 63.73 CM,
		 * SEDIMENTOLOGY</td> </tr> </table> <table class="ann_content"> <th
		 * class="indent"></th> <th class="time">02/19/07 - 10:42:29 CST</th>
		 * <th class="author">sjames</th> <tr> <td colspan="3"> <p class="c">
		 * Dark Spot. Diameter is 2.75 cm (diagonal) </p> </td> </tr> </table>
		 */

		String ls = System.getProperty("line.separator");
		String header = "";
		String depth = "";
		String group = "";
		String type = "";

		// collect marker information
		int value;
		value = SceneGraph.getCoreSectionMarkerGroup(trackid, secid, idx);
		group = ChatGroup.getGroupName(value);
		value = SceneGraph.getCoreSectionMarkerType(trackid, secid, idx);
		type = MarkerType.getMarkerName(value);
		type = type.substring(5);
		float fvalue;

		switch (value) {
			case 0: // point marker
				fvalue = SceneGraph.getCoreSectionMarkerXPos(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue);

				break;
			case 1: // span marker
				fvalue = SceneGraph.getCoreSectionMarkerV0(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue) + " - ";
				fvalue = SceneGraph.getCoreSectionMarkerV2(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue);
				break;
			case 2: // outline marker
				fvalue = SceneGraph.getCoreSectionMarkerV0(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue) + " - ";
				fvalue = SceneGraph.getCoreSectionMarkerV2(trackid, secid, idx);
				fvalue = fvalue * 2.54f / this.canvas_dpix;
				depth += String.format("%10.2f", fvalue);

				break;
		}

		// write header string
		header += "<!-- Annotation " + idx + " -->" + ls;
		header += "<table class=\"ann\">" + ls;
		header += "<tr><td>" + ls;
		header += "<table class=\"ann_header\">" + ls;
		header += "  <tr>" + ls;

		// if(label != null && !label.equals("")) {
		// header += "    <td>" + label;
		// } else {
		if (idx < 10) {
			header += "    <td>A_0" + idx;
		} else {
			header += "    <td>A_" + idx;
		}
		// }

		header += " @ " + depth + " CM, " + group + "</td>" + ls;
		header += "  </tr>" + ls;
		header += "</table>" + ls;
		return header;
	}

	public String writeFooter() {
		String ls = System.getProperty("line.separator");

		return ls + "</td>" + ls + "</tr>" + ls + "</table>" + ls + "</body>" + "</html>";
	}

	public String writeHeader(final int trackid, final int secid) {
		String ls = System.getProperty("line.separator");
		String header = "";
		String imageurl = SceneGraph.getImageURL(this.image_id);

		System.out.println("---> [DEBUG] trackId: " + trackid + ", secid: " + secid + ", imageId: " + this.image_id);
		System.out.println("---> [DEBUG] localName: '" + SceneGraph.getImageName(image_id));
		System.out.println("---> [DEBUG] URL:       '" + SceneGraph.getImageURL(this.image_id));

		// html document header
		header += "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
		header += ls + "<html lang=\"en\" xml:lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\">";
		header += ls + "<head>";
		header += ls + "<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\" />";
		header += ls + "<title>CoreWall Annotaiton</title>";
		header += ls + "<link REL=\"stylesheet\" TYPE=\"text/css\" href=\"css/annotationstyle.css\">";
		header += ls + "</head>";
		header += ls + "<body>" + ls + ls;

		/*
		 * Sameple header html <!-- Start of section header information -->
		 * <table width="750" border="0" cellspacing="0" cellpadding="0"> <tr>
		 * <td width="750"> <h1>CoreWall Annotation Repository</h1> </td> </tr>
		 * </table> <br> <table class="header"> <tr> <th width="60"> Project
		 * </th> <td colspan="7" width="690"> GLAD4 </td> </tr> <tr> <th
		 * width="60"> Site </th> <td width="120"> HST03 </td> <th width="50">
		 * Hole </th> <td width="120"> 1A </td> <th width="50"> Section </th>
		 * <td width="160"> GLAD4-HST03-1A-1H-1 </td> <th width="50"> Depth
		 * </th> <td width="60"> 15.2 M </td> </tr> <tr> <td colspan="8"
		 * width="750"> <b>Section Image URL: </b><a href="unknown"
		 * target="_blank">unknown</a> </td> </tr> </table> <br><br> <!-- End of
		 * section header information -->
		 */

		String depth = String.format("%10.2f", this.section_depth) + " M";
		header += "<!-- Start of section header information -->" + ls;
		header += "<table width=\"750\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">" + ls;
		header += "<tr>" + ls;
		header += "    <td width=\"750\">" + ls;
		header += "        <h1>CoreWall Annotation Repository</h1>" + ls;
		header += "    </td>" + ls;
		header += "</tr>" + ls;
		header += "</table>" + ls;
		header += "<br>" + ls;
		header += "<table class=\"header\">" + ls;
		header += "\t<tr>" + ls;
		header += "\t<th width=\"60\">" + ls;
		header += "\t\tProject" + ls;
		header += "\t</th>" + ls;
		header += "\t<td colspan=\"7\" width=\"690\">" + ls;
		header += "\t\t" + this.projectname + ls;
		header += "\t</td>" + ls;
		header += "\t</tr>" + ls;
		header += "\t<tr>" + ls;
		header += "\t<th width=\"60\">" + ls;
		header += "\t\tSite" + ls;
		header += "\t</th>" + ls;
		header += "\t<td width=\"120\">" + ls;
		header += "\t\t" + this.site + ls;
		header += "\t</td>" + ls;
		header += "\t<th width=\"50\">" + ls;
		header += "\t\tHole" + ls;
		header += "\t</th>" + ls;
		header += "\t<td width=\"100\">" + ls;
		header += "\t\t" + this.hole + ls;
		header += "\t</td>" + ls;
		header += "\t<th width=\"50\">" + ls;
		header += "\t\tSection" + ls;
		header += "\t</th>" + ls;
		header += "\t<td width=\"160\">" + ls;
		header += "\t\t" + this.section + ls;
		header += "\t</td>" + ls;
		header += "\t<th width=\"50\">" + ls;
		header += "\t\tDepth" + ls;
		header += "\t</th>" + ls;
		header += "\t<td width=\"80\">" + ls;
		header += "\t\t" + depth + ls;
		header += "\t</td>" + ls;
		header += "\t</tr>" + ls;
		header += "\t<tr>" + ls;
		header += "\t<td colspan=\"8\" width=\"750\">" + ls;
		header += "\t\t<b>Section Image URL: </b><a href=\"" + imageurl + "\" target=\"_blank\">" + imageurl + "</a>" + ls;
		header += "\t</td>" + ls;
		header += "\t</tr>" + ls;
		header += "</table>" + ls;
		header += "<br><br>" + ls;
		header += "<!-- End of section header information -->" + ls + ls;

		return header;

	}

	public boolean writeHtml(final String filename, final int trackid, final int secid) {
		// Copy css and image file to output dir
		// bin/resources/css/annotationstyle.css to
		// output/css/annotationstyle.css
		// downloads dir/image file to output/image/image file
		String sp = System.getProperty("file.separator");

		// destination dir path
		File outfile = new File(filename);
		this.outputdir = outfile.getPath();
		String fn = outfile.getName();
		int start = this.outputdir.lastIndexOf(fn);
		this.outputdir = this.outputdir.substring(0, start);

		// sub dir
		String cssDirPath = this.outputdir + "css";
		String imgDirPath = this.outputdir + "images";
		String attachmentDirPath = this.outputdir + "attachments";

		// image src/dst path
		int id = SceneGraph.getImageIdForSection(trackid, secid);
		String imgSrcPath = SceneGraph.getImageName(id);
		File imgfile = new File(imgSrcPath);
		String imgDstPath = imgDirPath + sp + imgfile.getName();

		// css src/dst path
		String cssDstPath = cssDirPath + sp + "annotationstyle.css";
		String cssSrcPath = app.preferences().appStart_Directory;
		cssSrcPath += sp + "resources" + sp + "css" + sp + "annotationstyle.css";

		// create css, image directory
		File cssDir = new File(cssDirPath);
		File imgDir = new File(imgDirPath);
		File attDir = new File(attachmentDirPath);

		if (!cssDir.exists()) {
			cssDir.mkdir();
		}
		if (!imgDir.exists()) {
			imgDir.mkdir();
		}
		if (!attDir.exists()) {
			attDir.mkdir();
		}

		// copy css, image file
		if (!FileUtility.copyFile(cssSrcPath, cssDstPath)) {
			return false;
		}
		if (!FileUtility.copyFile(imgSrcPath, imgDstPath)) {
			return false;
		}

		this.outputfile = filename;

		// collect section information here
		this.canvas_dpix = SceneGraph.getCanvasDPIX(0);
		this.canvas_dpiy = SceneGraph.getCanvasDPIY(0);

		this.image_orientation = SceneGraph.getSectionOrientation(trackid, secid);
		this.image_height = SceneGraph.getImageHeight(id);
		this.image_width = SceneGraph.getImageWidth(id);
		this.image_dpix = SceneGraph.getSectionDPIX(trackid, secid);
		this.image_dpiy = SceneGraph.getSectionDPIY(trackid, secid);
		this.section_depth = SceneGraph.getSectionDepth(trackid, secid) / 100.0f;
		this.image_id = SceneGraph.getImageIdForSection(trackid, secid);

		System.out.println("---> [DEBUG] id: " + id + ", this.image_id: " + this.image_id);

		String ls = System.getProperty("line.separator");
		String content = "";
		content = content + writeHeader(trackid, secid);
		content = content + writeImage(trackid, secid);

		// annotation write loop
		int numMarker = SceneGraph.getNumCoreSectionMarkers(trackid, secid);

		content += "<!-- Embeded Annotation table start here -->" + ls;

		// depth-sort annotations
		Hashtable<Float, Integer> indexDepthHash = new Hashtable<Float, Integer>();
		for (int i = 0; i < numMarker; i++) {
			float depth = SceneGraph.getCoreSectionMarkerXPos(trackid, secid, i);
			indexDepthHash.put(depth, i);
		}

		Vector<Float> v = new Vector<Float>(indexDepthHash.keySet());
		Collections.sort(v);
		Iterator<Float> it = v.iterator();
		while (it.hasNext()) {
			float depth = (Float) it.next();
			int i = indexDepthHash.get(depth);

			String name = SceneGraph.getCoreSectionMarkerLocal(trackid, secid, i);

			String label = SceneGraph.getCoreSectionMarkerText(trackid, secid, i);

			// for now, just use local copy of annotation html
			File annotfile = new File(name);
			String annotation = "";
			String annotationHeader = "";

			if (annotfile.exists()) {
				// read annotation html file to string
				try {
					BufferedReader reader = new BufferedReader(new FileReader(annotfile));
					reader.toString();
					String line = "";
					while ((line = reader.readLine()) != null) {
						annotation += line + ls;
					}

					reader.close();
				} catch (IOException e) {
					System.err.println("IOException in reading ser ver list file");
					continue;
				}

				// Scan through annotation content & copy required images
				// File srcFile = new File(fromFile);
				// File dstFile = new File(toFile);

				String prefix = outfile.getParent();
				String srcPrefix = annotfile.getParent(); // srcFile.getParent();

				String patternStr = "(src|href)\\s*=\\s*\"([^\"]+)\"";
				Pattern pattern = Pattern.compile(patternStr);
				Matcher matcher = pattern.matcher(annotation);

				while (matcher.find()) {
					String groupStr = matcher.group(0);
					String folderName;

					// Check target
					if (groupStr.toLowerCase().startsWith("src=")) {
						groupStr = groupStr.substring(5, groupStr.length() - 1);
						folderName = "images";
					} else if (groupStr.toLowerCase().startsWith("href=")) {
						groupStr = groupStr.substring(6, groupStr.length() - 1);
						folderName = "attachments";
					} else {
						continue;
					}

					String filepath;
					String attachmentFilename; // filename;

					try {
						// Add src annotation file prefix if not in file:///
						// protocol
						if (!groupStr.trim().toLowerCase().startsWith("file:")) {
							groupStr = "file:////" + srcPrefix + sp + groupStr.trim();
						}

						URL u = new URL(groupStr);
						filepath = u.getFile();
						attachmentFilename = new File(filepath).getName();

						// -- replace the string in input file
						String resString = folderName + "/" + attachmentFilename;
						annotation = annotation.replace(groupStr, resString);

						// -- Make a copy of attachment and place it into
						// -- annotation folder
						System.out.println("-- [INFO] Copy [" + filepath + "] to " + prefix + sp + folderName + sp + attachmentFilename);

						FileUtility.copyFile(filepath, prefix + sp + folderName + sp + attachmentFilename);

					} catch (MalformedURLException e) {
						System.err.println("MalformedURL! " + e);
					}

				}
				// end

				// generate annotation header table
				annotationHeader = writeAnnotationHeaderNew(trackid, secid, i, label);

			} else {
				continue;
			}

			content += annotationHeader;

			// String type;
			if (name.endsWith(".plist")) {
				// type = "dictionary";
				content += PropertyListUtility.generateHTMLTableString(annotfile);
			} else if (name.endsWith("html")) {
				// type = "html";
				content += extractAnnotaitonContentNew(annotation);
			} else {
				// type = "html";
				content += extractAnnotaitonContentNew(annotation);
			}

			content += "<!-- End of Annotation " + i + " -->" + ls;
		}

		content += "<!-- Embeded Annotation table end here -->" + ls;
		content = content + writeFooter();

		// write this to file
		try {
			File f = new File(filename);
			FileWriter fw = new FileWriter(f);
			fw.write(content, 0, content.length());
			fw.close();
		} catch (Exception ee) {
			System.out.println("Failed to write out html to disk:");
			ee.printStackTrace();
		}

		// open browser with this output file
		String app;
		try {
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				app = "cmd.exe /c explorer " + "file:///" + filename;
				Runtime.getRuntime().exec(app);
			} else {
				app = "open";
				String[] cmd = { app, "file:///" + filename };
				Runtime.getRuntime().exec(cmd);
			}

		} catch (IOException ex) {
			System.err.println("IOException in opening annotation output" + "file with system browser");
		}

		// we've done html writing
		return true;
	}

	public String writeImage(final int trackid, final int secid) {
		// step 1: generate svg
		// - find image file, check rotation & scale
		// - generate ruler
		// - generate graph if available
		// step 2: write html code for svg image

		// SVG filename
		File svgfile = new File(this.outputfile);
		String svgfilename = svgfile.getName();
		int end = svgfilename.lastIndexOf(".html");
		svgfilename = svgfilename.substring(0, end);
		svgfilename = svgfilename + ".svg"; // Name only

		// Section Image filename
		String imgName = SceneGraph.getImageName(this.image_id);
		File imgfile = new File(imgName);
		imgName = imgfile.getName(); // Name only
		// imgName = "image" + sp + imgName; ima

		// Generate SVG
		writeSVG(svgfilename, imgName, trackid, secid);

		/*
		 * html code sample <!-- Insert Section Image --> <table width="750"
		 * border="0" cellspacing="0" cellpadding="0"> <tr> <td rowspan="1"
		 * align="left" valign="top" width="420" aligh="top"> <object
		 * type="image/svg+xml" name="section_image" data="test_new.svg"
		 * width="400" height="830"></object> </td> <td align="left"
		 * valign="top" width="425"> <!-- End of Section Image -->
		 */

		// write html code
		String ls = System.getProperty("line.separator");
		String imagehtml = ls + "<!-- Main corelyzer.data.Table Starts -->" + ls;
		imagehtml += "<table class=\"main\">" + ls;
		imagehtml += "<!-- Insert Section Image -->" + ls;
		imagehtml += "<tr>" + ls;
		imagehtml += "<td class=\"image\">" + ls;
		imagehtml += "<object type=\"image/svg+xml\" name=\"section_image\" data=\"";
		imagehtml += "images/" + svgfilename + "\"";
		imagehtml += " width=\"400\" height=\"830\"></object>" + ls;
		imagehtml += "</td>" + ls;
		imagehtml += "<!-- End of Section Image -->" + ls + ls;
		imagehtml += "<td class=\"annotation\">" + ls;

		return imagehtml;
	}

	public void writeSVG(String filename, final String img, final int trackid, final int secid) {
		// return writeSVG(filename, "");
		String content = "";
		String ls = System.getProperty("line.separator");
		String sp = System.getProperty("file.separator");
		content += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls;
		content += "<svg height=\"825\" width=\"400\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">" + ls;
		content += ls + ls;

		content += "<g transform=\"translate(0, 25)\">" + ls;

		content += writeSVGRuler();
		content += writeSVGGraph(trackid, secid);
		content += writeSVGImage(img, trackid, secid);
		content += writeSVGMarker(trackid, secid);

		content += "</g>" + ls;
		content += "</svg>";

		// write this to file
		try {
			filename = this.outputdir + sp + "images" + sp + filename;
			File f = new File(filename);
			FileWriter fw = new FileWriter(f);
			fw.write(content, 0, content.length());
			fw.close();
		} catch (Exception ee) {
			System.out.println("Failed to write out svg to disk:");
			ee.printStackTrace();
		}

	}

	private String writeSVGGraph(final int trackid, final int secid) {
		String result = "";
		String ls = System.getProperty("line.separator");

		int field, length, gid, r, g, b;
		float min, max, vScale, height, value;
		String fieldname;
		int numGraph = SceneGraph.getNumGraphsForSection(trackid, secid);

		if (numGraph <= 0) {
			this.imageStart = this.graphStart;
			return result;
		}

		this.imageStart = this.graphStart + 55 * numGraph;

		float secheight;
		if (this.image_orientation == SceneGraph.LANDSCAPE) {
			float scale = 2.54f / this.image_dpix * this.CM_TO_PIXEL;
			secheight = this.image_width * scale;
		} else { // SceneGraph.PORTRAIT
			float scale = 2.54f / this.image_dpiy * this.CM_TO_PIXEL;
			secheight = this.image_height * scale;
		}

		for (int i = 0; i < numGraph; i++) {
			gid = SceneGraph.getGraphIDFromSectionSlot(trackid, secid, i);

			if (gid == -1) {
				continue;
			}

			int datasetID = SceneGraph.getDatasetReference(gid);
			int tableID = SceneGraph.getTableReference(gid);

			WellLogDataSet wdataset = (WellLogDataSet) app.getDataFileListModel().elementAt(datasetID);
			WellLogTable wtable = wdataset.getTable(tableID);

			// Assume that all graph in canvas will be printed out
			int unit = wtable.getDepthUnits();
			float unitScale = this.CM_TO_PIXEL; // convert depth to pixel for
												// svg
			if (unit == UnitLength.CM) {
				unitScale *= 1;
			} else if (unit == UnitLength.M) {
				unitScale *= 100.0f;
			} else if (unit == UnitLength.MM) {
				unitScale *= 0.5f;
			} else if (unit == UnitLength.INCH) {
				unitScale *= 2.54f;
			} else if (unit == UnitLength.FOOT) {
				unitScale *= 2.54f * 12.0f;
			} else if (unit == UnitLength.YARD) {
				unitScale *= 2.54f * 12.0f * 3.0f;
			}

			field = SceneGraph.getFieldReference(gid);
			r = (int) (SceneGraph.getLineGraphColorComponent(gid, 0) * 255);
			g = (int) (SceneGraph.getLineGraphColorComponent(gid, 1) * 255);
			b = (int) (SceneGraph.getLineGraphColorComponent(gid, 2) * 255);

			// in case of white, change it to black
			// since ouput background is white
			if (r == 255 && g == 255 && b == 255) {
				r = g = b = 0;
			}

			min = wtable.getColumnMin(field);
			max = wtable.getColumnMax(field);
			length = wtable.getNumRows();
			vScale = 50.0f / (max - min);
			fieldname = wtable.getHeader(field + 1);
			System.out.println("graph min, max: " + min + "," + max);
			result += "<!-- Core Section corelyzer.data.Graph " + i + " -->" + ls;

			// graph transform
			result += "<g transform=\"translate(" + (this.graphStart + 55 * i) + ", 0)\">" + ls;

			// write outline, unit, misc.
			// graph label
			result += "<text x = \"0\" y = \"" + -15 + "\" fill = \"black\" font-size = \"10\">" + fieldname + "</text>" + ls;
			// graph outline
			result += "<rect x=\"0\" y=\"0\" " + "width=\"" + 50 + "\" height=\"" + secheight + "\" "
					+ "fill=\"none\" stroke=\"black\" stroke-width=\"0.5\"  />" + ls;
			// some grid
			result += "<g style=\"stroke:black;file:none;stroke-width:0.3;stroke-dasharray: 3 3;\">" + ls;
			result += "<path d=\"M " + 10 + " 0 v " + secheight + "\"/>" + ls;
			result += "<path d=\"M " + 20 + " 0 v " + secheight + "\"/>" + ls;
			result += "<path d=\"M " + 30 + " 0 v " + secheight + "\"/>" + ls;
			result += "<path d=\"M " + 40 + " 0 v " + secheight + "\"/>" + ls;

			for (int k = 1; k < secheight * 0.1f / this.CM_TO_PIXEL; k++) {
				result += "<path d=\"M 0 " + k * 10 * this.CM_TO_PIXEL + " h 50\"/>" + ls;
			}

			result += "</g>" + ls;
			result += "<polyline fill=\"none\" stroke=\"rgb(" + r + "," + g + "," + b + ")\" stroke-width=\"1.5\"" + ls;
			result += "          points=\"" + ls;

			for (int j = 0; j < length; j++) {
				height = wtable.getDepth(j);
				height = height * unitScale;
				value = wtable.getCell(j, field + 1);

				if (value < min) {
					continue;
				}

				value = (value - min) * vScale;

				result += "                    " + value + ", " + height + ls;
			}

			result += "\"/>" + ls;
			result += "</g>" + ls;
			result += "<!-- End of Core Section corelyzer.data.Graph " + i + " -->" + ls + ls;
		}

		return result;
	}

	public String writeSVGImage(final String img, final int trackid, final int secid) {
		String result = "";
		String ls = System.getProperty("line.separator");

		// prepare for some factors: rotaton, scale, image size and etc
		float rotation = this.image_orientation == SceneGraph.LANDSCAPE ? 0.0f : 90.0f;

		float scalex = 2.54f / this.image_dpix * this.CM_TO_PIXEL;
		float scaley = 2.54f / this.image_dpiy * this.CM_TO_PIXEL;

		// correct rotation value: native code CCW, SVG CW
		// so, inverse native rotation and add 90 degree to align image as
		// vertical
		rotation = (-rotation + 90.0f) % 360.0f;
		float iarea, trans;
		if (rotation == 0) {
			// swap scale since output uses vertical aligment of coreimage
			float temp = scalex;
			scalex = scaley;
			scaley = temp;
			iarea = this.image_width * scalex;
			trans = 0.0f;
		} else {
			iarea = this.image_height * scaley;
			trans = this.image_height;
		}
		// float iarea = (rotation == 0.0f) ? (ix * scale) : (iy * scale);
		// float trans = (rotation == 0.0f) ? 0.0f : iy;
		this.markerStart = this.imageStart + (int) iarea;

		result += "<!-- Core Section Image -->" + ls;
		result += "<g transform=\"translate(" + this.imageStart + ", 0)\">" + ls;
		result += "<g transform=\"scale(" + scalex + ", " + scaley + ")\">" + ls;
		result += "<g transform=\"translate(" + trans + ", 0)\">" + ls;
		result += "<g transform=\"rotate(" + rotation + ")\">" + ls;
		result += "<image width=\"" + this.image_width + "px\" height=\"" + this.image_height + "px\" xlink:href=\"" + img + "\">" + ls;
		result += "</image>" + ls;
		result += "</g></g></g></g>" + ls;
		result += "<!-- End of Core Section Image -->" + ls + ls;

		return result;

	}

	public String writeSVGMarker(final int trackid, final int secid) {
		String result = "";
		String ls = System.getProperty("line.separator");

		int numMarkers = SceneGraph.getNumCoreSectionMarkers(trackid, secid);
		int markerType; // = MarkerType.CORE_DEFAULT_MARKER;
		float x, y, ax, ay, v0, v1, v2, v3;

		result += "<!-- Core Section Marker -->" + ls;
		result += "<g transform=\"translate(" + this.markerStart + ", 0)\">" + ls;

		for (int i = 0; i < numMarkers; i++) {
			// type: point, span, block
			markerType = SceneGraph.getCoreSectionMarkerType(trackid, secid, i);

			x = SceneGraph.getCoreSectionMarkerXPos(trackid, secid, i); // point
																		// in
																		// section
			y = SceneGraph.getCoreSectionMarkerYPos(trackid, secid, i); // point
																		// in
																		// section
			ax = SceneGraph.getCoreSectionMarkerIconXPos(trackid, secid, i); // icon
																				// pos
			ay = SceneGraph.getCoreSectionMarkerIconYPos(trackid, secid, i); // icon
																				// pos
			v0 = SceneGraph.getCoreSectionMarkerV0(trackid, secid, i);
			v1 = SceneGraph.getCoreSectionMarkerV1(trackid, secid, i);
			v2 = SceneGraph.getCoreSectionMarkerV2(trackid, secid, i);
			v3 = SceneGraph.getCoreSectionMarkerV3(trackid, secid, i);

			x = x * 2.54f * this.CM_TO_PIXEL / this.canvas_dpix; // pixel for
																	// SVG
			y = y * 2.54f * this.CM_TO_PIXEL / this.canvas_dpiy; // pixel for
																	// SVG
			ax = ax * 2.54f * this.CM_TO_PIXEL / this.canvas_dpix; // pixel for
																	// SVG
			ay = ay * 2.54f * this.CM_TO_PIXEL / this.canvas_dpiy; // pixel for
																	// SVG
			v0 = v0 * 2.54f * this.CM_TO_PIXEL / this.canvas_dpix; // pixel for
																	// SVG
			v1 = v1 * 2.54f * this.CM_TO_PIXEL / this.canvas_dpix; // pixel for
																	// SVG
			v2 = v2 * 2.54f * this.CM_TO_PIXEL / this.canvas_dpix; // pixel for
																	// SVG
			v3 = v3 * 2.54f * this.CM_TO_PIXEL / this.canvas_dpix; // pixel for
																	// SVG

			// adjust image offset
			// native -> horizontal arrangement, output -> vertical arrangement
			// therefore swap x and y
			// native y value is negative to upward, so change its sign here
			y = -y;
			ay = -ay;

			switch (markerType) {
				case MarkerType.CORE_POINT_MARKER: {
					// only need x,y,ax,ay
					result += "<!-- Marker 0" + i + " : POINT_MARKER -->" + ls;

					result += "\t<line x1=\"" + (y + 0.5) + "\" y1=\"" + (x + 0.5) + "\" x2=\"" + (ay + 0.5) + "\" y2=\"" + (ax + 0.5) + "\" "
							+ "style=\"stroke:rgb(0,0,0);stroke-width:1\"/>" + ls;
					result += "\t<line x1=\"" + y + "\" y1=\"" + x + "\" x2=\"" + ay + "\" y2=\"" + ax + "\" "
							+ "style=\"stroke:rgb(220,220,220);stroke-width:1\"/>" + ls;
					result += "\t<text x = \"" + (ay + 3) + "\" y = \"" + (ax + 7) + "\" fill = \"black\" font-size = \"18\">A_0" + i + "</text>" + ls;
				}
					break;
				case MarkerType.CORE_SPAN_MARKER: {
					result += "<!-- Marker 0" + i + " : SPAN_MARKER -->" + ls;

					// adjust sign
					v1 = -v1;
					v3 = -v3;

					// connector: cx,cy is center of span
					float cx = v1;
					float cy = (v0 + v2) / 2.0f;

					// shadow first: connector, span, end up, end bottom
					result += "\t<line x1=\"" + (cx + 0.5) + "\" y1=\"" + (cy + 0.5) + "\" x2=\"" + (ay + 0.5) + "\" y2=\"" + (ax + 0.5) + "\" "
							+ "style=\"stroke:rgb(0,0,0);stroke-width:1\"/>" + ls;
					result += "\t<line x1=\"" + (v1 + 0.5) + "\" y1=\"" + (v0 + 0.5) + "\" x2=\"" + (v3 + 0.5) + "\" y2=\"" + (v2 + 0.5) + "\" "
							+ "style=\"stroke:rgb(0,0,0);stroke-width:1\"/>" + ls;
					result += "\t<line x1=\"" + (v1 + 0.5 - 5) + "\" y1=\"" + (v0 + 0.5) + "\" x2=\"" + (v1 + 0.5 + 5) + "\" y2=\"" + (v0 + 0.5) + "\" "
							+ "style=\"stroke:rgb(0,0,0);stroke-width:1\"/>" + ls;
					result += "\t<line x1=\"" + (v3 + 0.5 - 5) + "\" y1=\"" + (v2 + 0.5) + "\" x2=\"" + (v3 + 0.5 + 5) + "\" y2=\"" + (v2 + 0.5) + "\" "
							+ "style=\"stroke:rgb(0,0,0);stroke-width:1\"/>" + ls;

					// bright line: connector span, end up, end bottom
					result += "\t<line x1=\"" + cx + "\" y1=\"" + cy + "\" x2=\"" + ay + "\" y2=\"" + ax + "\" "
							+ "style=\"stroke:rgb(220,220,220);stroke-width:1\"/>" + ls;
					result += "\t<line x1=\"" + v1 + "\" y1=\"" + v0 + "\" x2=\"" + v3 + "\" y2=\"" + v2 + "\" "
							+ "style=\"stroke:rgb(220,220,220);stroke-width:1\"/>" + ls;
					result += "\t<line x1=\"" + (v1 - 5) + "\" y1=\"" + v0 + "\" x2=\"" + (v1 + 5) + "\" y2=\"" + v0 + "\" "
							+ "style=\"stroke:rgb(220,220,220);stroke-width:1\"/>" + ls;
					result += "\t<line x1=\"" + (v3 - 5) + "\" y1=\"" + v2 + "\" x2=\"" + (v3 + 5) + "\" y2=\"" + v2 + "\" "
							+ "style=\"stroke:rgb(220,220,220);stroke-width:1\"/>" + ls;

					// marker label
					result += "\t<text x = \"" + (ay + 3) + "\" y = \"" + (ax + 7) + "\" fill = \"black\" font-size = \"18\">A_0" + i + "</text>" + ls;

				}
					break;
				case MarkerType.CORE_OUTLINE_MARKER: {
					result += "<!-- Marker 0" + i + " : OUTLINE_MARKER -->" + ls;

					// adjust sign
					v1 = -v1;
					v3 = -v3;

					// my is the middle of outline box
					float my = (v0 + v2) / 2.0f;
					float w = v1 - v3;
					float h = v2 - v0;

					// shadow: connector & outline
					result += "\t<line x1=\"" + (v1 + 0.5) + "\" y1=\"" + (my + 0.5) + "\" x2=\"" + (ay + 0.5) + "\" y2=\"" + (ax + 0.5) + "\" "
							+ "style=\"stroke:rgb(0,0,0);stroke-width:1\"/>" + ls;
					result += "\t<rect x=\"" + (v3 + 0.5) + "\" y=\"" + (v0 + 0.5) + "\" " + "width=\"" + (w + 0.5) + "\" height=\"" + (h + 0.5) + "\" "
							+ "fill=\"none\" stroke=\"black\" stroke-width=\"1\"  />" + ls;

					// connector & outline
					result += "\t<line x1=\"" + v1 + "\" y1=\"" + my + "\" x2=\"" + ay + "\" y2=\"" + ax + "\" "
							+ "style=\"stroke:rgb(220,220,220);stroke-width:1\"/>" + ls;
					result += "\t<rect x=\"" + v3 + "\" y=\"" + v0 + "\" " + "width=\"" + w + "\" height=\"" + h + "\" "
							+ "fill=\"none\" stroke=\"rgb(220,220,220)\" stroke-width=\"1\"  />" + ls;

					// marker label
					result += "\t<text x = \"" + (ay + 3) + "\" y = \"" + (ax + 7) + "\" fill = \"black\" font-size = \"18\">A_0" + i + "</text>" + ls;
				}
					break;
			}

		}

		result += "</g>" + ls;
		result += "<!-- End of Core Section Marker -->" + ls + ls;

		return result;

	}

	public String writeSVGRuler() {
		// generate svg body of ruler: tick on every cm
		// we use h * w = 820 * 400 pixel space (160 cm height)
		// 1 cm is 5 pixel to format output page
		// therefore, ruler space is w 20, h 800
		String result = "";
		String ls = System.getProperty("line.separator");
		float y = 0.0f;
		this.graphStart = 25;

		result += "<!-- Ruler -->" + ls;

		/*
		 * example code <line x1="0" y1="0" x2="300" y2="300"
		 * style="stroke:rgb(99,99,99);stroke-width:2"/>
		 * 
		 * <text x = "0" y = "0" fill = "black" font-size = "10">10</text>
		 */
		// a little adjustment
		// result += "<g transform=\"translate(0, 1)\">" + ls;
		result += "<text x = \"0\" y = \"" + -5 + "\" fill = \"black\" font-size = \"10\">CM</text>" + ls;
		result += "<g stroke=\"black\" stroke-width=\"1\">" + ls;
		for (int i = 0; i < 170; i++) {
			y = i * this.CM_TO_PIXEL;
			if (i % 10 == 0) {
				// longer tick 6 pix
				// result += "<line x1=\"13\" y1=\"" + y + "\" x2=\"19\" y2=\""
				// + y + "\" " +
				// "style=\"stroke:rgb(0,0,0);stroke-width:1\"/>" + ls;
				result += "<line x1=\"13\" y1=\"" + y + "\" x2=\"19\" y2=\"" + y + "\" />" + ls;
				// label of cm in every 10 cm
				result += "<text x = \"0\" y = \"" + (y + 3) + "\" fill = \"black\" font-size = \"8\">" + i + "</text>" + ls;

			} else {
				// short tick 3 pix
				// result += "<line x1=\"16\" y1=\"" + y + "\" x2=\"19\" y2=\""
				// + y + "\" " +
				// "style=\"stroke:rgb(0,0,0);stroke-width:1\"/>" + ls;
				result += "<line x1=\"16\" y1=\"" + y + "\" x2=\"19\" y2=\"" + y + "\" />" + ls;
			}
		}

		result += "</g>" + ls;
		result += "<!-- End of Ruler -->" + ls + ls;

		return result;
	}
}
