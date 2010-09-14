package corelyzer.lib.datamodel;

import java.util.Properties;

/**
 * Configuration options for parsing core images.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreImageConfiguration extends Properties {
	public static final long serialVersionUID = 6168422557598761723L;
	public static final String KEY_PATTERN = "pattern";
	public static final String KEY_DPI_X = "dpix";
	public static final String KEY_DPI_Y = "dpiy";
	public static final String KEY_ORIENTATION = "orientation";
	public static final String KEY_BASE_URL = "base-url";
	public static final String KEY_BASE_DIR = "base-dir";
	public static final String KEY_TRACK = "track";

	private static final String DEFAULT_PATTERN = "((\\d+)\\.(\\d+))";

	/**
	 * Gets the base directory.
	 * 
	 * @return the base directory or null if not set.
	 */
	public String getBaseDir() {
		return getProperty(KEY_BASE_DIR);
	}

	/**
	 * Gets the base URL.
	 * 
	 * @return the base URL or null if not set.
	 */
	public String getBaseURL() {
		return getProperty(KEY_BASE_URL);
	}

	/**
	 * Gets the default DPI in the X direction independent of orientation.
	 * 
	 * @return the DPI X or -1 if not set.
	 */
	public double getDPIX() {
		return Double.parseDouble(getProperty(KEY_DPI_X, "-1.0"));
	}

	/**
	 * Gets the default DPI in the Y direction independent of orientation.
	 * 
	 * @return the DPI Y or -1 if not set.
	 */
	public double getDPIY() {
		return Double.parseDouble(getProperty(KEY_DPI_Y, "-1.0"));
	}

	/**
	 * Gets the default image orientation.
	 * 
	 * @return the orientation.
	 */
	public String getOrientation() {
		return getProperty(KEY_ORIENTATION, Image.VERTICAL);
	}

	/**
	 * Gets the depth pattern.
	 * 
	 * @return the depth pattern.
	 */
	public String getPattern() {
		return getProperty(KEY_PATTERN, DEFAULT_PATTERN);
	}

	/**
	 * Gets the track.
	 * 
	 * @return the track.
	 */
	public String getTrack() {
		return getProperty(KEY_TRACK);
	}

	/**
	 * Load the configuration from a Properties object.
	 * 
	 * @param props
	 *            the properties.
	 */
	public void load(final Properties props) {
		for (Object key : props.keySet()) {
			put(key, props.get(key));
		}
	}

	/**
	 * Sets the base directory.
	 * 
	 * @param baseDir
	 *            the base directory.
	 */
	public void setBaseDir(final String baseDir) {
		setProperty(KEY_BASE_DIR, baseDir);
	}

	/**
	 * Sets the base URL.
	 * 
	 * @param baseURL
	 *            the base URL.
	 */
	public void setBaseURL(final String baseURL) {
		setProperty(KEY_BASE_URL, baseURL);
	}

	/**
	 * Sets the DPI X.
	 * 
	 * @param dpix
	 *            the DPI X.
	 */
	public void setDPIX(final double dpix) {
		setProperty(KEY_DPI_X, "" + dpix);
	}

	/**
	 * Sets the DPI Y.
	 * 
	 * @param dpiy
	 *            the DPI Y.
	 */
	public void setDPIY(final double dpiy) {
		setProperty(KEY_DPI_Y, "" + dpiy);
	}

	/**
	 * Sets the orientation.
	 * 
	 * @param orientation
	 *            the orientation.
	 */
	public void setOrientation(final String orientation) {
		setProperty(KEY_ORIENTATION, orientation);
	}

	/**
	 * Sets the depth pattern.
	 * 
	 * @param pattern
	 *            the depth pattern.
	 */
	public void setPattern(final String pattern) {
		setProperty(KEY_PATTERN, pattern);
	}

	/**
	 * Sets the track.
	 * 
	 * @param track
	 *            the track.
	 */
	public void setTrack(final String track) {
		setProperty(KEY_TRACK, track);
	}
}
