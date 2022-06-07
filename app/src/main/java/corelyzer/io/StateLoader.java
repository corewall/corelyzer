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

package corelyzer.io;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;

import corelyzer.data.ChatGroup;
import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionGraph;
import corelyzer.data.MarkerType;
import corelyzer.data.SectionTiePoint;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.WellLogTable;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.graphics.SceneGraph;
import corelyzer.helper.URLRetrieval;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.SwingSafeDirectoryChooser;
import corelyzer.util.FileUtility;

@SuppressWarnings({"unused"}) // stifle two goofy dead code warnings, find on "suppressed"

/**
 * Class used to load the working session state file in XML format.
 */
public class StateLoader {
	// Standard testing main program
	public static void main(final String[] argv) {
		System.out.println("CoreWall \'loadState.cpp\' Java port");

		if (argv.length != 1) {
			System.out.println("Usage: java corelyzer.io.StateLoader \"state_filename\"");
			System.exit(1);
		}

		StateLoader sfloader = new StateLoader();
		sfloader.loadState(argv[0]);
	}

	/**
	 * The string path of the session file to be loaded
	 */
	String stateFilename;
	float src_canvas_dpix;
	float src_canvas_dpiy;
	int totallength;
	int nodecount;

	float version;
	CorelyzerApp app;

	CoreGraph cg;
	JProgressBar pdlg;

	private String datadirectory;
	String proj_dir_path = null;
	String img_dir_path = null;
	String data_dir_path = null;

	String anno_dir_path = null;
	final String sp = System.getProperty("file.separator");
	final int ABORT = 0;
	final int IGNORE = 1;
	final int SELECT = 2;
	final int OK = 3;

	private boolean isAbort = false;

	final static float DEFAULT_DPI = 254.0f;

	public StateLoader() {
		app = CorelyzerApp.getApp();
		cg = CoreGraph.getInstance();

		pdlg = app.getProgressUI();
	}

	// ---------------------- Annotations --------------------------
	/**
	 * Load annotation information with root XML element
	 * 
	 * @param e
	 *            Root XML element
	 * @param trackId
	 *            Track ID the annotation associated with
	 * @param sectionId
	 *            Core section ID the annotation associated with
	 */
	void loadAnnotationXML(final Element e, final int trackId, final int sectionId) {
		// check whether this xml support new annotation maker style or not
		if (e.getAttribute("group").length() != 0) {
			// new style
			loadNewAnnotationXML(e, trackId, sectionId);
			return;
		}

		// here is old format of annotation xml parser
		float x, y;
		String local, urn;

		x = Float.valueOf(e.getAttribute("x"));
		y = Float.valueOf(e.getAttribute("y"));
		if (version == 0.5) {
			x = x * SceneGraph.getCanvasDPIX(0) / src_canvas_dpix;
			y = y * SceneGraph.getCanvasDPIY(0) / src_canvas_dpiy;
		} else {
			// ver 1.0 has position in meter, so convert it to pixel here
			x = x * 100.0f * src_canvas_dpix / 2.54f;
			y = y * 100.0f * src_canvas_dpiy / 2.54f;
		}

		local = e.getAttribute("local");
		urn = e.getAttribute("urn");

		// Replace annotation file path prefix if a project_dir exists
		if ((proj_dir_path != null) && !proj_dir_path.equals("")) {
			local = replacePathPrefix(local, anno_dir_path);
			try {
				urn = new File(local).toURI().toURL().toString();
			} catch (MalformedURLException e1) {
				urn = "file:///" + local;
				e1.printStackTrace();
			}
		}

		// Test whether local file exists, also try from path relative to
		// the session file
		File defaultAnnoFile = new File(local);
		File stateFile = new File(stateFilename);
		File possibleFile = new File(stateFile.getParent() + sp + local);

		if (!defaultAnnoFile.exists()) {
			if (!possibleFile.exists()) {
				// Just use URN
				// TODO put some default dummy local html page
				// local = "resources/index.html";

				// Both possiblities failed, signal user
				/*
				 * JOptionPane.showMessageDialog(app.getMainFrame(),
				 * "Cannot find the annotation file"); return;
				 */
			} else {
				local = possibleFile.getAbsolutePath();
				try {
					urn = possibleFile.toURI().toURL().toString();
				} catch (MalformedURLException e1) {
					urn = "file:///" + local;
					e1.printStackTrace();
				}
			}
		}

		float trackOffset = SceneGraph.getTrackXPos(trackId);
		float sectionOffset = SceneGraph.getSectionXPos(trackId, sectionId);
		x = x + trackOffset + sectionOffset;

		int markerId = SceneGraph.createCoreSectionMarker(trackId, sectionId, ChatGroup.UNDEFINED, MarkerType.CORE_POINT_MARKER, x, y);

		SceneGraph.setCoreSectionMarkerLocal(trackId, sectionId, markerId, local);
		SceneGraph.setCoreSectionMarkerURL(trackId, sectionId, markerId, urn);

	}

