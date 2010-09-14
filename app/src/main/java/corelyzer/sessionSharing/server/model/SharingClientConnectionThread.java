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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import corelyzer.sessionSharing.common.SharingClientRequest;
import corelyzer.sessionSharing.common.SharingServerResponse;
import corelyzer.sessionSharing.server.controller.ServerDownloadController;
import corelyzer.sessionSharing.server.controller.ServerListController;
import corelyzer.sessionSharing.server.controller.ServerPublishController;
import corelyzer.sessionSharing.server.controller.ServerSubscribeController;

public class SharingClientConnectionThread extends Thread {
	private boolean closeConnection;

	private Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos;

	public SharingClientConnectionThread() {
		super();
	}

	public SharingClientConnectionThread(final Socket s, final int id) {
		super("ClientConnectionThread");

		closeConnection = false;
		socket = s;
	}

	public DataInputStream getDis() {
		return dis;
	}

	public DataOutputStream getDos() {
		return dos;
	}

	public Socket getSocket() {
		return socket;
	}

	private void processConnect() {
		System.out.println("---> Process client CONNECT request");
		try {
			dos.write(SharingServerResponse.CONNECTED);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processShutdown() {
		try {
			dos.write(SharingServerResponse.SHUTDOWN_SUCCESS);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		SharingServerApplication server = SharingServerApplication.getAppServer();

		if (server != null) {
			server.setRunning(false);
		}
	}

	@Override
	public void run() {
		try {
			System.out.println("---> [SSA] SharingClientConnectionThread Working...");

			if (closeConnection) {
				System.out.println("---> [SSA] Close client connection");
			}

			socket.setSoTimeout(0);
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());

			while (!closeConnection) {
				if (dis == null) {
					closeConnection = true;
					continue;
				}

				try {
					byte msg = dis.readByte();
					System.out.println("---> [SSA] Incoming message: " + msg);

					switch (msg) {
						case SharingClientRequest.CONNECT:
							this.processConnect();
							break;

						case SharingClientRequest.SHUTDOWN:
							this.processShutdown();
							break;

						case SharingClientRequest.PUBLISH:
							ServerPublishController.processPublish(this);
							break;

						case SharingClientRequest.LIST:
							ServerListController.processList(this);
							break;

						case SharingClientRequest.DOWNLOAD:
							ServerDownloadController.processDownload(this);
							break;

						case SharingClientRequest.SUBSCRIBE:
							ServerSubscribeController.processSubscribe(this);
							break;
					}

					this.setCloseConnection();
				} catch (Exception e) {
					System.err.println("---> [SSA] " + e);
					e.printStackTrace();
				}
			}

			// SharingServerApplication server =
			// SharingServerApplication.getAppServer();
			// server.freeClient(this);

			// free up the entry in the has table
			// server.removeUserFromTable(new Integer(data.getUserID()) );

			socket.close();
		} catch (Exception e) {
			System.err.println("---> [SSA] " + e);
			e.printStackTrace();

			try {
				SharingServerApplication.getAppServer().freeClient(this);

				// free up the entry in the has table
				// SharingServerApplication.getAppServer().removeUserFromTable(
				// new Integer(data.getUserID()) );

				socket.close();
			} catch (Exception ee) {
				;
			}
		}
	}

	public void setCloseConnection() {
		SharingServerApplication.getAppServer().freeClient(this);

		closeConnection = true;
	}
}
