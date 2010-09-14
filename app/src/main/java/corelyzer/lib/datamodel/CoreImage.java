package corelyzer.lib.datamodel;

import java.net.URL;

/**
 * A class that implements the Core and Image interfaces.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreImage implements Core, Image {
	private final double depth;
	private final double length;
	private final double dpiX;
	private final double dpiY;
	private final String orientation;
	private final URL url;
	private final CoreImageConfiguration config;

	/**
	 * Create a new CoreImage.
	 */
	public CoreImage(final URL url, final double depth, final double length, final double dpiX, final double dpiY, final String orientation,
			final CoreImageConfiguration config) {
		this.url = url;
		this.depth = depth;
		this.length = length;
		this.dpiX = dpiX;
		this.dpiY = dpiY;
		this.orientation = orientation;
		if (config == null) {
			this.config = new CoreImageConfiguration();
		} else {
			this.config = config;
		}
	}

	/**
	 * Gets the configuration associated with this core image.
	 * 
	 * @return the configuration.
	 */
	public CoreImageConfiguration getConfiguration() {
		return config;
	}

	public double getDepth() {
		return depth;
	}

	public double getDPIX() {
		return dpiX;
	}

	public double getDPIY() {
		return dpiY;
	}

	public double getLength() {
		return length;
	}

	public String getOrientation() {
		return orientation;
	}

	public URL getURL() {
		return url;
	}
}
