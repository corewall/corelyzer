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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import corelyzer.data.CRPreferences;
import corelyzer.helper.ExampleFileFilter;

public class AttachmentURLDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1951882054116890075L;

	public static void main(final String[] args) {
		AttachmentURLDialog dialog = new AttachmentURLDialog();
		dialog.setLocationRelativeTo(null);
		dialog.pack();
		dialog.setVisible(true);
		System.exit(0);
	}

	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JButton buttonBrowse;

	private JTextField urlTextField;
	private boolean isImageLoader;

	private CRPreferences prefs;

	{
		// GUI initializer generated by IntelliJ IDEA GUI Designer
		// >>> IMPORTANT!! <<<
		// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	public AttachmentURLDialog() {
		init();
	}

	public AttachmentURLDialog(Dialog parent) {
		super(parent);
		init();
	}

	private void init() {
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonBrowse);

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
		buttonBrowse.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent actionEvent) {
				onBrowse();
			}
		});
		urlTextField.addPropertyChangeListener(new PropertyChangeListener() {

			public void propertyChange(final PropertyChangeEvent propertyChangeEvent) {
				if (urlTextField.getText() != null && !urlTextField.getText().equals("")) {
					buttonOK.setEnabled(true);
				}
			}
		});
		urlTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(final KeyEvent keyEvent) {
				if (urlTextField.getText() != null && !urlTextField.getText().equals("")) {
					buttonOK.setEnabled(true);
				} else {
					buttonOK.setEnabled(false);
				}
			}
		});

		try {
			setAlwaysOnTop(true);
		} catch (SecurityException e) {
			System.err.println("-- [WARN] Cannot set " + "corelyzer.ui.AttachmentURLDialog on top");
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
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buttonOK = new JButton();
		buttonOK.setEnabled(false);
		buttonOK.setText("OK");
		panel2.add(buttonOK, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		buttonBrowse = new JButton();
		buttonBrowse.setText("Browse...");
		panel2.add(buttonBrowse, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		buttonCancel = new JButton();
		buttonCancel.setText("Cancel");
		panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		contentPane
				.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
						0, false));
		final JLabel label1 = new JLabel();
		label1.setText("File URL: ");
		panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		urlTextField = new JTextField();
		urlTextField.setText("");
		panel3.add(urlTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
	}

	public String getURLString() {
		return this.urlTextField.getText();
	}

	private void onBrowse() {
		String dataRoot;
		if (prefs != null) {
			dataRoot = prefs.datastore_Directory;
		} else {
			dataRoot = System.getProperty("user.home");
		}

		JFileChooser chooser = new JFileChooser(dataRoot);

		ExampleFileFilter fileFilter = new ExampleFileFilter();

		if (isImageLoader) {
			chooser.setDialogTitle("Select Image File");
			fileFilter.setDescription("Image Files");
			fileFilter.addExtension("jpg");
			fileFilter.addExtension("jpeg");
			fileFilter.addExtension("png");
			fileFilter.addExtension("tif");
			fileFilter.addExtension("tiff");
			fileFilter.addExtension("bmp");

			chooser.resetChoosableFileFilters();
			chooser.setFileFilter(fileFilter);
		} else {
			chooser.setDialogTitle("Select Attachment File");
			fileFilter.setDescription("Attachments");
			chooser.resetChoosableFileFilters();
		}

		int returnVal = chooser.showOpenDialog(this);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = chooser.getSelectedFile();
			String fileURLString = "file:///" + selectedFile.getAbsolutePath();
			this.urlTextField.setText(fileURLString);
			this.buttonOK.setEnabled(true);
			getRootPane().setDefaultButton(buttonOK);
		}
	}

	private void onCancel() {
		dispose();
	}

	private void onOK() {
		System.out.println("-- [DEBUG] Attachment URL is: " + this.urlTextField.getText());
		dispose();
	}

	public void setIsImageLoader(final boolean b) {
		isImageLoader = b;
	}

	public void setPreferences(final CRPreferences p) {
		prefs = p;
	}
}
