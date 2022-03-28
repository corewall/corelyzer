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

package corelyzer.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;

import corelyzer.data.ChatGroup;
import corelyzer.data.CoreSection;
import corelyzer.data.MarkerType;
import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.WellLogDataSet;
import corelyzer.data.WellLogTable;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.PropertyListUtility;

public class StateWriter {
	FileWriter stateFile;
	String projectname = "";

	CorelyzerApp app;
	CoreGraph cg;

	// canvas dpi to convert pixel to cm
	float canvas_dpix;
	float canvas_dpiy;

	boolean[] sessionsToSave;

	public StateWriter() {
		super();
	}

	public StateWriter(final boolean[] sessionIndices) {
		sessionsToSave = sessionIndices;
	}

	private boolean areParentsDone(final LinkedList<TrackSceneNode> tracks, final TrackSceneNode t) {
		for (CoreSection section : t.getCoreSections()) {
			int trackId = t.getId();
			int sectionId = section.getId();

			int pTrackId = SceneGraph.getSectionParentTrackId(trackId, sectionId);

			for (TrackSceneNode track : tracks) {
				if (track.getId() == pTrackId) {
					return false;
				}
			}
		}

		return true;
	}

	private void processCoreAnnotation(final DocumentImpl doc, final Element e, final int tid, final int csid, final int marker) {
		// check if valid marker
		if (!SceneGraph.isCoreSectionMarker(tid, csid, marker)) {
			return;
		}

		String name = SceneGraph.getCoreSectionMarkerLocal(tid, csid, marker);
		String url = SceneGraph.getCoreSectionMarkerURL(tid, csid, marker);
		if ((name == null) || name.equals("")) {
			// System.out.println("empty local name anno");
		}

		// override scencegraph url with real local file
		try {
			URL aURL = new URL(url);
			if (aURL.getProtocol().toLowerCase().startsWith("file")) {
				url = new File(name).toURI().toURL().toString();
			}
		} catch (MalformedURLException urlEx) {
			try {
				url = new File(name).toURI().toURL().toString();
			} catch (Exception e1) {
				System.out.println("- [Annotation] Just ignore this: " + name);
			}
		}

		Element m = doc.createElement("visual");
		m.setAttributeNS(null, "type", "annotation");
		m.setAttributeNS(null, "local", name);
		m.setAttributeNS(null, "urn", url);

		String label = SceneGraph.getCoreSectionMarkerText(tid, csid, marker);
		m.setAttributeNS(null, "label", label);

		label = SceneGraph.getCoreSectionMarkerRelationText(tid, csid, marker);
		m.setAttributeNS(null, "rLabel", label);

		// addition for new marker type
		int group = SceneGraph.getCoreSectionMarkerGroup(tid, csid, marker);
		int markertype = SceneGraph.getCoreSectionMarkerType(tid, csid, marker);
		m.setAttributeNS(null, "group", ChatGroup.getGroupName(group));
		m.setAttributeNS(null, "marker", MarkerType.getMarkerName(markertype));

		float fvalue;
		float[] loc = new float[4];

		String str = "";
		fvalue = SceneGraph.getCoreSectionMarkerXPos(tid, csid, marker) * 2.54f / canvas_dpix;
		fvalue /= 100.0f;
		str = str + fvalue;
		m.setAttributeNS(null, "x", str);

		str = "";
		fvalue = SceneGraph.getCoreSectionMarkerYPos(tid, csid, marker) * 2.54f / canvas_dpiy;
		fvalue /= 100.0f;
		str = str + fvalue;
		m.setAttributeNS(null, "y", str);

		str = "";
		fvalue = SceneGraph.getCoreSectionMarkerIconXPos(tid, csid, marker) * 2.54f / canvas_dpix;
		fvalue /= 100.0f;
		str = str + fvalue;
		m.setAttributeNS(null, "ax", str);

		str = "";
		fvalue = SceneGraph.getCoreSectionMarkerIconYPos(tid, csid, marker) * 2.54f / canvas_dpiy;
		fvalue /= 100.0f;
		str = str + fvalue;
		m.setAttributeNS(null, "ay", str);

		str = "";
		fvalue = SceneGraph.getCoreSectionMarkerV0(tid, csid, marker) * 2.54f / canvas_dpix;
		loc[0] = fvalue;
		fvalue /= 100.0f;
		str = str + fvalue;
		m.setAttributeNS(null, "v0", str);

		str = "";
		fvalue = SceneGraph.getCoreSectionMarkerV1(tid, csid, marker) * 2.54f / canvas_dpiy;
		loc[1] = fvalue;
		fvalue /= 100.0f;
		str = str + fvalue;
		m.setAttributeNS(null, "v1", str);

		str = "";
		fvalue = SceneGraph.getCoreSectionMarkerV2(tid, csid, marker) * 2.54f / canvas_dpix;
		loc[2] = fvalue;
		fvalue /= 100.0f;
		str = str + fvalue;
		m.setAttributeNS(null, "v2", str);

		str = "";
		fvalue = SceneGraph.getCoreSectionMarkerV3(tid, csid, marker) * 2.54f / canvas_dpiy;
		loc[3] = fvalue;
		fvalue /= 100.0f;
		str = str + fvalue;
		m.setAttributeNS(null, "v3", str);

		e.appendChild(m);

		// FIXME updateAnnotations(group);
		if (group == ChatGroup.CLAST) {
			updateClastPlist(name, loc);
		} else if (group == ChatGroup.SAMPLE) {
			updateSampleRequestPlist(name, loc);
		}
	}

