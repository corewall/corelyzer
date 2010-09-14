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
package corelyzer.sessionSharing.server.model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.JTextArea;

public class SharingServerApplication extends Thread {
	static SharingServerApplication appServer;

	public static SharingServerApplication getAppServer() {
		return appServer;
	}

	public static void main(final String[] args) {
		if (args.length != 5) {
			usage();
			System.exit(0);
		}

		String srvName = args[0];
		String srvIP = args[1];
		int srvPort = Integer.parseInt(args[2]);
		String pPrefix = args[3];
		String uPrefix = args[4];

		System.out.println("---> Server name:\t" + srvName);
		System.out.println("---> Server address:\t" + srvIP);
		System.out.println("---> Server port:\t" + srvPort);
		System.out.println("---> Path prefix:\t" + pPrefix);
		System.out.println("---> Url prefix:\t" + uPrefix);

		SharingServerApplication aServer = new SharingServerApplication(srvName, srvIP, srvPort);

		aServer.setPathPrefix(pPrefix);
		aServer.setUrlPrefix(uPrefix);

		aServer.init();
		aServer.start();
	}

	private static void usage() {
		String usage = "Usage: java SharingServerApplication <name> <ip> <port> <pathPrefix> <urlPrefix>";
		System.out.println(usage);
	}

	// fixme config file
	String name = "SharingServerApplication";

	String ip = "127.0.0.1";
	int serverPort = 16688;

	boolean isRunning = false;
	// Access prefixes
	String pathPrefix = "/Users/julian/Sites/CSS/share";

	String urlPrefix = "http://" + ip + "/~julian/CSS/share";

	private ServerSocket server;

	private LinkedList<SharingClientConnectionThread> clientThreads;

	private JTextArea logger;

	public SharingServerApplication() {
		super("SharingServerApplication");
	}

	public SharingServerApplication(final String aName, final String anIp, final int port) {
		this();

		name = aName;
		ip = anIp;
		serverPort = port;

		appServer = this;
	}

	public void freeClient(final SharingClientConnectionThread c) {
		try {
			clientThreads.remove(c);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getIp() {
		return ip;
	}

	public JTextArea getLogger() {
		return logger;
	}

	public String getPathPrefix() {
		return pathPrefix;
	}

	public String getServerName() {
		return name;
	}

	public String getUrlPrefix() {
		return urlPrefix;
	}

	public void init() {
		String mesg = "" + new Date() + "\t---> [SSA] Initialize server.\n";
		System.out.println(mesg);
		if (logger != null) {
			logger.append(mesg);
		}

		isRunning = true;

		// userIdThreadTable = new HashMap< Integer, ClientConnectionThread>();
		clientThreads = new LinkedList<SharingClientConnectionThread>();

		try {
			server = new ServerSocket(serverPort);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mesg = "" + new Date() + "\t---> [SSA] Server Listening to port " + serverPort + "\n";
		System.out.println(mesg);
		if (logger != null) {
			logger.append(mesg);
		}
	}

	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void run() {
		try {
			server.setSoTimeout(10000);

			while (isRunning()) {
				Socket s;

				try {
					s = server.accept();

					String mesg = "" + new Date() + "\t---> [SSA] Connection accepted. Creating client thread\n";
					System.out.println(mesg);
					if (logger != null) {
						logger.append(mesg);
					}

					SharingClientConnectionThread scct = new SharingClientConnectionThread(s, clientThreads.size());

					clientThreads.add(scct);
					clientThreads.get(clientThreads.size() - 1).start();
				} catch (SocketTimeoutException e) {
					// do nothing keep waiting
				}
			}

			String mesg = "" + new Date() + "\t---> [SSA] Shutting down SSA server\n";
			System.out.println(mesg);
			if (logger != null) {
				logger.append(mesg);
			}

			// close connections
			// fixme Concurrentmodificationexception, 'coz of list interator
			for (SharingClientConnectionThread clientThread : clientThreads) {
				clientThread.setCloseConnection();
			}

			// join the clientThreads
			mesg = "" + new Date() + "\t---> [SSA] Waiting for client threads\n";
			System.out.println(mesg);
			if (logger != null) {
				logger.append(mesg);
			}

			for (SharingClientConnectionThread clientThread : clientThreads) {
				clientThread.join();
			}
			server.close();

			mesg = "" + new Date() + "\t---> [SSA] Server stopped\n";
			System.out.println(mesg);
			if (logger != null) {
				logger.append(mesg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setIp(final String ip) {
		this.ip = ip;
	}

	public void setLogger(final JTextArea logger) {
		this.logger = logger;
	}

	public void setPathPrefix(final String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	public void setRunning(final boolean running) {
		isRunning = running;
	}

	public void setServerName(final String name) {
		this.name = name;
	}

	public void setUrlPrefix(final String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}
}
