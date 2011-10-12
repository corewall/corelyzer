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

package corelyzer.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.miginfocom.swing.MigLayout;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import corelyzer.data.CheckBoxTable;
import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionGraph;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.UnitLength;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.WellLogTable;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.graphics.SceneGraph;

// TODO: update states more accurtely.
public class CRGraphDialog extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8879622939957228395L;

	public static void main(final String[] args) {
		CRGraphDialog dialog = new CRGraphDialog(null);
		dialog.pack();
		dialog.setSize(480, 480);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private JPanel contentPane;
	private JButton applybtn;
	private JButton closebtn;
	private JComboBox datasetList;
	//private JCheckBox allSectionsBox;
	private JList sectionsList;
	private JLabel sectionsListLabel;
	private JCheckBox ifCollapseGraphs;
	private JButton colorbtn;
	private JComboBox typeList;
	private JTextField scaleMinText;
	private JTextField scaleMaxText;
	private JTextField excludeMinText, excludeMaxText;
	private JCheckBox leaveGapsBox;

	private CheckBoxTable fieldsTable;
	DefaultComboBoxModel datasetModel;

	// 9/30/2011 brg: Why are we maintaining a member variable for something that is never
	// changed and can easily be acessed w/ this.sectionsList.getModel()???
	DefaultListModel sectionsListModel;

	// View (for scaling)
	Vector<Float> scaleMinVals, scaleMaxVals;

	// Data (original field data)
	Vector<Float> origMinVals, origMaxVals;

	// Min/max values in all sections/tables
//	Vector<Float> allMinVals, allMaxVals;

	// Min/max exclude range
	Vector<Float> excludeMinVals, excludeMaxVals;
	Vector<Integer> excludeStyleVals;
	
	Vector<Boolean> showGraphVals;
	Vector<Float> dataRangeMinVals, dataRangeMaxVals;
	
	Vector<Color> colors;

	// 8/24/2011 brgtodo: typing would be clearer with an enum
	Vector<Integer> graphTypes; // line, point, crosspoint, point&lines

	Vector<WellLogDataSet> datasets;

	ColorChooser colorchooser;

	// int currentDatasetIndex;
	int currentTableIndex;
	int selectedTrackId; // native code index (scenegraph)
	int selectedSectionId; // native code index (sceengraph)
	int selectedTrackListId; // java side index (CorelyzerApp)
	int selectedSectionListId; // java side index (CorelyzerApp)
	int selectedFieldId;

	int lastSelectedField; // 9/2/2011 brg: being updated but otherwise unused
	
	private class GraphParams {
		public int trackId;
		public int sectionId;
		public int datasetId;
		public int dataTableId;
		
		public GraphParams( final int trackId, final int sectionId, final int datasetId, final int dataTableId )
		{
			this.trackId = trackId;
			this.sectionId = sectionId;
			this.datasetId = datasetId;
			this.dataTableId = dataTableId;
		}
	}

	/**
	 * Querying SceneGraph about graphs associated with a given section is a common task, encapsulate
	 * logic for retrieving the necessary parameters here.
	 * @param sectionIndex
	 *		Index of section in sectionsList
	 */
	private GraphParams getGraphParams( final int sectionIndex )
	{
		GraphParams gpResult = null;
		
		CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
		final String sectionName = this.sectionsList.getModel().getElementAt( sectionIndex ).toString();

		for ( int trackIdx = 0; trackIdx < tmodel.size(); trackIdx++ )
		{
			TrackSceneNode tnode = (TrackSceneNode) tmodel.elementAt( trackIdx );
			final CoreSection cs = tnode.getCoreSection( sectionName );
			if ( cs != null )
			{
				final int dataTableId = this.getTableIndexByName( cs.getName() );
				if ( dataTableId == -1 ) return null;
				
				final WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();
				//final int gid = SceneGraph.getGraphID( tnode.getId(), cs.getId(), ds.getId(), dataTableId, row );
				gpResult = this.new GraphParams( tnode.getId(), cs.getId(), ds.getId(), dataTableId );
				break;
			}
		}
		
		return gpResult;
	}
	

	{
		// GUI initializer generated by IntelliJ IDEA GUI Designer
		// >>> IMPORTANT!! <<<
		// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	public CRGraphDialog(final Component parent) {
		super();
		this.setAlwaysOnTop(true);
		this.setLocationRelativeTo(parent);
		setTitle("Graph Dialog");

		myInit();

		setContentPane(contentPane);
		getRootPane().setDefaultButton(applybtn);

		applybtn.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onApply();
			}
		});

		closebtn.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onCancel();
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

		typeList.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				//System.out.println("Type List action: " + event.getActionCommand());
				onTypeListAction();
			}
		});
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return contentPane;
	}

	// brg 6/21/2011: Editing this by hand despite the warnings, can't get IntelliJ
	// set up properly and I'd rather not depend on it anyway!
	// brg 8/24/2011: Doing this by hand is truly painful. Consider MigLayout?
	/**
	 * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT
	 * edit this method OR call it in your code!
	 * 
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		MigLayout foo = new MigLayout("wrap 3");
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		closebtn = new JButton();
		closebtn.setText("Close");
		panel2.add(closebtn, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		applybtn = new JButton();
		applybtn.setText("Apply");
		panel2.add(applybtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		ifCollapseGraphs = new JCheckBox();
		ifCollapseGraphs.setText("Collapse Graphs?");
		panel1.add(ifCollapseGraphs, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
		contentPane
				.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
						0, false));
		final JPanel panel4 = new JPanel();
		panel4.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JLabel label1 = new JLabel();
		label1.setText("Choose a dataset:");
		panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		datasetList = new JComboBox();
		final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
		defaultComboBoxModel1.addElement("RGR_TEST.xml");
		datasetList.setModel(defaultComboBoxModel1);
		panel4.add(datasetList, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
//		allSectionsBox = new JCheckBox();
//		allSectionsBox.setSelected(true);
//		allSectionsBox.setText("Apply to all sections in this dataset?");
//		panel4.add(allSectionsBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK
//				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		sectionsListLabel = new JLabel( "Choose graph section: " );
		panel4.add(sectionsListLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label3 = new JLabel();
		label3.setText("Properties fields: ");
		panel4.add(label3, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JScrollPane scrollPane1 = new JScrollPane();
		panel4.add(scrollPane1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
				false));
		fieldsTable = new CheckBoxTable();
		scrollPane1.setViewportView(fieldsTable);
		final JScrollPane scrollPane2 = new JScrollPane();
		panel4.add(scrollPane2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
				false));
		sectionsList = new JList();
		final DefaultListModel defaultListModel1 = new DefaultListModel();
		sectionsList.setModel(defaultListModel1);
		scrollPane2.setViewportView(sectionsList);
		final JPanel panel5 = new JPanel();
		panel5.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		//valueMin = new JLabel();
		//valueMin.setText("Value min: -100.0");
		//panel5.add(valueMin, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
		//		GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(153, 16), null, 0, false));

		// Scale min/max
		final JPanel scalePanel = new JPanel();
		scalePanel.setLayout(new GridLayoutManager(1, 4, new Insets(0,0,0,0), -1, -1));
		final JLabel scaleMinLabel = new JLabel("Scale min:");
		final JLabel scaleMaxLabel = new JLabel("max:");
		scaleMinText = new JTextField();
		scaleMaxText = new JTextField();
		scalePanel.add(scaleMinLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
		    GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		scalePanel.add(scaleMinText, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
			GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		scalePanel.add(scaleMaxLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
			GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		scalePanel.add(scaleMaxText, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
			GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		
		// Excluded values
		final JPanel excludedValuesPanel = new JPanel();
		excludedValuesPanel.setLayout(new GridLayoutManager(2, 4, new Insets(0,0,0,0), -1, -1));
		final JLabel excludeMinLabel = new JLabel("Exclude values <");
		excludedValuesPanel.add(excludeMinLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
		    GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		excludeMinText = new JTextField();
		excludedValuesPanel.add(excludeMinText, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
		    GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel excludeMaxLabel = new JLabel("and/or >");
		excludedValuesPanel.add(excludeMaxLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
			GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		excludeMaxText = new JTextField();
		excludedValuesPanel.add(excludeMaxText, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
			GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		leaveGapsBox = new JCheckBox("Leave gaps at excluded values", false);
		excludedValuesPanel.add(leaveGapsBox, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
			GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		
		
//		final JPanel panel6 = new JPanel();
//		panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		panel5.add(scalePanel/*panel6*/, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

		//		final JLabel label4 = new JLabel();
