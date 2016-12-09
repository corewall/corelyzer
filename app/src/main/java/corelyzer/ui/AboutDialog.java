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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Frame;
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
import javax.swing.JSeparator;
import javax.swing.WindowConstants;

import corelyzer.util.VersionUtils;

import net.miginfocom.swing.MigLayout;
import org.json.JSONObject;

/**
 * Class extends JDialog in order to show the "About" dialog for the
 * application.
 */
public class AboutDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = -6020275856495545374L;
	JButton moreinfoBtn;
	JButton swUpdateBtn;
	
	public static void main(String[] args) {
		Frame frame = new Frame();
		AboutDialog ad = new AboutDialog(frame);
		ad.setVisible(true);
	}

	public AboutDialog(Frame parent) {
		super(parent);
		setTitle("About Corelyzer");
		this.setResizable(false);
		this.getContentPane().setLayout(new MigLayout("wrap", "[center]", ""));

		add(new JLabel(new ImageIcon("resources/corelyzer_icon.jpg")));
		String version = CorelyzerApp.getApp().getCorelyzerVersion();
		add(new JLabel("Version " + version));
		add(new JLabel("CSDCO/LacCore - University of Minnesota"));
		
		swUpdateBtn = new JButton("Check for Updates");
		swUpdateBtn.addActionListener(this);
		swUpdateBtn.setEnabled(true);
		add(swUpdateBtn, "split 2");

		moreinfoBtn = new JButton("More Info...");
		moreinfoBtn.addActionListener(this);
		add(moreinfoBtn);
		JSeparator sep = new JSeparator();
		add(sep, "growx");
		
		// Java Runtime info
		String javaVersion = System.getProperty("java.version");
		String javaVendor = System.getProperty("java.vendor");
		String javaHome = System.getProperty("java.home");
		add(new JLabel("Running Java " + javaVersion + " (" + javaVendor + ")"));
		add(new JLabel("Java home: " + javaHome));

		pack();
		this.setLocationRelativeTo(CorelyzerApp.getApp().getMainFrame());

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
			String url = null;
			try {
				url = "http://csdco.umn.edu/resources/software/corelyzer";
				URI uri = new URI(url);
				java.awt.Desktop.getDesktop().browse(uri);
			} catch (IOException ex) {
				System.err.println("IOException trying to browse to " + url + " from About Dialog");
			} catch (URISyntaxException urie) {
				System.err.println("URI Syntax Exception parsing " + url + ":" + urie.getMessage());
			}
			//catch (Exception e) {
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
						if (!VersionUtils.isLatestVersion(curVersion, latestVersion)) {
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
}

