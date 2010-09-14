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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class SplashScreenMain {
	public static boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

	private static void invokeMain(final String className, final String[] args) {
		try {
			Class.forName(className).getMethod("main", new Class[] { String[].class }).invoke(null, new Object[] { args });
		} catch (Exception e) {
			InternalError error = new InternalError("Failed to invoke main method");
			error.initCause(e);
			throw error;
		}
	}

	public static void main(final String[] args) {
		argv = args;

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		new SplashScreenMain();
	}

	SplashScreen screen;

	Vector<File> jarFiles;

	static String[] argv;

	public SplashScreenMain() {
		// initialize data
		jarFiles = findJars();

		splashScreenInit(jarFiles.size());

		// initialize classpathes
		startup_loading();

		screen.setIndeterminated(true);

		try {
			invokeMain("corelyzer.ui.CorelyzerApp", argv);
		} catch (InternalError e) {
			String message = "Cannot start Corelyzer:\n" + e;
			e.printStackTrace();

			// cleanup
			splashScreenDestruct();
			JOptionPane.showMessageDialog(null, message);

			System.exit(0);
		}

		screen.setIndeterminated(false);
		screen.setProgress("Corelyzer Loaded", this.jarFiles.size());

		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		splashScreenDestruct();
	}

	private Vector<File> findJars() {
		FilenameFilter jarFilenameFilter = new FilenameFilter() {

			public boolean accept(final File dir, final String filename) {

				return filename.toLowerCase().endsWith(".jar");
			}
		};

		Vector<File> jars = new Vector<File>();

		// 1st check ../lib, as in MacOSX application bundle
		File libDir = new File("../lib");
		if (libDir.exists() && libDir.isDirectory()) {
			File[] files = libDir.listFiles(jarFilenameFilter);
			jars.addAll(Arrays.asList(files));
		}

		// 2nd check ./jars
		File jarDir = new File("./jars");
		if (jarDir.exists() && jarDir.isDirectory()) {
			File[] files = jarDir.listFiles(jarFilenameFilter);
			jars.addAll(Arrays.asList(files));

			File osJarDir;
			if (MAC_OS_X) {
				osJarDir = new File("./jars/mac");
			} else {
				osJarDir = new File("./jars/windows/win32");
			}

			if (osJarDir.exists() && osJarDir.isDirectory()) {
				File[] moreFiles = osJarDir.listFiles(jarFilenameFilter);
				jars.addAll(Arrays.asList(moreFiles));
			}
		}

		// 3rd check plugins
		File pluginDir = new File("../plugins");
		if (pluginDir.exists() && pluginDir.isDirectory()) {
			File[] files = pluginDir.listFiles(jarFilenameFilter);
			jars.addAll(Arrays.asList(files));
		}

		return jars;
	}

	private void splashScreenDestruct() {
		screen.setProgress("Done", jarFiles.size());
		screen.setScreenVisible(false);
	}

	private void splashScreenInit(final int steps) {
		// ImageIcon myImage = new ImageIcon("resources/corelyzer_icon.jpg");
		ImageIcon myImage = new ImageIcon("resources/corewall_suite.png");

		screen = new SplashScreen(myImage);
		screen.setLocationRelativeTo(null);
		screen.setProgressMax(steps);
		screen.setScreenVisible(true);
	}

	private void startup_loading() {
		for (int i = 0; i < jarFiles.size(); i++) {
			File f = jarFiles.elementAt(i);

			// delays
			/*
			 * String isDebug = System.getProperty("corelyzer.debug"); if(
			 * (isDebug == null) || (isDebug.toLowerCase().equals("false")) ) {
			 * for(int j = 0; j<500000; j++) { String poop = " " + (j + i); } }
			 */

			// add classpath
			// String loadResult;
			try {
				ClassPathHacker.addFile(f);
				// loadResult = "Success";
			} catch (IOException e) {
				// loadResult = "Failed";
			}

			// String message = "Loading " + f.getName() + ": " + loadResult;
			String message = "Loading...";
			screen.setProgress(message, i);
		}
	}

}
