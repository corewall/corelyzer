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
package corelyzer.ui.annotation.freeform;

import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import corelyzer.controller.CRExperimentController;
import corelyzer.data.ChatGroup;
import corelyzer.data.CoreSection;
import corelyzer.data.TrackSceneNode;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.annotation.AnnotationUtils;
import corelyzer.util.FileUtility;
import corelyzer.util.StringUtility;
import corelyzer.util.TableSorter;

public class FreeformAnnotationListDialog extends JDialog {
	private class ColumnEntry {
		boolean isShown;
		int origIndex;
		TableColumn column;

		public ColumnEntry(final int index, final TableColumn aColumn) {
			origIndex = index;
			column = aColumn;
			isShown = true;
		}

		public TableColumn getColumn() {
			return column;
		}

		public int getOrigIndex() {
			return origIndex;
		}

		public boolean isShown() {
			return isShown;
		}

		public void setShown(final boolean shown) {
			isShown = shown;
		}
	}

	private class FreeformTableMouseListener implements MouseListener {
		JDialog owner;

		public FreeformTableMouseListener(final JDialog d) {
			owner = d;
		}

		private JPopupMenu createPopupMenu(final int row) {
			// get scenegraph ids
			final int trackId = Integer.parseInt((String) model.getValueAt(row, 6));
			final int sectionId = Integer.parseInt((String) model.getValueAt(row, 7));
			final int markerId = Integer.parseInt((String) model.getValueAt(row, 8));

			JPopupMenu p = new JPopupMenu();

			JMenuItem open = new JMenuItem("Open");
			open.addActionListener(new ActionListener() {

				public void actionPerformed(final ActionEvent e) {
					AnnotationUtils.openAnnotation(owner, trackId, sectionId, markerId);

					onRefresh();
				}
			});

			JMenuItem delete = new JMenuItem("Delete");
			delete.addActionListener(new ActionListener() {

				public void actionPerformed(final ActionEvent e) {
					deleteAnnotationsAction();
				}
			});

			p.add(open);
			p.add(delete);

			// show in finder if it's local
			String localFilePath = SceneGraph.getCoreSectionMarkerLocal(trackId, sectionId, markerId);
			final File localFile = new File(localFilePath);

			JMenuItem showInFinder = new JMenuItem("Show in Finder");
			showInFinder.addActionListener(new ActionListener() {

				public void actionPerformed(final ActionEvent event) {
					FileUtility.showFileInFinder(localFile);
				}
			});
			showInFinder.setEnabled(localFile.exists());
			p.add(showInFinder);

			return p;
		}

		public void mouseClicked(final MouseEvent event) {
			Point p = event.getPoint();

			TableSorter sorter = (TableSorter) table.getModel();
			int row = sorter.modelIndex(table.rowAtPoint(p));

			if (event.getClickCount() == 2) { // goto & open
				try {
					int trackId = Integer.parseInt((String) model.getValueAt(row, 6));
					int sectionId = Integer.parseInt((String) model.getValueAt(row, 7));
					int markerId = Integer.parseInt((String) model.getValueAt(row, 8));

					CRExperimentController.locateAnnotation(trackId, sectionId, markerId);
					AnnotationUtils.openAnnotation(owner, trackId, sectionId, markerId);
				} catch (NumberFormatException e) {
					return;
				}
			}

			if (event.isPopupTrigger()) { // right click
				createPopupMenu(row).show(table, p.x, p.y);
			}
		}

		public void mouseEntered(final MouseEvent event) {
		}

		public void mouseExited(final MouseEvent event) {
		}

		public void mousePressed(final MouseEvent event) {
		}

		public void mouseReleased(final MouseEvent event) {
		}
	}

	private class TableHeaderMouseListener implements MouseListener {
		public TableHeaderMouseListener() {
			super();
		}

		public void mouseClicked(final MouseEvent event) {
			if (event.isPopupTrigger()) {
				JPopupMenu menu = new JPopupMenu("Attribs");
				Point p = event.getPoint();

				final JCheckBoxMenuItem colItem = new JCheckBoxMenuItem("Resize Column Width");
				final boolean isAutoResize = table.getAutoResizeMode() == JTable.AUTO_RESIZE_ALL_COLUMNS;

				colItem.setSelected(isAutoResize);
				colItem.addActionListener(new ActionListener() {

					public void actionPerformed(final ActionEvent e) {
						int mode;
						if (isAutoResize) {
							mode = JTable.AUTO_RESIZE_OFF;
						} else {
							mode = JTable.AUTO_RESIZE_ALL_COLUMNS;
						}

						table.setAutoResizeMode(mode);
						colItem.setSelected(!isAutoResize);
					}
				});
				menu.add(colItem);

				menu.addSeparator();

				for (Map.Entry<String, ColumnEntry> entry : defaultColumns.entrySet()) {
					String label = entry.getKey();
					final ColumnEntry column = entry.getValue();
					final JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, column.isShown());

					if (label.equalsIgnoreCase("title")) {
						item.setEnabled(false);
					}

					item.addActionListener(new ActionListener() {

						public void actionPerformed(final ActionEvent e) {
							column.setShown(item.isSelected());

							if (item.isSelected()) {
								table.addColumn(column.getColumn());
								if (column.getOrigIndex() < table.getColumnCount()) {
									table.moveColumn(table.getColumnCount() - 1, column.getOrigIndex());
								}
							} else {
								table.removeColumn(column.getColumn());
							}
						}
					});
					menu.add(item);
				}

				menu.show(table.getTableHeader(), p.x, p.y);
			}
		}