	// -------------------------- LOAD IMAGES ---------------------
	/**
	 * Load core section image information with root XML element
	 * 
	 * @param e
	 *            Root XML element
	 * @param trackId
	 *            The track ID that this core section image associated with
	 */
	int loadCoreImageXML(final Element e, final TrackSceneNode destTrack) {
		nodecount++;
		pdlg.setValue(nodecount);
		final String progressStat = " (" + Integer.toString(nodecount) + "/" + Integer.toString(totallength) + ")"; 

		int sectionId;

		String local = e.getAttribute("local");
		String urn = e.getAttribute("urn");
		String name = e.getAttribute("name");

		pdlg.setString("Loading Image");

		if (local.equals("")) {
			if (urn.equals("")) {
				Object[] options = { "Abort", "Ignore" };

				String message = "Cannot find image, both 'local' and " + "'urn' path properties are empty";
				String title = "Empty Image Source Parameters";

				// noinspection UnnecessaryLocalVariable
				int result = JOptionPane.showOptionDialog(app.getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						options, options[1]);
				return result;
			} else {
				URL u = null;

				try {
					u = new URL(urn);
				} catch (MalformedURLException ex) {
					String message = "Cannot find image, malformed URL";
					String title = "Invalid URL";

					Object[] options = { "Abort", "Ignore" };

					// noinspection UnnecessaryLocalVariable
					int result = JOptionPane.showOptionDialog(app.getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
							null, options, options[1]);
					return result;
				}

				// protocols?
				if (u != null) {
					String thewholething = u.getFile();
					String[] all = thewholething.split("/");

					// what if it's just a url with no filename?
					local = app.preferences().download_Directory + sp + all[all.length - 1];
				} else { // suppressed dead code warning - u can be null
					String message = "Cannot find image, URL is empty";
					String title = "Empty URL";

					Object[] options = { "Abort", "Ignore" };

					// noinspection UnnecessaryLocalVariable
					int result = JOptionPane.showOptionDialog(app.getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
							null, options, options[1]);
					return result;
				}

				pdlg.setString("Downloading " + local);

				// If already exist, skip downloading
				File f = new File(local);
				boolean isDownloaded = f.exists();

				if (!isDownloaded) {
					try {
						isDownloaded = URLRetrieval.retrieveLocalCopy(urn, local);
					} catch (IOException e1) {
						isDownloaded = false;
					}
				}

				if (isDownloaded) {
					pdlg.setString("Processing " + local + progressStat);

					sectionId = app.loadImage(new File(local), urn, name, destTrack);
				} else {
					String message = "Cannot download image from: \n" + urn;
					String title = "Download Error";

					Object[] options = { "Abort", "Ignore" };

					// noinspection UnnecessaryLocalVariable
					int result = JOptionPane.showOptionDialog(app.getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
							null, options, options[1]);
					return result;
				}
			}
		} else {

			// Replace image file path prefix if a project_dir exists
			if ((proj_dir_path != null) && !proj_dir_path.equals("")) {
				local = replacePathPrefix(local, img_dir_path);
			}

			File fptr = new File(local);

			// 1st try with default local info
			if (fptr.exists()) {
				pdlg.setString("Processing " + fptr.getName() + progressStat);

				try {
					URL u = new URL(urn);

					if (u.getProtocol().contains("file")) {
						urn = fptr.toURI().toURL().toString();
					}
				} catch (MalformedURLException e1) {
					System.err.println("---> [Exception] " + e1);

					if (urn.toLowerCase().startsWith("file:")) {
						try {
							urn = fptr.toURI().toURL().toString();
						} catch (MalformedURLException e2) {
							urn = "file:////" + fptr.getAbsolutePath();
						}
					}

					e1.printStackTrace();
				}

				sectionId = app.loadImage(fptr, urn, name, destTrack);
			} else {
				// 2nd try with the path relative to where the session is
				File stateFile = new File(stateFilename);
				File possibleFile = new File(stateFile.getParent() + sp + local);

				if (possibleFile.exists()) {
					fptr = possibleFile;

					try {
						URL u = new URL(urn);

						if (u.getProtocol().contains("file")) {
							urn = fptr.toURI().toURL().toString();
						}
					} catch (MalformedURLException e1) {
						System.err.println("---> [Exception] " + e1);

						if (urn.startsWith("file:") || urn.startsWith("FILE:")) {
							urn = "file:////" + fptr.getAbsolutePath();
						}

						e1.printStackTrace();
					}

					sectionId = app.loadImage(fptr, urn, name, destTrack);
				} else {
					pdlg.setString("Downloading " + local);

					// Tried local & 1-level search in state file dir
					// file still does not exist, check urn
					if (urn.equals("") || urn.equals("unknown") || urn.startsWith("file:")) {
						// we have to find file in local drive
						// need to show up user browser window to specify dir
						// before show up option panel, check user specified dir
						// so that user need to choose only once if all data is
						// that dir
						String filename1 = datadirectory + sp + fptr.getName();
						fptr = new File(filename1);

						if (!fptr.exists()) {
							// Abort option to drop all image loading after this
							Object[] options = { "Abort", "Ignore", "Select..." };

							String message = "Could not find image file: \n" + fptr.getName() + "\n" + "Do you want to specify file location?";
							String title = "Image file not found";

							// noinspection UnnecessaryLocalVariable
							int result = JOptionPane.showOptionDialog(app.getMainFrame(), message, title, JOptionPane.YES_NO_OPTION,
									JOptionPane.QUESTION_MESSAGE, null, options, options[2]);

							if (result == 2) { // select...
								// show file browser
								File sessionFileParent = new File(stateFilename).getParentFile();
								final int retval = SwingSafeDirectoryChooser.chooseFile(sessionFileParent, app.getMainFrame(), "Select directory for image files");
								if (retval == JFileChooser.APPROVE_OPTION) {
									File f = SwingSafeDirectoryChooser.selectedDir;										
									datadirectory = f.getAbsolutePath();
									local = datadirectory + sp + fptr.getName();
									fptr = new File(local);

									if (!fptr.exists()) {
										return IGNORE;
									}
								} else if (retval == JFileChooser.CANCEL_OPTION) {
									return IGNORE;
								}
							} else {
								return result;
							}
						}

						try {
							URL u = new URL(urn);

							if (u.getProtocol().contains("file")) {
								urn = fptr.toURI().toURL().toString();
							}
						} catch (MalformedURLException e1) {
							System.err.println("---> [Exception] " + e1);

							if (urn.startsWith("file:") || urn.startsWith("FILE:")) {
								urn = "file:////" + fptr.getAbsolutePath();
							}

							e1.printStackTrace();
						}

						sectionId = app.loadImage(fptr, urn, name, destTrack);
					} else {
						// Prepare local place holder
						// String filename = (new File(local)).getName();
						// Because:
						// File.getName() returns full Windows path in UNIX
						String preFilename = new File(local).getName();

						String filename;
						if (preFilename.contains("\\")) {
							String[] toks = preFilename.split("\\\\");
							filename = toks[toks.length - 1];
						} else {
							filename = preFilename;
						}

						local = app.preferences().download_Directory + sp + filename;

						File cacheFile = new File(local);

						// if already have it, just load
						if (cacheFile.exists()) {
							pdlg.setString("Processing " + cacheFile.getName() + progressStat);

							sectionId = app.loadImage(cacheFile, urn, name, destTrack);
						} else { // if not, get it from URL
							boolean isDownloaded;

							try {
								isDownloaded = URLRetrieval.retrieveLocalCopy(urn, local);
							} catch (IOException e1) {
								System.err.println(e);
								isDownloaded = false;
							}

							if (isDownloaded) {
								pdlg.setString("Processing " + cacheFile.getName() + progressStat);

								sectionId = app.loadImage(cacheFile, urn, name, destTrack);
							} else {
								System.err.println("--- Download CoreImage from " + urn + "failed");

								JOptionPane.showMessageDialog(app.getMainFrame(), "Fail to download image from URL");
								return IGNORE;
							}
						}
					}
				}
			}
		}

		float x, y;
		float dpi_x, dpi_y;

		float rotate;
		String orientStr;
		boolean orientation;

		float intervalTop;
		float intervalBottom;

		try {
			intervalTop = Float.valueOf(e.getAttribute("intervalTop"));
		} catch (Exception ex) {
			intervalTop = 0.0f;
		}

		try {
			intervalBottom = Float.valueOf(e.getAttribute("intervalBottom"));
		} catch (Exception ex) {
			intervalBottom = 500.0f;
		}

		try {
			rotate = Float.valueOf(e.getAttribute("rot"));
		} catch (Exception ex) {
			rotate = 0.0f;
		}

		// since version 1.5, each section should have the orientation property
		orientStr = e.getAttribute("orientation");

		if ((orientStr == null) || orientStr.equals("")) {
			if (rotate == 90.0f) {
				orientation = SceneGraph.PORTRAIT;
			} else {
				orientation = SceneGraph.LANDSCAPE;
			}

			rotate = 0.0f;
		} else {
			if (orientStr.equalsIgnoreCase("portrait")) {
				orientation = SceneGraph.PORTRAIT;
			} else {
				orientation = SceneGraph.LANDSCAPE; // default
			}
		}

		// Get dpi values if 'width' and 'height' properties available
		// - dpi = imageDimension/(size * 100 / 2.54)
		// - "dpi" property has higher presedence. If it's available and
		// non-negative, use it.

		try {
			dpi_x = Float.valueOf(e.getAttribute("dpi_x"));
			dpi_y = Float.valueOf(e.getAttribute("dpi_y"));
		} catch (Exception ex) {
			float dpi;
			try {
				dpi = Float.valueOf(e.getAttribute("dpi"));
			} catch (Exception ex1) {
				dpi = -1.0f;
			}
			dpi_x = dpi_y = dpi;
		}

		final int trackId = destTrack.getId();
		
		// Empty or bad DPIs
		if (dpi_x <= 0.0f) {
			try {
				float length = Float.valueOf(e.getAttribute("length"));

				int imageId = SceneGraph.getImageIdForSection(trackId, sectionId);
				float imageWidth = SceneGraph.getImageWidth(imageId);
				float imageHeight = SceneGraph.getImageHeight(imageId);

				float lengthInPixel;
				if (orientation == SceneGraph.LANDSCAPE) {
					lengthInPixel = imageWidth;
				} else {
					lengthInPixel = imageHeight;
				}

				dpi_x = (float) (lengthInPixel / (length * 100 / 2.54));
			} catch (NumberFormatException ex) {
				dpi_x = StateLoader.DEFAULT_DPI;
			}
		}

		if (dpi_y <= 0.0f) {
			try {
				float width = Float.valueOf(e.getAttribute("width"));

				int imageId = SceneGraph.getImageIdForSection(trackId, sectionId);
				float imageWidth = SceneGraph.getImageWidth(imageId);
				float imageHeight = SceneGraph.getImageHeight(imageId);

				float widthInPixel;
				if (orientation == SceneGraph.LANDSCAPE) {
					widthInPixel = imageHeight;
				} else {
					widthInPixel = imageWidth;
				}

				dpi_y = (float) (widthInPixel / (width * 100 / 2.54));
			} catch (NumberFormatException ex) {
				dpi_y = dpi_x;
			}
		}

		if (version == 0.5) {
			x = Float.valueOf(e.getAttribute("x"));
			y = Float.valueOf(e.getAttribute("y"));
			x = x * SceneGraph.getCanvasDPIX(0) / src_canvas_dpix;
			y = y * SceneGraph.getCanvasDPIY(0) / src_canvas_dpiy;
		} else {
			// ver 1.0 has position in meter, so convert it to pixel here
			x = Float.valueOf(e.getAttribute("depth"));
			x = x * 100.0f * src_canvas_dpix / 2.54f;
			y = 0.0f;
		}

		// source section
		String parentTrackName = e.getAttribute("parentTrack");
		String parentSectionName = e.getAttribute("parentSection");

		if ((parentTrackName != null) && !parentTrackName.equals("") && (parentSectionName != null) && !parentSectionName.equals("")) {
			// but when in concurrent...
			String currentSessionName = CoreGraph.getInstance().getCurrentSession().getName();
			int srcTrackId = SceneGraph.getTrackIDByName(currentSessionName, parentTrackName);
			int srcSectionId = SceneGraph.getSectionIDByName(srcTrackId, parentSectionName);

			SceneGraph.setSectionParentIds(trackId, sectionId, srcTrackId, srcSectionId);
		}

		// assign info to C-side scenegraph
		pdlg.setString("Adding to the Scene");

		SceneGraph.setSectionDPI(trackId, sectionId, dpi_x, dpi_y);
		SceneGraph.positionSection(trackId, sectionId, x, y);
		SceneGraph.setSectionOrientation(trackId, sectionId, orientation);
		SceneGraph.rotateSection(trackId, sectionId, rotate);
		SceneGraph.setSectionIntervalTop(trackId, sectionId, intervalTop);
		SceneGraph.setSectionIntervalBottom(trackId, sectionId, intervalBottom);

		// java side section information
		if (destTrack == null) { // suppressed dead code warning - destTrack could be null
			System.err.println("--->[WARN] CurrentTrack TrackSceneNode is null, return IGNORE");
			return IGNORE;
		}

		CoreSection cs = destTrack.getCoreSectionByGID(sectionId);

		if (cs != null) {
			cs.setDepth(SceneGraph.getSectionDepth(trackId, sectionId) / 100.0f);

			String disId = e.getAttribute("DISId");
			if ((disId == null) || disId.equals("")) {
				disId = "N/A";
			}

			cs.setDISId(disId);

			String MCDDepth = e.getAttribute("mcd_depth");
			if ((MCDDepth == null) || MCDDepth.equals("")) {
				MCDDepth = e.getAttribute("depth");
			}
			cs.setMCDDepth(Float.parseFloat(MCDDepth));

			// Used by DIS import/export
			String length = e.getAttribute("length");
			if ((length == null) || length.equals("")) {
				length = String.valueOf(SceneGraph.getSectionLength(trackId, sectionId));
			}
			cs.setLength(Float.parseFloat(length));
		} else {
			// assuming app.loadImage() is good. CoreSection cs should have been
			// created already
			System.err.println("---> [WARN] CoreSection is null! tnode name: " + destTrack.getName() + ", NativeSectionId: " + sectionId);
		}

		// go through section annotations
		NodeList list = e.getChildNodes();

		for (int i = 0; i < list.getLength(); i++) {

			if (!(list.item(i) instanceof Element)) {
				continue;
			}

			Element c = (Element) list.item(i);
			String tagname = c.getTagName();

			if (tagname.equals("visual")) {
				String type = c.getAttribute("type");

				if (version == 0.5) {
					// if (type.equals("annothread")) {
					if (type.toLowerCase().equals("annotation")) {
						loadAnnotationXML(c, trackId, sectionId);

					} else if (type.toLowerCase().equals("graph")) {
						this.loadGraphXML(c, trackId, sectionId);
					}
				} else if (version > 0.5) {
					// ver 1.0: graph is in the same level of section image
					// so, don't need to process here
					if (type.toLowerCase().equals("annotation")) {
						loadAnnotationXML(c, trackId, sectionId);
					}
				}
			}
		}

		if (app != null) {
			app.updateGLWindows();
		}

		return OK;
	}