//		label4.setText("Scale min: ");
//		panel6.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
//				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
//		scaleMinText = new JTextField();
//		scaleMinText.setHorizontalAlignment(11);
//		panel6.add(scaleMinText, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
//				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));

		// Color/line style
		JPanel colorLinePanel = new JPanel();
		colorLinePanel.setLayout(new GridLayoutManager(1, 2, new Insets(0,0,0,0), -1, -1));
		colorbtn = new JButton();
		colorbtn.setText("Color");
		colorLinePanel.add(colorbtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(153, 29),
				null, 0, false));
		typeList = new JComboBox();
		final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
		defaultComboBoxModel2.addElement("Line");
		defaultComboBoxModel2.addElement("Point");
		defaultComboBoxModel2.addElement("Cross point");
		defaultComboBoxModel2.addElement("Line & Points");
		typeList.setModel(defaultComboBoxModel2);
		colorLinePanel.add(typeList, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

		panel5.add(colorLinePanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
		   GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));									   

		panel5.add(excludedValuesPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
			| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

		
		//valueMax = new JLabel();
		//valueMax.setText("Value max: +100.0");
		//panel5.add(valueMax, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
		//		GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(153, 16), null, 0, false));
//		final JPanel panel7 = new JPanel();
//		panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
//		panel5.add(panel7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
//				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
//		final JLabel label5 = new JLabel();
//		label5.setText("Scale max: ");
//		panel7.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
//				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
//		brgText = new JTextField("bonertown");
//		panel7.add(brgText, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
//				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
//		scaleMaxText = new JTextField();
//		scaleMaxText.setHorizontalAlignment(11);
//		panel7.add(scaleMaxText, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
//				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
	}

	private void applyGraphSelection( final int sectionIndex ) {
		// brg 10/5/2011 this will never happen...
/*
		if (section < 0) {
			return;
		}

		WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();

		// loop through field list, check each checkbox's status
		for (int curRowIndex = 0; curRowIndex < fieldsTable.getRowCount(); curRowIndex++) {
			boolean isChecked = fieldsTable.isRowChecked( curRowIndex );

			if (isChecked) {
				// get the graph id and set the min/max and color values
				// apply the changes to the graph. If there isn't a graph
				// then create it, if possible

				int gid = SceneGraph.getGraphID(selectedTrackId, section, ds.getId(), // currentDatasetIndex,
						currentTableIndex, curRowIndex );

				if (gid < 0) {
					gid = SceneGraph.addLineGraphToSection(selectedTrackId, section, ds.getId(), // getDatasetId(currentDatasetIndex),
							currentTableIndex, curRowIndex );
				}

				if (gid == -1) {
					System.out.println("- GraphID of -1 after calling SceneGraph.addLineGraphToSection.");
					continue;
				} else {
					System.out.println("- GraphID = " + gid + " (success)");
				}
				
				this.setGraphProperties( gid, curRowIndex );
			} else {
				// make sure it's removed if there exists a graph
				int gid = SceneGraph.getGraphID(selectedTrackId, section, ds.getId(), currentTableIndex, curRowIndex );

				if (gid >= 0) {
					SceneGraph.removeLineGraphFromSection(gid);
				}
			}
		}
*/
		
		final GraphParams gp = this.getGraphParams( sectionIndex );
		if ( gp != null )
		{
			for ( int curRowIndex = 0; curRowIndex < fieldsTable.getRowCount(); curRowIndex++ )
			{
				final boolean showGraph = fieldsTable.isRowChecked( curRowIndex );
				if ( showGraph ) {
					// Get the graph id and set properties (min/maxes, color values, type, etc.)
					// If there isn't a graph try to create one.
					
					int gid = SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, curRowIndex );
					
					if (gid < 0) {
						gid = SceneGraph.addLineGraphToSection( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, curRowIndex );
					}
					
					if (gid == -1) {
						System.out.println("- GraphID of -1 after calling SceneGraph.addLineGraphToSection.");
						continue;
					} else {
						System.out.println("- GraphID = " + gid + " (success)");
					}
					
					this.setGraphProperties( gid, curRowIndex );
				} else {
					// make sure it's removed if there exists a graph
					int gid = SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, curRowIndex );
					
					if (gid >= 0) {
						SceneGraph.removeLineGraphFromSection( gid );
					}
				}				
			}
		}
	}

	// Returns an integer array containing indices of selected items in sectionsList.
	private int[] getSelectedSections()
	{
/*
		int[] selectedSections = null;
		if ( allSectionsBox.isSelected() )
		{
			// if "all sections" box is checked, fake out the array since
			// the actual JList elements won't be selected
			final int numSections = sectionsList.getModel().getSize();
			selectedSections = new int[numSections];
			for ( int i = 0; i < numSections; i++ ) { selectedSections[i] = i; }
		}
		else // use the JList method
		{
			selectedSections = sectionsList.getSelectedIndices();
		}
		
		return selectedSections;
*/
		return sectionsList.getSelectedIndices();
	}
	
	// find data table ID for the specified section
	private int getTableIndexByName( final String sectionName ) {
		WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();
		
		if ( ds == null ) {
			System.out.println("dataset couldn't be found");
			return -1;
		}
		
		for (int i = 0; i < ds.getNumTables(); i++) {
			WellLogTable t = ds.getTable(i);
			
			String tableName = t.getName().toLowerCase();
			
			// only pick up the table/section name matched with selected
			// image section filename, and ignore case...
			if ( sectionName.compareToIgnoreCase( tableName ) == 0 ) {
				return i;
			}
		}
		
		System.out.println("Couldn't find section [" + sectionName + "] in dataset [" + ds.getSourceFilename() + "]");
		return -1;
	}
	
	// find data table ID for the specified section
	private int getTableIndex(final int sectionId) {
		String secname = this.sectionsList.getModel().getElementAt(sectionId).toString();
		
		return getTableIndexByName( secname );
	}

	private void ifCollapseAction() {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				SceneGraph.setGraphsCollapse(ifCollapseGraphs.isSelected());

				CorelyzerApp.getApp().updateGLWindows();
			}
		});
	}
	
	private void leaveGapsAction() {
		int field = fieldsTable.getSelectedRow();
		
		if (field < 0) {
			return;
		}
		
		final int egType = leaveGapsBox.isSelected() ? 1 : 0;
		excludeStyleVals.setElementAt( egType, field );
	}

	private void myInit() {
		// data
		// currentDatasetIndex = -1;
		currentTableIndex = -1;
		selectedFieldId = -1;
		selectedTrackId = -1;
		selectedSectionId = -1;

		// ui
		this.colorchooser = new ColorChooser(this);
		this.colorchooser.setVisible(false);
		this.colorchooser.addReturnActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				onColorChooserReturn();
			}
		});

		this.datasetModel = new DefaultComboBoxModel();
		this.datasetList.setModel(datasetModel);
		this.datasetList.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onDatasetAction();
			}
		});