	private void processGraph(final DocumentImpl doc, final Element e, final int tid, final int csid, final int gid, final int dsId, final int table,
			final int field, final boolean toSaveAnnotation) {

		Element g = doc.createElement("visual");
		g.setAttributeNS(null, "type", "graph");
		g.setAttributeNS(null, "dataset", SceneGraph.getDatasetName(dsId));
		g.setAttributeNS(null, "table", SceneGraph.getTableName(dsId, table));

		// WellLogDataSet ds =
		// (WellLogDataSet) app.getDataFileListModel().elementAt(set);
		WellLogDataSet ds = null;
		CoreGraph cg = CoreGraph.getInstance();
		for (Session s : cg.getSessions()) {
			if (s != null) {
				for (WellLogDataSet d : s.getDatasets()) {
					if (d.getId() == dsId) {
						ds = d;
						break;
					}
				}
			}
		}

		if (ds == null) {
			System.out.println("- Cannot find WellLogDataSet. Ignore this graph.");
			return;
		}

		// System.out.println("- Accessing WellLogDataSet: " +
		// ds.getSourceFilename());

		WellLogTable t = ds.getTable(table);
		String fieldName = t.getHeader(field + 1);

		g.setAttributeNS(null, "field", fieldName);
		String str = "";
		str = str + SceneGraph.getLineGraphColorComponent(gid, 0);
		g.setAttributeNS(null, "r", str);
		str = "";

		str = str + SceneGraph.getLineGraphColorComponent(gid, 1);
		g.setAttributeNS(null, "g", str);
		str = "";

		str = str + SceneGraph.getLineGraphColorComponent(gid, 2);
		g.setAttributeNS(null, "b", str);
		str = "";

		str = str + SceneGraph.getGraphMin(gid);
		g.setAttributeNS(null, "min", str);
		str = "";

		str = str + SceneGraph.getGraphMax(gid);
		g.setAttributeNS(null, "max", str);
		str = "";
		
		str = str + SceneGraph.getGraphExcludeMin(gid);
		g.setAttributeNS(null, "exclude_min", str);
		str = "";

		str = str + SceneGraph.getGraphExcludeMax(gid);
		g.setAttributeNS(null, "exclude_max", str);
		str = "";
		
		str = str + SceneGraph.getGraphExcludeStyle(gid);
		g.setAttributeNS(null, "exclude_style", str);
		str = "";
		
		str = str + SceneGraph.getGraphSlot(gid);
		g.setAttributeNS(null, "slot", str);
		str = "";

		// new addition graph type: use style keyword instead type here
		str = str + SceneGraph.getLineGraphType(gid);
		g.setAttributeNS(null, "style", str);
		str = "";

		// new addition graph depth: ver 1.0
		// / TrackSceneNode tnode = (TrackSceneNode)
		// / app.getTrackListModel().elementAt(tid);
		// str = str + corelyzer.helper.SceneGraph.getSectionXPos(tid, csid) *
		// 2.54f / canvas_dpix;
		float offset = SceneGraph.getSectionGraphOffset(tid, csid);

		if (offset == 0.0f) {
			str = str + SceneGraph.getSectionDepth(tid, csid) / 100.0f;
		} else {
			str = str + (offset + SceneGraph.getSectionDepth(tid, csid)) / 100.0f;
		}

		g.setAttributeNS(null, "depth", str);

		if (toSaveAnnotation) {
			// Only save it once
			// do annotation markers
			int nmarkers = SceneGraph.getNumCoreSectionMarkers(tid, csid);
			for (int i = 0; i < nmarkers; i++) {
				processCoreAnnotation(doc, g, tid, csid, i);
			}
		}

		e.appendChild(g);
	}

