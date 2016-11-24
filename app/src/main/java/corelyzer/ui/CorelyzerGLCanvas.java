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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;

import corelyzer.controller.CRExperimentController;
import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionImage;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.graphics.SceneGraph;
import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.remoteControl.server.controller.actions.FineTuneDialog;
import corelyzer.ui.annotation.AbstractAnnotationDialog;
import corelyzer.ui.annotation.AnnotationType;
import corelyzer.ui.annotation.AnnotationTypeDirectory;
import corelyzer.ui.annotation.AnnotationUtils;
import corelyzer.ui.annotation.freeform.CRAnnotationWindow;
import corelyzer.util.CRUtility;
import corelyzer.util.PropertyListUtility;

import org.apache.commons.lang3.ArrayUtils;

/**
 * A container class that holds handles to the GLCanvas object, index to the
 * canvas id in the SceneGraph. It also handles input events from users when
 * direct interaction occurs (e.g. mouse clicks, dragging, etc.) This also
 * captures events for GLEvents (e.g. drawing).
 */
public class CorelyzerGLCanvas implements GLEventListener, MouseListener, MouseWheelListener, MouseMotionListener, ActionListener, KeyListener {
	public static boolean isFineTune() {
		return isFineTune;
	}

	public static void setFineTune(final boolean fineTune, final FineTuneDialog aDialog) {
		isFineTune = fineTune;
		finetuneDialog = aDialog;
	}

	public static void setFinetuneDialog(final FineTuneDialog finetuneDialog) {
		CorelyzerGLCanvas.finetuneDialog = finetuneDialog;
	}

	private final GLCanvas canvas;
	private final int canvasId;
	public static int PAN_MODE = 0;

	public static int ZOOM_MODE = 0;
	private static int selectedTrack = -1;

	private static int selectedTrackSection = -1;
	// index to the current list model
	private static int selectedTrackIndex = -1;
	private static int selectedTrackSectionIndex = -1;
	private static int selectedMarker = -1;

	private static int selectedGraph = -1;

	private static int selectedFreeDraw = -1;
	private final ReentrantLock canvasLock = new ReentrantLock();

	// for manipulating marker
	public static int MANIPULATE_MODE = 0;
	// For capturing mouse dragging displacements
	Point prePos;
	Point rightClickPos;

	private static float scenePos[] = { 0.0f, 0.0f };
	private static float prescenePos[] = { 0.0f, 0.0f };
	public static int canvasMode = 0;
	// PopupMenu UIs
	JPopupMenu scenePopupMenu;
	JRadioButtonMenuItem normalMode;
	JRadioButtonMenuItem measureMode;

	JRadioButtonMenuItem markerMode;
	JRadioButtonMenuItem clastMode;

	JRadioButtonMenuItem cutMode;

	JMenuItem propertyMenuItem;
	JMenuItem splitMenuItem;

	// 8/2/2012 brg: TODO Index-based approach isn't ideal when inserting (rather than appending)
	// menu items - keep references to JMenuItems instead?
	// Section-based items' indices:
	// {Lock Section, Lock Section Graph, [separator], Graph, Property, Split, Delete, Stagger, Trim, Stack}
	static int[] sectionBasedPopupMenuItemIndices = { 7, 8, 10, 11, 12, 13, 14, 15, 16 };

	// For track depth fine tune action called from remote control server
	private static boolean isFineTune = false;

	private static FineTuneDialog finetuneDialog = null;

	public static CRAnnotationWindow getAnnotationWindow() {
		// which type? which one? need more info, especially for the
		// CorelyzerSessionClientPlugin.java
		CRAnnotationWindow dlg = new CRAnnotationWindow();

		dlg.setTrackId(selectedTrack);
		dlg.setSectionId(selectedTrackSection);
		dlg.pack();

		return dlg;
	}

	// Returns the mouse's position in the scene graph. Position are two
	// floats in units of pixels.
	public static float[] getMousePosInScene() {
		return scenePos;
	}

	public static float[] getMouseScenePosInMeters() {
		float p[] = new float[2];
		p[0] = scenePos[0] / SceneGraph.getCanvasDPIX(0) * 2.54f / 100.0f;
		p[1] = scenePos[1] / SceneGraph.getCanvasDPIY(0) * 2.54f / 100.0f;
		return p;
	}

	public CorelyzerGLCanvas(final GLCanvas c, final int width, final int height, final float px, final float py, final int id) {
		canvasId = id;
		canvas = c;
		canvas.setSize(width, height);
		canvas.setLocation(0, 0);
		canvas.addGLEventListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addMouseWheelListener(this);
		canvas.addKeyListener(this);

		this.createPopupMenuUI();

		System.out.println("---> px, py: " + px + ", " + py);
	}

