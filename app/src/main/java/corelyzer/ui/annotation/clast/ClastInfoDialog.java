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
package corelyzer.ui.annotation.clast;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import corelyzer.data.CRPreferences;
import corelyzer.data.ChatGroup;
import corelyzer.data.MarkerType;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.annotation.AbstractAnnotationDialog;
import corelyzer.util.PropertyListUtility;

public class ClastInfoDialog extends AbstractAnnotationDialog implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5697133250484698105L;

	public static void main(final String[] args) {
		ClastInfoDialog dialog = new ClastInfoDialog();
		dialog.setCoreName("Sample3456");
		dialog.pack();
		dialog.setSize(700, 700);
		dialog.setVisible(true);
		System.exit(0);
	}

	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JTextField coreNameField;
	private JToggleButton granuleButton;
	private JToggleButton pebbleButton;
	private JToggleButton cobbleButton;
	private JToggleButton boulderButton;
	private JToggleButton angularButton;
	private JToggleButton subAngularButton;
	private JToggleButton subRoundedButton;
	private JToggleButton roundedButton;
	private JToggleButton granitoidButton;
	private JToggleButton sedimentaryButton;
	private JToggleButton metamorphicButton;
	private JToggleButton volcanicButton;
	private JToggleButton quartzButton;
	private JToggleButton doleriteButton;
	private JToggleButton intraclastButton;
	private JToggleButton vfgButton;
	private JToggleButton fgButton;
	private JToggleButton mgButton;
	private JToggleButton cgButton;
	private JTextArea notesArea;
	private JTextField textureField;
	private JTextField mineralsField;
	private JTextField sampleNoField;
	private JButton statisticsButton;
	private JTextField colorField;
	private JTextField upperLeftPositionField;
	private JTextField lowerRightPositionField;
	private JTextField widthField;
	private JTextField heightField;
	private JTextField trackNameField;
	private JTabbedPane singleOrCounted;
	private JPanel singleClastPane;
	private JPanel countedClastPane;
	private JPanel sizePane;
	private JTextField granuleCount;
	private JTextField pebbleCount;
	private JTextField cobbleCount;
	private JTextField boulderCount;
	private JPanel shapePane;
	private JTextField angularCount;
	private JTextField subAngularCount;
	private JTextField subRoundedCount;
	private JTextField roundedCount;
	private JPanel lithoPane;
	private JTextField volcanicCount;
	private JTextField granitoidCount;
	private JTextField sedimentaryCount;
	private JTextField metamorphicCount;
	private JTextField quartzCount;
	private JTextField doleriteCount;
	private JTextField intraclastCount;
	private JPanel grainSizePane;
	private JTextField vfgCount;
	private JTextField fgCount;
	private JTextField mgCount;
	private JTextField cgCount;
	private JPanel counterClastPane;
	private JButton deleteButton;
	private JTextField sessionNameField;
	private JTextField userNameField;

	private JTextField dateField;
	private ButtonGroup sizeGroup;
	private ButtonGroup shapeGroup;
	private ButtonGroup lithologyGroup;

	private ButtonGroup grainSizeGroup;
	// private Hashtable<String, String> attribs;
	private final Hashtable<String, AbstractButton> buttons;
	private final Hashtable<String, JTextComponent> textCompos;

	private final HashSet<String> buttonGroupSet;
	// Mode: single clast or multiple counted clasts
	public static int SINGLE = 0; // "SINGLE"

	public static int MULTIPLE = 1; // "MULTIPLE"

	{
		// GUI initializer generated by IntelliJ IDEA GUI Designer
		// >>> IMPORTANT!! <<<
		// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	public ClastInfoDialog() {
		this(null);
	}

	public ClastInfoDialog(final Frame f) {
		super(f);

		// attribs = new Hashtable<String, String>();
		// upperLeftPoint = new float[2];
		// lowerRightPoint = new float[2];

		// Model init
		buttons = new Hashtable<String, AbstractButton>();
		textCompos = new Hashtable<String, JTextComponent>();
		buttonGroupSet = new HashSet<String>();
		buttonGroupSet.add("size");
		buttonGroupSet.add("shape");
		buttonGroupSet.add("lithology");
		buttonGroupSet.add("grainsize");

		// View init
		setTitle("Clast Information");
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);

		buttonGroupsSetup();
		buildTextUIHash();

		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onSave();
			}
		});

		buttonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onCancel();
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent e) {
				onCancel();
			}
		});

		contentPane.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onCancel();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		statisticsButton.addActionListener(this);

		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				onDelete();
			}
		});
	}

	public ClastInfoDialog(final Frame owner, final String trackName, final String coreName, final Point ul, final Point lr) {
		this(owner);
		setTrackName(trackName);
		setCoreName(coreName);
		setRange(ul, lr);
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
		panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buttonOK = new JButton();
		buttonOK.setText("Save");
		panel2.add(buttonOK, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		buttonCancel = new JButton();
		buttonCancel.setText("Cancel");
		panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		deleteButton = new JButton();
		deleteButton.setText("Delete");
		panel2.add(deleteButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		statisticsButton = new JButton();
		statisticsButton.setText("Statistics...");
		panel1.add(statisticsButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
		contentPane
				.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
						0, false));
		final JPanel panel4 = new JPanel();
		panel4.setLayout(new GridLayoutManager(5, 4, new Insets(0, 0, 0, 0), -1, -1));
		panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JLabel label1 = new JLabel();
		label1.setText("Track Name");
		panel4.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label2 = new JLabel();
		label2.setText("Width (cm)");
		panel4.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		widthField = new JTextField();
		widthField.setEditable(false);
		widthField.setToolTipText("Width");
		panel4.add(widthField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(214, 22), null, 0, false));
		heightField = new JTextField();
		heightField.setEditable(false);
		heightField.setToolTipText("Height");
		panel4.add(heightField, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label3 = new JLabel();
		label3.setText("Range min x, y (cm)");
		label3.setToolTipText("Relative to the upper left corner of this core section");
		panel4.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
		upperLeftPositionField = new JTextField();
		upperLeftPositionField.setEditable(false);
		upperLeftPositionField.setToolTipText("Upper-Left Corner Position (relative to the upper-left corner of this core section)");
		panel4.add(upperLeftPositionField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(214, 22), null, 0, false));
		lowerRightPositionField = new JTextField();
		lowerRightPositionField.setEditable(false);
		lowerRightPositionField.setToolTipText("Lower-Right Position (relative to the upper-left corner of this core section)");
		panel4.add(lowerRightPositionField, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		coreNameField = new JTextField();
		coreNameField.setEditable(false);
		coreNameField.setToolTipText("Core Name");
		panel4.add(coreNameField, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		trackNameField = new JTextField();
		trackNameField.setColumns(0);
		trackNameField.setEditable(false);
		trackNameField.setToolTipText("Track Name");
		panel4.add(trackNameField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(214, 22), null, 0, false));
		final JLabel label4 = new JLabel();
		label4.setText("Core Name");
		panel4.add(label4, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label5 = new JLabel();
		label5.setText("Height (cm)");
		panel4.add(label5, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label6 = new JLabel();
		label6.setText("Range max x, y (cm)");
		panel4.add(label6, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label7 = new JLabel();
		label7.setText("Session Name");
		panel4.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		sessionNameField = new JTextField();
		sessionNameField.setEditable(false);
		sessionNameField.setEnabled(true);
		panel4.add(sessionNameField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label8 = new JLabel();
		label8.setText("Date");
		panel4.add(label8, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		dateField = new JTextField();
		dateField.setEditable(false);
		panel4.add(dateField, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label9 = new JLabel();
		label9.setText("Username");
		panel4.add(label9, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		userNameField = new JTextField();
		panel4.add(userNameField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JPanel panel5 = new JPanel();
		panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		singleOrCounted = new JTabbedPane();
		panel5.add(singleOrCounted, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
		singleClastPane = new JPanel();
		singleClastPane.setLayout(new GridLayoutManager(11, 1, new Insets(0, 0, 0, 0), -1, -1));
		singleOrCounted.addTab("Single Clast", singleClastPane);
		final JPanel panel6 = new JPanel();
		panel6.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
		singleClastPane.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		granuleButton = new JRadioButton();
		granuleButton.setText("granule");
		granuleButton.setToolTipText("2 - 4 mm");
		panel6.add(granuleButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED,
				null, new Dimension(213, 22), null, 0, false));
		pebbleButton = new JRadioButton();
		pebbleButton.setText("pebble");
		pebbleButton.setToolTipText("4 - 64 mm");
		panel6.add(pebbleButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		cobbleButton = new JRadioButton();
		cobbleButton.setText("cobble");
		cobbleButton.setToolTipText("64 - 256 mm");
		panel6.add(cobbleButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(213, 22), null, 0, false));
		boulderButton = new JRadioButton();
		boulderButton.setText("boulder");
		boulderButton.setToolTipText("> 256 mm");
		panel6.add(boulderButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label10 = new JLabel();
		label10.setText("Size");
		panel6.add(label10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
		final Spacer spacer2 = new Spacer();
		singleClastPane.add(spacer2, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		final JPanel panel7 = new JPanel();
		panel7.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
		singleClastPane.add(panel7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		angularButton = new JRadioButton();
		angularButton.setText("angular");
		panel7.add(angularButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(213, 22), null, 0, false));
		subAngularButton = new JRadioButton();
		subAngularButton.setText("sub-angular");
		panel7.add(subAngularButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		subRoundedButton = new JRadioButton();
		subRoundedButton.setText("sub-rounded");
		panel7.add(subRoundedButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(213, 22), null, 0, false));
		roundedButton = new JRadioButton();
		roundedButton.setText("rounded");
		panel7.add(roundedButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label11 = new JLabel();
		label11.setText("Shape");
		panel7.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
		final JPanel panel8 = new JPanel();
		panel8.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
		singleClastPane.add(panel8, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		volcanicButton = new JRadioButton();
		volcanicButton.setText("Volcanic");
		panel8.add(volcanicButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(213, 22), null, 0, false));
		intraclastButton = new JRadioButton();
		intraclastButton.setText("Intraclast");
		panel8.add(intraclastButton, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(213, 22), null, 0, false));
		granitoidButton = new JRadioButton();
		granitoidButton.setText("Granitoid");
		panel8.add(granitoidButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		sedimentaryButton = new JRadioButton();
		sedimentaryButton.setText("Sedimentary");
		panel8.add(sedimentaryButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(213, 22), null, 0, false));
		metamorphicButton = new JRadioButton();
		metamorphicButton.setText("Metamorphic");
		panel8.add(metamorphicButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		quartzButton = new JRadioButton();
		quartzButton.setText("Quartz");
		panel8.add(quartzButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(213, 22), null, 0, false));
		doleriteButton = new JRadioButton();
		doleriteButton.setText("Dolerite");
		panel8.add(doleriteButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label12 = new JLabel();
		label12.setText("Lithology");
		panel8.add(label12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
		final JPanel panel9 = new JPanel();
		panel9.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
		singleClastPane.add(panel9, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		vfgButton = new JRadioButton();
		vfgButton.setText("vfg");
		panel9.add(vfgButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(212, 22), null, 0, false));
		fgButton = new JRadioButton();
		fgButton.setText("fg");
		panel9.add(fgButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		mgButton = new JRadioButton();
		mgButton.setText("mg");
		panel9.add(mgButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(212, 22), null, 0, false));
		cgButton = new JRadioButton();
		cgButton.setText("cg");
		panel9.add(cgButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label13 = new JLabel();
		label13.setText("Grain-Size");
		panel9.add(label13, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
		final JPanel panel10 = new JPanel();
		panel10.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		singleClastPane.add(panel10, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JScrollPane scrollPane1 = new JScrollPane();
		panel10.add(scrollPane1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		notesArea = new JTextArea();
		notesArea.setRows(5);
		scrollPane1.setViewportView(notesArea);
		final JLabel label14 = new JLabel();
		label14.setText("Note");
		panel10.add(label14, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
		final JPanel panel11 = new JPanel();
		panel11.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
		singleClastPane.add(panel11, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JLabel label15 = new JLabel();
		label15.setText("Texture");
		panel11.add(label15, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
		final JLabel label16 = new JLabel();
		label16.setText("Minerals");
		panel11.add(label16, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label17 = new JLabel();
		label17.setText("Sample no. (TAL)");
		panel11.add(label17, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		textureField = new JTextField();
		panel11.add(textureField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		mineralsField = new JTextField();
		panel11.add(mineralsField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		sampleNoField = new JTextField();
		panel11.add(sampleNoField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label18 = new JLabel();
		label18.setText("Colour");
		panel11.add(label18, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
		colorField = new JTextField();
		panel11.add(colorField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final Spacer spacer3 = new Spacer();
		singleClastPane.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		final Spacer spacer4 = new Spacer();
		singleClastPane.add(spacer4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		final Spacer spacer5 = new Spacer();
		singleClastPane.add(spacer5, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		final Spacer spacer6 = new Spacer();
		singleClastPane.add(spacer6, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		counterClastPane = new JPanel();
		counterClastPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		singleOrCounted.addTab("Counted Clasts", counterClastPane);
		final JScrollPane scrollPane2 = new JScrollPane();
		counterClastPane.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		countedClastPane = new JPanel();
		countedClastPane.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
		scrollPane2.setViewportView(countedClastPane);
		sizePane = new JPanel();
		sizePane.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
		countedClastPane.add(sizePane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		sizePane.setBorder(BorderFactory.createTitledBorder("Size"));
		final JLabel label19 = new JLabel();
		label19.setText("granule");
		sizePane.add(label19, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		granuleCount = new JTextField();
		granuleCount.setText("0");
		sizePane.add(granuleCount, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label20 = new JLabel();
		label20.setText("Property*");
		sizePane.add(label20, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label21 = new JLabel();
		label21.setText("Value*");
		sizePane.add(label21, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label22 = new JLabel();
		label22.setText("pebble");
		sizePane.add(label22, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		pebbleCount = new JTextField();
		pebbleCount.setText("0");
		sizePane.add(pebbleCount, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label23 = new JLabel();
		label23.setText("cobble");
		sizePane.add(label23, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label24 = new JLabel();
		label24.setText("boulder");
		sizePane.add(label24, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		cobbleCount = new JTextField();
		cobbleCount.setText("0");
		sizePane.add(cobbleCount, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		boulderCount = new JTextField();
		boulderCount.setText("0");
		sizePane.add(boulderCount, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		shapePane = new JPanel();
		shapePane.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
		countedClastPane.add(shapePane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		shapePane.setBorder(BorderFactory.createTitledBorder("Shape"));
		final JLabel label25 = new JLabel();
		label25.setText("angular");
		shapePane.add(label25, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		angularCount = new JTextField();
		angularCount.setText("0");
		shapePane.add(angularCount, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label26 = new JLabel();
		label26.setText("sub-angular");
		shapePane.add(label26, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label27 = new JLabel();
		label27.setText("sub-rounded");
		shapePane.add(label27, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label28 = new JLabel();
		label28.setText("rounded");
		shapePane.add(label28, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		subAngularCount = new JTextField();
		subAngularCount.setText("0");
		shapePane.add(subAngularCount, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		subRoundedCount = new JTextField();
		subRoundedCount.setText("0");
		shapePane.add(subRoundedCount, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		roundedCount = new JTextField();
		roundedCount.setText("0");
		shapePane.add(roundedCount, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		lithoPane = new JPanel();
		lithoPane.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
		countedClastPane.add(lithoPane, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		lithoPane.setBorder(BorderFactory.createTitledBorder("Lithology"));
		final JLabel label29 = new JLabel();
		label29.setText("volcanic");
		lithoPane.add(label29, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		volcanicCount = new JTextField();
		volcanicCount.setText("0");
		lithoPane.add(volcanicCount, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label30 = new JLabel();
		label30.setText("granitoid");
		lithoPane.add(label30, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label31 = new JLabel();
		label31.setText("sedimentary");
		lithoPane.add(label31, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label32 = new JLabel();
		label32.setText("metamorphic");
		lithoPane.add(label32, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label33 = new JLabel();
		label33.setText("quartz");
		lithoPane.add(label33, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label34 = new JLabel();
		label34.setText("dolerite");
		lithoPane.add(label34, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label35 = new JLabel();
		label35.setText("intraclast");
		lithoPane.add(label35, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		granitoidCount = new JTextField();
		granitoidCount.setText("0");
		lithoPane.add(granitoidCount, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		sedimentaryCount = new JTextField();
		sedimentaryCount.setText("0");
		lithoPane.add(sedimentaryCount, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		metamorphicCount = new JTextField();
		metamorphicCount.setText("0");
		lithoPane.add(metamorphicCount, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		quartzCount = new JTextField();
		quartzCount.setText("0");
		lithoPane.add(quartzCount, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		doleriteCount = new JTextField();
		doleriteCount.setText("0");
		lithoPane.add(doleriteCount, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		intraclastCount = new JTextField();
		intraclastCount.setText("0");
		lithoPane.add(intraclastCount, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		grainSizePane = new JPanel();
		grainSizePane.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
		countedClastPane.add(grainSizePane, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		grainSizePane.setBorder(BorderFactory.createTitledBorder("Grain-size"));
		final JLabel label36 = new JLabel();
		label36.setText("vfg");
		grainSizePane.add(label36, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		vfgCount = new JTextField();
		vfgCount.setText("0");
		grainSizePane.add(vfgCount, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label37 = new JLabel();
		label37.setText("fg");
		grainSizePane.add(label37, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label38 = new JLabel();
		label38.setText("mg");
		grainSizePane.add(label38, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		final JLabel label39 = new JLabel();
		label39.setText("cg");
		grainSizePane.add(label39, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(101, 16), null, 0, false));
		fgCount = new JTextField();
		fgCount.setText("0");
		grainSizePane.add(fgCount, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		mgCount = new JTextField();
		mgCount.setText("0");
		grainSizePane.add(mgCount, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		cgCount = new JTextField();
		cgCount.setText("0");
		grainSizePane.add(cgCount, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
	}

	public void actionPerformed(final ActionEvent actionEvent) {
		this.onSave();

		ClastStatisticsDialog collectInfo = new ClastStatisticsDialog( CorelyzerApp.getApp().getMainFrame() );
		collectInfo.setLocationRelativeTo(this);
		collectInfo.pack();
		collectInfo.onRefresh();
		collectInfo.setVisible(true);
	}

	private void buildTextUIHash() {
		// Textfields for single tab panel
		textCompos.put("username", this.userNameField);
		textCompos.put("date", this.dateField);
		textCompos.put("notes", this.notesArea);

		textCompos.put("sessionname", this.sessionNameField);
		textCompos.put("trackname", this.trackNameField);
		textCompos.put("corename", this.coreNameField);

		textCompos.put("upperleft", this.upperLeftPositionField);
		textCompos.put("lowerright", this.lowerRightPositionField);

		// Clast-specifics
		textCompos.put("width", this.widthField);
		textCompos.put("height", this.heightField);
		textCompos.put("color", this.colorField);
		textCompos.put("texture", this.textureField);
		textCompos.put("minerals", this.mineralsField);
		textCompos.put("samplenumber", this.sampleNoField);

		// textfields for multiple counted tab panel
		textCompos.put("granuleCount", granuleCount);
		textCompos.put("pebbleCount", pebbleCount);
		textCompos.put("cobbleCount", cobbleCount);
		textCompos.put("boulderCount", boulderCount);

		// shape:
		textCompos.put("angularCount", angularCount);
		textCompos.put("sub-AngularCount", subAngularCount);
		textCompos.put("sub-RoundedCount", subRoundedCount);
		textCompos.put("roundedCount", roundedCount);

		// lithology
		textCompos.put("volcanicCount", volcanicCount);
		textCompos.put("granitoidCount", granitoidCount);
		textCompos.put("sedimentaryCount", sedimentaryCount);
		textCompos.put("metamorphicCount", metamorphicCount);
		textCompos.put("quartzCount", quartzCount);
		textCompos.put("doleriteCount", doleriteCount);
		textCompos.put("intraclastCount", intraclastCount);

		// grainSize
		textCompos.put("vfgCount", vfgCount);
		textCompos.put("fgCount", fgCount);
		textCompos.put("mgCount", mgCount);
		textCompos.put("cgCount", cgCount);
	}

	private void buttonGroupsSetup() {
		sizeGroup = new ButtonGroup();
		shapeGroup = new ButtonGroup();
		lithologyGroup = new ButtonGroup();
		grainSizeGroup = new ButtonGroup();

		sizeGroup.add(this.granuleButton);
		sizeGroup.add(this.pebbleButton);
		sizeGroup.add(this.cobbleButton);
		sizeGroup.add(this.boulderButton);

		shapeGroup.add(this.angularButton);
		shapeGroup.add(this.subAngularButton);
		shapeGroup.add(this.subRoundedButton);
		shapeGroup.add(this.roundedButton);

		lithologyGroup.add(this.volcanicButton);
		lithologyGroup.add(this.granitoidButton);
		lithologyGroup.add(this.sedimentaryButton);
		lithologyGroup.add(this.metamorphicButton);
		lithologyGroup.add(this.quartzButton);
		lithologyGroup.add(this.doleriteButton);
		lithologyGroup.add(this.intraclastButton);

		grainSizeGroup.add(this.vfgButton);
		grainSizeGroup.add(this.fgButton);
		grainSizeGroup.add(this.mgButton);
		grainSizeGroup.add(this.cgButton);

		// Add to Button UI hash
		ButtonGroup[] groups = { sizeGroup, shapeGroup, lithologyGroup, grainSizeGroup };

		for (ButtonGroup aGroup : groups) {
			Enumeration<AbstractButton> e = aGroup.getElements();
			while (e.hasMoreElements()) {
				AbstractButton b = e.nextElement();
				buttons.put(b.getText(), b);
			}
		}
	}

	public void collectViewInfo() {
		if (attribs != null) {
			String mode = this.getSelectMode() == ClastInfoDialog.SINGLE ? "SINGLE" : "MULTIPLE";
			attribs.put("mode", mode);

			// Single Clast, non-text components
			attribs.put("size", getClastSize());
			attribs.put("shape", getClastShape());
			attribs.put("lithology", getClastLithology());
			attribs.put("grainsize", getClastGrainSize());

			// Multiple counted clasts (all textComponents)
			// + some textComponents from single
			for (Map.Entry<String, JTextComponent> tc : textCompos.entrySet()) {
				String property = tc.getKey();
				String value = tc.getValue().getText();

				attribs.put(property, value);
			}
		}
	}

	public String generateFilename() {
		// Get user and time info
		String user = System.getProperty("user.name");

		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyy hh:mm:ss a z");
		String now = formatter.format(today);
		String title = "Annotation by " + user + "@\"" + now + "\"";
		System.out.println("Last edited by:\n" + title);

		// shorter string for annotation filename
		formatter = new SimpleDateFormat("MMddyyyyhhmmssz");
		now = formatter.format(today);

		// Save it into somewhere, with some unique naming
		// String cwd = System.getProperty("user.dir");
		CRPreferences prefs = CorelyzerApp.getApp().preferences();
		String sp = System.getProperty("file.separator");

		String anno_dir = prefs.annotation_Directory;
		String filename = "clast_annotation_" + trackId + "_" + sectionId + "_" + markerId + "_" + user + "_" + now + ".plist";

		return anno_dir + sp + filename;
	}

	public Hashtable getClastAttributes() {
		return attribs;
	}

	public String getClastGrainSize() {
		return getValueFromButtonGroupKey("grainsize");
	}

	public String getClastLithology() {
		return getValueFromButtonGroupKey("lithology");
	}

	public String getClastShape() {
		return getValueFromButtonGroupKey("shape");
	}

	public String getClastSize() {
		return getValueFromButtonGroupKey("size");
	}

	private int getSelectMode() {
		// 0: single
		// 1: multiple
		return this.singleOrCounted.getSelectedIndex();
	}

	public String getValueFromButtonGroupKey(final String key) {
		if (key == null) {
			return "NA";
		}
		String value = "NA";

		ButtonGroup aGroup;

		if (key.equalsIgnoreCase("size")) {
			aGroup = sizeGroup;
		} else if (key.equalsIgnoreCase("shape")) {
			aGroup = shapeGroup;
		} else if (key.equalsIgnoreCase("lithology")) {
			aGroup = lithologyGroup;
		} else if (key.equalsIgnoreCase("grainsize")) {
			aGroup = grainSizeGroup;
		} else {
			return "NA";
		}

		ButtonModel aModel = aGroup.getSelection();
		Enumeration e = aGroup.getElements();

		while (e.hasMoreElements()) {
			JToggleButton b = (JToggleButton) e.nextElement();
			if (aModel == b.getModel()) {
				value = b.getText();
				break;
			}
		}

		return value;
	}

	public String getValueFromKey(final String key) {
		return attribs.get(key);
	}

	private void onCancel() {
		dispose();
	}

	public void onSave() {
		collectViewInfo();

		System.out.println("---> Creating annotation in (trackId, sectionId) " + this.trackId + ", " + this.sectionId);

		// Create annotation content and save to an annotation(HTML) file
		SceneGraph.lock();
		{
			// ps. x_pos, y_pos are scenepos in GL context space
			float markerX;
			float markerY;

			if (markerId == -1) {
				markerX = (upperLeftPoint[0] + lowerRightPoint[0]) / 2.0f;
				markerY = (upperLeftPoint[1] + lowerRightPoint[1]) / 2.0f;

				markerId = SceneGraph.createCoreSectionMarker(trackId, sectionId, ChatGroup.CLAST, MarkerType.CORE_OUTLINE_MARKER, markerX, -markerY / 2);

				SceneGraph.setCoreSectionMarkerVertex(trackId, sectionId, markerId, markerX, -markerY / 2, this.upperLeftPoint[0], this.upperLeftPoint[1],
						this.lowerRightPoint[0], this.lowerRightPoint[1]);
			}

			if (markerId != -1) {
				// Summarize information collected and transform them into
				// clast annotations
				File aFile;

				String localFilePath = SceneGraph.getCoreSectionMarkerLocal(trackId, sectionId, markerId);

				// Check if newly add or editing
				if (localFilePath == null || localFilePath.equalsIgnoreCase("")) {
					aFile = new File(generateFilename());

					SceneGraph.setCoreSectionMarkerLocal(trackId, sectionId, markerId, aFile.getAbsolutePath());
				} else {
					aFile = new File(localFilePath);
				}

				// For DIS Export
				String preState = attribs.get("anno_state");
				if (preState == null || preState.equals("")) {
					attribs.put("anno_state", "added");
				} else if (preState.equalsIgnoreCase("unchanged")) {
					attribs.put("anno_state", "edited");
				}

				// For DIS Export
				attribs.put("anno_app", "CLR");
				attribs.put("anno_class", "CLR_clast");
				PropertyListUtility.saveHashtableToProperListFile(attribs, aFile);
			}
		}

		SceneGraph.unlock();
		CorelyzerApp.getApp().updateGLWindows();

		dispose();
	}

	private void resetViewStatus() {
		// Buttons
		Enumeration<String> keys = this.buttons.keys();
		while (keys.hasMoreElements()) {
			String aKey = keys.nextElement();
			AbstractButton b = buttons.get(aKey);
			b.setSelected(false);
		}

		// Text components
		for (Map.Entry<String, JTextComponent> entry : textCompos.entrySet()) {
			entry.getValue().setText("");
		}
	}

	public void setAttributes(final Hashtable<String, String> aDict) {
		attribs = aDict;

		// For DIS Export
		String annoClass = attribs.get("anno_class");
		if (annoClass == null || annoClass.equals("")) {
			attribs.put("anno_class", "CLR_clast");
		}

		syncAttribsToView();
	}

	public void setCoreName(final String coreName) {
		this.coreNameField.setText(coreName);
	}

	public void setMarkerId(final int markerId) {
		this.markerId = markerId;
	}

	public void setName(final String tName, final String cName) {
		setTrackName(tName);
		setCoreName(cName);
	}

	// (ulx, uly), (lrx, lry) are in physical absolute coordinates
	// This dialog displays points's location relative to the start (upperleft)
	// of this core section
	public void setRange(float ulx, float uly, float lrx, float lry) {
		super.setRange(ulx, uly, lrx, lry);

		// TODO: fill in common ui components
		float scale = SceneGraph.getCanvasDPIX(0) / 2.54f;

		ulx = this.upperLeftPoint[0] / scale;
		uly = this.upperLeftPoint[1] / scale;
		lrx = this.lowerRightPoint[0] / scale;
		lry = this.lowerRightPoint[1] / scale;

		// in cm
		this.upperLeftPositionField.setText(String.format("%.3f", ulx) + ", " + String.format("%.3f", uly));
		this.lowerRightPositionField.setText(String.format("%.3f", lrx) + ", " + String.format("%.3f", lry));

		// update width & height
		float width = Math.abs(ulx - lrx);
		float height = Math.abs(uly - lry);
		this.widthField.setText(String.format("%.3f", width));
		this.heightField.setText(String.format("%.3f", height));

		// Help pre-select size buttongroup
		// float size = (width > height) ? width : height;
		double size = Math.sqrt(width * width + height * height);

		if (size >= 0.02 && size <= 0.04) {
			this.granuleButton.setSelected(true);
		} else if (size > 0.04 && size <= 0.64) {
			this.pebbleButton.setSelected(true);
		} else if (size > 0.64 && size <= 2.56) {
			this.cobbleButton.setSelected(true);
		} else if (size > 2.56) {
			this.boulderButton.setSelected(true);
		}
	}

	public void setRange(final Point ul, final Point lr) {
		setRange(ul.x, ul.y, lr.x, lr.y);
	}

	public void setSectionId(final int sectionId) {
		this.sectionId = sectionId;
	}

	public void setTrackId(final int trackId) {
		this.trackId = trackId;
	}

	public void setTrackName(final String trackName) {
		this.trackNameField.setText(trackName);
	}

	public void setValueForKey(final String key, final String value) {
		super.setValueForKey(key, value);
		syncAttribsToView();
	}

	public void setVisible(final boolean b) {
		this.textCompos.get("sessionname").setText(this.getValueFromKey("sessionname"));
		this.textCompos.get("trackname").setText(this.getValueFromKey("trackname"));
		this.textCompos.get("corename").setText(this.getValueFromKey("corename"));

		super.setVisible(b);
	}

	private void syncAttribsToView() {
		resetViewStatus();

		Enumeration<String> e = attribs.keys();
		while (e.hasMoreElements()) {
			String aKey = e.nextElement();

			if (buttonGroupSet.contains(aKey)) { // Belongs to ButtonGroup
				String value = attribs.get(aKey);

				AbstractButton button = buttons.get(value);
				if (button != null) {
					button.setSelected(true);
				}
			} else { // JTextComponents
				JTextComponent textCompo = textCompos.get(aKey);
				String value = this.attribs.get(aKey);

				if (textCompo != null) {
					textCompo.setText(value);
				}
			}
		}

		String mode = attribs.get("mode");
		if (mode != null) {
			if (mode.equalsIgnoreCase("multiple")) {
				this.singleOrCounted.setSelectedIndex(1);

				// this.singleOrCounted.setEnabledAt(1, true);
				// this.singleOrCounted.setEnabledAt(0, false);
			} else {
				this.singleOrCounted.setSelectedIndex(0);

				// this.singleOrCounted.setEnabledAt(0, true);
				// this.singleOrCounted.setEnabledAt(1, false);
			}
		}
	}
}
