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
package corelyzer.ui.annotation;

public class AnnotationType {
	String name;
	String formName;
	String description;
	String dictFilename;

	public AnnotationType() {
		super();
	}

	public AnnotationType(final String name, final String form, final String description, final String dictFilename) {
		this();

		this.name = name;
		this.formName = form;
		this.description = description;
		this.dictFilename = dictFilename;
	}

	public String getDescription() {
		return this.description;
	}

	public String getDictFilename() {
		return dictFilename;
	}

	public String getFormName() {
		return this.formName;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return "Annotation type: " + name + ", formName: " + formName + ", description: " + description + ", using dictionary: " + dictFilename;
	}
}
