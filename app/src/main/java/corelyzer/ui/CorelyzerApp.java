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

package corelyzer.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.help.CSH;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.install4j.api.launcher.StartupNotification;

import corelyzer.controller.CRExperimentController;
import corelyzer.data.CRPreferences;
import corelyzer.data.CoreSection;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.dis.DISListsDialog;
import corelyzer.data.lims.IODPListsDialog;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.data.lists.CRListModels;
import corelyzer.graphics.SceneGraph;
import corelyzer.handlers.ProgressHandler;
import corelyzer.helper.FilenameExtensionFilter;
import corelyzer.io.StateLoader;
import corelyzer.plugin.CorelyzerPlugin;
import corelyzer.plugin.CorelyzerPluginManager;
import corelyzer.ui.annotation.CRNavigationSetupDialog;
import corelyzer.ui.annotation.clast.ClastStatisticsDialog;
import corelyzer.ui.annotation.freeform.FreeformAnnotationListDialog;
import corelyzer.ui.annotation.sampling.SampleRequestListDialog;

public class CorelyzerApp extends WindowAdapter implements MouseListener, StartupNotification.Listener {
	static boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

	final static int MENU_MASK = MAC_OS_X ? Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() : ActionEvent.CTRL_MASK;

	private static void addAllPlugins(final File file, final java.util.List<String> plugins) {
		if (file.exists()) {
			String[] allJarFiles = file.list(new FilenameExtensionFilter("jar"));

			for (String filename : allJarFiles) {
				String pluginName = filename.substring(0, filename.length() - 4);
				plugins.add(pluginName);
				System.out.println("-- [Found Plugin] " + pluginName);
			}
		}
	}

	// ====================================================================
	private static CRPreferences handlePreferences() {
		CRPreferences prefs = new CRPreferences();

		boolean hasConfDir = false;
		boolean hasDirConf = false;
		boolean hasDirs = true;
		boolean hasDisplayConf = false;
		boolean hasUIConf = false;

		// Create Corelyzer config dir if there's none.
		String sp = System.getProperty("file.separator");
		File conf_dir = new File(prefs.config_Directory);
		File data_dir = new File(prefs.datastore_Directory);
		File anno_dir = new File(prefs.annotation_Directory);

		if (!conf_dir.exists()) {
			conf_dir.mkdir();
		} else {
			hasConfDir = true;
		}

		if (!data_dir.exists()) {
			data_dir.mkdir();
		}

		if (!anno_dir.exists()) {
			anno_dir.mkdir();
		}
		new File(prefs.annotation_Directory + sp + "attachments").mkdir();

		// Load directory config if there is a config file available
		File dirs_config = new File(prefs.config_Directory + "/directories.txt");

		if (dirs_config.exists()) {
			hasDirConf = prefs.readDirectoryConfig(dirs_config);
		} else { // no dir setup, so create default ones
			File cache_dir = new File(prefs.cache_Directory);
			File imgb_dir = new File(prefs.texBlock_Directory);
			File down_dir = new File(prefs.download_Directory);
			File tmp_dir = new File(prefs.tmp_Directory);

			if (!cache_dir.exists()) {
				System.out.println("-- [INFO] Create cache dir: " + cache_dir);
				cache_dir.mkdir();
			}

			if (!imgb_dir.exists()) {
				System.out.println("-- [INFO] Create imgblock dir: " + imgb_dir);
				imgb_dir.mkdir();
			}

			if (!down_dir.exists()) {
				System.out.println("-- [INFO] Create download dir: " + down_dir);
				down_dir.mkdir();
			}

			if (!tmp_dir.exists()) {
				System.out.println("-- [INFO] Create temp dir: " + tmp_dir);
				tmp_dir.mkdir();
			}
		}

		// Load display config if there is a config file available
		File display_config = new File(prefs.config_Directory + "/display.conf");
		if (display_config.exists()) {
			hasDisplayConf = prefs.readDisplayConfig(display_config);
		}

		// Load UI config if there is a config file available
		File ui_config = new File(prefs.config_Directory + "/ui.conf");
		if (ui_config.exists()) {
			hasUIConf = prefs.readUIConfig(ui_config);
		}

		// Check what's missing & invoke preference dialog accordingly

		if (!hasConfDir || !(hasDirConf && hasDisplayConf && hasUIConf) || !hasDirs) {
			CRPreferencesDialog prefDialog = new CRPreferencesDialog(null, prefs);
			prefDialog.pack();
			prefDialog.setSize(450, 750);

			Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
			int loc_x = scrnsize.width / 2 - prefDialog.getSize().width / 2;
			int loc_y = scrnsize.height / 2 - prefDialog.getSize().height / 2;
			prefDialog.setLocation(loc_x, loc_y);
			prefDialog.setAlwaysOnTop(true);

			int mode = 0;
			if (!hasConfDir) {
				mode = 0;
			} else if (!hasDirConf || !hasDirs) {
				mode = 1;
			} else if (!hasDisplayConf) {
				mode = 2;
			} /*
			 * else if (!hasUIConf) { mode = 3; }
			 */

			prefDialog.start(mode, prefs);
			prefDialog.setVisible(true);

			prefs = prefDialog.getPreferences();
			prefDialog.dispose();
		} else {
			prefs.isInited = true;
		}

		if (prefs != null) {
			prefs.isInited = true;
		}

		return prefs;
	}

	private static String[] initPlugins() {
		Vector<String> plugins = new Vector<String>();
		addAllPlugins(new File("plugins"), plugins);
		addAllPlugins(new File("../plugins"), plugins);

		return plugins.toArray(new String[plugins.size()]);
	}

	private String currentSessionFile = "";

	String fileassociation = "";

	// SceneGraph data strcture
	CRPreferences preferences;
	boolean isGLInited = false;
	private static CorelyzerApp app;
	private GLContext baseContext;

	private Vector<CorelyzerGLCanvas> canvasVec;
	private Vector<Window> windowVec;

	Vector<JMenuItem> pluginMenuItemVec;
	// controller
	CorelyzerAppController controller;

	String[] plugins;
	// view
	// GUI components
	boolean usePluginUI = false;

	int pluginUIIndex = -1;
	JFrame mainFrame;
	JPanel rootPanel;
	JList sessionList;
	JList trackList;

	JList sectionList;
	JList dataFileList;
	JList fieldList;

	JMenu friendsMenu;

	JMenu pluginMenu;
	JMenu recentSessionsMenu;
	JMenuBar menuBar;
	// File Menu
	JMenuItem createTrackMenuItem;

	JMenuItem loadDataMenuItem;

	JMenuItem loadStateFileMenuItem;
	JMenuItem loadImageMenuItem;

	// sessionList PopupMenu
	JPopupMenu sessionPopupMenu;
	// trackList PopupMenu
	JPopupMenu trackPopupMenu;

	JMenuItem trackPopupDeleteMenuItem;
	// dataSet PopupMenu
	JPopupMenu dataPopupMenu;
	JMenuItem dataPopupGraphMenuItem;
	CRToolPalette toolFrame;
	static final int APP_NORMAL_MODE = 0;
	static final int APP_MEASURE_MODE = 1;

	static final int APP_MARKER_MODE = 2;

	static final int APP_CLAST_MODE = 3;

	static final int APP_CUT_MODE = 4;

	// Returns the single instance of the CorelyzerApp class
	public static CorelyzerApp getApp() {
		return CorelyzerApp.app;
	}

