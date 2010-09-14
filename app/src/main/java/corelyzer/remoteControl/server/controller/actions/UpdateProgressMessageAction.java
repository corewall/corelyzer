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
package corelyzer.remoteControl.server.controller.actions;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

import corelyzer.handlers.ProgressHandler;

public class UpdateProgressMessageAction extends AbstractAction {
	public UpdateProgressMessageAction(final String[] toks) {
		super(toks);
	}

	public void run() {
		if (app == null) {
			return;
		}

		if (app.isUsePluginUI()) {
			JFrame f = app.getMainFrame();

			if (f instanceof ProgressHandler) {
				JProgressBar pb = (JProgressBar) ((ProgressHandler) f).getProgressUI();

				// cmds[0] <- "update_progress"
				// cmds[1] <- Max
				// cmds[2] <- Current
				// cmds[3] <- Message
				// cmds[4] <- isIndetermined?
				pb.setMaximum(Integer.parseInt(cmds[1]));
				pb.setValue(Integer.parseInt(cmds[2]));
				pb.setString(cmds[3]);
				pb.setIndeterminate(Boolean.parseBoolean(cmds[4]));
			}
		}
	}
}