//		this.allSectionsBox.addActionListener(new ActionListener() {
//
//			public void actionPerformed(final ActionEvent e) {
//				onAllSectionsAction();
//			}
//		});

		this.sectionsListModel = new DefaultListModel();
		this.sectionsList.setModel(this.sectionsListModel);

		// 9/2/2011 brgtodo: experiment with multiple selection
		this.sectionsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		this.sectionsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				onSectionsListChanged();
			}
		});
		
		// all sections box is checked on open (see setupUI()), disable section list/label
//		sectionsList.clearSelection();
//		sectionsList.setEnabled( false );
//		sectionsListLabel.setEnabled( false );

		fieldsTable.setShowHorizontalLines(true);
		fieldsTable.addCheckEventListener(new TableModelListener() {

			public void tableChanged(final TableModelEvent event) {
				onFieldsTableChanged(event);
			}
		});
		fieldsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(final ListSelectionEvent event) {
				onFieldsListChanged();
			}
		});

		colors = new Vector<Color>();
		graphTypes = new Vector<Integer>();
		scaleMinVals = new Vector<Float>();
		scaleMaxVals = new Vector<Float>();

		this.origMinVals = new Vector<Float>();
		this.origMaxVals = new Vector<Float>();
//		this.allMinVals = new Vector<Float>();
//		this.allMaxVals = new Vector<Float>();
		this.excludeMinVals = new Vector<Float>();
		this.excludeMaxVals = new Vector<Float>();
		this.excludeStyleVals = new Vector<Integer>();
		this.dataRangeMinVals = new Vector<Float>();
		this.dataRangeMaxVals = new Vector<Float>();
		this.showGraphVals = new Vector<Boolean>();

		colorbtn.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onColorButton();
			}
		});
		scaleMinText.addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent event) {
			}

			public void focusLost(final FocusEvent event) {
				onScaleMinTextLostFocus();
			}
		});
		scaleMaxText.addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent event) {
			}

			public void focusLost(final FocusEvent event) {
				onScaleMaxTextLostFocus();
			}
		});
		
		excludeMinText.addFocusListener(new FocusListener() {
			public void focusGained(final FocusEvent event) { }
			public void focusLost(final FocusEvent event) { onExcludeMinLostFocus(); }
		});
		
		excludeMaxText.addFocusListener(new FocusListener() {
			public void focusGained(final FocusEvent event) { }
			public void focusLost(final FocusEvent event) { onExcludeMaxLostFocus(); }
		});
		
		leaveGapsBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				leaveGapsAction(); // save checkbox state
			}
		});

		ifCollapseGraphs.setSelected(SceneGraph.getGraphsCollapse());
		ifCollapseGraphs.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				ifCollapseAction();
			}
		});
	}

