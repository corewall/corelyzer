//package com.sun.syndication.feed.module.core;
package corelyzer.util.core;

import com.sun.syndication.feed.module.ModuleImpl;

/**
 * An implementation of the CoreModule interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreModuleImpl extends ModuleImpl implements CoreModule {
	private static final long serialVersionUID = 2881003877097011952L;
	private double depth = 0.0;
	private double length = 0.0;

	/**
	 * Create a new CoreInfoModuleImpl.
	 */
	public CoreModuleImpl() {
		super(CoreModuleImpl.class, CoreModule.CORE_URI);
	}

	public void copyFrom(final Object obj) {
		CoreModuleImpl copyFrom = (CoreModuleImpl) obj;
		setDepth(copyFrom.getDepth());
		setLength(copyFrom.length);
	}

	public double getDepth() {
		return depth;
	}

	public Class<CoreModule> getInterface() {
		return CoreModule.class;
	}

	public double getLength() {
		return length;
	}

	public void setDepth(final double depth) {
		this.depth = depth;
	}

	public void setLength(final double length) {
		this.length = length;
	}
}
