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
package corelyzer.controller.preprocessing;

import java.io.File;

import corelyzer.graphics.SceneGraph;

public class CRImageBlockPreProcessingController {
	public static void main(final String[] args) {
		System.out.println("- Test CRImageBlockPreProcessingController...");

		if (args.length < 2) {
			System.out.println("Usage: java <...ClassName...> inputDir outputDir");
			System.exit(0);
		}

		File inputDir = new File(args[0]);
		File outputDir = new File(args[1]);

		if (!outputDir.exists()) {
			boolean b = outputDir.mkdir();
			if (!b) {
				System.err.println("- Can't create output directory '" + outputDir + "'");
				System.exit(1);
			}
		}

		for (File inputFile : inputDir.listFiles()) {

			if (inputFile.exists() && inputFile.isFile()) {
				boolean b = CRImageBlockPreProcessingController.processImageBlocksOfAImage(inputFile.getAbsolutePath(), args[1]);

				String result = b ? "Success" : "Fail";
				System.out.println("- Preprocessing '" + inputFile.getAbsolutePath() + "': " + result);
			} else {
				System.out.println("- No file '" + inputFile.getAbsolutePath() + "'");
			}
		}
	}

	public static boolean processImageBlocksOfAImage(final String inputImageFile, final String outputDirectory) {
		return SceneGraph.genTextureBlocksToDirectory(inputImageFile, outputDirectory);
	}
}