	private void processSection(final DocumentImpl doc, final Element e, final TrackSceneNode t, final CoreSection cs) {
		// do graphs first
		int csid = cs.getId();
		int tid = t.getId();

		/*
		 * int ngraphs =
		 * corelyzer.helper.SceneGraph.getNumGraphsForSection(tid,csid); int i;
		 * System.out.println("Num Graphs: " + ngraphs); for( i = 0; i <
		 * ngraphs; i++) { int gid =
		 * corelyzer.helper.SceneGraph.getGraphIDFromSectionSlot(tid,csid,i);
		 * int set = corelyzer.helper.SceneGraph.getDatasetReference(gid); int
		 * table = corelyzer.helper.SceneGraph.getTableReference(gid); int field
		 * = corelyzer.helper.SceneGraph.getFieldReference(gid);
		 * 
		 * if( gid > -1 && set > -1 && table > -1 && field > -1) {
		 * System.out.println("corelyzer.data.Graph error: gid: " + gid +
		 * " set: " + set + " table: " + table + " field: " + field);
		 * processGraph(doc,e,tid,csid,gid,set,table,field); } else {
		 * System.out.println("corelyzer.data.Graph error: gid: " + gid +
		 * " set: " + set + " table: " + table + " field: " + field); } }
		 */

		// do annotation markers
		int i;
		int nmarkers = SceneGraph.getNumCoreSectionMarkers(tid, csid);
		for (i = 0; i < nmarkers; i++) {
			processCoreAnnotation(doc, e, tid, csid, i);
		}

	}

