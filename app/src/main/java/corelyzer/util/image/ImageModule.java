//package com.sun.syndication.feed.module.image;
package corelyzer.util.image;

import org.jdom.Namespace;

import com.sun.syndication.feed.module.Module;

/**
 * The interface for the image info module.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface ImageModule extends Module {
	/**
	 * The image info namespace URI.
	 */
	public static final String IMAGE_URI = "http://www.corewall.org/image";

	/**
	 * The image info namespace.
	 */
	public static final Namespace IMAGE_NS = Namespace.getNamespace("image", IMAGE_URI);

	/**
	 * The orientation element.
	 */
	public static final String ORIENTATION_ELM = "orientation";

	public static final String VERTICAL = "vertical";
	public static final String HORIZONTAL = "horizontal";

	/**
	 * The DPI X element.
	 */
	public static final String DPIX_ELM = "dpiX";

	/**
	 * The DPI Y element.
	 */
	public static final String DPIY_ELM = "dpiY";

	/**
	 * The thumbnail element tag.
	 */
	public static final String THUMBNAIL_ELM = "thumbnail";

	/**
	 * Gets the DPI in the X (width) direction independent of the orientation.
	 * 
	 * @return the DPI in the X (width) direction.
	 */
	public double getDPIX();

	/**
	 * Gets the DPI in the Y (height) direction independent of the orientation.
	 * 
	 * @return the DPI in the Y (height) direction.
	 */
	public double getDPIY();

	/**
	 * Gets the orientation of the image.
	 * 
	 * @return the orientation.
	 */
	public String getOrientation();

	// TODO: add any new image info properties

	/**
	 * Gets the thumbnail URL.
	 * 
	 * @return the thumbnail URL.
	 */
	public String getThumbnail();

	/**
	 * Sets the DPI in the X (width) direction independent of the orientation.
	 * 
	 * @param dpiX
	 *            the DPI in the X (width) direction.
	 */
	public void setDPIX(double dpiX);

	/**
	 * Sets the DPI in the Y (height) direction independent of the orientation.
	 * 
	 * @param dpiY
	 *            the DPI in the Y (height) direction.
	 */
	public void setDPIY(double dpiY);

	/**
	 * Sets the orientation of the image.
	 * 
	 * @param orientation
	 *            the orientation, either VERTICAL or HORIZONTAL, of the image.
	 */
	public void setOrientation(String orientation);

	/**
	 * Sets the thumbnail URL.
	 * 
	 * @param imgURL
	 *            the thumbnail URL.
	 */
	public void setThumbnail(String imgURL);
}
