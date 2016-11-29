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
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.json.JSONObject;

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
				System.out.println(url);

				// todo
				//URI uri = new URI(url);
				//java.awt.Desktop.getDesktop().browse(uri);

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
			} //catch (Exception e) {
			//	System.err.println("Exception attempting to open URL in default browser: " + e.getMessage());
			//}
		} else if (ae.getSource().equals(this.swUpdateBtn)) {
			this.checkUpdateAction(false);
		} else {
			System.out.println("Nothing fun here");
		}

		dispose();
	}
	
	// if silent = true, no message dialog will be popped if Corelyzer is up to date
	public void checkUpdateAction(final boolean silent) {
		Runnable getLatest = new Runnable() {
			public void run() {
				// todo: move to non-existent testing code!
//				System.out.println(isLatestVersion("2", "2"));
//				System.out.println(isLatestVersion("2", "1"));
//				System.out.println(isLatestVersion("1.0.0", "1.0.0"));
//				System.out.println(isLatestVersion("1.0.1", "1.0.0"));
//				System.out.println(isLatestVersion("1.0.1", "1.0"));
//				System.out.println(isLatestVersion("1.0.1", "1.0.0.0"));
//				System.out.println(isLatestVersion("1.2.3.0", "1.2.3"));
//				System.out.println(isLatestVersion("1.2.3.0", "1.2.4"));
		        
				// grab latest release's version from Github
				String urlStr = "https://api.github.com/repos/corewall/corelyzer/releases/latest";
				BufferedReader reader = null;
				JSONObject jsonObj = null;
				try {
					reader = new BufferedReader(new InputStreamReader(new URL(urlStr).openStream()));
					String jsonResponse = "";
					String inputLine = null;
					while ((inputLine = reader.readLine()) != null) {
						jsonResponse += inputLine;
					}
					jsonObj = new JSONObject(jsonResponse);
					if (reader != null)
						reader.close();
				} catch (IOException ioe) {
					msg("Latest version data unavailable");
				}				
				
				String latestVersion = jsonObj.getString("tag_name");
				String curVersion = CorelyzerApp.getApp().getCorelyzerVersion();
				
				try {
					if (latestVersion != null) {
						if (!isLatestVersion(curVersion, latestVersion)) {
							final String msg = "Corelyzer " + latestVersion + " is available, open download page in default browser?";
							if (JOptionPane.showConfirmDialog(CorelyzerApp.getApp().getPopupParent(), msg, "New Version Available", JOptionPane.YES_NO_OPTION) == 0)
							{
								java.awt.Desktop.getDesktop().browse(new URI(jsonObj.getString("html_url")));
							}
						} else if (!silent) {
							msg("Corelyzer is up to date.");
						}
					}
				} catch (NumberFormatException nfe) {
					msg("Version check failed, couldn't parse version: " + nfe.getMessage());
					return;
				} catch (URISyntaxException use) {
					msg("Version check failed, invalid URI syntax: + " + use.getMessage());
					return;
				} catch (Exception e) {
					msg("Version check failed.");
				}
			}
		};
		new Thread(getLatest).start();
	}
	
	private void msg(String text) { JOptionPane.showMessageDialog(CorelyzerApp.getApp().getPopupParent(), text); }

	// strip qualifying text from a version string e.g. "-beta" from "1.2.3-beta"
	private String stripVersionQualifier(String version) {
		int[] delims = { '-', '_', ' ' };
		for (int i = 0; i < delims.length; i++) {
			int idx = version.indexOf(delims[i]);
			if (idx != -1) {
				version = version.substring(0, idx);
				break;
			}
		}

		return version;
	}

	// return true if version1 >= version2 
	private boolean isLatestVersion(String version1, String version2) throws NumberFormatException {
		String[] v1 = stripVersionQualifier(version1).split("\\.");
		String[] v2 = stripVersionQualifier(version2).split("\\.");

		boolean latest = false;
		boolean diffFound = false;
		int index = 0;
		while (index < v1.length && index < v2.length) {
			try {
				int comp = Integer.valueOf(v1[index]).compareTo(Integer.valueOf(v2[index]));
				if (comp != 0) {
					latest = (comp > 0); // comp > 0 implies v1 > v2
					diffFound = true;
					break;
				}
			} catch (NumberFormatException nfe) {
				throw nfe;
			}
			index++;
		}
		
		// if equivalent up to this point, longer version is greater
		if (!diffFound)
			latest = v1.length >= v2.length;
			
		return latest;
	}
}