	private void processTrack(final DocumentImpl doc, final Element e, final TrackSceneNode t) {

		int tid = t.getId();
		// go through and determine children
		for (int i = 0; i < t.getNumCores(); i++) {
			CoreSection cs = t.getCoreSection(i);
			String sectionName = cs.getName();
			int csid = cs.getId();
			Element c = doc.createElement("visual");

			// coresection may have image and/or graphs (from ver 1.0)
			if (cs.hasImage()) {
				int imageId = SceneGraph.getImageIdForSection(tid, csid);

				String name = SceneGraph.getImageName(imageId);
				String url = SceneGraph.getImageURL(imageId);

				// override scencegraph url with real local file
				try {
					URL aURL = new URL(url);
					if (aURL.getProtocol().toLowerCase().startsWith("file")) {
						url = new File(name).toURI().toURL().toString();
					}
				} catch (MalformedURLException urlEx) {
					try {
						url = new File(name).toURI().toURL().toString();
					} catch (MalformedURLException e1) {
						System.out.println("Ignore this: " + name);
					}
				}

				c.setAttributeNS(null, "name", sectionName);
				c.setAttributeNS(null, "type", "core_section");
				c.setAttributeNS(null, "local", name);
				c.setAttributeNS(null, "urn", url);
				c.setAttributeNS(null, "DISId", cs.getDISId());
				c.setAttributeNS(null, "length", String.valueOf(cs.getLength()));
				c.setAttributeNS(null, "mcd_depth", String.valueOf(cs.getMCDDepth()));

				String str = "";
				str = str + SceneGraph.getSectionXPos(tid, csid) * 2.54f / canvas_dpix / 100.0f;
				c.setAttributeNS(null, "depth", str);
				str = "";

				/*
				 * ver 1.0 does not use y in section element str = str + (
				 * corelyzer.helper.SceneGraph.getSectionYPos(tid, csid) * 2.54f
				 * / canvas_dpiy ) / 100.0f; c.setAttributeNS(null,"y",str); str
				 * = "";
				 */

				float image_dpix = SceneGraph.getSectionDPIX(tid, csid);
				float image_dpiy = SceneGraph.getSectionDPIY(tid, csid);

				str = str + image_dpix;

				c.setAttributeNS(null, "dpi_x", str);
				str = "";

				str = str + image_dpiy;

				c.setAttributeNS(null, "dpi_y", str);
				str = "";

				str = str + SceneGraph.getSectionRotation(tid, csid);

				c.setAttributeNS(null, "rot", str);

				boolean isPortrait = SceneGraph.getSectionOrientation(tid, csid);
				if (isPortrait) {
					str = "PORTRAIT";
				} else {
					str = "LANDSCAPE";
				}

				c.setAttributeNS(null, "orientation", str);
				str = "";

				str = str + SceneGraph.getSectionIntervalTop(tid, csid);
				c.setAttributeNS(null, "intervalTop", str);
				str = "";

				str = str + SceneGraph.getSectionIntervalBottom(tid, csid);
				c.setAttributeNS(null, "intervalBottom", str);

				// Length, width, x, y, orientation? definition?
				float imageWidth = SceneGraph.getImageWidth(imageId);
				float imageHeight = SceneGraph.getImageHeight(imageId);

				float width = imageWidth / image_dpix * 2.54f / 100.0f;
				float height = imageHeight / image_dpiy * 2.54f / 100.0f;

				c.setAttributeNS(null, "width", "" + width);
				c.setAttributeNS(null, "height", "" + height);

				int parentTrackId = SceneGraph.getSectionParentTrackId(tid, csid);
				int parentSectionId = SceneGraph.getSectionParentSectionId(tid, csid);

				TrackSceneNode srcTrack = cg.getCurrentSession().getTrackSceneNodeWithTrackId(parentTrackId);
				if (srcTrack != null) {
					CoreSection srcSection = srcTrack.getCoreSectionByGID(parentSectionId);

					if (srcSection != null) {
						String parentTrackName = srcTrack.getName();
						String parentSectionName = srcSection.getName();

						c.setAttributeNS(null, "parentTrack", parentTrackName);
						c.setAttributeNS(null, "parentSection", parentSectionName);
					}
				}

				e.appendChild(c);

				processSection(doc, c, t, cs);
			} else {
				// System.out.println("!!! " + cs + " has no images!");
			}

			// go through graph here (ver 1.0 change: graph is the same level as
			// image)
			if (cs.hasGraph()) {
				int ngraphs = SceneGraph.getNumGraphsForSection(tid, csid);

				// System.out.println("Num Graphs: " + ngraphs);
				boolean toSaveAnnotation = true;
				for (int j = 0; j < ngraphs; j++) {
					int gid = SceneGraph.getGraphIDFromSectionSlot(tid, csid, j);
					int set = SceneGraph.getDatasetReference(gid);
					int table = SceneGraph.getTableReference(gid);
					int field = SceneGraph.getFieldReference(gid);

					if ((gid > -1) && (set > -1) && (table > -1) && (field > -1)) {
						// System.out.println("---> Saving Graph: gid: " + gid +
						// " set: " + set
						// + " table: " + table + " field: " + field);
						processGraph(doc, e, tid, csid, gid, set, table, field, toSaveAnnotation);

						// Just save the annotations in one of the graph
						if (toSaveAnnotation) {
							toSaveAnnotation = false;
						}
					} else {
						// System.out.println("---> Graph error: gid: " + gid +
						// " set: " + set
						// + " table: " + table + " field: " + field);
					}
				}
			} else {
				// System.out.println("---> CoreSection " + cs +
				// " has no graph");
			}
		}

	}

	private void updateClastPlist(final String aLocalFilePath, final float[] currentPos) {
		File f = new File(aLocalFilePath);

		if (f.exists()) {
			Hashtable<String, String> aDict = PropertyListUtility.generateHashtableFromFile(f);

			String newULPosStr = currentPos[0] + ", " + currentPos[1];
			String newLRPosStr = currentPos[2] + ", " + currentPos[3];

			aDict.put("upperleft", newULPosStr);
			aDict.put("lowerright", newLRPosStr);

			aDict.put("height", "" + Math.abs(currentPos[1] - currentPos[3]));
			aDict.put("width", "" + Math.abs(currentPos[2] - currentPos[0]));

			// System.out.println("---> [INFO] Saving plist '" + f + "'");
			PropertyListUtility.saveHashtableToProperListFile(aDict, f);
		}
	}

	private void updateSampleRequestPlist(final String aLocalFilePath, final float[] currentPos) {
		File f = new File(aLocalFilePath);

		if (f.exists()) {
			Hashtable<String, String> aDict = PropertyListUtility.generateHashtableFromFile(f);

			String newLocStr = "(" + currentPos[0] + ", " + currentPos[1] + ") - (" + currentPos[2] + ", " + currentPos[3] + ")";

			aDict.put("sampleLocation", newLocStr);

			// System.out.println("---> [INFO] Saving plist '" + f + "'");
			PropertyListUtility.saveHashtableToProperListFile(aDict, f);
		}
	}

	public boolean writeState(final String filename) {
		return writeState(filename, "");
	}

