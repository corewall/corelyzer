/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2008 Julian Yu-Chung Chen
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
package corelyzer.io;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.JOptionPane;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import corelyzer.controller.CRExperimentController;
import corelyzer.data.ChatGroup;
import corelyzer.data.CoreSection;
import corelyzer.data.MarkerType;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.annotation.AnnotationType;
import corelyzer.ui.annotation.AnnotationTypeDirectory;
import corelyzer.util.FileUtility;
import corelyzer.util.PropertyListUtility;

public class DISLoader {
	String DISPrefix;

	CorelyzerApp app;
	CoreGraph cg;

	final String sp = System.getProperty("file.separator");

	/*
	 * final int ABORT = 0; final int IGNORE = 1; final int SELECT = 2; final
	 * int OK = 3; private boolean isAbort = false;
	 */

	public DISLoader() {
		super();

		app = CorelyzerApp.getApp();
		cg = CoreGraph.getInstance();
	}

	private int createNativeAnnotation(final int nativeTrackId, final int nativeSectionId, final String localAnnotFile, final String annotURLStr,
			final float starting_depth, final float starting_width, final float ending_depth, final float ending_width, final int group, final int shape) {
		float dpix = SceneGraph.getCanvasDPIX(0);
		float dpiy = SceneGraph.getCanvasDPIY(0);

		float startDepth = dpix * starting_depth / 2.54f;
		float startWidth = dpiy * starting_width / 2.54f;
		float endDepth = dpix * ending_depth / 2.54f;
		float endWidth = dpiy * ending_width / 2.54f;

		float xoffset = (startDepth + endDepth) / 2.0f;
		float yoffset = (startWidth + endWidth) / 2.0f;

		xoffset += SceneGraph.getTrackXPos(nativeTrackId) + SceneGraph.getSectionXPos(nativeTrackId, nativeSectionId);
		yoffset += SceneGraph.getTrackYPos(nativeTrackId) + SceneGraph.getSectionYPos(nativeTrackId, nativeSectionId);

		int markerId = SceneGraph.createCoreSectionMarker(nativeTrackId, nativeSectionId, group, shape, xoffset, yoffset);

		if (markerId >= 0) {
			SceneGraph.setCoreSectionMarkerLocal(nativeTrackId, nativeSectionId, markerId, localAnnotFile);
			SceneGraph.setCoreSectionMarkerURL(nativeTrackId, nativeSectionId, markerId, annotURLStr);
			SceneGraph.setCoreSectionMarkerVertex(nativeTrackId, nativeSectionId, markerId, (startDepth + endDepth) / 2.0f, (startWidth + endWidth) / 2.0f,
					startDepth, startWidth, endDepth, endWidth);
		} else {
			System.err.println("Cannot create new marker, skip.");
		}

		return markerId;
	}

	private boolean generateClastAnnotation(final Element annotElement, final String sessionName, final int nativeTrackId, final int nativeSectionId) {
		System.out.println("- Generate Corelyzer Clast annotation.");

		Hashtable<String, String> annotHash = initAnnotDict("Clast");

		for (String dictStr : annotHash.keySet()) {
			annotHash.put(dictStr, annotElement.getAttribute(dictStr));
		}

		// generate a DIS annotation property using the Hashtable
		String annotName = annotHash.get("name");
		if ((annotName == null) || annotName.equals("")) {
			annotName = "clast";
		} else if (annotName.equalsIgnoreCase("n/a")) {
			annotName = "NA";
		}
		Date now = new Date();
		annotName = annotName + "-" + now.getTime();

		String repoPrefix = app.preferences().getLocalRepositoryPath();

		String localAnnotFile = repoPrefix + sp + sessionName + sp + "annotations" + sp + annotName + ".plist";
		File annotFile = new File(localAnnotFile);

		String annotURLStr;
		try {
			annotURLStr = annotFile.toURI().toURL().toString();
		} catch (MalformedURLException e) {
			System.out.println("- Malformed URL: " + annotFile);
			annotURLStr = "file:///" + annotFile.toString();
		}
		FileUtility.createDirsIfNecessary(annotFile.getParentFile());

		boolean isSaved = PropertyListUtility.saveHashtableToProperListFile(annotHash, annotFile);

		if (!isSaved) {
			System.err.println("- Saving annotation file '" + localAnnotFile + "' failed, skip");
			return false;
		}

		// create marker in native scenegraph
		createNativeAnnotation(nativeTrackId, nativeSectionId, localAnnotFile, annotURLStr, Float.parseFloat(annotHash.get("starting_depth")),
				Float.parseFloat(annotHash.get("starting_width")), Float.parseFloat(annotHash.get("ending_depth")),
				Float.parseFloat(annotHash.get("ending_width")), ChatGroup.CLAST, MarkerType.CORE_OUTLINE_MARKER);

		return true;
	}

