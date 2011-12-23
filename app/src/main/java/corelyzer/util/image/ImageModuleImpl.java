//package com.sun.syndication.feed.module.image;
package corelyzer.util.image;

import com.sun.syndication.feed.module.ModuleImpl;

/**
 * An implementation of the ImageModule interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ImageModuleImpl extends ModuleImpl implements ImageModule {
	private static final long serialVersionUID = -7003510567346453873L;
	private String orientation = ImageModule.HORIZONTAL;
	private double dpiX = 0.0;
	private double dpiY = 0.0;
	private String thumbnail = null;

	/**
	 * Create a new ImageModuleImpl.
	 */
	public ImageModuleImpl() {
		super(ImageModuleImpl.class, ImageModule.IMAGE_URI);
	}

	public void copyFrom(final Object obj) {
		ImageModuleImpl from = (ImageModuleImpl) obj;
		setOrientation(from.getOrientation());
		setDPIX(from.getDPIX());
		setDPIY(from.getDPIY());
	}

	public double getDPIX() {
		return dpiX;
	}

	public double getDPIY() {
		return dpiY;
	}

	public Class<ImageModule> getInterface() {
		return ImageModule.class;
	}

	public String getOrientation() {
		return orientation;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setDPIX(final double dpiX) {
		this.dpiX = dpiX;
	}

	public void setDPIY(final double dpiY) {
		this.dpiY = dpiY;
	}

	public void setOrientation(final String orientation) {
		this.orientation = orientation;
	}

	public void setThumbnail(final String imgURL) {
		thumbnail = imgURL;
	}
}
