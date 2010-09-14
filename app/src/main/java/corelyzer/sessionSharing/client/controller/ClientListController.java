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
package corelyzer.sessionSharing.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Vector;

import corelyzer.sessionSharing.common.SharingClientRequest;
import corelyzer.sessionSharing.common.SharingServerResponse;

public class ClientListController {

	@SuppressWarnings({ "ConstantConditions" })
	public static byte doList(final SharingClient client) {
		Socket socket;
		DataOutputStream dos;
		DataInputStream dis;

		try {
			socket = new Socket(client.getServerAddress(), client.getServerPort());

			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());

			dos.writeByte(SharingClientRequest.LIST);
			dos.flush();

			int numberOfSessions = dis.readInt();
			Vector<Hashtable<String, String>> sessions = new Vector<Hashtable<String, String>>();

			for (int i = 0; i < numberOfSessions; i++) {
				Hashtable<String, String> aSession;

				// "name", "author", "desc", "date"
				String sessionName = dis.readUTF();
				String sessionAuthor = dis.readUTF();
				String sessionDesc = dis.readUTF();
				String sessionUpdateDate = dis.readUTF();

				aSession = new Hashtable<String, String>();
				aSession.put("name", sessionName);
				aSession.put("author", sessionAuthor);
				aSession.put("description", sessionDesc);
				aSession.put("lastUpdateDate", sessionUpdateDate);

				sessions.add(aSession);
			}

			byte res = dis.readByte();

			dis.close();
			dos.close();
			socket.close();

			// do something with sessions
			client.setListResult(sessions);

			return res;
		} catch (Exception e) {
			e.printStackTrace();
			return SharingServerResponse.LIST_FAILED;
		}
	}
}
