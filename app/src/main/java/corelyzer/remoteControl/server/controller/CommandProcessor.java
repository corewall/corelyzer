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
package corelyzer.remoteControl.server.controller;

import java.io.Writer;
import java.util.Hashtable;

import corelyzer.remoteControl.server.controller.actions.AbstractAction;
import corelyzer.remoteControl.server.controller.actions.AffineTableAction;
import corelyzer.remoteControl.server.controller.actions.CutIntervalToNewTrackAction;
import corelyzer.remoteControl.server.controller.actions.DeleteAllAction;
import corelyzer.remoteControl.server.controller.actions.DeleteHoleAction;
import corelyzer.remoteControl.server.controller.actions.DeleteLegAction;
import corelyzer.remoteControl.server.controller.actions.DeleteSectionAction;
import corelyzer.remoteControl.server.controller.actions.FineTuneAction;
import corelyzer.remoteControl.server.controller.actions.JumpToDepthAction;
import corelyzer.remoteControl.server.controller.actions.LoadCoreAction;
import corelyzer.remoteControl.server.controller.actions.LoadLIMSTableAction;
import corelyzer.remoteControl.server.controller.actions.LoadSectionAction;
import corelyzer.remoteControl.server.controller.actions.LocateSectionAction;
import corelyzer.remoteControl.server.controller.actions.LocateTrackAction;
import corelyzer.remoteControl.server.controller.actions.MoveSceneAction;
import corelyzer.remoteControl.server.controller.actions.ResetAction;
import corelyzer.remoteControl.server.controller.actions.ScaleSceneCenterAction;
import corelyzer.remoteControl.server.controller.actions.SetSectionTopDepthAction;
import corelyzer.remoteControl.server.controller.actions.SetSectionVisibleRangeAction;
import corelyzer.remoteControl.server.controller.actions.ShiftSectionAction;
import corelyzer.remoteControl.server.controller.actions.ShowDepthRangeAction;
import corelyzer.remoteControl.server.controller.actions.ShutdownAction;
import corelyzer.remoteControl.server.controller.actions.SplitSectionAction;
import corelyzer.remoteControl.server.controller.actions.TieUpdateAction;
import corelyzer.remoteControl.server.controller.actions.UpdateProgressMessageAction;

public class CommandProcessor {
	String[] cmds;
	Writer output;

	Hashtable<String, AbstractAction> validCommands;

	public CommandProcessor() {
		super();
	}

	public CommandProcessor(final String[] toks, final Writer aWriter) {
		this();

		cmds = toks;
		output = aWriter;

		initValidCommands();
	}

	public void execute() {
		if (cmds.length > 0) {
			if (this.validCommands.containsKey(cmds[0])) {
				ControlServerApplication app = ControlServerApplication.getControlServer();
				AbstractAction action = validCommands.get(cmds[0].toLowerCase());

				if (app != null && action != null) {
					if (action.getActionType() == AbstractAction.Type.VIEW) {
						app.addATaskToViewExecutor(action);
					} else {
						app.addATaskToExecutor(action);
					}
				}
			} else {
				System.err.println("---> [CommandProcessor] Unknown command: '" + cmds[0] + "'");
			}
		}
	}

	private void initValidCommands() {
		validCommands = new Hashtable<String, AbstractAction>();

		// Insert valid commands to the validCommands table

		// System
		validCommands.put("shutdown", new ShutdownAction(cmds));
		validCommands.put("reset", new ResetAction(cmds));

		// IO
		validCommands.put("load_section", new LoadSectionAction(cmds));
		validCommands.put("delete_section", new DeleteSectionAction(cmds));
		validCommands.put("delete_hole", new DeleteHoleAction(cmds));
		validCommands.put("delete_leg", new DeleteLegAction(cmds));
		validCommands.put("delete_all", new DeleteAllAction(cmds));

		validCommands.put("set_section_top_depth", new SetSectionTopDepthAction(cmds));
		validCommands.put("set_section_visible_range", new SetSectionVisibleRangeAction(cmds));
		validCommands.put("split_section", new SplitSectionAction(cmds));
		validCommands.put("cut_interval_to_new_track", new CutIntervalToNewTrackAction(cmds));

		validCommands.put("locate_track", new LocateTrackAction(cmds));
		validCommands.put("locate_section", new LocateSectionAction(cmds));
		validCommands.put("shift_section", new ShiftSectionAction(cmds));
		validCommands.put("load_lims_table", new LoadLIMSTableAction(cmds));
		validCommands.put("fine_tune", new FineTuneAction(cmds));
		validCommands.put("tie_update", new TieUpdateAction(cmds));
		validCommands.put("load_core", new LoadCoreAction(cmds));
		validCommands.put("update_progress", new UpdateProgressMessageAction(cmds));

		// View
		validCommands.put("show_depth_range", new ShowDepthRangeAction(cmds));
		validCommands.put("jump_to_depth", new JumpToDepthAction(cmds));
		validCommands.put("move_scene", new MoveSceneAction(cmds));
		validCommands.put("scale_center", new ScaleSceneCenterAction(cmds));

		validCommands.put("affine_table", new AffineTableAction(cmds));
	}
}
