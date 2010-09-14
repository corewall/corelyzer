/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2007 Julian Yu-Chung Chen
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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.syndication.feed.module.core.CoreModule;
import com.sun.syndication.feed.module.core.CoreModuleImpl;
import com.sun.syndication.feed.module.image.ImageModule;
import com.sun.syndication.feed.module.image.ImageModuleImpl;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

public class CMLAtomConvertor {
	final String sp = System.getProperty("file.separator");

	static File inFile = null;
	static File outFile = null;

	static List<SyndEntry> entries = null;

	static String authorName = "iCoresAuthor";
	static String projectName = "iCoresProject";
	static String projectDescription = "A example description";

	@SuppressWarnings({ "ConstantConditions" })
	public static void CML2AtomFeed(final File aCML, final File anAtomFeed, final String anAuthor, final String aProject, final String aDescription) {
		if (!aCML.exists()) {
			return;
		}

		inFile = aCML;
		outFile = anAtomFeed;

		authorName = anAuthor;
		projectName = aProject;
		projectDescription = aDescription;

		try {
			DOMParser parser = new DOMParser();
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);

			String inputFile = aCML.toURI().toURL().toString();
			parser.parse(inputFile);

			Document doc = parser.getDocument();

			Element e = doc.getDocumentElement();
			String str;

			// Needs version 1.0
			str = e.getAttribute("version");
			System.out.println("---> cml version: " + str);

			float version;
			if (!str.equals("")) {
				version = Float.valueOf(str);
			} else {
				version = 0.5f;
			}

			// Prepare Atom elements
			SyndFeed feed = new SyndFeedImpl();
			feed.setFeedType("atom_1.0");
			feed.setUri("tag:corewall.org," + ROMEUtils.DATE.format(new Date()) + ":/" + outFile);
			feed.setTitle(projectName + ": " + inFile.getName());
			feed.setAuthor(authorName);
			feed.setLinks(new ArrayList());
			feed.getLinks().add(ROMEUtils.createLink(outFile.toURI().toURL().toString(), "self", null, null));
			feed.setDescription(projectDescription);
			feed.setPublishedDate(new Date());

			List<SyndCategory> categories = new ArrayList<SyndCategory>();
			categories.add(ROMEUtils.createCategory("feed", null));
			feed.setCategories(categories);

			// prepare entries
			if (entries != null) {
				entries.clear();
				entries = null;
			}

			entries = new ArrayList<SyndEntry>();

			// parse input file
			if (version < 1.5) {
				System.err.println("Must use CML version 1.5 or above. '" + inputFile + "' is in version: " + version);
				return;
			} else {
				NodeList list = e.getChildNodes();

				for (int i = 0; i < list.getLength(); i++) {
					if (!(list.item(i) instanceof Element)) {
						continue;
					}

					Element sessionElement = (Element) list.item(i);
					String tagName = sessionElement.getNodeName();

					if (!tagName.equalsIgnoreCase("session")) {
						continue;
					}

					loadStateXML(sessionElement);
				}

			}

			feed.setEntries(entries);
			ROMEUtils.writeFeed(feed, outFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void loadCoreImageXML(final Element e) {
		String local = e.getAttribute("local");
		String url = e.getAttribute("urn");
		String dpi_x = e.getAttribute("dpi_x");
		String dpi_y = e.getAttribute("dpi_y");

		String filename = new File(local).getName();

		// ver 1.0 has position in meter, so convert it to pixel here
		float depth = Float.valueOf(e.getAttribute("depth"));
		float intervalBottom = Float.valueOf(e.getAttribute("intervalBottom"));
		float intervalTop = Float.valueOf(e.getAttribute("intervalTop"));
		float length = Math.abs(intervalBottom - intervalTop) / 100.0f;

		String orientation = e.getAttribute("orientation");
		if (orientation.equalsIgnoreCase("landscape")) {
			orientation = "horizontal";
		} else {
			orientation = "vertical";
		}

		// Output to AtomFeed
		// create our categories
		List<SyndCategory> categories = new ArrayList<SyndCategory>();
		categories.add(ROMEUtils.createCategory("image", null));

		SyndEntry entry = new SyndEntryImpl();
		entry.setUri("tag:corewall.org," + ROMEUtils.DATE.format(new Date()) + ":/cml/" + filename);
		entry.setTitle(filename + " core image");
		entry.setAuthor(authorName);
		entry.setLink(url);

		// FIXME Create 3 links for 3 levels
		/*
		 * 
		 * ArrayList<SyndLink> links = new ArrayList<SyndLink>(); SyndLinkImpl
		 * aLink = new SyndLinkImpl(); aLink.setHref(url);
		 * aLink.setType("orig"); links.add(aLink);
		 * 
		 * aLink = new SyndLinkImpl(); aLink.setHref(midUrl); links.add(aLink);
		 * 
		 * aLink = new SyndLinkImpl(); aLink.setHref(lowUrl); links.add(aLink);
		 * 
		 * entry.setLinks(links);
		 */

		entry.setCategories(categories);
		entry.setPublishedDate(new Date());
		SyndContent content = new SyndContentImpl();
		content.setValue("This is image of filename: " + filename);
		entry.setDescription(content);

		if (entries != null) {
			entries.add(entry);
		}

		// add our core info
		CoreModule module = new CoreModuleImpl();
		module.setDepth(depth);
		module.setLength(length);
		// noinspection unchecked
		entry.getModules().add(module);

		ImageModule iModule = new ImageModuleImpl();
		iModule.setDPIX(Float.valueOf(dpi_x));
		iModule.setDPIY(Float.valueOf(dpi_y));

		iModule.setOrientation(orientation);
		// noinspection unchecked
		entry.getModules().add(iModule);
	}

	static void loadDataSetXML(final Element e) {
		String filename = e.getAttribute("local");
		String urn = e.getAttribute("urn");

		// Use extension to select different file type handlers
		String fileExtension = FileUtility.getFileExtension(filename);

		SyndEntry entry = new SyndEntryImpl();
		entry.setTitle(filename + " dataset");
		entry.setAuthor(authorName);
		entry.setLink(urn);

		List<SyndCategory> categories = new ArrayList<SyndCategory>();
		categories.add(ROMEUtils.createCategory(fileExtension + " dataset", null));
		entry.setCategories(categories);

		entry.setPublishedDate(new Date());
		SyndContent content = new SyndContentImpl();
		content.setValue("This is dataset file of type: " + fileExtension + " filename: " + filename);
		entry.setDescription(content);

		if (entries != null) {
			entries.add(entry);
		}
	}

	static void loadStateXML(final Element xmlRoot) {
		NodeList list = xmlRoot.getChildNodes();

		for (int i = 0; i < list.getLength(); i++) {
			if (!(list.item(i) instanceof Element)) {
				continue;
			}

			Element e = (Element) list.item(i);
			String tagname = e.getTagName();

			if (tagname.equals("dataset")) {
				System.out.println("--- Loading dataset ----");
				loadDataSetXML(e);
			} else if (tagname.equals("visual")) {
				String type = e.getAttribute("type");

				if (type.equals("annothread")) {
					System.out.println("--- [Ignored] Annotation in loadStateXML");
					// this.loadAnnotationXML(e, -1, -1);
				} else if (type.equals("track")) {
					System.out.println("--- Loading Track ----");
					loadTrackXML(e);
				}
			} else {
				System.out.println("---> Something I don't know: " + tagname);
			}
		}
	}

	// Load track information with XML root element
	static void loadTrackXML(final Element e) {
		e.getAttribute("name");

		// go through annotations && images
		NodeList list = e.getChildNodes();

		for (int i = 0; i < list.getLength(); i++) {

			if (!(list.item(i) instanceof Element)) {
				continue;
			}

			Element c = (Element) list.item(i);

			String tagname = c.getTagName();

			if (tagname.equals("visual")) {
				String type = c.getAttribute("type");

				if (type.equals("core_section")) {
					loadCoreImageXML(c);
				}
			}
		}

	}

	/*
	 * TODO public static void AtomFeed2CML(File anAtomFeed, File aCML) { }
	 * 
	 * public static String CML2AtomFeed(String aCML) { return ""; }
	 * 
	 * public static String AtomFeed2CML(String anAtomFeed) { return ""; }
	 */

	public static void main(final String[] args) {
		System.out.println("Hello World! ");

		if (args.length != 2) {
			System.out.println("Usage: command <inputCML> <outputAtom>");
			return;
		}

		String cmlFile = args[0];
		String atomFile = args[1];

		File inputFile = new File(cmlFile);
		File outputFile = new File(atomFile);

		if (!inputFile.exists()) {
			System.out.println("Inputfile '" + inputFile + "' does not exist");
			return;
		}

		System.out.println("Convert '" + cmlFile + "' to '" + atomFile + "'");

		CMLAtomConvertor.CML2AtomFeed(inputFile, outputFile, "DemoUser", "DemoProject", "DemoDescs");

		System.out.println("Done!");
	}
}
