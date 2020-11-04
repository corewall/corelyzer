package corelyzer.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.Vector;

import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.*;
import corelyzer.graphics.SceneGraph;

import net.miginfocom.swing.MigLayout;

public class TrimDialog extends JDialog
{
	public static final long serialVersionUID = 1L;
	
	public static void main(final String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		TrimDialog dialog = new TrimDialog(null, 0, 0);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		System.exit(0);
	}
	
	private JPanel contentPane;
	private JComboBox<String> trimTypeBox;
	private JTextField trimField;
	private JLabel fromToLabel;
	private JComboBox<String> beginEndBox;
	private JComboBox<String> affectedSectionsBox;
	private JButton applyButton, closeButton;
	private int selectedTrack = -1, selectedTrackSection = -1;
	
	public TrimDialog(final Component parent, final int selectedTrack, final int selectedTrackSection)
	{
		super();
		
		this.selectedTrack = selectedTrack;
		this.selectedTrackSection = selectedTrackSection;
		
		initUI();
		
		setAlwaysOnTop(true);
		setLocationRelativeTo(parent);
		setTitle("Trim Sections");

		setContentPane(contentPane);	
		setModal(true);
	}

	private void initUI()
	{
		contentPane = new JPanel(new MigLayout());
		
		trimTypeBox = new JComboBox<String>();
		final DefaultComboBoxModel<String> trimTypeModel = new DefaultComboBoxModel<String>();
		trimTypeModel.addElement("Trim");
		trimTypeModel.addElement("Add");
		trimTypeBox.setModel(trimTypeModel);
		trimTypeBox.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) { updateFromToLabel(); }
		});

		trimField = new JTextField();

		fromToLabel = new JLabel("cm from");
		
		beginEndBox = new JComboBox<String>();
		final DefaultComboBoxModel<String> beginEndModel = new DefaultComboBoxModel<String>();
		beginEndModel.addElement("bottom of");
		beginEndModel.addElement("top of");
		beginEndBox.setModel(beginEndModel);
		beginEndBox.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { updateTrimField();	}
		});
		
		affectedSectionsBox = new JComboBox<String>();
		final DefaultComboBoxModel<String> affectedSectionsModel = new DefaultComboBoxModel<String>();
		affectedSectionsModel.addElement("selected section only");
                affectedSectionsModel.addElement("selected and deeper sections");
		affectedSectionsBox.setModel(affectedSectionsModel);
		
		closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { dispose(); }
		});
		
		applyButton = new JButton("Apply");
		applyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { onApply(); }
		});

		contentPane.add(trimTypeBox);
		contentPane.add(trimField, "wmin 50");
		contentPane.add(fromToLabel);
		contentPane.add(beginEndBox);
		contentPane.add(affectedSectionsBox, "wrap");
	
		contentPane.add(applyButton, "span, split 2, align right");
		contentPane.add(closeButton, "align right");
		
		updateTrimField();
	}
	
	private void updateTrimField()
	{
		final boolean getBottomTrim = (beginEndBox.getSelectedIndex() == 0);
		final float trimValue = getCurrentTrim(getBottomTrim);
		if (trimValue != -1.0f)
			trimField.setText(Float.toString(trimValue));
		else
			trimField.setText("0");
	}
	
	private void updateFromToLabel()
	{
		if ( trimTypeBox.getSelectedIndex() == 0 )
			fromToLabel.setText("cm from");
		else
			fromToLabel.setText("cm to");
	}
	
	// If trim values don't match across selected sections, return -1 to indicate failure
	private float getCurrentTrim(boolean getBottom)
	{
		float prevTrim = 0.0f;
		TrackSceneNode tsn = CoreGraph.getInstance().getCurrentTrack();
		
		if (tsn == null) return 0.0f;
		
		Vector<CoreSection> secVec = tsn.getCoreSections();
		boolean didOnce = false;
		for ( CoreSection section : secVec )
		{
			final float length = SceneGraph.getSectionLength(tsn.getId(), section.getId());
			float secTrim = 0.0f;
			if (getBottom)
				secTrim = length - SceneGraph.getSectionIntervalBottom(tsn.getId(), section.getId());
			else
				secTrim = SceneGraph.getSectionIntervalTop(tsn.getId(), section.getId());
			
			if (!didOnce)
			{
				prevTrim = secTrim;
				didOnce = true;
			}
			else
			{
				if (secTrim != prevTrim) // 2/15/2012 brg: epsilon comparison?
					return -1.0f;
			}
		}
		
		return prevTrim; // sections' trim values matched, return
	}

	private void onApply()
	{
		float trim = 0.0f;
		try {
			trim = Float.parseFloat(trimField.getText());
		} catch (NumberFormatException e ) {
			JOptionPane.showMessageDialog(this, trimField.getText() + " is not a valid trim value");
			return;
		}
		
		final boolean fromBottom = ( beginEndBox.getSelectedIndex() == 0 ); // "bottom"
		final boolean trimSelAndDeeper = ( affectedSectionsBox.getSelectedIndex() == 0 ); // "selected and deeper sections"
		if ( trimTypeBox.getSelectedIndex() == 1 ) // if adding, swap trim sign
			trim = -trim;
		SceneGraph.trimSections(this.selectedTrack, this.selectedTrackSection, trim, fromBottom, trimSelAndDeeper);
		CorelyzerApp.getApp().updateGLWindows();
	}
}