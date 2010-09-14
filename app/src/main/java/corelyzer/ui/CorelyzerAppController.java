/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2007 Julian Yu-Chung Chen
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
package corelyzer.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.apache.xerces.parsers.DOMParser;

import corelyzer.data.CRPreferences;
import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionImage;
import corelyzer.data.SAXWellLogDataSet;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.UnitLength;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.WellLogTable;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.data.lists.CRListModels;
import corelyzer.graphics.SceneGraph;
import corelyzer.handlers.SubscribeHandler;
import corelyzer.helper.OSXAdapter;
import corelyzer.io.CRDISDepthValueDataLoader;
import corelyzer.io.CRDepthValueDataLoader;
import corelyzer.io.DISLoader;
import corelyzer.io.DISWriter;
import corelyzer.io.StateLoader;
import corelyzer.io.StateWriter;
import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.plugin.CorelyzerPluginManager;
import corelyzer.remoteControl.server.controller.ControlServerApplication;
import corelyzer.sessionSharing.client.controller.SharingClient;
import corelyzer.sessionSharing.client.view.SessionSharingClientUI;
import corelyzer.sessionSharing.client.view.SessionSharingListUI;
import corelyzer.sessionSharing.common.SharingServerResponse;
import corelyzer.ui.annotation.AnnotationTypeDirectory;
import corelyzer.util.FileUtility;
import corelyzer.util.StringUtility;

public class CorelyzerAppController implements ActionListener {
	// handy utilities
	static boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
	String sp = System.getProperty("file.separator");

	SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy-hhmmss-z");
	String now = formatter.format(new Date());
	String autoSavePath = "autoSave-" + now + ".cml";

	CorelyzerApp view;

	CoreGraph cg; // Java scenegraph
	CRListModels listModels; // list models

	// States
	boolean isCollaborativeMode = false;

	// Plugin Handler
	CorelyzerPluginManager pluginManager;

	// private BonjourManager bonjourManager;
	private ControlServerApplication remoteControlServer;

	public CorelyzerAppController() {
		super();
	}

	public CorelyzerAppController(final CorelyzerApp app) {
		this();

		// view
		view = app;

		// model
		cg = CoreGraph.getInstance();
		listModels = new CRListModels(cg);

		pluginManager = new CorelyzerPluginManager(view.plugins);
	}

	public void about() {
		AboutDialog aboutdlg = new AboutDialog();
		aboutdlg.setVisible(true);
	}

	public void actionPerformed(final ActionEvent e) {
		Object actionSource = e.getSource();

		// check the plugin menu items
		for (int k = 0; k < view.pluginMenuItemVec.size(); k++) {
			if (actionSource.equals(view.pluginMenuItemVec.elementAt(k))) {
				pluginManager.showPlugin(k);
			}
		}
	}

	protected void cleanThingsUp() {
		if (remoteControlServer != null) {
			remoteControlServer.setRunning(false);
		}

		// Save previous session file
		String previousSession = view.preferences().config_Directory + sp + "previousSession.cml";
		StateWriter sw = new StateWriter();
		sw.writeState(previousSession);

		view.preferences().save();
		pluginManager.Shutdown();
		SceneGraph.closeDown();
	}

	public void clearImageCache() {

		File texDir = new File(view.preferences().texBlock_Directory);

		if (texDir.exists() && texDir.isDirectory()) {

			Object[] options = { "No", "Yes" };

			int sel = JOptionPane.showOptionDialog(view.getMainFrame(), "Are you sure want to " + "clean your image texture " + "cache?", "Clear Confirmation",
					JOptionPane.YES_NO_OPTION, JOptionPane.CANCEL_OPTION, null, options, options[0]);

			if (sel == 1) {

				sel = JOptionPane.showOptionDialog(view.getMainFrame(), "All files in " + texDir.getAbsolutePath() + " will be deleted\n"
						+ "Are you really sure?", "Clear Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.CANCEL_OPTION, null, options, options[0]);

				if (sel == 1) {

					System.out.println("-- CRITICAL Section Deleteing " + texDir.getAbsolutePath());

					String[] dirList = texDir.list();

					for (String aDirList : dirList) {
						String myDirPath = texDir.getAbsolutePath() + "/" + aDirList;
						File d = new File(myDirPath);

						if (d.exists() && d.isDirectory()) {
							System.out.print("Delete " + d.getAbsolutePath());
							boolean isSuccess = FileUtility.deleteDir(d);
							String result = isSuccess ? "SUCCESS" : "FAILED";
							System.out.print(" : " + result + "\n");
						}
					}
				}
			}
		}
	}

	// Returns true if a dataset name in the dataset list model exists.
	public boolean containsDatasetName(final String name) {
		CRDefaultListModel dataFilesListModel = listModels.getListModel(CRListModels.DATASET);

		for (int i = 0; i < dataFilesListModel.size(); i++) {
			WellLogDataSet ds = (WellLogDataSet) dataFilesListModel.elementAt(i);
			if (ds.toString().equals(name)) {
				return true;
			}
		}

		return false;
	}

	// Returns true if a section name in the section list model exists.
	public boolean containsSectionName(final int trackId, final String name) {
		CRDefaultListModel trackListModel = listModels.getListModel(CRListModels.TRACK);

		for (int i = 0; i < trackListModel.size(); i++) {
			TrackSceneNode t = (TrackSceneNode) trackListModel.elementAt(i);
			if (t.getId() == trackId) {
				return t.containsSectionName(name);
			}
		}

		return false;
	}

	// Returns true if a session name in the session list model exists.
	public boolean containsSessionName(final String name) {
		CRDefaultListModel listModel = listModels.getListModel(CRListModels.SESSION);

		for (int i = 0; i < listModel.size(); i++) {
			Session s = (Session) listModel.elementAt(i);
			String elementName = s.getName();

			if (name.equals(elementName)) {
				return true;
			}
		}

		return false;
	}

	// Returns true if a track name in the track list model exists.
	public boolean containsTrackName(final String name) { // FIXME consider
															// session?
		CRDefaultListModel listModel = listModels.getListModel(CRListModels.TRACK);

		for (int i = 0; i < listModel.size(); i++) {
			TrackSceneNode t = (TrackSceneNode) listModel.elementAt(i);
			String elementName = t.getName();

			if (name.equals(elementName)) {
				return true;
			}
		}

		return false;
	}

	public void createSession() {
		String name = getAValidInputString("", "Create a session", "Please input a session name", "Empty Input", "Please input non-empty string.",
				"Duplicate Input", "Please input non-duplicated string.", CRListModels.SESSION);

		if (name == null) {
			return;
		}

		createSession(name);
	}

	public void createSession(String sessionName) {
		// check null, empty and duplication
		if (sessionName == null || sessionName.equals("") || this.containsSessionName(sessionName)) {
			String name = getAValidInputString("", "Create a session", "Please input a session name", "Empty Input", "Please input non-empty string.",
					"Duplicate Input", "Please input non-duplicated string.", CRListModels.SESSION);

			if (name == null) {
				return;
			} else {
				sessionName = name;
			}
		}

		Session s = new Session(sessionName);
		cg.addSession(s);
		view.sessionList.setSelectedIndex(cg.getCurrentSessionIdx());
	}

	/**
	 * Method called when the "Create Track" menu item is selected. This creates
	 * a text entry dialog where the returned string is used to create a track.
	 */
	public void createTrack() {
		String name = getAValidInputString("", "Create a track", "Please input a track name", "Empty Input", "Please input non-empty string.",
				"Duplicate Input", "Please input non-duplicated string.", CRListModels.TRACK);

		if (name == null) {
			return;
		}

		int trackId = createTrack(name);

		// broadcast the event to plugins
		if (trackId > -1) {
			pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.TRACK_CREATED, name);
		}
	}

