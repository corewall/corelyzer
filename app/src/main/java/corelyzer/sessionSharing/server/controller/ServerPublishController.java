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
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import corelyzer.sessionSharing.common.SharingServerResponse;
import corelyzer.sessionSharing.server.model.SharingClientConnectionThread;
import corelyzer.sessionSharing.server.model.SharingServerApplication;
import corelyzer.util.FileUtility;

public class ServerPublishController {
	public static void processPublish(final SharingClientConnectionThread clientThread) {
		if (clientThread == null) {
			return;
		}

		Socket socket = clientThread.getSocket();
		if (socket == null) {
			return;
		}

		System.out.println("---> [SSA] Process client 'Publish' request");

		try {
			DataInputStream dis = clientThread.getDis();
			DataOutputStream dos = clientThread.getDos();

			String username = dis.readUTF();
			String sessionName = dis.readUTF();
			String desc = dis.readUTF();
			String sessionStr = dis.readUTF();

			processPublishLocal(username, sessionName, desc, sessionStr);

			// write download and feed URLs
			SharingServerApplication server = SharingServerApplication.getAppServer();
			String cmlURL = server.getUrlPrefix() + "/" + username + "/" + sessionName + "/session.cml";
			String feedURL = server.getUrlPrefix() + "/" + username + "/" + sessionName + "/feed.xml";

			dos.writeUTF(cmlURL);
			dos.writeUTF(feedURL);

			dos.write(SharingServerResponse.PUBLISH_SUCCESS);
			dos.flush();
		} catch (IOException e) {
			System.err.println("---> [SSA] Exception in ServerPublishController: " + e);
			e.printStackTrace();
		}
	}

	private static void processPublishLocal(final String username, final String sessionName, final String description, final String sessStr) {
		SharingServerApplication server = SharingServerApplication.getAppServer();
		String sp = System.getProperty("file.separator");

		File userDir = new File(server.getPathPrefix() + sp + username);
		File sessDir = new File(userDir, sessionName);

		if (!userDir.exists()) {
			userDir.mkdir();
		}

		if (!sessDir.exists()) {
			sessDir.mkdir();
		}

		File sessionFile = new File(sessDir, "session.cml");
		File feedFile = new File(sessDir, "feed.xml");
		File descFile = new File(sessDir, "desc.txt");

		try {
			FileUtility.saveAStringAsAFile(description + "\n", descFile);
			FileUtility.saveAStringAsAFile(sessStr + "\n", sessionFile);

			int res = FormatTranslator.convertCMLtoAtomFeed(sessionFile, feedFile, username, sessionName, description);

			if (res != 0) {
				String mesg = "---> [SSA] Convert CML to Atom feed error! '" + sessionFile + "'";
				System.err.println(mesg);
			}
		} catch (IOException e) {
			System.err.println("---> [SSA] Save session or description failed.");
			e.printStackTrace();
		}
	}
}
