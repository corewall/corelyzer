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

public class AnnotationThread {
	Boolean arrow;
	float refx; // where it is pointing to
	float refy;
	float range; // the full range, with imgx & imgy at the center

	public AnnotationThread() {

	}

	public AnnotationThread(final float x, final float y, final float r) {
		this();
		refx = x;
		refy = y;
		range = r;
	}

	float getRange() {
		return range;
	}

	float getRefX() {
		return refx;
	}

	float getRefY() {
		return refy;
	}

	void setRange(final float r) {
		range = r;
	}

	void setRef(final float x, final float y) {
		refx = x;
		refy = y;
	}

	void update() {
		;
	}

	/*
	 * virtual void Render(ViewControl); virtual bool
	 * OnMouseClick(GLUI::MouseEvent&); virtual void setDimensions(float,float);
	 * 
	 * void drawArrow(bool);
	 * 
	 * GLUI::Window* getEntry(int i); int getNumEntries(); ThreadWindow*
	 * getThreadWindow();
	 * 
	 * int selectedButton();
	 * 
	 * void Update();
	 */
}
