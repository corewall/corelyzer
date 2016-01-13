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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import corelyzer.ui.AlphanumComparator;

/**
 * Data structure to hold core track information in Java side.
 * 
 * 
 */
public class TrackSceneNode implements Comparable<TrackSceneNode> {
	/** Name of the track */
	String trackName;
	String DISId = "N/A_N/A";

	float topDepth = 0.0f;
	float MCDDepth = 0.0f;
	float length = 0.0f;

	/** ID of the track */
	int trackId;

	AnnotationRepository annorepo;

	/** Core sections in this track */
	Vector<CoreSection> secVec;
	/** Z order of Core sections in this track */
	Vector<Integer> zOrder;

	Boolean highlight;
	CoreSectionImage[] selectedSection;

	/** hashtable for section name (key) and coresection (value) */
	Hashtable secnameHash;
	/** hashtable for section id (key) and coresection (value) */
	Hashtable secidHash;

	/**
	 * Constructor
	 * 
	 * @param id
	 *            track ID from the native scene graph
	 */
	public TrackSceneNode(final int id) {
		highlight = false;
		// this.imgVec = new Vector<corelyzer.data.CoreSectionImage>();
		this.secVec = new Vector<CoreSection>();
		this.zOrder = new Vector<Integer>();
		trackId = id;
		secnameHash = new Hashtable();
		secidHash = new Hashtable();
	}

	/**
	 * Constructor
	 * 
	 * @param n
	 *            track name
	 * @param id
	 *            track ID from the native scene graph
	 */
	public TrackSceneNode(final String n, final int id) {
		this(id);
		this.setName(n);
	}

	/** Append a object to this node as a child in scene graph */
	public void addChild(final Object child, final int secId, final int gid) {
		if (child == null) {
			return;
		}

		Boolean b1 = false;
		Boolean b2 = false;
		Boolean b3 = false;

		try {
			b1 = Class.forName("corelyzer.data.CoreSectionImage").isInstance(child);
			b2 = Class.forName("corelyzer.data.AnnotationThread").isInstance(child);
			b3 = Class.forName("corelyzer.data.CoreSectionGraph").isInstance(child);
		} catch (ClassNotFoundException e) {
			System.err.println("--- ClassNotFoundException!");
		}

		if (b1) {
			// System.out.println("[DEBUG] child is corelyzer.data.CoreSectionImage");
			this.addCoreSectionImage((CoreSectionImage) child, secId, gid);
		} else if (b2) {
			// System.out.println("[DEBUG] child is corelyzer.data.AnnotationThread");
			this.annorepo.moveToFront(annorepo.insert((AnnotationThread) child));
		} else if (b3) {
			// System.out.println("[DEBUG] child is corelyzer.data.Graph");
			this.addCoreSectionGraph((CoreSectionGraph) child, secId, gid);
		} else {
			System.err.println("[DEBUG] Error in corelyzer.data.TrackSceneNode#addChild()");
		}
	}

	// Add CoreSection cs to the end of secVec
	public void addCoreSection(final CoreSection cs) {
		addCoreSection(cs, -1);
	}
	
	// If insertIndex != -1, insert CoreSection cs into secVec at insertIndex, otherwise add to the end 
	public void addCoreSection(final CoreSection cs, final int insertIndex)
	{
		if (cs == null) {
			return;
		}

		// bail out if a section with this name already exists
		CoreSection sec = this.getCoreSection(cs.getName());
		if (sec != null) {
			return;
		} else {
			if ( insertIndex == -1 )
				this.secVec.addElement(cs);
			else
				this.secVec.insertElementAt(cs, insertIndex);
			
			this.zOrder.addElement(this.secVec.size() - 1);
			this.secnameHash.put(cs.getName(), cs);
			this.secidHash.put(cs.getId(), cs);
		}
	}
	
	// graph, section gid, graph gid
	public void addCoreSectionGraph(final CoreSectionGraph grp, final int secId, final int gid) {

		if (grp == null) {
			return;
		}
		// assume that we already created section before this point
		CoreSection sec = this.getCoreSection(grp.getName());
		if (sec != null) {
			sec.addGraph(grp);
		}
	}

	void addCoreSectionImage(final CoreSectionImage img, final int secId, final int gid) {

		if (img == null) {
			return;
		}
		// assume that we already created section before this point
		CoreSection sec = this.getCoreSection(img.getSectionName());
		if (sec != null) {
			sec.setImage(img);
		}
	}