	/**
	 * Entry point of the application. Initialized the application, starts up
	 * the corelyzer.helper.SceneGraph system, and setups up last known
	 * settings.
	 * 
	 * @param args
	 *            Plugin names and input session file, if there's any
	 */
	public static void main(final String[] args) {
		CRPreferences prefs = handlePreferences();

		// If somehow the user click on 'Cancel' in their first run, just quit.
		if (prefs == null) {
			System.exit(0);
		}

		// Test Quaqua Look and Feel
		/*
		 * if (prefs.getUseQuaqua()) {
		 * System.setProperty("Quaqua.tabLayoutPolicy", "wrap");
		 * 
		 * try { UIManager.setLookAndFeel(
		 * "ch.randelshofer.quaqua.QuaquaLookAndFeel"); } catch (Exception e) {
		 * System.err.println("Exception in setting LAF: " + e); } }
		 */

		String[] plugins = initPlugins();

		CorelyzerApp myApp = new CorelyzerApp(plugins, prefs);
		// CRNotificationPrompt.notifyGrowl("Execution", "Status",
		// "Corelyzer Started");

		if (prefs.getAutoCheckVersion()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					AboutDialog aboutDlg = new AboutDialog();
					aboutDlg.checkUpdateAction();
					aboutDlg.dispose();
				}
			});
		}

		if (myApp.preferences.isInited) {
			myApp.startup();
		}

		// apply preferences
		SceneGraph.startUp();
		SceneGraph.setTexBlockDirectory(myApp.preferences.texBlock_Directory);

		// apply ui preferences
		myApp.preferences.applyUIConfig();

		// file association
		// on windows when app start with associated file
		boolean bOpenFile = false;
		if (myApp.fileassociation.length() == 0) {
			for (int i = 0; i < args.length; i++) {
				System.out.println("app args[" + i + "]:" + args[i]);
				if (args[i].toLowerCase().endsWith(".cml")) {
					myApp.fileassociation = args[i];
					bOpenFile = true;
				}
			}
		} else {
			bOpenFile = true;
		}

		// load associated file if app start with it: windows only
		if (bOpenFile) {
			StateLoader stateLoader = new StateLoader();
			stateLoader.loadState(myApp.fileassociation);
			myApp.updateGLWindows();
		}
	}

	// Reuse helpAction in main frame and tool palette
	ActionListener helpActionListener = null;

	// Constructor. Accepts a listing of strings to plugin objects to
	// load up and incorporate into the system. This method, creates objects,
	// the base OpenGL context shared among contexts created later,
	// and brings up the display configuration dialog.
	public CorelyzerApp() {
		super();
	}

	public CorelyzerApp(final String[] plugins) {
		this(plugins, new CRPreferences());
	}

	public CorelyzerApp(final String[] plugins, final CRPreferences prefs) {
		this();

		this.plugins = plugins;
		controller = new CorelyzerAppController(this);

		// create UI components
		app = this;
		setPreferences(prefs);
		setupUI();

		controller.startup();

		// create an initial OpenGL context!!
		// create our default gljpanel for all panels to share with it's
		// context, don't attach it to anything

		GLCanvas basePanel = new GLCanvas();
		canvasVec = new Vector<CorelyzerGLCanvas>();
		windowVec = new Vector<Window>();

		baseContext = basePanel.getContext();

		controller.macOSXRegistration();

		StartupNotification startupNotify = new StartupNotification();
		// noinspection AccessStaticViaInstance
		StartupNotification.registerStartupListener(this);
	}

	public boolean containsDatasetName(final String aName) {
		return controller.containsTrackName(aName);
	}

	public boolean containsTrackName(final String aName) {
		return controller.containsTrackName(aName);
	}

	/**
	 * Called by the DisplayConfiguration dialog class to begin the creation of
	 * the OpenGL windows given previously set parameters of rows and columns of
	 * monitors, and the properties of each monitor
	 */
	public void createGLWindows() {
		int nrows = preferences.numberOfRows;
		int ncols = preferences.numberOfColumns;
		int tileWidth = preferences.screenWidth;
		int tileHeight = preferences.screenHeight;

		float borderLeft = preferences.borderLeft;
		float borderRight = preferences.borderRight;
		float borderDown = preferences.borderDown;
		float borderUp = preferences.borderUp;

		float screenDpiX = preferences.dpix;
		float screenDpiY = preferences.dpiy;

		int row_offset, column_offset;

		try {
			row_offset = Integer.parseInt(preferences.getProperty("display.row_offset"));
			column_offset = Integer.parseInt(preferences.getProperty("display.column_offset"));
		} catch (NumberFormatException e) {
			row_offset = 0;
			column_offset = 0;
		}

		SceneGraph.setCanvasRowcAndColumn(nrows, ncols);

		int r, c;
		for (r = 0; r < nrows; r++) {
			for (c = 0; c < ncols; c++) {
				// System.out.println("Making tile " + r + ", " + c);
				Window win;
				GLCanvas cvs;
				CorelyzerGLCanvas cglc;
				float px, py;
				int id;

				// Allow alpha GL context
				GLCapabilities cap = new GLCapabilities();
				cap.setAlphaBits(8);
				// System.out.println("---> GL " + cap.toString());

				/*
				 * if(MAC_OS_X) { win = new JFrame(); ((JFrame)
				 * win).setUndecorated(true); } else { win = new JWindow(); }
				 */

				win = new JFrame();
				((JFrame) win).setUndecorated(true);

				win.setLocation(c * tileWidth + column_offset, r * tileHeight + row_offset);
				cvs = new GLCanvas(cap, null, baseContext, null);
				win.add(cvs);
				win.addWindowFocusListener(new WindowFocusListener() {
					public void windowGainedFocus(final WindowEvent event) {
						// do nothing
					}

					public void windowLostFocus(final WindowEvent event) {
						String isCanvasAlwaysBelow = preferences.getProperty("ui.canvas.alwaysBelow");

						boolean b;
						try {
							b = Boolean.parseBoolean(isCanvasAlwaysBelow);
						} catch (Exception e) {
							b = true;
						}

						if (b) {
							GLWindowsToBack();
						}
					}
				});
				windowVec.add(win);

				px = tileWidth * c + (borderLeft + borderRight) * screenDpiX * c;
				py = tileHeight * r + (borderUp + borderDown) * screenDpiY * r;

				id = SceneGraph.genCanvas(px, py, tileWidth, tileHeight, screenDpiX, screenDpiY);

				cglc = new CorelyzerGLCanvas(cvs, tileWidth, tileHeight, px, py, id);
				canvasVec.add(cglc);

				// if it's the bottom most screen or the first column,
				// then mark to draw depth scale
				if (c == 0) {
					SceneGraph.setCanvasFirstColumn(cglc.getCanvasID(), true);
				}

				if (r == nrows - 1) {
					SceneGraph.setCanvasBottomRow(cglc.getCanvasID(), true);
				}

				win.pack();
				win.setVisible(true);
				win.toBack();
				// win.show();
			}
		}

		createTrackMenuItem.setEnabled(true);
		loadDataMenuItem.setEnabled(true);
		loadStateFileMenuItem.setEnabled(true);

		isGLInited = true;
	}

	// Called by the PluginManager to create the menu items to
	// select which plugin window to pull up
	public void createPluginMenuItems(final Vector pluginNames) {
		JMenuItem pluginMenuItem;
		for (int k = 0; k < pluginNames.size(); k++) {
			pluginMenuItem = new JMenuItem((String) pluginNames.elementAt(k));
			pluginMenuItem.addActionListener(controller);
			pluginMenu.add(pluginMenuItem);
			pluginMenuItemVec.add(pluginMenuItem);

			if (k == getPluginUIIndex()) {
				pluginMenuItem.setEnabled(false);
			}
		}
	}

	// delegate methods to controller
	public void createSession(final String aName) {
		controller.createSession(aName);
	}

	public int createTrack(final String aName) {
		return controller.createTrack(aName);
	}

	public void deleteSection(final int trackId, final int sectionId) {
		controller.deleteSection(trackId, sectionId);
	}

	public void deleteTrack(final String aName) {
		controller.deleteTrack(aName);
	}

	/**
	 * Called withing the system to destroy the OpenGL windows. WARNING!!!
	 * Should not be called by anyone.
	 */
	public void destroyGLWindows() {
		SceneGraph.destroyCanvases();
		canvasVec.clear();
		windowVec.clear();
		isGLInited = false;
	}

	// Returns the thickness of lower part of each monitor in inches.
	public float getBorderDown() {
		return preferences.borderDown;
	}

	// Returns the thickness of left part of each monitor in inches.
	public float getBorderLeft() {
		return preferences.borderLeft;
	}

	// Returns the thickness of right part of each monitor in inches.
	public float getBorderRight() {
		return preferences.borderRight;
	}

	// Returns the thickness of upper part of each monitor in inches.
	public float getBorderUp() {
		return preferences.borderUp;
	}

	// Returns the Vector object holding references to the CorelyzerGLCanvases
	public Vector<CorelyzerGLCanvas> getCanvasVec() {
		return canvasVec;
	}

	public CorelyzerAppController getController() {
		return controller;
	}

	public String getCurrentSessionFile() {
		return currentSessionFile;
	}

	// Returns the JList object holding the datafile names
	public JList getDataFileList() {
		return dataFileList;
	}

	// Returns the DefaultListModel of the datafile list object
	public CRDefaultListModel getDataFileListModel() {
		return getListModel(CRListModels.DATASET);
	}

	public JFrame getDefaultMainFrame() {
		return mainFrame;
	}

	public String getDownloadDirectoryPath() {
		return controller.getDownloadDirectoryPath();
	}

	// Returns the JList object holding the datafile's field names
	public JList getFieldList() {
		return fieldList;
	}

	// Returns the DatasetRepository object that helps to keep track of
	// the datasets loaded up
	// public DataSetRepository getDataSetRepo() {
	// return controller.datasetRepository;
	// }

	// Returns the DefaultListModel of the field list object
	public CRDefaultListModel getFieldListModel() {
		return getListModel(CRListModels.FIELD);
	}

	public ActionListener getHelpActionListener() {
		return helpActionListener;
	}

	public CRDefaultListModel getListModel(final String modelName) {
		return controller.listModels.getListModel(modelName);
	}

	// Returns the main user interface JFrame
	public JFrame getMainFrame() {
		JFrame f;

		if (usePluginUI) {
			mainFrame.setVisible(false);
			f = getPluginFrame();
		} else {
			f = mainFrame;
		}

		f.setVisible(true);
		return f;
	}

	public JMenuBar getMenuBar() {
		return menuBar;
	}

	// Returns the number of columns of monitors.
	public int getNumCols() {
		return preferences.numberOfColumns;
	}

	// Returns the number of rows of monitors.
	public int getNumRows() {
		return preferences.numberOfRows;
	}

	public JFrame getPluginFrame() {
		if (pluginUIIndex != -1) {
			CorelyzerPlugin p = getPluginManager().getPlugin(pluginUIIndex);
			return p.getFrame();
		}

		return null;
	}

	public CorelyzerPluginManager getPluginManager() {
		return controller.pluginManager;
	}

	public int getPluginUIIndex() {
		return pluginUIIndex;
	}

	public JProgressBar getProgressUI() {
		if (usePluginUI) {
			JFrame f = getPluginFrame();

			if (f instanceof ProgressHandler) {
				ProgressHandler ph = (ProgressHandler) f;
				return (JProgressBar) ph.getProgressUI();
			}
		}

		return new JProgressBar();
	}

	// Returns the main user interface JPanel
	public JPanel getRootPanel() {
		return rootPanel;
	}

	// Returns the horizontal DPI of all the screens.
	public float getScreenDpiX() {
		return preferences.dpix;
	}

	/**
	 * Returns the vertical DPI of all the screens.
	 * 
	 * @return float
	 */
	public float getScreenDpiY() {
		return preferences.dpiy;
	}

	// --------------------------------------------------------------

	// Returns the JList object holding the selected tracks's section names
	public JList getSectionList() {
		return sectionList;
	}

	// Returns the DefaultListModel of the section list object
	public CRDefaultListModel getSectionListModel() {
		return getListModel(CRListModels.SECTION);
	}

	public WellLogDataSet getSelectedDataset() {
		return (WellLogDataSet) dataFileList.getSelectedValue();
	}

	public String getSelectedField() {
		return (String) fieldList.getSelectedValue();
	}

	public CoreSection getSelectedSection() {
		return (CoreSection) sectionList.getSelectedValue();
	}

	// accessor to selected items
	public Session getSelectedSession() {
		return (Session) sessionList.getSelectedValue();
	}

	public TrackSceneNode getSelectedTrack() {
		return (TrackSceneNode) trackList.getSelectedValue();
	}

	public int getSelectedTrackIndex() {
		return trackList.getSelectedIndex();
	}

	public JList getSessionList() {
		return sessionList;
	}

	public CRDefaultListModel getSessionListModel() {
		return getListModel(CRListModels.SESSION);
	}

	// Returns the height of each monitor in pixels.
	public float getTileHeight() {
		return preferences.screenHeight;
	}

	// Returns the width of each monitor in pixels.
	public float getTileWidth() {
		return preferences.screenWidth;
	}

	// Returns the main tool interface JFrame
	public CRToolPalette getToolFrame() {
		return toolFrame;
	}

	// Returns the JList object holding the track names
	public JList getTrackList() {
		return trackList;
	}

	// Returns the DefaultListModel of the track list object
	public CRDefaultListModel getTrackListModel() {
		return getListModel(CRListModels.TRACK);
	}

	public void GLWindowsToBack() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (final Window win : windowVec) {
					win.toBack();
				}
			}
		});
	}

	private boolean isEven(final int aNumber) {
		return aNumber % 2 == 0;
	}

	public boolean isUsePluginUI() {
		return usePluginUI;
	}

	public int loadData(final File aFile) {
		return controller.loadData(aFile, true);
	}

	public int loadData(final File aFile, final boolean useSAX) {
		return controller.loadData(aFile, useSAX);
	}

	public int loadImage(final File aFile, final String url) {
		return controller.loadImage(aFile, url);
	}

	public int loadImage(final File aFile, final String url, final String aName) {
		return controller.loadImage(aFile, url, aName);
	}

	public void mouseClicked(final MouseEvent e) {

		if (e.getClickCount() == 2) { // double clicks
			Object actionSource = e.getSource();

			if (actionSource.equals(sectionList)) {
				int selectedTrackIdx = trackList.getSelectedIndex();
				int selectedSectionIdx = sectionList.getSelectedIndex();

				if ((selectedTrackIdx < 0) || (selectedSectionIdx < 0)) {
					// System.out.println("" + selectedTrackIdx + ", "
					// + selectedSectionIdx);

					return;
				}

				CRDefaultListModel trackListModel = getTrackListModel();
				CRDefaultListModel sectionsListModel = getSectionListModel();

				if ((trackListModel.getSize() == 0) || (sectionsListModel.getSize() == 0)) {
					System.out.println("0 list models");

					return;
				}

				TrackSceneNode t = (TrackSceneNode) trackListModel.getElementAt(selectedTrackIdx);
				String secname = ((CoreSection) sectionsListModel.getElementAt(selectedSectionIdx)).getName();

				if (t == null) {
					System.out.println("- Null track list selection");
					return;
				}

				CoreSection cs = t.getCoreSection(secname);
				if (cs == null) {
					System.out.println("- Null CoreSection list section");
					return;
				}

				CRExperimentController.locateSection(t.getId(), cs.getId());
			} else if (actionSource.equals(trackList)) {
				// if it's track, move to track's Y-pos
				TrackSceneNode t = (TrackSceneNode) trackList.getSelectedValue();

				if (t != null) {
					int nativeTrackId = t.getId();
					if (nativeTrackId < 0) {
						return;
					}

					float posX;
					float posY;
					SceneGraph.lock();
					{
						if (SceneGraph.getDepthOrientation()) {
							posX = SceneGraph.getSceneCenterX();
							posY = SceneGraph.getTrackYPos(nativeTrackId);
						} else {
							posX = -SceneGraph.getTrackYPos(nativeTrackId);
							posY = SceneGraph.getSceneCenterY();
						}

						SceneGraph.positionScene(posX, posY);
					}
					SceneGraph.unlock();

					updateGLWindows();
				}
			}
		}
	}

	public void mouseEntered(final MouseEvent e) {
	}

	public void mouseExited(final MouseEvent e) {
	}

	public void mousePressed(final MouseEvent e) {
		// From JDK Doc
		// Note: Popup menus are triggered differently on different systems.
		// Therefore, isPopupTrigger should be checked in both mousePressed and
		// mouseReleased for proper cross-platform functionality.

		Point p = e.getPoint();
		Object actionSource = e.getSource();

		if (actionSource instanceof JList) {
			// find the index of the clicked item in the JList
			int index = ((JList) e.getSource()).locationToIndex(e.getPoint());
			if (index < 0) {
				return;
			}

			// show our popup menu if it was a right/ctrl-click
			if (e.isPopupTrigger()) {
				if (actionSource.equals(sessionList)) {
					Session s = (Session) sessionList.getSelectedValue();
					JMenuItem t;

					// Show label switching
					if (s == null) {
						return;
					}

					String l = s.isShow() ? "Hide" : "Show";
					t = (JMenuItem) sessionPopupMenu.getComponent(0);
					t.setText(l);

					sessionPopupMenu.show(e.getComponent(), p.x, p.y);
				} else if (actionSource.equals(trackList)) {
					((JList) e.getSource()).setSelectedIndex(index);

					// Update context-aware show/hide
					TrackSceneNode t = (TrackSceneNode) trackList.getSelectedValue();
					if ((t != null) && (t.getId() >= 0)) {
						boolean isShown = SceneGraph.getTrackShow(t.getId());
						String label = isShown ? "Hide" : "Show";
						((JMenuItem) trackPopupMenu.getComponent(0)).setText(label);
					}

					trackPopupMenu.show(e.getComponent(), p.x, p.y);
				} else if (actionSource.equals(sectionList)) {
					int[] rows = getSectionList().getSelectedIndices();

					JPopupMenu menu = sectionListPopupMenu(rows);
					menu.show(e.getComponent(), p.x, p.y);
				} else if (actionSource.equals(dataFileList)) {
					((JList) e.getSource()).setSelectedIndex(index);
					dataPopupMenu.show(e.getComponent(), p.x, p.y);
				}
			}
		}
	}

	public void mouseReleased(final MouseEvent e) {
		// From JDK Doc
		// Note: Popup menus are triggered differently on different systems.
		// Therefore, isPopupTrigger should be checked in both mousePressed and
		// mouseReleased for proper cross-platform functionality.

		Point p = e.getPoint();
		Object actionSource = e.getSource();

		if (actionSource instanceof JList) {
			// find the index of the clicked item in the JList
			int index = ((JList) e.getSource()).locationToIndex(e.getPoint());
			if (index < 0) {
				return;
			}

			// show our popup menu if it was a right/ctrl-click
			if (e.isPopupTrigger()) {
				if (actionSource.equals(sessionList)) {
					Session s = (Session) sessionList.getSelectedValue();
					JMenuItem t;

					// Show label switching
					if (s == null) {
						return;
					}

					String l = s.isShow() ? "Hide" : "Show";
					t = (JMenuItem) sessionPopupMenu.getComponent(0);
					t.setText(l);

					sessionPopupMenu.show(e.getComponent(), p.x, p.y);
				} else if (actionSource.equals(trackList)) {
					((JList) e.getSource()).setSelectedIndex(index);

					// Update context-aware show/hide
					TrackSceneNode t = (TrackSceneNode) trackList.getSelectedValue();
					if ((t != null) && (t.getId() >= 0)) {
						boolean isShown = SceneGraph.getTrackShow(t.getId());
						String label = isShown ? "Hide" : "Show";
						((JMenuItem) trackPopupMenu.getComponent(0)).setText(label);
					}

					trackPopupMenu.show(e.getComponent(), p.x, p.y);
				} else if (actionSource.equals(sectionList)) {
					int[] rows = getSectionList().getSelectedIndices();

					JPopupMenu menu = sectionListPopupMenu(rows);
					menu.show(e.getComponent(), p.x, p.y);
				} else if (actionSource.equals(dataFileList)) {
					((JList) e.getSource()).setSelectedIndex(index);
					dataPopupMenu.show(e.getComponent(), p.x, p.y);
				}
			}
		}
	}

	private void onDeleteSelectedSections(final int[] rows) {
		String mesg = "Are you sure you want to remove all\n" + "selected sections?";

		Object[] options = { "Cancel", "Yes" };
		int ans = JOptionPane.showOptionDialog(app.getMainFrame(), mesg, "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				options, options[0]);

		if (ans == 1) {
			CoreGraph cg = CoreGraph.getInstance();
			TrackSceneNode t = cg.getCurrentTrack();

			if (t != null) {
				int tid = t.getId();

				// delete in reverse order
				for (int i = rows.length - 1; i >= 0; i--) {
					int row = rows[i];

					CoreSection cs = t.getCoreSection(row);
					if (cs != null) {
						int csid = cs.getId();
						controller.deleteSection(tid, csid);
					}
				}
			}
		}
	}

	private void onLocateSelectedSection() {
		int selectedTrackIdx = trackList.getSelectedIndex();
		int selectedSectionIdx = sectionList.getSelectedIndex();

		if ((selectedTrackIdx < 0) || (selectedSectionIdx < 0)) {
			return;
		}

		CRDefaultListModel trackListModel = getTrackListModel();
		CRDefaultListModel sectionsListModel = getSectionListModel();

		if ((trackListModel.getSize() == 0) || (sectionsListModel.getSize() == 0)) {
			return;
		}

		TrackSceneNode t = (TrackSceneNode) trackListModel.getElementAt(selectedTrackIdx);
		String secname = ((CoreSection) sectionsListModel.getElementAt(selectedSectionIdx)).getName();

		CoreSection cs = t.getCoreSection(secname);
		if (cs == null) {
			return;
		}

		CRExperimentController.locateSection(t.getId(), cs.getId());
	}

	public CRPreferences preferences() {
		return preferences;
	}

	public void quit() {
		controller.quit();
	}

	public void relocateToolMenu(final int id) {
		Point p;
		Window jf;
		jf = windowVec.elementAt(id);
		if ((jf != null) && jf.isVisible()) {
			p = jf.getLocationOnScreen();

			// Dimension screenDim =
			// Toolkit.getDefaultToolkit().getScreenSize();
			int canvasWidth = preferences().screenWidth;
			Dimension toolFrameDim = toolFrame.getSize();
			p.x += canvasWidth / 2 - toolFrameDim.width / 2;

			toolFrame.setLocation(p.x, p.y);
		}
	}

	public void scaleSceneCenter(final float scale) {
		// Find out which canvas covers the scene center
		int canvasIndex = getNumCols() * (getNumRows() / 2) + getNumCols() / 2;
		int mX, mY;
		if (!isEven(getNumRows()) && !isEven(getNumCols())) {
			mX = (int) getTileWidth() / 2;
			mY = (int) getTileHeight() / 2;
		} else {
			// X direction
			if (isEven(getNumCols())) {
				mX = 0;
			} else {
				mX = (int) (getTileWidth() + 1) / 2;
			}

			// Y direction
			if (isEven(getNumRows())) {
				mY = 0;
			} else {
				mY = (int) (getTileHeight() + 1) / 2;
			}
		}

		CorelyzerGLCanvas canvas = canvasVec.elementAt(canvasIndex);

		// MouseWheelEvent(Component source, int id, long when, int modifiers,
		// int x, int y, int clickCount, boolean popupTrigger, int scrollType,
		// int scrollAmount, int wheelRotation)
		MouseWheelEvent scrollEvent = new MouseWheelEvent(canvas.getCanvas(), MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, mX, mY, 0, false, 0,
				(int) Math.abs(scale), (int) scale);
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(scrollEvent);
	}

	private JPopupMenu sectionListPopupMenu(final int[] rows) {
		// section popup
		JPopupMenu menu = new JPopupMenu("Sections");

		// Section/Image property
		JMenuItem propertiesMenuItem = new JMenuItem("Properties...");
		propertiesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, MENU_MASK));
		propertiesMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				controller.sectionProperties(rows);
			}
		});

		JMenuItem splitMenuItem = new JMenuItem("Split...");
		if (rows.length > 1) {
			splitMenuItem.setEnabled(false);
		}
		splitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				controller.sectionSplit();
			}
		});

		JMenuItem deleteMenuItem = new JMenuItem("Delete");
		deleteMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onDeleteSelectedSections(rows);
			}
		});

		JMenuItem locateMenuItem = new JMenuItem("Locate");
		locateMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onLocateSelectedSection();
			}
		});

		menu.add(locateMenuItem);
		menu.add(splitMenuItem);
		menu.add(propertiesMenuItem);
		menu.add(deleteMenuItem);

		return menu;
	}

	public void selectTrackByNativeTrackID(final int trackId) {
		controller.selectTrackByNativeTrackID(trackId);
	}

	public void setBorderDown(final float f) {
		preferences.borderDown = f;
	}

	public void setBorderLeft(final float f) {
		preferences.borderLeft = f;
	}

	public void setBorderRight(final float f) {
		preferences.borderRight = f;
	}

	public void setBorderUp(final float f) {
		preferences.borderUp = f;
	}

	public void setCollaborationMode(final boolean mode) {
		controller.isCollaborativeMode = mode;
	}

	public void setCurrentSessionFile(final String currentSessionFile) {
		this.currentSessionFile = currentSessionFile;

		// Update mainFrame title
		if (!currentSessionFile.equals("")) {
			File f = new File(currentSessionFile);
			String title = "Corelyzer (" + f.getName() + ")";
			getMainFrame().setTitle(title);
		}
	}

	public void setDisplayOffsets(final int column_offset, final int row_offset) {
		preferences.column_offset = column_offset;
		preferences.row_offset = row_offset;

		preferences.setProperty("display.column_offset", "" + column_offset);
		preferences.setProperty("display.row_offset", "" + row_offset);
	}

	public void setMode(final int mode) {
		// 0: normal, 1: measure, 2: marker_mod, 3: create_annotation, 4:
		// cut_section_to_new_track
		SceneGraph.setMode(mode);
		SceneGraph.setCoreSectionMarkerFocus(false);

		// set canvas mode
		for (int i = 0; i < canvasVec.size(); ++i) {
			canvasVec.elementAt(i).setMode(mode);
		}
	}

	public void setNumCols(final int i) {
		preferences.numberOfColumns = i;
	}

	public void setNumRows(final int i) {
		preferences.numberOfRows = i;
	}

	public void setPluginUIIndex(final int pluginUIIndex) {
		this.pluginUIIndex = pluginUIIndex;
	}

	public void setPreferences(final CRPreferences p) {
		preferences = p;
	}

	public void setScreenDpiX(final float f) {
		preferences.dpix = f;
	}

	public void setScreenDpiY(final float f) {
		preferences.dpiy = f;
	}

	public void setSelectedTrack(final int trackId) {
		controller.setSelectedTrack(trackId);
	}

	public void setTileHeight(final int i) {
		preferences.screenHeight = i;
	}

	public void setTileWidth(final int i) {
		preferences.screenWidth = i;
	}

	private void setupMenuStuff() {
		menuBar = new JMenuBar();

		// Create File Menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);

		JMenuItem createSessionMenuItem = new JMenuItem("Create a Session", KeyEvent.VK_N);
		createSessionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_MASK));
		createSessionMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.createSession();
			}
		});
		createSessionMenuItem.setEnabled(true);
		fileMenu.add(createSessionMenuItem);

		createTrackMenuItem = new JMenuItem("Create a Track", KeyEvent.VK_T);
		createTrackMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, MENU_MASK));
		createTrackMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.createTrack();
			}
		});
		createTrackMenuItem.setEnabled(false);
		fileMenu.add(createTrackMenuItem);

		fileMenu.addSeparator();

		// Images
		JMenu loadImageMenu = new JMenu("Load Images");
		loadImageMenuItem = new JMenuItem("Open Local Image Files...", KeyEvent.VK_M);
		loadImageMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, MENU_MASK));
		loadImageMenuItem.setEnabled(false);
		loadImageMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.loadImageAction();
			}
		});
		loadImageMenu.add(loadImageMenuItem);

		// online image services
		JMenuItem chronosMenuItem = new JMenuItem("Online Image Services...");
		loadImageMenu.add(chronosMenuItem);
		chronosMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				IODPListsDialog d = new IODPListsDialog(getMainFrame());
				d.pack();
				d.setSize(800, 600);
				d.setLocationRelativeTo(getMainFrame());
				d.setVisibleTab(1);
				d.setVisible(true);
			}
		});

		fileMenu.add(loadImageMenu);

		// Numbercal data (plots)
		JMenu loadDataMenu = new JMenu("Load Data");

		loadDataMenuItem = new JMenuItem("Open Local Dataset Files", KeyEvent.VK_D);
		loadDataMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, MENU_MASK));
		loadDataMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				Runnable r = new Runnable() {
					public void run() {
						controller.loadData();
					}
				};

				new Thread(r).start();
			}
		});
		loadDataMenu.add(loadDataMenuItem);
		fileMenu.add(loadDataMenu);

		JMenuItem quickDataImportMenuItem = new JMenuItem("Quick Data Import...");
		quickDataImportMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.quickDataImport();
			}
		});
		loadDataMenu.add(quickDataImportMenuItem);

		JMenuItem importDataMenuItem = new JMenuItem("Custom Data Import...");
		importDataMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.importData();
			}
		});
		loadDataMenu.add(importDataMenuItem);

		// LoggingDB
		JMenuItem loggingDBMenuItem = new JMenuItem("LDEO logging DB...");
		loggingDBMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				IODPListsDialog d = new IODPListsDialog(getMainFrame());
				d.pack();
				d.setSize(800, 600);
				d.setLocationRelativeTo(getMainFrame());
				d.setVisibleTab(2);
				d.setVisible(true);
			}
		});
		loadDataMenu.add(loggingDBMenuItem);

		fileMenu.addSeparator();

		loadStateFileMenuItem = new JMenuItem("Open a Session File", KeyEvent.VK_O);
		loadStateFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK));
		loadStateFileMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.loadStateFile();
			}
		});
		loadStateFileMenuItem.setEnabled(false);
		fileMenu.add(loadStateFileMenuItem);

		/*
		 * JMenuItem saveMenuItem = new JMenuItem("Save", KeyEvent.VK_S);
		 * saveMenuItem.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_S,
		 * MENU_MASK)); saveMenuItem.addActionListener(new ActionListener() {
		 * public void actionPerformed(ActionEvent event) {
		 * controller.saveCurrentSession(); } }); fileMenu.add(saveMenuItem);
		 */

		JMenuItem saveStateToFileMenuItem = new JMenuItem("Save Session As...", KeyEvent.VK_S);
		saveStateToFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, MENU_MASK));
		saveStateToFileMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.saveStateToFile();
			}
		});
		fileMenu.add(saveStateToFileMenuItem);

		// Session History
		recentSessionsMenu = new JMenu("Recent Sessions");
		controller.refreshSessionHistoryMenu();
		fileMenu.add(recentSessionsMenu);

		fileMenu.addSeparator();

		JMenu packageMenu = new JMenu("Core Archive");
		JMenuItem importMenuItem = new JMenuItem("Import...");
		importMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.importPackage();
			}
		});
		packageMenu.add(importMenuItem);

		JMenuItem exportMenuItem = new JMenuItem("Export...");
		exportMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.exportTheWholeScene();
			}
		});
		packageMenu.add(exportMenuItem);
		fileMenu.add(packageMenu);

		JMenu limsMenu = new JMenu("IODP");

		JMenuItem allIODPLists = new JMenuItem("All IODP lists...");
		allIODPLists.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				IODPListsDialog d = new IODPListsDialog(getMainFrame());
				d.pack();
				d.setSize(800, 600);
				d.setLocationRelativeTo(getMainFrame());
				d.setVisible(true);
			}
		});
		limsMenu.add(allIODPLists);

		JMenuItem loadLIMSTables = new JMenuItem("Load a section list...");
		loadLIMSTables.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				IODPListsDialog d = new IODPListsDialog(getMainFrame());
				d.loadLIMSTables(getMainFrame());

				d.pack();
				d.setSize(800, 600);
				d.setLocationRelativeTo(getMainFrame());
				d.setVisibleTab(0);
				d.setVisible(true);
			}
		});
		limsMenu.add(loadLIMSTables);

		JMenuItem loadAffineTable = new JMenuItem("Load an affine table...");
		loadAffineTable.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				IODPListsDialog d = new IODPListsDialog(getMainFrame());
				d.loadAAffineTable(getMainFrame());

				d.pack();
				d.setSize(800, 600);
				d.setLocationRelativeTo(getMainFrame());
				d.setVisibleTab(3);
				d.setVisible(true);
			}
		});
		limsMenu.add(loadAffineTable);

		JMenuItem loadSpliceTable = new JMenuItem("Load a splice table...");
		loadSpliceTable.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				IODPListsDialog d = new IODPListsDialog(getMainFrame());
				d.loadASpliceTable(getMainFrame());

				d.pack();
				d.setSize(800, 600);
				d.setLocationRelativeTo(getMainFrame());
				d.setVisibleTab(4);
				d.setVisible(true);
			}
		});
		limsMenu.add(loadSpliceTable);

		fileMenu.add(limsMenu);

		JMenu disMenu = new JMenu("DIS");

		JMenuItem disListMenuItem = new JMenuItem("Accessing DIS...");
		disListMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				DISListsDialog d = new DISListsDialog(getMainFrame());
				d.pack();
				d.setSize(800, 600);
				d.setLocationRelativeTo(getMainFrame());
				d.setVisible(true);
			}
		});

		JMenuItem disImport = new JMenuItem("Import...");
		disImport.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.disImport();
			}
		});
		JMenuItem disExport = new JMenuItem("Export...");
		disExport.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.disExport();
			}
		});

		JMenuItem disBatchDataLoad = new JMenuItem("Load Tab Delimited Data File...");
		disBatchDataLoad.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.disBatchDataLoad();
			}
		});

		disMenu.add(disListMenuItem);
		disMenu.add(disImport);
		disMenu.add(disExport);
		disMenu.add(disBatchDataLoad);
		fileMenu.add(disMenu);

		JMenuItem saveOutputToFileMenuItem = new JMenuItem("Export Sheets...", KeyEvent.VK_A);
		saveOutputToFileMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.saveOutputToFile();
			}
		});
		fileMenu.add(saveOutputToFileMenuItem);

		fileMenu.addSeparator();

		JMenuItem quitMenuItem = new JMenuItem("Quit");
		fileMenu.add(quitMenuItem);
		quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MASK));
		quitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.quit();
			}
		});

		menuBar.add(fileMenu);

		// Create Edit Menu
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);

		JMenuItem clearImageCacheMenuItem = new JMenuItem("Clear Image Cache...", KeyEvent.VK_I);
		clearImageCacheMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.clearImageCache();
			}
		});
		editMenu.add(clearImageCacheMenuItem);

		editMenu.addSeparator();
		JMenuItem depthDirection = new JMenuItem("Switch Depth Direction", KeyEvent.VK_K);
		depthDirection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, MENU_MASK));
		depthDirection.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				boolean b = SceneGraph.getDepthOrientation();
				SceneGraph.setDepthOrientation(!b);

				CorelyzerApp.getApp().updateGLWindows();
			}
		});
		editMenu.add(depthDirection);

		JMenuItem tour = new JMenuItem("Tour...");
		tour.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				CRNavigationSetupDialog dlg = new CRNavigationSetupDialog();
				dlg.pack();
				dlg.setLocationRelativeTo(getApp().getMainFrame());

				dlg.setAlwaysOnTop(true);
				dlg.setVisible(true);
			}
		});

		editMenu.add(tour);

		editMenu.addSeparator();
		JMenuItem preferencesMenuItem = new JMenuItem("Preferences...");
		editMenu.add(preferencesMenuItem);
		preferencesMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.doPreferences();
			}
		});

		menuBar.add(editMenu);

		// Create Share Menu
		JMenu shareMenu = new JMenu("Share");

		JMenuItem publishMenuItem = new JMenuItem("Publish...");
		publishMenuItem.setEnabled(true);
		publishMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				controller.publishASession();
			}
		});
		shareMenu.add(publishMenuItem);

		JMenuItem listMenuItem = new JMenuItem("List...");
		listMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				controller.listSessions();
			}
		});
		shareMenu.add(listMenuItem);

		shareMenu.addSeparator();

		friendsMenu = new JMenu("Friends");
		shareMenu.add(friendsMenu);

		menuBar.add(shareMenu);

		// Create Debug Menu
		JMenu debugMenu = new JMenu("Debug");
		debugMenu.setMnemonic(KeyEvent.VK_D);

		JMenuItem matchTestMenuItem = new JMenuItem("Match-Test", KeyEvent.VK_B);
		matchTestMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				controller.testAndMatch();
			}
		});

		debugMenu.add(matchTestMenuItem);

		if ((System.getenv("DEBUG") != null) && System.getenv("DEBUG").equals("YES")) {
			menuBar.add(shareMenu);
		}

		// Create the Plugin Menu
		pluginMenu = new JMenu("Plugins");
		menuBar.add(pluginMenu);
		pluginMenuItemVec = new Vector<JMenuItem>();

		// Lists
		JMenu listsMenu = new JMenu("Lists");

		JMenuItem iodpLists = new JMenuItem("IODP lists...");
		iodpLists.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				IODPListsDialog d = new IODPListsDialog(getMainFrame());
				d.pack();
				d.setSize(800, 600);
				d.setLocationRelativeTo(getMainFrame());
				d.setVisible(true);
			}
		});
		listsMenu.add(iodpLists);

		JMenuItem disLists = new JMenuItem("DIS lists...");
		disLists.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				DISListsDialog d = new DISListsDialog(getMainFrame());
				d.pack();
				d.setSize(800, 600);
				d.setLocationRelativeTo(getMainFrame());
				d.setVisible(true);
			}
		});
		listsMenu.add(disLists);

		JMenu annotLists = new JMenu("Annotation");

		JMenuItem freeformsItem = new JMenuItem("Default");
		freeformsItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				FreeformAnnotationListDialog dlg = new FreeformAnnotationListDialog();
				dlg.pack();
				dlg.setLocationRelativeTo(getApp().getMainFrame());
				dlg.onRefresh();

				dlg.setAlwaysOnTop(true);
				dlg.setVisible(true);
			}
		});
		annotLists.add(freeformsItem);

		JMenuItem clastListItem = new JMenuItem("Clast");
		clastListItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				ClastStatisticsDialog dlg = new ClastStatisticsDialog();
				dlg.pack();
				dlg.setLocationRelativeTo(getApp().getMainFrame());
				dlg.onRefresh();

				dlg.setAlwaysOnTop(true);
				dlg.setVisible(true);
			}
		});
		annotLists.add(clastListItem);

		JMenuItem sampleReqsItem = new JMenuItem("Sample");
		sampleReqsItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				SampleRequestListDialog dlg = new SampleRequestListDialog();
				dlg.pack();
				dlg.setLocationRelativeTo(getApp().getMainFrame());
				dlg.onRefresh();

				dlg.setAlwaysOnTop(true);
				dlg.setVisible(true);
			}
		});
		annotLists.add(sampleReqsItem);
		listsMenu.add(annotLists);

		menuBar.add(listsMenu);

		// Create Tools Menu
		JMenu toolsMenu = new JMenu("Tools");
		JMenuItem wholeCoreViewerMenuItem = new JMenuItem("Get 3D Whole Core Imagery Viewer...");
		wholeCoreViewerMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				try {
					String app;
					String url = "http://www.evl.uic.edu/cavern/corewall/SciVizCore/";

					if (System.getProperty("os.name").toLowerCase().contains("windows")) {
						app = "cmd.exe /c explorer " + url;
						Runtime.getRuntime().exec(app);
					} else {
						app = "open";
						String[] cmd = { app, url };
						Runtime.getRuntime().exec(cmd);
					}
				} catch (IOException ex) {
					System.err.println("IOException in opening SciVizCore link");
				}
			}
		});

		toolsMenu.add(wholeCoreViewerMenuItem);

		// DEBUG
		String debug = System.getProperty("DEBUG");
		if ((debug != null) && System.getProperty("DEBUG").equals("true")) {
			JMenuItem gcItem = new JMenuItem("GC");
			gcItem.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent event) {
					long mem0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
					System.out.println("- B4 mem:\t" + mem0 / 1000000.0f);

					int trials = 10000;
					for (int i = 0; i < trials; i++) {
						updateGLWindows();
					}

					long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
					System.out.println("- " + trials + " mem:\t" + mem1 / 1000000.0f);

					System.gc();
					System.gc();
					System.gc();

					long mem2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
					System.out.println("- AferGC mem:\t" + mem2 / 1000000.0f);
				}
			});
			toolsMenu.add(gcItem);
		}

		menuBar.add(toolsMenu);

		// Create Help Menu
		// Create JavaHelp hooks
		File helpSet = new File("../help/jhelpset.hs");
		HelpSet hs = null;
		try {
			URL hsURL = helpSet.toURI().toURL();
			hs = new HelpSet(null, hsURL);
		} catch (HelpSetException e) {
			JOptionPane.showMessageDialog(getMainFrame(), "Cannot find help: format error.");
			e.printStackTrace();
		} catch (MalformedURLException e) {
			JOptionPane.showMessageDialog(getMainFrame(), "Cannot find help: malformed URL.");
			e.printStackTrace();
		}

		// Help UI
		JMenu helpMenu = new JMenu("Help");
		JMenuItem helpMenuItem = new JMenuItem("Help", KeyEvent.VK_H);

		if (hs != null) {
			HelpBroker hb = hs.createHelpBroker();
			helpActionListener = new CSH.DisplayHelpFromSource(hb);
			helpMenuItem.addActionListener(helpActionListener);
		} else {
			helpMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent event) {
					controller.helpAction();
				}
			});
		}
		helpMenu.add(helpMenuItem);

		helpMenu.addSeparator();
		JMenuItem aboutMenuItem = new JMenuItem("About Corelyzer");
		helpMenu.add(aboutMenuItem);
		aboutMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.about();
			}
		});

		menuBar.add(helpMenu);

		mainFrame.setJMenuBar(menuBar);
	}

	private void setupPopupMenu() {
		// session popup
		sessionPopupMenu = new JPopupMenu("Sessions");

		JMenuItem hideSession = new JMenuItem("Hide");
		hideSession.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				JMenuItem m = (JMenuItem) e.getSource();
				Session s = (Session) sessionList.getSelectedValue();
				String l = s.isShow() ? "Show" : "Hide";
				controller.setSessionVisible(!s.isShow());
				m.setText(l);
			}
		});
		sessionPopupMenu.add(hideSession);

		JMenuItem renameSession = new JMenuItem("Rename...");
		renameSession.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				controller.renameSession();
			}
		});
		sessionPopupMenu.add(renameSession);

		JMenuItem removeSession = new JMenuItem("Close");
		removeSession.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				Session s = (Session) sessionList.getSelectedValue();
				controller.removeSession(s);
			}
		});
		sessionPopupMenu.add(removeSession);

		// track popup
		trackPopupMenu = new JPopupMenu("Tracks");

		JMenuItem hide = new JMenuItem("Hide");
		hide.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				controller.setTrackVisible();
			}
		});
		trackPopupMenu.add(hide);

		JMenuItem rename = new JMenuItem("Rename...");
		rename.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				controller.renameTrack();
			}
		});
		trackPopupMenu.add(rename);

		trackPopupDeleteMenuItem = new JMenuItem("Delete");
		trackPopupDeleteMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.deleteTrack();
			}
		});
		trackPopupMenu.add(trackPopupDeleteMenuItem);

		// data popup
		dataPopupMenu = new JPopupMenu("Datasets");

		dataPopupGraphMenuItem = new JMenuItem("Graph...");
		dataPopupGraphMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.showGraphDialog();
			}
		});
		dataPopupMenu.add(dataPopupGraphMenuItem);
	}

	private void setupUI() {
		String versionNumber = System.getProperty("corelyzer.version");
		if ((versionNumber == null) || versionNumber.equals("")) {
			versionNumber = "undetermined";
		}
		mainFrame = new JFrame("Corelyzer " + versionNumber);
		mainFrame.setVisible(false);

		mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		mainFrame.setSize(320, 100);
		mainFrame.setLocation(600, 100);
		mainFrame.addWindowListener(this);

		GridLayout layout = new GridLayout(1, 1);
		mainFrame.getContentPane().setLayout(layout);

		rootPanel = new JPanel(new GridLayout(1, 5));
		rootPanel.setBorder(BorderFactory.createTitledBorder("Main Panel"));

		// add lists/panels
		JPanel sessionPanel = new JPanel(new GridLayout(1, 1));
		sessionPanel.setBorder(BorderFactory.createTitledBorder("Session"));
		sessionList = new JList(getSessionListModel());
		sessionList.setName("SessionList");
		sessionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sessionList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				int idx = sessionList.getSelectedIndex();
				if (idx >= 0) {
					CoreGraph cg = CoreGraph.getInstance();
					cg.setCurrentSessionIdx(idx);
				}
			}
		});
		sessionList.addMouseListener(this);
		JScrollPane sessionScrollPane = new JScrollPane(sessionList);
		sessionPanel.add(sessionScrollPane);
		rootPanel.add(sessionPanel);

		JPanel trackPanel = new JPanel(new GridLayout(1, 1));
		trackPanel.setBorder(BorderFactory.createTitledBorder("Track"));
		trackList = new JList(getTrackListModel());
		trackList.setName("TrackList");
		trackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		trackList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				int idx = trackList.getSelectedIndex();
				if (idx >= 0) {
					CoreGraph cg = CoreGraph.getInstance();
					cg.setCurrentTrackIdx(idx);
				}
			}
		});
		trackList.addMouseListener(this);
		JScrollPane trackScrollPane = new JScrollPane(trackList);
		trackPanel.add(trackScrollPane);
		rootPanel.add(trackPanel);

		JPanel sectionsPanel = new JPanel(new GridLayout(1, 1));
		sectionsPanel.setBorder(BorderFactory.createTitledBorder("Sections"));
		sectionList = new JList(getSectionListModel());
		sectionList.setName("SectionList");
		sectionList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				int idx = sectionList.getSelectedIndex();
				if (idx >= 0) {
					CoreGraph cg = CoreGraph.getInstance();
					cg.setCurrentSectionIdx(idx);
				}
			}
		});
		sectionList.addMouseListener(this);
		JScrollPane sectionsScrollPane = new JScrollPane(sectionList);
		sectionsPanel.add(sectionsScrollPane);
		rootPanel.add(sectionsPanel);

		JPanel dataFilesPanel = new JPanel(new GridLayout(1, 1));
		dataFilesPanel.setBorder(BorderFactory.createTitledBorder("Data Files"));
		dataFileList = new JList(getDataFileListModel());
		dataFileList.setName("DatafileList");
		dataFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dataFileList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				int idx = dataFileList.getSelectedIndex();
				if (idx >= 0) {
					CoreGraph cg = CoreGraph.getInstance();
					cg.setCurrentDatasetIdx(idx);
				}
			}
		});
		dataFileList.addMouseListener(this);
		JScrollPane dataFilesScrollPane = new JScrollPane(dataFileList);
		dataFilesPanel.add(dataFilesScrollPane);
		rootPanel.add(dataFilesPanel);

		JPanel fieldsPanel = new JPanel(new GridLayout(1, 1));
		fieldsPanel.setBorder(BorderFactory.createTitledBorder("Fields"));
		fieldList = new JList(getFieldListModel());
		fieldList.setName("FieldList");
		fieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fieldList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				int idx = fieldList.getSelectedIndex();
				if (idx >= 0) {
					CoreGraph cg = CoreGraph.getInstance();
					cg.setCurrentFieldIdx(idx);
				}
			}
		});
		JScrollPane fieldsScrollPane = new JScrollPane(fieldList);
		fieldsPanel.add(fieldsScrollPane);
		rootPanel.add(fieldsPanel);

		mainFrame.getContentPane().add(rootPanel);

		setupMenuStuff();
		setupPopupMenu();
		mainFrame.setAlwaysOnTop(true);
		mainFrame.setVisible(false);

		// init new mode tool frame
		toolFrame = new CRToolPalette();
		toolFrame.pack();
		int canvasWidth = preferences().screenWidth;
		Dimension mydim = toolFrame.getSize();
		int myLocX = canvasWidth / 2 - mydim.width / 2;
		toolFrame.setLocation(myLocX, 0);
		toolFrame.setVisible(true);

		// delete key listener on track and section list
		KeyListener listKeyListener = new KeyListener() {
			public void keyPressed(final KeyEvent keyEvent) {
			}

			public void keyReleased(final KeyEvent keyEvent) {

				int keyCode = keyEvent.getKeyCode();

				if (keyCode == KeyEvent.VK_DELETE) {
					Object actionSource = keyEvent.getSource();

					if (actionSource.equals(trackList)) {
						controller.deleteSelectedTrack();
					} else if (actionSource.equals(sectionList)) {
						int[] rows = getSectionList().getSelectedIndices();
						onDeleteSelectedSections(rows);
					}
				}
			}

			public void keyTyped(final KeyEvent keyEvent) {
			}
		};

		trackList.addKeyListener(listKeyListener);
		sectionList.addKeyListener(listKeyListener);
	}

	public void setUsePluginUI(final boolean usePluginUI) {
		this.usePluginUI = usePluginUI;
	}

	private void startup() {
		destroyGLWindows();

		setNumRows(preferences.numberOfRows);
		setNumCols(preferences.numberOfColumns);
		setTileWidth(preferences.screenWidth);
		setTileHeight(preferences.screenHeight);
		setScreenDpiX(preferences.dpix);
		setScreenDpiY(preferences.dpiy);
		setBorderLeft(preferences.borderLeft);
		setBorderRight(preferences.borderRight);
		setBorderDown(preferences.borderDown);
		setBorderUp(preferences.borderUp);

		createGLWindows();
		isGLInited = true;

		getToolFrame().setVisible(true);
		getMainFrame().setVisible(true);
	}

	/**
	 * install4j launcher extended api // Mac case: invoked when app start with
	 * associate file // Win case: invoked when app is already running and user
	 * exec another app // or double cliked on associate file.
	 */
	public void startupPerformed(final String parameters) {

		// parameter passes file name with quatation: "filename"
		System.out.println("got startup parameter: " + parameters);

		// check length of filename
		if (parameters.length() == 0) {
			return;
		}

		// remove quatation
		String filename = parameters.replaceAll("\"", "");

		// load session file
		StateLoader stateLoader = new StateLoader();
		stateLoader.loadState(filename);
		app.updateGLWindows();
	}

	/**
	 * Called to indicate that the OpenGL windows should be repainted
	 */
	public void updateGLWindows() {
		for (final CorelyzerGLCanvas canvas : canvasVec) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					canvas.getCanvas().repaint();
				}
			});
		}
	}

	// Handles when the main frame closes so that the application and
	// corelyzer.helper.SceneGraph system can close down properly
	@Override
	public void windowClosing(final WindowEvent e) {
		controller.quit();
	}

	@Override
	public void windowDeiconified(final WindowEvent e) {
		// resume the GL windows
		for (int i = 0; i < windowVec.size(); i++) {
			Window jf;
			jf = windowVec.elementAt(i);
			if (jf != null) {
				jf.setVisible(true);
			}
		}

		// resume the tool window
		toolFrame.setVisible(true);

		// resume all plugin frame
		controller.deiconifyAllPlugins();

		// to restore workaround in iconifing window
		if (!MAC_OS_X && usePluginUI) {
			getDefaultMainFrame().setVisible(false);
			getMainFrame().setExtendedState(Frame.NORMAL);
		}

		getMainFrame().setVisible(toolFrame.isAppFrameSelected());
	}

	@Override
	public void windowIconified(final WindowEvent e) {
		// minimize the GL windows
		for (int i = 0; i < windowVec.size(); i++) {
			Window jf;
			jf = windowVec.elementAt(i);
			if (jf != null) {
				jf.setVisible(false);
			}
		}

		// minimize the tool window
		toolFrame.setVisible(false);

		// iconify all plugin fram if there is
		controller.iconifyAllPlugins();

		// to avoid missing application in taskbar under Windows
		if (!MAC_OS_X && usePluginUI) {
			getDefaultMainFrame().setVisible(true);
			getDefaultMainFrame().setExtendedState(Frame.ICONIFIED);
		}
	}
}
