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

import corelyzer.sessionSharing.common.SharingClientRequest;
import corelyzer.sessionSharing.common.SharingServerResponse;

public class ClientSubscribeController {
	public static byte doSubscribe(final SharingClient client) {
		Socket socket;
		DataOutputStream dos;
		DataInputStream dis;

		String authorName = client.getUsername();
		String projectName = client.getSessionName();

		try {
			socket = new Socket(client.getServerAddress(), client.getServerPort());

			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());

			dos.writeByte(SharingClientRequest.SUBSCRIBE);
			dos.writeUTF(authorName);
			dos.writeUTF(projectName);
			dos.flush();

			String feedUrl = dis.readUTF();
			byte res = dis.readByte();

			dis.close();
			dos.close();
			socket.close();

			client.setFeedURL(feedUrl);

			return res;
		} catch (Exception e) {
			e.printStackTrace();
			return SharingServerResponse.SUBSCRIBE_FAILED;
		}
	}
}
