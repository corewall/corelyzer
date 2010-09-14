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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class JanusUtility {
	public static float DEFAULT_DPI = 254.0f;
	public static float DEFAULT_DEPTH = 0.0f;

	/* Return Janus image access url with given expedition information */
	public static String generateJanusImageAccessURL(final String leg, final String site, final String hole, final String core, final String type,
			final String section) {

		String urlPrefix = "http://www.iodp.tamu.edu/publications/";

		String sectionName = site + hole.toLowerCase() + "_" + StringUtility.expandNums(core, 3) + type.toLowerCase() + "_"
				+ StringUtility.expandNums(section, 2);

		// noinspection UnnecessaryLocalVariable
		String imageURLStr = urlPrefix + leg + "_IR/VOLUME/CORES/JPEG/" + site + hole.toLowerCase() + "/" + sectionName + ".jpg";

		return imageURLStr;
	}

	public static String generateJanusImageName(final String leg, final String site, final String hole, final String core, final String type,
			final String section) {
		String urlString = generateJanusImageAccessURL(leg, site, hole, core, type, section);
		String[] toks = urlString.split("/");

		return toks[toks.length - 1];
	}

	public static String getJanusImageDepth(final String leg, final String site, final String hole, final String core, final String type, final String section)
			throws IOException {
		// http://services.chronos.org/xqe/public/iodp.janus.core-images?callback=displayNexus&site=1215&hole=B&serializeAs=tsv
		String query = "http://services.chronos.org/xqe/public" + "/iodp.janus.core-images?callback=displayNexus" + "&leg=" + leg + "&site=" + site + "&hole="
				+ hole + "&serializeAs=tsv" + "&noHeader=true";

		URL remote = new URL(query);
		URLConnection uc = remote.openConnection();
		InputStreamReader isr = new InputStreamReader(uc.getInputStream());
		BufferedReader br = new BufferedReader(isr);

		String line;
		while ((line = br.readLine()) != null) {
			String[] tok = line.split("\t");

			// Example return row:
			String legNumber = tok[0];
			String siteNumber = tok[1];
			String holeNumber = tok[2];
			String coreNumber = tok[3];
			String sectionNumber = tok[4];

			// float length = Float.parseFloat(tok[8]);
			// String loc = tok[13];
			if (legNumber.equals(leg) && siteNumber.equals(site) && holeNumber.toLowerCase().equals(hole.toLowerCase()) && coreNumber.equals(core)
					&& sectionNumber.equals(section)) {
				return tok[10]; // depth
			}
		}

		return "0.0";
	}

	public static String getJanusImageDPI(final String leg, final String site, final String hole, final String core, final String type, final String section)
			throws IOException {
		String query = "http://services.chronos.org/xqe/public" + "/iodp.janus.core-images?callback=displayNexus" + "&leg=" + leg + "&site=" + site + "&hole="
				+ hole + "&serializeAs=tsv" + "&noHeader=true";

		URL remote = new URL(query);
		URLConnection uc = remote.openConnection();
		InputStreamReader isr = new InputStreamReader(uc.getInputStream());
		BufferedReader br = new BufferedReader(isr);

		String line;
		while ((line = br.readLine()) != null) {
			String[] tok = line.split("\t");

			// Example return row:
			String legNumber = tok[0];
			String siteNumber = tok[1];
			String holeNumber = tok[2];
			String coreNumber = tok[3];
			String sectionNumber = tok[4];

			// float length = Float.parseFloat(tok[8]);
			// String loc = tok[13];
			if (legNumber.equals(leg) && siteNumber.equals(site) && holeNumber.toLowerCase().equals(hole.toLowerCase()) && coreNumber.equals(core)
					&& sectionNumber.equals(section)) {
				return tok[12]; // dpi
			}
		}

		return "-1.0";
	}

	public static float getJanusSectionDepth(final String url, final String sessionName, final String trackName, final float inputDepth) {
		String[] toks = sessionName.split("_");
		String leg = toks[0];
		String site = toks[1];

		toks = trackName.split("_");
		String hole = toks[0];
		String core = toks[1];

		toks = url.split("/");
		String sectionName = toks[toks.length - 1];

		toks = sectionName.split("_");
		String type = toks[1].substring(toks[1].length() - 1);

		String section = toks[toks.length - 1];
		int dotPos = section.lastIndexOf(".");
		section = section.substring(0, dotPos);
		section = StringUtility.shrinkStringToANumber(section);

		System.out.println("---> [Janus] " + leg + ", " + site + ", " + hole + ", " + core + ", " + type + ", " + section);

		try {
			return Float.valueOf(getJanusImageDepth(leg, site, hole, core, type, section));
		} catch (Exception e) {
			System.out.println("---> [WARN] Cannot get depth from ChronosJanus.");
			return inputDepth;
		}
	}

	public static String getJanusSectionInfo(final String url, final String sessionName, final String trackName) throws IOException {
		String[] toks = sessionName.split("_");
		String leg = toks[0];
		String site = toks[1];

		toks = trackName.split("_");
		String hole = toks[0];
		String core = toks[1];

		toks = url.split("/");
		String sectionName = toks[toks.length - 1];

		toks = sectionName.split("_");
		String type = toks[1].substring(toks[1].length() - 1);

		String section = toks[toks.length - 1];
		int dotPos = section.lastIndexOf(".");
		section = section.substring(0, dotPos);
		section = StringUtility.shrinkStringToANumber(section);

		System.out.println("---> [Janus] " + leg + ", " + site + ", " + hole + ", " + core + ", " + type + ", " + section);

		// http://services.chronos.org/xqe/public/iodp.janus.core-images?callback=displayNexus&site=1215&hole=B&serializeAs=tsv
		String query = "http://services.chronos.org/xqe/public" + "/iodp.janus.core-images?callback=displayNexus" + "&leg=" + leg + "&site=" + site + "&hole="
				+ hole + "&serializeAs=tsv" + "&noHeader=true";

		URL remote = new URL(query);
		URLConnection uc = remote.openConnection();
		InputStreamReader isr = new InputStreamReader(uc.getInputStream());
		BufferedReader br = new BufferedReader(isr);

		String line;
		while ((line = br.readLine()) != null) {
			String[] tok = line.split("\t");

			// Example return row:
			String legNumber = tok[0];
			String siteNumber = tok[1];
			String holeNumber = tok[2];
			String coreNumber = tok[3];
			String sectionNumber = tok[4];

			if (legNumber.equals(leg) && siteNumber.equals(site) && holeNumber.toLowerCase().equals(hole.toLowerCase()) && coreNumber.equals(core)
					&& sectionNumber.equals(section)) {
				return line;
			}
		}

		return null;
	}

	public static void main(final String[] args) {
		System.out.println("Test Getting depth from ChronosJanus");
	}
}
