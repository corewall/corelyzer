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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import org.chronos.util.j2k.J2KUtils;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import corelyzer.data.CRPreferences;
import corelyzer.data.CoreSection;
import corelyzer.data.ImagePropertyTable;
import corelyzer.data.ImagePropertyTableModel;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.graphics.SceneGraph;
import corelyzer.helper.ExampleFileFilter;
import corelyzer.util.FeedUtils;
import corelyzer.util.FileUtility;
import corelyzer.util.core.CoreModule;
import corelyzer.util.image.ImageModule;

public class CRLoadImageDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 627120465409891512L;

	public static void main(final String[] args) {
		CRLoadImageDialog dialog = new CRLoadImageDialog(null);
		dialog.pack();
		dialog.setVisible(true);
		System.exit(0);
	}

	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JButton filesButton;
	private JButton openButton;
	private JButton saveButton;
	private JButton resetButton;
	private JButton helpButton;
	private JTable imageTable;

	private JPanel batchInputPanel;
	private JCheckBox useBatchInputCheckbox;
	private JLabel orientationLabel;
	private JComboBox orientationComboBox;
	private JLabel lengthLabel;
	private JTextField lengthField;
	private JLabel dpiXLabel;
	private JTextField dpiXField;
	private JLabel dpiYLabel;
	private JTextField dpiYField;
	private JLabel startDepthLabel;
	private JTextField startDepthField;
	private JLabel depthIncLabel;
	private JTextField depthIncField;
	private JButton applyButton;

	public CRLoadImageDialog(final Frame owner) {
		super(owner);
		$$$setupUI$$$();
		setTitle("Load Images");

		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);

		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onOK();
			}
		});

		buttonCancel.addActionListener(new ActionListener() {
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
		filesButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onFiles();
			}
		});
		openButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				selectAndLoadCSVFileToList(",");
			}
		});
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				saveTableToCSVFile(",");
			}
		});
		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onReset();
			}
		});
		applyButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onApply();
			}
		});
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onHelp();
			}
		});

		useBatchInputCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onBatch();
			}
		});
		
		// batch label enabling/disabling based on field contents
		orientationComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				if (orientationComboBox.getSelectedIndex() == 2) // [Blank]
					orientationLabel.setEnabled(false);
				else
					orientationLabel.setEnabled(true);
			}
		});
		lengthField.getDocument().addDocumentListener( LabelEnablerFactory.create(lengthField, lengthLabel ));
		dpiXField.getDocument().addDocumentListener( LabelEnablerFactory.create(dpiXField, dpiXLabel ));
		dpiYField.getDocument().addDocumentListener( LabelEnablerFactory.create(dpiYField, dpiYLabel ));
		startDepthField.getDocument().addDocumentListener( LabelEnablerFactory.create(startDepthField, startDepthLabel ));
		depthIncField.getDocument().addDocumentListener( LabelEnablerFactory.create(depthIncField, depthIncLabel ));
	}
	
	private static class LabelEnablerFactory {
		public static DocumentListener create(final JTextField field, final JLabel label)
		{
			DocumentListener dl = new DocumentListener() {
				public void insertUpdate(DocumentEvent e) { doUpdate(); }
				public void removeUpdate(DocumentEvent e) {	doUpdate(); }
				public void changedUpdate(DocumentEvent e) { }
				
				private void doUpdate()
				{
					final boolean populated = !field.getText().equals("");
					label.setEnabled(populated);
				}
			};
			
			return dl;
		}
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
		createUIComponents();
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buttonCancel = new JButton();
		buttonCancel.setText("Cancel");
		panel2.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		buttonOK = new JButton();
		buttonOK.setText("OK");
		panel2.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		helpButton = new JButton();
		helpButton.setText("Help");
		panel1.add(helpButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
		contentPane
				.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
						0, false));
		panel3.setBorder(BorderFactory.createTitledBorder("Image Files and Properties"));
		final JPanel panel4 = new JPanel();
		panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JPanel panel5 = new JPanel();
		panel5.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel4.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final Spacer spacer2 = new Spacer();
		panel5.add(spacer2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(122, 14), null, 0, false));
		final JPanel panel6 = new JPanel();
		panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel5.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
				new Dimension(122, 33), null, 0, false));
		filesButton = new JButton();
		filesButton.setText("Select Image Files");
		panel6.add(filesButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, 29), null, 0, false));
		final JPanel panel7 = new JPanel();
		panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel5.add(panel7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
				new Dimension(122, 33), null, 0, false));
		openButton = new JButton();
		openButton.setEnabled(true);
		openButton.setText("Open an Image Listing File");
		panel7.add(openButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel8 = new JPanel();
		panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel5.add(panel8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
				new Dimension(122, 33), null, 0, false));
		saveButton = new JButton();
		saveButton.setEnabled(true);
		saveButton.setText("Save the Image Listing");
		panel8.add(saveButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel9 = new JPanel();
		panel9.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel5.add(panel9, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
				new Dimension(122, 33), null, 0, false));
		resetButton = new JButton();
		resetButton.setText("Reset");
		panel9.add(resetButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel10 = new JPanel();
		panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel4.add(panel10, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JScrollPane scrollPane1 = new JScrollPane();
		panel10.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		scrollPane1.setViewportView(imageTable);
		batchInputPanel = new JPanel();
		batchInputPanel.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
		batchInputPanel.setEnabled(true);
		panel3.add(batchInputPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		batchInputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
		final Spacer spacer3 = new Spacer();
		batchInputPanel.add(spacer3, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(195, 14), null, 0, false));
		dpiXLabel = new JLabel("DPI X: ");
		batchInputPanel.add(dpiXLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(195, 16), null, 0, false));
		dpiYLabel = new JLabel("DPI Y: ");
		batchInputPanel.add(dpiYLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(195, 16), null, 0, false));
		startDepthLabel = new JLabel("Start Depth (meter): ");
		batchInputPanel.add(startDepthLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(195, 16), null, 0, false));
		depthIncLabel = new JLabel("Depth Increment (meter): ");
		batchInputPanel.add(depthIncLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(195, 16), null, 0, false));
		
		dpiXField = new JTextField();
		dpiXField.setEnabled(false);
		dpiXField.setHorizontalAlignment(11);
		dpiXField.setText("254");
		
		dpiYField = new JTextField();
		dpiYField.setEnabled(false);
		dpiYField.setHorizontalAlignment(11);
		dpiYField.setText("254");
		
		batchInputPanel.add(dpiXField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
		
		batchInputPanel.add(dpiYField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
		
		startDepthField = new JTextField();
		startDepthField.setEnabled(false);
		startDepthField.setHorizontalAlignment(11);
		startDepthField.setText("0.0");
		batchInputPanel.add(startDepthField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
		depthIncField = new JTextField();
		depthIncField.setEnabled(false);
		depthIncField.setHorizontalAlignment(11);
		depthIncField.setText("1.5");
		batchInputPanel.add(depthIncField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
		final JPanel panel11 = new JPanel();
		panel11.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		batchInputPanel.add(panel11, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		applyButton = new JButton();
		applyButton.setEnabled(false);
		applyButton.setText("Apply");
		panel11.add(applyButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final Spacer spacer4 = new Spacer();
		panel11.add(spacer4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		orientationComboBox = new JComboBox();
		orientationComboBox.setEnabled(false);
		final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
		defaultComboBoxModel1.addElement("Horizontal");
		defaultComboBoxModel1.addElement("Vertical");
		defaultComboBoxModel1.addElement("[Blank]");
		orientationComboBox.setModel(defaultComboBoxModel1);
		batchInputPanel.add(orientationComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		lengthLabel = new JLabel("Length (meter): ");
		batchInputPanel.add(lengthLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		lengthField = new JTextField();
		lengthField.setEnabled(false);
		lengthField.setHorizontalAlignment(11);
		lengthField.setText("1.0");
		batchInputPanel.add(lengthField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		orientationLabel = new JLabel("Orientation: ");
		batchInputPanel.add(orientationLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(195, 16), null, 0, false));
		useBatchInputCheckbox = new JCheckBox();
		useBatchInputCheckbox.setText("Batch input: blank fields will not be applied");
		panel3.add(useBatchInputCheckbox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		
		onBatch();
	}

	private void applyGroupPropToTable() {
		ImagePropertyTable theTable = (ImagePropertyTable) imageTable;
		
		final int orientation = orientationComboBox.getSelectedIndex();
		if ( orientation < 2 )
			theTable.applyAllOrientation( orientation );

		if (!dpiXField.getText().equals(""))
		{
			final int dpiX = Integer.valueOf(dpiXField.getText());
			theTable.applyAllDPIX(dpiX);
		}
		
		if (!dpiYField.getText().equals(""))
		{
			final int dpiY = Integer.valueOf(dpiYField.getText());
			theTable.applyAllDPIY(dpiY);
		}

		if (!lengthField.getText().equals(""))
		{
			final float length = Float.valueOf(lengthField.getText());
			theTable.applyAllLength(length);
		}
		
		// depth increment and start depth: both must be populated to apply
		if (!depthIncField.getText().equals("") && !startDepthField.getText().equals(""))
		{
			final float depthInc = Float.valueOf(depthIncField.getText());
			final float depthStart = Float.valueOf(startDepthField.getText());
			theTable.applyAllDepths(depthStart, depthInc);
		}
		else if (!depthIncField.getText().equals("") || !startDepthField.getText().equals(""))
		{
			JOptionPane.showMessageDialog(this, "Both Start Depth and Depth Increment must be populated to apply values in Depth column");
		}
	}

	private void createUIComponents() {
		imageTable = new ImagePropertyTable();
	}

	public void fillListWithFiles(final File[] files) {
		ImagePropertyTable theTable = (ImagePropertyTable) imageTable;

		if (files.length > 0) {
			for (File file : files) {
				String s = file.getAbsolutePath();

				String ext = FileUtility.getExtension(file);
				if (ext.equalsIgnoreCase("jp2")) { // jpeg2000
					List<String> xmlPayloads = J2KUtils.getXMLPayloads(file);

					// FIXME
					if ((xmlPayloads == null) || (xmlPayloads.size() == 0)) {
						theTable.addImagePath(s);
						continue;
					}

					for (String xml : xmlPayloads) {
						// see if it's core section image spec related
						if (FeedUtils.isValidSyndEntry(xml)) {
							try {
								file.toURI().toURL().toString();
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}

							float dpix = 254;
							float dpiy = 254;
							float depth = 0;
							float length = 1;
							String orientation = ImagePropertyTableModel.HORIZONTAL;

							CoreModule module1 = FeedUtils.getCoreModule(xml);
							if (module1 == null) {
								System.err.println(file + " has no CoreModule");
							} else {
								depth = (float) module1.getDepth();
								length = (float) module1.getLength();
							}

							ImageModule module2 = FeedUtils.getImageModule(xml);
							if (module2 == null) {
								System.err.println(file + " has no ImageModule");
							} else {
								dpix = (float) module2.getDPIX();
								dpiy = (float) module2.getDPIY();
								orientation = module2.getOrientation();
							}

							theTable.addImageAndProperties(s, orientation, length, dpix, dpiy, depth);

							System.out.println(s + " added");

							break;
						} else {
							System.out.println("Not feed entry");
						}
					}
				} else {
					theTable.addImagePath(s);
				}
			}
		}

		imageTable.updateUI();
	}

	private boolean isValidFloat(final String s) {
		try {
			Float.parseFloat(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isValidInt(final String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void loadImagesWithProperties() {
		CorelyzerApp app = CorelyzerApp.getApp();

		if (app != null) {
			dispose();

			int trackIdx = app.getSelectedTrackIndex();

			JProgressBar progress = app.getProgressUI();
			progress.setString("Loading Images");
			progress.setMaximum(imageTable.getRowCount());
			progress.setValue(0);
			// progress.setVisible(true);

			ImagePropertyTable theTable = (ImagePropertyTable) imageTable;

			for (int i = 0; i < imageTable.getRowCount(); i++) {
				String filepath = theTable.model.filepathVec.elementAt(i);
				String filename = theTable.model.fileNameVec.elementAt(i);
				String orientation = (String) theTable.model.getValueAt(i, 1);

				float length = (Float) theTable.model.getValueAt(i, 2);
				float dpix = (Float) theTable.model.getValueAt(i, 3);
				float dpiy = (Float) theTable.model.getValueAt(i, 4);
				float depth = (Float) theTable.model.getValueAt(i, 5);

				String fn = new File(filepath).getName();
				System.out.println("Loading image " + fn + " Orientation: " + orientation + " Length: " + length + " DPIX: " + dpix + " DPIY: " + dpiy
						+ " Depth:" + depth);

				progress.setValue(i + 1);
				progress.setString(fn);

				File f = new File(filepath);
				String url;
				int secId;
				try {
					url = f.toURI().toURL().toString();
					secId = app.loadImage(f, url);
				} catch (MalformedURLException e) {
					secId = -1;
					e.printStackTrace();
				}

				if (secId < 0) {
					continue;
				}

				SceneGraph.lock();
				{
					// check currnet section depth
					// If not zero, leave it as is
					// Otherwize, set depth by property dlg
					CRDefaultListModel tmodel = app.getTrackListModel();
					TrackSceneNode tnode = (TrackSceneNode) tmodel.elementAt(trackIdx);
					String secname = filename.substring(0, filename.lastIndexOf('.'));
					CoreSection cs = tnode.getCoreSection(secname);
					int track = tnode.getId();

					boolean isVertical = orientation.toLowerCase().equals("vertical");
					SceneGraph.setSectionOrientation(track, secId, isVertical);

					if (cs == null) {
						return;
					}

					if (cs.getDepth() == 0) {
						SceneGraph.positionSection(track, secId, depth * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0), 0);
						cs.setDepth(SceneGraph.getSectionDepth(track, secId));
					}

					// Determine DPI
					if ((dpix > 0) && (dpiy > 0)) { // use DPI if available
						SceneGraph.setSectionDPI(track, secId, dpix, dpiy);
					} else if (length != 0) { // use Length & image size
						float dpi = ImagePropertyTable.DEFAULT_DPI;

						int imageId = SceneGraph.getImageIdForSection(track, secId);

						float imageWidth = SceneGraph.getImageWidth(imageId);
						float imageHeight = SceneGraph.getImageHeight(imageId);

						float lengthInPixel;
						if (orientation.toLowerCase().equals("horizontal")) {
							lengthInPixel = imageWidth;
						} else {
							lengthInPixel = imageHeight;
						}

						dpi = (float) (lengthInPixel / (length * 100 / 2.54));

						SceneGraph.setSectionDPI(track, secId, dpi, dpi);
					} else { // use default_dpi
						SceneGraph.setSectionDPI(track, secId, ImagePropertyTable.DEFAULT_DPI, ImagePropertyTable.DEFAULT_DPI);
					}

					SceneGraph.bringSectionToFront(track, secId);

					boolean imageLock = app.preferences.lockCoreSectionImage;
					SceneGraph.markSectionImmovable(track, secId, imageLock);
				}
				SceneGraph.unlock();

				app.updateGLWindows();
			}

			progress.setValue(imageTable.getRowCount());
			progress.setString("All images loaded");
			progress.setValue(0);
			// progress.dispose();
		}
	}

	private void onApply() {
		try {
			applyGroupPropToTable();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Invalid value: " + e.getMessage());
		}
	}

	private void onBatch() {
		final boolean b = useBatchInputCheckbox.isSelected();
		orientationLabel.setEnabled(b);
		orientationComboBox.setEnabled(b);
		lengthLabel.setEnabled(b);
		lengthField.setEnabled(b);
		dpiXLabel.setEnabled(b);
		dpiXField.setEnabled(b);
		dpiYLabel.setEnabled(b);
		dpiYField.setEnabled(b);
		startDepthLabel.setEnabled(b);
		startDepthField.setEnabled(b);
		depthIncLabel.setEnabled(b);
		depthIncField.setEnabled(b);
		applyButton.setEnabled(b);
	}

	private void onCancel() {
		dispose();
	}

	private void onFiles() {
		selectAndLoadImagesToList();
	}

	private void onHelp() {
		WikiHelpDialog dialog = new WikiHelpDialog(this, "CRLoadImageDialog");
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	private void onOK() {
		Runnable loading = new Runnable() {
			public void run() {
				loadImagesWithProperties();
			}
		};
		new Thread(loading).start();

		dispose();
	}

	private void onReset() {
		((ImagePropertyTable) imageTable).clearTable();
		imageTable.updateUI();
	}

	private void saveTableToCSVFile(final String delimiter) {
		// save current image list with each values to csv file
		// firt check if there is image file in the list
		int nFiles = imageTable.getRowCount();
		if (nFiles == 0) {
			JOptionPane.showMessageDialog(this, "Empty List!");
			return;
		}

		// show up general info message
		JOptionPane.showMessageDialog(this, "Save Image List File\n\n" + "Only support Comma Delimited File (.csv)\n"
				+ "File will inlcude below values in each line.\n" + "filename, orientation, length, dpix, dpiy, depth");

		// String saveFile = FileUtility.selectASingleFile(this,
		// "Save a image list file...",
		// "csv", FileUtility.SAVE);
		String saveFile = FileUtility.selectSingleFile(this, "Save an image list file", "csv", FileUtility.SAVE);

		if (saveFile == null) {
			return;
		}

		File selectedFile = new File(saveFile);

		// make sure it has .cvs at the end
		String path = selectedFile.getAbsolutePath();
		path = path.replace('\\', '/');
		String[] toks = path.split(delimiter);
		if (!toks[toks.length - 1].contains(".csv")) {
			path = path + ".csv";
			selectedFile = new File(path);
		}

		System.out.println("Saving to a CSV file..." + selectedFile);

		String filename, orientation;
		float length, dpix, dpiy, depth;

		try {
			String outLine;
			FileWriter fw = new FileWriter(selectedFile);
			BufferedWriter bw = new BufferedWriter(fw);

			ImagePropertyTable theTable = (ImagePropertyTable) imageTable;

			for (int i = 0; i < nFiles; i++) {
				// filename = (String) theTable.model.getValueAt(i, 0);
				filename = theTable.model.filepathVec.elementAt(i);
				orientation = (String) theTable.model.getValueAt(i, 1);
				length = (Float) theTable.model.getValueAt(i, 2);
				dpix = (Float) theTable.model.getValueAt(i, 3);
				dpiy = (Float) theTable.model.getValueAt(i, 4);
				depth = (Float) theTable.model.getValueAt(i, 5);

				// write property
				outLine = filename + "," + orientation + "," + length + "," + dpix + "," + dpiy + "," + depth + "\n";
				bw.write(outLine);
			}
			bw.flush();
			fw.flush();

			bw.close();
			fw.close();

			// show up success message
			JOptionPane.showMessageDialog(this, "Saving done successfully!");

		} catch (IOException e) {
			String mesg = "IOException in writing CSV file " + selectedFile;
			JOptionPane.showMessageDialog(this, mesg);

			e.printStackTrace();
		}
	}

	// load user processed csv file
	// each line inlcudes five tuples
	// filename, orientation, length, dpix, dpiy, depth
	private void selectAndLoadCSVFileToList(final String delimiter) {
		// show up general info message
		JOptionPane.showMessageDialog(this, "Open Image List File\n\n" + "Only support Comma Delimited File (.csv)\n"
				+ "File must follow below format in each line.\n" + "filename, orientation, length, dpix, dpiy, depth");

		// String aFileStr = FileUtility.selectASingleFile(this,
		// "Load a image list file...", "csv", FileUtility.LOAD);
		String aFileStr = FileUtility.selectSingleFile(this, "Load an image list file", "csv", FileUtility.LOAD);

		if (aFileStr == null) {
			return;
		}

		File selectedFile = new File(aFileStr);
		String basePath = new File(selectedFile.getParent()).getAbsolutePath();

		// vars for each tuples
		String filepath, orientation;
		float length, dpix, dpiy, depth;

		try {
			BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
			String line;
			String[] toks;
			int nLine = 0;
			while ((line = reader.readLine()) != null) {
				toks = line.split(delimiter);
				if (toks.length != 6) {
					JOptionPane.showMessageDialog(this, "Line format error!\n\n" + "Line does not have 6 separate values.\n" + "File: " + selectedFile + "\n"
							+ "Line: " + nLine + "\n" + line);
					nLine++;
					continue;
				}

				// check simple error
				boolean validated = true;
				for (int j = 0; j < 6; j++) {
					toks[j] = toks[j].trim();
					if (toks[j].length() == 0) { // missing token value!!!
						validated = false;
						break;
					}
				}

				if (!validated) {
					JOptionPane.showMessageDialog(this, "Missing value found!\n\n" + "File: " + selectedFile + "\n" + "Line: " + nLine + "\n" + line);
					nLine++;
					continue;
				}

				File imageFile = new File(toks[0]);
				if (imageFile.exists()) {
					filepath = toks[0];
				} else { // check relative path
					String sp = System.getProperty("file.separator");
					String composedFullPath = basePath + sp + toks[0];

					imageFile = new File(composedFullPath);

					if (imageFile.exists()) {
						filepath = imageFile.getAbsolutePath();
					} else {
						String mesg = "Image file '" + toks[0] + "' doesn't exist";
						JOptionPane.showMessageDialog(this, mesg);
						continue;
					}
				}

				orientation = toks[1];
				length = Float.parseFloat(toks[2]);
				dpix = Float.parseFloat(toks[3]);
				dpiy = Float.parseFloat(toks[4]);
				depth = Float.parseFloat(toks[5]);

				((ImagePropertyTable) imageTable).addImageAndProperties(filepath, orientation, length, dpix, dpiy, depth);
				nLine++;
			}
		} catch (Exception e) {
			String mesg = "Image List File Parse error";
			JOptionPane.showMessageDialog(this, mesg);
			System.err.println(mesg + ": " + e);
		}

		imageTable.updateUI();
	}

	private void selectAndLoadImagesToList() {
		ExampleFileFilter imageFileFilter = new ExampleFileFilter();
		imageFileFilter.setDescription("Images");
		imageFileFilter.addExtension("jpg");
		imageFileFilter.addExtension("jpeg");
		imageFileFilter.addExtension("png");
		imageFileFilter.addExtension("tif");
		imageFileFilter.addExtension("tiff");
		imageFileFilter.addExtension("bmp");
		imageFileFilter.addExtension("jp2"); // jpeg2000

		// 2/2/2012 brg: Sorting filenames properly, i.e. [file1, file2, file3, file20], not [file1, file2, file20, file3],
		// is tricky. Ended up with a mixed solution: using a funky JFileChooser subclass trick on Windows to override the
		// sort algorithm, and Quaqua on Mac. (The subclass trick works on Mac too, but the dialog looks unacceptably
		// strange while Quaqua's looks very nice.)
		JFileChooser chooser = null;
		LookAndFeel oldLAF = null;
		
		final boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if ( MAC_OS_X )
		{
			// 2/2/2012 brg: On Mac, use Quaqua file chooser to sort image files
			// properly: 
			oldLAF = UIManager.getLookAndFeel();

			try {
				UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
			} catch (ClassNotFoundException cnfe) {
				try {
					UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel15");
				} catch (Exception e) {
					System.out.println("Couldn't set Quaqua LAF - Java 1.5 or 1.6 required");
				}
			} catch (Exception e) { System.out.println("Couldn't set Quaqua LAF"); }

			chooser = new JFileChooser();
		}
		else // Windows
		{
			chooser = new LoadImageChooser();
		}
		
		chooser.setDialogTitle("Load image file(s)");
		chooser.setCurrentDirectory(new File(CRPreferences.getCurrentDir()));
		chooser.resetChoosableFileFilters();
		chooser.setFileFilter(imageFileFilter);
		chooser.setMultiSelectionEnabled(true);
		int returnVal = chooser.showOpenDialog(this);

		if ( MAC_OS_X ) // restore original look and feel
		{
			try {
				UIManager.setLookAndFeel( oldLAF );
			} catch (UnsupportedLookAndFeelException e) {
				System.out.println("Couldn't restore original LAF");
			}
		}
		
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			final File[] selectedFiles = chooser.getSelectedFiles();

			filesButton.setEnabled(false);
			buttonOK.setEnabled(false);

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					fillListWithFiles(selectedFiles);

					filesButton.setEnabled(true);
					buttonOK.setEnabled(true);
				}
			});

			if (selectedFiles.length != 0) {
				CRPreferences.setCurrentDir(selectedFiles[0].getParent());
			}
		}

		chooser.setMultiSelectionEnabled(false);
	}
}
