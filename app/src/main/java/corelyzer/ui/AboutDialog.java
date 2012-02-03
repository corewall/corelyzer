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
package corelyzer.ui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import corelyzer.helper.URLRetrieval;

/**
 * Class extends JDialog in order to show the "About" dialog for the
 * application.
 */
public class AboutDialog extends JDialog implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6020275856495545374L;
	JButton moreinfoBtn;
	JButton swUpdateBtn;

	public AboutDialog() {
		super(CorelyzerApp.getApp().getMainFrame());
		setTitle("About Corelyzer");
		setSize(310, 460);
		this.setResizable(false);

		// Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
		// int loc_x = scrnsize.width / 2 - (this.getSize().width / 2);
		// int loc_y = scrnsize.height / 2 - (this.getSize().height / 2);
		// this.setLocation(loc_x, loc_y);
		this.setLocationRelativeTo(CorelyzerApp.getApp().getMainFrame());

		setLayout(new FlowLayout(FlowLayout.CENTER, 100, 5));

		JLabel label = new JLabel(new ImageIcon("resources/corelyzer_icon.jpg"));
		add(label);

		String version = CorelyzerApp.getApp().getCorelyzerVersion();
		label = new JLabel("Version " + version);
		add(label);

		swUpdateBtn = new JButton("Software Update...");
		swUpdateBtn.addActionListener(this);
		swUpdateBtn.setEnabled(true);
		add(swUpdateBtn);

		label = new JLabel("Electronic Visualization Laboratory");
		add(label);
		label = new JLabel("University of Illinois at Chicago");
		add(label);

		moreinfoBtn = new JButton("More Info...");
		moreinfoBtn.addActionListener(this);
		add(moreinfoBtn);

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowActivated(final WindowEvent e) {
				moreinfoBtn.requestFocusInWindow();
			}
		});
	}

	public void actionPerformed(final ActionEvent ae) {

		if (ae.getSource().equals(this.moreinfoBtn)) {
			try {
				String app;
				String url = System.getProperty("corelyzer.projecturl");

				if (System.getProperty("os.name").toLowerCase().contains("windows")) {
					app = "cmd.exe /c explorer " + url;
					Runtime.getRuntime().exec(app);
				} else {
					app = "open";
					String[] cmd = { app, url };
					Runtime.getRuntime().exec(cmd);
				}

			} catch (IOException ex) {
				System.err.println("IOException in About#MoreInfo button");
			}
		} else if (ae.getSource().equals(this.swUpdateBtn)) {
			this.checkUpdateAction();
		} else {
			System.out.println("Nothing fun here");
		}

		dispose();
	}

	public void checkUpdateAction() {
		Runnable checkStatus = new Runnable() {

			public void run() {
				String sp = System.getProperty("file.separator");

				String versionURLString = "http://www.evl.uic.edu/cavern/corewall/distros/" + "current_version";
				String localVersionFile = new File("./").getAbsolutePath() + sp + "latestVersion";

				boolean retResult;

				try {
					retResult = URLRetrieval.retrieveLocalCopy(versionURLString, localVersionFile);
				} catch (IOException e) {
					System.out.println("---> [Exception] AboutDialog runnnable " + e);
					retResult = false;
				}

				if (!retResult) {
					JOptionPane.showMessageDialog(CorelyzerApp.getApp().getMainFrame(), "Couldn't get latest version info.\n" + "Please try again later.");
					return;
				}

				checkVersion(localVersionFile);
			}
		};

		System.out.println("---> Start the version checking thread");
		new Thread(checkStatus).start();
	}

	private void checkVersion(final String aFile) {
		File f = new File(aFile);
		String latest_version = "";

		if (f.exists()) {
			try {
				FileReader fr = new FileReader(f);
				BufferedReader br = new BufferedReader(fr);

				String line;

				while ((line = br.readLine()) != null) {
					String[] toks = line.split("=");

					if (toks[0].trim().equalsIgnoreCase("current_version")) {
						latest_version = toks[1].trim();
					}
				}

				br.close();
				fr.close();
			} catch (IOException e) {
				System.err.println("-- [INFO] IOException - Read version info failed");
			}

			String myVersion = CorelyzerApp.getApp().getCorelyzerVersion();
			if (myVersion.compareTo(latest_version) >= 0) {
			} else {
				Object[] options = { "Download from CoreWall.org Now", "Later" };

				int sel = JOptionPane.showOptionDialog(CorelyzerApp.getApp().getMainFrame(), "The latest version is " + latest_version
						+ ". Do you want to download now?", "Software Update", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
						options[0]);

				switch (sel) {
					case 0:
						try {
							String app;
							String url = "http://www.corewall.org/downloads.php";

							if (System.getProperty("os.name").toLowerCase().contains("windows")) {
								app = "cmd.exe /c explorer " + url;
								Runtime.getRuntime().exec(app);
							} else {
								app = "open";
								String[] cmd = { app, url };
								Runtime.getRuntime().exec(cmd);
							}

							this.setVisible(false);

						} catch (IOException ex) {
							System.err.println("IOException in About#swUpdate button");
						}

						break;

					default:
						System.out.println("Do nothing");
				}

			}
		}
	}

}
