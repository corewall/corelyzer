//package com.sun.syndication.feed.module.image;
package corelyzer.util.image;

import org.jdom.Element;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleParser;

/**
 * A parser for the image info module.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ImageModuleParser implements ModuleParser {

	public String getNamespaceUri() {
		return ImageModule.IMAGE_URI;
	}

	public Module parse(final Element root) {
		boolean found = false;
		ImageModule module = new ImageModuleImpl();

		// look for our DPI X element
		Element e = root.getChild(ImageModule.DPIX_ELM, ImageModule.IMAGE_NS);
		if (e != null) {
			try {
				module.setDPIX(Double.parseDouble(e.getTextTrim()));
				found = true;
			} catch (NumberFormatException nfe) {
				System.err.println("Invalid number for dpix: " + e.getTextTrim());
			}
		}

		// look for our DPI Y element
		e = root.getChild(ImageModule.DPIY_ELM, ImageModule.IMAGE_NS);
		if (e != null) {
			try {
				module.setDPIY(Double.parseDouble(e.getTextTrim()));
				found = true;
			} catch (NumberFormatException nfe) {
				System.err.println("Invalid number for dpiy: " + e.getTextTrim());
			}
		}

		// look for out orientation element
		e = root.getChild(ImageModule.ORIENTATION_ELM, ImageModule.IMAGE_NS);
		if (e != null) {
			String text = e.getTextTrim().toLowerCase();
			if (text.equals("vertical")) {
				module.setOrientation(ImageModule.VERTICAL);
			} else {
				module.setOrientation(ImageModule.HORIZONTAL);
			}
		}

		// look for our length element
		e = root.getChild(ImageModule.THUMBNAIL_ELM, ImageModule.IMAGE_NS);
		if (e != null) {
			module.setThumbnail(e.getTextTrim());
			found = true;
		}

		// return the module if found
		return found ? module : null;
	}
}
