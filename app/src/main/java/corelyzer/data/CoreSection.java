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

package corelyzer.data;

import java.util.Vector;

import corelyzer.graphics.SceneGraph;

/**
 * Data structure to hold core track information in Java side.
 * 
 * 
 */
public class CoreSection {
	/** Name of the section: w/o extension */
	String sectionName;
	/** ID of the section : native code id */
	int sectionId;
	/** Depth of the section : cm unit */
	float depth;

	/** Core section images */
	CoreSectionImage sectionImg;
	/** Core section graph */
	Vector<CoreSectionGraph> sectionGraph;

	String DISId = "N/A_N/A";
	float MCDDepth = -0.0f;
	float length = -1.0f;

	/**
	 * Constructor
	 */
	public CoreSection(final int id) {
		sectionId = id;
		this.sectionGraph = new Vector<CoreSectionGraph>();
	}

	/**
	 * Constructor
	 * 
	 * @param n
	 *            section name
	 */
	public CoreSection(final String n, final int id) {
		this(id);
		this.setName(n);
	}

	public void addGraph(final CoreSectionGraph grp) {
		if (grp == null) {
			return;
		}
		this.sectionGraph.addElement(grp);
	}

	/** Access method */
	public CoreSectionGraph getCoreSectionGraph(final int i) {
		if (i < 0 || i > this.sectionGraph.size() - 1) {
			return null;
		}

		return this.sectionGraph.elementAt(i);
	}

	/** Access method */
	public CoreSectionImage getCoreSectionImage() {
		return this.sectionImg;
	}

	/** Access method */
	public float getDepth() {
		return this.depth;
	}

	public String getDISId() {
		return DISId;
	}

	/** Access method */
	public int getId() {
		return this.sectionId;
	}

	public float getLength() {
		return length;
	}

	public float getMCDDepth() {
		return MCDDepth;
	}

	/** Access method */
	public String getName() {
		return this.sectionName;
	}

	public boolean hasGraph() {
		return !this.sectionGraph.isEmpty();
	}

	public boolean hasImage() {
		if (this.sectionImg == null) {
			return false;
		} else {
			return true;
		}
	}

	public void removeAllGraph() {
		// take care of native graph
		int gid;
		for (int i = 0; i < this.sectionGraph.size(); i++) {
			gid = this.sectionGraph.elementAt(i).getId();
			SceneGraph.removeLineGraphFromSection(gid);
		}

		this.sectionGraph.removeAllElements();
	}

	public void removeGraph(final CoreSectionGraph grp) {
		this.sectionGraph.removeElement(grp);
		this.sectionGraph = null;
	}

	public void removeImage() {
		this.sectionImg = null;
	}

	/** Access method */
	public void setDepth(final float value) {
		this.depth = value;
	}

	/** Access method */
	public void setDimensions(final float w, final float h) {
	}

	public void setDISId(final String DISId) {
		this.DISId = DISId;
	}

	/**
	 * Add a core section image to this track
	 * 
	 * @param img
	 *            core section image to be added to this track
	 */
	public void setImage(final CoreSectionImage img) {
		if (img == null) {
			return;
		}
		this.sectionImg = img;
	}

	public void setLength(final float length) {
		this.length = length;
	}

	public void setMCDDepth(final float MCDDepth) {
		this.MCDDepth = MCDDepth;
	}

	/** Access method */
	public void setName(final String name) {
		this.sectionName = name;
	}

	/** Access method */
	public void setSectionDPI(final CoreSectionImage img, final float dpi) {
	}

	/**
	 * Override method to return the name of the section as the object
	 * 
	 * @return section name
	 */

	@Override
	public String toString() {
		return this.sectionName;
	}

	void Update() {
	}
}