	/**
	 * Handles events from selecting menu options from the pop-up menu created
	 * from right click events.
	 */
	public void actionPerformed(final ActionEvent e) {
		System.out.println("[CorelyzerGLCanvas] Unhandled actionEvent: " + e);

		/*
		 * JMenuItem src = (JMenuItem) e.getSource();
		 * 
		 * // System.out.println("-- MenuItem ActionPerformed: " +
		 * src.getText());
		 * 
		 * if (src.getText().equals("Add Annotation")) { selectedTrack =
		 * SceneGraph.accessPickedTrack(); selectedTrackSection =
		 * SceneGraph.accessPickedSection();
		 * 
		 * // System.out.println("--- Add Annotation track: " + selectedTrack //
		 * + " section: " + selectedTrackSection); if ((selectedTrack < 0) ||
		 * (selectedTrackSection < 0)) return;
		 * 
		 * // System.out.println("--- Annotation on track:" + selectedTrack + //
		 * ", " + selectedTrackSection);
		 * 
		 * this.convertMousePointToSceneSpace(this.rightClickPos, scenePos);
		 * 
		 * annotationDialog.setTrackId(selectedTrack);
		 * annotationDialog.setSectionId(selectedTrackSection);
		 * 
		 * float x0 = SceneGraph.getDepthOrientation() ? scenePos[0] :
		 * scenePos[1]; float y0 = SceneGraph.getDepthOrientation() ?
		 * scenePos[1] : -scenePos[0];
		 * 
		 * annotationDialog.setAnnotationXPosition(x0);
		 * annotationDialog.setAnnotationYPosition(y0);
		 * 
		 * annotationDialog.setEditNewMode();
		 * annotationDialog.setWriteLocalCopy(true);
		 * annotationDialog.setGroup(ChatGroup.UNDEFINED);
		 * annotationDialog.setType(MarkerType.CORE_POINT_MARKER);
		 * annotationDialog.setLocation(prePos.x + 20, prePos.y -
		 * annotationDialog.getSize().height / 2);
		 * 
		 * annotationDialog.setVisible(true); } else if
		 * (src.getText().equals("Point Marker")) {
		 * 
		 * selectedTrack = SceneGraph.accessPickedTrack(); selectedTrackSection
		 * = SceneGraph.accessPickedSection();
		 * 
		 * // System.out.println("--- Add Annotation track: " + selectedTrack //
		 * + " section: " + selectedTrackSection); if ((selectedTrack < 0) ||
		 * (selectedTrackSection < 0)) return;
		 * 
		 * // System.out.println("--- Annotation on track:" + selectedTrack + //
		 * ", " + selectedTrackSection);
		 * 
		 * this.convertMousePointToSceneSpace(this.rightClickPos, scenePos);
		 * 
		 * annotationDialog.setTrackId(selectedTrack);
		 * annotationDialog.setSectionId(selectedTrackSection);
		 * 
		 * float x0 = SceneGraph.getDepthOrientation() ? scenePos[0] :
		 * scenePos[1]; float y0 = SceneGraph.getDepthOrientation() ?
		 * scenePos[1] : -scenePos[0];
		 * 
		 * annotationDialog.setAnnotationXPosition(x0);
		 * annotationDialog.setAnnotationYPosition(y0);
		 * 
		 * annotationDialog.setEditNewMode();
		 * annotationDialog.setWriteLocalCopy(true);
		 * annotationDialog.setGroup(ChatGroup.UNDEFINED);
		 * annotationDialog.setType(MarkerType.CORE_POINT_MARKER);
		 * annotationDialog.setLocation(prePos.x + 20, prePos.y -
		 * annotationDialog.getSize().height / 2);
		 * 
		 * annotationDialog.setVisible(true); } else if
		 * (src.getText().equals("Span Marker")) {
		 * 
		 * selectedTrack = SceneGraph.accessPickedTrack(); selectedTrackSection
		 * = SceneGraph.accessPickedSection();
		 * 
		 * // System.out.println("--- Add Annotation track: " + selectedTrack //
		 * + " section: " + selectedTrackSection); if ((selectedTrack < 0) ||
		 * (selectedTrackSection < 0)) return;
		 * 
		 * // System.out.println("--- Annotation on track:" + selectedTrack + //
		 * ", " + selectedTrackSection);
		 * 
		 * this.convertMousePointToSceneSpace(this.rightClickPos, scenePos);
		 * 
		 * annotationDialog.setTrackId(selectedTrack);
		 * annotationDialog.setSectionId(selectedTrackSection);
		 * 
		 * float x0 = SceneGraph.getDepthOrientation() ? scenePos[0] :
		 * scenePos[1]; float y0 = SceneGraph.getDepthOrientation() ?
		 * scenePos[1] : -scenePos[0];
		 * 
		 * annotationDialog.setAnnotationXPosition(x0);
		 * annotationDialog.setAnnotationYPosition(y0);
		 * 
		 * annotationDialog.setEditNewMode();
		 * annotationDialog.setWriteLocalCopy(true);
		 * annotationDialog.setGroup(ChatGroup.UNDEFINED);
		 * annotationDialog.setType(MarkerType.CORE_SPAN_MARKER);
		 * annotationDialog.setLocation(prePos.x + 20, prePos.y -
		 * annotationDialog.getSize().height / 2);
		 * 
		 * annotationDialog.setVisible(true); } else if
		 * (src.getText().equals("Block Marker")) {
		 * 
		 * selectedTrack = SceneGraph.accessPickedTrack(); selectedTrackSection
		 * = SceneGraph.accessPickedSection();
		 * 
		 * // System.out.println("--- Add Annotation track: " + selectedTrack //
		 * + " section: " + selectedTrackSection); if ((selectedTrack < 0) ||
		 * (selectedTrackSection < 0)) return;
		 * 
		 * // System.out.println("--- Annotation on track:" + selectedTrack + //
		 * ", " + selectedTrackSection);
		 * 
		 * this.convertMousePointToSceneSpace(this.rightClickPos, scenePos);
		 * 
		 * annotationDialog.setTrackId(selectedTrack);
		 * annotationDialog.setSectionId(selectedTrackSection);
		 * 
		 * float x0 = SceneGraph.getDepthOrientation() ? scenePos[0] :
		 * scenePos[1]; float y0 = SceneGraph.getDepthOrientation() ?
		 * scenePos[1] : -scenePos[0];
		 * 
		 * annotationDialog.setAnnotationXPosition(x0);
		 * annotationDialog.setAnnotationYPosition(y0);
		 * 
		 * annotationDialog.setEditNewMode();
		 * annotationDialog.setWriteLocalCopy(true);
		 * annotationDialog.setGroup(ChatGroup.UNDEFINED);
		 * annotationDialog.setType(MarkerType.CORE_OUTLINE_MARKER);
		 * annotationDialog.setLocation(prePos.x + 20, prePos.y -
		 * annotationDialog.getSize().height / 2);
		 * 
		 * annotationDialog.setVisible(true); } else {
		 * System.out.println("---> Unhandled Action " + e.getActionCommand());
		 * }
		 */
	}

	// Given a point on the canvas, the point is then converted to
	// a point in the scene graph.
	void convertMousePointToSceneSpace(final Point mp, final float pos[]) {
		float x, y, w, s;

		x = SceneGraph.getCanvasPositionX(canvasId);
		y = SceneGraph.getCanvasPositionY(canvasId);
		w = SceneGraph.getCanvasWidth(canvasId);
		s = w / canvas.getWidth();
		pos[0] = mp.x * s + x;
		pos[1] = mp.y * s + y;
	}

	void convertScenePointToAbsolute(final float pos[], final float result[]) {
		float dpi;
		dpi = SceneGraph.getCanvasDPIX(canvasId);
		result[0] = pos[0] / dpi * 2.54f;
		result[1] = pos[1] / dpi * 2.54f;

	}

