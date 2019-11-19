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

package corelyzer.helper;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JButton;
import javax.swing.JProgressBar;

import corelyzer.ui.AuthDialog;
import corelyzer.ui.CorelyzerApp;

/** Reusable static class to download file from given URL to a local file */
public class URLRetrieval extends Thread {

	// Helper class to access URLs which require authentication
	static class MyAuthenticator extends Authenticator implements ActionListener {
		private boolean authentication_cancelled = false;

		public void actionPerformed(final ActionEvent event) {
			// let only authDialog's cancelBtn register for actionEvent,
			// so only when cancel button clicked will trigger the following
			// actions
			JButton btn = (JButton) event.getSource();

			if (btn.getText().toLowerCase().contains("cancel")) {
				authentication_cancelled = true;
			}
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			// FIXME
			CorelyzerApp app = CorelyzerApp.getApp();
			Frame f = app == null ? null : app.getMainFrame();

			AuthDialog authDialog = new AuthDialog(f, getRequestingPrompt());

			authDialog.setUsername(username);
			authDialog.setPassword(password);
			authDialog.setCancelActionListener(this);

			Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
			int loc_x = scrnsize.width / 2 - authDialog.getSize().width / 2;
			int loc_y = scrnsize.height / 2 - authDialog.getSize().height / 2;
			authDialog.setLocation(loc_x, loc_y);

			authDialog.pack();
			authDialog.setVisible(true);

			if (!authentication_cancelled) {
				return new PasswordAuthentication(authDialog.getUsername(), authDialog.getPassword());
			} else {
				return null;
			}
		}

	}

	static String username;

	static String password;

