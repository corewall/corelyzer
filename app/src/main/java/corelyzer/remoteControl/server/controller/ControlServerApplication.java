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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ControlServerApplication extends Thread {
	static ControlServerApplication controlServerApplication;
	static int serverPort = 17799;
	static int serverSocketTimeout = 3000;
	static String ip = "127.0.0.1";

	public static ControlServerApplication getControlServer() {
		return controlServerApplication;
	}

	public static void main(final String[] args) {
		String srvName = "Internal Remote Control Server";
		String srvIP = "127.0.0.1";
		int srvPort = 17799;

		System.out.println("---> Server name:\t" + srvName);
		System.out.println("---> Server address:\t" + srvIP);
		System.out.println("---> Server port:\t" + srvPort);

		ControlServerApplication aServerApplication = new ControlServerApplication(srvName, srvIP, srvPort);

		aServerApplication.init();
		aServerApplication.start();
	}

	String name = "RemoteControlApplication";

	boolean isRunning = false;

	private ServerSocket server;
	// only allow one client at a time
	private ControlClientConnectionThread clientThread;

	private ExecutorService executor;

	private ExecutorService viewExecutor;

	public ControlServerApplication() {
		super("ControlServerApplication");
	}

	public ControlServerApplication(final String aName, final String anIp, final int port) {
		this();

		name = aName;
		ip = anIp;
		serverPort = port;

		controlServerApplication = this;

		executor = Executors.newSingleThreadExecutor();
		viewExecutor = Executors.newSingleThreadExecutor();
	}

	public void addATaskToExecutor(final Runnable aTask) {
		if (this.executor != null) {
			this.executor.submit(aTask);
		} else {
			System.out.println("---> [CSA] Missing executor, ignore the task.");
		}
	}

	public void addATaskToViewExecutor(final Runnable aViewTask) {
		if (this.viewExecutor != null) {
			this.viewExecutor.submit(aViewTask);
		} else {
			System.out.println("---> [CSA] Missing viewExecutor, ignore the viewTask.");
		}
	}

	public void init() {
		String mesg = "---> [CSA] Initialize server";
		System.out.println(mesg);

		isRunning = true;

		try {
			server = new ServerSocket(serverPort);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mesg = "---> [CSA] Server Listening to port " + serverPort;
		System.out.println(mesg);
	}

	public boolean isInUse() {
		return this.clientThread != null && clientThread.isAlive();
	}

	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void run() {
		try {
			server.setSoTimeout(serverSocketTimeout);

			while (isRunning()) {
				Socket s;

				try {
					s = server.accept();

					String mesg = "---> [CSA] Connection accepted. Creating clientThread";
					System.out.println(mesg);

					if (this.isInUse()) {
						System.out.println("---> [CSA] In use. Disconnecting");
						s.close();

						continue;
					}

					clientThread = new ControlClientConnectionThread(s);
					clientThread.start();
				} catch (SocketTimeoutException e) {
					// do nothing keep waiting
					// String mesg =
					// "---> [CSA] Server timout: " + serverSocketTimeout;
					// System.err.println(mesg);
				}
			}

			String mesg = "---> [CSA] Shutting down CSA server";
			System.out.println(mesg);

			mesg = "---> [CSA] Waiting for client threads";
			System.out.println(mesg);

			if (clientThread != null) {
				clientThread.join();
			}

			server.close();

			mesg = "---> [CSA] Server stopped";
			System.out.println(mesg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setRunning(final boolean running) {
		isRunning = running;

		if (!running) {
			this.executor.shutdown();
			this.viewExecutor.shutdown();
		}
	}
}
