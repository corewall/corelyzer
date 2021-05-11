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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;

import javax.swing.border.*;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import corelyzer.controller.CRExperimentController;
import corelyzer.data.CoreSection;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;

public class SectionImagePropertyDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2265058018882176219L;

	public static void main(final String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		SectionImagePropertyDialog dialog = new SectionImagePropertyDialog(null);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		System.exit(0);
	}

	private JPanel contentPane;
	private JButton okButton;
	private JButton cancelButton;
	private JTextArea miscInfoField;
	private JScrollPane miscInfoScrollPane;
	private boolean showMiscInfo = false;
	private JButton showHideMiscButton;
	private JLabel dpiXLabel;
	private JTextField dpiXField;
	private JLabel dpiYLabel;
	private JTextField dpiYField;
	private JTextField rotationField;
	private JButton applyButton;
	private JLabel lengthLabel;
	private JTextField lengthField;
	private JLabel widthLabel;
	private JTextField widthField;
	private JLabel orientationLabel;
	private JComboBox<String> orientationBox;
	private JPanel visIntervalPanel;
	private JTextField topDepthField;
	private JTextField intervalTopField;
	private JSlider topSlider;
	private JTextField intervalBottomField;
	private JSlider bottomSlider;
	private JButton helpButton;
	private JButton degreeDecButton;
	private JButton degreeIncButton;
	private JTextField incField;
	private JRadioButton useMeasureButton;
	private JRadioButton useDPIButton;
	private JComboBox<String> applyTargetOption;
	private JButton nextButton;
	private JButton previousButton;
	private JLabel bottomVisLabel;
	private JLabel topDepthLabel;
	private JLabel topVisLabel;
	
	// current active
	int trackId, sectionId;
	int sectionIndex = 0;

	int[] allSectionIds; // all
	float pre_dpix;
	float pre_dpiy;
	float pre_rotation;
	float pre_top;
	float pre_bottom;

	int pre_orientation;

	int last_orientationIndex = 0;

	float imageWidth, imageHeight;
	// Apply target indices
	final static private int APPLY_TARGET_SECTION = 0;

	final static private int APPLY_TARGET_SELECTED = 1;

	{
		setupMigUI();		
	}

	public SectionImagePropertyDialog(final Component f) {
		super();
		setAlwaysOnTop(true);
		setLocationRelativeTo(f);
		setTitle("Image Section Properties");

		setContentPane(contentPane);	
		setModal(true);

		getRootPane().setDefaultButton(this.applyButton);

		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onOK();
			}
		});

		cancelButton.addActionListener(new ActionListener() {

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
		applyButton.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				onApply();
			}
		});

		final JDialog parent = this;
		rotationField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(final KeyEvent event) {
				super.keyTyped(event);

				if (event.getKeyChar() == KeyEvent.VK_ENTER) {
					float value = 0.0f;
					try {
						value = Float.parseFloat(rotationField.getText());
					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(parent, "Number Format Error!");

						rotationField.setText("" + 0.0f);
					}

					// call to native
					if (CorelyzerApp.getApp() != null) {
						SceneGraph.rotateSection(trackId, sectionId, value);

						CorelyzerApp.getApp().updateGLWindows();
					}
				}
			}
		});

		orientationBox.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent event) {
				// only flip if orientation changed
				if (last_orientationIndex != orientationBox.getSelectedIndex()) {
					// swap length & width
					String t = lengthField.getText();
					lengthField.setText(widthField.getText());
					widthField.setText(t);

					// swap dpi values
					t = dpiXField.getText();
					dpiXField.setText(dpiYField.getText());
					dpiYField.setText(t);

					// update slider max
					int max = (int) Math.ceil(10 * Float.parseFloat(lengthField.getText()));
					topSlider.setMaximum(max);
					bottomSlider.setMaximum(max);

					if (CorelyzerApp.getApp() == null) {
						return;
					}

					boolean isPortrait = orientationBox.getSelectedIndex() == 1;
					SceneGraph.setSectionOrientation(trackId, sectionId, isPortrait);

					last_orientationIndex = orientationBox.getSelectedIndex();
					CorelyzerApp.getApp().updateGLWindows();
				}
			}
		});

		topSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent event) {
				int value = topSlider.getValue();
				float realValue = value / 10.0f;
				intervalTopField.setText(String.valueOf(realValue));
				bottomSlider.setMinimum(value + 1);

				SceneGraph.setSectionIntervalTop(trackId, sectionId, realValue);
				CorelyzerApp.getApp().updateGLWindows();
			}
		});

		bottomSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent event) {
				int value = bottomSlider.getValue();
				float realValue = value / 10.0f;
				intervalBottomField.setText(String.valueOf(realValue));
				topSlider.setMaximum(value - 1);

				SceneGraph.setSectionIntervalBottom(trackId, sectionId, realValue);

				CorelyzerApp.getApp().updateGLWindows();
			}
		});

		degreeDecButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				float origValue = Float.parseFloat(rotationField.getText());
				float incVal = Float.parseFloat(incField.getText());

				rotationField.setText(String.valueOf(origValue - incVal));
				applyRotation();
			}
		});

		degreeIncButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				float origValue = Float.parseFloat(rotationField.getText());
				float incVal = Float.parseFloat(incField.getText());

				rotationField.setText(String.valueOf(origValue + incVal));
				applyRotation();
			}
		});

		useMeasureButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onChecked();
			}
		});

		useDPIButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onChecked();
			}
		});

		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onNext();
			}
		});
		previousButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onPrevious();
			}
		});
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onHelp();
			}
		});

		applyTargetOption.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				int targetIdx = applyTargetOption.getSelectedIndex();
				boolean isSingleSection = targetIdx == APPLY_TARGET_SECTION;

				// Enable/disable uncommon options
				topDepthField.setEnabled(isSingleSection);
				topDepthLabel.setEnabled(isSingleSection);

				visIntervalPanel.setEnabled(isSingleSection);
				intervalTopField.setEnabled(isSingleSection);
				topSlider.setEnabled(isSingleSection);
				topVisLabel.setEnabled(isSingleSection);

				intervalBottomField.setEnabled(isSingleSection);
				bottomSlider.setEnabled(isSingleSection);
				bottomVisLabel.setEnabled(isSingleSection);

				// Next and Previous button
				if (isSingleSection) {
					enableNextPrev();
				} else {
					nextButton.setEnabled(false);
					previousButton.setEnabled(false);
				}
			}
		});
	}

	// 2/16/2012 brg: Attempt to make better use of space in this dialog - still
	// clunkier than I'd like, but much easier to tweak with MigLayout than old
	// IntelliJ layout code.
	private void setupMigUI()
	{
		contentPane = new JPanel();

		// single column of panels
		contentPane.setLayout(new MigLayout("wrap 1, ins 5", "", "[]5[]"));
		
		// measure/DPI panel
		JPanel measureDPIPanel = new JPanel(new MigLayout("fillx, ins 5"));
		Border mdpBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Size");
		measureDPIPanel.setBorder(mdpBorder);
		
		useMeasureButton = new JRadioButton();
		useMeasureButton.setSelected(true);
		useMeasureButton.setText("Use Measure (cm)");
		
		lengthLabel = new JLabel("Length:");
		
		lengthField = new JTextField("1.0");
		lengthField.setHorizontalAlignment(JTextField.TRAILING);
		//lengthField.setText("1.0");
		
		widthLabel = new JLabel("Width:");
		
		widthField = new JTextField("1.0");
		widthField.setHorizontalAlignment(JTextField.TRAILING);
		
		useDPIButton = new JRadioButton();
		useDPIButton.setText("Use DPI");

		// associate radio buttons
		ButtonGroup buttonGroup;
		buttonGroup = new ButtonGroup();
		buttonGroup.add(useMeasureButton);
		buttonGroup.add(useDPIButton);
		
		dpiXLabel = new JLabel("DPI X:");
		dpiXLabel.setEnabled(false);

		dpiXField = new JTextField("254.0");
		dpiXField.setEnabled(false);
		dpiXField.setHorizontalAlignment(JTextField.TRAILING);
		
		dpiYLabel = new JLabel("DPI Y:");
		dpiYLabel.setEnabled(false);

		dpiYField = new JTextField("254.0");
		dpiYField.setEnabled(false);
		dpiYField.setHorizontalAlignment(JTextField.TRAILING);
		
		measureDPIPanel.add(useMeasureButton);
		measureDPIPanel.add(lengthLabel, "align right");
		measureDPIPanel.add(lengthField, "growx");
		measureDPIPanel.add(widthLabel, "align right");
		measureDPIPanel.add(widthField, "growx, wrap");
		
		measureDPIPanel.add(useDPIButton);
		measureDPIPanel.add(dpiXLabel, "align right");
		measureDPIPanel.add(dpiXField, "growx");
		measureDPIPanel.add(dpiYLabel, "align right");
		measureDPIPanel.add(dpiYField, "growx");

		// top depth, original orientation
		JPanel depthOrientationPanel = new JPanel(new MigLayout("fillx, ins 5"));
		Border dopBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Position");
		depthOrientationPanel.setBorder(dopBorder);	
		
		orientationLabel = new JLabel("Original Orientation:");
		orientationBox = new JComboBox<String>();
		final String orntTip = new String("Orientation of source image file");
		orientationLabel.setToolTipText(orntTip);
		orientationBox.setToolTipText(orntTip);
		final DefaultComboBoxModel<String> orientationBoxModel = new DefaultComboBoxModel<String>();
		orientationBoxModel.addElement("Horizontal");
		orientationBoxModel.addElement("Vertical");
		orientationBox.setModel(orientationBoxModel);
		
		topDepthField = new JTextField("0.0");
		topDepthField.setHorizontalAlignment(JTextField.TRAILING);
		topDepthLabel = new JLabel("Top Depth (m):");
		final String topDepthTip = new String("Meters Below Sea Floor");
		topDepthLabel.setToolTipText(topDepthTip);
		topDepthField.setToolTipText(topDepthTip);

		depthOrientationPanel.add(orientationLabel);
		depthOrientationPanel.add(orientationBox);
		depthOrientationPanel.add(topDepthLabel, "gapleft 10");
		depthOrientationPanel.add(topDepthField, "wmin 50px, growx");
		
		// visible interval top/bottom fields, sliders
		visIntervalPanel = new JPanel(new MigLayout("fillx, ins 5"));
		Border vipBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Visible Interval (cm)");
		visIntervalPanel.setBorder(vipBorder);
		
		topVisLabel = new JLabel("Begin:");

		intervalTopField = new JTextField("0.0");
		intervalTopField.setHorizontalAlignment(JTextField.TRAILING);

		topSlider = new JSlider();
		topSlider.setMaximum(499);
		topSlider.setValue(0);
		
		bottomVisLabel = new JLabel("End:");

		intervalBottomField = new JTextField("100");
		intervalBottomField.setHorizontalAlignment(JTextField.TRAILING);

		bottomSlider = new JSlider();
		bottomSlider.setMaximum(999);
		bottomSlider.setMinimum(500);
		bottomSlider.setValue(999);
		
		visIntervalPanel.add(topVisLabel, "align right");
		visIntervalPanel.add(intervalTopField, "growx, wmin 60px");
		visIntervalPanel.add(topSlider, "wrap");
		visIntervalPanel.add(bottomVisLabel, "align right");
		visIntervalPanel.add(intervalBottomField, "growx, wmin 60px");
		visIntervalPanel.add(bottomSlider);
		
		// rotation, trim, apply popup and button
		JPanel appearancePanel = new JPanel(new MigLayout("fillx, ins 5"));
		Border rtpBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Appearance");
		appearancePanel.setBorder(rtpBorder);

		rotationField = new JTextField();
		rotationField.setHorizontalAlignment(JTextField.TRAILING);
		rotationField.setText("0.0");
		ImageIcon decIcon = new ImageIcon(getClass().getResource("/corelyzer/ui/resources/decrement.gif"));
		degreeDecButton = new JButton(decIcon);
		degreeDecButton.setToolTipText("Decrease Rotation");

		incField = new JTextField("2.0");
		incField.setToolTipText("Rotation Increment");

		ImageIcon incIcon = new ImageIcon(getClass().getResource("/corelyzer/ui/resources/increment.gif"));
		degreeIncButton = new JButton(incIcon);
		degreeIncButton.setToolTipText("Increase Rotation");
		
		applyTargetOption = new JComboBox<String>();
		applyTargetOption.setEnabled(true);
		final DefaultComboBoxModel<String> applyTargetModel = new DefaultComboBoxModel<String>();
		applyTargetModel.addElement("This section only");
		applyTargetModel.addElement("All selected sections");
		applyTargetOption.setModel(applyTargetModel);
		
		applyButton = new JButton();
		applyButton.setText("Apply");
		
		JPanel rotPanel = new JPanel(new MigLayout("fillx, ins 5"));
		Border rpBorder = BorderFactory.createEtchedBorder();
		rotPanel.setBorder(rpBorder);
		
		rotPanel.add(new JLabel("Rotation:"), "align right");
		rotPanel.add(degreeDecButton, "wmax 20");
		rotPanel.add(rotationField, "wmin 50, growx");
		rotPanel.add(degreeIncButton, "wmax 20, wrap");
		rotPanel.add(new JLabel("Increment:"), "align right");
		rotPanel.add(incField, "growx, span 3");
		
		appearancePanel.add(visIntervalPanel, "growx");
		appearancePanel.add(rotPanel);
		
		// apply popup, button
		JPanel applyPanel = new JPanel(new MigLayout("", "[]rel[grow][]"));
		applyPanel.add( new JLabel("Apply changes to:") );
		applyPanel.add(applyTargetOption);
		applyPanel.add(applyButton);

		// help prev next cancel ok buttons
		JPanel buttonPanel = new JPanel(new MigLayout("ins 5", "[][][grow][][]"));

		helpButton = new JButton("Help");
		helpButton.setEnabled(true);

		cancelButton = new JButton("Cancel");

		okButton = new JButton("OK");
		okButton.setText("OK");

		nextButton = new JButton("Next");
		nextButton.setEnabled(false);

		previousButton = new JButton("Previous");
		previousButton.setEnabled(false);
		
		buttonPanel.add(helpButton);
		buttonPanel.add(previousButton);
		buttonPanel.add(nextButton);
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
				
		final JPanel miscInfoPanel = new JPanel(new MigLayout("fillx, wrap 1"));
		showHideMiscButton = new JButton("Show Additional Section Info");
		showHideMiscButton.addActionListener( new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				showMiscInfo = !showMiscInfo;
				if ( showMiscInfo )
					miscInfoPanel.add(miscInfoScrollPane);
				else
					miscInfoPanel.remove(miscInfoScrollPane);
				
				miscInfoPanel.revalidate();
				updateForMiscInfo();
			}
		});
		
		miscInfoPanel.add(showHideMiscButton);
		miscInfoScrollPane = new JScrollPane();
		miscInfoField = new JTextArea(10, 50);
		miscInfoScrollPane.setViewportView(miscInfoField);
		
		contentPane.add(measureDPIPanel, "growx");
		contentPane.add(depthOrientationPanel, "growx");
		contentPane.add(appearancePanel);
		contentPane.add(applyPanel, "growx");
		contentPane.add(miscInfoPanel, "growx");
		contentPane.add(buttonPanel, "growx");
	}
	
	private void updateForMiscInfo()
	{
		Dimension cur = this.getSize();
		final int sizeOffset = showMiscInfo ? 150 : -150;
		this.setSize(cur.width, cur.height + sizeOffset);

		final String showHide = !showMiscInfo ? "Show" : "Hide";
		showHideMiscButton.setText(showHide + " Additional Section Info");
	}

	private void applyRotation() {
		float rotation;
		try {
			rotation = Float.parseFloat(this.rotationField.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Invalid Rotation inputs");
			e.printStackTrace();

			return;
		}

		// call to native
		if (CorelyzerApp.getApp() != null) {
			SceneGraph.rotateSection(trackId, sectionId, rotation);

			CorelyzerApp.getApp().updateGLWindows();
		}
	}

	private void applyToASection() {
		if (CorelyzerApp.getApp() == null) {
			return;
		}

		float depth;
		try {
			depth = Float.parseFloat(this.topDepthField.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Invalid Depth Input");
			e.printStackTrace();

			return;
		}

		float rotation;
		try {
			rotation = Float.parseFloat(this.rotationField.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Invalid Rotation input");
			e.printStackTrace();

			return;
		}

		boolean isPortrait = this.orientationBox.getSelectedIndex() == 1;

		float dpix;
		float dpiy;

		if (this.useDPIButton.isSelected()) { // use DPI directly
			try {
				dpix = Float.parseFloat(dpiXField.getText());
				dpiy = Float.parseFloat(dpiYField.getText());

				float l, w;
				if (isPortrait) {
					l = 2.54f * this.imageHeight / dpix;
					w = 2.54f * this.imageWidth / dpiy;
				} else {
					l = 2.54f * this.imageWidth / dpix;
					w = 2.54f * this.imageHeight / dpiy;
				}

				this.lengthField.setText("" + l);
				this.widthField.setText("" + w);

				// update visible interval bottom max
				int maxValue = (int) Math.ceil(l * 10);
				this.topSlider.setMaximum(maxValue);
				this.bottomSlider.setMaximum(maxValue);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Invalid DPI inputs");
				e.printStackTrace();

				return;
			}
		} else { // use physical length and width
			try {
				float length = Float.parseFloat(this.lengthField.getText());
				float width = Float.parseFloat(this.widthField.getText());

				if (isPortrait) {
					dpix = this.imageHeight / (length / 2.54f);
					dpiy = this.imageWidth / (width / 2.54f);
				} else {
					dpix = this.imageWidth / (length / 2.54f);
					dpiy = this.imageHeight / (width / 2.54f);
				}

				this.dpiXField.setText("" + dpix);
				this.dpiYField.setText("" + dpiy);

				// update visible interval bottom max
				// float label_l =
				// Float.parseFloat(intervalBottomField.getText());
				// if (label_l != pre_bottom) {
				int maxValue = (int) Math.ceil(length * 10);
				this.topSlider.setMaximum(maxValue);
				this.bottomSlider.setMaximum(maxValue);
				// }
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Invalid Length/Width");
				e.printStackTrace();

				return;
			}
		}

		if (isPortrait) {
			SceneGraph.setSectionDPI(this.trackId, this.sectionId, dpix, dpiy);
		} else {
			SceneGraph.setSectionDPI(this.trackId, this.sectionId, dpiy, dpix);
		}

		SceneGraph.setSectionOrientation(trackId, sectionId, isPortrait);
		SceneGraph.rotateSection(this.trackId, this.sectionId, rotation);

		float intervalTop = Float.valueOf(this.intervalTopField.getText());
		float intervalBottom = Float.valueOf(intervalBottomField.getText());

		// swap top and bottom
		if (intervalTop > intervalBottom) {
			float tmp = intervalTop;
			intervalTop = intervalBottom;
			intervalBottom = tmp;

			// update view
			this.topSlider.setValue((int) Math.floor(intervalTop * 10));
			this.bottomSlider.setValue((int) Math.ceil(intervalBottom * 10));
		}

		SceneGraph.setSectionIntervalTop(trackId, sectionId, intervalTop);
		SceneGraph.setSectionIntervalBottom(trackId, sectionId, intervalBottom);

		// depth: taking into account the intervalTop
		float depthInPx = (depth * 100.0f - intervalTop) / 2.54f * SceneGraph.getCanvasDPIX(0);
		float yPosInPx = SceneGraph.getSectionYPos(trackId, sectionId);
		SceneGraph.positionSection(trackId, sectionId, depthInPx, yPosInPx);

		CorelyzerApp.getApp().updateGLWindows();
	}

	// Apply part of specified options to multiple sections

	private void applyToSections() {
		if (CorelyzerApp.getApp() == null) {
			return;
		}

		float rotation;
		try {
			rotation = Float.parseFloat(this.rotationField.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Invalid Rotation input");
			e.printStackTrace();

			return;
		}

		boolean isPortrait = this.orientationBox.getSelectedIndex() == 1;

		float dpix;
		float dpiy;

		if (this.useDPIButton.isSelected()) { // use DPI directly
			try {
				dpix = Float.parseFloat(dpiXField.getText());
				dpiy = Float.parseFloat(dpiYField.getText());

				float l, w;
				if (isPortrait) {
					l = 2.54f * this.imageHeight / dpix;
					w = 2.54f * this.imageWidth / dpiy;
				} else {
					l = 2.54f * this.imageWidth / dpix;
					w = 2.54f * this.imageHeight / dpiy;
				}

				this.lengthField.setText("" + l);
				this.widthField.setText("" + w);

				// update visible interval bottom max
				int maxValue = (int) Math.ceil(l * 10);
				this.topSlider.setMaximum(maxValue);
				this.bottomSlider.setMaximum(maxValue);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Invalid DPI inputs");
				e.printStackTrace();

				return;
			}
		} else { // use physical length and width
			try {
				float length = Float.parseFloat(this.lengthField.getText());
				float width = Float.parseFloat(this.widthField.getText());

				if (isPortrait) {
					dpix = this.imageHeight / (length / 2.54f);
					dpiy = this.imageWidth / (width / 2.54f);
				} else {
					dpix = this.imageWidth / (length / 2.54f);
					dpiy = this.imageHeight / (width / 2.54f);
				}

				this.dpiXField.setText("" + dpix);
				this.dpiYField.setText("" + dpiy);

				// update visible interval bottom max
				int maxValue = (int) Math.ceil(length * 10);
				this.topSlider.setMaximum(maxValue);
				this.bottomSlider.setMaximum(maxValue);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Invalid Length/Width");
				e.printStackTrace();

				return;
			}
		}

		// Looping depends on apply target selection
		switch (this.applyTargetOption.getSelectedIndex()) {
			case APPLY_TARGET_SECTION:
				System.out.println("Apply to single target, shouldn't happen here...");
				break;

			case APPLY_TARGET_SELECTED:
				for (int allSectionId : this.allSectionIds) {
					SceneGraph.setSectionDPI(this.trackId, allSectionId, dpix, dpiy);
					SceneGraph.setSectionOrientation(trackId, allSectionId, isPortrait);
					SceneGraph.rotateSection(this.trackId, allSectionId, rotation);

					// Set full length visible
					float fullLength = 1000.0f;
					SceneGraph.setSectionIntervalTop(trackId, allSectionId, 0);
					SceneGraph.setSectionIntervalBottom(trackId, allSectionId, fullLength);

					CorelyzerApp.getApp().updateGLWindows();
				}

				break;

			default: // only current section
				applyToASection();
		}
	}

	private String generateMetaString(final int tid, final int csid) {
		CoreGraph cg = CoreGraph.getInstance();
		CoreSection cs = cg.getSection(cg.getCurrentSessionIdx(), tid, csid);

		if (cs == null) {
			System.err.println("---> [WARN] CoreSection is null in generateMetaString(): trackId: " + tid + ", csid: " + csid);

			return "Empty string, because of null CoreSection";
		}

		this.setTitle(cs.getName());

		int imageId = SceneGraph.getImageIdForSection(tid, csid);

		int csImgId = -1;
		if (cs.hasImage()) {
			csImgId = cs.getCoreSectionImage().getId();
		}

		float rotationAngle = SceneGraph.getSectionRotation(tid, csid);
		String label = cs.getName() + "\nTrackId: " + tid + " CoreSectionId:" + csid + " CoreSectionImageId: " + csImgId + " nativeImageID: " + imageId;

		label += "\nLocal: " + SceneGraph.getImageName(imageId);
		label += "\nURL: " + SceneGraph.getImageURL(imageId);
		label += "\nRotation: " + rotationAngle;
		label += "\nDPI: " + SceneGraph.getSectionDPIX(tid, cs.getId());
		label += ", " + SceneGraph.getSectionDPIY(tid, cs.getId());
		label += "\nVisibleTop: " + SceneGraph.getSectionIntervalTop(tid, csid);
		label += "\nVisibleBottom: " + SceneGraph.getSectionIntervalBottom(tid, csid);

		label += "\nParentTrackId: " + SceneGraph.getSectionParentTrackId(tid, csid);
		label += "\nParentSectionId: " + SceneGraph.getSectionParentSectionId(tid, csid);
		label += "\nName: '" + SceneGraph.getSectionName(tid, csid) + "'";

		return label;
	}

	private void onApply() {
		int targetIdx = this.applyTargetOption.getSelectedIndex();

		if (targetIdx != APPLY_TARGET_SECTION) {
			applyToSections();
		} else {
			applyToASection();
		}
	}

	private void onCancel() {
		this.useDPIButton.setSelected(true);
		this.dpiXField.setText(String.valueOf(this.pre_dpix));
		this.dpiYField.setText(String.valueOf(this.pre_dpiy));
		this.orientationBox.setSelectedIndex(this.pre_orientation);
		this.rotationField.setText(String.valueOf(this.pre_rotation));
		this.intervalTopField.setText(String.valueOf(this.pre_top));
		this.intervalBottomField.setText(String.valueOf(this.pre_bottom));

		onApply();
		dispose();
	}

	private void onChecked() {
		boolean useMeasure = this.useMeasureButton.isSelected();
		boolean useDPI = this.useDPIButton.isSelected();

		dpiXLabel.setEnabled(useDPI);
		dpiXField.setEnabled(useDPI);
		dpiYLabel.setEnabled(useDPI);
		dpiYField.setEnabled(useDPI);

		lengthLabel.setEnabled(useMeasure);
		lengthField.setEnabled(useMeasure);
		widthLabel.setEnabled(useMeasure);
		widthField.setEnabled(useMeasure);
	}

	private void onHelp() {
		WikiHelpDialog dialog = new WikiHelpDialog(this, "CRSectionPropertiesDialog");
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	private void onNext() {
		this.sectionIndex++;

		if (this.sectionIndex < this.allSectionIds.length) { // first
			this.sectionId = this.allSectionIds[sectionIndex];

			enableNextPrev();
			
			String label = this.generateMetaString(trackId, sectionId);
			setProperties(trackId, sectionId, label);

			// center the section
			CRExperimentController.locateSection(this.trackId, this.sectionId);
		}
	}

	private void onOK() {
		onApply();
		dispose();
	}
	
	private void enableNextPrev() {
		if (this.sectionIndex == 0) { // the first
			this.nextButton.setEnabled(true);
			this.previousButton.setEnabled(false);
		} else if (this.sectionIndex == this.allSectionIds.length - 1) {// last
			this.nextButton.setEnabled(false);
			this.previousButton.setEnabled(true);
		} else {
			this.nextButton.setEnabled(true);
			this.previousButton.setEnabled(true);
		}
	}

	private void onPrevious() {
		this.sectionIndex--;

		if (this.sectionIndex >= 0) {
			this.sectionId = this.allSectionIds[sectionIndex];

			enableNextPrev();

			String label = this.generateMetaString(trackId, sectionId);
			setProperties(trackId, sectionId, label);

			// center the section
			CRExperimentController.locateSection(this.trackId, this.sectionId);
		}
	}

	public void setMultiProperties(final int trackId, final int[] sectionIds) {
		this.trackId = trackId;
		this.allSectionIds = sectionIds;

		if (sectionIds.length != 0) {
			this.nextButton.setEnabled((sectionIds.length > 1));

			this.sectionIndex = 0;
			this.sectionId = sectionIds[sectionIndex];
		}

		String label = this.generateMetaString(trackId, sectionId);
		setProperties(trackId, sectionId, label);
	}

	public void setProperties(final int trackId, final int sectionId) {
		String label = this.generateMetaString(trackId, sectionId);
		setProperties(trackId, sectionId, label);
	}

	public void setProperties(final int trackId, final int sectionId, final String label) {
		this.trackId = trackId;
		this.sectionId = sectionId;
		this.miscInfoField.setText(label);

		// original section properties
		float topDepth = SceneGraph.getSectionDepth(trackId, sectionId) / 100.0f;
		float rotation = SceneGraph.getSectionRotation(trackId, sectionId);
		boolean orientation = SceneGraph.getSectionOrientation(trackId, sectionId);

		float dpix = SceneGraph.getSectionDPIX(trackId, sectionId);
		float dpiy = SceneGraph.getSectionDPIY(trackId, sectionId);

		// image properties
		int imageId = SceneGraph.getImageIdForSection(trackId, sectionId);
		this.imageHeight = SceneGraph.getImageHeight(imageId);
		this.imageWidth = SceneGraph.getImageWidth(imageId);

		// Interval
		float intervalTop = SceneGraph.getSectionIntervalTop(trackId, sectionId);
		float intervalBottom = SceneGraph.getSectionIntervalBottom(trackId, sectionId);

		// Orientation
		float length, width;
		if (orientation == SceneGraph.PORTRAIT) { // true: portrait
			this.orientationBox.setSelectedIndex(1);

			length = imageHeight / dpix * 2.54f;
			width = imageWidth / dpiy * 2.54f;
		} else { // false: landscape
			this.orientationBox.setSelectedIndex(0);

			length = imageWidth / dpiy * 2.54f;
			width = imageHeight / dpix * 2.54f;
		}

		this.dpiXField.setText("" + dpix);
		this.dpiYField.setText("" + dpiy);
		this.rotationField.setText("" + rotation);

		this.lengthField.setText("" + length);
		this.widthField.setText("" + width);

		// update slider view
		int maxValue = (int) Math.ceil(length * 10);
		int currentValue = intervalTop == length ? maxValue : (int) (intervalTop * 10);

		this.topSlider.setMinimum(0);
		this.topSlider.setMaximum(maxValue);
		this.topSlider.setValue(currentValue);
		this.intervalTopField.setText(String.valueOf(intervalTop));

		currentValue = (int) intervalBottom * 10;
		this.bottomSlider.setMinimum(0);
		this.bottomSlider.setMaximum(maxValue);
		this.bottomSlider.setValue(currentValue);
		this.intervalBottomField.setText(String.valueOf(intervalBottom));

		// Depth
		DecimalFormat df = new DecimalFormat("#.###");
		float dval = (topDepth + intervalTop / 100.0f);
		this.topDepthField.setText(df.format(dval));

		// Keep a copy of original numbers
		this.pre_dpix = dpix;
		this.pre_dpiy = dpiy;
		this.pre_rotation = rotation;
		this.pre_top = intervalTop;
		this.pre_bottom = intervalBottom;
		this.pre_orientation = orientation ? 1 : 0;

		onApply();
	}
}
