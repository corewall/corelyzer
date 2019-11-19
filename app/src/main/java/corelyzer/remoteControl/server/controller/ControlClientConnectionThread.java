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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import corelyzer.graphics.SceneGraph;
import corelyzer.remoteControl.server.controller.actions.FineTuneAction;
import corelyzer.ui.CorelyzerApp;

public class ControlClientConnectionThread extends Thread {
	private Socket socket;
	private boolean keepRunning = true;

	CorelyzerApp app = CorelyzerApp.getApp();

	public ControlClientConnectionThread() {
		super();
	}

	public ControlClientConnectionThread(final Socket s) {
		super("ControlClientConnectionThread");
		socket = s;
	}

	public boolean isKeepRunning() {
		return keepRunning;
	}

	@Override
	@SuppressWarnings({"ConstantConditions", "unused"})
	public void run() {
		try {
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			if (dis == null) {
				System.out.println("---> [ClientThread] Cannot get dis, close.");
				this.socket.close();

				return;
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dos));

			SceneGraph.setDepthOrientation(SceneGraph.VERTICAL);
			SceneGraph.setRemoteControl(true);
			app.updateGLWindows();

			while (keepRunning) {
				String textLine = reader.readLine();

				if (textLine != null && !textLine.equals("")) {
					textLine = textLine.trim();
					String[] toks = textLine.split("\t");

					if (toks.length > 0) {
						if (toks[0].equalsIgnoreCase("quit")) {
							SceneGraph.setDepthOrientation(SceneGraph.HORIZONTAL);
							this.setKeepRunning(false);
						} else if (toks[0].equalsIgnoreCase("help")) {
							this.usage(writer);
						} else if (toks[0].equalsIgnoreCase("test")) {
							this.test(writer);
						} else if (toks[0].equalsIgnoreCase("fine_tune")) {
							FineTuneAction action = new FineTuneAction(toks);
							action.setOutput(writer);
							action.run();
						} else {
							CommandProcessor proc = new CommandProcessor(toks, writer);
							proc.execute();
						}
					}

					app.updateGLWindows();
				}
			}
		} catch (IOException e) {
			System.err.println("---> [ClientThread] Cannot read from input stream: " + e);

			// Restore original direction
			CorelyzerApp app = CorelyzerApp.getApp();
			if (app != null) {
				SceneGraph.setDepthOrientation(SceneGraph.HORIZONTAL);
				app.updateGLWindows();
			}
		} finally {
			try {
				SceneGraph.setRemoteControl(false);
				app.updateGLWindows();

				socket.close();
			} catch (Exception e) {
				System.err.println("---> [ClientThread] Exception in closing server: " + e);
				e.printStackTrace();
			}
		}
	}

	public void setKeepRunning(final boolean keepRunning) {
		this.keepRunning = keepRunning;
	}

	/* a simple hand test case */
	private void test(final BufferedWriter writer) throws IOException {
		String cmd1 = "load_section\t199\t1218\ta\t1\th\t1";
		String cmd2 = "cut_interval_to_new_track\t199\t1218\ta\t1\th\t1\t0.9\t1.2\tcomposite";

		String[] toks = cmd1.split("\t");

		CommandProcessor proc = new CommandProcessor(toks, writer);
		proc.execute();

		toks = cmd2.split("\t");
		proc = new CommandProcessor(toks, writer);
		proc.execute();
	}

	private void usage(final BufferedWriter writer) throws IOException {
		String mesg = "Available commands:\n";

		mesg += "load_section <leg> <site> <hole> <core> <type> <section>\n";
		mesg += "delete_section <leg> <site> <hole> <core> <type> <section>\n";
		mesg += "set_section_top_depth <leg> <site> <hole> <core> <type> <section> <top_depth>\n";
		mesg += "set_section_visible_range <leg> <site> <hole> <core> <type> <section> <interval_top> <interval_bottom>\n";
		mesg += "split_section <leg> <site> <hole> <core> <type> <section> <split_interval_top> <split_interval_bottom>\n";
		mesg += "cut_interval_to_new_track <leg> <site> <hole> <core> <type> <section> <interval_start> <interlval_end> <new_track_name>\n";

		mesg += "jump_to_depth <meter>\n";
		mesg += "show_depth_range <top_depth> <end_depth>\n";

		mesg += "locate_section <leg> <site> <hole> <core> <type> <section>\n";
		mesg += "locate_track <hole> <core>\n";

		mesg += "delete_hole <leg> <site> <hole>\n";
		mesg += "delete_leg  <leg>\n";

		mesg += "delete_all\n";

		mesg += "shift_section <leg> <site> <hole> <core> <type> <section> <shift_distance_in_meter>\n";
		mesg += "move_scene <deltaX> <deltaY>\n";
		mesg += "scale_center <scale>\n";

		mesg += "reset\n";

		writer.write(mesg);
		writer.flush();
	}
}
