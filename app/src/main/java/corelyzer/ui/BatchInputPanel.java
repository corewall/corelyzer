package corelyzer.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.*;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import net.miginfocom.swing.MigLayout;

import corelyzer.data.ImagePropertyTable;

public class BatchInputPanel extends JPanel {
	
	private ImagePropertyTable imageTable; // table to which batch settings will be applied
	
	public JCheckBox useBatchInputCheckbox;
	public JLabel orientationLabel;
	public JComboBox orientationComboBox;
	public JLabel lengthLabel;
	public JTextField lengthField;
	public JLabel dpiXLabel;
	public JTextField dpiXField;
	public JLabel dpiYLabel;
	public JTextField dpiYField;
	public JLabel startDepthLabel;
	public JTextField startDepthField;
	public JLabel depthIncLabel;
	public JTextField depthIncField;
	public JButton applyToAllButton, applyToSelectedButton;
	
	public BatchInputPanel(final ImagePropertyTable imageTable)
	{
		super(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
		this.imageTable = imageTable;
		
		setupUI();
		
		useBatchInputCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onBatch();
			}
		});

		applyToAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onApply( true );
			}
		});
		
		applyToSelectedButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onApply( false );
			}
		});
		
		orientationComboBox.addActionListener( new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				final boolean enableLabel = ( orientationComboBox.getSelectedIndex() != 2 /* [Blank] */);
					orientationLabel.setEnabled( enableLabel );
			}
		});
		lengthField.getDocument().addDocumentListener( LabelEnablerFactory.create( lengthField, lengthLabel ));
		dpiXField.getDocument().addDocumentListener( LabelEnablerFactory.create( dpiXField, dpiXLabel ));
		dpiYField.getDocument().addDocumentListener( LabelEnablerFactory.create( dpiYField, dpiYLabel ));
		startDepthField.getDocument().addDocumentListener( LabelEnablerFactory.create( startDepthField, startDepthLabel ));
		depthIncField.getDocument().addDocumentListener( LabelEnablerFactory.create( depthIncField, depthIncLabel ));
	}

	private void setupUI()
	{
		JPanel batchInputPanel = new JPanel();
		batchInputPanel.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
		batchInputPanel.setEnabled(true);
		this.add(batchInputPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
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
		
		dpiYField = new JTextField();
		dpiYField.setEnabled(false);
		dpiYField.setHorizontalAlignment(11);
		
		batchInputPanel.add(dpiXField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
		
		batchInputPanel.add(dpiYField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
		
		startDepthField = new JTextField();
		startDepthField.setEnabled(false);
		startDepthField.setHorizontalAlignment(11);

		batchInputPanel.add(startDepthField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
		depthIncField = new JTextField();
		depthIncField.setEnabled(false);
		depthIncField.setHorizontalAlignment(11);

		batchInputPanel.add(depthIncField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
		final JPanel applyPanel = new JPanel();
		applyPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		batchInputPanel.add(applyPanel, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		applyToAllButton = new JButton("Apply to All Rows");
		applyToAllButton.setEnabled(false);
		applyPanel.add(applyToAllButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		applyToSelectedButton = new JButton("Apply to Selected Rows");
		applyToSelectedButton.setEnabled(false);
		applyPanel.add(applyToSelectedButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
//		final Spacer spacer4 = new Spacer();
//		applyPanel.add(spacer4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
//				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		orientationComboBox = new JComboBox();
		orientationComboBox.setEnabled(false);
		final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
		defaultComboBoxModel1.addElement("Horizontal");
		defaultComboBoxModel1.addElement("Vertical");
		defaultComboBoxModel1.addElement("[Blank]");
		orientationComboBox.setModel(defaultComboBoxModel1);
		orientationComboBox.setSelectedIndex( 2 ); // [Blank]
		
		batchInputPanel.add(orientationComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		lengthLabel = new JLabel("Length (meter): ");
		batchInputPanel.add(lengthLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		lengthField = new JTextField();
		lengthField.setEnabled(false);
		lengthField.setHorizontalAlignment(11);
		
		batchInputPanel.add(lengthField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		orientationLabel = new JLabel("Orientation: ");
		batchInputPanel.add(orientationLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(195, 16), null, 0, false));
		useBatchInputCheckbox = new JCheckBox();
		useBatchInputCheckbox.setText("Batch input: blank fields will not be applied");
		this.add(useBatchInputCheckbox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		
		onBatch();
	}
	
	private void applyGroupPropToTable( final boolean applyToAllRows ) {
		int[] applyRowIndices = null;
		if ( applyToAllRows )
		{
			applyRowIndices = new int[ imageTable.getRowCount() ];
			for ( int i = 0; i < applyRowIndices.length; i++ ) { applyRowIndices[i] = i; }
		}
		else
		{
			applyRowIndices = imageTable.getSelectedRows();
		}
		
		final int orientation = orientationComboBox.getSelectedIndex();
		if ( orientation < 2 )
			imageTable.applyOrientation( orientation, applyRowIndices );

		if (!dpiXField.getText().equals(""))
		{
			final int dpiX = Integer.valueOf(dpiXField.getText());
			imageTable.applyDPIX(dpiX, applyRowIndices );
		}
		
		if (!dpiYField.getText().equals(""))
		{
			final int dpiY = Integer.valueOf(dpiYField.getText());
			imageTable.applyDPIY(dpiY, applyRowIndices);
		}

		if (!lengthField.getText().equals(""))
		{
			final float length = Float.valueOf(lengthField.getText());
			imageTable.applyLength(length, applyRowIndices);
		}
		
		// depth increment and start depth: both must be populated to apply
		if (!depthIncField.getText().equals("") && !startDepthField.getText().equals(""))
		{
			final float depthInc = Float.valueOf(depthIncField.getText());
			final float depthStart = Float.valueOf(startDepthField.getText());
			imageTable.applyDepths(depthStart, depthInc, applyRowIndices);
		}
		else if (!depthIncField.getText().equals("") || !startDepthField.getText().equals(""))
		{
			JOptionPane.showMessageDialog(this, "Both Start Depth and Depth Increment must be populated to apply values in Depth column");
		}
	}
	

	private void onApply( boolean applyToAllRows ) {
		try {
			applyGroupPropToTable( applyToAllRows );
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Invalid value: " + e.getMessage());
		}
	}
	
	private void onBatch() {
		final boolean b = useBatchInputCheckbox.isSelected();

		orientationComboBox.setEnabled(b);
		lengthField.setEnabled(b);
		dpiXField.setEnabled(b);
		dpiYField.setEnabled(b);
		startDepthField.setEnabled(b);
		depthIncField.setEnabled(b);
		applyToAllButton.setEnabled(b);
		applyToSelectedButton.setEnabled(b);

		if (!b)
		{
			orientationLabel.setEnabled(b);
			lengthLabel.setEnabled(b);
			dpiXLabel.setEnabled(b);
			dpiYLabel.setEnabled(b);
			startDepthLabel.setEnabled(b);
			depthIncLabel.setEnabled(b);
		}
		else
		{
			orientationLabel.setEnabled( orientationComboBox.getSelectedIndex() != 2 ); // [Blank]
			lengthLabel.setEnabled(!lengthField.getText().equals(""));
			dpiXLabel.setEnabled(!dpiXField.getText().equals(""));
			dpiYLabel.setEnabled(!dpiYField.getText().equals(""));
			startDepthLabel.setEnabled(!startDepthField.getText().equals(""));
			depthIncLabel.setEnabled(!depthIncField.getText().equals(""));
		}
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
	
	// 5/6/2012 brg: TODO Mig-ify!
//	private void setupMigUI()
//	{
//		JPanel checkboxPanel = new JPanel();
//		useBatchInputCheckbox = new JCheckBox();
//		useBatchInputCheckbox.setText("Batch input: blank fields will not be applied");
//		checkboxPanel.add(useBatchInputCheckbox);
//		
//		this.add(checkboxPanel);
//		
//		JPanel fieldsPanel = new JPanel( new MigLayout( "wrap 2" ));
//
//		fieldsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
//
//		orientationComboBox = new JComboBox();
//		orientationComboBox.setEnabled(false);
//		final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
//		defaultComboBoxModel1.addElement("Horizontal");
//		defaultComboBoxModel1.addElement("Vertical");
//		defaultComboBoxModel1.addElement("[Blank]");
//		orientationComboBox.setModel(defaultComboBoxModel1);
//		orientationLabel = new JLabel("Orientation: ");
//		fieldsPanel.add(orientationLabel, "split 2");
//		fieldsPanel.add(orientationComboBox);
//
//		lengthLabel = new JLabel("Length (meter): ");
//		fieldsPanel.add(lengthLabel, "split 2");
//		lengthField = new JTextField();
//		lengthField.setEnabled(false);
//		lengthField.setHorizontalAlignment(11);
//		lengthField.setText("1.0");
//		fieldsPanel.add(lengthField);
//		
//		dpiXLabel = new JLabel("DPI X: ");
//		fieldsPanel.add(dpiXLabel, "split 2");
//		dpiXField = new JTextField();
//		dpiXField.setEnabled(false);
//		dpiXField.setHorizontalAlignment(11);
//		dpiXField.setText("254");
//		fieldsPanel.add(dpiXField, "growx");
//
//		dpiYLabel = new JLabel("DPI Y: ");
//		fieldsPanel.add(dpiYLabel, "split 2");
//		dpiYField = new JTextField();
//		dpiYField.setEnabled(false);
//		dpiYField.setHorizontalAlignment(11);
//		dpiYField.setText("254");
//		fieldsPanel.add(dpiYField, "growx");
//		
//		startDepthLabel = new JLabel("Start Depth (meter): ");
//		fieldsPanel.add(startDepthLabel);
//		startDepthField = new JTextField();
//		startDepthField.setEnabled(false);
//		startDepthField.setHorizontalAlignment(11);
//		startDepthField.setText("0.0");
//		fieldsPanel.add(startDepthField);
//
//		depthIncLabel = new JLabel("Depth Increment (meter): ");
//		fieldsPanel.add(depthIncLabel);
//		depthIncField = new JTextField();
//		depthIncField.setEnabled(false);
//		depthIncField.setHorizontalAlignment(11);
//		depthIncField.setText("1.5");
//		fieldsPanel.add(depthIncField);
//
//
//		applyButton = new JButton();
//		applyButton.setEnabled(false);
//		applyButton.setText("Apply");
//		fieldsPanel.add(applyButton);
//		
//		this.add(fieldsPanel);
//		
//		onBatch();
//	}
}
