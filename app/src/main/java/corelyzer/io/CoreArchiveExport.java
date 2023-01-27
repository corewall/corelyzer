package corelyzer.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import corelyzer.data.CoreSection;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;

public class CoreArchiveExport {
    private static CorelyzerApp app = CorelyzerApp.getApp();


	private static String readFileAsString(final String filePath) throws IOException {
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[1024];
		int numRead;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

	private static void copyFile(final String src, final String dst) {
		try {
			// Create channel on the source
			FileInputStream fis = new FileInputStream(src);
			FileChannel srcChannel = fis.getChannel();

			// Create channel on the destination
			FileOutputStream fos = new FileOutputStream(dst);
			FileChannel dstChannel = fos.getChannel();

			// Copy file contents from source to destination
			dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

			// Close the channels
			srcChannel.close();
			fis.close();
			dstChannel.close();
			fos.close();
		} catch (IOException e) {
			System.err.println("--- Exception in copying files: " + src + ", " + dst + ", " + e);
			JOptionPane.showMessageDialog(app.getMainFrame(), "Copy file " + src + " failed");
		}
	}

	private static boolean deleteDir(final File dir) {
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

	// -------------------------------------------------------------------------

	public static void export(final String exportFileStr) {
        if (exportFileStr != null) {
			Thread exportThread = new Thread(new Runnable() {
				public void run() {
					final boolean result = exportTheScene(exportFileStr);
                    String resultStr = result ? "succeeded." : "failed.";
                    File f = new File(exportFileStr);
                    JOptionPane.showMessageDialog(app.getMainFrame(), "Core Archive export to file " + f.getName() + " " + resultStr);
				}
			});

			exportThread.start();
		}
	}

	private static boolean exportTheScene(final String proj_file) {
		/**
		 * Ask for prj info... <proj_id>, <where to save the proj> Build a proj
		 * dir in Cache/tmp dir copy related files w.r.t. proj_dir:
		 * <prj>/images, <prj>/data <prj>/annotations Save a state file to
		 * proj_dir Replace prefix_path to proj settings zip stuff together
		 */

        // brg 11/14/2022 Project name isn't user-facing anywhere, so we elected to drop
        // the prompt for a project name. Just use the first session's name.
        final String proj_name = CoreGraph.getInstance().getSession(0).getName();

		// Prepare project directories
		// brg 1/15/2016: Create tmpDirPath through File to ensure a directory path
		// not ending in a path separator regardless of whether app.preferences.tmp_Directory
		// does or not. A tmpDirPath ending in a path separator will result in bogus names
		// when writing to Zip
		File corelyzerTmpDir = new File(app.preferences().tmp_Directory);

		// Ensure Corelyzer's tmp dir (default is Documents/Corelyzer/Caches/tmp) exists.
		// If it doesn't, the below tmpDir.mkdir() call to create the proj_name subdir of the tmp dir
		// will fail because File.mkdir() cannot create that File's parent dir (tmp).
		if (!corelyzerTmpDir.exists()) {
			corelyzerTmpDir.mkdir();
		}

		String tmpDirPath = new File(corelyzerTmpDir, proj_name).getAbsolutePath();
        
		final String sp = System.getProperty("file.separator");
		String imgDirPath = tmpDirPath + sp + "images";
		String dataDirPath = tmpDirPath + sp + "datasets";
		String annoDirPath = tmpDirPath + sp + "annotations";
		String annoImgDirPath = annoDirPath + sp + "images";
		String annoAttachDirPath = annoDirPath + sp + "attachments";

		File tmpDir = new File(tmpDirPath);
		File imgDir = new File(imgDirPath);
		File dataDir = new File(dataDirPath);
		File annoDir = new File(annoDirPath);
		File aImgDir = new File(annoImgDirPath);
		File aAttDir = new File(annoAttachDirPath);

		if (!tmpDir.exists()) {
			tmpDir.mkdir();
		} else {
            System.out.println("Core archive export: cleaning up old tmp dir " + tmpDir.toString());
            deleteDir(tmpDir);
            tmpDir.mkdir();
		}

		if (!imgDir.exists()) {
			imgDir.mkdir();
		}
		if (!dataDir.exists()) {
			dataDir.mkdir();
		}
		if (!annoDir.exists()) {
			annoDir.mkdir();
		}
		if (!aImgDir.exists()) {
			aImgDir.mkdir();
		}
		if (!aAttDir.exists()) {
			aAttDir.mkdir();
		}

		// -- Copy related files to a tmp project dir
		// ProgressDialog progress = new ProgressDialog();
		// progress.setStatusText("");
		JProgressBar progress = app.getProgressUI();

		// Datasets
		progress.setString("Exporting datasets");
		// progress.setProgressMax(app.getDataFileListModel().size());
		// progress.setVisible(true);

		for (int i = 0; i < app.getSessionListModel().size(); i++) {
			Session s = (Session)app.getSessionListModel().elementAt(i);
			System.out.println("---- Session: " + s.getName());
			for (WellLogDataSet d : s.getDatasets()) {
				String name = d.getSourceFilename();
				File f = new File(name);
				if (f.exists()) {
					String filename = f.getName();
					System.out.println("---- Dataset f: " + i + ", " + filename);
					copyFile(name, dataDirPath + sp + filename);

					progress.setValue(i);
				}
			}
			for (TrackSceneNode t : s.getTrackSceneNodes()) {
				// Tracks & Images
				progress.setString("Exporting track and images");
				String trackName = SceneGraph.getTrackName(t.getId());
				System.out.println("---- Process Track " + trackName);

				// dig into track to find images
				int tid = t.getId();

				progress.setMaximum(t.getNumCores());
				for (int j = 0; j < t.getNumCores(); j++) {
					progress.setValue( j + 1 );

					CoreSection cs = t.getCoreSection(j);
					int csid = cs.getId();

					String name = SceneGraph.getImageName(SceneGraph.getImageIdForSection(tid, csid));

					if (name == null) {
						System.out.println("---> section: tid:" + tid + ", csid" + csid + " name is null");
						continue;
					}

					File f = new File(name);

					if (f.exists()) {
						String filename = f.getName();
						System.out.println("---- Core Image: Name: " + filename);
						copyFile(name, imgDirPath + sp + filename);
					}

					// Annotation
					int nmarkers = SceneGraph.getNumCoreSectionMarkers(tid, csid);
					System.out.println("-- Exporting " + nmarkers + " markers from" + tid + ", " + csid);
					for (int k = 0; k < nmarkers; k++) {
						// TODO get annotationMarkerId from k?!;
						String anno_name = SceneGraph.getCoreSectionMarkerLocal(tid, csid, k);

						File af = new File(anno_name);

						if (af.exists()) {
							String anno_filename = af.getName();
							System.out.println("-- Annotation name: " + anno_name);
							// this.copyFile(anno_name,
							// annoDirPath + sp + anno_filename);

							handleAnnotationAndAttachments(anno_name, annoDirPath + sp + anno_filename);
						}
					}
				}
			}
		}

		// Save session with project_name as prefix parameter to the writer
		String session_filepath = tmpDirPath + sp + "session.cml";
		StateWriter sw = new StateWriter();

		System.out.println("-- Saving Session file " + session_filepath);
		if (!sw.writeState(session_filepath, proj_name)) {
			progress.setString("Create session file failed");
			JOptionPane.showMessageDialog(app.getMainFrame(), "Create Session File Failed");
			return false;
		}

		// ZIP the project directory
		try {
			makeZip(tmpDirPath, proj_file);
		} catch (Exception e) {
			progress.setString("Create core archive failed");
			JOptionPane.showMessageDialog(app.getMainFrame(), "Creating Core Archive Failed!");
			System.err.println(e);
			return false;
		}

		// 5/24/2012 brg: Always clean up temp directory, no need to ask.
		System.out.println("Cleaning up " + tmpDir.toString());
		deleteDir(tmpDir);

		progress.setValue(0);
		progress.setString("Core archive created");

		return true;
	}

	// -------------------------------------------------------------------------

	private static void handleAnnotationAndAttachments(final String fromFile, final String toFile) {
		File srcFile = new File(fromFile);
		File dstFile = new File(toFile);

		if (srcFile.exists()) {
			String prefix = dstFile.getParent();
			String srcPrefix = srcFile.getParent();
			String sp = System.getProperty("file.separator");

			try {
				String allContentString = readFileAsString(fromFile);

				String patternStr = "(src|href)\\s*=\\s*\"([^\"]+)\"";
				Pattern pattern = Pattern.compile(patternStr);
				Matcher matcher = pattern.matcher(allContentString);

				while (matcher.find()) {
					String groupStr = matcher.group(0);
					String folderName;

					// Check target
					if (groupStr.toLowerCase().startsWith("src=")) {
						groupStr = groupStr.substring(5, groupStr.length() - 1);
						folderName = "images";
					} else if (groupStr.toLowerCase().startsWith("href=")) {
						groupStr = groupStr.substring(6, groupStr.length() - 1);
						folderName = "attachments";
					} else {
						continue;
					}

					String filepath;
					String filename;

					try {
						// Add src annotation file prefix if not in file:///
						// protocol
						if (!groupStr.trim().toLowerCase().startsWith("file:")) {
							groupStr = "file:////" + srcPrefix + sp + groupStr.trim();
						}

						URL u = new URL(groupStr);
						filepath = u.getFile();
						filename = new File(filepath).getName();

						// -- replace the string in input file
						String resString = folderName + "/" + filename;
						allContentString = allContentString.replace(groupStr, resString);

						// -- Make a copy of attachment and place it into
						// -- annotation folder
						System.out.println("-- [INFO] Copy [" + filepath + "] to " + prefix + sp + folderName + sp + filename);

						copyFile(filepath, prefix + sp + folderName + sp + filename);

					} catch (MalformedURLException e) {
						System.err.println("MalformedURL! " + e);
					}

				}

				System.out.println("------------------------------------");

				// -- Write out modified HTML file
				System.out.println("-- [INFO] Write result annotation file");
				FileWriter fw = new FileWriter(dstFile);
				fw.write(allContentString);
				fw.close();

			} catch (FileNotFoundException e) {
				System.err.println("-- [ERROR] Cannot find file!" + e);
			} catch (IOException e) {
				System.err.println("-- [ERROR] Cannot read/write file!" + e);
			}
		}
	}
	
	private static int fileCount;
	private static int countFilesInDir( final File file ) {
		if ( file.isDirectory()) {
			String[] fileNames = file.list();

			if (fileNames != null) {
				for (String fileName : fileNames) {
					countFilesInDir(new File(file, fileName));
				}
			}
		}
		else
			fileCount++;
		
		return fileCount;
	}

	public static void makeZip(final String fileName, final String zipFile) throws IOException {
		File file = new File(fileName);

		// set up determinate progress bar to give user some sense of how long
		// this operation will take
		fileCount = 0;
		final int filesInDir = countFilesInDir( file );
		JProgressBar progress = app.getProgressUI();
		progress.setValue(0);
		progress.setMaximum( filesInDir );
		progress.setString("Compressing archive files...");
		
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));

		recurseFiles(file, fileName, zos, progress);

		zos.close();
		zos = null;
	}

	private static void recurseFiles(final File file, final String prefix, ZipOutputStream zos, final JProgressBar progress) throws IOException {
		if (file.isDirectory()) {
			String[] fileNames = file.list();

			if (fileNames != null) {

				for (String fileName : fileNames) {
					recurseFiles(new File(file, fileName), prefix, zos, progress);
				}

			}
		} else {
			int curProgressValue = progress.getValue();
			progress.setValue( ++curProgressValue );
			
			byte[] buf = new byte[1024];
			int len;

			// Just want relative path in zip file
			// String sp = System.getProperty("file.separator");
			String baseName = new File(prefix).getName();
			String zipEntryString = file.toString().replace(prefix, baseName);

			ZipEntry zipEntry = new ZipEntry(zipEntryString);

			FileInputStream fin = new FileInputStream(file);
			BufferedInputStream in = new BufferedInputStream(fin);
			zos.putNextEntry(zipEntry);

			while ((len = in.read(buf)) >= 0) {
				zos.write(buf, 0, len);
			}

			in.close();
			zos.closeEntry();
		}
	}    
}
