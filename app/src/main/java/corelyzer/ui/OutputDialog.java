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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import corelyzer.data.CRPreferences;
import corelyzer.data.CoreSection;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.WellLogTable;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.graphics.SceneGraph;
import corelyzer.helper.ExampleFileFilter;
import corelyzer.io.OutputWriter;

/**
 * This class is used to help the user generate annotation output html of
 * section
 */
public class OutputDialog extends JFrame implements ActionListener, ListSelectionListener, TableModelListener, FocusListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7617584389671680149L;

	// Track Selection
	JComboBox tracksList;

	// Section Selection
	JComboBox sectionsList;

	// Date format Selection
	JComboBox dateformatList;

	// Dataset Selection
	JComboBox datasetList;
	DefaultComboBoxModel datasetModel;

	JCheckBox allSections;
	JCheckBox hourSelections; // check 24 hr, uncheck 12 hr

	JTextField projectText;
	JTextField siteText;
	JTextField holeText;
	JTextField sectionText;
	JTextField filenameText;
	JTextField dateformatText;

	JButton selectfilebtn;
	JButton generatebtn;
	JButton cancelbtn;

	Vector<WellLogDataSet> datasets;

	int currentDatasetIndex;
	int currentTableIndex;

	int selectedTrackId; // native code index (scenegraph)
	int selectedSectionId; // native code index (sceengraph)
	int selectedTrackListId; // java side index (CorelyzerApp)
	int selectedSectionListId; // java side index (CorelyzerApp)

	private JFileChooser chooser;

	String timeformat;
	String dateformat;
	Date sampleDate;

	// ---------------------------------------------------------------
	public OutputDialog(final String title) {
		super(title);

		currentDatasetIndex = -1;
		currentTableIndex = -1;

		selectedTrackId = -1;
		selectedSectionId = -1;

		selectedTrackListId = -1;
		selectedSectionListId = -1;

		this.setupUI();
		this.setSize(350, 400);
		this.setLocation(100, 100);

		timeformat = "HH:mm:ss z";
		dateformat = "MM/dd/yy";
		sampleDate = new Date(System.currentTimeMillis());
	}

	// ---------------------------------------------------------------
	/**
	 * Handles action events created by the close, apply and color buttons. As
	 * well as, the field checkbox table, and the dataset combobox selection.
	 */

	public void actionPerformed(final ActionEvent e) {

		if (e.getSource().equals(cancelbtn)) {
			// CLOSE BUTTON
			setVisible(false);
		} else if (e.getSource().equals(generatebtn)) {
			// APPLY BUTTON
			if (allSections.isSelected()) {
				// Todo: need to implement all section output
				int numsecs = SceneGraph.getNumSections(selectedTrackId);
				int temp = currentTableIndex;

				for (int i = 0; i < numsecs; i++) {
					currentTableIndex = this.getTableIndex(i);

				}

				currentTableIndex = temp;
			} else {
				// Todo: check availability of annotation
				// does this section have image?
				// does this section have annotation?
				int imageId = SceneGraph.getImageIdForSection(this.selectedTrackId, this.selectedSectionId);
				if (imageId == -1) {
					// this section does not have image
					JOptionPane.showMessageDialog(this, "This section does not have image!");
					return;
				}

				int numAnnotation = SceneGraph.getNumCoreSectionMarkers(this.selectedTrackId, this.selectedSectionId);
				if (numAnnotation < 1) {
					// this section does not have annotation
					JOptionPane.showMessageDialog(this, "This section does not annotation to print out!");
					return;
				}

				System.out.println("Annotation output  track:" + this.selectedTrackId + " section:" + this.selectedSectionId);
				OutputWriter sw = new OutputWriter();
				sw.setProjectDesc(this.projectText.getText(), this.siteText.getText(), this.holeText.getText(), this.sectionText.getText());
				sw.setDateFormat(this.dateformat + " '-' " + this.timeformat);
				boolean result = sw.writeHtml(this.filenameText.getText(), this.selectedTrackId, this.selectedSectionId);

				if (result) {
					// output success
					int retval = JOptionPane.showConfirmDialog(this, "Generated annotation output successfully.\n\n" + "Do you want to generate other output?",
							"Output Success", JOptionPane.YES_NO_OPTION);

					// keep working or exit to main app?
					if (retval == JOptionPane.YES_OPTION) {
						// restore some fields
						this.generatebtn.setEnabled(false);
						this.filenameText.setText("output filename");
					} else {
						// restore some fields and hide gui
						this.generatebtn.setEnabled(false);
						this.filenameText.setText("output filename");
						setVisible(false);
					}

				} else {
					// in case of failure, show up message
					JOptionPane.showMessageDialog(this, "Output generation failed!");
				}
			}
		} else if (e.getSource().equals(this.allSections)) {
			// APPLY TO TRACK
			System.out.println("allSections clicked " + allSections.isSelected());
		} else if (e.getSource().equals(this.tracksList)) {
			JComboBox cb = (JComboBox) e.getSource();
			int idx = cb.getSelectedIndex();
			System.out.println("trackList action selected id: " + idx);

			if (idx < 0) {
				return;
			}

			// if (idx == this.selectedTrackListId)
			// return;

			// update sectionsListModel
			CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
			TrackSceneNode tt = (TrackSceneNode) tmodel.elementAt(idx);
			int ssize = tt.getNumCores();
			CoreSection cs;

			this.sectionsList.removeAllItems();
			for (int i = 0; i < ssize; i++) {
				cs = tt.getCoreSection(i);
				this.sectionsList.addItem(cs.getName());
			}

			this.selectedTrackListId = idx; // java index
			this.selectedTrackId = tt.getId(); // native index
			if (ssize > 0) {
				this.sectionsList.setSelectedIndex(0);
			}

			System.out.println("trackList selected: " + this.selectedTrackListId);
		} else if (e.getSource().equals(this.sectionsList)) {
			JComboBox cb = (JComboBox) e.getSource();
			int idx = cb.getSelectedIndex();
			System.out.println("sectionList action selected id: " + idx);

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

			this.sectionText.setText((String) this.sectionsList.getSelectedItem());
			System.out.println("sectionList selected: " + this.selectedSectionListId);

		} else if (e.getSource().equals(this.selectfilebtn)) {

			// open file browser, have user specify output filename (.html)
			ExampleFileFilter cmlFilter = new ExampleFileFilter("html", "html File");

			// data path from preference
			this.chooser.setCurrentDirectory(new File(CRPreferences.getCurrentDir()));
			// default filename as section name
			String sectionname = (String) this.sectionsList.getSelectedItem();
			this.chooser.setSelectedFile(new File(sectionname));

			this.chooser.setFileFilter(cmlFilter);
			this.chooser.setDialogTitle("Save a Annotation Output file");
			int returnVal = chooser.showSaveDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File selectedFile = chooser.getSelectedFile().getAbsoluteFile();

				CRPreferences.setCurrentDir(selectedFile.getParent());

				// make sure it has .html at the end
				String path = selectedFile.getAbsolutePath();
				path = path.replace('\\', '/');
				String[] toks = path.split("/");
				if (!toks[toks.length - 1].contains(".html")) {
					path = path + ".html";
					selectedFile = new File(path);
				}

				this.filenameText.setText(selectedFile.getAbsolutePath());
				this.generatebtn.setEnabled(true);
			}

		} else if (e.getSource().equals(this.dateformatList)) {
			JComboBox cb = (JComboBox) e.getSource();
			int idx = cb.getSelectedIndex();
			System.out.println("dateformatList action selected id: " + idx);

			if (idx < 0) {
				return;
			}

			if (idx == 0) {
				this.dateformat = "MM/dd/yy";
			} else {
				this.dateformat = (String) cb.getSelectedItem();
			}

			SimpleDateFormat format = new SimpleDateFormat(this.dateformat + " '-' " + this.timeformat);
			this.dateformatText.setText(format.format(this.sampleDate));

		} else if (e.getSource().equals(this.hourSelections)) {
			if (this.hourSelections.isSelected()) {
				// 12 hour format
				this.timeformat = "hh:mm:ss a z";

				SimpleDateFormat format = new SimpleDateFormat(this.dateformat + " '-' " + this.timeformat);
				this.dateformatText.setText(format.format(this.sampleDate));

			} else {
				// 24 hour format
				this.timeformat = "hh:mm:ss z";

				SimpleDateFormat format = new SimpleDateFormat(this.dateformat + " '-' " + this.timeformat);
				this.dateformatText.setText(format.format(this.sampleDate));

			}
		} else {
			System.out.println("Action: " + e.getSource());
		}
	}

	// ---------------------------------------------------------------

	public void focusGained(final FocusEvent e) {
	}

	// ---------------------------------------------------------------

	public void focusLost(final FocusEvent e) {
	}

	// ---------------------------------------------------------------
	private int getTableIndex(final int sectionId) {
		// get section name from sectionId

		// index bound test
		// sectionId based on native vector size
		int id = SceneGraph.getImageIdForSection(selectedTrackId, sectionId);
		String sectionName = SceneGraph.getImageName(id);
		if (sectionName == null) {
			return -1;
		}

		// get dataset tables
		WellLogDataSet ds = this.datasets.elementAt(this.currentDatasetIndex);

		for (int i = 0; i < ds.getNumTables(); i++) {
			WellLogTable t = ds.getTable(i);

			String tableName = t.getName().toLowerCase();

			// only pick up the table/section name matched with selected
			// image section filename, and ignore case...
			if (sectionName.toLowerCase().contains(tableName)) {
				return i;
			}
		}

		return -1;
	}

	// ---------------------------------------------------------------
	private void PositionWidget(final Component c, final SpringLayout l, final int x, final int y) {
		l.putConstraint(SpringLayout.WEST, c, x, SpringLayout.WEST, this.getContentPane());
		l.putConstraint(SpringLayout.NORTH, c, y, SpringLayout.NORTH, this.getContentPane());
	}

	// ---------------------------------------------------------------
	void setDatasetVec(final Vector<WellLogDataSet> dsvec) {
		if (dsvec.size() <= 0) {
			return;
		}

		this.datasets = dsvec;

		// Update Datalists ComboBox UI
		this.datasetModel.removeAllElements();
		for (int i = 0; i < datasets.size(); i++) {
			WellLogDataSet ds = datasets.elementAt(i);
			String name = ds.getSourceFilename();
			name = name.replace('\\', '/');
			String tokens[] = name.split("/");
			this.datasetModel.addElement(tokens[tokens.length - 1]);
		}

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

	// ---------------------------------------------------------------
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

	// ---------------------------------------------------------------
	void setupUI() {

		chooser = new JFileChooser();

		SpringLayout layout = new SpringLayout();
		this.getContentPane().setLayout(layout);

		JLabel title = new JLabel("Choose a Track:");
		title.setPreferredSize(new Dimension(330, 25));
		this.getContentPane().add(title);
		PositionWidget(title, layout, 10, 10);

		tracksList = new JComboBox();
		tracksList.setEditable(false);
		tracksList.addActionListener(this);
		tracksList.setPreferredSize(new Dimension(320, 25));
		this.getContentPane().add(tracksList);
		PositionWidget(tracksList, layout, 10, 30);

		title = new JLabel("Choose a Section:");
		title.setPreferredSize(new Dimension(330, 25));
		this.getContentPane().add(title);
		PositionWidget(title, layout, 10, 60);

		sectionsList = new JComboBox();
		sectionsList.setEditable(false);
		sectionsList.addActionListener(this);
		sectionsList.setPreferredSize(new Dimension(320, 25));
		this.getContentPane().add(sectionsList);
		PositionWidget(sectionsList, layout, 10, 80);

		datasetModel = new DefaultComboBoxModel();
		datasetList = new JComboBox(datasetModel);

		allSections = new JCheckBox("Apply to all sections in the track?");
		allSections.setPreferredSize(new Dimension(330, 25));
		allSections.addActionListener(this);
		// this.getContentPane().add(allSections);
		// PositionWidget(allSections, layout, 10, 120);

		title = new JLabel("Output Information:");
		title.setPreferredSize(new Dimension(330, 25));
		this.getContentPane().add(title);
		PositionWidget(title, layout, 10, 150);

		projectText = new JTextField("project");
		projectText.addFocusListener(this);
		siteText = new JTextField("site");
		siteText.addFocusListener(this);
		holeText = new JTextField("hole");
		holeText.addFocusListener(this);
		sectionText = new JTextField("section");
		sectionText.addFocusListener(this);

		JPanel p = new JPanel(new GridLayout(2, 2));
		p.add(projectText);
		p.add(siteText);
		p.add(holeText);
		p.add(sectionText);
		p.setPreferredSize(new Dimension(330, 55));

		layout.putConstraint(SpringLayout.EAST, this.getContentPane(), 10, SpringLayout.EAST, p);
		this.getContentPane().add(p);
		PositionWidget(p, layout, 10, 170);

		// date format related
		title = new JLabel("Date Format:");
		title.setPreferredSize(new Dimension(150, 25));
		this.getContentPane().add(title);
		PositionWidget(title, layout, 10, 230);

		hourSelections = new JCheckBox("12 Hours");
		hourSelections.setPreferredSize(new Dimension(100, 25));
		hourSelections.addActionListener(this);
		this.getContentPane().add(hourSelections);
		PositionWidget(hourSelections, layout, 130, 230);

		dateformatList = new JComboBox();
		dateformatList.setEditable(false);
		dateformatList.setPreferredSize(new Dimension(110, 25));
		this.getContentPane().add(dateformatList);
		PositionWidget(dateformatList, layout, 10, 260);
		dateformatList.addItem("Use Original");
		dateformatList.addItem("MM/dd/yyy");
		dateformatList.addItem("yyyy.MM.dd");
		dateformatList.addItem("MMM d, yyyy");
		dateformatList.addActionListener(this);

		dateformatText = new JTextField("sample date format");
		dateformatText.setEditable(false);
		dateformatText.setPreferredSize(new Dimension(200, 25));
		this.getContentPane().add(dateformatText);
		PositionWidget(dateformatText, layout, 130, 260);

		// end of date format

		title = new JLabel("Output File Name:");
		title.setPreferredSize(new Dimension(330, 25));
		this.getContentPane().add(title);
		PositionWidget(title, layout, 10, 290);

		filenameText = new JTextField("output filename");
		filenameText.setEditable(false);
		filenameText.setPreferredSize(new Dimension(260, 25));
		this.getContentPane().add(filenameText);
		PositionWidget(filenameText, layout, 10, 310);

		selectfilebtn = new JButton("File");
		selectfilebtn.addActionListener(this);
		this.getContentPane().add(selectfilebtn);
		PositionWidget(selectfilebtn, layout, 275, 310);

		generatebtn = new JButton("Generate");
		generatebtn.setEnabled(false);
		cancelbtn = new JButton("Cancel");
		generatebtn.addActionListener(this);
		cancelbtn.addActionListener(this);
		this.getContentPane().add(generatebtn);
		PositionWidget(generatebtn, layout, 165, 340);
		this.getContentPane().add(cancelbtn);
		PositionWidget(cancelbtn, layout, 255, 340);

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	}

	// ---------------------------------------------------------------
	/**
	 * Given a change in the table (i.e. a checkbox is toggled on or off), line
	 * graphs are created or removed for a section or whole track.
	 */

	public void tableChanged(final TableModelEvent e) {
	}

	public void updateList() {
		int tsize, ssize;
		CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
		tsize = tmodel.getSize();
		TrackSceneNode tt;
		CoreSection cs;

		this.tracksList.removeAllItems();
		this.sectionsList.removeAllItems();
		for (int i = 0; i < tsize; i++) {
			tt = (TrackSceneNode) tmodel.elementAt(i);
			this.tracksList.addItem(tt.getName());
			if (i == 0) {
				ssize = tt.getNumCores();
				for (int j = 0; j < ssize; j++) {
					cs = tt.getCoreSection(j);
					this.sectionsList.addItem(cs.getName());
				}
			}
		}

		// set focus to the first track
		// then, it will rearrange sectionList too. (actionPerformed)
		this.tracksList.setSelectedIndex(0);
	}

	// ---------------------------------------------------------------

	public void valueChanged(final ListSelectionEvent e) {
	}

}
