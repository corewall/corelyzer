package com.sun.syndication.feed.module.core;

import org.jdom.Element;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleParser;

/**
 * A parser for the core info module.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreModuleParser implements ModuleParser {

	public String getNamespaceUri() {
		return CoreModule.CORE_URI;
	}

	public Module parse(final Element root) {
		boolean found = false;
		CoreModule module = new CoreModuleImpl();

		// look for our depth element
		Element e = root.getChild(CoreModule.DEPTH_ELM, CoreModule.CORE_NS);
		if (e != null) {
			try {
				module.setDepth(Double.parseDouble(e.getTextTrim()));
				found = true;
			} catch (NumberFormatException nfe) {
				System.err.println("Invalid number for depth: " + e.getTextTrim());
			}
		}

		// look for our length element
		e = root.getChild(CoreModule.LENGTH_ELM, CoreModule.CORE_NS);
		if (e != null) {
			try {
				module.setLength(Double.parseDouble(e.getTextTrim()));
				found = true;
			} catch (NumberFormatException nfe) {
				System.err.println("Invalid number for length: " + e.getTextTrim());
			}
		}

		// return the module if found
		return found ? module : null;
	}
}
