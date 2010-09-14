package corelyzer.lib.datamodel;

import java.net.URL;

/**
 * Defines the interface for image-related properties.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Image {
	String HORIZONTAL = "horizontal";
	String VERTICAL = "vertical";

	/**
	 * Gets the DPI in the X (width) direction independent of the orientation.
	 * 
	 * @return the DPI in the X (width) direction.
	 */
	double getDPIX();

	/**
	 * Gets the DPI in the Y (height) direction independent of the orientation.
	 * 
	 * @return the DPI in the Y (height) direction.
	 */
	double getDPIY();

	/**
	 * Gets the orientation of this image.
	 * 
	 * @return the orientation.
	 */
	String getOrientation();

	/**
	 * Gets the URL associated with this Image.
	 * 
	 * @return the URL.
	 */
	URL getURL();
}
