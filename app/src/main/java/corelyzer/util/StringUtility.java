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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

public class StringUtility {

	public static String capitalizeHeadingCharacter(final String value) {
		return value.length() > 0 ? Character.toUpperCase(value.charAt(0)) + value.substring(1) : value;
	}

	public static String expandNums(final String anIntString, final int length) {
		if (anIntString.length() > length) {
			return anIntString;
		} else {
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < length - anIntString.length(); ++i) {
				sb.append("0");
			}
			sb.append(anIntString);

			return sb.toString();
		}
	}

	// Courtesy of :
	// http://exampledepot.com/egs/java.awt.datatransfer/ToClip.html
	public static String getClipboard() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				String text = (String) t.getTransferData(DataFlavor.stringFlavor);
				return text;
			}
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {
		}

		return "";
	}

	public static String getExtension(final File f) {
		if (f != null) {
			String filename = f.getName();
			int i = filename.lastIndexOf('.');
			if (i > 0 && i < filename.length() - 1) {
				return filename.substring(i + 1).toLowerCase();
			}
		}

		return null;
	}

	public static String getSHASum(final String aString) {
		String hash;

		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(aString.getBytes());

			Hex hex = new Hex();
			hash = new String(hex.encode(md.digest()));
		} catch (NoSuchAlgorithmException e) {
			System.err.println("---> [Error] NoSuchAlgorithmException!");
			hash = "no-sha";
		}

		return hash;
	}

	public static String normalizeFilename(final String filePathString, final int length) {
		String filenameString = new File(filePathString).getName();

		if (filenameString.length() > length) {
			String extension = getExtension(new File(filePathString));
			int endIndex = length * 2 / 3;
			filenameString = filenameString.substring(0, endIndex) + "..." + extension;
		}

		return filenameString;
	}

	public static void setClipboard(final String aString) {
		StringSelection stringSelection = new StringSelection(aString);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

		clipboard.setContents(stringSelection, new ClipboardOwner() {

			public void lostOwnership(final Clipboard clipboard, final Transferable transferable) {
				// do nothing
			}
		});
	}

	// the reverse of expandNums()
	public static String shrinkStringToANumber(String inputStr) {
		while (inputStr.substring(0, 1).equals("0")) {
			inputStr = inputStr.substring(1);
		}

		return inputStr;
	}
}
