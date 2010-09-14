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
package corelyzer.data.lists;

import java.util.Hashtable;

import corelyzer.data.coregraph.CoreGraph;

public class CRListModels {
	public static String SESSION = "SESSION";
	public static String TRACK = "TRACK";
	public static String SECTION = "SECTION";
	public static String DATASET = "DATASET";
	public static String FIELD = "FIELD";

	Hashtable<String, CRDefaultListModel> listModels;

	public CRListModels(final CoreGraph cg) {
		listModels = new Hashtable<String, CRDefaultListModel>();

		listModels.put(SESSION, new SessionListModel(cg));
		listModels.put(TRACK, new TrackListModel(cg));
		listModels.put(SECTION, new SectionListModel(cg));
		listModels.put(DATASET, new DatasetListModel(cg));
		listModels.put(FIELD, new FieldListModel(cg));
	}

	public CRDefaultListModel getListModel(final String aName) {
		return listModels.get(aName);
	}
}
