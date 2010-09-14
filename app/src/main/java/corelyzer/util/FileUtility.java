/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004 - 2007 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
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

package corelyzer.util;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFileChooser;

import com.jtechlabs.ui.widget.directorychooser.JDirectoryChooser;

import corelyzer.data.CRPreferences;
import corelyzer.helper.ExampleFileFilter;
import corelyzer.helper.FilenameExtensionFilter;
import corelyzer.ui.CorelyzerApp;

public class FileUtility {

	final public static int LOAD = 0;

	final public static int SAVE = 1;

	static public boolean copyFile(final String src, final String dst) {
		try {
			// Create channel on the source
			FileChannel srcChannel = new FileInputStream(src).getChannel();

			// Create channel on the destination
			FileChannel dstChannel = new FileOutputStream(dst).getChannel();

			// Copy file contents from source to destination
			dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

			// Close the channels
			srcChannel.close();
			dstChannel.close();

			return true;
		} catch (IOException e) {
			System.err.println("---> Exception in copying files: " + src + ", " + dst + e);

			return false;
		}
	}

	//
	public static void createDirsIfNecessary(final File aDir) {
		if (aDir.exists()) {
			return;
		}

		File parent = aDir.getParentFile();
		if (!parent.exists()) {
			createDirsIfNecessary(parent);
		}

		aDir.mkdir();
	}

	// --------------------------------------------------------------------------
	// Reference: Java Developers Alamanac Example
	// http://javaalmanac.com/egs/java.io/DeleteDir.html
	static public boolean deleteDir(final File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (String aChildren : children) {
				boolean success = deleteDir(new File(dir, aChildren));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}

	static public boolean deleteDirectory(final String fullDirPath) {
		String sp = System.getProperty("file.separator");
		File texDir = new File(fullDirPath);

		if (texDir.exists() && texDir.isDirectory()) {
			String[] dirList = texDir.list();

			for (String aDirList : dirList) {
				String myDirPath = texDir.getAbsolutePath() + sp + aDirList;
				File d = new File(myDirPath);

				if (d.exists() && d.isDirectory()) {
					System.out.print("Delete " + d.getAbsolutePath());
					boolean isSuccess = deleteDir(d);
					String result = isSuccess ? "SUCCESS" : "FAILED";
					System.out.print(" : " + result + "\n");
				}
			}

			texDir.delete();
		}

		return false;
	}

	public static String getExtension(final File f) {
		if (f != null) {
			String filename = f.getName();
			int i = filename.lastIndexOf('.');
			if (i > 0 && i < filename.length() - 1) {
				return filename.substring(i + 1).toLowerCase();
			}
		}

		return null;
	}

	public static String getFileContentAsAString(final File aFile) throws IOException {
		FileInputStream fis = new FileInputStream(aFile);
		int x = fis.available();
		byte b[] = new byte[x];
		fis.read(b);
		return new String(b);
	}

	public static String getFileExtension(final String filename) {
		String ext;

		int dotPlace = filename.lastIndexOf('.');

		if (dotPlace >= 0) {
			ext = filename.substring(dotPlace + 1);
		} else {
			ext = "";
		}

		return ext.toLowerCase();
	}

	public static String normalizeFilename(final String filePathString, final int length) {
		String filenameString = new File(filePathString).getName();

		if (filenameString.length() > length) {
			String extension = getExtension(new File(filePathString));
			int endIndex = length * 2 / 3;
			filenameString = filenameString.substring(0, endIndex) + "..." + extension;
		}

		return filenameString;
	}

	public static void saveAStringAsAFile(final String aString, final File aFile) throws IOException {
		FileOutputStream fStream = new FileOutputStream(aFile);
		fStream.write(aString.getBytes());
		fStream.close();
	}

	public static String selectADirectory(final Window parent, final String title) {
		File f = new File(CRPreferences.getCurrentDir());
		if ((f = JDirectoryChooser.showDialog(parent, f)) != null) {
			return f.getAbsolutePath();
		}

		return null;
	}

	public static String selectASingleFile(final Window parent, final String title, final String filterStr, final int mode) {
		setAlwaysOnTop(false, parent);

		boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

		if (MAC_OS_X) {
			FileDialog dlg;

			if (parent instanceof Dialog) {
				dlg = new FileDialog((Dialog) parent, title);
			} else if (parent instanceof Frame) {
				dlg = new FileDialog((Frame) parent, title);
			} else {
				dlg = new FileDialog(new Frame(), title);
			}
			dlg.setAlwaysOnTop(true);
			dlg.setModal(true);

			if (mode == FileUtility.LOAD) {
				dlg.setMode(FileDialog.LOAD);
			} else {
				dlg.setMode(FileDialog.SAVE);
			}

			dlg.setDirectory(CRPreferences.getCurrentDir());

			if (filterStr != null) {
				FilenameExtensionFilter filter = new FilenameExtensionFilter(filterStr);

				dlg.setFilenameFilter(filter);
			}

			// parent.setAlwaysOnTop(false);
			dlg.pack();
			dlg.setVisible(true);

			String directory = dlg.getDirectory();
			CRPreferences.setCurrentDir(directory);

			String filename = dlg.getFile();
			if (filename != null && mode == FileUtility.SAVE && filterStr != null && !filename.toLowerCase().endsWith(filterStr.toLowerCase())) {
				filename = filename + "." + filterStr;
			}

			if (directory == null || filename == null) {
				setAlwaysOnTop(true, parent);
				return null;
			}

			CRPreferences.setCurrentDir(directory);
			dlg.dispose();

			File f = new File(directory + filename);

			setAlwaysOnTop(true, parent);
			return f.getAbsolutePath();
		} else {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File(CRPreferences.getCurrentDir()));
			chooser.setDialogTitle(title);
			chooser.resetChoosableFileFilters();

			if (mode == FileUtility.SAVE) {
				chooser.setDialogType(JFileChooser.SAVE_DIALOG);
			} else {
				chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			}

			if (filterStr != null) {
				ExampleFileFilter filter = new ExampleFileFilter(filterStr);
				chooser.setFileFilter(filter);
			}

			int returnVal;

			if (mode == FileUtility.SAVE) {
				returnVal = chooser.showSaveDialog(parent);
			} else {
				returnVal = chooser.showOpenDialog(parent);
			}

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File selectedFile = chooser.getSelectedFile();
				CRPreferences.setCurrentDir(selectedFile.getParent());

				String filePath = selectedFile.getAbsolutePath();
				if (mode == FileUtility.SAVE && filterStr != null && !filePath.toLowerCase().endsWith(filterStr.toLowerCase())) {
					filePath = filePath + "." + filterStr;
				}

				setAlwaysOnTop(true, parent);
				return filePath;
			}
		}

