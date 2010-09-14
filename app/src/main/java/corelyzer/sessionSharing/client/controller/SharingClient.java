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

import javax.swing.JDialog;

import corelyzer.sessionSharing.common.SharingClientRequest;
import corelyzer.sessionSharing.common.SharingServerResponse;

public class SharingClient {
	JDialog view;
	byte operation;

	// for connect
	String serverAddress;
	int serverPort;

	// for publish
	String username;
	String sessionName;
	String description;
	String sessionString;

	Socket socket;
	DataInputStream dis;
	DataOutputStream dos;

	String cmlURL, feedURL;

	Vector<Hashtable<String, String>> listResult;

	public SharingClient() {
		super();
	}

	public SharingClient(final JDialog view) {
		this();
		this.view = view;
	}

	private byte doConnect() {
		try {
			socket = new Socket(this.serverAddress, serverPort);
			// todo socket.setSoTimeout(1000);

			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());

			dos.writeByte(SharingClientRequest.CONNECT);
			dos.flush();

			byte res = dis.readByte();
			System.out.println("Got server connect response: " + res);

			dis.close();
			dos.close();
			socket.close();

			return res;
		} catch (Exception e) {
			e.printStackTrace();
			return SharingServerResponse.DISCONNECTED;
		}
	}

	private byte doShutdown() {
		try {
			socket = new Socket(this.serverAddress, serverPort);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());

			dos.writeByte(SharingClientRequest.SHUTDOWN);
			dos.flush();

			byte res = dis.readByte();
			System.out.println("Got server connect response: " + res);

			dis.close();
			dos.close();
			socket.close();

			return res;
		} catch (Exception e) {
			e.printStackTrace();
			return SharingServerResponse.SHUTDOWN_FAILED;
		}
	}

	public byte execute() {
		// do actual work
		byte result = SharingServerResponse.DISCONNECTED;

		switch (operation) {
			case SharingClientRequest.CONNECT:
				result = this.doConnect();
				break;

			case SharingClientRequest.SHUTDOWN:
				result = doShutdown();
				break;

			case SharingClientRequest.PUBLISH:
				result = ClientPublishController.doPublish(this);
				break;

			case SharingClientRequest.LIST:
				result = ClientListController.doList(this);
				break;

			case SharingClientRequest.DOWNLOAD:
				result = ClientDownloadController.doDownload(this);
				break;

			case SharingClientRequest.SUBSCRIBE:
				result = ClientSubscribeController.doSubscribe(this);
				break;
		}

		return result;
	}

	public String getCmlURL() {
		return cmlURL;
	}

	public String getDescription() {
		return description;
	}

	public String getFeedURL() {
		return feedURL;
	}

	public Vector<Hashtable<String, String>> getListResult() {
		return listResult;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public String getSessionName() {
		return sessionName;
	}

	public String getSessionString() {
		return sessionString;
	}

	public String getUsername() {
		return username;
	}

	public void setCmlURL(final String cmlURL) {
		this.cmlURL = cmlURL;
	}

	public void setConnectOperation(final String addr, final int port) {
		this.operation = SharingClientRequest.CONNECT;
		this.serverAddress = addr;
		this.serverPort = port;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setDownloadOperation(final String username, final String sessionName) {
		operation = SharingClientRequest.DOWNLOAD;
		this.username = username;
		this.sessionName = sessionName;
	}

	public void setFeedURL(final String feedURL) {
		this.feedURL = feedURL;
	}

	public void setListOperation() {
		operation = SharingClientRequest.LIST;
	}

	public void setListResult(final Vector<Hashtable<String, String>> listResult) {
		this.listResult = listResult;
	}

	public void setPublishOperation(final String username, final String sessionName, final String desc, final String sessionStr) {
		this.operation = SharingClientRequest.PUBLISH;
		this.username = username;
		this.sessionName = sessionName;
		this.description = desc;
		this.sessionString = sessionStr;
	}

	public void setServerAddress(final String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public void setServerPort(final int serverPort) {
		this.serverPort = serverPort;
	}

	public void setSessionName(final String sessionName) {
		this.sessionName = sessionName;
	}

	public void setSessionString(final String sessionString) {
		this.sessionString = sessionString;
	}

	public void setShutdownOperation(final String addr, final int port) {
		this.operation = SharingClientRequest.SHUTDOWN;
		this.serverAddress = addr;
		this.serverPort = port;
	}

	public void setSubscribeOperation(final String username, final String sessionName) {
		operation = SharingClientRequest.SUBSCRIBE;
		this.username = username;
		this.sessionName = sessionName;
	}

	public void setUsername(final String username) {
		this.username = username;
	}
}