/*
	private void onAllSectionsAction() {
		// 9/2/2011 brgtodo: experiment with multiple selection
		if ( allSectionsBox.isSelected() ) {
			sectionsList.clearSelection();
			sectionsList.setEnabled( false );
			sectionsListLabel.setEnabled( false );
			this.onFieldsListChanged();
		} else {
			sectionsList.setEnabled( true );
			sectionsListLabel.setEnabled( true );
		}
	}
*/
	private void onApply() {
		// APPLY BUTTON
/*
		if (allSectionsBox.isSelected()) {
			int numsecs = SceneGraph.getNumSections(selectedTrackId);
			int temp = currentTableIndex;

			for (int i = 0; i < numsecs; i++) {
				currentTableIndex = this.getTableIndex(i);
				applyGraphSelection(i);
			}

			currentTableIndex = temp;
		} else {
			applyGraphSelection(selectedSectionId);
		}
*/
		int[] selectedSections = this.getSelectedSections();
		for ( int curSectionIndex : selectedSections )
		{
			applyGraphSelection( curSectionIndex );
		}
		
		CorelyzerApp.getApp().updateGLWindows();
	}

	private void onCancel() {
		dispose();
	}

	private void onColorButton() {
		this.colorchooser.setVisible(true);
		this.colorchooser.setTarget(this.colorbtn);
		int field = fieldsTable.getSelectedRow();
		this.colorchooser.setColor(colors.elementAt(field));
	}

	private void onColorChooserReturn() {
		// COLOR SELECTED
		int field = fieldsTable.getSelectedRow();
		if (field < 0) {
			return;
		}

		colors.setElementAt(colorchooser.getColor(), field);

		// update current section info f i x m e
/*
		CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
		TrackSceneNode tnode = (TrackSceneNode) tmodel.elementAt(selectedTrackListId);
		String secname = this.sectionsListModel.getElementAt(this.selectedSectionListId).toString();

		CoreSection sec = tnode.getCoreSection(secname);
		if (sec == null) {
			this.selectedSectionId = -1;
		} else {
			this.selectedSectionId = sec.getId();
		}

		WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();
*/
		
		int[] selectedSections = this.getSelectedSections();
		for ( int curSectionIndex : selectedSections )
		{
			final GraphParams gp = this.getGraphParams( curSectionIndex );
			if ( gp == null )
				continue;
			
			final int gid = SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, field );
			if (gid < 0)
				continue;
			
			// if a graph is present, change its color
			final Color c = colors.elementAt(field);
			SceneGraph.setLineGraphColor(gid, (c.getRed() / 255.0f), (c.getGreen() / 255.0f), (c.getBlue() / 255.0f));
		}

		CorelyzerApp.getApp().updateGLWindows();
		//this.onApply();
	}

	private void onDatasetAction() {
		// DATASET COMBOBOX
		/*
		 * int idx = this.datasetList.getSelectedIndex();
		 * 
		 * if (idx < 0) return;
		 * 
		 * String dsName = (String) this.datasetList.getSelectedItem();
		 * 
		 * System.out.println("---- Selected Dataset " + dsName + " idx: " +
		 * idx);
		 * 
		 * // set dataset index and update fields
		 * CorelyzerApp.getApp().getDataFileList().setSelectedIndex(idx);
		 * this.currentDatasetIndex = idx; this.repaint(); lastSelectedField =
		 * 0;
		 * 
		 * // update section list for this dataset
		 * this.sectionsListModel.removeAllElements(); WellLogDataSet ds =
		 * this.datasets.elementAt(idx); for (int i = 0; i < ds.getNumTables();
		 * i++) { String name = ds.getTable(i).getName();
		 * this.sectionsListModel.addElement(name); }
		 * //this.selectedSectionListId = this.sectionsList.getSelectedIndex();
		 * this.selectedSectionListId = 0;
		 * this.sectionsList.setSelectedIndex(0);
		 * 
		 * fieldsTable.tableChanged(new TableModelEvent(
		 * fieldsTable.getModel()));
		 */
		WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();
		if (ds == null) {
			return;
		}

		// System.out.println("[Bremen] Selected dataset: " + ds.toString() +
		// ", id: " + ds.getId());

		CoreGraph cg = CoreGraph.getInstance();
		Session s = cg.getCurrentSession();
		if (s != null) {
			for (int i = 0; i < s.getDatasets().size(); i++) {
				WellLogDataSet d = s.getDatasets().elementAt(i);

				if (d.getId() == ds.getId()) {
					// System.out.println("[Bremen] Found dataset index in session: "
					// + s.getName() + ", index: " + i
					// + ", id: " + ds.getId());

					cg.setCurrentDatasetIdx(i);

					// this.currentDatasetIndex = i;
					this.repaint();

					break;
				}
			}
		}

		this.sectionsListModel.removeAllElements();
		for (int i = 0; i < ds.getNumTables(); i++) {
			String name = ds.getTable(i).getName();
			this.sectionsListModel.addElement(name);
		}
		// this.selectedSectionListId = this.sectionsList.getSelectedIndex();
		this.selectedSectionListId = 0;
		this.sectionsList.setSelectedIndex(0);

		fieldsTable.tableChanged(new TableModelEvent(fieldsTable.getModel()));
	}

	// ---------------------------------------------------------------

	private void onFieldsListChanged() {
		// if (currentDatasetIndex < 0) {
		WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();

		if (ds == null) {
			return;
		}

		int field = fieldsTable.getSelectedRow();
		if (field < 0) {
			return;
		}

		lastSelectedField = field;

		// (View) update min/max text with the values of selected field index
		scaleMinText.setText(scaleMinVals.elementAt(field).toString());
		scaleMaxText.setText(scaleMaxVals.elementAt(field).toString());
		
		try {
			float minVal = excludeMinVals.elementAt(field);
			String minStr;
			if ( minVal <= -Float.MAX_VALUE )
				minStr = new String();
			else
				minStr = excludeMinVals.elementAt(field).toString();

			float maxVal = excludeMaxVals.elementAt(field);
			String maxStr;
			if ( maxVal >= Float.MAX_VALUE )
				maxStr = new String();
			else
				maxStr = excludeMaxVals.elementAt(field).toString();
			
			excludeMinText.setText(minStr);// System.out.println("setting exclude min: " + minStr);
			excludeMaxText.setText(maxStr);
		} catch (Exception e) {
			System.out.println("Couldn't set exclude min/max field: " + e.getMessage() );
		}
		
		// (Data) Update field's min/max values
//		DecimalFormat df = new DecimalFormat("#,###,###,##0.000");
//		float allValueMin = this.allSectionsBox.isSelected() ? this.allMinVals.elementAt(field) : this.origMinVals.elementAt(field);
//		float allValueMax = this.allSectionsBox.isSelected() ? this.allMaxVals.elementAt(field) : this.origMaxVals.elementAt(field);

		//this.valueMin.setText("Value min: " + df.format(allValueMin));
		//this.valueMax.setText("Value max: " + df.format(allValueMax));

		colorbtn.setBackground(colors.elementAt(field));

		// brg 10/10/2011: only want to call onTypeListAction() when the user changes
		// the popup, not when we programmatically set its value when the fields table
		// selection changes. i can't find a way to distinguish between those events
		// (and indeed there may not be). this method is gross but works: remove the popup's
		// action listeners, set the popup selection, then restore the listeners.
		ActionListener[] savedListeners = typeList.getActionListeners();
		for ( ActionListener l : savedListeners ) { typeList.removeActionListener( l ); }
		
		typeList.setSelectedIndex(graphTypes.elementAt(field));
		
		for ( ActionListener l : savedListeners ) { typeList.addActionListener( l ); }
		
		final boolean isSelected = ( excludeStyleVals.elementAt(field) == 1 ) ? true : false;
		leaveGapsBox.setSelected( isSelected );

		repaint();
	}

	private void onFieldsTableChanged(final TableModelEvent event) {
		System.out.println("onFieldsTableChanged():");
		int row = event.getFirstRow();
		boolean checked = fieldsTable.isRowChecked(row);
		CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();

		if (!checked) {
			// if there is a graph id to match the track, section, dataset, table, field attributes
			// then have it removed, and for each section in the track
			//TrackSceneNode tnode = null;

/*			int[] selectedSections = this.getSelectedSections();
			for ( int selSectionIdx = 0; selSectionIdx < selectedSections.length; selSectionIdx++ )
			{
				final String selSectionName = this.sectionsList.getModel().getElementAt( selectedSections[selSectionIdx] ).toString();
				for ( int trackIdx = 0; trackIdx < tmodel.size(); trackIdx++ )
				{
					tnode = (TrackSceneNode) tmodel.elementAt( trackIdx );
					final CoreSection cs = tnode.getCoreSection( selSectionName );
					if ( cs != null )
					{
						final int dataTableId = this.getTableIndexByName( cs.getName() );
						final WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();
						System.out.println("Get scenegraph id for graph: track " + trackIdx + " section " + cs.getId() +
										   " dataset " + ds.getId() + " table " + dataTableId + " field " + row);
						final int gid = SceneGraph.getGraphID( tnode.getId(), cs.getId(), ds.getId(), dataTableId, row );
						
						SceneGraph.removeLineGraphFromSection(gid);
					}
				}
			}
*/
			
			//int[] selectedSections = this.getSelectedSections();
			for ( int curSectionIndex : this.getSelectedSections() )
			{
				final GraphParams gp = this.getGraphParams( curSectionIndex );
				if ( gp != null )
				{
					System.out.println("Get scenegraph id for graph: track " + gp.trackId + " section " + gp.sectionId +
									   " dataset " + gp.datasetId + " table " + gp.dataTableId + " field " + row );
					final int gid = SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, row );
					SceneGraph.removeLineGraphFromSection(gid);
				}
			}
			
			
			
//			WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();

//			if (!allSectionsBox.isSelected()) {
//				int gid = SceneGraph.getGraphID(selectedTrackId, selectedSectionId, ds.getId(), // currentDatasetIndex,
//						currentTableIndex, row);
//
//				SceneGraph.removeLineGraphFromSection(gid);
//
//			} else {
//			{
				// brg 10/2/2011: Lots of logic based on the current track, but we really want to search all tracks always
				//CoreGraph cg = CoreGraph.getInstance();
				//TrackSceneNode tnode = cg.getCurrentTrack();
//				CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
//				TrackSceneNode tnode = null;
				//CoreSection sec = null;
/*				
				for ( int trackIdx = 0; trackIdx < tmodel.size(); trackIdx++ )
				{
					tnode = (TrackSceneNode) tmodel.elementAt( trackIdx );

					if (tnode != null) {
						for (int sectionIdx = 0; sectionIdx < tnode.getNumCores(); sectionIdx++) {
							CoreSection cs = tnode.getCoreSection( sectionIdx );

							if (cs != null) {
								//int tid = this.getTableIndex( sectionIdx );
								final int tid = this.getTableIndexByName( cs.getName() );
								if ( tid == -1 )
									continue;
								
								// section is in dataset - is it selected in sections list? no need
								// to check if all sections box is checked
								if ( !allSectionsBox.isSelected() )
								{
									System.out.println("looking for section [" + cs.getName() + "]");
									final int[] selectedSections = this.sectionsList.getSelectedIndices();
									boolean matchFound = false;
									for ( int selIdx = 0; selIdx < selectedSections.length; selIdx++ )
									{
										final int curSectionIdx = selectedSections[selIdx];
										final String curSectionName = this.sectionsList.getModel().getElementAt(curSectionIdx).toString();
										System.out.println("\t" + curSectionName);
										if ( cs.getName().compareToIgnoreCase( curSectionName ) == 0 )
										{
											matchFound = true;
											System.out.println("match found!");
											break;
										}
									}
									
									if ( !matchFound )
									{
										System.out.println("no match");
										continue;
									}
								}
								
								System.out.println("Get scenegraph id for graph: track " + trackIdx + " section " + cs.getId() +
												   " dataset " + ds.getId() + " table " + tid + " field " + row);
								int gid = SceneGraph.getGraphID( trackIdx, cs.getId(), ds.getId(), // currentDatasetIndex,
										tid, row);

								SceneGraph.removeLineGraphFromSection(gid);
							} else {
								System.out.println("- cs is null!");
							}
						}
					} else {
						System.out.println("- tnode is null!");
					}
				}
			}
*/
		}
			// end removing graphs
		else // adding graphs
		{
			//int[] selectedSections = this.getSelectedSections();
			//System.out.println("num selected sections = " + selectedSections.length);
			
//			for ( int sidx = 0; sidx < selectedSections.length; sidx++ )
			for ( int curSectionIndex : this.getSelectedSections() ) //selectedSections )
			{
//				final int curSectionIndex = selectedSections[sidx];
			
//				CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
//				TrackSceneNode tnode = null;
//				CoreSection sec = null;
				//TrackSceneNode tnode = (TrackSceneNode) tmodel.elementAt(selectedTrackListId);

				GraphParams gp = this.getGraphParams( curSectionIndex );
				if ( gp == null )
					continue; // brgtodo: stifle "add missing section" code for now
				//boolean matchFound = (gp != null);
				
				//				final String secname = this.sectionsListModel.getElementAt(curSectionIndex).toString();
//				for ( int trackIdx = 0; !matchFound && trackIdx < tmodel.size(); trackIdx++ )
//				{
//					tnode = (TrackSceneNode) tmodel.elementAt( trackIdx );
//					sec = tnode.getCoreSection(secname);
//					if ( sec != null )
//						matchFound = true;
//				}
				
				//boolean justAppend = false;

				// If the dataset table contains section depth offset, use it.
				WellLogDataSet ds = (WellLogDataSet) this.datasetModel.getSelectedItem();
				//final int dataTableId = this.getTableIndexByName( secname );
				//if ( dataTableId == -1 ) return;
				
				WellLogTable table = ds.getTable( gp.dataTableId );

				// brgtodo ??? the whole "add an unfound section when
				// graphing" model seems to invite bad results.  We're better at finding sections now,
				// but may want to stifle this behavior and eventually make it modifiable in prefs?
				// commenting out for now.
/*
				if ( !matchFound )
				{
					// need to create new section
					// native first, java next for gid
					int secid = SceneGraph.addSectionToTrack(tnode.getId(), tnode.getNumCores());
					SceneGraph.setSectionName(tnode.getId(), secid, secname);
					// property?
					sec = new CoreSection(secname, secid);
					tnode.addCoreSection(sec);
					CoreGraph.getInstance().notifyListeners();

					// Figure out the top depth of a section
					float sectionBeginDepth;
					float sectionTopDepth = table.getTopDepth(); // in meters
					final float unitScale = UnitLength.getUnitScale( table.getDepthUnits() );

					if (sectionTopDepth != -1) {
						sectionBeginDepth = sectionTopDepth;
					} else if (table.getDepth_offset() != -1) {
						sectionBeginDepth = table.getDepth_offset() * unitScale;
					} else {
						sectionBeginDepth = 0.0f;
						justAppend = true;
					}

					SceneGraph.positionSection(tnode.getId(), secid, sectionBeginDepth * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0), 0);
				}
*/
				// if we created new coresection and add new graph
				// but has no sectionTopDepth or depth_offset
				// send section to the end of the track
				//if (justAppend) {
				//	SceneGraph.pushSectionToEnd(tnode.getId(), sec.getId());
				//	sec.setDepth(SceneGraph.getSectionDepth(tnode.getId(), sec.getId()));
				//}

				// now we have the section
				// native first, java next for gid
				// System.out.println("[Bremen] Creating graph using dataset: "
				// + ds.toString()
				// + " with id: " + ds.getId() + ", tableIdx: " +
				// this.currentTableIndex);


				//final int gid = SceneGraph.addLineGraphToSection( tnode.getId(), sec.getId(), ds.getId(), dataTableId, row );
				final int gid = SceneGraph.addLineGraphToSection( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, row );
				if ( gid == -1 ) {
					System.out.println("- Creating graph, but gid is: " + gid);
					return;
				}
				System.out.println( "setting properties for gid = " + gid );
				this.setGraphProperties( gid, row );

				// java side
				TrackSceneNode tnode = CoreGraph.getInstance().getCurrentSession().getTrackSceneNodeWithTrackId( gp.trackId );
				CoreSectionGraph csg = new CoreSectionGraph( gp.datasetId/*ds.getId()*/, gp.dataTableId, row, gid, tnode);
				
				final String secname = this.sectionsListModel.getElementAt(curSectionIndex).toString();
				csg.setName(secname);
				
				tnode.addCoreSectionGraph(csg, gp.sectionId/*sec.getId()*/, gid);

				// update current gid : native code id
				//this.selectedTrackId = tnode.getId();
				//this.selectedSectionId = sec.getId();
			}
		} // end adding graphs
		
		CorelyzerApp.getApp().updateGLWindows();
	}
	
	// Set ranges, graph color and style
	private void setGraphProperties( final int gid, final int row )
	{
		//System.out.println("onFieldsTableChanged: all sections set range");
		SceneGraph.setLineGraphRange( gid, scaleMinVals.elementAt( row ), scaleMaxVals.elementAt( row ));
		
		try {
			SceneGraph.setLineGraphExcludeRange( gid, excludeMinVals.elementAt(row), excludeMaxVals.elementAt( row ));
		} catch (Exception e) {
			System.out.println("Couldn't set exclude range: " + e.getMessage() );
		}
		
		SceneGraph.setLineGraphExcludeStyle( gid, excludeStyleVals.elementAt( row ));
		
		final Color c = colors.elementAt( row );
		SceneGraph.setLineGraphColor(gid, (c.getRed() / 255.0f), (c.getGreen() / 255.0f), (c.getBlue() / 255.0f));
		
		final Integer graphType = graphTypes.elementAt(row);
		SceneGraph.setLineGraphType( gid, graphType );
		
		final String fieldLabel = this.fieldsTable.getRowLabel(row);
		SceneGraph.setLineGraphLabel( gid, fieldLabel );
		
	}

	private void onScaleMaxTextLostFocus() {
		int field = fieldsTable.getSelectedRow();
		// System.out.println("MODIFIYING FIELD AT ROW: " + field);
		if (field < 0) {
			return;
		}

		try {
			float max;
			max = Float.parseFloat(scaleMaxText.getText());
			scaleMaxVals.setElementAt(max, field);
		} catch (NumberFormatException exp) {
			System.err.println("Wrong number format! " + scaleMaxText.getText());
		}
		// System.out.println("Max text: " + scaleMaxVals.elementAt(field));
	}

	private void onScaleMinTextLostFocus() {
		int field = fieldsTable.getSelectedRow();
		// System.out.println("MODIFIYING FIELD AT ROW: " + field);

		if (field < 0) {
			return;
		}

		try {
			float min;
			min = Float.parseFloat(scaleMinText.getText());
			scaleMinVals.setElementAt(min, field);
		} catch (NumberFormatException exp) {
			System.err.println("Wrong number format!" + scaleMinText.getText());
		}
		// System.out.println("Min text: " + scaleMinVals.elementAt(field));
	}
	
	private void onExcludeMaxLostFocus() {
		int field = fieldsTable.getSelectedRow();
		// System.out.println("MODIFIYING FIELD AT ROW: " + field);
		
		if (field < 0) {
			return;
		}
		
		try {
			float max;
			max = Float.parseFloat(excludeMaxText.getText());
			excludeMaxVals.setElementAt(max, field);
		} catch (NumberFormatException exp) {
			//System.err.println("Wrong number format!" + excludeMaxText.getText());
			excludeMaxVals.setElementAt(Float.MAX_VALUE, field);
		}
	}
	
	private void onExcludeMinLostFocus() {
		int field = fieldsTable.getSelectedRow();
		// System.out.println("MODIFIYING FIELD AT ROW: " + field);
		
		if (field < 0) {
			return;
		}
		
		try {
			float min;
			min = Float.parseFloat(excludeMinText.getText());
			excludeMinVals.setElementAt(min, field);
		} catch (NumberFormatException exp) {
			//System.err.println("Wrong number format!" + excludeMaxText.getText());
			excludeMinVals.setElementAt(-Float.MAX_VALUE, field);
		}		
	}

	private void onSectionsListChanged() {
		WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();

		// if (currentDatasetIndex < 0) {
		if (ds == null) {
			return;
		}

		this.selectedSectionListId = this.sectionsList.getSelectedIndex();
		if (this.selectedSectionListId == -1) {
			return;
		}

		this.currentTableIndex = this.selectedSectionListId;
		
		// 9/29/2011 brg: all the work to maintain currently selected section and track
		// on both sides of the fence is pervasive, and, I think, unnecessary. This is a
		// dialog, performance isn't important - just grab what you need at the last minute
		// and use it!

		// need to update selectedSectionId
		CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
		TrackSceneNode tnode = (TrackSceneNode) tmodel.elementAt(selectedTrackListId);
		String secname = this.sectionsListModel.getElementAt(this.selectedSectionListId).toString();

		CoreSection sec = tnode.getCoreSection(secname);
		if (sec == null) {
			this.selectedSectionId = -1;
		} else {
			this.selectedSectionId = sec.getId();
		}

		sectionsListLabel.setText( "Choose graph section(s): (" + sectionsList.getSelectedIndices().length + "/" +
								  sectionsList.getModel().getSize() + " selected)" );
		
		// this.updateFields(this.currentDatasetIndex);
		this.updateFields(ds);

		this.fieldsTable.setRowSelectionInterval(0, 0);
		this.fieldsTable.updateUI();

		this.onFieldsListChanged();
	}

	private void onTypeListAction() {
		System.out.println("onTypeListAction():");
		// TYPE SELECTED
		int field = fieldsTable.getSelectedRow();

		if (field < 0) {
			return;
		}

		graphTypes.setElementAt( typeList.getSelectedIndex(), field );

		// if there is a graph already there then change the type of
		// the graph

		// update current section info f i x m e
/*
		CRDefaultListModel tmodel = CorelyzerApp.getApp().getTrackListModel();
		TrackSceneNode tnode = (TrackSceneNode) tmodel.elementAt(selectedTrackListId);
		String secname = this.sectionsListModel.getElementAt(this.selectedSectionListId).toString();

		CoreSection sec = tnode.getCoreSection(secname);
		if (sec == null) {
			this.selectedSectionId = -1;
		} else {
			this.selectedSectionId = sec.getId();
		}

		WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();
*/
		
		int[] selectedSections = this.getSelectedSections();
		for ( int curSectionIndex : selectedSections )
		{
			final GraphParams gp = this.getGraphParams( curSectionIndex );
			if ( gp == null )
				continue;
			
			final int gid = SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, field );
			if (gid < 0)
				continue;
			
			final Integer t = graphTypes.elementAt(field);
			SceneGraph.setLineGraphType(gid, t);
		}

		CorelyzerApp.getApp().updateGLWindows();
		//this.onApply();
	}

	public void selectDataset(final int index) {

		if (index >= this.datasets.size() || index < 0) {
			return;
		}

		this.datasetList.setSelectedIndex(index);
	}

	public void setDatasetVec(final Vector<WellLogDataSet> dsvec) {
		if (dsvec.size() <= 0) {
			return;
		}

		this.datasets = dsvec;

		// Update Datalists ComboBox UI
		this.datasetModel.removeAllElements();
		for (int i = 0; i < datasets.size(); i++) {
			WellLogDataSet ds = datasets.elementAt(i);

			// String name = ds.getSourceFilename();
			// name = name.replace('\\', '/');
			// String tokens[] = name.split("/");

			// this.datasetModel.addElement(tokens[tokens.length - 1]);
			this.datasetModel.addElement(ds);
		}
	}

	/*
	 * private int getDatasetId(int datasetIndex) { int datasetId = -1;
	 * 
	 * CoreGraph cg = CoreGraph.getInstance(); if (cg != null) { Session s =
	 * cg.getCurrentSession(); if (s != null) { WellLogDataSet d =
	 * s.getDataset(datasetIndex); if (d != null) { datasetId = d.getId(); } } }
	 * 
	 * return datasetId; }
	 */

	public void setSelectedSectionId(final int id) {
		if (id < 0) {
			return;
		}
		this.selectedSectionId = id;
	}

	public void setSelectedSectionListId(final int id) {
		if (id < 0) {
			return;
		}
		this.selectedSectionListId = id;
	}

	public void setSelectedTrackAndSectionId(final int trackId, final int sectionId) {
		// System.out.println("[Bremen] setSelectedTrackAndSectionId: " +
		// trackId + ", " + sectionId);

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
			if (trackId == i) {
				tt = (TrackSceneNode) tmodel.elementAt(i);
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

		this.sectionsList.setSelectedIndex(sectionId);
		this.onSectionsListChanged();
	}

	public void setSelectedTrackId(final int id) {
		if (id < 0) {
			return;
		}
		this.selectedTrackId = id;
	}

	public void setSelectedTrackListId(final int id) {
		if (id < 0) {
			return;
		}
		this.selectedTrackListId = id;
	}
	
	// Aggregate data for currently selected sections
	private void aggregateEverything( final WellLogDataSet ds )
	{
		for ( int field = 0; field < ds.getTable( 0 ).getNumFields(); field++ )
		{
			boolean fieldHasGraph = false;
			int excludeStyle = 0;
			int graphType = 0;
			boolean graphTypeSet = false;

			float graphScaleMin = Float.MAX_VALUE, graphScaleMax = -Float.MAX_VALUE;
			float noGraphScaleMin = Float.MAX_VALUE, noGraphScaleMax = -Float.MAX_VALUE;

			float dataRangeMin = Float.MAX_VALUE, dataRangeMax = -Float.MAX_VALUE;
			
			float graphExcludeMin = -Float.MAX_VALUE, graphExcludeMax = Float.MAX_VALUE;
			float noGraphExcludeMin = -Float.MAX_VALUE, noGraphExcludeMax = Float.MAX_VALUE;
			
			Color graphColor = null;
			
			int[] selectedSections = this.getSelectedSections();
			for ( int curSectionIndex : selectedSections )
			{
				final GraphParams gp = this.getGraphParams( curSectionIndex );
				if ( gp != null )
				{
					final int gid = SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, field );
					if ( gid >= 0 ) // graph exists, get values from scenegraph
					{
						// scale min/max
						graphScaleMin = Math.min( graphScaleMin, SceneGraph.getGraphMin( gid ));
						graphScaleMax = Math.max( graphScaleMax, SceneGraph.getGraphMax( gid ));
						
						// exclude min/max - look for a non-default minimum
						// brgtodo 10/5/2011: Another situation where a "exclude or not?" flag would
						// result in more elegant code than the Float.MAX_VALUE default stuff,
						// which could also cause weirdness when moving files between 32-bit and
						// 64-bit environments.
						final float sgExcludeMin = SceneGraph.getGraphExcludeMin( gid );
						final float sgExcludeMax = SceneGraph.getGraphExcludeMax( gid );

						if ( graphExcludeMin <= -Float.MAX_VALUE )
						{						
							if ( sgExcludeMin > -Float.MAX_VALUE )
								graphExcludeMin = sgExcludeMin;
						}
						else
						{
							if ( sgExcludeMin > -Float.MAX_VALUE )
								graphExcludeMin = Math.min( graphExcludeMin, sgExcludeMin );
						}
						
						if ( graphExcludeMax >= Float.MAX_VALUE )
						{
							if ( sgExcludeMax < Float.MAX_VALUE )
								graphExcludeMax = sgExcludeMax;
						}
						else
						{
							if ( sgExcludeMax < Float.MAX_VALUE )
								graphExcludeMax = Math.max( graphExcludeMax, sgExcludeMax );
						}

						fieldHasGraph = true;
						
						// graph type - since there's no way to aggregate this, use the first type encountered
						if ( !graphTypeSet )
						{
							graphType = SceneGraph.getLineGraphType( gid );
							graphTypeSet = true;
						}
						
						// color
						final float r = SceneGraph.getLineGraphColorComponent( gid, 0 );
						final float g = SceneGraph.getLineGraphColorComponent( gid, 1 );
						final float b = SceneGraph.getLineGraphColorComponent( gid, 2 );
						graphColor = new Color( r, g, b );
						//colors.add(new Color(r, g, b));
						
						// exclude style
						if ( SceneGraph.getGraphExcludeStyle( gid ) > 0 )
							excludeStyle = 1; // leave gaps
						
//						System.out.println("graph on section: exclmin = " + graphExcludeMin + " exclmax = " + graphExcludeMax );
					}
					else // no graph, use defaults 
					{
						final WellLogTable t = ds.getTable( gp.dataTableId );
						noGraphScaleMin = Math.min( noGraphScaleMin, t.getColumnMin( field ));
						noGraphScaleMax = Math.max( noGraphScaleMax, t.getColumnMax( field ));
						noGraphExcludeMin = -Float.MAX_VALUE;//Math.min( noGraphExcludeMin, Float.MAX_VALUE );//SceneGraph.getGraphExcludeMin( gid ));
						noGraphExcludeMax = Float.MAX_VALUE;//Math.max( noGraphExcludeMax, -Float.MAX_VALUE );//SceneGraph.getGraphExcludeMax( gid ));

						Random randGen = new Random();
						int r = (int) (100 + 0.49803 * randGen.nextInt(256));
						int g = (int) (100 + 0.49803 * randGen.nextInt(256));
						int b = (int) (100 + 0.49803 * randGen.nextInt(256));
						graphColor = new Color( r, g, b );
						//colors.add(new Color(r, g, b));
						
						//	System.out.println("no graph on section: exclmin = " + noGraphExcludeMin + " exclmax = " + noGraphExcludeMax );
					}

					// data range min/max: same whether or not there's a graph
					final WellLogTable t = ds.getTable( gp.dataTableId );
					dataRangeMin = Math.min( dataRangeMin, t.getColumnMin( field ));
					dataRangeMax = Math.max( dataRangeMax, t.getColumnMax( field ));
					
				} else {
					System.out.println("Couldn't get GraphParams for section index = " + curSectionIndex);
				}
			}
			
			scaleMinVals.add( fieldHasGraph ? graphScaleMin : noGraphScaleMin );
			scaleMaxVals.add( fieldHasGraph ? graphScaleMax : noGraphScaleMax );
			dataRangeMinVals.add( dataRangeMin );
			dataRangeMaxVals.add( dataRangeMax );
			excludeMinVals.add( fieldHasGraph ? graphExcludeMin : noGraphExcludeMin );
			excludeMaxVals.add( fieldHasGraph ? graphExcludeMax : noGraphExcludeMax );
			graphTypes.add( graphType );
			excludeStyleVals.add( excludeStyle );
			showGraphVals.add( new Boolean( fieldHasGraph ));
			colors.add( graphColor );
		}
	}

	// private void updateFields(int dsIdx) {
	private void updateFields(final WellLogDataSet ds) {
		// if (dsIdx < 0) return;
		if (ds == null) {
			return;
		}

		lastSelectedField = -1;

		// clear the fields table and the min/max value vectors
		fieldsTable.clearTable();
		colors.clear();
		graphTypes.clear();

		scaleMinVals.clear();
		scaleMaxVals.clear();
		origMinVals.clear();
		origMaxVals.clear();
		
		excludeMinVals.clear();
		excludeMaxVals.clear();
		excludeStyleVals.clear();
		
		dataRangeMinVals.clear();
		dataRangeMaxVals.clear();
		
		showGraphVals.clear();

//		this.allMinVals.clear();
//		this.allMaxVals.clear();

		this.aggregateEverything( ds );
		
		int[] selectedSections = this.getSelectedSections(); //sectionsList.getSelectedIndices();
		// aggregate showGraph values dumbly - if *any* of the selected sections has a graph
		// on a field, check the "Show" box
//		final int numFields = ds.getTable(0).getNumFields();
//		boolean[] showGraph = new boolean[numFields];
//		for ( int i = 0; i < showGraph.length; i++ ) { showGraph[i] = false; }
			
/*
		for ( int curSectionIndex : selectedSections )
		{
			// search for graphs in current section
			final GraphParams gp = this.getGraphParams( curSectionIndex );
			if ( gp != null )
			{
				// for each field, see if there's a graph in the section
				for ( int fieldId = 0; fieldId < ds.getTable(0).getNumFields(); fieldId++ )
				{
					if ( SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, fieldId ) >= 0 )
					{
						if ( !showGraph[ fieldId ] )
							showGraph[ fieldId ] = true;
					}
				}
			}
		}
*/
		// 9/29/2011 brg: aggregate min/max data range for all selected sections to display
		// in Data Range column of fields table
//		WellLogTable t = ds.getTable(0);
//		Vector<Float> dataRangeMin = new Vector<Float>();
//		Vector<Float> dataRangeMax = new Vector<Float>();
//		for ( int fidx = 0; fidx < t.getNumFields(); fidx++ ) {
//			float min = Float.MAX_VALUE; //0.0f;
//			float max = -Float.MAX_VALUE;//0.0f;
//			for ( int curSectionIndex : selectedSections ) { //selIdx = 0; selIdx < selectedSections.length; selIdx++) {
//				final int dataTableId = this.getTableIndex( curSectionIndex );
//				t = ds.getTable( dataTableId );
//				//t = ds.getTable(selectedSections[selIdx]);
//		
//				min = ( min > t.getColumnMin( fidx )) ? t.getColumnMin( fidx ) : min;
//				max = ( max < t.getColumnMax( fidx )) ? t.getColumnMax( fidx ) : max;
//			}
//			
//			dataRangeMinVals.add( min );
//			dataRangeMaxVals.add( max );
//		}
		
		// WellLogDataSet ds = datasets.elementAt(dsIdx);
		//WellLogTable t = ds.getTable(this.selectedSectionListId);

		final WellLogTable t = ds.getTable( 0 ); // only used to get field count and names, any table will do so use the first
		Random generator = new Random(System.currentTimeMillis());
		for (int j = 0; j < t.getNumFields(); j++)
		{
			final boolean checkShowGraph = showGraphVals.elementAt( j ).booleanValue();
			final String fieldName = t.getHeader( j + 1 );
			final String dataRange = String.valueOf( dataRangeMinVals.elementAt( j )) + " - " + String.valueOf( dataRangeMaxVals.elementAt( j ));
			fieldsTable.addRow( checkShowGraph, fieldName, dataRange );

			// if there is already a graph then we need to
			// pull up the info about the graph, otherwise
			// we use defaults
			//this.origMinVals.add(t.getColumnMin(j));
			//this.origMaxVals.add(t.getColumnMax(j));

			//if ( !checkShowGraph ) {
				//scaleMinVals.add(t.getColumnMin(j));
				//scaleMaxVals.add(t.getColumnMax(j));
				//scaleMinVals.add( dataRangeMin.elementAt( j ));
				//scaleMaxVals.add( dataRangeMax.elementAt( j ));
				//graphTypes.add(0);
				
				// dummys for exclude range and style
				//excludeMinVals.add( -Float.MAX_VALUE );
				//excludeMaxVals.add( Float.MAX_VALUE );
				//excludeStyleVals.add( 0 ); // no gaps 10/2/2011 brgtodo ENUM

				//int r = (int) (100 + 0.49803 * generator.nextInt(256));
				//int g = (int) (100 + 0.49803 * generator.nextInt(256));
				//int b = (int) (100 + 0.49803 * generator.nextInt(256));

				//colors.add(new Color(r, g, b));
			//} else {
				// 10/4/2011 brgtodo: just use graph of first selected section for now...
				//GraphParams gp = this.getGraphParams( selectedSections[0] );
				//final int gid = SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, j );
				//System.out.println("updateFields: gid = " + gid);
				//final int gid = SceneGraph.getGraphID( selectedTrackId, selectedSectionId, ds.getId(), this.selectedSectionListId, j );

				//scaleMinVals.add(SceneGraph.getGraphMin(gid));
				//scaleMaxVals.add(SceneGraph.getGraphMax(gid));
				
				//excludeMinVals.add(SceneGraph.getGraphExcludeMin(gid));
				//excludeMaxVals.add(SceneGraph.getGraphExcludeMax(gid));
				//excludeStyleVals.add(SceneGraph.getGraphExcludeStyle(gid));
				
				//float r, g, b;
				//r = SceneGraph.getLineGraphColorComponent(gid, 0);
				//g = SceneGraph.getLineGraphColorComponent(gid, 1);
				//b = SceneGraph.getLineGraphColorComponent(gid, 2);
				//colors.add(new Color(r, g, b));
				
				//final int gt = SceneGraph.getLineGraphType(gid);
				//graphTypes.add(gt);
			//}
		}

		this.scaleMinText.setText("");
		this.scaleMaxText.setText("");

		// collect fields' all min/max
//		for (int i = 0; i < t.getNumFields(); i++) {
//			float min = 0.0f;
//			float max = 0.0f;
//
//			for (int j = 0; j < ds.getNumTables(); j++) {
//				WellLogTable table = ds.getTable(j);
//
//				if (j == 0) {
//					min = table.getColumnMin(i);
//					max = table.getColumnMax(i);
//				} else {
//					min = min > table.getColumnMin(i) ? table.getColumnMin(i) : min;
//					max = max < table.getColumnMax(i) ? table.getColumnMax(i) : max;
//				}
//			}
//
//			this.allMinVals.add(min);
//			this.allMaxVals.add(max);
//		}
	}
}