	private boolean generateFreeformAnnotation(final Element annotElement, final String sessionName, final int nativeTrackId, final int nativeSectionId) {
		// DO NOT SUPPORT FREEFORM for now in DIS, since the limits of 256 bytes
		// per field.
		System.out.println("- [TODO] Generate Corelyzer Freeform annotation.");

		return false;
	}

	private boolean generatePropValueAnnotation(final Element annotElement, final String sessionName, final int nativeTrackId, final int nativeSectionId) {
		Hashtable<String, String> annotHash = initAnnotDict("Property Values");

		for (String dictStr : annotHash.keySet()) {
			annotHash.put(dictStr, annotElement.getAttribute(dictStr));
		}

		// generate a DIS annotation property using the Hashtable
		String annotName = annotHash.get("name");
		if ((annotName == null) || annotName.equals("")) {
			annotName = "PropValue";
		} else if (annotName.equalsIgnoreCase("n/a")) {
			annotName = "NA";
		}
		Date now = new Date();
		annotName = annotName + "-" + now.getTime();

		String repoPrefix = app.preferences().getLocalRepositoryPath();

		String localAnnotFile = repoPrefix + sp + sessionName + sp + "annotations" + sp + annotName + ".plist";
		File annotFile = new File(localAnnotFile);

		String annotURLStr;
		try {
			annotURLStr = annotFile.toURI().toURL().toString();
		} catch (MalformedURLException e) {
			System.out.println("- Malformed URL: " + annotFile);
			annotURLStr = "file:///" + annotFile.toString();
		}
		FileUtility.createDirsIfNecessary(annotFile.getParentFile());

		boolean isSaved = PropertyListUtility.saveHashtableToProperListFile(annotHash, annotFile);

		if (!isSaved) {
			System.err.println("- Saving annotation file '" + localAnnotFile + "' failed, skip");
			return false;
		}

		// create marker in native scenegraph
		createNativeAnnotation(nativeTrackId, nativeSectionId, localAnnotFile, annotURLStr, Float.parseFloat(annotHash.get("starting_depth")),
				Float.parseFloat(annotHash.get("starting_width")), Float.parseFloat(annotHash.get("ending_depth")),
				Float.parseFloat(annotHash.get("ending_width")), ChatGroup.DIS, MarkerType.CORE_OUTLINE_MARKER);

		return true;
	}

	private boolean generateSamplingAnnotation(final Element annotElement, final String sessionName, final int nativeTrackId, final int nativeSectionId) {
		System.out.println("- Genereate Corelyzer Sampling annotation.");

		Hashtable<String, String> annotHash = initAnnotDict("Sample");

		for (String dictStr : annotHash.keySet()) {
			annotHash.put(dictStr, annotElement.getAttribute(dictStr));
		}

		// generate a DIS annotation property using the Hashtable
		String annotName = annotHash.get("name");
		if ((annotName == null) || annotName.equals("")) {
			annotName = "sampling";
		} else if (annotName.equalsIgnoreCase("n/a")) {
			annotName = "NA";
		}
		Date now = new Date();
		annotName = annotName + "-" + now.getTime();

		String repoPrefix = app.preferences().getLocalRepositoryPath();

		String localAnnotFile = repoPrefix + sp + sessionName + sp + "annotations" + sp + annotName + ".plist";
		File annotFile = new File(localAnnotFile);

		String annotURLStr;
		try {
			annotURLStr = annotFile.toURI().toURL().toString();
		} catch (MalformedURLException e) {
			System.out.println("- Malformed URL: " + annotFile);
			annotURLStr = "file:///" + annotFile.toString();
		}
		FileUtility.createDirsIfNecessary(annotFile.getParentFile());

		boolean isSaved = PropertyListUtility.saveHashtableToProperListFile(annotHash, annotFile);

		if (!isSaved) {
			System.err.println("- Saving annotation file '" + localAnnotFile + "' failed, skip");
			return false;
		}

		// create marker in native scenegraph
		createNativeAnnotation(nativeTrackId, nativeSectionId, localAnnotFile, annotURLStr, Float.parseFloat(annotHash.get("starting_depth")),
				Float.parseFloat(annotHash.get("starting_width")), Float.parseFloat(annotHash.get("ending_depth")),
				Float.parseFloat(annotHash.get("ending_width")), ChatGroup.SAMPLE, MarkerType.CORE_OUTLINE_MARKER);

		return true;
	}