	void createPopupMenuUI() {
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		this.scenePopupMenu = new JPopupMenu();

		JMenuItem trackName = new JMenuItem("Track Name");
		trackName.setEnabled(false);
		this.scenePopupMenu.add(trackName);

		JMenuItem sectionName = new JMenuItem("Section name");
		sectionName.setEnabled(false);
		this.scenePopupMenu.add(sectionName);

		this.scenePopupMenu.addSeparator();

		// Mode menu
		JMenu modeMenu = new JMenu("Mode");
		ButtonGroup modeGroup = new ButtonGroup();
		this.normalMode = new JRadioButtonMenuItem("Normal mode");
		this.normalMode.setIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/normal.gif")));
		this.normalMode.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				CorelyzerApp.getApp().getToolFrame().setMode(0);
			}
		});
		this.normalMode.setSelected(true);
		modeGroup.add(this.normalMode);
		modeMenu.add(this.normalMode);

		this.clastMode = new JRadioButtonMenuItem("Create annotation mode");
		this.clastMode.setIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/copyright.gif")));
		this.clastMode.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				CorelyzerApp.getApp().getToolFrame().setMode(3);
			}
		});
		modeGroup.add(this.clastMode);
		modeMenu.add(this.clastMode);

		this.markerMode = new JRadioButtonMenuItem("Modify annotation marker mode");
		this.markerMode.setIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/marker.gif")));
		this.markerMode.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				CorelyzerApp.getApp().getToolFrame().setMode(2);
			}
		});
		modeGroup.add(this.markerMode);
		modeMenu.add(this.markerMode);

		this.measureMode = new JRadioButtonMenuItem("Measure mode");
		this.measureMode.setIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/ruler.gif")));
		this.measureMode.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				CorelyzerApp.getApp().getToolFrame().setMode(1);
			}
		});
		modeGroup.add(this.measureMode);
		modeMenu.add(this.measureMode);

		this.cutMode = new JRadioButtonMenuItem("Cut mode");
		this.cutMode.setIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/cut.gif")));
		this.cutMode.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				CorelyzerApp.getApp().getToolFrame().setMode(4);
			}
		});
		modeGroup.add(this.cutMode);
		modeMenu.add(this.cutMode);

		this.scenePopupMenu.add(modeMenu);

		JMenuItem hideTrackMenuItem = new JMenuItem("Hide track");
		hideTrackMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				doHideTrack();
			}
		});
		this.scenePopupMenu.add(hideTrackMenuItem);

		JMenuItem exportTrackMenuItem = new JMenuItem("Export track");
		exportTrackMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent actionEvent) {
				doExportTrack();
			}
		});
		this.scenePopupMenu.add(exportTrackMenuItem);
		
		JMenuItem lockSectionMenuItem = new JCheckBoxMenuItem("Lock Section");
		lockSectionMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				AbstractButton b = (AbstractButton)actionEvent.getSource();
				doLockSection( b.getModel().isSelected() );
			}
		});
		
		JMenuItem lockSectionGraphMenuItem = new JCheckBoxMenuItem("Lock Section Graphs");
		lockSectionGraphMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				AbstractButton b = (AbstractButton)actionEvent.getSource();
				doLockSectionGraph( b.getModel().isSelected() );
			}
		});

		JMenuItem graphMenuItem = new JMenuItem("Graph...");
		graphMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				doGraphDialog();
			}
		});

		this.propertyMenuItem = new JMenuItem("Properties...");
		this.propertyMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				SectionImagePropertyDialog dialog = new SectionImagePropertyDialog(canvas);
				dialog.setProperties(selectedTrack, selectedTrackSection);
				dialog.pack();
				dialog.setLocationRelativeTo(canvas);
				dialog.setVisible(true);
				dialog.dispose();
			}
		});

		splitMenuItem = new JMenuItem("Split...");
		splitMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				CorelyzerApp app = CorelyzerApp.getApp();
				if (app != null) {
					app.getController().sectionSplit();
				}
			}
		});

		JMenuItem deleteItem = new JMenuItem("Delete...");
		deleteItem.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent actionEvent) {
				doDeleteSection();
			}
		});
		
		JMenuItem staggerSectionsItem = new JCheckBoxMenuItem("Stagger Sections", false);
		staggerSectionsItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				AbstractButton b = (AbstractButton)e.getSource();
				doStaggerSections(b.getModel().isSelected());
			}
		});
		
		JMenuItem trimSectionsItem = new JMenuItem("Trim Sections...");
		trimSectionsItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				doTrimSections();
			}
		});
		
		JMenuItem stackSectionsItem = new JMenuItem("Stack Sections");
		stackSectionsItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				doStackSections();
			}
		});

		this.scenePopupMenu.addSeparator();
		this.scenePopupMenu.add(lockSectionMenuItem);
		this.scenePopupMenu.add(lockSectionGraphMenuItem);
		this.scenePopupMenu.addSeparator();
		this.scenePopupMenu.add(graphMenuItem);
		this.scenePopupMenu.add(splitMenuItem);
		this.scenePopupMenu.add(propertyMenuItem);
		this.scenePopupMenu.add(deleteItem);
		this.scenePopupMenu.add(staggerSectionsItem);
		this.scenePopupMenu.add(trimSectionsItem);
		this.scenePopupMenu.add(stackSectionsItem);

		CorelyzerApp.getApp().getPluginManager().addPluginPopupSubMenus(this.scenePopupMenu);
	}

	// Calls the corelyzer.helper.SceneGraph.performPick method, and then
	// holds ids of objects picked (e.g. picked tracks, sections, graphs,
	// markers, etc.)
	void determineSelectedSceneComponents(final float p[], final MouseEvent event) {
		SceneGraph.performPick(canvasId, p[0], p[1]);
		int track, section;

		track = SceneGraph.accessPickedTrack();
		section = SceneGraph.accessPickedSection();

		if (track != selectedTrack || section != selectedTrackSection) {
			CorelyzerApp.getApp().updateGLWindows();
		}

		selectedTrack = track;
		selectedTrackSection = section;
		selectedMarker = SceneGraph.accessPickedMarker();
		selectedGraph = SceneGraph.accessPickedGraph();
		selectedFreeDraw = SceneGraph.accessPickedFreeDraw();

		if (track > -1) {
			SceneGraph.bringTrackToFront(selectedTrack);
			if (section > -1) {
				SceneGraph.bringSectionToFront(selectedTrack, selectedTrackSection);
			}
		}

		updateMainFrameListSelection(track, section, event);
	}

	/**
	 * Display method, which makes the call to SceneGraph.render. WARNING!!! Do
	 * not call SceneGraph.render anywhere else. Only this method can do so!
	 */
	public void display(final GLAutoDrawable drawable) {
		try {
			canvasLock.lock();
			{
				SceneGraph.lock();
				{
					String debug = System.getProperty("DEBUG");
					if (debug != null && System.getProperty("DEBUG").equals("true")) {
						long b4 = new Date().getTime();
						{
							SceneGraph.render(canvasId);
						}
						long after = new Date().getTime();

						System.out.println("Canvas: " + canvasId + "\t" + 1000.0f / (after - b4) + " fps");
					} else {
						SceneGraph.render(canvasId);
					}
				}
				SceneGraph.unlock();
			}
			canvasLock.unlock();
		} catch (Exception e) {
			System.out.println("EXCEPTION: ");
			e.printStackTrace();
		}
	}

	public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {
		// do nothing
	}
	
	private Component getPopupParent() { return CorelyzerApp.getApp().getPopupParent(this); }

	private void doDeleteSection() {
		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(getPopupParent(), "Are you sure?")) {
			CorelyzerApp app = CorelyzerApp.getApp();

			if (app != null) {
				app.getController().deleteSection(selectedTrack, selectedTrackSection);
				app.updateGLWindows();
			}
		}
	}
	
	private void doStaggerSections(final boolean stagger)
	{
		SceneGraph.staggerTrackSections(selectedTrack, stagger);
		CorelyzerApp.getApp().updateGLWindows();
	}
	
	private void doTrimSections()
	{
		TrimDialog td = new TrimDialog(this.canvas, selectedTrack, selectedTrackSection);
		td.pack();
		td.setLocationRelativeTo(null);
		td.setVisible(true);
	}
	
	private void doStackSections()
	{
		SceneGraph.stackSections(selectedTrack, selectedTrackSection);
		CorelyzerApp.getApp().updateGLWindows();
	}

	private void doExportTrack() {
		CorelyzerApp app = CorelyzerApp.getApp();

		if (app != null) {
			app.getController().exportSelectedTrack(this.canvas);
		}
	}
	
	private void doLockSection(final boolean lock) {
		CorelyzerApp app = CorelyzerApp.getApp();
		if ( app != null ) {
			SceneGraph.setSectionMovable( selectedTrack, selectedTrackSection, !lock );
		}
	}
	
	private void doLockSectionGraph(final boolean lock) {
		CorelyzerApp app = CorelyzerApp.getApp();
		if ( app != null ) {
			SceneGraph.setSectionGraphMovable( selectedTrack, selectedTrackSection, !lock );
		}		
	}

	private void doGraphDialog() {
		CorelyzerApp view = CorelyzerApp.getApp();
		Session session = view.getSelectedSession();
		Vector<WellLogDataSet> datasets = session.getDatasets();

		if (datasets.size() <= 0) {
			JOptionPane.showMessageDialog(getPopupParent(), "At least one dataset must be loaded.");

			return;
		}

		// set and show corelyzer.ui.DrawGraphDialog
		CRDefaultListModel model = CorelyzerApp.getApp().getTrackListModel();

		int t = SceneGraph.accessPickedTrack();
		int s = SceneGraph.accessPickedSection();

		// System.out.println("[Bremen] doGraphDialog() trackId:   " + t);
		// System.out.println("[Bremen] doGraphDialog() sectionId: " + s);

		// here we need some trick to match native index and java list index
		// this results from adding deletion feature of track and section
		// index conversion (native to java list)
		int i, j, tsize, ssize;
		boolean found = false;
		tsize = model.getSize();
		TrackSceneNode tt;
		CoreSection cs; // = null;
		for (i = 0; i < tsize; i++) {
			tt = (TrackSceneNode) model.elementAt(i);
			if (t == tt.getId()) {
				t = i;
				ssize = tt.getNumCores();
				for (j = 0; j < ssize; j++) {
					cs = tt.getCoreSection(j);
					if (s == cs.getId()) {
						s = j;
						found = true;
						break;
					}
				}
				break;
			}
		}
		if (!found) {
			return;
			// end of index matching
		}

		// Show graph dialog
		CRGraphDialog graphDialog = new CRGraphDialog(this.canvas);
		graphDialog.setDatasetVec(datasets);
		graphDialog.setSelectedTrackAndSectionId(t, s);

		graphDialog.pack();
		graphDialog.setSize(480, 500);
		graphDialog.setVisible(true);
	}

	private void doHideTrack() {
		CorelyzerApp app = CorelyzerApp.getApp();

		if (app != null) {
			app.getController().setTrackVisible();
			app.updateGLWindows();
		}
	}

	public GLCanvas getCanvas() {
		return canvas;
	}

	public int getCanvasID() {
		return canvasId;
	}

	// Generalize and replace the above 3 methods
	private void handleAnnotationEvent(final String annotationTypeName, final String sessionname, final String trackname, final String secname,
			final float[] upperLeft, final float[] lowerRight) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		if (annotationTypeName.equalsIgnoreCase("cancel")) {
			return;
		}

		AnnotationTypeDirectory dir = AnnotationTypeDirectory.getLocalAnnotationTypeDirectory();
		if (dir == null) {
			System.out.println("Null AnnotationTypeDirectory");
			return;
		}

		AnnotationType t = dir.getAnnotationType(annotationTypeName);
		if (t == null) {
			System.out.println("Null AnnotationType");
			return;
		}

		String formClassName = t.getFormName();
		AbstractAnnotationDialog dlg = (AbstractAnnotationDialog) Class.forName(formClassName).newInstance();

		Date now = new Date(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyy hh:mm:ss a z");
		String time = format.format(now);

		// init empty dictionary
		String dictFilePath = "resources/annotations/" + t.getDictFilename();
		File f = new File(dictFilePath);
		Hashtable<String, String> dict = PropertyListUtility.generateHashtableFromFile(f);

		System.out.println("- [DEBUG] Init empty dictionary with file: " + f.getAbsolutePath() + ", size: " + dict.size());

		dlg.setAttributes(dict);
		dlg.setValueForKey("sessionname", sessionname);
		dlg.setValueForKey("trackname", trackname);
		dlg.setValueForKey("corename", secname);
		dlg.setValueForKey("username", System.getProperty("user.name"));
		dlg.setValueForKey("date", time);

		dlg.setTrackId(selectedTrack);
		dlg.setSectionId(selectedTrackSection);
		dlg.setRange(upperLeft[0], upperLeft[1], lowerRight[0], lowerRight[1]);

		dlg.pack();
		dlg.setLocation(prePos.x + 20, prePos.y - dlg.getSize().height / 2);

		dlg.setVisible(true);
	}

	private void handleClastMouseReleased(final MouseEvent e) {
		if (selectedTrackSection != -1) {
			Point releasePos = e.getPoint();
			float[] releaseScenePos = { 0.0f, 0.0f };
			float[] releaseAbsPos = { 0.0f, 0.0f };

			convertMousePointToSceneSpace(releasePos, releaseScenePos);

			if (!SceneGraph.getDepthOrientation()) {
				float t = scenePos[0];
				scenePos[0] = scenePos[1];
				scenePos[1] = -t;
			}

			SceneGraph.addClastPoint2(scenePos[0], scenePos[1]);

			convertScenePointToAbsolute(scenePos, releaseAbsPos);
			CorelyzerApp.getApp().getToolFrame().setClastLowerRight(releaseAbsPos);
			float[] clastUpperLeft = CorelyzerApp.getApp().getToolFrame().getClastUpperLeft();

			String trackname = CorelyzerApp.getApp().getTrackListModel().getElementAt(selectedTrackIndex).toString();
			String secname = CorelyzerApp.getApp().getSectionListModel().getElementAt(selectedTrackSectionIndex).toString();
			String sessionname = CoreGraph.getInstance().getCurrentSession().getName();

			// Different kinds of annotations
			AnnotationTypeDirectory dir = AnnotationTypeDirectory.getLocalAnnotationTypeDirectory();
			if (dir == null) {
				System.out.println("Null AnnotationTypeDirectory abort.");
				return;
			}

			Enumeration<String> keys = dir.keys();
			Vector<String> annotationOptions = new Vector<String>();
			annotationOptions.add("Cancel");

			while (keys.hasMoreElements()) {
				annotationOptions.add(keys.nextElement());
			}

			Object[] options = annotationOptions.toArray();

			int sel = JOptionPane.showOptionDialog(getPopupParent(), "Which kind of Annotation?", "Annotation Form Selector", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

			if (sel < 0) {
				return;
			}

			try {
				this.handleAnnotationEvent(options[sel].toString(), sessionname, trackname, secname, clastUpperLeft, releaseAbsPos);
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void handleCutMouseReleased(final MouseEvent e) {
		// TODO: series of cutting actions
		if (selectedTrackSection != -1) {
			// Get selection info
			Point releasePos = e.getPoint();
			float[] releaseScenePos = { 0.0f, 0.0f };
			float[] releaseAbsPos = { 0.0f, 0.0f };

			convertMousePointToSceneSpace(releasePos, releaseScenePos);

			if (!SceneGraph.getDepthOrientation()) {
				float t = scenePos[0];
				scenePos[0] = scenePos[1];
				scenePos[1] = -t;
			}

			SceneGraph.addClastPoint2(scenePos[0], scenePos[1]);

			convertScenePointToAbsolute(scenePos, releaseAbsPos);
			CorelyzerApp.getApp().getToolFrame().setClastLowerRight(releaseAbsPos);
			float[] clastUpperLeft = CorelyzerApp.getApp().getToolFrame().getClastUpperLeft();

			// Check whether this is the root section
			int parentTrackId = SceneGraph.getSectionParentTrackId(selectedTrack, selectedTrackSection);
			int parentSectionId = SceneGraph.getSectionParentSectionId(selectedTrack, selectedTrackSection);

			if (parentTrackId != -1 && parentSectionId != -1) {
				String mesg = "You can only cut the original sections";
				JOptionPane.showMessageDialog(getPopupParent(), mesg);

				return;
			}

			// todo: disable original track in selection
			int newTrackId = CRUtility.getTargetTrackID(CorelyzerApp.getApp(), this.canvas);
			String newTrackName = SceneGraph.getTrackName(newTrackId);

			/*
			 * String newSectionName = JOptionPane.showInputDialog(this.canvas,
			 * "Enter the section name");
			 * 
			 * if(newTrackName == null || newTrackName.equals("") ||
			 * newSectionName.equals("")) {
			 * JOptionPane.showMessageDialog(this.canvas, "Invalid inputs");
			 * return; }
			 */

			// convert to meter
			float startDepth = clastUpperLeft[0] / 100.0f;
			float endDepth = releaseAbsPos[0] / 100.0f;

			int[] loc = { selectedTrack, selectedTrackSection };
			CRExperimentController.cutIntervalToNewTrack(loc, startDepth, endDepth, CRExperimentController.ABSOLUTE_DEPTH, newTrackName, null);
		}
	}

	// Handles events from right mouse clicks, including bringing up
	// the correct popup menu items.
	void handleRightMouseClick(final MouseEvent e) {
		Point p = e.getPoint();

		// save a keep of click position
		// so some menu actions can make use of it
		this.rightClickPos = p;

		// float sp[] = {0.0f, 0.0f};
		this.convertMousePointToSceneSpace(p, scenePos);

		// System.out.println("---- Mouse Right Click at " +
		// p.x + ", " + p.y);
		// System.out.println("---- Which is Scene Space: " +
		// scenePos[0] + ", " + scenePos[1]);

		determineSelectedSceneComponents(scenePos, e);

		this.scenePopupMenu.show(e.getComponent(), p.x, p.y);

		selectedMarker = SceneGraph.accessPickedMarker();

		// System.out.println("MarkerId: " + selectedMarker + " is selected");

		if (selectedMarker < 0) {
			return;
		} else {
			// JMenuItem title =
			// (JMenuItem) this.scenePopupMenu.getComponent(4);
			// title.setText("Edit Annotation");
			// this.annotationEditMenu.setEnabled(true);
			// this.annotationAddMenu.setEnabled(false);
		}

		this.scenePopupMenu.repaint();
	}

	/**
	 * Implementation of init method for a GLEventListener
	 */
	public void init(final GLAutoDrawable drawable) {
		// init
		drawable.getGL().setSwapInterval(0);
	}

	public void keyPressed(final KeyEvent event) {
		canvasLock.lock();

		char key = event.getKeyChar();
		if (key == '+' || key == '=' || key == '-') { // Zoom
			float dS = 1.33f;
			float scale = 1.0f;
			float[] cp = { 0.0f, 0.0f };
			float[] sc = { 0.0f, 0.0f };

			Point mousePos = canvas.getMousePosition();

			this.convertMousePointToSceneSpace(mousePos, cp);
			sc[0] = SceneGraph.getSceneCenterX();
			sc[1] = SceneGraph.getSceneCenterY();

			switch (key) {
				case '+':
				case '=':
					scale = 1 / dS;
					break;

				case '-':
					scale = dS;
					break;
			}

			SceneGraph.scaleScene(scale);

			float ncp[] = { 0.0f, 0.0f };
			this.convertMousePointToSceneSpace(mousePos, ncp);

			ncp[0] = ncp[0] - cp[0];
			ncp[1] = ncp[1] - cp[1];

			SceneGraph.panScene(-ncp[0], -ncp[1]);
		} else if (key == 'j' || key == 'J') {
			CorelyzerApp app = CorelyzerApp.getApp();
			app.getMainFrame().setAlwaysOnTop(false);

			String inputValue = JOptionPane.showInputDialog(getPopupParent(), "Please input the depth to jump to in meters: ");

			app.getMainFrame().setAlwaysOnTop(true);

			if (inputValue == null) {
				return;
			}

			try {
				float depthValue = Float.parseFloat(inputValue);
				float dpix = SceneGraph.getCanvasDPIX(this.canvasId);
				float dpiy = SceneGraph.getCanvasDPIY(this.canvasId);

				float px, py;
				if (SceneGraph.getDepthOrientation()) {
					px = depthValue * 100.0f / 2.54f * dpix;
					py = 0;
				} else {
					px = 0;
					py = depthValue * 100.0f / 2.54f * dpiy;
				}

				SceneGraph.lock();
				{
					SceneGraph.positionScene(px, py);
				}
				SceneGraph.unlock();
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(getPopupParent(), "Please type in a number", "Alert", JOptionPane.ERROR_MESSAGE);
			}
		} else if (key == KeyEvent.VK_OPEN_BRACKET || key == KeyEvent.VK_CLOSE_BRACKET || key == '9' || key == '0') {
			// graph scaling
			float dS = 1.00f;

			if (key == KeyEvent.VK_OPEN_BRACKET || key == '9') {
				dS = 1.0f / 1.33f;
			} else if (key == KeyEvent.VK_CLOSE_BRACKET || key == '0') {
				dS = 1.33f;
			}

			SceneGraph.setGraphScale(dS);
			// SceneGraph.setMarkerScale(dS);
		} else if (key == 'D') { // Show onscreen debug information in
									// scenegraph
			boolean b = SceneGraph.getDebug();
			SceneGraph.setDebug(!b);
		} else if ( key == 'Q' || key == 'W' || key == 'E' || key == 'R') {
			int keyId = 0;
			if ( key == 'Q' )
				keyId = 1; // scaling
			if ( key == 'W' )
				keyId = 2; // labels
			if ( key == 'E' )
				keyId = 3; // border
			else if ( key == 'R' )
				keyId = 4; // scissoring
			SceneGraph.debugKey( keyId );
		} else { // pan
			float movX = 0.0f;
			float movY = 0.0f;

			int dX = 10;
			int dY = 10;

			float sx, sy;
			float w, h;

			w = SceneGraph.getCanvasWidth(canvasId);
			h = SceneGraph.getCanvasHeight(canvasId);

			sx = w / canvas.getWidth();
			sy = h / canvas.getHeight();

			switch (event.getKeyCode()) {
				case KeyEvent.VK_UP:
					movX = 0.0f;
					movY = -dY * sy;
					break;

				case KeyEvent.VK_DOWN:
					movX = 0.0f;
					movY = dY * sy;
					break;

				case KeyEvent.VK_LEFT:
					movX = -dX * sx;
					movY = 0.0f;
					break;

				case KeyEvent.VK_RIGHT:
					movX = dX * sx;
					movY = 0.0f;
					break;
			}

			SceneGraph.panScene(movX, movY);
		}

		canvasLock.unlock();
		CorelyzerApp.getApp().updateGLWindows();
	}

	public void keyReleased(final KeyEvent event) {
	}

	public void keyTyped(final KeyEvent event) {
	}

	public void mouseClicked(final MouseEvent e) {
		Point prePos = e.getPoint();
		float sp[] = { 0.0f, 0.0f };
		this.convertMousePointToSceneSpace(prePos, scenePos);

		switch (e.getButton()) {
			case MouseEvent.BUTTON1:
				if (e.getClickCount() > 1) { // doubleclick zoom in/out
					Point mousePos = e.getPoint();

					float[] cp = { 0.0f, 0.0f };
					float[] sc = { 0.0f, 0.0f };

					canvasLock.lock();

					convertMousePointToSceneSpace(mousePos, cp);

					sc[0] = SceneGraph.getSceneCenterX();
					sc[1] = SceneGraph.getSceneCenterY();

					// alt down for zoom out?!
					float base;
					if (e.isAltDown()) { // zoom out
						base = 1.05f;
					} else { // zoom out
						base = 0.95f;
					}

					float scale = (float) Math.pow(base, 3);

					SceneGraph.scaleScene(scale);

					float ncp[] = { 0.0f, 0.0f };

					this.convertMousePointToSceneSpace(mousePos, ncp);
					ncp[0] = ncp[0] - cp[0];
					ncp[1] = ncp[1] - cp[1];

					SceneGraph.panScene(-ncp[0], -ncp[1]);
					canvasLock.unlock();

					CorelyzerApp.getApp().updateGLWindows();
					return;
				}

				// measuring mode
				if (canvasMode == CorelyzerApp.APP_MEASURE_MODE) {
					int nPoint = SceneGraph.addMeasurePoint(scenePos[0], scenePos[1]);
					this.convertScenePointToAbsolute(scenePos, sp);
					if (nPoint == 1) {
						CorelyzerApp.getApp().getToolFrame().addMeasure(sp, 1);
					} else if (nPoint == 2) {
						CorelyzerApp.getApp().getToolFrame().addMeasure(sp, 2);
					}

					PAN_MODE = 0;
					return;
				}
				
				if (selectedFreeDraw > -1) {
					CorelyzerApp
							.getApp()
							.getPluginManager()
							.broadcastEventToPlugin(SceneGraph.getFreeDrawPluginID(selectedFreeDraw), CorelyzerPluginEvent.FREE_DRAW_SELECTED,
									"" + selectedFreeDraw);
					return;
				}

				selectedMarker = SceneGraph.accessPickedMarker();

				if (selectedMarker < 0) {
					if (canvasMode == CorelyzerApp.APP_MARKER_MODE) {
						SceneGraph.setCoreSectionMarkerFocus(false);
						CorelyzerApp.getApp().updateGLWindows();
					}
					return;
				} else {
					if (canvasMode == CorelyzerApp.APP_NORMAL_MODE) {
						AnnotationUtils.openAnnotation(getCanvas(), selectedTrack, selectedTrackSection, selectedMarker);
					} else { // marker mode
						// TODO do something if want to allow user adjust the
						// selected
						// TODO block, like sync to ClastInfo hash...
						if (selectedMarker != SceneGraph.focusedMarker || selectedTrackSection != SceneGraph.focusedTrackSection
								|| selectedTrack != SceneGraph.focusedTrack) {
							SceneGraph.setCoreSectionMarkerFocus(false);

							SceneGraph.focusedTrack = selectedTrack;
							SceneGraph.focusedTrackSection = selectedTrackSection;
							SceneGraph.focusedMarker = selectedMarker;
							SceneGraph.setCoreSectionMarkerFocus(true);
						}
					}
				}

				break;

			case MouseEvent.BUTTON2:
				/*
				 * System.out.println("---- Middle button clicked at: " +
				 * prePos.x + ", " + prePos.y);
				 * System.out.println("Converted to Scene Space: " + scenePos[0]
				 * + ", " + scenePos[1]);
				 */
				break;

			case MouseEvent.BUTTON3:
				/*
				 * System.out.println("---- Right button clicked at: " +
				 * prePos.x + ", " + prePos.y);
				 * System.out.println("Converted to Scene Space: " + scenePos[0]
				 * + ", " + scenePos[1]);
				 */
				this.handleRightMouseClick(e);
				break;
			default:
				if (e.isPopupTrigger()) {
					this.handleRightMouseClick(e);
				}
		}
	}

	/**
	 * Handles mouse dragging events: panning, sliding sections, sliding tracks,
	 * and trackpad based zooming.
	 */
	public void mouseDragged(final MouseEvent e) {

		Point currentPos = e.getPoint();
		int dX = currentPos.x - prePos.x;
		int dY = currentPos.y - prePos.y;

		float sx, sy;
		float w, h;

		canvasLock.lock();

		w = SceneGraph.getCanvasWidth(canvasId);
		h = SceneGraph.getCanvasHeight(canvasId);

		sx = w / canvas.getWidth();
		sy = h / canvas.getHeight();

		this.convertMousePointToSceneSpace(currentPos, scenePos);

		// play a bit measuring test
		if (canvasMode == CorelyzerApp.APP_MEASURE_MODE) {
			SceneGraph.positionMouse(scenePos[0], scenePos[1]);

			// some work for panning
			// automatically pan mode
			canvas.setCursor(new Cursor(Cursor.MOVE_CURSOR));
			SceneGraph.panScene(-dX * sx, -dY * sy);

			canvasLock.unlock();
			prePos = currentPos;
			CorelyzerApp.getApp().updateGLWindows();
			return;
		} else if (canvasMode == CorelyzerApp.APP_CLAST_MODE || canvasMode == CorelyzerApp.APP_CUT_MODE) {
			SceneGraph.positionMouse(scenePos[0], scenePos[1]);

			// Don't have a selected section, just pan
			if (selectedTrackSection == -1) {
				canvas.setCursor(new Cursor(Cursor.MOVE_CURSOR));
				SceneGraph.panScene(-dX * sx, -dY * sy);
			}

			canvasLock.unlock();
			prePos = currentPos;

			CorelyzerApp.getApp().updateGLWindows();
			return;
		}

		// check focused marker manipulation
		if (MANIPULATE_MODE == 1) {
			float dx, dy;
			dx = scenePos[0] - prescenePos[0];
			dy = scenePos[1] - prescenePos[1];
			prescenePos[0] = scenePos[0];
			prescenePos[1] = scenePos[1];
			SceneGraph.manipulateMarker(canvasId, dx, dy);
			SceneGraph.positionMouse(scenePos[0], scenePos[1]);
			canvasLock.unlock();
			prePos = currentPos;
			CorelyzerApp.getApp().updateGLWindows();
			return;
		} else {
			if (e.isAltDown()) // slide track section
			{
				// TODO consider separate move of section image and graph
				// moveSectionImage & moveSectionGraph
				if (selectedTrack >= 0 && selectedTrackSection >= 0) {
					if (canvas.getCursor().getType() != Cursor.HAND_CURSOR) {
						canvas.setCursor(new Cursor(Cursor.HAND_CURSOR));
					}

					// depth orientation
					float tX = SceneGraph.getDepthOrientation() ? dX * sx : dY * sy;

					// allow vertical movements?
					float tY = 0;
					// float tY = SceneGraph.getDepthOrientation() ?
					// (dY * sy) : (-dX * sx);

					if (selectedGraph >= 0) {
						// moving graph instead of whole section
						SceneGraph.moveSectionGraph(selectedTrack, selectedTrackSection, tX, tY);
					} else {
						Object[] sections = CorelyzerApp.getApp().getSectionList().getSelectedValues();
						int[] secids = new int[sections.length];
						for (int i = 0; i < sections.length; i++) {
							CoreSection cs = (CoreSection)sections[i];
							secids[i] = (cs != null ? cs.getId() : -1);
						}
						SceneGraph.moveSections(selectedTrack, secids, tX, tY);
					}

					// broadcast event to plugins
					String msg = "";
					msg = msg + selectedTrack + "\t" + selectedTrackSection;
					msg = msg + "\t" + dX * sx / SceneGraph.getCanvasDPIX(canvasId) + "\t0";

					CorelyzerApp.getApp().getPluginManager().broadcastEventToPlugins(CorelyzerPluginEvent.SECTION_MOVED, msg);
				}
			} else if (e.isShiftDown()) { // slide track
				if (selectedTrack >= 0) {
					if (canvas.getCursor().getType() != Cursor.HAND_CURSOR) {
						canvas.setCursor(new Cursor(Cursor.HAND_CURSOR));
					}

					// fine tune allows depth movements
					if (isFineTune() && finetuneDialog != null) {
						// depth orientation
						if (SceneGraph.getDepthOrientation()) { // landscape
							SceneGraph.moveTrack(selectedTrack, dX * sx, dY * sy);
						} else { // Portrait
							SceneGraph.moveTrack(selectedTrack, dY * sy, -dX * sx);
						}

						float currentTrackPosX = SceneGraph.getTrackXPos(selectedTrack);
						float canvasDPIX = SceneGraph.getCanvasDPIX(0);

						JTextField fineTuneCoreADepthStatus = finetuneDialog.getCoreAAdjustedDepthTextField();
						JTextField fineTuneCoreBDepthStatus = finetuneDialog.getCoreBAdjustedDepthTextField();
						int coreANativeID = finetuneDialog.getCoreANativeID();
						int coreBNativeID = finetuneDialog.getCoreBNativeID();
						float coreAOrigDepth = finetuneDialog.getCoreAOrigDepth();
						float coreBOrigDepth = finetuneDialog.getCoreBOrigDepth();

						if (fineTuneCoreADepthStatus != null && coreANativeID == selectedTrack) {
							float depth = coreAOrigDepth + 2.54f * currentTrackPosX / (100 * canvasDPIX);
							fineTuneCoreADepthStatus.setText(String.valueOf(depth));
						}

						if (fineTuneCoreBDepthStatus != null && coreBNativeID == selectedTrack) {
							float depth = coreBOrigDepth + 2.54f * currentTrackPosX / (100 * canvasDPIX);
							fineTuneCoreBDepthStatus.setText(String.valueOf(depth));
						}
					} else { // normal
						// depth orientation
						if (SceneGraph.getDepthOrientation()) {
							SceneGraph.moveTrack(selectedTrack, 0.0f, dY * sy);
						} else {
							SceneGraph.moveTrack(selectedTrack, 0.0f, -dX * sx);
						}
					}

					// broadcast message to plugins
					String msg = "";
					msg = msg + selectedTrack;
					msg = msg + "\t0\t" + dY * sx / SceneGraph.getCanvasDPIY(canvasId);
					CorelyzerApp.getApp().getPluginManager().broadcastEventToPlugins(CorelyzerPluginEvent.TRACK_MOVED, msg);
				}
			} else if (e.isControlDown()) // zooming
			{
				sy = (float) dY / (float) canvas.getHeight();
				SceneGraph.scaleScene(1.0f + sy);
			} else if (PAN_MODE == 1) {
				// automatically pan mode
				canvas.setCursor(new Cursor(Cursor.MOVE_CURSOR));
				SceneGraph.panScene(-dX * sx, -dY * sy);
			}
		} // end of else (manipulation mode)

		SceneGraph.positionMouse(scenePos[0], scenePos[1]);
		canvasLock.unlock();
		prePos = currentPos;
		CorelyzerApp.getApp().updateGLWindows();

	}

	public void mouseEntered(final MouseEvent e) {
		// System.out.println("MOUSE Entered EVENT!!!!\n");
		CorelyzerApp.getApp().relocateToolMenu(canvasId);

		e.getComponent().requestFocus();
	}

	public void mouseExited(final MouseEvent e) {
		// System.out.println("MOUSE Exited EVENT!!!!\n");
	}

	public void mouseMoved(final MouseEvent e) {
		if (SceneGraph.hasCrossHair()) {
			this.convertMousePointToSceneSpace(e.getPoint(), scenePos);

			String msg = "";
			msg = msg + scenePos[0] / SceneGraph.getCanvasDPIX(canvasId) + "\t" + scenePos[1] / SceneGraph.getCanvasDPIY(canvasId);

			CorelyzerApp.getApp().getPluginManager().broadcastEventToPlugins(CorelyzerPluginEvent.MOUSE_MOTION, msg);

			// set mouse position to draw cross hair
			SceneGraph.positionMouse(scenePos[0], scenePos[1]);
			CorelyzerApp.getApp().updateGLWindows();
		}
	}

	public void mousePressed(final MouseEvent e) {
		prePos = e.getPoint();
		float sp[] = { 0.0f, 0.0f };
		this.convertMousePointToSceneSpace(prePos, scenePos);

		PAN_MODE = 0;
		ZOOM_MODE = 0;
		MANIPULATE_MODE = 0;

		// For mouse right-click or Ctrl-Left-Click
		if (e.isPopupTrigger()) {
			this.handleRightMouseClick(e);

			return;
		}

		switch (e.getButton()) {
			case MouseEvent.BUTTON1:

				if (canvasMode == CorelyzerApp.APP_MEASURE_MODE) {
					return;
				} else if (canvasMode == CorelyzerApp.APP_MARKER_MODE) {

					// the first check up: focused marker manipulation
					if (SceneGraph.focusedMarker > -1) {
						if (SceneGraph.hitMarker(canvasId, scenePos[0], scenePos[1])) {
							MANIPULATE_MODE = 1;

							/*
							 * System.out.println(
							 * "---- Left button pressed down at: " + prePos.x +
							 * ", " + prePos.y); System.out.println(
							 * "Converted to Scene Space: " + scenePos[0] + ", "
							 * + sp[1]);
							 * System.out.println("Marker manipulator hit!");
							 */

							prescenePos[0] = scenePos[0];
							prescenePos[1] = scenePos[1];

							return;
						}
					}
				} else if (canvasMode == CorelyzerApp.APP_CLAST_MODE || canvasMode == CorelyzerApp.APP_CUT_MODE)

				{
					determineSelectedSceneComponents(scenePos, e);

					if (selectedTrackSection != -1) {
						if (!SceneGraph.getDepthOrientation()) {
							float t = scenePos[0];
							scenePos[0] = scenePos[1];
							scenePos[1] = -t;
						}

						SceneGraph.addClastPoint1(scenePos[0], scenePos[1]);
						this.convertScenePointToAbsolute(scenePos, sp);

						CorelyzerApp.getApp().getToolFrame().setClastUpperLeft(sp);
					}

					PAN_MODE = 0;
					return;
				}

				PAN_MODE = 1;

				// System.out.println("---- Left button pressed down at: " +
				// prePos.x + ", " + prePos.y);
				// System.out.println("Converted to Scene Space: " +
				// scenePos[0] + ", " + sp[1]);

				determineSelectedSceneComponents(scenePos, e);

				break;
			case MouseEvent.BUTTON2:
				// System.out.println("MOUSE BUTTON 2!!!???");
				break;
			case MouseEvent.BUTTON3:
				// System.out.println("---- Right button clicked at: " +
				// prePos.x + ", " + prePos.y);
				// System.out.println("Converted to Scene Space: " +
				// scenePos[0] + ", " + sp[1]);
				this.handleRightMouseClick(e);
				break;
			default:
				// more reliable than BUTTON3 (ctrl-click)
				if (e.isPopupTrigger()) {
					this.handleRightMouseClick(e);
				}
		}
	}

	public void mouseReleased(final MouseEvent e) {
		// For mouse right-click or Ctrl-Left-Click
		if (e.isPopupTrigger()) {
			this.handleRightMouseClick(e);

			return;
		}

		if (e.getButton() == MouseEvent.BUTTON1) {
			if (canvasMode == CorelyzerApp.APP_CLAST_MODE) {
				this.handleClastMouseReleased(e);
			} else if (canvasMode == CorelyzerApp.APP_CUT_MODE) {
				this.handleCutMouseReleased(e);
			}
		}

		prePos.setLocation(0, 0);
		canvas.setCursor(Cursor.getDefaultCursor());

		CorelyzerApp.getApp().updateGLWindows();
	}

	// MouseListeners methods ----------------------------------------
	/**
	 * Handles mouse wheel events, in particular for the zooming operations.
	 */
	public void mouseWheelMoved(final MouseWheelEvent e) {
		boolean useDepthScroll;

		try {
			useDepthScroll = Boolean.parseBoolean(CorelyzerApp.getApp().preferences().getProperty("ui.verticalDepthScroll"));
		} catch (Exception ex) {
			useDepthScroll = false;
		}

		if (SceneGraph.getDepthOrientation() == SceneGraph.VERTICAL && useDepthScroll) {
			this.scrollSceneWithScrollWheel(e);
		} else {
			this.zoomSceneWithScrollWheel(e);
		}

		CorelyzerApp.getApp().updateGLWindows();
	}

	public void reshape(final GLAutoDrawable drawble, final int x, final int y, final int w, final int h) {
		// do nothing
	}
	
	public void dispose(final GLAutoDrawable drawable) { }

	private void scrollSceneWithScrollWheel(final MouseWheelEvent e) {
		int scrollAmount = e.getScrollAmount();
		int wheelRotation = e.getWheelRotation();
		int ample = 3;

		Point currentPos = e.getPoint();
		int dY = ample * scrollAmount * wheelRotation;

		float sy;
		float h;

		canvasLock.lock();
		{
			h = SceneGraph.getCanvasHeight(canvasId);

			sy = h / canvas.getHeight();

			SceneGraph.panScene(0, dY * sy);

			this.convertMousePointToSceneSpace(currentPos, scenePos);
			SceneGraph.positionMouse(scenePos[0], scenePos[1]);
		}
		canvasLock.unlock();
		prePos = currentPos;
	}

	private void setEnableSectionBasedPopupMenuOptions(final boolean b) {
		for (int idx : CorelyzerGLCanvas.sectionBasedPopupMenuItemIndices) {
			this.scenePopupMenu.getComponent(idx).setEnabled(b);
		}
	}

	public void setMode(final int imode) {

		// change canvas mode
		canvasMode = imode;

		// update popupmenu mode button
		switch (imode) {
			case 0:
				this.normalMode.setSelected(true);
				break;

			case 1:
				this.measureMode.setSelected(true);
				break;

			case 2:
				this.markerMode.setSelected(true);
				break;

			case 3:
				this.clastMode.setSelected(true);
				break;

			case 4:
				this.cutMode.setSelected(true);
				break;
		}
	}
	
	// return integer array with values between min and max inclusive
	private int[] makeRangeArray(final int min, final int max) {
		int[] result = {};
		if (min <= max) {
			final int arraySize = max - min + 1;
			result = new int[arraySize];
			for (int i = 0; i < arraySize; i++) { result[i] = i + min; }
		}
		return result;
	}

	private void updateMainFrameListSelection(final int track, final int section, final MouseEvent event) {
		CorelyzerApp app = CorelyzerApp.getApp();
		if (app == null) {
			return;
		}

		if (track >= 0) {
			// Now, we need to traverse app's list model
			// to find match of native id
			// index conversion (native to java list)

			CRDefaultListModel sessionModel = app.getSessionListModel();
			int sessionIndex = -1;
			TrackSceneNode trackNode = null;
			for (int i = 0; i < sessionModel.size(); i++) {
				Session session = (Session) sessionModel.elementAt(i);
				trackNode = session.getTrackSceneNodeWithTrackId(track);

				if (trackNode != null) {
					sessionIndex = i;
				}
			}

			if (sessionIndex < 0) {
				return;
			}

			// Set selected session
			app.getSessionList().setSelectedIndex(sessionIndex);

			// Track
			int ssize;
			boolean found = false;
			CRDefaultListModel tmodel = app.getTrackListModel();
			// tsize = tmodel.getSize();
			TrackSceneNode tt;
			CoreSection cs = null;

			for (int i = 0; i < tmodel.size() && !found; i++) {
				tt = (TrackSceneNode) tmodel.elementAt(i);

				if (track == tt.getId()) {
					selectedTrackIndex = i;
					ssize = tt.getNumCores();

					for (int j = 0; j < ssize; j++) {
						cs = tt.getCoreSection(j);
						if (section == cs.getId()) {
							selectedTrackSectionIndex = j;
							found = true;
							break;
						}
					}
				}
			}

			if (!found || cs == null) {
				return;
			}

			// update ui
			CorelyzerApp.getApp().getTrackList().setSelectedIndex(selectedTrackIndex);
			JList secList = CorelyzerApp.getApp().getSectionList();
			boolean selected = secList.isSelectedIndex(selectedTrackSectionIndex);
			List<Integer> indices = new ArrayList<Integer>();
			indices.addAll(Arrays.asList(ArrayUtils.toObject(secList.getSelectedIndices())));
			if (event.isControlDown() || (event.isMetaDown() && CorelyzerApp.MAC_OS_X)) { // toggle selection
				if (indices.contains(selectedTrackSectionIndex))
					indices.remove(new Integer(selectedTrackSectionIndex));
				else
					indices.add(selectedTrackSectionIndex);
				
				int[] newSelArray = ArrayUtils.toPrimitive(indices.toArray(new Integer[0]));
				secList.setSelectedIndices(newSelArray);
			} else if (event.isShiftDown()) { // select range
				int[] toSel = null;
				if (indices.size() == 0) {
					toSel = makeRangeArray(0, selectedTrackSectionIndex);
				} else {
					final int minSel = Collections.min(indices);
					final int maxSel = Collections.max(indices);
					if (selectedTrackSectionIndex < minSel) {
						toSel = makeRangeArray(selectedTrackSectionIndex, minSel);
					} else if (selectedTrackSectionIndex > maxSel) {
						toSel = makeRangeArray(maxSel, selectedTrackSectionIndex);
					}
				}
				secList.setSelectedIndices(toSel);
			} else if (!(event.isAltDown() && selected)) {
				// don't modify selection if Alt is down and section was already
				// selected...user is presumably trying to move it
				secList.setSelectedIndex(selectedTrackSectionIndex);
			}

			CRDefaultListModel lm = CorelyzerApp.getApp().getSectionListModel();
			String secName = null;
			if (lm != null) {
				Object selSec = lm.getElementAt(selectedTrackSectionIndex);
				if (selSec != null) {
					secName = selSec.toString();
				} else { System.out.println("no object at index"); }
			} else { System.out.println("no list model"); }

			JMenuItem title = (JMenuItem) this.scenePopupMenu.getComponent(0);
			String trackName = CorelyzerApp.getApp().getTrackListModel().getElementAt(selectedTrackIndex).toString();
			title.setText("Track: " + trackName);
			JMenuItem stitle = (JMenuItem) this.scenePopupMenu.getComponent(1);
			stitle.setText("Section: " + secName);

			// Enable section-based popupMenu options
			this.setEnableSectionBasedPopupMenuOptions(true);
			
			// 2/5/2012 brg: check Stagger Sections menu item if necessary
			final boolean trackIsStaggered = SceneGraph.trackIsStaggered(selectedTrack);
			AbstractButton ab = (AbstractButton)this.scenePopupMenu.getComponent(14);
			ab.getModel().setSelected(trackIsStaggered);
			
			// check section and graph lock menu items
			final boolean sectionIsLocked = !SceneGraph.isSectionMovable(selectedTrack, selectedTrackSection);
			ab = (AbstractButton)this.scenePopupMenu.getComponent(7);
			ab.getModel().setSelected(sectionIsLocked);
			final boolean sectionGraphIsLocked = !SceneGraph.isSectionGraphMovable(selectedTrack, selectedTrackSection);
			ab = (AbstractButton)this.scenePopupMenu.getComponent(8);
			ab.getModel().setSelected(sectionGraphIsLocked);

			CoreSectionImage csImg = cs.getCoreSectionImage();
			if (csImg != null && csImg.getId() != -1) {
				this.propertyMenuItem.setEnabled(true);
				this.splitMenuItem.setEnabled(true);
			} else {
				this.propertyMenuItem.setEnabled(false);
				this.splitMenuItem.setEnabled(false);
			}

			// System.out.println("---> [in Java DS] Picked Track " + track +
			// " and Track Section " + section);
			// String secname = CorelyzerApp.getApp().getSectionListModel().
			// getElementAt(section).toString();
			// System.out.println("---> [INFO] Section " + secname +
			// " is selected");
		} else {
			JMenuItem title = (JMenuItem) this.scenePopupMenu.getComponent(0);
			title.setText("Track: N/A");
			JMenuItem stitle = (JMenuItem) this.scenePopupMenu.getComponent(1);
			stitle.setText("Section: N/A");

			// disable section based items
			this.setEnableSectionBasedPopupMenuOptions(false);
		}
	}

	private void zoomSceneWithScrollWheel(final MouseWheelEvent e) {
		// FIXME MouseWheelEvent info
		/*
		 * System.out.println(e);
		 * 
		 * String mesg = "Component: " + e.getComponent(); mesg += ", id: " +
		 * e.getID(); mesg += ", when: " + e.getWhen(); mesg += ", modifiers: "
		 * + e.getModifiers(); mesg += ", x: " + e.getX(); mesg += ", y: " +
		 * e.getY(); mesg += ", clickCount: " + e.getClickCount(); mesg +=
		 * ", scrollType: " + e.getScrollType(); mesg += ", scrollAmount: " +
		 * e.getScrollAmount(); mesg += ", wheelRot: " + e.getWheelRotation();
		 * 
		 * System.out.println(mesg);
		 */
		// --

		int scrollAmount = e.getScrollAmount();
		int wheelRotation = e.getWheelRotation();
		Point mousePos = e.getPoint();

		/*
		 * // int scrollType = e.getScrollType(); // int scrollUnit =
		 * e.getUnitsToScroll();
		 * 
		 * System.out.println("---- ScrollAmount:   " + scrollAmount);
		 * System.out.println("---- ScrollType:     " + scrollType);
		 * System.out.println("---- ScrollUnit:     " + scrollUnit);
		 * System.out.println("---- WheelRotation:  " + wheelRotation);
		 * System.out.println("---- Mouse Location: " + mousePos);
		 */
		// determine what point to zoom in or out on

		float cp[] = { 0.0f, 0.0f }; // canvas position x,y
		float sc[] = { 0.0f, 0.0f }; // scene center x,y
		float s; // scale factor
		// float w; // canvas width

		canvasLock.lock();
		{
			this.convertMousePointToSceneSpace(mousePos, cp);

			sc[0] = SceneGraph.getSceneCenterX();
			sc[1] = SceneGraph.getSceneCenterY();

			/*
			 * System.out.println("Mouse Position Translated to " + cp[0] + ", "
			 * + cp[1]);
			 * 
			 * System.out.println("Difference from center " + (cp[0] - sc[0]) +
			 * ", " + (cp[1] - sc[1]));
			 */

			// each amount is approximately 5%

			if (wheelRotation <= 0) {
				s = (float) Math.pow(.95, scrollAmount);
			} else {
				s = (float) Math.pow(1.05f, scrollAmount);
			}

			// System.out.println("Scale Factor " + s);

			SceneGraph.scaleScene(s);

			// mouse in same place in screen, used to determine pan amount
			float ncp[] = { 0.0f, 0.0f };

			this.convertMousePointToSceneSpace(mousePos, ncp);

			// System.out.println("New Pos of mouse point after scale " + ncp[0]
			// +
			// ", " + ncp[1]);

			ncp[0] = ncp[0] - cp[0];
			ncp[1] = ncp[1] - cp[1];

			// System.out.println("Difference of " + ncp[0] + ", " + ncp[1]);
			SceneGraph.panScene(-ncp[0], -ncp[1]);
		}
		canvasLock.unlock();
	}
}
