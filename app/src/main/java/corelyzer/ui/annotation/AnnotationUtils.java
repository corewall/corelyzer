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
package corelyzer.ui.annotation;

import java.awt.Component;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JOptionPane;

import corelyzer.data.CRPreferences;
import corelyzer.data.ChatGroup;
import corelyzer.data.CoreSection;
import corelyzer.data.MarkerType;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;
import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.ui.AttachmentURLDialog;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.annotation.clast.ClastInfoDialog;
import corelyzer.ui.annotation.freeform.CRAnnotationWindow;
import corelyzer.ui.annotation.freeform.FreeformAnnotationTableModel;
import corelyzer.ui.annotation.propertyList.DefaultFormDialog;
import corelyzer.ui.annotation.sampling.SampleRequestDialog;
import corelyzer.util.CROptionPane;
import corelyzer.util.FileUtility;
import corelyzer.util.PropertyListUtility;

public class AnnotationUtils {
	static boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

	private static int FORMAT_OK = 0;

	private static int NUM_FORMAT_ERROR = 1;

	private static int NO_TRACK_OR_SECTION = 2;

	private static int CREATE_MARKER_FAIL = 3;

	private static String createAnAnnotationFile(final Vector<String> titles, final String[] toks, final int trackId, final int sectionId, final int markerId) {
		String local = "";

		if (toks.length == 10) {
			try {
				return new URL(toks[9]).getFile();
			} catch (MalformedURLException e) {
				System.err.println("The last token is not URL, use it as comments");
			}
		}

		// pull all tokens behide, to form a plain text annotation html file
		String preMesg = "<html>\n" + "  <head>\n" + "    \n" + "  </head>\n" + "  <body>\n" + "    <p style=\"margin-right: 10%; margin-left: 5%\">\n"
				+ "      \n" + "    </p>\n" + "    <table bgcolor=\"#ffffff\" width=\"100%\" border=\"0\">\n" + "      <tr>\n" + "        <td>";

		String postMesg = "        </td>\n" + "      </tr>\n" + "    </table>\n" + "    <hr>\n" + "  </body>\n" + "</html>";

		// who wrote this
		Date now = new Date(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyy hh:mm:ss a z");
		String time = format.format(now);

		String header = "<b>" + "On " + time + " " + System.getProperty("user.name") + " wrote: </b><br>";

		String mesg = preMesg + header;
		mesg += "\n\t<p style=\"margin-right: 10%; margin-left: 5%\">\n";

		Hashtable<String, String> aDict = new Hashtable<String, String>();
		for (int i = 0; i < toks.length; i++) {
			String title = "Unknown";

			if (i < titles.size()) {
				title = titles.elementAt(i);
			}

			aDict.put(title, toks[i]);
		}

		mesg += "\n" + PropertyListUtility.generateHTMLTableString(aDict, titles) + "\n";

		mesg += "\n\t</p>\n";
		mesg += postMesg;

		local = generateFilename(trackId, sectionId, markerId);

		// fill in local
		File f = new File(local);
		try {
			FileWriter fw = new FileWriter(f);
			fw.write(mesg, 0, mesg.length());
			fw.flush();
			fw.close();
		} catch (IOException e) {
			System.err.println("---> Write annotation file '" + local + "' failed");

			e.printStackTrace();
		}

		return local;
	}

	public static String generateFilename(final int trackId, final int sectionId, final int markerId) {
		// Get user and time info
		String user = System.getProperty("user.name");

		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyy hh:mm:ss a z");
		String now = formatter.format(today);
		// shorter string for annotation filename
		formatter = new SimpleDateFormat("MMddyyyyhhmmssz");
		now = formatter.format(today);

		// Save it into somewhere, with some unique naming
		// String cwd = System.getProperty("user.dir");
		CRPreferences prefs = CorelyzerApp.getApp().preferences();
		String sp = System.getProperty("file.separator");

		String anno_dir = prefs.annotation_Directory;
		String filename = "annotation_" + trackId + "_" + sectionId + "_" + markerId + "_" + user + "_" + now + ".html";

		return anno_dir + sp + filename;
	}

	public static float[] getAnnotationArea(final int trackId, final int sectionId, final int markerId) {

		float canvas_dpix = SceneGraph.getCanvasDPIX(0);

		float v0 = SceneGraph.getCoreSectionMarkerV0(trackId, sectionId, markerId) * 2.54f / canvas_dpix;

		float v1 = SceneGraph.getCoreSectionMarkerV1(trackId, sectionId, markerId) * 2.54f / canvas_dpix;

		float v2 = SceneGraph.getCoreSectionMarkerV2(trackId, sectionId, markerId) * 2.54f / canvas_dpix;

		float v3 = SceneGraph.getCoreSectionMarkerV3(trackId, sectionId, markerId) * 2.54f / canvas_dpix;

		// fvalue /= 100.0f;

		return new float[] { v0, v1, v2, v3 };
	}

	private static Vector<String> getTitles(String line) {
		Vector<String> ret = new Vector<String>();

		if (line.startsWith("#")) {
			line = line.substring(1);
		}

		String[] toks = line.split(",");
		for (String s : toks) {
			s = s.trim();
			ret.add(s);
		}

		return ret;
	}

	private static int handleCRFormat(final Vector<String> titles, final String[] toks) {
		try {
			String track = toks[0].trim();
			String section = toks[1].trim();
			String group = toks[2].trim();
			String text = toks[3].trim();
			String type = toks[4].trim();

			CoreGraph cg = CoreGraph.getInstance();
			TrackSceneNode t = cg.getCurrentSession().getTrackSceneNodeWithName(track);
			if (t == null) {
				return NO_TRACK_OR_SECTION;
			}

			CoreSection cs = t.getCoreSection(section);
			if (cs == null) {
				return NO_TRACK_OR_SECTION;
			}

			int trackId = t.getId();
			int sectionId = cs.getId();
			int groupId = ChatGroup.getGroupId(group);
			int typeId = MarkerType.getMarkerId(type);

			if ((trackId == -1) || (sectionId == -1)) {
				return NO_TRACK_OR_SECTION;
			}

			// position
			float canvas_dpix = SceneGraph.getCanvasDPIX(0);
			float canvas_dpiy = SceneGraph.getCanvasDPIY(0);

			// options to specify measurements
			String p0SpecTitle = titles.elementAt(5);
			String[] specToks = p0SpecTitle.split(":");

			// default use relative measure to the origin of the section
			// default scale to meter
			boolean isMbsf = false;

			if (specToks.length == 2) { // with options
				if (specToks[1].trim().equals("mbsf")) {
					isMbsf = true;
				}
			}

			float xpos; // need to convert to pixles for native scenegraph
			float ypos;
			float v0, v1, v2, v3;

			switch (MarkerType.getMarkerId(type)) {
				case MarkerType.CORE_SPAN_MARKER:
					v0 = Float.parseFloat(toks[5].trim()) * 100.0f * canvas_dpix / 2.54f;
					v1 = 0.0f;

					v2 = Float.parseFloat(toks[7].trim()) * 100.0f * canvas_dpiy / 2.54f;
					v3 = 0.0f;

					xpos = (v0 + v2) / 2.0f;
					ypos = 0.0f;
					break;

				case MarkerType.CORE_OUTLINE_MARKER:
					v0 = Float.parseFloat(toks[5].trim()) * 100.0f * canvas_dpix / 2.54f;
					v1 = Float.parseFloat(toks[6].trim()) * 100.0f * canvas_dpiy / 2.54f;
					v2 = Float.parseFloat(toks[7].trim()) * 100.0f * canvas_dpix / 2.54f;
					v3 = Float.parseFloat(toks[8].trim()) * 100.0f * canvas_dpiy / 2.54f;

					xpos = (v0 + v2) / 2.0f;
					ypos = (v1 + v3) / 2.0f;
					break;

				case MarkerType.CORE_POINT_MARKER:

				default:
					xpos = Float.parseFloat(toks[5].trim()) * 100.0f * canvas_dpix / 2.54f;

					if (toks[6].trim().equals("")) {
						ypos = 0.0f;
					} else {
						ypos = Float.parseFloat(toks[6].trim()) * 100.0f * canvas_dpiy / 2.54f;
					}

					v0 = 0.0f;
					v1 = 0.0f;
					v2 = 0.0f;
					v3 = 0.0f;
			}

			// determine whether need to consider track and section position
			// x-direction could be 'mbsf' or 'down section depth'
			// y-direction should always be 'cross section measurement'
			float trackX = SceneGraph.getTrackXPos(trackId);
			float trackY = SceneGraph.getTrackYPos(trackId);
			float sectionX = SceneGraph.getSectionXPos(trackId, sectionId);
			float sectionY = SceneGraph.getSectionYPos(trackId, sectionId);

			float markerPosX = isMbsf ? xpos : xpos + trackX + sectionX;
			float markerPosY = ypos + trackY + sectionY;

			float iconPosX = markerPosX - trackX - sectionX - canvas_dpix / 2.25f;
			float iconPosY = ypos - canvas_dpiy;

			int markerId = SceneGraph.createCoreSectionMarker(trackId, sectionId, groupId, typeId, markerPosX, markerPosY);

			if (markerId < 0) {
				return CREATE_MARKER_FAIL;
			}

			SceneGraph.setCoreSectionMarkerType(trackId, sectionId, markerId, typeId);
			SceneGraph.setCoreSectionMarkerGroup(trackId, sectionId, markerId, groupId);

			String[] labelTitleStrs = titles.elementAt(3).trim().split(":");
			String property = labelTitleStrs.length > 1 ? labelTitleStrs[1].trim() : "";
			text = property + ": " + text;
			SceneGraph.setCoreSectionMarkerText(trackId, sectionId, markerId, text);
			SceneGraph.setCoreSectionMarkerVertex(trackId, sectionId, markerId, iconPosX, iconPosY, v0, v1, v2, v3);

			// annotation contents: from file url or text string
			String url = toks[9].trim();
			String local = "";

			try {
				URL u = new URL(url);

				if (u.getProtocol().equals("file")) {
					local = u.getFile();
				}
			} catch (MalformedURLException e) {
				// not a valid url, put the reset string into a new annot file
				local = createAnAnnotationFile(titles, toks, trackId, sectionId, markerId);
				try {
					url = new File(local).toURI().toURL().toString();
				} catch (MalformedURLException e1) {
					url = "file:////" + local;
				}
			}

			SceneGraph.setCoreSectionMarkerURL(trackId, sectionId, markerId, url);
			SceneGraph.setCoreSectionMarkerLocal(trackId, sectionId, markerId, local);

		} catch (NumberFormatException e) {
			System.out.println("CR: NumberFormatExcetion, will ignore the rest");

			return NUM_FORMAT_ERROR;
		}

		return FORMAT_OK;
	}

	// load a annotation table from File f and adding to scenegraph
	public static boolean loadCSVAnnotationList(final Window owner, final File f) {
		if ((f == null) || !f.exists()) {
			return false;
		}

		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;
			String[] toks;

			// header
			// create header titles
			line = reader.readLine();
			System.out.println("---> Columns labels: " + line);
			Vector<String> titles = getTitles(line);

			while ((line = reader.readLine()) != null) {
				toks = line.split(",");
				int cols = toks.length;

				if (cols >= 10) {
					int res = handleCRFormat(titles, toks);

					if (res == NUM_FORMAT_ERROR) {
						String mesg = "Format error. Stop.";
						System.err.println("---> " + mesg);
						JOptionPane.showMessageDialog(owner, mesg);

						break;
					} else if ((res == NO_TRACK_OR_SECTION) || (res == CREATE_MARKER_FAIL)) {
						String mesg = "Cannot create marker, continue.";
						System.err.println("---> " + mesg);
					}
				} else {
					// just ignore
					System.err.println("---> Columns not match, just ignore. " + cols);
				}

				CorelyzerApp app = CorelyzerApp.getApp();
				if (app != null) {
					app.updateGLWindows();
				}
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static void openAnnotation(final Component owner, final int trackId, final int sectionId, final int markerId) {
		String local = SceneGraph.getCoreSectionMarkerLocal(trackId, sectionId, markerId);
		String url = SceneGraph.getCoreSectionMarkerURL(trackId, sectionId, markerId);

		// Ignore annotation popup if local and url are nulls
		if ((local == null) && (url == null)) {
			JOptionPane.showMessageDialog(owner, "Can not find annotation.\nBoth URL and local file path are invalid.");
			return;
		}

		int groupid = SceneGraph.getCoreSectionMarkerGroup(trackId, sectionId, markerId);

		int typeid = SceneGraph.getCoreSectionMarkerType(trackId, sectionId, markerId);

		boolean haveURL = true;

		CRAnnotationWindow annotationDialog = new CRAnnotationWindow("Annotation");

		if (local != null) {
			File f = new File(local);
			if (f.exists()) {
				// TODO "Framework-lize" this: CorelyzerGLCanvas#666
				// Switch to use ClastInfoDialog
				if (groupid == ChatGroup.CLAST) {
					// read the file into ClastInfoDialog
					Hashtable<String, String> aDict = PropertyListUtility.generateHashtableFromFile(f);

					float[] currentPos = getAnnotationArea(trackId, sectionId, markerId);

					// FIXME
					// String oldULPosStr = aDict.get("upperleft");
					String newULPosStr = currentPos[0] + ", " + currentPos[1];

					// String oldLRPosStr = aDict.get("lowerright");
					String newLRPosStr = currentPos[2] + ", " + currentPos[3];

					// Update hashtable with current location
					// on screen
					aDict.put("upperleft", newULPosStr);
					aDict.put("lowerright", newLRPosStr);

					aDict.put("height", "" + Math.abs(currentPos[1] - currentPos[3]));
					aDict.put("width", "" + Math.abs(currentPos[2] - currentPos[0]));

					// Default: area
					aDict.put("starting_depth", String.format("%.3f", currentPos[0]));
					aDict.put("starting_width", String.format("%.3f", currentPos[1]));
					aDict.put("ending_depth", String.format("%.3f", currentPos[2]));
					aDict.put("ending_width", String.format("%.3f", currentPos[3]));

					ClastInfoDialog dlg = new ClastInfoDialog();
					dlg.setAttributes(aDict);
					dlg.setTrackId(trackId);
					dlg.setSectionId(sectionId);
					dlg.setMarkerId(markerId);
					dlg.pack();
					dlg.setLocationRelativeTo(owner);
					dlg.setVisible(true);

					return;
				} else if (groupid == ChatGroup.SAMPLE) {
					Hashtable<String, String> aDict = PropertyListUtility.generateHashtableFromFile(f);

					// Update hashtable with current location
					// on screen
					float[] currentPos = getAnnotationArea(trackId, sectionId, markerId);

					String newLocStr = "(" + currentPos[0] + ", " + currentPos[1] + ") - (" + currentPos[2] + ", " + currentPos[3] + ")";

					aDict.put("sampleLocation", newLocStr);

					// Default: area
					aDict.put("starting_depth", String.format("%.3f", currentPos[0]));
					aDict.put("starting_width", String.format("%.3f", currentPos[1]));
					aDict.put("ending_depth", String.format("%.3f", currentPos[2]));
					aDict.put("ending_width", String.format("%.3f", currentPos[3]));

					SampleRequestDialog dlg = new SampleRequestDialog();
					dlg.setAttributes(aDict);
					dlg.setTrackId(trackId);
					dlg.setSectionId(sectionId);
					dlg.setMarkerId(markerId);
					dlg.setLocationRelativeTo(owner);
					dlg.pack();
					dlg.setVisible(true);

					return;
				} else if (groupid == ChatGroup.DIS) {
					Hashtable<String, String> aDict = PropertyListUtility.generateHashtableFromFile(f);

					// Update hashtable with current location on screen
					float[] currentPos = getAnnotationArea(trackId, sectionId, markerId);

					String newLocStr = "(" + currentPos[0] + ", " + currentPos[1] + ") - (" + currentPos[2] + ", " + currentPos[3] + ")";

					aDict.put("coordinates", newLocStr);

					// Default: area
					aDict.put("starting_depth", String.format("%.3f", currentPos[0]));
					aDict.put("starting_width", String.format("%.3f", currentPos[1]));
					aDict.put("ending_depth", String.format("%.3f", currentPos[2]));
					aDict.put("ending_width", String.format("%.3f", currentPos[3]));

					DefaultFormDialog dlg = new DefaultFormDialog();
					dlg.setAttributes(aDict);
					dlg.setTrackId(trackId);
					dlg.setSectionId(sectionId);
					dlg.setMarkerId(markerId);
					dlg.setLocationRelativeTo(owner);
					dlg.pack();
					dlg.setVisible(true);

					return;
				}

				String u;
				try {
					u = new File(local).toURI().toURL().toString();
				} catch (MalformedURLException e) {
					u = "file:////" + local;
				}

				try {
					haveURL = annotationDialog.setURL(new URL(u));
				} catch (MalformedURLException urle) {
					JOptionPane.showMessageDialog(owner, urle);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					haveURL = annotationDialog.setURL(new URL(url));
				} catch (MalformedURLException urle) {
					JOptionPane.showMessageDialog(owner, urle);
					System.err.println("URL error!" + url);
				} catch (IOException e) {
					Object[] options = { "Locate...", "Delete", "Cancel" };

					int sel = CROptionPane.showOptionDialog(owner, "What do you want to do?", "Missing Annotation", JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

					switch (sel) {
						case 0:
							// remote or local
							AttachmentURLDialog dialog = new AttachmentURLDialog();
							dialog.setIsImageLoader(false);
							dialog.setPreferences(CorelyzerApp.getApp().preferences());
							dialog.pack();
							dialog.setLocationRelativeTo(owner);
							dialog.setVisible(true);

							String value = dialog.getURLString();
							if ((value != null) && !value.equals("")) {
								try {
									URL newURL = new URL(value);
									haveURL = annotationDialog.setURL(newURL);

									SceneGraph.setCoreSectionMarkerURL(trackId, sectionId, markerId, value);

									if (newURL.getProtocol().equalsIgnoreCase("file")) {
										SceneGraph.setCoreSectionMarkerLocal(trackId, sectionId, markerId, newURL.getFile());
									}
								} catch (IOException e1) {
									// still IOException, just return
									JOptionPane.showMessageDialog(owner, "Cannot find selected URL", "ERROR", JOptionPane.ERROR_MESSAGE);

									return;
								}
								// saveSession();
							} else {
								return;
							}

							break;

						case 1:
							removeAnnotation(owner, trackId, sectionId, markerId);
							// saveSession();

						case 2:
							return;
					}
				}
			}
		}

		// Freeform annotation
		annotationDialog.setTrackId(trackId);
		annotationDialog.setSectionId(sectionId);
		annotationDialog.setMarkerId(markerId);
		annotationDialog.setEditExistingMode();
		annotationDialog.setWriteLocalCopy(true);
		annotationDialog.setGroup(groupid);
		annotationDialog.setType(typeid);

		annotationDialog.pack();
		annotationDialog.setLocationRelativeTo(owner);

		annotationDialog.setVisible(haveURL);

		String desc = "" + trackId + "\t" + sectionId + "\t" + markerId;

		CorelyzerApp.getApp().getPluginManager().broadcastEventToPlugins(CorelyzerPluginEvent.EDIT_ANNOTATION, desc);
	}

	public static void removeAnnotation(final Component owner, final int trackId, final int sectionId, final int markerId) {
		String mesg = "Using Drilling Information System (DIS)?";
		int sel = JOptionPane.showConfirmDialog(owner, mesg, "Confirmation", JOptionPane.YES_NO_OPTION);

		if (sel == JOptionPane.YES_OPTION) {
			removeAnnotationForDIS(owner, trackId, sectionId, markerId);
		} else {
			removeAnnotationPermanently(owner, trackId, sectionId, markerId);
		}
	}

	// private boolean isUsingDIS = false;
	public static void removeAnnotationForDIS(final Component owner, final int trackId, final int sectionId, final int markerId) {
		// Just hide the annotation, so the DISExport can change the
		// annotation's state to "deleted"
		String mesg = "Delete the annotation?";

		if (JOptionPane.showConfirmDialog(owner, mesg, "Confirmation", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
			SceneGraph.setCoreSectionMarkerVisibility(trackId, sectionId, markerId, false);

			// refresh canvas
			CorelyzerApp app = CorelyzerApp.getApp();
			if (app != null) {
				app.updateGLWindows();
			}
		}
	}

	public static void removeAnnotationPermanently(final Component owner, final int trackId, final int sectionId, final int markerId) {
		String mesg = "Delete the annotation?";

		if (JOptionPane.showConfirmDialog(owner, mesg, "Confirmation", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
			String local = SceneGraph.getCoreSectionMarkerLocal(trackId, sectionId, markerId);

			SceneGraph.removeCoreSectionMarker(trackId, sectionId, markerId);

			// refresh canvas
			CorelyzerApp app = CorelyzerApp.getApp();
			if (app != null) {
				app.updateGLWindows();
			}

			// delete local file?
			if (local == null) {
				return;
			}

			File f = new File(local);
			if (f.exists()) {
				int ans = JOptionPane.showConfirmDialog(owner, "Delete annotation local file?", "Confirmation", JOptionPane.YES_NO_OPTION);

				if (ans == JOptionPane.YES_OPTION) { // remove the file
					if (!f.delete()) {
						mesg = "Local file \n'" + local + "'\n deletion failed";
						JOptionPane.showMessageDialog(owner, mesg);
					}
				} else { // show the file locally
					String _app;
					try {
						if (System.getProperty("os.name").toLowerCase().contains("windows")) {
							_app = "cmd.exe /c explorer " + local;
							Runtime.getRuntime().exec(_app);
						} else {
							if (MAC_OS_X) {
								FileUtility.showFileInFinder(f);
							} else {
								_app = "open";
								String[] cmd = { _app, local };
								Runtime.getRuntime().exec(cmd);
							}
						}
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(owner, ex);
						System.err.println("IOException in showing " + "file with system browser");
					}
				}
			}
		}
	}

	public static void removeAnnotations(final Component owner, final int[] trackIds, final int[] sectionIds, final int[] markerIds) {
		// check inputs
		if ((trackIds.length != sectionIds.length) || (trackIds.length != markerIds.length) || (sectionIds.length != markerIds.length)) {
			return;
		}

		String mesg = "Delete selected annotations?";

		if (JOptionPane.showConfirmDialog(owner, mesg, "Confirmation", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.YES_OPTION) {

			for (int i = 0; i < trackIds.length; i++) {
				int trackId = trackIds[i];
				int sectionId = sectionIds[i];
				int markerId = markerIds[i];

				String local = SceneGraph.getCoreSectionMarkerLocal(trackId, sectionId, markerId);

				SceneGraph.removeCoreSectionMarker(trackId, sectionId, markerId);

				// refresh canvas
				CorelyzerApp app = CorelyzerApp.getApp();
				if (app != null) {
					app.updateGLWindows();
				}

				// delete local file?
				File f = new File(local);
				if (f.exists()) {
					if (!f.delete()) {
						mesg = "Local file \n'" + local + "'\n deletion failed";
						JOptionPane.showMessageDialog(owner, mesg);
					}
				}
			}
		}
	}

	public static boolean saveCSVAnnotationList(final File f, final FreeformAnnotationTableModel model) {
		if ((f == null) || (model == null)) {
			return false;
		}

		String outputLine;
		try {
			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);

			// writer header
			outputLine = "# track,section,group,text,type,[position],[url|...]\n";
			bw.write(outputLine);

			for (int i = 0; i < model.getRowCount(); ++i) {
				// track and section names
				String trackName = (String) model.getValueAt(i, 1);
				String sectionName = (String) model.getValueAt(i, 2);

				outputLine = trackName + "," + sectionName;

				int trackId = Integer.parseInt((String) model.getValueAt(i, 6));
				int sectionId = Integer.parseInt((String) model.getValueAt(i, 7));
				int markerId = Integer.parseInt((String) model.getValueAt(i, 8));

				// type, group, text
				int type = SceneGraph.getCoreSectionMarkerType(trackId, sectionId, markerId);

				String groupName = (String) model.getValueAt(i, 4);
				String url = (String) model.getValueAt(i, 5);
				String text = SceneGraph.getCoreSectionMarkerText(trackId, sectionId, markerId);
				if (text == null) {
					text = "";
				}

				outputLine += "," + groupName + "," + text + "," + MarkerType.getMarkerName(type);

				// position
				float canvas_dpix = SceneGraph.getCanvasDPIX(0);
				float canvas_dpiy = SceneGraph.getCanvasDPIY(0);

				// todo: mbsf or down core depth_coord?
				String position = "";
				switch (type) {
					case MarkerType.CORE_SPAN_MARKER:
						float start = SceneGraph.getCoreSectionMarkerV0(trackId, sectionId, markerId) * 2.54f / (canvas_dpix * 100.0f);
						float end = SceneGraph.getCoreSectionMarkerV2(trackId, sectionId, markerId) * 2.54f / (canvas_dpiy * 100.0f);

						position += "," + start + ",0.0," + end + ",0.0";

						break;

					case MarkerType.CORE_OUTLINE_MARKER:
						float ulx = SceneGraph.getCoreSectionMarkerV0(trackId, sectionId, markerId) * 2.54f / (canvas_dpix * 100.0f);
						float uly = SceneGraph.getCoreSectionMarkerV1(trackId, sectionId, markerId) * 2.54f / (canvas_dpiy * 100.0f);
						float lrx = SceneGraph.getCoreSectionMarkerV2(trackId, sectionId, markerId) * 2.54f / (canvas_dpix * 100.0f);
						float lry = SceneGraph.getCoreSectionMarkerV3(trackId, sectionId, markerId) * 2.54f / (canvas_dpiy * 100.0f);

						position += "," + ulx + "," + uly + "," + lrx + "," + lry;

						break;

					default: // CORE_POINT_MARKER
						float x_pos = SceneGraph.getCoreSectionMarkerXPos(trackId, sectionId, markerId) * 2.54f / (canvas_dpix * 100.0f);
						float y_pos = SceneGraph.getCoreSectionMarkerYPos(trackId, sectionId, markerId) * 2.54f / (canvas_dpiy * 100.0f);

						position += "," + x_pos + "," + y_pos + "," + x_pos + "," + y_pos;

						break;
				}

				outputLine += position + "," + url;

				outputLine += "\n";
				bw.write(outputLine);
			}

			bw.flush();
			bw.close();
			fw.close();

			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

}