	public static void main(final String[] args) {
		try {
			if (!URLRetrieval.retrieveLocalCopy(args[0], args[1])) {
				System.out.println("Failed");
			} else {
				System.out.println("Success");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean retrieveLocalCopy(final String url, final String local_copy) throws IOException {
		return retrieveLocalCopyWithLength(url, local_copy) != -1;
	}

	/**
	 * Static method to be called to get a file from given URL
	 * 
	 * @param url
	 *            The URL of file to be downloaded
	 * @param local_copy
	 *            The local file path of downloaded file
	 * @param progressBar
	 *            A progress bar that should have it's progress updated
	 */
	public static boolean retrieveLocalCopy(final String url, final String local_copy, final JProgressBar progressBar) {

		URL remote;
		URLConnection uc;
		DataInputStream dis = null;
		FileOutputStream fos = null;

		int length = 0;
		int iread = 0;

		// OPEN STREAMS
		try {
			remote = new URL(url);
			uc = remote.openConnection();
			fos = new FileOutputStream(local_copy);
			dis = new DataInputStream(uc.getInputStream());

			int len = uc.getContentLength();
			len = len / 4;

			if (len <= 0) {
				progressBar.setMaximum(len);
			} else {
				progressBar.setIndeterminate(true);
			}

			/*
			 * int data; int prog = 0; while((data = dis.read()) != -1) {
			 * fos.write(data); prog++; progressBar.setValue(prog); }
			 */

			length = uc.getContentLength();
			// System.out.println("urlretrieval file size: " + length);

			byte[] buffer = new byte[1024];
			int bytesRead = -1;
			int prog = 0;
			while (true) {
				bytesRead = dis.read(buffer);
				if (bytesRead == -1) {
					break;
				}
				iread += bytesRead;
				fos.write(buffer, 0, bytesRead);
				prog += bytesRead;
				progressBar.setValue(prog);
			}

		} catch (MalformedURLException exception) {
			// close io handle
			try {
				if (dis != null) {
					dis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) {
			}

			// delete incomplete download file
			File incomplete = new File(local_copy);
			incomplete.delete();
			System.err.println("[EXCEPTION] " + exception);
			exception.printStackTrace();
			return false;
		} catch (IOException exception) {
			// close io handle
			try {
				if (dis != null) {
					dis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) {
			}

			// delete incomplete download file
			File incomplete = new File(local_copy);
			incomplete.delete();
			System.err.println("[EXCEPTION] " + exception);
			exception.printStackTrace();
			return false;
		} catch (Exception exception) {
			// close io handle
			try {
				if (dis != null) {
					dis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) {
			}

			// delete incomplete download file
			File incomplete = new File(local_copy);
			incomplete.delete();
			System.err.println("[EXCEPTION] " + exception);
			exception.printStackTrace();
			return false;
		}

		try {
			dis.close();
			fos.flush();
			fos.close();

			// check download size
			if (iread != length) {
				// something wrong here
				// at least, close all io and delete downloaded file
				File incomplete = new File(local_copy);
				incomplete.delete();

				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;

	}

	public static String retrieveLocalCopy(final String url, final String project, final String session) {
		CorelyzerApp app = CorelyzerApp.getApp();

		if (app != null) {
			String prefix = app.getDownloadDirectoryPath();
			String sp = System.getProperty("file.separator");
			String fileName = new File(url).getName();

			File projDir = new File(prefix + sp + project);
			File sessionDir = new File(prefix + sp + project + sp + session);

			if (!projDir.exists()) {
				projDir.mkdir();
			}

			if (!sessionDir.exists()) {
				sessionDir.mkdir();
			}

			String localFileStr = sessionDir.getAbsolutePath() + sp + fileName;

			try {
				int isDownloaded = retrieveLocalCopyWithLength(url, localFileStr, "username", "password");

				if (isDownloaded != -1) {
					return localFileStr;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		return null;
	}

	public static boolean retrieveLocalCopy(final String url, final String local_copy, final String uString, final String pString) throws IOException {
		return retrieveLocalCopyWithLength(url, local_copy, uString, pString) != -1;
	}

	/**
	 * Static method to be called to get a file from given URL
	 * 
	 * @param url
	 *            The URL of file to be downloaded
	 * @param local_copy
	 *            The local file path of downloaded file
	 * @return boolean Whether download successfully
	 */
	public static int retrieveLocalCopyWithLength(final String url, final String local_copy) throws IOException {
		Authenticator.setDefault(new MyAuthenticator());

		URL remote;
		URLConnection uc;
		DataInputStream dis = null;
		// DataOutputStream dos;
		FileOutputStream fos = null;

		int length;
		int iread = 0;

		// OPEN STREAMS
		try {

			remote = new URL(url);
			uc = remote.openConnection();
			fos = new FileOutputStream(local_copy);
			// dos = new DataOutputStream(fos);
			dis = new DataInputStream(uc.getInputStream());
			/*
			 * int data; while((data = dis.read()) != -1) { fos.write(data); }
			 */
			length = uc.getContentLength();
			System.out.println("urlretrieval file size: " + length); // fixme
			byte[] buffer = new byte[1024];
			int bytesRead; // = -1;
			while (true) {
				bytesRead = dis.read(buffer);
				if (bytesRead == -1) {
					break;
				}
				iread += bytesRead;
				fos.write(buffer, 0, bytesRead);
			}

		} catch (MalformedURLException exception) {
			// close io handle
			try {
				if (dis != null) {
					dis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) {
			}

			// delete incomplete download file
			File incomplete = new File(local_copy);
			incomplete.delete();
			System.err.println("[EXCEPTION] " + exception);
			exception.printStackTrace();
			return -1;
		} catch (FileNotFoundException exception) {
			// close io handle
			try {
				if (dis != null) {
					dis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// delete incomplete download file
			File incomplete = new File(local_copy);
			incomplete.delete();
			System.err.println("[EXCEPTION] " + exception);
			// exception.printStackTrace();
			// JOptionPane.showMessageDialog(null, "Cannot Find the File '" +
			// url + "'.\nDownload Request Aborted.");
			return -1;
		} /*
		 * catch(IOException exception) { // close io handle try { if (dis !=
		 * null) dis.close(); if (fos != null) fos.close(); } catch (Exception
		 * e) { System.err.println(
		 * "---> Cannot close Datastream in URLRetrieval:209");
		 * e.printStackTrace(); }
		 * 
		 * // delete incomplete download file File incomplete = new
		 * File(local_copy); incomplete.delete();
		 * System.err.println("[EXCEPTION] " + exception);
		 * exception.printStackTrace(); return -1; }
		 */

		try {
			dis.close();
			fos.flush();
			fos.close();

			// check download size
			/*
			 * fixme if (iread != length) { // something wrong here // at least,
			 * close all io and delete downloaded file
			 * System.out.println("iread: " + iread + ", length: " + length);
			 * 
			 * File incomplete = new File(local_copy); if (incomplete.exists())
			 * incomplete.delete();
			 * 
			 * return -1; }
			 */

		} catch (Exception e) {
			System.err.println("---> Cannot close Datastream in URLRetrieval:240");
			e.printStackTrace();
		}

		return iread;
		// return length;
	}

	public static int retrieveLocalCopyWithLength(final String url, final String local_copy, final String uString, final String pString) throws IOException {
		setUserPass(uString, pString);
		return retrieveLocalCopyWithLength(url, local_copy);
	}

	public static void setUserPass(final String uString, final String pString) {
		username = uString;
		password = pString;
	}

	private String u;

	private String l;

	private JProgressBar b;

	public URLRetrieval() {
		super();
		Authenticator.setDefault(new MyAuthenticator());
		System.out.println("-- [DEBUG] MyAuthenticator inited");
	}

	public URLRetrieval(final String url, final String local) {
		this();
		u = url;
		l = local;
		b = null;
	}

	public URLRetrieval(final String url, final String local, final JProgressBar bar) {
		this();
		u = url;
		l = local;
		b = bar;
	}

	@Override
	public void run() {
		try {
			if (b != null) {
				retrieveLocalCopy(u, l, b);
			} else {
				retrieveLocalCopy(u, l);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
