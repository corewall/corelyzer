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
import corelyzer.ui.ColorEditor;
import corelyzer.ui.ColorRenderer;

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
	private JButton applybtn, closebtn;
	private JComboBox datasetList;
	private JList sectionsList;
	private JLabel sectionsListLabel;
	private JCheckBox ifCollapseGraphs;
	private JComboBox typeList;
	private JTextField scaleMinText, scaleMaxText;
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

	// Min/max exclude range
	Vector<Float> excludeMinVals, excludeMaxVals;
	Vector<Integer> excludeStyleVals;
	
	Vector<Boolean> showGraphVals;
	Vector<Float> dataRangeMinVals, dataRangeMaxVals;
	
	static GraphColorsManager colorsManager = new GraphColorsManager();
	Vector<Color> colors;

	// 8/24/2011 brgtodo: typing would be clearer with an enum
	Vector<Integer> graphTypes; // line, point, crosspoint, point&lines

	Vector<WellLogDataSet> datasets;

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
				gpResult = new GraphParams( tnode.getId(), cs.getId(), ds.getId(), dataTableId );
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
		colorLinePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0,0,0,0), -1, -1));
//		colorbtn = new JButton();
//		colorbtn.setText("Color");
//		colorLinePanel.add(colorbtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
//				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(153, 29),
//				null, 0, false));
		typeList = new JComboBox();
		final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
		defaultComboBoxModel2.addElement("Line");
		defaultComboBoxModel2.addElement("Point");
		defaultComboBoxModel2.addElement("Cross point");
		defaultComboBoxModel2.addElement("Line & Points");
		typeList.setModel(defaultComboBoxModel2);
		colorLinePanel.add(typeList, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
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
		this.datasetModel = new DefaultComboBoxModel();
		this.datasetList.setModel(datasetModel);
		this.datasetList.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onDatasetAction();
			}
		});

		this.sectionsListModel = new DefaultListModel();
		this.sectionsList.setModel(this.sectionsListModel);
		this.sectionsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.sectionsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				onSectionsListChanged();
			}
		});

		fieldsTable.setDefaultRenderer(Color.class, new ColorRenderer( false ));
		fieldsTable.setDefaultEditor(Color.class, new ColorEditor());
		fieldsTable.setShowHorizontalLines(true);
		fieldsTable.getModel().addTableModelListener( new TableModelListener() {
			public void tableChanged( TableModelEvent event ) {
				onFieldsTableChanged( event );
			}
		});		
		fieldsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				onFieldsListChanged();
			}
		});

		graphTypes = new Vector<Integer>();
		scaleMinVals = new Vector<Float>();
		scaleMaxVals = new Vector<Float>();

		this.origMinVals = new Vector<Float>();
		this.origMaxVals = new Vector<Float>();
		this.excludeMinVals = new Vector<Float>();
		this.excludeMaxVals = new Vector<Float>();
		this.excludeStyleVals = new Vector<Integer>();
		this.dataRangeMinVals = new Vector<Float>();
		this.dataRangeMaxVals = new Vector<Float>();
		this.showGraphVals = new Vector<Boolean>();

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

	private void onApply() {
		for ( int curSectionIndex : this.sectionsList.getSelectedIndices() )
		{
			applyGraphSelection( curSectionIndex );
		}
		
		CorelyzerApp.getApp().updateGLWindows();
	}

	private void onCancel() {
		dispose();
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

		this.sectionsList.setSelectedIndex(0);
		
		fieldsTable.tableChanged(new TableModelEvent(fieldsTable.getModel()));
	}

	// ---------------------------------------------------------------

	private void onFieldsListChanged() {
		WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();

		if (ds == null) {
			return;
		}

		int field = fieldsTable.getSelectedRow();
		if (field < 0) {
			return;
		}

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

		// brg 10/10/2011: only want to call onTypeListAction() when the user changes
		// the popup, not when we programmatically set its value when the fields table
		// selection changes. Can't find a way to distinguish between those events
		// (and indeed there may not be). This method is gross but works: remove the popup's
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
		final int row = event.getFirstRow();
		final int col = event.getColumn();
		
		if ( col == 0 /* "Show" checkbox */ )
		{
			if ( !fieldsTable.isRowChecked( row )) // remove graph(s)
			{
				// if there is a graph id to match the track, section, dataset, table, field attributes
				// then have it removed, and for each section in the track
				for ( int curSectionIndex : this.sectionsList.getSelectedIndices() )
				{
					final GraphParams gp = this.getGraphParams( curSectionIndex );
					if ( gp != null )
					{
						//System.out.println("Get scenegraph id for graph: track " + gp.trackId + " section " + gp.sectionId +
						//				   " dataset " + gp.datasetId + " table " + gp.dataTableId + " field " + row );
						final int gid = SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, row );
						SceneGraph.removeLineGraphFromSection(gid);
					}
				}
			}
			else // add graph(s)
			{
				for ( int curSectionIndex : this.sectionsList.getSelectedIndices() )
				{
					GraphParams gp = this.getGraphParams( curSectionIndex );
					if ( gp == null )
						continue;
					
					// If the dataset table contains section depth offset, use it.
					WellLogDataSet ds = (WellLogDataSet) this.datasetModel.getSelectedItem();
					WellLogTable table = ds.getTable( gp.dataTableId );
					
					final int gid = SceneGraph.addLineGraphToSection( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, row );
					if ( gid == -1 ) {
						System.out.println("- Creating graph, but gid is: " + gid);
						return;
					}
					//System.out.println( "setting properties for gid = " + gid );
					this.setGraphProperties( gid, row );
					
					// java side
					TrackSceneNode tnode = CoreGraph.getInstance().getCurrentSession().getTrackSceneNodeWithTrackId( gp.trackId );
					CoreSectionGraph csg = new CoreSectionGraph( gp.datasetId, gp.dataTableId, row, gid, tnode);
					
					final String secname = this.sectionsListModel.getElementAt(curSectionIndex).toString();
					csg.setName(secname);
					
					tnode.addCoreSectionGraph(csg, gp.sectionId, gid);
				}
			} // end adding graphs
		}
		else if ( col == 2 /* Color */ )
		{
			Color newColor = (Color)fieldsTable.getModel().getValueAt( row, col );
			for ( int curSectionIndex : this.sectionsList.getSelectedIndices() )
			{
				final GraphParams gp = this.getGraphParams( curSectionIndex );
				if ( gp == null )
					continue;
				
				final int gid = SceneGraph.getGraphID( gp.trackId, gp.sectionId, gp.datasetId, gp.dataTableId, row );
				if (gid < 0)
					continue;
				
				// if a graph is present, change its color
				SceneGraph.setLineGraphColor(gid, ( newColor.getRed() / 255.0f), ( newColor.getGreen() / 255.0f), ( newColor.getBlue() / 255.0f));
			}

			final WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();
			CRGraphDialog.colorsManager.updateColorVector( ds, newColor, row );
		}
		
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
		
		final WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();
		final Color c = CRGraphDialog.colorsManager.getColorVector( ds ).elementAt( row );
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

	// brgtodo 10/12/2011: called twice when section is changed with a mouse click,
	// only once when changed with arrow key, why?
	private void onSectionsListChanged() {
		WellLogDataSet ds = (WellLogDataSet) this.datasetList.getSelectedItem();
		if (ds == null) {
			return;
		}

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

		for ( int curSectionIndex : this.sectionsList.getSelectedIndices() )
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

		this.sectionsList.setSelectedIndex( sectionId );
		this.onSectionsListChanged();
	}

	// Aggregate selected sections' data to display in fields table as follows:
	// - "Show" checkbox: if any sections have a graph on the current field, check
	// - Data range: min of all section mins, max of all section maxes
	// - Scale min/max: min of all section mins, max of all section maxes
	// - Exclude min/max: min of all section mins, max of all section maxes
	// - Graph type: use first type encountered in a graph
	// - Leave gaps checkbox (exclude style): if any sections leave gaps, check
	private void aggregateFieldsData( final WellLogDataSet ds )
	{
		for ( int field = 0; field < ds.getTable( 0 ).getNumFields(); field++ )
		{
			boolean fieldHasGraph = false;
			boolean graphTypeSet = false;
			int graphType = 0;
			int excludeStyle = 0;

			float graphScaleMin = Float.MAX_VALUE, graphScaleMax = -Float.MAX_VALUE;
			float noGraphScaleMin = Float.MAX_VALUE, noGraphScaleMax = -Float.MAX_VALUE;

			float dataRangeMin = Float.MAX_VALUE, dataRangeMax = -Float.MAX_VALUE;
			
			// init exclude min/max to defaults: if none of the sections exclude values,
			// these are the magic numbers that cause no text to be displayed in the exclude
			// fields. brgtodo 10/5/2011
			float graphExcludeMin = -Float.MAX_VALUE, graphExcludeMax = Float.MAX_VALUE;
			float noGraphExcludeMin = -Float.MAX_VALUE, noGraphExcludeMax = Float.MAX_VALUE;
			
			for ( int curSectionIndex : this.sectionsList.getSelectedIndices() )
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
		}
	}

	private void updateFields(final WellLogDataSet ds) {
		if (ds == null) {
			return;
		}

		// clear the fields table and the min/max value vectors
		fieldsTable.clearTable();
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

		this.aggregateFieldsData( ds );
		
		Vector<Color> colors = CRGraphDialog.colorsManager.getColorVector( ds );
		
		final WellLogTable t = ds.getTable( 0 ); // only used to get field count and names, any table will do so use the first
		Random generator = new Random(System.currentTimeMillis());
		for (int j = 0; j < t.getNumFields(); j++)
		{
			final boolean checkShowGraph = showGraphVals.elementAt( j ).booleanValue();
			final String fieldName = t.getHeader( j + 1 );
			final Color color = colors.elementAt( j );
			final String dataRange = String.valueOf( dataRangeMinVals.elementAt( j )) + " - " + String.valueOf( dataRangeMaxVals.elementAt( j ));
			fieldsTable.addRow( checkShowGraph, fieldName, color, dataRange );
		}

		this.scaleMinText.setText("");
		this.scaleMaxText.setText("");
	}
}

