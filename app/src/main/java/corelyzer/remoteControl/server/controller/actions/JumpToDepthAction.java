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

import corelyzer.controller.CRExperimentController;

public class JumpToDepthAction extends AbstractAction {
	public JumpToDepthAction(final String[] toks) {
		super(toks);
		this.setActionType(Type.VIEW);
	}

	public void run() {
		if (app != null) {
			float depth;

			try {
				depth = Float.parseFloat(cmds[1].trim());
			} catch (NumberFormatException e) {
				depth = 0.0f;
			}

			CRExperimentController.jumpToDepth(depth);
		} else {
			System.out.println("---> [JumpToDepthAction] to depth '" + cmds[1].trim() + "' meters");
		}
	}
}
