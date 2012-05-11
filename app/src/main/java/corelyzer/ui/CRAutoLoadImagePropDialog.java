package corelyzer.ui;

import java.util.Vector;

import java.awt.event.*;
import java.awt.Frame;

import javax.swing.*;
import javax.swing.event.*;

import net.miginfocom.swing.MigLayout;

import corelyzer.data.*;

public class CRAutoLoadImagePropDialog extends JDialog {
	
	JPanel contentPane;
	ImagePropertyTable imageTable;
	BatchInputPanel biPanel;
	JButton okButton, cancelButton;
	Vector<TrackSectionListElement> newSections;
	
	public static void main(String[] args)
	{
		CRAutoLoadImagePropDialog dlg = new CRAutoLoadImagePropDialog( null, null );
		dlg.pack();
		dlg.setVisible(true);
		System.exit(0);
	}
	
	public CRAutoLoadImagePropDialog( JDialog owner, Vector<TrackSectionListElement> newSections )
	{
		super(owner);
		this.newSections = newSections;
		
		setupUI();
		setupListeners();
		
		// load section properties into table: create if necessary
		for ( TrackSectionListElement section : newSections )
		{
			ImagePropertyTable.ImageProperties props = section.getImageProperties();
			imageTable.addImageAndProperties( section.getName(), props.orientation, props.length, props.dpix, props.dpiy, props.depth );
		}
	}
	
	private void setupUI()
	{
		contentPane = new JPanel( new MigLayout("wrap 1, fillx"));
		
		imageTable = new ImagePropertyTable();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView( imageTable );
		contentPane.add( scrollPane, "height 400::, growx");
		
		biPanel = new BatchInputPanel( imageTable );
		contentPane.add( biPanel, "growx" );

		cancelButton = new JButton("Cancel");
		contentPane.add(cancelButton, "split 2, align right");
		okButton = new JButton("OK");
		contentPane.add(okButton);
		
		setTitle("Set Image Properties");
		setContentPane(contentPane);
		setModal(true);
		pack();
		
		imageTable.updateUI();
	}
	
	private void setupListeners()
	{
		cancelButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		okButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		});
	}
	
	private void onOK()
	{
		for (int i = 0; i < imageTable.getRowCount(); i++) {
			TrackSectionListElement section = newSections.elementAt( i );
			
			section.getImageProperties().orientation = (String) imageTable.model.getValueAt(i, 1);
			section.getImageProperties().length = (Float) imageTable.model.getValueAt(i, 2);
			section.getImageProperties().dpix = (Float) imageTable.model.getValueAt(i, 3);
			section.getImageProperties().dpiy = (Float) imageTable.model.getValueAt(i, 4);
			section.getImageProperties().depth = (Float) imageTable.model.getValueAt(i, 5);
		}
		
		dispose();
	}
}
