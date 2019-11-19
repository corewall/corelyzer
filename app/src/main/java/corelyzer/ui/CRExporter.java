// Deprecated
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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import corelyzer.data.CRPreferences;
import corelyzer.data.CoreSection;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.WellLogDataSet;
import corelyzer.graphics.SceneGraph;
import corelyzer.helper.ExampleFileFilter;
import corelyzer.io.StateWriter;

/** corelyzer.ui.CRExporter to export the whole scene as a project package */
public class CRExporter extends JDialog implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5425569409881136566L;

	private static String readFileAsString(final String filePath) throws java.io.IOException {
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

	CorelyzerApp app;
	JTextField field_projectname;

	JTextField field_packagelocation;
	JButton selDirBtn;
	JButton exportBtn;

	JButton cancelBtn;

	ZipOutputStream zos;

	public CRExporter() {
		super();
		setupUI();
	}

	public CRExporter(final CorelyzerApp a) {
		this();
		this.app = a;

		this.setLocationRelativeTo(app.getMainFrame());
	}

	// -------------------------------------------------------------------------

	public void actionPerformed(final ActionEvent e) {
		Object actionSource = e.getSource();

		if (actionSource.equals(this.cancelBtn)) {
			this.dispose();
		} else if (actionSource.equals(this.exportBtn)) {
			Thread exportThread = new Thread(new Runnable() {

				public void run() {
					exportAction();
				}
			});

			exportThread.start();
			this.dispose();
		} else if (actionSource.equals(this.selDirBtn)) {
			// String dataRoot = System.getProperty("corelyzer.data.path");
			String dataRoot = this.app.preferences.datastore_Directory;
			System.out.println("-- Dataroot: " + dataRoot);
			JFileChooser chooser = new JFileChooser(CRPreferences.getCurrentDir());

			ExampleFileFilter zipFilter = new ExampleFileFilter("car", "Core ARchive files");

			chooser.setFileFilter(zipFilter);
			chooser.setDialogTitle("Export Session Package");
			int returnVal = chooser.showSaveDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File selectedFile = chooser.getSelectedFile().getAbsoluteFile();

				CRPreferences.setCurrentDir(selectedFile.getParent());

				// make sure it has .car at the end
				String path = selectedFile.getAbsolutePath();
				path = path.replace('\\', '/');
				String[] toks = path.split("/");

				if (!toks[toks.length - 1].contains(".car")) {
					path = path + ".car";
				}

				this.field_packagelocation.setText(path);
			}

		} else {
			System.out.println("nothing happened");
		}

	}

	private void copyFile(final String src, final String dst) {
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
			JOptionPane.showMessageDialog(this, "Copy file " + src + " failed");
		}
	}

	private boolean deleteDir(final File dir) {
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

	private void exportAction() {
		if (this.field_projectname.getText() == null || this.field_projectname.getText().equals("")) {
			JOptionPane.showMessageDialog(this, "Please input project name");
			return;
		}

		String result;

		if (exportTheScene()) {
			result = "SUCCESS";
		} else {
			result = "FAILED";
		}

		File f = new File(this.field_packagelocation.getText());
		String proj_name = this.field_projectname.getText();

		JOptionPane.showMessageDialog(app.getMainFrame(), "Export Project " + proj_name + " to Core ARchive file " + f.getName() + " : " + result);
		this.dispose();
	}

	// -------------------------------------------------------------------------

	private boolean exportTheScene() {
		/**
		 * Ask for prj info... <proj_id>, <where to save the proj> Build a proj
		 * dir in Cache/tmp dir copy related files w.r.t. proj_dir:
		 * <prj>/images, <prj>/data <prj>/annotations Save a state file to
		 * proj_dir Replace prefix_path to proj settings zip stuff together
		 */
		String proj_name = this.field_projectname.getText();
		String proj_file = this.field_packagelocation.getText();

		Object[] options = { "No", "Yes" };
		String sp = System.getProperty("file.separator");

		// -- Prepare project directories
		String tmpDirPath = app.preferences.tmp_Directory + sp + proj_name;
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
		} else { // already has the dir in tmp_dir
			int cleanfirst = JOptionPane.showOptionDialog(this, "Clean up old project dir " + "in temp before continue?", "Clean Confirmation",
					JOptionPane.YES_NO_OPTION, JOptionPane.CANCEL_OPTION, null, options, options[1]);

			if (cleanfirst == 1) {
				System.out.println("Cleaning up " + tmpDir.toString());
				this.deleteDir(tmpDir);
				tmpDir.mkdir();
			}
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

		for (int i = 0; i < app.getDataFileListModel().size(); i++) {
			WellLogDataSet d = (WellLogDataSet) app.getDataFileListModel().elementAt(i);

			String name = d.getSourceFilename();

			File f = new File(name);

			if (f.exists()) {
				String filename = f.getName();
				System.out.println("---- Dataset f: " + i + ", " + filename);
				this.copyFile(name, dataDirPath + sp + filename);

				progress.setValue(i);
			}
		}

		// Tracks & Images
		progress.setString("Exporting track and images");

		for (int i = 0; i < app.getTrackListModel().size(); i++) {
			TrackSceneNode t = (TrackSceneNode) app.getTrackListModel().elementAt(i);

			String trackName = SceneGraph.getTrackName(t.getId());
			System.out.println("---- Process Track " + trackName);

			// dig into track to find images
			int tid = t.getId();

			progress.setMaximum(t.getNumCores());
			progress.setValue(0);
			for (int j = 0; j < t.getNumCores(); j++) {

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
					this.copyFile(name, imgDirPath + sp + filename);
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

						this.handleAnnotationAndAttachments(anno_name, annoDirPath + sp + anno_filename);
					}
				}

				progress.setValue(j);
			}
		}

		// Save session with project_name as prefix parameter to the writer
		String session_filepath = tmpDirPath + sp + "session.cml";
		StateWriter sw = new StateWriter();

		System.out.println("-- Saving Session file " + session_filepath);
		if (!sw.writeState(session_filepath, proj_name)) {
			// progress.dispose();
			progress.setString("Create session file failed");
			JOptionPane.showMessageDialog(this, "Create Session File Failed");
			return false;
		}

		// ZIP the project directory
		progress.setValue(0);
		progress.setString("Making Core ARchive file");

		try {
			progress.setIndeterminate(true);
			progress.setString("Compressing core archive...");
			makeZip(tmpDirPath, proj_file);
			progress.setIndeterminate(false);
		} catch (Exception e) {
			// progress.dispose();
			progress.setString("Create core archive failed");
			JOptionPane.showMessageDialog(this, "Creating Core ARchive Failed!");
			System.err.println(e);
			return false;
		}

		// clean up tmp directory?
		int sel = JOptionPane.showOptionDialog(this, "Do you want to clean tmp_dir?", "Clean Confirmation", JOptionPane.YES_NO_OPTION,
				JOptionPane.CANCEL_OPTION, null, options, options[1]);

		if (sel == 1) {
			System.out.println("Cleaning up " + tmpDir.toString());
			this.deleteDir(tmpDir);
		}

		// progress.dispose();
		progress.setValue(0);
		progress.setString("Core archive created");

		return true;
	}

	private void handleAnnotationAndAttachments(final String fromFile, final String toFile) {
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

						this.copyFile(filepath, prefix + sp + folderName + sp + filename);

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

	// -------------------------------------------------------------------------

	public void makeZip(final String fileName, final String zipFile) throws IOException {
		File file = new File(fileName);
		zos = new ZipOutputStream(new FileOutputStream(zipFile));

		recurseFiles(file, fileName);

		zos.close();
		zos = null;
	}

	private void recurseFiles(final File file, final String prefix) throws IOException {
		if (file.isDirectory()) {
			String[] fileNames = file.list();

			if (fileNames != null) {

				for (String fileName : fileNames) {
					recurseFiles(new File(file, fileName), prefix);
				}

			}
		} else {
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

	// -------------------------------------------------------------------------

	private void setupUI() {
		setTitle("Corelyzer Exporter");
		setSize(640, 480);
		setResizable(false);

		this.setLocationRelativeTo(null);
		this.setLayout(new BorderLayout());

		JPanel p = new JPanel(new FlowLayout());
		p.setBorder(BorderFactory.createTitledBorder("Corelyzer Project Exporter"));

		JLabel label = new JLabel("Project Name: ");
		p.add(label);

		field_projectname = new JTextField(32);
		p.add(field_projectname);

		label = new JLabel("Project Package Location: ");
		p.add(label);

		selDirBtn = new JButton("Select file...");
		selDirBtn.addActionListener(this);
		p.add(selDirBtn);

		field_packagelocation = new JTextField(32);
		field_packagelocation.setEditable(false);
		p.add(field_packagelocation);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(this);
		btnPanel.add(cancelBtn);

		exportBtn = new JButton("Export");
		exportBtn.addActionListener(this);
		btnPanel.add(exportBtn);

		this.add(p, BorderLayout.CENTER);
		this.add(btnPanel, BorderLayout.SOUTH);

		try {
			setAlwaysOnTop(true);
		} catch (SecurityException e) {
			System.out.println(e);
			System.out.println("Could not set corelyzer.ui.CRExporter to always be front");
		}
	}
}