		public void mouseEntered(final MouseEvent event) {
		}

		public void mouseExited(final MouseEvent event) {
		}

		// Not used mouse events

		public void mousePressed(final MouseEvent event) {
		}

		public void mouseReleased(final MouseEvent event) {
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3989039001984831875L;

	public static void main(final String[] args) {
		FreeformAnnotationListDialog dialog = new FreeformAnnotationListDialog( null );
		dialog.pack();
		dialog.setVisible(true);
	}

	private JPanel contentPane;
	private JButton closeButton;

	private JButton refreshButton;
	private JTable table;

	private JButton loadButton;

	private JButton saveButton;

	private FreeformAnnotationTableModel model;

	Hashtable<String, ColumnEntry> defaultColumns;

	{
		// GUI initializer generated by IntelliJ IDEA GUI Designer
		// >>> IMPORTANT!! <<<
		// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	public FreeformAnnotationListDialog( final JFrame owner ) {
		super( owner );
		setTitle("Freeform Annotations");
		setContentPane(contentPane);
		getRootPane().setDefaultButton(closeButton);

		closeButton.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onClose();
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				onCancel();
			}
		});

		contentPane.registerKeyboardAction(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onCancel();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		refreshButton.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				onRefresh();
			}
		});

		decorateTable();

		loadButton.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				onLoad();
			}
		});

		saveButton.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				onSave();
			}
		});
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return contentPane;
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT
	 * edit this method OR call it in your code!
	 * 
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		closeButton = new JButton();
		closeButton.setText("Close");
		panel2.add(closeButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		refreshButton = new JButton();
		refreshButton.setText("Refresh");
		panel2.add(refreshButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		loadButton = new JButton();
		loadButton.setText("Load...");
		panel1.add(loadButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		saveButton = new JButton();
		saveButton.setText("Save...");
		panel1.add(saveButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		contentPane
				.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
						0, false));
		panel3.setBorder(BorderFactory.createTitledBorder("Freeform Annotations"));
		final JScrollPane scrollPane1 = new JScrollPane();
		panel3.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
				false));
		table = new JTable();
		scrollPane1.setViewportView(table);
	}

	private void decorateTable() {
		this.table.setShowVerticalLines(true);
		this.table.setDragEnabled(true);
		this.table.getTableHeader().setReorderingAllowed(true);
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		FreeformTableMouseListener listener = new FreeformTableMouseListener(this);

		this.table.addMouseListener(listener);
		this.table.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(final KeyEvent keyEvent) {
				if (keyEvent.getKeyChar() == KeyEvent.VK_DELETE) {
					deleteAnnotationsAction();
				}
			}
		});

		model = new FreeformAnnotationTableModel();
		TableSorter sorter = new TableSorter();
		sorter.setTableModel(model);
		sorter.setTableHeader(table.getTableHeader());

		this.table.setModel(sorter);

		// pre-process header labels
		for (int i = 0; i < model.indexKeyMapping.length; i++) {
			String header = model.indexKeyMapping[i];
			header = StringUtility.capitalizeHeadingCharacter(header);

			if (i == 3) { // depth
				header += " (m)";
			}

			table.getColumnModel().getColumn(i).setHeaderValue(header);
		}

		// Build a copy of default columns
		defaultColumns = new Hashtable<String, ColumnEntry>();
		TableColumnModel tModel = table.getColumnModel();

		for (int i = 0; i < sorter.getColumnCount(); i++) {
			String label = (String) tModel.getColumn(i).getHeaderValue();
			ColumnEntry entry = new ColumnEntry(i, tModel.getColumn(i));
			defaultColumns.put(label, entry);
		}

		// Not show certain columns
		ColumnEntry entry;
		String[] notShownColumns = { "TrackId", "SectionId", "MarkerId", "Url" };

		for (String aColumn : notShownColumns) {
			entry = defaultColumns.get(aColumn);

			if (entry != null) {
				table.removeColumn(entry.getColumn());
				entry.setShown(false);
			}
		}

		// TableHeader PopupMenu
		table.getTableHeader().addMouseListener(new TableHeaderMouseListener());
	}

	private void deleteAnnotationsAction() {
		int numberOfSelectedRows = table.getSelectedRowCount();

		if (numberOfSelectedRows <= 0) {
			return;
		} else if (numberOfSelectedRows == 1) {
			int row = table.getSelectedRow();

			int trackId = Integer.parseInt((String) model.getValueAt(row, 6));
			int sectionId = Integer.parseInt((String) model.getValueAt(row, 7));
			int markerId = Integer.parseInt((String) model.getValueAt(row, 8));

			AnnotationUtils.removeAnnotation(this, trackId, sectionId, markerId);
		} else {
			// bulk annot deletion
			int[] trackIds = new int[numberOfSelectedRows];
			int[] sectionIds = new int[numberOfSelectedRows];
			int[] markerIds = new int[numberOfSelectedRows];

			int[] rows = table.getSelectedRows();
			for (int i = 0; i < rows.length; i++) {
				int row = rows[i];

				int trackId = Integer.parseInt((String) model.getValueAt(row, 6));
				int sectionId = Integer.parseInt((String) model.getValueAt(row, 7));
				int markerId = Integer.parseInt((String) model.getValueAt(row, 8));

				trackIds[i] = trackId;
				sectionIds[i] = sectionId;
				markerIds[i] = markerId;
			}

			AnnotationUtils.removeAnnotations(this, trackIds, sectionIds, markerIds);
		}

		onRefresh();
	}

	private void onCancel() {
		dispose();
	}

	private void onClose() {
		dispose();
	}

	private void onLoad() {
		String filePath = FileUtility.selectASingleFile(this, "Load an annotation list file", "csv", FileUtility.LOAD);

		if (filePath != null && !filePath.equals("")) {
			File f = new File(filePath);

			if (f.exists()) {
				boolean res = AnnotationUtils.loadCSVAnnotationList(this, f);

				if (!res) { // failed
					JOptionPane.showMessageDialog(this, "Load failed");
				}

				this.onRefresh();
				dispose();
			} else {
				JOptionPane.showMessageDialog(this, "Selected file doesn't exist.");
			}
		}

	}

	public void onRefresh() {
		if (model != null) {
			model.clear();
		}

		CorelyzerApp app = CorelyzerApp.getApp();
		if (app == null) {
			return;
		}

		int numberOfTracks = app.getTrackListModel().size();

		// Traverse current scene and collect freeform annotation info
		// Track
		for (int i = 0; i < numberOfTracks; i++) {
			TrackSceneNode t = (TrackSceneNode) app.getTrackListModel().elementAt(i);

			int tId = t.getId();
			String trackName = t.getName();
			float trackOffsetX = SceneGraph.getTrackXPos(tId);
			// float trackOffsetY = SceneGraph.getTrackYPos(tId);

			// Section
			for (int j = 0; j < t.getNumCores(); j++) {
				CoreSection cs = t.getCoreSection(j);

				int csId = cs.getId();
				String csName = cs.getName();
				float sectionOffsetX = SceneGraph.getSectionXPos(tId, csId);
				// float sectionOffsetY = SceneGraph.getSectionYPos(tId, csId);

				// Annotations
				int numberOfMarkers = SceneGraph.getNumCoreSectionMarkers(tId, csId);

				for (int k = 0; k < numberOfMarkers; k++) {
					String url = SceneGraph.getCoreSectionMarkerURL(tId, csId, k);

					if (url == null) {
						continue;
					}

					int group = SceneGraph.getCoreSectionMarkerGroup(tId, csId, k);
					boolean visible = SceneGraph.getCoreSectionMarkerVisibility(tId, csId, k);

					// int type = SceneGraph.getCoreSectionMarkerType(tId, csId,
					// k);
					float pX = SceneGraph.getCoreSectionMarkerXPos(tId, csId, k);
					// float pY = SceneGraph.getCoreSectionMarkerYPos(tId, csId,
					// k);

					if (group == ChatGroup.SAMPLE || group == ChatGroup.CLAST) {
						continue;
					}

					float dpix = SceneGraph.getCanvasDPIX(0);
					float depth = (trackOffsetX + sectionOffsetX + pX) * 2.54f / (100.0f * dpix); // meter

					Hashtable<String, String> attribs = new Hashtable<String, String>();
					attribs.put("show", String.valueOf(visible));
					attribs.put("track", trackName);
					attribs.put("section", csName);
					attribs.put("depth", String.valueOf(depth));
					attribs.put("group", ChatGroup.getGroupName(group));
					attribs.put("url", url);

					// hidden attributes
					attribs.put("trackId", String.valueOf(tId));
					attribs.put("sectionId", String.valueOf(csId));
					attribs.put("markerId", String.valueOf(k));

					model.addAnnotation(attribs);
				} // end of markers
			} // end of section
		} // end of track

		table.clearSelection();
		table.updateUI();
	}

	private void onSave() {
		String filePath = FileUtility.selectASingleFile(this, "Save annotations to a list file", null, FileUtility.SAVE);

		if (filePath != null && !filePath.equals("")) {
			File f = new File(filePath);

			boolean isSuccess = AnnotationUtils.saveCSVAnnotationList(f, model);
			if (!isSuccess) {
				JOptionPane.showMessageDialog(this, "Save failed");
			}
		}

		dispose();
	}
}
