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
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.Map;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;

import corelyzer.data.CoreSection;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;
import corelyzer.helper.URLRetrieval;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.annotation.AnnotationUtils;
import corelyzer.util.PropertyListUtility;

public class DISWriter {
	CorelyzerApp app;
	CoreGraph cg;

	// canvas dpi to convert pixel to cm
	float canvas_dpix;
	float canvas_dpiy;

	public boolean writeFile(final String aFile) {
		System.out.println("---> [DISWriter] Save a DIS export file: '" + aFile + "'");

		if (aFile == null || aFile.equals("")) {
			return false;
		}

		app = CorelyzerApp.getApp();
		cg = CoreGraph.getInstance();

		canvas_dpix = SceneGraph.getCanvasDPIX(0);
		canvas_dpiy = SceneGraph.getCanvasDPIY(0);

		DocumentImpl doc;
		Element root = null;

		try {
			doc = new DocumentImpl();

			// = mapping =
			// session: expedition_site
			// track: hole_core
			// section: section&image

			String expeditionId;
			String expeditionName = "";
			boolean foundExp = false;

			for (Session s : cg.getSessions()) {
				String sessionName = s.getName();
				String sessionId = s.getDISId();

				String[] nameToks = sessionName.split("_");
				String[] idToks = sessionId.split("_");

				if (nameToks.length < 2 || idToks.length < 2) {
					System.out.println("short length: " + nameToks.length + ", " + idToks.length);
					continue;
				}

				if (!foundExp) {
					expeditionName = nameToks[0];
					expeditionId = idToks[0];

					System.out.println("expName:id = " + expeditionName + "," + expeditionId);

					root = doc.createElement("EXPEDITION");
					root.setAttributeNS(null, "id", expeditionId);
					root.setAttributeNS(null, "name", expeditionName);

					doc.appendChild(root);

					foundExp = true;
				}

				// ignore sites that not having the same expedition
				if (foundExp && !expeditionName.equals(nameToks[0])) {
					System.out.println("Ignore not the same expedition: " + nameToks[0]);

					continue;
				}

				System.out.println("siteName:id = " + nameToks[1] + ", " + idToks[1]);

				Element siteElement = doc.createElement("SITE");
				siteElement.setAttributeNS(null, "id", idToks[1]);
				siteElement.setAttributeNS(null, "name", nameToks[1]);

				if (root != null) {
					root.appendChild(siteElement);
				}

				// for-loop-in-track
				Hashtable<String, Element> holeMap = new Hashtable<String, Element>();
				for (TrackSceneNode t : s.getTrackSceneNodes()) {
					String[] holeCoreIdToks = t.getDISId().split("_");
					String[] holeCoreNameToks = t.getName().split("_");

					if (holeCoreIdToks.length < 2 || holeCoreNameToks.length < 2) {
						System.out.println("Invalid track token length: " + holeCoreNameToks.length + ", " + holeCoreIdToks.length);
						continue;
					}

					Element holeElement = holeMap.get(holeCoreNameToks[0]);
					if (holeElement == null) {
						holeElement = doc.createElement("HOLE");
						holeElement.setAttributeNS(null, "id", holeCoreIdToks[0]);
						holeElement.setAttributeNS(null, "name", holeCoreNameToks[0]);

						siteElement.appendChild(holeElement);
						holeMap.put(holeCoreNameToks[0], holeElement);
					}

					Element coreElement = doc.createElement("CORE");
					coreElement.setAttributeNS(null, "id", holeCoreIdToks[1]);
					coreElement.setAttributeNS(null, "name", holeCoreNameToks[1]);

					coreElement.setAttributeNS(null, "top_depth", "" + t.getTopDepth());
					coreElement.setAttributeNS(null, "mcd_depth", "" + t.getMCDDepth());
					coreElement.setAttributeNS(null, "length", "" + t.getLength());

					holeElement.appendChild(coreElement);

					// for-loop-in-sections
					for (CoreSection cs : t.getCoreSections()) {
						System.out.println("CoreSection " + cs + " id: " + cs.getDISId());

						String[] sectionImageIdToks = cs.getDISId().split("_");
						String sectionName = cs.getName();

						if (sectionImageIdToks.length < 2) {
							System.out.println("Invalid section token length: " + sectionImageIdToks.length);

							continue;
						}

						// Client should not modify recovery data
						float depth = cs.getDepth();
						float sectionMcdDepth = cs.getMCDDepth();

						float length = cs.getLength();
						if (length < 0) {
							boolean orient = SceneGraph.getSectionOrientation(t.getId(), cs.getId());

							if (SceneGraph.LANDSCAPE == orient) {
								length = SceneGraph.getSectionLength(t.getId(), cs.getId()) / 100.0f;
							} else {
								length = SceneGraph.getSectionHeight(t.getId(), cs.getId()) / 100.f;
							}
						}

						int imageId = SceneGraph.getImageIdForSection(t.getId(), cs.getId());
						String url = SceneGraph.getImageURL(imageId);

						String[] toks = url.split("/");
						String filename = toks[toks.length - 1];

						Element sectionElement = doc.createElement("SECTION");
						sectionElement.setAttributeNS(null, "id", sectionImageIdToks[0]);
						sectionElement.setAttributeNS(null, "name", sectionName);
						sectionElement.setAttributeNS(null, "top_depth", String.valueOf(depth));
						sectionElement.setAttributeNS(null, "mcd_depth", String.valueOf(sectionMcdDepth));
						sectionElement.setAttributeNS(null, "length", String.valueOf(length));

						Element imageElement = doc.createElement("IMAGE");
						imageElement.setAttributeNS(null, "id", sectionImageIdToks[1]);
						imageElement.setAttributeNS(null, "name", filename);
						imageElement.setAttributeNS(null, "url", url);

						sectionElement.appendChild(imageElement);
						coreElement.appendChild(sectionElement);

						// Annotations, numberOfAnnotations might contain NULL
						// elements. Need to check for null.
						int numberOfAnnotations = SceneGraph.getNumCoreSectionMarkers(t.getId(), cs.getId());
						for (int i = 0; i < numberOfAnnotations; ++i) {
							Element annotationElement = doc.createElement("ANNOTATION");

							float[] annotArea = AnnotationUtils.getAnnotationArea(t.getId(), cs.getId(), i);

							annotationElement.setAttributeNS(null, "starting_depth", "" + annotArea[0]);
							annotationElement.setAttributeNS(null, "starting_width", "" + annotArea[1]);
							annotationElement.setAttributeNS(null, "ending_depth", "" + annotArea[2]);
							annotationElement.setAttributeNS(null, "ending_width", "" + annotArea[3]);
							// annotationElement.setAttributeNS(null,
							// "annno_class", ChatGroup.getGroupName(group));

							String annotLocal = SceneGraph.getCoreSectionMarkerLocal(t.getId(), cs.getId(), i);
							String annotURL = SceneGraph.getCoreSectionMarkerURL(t.getId(), cs.getId(), i);
							// String annotText =
							// SceneGraph.getCoreSectionMarkerText(t.getId(),
							// cs.getId(), i);
							if (annotLocal == null && annotURL == null) {
								System.out.println("- [DEBUG] Annotation of trackId: " + t.getId() + ", section: " + cs.getId() + ", markerId: " + i
										+ " has already been deleted.");

								continue;
							}

							// Get the content of the annotation file
							File f = new File(annotLocal);
							if (!f.exists()) {
								// download file from URL
								boolean isDownloaded = URLRetrieval.retrieveLocalCopy(annotURL, annotLocal);

								if (isDownloaded) {
									f = new File(annotLocal);
								} else {
									System.err.println("Cannot download annotation file '" + annotURL + "', skip.");
									continue;
								}
							}

							// TODO:
							// Store these inside property list annotation file
							// anno_type: "point count area"
							// remarks: "test for point count area"
							// analyst: "WH"
							// coordinates:
							// "93.304673783714,448.91671139236,434.507042253521,193.661971830986"
							// roi_type: "Ellipse"
							// color: "#FF00FF3F"
							// line_width: "10"
							// rotation: "-10.53"
							// value1: "frisch"
							// value2: "angular"
							// value3: "very wel sorted"
							// value4: "banded"

							String annotId = "N/A";
							String annotName = "N/A";
							String annotApp = "unknown"; // Values: CLR:
															// Corelyzer, PSC:
															// PSICAT
							String annotClass = "unknown"; // Values: CLR_clast,
															// CLR_sampling,
															// CLR_propvalue,
															// CLR_freeform
							String annotState = "unknown"; // Values: added,
															// deleted, edited,
															// unchanged

							if (f.getAbsolutePath().endsWith(".plist")) {
								// plist-based annotations
								// Clast, Sampling and PropertyValueList
								Hashtable<String, String> propertyHash = PropertyListUtility.generateHashtableFromFile(f);

								// All customized fields
								for (Map.Entry<String, String> e : propertyHash.entrySet()) {
									String key = e.getKey();
									String value = e.getValue();

									annotationElement.setAttributeNS(null, key, value);
								}

								// ID, Name, annotClass, annotApp, annotState
								annotId = propertyHash.get("id");
								if (annotId == null || annotId.equals("")) {
									annotId = "N/A";
								}

								annotName = propertyHash.get("name");
								if (annotName == null || annotName.equals("")) {
									annotName = "N/A";
								}

								annotClass = propertyHash.get("anno_class");
								if (annotClass == null || annotClass.equals("")) {
									annotClass = "CLR_propvalue";
								}

								annotApp = propertyHash.get("anno_app");
								if (annotApp == null || annotApp.equals("")) {
									annotApp = "CLR";
								}

								annotState = propertyHash.get("anno_state");
								boolean visible = SceneGraph.getCoreSectionMarkerVisibility(t.getId(), cs.getId(), i);
								if (annotState == null || annotState.equals("")) {
									annotState = "added";
								} else if (!visible) {
									annotState = "deleted";
								}
							} else {
								// How about just forget about old html and
								// convert freeform storing format to plist?
								System.out.println("- [DEBUG] Ignore non-plist annotations for now.");
							}

							annotationElement.setAttributeNS(null, "id", annotId);
							annotationElement.setAttributeNS(null, "name", annotName);
							annotationElement.setAttributeNS(null, "anno_app", annotApp);
							annotationElement.setAttributeNS(null, "anno_class", annotClass);
							annotationElement.setAttributeNS(null, "anno_state", annotState);

							System.out.println("- [DEUBG] Annotation's id: " + annotId + " name: " + annotName + " anno_app: " + annotApp + " anno_class: "
									+ annotClass + " anno_state: " + annotState);

							imageElement.appendChild(annotationElement);
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("No! exception: " + e);
			e.printStackTrace();

			return false;
		}

		// Now write out the file
		try {
			OutputFormat format = new OutputFormat(doc);
			format.setIndenting(true);

			XMLSerializer serializer = new XMLSerializer(new FileOutputStream(new File(aFile)), format);

			serializer.serialize(doc);
		} catch (Exception e) {
			System.out.println("[EXCEPTION] When trying to write out XML");
			System.out.println("" + e);
			return false;
		}

		return true;
	}
}
