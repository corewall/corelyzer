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

import java.io.File;

/**
 * This class is used to help keep track of core section images loaded. and
 * retrieve the IDs of the section images
 */
public class CoreSectionImage {
	TrackSceneNode track;
	String filename;
	String sectionname;
	String fullfilepath;
	// int imagetype;

	/** corelyzer.data.CoreSectionImage ID: native code id */
	int sectionId;

	// GraphListing graphList;
	String DISId = "N/A";

	// -- methods
	public CoreSectionImage(final int id) {
		this.sectionId = id;
	}

	public CoreSectionImage(final TrackSceneNode trackNode, final String fullImagePath, final int sectionId) {
		this(trackNode, fullImagePath, sectionId, null);
	}

	public CoreSectionImage(final TrackSceneNode trackNode, final String fullImagePath, final int sectionId, final String secName) {
		this(sectionId);

		this.track = trackNode;
		this.fullfilepath = fullImagePath;

		File _f = new File(fullImagePath);
		this.filename = _f.getName();

		if (secName != null && !secName.equals("")) {
			this.sectionname = secName;
		} else {
			if (_f.exists()) {
				// Use filename w/o extension
				String str = this.filename;
				int idx = str.lastIndexOf('.');
				if (idx != -1) {
					this.sectionname = str.substring(0, idx);
				} else {
					this.sectionname = this.filename;
				}
			} else {
				this.sectionname = fullImagePath;
			}
		}

		// Issue C-part imagehander to load the image
		// Done in class CorelyzeAppController
	}

	public String getDISId() {
		return DISId;
	}

	/**
	 * Used in case you can not determine what the id of a section is.
	 * RECOMMENDED TO DO A LINEAR LOOK UP OF SECTIONS USING THE SCENEGRAPH CLASS
	 * INSTEAD!!
	 */
	public int getId() {
		return this.sectionId;
	}

	String getName() {
		return this.filename;
	}

	String getSectionName() {
		return this.sectionname;
	}

	public void setDISId(final String DISId) {
		this.DISId = DISId;
	}
}
