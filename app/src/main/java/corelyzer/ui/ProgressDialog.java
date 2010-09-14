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

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

/**
 * Reusable progress dialog to show current work progress. Used in loading core
 * section images, dataset files, and state files.
 */
// TODO make thread thing working properly!!!!
public class ProgressDialog extends Thread {
	String status;
	JDialog dlg;
	JProgressBar pb;
	JLabel label;
	public boolean keep_running;

	public ProgressDialog() {
		super();
		setupUI();
		dlg.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		// dlg.setVisible(true);
		start();
	}

	public void dispose() {
		/*
		 * keep_running = false; try{ this.join(); } catch( Exception e) {
		 * e.printStackTrace(); }
		 */
		dlg.dispose();
	}

	/** Access method */
	public String getLabelText() {
		return this.label.getText();
	}

	/** Access method */
	public int getProgress() {
		return pb.getValue();
	}

	/** Access method */
	public JProgressBar getProgressBar() {
		return pb;
	}

	/** Access method */
	public int getProgressMax() {
		return pb.getMaximum();
	}

	/** Access method */
	public int getProgressMin() {
		return pb.getMinimum();
	}

	/** Access method */
	public String getStatusText() {
		return pb.getString();
	}

	/** Main execution method of the progress update thread */

	@Override
	public void run() {
		keep_running = true;
		try {
			JPanel jp = (JPanel) dlg.getContentPane();
			while (keep_running) {
				jp.paintImmediately(0, 0, jp.getWidth(), jp.getHeight());
				Thread.sleep(10);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setIndeterminant() {
		pb.setIndeterminate(true);
	}

	/** Access method */
	public void setLabelText(final String t) {
		this.label.setText(t);
	}

	/** Access method */
	public void setProgress(final int p) {
		pb.setValue(p);
		int percentage = (int) (pb.getPercentComplete() * 100.0);
		String title = this.status + " " + percentage + "%";
		pb.setString(title);
	}

	/** Access method */
	public void setProgressMax(final int max) {
		pb.setMaximum(max);
	}

	/** Access method */
	public void setProgressMin(final int min) {
		pb.setMinimum(min);
	}

	/** Access method */
	public void setStatusText(final String t) {
		this.status = t;
		pb.setString(t);
	}

	private void setupUI() {
		/*
		 * try { if(Class.forName("CorelyzerApp") != null) { dlg = new
		 * JDialog(CorelyzerApp.getApp().getMainFrame()); } else { //
		 * System.out.println("CorelyzerApp null"); dlg = new JDialog(); } }
		 * catch (ClassNotFoundException e) { //
		 * System.err.println("CorelyzerApp not found"); dlg = new JDialog(); }
		 */
		dlg = new JDialog(CorelyzerApp.getApp().getMainFrame());

		((JPanel) dlg.getContentPane()).setBorder(BorderFactory.createTitledBorder("In Progress"));
		dlg.setSize(new Dimension(260, 120));
		// dlg.setLocation(600, 400);
		dlg.setLocationRelativeTo(CorelyzerApp.getApp().getMainFrame());
		dlg.setResizable(false);

		SpringLayout layout = new SpringLayout();
		dlg.getContentPane().setLayout(layout);

		label = new JLabel("Some init text here");
		label.setPreferredSize(new Dimension(240, 25));
		layout.putConstraint(SpringLayout.NORTH, label, 5, SpringLayout.NORTH, dlg.getContentPane());
		layout.putConstraint(SpringLayout.WEST, label, 5, SpringLayout.WEST, dlg.getContentPane());
		dlg.add(label);

		pb = new JProgressBar();
		pb.setPreferredSize(new Dimension(240, 25));
		pb.setStringPainted(true);
		layout.putConstraint(SpringLayout.NORTH, pb, 35, SpringLayout.NORTH, dlg.getContentPane());
		layout.putConstraint(SpringLayout.WEST, pb, 5, SpringLayout.WEST, dlg.getContentPane());
		dlg.add(pb);
	}

	public void setVisible(final boolean value) {
		try {
			dlg.setAlwaysOnTop(true);
		} catch (SecurityException e) {
			System.out.println(e);
			System.out.println("Could not set progressdialgo always be front");
		}

		dlg.setVisible(value);
	}
}