	// ---------- LOAD DATA ----------
	/**
	 * Load data information wit root XML element
	 * 
	 * @param e
	 *            Root XML element
	 */
	int loadDataSetXML(final Element e) {
		Session session = cg.getCurrentSession();
		if (session == null) {
			session = new Session("Default");
		}

		Vector<WellLogDataSet> datasets = session.getDatasets();
		if (datasets == null) { // datarepo == null) {
			JOptionPane.showMessageDialog(app.getMainFrame(), "No available dataset repository");
			return ABORT;
		}

		String filename = e.getAttribute("local");
		String urn = e.getAttribute("urn");

		// Replace dataset file path prefix if a project_dir exists
		if ((proj_dir_path != null) && !proj_dir_path.equals("")) {
			filename = replacePathPrefix(filename, data_dir_path);
		}

		File fptr = new File(filename);
		if (!fptr.exists()) {

			// try relative to session file dir
			if (!filename.equals("")) {
				File stateFile = new File(stateFilename);
				File possibleFile = new File(stateFile.getParent() + sp + filename);

				if (possibleFile.exists()) {
					filename = possibleFile.getAbsolutePath();
					fptr = possibleFile;
				}
			}

			// get it from the URN
			if (urn.equals("") || urn.equals("unknown") || urn.startsWith("file:")) {

				// we have to find file in local drive
				// need to show up user browser window to specify dataset
				// before show up option panel, check user specified dir
				// so that user need to choose only once if all data is that dir
				String filename1 = datadirectory + sp + fptr.getName();
				fptr = new File(filename1);
				if (!fptr.exists()) {
					Object[] options = { "Abort", "Ignore", "Select..." };
					String message = "Could not find dataset file: \n'" + fptr.getName() + "'\n" + "Do you want to specify file location?";

					String title = "Dataset file not found";
					int result = JOptionPane.showOptionDialog(app.getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
							null, options, options[2]);

					if (result == 2) { // TODO more about returns
						// show file browser
						final File sessionFileParent = new File(stateFilename).getParentFile();
						final int retval = SwingSafeDirectoryChooser.chooseFile(sessionFileParent, app.getMainFrame(), "Select directory for data files");
						if (retval == JFileChooser.APPROVE_OPTION) {
							File f = SwingSafeDirectoryChooser.selectedDir;			
							datadirectory = f.getAbsolutePath();
							filename = datadirectory + sp + fptr.getName();
							fptr = new File(filename);

							if (!fptr.exists()) {
								return IGNORE;
							}
						} else if (retval == JFileChooser.CANCEL_OPTION) {
							return IGNORE;
						}
					} else {
						return result;
					}
				}
			} else { // we have urn
				if (!filename.equals("")) {
					int lastslash = filename.lastIndexOf('.');

					while ((lastslash > 0) && (filename.charAt(lastslash) != '/') && (filename.charAt(lastslash) != '\\')) {
						lastslash--;
					}

					if (lastslash == 0) {
						System.err.println("---> [IGNORE] Dataset URL has not slash");

						return IGNORE;
					}

					filename = app.preferences().download_Directory + sp + filename.substring(lastslash + 1);
				} else {
					String filenameFromURL;

					try {
						filenameFromURL = new URL(urn).getFile();
						int lastslash = filenameFromURL.lastIndexOf(".");

						while ((lastslash > 0) && (filenameFromURL.charAt(lastslash) != '/') && (filenameFromURL.charAt(lastslash) != '\\')) {
							lastslash--;
						}

						if (lastslash == 0) {
							System.err.println("---> [IGNORE] Dataset URL has not slash");

							return IGNORE;
						}

						filenameFromURL = filenameFromURL.substring(lastslash + 1);

					} catch (MalformedURLException ex) {
						filenameFromURL = "someDatafile.xml";
					}

					filename = app.preferences().download_Directory + sp + filenameFromURL;
				}

				boolean isDownloaded;
				try {
					isDownloaded = URLRetrieval.retrieveLocalCopy(urn, filename);
				} catch (IOException e1) {
					System.err.println("---> [Exception] " + e1);
					e1.printStackTrace();

					isDownloaded = false;
				}

				if (!isDownloaded) {
					JOptionPane.showMessageDialog(app.getMainFrame(), "Error: Cannot download dataset " + urn);

					return IGNORE;
				}

				fptr = new File(filename);
			}
		}

		// check if dataset already exists or not
		if (app.containsDatasetName(fptr.getName())) {
			// show some messages
			JOptionPane.showMessageDialog(app.getMainFrame(), "Dataset '" + fptr.getName() + "' already exists!\nCorelyzer will skip this file!");

			return IGNORE;
		}

		String fileExtension = FileUtility.getFileExtension(filename);

		if (fileExtension.equals("xml")) {
		} else {
			return IGNORE;
		}

		pdlg.setString("Processing: " + filename);

		// call corelyzerapp#loadData
		int dsId = app.loadData(fptr);
		if (dsId != -1) {
			SceneGraph.setDatasetURL(dsId, urn);
		}

		return OK;
	}