	public boolean writeState(final String filename, final String project) {
		if (filename.equals("")) {
			return false;
		}

		app = CorelyzerApp.getApp();
		projectname = project;
		cg = CoreGraph.getInstance();
		if (cg.getSessions().size() == 0) {
			return false; // don't save an empty workspace
		}

		DocumentImpl doc;
		Element root;
		try {
			doc = new DocumentImpl();
			root = doc.createElement("scene");

			if (root == null) {
				System.out.println("---> [WARN] Cannot create scene root node");
			}

			canvas_dpix = SceneGraph.getCanvasDPIX(0);
			canvas_dpiy = SceneGraph.getCanvasDPIY(0);
			root.setAttributeNS(null, "name", projectname);
			root.setAttributeNS(null, "version", "1.5"); // current Jan 2008
			// root.setAttributeNS(null, "version", "1.0"); // Jan 2007

			Element e;
			doc.appendChild(root);

			int sessionCounter = 0;
			for (Session s : cg.getSessions()) {
				// Only save selected sessions
				if (sessionsToSave != null) {
					if ((sessionCounter >= 0) && (sessionCounter < sessionsToSave.length)) {
						if (!sessionsToSave[sessionCounter]) {
							sessionCounter++;
							continue;
						}
					} else {
						sessionCounter++;
						continue;
					}
					sessionCounter++;
				}

				// Session
				Element sessionElement = doc.createElement("session");
				sessionElement.setAttributeNS(null, "name", s.getName());
				sessionElement.setAttributeNS(null, "DISId", s.getDISId());
				root.appendChild(sessionElement);

				// Datasets
				for (WellLogDataSet d : s.getDatasets()) {
					String name = d.getSourceFilename();
					String url = d.getURN();

					// override scencegraph url with real local file
					try {
						URL aURL = new URL(url);
						if (aURL.getProtocol().toLowerCase().startsWith("file")) {
							url = new File(name).toURI().toURL().toString();
						}
					} catch (MalformedURLException urlEx) {
						try {
							url = new File(name).toURI().toURL().toString();
						} catch (MalformedURLException e1) {
							System.out.println("Ignore this: " + name);
						}
					}

					if (!name.equals("")) {
						e = doc.createElement("dataset");
						e.setAttributeNS(null, "local", name);
						e.setAttributeNS(null, "urn", url);
						sessionElement.appendChild(e);
					}
				}

				// Tracks
				// a naive way to delay children track to be saved last
				// this will not handle more complex cases
				LinkedList<TrackSceneNode> tracks = new LinkedList<TrackSceneNode>(s.getTrackSceneNodes());
				while (!tracks.isEmpty()) {
					TrackSceneNode t = tracks.poll();

					if (!areParentsDone(tracks, t)) {
						tracks.addLast(t);
					} else {
						e = doc.createElement("visual");
						e.setAttributeNS(null, "type", "track");
						e.setAttributeNS(null, "name", SceneGraph.getTrackName(t.getId()));

						// For DIS recovery data
						e.setAttributeNS(null, "DISId", t.getDISId());
						e.setAttributeNS(null, "length", String.valueOf(t.getLength()));
						e.setAttributeNS(null, "mcd_depth", String.valueOf(t.getMCDDepth()));
						e.setAttributeNS(null, "top_depth", String.valueOf(t.getTopDepth()));

						// unit should be meter from ver 1.0
						String str = "";
						str = str + SceneGraph.getTrackXPos(t.getId()) * 2.54f / canvas_dpix / 100.0f;
						e.setAttributeNS(null, "x", str);

						str = "" + SceneGraph.getTrackYPos(t.getId()) * 2.54f / canvas_dpiy / 100.0f;
						e.setAttributeNS(null, "y", str);
						e.setAttributeNS(null, "z", "0");

						sessionElement.appendChild(e);
						processTrack(doc, e, t);
					}
				}
			} // end of session loop
		} catch (Exception e) {
			System.out.println("[EXCEPTION] Error while building XML " + e);
			e.printStackTrace();

			return false;
		}

		// Now write out the file
		try {
			OutputFormat format = new OutputFormat(doc);
			format.setIndenting(true);

			XMLSerializer serializer = new XMLSerializer(new FileOutputStream(new File(filename)), format);

			serializer.serialize(doc);
		} catch (Exception e) {
			System.out.println("[EXCEPTION] When trying to write out XML");
			System.out.println("" + e);
			return false;
		}

		return true;
	}
}
