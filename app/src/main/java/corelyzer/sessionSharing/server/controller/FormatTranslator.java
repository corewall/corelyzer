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

import java.io.File;

import corelyzer.util.CMLAtomConvertor;

public class FormatTranslator {
	public static int convertCMLtoAtomFeed(final File inputFile, final File outputFile, final String author, final String project, final String desc) {
		if (!inputFile.exists()) {
			return -1;
		}

		System.out.println("Converting '" + inputFile + "' to '" + outputFile + "'");
		CMLAtomConvertor.CML2AtomFeed(inputFile, outputFile, author, project, desc);

		return 0;
	}

	public static void main(final String[] args) {
		// for quick test
		System.out.println("Hello FormatTranslator Test!");
		String cmlPath = "/Users/julian/Desktop/cml2atom.cml";
		String feedPath = "/Users/julian/Desktop/feed.xml";

		int res = FormatTranslator.convertCMLtoAtomFeed(new File(cmlPath), new File(feedPath), "demoUser", "demoProject", "demoDesc");

		if (res != 0) {
			System.err.println("Conversion test failed!");
		} else {
			System.out.println("Conversion test success!");
		}
	}
}
