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

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import corelyzer.util.PropertyListUtility;

public class AnnotationTypeDirectory {
	static AnnotationTypeDirectory localAnnotationTypeDirectory;

	public static AnnotationTypeDirectory getLocalAnnotationTypeDirectory() {
		return localAnnotationTypeDirectory;
	}

	Hashtable<String, AnnotationType> directory;

	public AnnotationTypeDirectory() {
		super();
		directory = new Hashtable<String, AnnotationType>();

		localAnnotationTypeDirectory = this;
	}

	public void addAnnotationType(final AnnotationType t) {
		directory.put(t.getName(), t);
	}

	public Set<Map.Entry<String, AnnotationType>> entrySet() {
		return this.directory.entrySet();
	}

	public AnnotationType getAnnotationType(final String s) {
		return directory.get(s);
	}

	public String getFormName(final String className) {
		String s = "default";

		Enumeration<String> keys = directory.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			AnnotationType t = directory.get(key);
			String formName = t.getFormName();

			if (formName.equals(className)) {
				s = key;
				break;
			}
		}

		return s;
	}

	// Load config file and initialize AnnotationTypes in the directory
	public void init() {
		System.out.print("---> [AnnotationTypeDirectory] Initializing...");
		File configFile = new File("resources/annotations/config.plist");

		if (!configFile.exists()) {
			System.out.println("- [AnnotationTypeDirectory] No annotation definition available. " + configFile.getAbsolutePath());

			return;
		}

		Hashtable<String, String> configHash = PropertyListUtility.generateHashtableFromFile(configFile);

		// name, form class, description and dictionary file: 4 fields for each
		// type of annotation
		int numberOfTypes = configHash.size() / 4;

		for (int i = 0; i < numberOfTypes; ++i) {
			String nameKey = "name" + i;
			String formKey = "form" + i;
			String descKey = "desc" + i;
			String dictKey = "dict" + i;

			AnnotationType t = new AnnotationType(configHash.get(nameKey), configHash.get(formKey), configHash.get(descKey), configHash.get(dictKey));
			directory.put(t.getName(), t);
			System.out.print("\"" + t.getName() + "\"...");
		}

		System.out.print("\n");
	}

	public Enumeration<String> keys() {
		return this.directory.keys();
	}

	public int size() {
		return directory.size();
	}
}