	private Hashtable<String, String> initAnnotDict(final String type) {
		AnnotationTypeDirectory dir = AnnotationTypeDirectory.getLocalAnnotationTypeDirectory();
		if ((dir == null) || (dir.size() <= 0)) {
			return null;
		}

		AnnotationType t = dir.getAnnotationType(type);
		if (t == null) {
			return null;
		}

		String dictFilename = t.getDictFilename();
		if ((dictFilename == null) || dictFilename.equals("")) {
			return null;
		}

		File defaultDictFile = new File("resources/annotations/default.plist");
		File dictFile = new File("resources/annotations/" + dictFilename);
		if (dictFile.exists()) {
			return PropertyListUtility.generateHashtableFromFile(dictFile);
		} else {
			if (defaultDictFile.exists()) {
				return PropertyListUtility.generateHashtableFromFile(defaultDictFile);
			} else {
				return null;
			}
		}
	}

	public boolean loadFile(final String aFilePath, final String prefix) {
		System.out.println("---> [DISLoader] Read DIS file: '" + aFilePath + "' with DIS-Prefix: '" + prefix + "'");

		File f = new File(aFilePath);
		if (!f.exists()) {
			String mesg = "DIS import file '" + aFilePath + "' doesn't exist!";
			JOptionPane.showMessageDialog(app.getMainFrame(), mesg);

			return false;
		}

		DISPrefix = prefix;

		// Do a XML check before really loading
		try {
			DOMParser parser = new DOMParser();
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);

			String fileURL = new File(aFilePath).toURI().toURL().toString();
			parser.parse(fileURL);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(app.getMainFrame(), "DIS import file format error.\n" + e);
			e.printStackTrace();

			return false;
		}

		try {
			DOMParser parser = new DOMParser();
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);
			parser.parse(aFilePath);
			Document doc = parser.getDocument();

			// The first element should be EXPEDITION
			Element e = doc.getDocumentElement();

			if (!e.getNodeName().equalsIgnoreCase("expedition")) {
				String mesg = "The first element is not EXPEDITION! '" + e.getNodeName();
				JOptionPane.showMessageDialog(app.getMainFrame(), mesg);

				return false;
			}

			String expName = e.getAttribute("name");
			String expId = e.getAttribute("id");

			System.out.println("EXP> name: " + expName + ", id: " + expId);