		setAlwaysOnTop(true, parent);
		return null;
	}

	public static String[] selectMultipleFiles(final Window parent, final String title, final String filterStr) {

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(CRPreferences.getCurrentDir()));
		chooser.setDialogTitle(title);
		chooser.resetChoosableFileFilters();
		chooser.setMultiSelectionEnabled(true);

		if (filterStr != null) {
			ExampleFileFilter filter = new ExampleFileFilter(filterStr);
			chooser.setFileFilter(filter);
		}

		int returnVal = chooser.showOpenDialog(parent);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File[] selectedFiles = chooser.getSelectedFiles();
			if (selectedFiles.length <= 0) {
				return null;
			}

			Vector<String> fileStrs = new Vector<String>();
			String[] strs = new String[0];

			for (File f : selectedFiles) {
				fileStrs.add(f.getAbsolutePath());
			}

			CRPreferences.setCurrentDir(selectedFiles[0].getParent());

			return fileStrs.toArray(strs);
		}

		return null;
	}

	public static String selectSingleFile(final Window parent, final String title, final String filterStr, final int mode) {

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(CRPreferences.getCurrentDir()));
		chooser.setDialogTitle(title);
		chooser.resetChoosableFileFilters();
		chooser.setMultiSelectionEnabled(false);

		if (filterStr != null) {
			ExampleFileFilter filter = new ExampleFileFilter(filterStr);
			chooser.setFileFilter(filter);
		}

		int returnVal;
		if (mode == FileUtility.LOAD) {
			returnVal = chooser.showOpenDialog(parent);
		} else if (mode == FileUtility.SAVE) {
			returnVal = chooser.showSaveDialog(parent);
		} else {
			returnVal = JFileChooser.CANCEL_OPTION;
		}

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = chooser.getSelectedFile();

			if (selectedFile == null) {
				return null;
			} else {
				CRPreferences.setCurrentDir(selectedFile.getParent());

				return selectedFile.getAbsolutePath();
			}
		}

		return null;
	}

	// Utilities with UIs
	private static void setAlwaysOnTop(final boolean isOnTop, final Window parent) {
		CorelyzerApp app = CorelyzerApp.getApp();

		if (app == null) {
			return;
		}

		app.getMainFrame().setAlwaysOnTop(isOnTop);

		parent.setAlwaysOnTop(isOnTop);
		parent.requestFocus();
	}

	static public void showFileInFinder(final File aFile) {
		final boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

		try {
			if (MAC_OS_X) {
				String[] params = new String[] { "osascript", "-e", "set p to \"" + aFile.getCanonicalPath() + "\"", "-e", "tell application \"Finder\"", "-e",
						"reveal (POSIX file p) as alias", "-e", "activate", "-e", "end tell", };

				Runtime.getRuntime().exec(params);
			} else {
				// how to do this in Windows?
			}
		} catch (IOException ioe) {
			System.err.println("Runtime exec in FileUtility#showFileInFinder: " + ioe);
		}
	}

	static public void unzip(final File zipFile, final String targetDir) {
		boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		int buf_size = 2048;

		try {
			BufferedOutputStream dest; // = null;
			FileInputStream fis = new FileInputStream(zipFile);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));

			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				System.out.println("-- Extracting: " + entry);

				int count;
				byte data[] = new byte[buf_size];

				// write the files to the disk
				String sp = System.getProperty("file.separator");
				String entryName = entry.getName();

				if (MAC_OS_X || System.getProperty("os.name").equalsIgnoreCase("linux")) {
					entryName = entry.getName().replaceAll("\\\\|/", sp);
				}

				System.out.println("EntryName: " + entryName);
				String targetFileName = targetDir + sp + entryName;
				FileOutputStream fos = new FileOutputStream(targetFileName);
				dest = new BufferedOutputStream(fos, buf_size);
				while ((count = zis.read(data, 0, buf_size)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
