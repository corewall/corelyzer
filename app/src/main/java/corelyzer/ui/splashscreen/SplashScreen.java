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
package corelyzer.ui.splashscreen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

public class SplashScreen extends JWindow {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6084478604277146239L;
	BorderLayout borderLayout1 = new BorderLayout();
	JLabel imageLabel = new JLabel();
	JPanel southPanel = new JPanel();
	FlowLayout southPanelFlowLayout = new FlowLayout();
	JProgressBar progressBar = new JProgressBar();
	ImageIcon imageIcon;

	public SplashScreen(final ImageIcon imageIcon) {
		this.imageIcon = imageIcon;
		try {
			jbInit();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		this.setAlwaysOnTop(false);
	}

	// note - this class created with JBuilder
	void jbInit() throws Exception {
		imageLabel.setIcon(imageIcon);
		this.getContentPane().setLayout(borderLayout1);
		southPanel.setLayout(southPanelFlowLayout);
		southPanel.setBackground(Color.BLACK);
		this.getContentPane().add(imageLabel, BorderLayout.CENTER);
		this.getContentPane().add(southPanel, BorderLayout.SOUTH);
		southPanel.add(progressBar, null);
		this.pack();
	}

	public void setIndeterminated(final boolean is) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				progressBar.setIndeterminate(is);
			}
		});
	}

	private void setMessage(String message) {
		if (message == null) {
			message = "";
			progressBar.setStringPainted(false);
		} else {
			progressBar.setStringPainted(true);
		}
		progressBar.setString(message);
	}

	public void setProgress(final int progress) {
		final int theProgress = progress;
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				progressBar.setValue(theProgress);
			}
		});
	}

	public void setProgress(final String message, final int progress) {
		final int theProgress = progress;
		final String theMessage = message;
		setProgress(progress);

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				progressBar.setValue(theProgress);
				setMessage(theMessage);
			}
		});
	}

	public void setProgressMax(final int maxProgress) {
		progressBar.setMaximum(maxProgress);
	}

	public void setScreenVisible(final boolean b) {
		final boolean boo = b;
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				setVisible(boo);
			}
		});
	}

}
