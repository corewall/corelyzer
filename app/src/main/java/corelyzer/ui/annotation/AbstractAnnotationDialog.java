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
package corelyzer.ui.annotation;

import java.awt.Frame;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.JDialog;

import corelyzer.data.CRPreferences;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;

public abstract class AbstractAnnotationDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6394007847487678736L;
	protected int trackId = -1;
	protected int sectionId = -1;
	protected int markerId = -1;

	// In GL scene coordinates
	protected float[] upperLeftPoint;
	protected float[] lowerRightPoint;

	public Hashtable<String, String> attribs;

	public AbstractAnnotationDialog() {
		this(null);
	}

	public AbstractAnnotationDialog(final Frame f) {
		super(f);

		attribs = new Hashtable<String, String>();
		upperLeftPoint = new float[2];
		lowerRightPoint = new float[2];

		this.setAlwaysOnTop(true);
	}

	public abstract void collectViewInfo();

	public String generateFilename(final String formName) {
		// Get user and time info
		String user = System.getProperty("user.name");

		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyy hh:mm:ss a z");
		String now = formatter.format(today);
		String title = "Annotation by " + user + "@\"" + now + "\"";
		System.out.println("Last edited by:\n" + title);

		// shorter string for annotation filename
		formatter = new SimpleDateFormat("MMddyyyyhhmmssz");
		now = formatter.format(today);

		// Save it into somewhere, with some unique naming
		// String cwd = System.getProperty("user.dir");
		CRPreferences prefs = CorelyzerApp.getApp().preferences();
		String sp = System.getProperty("file.separator");

		String anno_dir = prefs.annotation_Directory;
		String filename = formName + "_annotation_" + trackId + "_" + sectionId + "_" + markerId + "_" + user + "_" + now + ".plist";

		return anno_dir + sp + filename;
	}

	public Hashtable<String, String> getAttribs() {
		return attribs;
	}

	public int getMarkerId() {
		return this.markerId;
	}

	public int getSectionId() {
		return this.sectionId;
	}

	public int getTrackId() {
		return this.trackId;
	}

	public String getValueFromKey(final String aKey) {
		return attribs == null ? null : attribs.get(aKey);
	}

	protected void onDelete() {
		AnnotationUtils.removeAnnotation(this, trackId, sectionId, markerId);
		dispose();
	}

	public void setAttributes(final Hashtable<String, String> attribs) {
		this.attribs = attribs;
	}

	// Set which annotation marker is being edited.
	public void setMarkerId(final int id) {
		this.markerId = id;
	}

	// (ulx, uly), (lrx, lry) are in physical absolute coordinates
	// This dialog displays points's location relative to the start (upperleft)
	// of this core section
	public void setRange(float ulx, float uly, float lrx, float lry) {
		// float dpi = SceneGraph.getCanvasDPIX(0); // 0-> 1st, default CanvasId
		float scale = SceneGraph.getCanvasDPIX(0) / 2.54f;

		// Track & CoreSection offsets in scene coordinates
		float trackX = SceneGraph.getTrackXPos(trackId);
		float trackY = SceneGraph.getTrackYPos(trackId);
		float sectionX = SceneGraph.getSectionXPos(trackId, sectionId);
		float sectionY = SceneGraph.getSectionYPos(trackId, sectionId);

		float xOffset = (trackX + sectionX) / scale; // become 'cm'
		float yOffset = (trackY + sectionY) / scale;

		ulx -= xOffset;
		uly -= yOffset;
		lrx -= xOffset;
		lry -= yOffset;

		// Convert marked rect back to scene coord
		this.upperLeftPoint[0] = ulx * scale;
		this.upperLeftPoint[1] = uly * scale;
		this.lowerRightPoint[0] = lrx * scale;
		this.lowerRightPoint[1] = lry * scale;
	}

	// Set which section along the given track the annotation is being
	// associated with.
	public void setSectionId(final int id) {
		this.sectionId = id;
	}

	// Set which track the annotation is being associated with
	public void setTrackId(final int id) {
		this.trackId = id;
	}

	public void setValueForKey(final String key, final String value) {
		if (attribs != null) {
			attribs.put(key, value);
		}
	}
}
