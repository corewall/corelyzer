package com.sun.syndication.feed.module.image;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jdom.Element;
import org.jdom.Namespace;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleGenerator;

/**
 * A generator for the image info module.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ImageModuleGenerator implements ModuleGenerator {
	public static String DECIMAL_FORMAT = "0.0";

	private static final Set<Namespace> NAMESPACES;
	static {
		Set<Namespace> nss = new HashSet<Namespace>();
		nss.add(ImageModule.IMAGE_NS);
		NAMESPACES = Collections.unmodifiableSet(nss);
	}

	public void generate(final Module module, final Element element) {
		DecimalFormat fmt = new DecimalFormat(DECIMAL_FORMAT);

		// put our namespace on the root element
		Element root = element;
		while ((root.getParent() != null) && (root.getParent() instanceof Element)) {
			root = (Element) root.getParent();
		}
		root.addNamespaceDeclaration(ImageModule.IMAGE_NS);

		// add our module info
		ImageModule image = (ImageModule) module;
		if (image.getOrientation().equalsIgnoreCase(ImageModule.VERTICAL)) {
			element.addContent(generateElement(ImageModule.ORIENTATION_ELM, "vertical"));
		} else {
			element.addContent(generateElement(ImageModule.ORIENTATION_ELM, "horizontal"));
		}
		element.addContent(generateElement(ImageModule.DPIX_ELM, fmt.format(image.getDPIX())));
		element.addContent(generateElement(ImageModule.DPIY_ELM, fmt.format(image.getDPIY())));
		if (image.getThumbnail() != null) {
			element.addContent(generateElement(ImageModule.THUMBNAIL_ELM, image.getThumbnail()));
		}
	}

	private Element generateElement(final String name, final String value) {
		Element e = new Element(name, ImageModule.IMAGE_NS);
		e.addContent(value);
		return e;
	}

	public Set<Namespace> getNamespaces() {
		return NAMESPACES;
	}

	public String getNamespaceUri() {
		return ImageModule.IMAGE_URI;
	}
}
