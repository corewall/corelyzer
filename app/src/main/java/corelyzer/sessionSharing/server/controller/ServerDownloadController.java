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
package corelyzer.sessionSharing.server.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import corelyzer.sessionSharing.common.SharingServerResponse;
import corelyzer.sessionSharing.server.model.SharingClientConnectionThread;
import corelyzer.sessionSharing.server.model.SharingServerApplication;

public class ServerDownloadController {
	public static void processDownload(final SharingClientConnectionThread clientThread) {
		if (clientThread == null) {
			return;
		}

		Socket socket = clientThread.getSocket();
		if (socket == null) {
			return;
		}

		System.out.println("---> [SSA] Process client 'Download' request");

		try {
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			DataInputStream dis = new DataInputStream(socket.getInputStream());

			String authorName = dis.readUTF();
			String projectName = dis.readUTF();

			SharingServerApplication server = SharingServerApplication.getAppServer();
			String cmlURL = server.getUrlPrefix() + "/" + authorName + "/" + projectName + "/session.cml";
			dos.writeUTF(cmlURL);

			dos.write(SharingServerResponse.DOWNLOAD_SUCCESS);
			dos.flush();
		} catch (IOException e) {
			System.err.println("---> [SSA] Exception in ServerDownloadController: " + e);
			e.printStackTrace();
		}
	}
}