	// ----------------------- LOAD GRAPH -------------------------------------
	/**
	 * Load graph information with root XML element and track. This function
	 * only called in cml ver 1.0 case
	 * 
	 * @param e
	 *            Root XML element
	 * @param trackId
	 *            Track ID the graph associated with
	 */
	void loadGraphXML(final Element e, final int trackId) {
		int datasetIndex = -1;
		int tableIndex = -1;
		int field = -1;
		float depth = 0.0f; // new addition in ver 1.0
		String value;

		Session session = cg.getCurrentSession();
		if (session == null) {
			session = new Session("Default");
		}

		Vector<WellLogDataSet> datasets = session.getDatasets();
		WellLogDataSet dataset = null;
		WellLogTable table = null;

		// find dataset
		String datasetName = e.getAttribute("dataset");
		datasetName = datasetName.toLowerCase();

		pdlg.setString("Creating graph from dataset: " + datasetName);

		for (int i = 0; (dataset == null) && (i < datasets.size()); i++) {
			WellLogDataSet tempdataset = datasets.elementAt(i);
			String sourceFilename = tempdataset.getSourceFilename().toLowerCase();

			if (sourceFilename.contains(datasetName)) {
				dataset = tempdataset;
				datasetIndex = i;
			}
		}

		if (dataset == null) {
			System.err.println("FIXME! cannot find dataset: " + datasetName);
			return;
		}

		// find table
		String tableName = e.getAttribute("table");
		String tablename = tableName.toLowerCase();

		for (int i = 0; (table == null) && (i < dataset.getNumTables()); i++) {
			WellLogTable temptable = dataset.getTable(i);
			String tempName = temptable.getName().toLowerCase();
			if (tablename.equals(tempName)) {
				table = temptable;
				tableIndex = i;
			}
		}

		if (table == null) {
			return;
		}

		// find field
		String fieldName = e.getAttribute("field");
		fieldName = fieldName.toLowerCase();

		for (int i = 0; (field < 0) && (i < table.getNumColumns()); i++) {
			if (fieldName.equals(table.getHeader(i).toLowerCase())) {
				field = i;
			}
		}

		if (field < 0) {
			return;
		}

		// find depth (meter to cm)
		value = e.getAttribute("depth");
		if (!value.equals("")) {
			depth = Float.valueOf(value) * 100.0f;
		}

		int trackIndex = -1;
		Vector<TrackSceneNode> tracks = cg.getCurrentSession().getTrackSceneNodes();
		for (TrackSceneNode t : tracks) {
			if (t.getId() == trackId) {
				trackIndex = tracks.indexOf(t);
			}
		}

		if (trackIndex == -1) {
			return;
		}

		CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
		TrackSceneNode tnode = (TrackSceneNode) tmodel.elementAt(trackIndex);
		CoreSection sec = tnode.getCoreSection(tableName);
		boolean newSecCreated = false;

		// create required section
		if (sec == null) {
			// need to creat new section
			// native first, java next for gid

			int secid = SceneGraph.addSectionToTrack(tnode.getId(), tnode.getNumCores());

			SceneGraph.setSectionName(tnode.getId(), secid, tableName);

			// MORE property?
			sec = new CoreSection(tableName, secid);
			tnode.addCoreSection(sec);
			newSecCreated = true;

			CoreGraph.getInstance().notifyListeners();
		}

		// now we have the section
		// native first, java next for gid
		/*
		 * System.err.println("---> [DEBUG] Create graph with trackId: " +
		 * trackId + ", sectionId: " + sec.getId() + ", datasetId: " +
		 * dataset.getId() + ", tableIndex: " + tableIndex + ", field: " +
		 * (field-1));
		 */

		int gid = SceneGraph.addLineGraphToSection(trackId, sec.getId(), dataset.getId(), tableIndex, field - 1);

		SceneGraph.setLineGraphLabel(gid, fieldName);

		// if we created new coresection and add new graph
		// send section to the end of the track
		if (newSecCreated) {
			// convert depth unit meter to pixel
			depth = depth / 2.54f * SceneGraph.getCanvasDPIX(0);
			SceneGraph.positionSection(tnode.getId(), sec.getId(), depth, 0.0f);
			sec.setDepth(SceneGraph.getSectionDepth(tnode.getId(), sec.getId()));
		} else {
			// we add this graph to existing section
			// adjust offset here
			float currdepth = SceneGraph.getSectionDepth(tnode.getId(), sec.getId());
			if (depth != currdepth) {
				float offset = depth - currdepth;
				SceneGraph.setSectionGraphOffset(tnode.getId(), sec.getId(), offset);
			}
		}

		// graph min/max
		float min = 0.0f, max = 0.0f;
		String minValue = e.getAttribute("min");
		if (!minValue.equals("")) {
			min = Float.valueOf(minValue);
		}
		String maxValue = e.getAttribute("max");
		if (!maxValue.equals("")) {
			max = Float.valueOf(maxValue);
		}
		
		SceneGraph.setLineGraphRange(gid, min, max);
		
		// graph exclude range min/max
		// brgtodo: this pattern of value extraction is very common, extract method?
		float excludeMin = -Float.MAX_VALUE, excludeMax = Float.MAX_VALUE;
		String excludeMinValue = e.getAttribute("exclude_min");
		if (!excludeMinValue.equals("")) {
			excludeMin = Float.valueOf(excludeMinValue);
		}
		String excludeMaxValue = e.getAttribute("exclude_max");
		if (!excludeMaxValue.equals("")) {
			excludeMax = Float.valueOf(excludeMaxValue);
		}		
		SceneGraph.setLineGraphExcludeRange( gid, excludeMin, excludeMax );

		int excludeGraphStyle = 0;
		String excludeGraphStyleValue = e.getAttribute("exclude_style");
		if (!excludeGraphStyleValue.equals("")) {
			excludeGraphStyle = Integer.valueOf(excludeGraphStyleValue);
		}
		SceneGraph.setLineGraphExcludeStyle(gid, excludeGraphStyle);		
		
		// color
		float red = 0.0f;
		float green = 0.0f;
		float blue = 0.0f;

		value = e.getAttribute("r");
		if (!value.equals("")) {
			red = Float.valueOf(value);
		}

		value = e.getAttribute("g");
		if (!value.equals("")) {
			green = Float.valueOf(value);
		}

		value = e.getAttribute("b");
		if (!value.equals("")) {
			blue = Float.valueOf(value);
		}

		SceneGraph.setLineGraphColor(gid, red, green, blue);

		// graph type (style): line, point, crosspoint
		int style = 0; // default line graph
		value = e.getAttribute("style");
		if (!value.equals("")) {
			style = Integer.valueOf(value);
		}
		SceneGraph.setLineGraphType(gid, style);

		// Add graph to sectionList
		CoreSectionGraph csg = new CoreSectionGraph(datasetIndex, tableIndex, field - 1, gid, tnode);
		csg.setName(tableName);
		tnode.addCoreSectionGraph(csg, sec.getId(), gid);

		// go through section annotations
		int sectionId = sec.getId();

		NodeList list = e.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (!(list.item(i) instanceof Element)) {
				continue;
			}

			Element c = (Element) list.item(i);
			String tagname = c.getTagName();

			if (tagname.equals("visual")) {
				String type = c.getAttribute("type");

				if (version == 0.5) {
					// if (type.equals("annothread")) {
					if (type.toLowerCase().equals("annotation")) {
						loadAnnotationXML(c, trackId, sectionId);

					} else if (type.toLowerCase().equals("graph")) {
						this.loadGraphXML(c, trackId, sectionId);
					}
				} else if (version > 0.5) {
					// ver 1.0: graph is in the same level of section image
					// so, don't need to process here
					if (type.toLowerCase().equals("annotation")) {
						loadAnnotationXML(c, trackId, sectionId);
					}
				}
			}
		}