	// Method used to handle the creation of new tracks in the
	// corelyzer.helper.SceneGraph and the list models. If a track of the given
	// name already
	// exists, then the id of that track is returned.
	public int createTrack(String trackName) {
		Session s = cg.getCurrentSession();

		if (s == null) {
			this.createSession("Default");
			s = cg.getCurrentSession();
		}

		// check null, empty and duplication
		if (trackName == null || trackName.equals("") || this.containsTrackName(trackName)) {
			String name = getAValidInputString("", "Create a track", "Please input a track name", "Empty Input", "Please input non-empty string.",
					"Duplicate Input", "Please input non-duplicated string.", CRListModels.TRACK);

			if (name == null) {
				return -1;
			} else {
				trackName = name;
			}
		}

		int id;

		// determine if the track name is already there if so, don't make it
		// again
		if ((id = SceneGraph.getTrackIDByName(s.getName(), trackName)) > -1) {
			return id;
		}
		if ((id = SceneGraph.addTrack(s.getName(), trackName)) < 0) {
			return id;
		}

		// create the node and add it to the listing of tracks
		TrackSceneNode t = new TrackSceneNode(trackName, id);
		cg.addTrack(s, t);

		view.loadImageMenuItem.setEnabled(true);
		view.sessionList.setSelectedIndex(cg.getCurrentSessionIdx());
		view.trackList.setSelectedIndex(cg.getCurrentTrackIdx());

		return id;
	}

	public void deiconifyAllPlugins() {
		pluginManager.iconifyAllPlugins();
	}

	public void deleteDataset(final Session s, final WellLogDataSet d) {
		System.out.println("---> [INFO] Closing dataset: " + d.getSourceFilename());
		int datasetId = d.getId();

		SceneGraph.lock();
		{
			SceneGraph.deleteDataset(datasetId);
			// cg.removeDataset(s, d);
		}
		SceneGraph.unlock();

		view.updateGLWindows();
	}

