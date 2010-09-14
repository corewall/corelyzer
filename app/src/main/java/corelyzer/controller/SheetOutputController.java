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
package corelyzer.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import corelyzer.data.CoreSection;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.graphics.SceneGraph;
import corelyzer.io.OutputWriter;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.SheetOutputDialog;
import corelyzer.util.FileUtility;

public class SheetOutputController implements ActionListener {
	boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

	private SheetOutputDialog view;

	// Track and Section IDs
	int currentDatasetIndex = -1;
	int currentTableIndex = -1;

	int selectedTrackId = -1; // native code index (scenegraph)
	int selectedSectionId = -1; // native code index (sceengraph)
	int selectedTrackListId = -1; // java side index (CorelyzerApp)
	int selectedSectionListId = -1; // java side index (CorelyzerApp)

	String timeformat;
	String dateformat;
	Date sampleDate;

	public SheetOutputController() {
		super();

		timeformat = "HH:mm:ss z";
		dateformat = "MM/dd/yy";
		sampleDate = new Date(System.currentTimeMillis());
	}

	public SheetOutputController(final SheetOutputDialog dlg) {
		this();
		view = dlg;
	}

	public void actionPerformed(final ActionEvent actionEvent) {
		if (actionEvent.getSource().equals(view.getTrackComboBox())) {
			JComboBox cb = (JComboBox) actionEvent.getSource();
			int idx = cb.getSelectedIndex();

			if (idx < 0) {
				return;
			}

			// update sectionsListModel
			CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
			TrackSceneNode tt = (TrackSceneNode) tmodel.elementAt(idx);
			int ssize = tt.getNumCores();
			CoreSection cs;

			view.getSectionComboBox().removeAllItems();
			for (int i = 0; i < ssize; i++) {
				cs = tt.getCoreSection(i);
				view.getSectionComboBox().addItem(cs.getName());
			}

			this.selectedTrackListId = idx; // java index
			this.selectedTrackId = tt.getId(); // native index

			if (ssize > 0) {
				view.getSectionComboBox().setSelectedIndex(0);
			}
		} else if (actionEvent.getSource().equals(view.getSectionComboBox())) {
			JComboBox cb = (JComboBox) actionEvent.getSource();
			int idx = cb.getSelectedIndex();

			if (idx < 0 || this.selectedTrackListId < 0) {
				return;
			}

			if (idx == this.selectedSectionListId) {
				return;
			}

			CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
			TrackSceneNode tt = (TrackSceneNode) tmodel.elementAt(this.selectedTrackListId);
			CoreSection cs = tt.getCoreSection(idx);

			this.selectedSectionListId = idx; // java index
			this.selectedSectionId = cs.getId(); // native index
		} else if (actionEvent.getSource().equals(view.getDateFormatComboBox())) {
			JComboBox cb = (JComboBox) actionEvent.getSource();
			int idx = cb.getSelectedIndex();

			if (idx < 0) {
				return;
			}

			if (idx == 0) {
				this.dateformat = "MM/dd/yy";
			} else {
				this.dateformat = (String) cb.getSelectedItem();
			}

			SimpleDateFormat format = new SimpleDateFormat(dateformat + " '-' " + timeformat);
			view.getExampleDateField().setText(format.format(sampleDate));

		}

	}

	public boolean generate() {
		if (view.getAllTracksCheckBox().isSelected()) {
			return this.generateMultipleTracks();
		} else {
			return this.generateSingleTrack();
		}
	}

	public boolean generateMultipleTracks() {
		String title = "Select output directory";
		String dirPath = FileUtility.selectADirectory(view, title);

		if (dirPath == null) {
			return false;
		}

		boolean result = true;
		for (int j = 0; j < view.getTrackComboBox().getItemCount(); j++) {
			TrackSceneNode t = (TrackSceneNode) view.getTrackComboBox().getItemAt(j);
			File selectedFile = new File(dirPath, (t.getName() + ".html"));

			for (int i = 0; i < t.getNumCores(); i++) {
				int sectionId = t.getCoreSection(i).getId();
				String sectionName = t.getCoreSection(i).getName();
				String aFilePath = generateSequenceFileName(selectedFile.getAbsolutePath(), sectionName);
				boolean res = writeASection(aFilePath, t.getId(), sectionId);

				result = result && res;
			}
		}

		return result;
	}

	private String generateSequenceFileName(final String aBaseFilePath, final String sectionName) {
		int dotIdx = aBaseFilePath.lastIndexOf(".");
		String prefix, suffix;

		if (dotIdx != -1) {
			prefix = aBaseFilePath.substring(0, dotIdx);
			suffix = aBaseFilePath.substring(dotIdx);
		} else {
			prefix = aBaseFilePath;
			suffix = ".html";
		}

		return prefix + "_" + sectionName + suffix;
	}