// Convenience class that aggregates values needed to query SceneGraph about graphs
class GraphParams {
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

// Maintains a list of graphing colors for the session.
class GraphColorsManager
{
	public GraphColorsManager()
	{
		colorVec = new Vector<Vector<Color>>();
		nameVec = new Vector<String>();
	}
	
	// Returns a vector of graph colors in field order for specified dataset.
	// If no vector exists, it will be created, using colors from existing graphs
	// and random values for the rest.
	public Vector<Color> getColorVector( final WellLogDataSet ds )
	{
		//System.out.print("Looking for matching color vector...");
		Vector<Color> v = this.findMatchingVector( ds );
		if ( v == null )
		{
			//System.out.println("not found, creating new vector");
			v = new Vector<Color>();
			for ( int fidx = 0; fidx < ds.getTable( 0 ).getNumFields(); fidx++ )
			{
				Color c = null;
				final int colorSourceGraph = SceneGraph.findGraphByField( ds.getId(), fidx );
				if ( colorSourceGraph >= 0 )
				{
					final float r = SceneGraph.getLineGraphColorComponent( colorSourceGraph, 0 );
					final float g = SceneGraph.getLineGraphColorComponent( colorSourceGraph, 1 );
					final float b = SceneGraph.getLineGraphColorComponent( colorSourceGraph, 2 );				
					c = new Color( r, g, b );
				}
				else
				{
					c = this.makeRandomGraphColor();
				}

				v.add( c );
			}
			
			colorVec.add( v );
			nameVec.add( ds.getSourceFilename() );
		}
		
		return v;
	}
	
	public void updateColorVector( final WellLogDataSet ds, final Color newColor, final int field )
	{
		Vector<Color> v = this.findMatchingVector( ds );
		if ( v != null )
		{
			v.setElementAt( newColor, field );
		}
	}
	
	private Vector<Color> findMatchingVector( final WellLogDataSet ds )
	{
		for ( int i = 0; i < nameVec.size(); i++ )
		{
			if ( ds.getSourceFilename() == nameVec.elementAt( i ))
			{
				return colorVec.elementAt( i );
			}
		}

		return null;
	}
	
	private Color makeRandomGraphColor()
	{
		Random randGen = new Random();
		int r = (int) (100 + 0.49803 * randGen.nextInt(256));
		int g = (int) (100 + 0.49803 * randGen.nextInt(256));
		int b = (int) (100 + 0.49803 * randGen.nextInt(256));
		Color c = new Color( r, g, b );
		return c;
	}
	
	Vector<Vector<Color>> colorVec;
	Vector<String> nameVec;
}
