/** This class is deprecated */

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

import java.io.File;
import java.io.FileFilter;

/**
 * Corelyzer startup class used for taking care of classpaths and plugins
 * initialization loadups in different operation systems.
 */
public class Startup implements FileFilter {

	public static boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

	public static void main(final String[] args) {

		String cp = System.getProperty("java.class.path");
		String fs = System.getProperty("path.separator");
		String os = System.getProperty("os.name");

		// Common dir
		// $(PWD)/bin/jars/jdc.jar:
		// $(PWD)/bin/jars/i4jruntime.jar:
		// $(PWD)/bin/jars/javac2.jar:

		cp = cp + fs + "./jars/jdc.jar" + fs + "./jars/i4jruntime.jar" + fs + "./jars/javac2.jar";

		// Windows dir

		// windows case
		if (os.contains("Windows")) {
			// xp 64 case
			if (os.contains("64")) {
				cp = cp + fs + "./jars/windows/win32/jogl.jar" + fs + "./jars/windows/win32/xml-apis.jar" + fs + "./jars/windows/win32/resolver.jar" + fs
						+ "./Corelyzer.jar" + fs + "./jars/windows/win32/xercesImpl.jar";
			} else {
				cp = cp + fs + "./jars/windows/win32/jogl.jar" + fs + "./jars/windows/win32/xml-apis.jar" + fs + "./jars/windows/win32/resolver.jar" + fs
						+ "./Corelyzer.jar" + fs + "./jars/windows/win32/xercesImpl.jar";
			}
		}
		// mac case
		else if (os.contains("Mac OS X")) {
			cp = cp + fs + "./jars/mac/jogl.jar" + fs + "./jars/mac/xml-apis.jar" + fs + "./jars/mac/resolver.jar" + fs + "./Corelyzer.jar" + fs
					+ "./jars/mac/xercesImpl.jar" + fs + "/System/Library/Java";
		}
		// linux case
		else {
			// 64 bit case
			if (os.contains("64")) {

			} else {

			}
		}

		// build the plugin listing... add to the classpath and
		// make command line arguments
		File pluginDir = new File("../plugins");
		File[] plugins = pluginDir.listFiles(new Startup());
		String appArgs = "";

		for (File plugin : plugins) {
			cp = cp + fs + plugin.getPath();
			String[] pluginPath;
			pluginPath = plugin.getPath().replace('\\', '/').split("/");
			String pluginName = pluginPath[pluginPath.length - 1];
			pluginName = pluginName.replace(".jar", "");

			appArgs = appArgs + " " + pluginName;

		}

		try {
			System.out.println("Running:\n" + "java -cp " + cp + " corelyzer.ui.CorelyzerApp " + appArgs);
			Runtime.getRuntime().exec("java -cp " + cp + " corelyzer.ui.CorelyzerApp " + appArgs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Override accept method of FileFilter class. Only accept Java jar files in
	 * input path parameter
	 * 
	 * @param pathname
	 *            File object to be checked whether it is Java jar file
	 */

	public boolean accept(final File pathname) {
		return pathname.getPath().contains(".jar");
	}
}