			// site
			NodeList siteList = e.getChildNodes();
			for (int i = 0; i < siteList.getLength(); ++i) {
				if (!(siteList.item(i) instanceof Element)) {
					continue;
				}

				Element siteElement = (Element) siteList.item(i);

				if (!siteElement.getNodeName().equalsIgnoreCase("site")) {
					continue;
				}

				String siteName = siteElement.getAttribute("name");
				String siteId = siteElement.getAttribute("id");

				System.out.println("SITE> name: " + siteName + ", id: " + siteId);

				// local naming
				String sessionName = expName + "_" + siteName;
				String sessionId = expId + "_" + siteId;

				Session s = new Session(sessionName);
				s.setDISId(sessionId);
				cg.addSession(s);

				// hole
				NodeList holeList = siteElement.getChildNodes();
				for (int j = 0; j < holeList.getLength(); ++j) {
					if (!(holeList.item(j) instanceof Element)) {
						continue;
					}

					Element holeElement = (Element) holeList.item(j);

					if (!holeElement.getNodeName().equalsIgnoreCase("hole")) {
						continue;
					}

					String holeName = holeElement.getAttribute("name");
					String holeId = holeElement.getAttribute("id");

					System.out.println("HOLE> name: " + holeName + ", id: " + holeId);

					// core
					NodeList coreList = holeElement.getChildNodes();
					for (int k = 0; k < coreList.getLength(); ++k) {
						if (!(coreList.item(k) instanceof Element)) {
							continue;
						}

						Element coreElement = (Element) coreList.item(k);

						if (!coreElement.getNodeName().equalsIgnoreCase("core")) {
							continue;
						}

						String coreName = coreElement.getAttribute("name");
						String coreId = coreElement.getAttribute("id");
						String coreTopDepth = coreElement.getAttribute("top_depth");
						String coreMcdDepth = coreElement.getAttribute("mcd_depth");
						String coreLength = coreElement.getAttribute("length");

						System.out.println("CORE> name: " + coreName + ", id: " + coreId + ", topDepth: " + coreTopDepth + ", mcd_depth: " + coreMcdDepth
								+ ", length: " + coreLength);

						// local naming
						String trackName = holeName + "_" + coreName;
						String trackId = holeId + "_" + coreId;

						int nativeTrackId = app.createTrack(trackName);
						TrackSceneNode t = s.getTrackSceneNodeWithTrackId(nativeTrackId);
						t.setDISId(trackId);
						t.setTopDepth(Float.parseFloat(coreTopDepth));
						t.setMCDDepth(Float.parseFloat(coreMcdDepth));
						t.setLength(Float.parseFloat(coreLength));

						// section
						NodeList sectionList = coreElement.getChildNodes();
						for (int l = 0; l < sectionList.getLength(); ++l) {
							if (!(sectionList.item(l) instanceof Element)) {
								continue;
							}

							Element sectionElement = (Element) sectionList.item(l);
							if (!sectionElement.getNodeName().equalsIgnoreCase("section")) {
								continue;
							}

							String sectionName = sectionElement.getAttribute("name");
							String sectionId = sectionElement.getAttribute("id");
							String sectionTopDepth = sectionElement.getAttribute("top_depth");
							String sectionMcdDepth = sectionElement.getAttribute("mcd_depth");
							String sectionLength = sectionElement.getAttribute("length");

							System.out.println("SECTION> name: " + sectionName + ", id: " + sectionId + ", topDepth: " + sectionTopDepth + ", mcdDepth: "
									+ sectionMcdDepth + ", length: " + sectionLength);

							// image
							NodeList imageList = sectionElement.getChildNodes();
							for (int m = 0; m < imageList.getLength(); ++m) {
								if (!(imageList.item(m) instanceof Element)) {
									continue;
								}

								Element imageElement = (Element) imageList.item(m);

								if (!imageElement.getNodeName().equalsIgnoreCase("image")) {
									continue;
								}

								String imageName = imageElement.getAttribute("name");
								String imageId = imageElement.getAttribute("id");
								String imageUrl = imageElement.getAttribute("url");

								System.out.println("IMAGE> name: " + imageName + ", id: " + imageId + ", url: " + imageUrl);

								// If imageUrl using protocol file://, replace
								// with dis.prefix + relative file path
								if (imageUrl.toLowerCase().startsWith("file")) {
									System.out.println("imageUrl is: " + imageUrl);

									URL u;
									try {
										u = new URL(imageUrl);
										String relativePath = u.getPath();
										String fff = u.getFile();

										System.out.println("[URL OK] the path is: " + relativePath);
										System.out.println("[URL OK] the file is: " + fff);

										// replace with local prefix + pathos.name
										boolean WIN = System.getProperty("os.name").toLowerCase().contains("win");
										if (WIN) { // assume Windows only
											// "FILE://C:\DIS\Images\keke.jpg"
											imageUrl = "file:///" + app.preferences().getProperty("dis.prefix") + sp + imageUrl.substring(10);

											System.out.println("[DISLoader] !Mac's imageUrl: " + imageUrl);
										} else {
                                                                                    
                                                                                }
									} catch (MalformedURLException ex) {
										// might be the case of
										// "FILE://C:\DIS\Images\keke.jpg" in
										// UNIX-like OS
										String path = imageUrl.substring(7);
										System.out.println("[MalformedURL] pathString: " + path);

										// check if from Windows with disk drive
										// ID
										if (imageUrl.substring(8, 9).equalsIgnoreCase(":")) {
											String relativePath = imageUrl.substring(10);
											relativePath = relativePath.replace("\\", sp);
											System.out.println("[MalformedURL] relativePath: " + relativePath);

											imageUrl = "file://" + app.preferences().getProperty("dis.prefix") + sp + relativePath;

											File localFile = new File(imageUrl);
											System.out.println("[MalformedURL] if local file exists? " + localFile.exists());
											System.out.println("[MalformedURL] Updated imageUrl: " + imageUrl);
										}
									}
								}

								// Load image
								CRExperimentController.loadSectionImageWithURL(imageUrl, null, sessionName, trackName, Float.parseFloat(sectionTopDepth));

								int dotPos = imageName.lastIndexOf(".");
								String csName = imageName.substring(0, dotPos);

								CoreSection cs = t.getCoreSection(csName);

								if (cs == null) {
									System.err.println("[DISLoader] Image: '" + imageUrl + "' is not loaded, continue");
									continue;
								}
								t.removeCoreSection(cs);

								int nativeSectionId = cs.getId();

								cs.setDISId(sectionId + "_" + imageId);
								cs.setName(sectionName);
								cs.setDepth(Float.parseFloat(sectionTopDepth));
								cs.setMCDDepth(Float.parseFloat(sectionMcdDepth));
								cs.setLength(Float.parseFloat(sectionLength));
								t.addCoreSection(cs);

								// Assign DPI
								float length = Float.parseFloat(sectionLength);
								int nativeImageId = SceneGraph.getImageIdForSection(nativeTrackId, nativeSectionId);

								if (nativeImageId >= 0) {
									// Images in DIS are always in PORTRAIT mode
									SceneGraph.setSectionOrientation(nativeTrackId, nativeSectionId, SceneGraph.PORTRAIT);

									int imageHeight = SceneGraph.getImageHeight(nativeImageId);
									float dpi = imageHeight / (length * 100.0f / 2.54f);
									SceneGraph.setSectionDPI(nativeTrackId, nativeSectionId, dpi, dpi);
								}

								// Annotation
								NodeList annotList = imageElement.getChildNodes();
								for (int n = 0; n < annotList.getLength(); ++n) {
									if (!(annotList.item(n) instanceof Element)) {
										continue;
									}

									Element annotElement = (Element) annotList.item(n);
									if (!annotElement.getNodeName().equalsIgnoreCase("annotation")) {
										continue;
									}

									String annotClass = annotElement.getAttribute("anno_class");
									if (annotClass.equalsIgnoreCase("clr_propvalue")) {
										generatePropValueAnnotation(annotElement, sessionName, nativeTrackId, nativeSectionId);
									} else if (annotClass.equalsIgnoreCase("clr_sampling")) {
										generateSamplingAnnotation(annotElement, sessionName, nativeTrackId, nativeSectionId);
									} else if (annotClass.equalsIgnoreCase("clr_clast")) {
										generateClastAnnotation(annotElement, sessionName, nativeTrackId, nativeSectionId);
									} else if (annotClass.equalsIgnoreCase("clr_freeform")) {
										generateFreeformAnnotation(annotElement, sessionName, nativeTrackId, nativeSectionId);
									} else {
										System.out.println("- Unrecognized anno_class: '" + annotClass + "'");
										generatePropValueAnnotation(annotElement, sessionName, nativeTrackId, nativeSectionId);
									}
								} // n: annotation
							} // m: image
						} // l: section
					} // k: core
				} // j: hole
			} // i: site
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}

		return true;
	}
}
