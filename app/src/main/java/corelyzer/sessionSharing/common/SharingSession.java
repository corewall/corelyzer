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
package corelyzer.sessionSharing.common;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import corelyzer.util.FileUtility;

public class SharingSession {

	String name;
	String author;
	String description;

	String cmlLoc;
	String feedLoc;

	Date createDate;
	Date updateDate;

	public SharingSession() {
	}

	public SharingSession(final String aName, final String anAuthor, final String aDesc, final String aSession) {
		this();

		this.setName(aName);
		this.setAuthor(anAuthor);
		this.setDescription(aDesc);

		// todo save the session as a file, update cmlLoc
		String sp = System.getProperty("file.separator");
		cmlLoc = author + sp + name + sp + "session.cml";
		feedLoc = author + sp + name + sp + "feed.xml";

		String prefix = "/tmp/CSS";
		File aFile = new File(prefix + sp + cmlLoc);
		try {
			FileUtility.saveAStringAsAFile(aSession, aFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		createDate = new Date(System.currentTimeMillis());
		updateDate = new Date(System.currentTimeMillis());
	}

	public String getAuthor() {
		return author;
	}

	public String getCmlLoc() {
		return cmlLoc;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public String getDescription() {
		return description;
	}

	public String getFeedLoc() {
		return feedLoc;
	}

	public String getName() {
		return name;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setAuthor(final String author) {
		this.author = author;
	}

	public void setCmlLoc(final String cmlLoc) {
		this.cmlLoc = cmlLoc;
	}

	public void setCreateDate(final Date createDate) {
		this.createDate = createDate;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setFeedLoc(final String feedLoc) {
		this.feedLoc = feedLoc;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setUpdateDate(final Date updateDate) {
		this.updateDate = updateDate;
	}
}