	// Method used to delete a section from app
	public void deleteSection(final int trackId, final int secId) {
		// perform range test
		if (trackId < 0 || secId < 0) {
			return;
		}

		CRDefaultListModel trackListModel = listModels.getListModel(CRListModels.TRACK);

		TrackSceneNode t = (TrackSceneNode) trackListModel.elementAt(trackId);

		// need to find index of section in ListModel
		int sid = -1;
		CoreSection cs;
		for (int i = 0; i < t.getNumCores(); i++) {
			cs = t.getCoreSection(i);
			if (cs.getId() == secId) {
				sid = i;
				break;
			}
		}
		if (sid < 0) {
			return;
		}

		SceneGraph.lock();
		{
			// for native code, use native section id
			t.removeCoreSection(t.getCoreSection(sid));
			SceneGraph.removeSectionImageFromTrack(trackId, secId);
		}
		SceneGraph.unlock();
		cg.notifyListeners();
		view.updateGLWindows();

		// broadcast to plugins
		// use native index so that we can utilize hashtable in session plugin
		String message = "";
		message = message + trackId + "\t" + secId;
		if (secId > -1) {
			pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.SECTION_REMOVED, message);
		}
	}

	/**
	 * Method used to handle the request of section deletion in the SceneGraph
	 * and the list models.
	 */
	public void deleteSelectedSection() {
		Session s = cg.getCurrentSession();
		TrackSceneNode t = cg.getCurrentTrack();
		CoreSection cs = cg.getCurrentSection();

		if (s == null || t == null || cs == null) {
			return;
		}

		String message = t.getId() + "\t" + cs.getId();
		SceneGraph.lock();
		{
			t.removeCoreSection(cs);
			SceneGraph.removeSectionImageFromTrack(t.getId(), cs.getId());
		}
		SceneGraph.unlock();
		cg.notifyListeners();
		view.updateGLWindows();

		// broadcast to plugins
		// use native index so that we can utilize hashtable in session plugin
		// if (cs != null)
		pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.SECTION_REMOVED, message);
	}

	/**
	 * Method used to handle the deletion of track in the
	 * corelyzer.helper.SceneGraph and the list models.
	 */
	public void deleteSelectedTrack() {

		if (isCollaborativeMode) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Can't delete track in collaboration mode!\n" + "You logged in session server!");
			return;
		}

		TrackSceneNode t = (TrackSceneNode) view.trackList.getSelectedValue();
		if (t == null) {
			return;
		}

		int idx = t.getId();
		t.removeAllCoreSection();

		SceneGraph.lock();
		{
			SceneGraph.deleteTrack(idx);
			cg.removeTrack(cg.getCurrentSession(), t);
		}
		SceneGraph.unlock();
		view.updateGLWindows();

		// broadcast to plugins
		String message = "";
		message = message + idx;
		if (idx > -1) {
			pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.TRACK_REMOVED, message);
		}
	}

	public void deleteTrack() {
		String currentTrack = cg.getCurrentTrack().getName();
		deleteTrack(currentTrack);
	}

	public void deleteTrack(final Session s, final TrackSceneNode t) {
		// System.out.println("---> [INFO] Closing track: " + t.getName());

		if (isCollaborativeMode) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Can't delete track in collaboration mode!\n" + "You logged in session server!");
			return;
		}

		int idx;
		SceneGraph.lock();
		{
			idx = t.getId();
			t.removeAllCoreSection();
			SceneGraph.deleteTrack(t.getId());
			// cg.removeTrack(s, t);
		}
		SceneGraph.unlock();

		// cg.notifyListeners(); already did in cg.removeTrack()
		view.updateGLWindows();

		// broadcast to plugins
		String message = "";
		message = message + idx;
		if (idx > -1) {
			pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.TRACK_REMOVED, message);
		}
	}

	// Method used to handle the deletion of track in the
	// SceneGraph and the list models.
	public void deleteTrack(final String name) {
		if (isCollaborativeMode) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Can't delete track in collaboration mode!\n" + "You logged in session server!");
			return;
		}

		String message = "Are you sure you want to delete track: " + name + " ?";

		if (JOptionPane.showConfirmDialog(view.getMainFrame(), message) == JOptionPane.OK_OPTION) {

			int idx = -1;
			for (Session s : cg.getSessions()) {
				for (TrackSceneNode t : s.getTrackSceneNodes()) {
					String trackName = t.getName();

					if (name.equals(trackName)) {
						SceneGraph.lock();
						{
							idx = t.getId();
							t.removeAllCoreSection();
							SceneGraph.deleteTrack(t.getId());
							cg.removeTrack(s, t);
						}
						SceneGraph.unlock();
						break;
					}
				}
			}

			cg.notifyListeners();
			view.updateGLWindows();

			// broadcast to plugins
			message = "";
			message = message + idx;
			if (idx > -1) {
				pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.TRACK_REMOVED, message);
			}
		}
	}

	public void disBatchDataLoad() {
		String fileString = FileUtility.selectASingleFile(view.getMainFrame(), "Select a Data File", "txt", FileUtility.LOAD);

		if (fileString == null) {
			return;
		}

		File selectedFile = new File(fileString);
		if (selectedFile.exists()) {
			// Load the DIS data file. And use the 'Hole', 'Expedition' data in
			// the file to
			// create session and track names.
			CRDISDepthValueDataLoader dvLoader = new CRDISDepthValueDataLoader(selectedFile);
			dvLoader.load();
		}
	}

	public void disExport() {
		String aFile = FileUtility.selectASingleFile(view.getMainFrame(), "Select a DIS export file", "xml", FileUtility.SAVE);

		if (aFile != null && !aFile.equals("")) {
			DISWriter disWriter = new DISWriter();
			boolean isSuccess = disWriter.writeFile(aFile);

			String mesg;
			if (!isSuccess) {
				mesg = "Failed!";
				JOptionPane.showMessageDialog(view.getMainFrame(), mesg);
			}
		}
	}

	public void disImport() {
		final String aFile = FileUtility.selectASingleFile(view.getMainFrame(), "Select a DIS import file", "xml", FileUtility.LOAD);

		if (aFile != null && !aFile.equals("")) {
			Thread loadDISThread = new Thread() {
				public void run() {
					DISLoader disLoader = new DISLoader();
					String disPrefix = view.preferences().getProperty("dis.prefix");
					disPrefix = disPrefix == null ? "" : disPrefix;

					boolean isSuccess = disLoader.loadFile(aFile, disPrefix);

					String mesg;
					if (!isSuccess) {
						mesg = "Failed!";
						JOptionPane.showMessageDialog(view.getMainFrame(), mesg);
					}

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							view.updateGLWindows();
						}
					});
				}
			};

			loadDISThread.start();
		}
	}

	public void doAssociatedFile(final String aFilename) {
		System.out.println("[FIXME] Mac: opening file: " + aFilename);

		File aFile = new File(aFilename);
		if (!aFile.exists()) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Unknown file format");
			return;
		}

		String extension = StringUtility.getExtension(aFile);

		if (extension.toLowerCase().equals("cml")) {
			System.out.println("---> [INFO] Loading CML: " + aFile);
			loadStateFile(aFilename);
		} else {
			String message = "Do not know how to handle file '" + aFilename + "' with extension: '" + extension + "'";
			JOptionPane.showMessageDialog(view.getMainFrame(), message);
		}
	}

	public void doPreferences() {
		CRPreferencesDialog dlg = new CRPreferencesDialog(view.getMainFrame(), view.preferences);

		dlg.pack();
		dlg.setSize(450, 750);
		dlg.positionCentricTo(view.getMainFrame());
		dlg.setVisible(true);
		dlg.dispose();
	}

	public void exportSelectedTrack(final Component ownerComponent) {
		// Write out the composistion of the track with sorted depth
		TrackSceneNode t = (TrackSceneNode) view.trackList.getSelectedValue();
		if (t.getId() < 0) {
			JOptionPane.showMessageDialog(ownerComponent, "Invalid track");
			return;
		}

		String fileString = FileUtility.selectASingleFile(CorelyzerApp.getApp().getMainFrame() /* fixme */, "Save tables", "", FileUtility.SAVE);

		String affineTableString = fileString + ".affine_table";
		String spliceTableString = fileString + ".splice_table";
		String sessionFileString = fileString + ".cml";

		// Save all sessions
		CoreGraph cg = CoreGraph.getInstance();
		int numSessions = cg.getNumberOfSessions();
		boolean[] sessionIndices = new boolean[numSessions];
		for (int i = 0; i < numSessions; ++i) {
			sessionIndices[i] = true;
		}

		StateWriter writer = new StateWriter(sessionIndices);
		writer.writeState(sessionFileString);

		// Open files
		File affineTableFile = new File(affineTableString);
		File spliceTableFile = new File(spliceTableString);

		System.out.println("- Affine table: " + affineTableFile.getAbsolutePath());
		System.out.println("- Splice table: " + spliceTableFile.getAbsolutePath());

		try {
			FileWriter affineFileWriter = new FileWriter(affineTableFile);
			BufferedWriter affineWriter = new BufferedWriter(affineFileWriter);

			FileWriter spliceFileWriter = new FileWriter(spliceTableFile);
			BufferedWriter spliceWriter = new BufferedWriter(spliceFileWriter);

			// ---

			// Depth-sort sections
			Vector<CoreSection> coresections = t.getCoreSections();
			Hashtable<Float, Integer> indexDepthHash = new Hashtable<Float, Integer>();
			for (int i = 0; i < coresections.size(); ++i) {
				CoreSection cs = coresections.elementAt(i);
				float intervalStart = SceneGraph.getSectionIntervalTop(t.getId(), cs.getId());
				float topDepth = (SceneGraph.getSectionDepth(t.getId(), cs.getId()) + intervalStart) / 100.0f;

				indexDepthHash.put(topDepth, i);
			}

			Vector<Float> v = new Vector<Float>(indexDepthHash.keySet());
			Collections.sort(v);

			for (int i = 0; i < v.size(); ++i) {
				float topDepth = v.elementAt(i);

				int idx = indexDepthHash.get(topDepth);
				CoreSection cs = coresections.elementAt(idx);

				int pTrackId = SceneGraph.getSectionParentTrackId(t.getId(), cs.getId());
				int pSectionId = SceneGraph.getSectionParentSectionId(t.getId(), cs.getId());

				float intervalStart = SceneGraph.getSectionIntervalTop(t.getId(), cs.getId());
				float intervalEnd = SceneGraph.getSectionIntervalBottom(t.getId(), cs.getId());

				String parentTrackName = SceneGraph.getTrackName(pTrackId);
				String parentSectionName = SceneGraph.getSectionName(pTrackId, pSectionId);
				float parentTopDepth = SceneGraph.getSectionDepth(pTrackId, pSectionId) / 100.0f;

				float childTopDepth = SceneGraph.getSectionDepth(t.getId(), cs.getId()) / 100.0f;

				// Get session name
				String sessionName = "Unknown";
				Vector<Session> sessions = cg.getSessions();
				for (Session s : sessions) {
					TrackSceneNode trackNode = s.getTrackSceneNodeWithTrackId(pTrackId);
					if (trackNode != null) {
						sessionName = s.getName();
					}
				}

				// Write affine shift for each section
				String affineLine = sessionName + "\t" + parentTrackName + "\t" + parentSectionName + "\t" + (childTopDepth - parentTopDepth) + "\n";
				affineWriter.write(affineLine);

				// Write splice line
				float topMCD = topDepth;
				float topMBSF = parentTopDepth + intervalStart / 100.0f;
				float bottomMCD = (SceneGraph.getSectionDepth(t.getId(), cs.getId()) + intervalEnd) / 100.0f;
				;
				float bottomMBSF = parentTopDepth + intervalEnd / 100.0f;

				String spliceLine = sessionName + "\t" + parentTrackName + "\t" + parentSectionName + "\t" + topMCD + "\t" + topMBSF + "\t" + bottomMCD + "\t"
						+ bottomMBSF + "\n";

				spliceWriter.write(spliceLine);
			}

			affineWriter.flush();
			affineFileWriter.flush();
			affineWriter.close();
			affineFileWriter.close();

			spliceWriter.flush();
			spliceFileWriter.flush();
			spliceWriter.close();
			spliceFileWriter.close();

			// popup notification
			String mesg = "3 files are written:\n" + "Affine table: " + affineTableFile + "\n" + "Splice table: " + spliceTableFile + "\n"
					+ "Corelyzer session: " + sessionFileString;
			JOptionPane.showMessageDialog(ownerComponent, mesg);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(ownerComponent, "Save tables error.");

			e.printStackTrace();
		}

	}

	public void exportTheWholeScene() {
		// Prompt and Save the Whole session along with images & data
		CRExporterDialog dialog = new CRExporterDialog(view);
		dialog.pack();
		dialog.setVisible(true);
	}

	/*
	 * A reusable call to validate empty or duplicate input string with a given
	 * list model
	 */
	private String getAValidInputString(final String initInputString, final String initTitle, final String initMessage, final String emptyTitle,
			final String emptyMessage, final String dupTitle, final String dupMessage, final String listType) {
		String aName = initInputString;
		String title = initTitle;
		String message = initMessage;

		while ((aName = (String) JOptionPane.showInputDialog(view.getMainFrame(), message, title, JOptionPane.QUESTION_MESSAGE, null, null, aName)) != null) {
			// Empty
			if (aName.equals("")) {
				title = emptyTitle;
				message = emptyMessage;

				continue;
			}

			// Duplicates
			if (listType.equals(CRListModels.SESSION)) {
				if (this.containsSessionName(aName)) {
					title = dupTitle;
					message = dupMessage;

					aName = this.incrementAStringWithNumber(aName, CRListModels.SESSION);

					continue;
				}
			} else if (listType.equals(CRListModels.TRACK)) {
				if (this.containsTrackName(aName)) {
					title = dupTitle;
					message = dupMessage;

					aName = this.incrementAStringWithNumber(aName, CRListModels.TRACK);

					continue;
				}
			}

			break;
		}

		return aName;
	}

	// --------------------------------------------------------------------------

	// Returns the currently set directory to store downloaded files
	public String getDownloadDirectoryPath() {
		return view.preferences().download_Directory;
	}

	private int getSelectedSection() {
		CRDefaultListModel sectionsListModel = listModels.getListModel(CRListModels.SECTION);

		int idx = view.getSectionList().getSelectedIndex();
		CoreSectionImage cs = (CoreSectionImage) sectionsListModel.elementAt(idx);
		return cs.getId();
	}

	public int getSelectedTrack() { // return native track id
		CRDefaultListModel trackListModel = listModels.getListModel(CRListModels.TRACK);

		int idx = view.trackList.getSelectedIndex();
		TrackSceneNode t = (TrackSceneNode) trackListModel.elementAt(idx);

		return t.getId();
	}

	public void helpAction() {
		WikiHelpDialog dialog = new WikiHelpDialog(view.getMainFrame(), "Corelyzer");
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
	}

	public void iconifyAllPlugins() {
		pluginManager.iconifyAllPlugins();
	}

	// Prepare and launch corelyzer.ui.DataImportWizard
	public void importData() {
		String selected = FileUtility.selectASingleFile(view.getMainFrame(), "Choose a text file", null, FileUtility.LOAD);

		if (selected != null) {
			File selectedFile = new File(selected);

			if (selectedFile.exists()) {
				DataImportWizard wiz = new DataImportWizard(view.getMainFrame());
				wiz.setInputFile(selectedFile);
				wiz.setVisible(true);
			}
		}
	}

	public void importPackage() {
		String selected = FileUtility.selectASingleFile(view.getMainFrame(), "Select a session package", "car", FileUtility.LOAD);

		if (selected != null) {
			final File f = new File(selected);

			if (f.exists()) {
				Runnable r = new Runnable() {
					public void run() {
						processCARFile(f);
					}
				};

				new Thread(r).start();
			}
		}
	}

	/*
	 * Generate new string with an incremented number at the end of the input
	 * string
	 */
	private String incrementAStringWithNumber(final String inputString, final String listType) {
		String outputString = inputString;

		if (listType.equals(CRListModels.SESSION)) {
			int i = 1;
			while (this.containsSessionName(outputString)) {
				outputString = inputString + " (" + i + ")";
				i++;
			}
		} else if (listType.equals(CRListModels.TRACK)) {
			int i = 1;
			while (this.containsTrackName(outputString)) {
				outputString = inputString + " (" + i + ")";
				i++;
			}
		}

		return outputString;
	}

	private void initAnnotationTypeDirectory() {
		AnnotationTypeDirectory annotationTypeDirectory = new AnnotationTypeDirectory();
		annotationTypeDirectory.init();
	}

	private void initBonjour() {
		if (MAC_OS_X) {
			// this.bonjourManager = new BonjourManager(view.friendsMenu);
			System.out.println("[Disabled] Initialize Bonjour...");
		}
	}

	private void initRemoteControlServer() {
		remoteControlServer = new ControlServerApplication("LocalREmoteControl", "127.0.0.1", 17799);
		remoteControlServer.init();
		remoteControlServer.start();
	}

	// ---------------------------------------------------------------

	public void listSessions() {
		String serverAddress;
		int serverPort;

		// Fetch setup preferences if available
		CorelyzerApp app = CorelyzerApp.getApp();

		if (app == null) {
			serverAddress = "127.0.0.1";
			serverPort = 16688;
		} else {
			CRPreferences prefs = app.preferences();

			String srvAddr;
			int srvPort;

			if (prefs.getProperty("sessionSharing.serverAddress") == null || prefs.getProperty("sessionSharing.serverAddress").equals("")) {
				srvAddr = "127.0.0.1";
			} else {
				srvAddr = prefs.getProperty("sessionSharing.serverAddress");
			}

			if (prefs.getProperty("sessionSharing.serverPort") == null || prefs.getProperty("sessionSharing.serverPort").equals("")) {
				srvPort = 16688;
			} else {
				srvPort = Integer.parseInt(prefs.getProperty("sessionSharing.serverPort"));
			}

			serverAddress = srvAddr;
			serverPort = srvPort;
		}

		// delegte to client code execution
		SharingClient client = new SharingClient(null);
		client.setConnectOperation(serverAddress, serverPort);

		// try connect first
		byte res = client.execute();
		if (res != SharingServerResponse.CONNECTED) {
			// this.setRunningIndicator(false, "Idle");

			String mesg = "Cannot connect to server \n'" + serverAddress + "'";
			JOptionPane.showMessageDialog(view.getMainFrame(), mesg);
			return;
		}

		// do real list
		client.setListOperation();
		res = client.execute();

		if (res != SharingServerResponse.LIST_SUCCESS) {
			String mesg = "Cannot list";
			JOptionPane.showMessageDialog(view.getMainFrame(), mesg);
		} else {
			Vector<Hashtable<String, String>> sessions = client.getListResult();
			if (sessions != null) {
				SessionSharingListUI listUI = new SessionSharingListUI();
				listUI.setAlwaysOnTop(true);
				listUI.setSessions(sessions);
				listUI.pack();
				listUI.setSize(800, 300);
				listUI.setLocationRelativeTo(view.getMainFrame());
				listUI.setVisible(true);
			}
		}
	}

	public void loadData() {
		String[] selected = FileUtility.selectMultipleFiles(view.getMainFrame(), "Select Data Files", "xml");

		if (selected != null) {
			// Progress Dialog Stuff
			// ProgressDialog progress = new ProgressDialog();
			JProgressBar progress = view.getProgressUI();
			progress.setString("Loading Dataset");
			progress.setMaximum(selected.length);
			progress.setValue(0);
			// progress.setVisible(true);

			for (int i = 0; i < selected.length; ++i) {
				String s = selected[i];
				File f = new File(s);

				if (f.exists()) {
					progress.setString(f.getName());

					this.loadData(f, true); // Use SAX or not. True: SAX, False:
											// DOM
					progress.setValue(i);
				}
			}

			progress.setValue(selected.length);
			progress.setString("Data loaded");
			progress.setValue(0);
			// progress.dispose();
		}

		// refresh our main frame
		/*
		 * view.rootPanel.paintImmediately(0, 0, view.rootPanel.getWidth(),
		 * view.rootPanel.getHeight());
		 */

	}

	// Loads the data from a local XML file holding MSCL data in the form
	// of a Corelyzer XML Schema. Please see the CoreWall wiki for more
	// information.
	public int loadData(final File filename, final boolean useSAX) {
		final JProgressBar pb = view.getProgressUI();

		if (pb != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					pb.setStringPainted(true);
					pb.setIndeterminate(true);
					pb.setString("Loading dataset file: " + filename.getName());
				}
			});
		}

		// Check the data file format before really loading
		if (filename == null || filename.getName().equals("")) {
			if (pb != null) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						pb.setIndeterminate(false);
					}
				});
			}

			return -1;
		}

		// Only check XML now
		try {
			DOMParser parser = new DOMParser();
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);
			parser.parse(filename.getAbsolutePath());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(CorelyzerApp.getApp().getMainFrame(), "Dataset file loading error\n" + e);

			if (pb != null) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						pb.setIndeterminate(false);
					}
				});
			}

			return -1;
		}

		// Add dataset to corelyzer.helper.SceneGraph(C-side)
		int dsId = SceneGraph.addDataset(filename.getName());

		// Java side
		WellLogDataSet dataset;

		long before = new Date().getTime();
		{
			if (useSAX) {
				System.out.println("- Loading dataset using SAX");
				dataset = new SAXWellLogDataSet(filename.toString());
			} else {
				System.out.println("- Loading dataset using DOM");
				dataset = new WellLogDataSet(filename.toString());
			}
		}
		long after = new Date().getTime();
		System.out.println("- Loading " + filename.getName() + " data took: " + (after - before) / 1000.0f + " seconds.");

		dataset.setId(dsId);
		this.syncDatasetToSceneGraph(dataset);

		Session s = cg.getCurrentSession();
		if (s == null) {
			s = new Session("Default");
			cg.addSession(s);
		}
		cg.addDataset(s, dataset);

		view.getSessionList().setSelectedIndex(cg.getCurrentSessionIdx());
		view.getDataFileList().setSelectedIndex(cg.getCurrentDatasetIdx());

		System.out.println("---> [INFO] A new Dataset[" + filename.toString() + "] added with SceneGraph id " + dsId);

		if (pb != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					pb.setIndeterminate(false);
					pb.setString("Dataset '" + filename.getName() + "' loaded.");
				}
			});
		}

		return dsId;
	}

	public int loadImage(final File filename, final String URL) {
		return this.loadImage(filename, URL, null);
	}

	public int loadImage(final File filename, final String URL, String aName) {
		if (aName == null || aName.equals("")) {
			// let's use filename only w/o extension
			String str = filename.getName();
			int idx = str.lastIndexOf('.');
			str = str.substring(0, idx);
			aName = str;
		}

		// Get TrackSceneNode with its name
		CRDefaultListModel trackListModel = listModels.getListModel(CRListModels.TRACK);
		CRDefaultListModel sectionsListModel = listModels.getListModel(CRListModels.SECTION);

		int selectedIndex = view.trackList.getSelectedIndex(); // FIXME -1?
		TrackSceneNode t = (TrackSceneNode) trackListModel.elementAt(selectedIndex);

		if (t == null) {
			return -1;
		}

		// load the file, build the textures, build the model,
		// add to scene

		// 2-steps image loading snippet
		int secid = -1;
		int id;
		SceneGraph.imageLock();
		{
			int genblockStatus = SceneGraph.genTextureBlocks(filename.toString());

			if (genblockStatus == -1) {
				String mesg = "Unknown file format: '" + filename + "'";
				JOptionPane.showMessageDialog(view.getMainFrame(), mesg);
				System.err.println("---> [INFO] Generate texture failed!");

				SceneGraph.imageUnlock();
				return -1;
			}

			SceneGraph.lock();
			{
				id = SceneGraph.loadImage(filename.toString());
			}
			SceneGraph.unlock();
		}
		SceneGraph.imageUnlock();

		// Section: place holder
		CoreSection sec = t.getCoreSection(aName);
		if (sec == null) {
			// need to creat new section
			secid = SceneGraph.addSectionToTrack(t.getId(), sectionsListModel.size()); // fixme

			if (secid != -1) {
				SceneGraph.setSectionName(t.getId(), secid, aName);

				sec = new CoreSection(aName, secid);
				t.addCoreSection(sec);
			} else {
				return -1;
			}
		}

		if (URL == null) {
			SceneGraph.setImageURL(id, "unknown");
		} else {
			SceneGraph.setImageURL(id, URL);
		}

		// Image part
		if (id != -1) {
			secid = sec.getId();
			SceneGraph.addSectionImageToTrack(t.getId(), secid, id);

			CoreSectionImage node = new CoreSectionImage(t, filename.toString(), secid, aName);
			t.addChild(node, secid, id);
			t.Update();
			cg.notifyListeners();

			// broadcast the event to plugins

			String message = "";
			message = message + SceneGraph.getImageName(id);
			message = message + "\t" + SceneGraph.getImageURL(id);
			pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.IMAGE_LOADED, message);

			message = "";
			message = message + t.getId();
			message = message + "\t" + SceneGraph.getSectionSourceImage(t.getId(), secid);
			message = message + "\t" + SceneGraph.getSectionXPos(t.getId(), secid) / SceneGraph.getCanvasDPIX(0);
			message = message + "\t" + SceneGraph.getSectionYPos(t.getId(), secid) / SceneGraph.getCanvasDPIY(0);

			pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.SECTION_CREATED, message);

		}

		return secid;
	}

	// ---------------------------------------------------------------

	public void loadImageAction() {
		CRLoadImageDialog dialog = new CRLoadImageDialog(view.getMainFrame());
		dialog.pack();
		dialog.setLocationRelativeTo(view.getMainFrame());
		dialog.setVisible(true);
	}

	// --------------------------------------------------------------

	public void loadStateFile() {
		String selected = FileUtility.selectASingleFile(view.getMainFrame(), "Select a session file", "cml", FileUtility.LOAD);

		if (selected != null) {
			File f = new File(selected);

			if (f.exists()) {
				loadStateFile(selected);
			}
		}
	}

	public void loadStateFile(final String filename) {
		final File openFile = new File(filename);

		if (openFile.exists()) {
			final StateLoader stateLoader = new StateLoader();

			/*
			 * if(MAC_OS_X) { Runnable loading = new Runnable() { public void
			 * run () { stateLoader.loadState(openFile.getAbsolutePath()); } };
			 * (new Thread(loading)).start(); } else {
			 * stateLoader.loadState(openFile.getAbsolutePath()); }
			 */

			Runnable loading = new Runnable() {
				public void run() {
					stateLoader.loadState(openFile.getAbsolutePath());
				}
			};
			new Thread(loading).start();

			if (!openFile.getName().equals("previousSession.cml")) {
				Vector<String> hst = view.preferences().getSessionHistory();
				if (!hst.contains(openFile.getAbsolutePath())) {
					view.preferences().getSessionHistory().add(openFile.getAbsolutePath());
				}
			}

			refreshSessionHistoryMenu();

			// view.updateGLWindows();
		} else {
			JOptionPane.showMessageDialog(null, "The selected file does not exist");
		}

		view.setCurrentSessionFile(filename);
		CRPreferences.setCurrentDir(openFile.getParent());
	}

	// --------------------------------------------------------------------------
	// ---- Courtesy of Apple MacOSX "OSXAdapter" Example Code ----
	// Generic registration with the Mac OS X application menu.
	// Checks the platform, then attempts
	// to register with the Apple EAWT.
	// This method calls OSXAdapter.registerMacOSXApplication() and
	// OSXAdapter.enablePrefs().
	// See corelyzer.helper.OSXAdapter.java for the signatures of these methods.
	public void macOSXRegistration() {
		if (MAC_OS_X) {
			try {
				// Generate and register the OSXAdapter, passing it a hash of
				// all the methods we wish to
				// use as delegates for various
				// com.apple.eawt.ApplicationListener methods
				OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[]) null));
				OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", (Class[]) null));
				OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("doPreferences", (Class[]) null));

				OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("doAssociatedFile", new Class[] { String.class }));
			} catch (Exception e) {
				System.err.println("Error while loading the OSXAdapter:");
				e.printStackTrace();
			}
		}
	}

	public void onSubscribe() {
		if (view.isUsePluginUI()) {
			JFrame pluginFrame = view.getPluginFrame();

			if (pluginFrame instanceof SubscribeHandler) {
				((SubscribeHandler) pluginFrame).onSubscribe();
			}

			return;
		}

		String mesg = "Your current setup cannot handle subscribe requests.";
		JOptionPane.showMessageDialog(view.getMainFrame(), mesg);
	}

	private void processCARFile(final File selectedFile) {
		String dataRoot = view.preferences().getLocalRepositoryPath();

		FileInputStream fis;

		try {
			fis = new FileInputStream(selectedFile);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Could not open " + selectedFile + ": " + e);
			e.printStackTrace();
			return;
		}

		ZipInputStream zin = new ZipInputStream(new BufferedInputStream(fis));

		// Check...
		boolean isCRPackage = false;
		String proj_name = "";

		ZipEntry en;

		// ProgressDialog progress = new ProgressDialog();
		JProgressBar progress = view.getProgressUI();
		progress.setIndeterminate(true);
		progress.setString("Checking Package File...");
		// progress.setIndeterminant();
		// progress.setVisible(true);

		try {
			while ((en = zin.getNextEntry()) != null) {
				// check checking for session.cml for package signature
				String regexp = "\\\\|/";
				String[] toks = en.getName().split(regexp);

				if (toks[1].equals("session.cml")) {
					System.out.println("-- Found package signature!");
					isCRPackage = true;
					proj_name = toks[0];
					break;
				}
			}

			zin.close();
			fis.close();
		} catch (IOException e) {
			System.err.println("-- IOException in zip listing");
			return;
		}

		progress.setIndeterminate(false);

		if (isCRPackage) {
			// Prepare dirs
			String proj_dir_path = dataRoot + sp + proj_name;
			String img_dir_path = proj_dir_path + sp + "images";
			String data_dir_path = proj_dir_path + sp + "datasets";
			String anno_dir_path = proj_dir_path + sp + "annotations";
			String annoImg_dir_path = anno_dir_path + sp + "images";
			String annoAtt_dir_path = anno_dir_path + sp + "attachments";

			File proj_dir = new File(proj_dir_path);
			if (!proj_dir.exists()) {
				proj_dir.mkdir();
			}

			new File(img_dir_path).mkdir();
			new File(data_dir_path).mkdir();
			new File(anno_dir_path).mkdir();
			new File(annoImg_dir_path).mkdir();
			new File(annoAtt_dir_path).mkdir();

			// UNZIP the file to project dir
			progress.setIndeterminate(true);
			progress.setString("Uncompressing Package...");

			FileUtility.unzip(selectedFile, dataRoot);

			progress.setIndeterminate(false);
			progress.setString("Uncompressing package... done");
			// progress.setVisible(false);
			// progress.dispose();

			// locate session file
			String sessionFilePath = proj_dir_path + sp + "session.cml";

			// StateLoader with Prefix!
			System.out.println("---> StateLoader loading " + sessionFilePath + ", with " + proj_dir);

			StateLoader stateLoader = new StateLoader();
			stateLoader.loadState(sessionFilePath, proj_dir_path);

			view.updateGLWindows();

			// Write current state back, so reopen the session file should
			// be just fine
			StateWriter writer = new StateWriter();

			// FIXME wrt to A session per-file: something bad happens when
			// importing multiple packages etc
			writer.writeState(sessionFilePath);
		} else {
			System.err.println("-- Error: File selected is not a " + "Corelyzer package file");
			JOptionPane.showMessageDialog(view.getMainFrame(), "Error, file selected is not a Corelyzer package file");
		}
	}

	public void publishASession() {
		SessionSharingClientUI aShareClient = new SessionSharingClientUI();
		aShareClient.pack();
		aShareClient.setSize(600, 600);
		aShareClient.setAlwaysOnTop(true);
		aShareClient.setLocationRelativeTo(view.getMainFrame());
		aShareClient.setVisible(true);

		aShareClient.dispose();
	}

	public void quickDataImport() {
		String selected = FileUtility.selectASingleFile(view.getMainFrame(), "Choose a depth-value file", null, FileUtility.LOAD);

		if (selected != null) {
			File selectedFile = new File(selected);

			if (selectedFile.exists()) {
				CRDepthValueDataLoader dvLoader = new CRDepthValueDataLoader(selectedFile);
				dvLoader.load();
			}
		}
	}

	// --------------------------------------------------------------

	public void quit() {
		Object[] options = { "Save", "Don't Save", "Cancel" };

		int sel = JOptionPane.showOptionDialog(view.getMainFrame(), "Do you want to save your " + "session before leaving " + "Corelyzer?",
				"Exit Confirmation", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

		switch (sel) {
			case 0: // save to session file and quit
				if (saveStateToFile()) {
					cleanThingsUp();
					System.exit(0);
				}

				break;

			case 1: // don't save, just quit
				cleanThingsUp();
				System.exit(0);

			default:
				view.getMainFrame().setVisible(true);
				view.toolFrame.setVisible(true);
				view.toolFrame.setAppFrameSelected(true);

				// Hack way to stop quitting, only useful when Cmd+Q hit
				if (MAC_OS_X) {
					throw new IllegalStateException("Hack way to cancel quit");
				}
		}
	}

	public void refreshSessionHistoryMenu() {
		view.recentSessionsMenu.removeAll();

		// Show previous session if $(conf_dir)/previousSession.cml exists
		final String previousSession = view.preferences().config_Directory + sp + "previousSession.cml";
		final File prev = new File(previousSession);
		if (prev.exists()) {
			JMenuItem aItem = new JMenuItem("Previous Session");
			aItem.setToolTipText("Previous session before closing Corelyzer");
			aItem.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					System.out.println("---> Loading previous session from " + previousSession);
					if (prev.exists()) {
						loadStateFile(previousSession);
					}
				}
			});

			view.recentSessionsMenu.add(aItem);
			view.recentSessionsMenu.addSeparator();
		}

		// update history to view
		Vector<String> hst = view.preferences().getSessionHistory();
		Vector<String> listedPaths = new Vector<String>();
		int count = 0;

		for (int i = hst.size() - 1; i >= 0 && count < CRPreferences.maxHistoryEntries; i--, count++) {
			final String path = view.preferences().getSessionHistory().get(i);
			final String name = new File(path).getName();

			if (listedPaths.contains(path)) {
				continue;
			}
			listedPaths.add(path);

			JMenuItem aItem = new JMenuItem(name);
			aItem.setToolTipText(path);
			aItem.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					System.out.println("---> Loading " + path + " from history");

					if (new File(path).exists()) {
						loadStateFile(path);
					} else {
						view.getMainFrame().setAlwaysOnTop(false);
						JOptionPane.showMessageDialog(null, "The selected file does not exist anymore");
						view.getMainFrame().setAlwaysOnTop(true);
					}
				}
			});

			view.recentSessionsMenu.add(aItem);
		}

		view.recentSessionsMenu.addSeparator();

		// List auto-saved session files in autoSaves dir
		File confDir = new File(view.preferences().config_Directory + sp + "autoSaves");

		if (confDir.exists()) {
			File[] allFiles = confDir.listFiles();

			int autoSaveCount = 0;
			for (File f : allFiles) {
				if (f.isFile() && f.getName().toLowerCase().startsWith("autosave") && f.getName().toLowerCase().endsWith("cml")
						&& autoSaveCount < CRPreferences.maxHistoryEntries) {
					final String path = f.getAbsolutePath();
					final String name = f.getName();

					JMenuItem aItem = new JMenuItem(name);
					aItem.setToolTipText(path);
					aItem.addActionListener(new ActionListener() {
						public void actionPerformed(final ActionEvent e) {
							System.out.println("---> Loading " + path + " from autoSaved");

							if (new File(path).exists()) {
								loadStateFile(path);
							} else {
								view.getMainFrame().setAlwaysOnTop(false);
								JOptionPane.showMessageDialog(null, "The selected file does not exist.");
								view.getMainFrame().setAlwaysOnTop(true);
							}
						}
					});

					autoSaveCount++;
					view.recentSessionsMenu.add(aItem);
				}
			}
		}

		view.recentSessionsMenu.addSeparator();

		JMenuItem aItem = new JMenuItem("Clear List");
		aItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				int retVal = JOptionPane.showConfirmDialog(view.getMainFrame(), "Are you sure that you want to clear all " + "listed session files?",
						"Confirmation", JOptionPane.YES_NO_OPTION);

				if (retVal == JOptionPane.OK_OPTION) {
					view.preferences().getSessionHistory().clear();

					// clean autoSaved sessions
					File autoSaveDir = new File(view.preferences().config_Directory + sp + "autoSaves");

					if (autoSaveDir.exists() && autoSaveDir.isDirectory()) {
						for (File f : autoSaveDir.listFiles()) {
							if (f.getName().toLowerCase().endsWith(".cml")) {
								System.out.println("---> [INFO] Delete " + f);
								f.delete();
							}
						}
					}

					// Update view
					refreshSessionHistoryMenu();
				}
			}
		});

		view.recentSessionsMenu.add(aItem);
	}

	public void removeSession(final Session s) {
		String message = "Are you sure you want to close session: " + s.getName() + " ?";

		if (JOptionPane.showConfirmDialog(view.getMainFrame(), message) == JOptionPane.OK_OPTION) {

			// remove s.tracks
			java.util.Iterator<TrackSceneNode> tIter = s.getTrackSceneNodes().iterator();

			// Native
			// noinspection WhileLoopReplaceableByForEach
			while (tIter.hasNext()) {
				TrackSceneNode t = tIter.next();

				deleteTrack(s, t);
				// cg.removeTrack(s, t);
			}
			// Java
			cg.removeTracks(s);

			// remove s.datasets
			java.util.Iterator<WellLogDataSet> dIter = s.getDatasets().iterator();

			// Native
			// noinspection WhileLoopReplaceableByForEach
			while (dIter.hasNext()) {
				WellLogDataSet d = dIter.next();

				deleteDataset(s, d);
				// cg.removeDataset(s, d);
			}
			SceneGraph.resetDefaultTrackYPos();

			// Java
			cg.removeDatasets(s);
			cg.removeSession(s);
		}
	}

	// TODO PopupMenuControllers
	// sessionPopupMenu actions
	public void renameSession() {
		Session s = (Session) view.sessionList.getSelectedValue();

		String newName = JOptionPane.showInputDialog(view.getMainFrame(), "Please type the new session name", s.getName());

		if (newName == null) {

		} else if (newName.equals("")) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Session name can not be empty.");
		} else {
			s.setName(newName);
		}
	}

	public void renameTrack() {
		TrackSceneNode t = (TrackSceneNode) view.trackList.getSelectedValue();
		String trackName = t.toString();

		String currentSessionName = CoreGraph.getInstance().getCurrentSession().getName();
		int trackId = SceneGraph.getTrackIDByName(currentSessionName, trackName);
		String newName = JOptionPane.showInputDialog(view.getMainFrame(), "Please type the new track name", trackName);

		if (newName == null) {

		} else if (newName.equals("")) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Track name can not be empty.");
		} else {
			t.setName(newName);
			SceneGraph.renameTrack(trackId, newName);
		}
	}

	public boolean saveCurrentSession() {
		return saveStateToFile(view.getCurrentSessionFile());
	}

	// --------------------------------------------------------------
	public boolean saveOutputToFile() {
		// check if there exists section at this point
		CRDefaultListModel trackListModel = listModels.getListModel(CRListModels.TRACK);

		boolean hasSection = false;
		for (int i = 0; i < trackListModel.size(); i++) {
			TrackSceneNode t = (TrackSceneNode) trackListModel.elementAt(i);

			if (t.getNumCores() > 0) {
				hasSection = true;
				break;
			}
		}

		if (!hasSection) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Nothing to Output");
			return false;
		}

		SheetOutputDialog dialog = new SheetOutputDialog(view.getMainFrame());
		dialog.pack();
		dialog.setSize(320, dialog.getHeight());
		dialog.setLocationRelativeTo(view.getMainFrame());

		view.getMainFrame().setAlwaysOnTop(false);
		dialog.setVisible(true);
		view.getMainFrame().setAlwaysOnTop(true);

		return true;
	}

	public boolean saveStateToFile() {
		return saveStateToFile(null);
	}

	private boolean saveStateToFile(final String aFilePath) {
		// only save selected sessions
		SessionsSelectDialog s = new SessionsSelectDialog(view.getMainFrame());
		s.pack();
		s.setLocationRelativeTo(view.getMainFrame());
		s.setVisible(true);

		boolean[] isSelected = s.getSelectedIndex();
		s.dispose();

		if (isSelected == null) {
			return false;
		}

		String selected;
		if (aFilePath == null || aFilePath.equals("")) {
			String title = "Save a Session file";
			selected = FileUtility.selectASingleFile(view.getMainFrame(), title, "cml", FileUtility.SAVE);
		} else {
			selected = aFilePath;
		}

		if (selected != null) {
			// make sure it has .xml at the end
			String path = selected.replace('\\', '/');
			String[] toks = path.split("/");
			if (!toks[toks.length - 1].contains(".cml")) {
				path = path + ".cml";
			}

			StateWriter sw = new StateWriter(isSelected);
			boolean writeResult = sw.writeState(path);

			if (!writeResult) {
				JOptionPane.showMessageDialog(view.getMainFrame(), "Failed");
			}

			// Save to history vector
			Vector<String> hst = view.preferences().getSessionHistory();
			if (!hst.contains(path)) {
				view.preferences().getSessionHistory().add(path);
			}
			this.refreshSessionHistoryMenu();

			if (writeResult) {
				view.setCurrentSessionFile(new File(selected).getName());
			}

			return writeResult;
		}

		return false;
	}

	// sectionPopupMenu actions
	public void sectionProperties(final int[] rows) {
		int selectedTrackIdx = view.trackList.getSelectedIndex();

		if (selectedTrackIdx < 0 || rows.length <= 0) {
			return;
		}

		CRDefaultListModel trackListModel = view.getTrackListModel();
		CRDefaultListModel sectionsListModel = view.getSectionListModel();

		TrackSceneNode t = (TrackSceneNode) trackListModel.getElementAt(selectedTrackIdx);

		// build native section id array
		int[] sectionIds = new int[rows.length];
		for (int i = 0; i < rows.length; i++) {
			int row = rows[i];
			CoreSection cs = (CoreSection) sectionsListModel.getElementAt(row);

			if (cs != null) {
				sectionIds[i] = cs.getId();
			} else {
				System.err.println("---> [WARN] CoreSection is null " + "in setctionProperties.");
			}
		}

		// bring up dialog interface
		SectionImagePropertyDialog dialog = new SectionImagePropertyDialog(view.getMainFrame());
		dialog.setMultiProperties(t.getId(), sectionIds);
		dialog.pack();
		dialog.setLocationRelativeTo(view.getMainFrame());
		dialog.setVisible(true);
		dialog.dispose();
	}

	// sectionSplit actions
	public void sectionSplit() {
		int selectedTrackIdx = view.getTrackList().getSelectedIndex();
		int selectedSectionIdx = view.getSectionList().getSelectedIndex();

		if (selectedTrackIdx < 0 || selectedSectionIdx < 0) {
			return;
		}

		CRDefaultListModel trackListModel = view.getTrackListModel();
		CRDefaultListModel sectionsListModel = view.getSectionListModel();

		TrackSceneNode t = (TrackSceneNode) trackListModel.getElementAt(selectedTrackIdx);
		CoreSection cs = (CoreSection) sectionsListModel.getElementAt(selectedSectionIdx);

		int imageId = SceneGraph.getImageIdForSection(t.getId(), cs.getId());

		int csImgId = -1;
		if (cs.hasImage()) {
			csImgId = cs.getCoreSectionImage().getId();
		}

		// Duplicate select section and sample texture model in native
		// scenegraph
		ImageSplitDialog dialog = new ImageSplitDialog(view.getMainFrame());
		dialog.setSectionName(cs.getName());
		dialog.setProperties(t.getId(), cs.getId());

		dialog.pack();
		dialog.setLocationRelativeTo(view.getMainFrame());
		dialog.setVisible(true);
		dialog.dispose();
	}

	public void selectTrackByNativeTrackID(int track) {
		if (track >= 0) {
			// Now, we need to traverse app's list model
			// to find match of native id
			// index conversion (native to java list)
			int i, tsize;
			boolean found = false;
			CRDefaultListModel tmodel = listModels.getListModel(CRListModels.TRACK);
			tsize = tmodel.getSize();
			TrackSceneNode tt;

			for (i = 0; i < tsize; i++) {
				tt = (TrackSceneNode) tmodel.elementAt(i);

				if (track == tt.getId()) {
					track = i;
					found = true;
					break;
				}
			}

			if (!found) {
				return;
			}

			// update ui
			CorelyzerApp.getApp().getTrackList().setSelectedIndex(track);
		}
	}

	// Sets the source URL from where a dataset in the
	// corelyzer.helper.SceneGraph system
	// comes from. RECOMMEND using the corelyzer.helper.SceneGraph version of
	// this method.
	public void setDatasetURL(final int dataset, final String URL) {
		SceneGraph.setDatasetURL(dataset, URL);
	}

	public void setSelectedTrack(final int index) {
		CRDefaultListModel trackListModel = listModels.getListModel(CRListModels.TRACK);

		if (index < 0 || index > trackListModel.size() - 1) {
			return;
		}
		view.trackList.setSelectedIndex(index);
	}

	public void setSelectedTrack(final String trackname) {
		CRDefaultListModel trackListModel = listModels.getListModel(CRListModels.TRACK);

		if (!containsTrackName(trackname)) {
			return;
		}

		for (int i = 0; i < trackListModel.size(); i++) {
			TrackSceneNode t = (TrackSceneNode) trackListModel.elementAt(i);

			if (t.getName().equals(trackname)) {
				view.trackList.setSelectedIndex(i);
				return;
			}

		}
	}

	public void setSessionVisible(final boolean b) {
		Session s = (Session) view.sessionList.getSelectedValue();
		s.setShow(b);

		for (TrackSceneNode t : s.getTrackSceneNodes()) {
			int trackId = t.getId();
			SceneGraph.setTrackShow(trackId, b);
		}

		view.updateGLWindows();
	}

	// trackPopupMenu actions
	public void setTrackVisible() {
		TrackSceneNode t = (TrackSceneNode) view.trackList.getSelectedValue();

		if (t.getId() >= 0) {
			boolean isShown = SceneGraph.getTrackShow(t.getId());
			SceneGraph.setTrackShow(t.getId(), !isShown);

			// view.getTrackList().getSelectedValue();

			view.updateGLWindows();
		}
	}

	// enabled by file menu
	public void showGraphDialog() {
		Session s = view.getSelectedSession();
		Vector<WellLogDataSet> datasets = s.getDatasets();

		if (datasets.size() <= 0) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Please load at least a dataset first");

			return;
		}

		// set and show DrawGraphDialog
		CRDefaultListModel model = listModels.getListModel(CRListModels.TRACK);
		if (model.isEmpty()) {
			JOptionPane.showMessageDialog(view.getMainFrame(), "Please create at least a track first");

			return;
		}

		int t = view.getTrackList().getSelectedIndex();
		int d = view.getDataFileList().getSelectedIndex();

		// GraphDialog graphDialog = new GraphDialog("Graph Dialog");
		CRGraphDialog graphDialog = new CRGraphDialog(this.view.getMainFrame());
		graphDialog.setSelectedTrackListId(t);
		graphDialog.setDatasetVec(datasets);
		graphDialog.selectDataset(d);
		graphDialog.pack();
		graphDialog.setSize(480, 480);
		graphDialog.setVisible(true);
	}

	public void startup() {
		pluginManager.startup();

		// Auto session saving thread
		// noinspection ConstantConditions
		Thread autoSave = new Thread() {
			StateWriter writer;

			public void run() {
				while (true) {
					// Auto-save session every 60 secons
					try {
						if (!autoSavePath.contains(view.preferences().config_Directory)) {
							File autoSaveDir = new File(view.preferences().config_Directory + sp + "autoSaves");

							boolean isCreated = true;
							if (!autoSaveDir.exists()) {
								isCreated = autoSaveDir.mkdir();
							}

							if (isCreated) {
								autoSavePath = view.preferences().config_Directory + sp + "autoSaves" + sp + autoSavePath;
							} else {
								System.out.println("---> [WARNING] Cannot " + "create autoSaves dir, break");
								break;
							}

						}

						if (writer == null) {
							writer = new StateWriter();
						}

						writer.writeState(autoSavePath);

						Thread.sleep(60 * 1000); // save every minute
					} catch (Exception e) {
						System.err.println("---> [EXCEPTION] In Thread sleep: " + e);
						e.printStackTrace();
					}
				}
			}
		};
		autoSave.start();

		this.initBonjour();
		this.initRemoteControlServer();
		this.initAnnotationTypeDirectory();
	}

	private void syncDatasetToSceneGraph(final WellLogDataSet ds) {
		System.out.println("---- Sync dataset ----");

		int numOfTables = ds.getNumTables();

		for (int tableId = 0; tableId < numOfTables; tableId++) {
			WellLogTable t = ds.getTable(tableId);

			SceneGraph.addTable(ds.getId(), t.getName());
			SceneGraph.setTableHeightAndFieldCount(ds.getId(), tableId, t.getNumRows(), t.getNumFields());

			float scale = 1.0f;
			int unit = t.getDepthUnits();

			if (unit == UnitLength.CM) {
				scale = 1.0f;
			} else if (unit == UnitLength.M) {
				scale = 100.0f;
			} else if (unit == UnitLength.MM) {
				scale = 1 / 10.0f;
			} else if (unit == UnitLength.INCH) {
				scale = 2.54f;
			} else if (unit == UnitLength.FOOT) {
				scale = 30.48f;
			} else if (unit == UnitLength.YARD) {
				scale = 91.44f;
			}

			SceneGraph.setTableDepthUnitScale(ds.getId(), tableId, scale);

			// System.out.println("-- corelyzer.data.Table name[" + t.getName()
			// +
			// "] id[" + tableId +
			// "] Rows[" + t.getNumRows() +
			// "] Fields[" + t.getNumFields() +
			// "] added");

			float currentDepth = 0.0f;
			float value; // / = 0.0f;
			boolean isValid; // / = false;

			// TODO Rearrange loops to column then row
			for (int r = 0; r < t.getNumRows(); r++) {
				for (int c = 0; c < t.getNumColumns(); c++) {
					if (c == 0) { // WellTable's column 0 is depth value
						currentDepth = t.getCell(r, c);
					} else {
						value = t.getCell(r, c);
						isValid = t.isCellValid(r, c);

						SceneGraph.setTableCell(ds.getId(), tableId, (c - 1), r, isValid, currentDepth, value);

						/*
						 * float jccValue =
						 * corelyzer.helper.SceneGraph.getTableCell(ds.getId(),
						 * tableId, (c-1), r); if( value != jccValue ) {
						 * System.out.println("JCC: " + value + ", " +
						 * jccValue); }
						 */
					}
				}// end-for-c
			}// end-for-r

			// TODO Merge with upper rearranged loops
			for (int c = 0; c < t.getNumColumns() - 1; c++) {
				float min = t.getColumnMin(c);
				float max = t.getColumnMax(c);
				SceneGraph.setFieldMinMax(ds.getId(), tableId, c, min, max);
			}
		}// end-for-i

		System.out.println("---- Sync End ----");
		// TODO should also get rid of WellData here, but maybe keep some
		// small info like min/max, view_min/view_max etc.
		/*
		 * System.out.println("---- C-side, dataset[" + ds.getId() + "] has " +
		 * corelyzer.helper.SceneGraph.getNumberOfTables(ds.getId()) +
		 * " tables");
		 * 
		 * System.out.println("---- C-side, table[0] has " +
		 * corelyzer.helper.SceneGraph.getTableHeight(ds.getId(), 0) + " rows");
		 */
	}

	// --------------------------------------------------------------
	/** Just for test and debugging */
	public void testAndMatch() {
		// Test if Java-side data structure match with C-side
		// Access Java side starting from: WellLogDataset
		// C side starting from: corelyzer.helper.SceneGraph

		// Use current_datasets
		CoreGraph cg = CoreGraph.getInstance();
		int jdsSize = cg.getCurrentSession().getNumberOfDatasets();

		// int cDSSize = corelyzer.helper.SceneGraph.getNumberOfDatasets();
		System.out.println("\n----------------------------------------\n");
		System.out.println("---- Java side: We have [" + jdsSize + "] datasets");

		boolean isMatch = true;
		for (int i = 0; i < jdsSize; i++) {
			// Use current_datasets
			WellLogDataSet ds = cg.getDataset(cg.getCurrentSessionIdx(), i);
			int jnumTables = ds.getNumTables();
			int cnumTables = SceneGraph.getNumberOfTables(i);

			System.out.println("Dataset [" + i + "] has [Java:" + jnumTables + "][C:" + cnumTables + "] tables");

			for (int j = 0; j < jnumTables; j++) {
				WellLogTable t = ds.getTable(j);
				/*
				 * Not used int jfields = t.getNumFields(); int cfields =
				 * SceneGraph.getTableFields(i, j);
				 * 
				 * int jrows = t.getNumRows(); int crows =
				 * SceneGraph.getTableHeight(i, j);
				 */

				/*
				 * System.out.println("DS[" + i + "], Tab[" + j + "]");
				 * System.out.println("[J-f:" + jfields + "][C-F: " + cfields +
				 * "]"); System.out.println("[J-R:" + jrows + "][C-R:" + crows +
				 * "]");
				 */

				// Do compare with C-side cell by cell
				float currentDepth = 0.0f;
				for (int r = 0; r < t.getNumRows(); r++) {
					for (int c = 0; c < t.getNumColumns(); c++) {
						if (c == 0) { // column 0 is depth value
							// Get depth value from table entries
							currentDepth = t.getCell(r, c);
						} else {
							boolean jIsValid = t.isCellValid(r, c);
							float jValue = t.getCell(r, c);
							// / float jDepth = currentDepth;
							// t.getDepth(r); // or t.getCell(r, 0);

							boolean cIsValid = SceneGraph.isTableCellValid(i, j, c - 1, r);
							float cValue = SceneGraph.getTableCell(i, j, c - 1, r);
							float cDepth = SceneGraph.getTableCellDepth(i, j, c - 1, r);

							if (jIsValid != cIsValid || jValue != cValue || currentDepth != cDepth) // (jDepth
																									// !=
																									// cDepth))
							{
								System.out.println("[Not Match] Table[" + j + "]");
								System.out.println("[Not Match] " + r + ", " + c + ", " + jValue + ", " + cValue);

								isMatch = false;
							}
						}
					}
				}

			}// end-j
		}// end-i

		if (isMatch) {
			System.out.println("[!!!] Congrats! It Matches!");
		} else {
			System.out.println("[!!!] Oops! It doesn't match!");
		}
	}

}
