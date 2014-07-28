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

import java.awt.Component;
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
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.jtechlabs.ui.widget.directorychooser.JDirectoryChooser;

import corelyzer.data.CRPreferences;
import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionImage;
import corelyzer.data.ImagePropertyTable;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.graphics.SceneGraph;
import corelyzer.helper.ExampleFileFilter;
import corelyzer.helper.FilenameExtensionFilter;
import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.plugin.CorelyzerPluginManager;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.LoadImageChooser;

public class FileUtility {

	final public static int LOAD = 0;
	final public static int SAVE = 1;
	
	// image file loading error codes, corresponding to those in scenegraph.cpp
	final public static int FILE_READ_ERROR = -1;
	final public static int FILE_DOES_NOT_EXIST = -2;
	final public static int FILE_IS_EMPTY = -3;

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
		return selectASingleFile( parent, title, filterStr, mode, null );
	}
	
	public static String selectASingleFile(final Window parent, final String title, final String filterStr, final int mode, final String suggestedFilename) {
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
			
			if ( suggestedFilename != null )
				dlg.setFile( suggestedFilename );

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
			
			if ( suggestedFilename != null ) {
				final File suggestedFile = new File( suggestedFilename );
				chooser.setSelectedFile( suggestedFile );
			}
			
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
	
	// 5/2/2012 brg: TODO replace the many duplicated extension stripping lines throughout the source
	// with this routine!
	static public String stripExtension(final String filename) {
		final int dotIndex = filename.lastIndexOf('.');
		if ( dotIndex >= 0 )
		{
			return filename.substring( 0, dotIndex );
		}
		
		return filename;
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
	
	// "Full Track ID" means expedition + [optional LacCore lake/year ID] + site/hole.
	// Currently only looking for IODP and LacCore-style section image naming.
	// - IODP expeditions are always numeric, LacCore are always alphabetic.
	// - IODP names have three delimiters (usually hyphens but we also look for underscores),
	// LacCore have four. (If Archive (A)/Working (W) half is indicated in the name, four
	// and five delimiters, respectively.)
	public static String parseFullTrackID(final String filename)
	{
		final String strippedFilename = stripExtension(filename);
		StringTokenizer tokenizer = new StringTokenizer(strippedFilename, "-_");

		final int tokenCount = tokenizer.countTokens();
		if ( tokenCount < 3 ) {
			System.out.println("too few tokens in " + filename + " to determine section image file naming convention");
			return null;
		}
		
		if ( tokenCount > 6 ) { // on the off-chance there's a whole mess of delimiters
			System.out.println("so many delimiters in " + filename + "! unable to determine section image file naming convention");
			return null;
		}
				
		// for now, rely only on expedition name to determine type
		final String expeditionToken = tokenizer.nextToken();
		final boolean isIODP = Character.isDigit( expeditionToken.charAt(0) );
		
		String lakeYearToken = null;
		if ( !isIODP ) // skip LacCore lake/year token
			lakeYearToken = tokenizer.nextToken();
		
		// next token indicates site and track/hole, e.g. U1363C = site U1363, track/hole C
		String siteTrackToken = tokenizer.nextToken();
		String fullTrackID = expeditionToken + "-" + ( isIODP ? "" : lakeYearToken + "-" ) + siteTrackToken;
		
		return fullTrackID;
	}

	// 4/26/2012 brg: Extracted for general image loading use: returns null if user cancels
	// out of dialog, client is responsible for checking this. (Better to have Vector<File> as an
	// output parameter and return boolean indicating dialog confirm/cancel status instead?)
	public static Vector<File> loadLocalImages(Component parent) {
		ExampleFileFilter imageFileFilter = new ExampleFileFilter();
		imageFileFilter.setDescription("Images");
		imageFileFilter.addExtension("jpg");
		imageFileFilter.addExtension("jpeg");
		imageFileFilter.addExtension("png");
		imageFileFilter.addExtension("tif");
		imageFileFilter.addExtension("tiff");
		imageFileFilter.addExtension("bmp");
		imageFileFilter.addExtension("jp2"); // jpeg2000

		// 2/2/2012 brg: Sorting filenames properly, i.e. [file1, file2, file3, file20], not [file1, file2, file20, file3],
		// is tricky. Ended up with a mixed solution: using a funky JFileChooser subclass trick on Windows to override the
		// sort algorithm, and Quaqua on Mac. (The subclass trick works on Mac too, but the dialog looks unacceptably
		// strange while Quaqua's looks very nice.)
		JFileChooser chooser = null;
		LookAndFeel oldLAF = null;
		
		final boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if ( MAC_OS_X )
		{
			// 2/2/2012 brg: On Mac, use Quaqua file chooser to sort image files
			// properly: 
			oldLAF = UIManager.getLookAndFeel();

			try {
				UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
			} catch (ClassNotFoundException cnfe) {
				try {
					UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel15");
				} catch (Exception e) {
					System.out.println("Couldn't set Quaqua LAF - Java 1.5 or 1.6 required");
				}
			} catch (Exception e) { System.out.println("Couldn't set Quaqua LAF"); }

			chooser = new JFileChooser();
		}
		else // Windows
		{
			chooser = new LoadImageChooser();
		}
		
		chooser.setDialogTitle("Load image file(s)");
		chooser.setCurrentDirectory(new File(CRPreferences.getCurrentDir()));
		chooser.resetChoosableFileFilters();
		chooser.setFileFilter(imageFileFilter);
		chooser.setMultiSelectionEnabled(true);
		int returnVal = chooser.showOpenDialog(parent);

		if ( MAC_OS_X ) // restore original look and feel
		{
			try {
				UIManager.setLookAndFeel( oldLAF );
			} catch (UnsupportedLookAndFeelException e) {
				System.out.println("Couldn't restore original LAF");
			}
		}
		
		File[] selectedFiles = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			selectedFiles = chooser.getSelectedFiles();
			if (selectedFiles.length != 0) {
				CRPreferences.setCurrentDir(selectedFiles[0].getParent());
			}
		}

		chooser.setMultiSelectionEnabled(false);
		
		Vector<File> selectedFilesVec = new Vector<File>();
		if ( selectedFiles != null )
			selectedFilesVec = new Vector<File>( Arrays.asList( selectedFiles ));

		return selectedFilesVec;
	}
	
	public static int loadImageFile( File imageFile, String URL, String sectionName, TrackSceneNode destTrack )
	{
		return loadImageFile( imageFile, URL, sectionName, destTrack, -1 /* add section to end */ );
	}
	
	public static int loadImageFile( File imageFile, String URL, String sectionName, TrackSceneNode destTrack, final int insertIndex )
	{
		// load the file, build the textures, build the model,
		// add to scene

		// 2-steps image loading snippet
		int sectionId = -1;
		int imageId;
		SceneGraph.imageLock();
		{
			final int genblockStatus = SceneGraph.genTextureBlocks( imageFile.toString() );
			if ( genblockStatus < 0 )
			{
				String msg = null;
				if (genblockStatus == FILE_READ_ERROR)
					msg = "Unknown file format: '" + imageFile + "'";
				else if (genblockStatus == FILE_DOES_NOT_EXIST)
					msg = "File could not be found: '" + imageFile + "'";
				else if (genblockStatus == FILE_IS_EMPTY)
					msg = "File contains no data: '" + imageFile + "'";
				else
					msg = "File could not be opened, unrecognized error: '" + imageFile + "'";
				
				JOptionPane.showMessageDialog(CorelyzerApp.getApp().getMainFrame(), msg);
				System.err.println("---> [INFO] Generate texture failed!");

				SceneGraph.imageUnlock();
				return -1;
			}

			SceneGraph.lock();
			{
				imageId = SceneGraph.loadImage(imageFile.toString());
			}
			SceneGraph.unlock();
		}
		SceneGraph.imageUnlock();

		// Section: place holder
		CoreSection sec = destTrack.getCoreSection(sectionName);
		if (sec == null) {
			// create and append new section
			sectionId = SceneGraph.addSectionToTrack( destTrack.getId(), destTrack.getNumCores() );

			if (sectionId != -1) {
				SceneGraph.setSectionName(destTrack.getId(), sectionId, sectionName);

				sec = new CoreSection(sectionName, sectionId);
				destTrack.addCoreSection(sec, insertIndex);
			} else {
				return -1;
			}
		}

		if (URL == null) {
			SceneGraph.setImageURL(imageId, "unknown");
		} else {
			SceneGraph.setImageURL(imageId, URL);
		}

		// Image part
		if (imageId != -1) {
			sectionId = sec.getId();
			SceneGraph.addSectionImageToTrack(destTrack.getId(), sectionId, imageId);

			CoreSectionImage node = new CoreSectionImage(destTrack, imageFile.toString(), sectionId, sectionName);
			destTrack.addChild(node, sectionId, imageId);
			destTrack.Update();
			CoreGraph.getInstance().notifyListeners();

			// broadcast the event to plugins
			final CorelyzerPluginManager pluginManager = CorelyzerApp.getApp().getPluginManager();
			String message = SceneGraph.getImageName(imageId) + "\t" + SceneGraph.getImageURL(imageId);
			pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.IMAGE_LOADED, message);

			message = destTrack.getId() + "\t" + SceneGraph.getSectionSourceImage(destTrack.getId(), sectionId);
			message += "\t" + SceneGraph.getSectionXPos(destTrack.getId(), sectionId) / SceneGraph.getCanvasDPIX(0);
			message += "\t" + SceneGraph.getSectionYPos(destTrack.getId(), sectionId) / SceneGraph.getCanvasDPIY(0);
			pluginManager.broadcastEventToPlugins(CorelyzerPluginEvent.SECTION_CREATED, message);
		}
		
		return sectionId;
	}
	
	// 5/11/2012 brg: TODO doesn't really belong in FileUtility - new utility class for "core management"?
	public static void setSectionImageProperties( TrackSceneNode track, final String sectionName, final int nativeSectionId,
			final float length, final float depth, final float dpix, final float dpiy, final String orientation )
	{
		SceneGraph.lock();
		{
			CoreSection cs = track.getCoreSection(sectionName);
			final int trackId = track.getId();

			boolean isVertical = orientation.toLowerCase().equals("vertical");
			SceneGraph.setSectionOrientation(trackId, nativeSectionId, isVertical);

			if (cs == null) {
				return;
			}

			if (cs.getDepth() == 0) {
				SceneGraph.positionSection(trackId, nativeSectionId, depth * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0), 0);
				cs.setDepth(SceneGraph.getSectionDepth(trackId, nativeSectionId));
			}

			// Determine DPI
			if ((dpix > 0) && (dpiy > 0)) { // use DPI if available
				SceneGraph.setSectionDPI(trackId, nativeSectionId, dpix, dpiy);
			} else if (length != 0) { // use Length & image size
				float dpi = ImagePropertyTable.DEFAULT_DPI;

				int imageId = SceneGraph.getImageIdForSection(trackId, nativeSectionId);

				float imageWidth = SceneGraph.getImageWidth(imageId);
				float imageHeight = SceneGraph.getImageHeight(imageId);

				float lengthInPixel;
				if (orientation.toLowerCase().equals("horizontal")) {
					lengthInPixel = imageWidth;
				} else {
					lengthInPixel = imageHeight;
				}

				dpi = (float) (lengthInPixel / (length * 100 / 2.54));

				SceneGraph.setSectionDPI(trackId, nativeSectionId, dpi, dpi);
			} else { // use default_dpi
				SceneGraph.setSectionDPI(trackId, nativeSectionId, ImagePropertyTable.DEFAULT_DPI, ImagePropertyTable.DEFAULT_DPI);
			}

			SceneGraph.bringSectionToFront(trackId, nativeSectionId);

			boolean imageLock = CorelyzerApp.getApp().preferences().lockCoreSectionImage;
			SceneGraph.setSectionMovable(trackId, nativeSectionId, !imageLock);
		}
		SceneGraph.unlock();

		CorelyzerApp.getApp().updateGLWindows();
	}
}
