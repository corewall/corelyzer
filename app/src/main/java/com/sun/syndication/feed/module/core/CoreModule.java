package com.sun.syndication.feed.module.core;

import org.jdom.Namespace;

import com.sun.syndication.feed.module.Module;

/**
 * The interface for the core info module.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface CoreModule extends Module {
	/**
	 * The core info namespace URI.
	 */
	public static final String CORE_URI = "http://www.corewall.org/core";

	/**
	 * The depth element tag.
	 */
	public static final String DEPTH_ELM = "depth";

	/**
	 * The length element tag.
	 */
	public static final String LENGTH_ELM = "length";

	/**
	 * The core info namespace.
	 */
	public static final Namespace CORE_NS = Namespace.getNamespace("core", CORE_URI);

	/**
	 * Gets the depth of the core.
	 * 
	 * @return the depth of the core.
	 */
	public double getDepth();

	/**
	 * Gets the length of the core.
	 * 
	 * @return the length of the core.
	 */
	public double getLength();

	// TODO: add any new core info property methods

	/**
	 * Sets the depth of the core.
	 * 
	 * @param depth
	 *            the depth.
	 */
	public void setDepth(double depth);

	/**
	 * Sets the length of the core.
	 * 
	 * @param length
	 *            the length.
	 */
	public void setLength(double length);
}