	public boolean generateSingleTrack() {
		String title = "Save Sheet As";
		String selected = FileUtility.selectASingleFile(view, title, "html", FileUtility.SAVE);

		File selectedFile = new File(selected);
		System.out.println("Save the file as: " + selectedFile);

		boolean result = true;
		if (view.getAllSectionsCheckBox().isSelected()) {
			// All sections
			TrackSceneNode t = (TrackSceneNode) view.getTrackComboBox().getSelectedItem();

			for (int i = 0; i < t.getNumCores(); i++) {
				int sectionId = t.getCoreSection(i).getId();
				String sectionName = t.getCoreSection(i).getName();
				String aFilePath = generateSequenceFileName(selectedFile.getAbsolutePath(), sectionName);

				boolean res = writeASection(aFilePath, this.selectedTrackId, sectionId);

				result = result && res;
			}
		} else { // single section
			result = writeASection(selectedFile.getAbsolutePath(), this.selectedTrackId, this.selectedSectionId);
		}

		return result;
	}

	// ---------------------------------------------------------------
	public void setSelectedSectionId(final int id) {
		if (id < 0) {
			return;
		}
		this.selectedSectionId = id;
	}

	// ---------------------------------------------------------------
	public void setSelectedSectionListId(final int id) {
		if (id < 0) {
			return;
		}
		this.selectedSectionListId = id;
	}

	// -------------- Copyed methodds ------------------------
	public void setSelectedTrackAndSectionId(final int trackId, final int sectionId) {

		// ids are based on java code might not same as native index
		// this results from deletion feature of tracks and sections
		// find native code index first
		int tid, sid;
		tid = sid = -1;
		int i, j, tsize, ssize;
		boolean found = false;
		CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
		tsize = tmodel.getSize();
		TrackSceneNode tt;
		CoreSection cs;
		for (i = 0; i < tsize; i++) {
			tt = (TrackSceneNode) tmodel.elementAt(i);
			if (trackId == i) {
				tid = tt.getId();
				ssize = tt.getNumCores();
				for (j = 0; j < ssize; j++) {
					cs = tt.getCoreSection(j);
					if (sectionId == j) {
						sid = cs.getId();
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

		setSelectedTrackListId(trackId); // java index
		setSelectedSectionListId(sectionId); // java index
		setSelectedTrackId(tid); // native index
		setSelectedSectionId(sid); // native index

	}

	// ---------------------------------------------------------------
	public void setSelectedTrackId(final int id) {
		if (id < 0) {
			return;
		}
		this.selectedTrackId = id;
	}

	// ---------------------------------------------------------------
	public void setSelectedTrackListId(final int id) {
		if (id < 0) {
			return;
		}
		this.selectedTrackListId = id;
	}

	public void updateList() {
		if (view == null) {
			return;
		}

		CorelyzerApp app = CorelyzerApp.getApp();
		if (app == null) {
			return;
		}

		int tsize, ssize;
		CRDefaultListModel tmodel = app.getTrackListModel();
		tsize = tmodel.getSize();

		TrackSceneNode tt;
		CoreSection cs;

		view.getTrackComboBox().removeAllItems();
		view.getSectionComboBox().removeAllItems();

		for (int i = 0; i < tsize; i++) {
			tt = (TrackSceneNode) tmodel.elementAt(i);
			view.getTrackComboBox().addItem(tt);

			if (i == 0) {
				ssize = tt.getNumCores();
				for (int j = 0; j < ssize; j++) {
					cs = tt.getCoreSection(j);
					view.getSectionComboBox().addItem(cs);
				}
			}
		}

		view.getTrackComboBox().setSelectedIndex(0);
	}

	private boolean writeASection(String aFilePath, final int trackId, final int sectionId) {
		if (!aFilePath.endsWith(".html")) {
			aFilePath += ".html";
		}

		System.out.println("---> [INFO] Writing '" + aFilePath + ": " + trackId + ", " + sectionId);

		// Check availability of image
		// does this section have image?
		// does this section have annotation?
		int imageId = SceneGraph.getImageIdForSection(trackId, sectionId);

		if (imageId == -1) {
			// this section does not have image
			JOptionPane.showMessageDialog(view, "This section does not have image!");
			return false;
		}

		System.out.println("Annotation output for track:" + trackId + " section:" + sectionId);

		int imgId = SceneGraph.getImageIdForSection(trackId, sectionId);

		String sectionName;
		if (imgId < 0) {
			sectionName = "N/A";
		} else {
			sectionName = new File(SceneGraph.getImageName(imgId)).getName();
			String[] toks = sectionName.split(".");

			if (toks.length > 0) {
				sectionName = toks[0];
			}
		}

		OutputWriter sw = new OutputWriter();
		sw.setProjectDesc(view.getProjectNameField().getText(), view.getSiteNameField().getText(), view.getHoleNameField().getText(), sectionName);

		sw.setDateFormat(this.dateformat + " '-' " + this.timeformat);

		return sw.writeHtml(aFilePath, trackId, sectionId);
	}
}
