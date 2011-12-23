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
package corelyzer.util;

import java.io.CharArrayReader;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import corelyzer.util.core.CoreModule;
import corelyzer.util.image.ImageModule;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;

public class FeedUtils {
	public static CoreModule getCoreModule(final String xmlString) {
		if (!isValidSyndEntry(xmlString)) {
			return null;
		}

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Reader reader = new CharArrayReader(xmlString.toCharArray());
			Document doc = builder.parse(new org.xml.sax.InputSource(reader));

			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(doc);

			CoreModule cmod = (CoreModule) feed.getModule(CoreModule.CORE_URI);
			if (cmod != null) {
				return cmod;
			}
		} catch (Exception e) {
			System.err.println("Error! " + e);
			e.printStackTrace();
		}

		return null;
	}

	public static ImageModule getImageModule(final String xmlString) {
		if (!isValidSyndEntry(xmlString)) {
			return null;
		}

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Reader reader = new CharArrayReader(xmlString.toCharArray());
			Document doc = builder.parse(new org.xml.sax.InputSource(reader));

			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(doc);

			ImageModule imod = (ImageModule) feed.getModule(ImageModule.IMAGE_URI);
			if (imod != null) {
				return imod;
			}
		} catch (Exception e) {
			System.err.println("Error! " + e);
			e.printStackTrace();
		}

		return null;
	}

	public static boolean isValidSyndEntry(final String xmlString) {
		try {
			// Create a factory
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Reader reader = new CharArrayReader(xmlString.toCharArray());
			Document doc = builder.parse(new org.xml.sax.InputSource(reader));

			Node node = doc.getDocumentElement();
			if (node instanceof Element) {
				Element e = (Element) node;

				return e.getTagName().equalsIgnoreCase("entry");
			}
		} catch (Exception e) {
			System.err.println("Error! " + e);
			// e.printStackTrace();
		}

		return false;
	}

	public static void main(final String[] args) {
		System.out.println("SyndEntry test");

		String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:core=\"http://www.corewall.org/core\"\n"
				+ "xmlns:taxo=\"http://purl.org/rss/1.0/modules/taxonomy/\"\n"
				+ "xmlns:image=\"http://www.corewall.org/image\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
				+ "<title>A2As_0102.32-0103.32.jp2</title>\n" + "<link rel=\"alternate\"\n"
				+ "href=\"http://sms.andrill.org/Science/Images/split%20core/processed/0-2/A2As_0102.32-0103.32.jp2\"/>\n"
				+ "<category term=\"split-core-image\"/>\n" + "<author>\n" + "<name>AND2-2A Science Team</name>\n" + "</author>\n"
				+ "<id>tag:sms.andrill.org,2008-08-06:/Science/Images/split%20core/processed/0-2/A2As_0102.32-0103.32.jp2</id>\n"
				+ "<updated>2008-02-06T16:33:59Z</updated>\n" + "<published>2008-02-06T16:33:59Z</published>\n"
				+ "<summary>A2As_0102.32-0103.32.jp2</summary>\n" + "<dc:creator>AND2-2A Science Team</dc:creator>\n"
				+ "<dc:publisher>http://andrill.org</dc:publisher>\n" + "<dc:type>http://purl.org/dc/dcmitype/Image</dc:type>\n"
				+ "<dc:date>2008-02-06T16:33:59Z</dc:date>\n" + "<core:depth>102.32</core:depth>\n" + "<core:length>1.0</core:length>\n"
				+ "<image:orientation>vertical</image:orientation>\n" + "<image:dpiX>114.5</image:dpiX>\n" + "<image:dpiY>127.1524</image:dpiY>\n" + "</entry>";

		String notAEntryString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><corewall/>";

		boolean isEntry = isValidSyndEntry(notAEntryString);
		System.out.println("notAEntryString is valid: " + isEntry);

		isEntry = isValidSyndEntry(xmlString);
		System.out.println("xmlString is valid: " + isEntry);

		if (isEntry) {
			CoreModule module1 = getCoreModule(xmlString);
			if (module1 == null) {
				System.out.println("Has no CoreModule");
			} else {
				System.out.println("Core module: length: " + module1.getLength() + ", depth: " + module1.getDepth());
			}

			ImageModule module2 = getImageModule(xmlString);
			if (module2 == null) {
				System.out.println("Has no ImageModule");
			} else {
				System.out.println("Image module: dpix: " + module2.getDPIX() + ", dpiy: " + module2.getDPIY() + ", orientation: " + module2.getOrientation());
			}
		}
	}
}
