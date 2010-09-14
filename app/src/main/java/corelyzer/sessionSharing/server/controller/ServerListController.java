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

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

import corelyzer.sessionSharing.common.SharingServerResponse;
import corelyzer.sessionSharing.server.model.SharingClientConnectionThread;
import corelyzer.sessionSharing.server.model.SharingServerApplication;
import corelyzer.util.FileUtility;

public class ServerListController {
	public static int getNumberOfSessions() {
		int numberOfSessions = 0;

		SharingServerApplication server = SharingServerApplication.getAppServer();
		File shareDir = new File(server.getPathPrefix());

		if (shareDir.exists()) {
			for (File authorDir : shareDir.listFiles()) {
				if (authorDir.isDirectory()) {
					for (File projectDir : authorDir.listFiles()) {
						if (projectDir.isDirectory()) {
							numberOfSessions++;
						}
					}
				}
			}
		}

		return numberOfSessions;
	}

	public static void processList(final SharingClientConnectionThread clientThread) {
		if (clientThread == null) {
			return;
		}

		Socket socket = clientThread.getSocket();
		if (socket == null) {
			return;
		}

		System.out.println("---> [SSA] Process client 'List' request");

		try {
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			int numberOfSessions = getNumberOfSessions();
			dos.writeInt(numberOfSessions);

			SharingServerApplication server = SharingServerApplication.getAppServer();
			File shareDir = new File(server.getPathPrefix());

			String projectName, projectAuthor, projectDate;
			String projectDescription = "Not available";

			if (shareDir.exists()) {
				for (File authorDir : shareDir.listFiles()) {
					if (authorDir.isDirectory()) {
						projectAuthor = authorDir.getName();

						for (File projectDir : authorDir.listFiles()) {
							if (projectDir.isDirectory()) {
								projectName = projectDir.getName();

								File descFile = new File(projectDir, "desc.txt");
								if (descFile.exists()) {
									projectDescription = FileUtility.getFileContentAsAString(descFile);
								}

								Date date;
								File cmlFile = new File(projectDir, "session.cml");

								if (cmlFile.exists()) {
									date = new Date(cmlFile.lastModified());
								} else {
									date = new Date(projectDir.lastModified());
								}
								projectDate = date.toString();

								// loop(name, author, desc, date)
								dos.writeUTF(projectName);
								dos.writeUTF(projectAuthor);
								dos.writeUTF(projectDescription);
								dos.writeUTF(projectDate);
							}
						}
					}
				}
			}

			dos.write(SharingServerResponse.LIST_SUCCESS);
			dos.flush();
		} catch (IOException e) {
			System.err.println("---> [SSA] Exception in ServerListController: " + e);
			e.printStackTrace();
		}
	}
}
