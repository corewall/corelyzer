//package com.sun.syndication.feed.module.core;
package corelyzer.util.core;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jdom.Element;
import org.jdom.Namespace;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleGenerator;

/**
 * A generator for the core info module.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreModuleGenerator implements ModuleGenerator {
	public static String DECIMAL_FORMAT = "0.00";

	private static final Set<Namespace> NAMESPACES;
	static {
		Set<Namespace> nss = new HashSet<Namespace>();
		nss.add(CoreModule.CORE_NS);
		NAMESPACES = Collections.unmodifiableSet(nss);
	}

	public void generate(final Module module, final Element element) {
		// put our namespace on the root element
		Element root = element;
		while ((root.getParent() != null) && (root.getParent() instanceof Element)) {
			root = (Element) root.getParent();
		}
		root.addNamespaceDeclaration(CoreModule.CORE_NS);

		// add our module info
		DecimalFormat fmt = new DecimalFormat(DECIMAL_FORMAT);
		CoreModule core = (CoreModule) module;
		element.addContent(generateElement(CoreModule.DEPTH_ELM, fmt.format(core.getDepth())));
		element.addContent(generateElement(CoreModule.LENGTH_ELM, fmt.format(core.getLength())));
	}

	private Element generateElement(final String name, final String value) {
		Element e = new Element(name, CoreModule.CORE_NS);
		e.addContent(value);
		return e;
	}

	public Set<Namespace> getNamespaces() {
		return NAMESPACES;
	}

	public String getNamespaceUri() {
		return CoreModule.CORE_URI;
	}
}