	public boolean containsSectionName(final String name) {
		if (secVec.isEmpty()) {
			return false;
		}

		return this.secnameHash.containsKey(name);
	}

	/** Access method */
	AnnotationRepository getAnnoTrreadRepo() {
		return this.annorepo;
	}

	public CoreSection getCoreSection(final int i) {
		if (i < 0 || i >= secVec.size()) {
			return null;
		}

		return this.secVec.elementAt(i);
	}

	public CoreSection getCoreSection(final String secname) {

		if (secVec.isEmpty() || secname == null) {
			return null;
		}

		for (CoreSection cs : this.secVec) {
			// So DIS' SS_XXXX image section can match for MSCL data sections
			// ImageSection example: SS_313_27_A_9_2_1
			// MSCL data section: 313_27_A_9_2_1
			if (cs.getName().endsWith(secname)) {
				return cs;
			}
		}

		return null;
		// return (CoreSection) this.secnameHash.get(secname);
	}

	public CoreSection getCoreSectionByGID(final int i) {

		if (secVec.isEmpty()) {
			return null;
		}

		return (CoreSection) this.secidHash.get(i);
	}

	/** Access method */
	CoreSectionGraph getCoreSectionGraph(final int i) {
		return this.secVec.elementAt(i).getCoreSectionGraph(i);
	}

	/** Access method */
	CoreSectionImage getCoreSectionImage(final int i) {
		return this.secVec.elementAt(i).getCoreSectionImage();
	}

	int getCoreSectionIndex(final CoreSection sec) {
		if (sec == null || this.secVec.isEmpty()) {
			return -1;
		}
		return this.secVec.indexOf(sec);
	}

	public Vector<CoreSection> getCoreSections() {
		return secVec;
	}

	/** Access method */
	Vector<Integer> getCoreZOrder() {
		return this.zOrder;
	}

	public String getDISId() {
		return DISId;
	}

	/** Access method */
	public int getId() {
		return this.trackId;
	}

	public float getLength() {
		return length;
	}

	public float getMCDDepth() {
		return MCDDepth;
	}

	/** Access method */
	public String getName() {
		return this.trackName;
	}

	/** Access method */
	public int getNumCores() {
		return this.secVec.size();
	}

	/** Access method */
	CoreSectionImage[] getSelected() {
		return this.selectedSection;
	}

	public float getTopDepth() {
		return topDepth;
	}

	public void removeAllCoreSection() {

		// remove graph in the section first
		CoreSection sec = null;
		Enumeration en = this.secVec.elements();
		while (en.hasMoreElements()) {
			sec = (CoreSection) en.nextElement();
			sec.removeAllGraph();
		}

		// clean up section & zOrder vector
		this.secnameHash.clear();
		this.secidHash.clear();
		this.secVec.clear();
		this.zOrder.clear();
	}

	public void removeCoreSection(final CoreSection sec) {

		if (sec == null) {
			return;
		}

		// remove graph including native resource
		sec.removeAllGraph();
		this.secnameHash.remove(sec.getName());
		this.secidHash.remove(sec.getId());
		int idx = secVec.indexOf(sec);
		this.secVec.removeElement(sec);
		this.zOrder.removeElement(idx);
	}

	/** Access method */
	void setDimensions(final float w, final float h) {
	}

	public void setDISId(final String DISId) {
		this.DISId = DISId;
	}

	/** Access method */
	void setId(final int i) {
		this.trackId = i;
	}

	public void setLength(final float length) {
		this.length = length;
	}

	public void setMCDDepth(final float MCDDepth) {
		this.MCDDepth = MCDDepth;
	}

	/** Access method */
	public void setName(final String name) {
		this.trackName = name;
	}

	/** Access method */
	void setSectionDPI(final CoreSectionImage img, final float dpi) {
	}

	public void setTopDepth(final float topDepth) {
		this.topDepth = topDepth;
	}

	/**
	 * Override method to return the name of the track as the object
	 * 
	 * @return track name
	 */

	@Override
	public String toString() {
		return this.trackName;
	}

	public void Update() {
	}
	
	public int compareTo(TrackSceneNode track) {
		return AlphanumComparator.compare(this.getName(), track.getName());
	}
}
