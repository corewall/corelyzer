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
package corelyzer.util;

import java.awt.Component;
import java.awt.Frame;

import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.lists.CRDefaultListModel;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;

public class CRUtility {

	public static int getTargetTrackID(final CorelyzerApp app, final Component owner) {
		int trackId;
		CRDefaultListModel model = app.getTrackListModel();
		CRTrackSelectionDialog dialog = new CRTrackSelectionDialog(owner);
		dialog.setListModel(model);
		dialog.pack();
		dialog.setVisible(true);

		String trackName = dialog.getSelectionTrackName();

		// Get tracID with scenegraph
		if (app.containsTrackName(trackName)) {
			String currentSessionName = CoreGraph.getInstance().getCurrentSession().getName();
			trackId = SceneGraph.getTrackIDByName(currentSessionName, trackName);
		} else {
			trackId = app.createTrack(trackName);
		}

		return trackId;
	}

	public static int getTargetTrackID(final CorelyzerApp app, final Frame whoIsAsking) {
		int trackId;
		CRDefaultListModel model = app.getTrackListModel();
		CRTrackSelectionDialog dialog = new CRTrackSelectionDialog(whoIsAsking);
		dialog.setListModel(model);
		dialog.pack();
		dialog.setVisible(true);

		String trackName = dialog.getSelectionTrackName();

		// Get tracID with scenegraph
		if (app.containsTrackName(trackName)) {
			String currentSessionName = CoreGraph.getInstance().getCurrentSession().getName();
			trackId = SceneGraph.getTrackIDByName(currentSessionName, trackName);
		} else {
			trackId = app.createTrack(trackName);
		}

		return trackId;
	}
}