		if (app != null) {
			app.updateGLWindows();
		}
	}

	// ----------------------- LOAD GRAPH -------------------------------------
	/**
	 * Load graph information with root XML element, track and section IDs
	 * 
	 * @param e
	 *            Root XML element
	 * @param trackId
	 *            Track ID the graph associated with (native scene)
	 * @param sectionId
	 *            Core section ID the graph associated with (native scene)
	 */
	void loadGraphXML(final Element e, final int trackId, final int sectionId) {
		int datasetIndex = -1;
		int tableIndex = -1;
		int field = -1;

		Session session = cg.getCurrentSession();
		if (session == null) {
			session = new Session("Default");
		}

		Vector<WellLogDataSet> datasets = session.getDatasets();
		WellLogDataSet dataset = null;
		WellLogTable table = null;

		// find dataset
		String datasetName = e.getAttribute("dataset");
		datasetName = datasetName.toLowerCase();

		pdlg.setString("Creating graph from dataset: " + datasetName);

		for (int i = 0; (dataset == null) && (i < datasets.size()); /*datarepo.length();*/ i++) {
			WellLogDataSet tempdataset = datasets.elementAt(i); // datarepo.getDataSet(i);
			String sourceFilename = tempdataset.getSourceFilename().toLowerCase();

			if (sourceFilename.contains(datasetName)) {
				dataset = tempdataset;
				datasetIndex = i;
			}
		}

		if (dataset == null) {
			return;
		}

		// find table
		String tableName = e.getAttribute("table");
		String tablename = tableName.toLowerCase();

		for (int i = 0; (table == null) && (i < dataset.getNumTables()); i++) {
			WellLogTable temptable = dataset.getTable(i);
			String tempName = temptable.getName().toLowerCase();
			if (tablename.equals(tempName)) {
				table = temptable;
				tableIndex = i;
			}
		}

		if (table == null) {
			return;
		}

		// find field
		String fieldName = e.getAttribute("field");
		fieldName = fieldName.toLowerCase();

		for (int i = 0; (field < 0) && (i < table.getNumColumns()); i++) {
			if (fieldName.equals(table.getHeader(i).toLowerCase())) {
				field = i;
			}
		}

		if (field < 0) {
			System.out.println("---> Return, field: " + field);
			return;
		}

		int gid = SceneGraph.addLineGraphToSection(trackId, sectionId, datasetIndex, tableIndex, field - 1);

		SceneGraph.setLineGraphLabel(gid, fieldName);

		// graph min/max
		float min = 0.0f, max = 0.0f;
		String minValue = e.getAttribute("min");
		if (!minValue.equals("")) {
			min = Float.valueOf(minValue);
		}
		String maxValue = e.getAttribute("max");
		if (!maxValue.equals("")) {
			max = Float.valueOf(maxValue);
		}

		SceneGraph.setLineGraphRange( gid, min, max );
		
		// graph exclude range min/max
		// brgtodo: this pattern of value extraction is very common, extract method?
		float excludeMin = -Float.MAX_VALUE, excludeMax = Float.MAX_VALUE;
		String excludeMinValue = e.getAttribute("exclude_min");
		if (!excludeMinValue.equals("")) {
			excludeMin = Float.valueOf(excludeMinValue);
		}
		String excludeMaxValue = e.getAttribute("exclude_max");
		if (!excludeMaxValue.equals("")) {
			excludeMax = Float.valueOf(excludeMaxValue);
		}
		SceneGraph.setLineGraphExcludeRange( gid, excludeMin, excludeMax );
		
		int excludeGraphStyle = 0;
		String excludeGraphStyleValue = e.getAttribute("exclude_style");
		if (!excludeGraphStyleValue.equals("")) {
			excludeGraphStyle = Integer.valueOf(excludeGraphStyleValue);
		}
		SceneGraph.setLineGraphExcludeStyle(gid, excludeGraphStyle);
		
		// color
		float red = 0.0f;
		float green = 0.0f;
		float blue = 0.0f;
		String value;

		value = e.getAttribute("r");
		if (!value.equals("")) {
			red = Float.valueOf(value);
		}

		value = e.getAttribute("g");
		if (!value.equals("")) {
			green = Float.valueOf(value);
		}

		value = e.getAttribute("b");
		if (!value.equals("")) {
			blue = Float.valueOf(value);
		}

		SceneGraph.setLineGraphColor(gid, red, green, blue);

		// new addition. graph type (style): line, point, crosspoint
		int style = 0; // default line graph
		value = e.getAttribute("style");
		if (!value.equals("")) {
			style = Integer.valueOf(value);
		}

		SceneGraph.setLineGraphType(gid, style);

		// Add graph to sectionList
		// Notice: trackId is in native scenegraph
		TrackSceneNode tnode = CoreGraph.getInstance().getCurrentTrack();
		CoreSection sec = tnode.getCoreSection(tableName);

		if (sec != null) {
			CoreSectionGraph csg = new CoreSectionGraph(datasetIndex, tableIndex, field - 1, gid, tnode);
			csg.setName(tableName);
			tnode.addCoreSectionGraph(csg, sec.getId(), gid);
		}
	}

	/**
	 * Load annotation information with root XML element
	 * 
	 * @param e
	 *            Root XML element
	 * @param trackId
	 *            Track ID the annotation associated with
	 * @param sectionId
	 *            Core section ID the annotation associated with
	 */
	void loadNewAnnotationXML(final Element e, final int trackId, final int sectionId) {
		float x, y, ax, ay, v0, v1, v2, v3;
		String local, urn, groupName, markerName, label;

		// all coordinate relative to coresection base point
		x = Float.valueOf(e.getAttribute("x")); // marker depthX
		y = Float.valueOf(e.getAttribute("y")); // marker depthY
		ax = Float.valueOf(e.getAttribute("ax")); // icon x
		ay = Float.valueOf(e.getAttribute("ay")); // icon y
		v0 = Float.valueOf(e.getAttribute("v0")); // marker pt v0
		v1 = Float.valueOf(e.getAttribute("v1")); // marker pt v1
		v2 = Float.valueOf(e.getAttribute("v2")); // marker pt v2
		v3 = Float.valueOf(e.getAttribute("v3")); // marker pt v3
		if (version == 0.5) {
			x = x * SceneGraph.getCanvasDPIX(0) / src_canvas_dpix;
			y = y * SceneGraph.getCanvasDPIY(0) / src_canvas_dpiy;
			ax = ax * SceneGraph.getCanvasDPIX(0) / src_canvas_dpix;
			ay = ay * SceneGraph.getCanvasDPIY(0) / src_canvas_dpiy;
			v0 = v0 * SceneGraph.getCanvasDPIX(0) / src_canvas_dpix;
			v1 = v1 * SceneGraph.getCanvasDPIY(0) / src_canvas_dpiy;
			v2 = v2 * SceneGraph.getCanvasDPIX(0) / src_canvas_dpix;
			v3 = v3 * SceneGraph.getCanvasDPIY(0) / src_canvas_dpiy;
		} else {
			// ver 1.0 has position in meter, so convert it to pixel here
			x = x * 100.0f * src_canvas_dpix / 2.54f;
			y = y * 100.0f * src_canvas_dpiy / 2.54f;
			ax = ax * 100.0f * src_canvas_dpix / 2.54f;
			ay = ay * 100.0f * src_canvas_dpiy / 2.54f;
			v0 = v0 * 100.0f * src_canvas_dpix / 2.54f;
			v1 = v1 * 100.0f * src_canvas_dpiy / 2.54f;
			v2 = v2 * 100.0f * src_canvas_dpix / 2.54f;
			v3 = v3 * 100.0f * src_canvas_dpiy / 2.54f;
		}

		local = e.getAttribute("local");
		urn = e.getAttribute("urn");
		groupName = e.getAttribute("group"); // annotation group
		markerName = e.getAttribute("marker"); // annotation marker type
		label = e.getAttribute("label");

		// Replace annotation file path prefix if a project_dir exists
		if ((proj_dir_path != null) && !proj_dir_path.equals("")) {
			local = replacePathPrefix(local, anno_dir_path);

			try {
				urn = new File(local).toURI().toURL().toString();
			} catch (MalformedURLException e1) {
				urn = "file:///" + local;
				e1.printStackTrace();
			}
		}

		// adjust offset of track and section for depthX
		float trackOffset = SceneGraph.getTrackXPos(trackId);
		float sectionOffset = SceneGraph.getSectionXPos(trackId, sectionId);
		x = x + trackOffset + sectionOffset;

		trackOffset = SceneGraph.getTrackYPos(trackId);
		sectionOffset = SceneGraph.getSectionYPos(trackId, sectionId);
		y = y + trackOffset + sectionOffset;

		// Check for duplicates. If found, ignore this one.
		// brg 3/30/2021 Looks like annotation elements are added to the core image
		// and associated dataset elements in the CML file, so the below "ignore" message
		// comes up frequently when loading sessions with annotations and plotted data.
		final boolean isDuplicate = SceneGraph.isDuplicateAnnotation(trackId, sectionId, x, y, v0, v1, v2, v3, urn, local);
		if (isDuplicate) {
			System.out.println("-> Ignore this duplicate annotation: '" + urn + "'.");
			return;
		}

		int markerId = SceneGraph.createCoreSectionMarker(trackId, sectionId, ChatGroup.getGroupId(groupName), MarkerType.getMarkerId(markerName), x, y);

		SceneGraph.setCoreSectionMarkerLocal(trackId, sectionId, markerId, local);
		SceneGraph.setCoreSectionMarkerURL(trackId, sectionId, markerId, urn);

		// set group and vertex
		SceneGraph.setCoreSectionMarkerVertex(trackId, sectionId, markerId, ax, ay, v0, v1, v2, v3);

		if (label == null) {
			label = "";
		}
		SceneGraph.setCoreSectionMarkerText(trackId, sectionId, markerId, label);

		label = e.getAttribute("rLabel");
		if (label == null) {
			label = "";
		}
		SceneGraph.setCoreSectionMarkerRelationText(trackId, sectionId, markerId, label);
	}

	/**
	 * Load the session with input filename
	 * 
	 * @param filename
	 *            Input session file path
	 * @return Is the state file is written successfully
	 */
	public boolean loadState(final String filename) {
		return this.loadState(filename, "");
	}

	public boolean loadState(String filename, final String prefix) {
		// Do a XML check before really loading
		try {
			DOMParser parser = new DOMParser();
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);

			filename = new File(filename).toURI().toURL().toString();
			parser.parse(filename);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(app.getMainFrame(), "CML file format error.\n" + e);

			e.printStackTrace();

			pdlg.setString("Abort loading '" + filename + "'");
			return false;
		}

		setStateFilename(filename);
		final long startTimeMs = System.currentTimeMillis();

		// Use non-null prefix to overrisde where the files should be
		if (!prefix.equals("")) {
			proj_dir_path = prefix;
			img_dir_path = proj_dir_path + sp + "images";
			data_dir_path = proj_dir_path + sp + "datasets";
			anno_dir_path = proj_dir_path + sp + "annotations";
		}

		if (stateFilename != null) {
			System.out.println("---> Loading state file: " + stateFilename);
			pdlg.setString("Loading " + new File(stateFilename).getName());

			try {
				DOMParser parser = new DOMParser();
				parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);
				parser.parse(stateFilename);
				Document doc = parser.getDocument();

				// version check: new version is 1.0 (Jan 2007)
				// version 1.5: Jan 2008
				Element e = doc.getDocumentElement(); // should be scene
				String str = e.getAttribute("version");

				if (!e.getNodeName().trim().toLowerCase().equals("scene")) {
					JOptionPane.showMessageDialog(app.getMainFrame(), "CML file '" + stateFilename + "' format error.");

					return false;
				}

				if (!str.equals("")) {
					version = Float.valueOf(str);
				} else {
					version = 0.5f;
				}

				System.out.println("---> [INFO] CML Version: " + version);

				src_canvas_dpix = SceneGraph.getCanvasDPIX(0);
				src_canvas_dpiy = SceneGraph.getCanvasDPIY(0);

				if (version == 0.5) {
					str = e.getAttribute("source_canvas_dpi_x");
					if (str != null) {
						src_canvas_dpix = Float.valueOf(str);
					}

					str = e.getAttribute("source_canvas_dpi_y");
					if (str != null) {
						src_canvas_dpiy = Float.valueOf(str);
					}
				}

				if (version < 1.5) {
					// e -> holder of Track, should be scene in 1.0
					File f = new File(stateFilename);
					String sessionName = f.getName();

					if (sessionName.equals("session.cml")) {
						sessionName = new File(f.getParent()).getName();
					}

					Session s = new Session(sessionName);
					cg.addSession(s);

					loadStateXML(e, s);
				} else if (version >= 1.5) {
					// e -> holder of Track, should be session in 1.5
					NodeList list = e.getChildNodes();

					for (int i = 0; i < list.getLength(); i++) {
						if (!(list.item(i) instanceof Element)) {
							continue;
						}

						Element sessionElement = (Element) list.item(i);
						String tagName = sessionElement.getNodeName();

						if (!tagName.equalsIgnoreCase("session")) {
							continue;
						}

						String sessionName = sessionElement.getAttribute("name");
						Session s = new Session(sessionName);

						String disId = sessionElement.getAttribute("DISId");
						if ((disId == null) || disId.equals("")) {
							disId = "N/A";
						}
						s.setDISId(disId);

						cg.addSession(s);

						loadStateXML(sessionElement, s);
					}
				}
			} catch (Exception ex) {
				System.err.println(ex);
				ex.printStackTrace();
			}
		} else {
			System.err.println("[Error] State file is not specified in StateLoader.");
		}

		final float elapsedTime = (System.currentTimeMillis() - startTimeMs) / 1000.0f;
		pdlg.setString("Session loaded in " + Float.toString(elapsedTime) + " seconds");
		pdlg.setValue(0);

		return true;
	}

	// Load session state with incoming XML state root element
	void loadStateXML(final Element xmlRoot, final Session session) { // xmlRoot -> scene(1.0) or
												// session(1.5)
		totallength = recursiveTreeSize(xmlRoot);
		nodecount = 0;
		NodeList list = xmlRoot.getChildNodes();

		pdlg.setMaximum(totallength);
		pdlg.setValue(0);

		for (int i = 0; i < list.getLength(); i++) {
			if (!isAbort) {
				pdlg.setValue(nodecount);

				if (!(list.item(i) instanceof Element)) {
					continue;
				}

				Element e = (Element) list.item(i);
				String tagname = e.getTagName();

				if (tagname.equals("dataset")) {
					int result = loadDataSetXML(e);

					if (result == ABORT) {
						isAbort = true;
						break;
					}
				} else if (tagname.equals("visual")) {
					String type = e.getAttribute("type");

					if (type.equals("annothread")) {
						loadAnnotationXML(e, -1, -1);
					} else if (type.equals("track")) {
						loadTrackXML(e, session);
					} else if (type.equals("tie")) {
						loadTieXML(e, session);
					}
				} else {
					System.err.println("---> [IGNORE] Some tagname I don't know: " + tagname);
				}
			} else {
				// Maybe do some cleanup, rollbacks
			}
		}

		pdlg.setValue(totallength);
	}

	void loadTieXML(final Element e, final Session session) {
		SectionTiePoint ptA = new SectionTiePoint(e.getAttribute("atrack"), e.getAttribute("asection"), Float.valueOf(e.getAttribute("ax")), Float.valueOf(e.getAttribute("ay")), e.getAttribute("adesc"));
		SectionTiePoint ptB = new SectionTiePoint(e.getAttribute("btrack"), e.getAttribute("bsection"), Float.valueOf(e.getAttribute("bx")), Float.valueOf(e.getAttribute("by")), e.getAttribute("bdesc"));

		int a_track_id, a_section_id, b_track_id, b_section_id;
		a_track_id = SceneGraph.getTrackIDByName(session.getName(), ptA.track);
		a_section_id = SceneGraph.getSectionIDByName(a_track_id, ptA.section);
		b_track_id = SceneGraph.getTrackIDByName(session.getName(), ptB.track);
		b_section_id = SceneGraph.getSectionIDByName(b_track_id, ptB.section);

		final float ax = ((ptA.x * 100.0f) / 2.54f) * SceneGraph.getCanvasDPIX(0);
		final float ay = ((ptA.y * 100.0f) / 2.54f) * SceneGraph.getCanvasDPIY(0);
		final float bx = ((ptB.x * 100.0f) / 2.54f) * SceneGraph.getCanvasDPIX(0);
		final float by = ((ptB.y * 100.0f) / 2.54f) * SceneGraph.getCanvasDPIY(0);
		final boolean show = Boolean.valueOf(e.getAttribute("show"));

		SceneGraph.createSectionTie(ax, ay, e.getAttribute("adesc"), a_track_id, a_section_id, bx, by, e.getAttribute("bdesc"), b_track_id, b_section_id, show);
	}

	// Load track information with XML root element
	void loadTrackXML(final Element e, final Session session) {
		int trackId;

		String name = e.getAttribute("name");

		if (name != null) {
			// if track already exists then, skip all child of it
			if (app.containsTrackName(name)) {
				// To ask whether replace/rename/skip
				Object[] options = { "Replace", "Rename", "Skip" };

				int retval = JOptionPane.showOptionDialog(app.getMainFrame(), "Track '" + name + "' already exists!\n"
						+ "Do you want replace or rename or skip this track?", "Track name conflict", JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
				if (retval == JOptionPane.YES_OPTION) { // replace
					app.deleteTrack(name);
				} else if (retval == JOptionPane.NO_OPTION) { // rename
					String newname = JOptionPane.showInputDialog(app.getMainFrame(), "Type new track name for '" + name + "'", "Rename track",
							JOptionPane.PLAIN_MESSAGE);
					if (newname == null) {
						return;
					}

					name = newname;
				} else { // skip
					return;
				}
			}

			// this would be the merge case
			trackId = app.createTrack(name);
		} else {
			System.out.println("---> [WARN] Track has no name! Doing nothing");
			return;
		}

		float x = Float.valueOf(e.getAttribute("x"));
		float y = Float.valueOf(e.getAttribute("y"));

		TrackSceneNode track = session.getTrackSceneNodeWithTrackId(trackId);

		// DIS metadata
		String disId = e.getAttribute("DISId");
		if ((disId == null) || disId.equals("")) {
			disId = "N/A";
		}
		track.setDISId(disId);

		String MCDDepth = e.getAttribute("mcd_depth");
		if ((MCDDepth == null) || MCDDepth.equals("")) {
			track.setMCDDepth(-0.0f);
		} else {
			track.setMCDDepth(Float.parseFloat(MCDDepth));
		}

		String topDepth = e.getAttribute("top_depth");
		if ((topDepth == null) || topDepth.equals("")) {
			track.setTopDepth(-0.0f);
		} else {
			track.setTopDepth(Float.parseFloat(topDepth));
		}

		// Used by DIS import/export
		String length = e.getAttribute("length");
		if ((length == null) || length.equals("")) {
			length = "1.0";
		}
		track.setLength(Float.parseFloat(length));

		// go through annotations && images
		NodeList list = e.getChildNodes();

		for (int i = 0; i < list.getLength(); i++) {
			if (!(list.item(i) instanceof Element)) {
				continue;
			}

			Element c = (Element) list.item(i);
			String tagname = c.getTagName();

			if (tagname.equals("visual")) {
				String type = c.getAttribute("type");

				if (version <= 0.5) {
					if (type.equals("annothread")) {
						loadAnnotationXML(c, trackId, -1);
					} else if (type.equals("core_section")) {
						int result = loadCoreImageXML(c, track);
						if (result == ABORT) {
							isAbort = true;
							break;
						}
					}
				} else if (version > 0.5) {
					if (type.equals("annothread")) {
						loadAnnotationXML(c, trackId, -1);
					} else if (type.equals("core_section")) {
						int result = loadCoreImageXML(c, track);
						if (result == ABORT) {
							isAbort = true;
							break;
						}
					} else if (type.equals("graph")) {
						this.loadGraphXML(c, trackId);
					}
				}
			}
		}

		if (version == 0.5) {
			x = x * SceneGraph.getCanvasDPIX(0) / src_canvas_dpix;
			y = y * SceneGraph.getCanvasDPIY(0) / src_canvas_dpiy;
		} else {
			// ver 1.0 has position in meter, so convert it to pixel here
			x = x * 100.0f * src_canvas_dpix / 2.54f;
			y = y * 100.0f * src_canvas_dpiy / 2.54f;
		}

		// Use absolute track moving, 'coz now the track will auto-shift in
		// y-direction
		SceneGraph.moveTrackAbsX(trackId, x);
		SceneGraph.moveTrackAbsY(trackId, y);
	}

	// used to determine size of progress bar - only count core section images,
	// since they dominate session loading time
	int recursiveTreeSize(final Node n) {
		int total = 0;
		NodeList list = n.getChildNodes();

		for (int i = 0; i < list.getLength(); i++) {
			Node cn = list.item(i);
			if (cn.hasAttributes()) {
				NamedNodeMap attrMap = cn.getAttributes();
				for (int j = 0; j < attrMap.getLength(); j++) {
					Attr attr = (Attr)attrMap.item(j);
					if (attr.getName().equals("type") && attr.getValue().equals("core_section")) {
						total++;
						break;
					}
				}
			}
			total += recursiveTreeSize(cn);
		}

		return total;
	}

	private String replacePathPrefix(final String orig, final String prefix) {
		String[] toks = orig.split("\\\\|/");
		String filename = toks[toks.length - 1];

		return prefix + sp + filename;
	}

	/**
	 * Access method to set session file string
	 * 
	 * @param filename
	 *            Input session file path in String
	 */
	public void setStateFilename(final String filename) {
		stateFilename = filename;
	}
}
