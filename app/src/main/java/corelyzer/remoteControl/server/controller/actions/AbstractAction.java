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

import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import corelyzer.ui.CorelyzerApp;

public abstract class AbstractAction implements Runnable {
	public static enum Type {
		SYSTEM, IO, VIEW
	}

	static boolean keepMissingSectionWarning = true;

	String[] cmds;
	Type actionType = Type.IO;

	JLabel statusLabel;

	static int MAX_RETRY = 1;
	int retryCount = MAX_RETRY;

	static CorelyzerApp app;
	static {
		app = CorelyzerApp.getApp();
	}

	public AbstractAction(final String[] toks) {
		cmds = toks;
	}

	protected String[] generateLoadSectionCommand() {
		Vector<String> loadCmdVector = new Vector<String>();
		loadCmdVector.add("load_section");

		if (cmds.length >= 7) {
			for (int i = 1; i <= 6; i++) {
				loadCmdVector.add(cmds[i]);
			}

			// add "depth, length, url" parameters to force lookup
			if (cmds.length == 10) {
				loadCmdVector.add(cmds[7]); // depth
				loadCmdVector.add(cmds[8]); // length
				loadCmdVector.add(cmds[9]); // url
			} else {
				loadCmdVector.add("n/a"); // depth
				loadCmdVector.add("n/a"); // length
				loadCmdVector.add("n/a"); // dpi
				loadCmdVector.add("n/a"); // url
			}
		} else if (cmds.length >= 2) {
			loadCmdVector.add(cmds[1]);
		} else {
			String mesg = "---> [GenerateLoadSection] Cannot generate " + "load_section command for " + cmds[0] + ", " + cmds.length;
			System.out.println(mesg);

			return null;
		}

		return loadCmdVector.toArray(new String[loadCmdVector.size()]);
	}

	public Type getActionType() {
		return actionType;
	}

	public JLabel getStatusLabel() {
		return statusLabel;
	}

	protected boolean isKeepRetry() {
		if (this.retryCount-- > 0) {
			System.out.println("[" + cmds[0] + "] Retry load_section: " + retryCount);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				System.err.println("[" + cmds[0] + "] Cannot do Thread.sleep()");
				e.printStackTrace();
			}

			return true;
		} else {
			if (keepMissingSectionWarning) {
				String mesg = "Cannot find URL of section image:\nleg: " + cmds[1] + ", site: " + cmds[2] + ", hole: " + cmds[3] + ", core: " + cmds[4]
						+ ", type: " + cmds[5] + ", section: " + cmds[6];
				String title = "Missing core section";
				Object[] options = { "Ignore all", "OK" };

				int choice = JOptionPane.showOptionDialog(app.getMainFrame(), mesg, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
						options, options[1]);

				if (choice == 0) {
					keepMissingSectionWarning = false;
				}
			}

			return false;
		}
	}

	public void setActionType(final Type actionType) {
		this.actionType = actionType;
	}

	public void setStatusLabel(final JLabel statusLabel) {
		this.statusLabel = statusLabel;
	}
}
