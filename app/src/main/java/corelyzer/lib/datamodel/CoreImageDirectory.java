package corelyzer.lib.datamodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corelyzer.lib.datamodel.util.ImageFileFilter;
import corelyzer.lib.datamodel.util.ImageInfo;

/**
 * A directory of core images.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreImageDirectory {
	private static final String CONFIG_FILE = "corelyzer.properties";
	private static final String SEPARATOR = "\\D+";
	private static final double M_PER_IN = 2.54 / 100;
	private static ImageFileFilter FILTER = new ImageFileFilter();

	/**
	 * Parse a CoreImage from the specified file.
	 * 
	 * @param file
	 *            the file.
	 * @return the
	 */
	public static CoreImage parseImage(final File file, final CoreImageConfiguration config) {
		// create our ImageInfo object
		ImageInfo ii = new ImageInfo();
		try {
			ii.setInput(new FileInputStream(file));
			if (!ii.check()) {
				return null;
			}
		} catch (FileNotFoundException e) {
			return null;
		}

		// set our orientation and DPIs
		String orientation = config.getOrientation();
		double dpiX = config.getDPIX();
		double dpiY = config.getDPIY();
		double depth = -1.0;
		double length = -1.0;

		// create our depth pattern matcher and parse our depth
		Pattern pattern = Pattern.compile(config.getPattern());
		Matcher matcher = pattern.matcher(file.getAbsolutePath());
		if (matcher.find()) {
			// found a depth, so parse it
			try {
				depth = Double.parseDouble(matcher.group(1).replaceAll(SEPARATOR, "."));
			} catch (NumberFormatException nfe) {
				return null; // couldn't find a depth
			}
		} else {
			return null; // couldn't find a depth
		}

		// check for a length
		if (matcher.find()) {
			try {
				length = Double.parseDouble(matcher.group(1).replaceAll(SEPARATOR, ".")) - depth;
			} catch (NumberFormatException nfe) {
				// couldn't parse a length
			}
		}

		// calculate any remaining values
		if (dpiX == -1) {
			if (orientation.equals(Image.HORIZONTAL) && length > 0) {
				dpiX = ii.getWidth() / (length / M_PER_IN);
			} else {
				dpiX = ii.getPhysicalWidthDpi();
			}
		}

		if (dpiY == -1) {
			if (orientation.equals(Image.VERTICAL) && length > 0) {
				dpiY = ii.getHeight() / (length / M_PER_IN);
			} else {
				dpiY = ii.getPhysicalHeightDpi();
			}
		}

		if (length == -1) {
			if (orientation.equals(Image.HORIZONTAL) && dpiX > 0) {
				length = ii.getWidth() / dpiX * M_PER_IN;
			} else if (orientation.equals(Image.VERTICAL) && dpiY > 0) {
				length = ii.getHeight() / dpiY * M_PER_IN;
			}
		}

		// make sure we got valid values for everything
		if (orientation != null && depth >= 0 && length >= 0 && dpiX > 0 && dpiY > 0) {
			try {
				return new CoreImage(file.toURI().toURL(), depth, length, dpiX, dpiY, orientation, config);
			} catch (MalformedURLException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	private File directory = null;

	private final CoreImageConfiguration config;

	/**
	 * Create a new CoreImageDirectory.
	 */
	public CoreImageDirectory() {
		config = new CoreImageConfiguration();
	}

	/**
	 * Create a new CoreImageDirectory with the specified directory.
	 * 
	 * @param directory
	 *            the directory.
	 */
	public CoreImageDirectory(final File directory) {
		this.directory = directory;
		config = new CoreImageConfiguration();
		loadConfig();
	}

	/**
	 * Gets the configuration.
	 * 
	 * @return the configuration.
	 */
	public CoreImageConfiguration getConfig() {
		return config;
	}

	/**
	 * Gets the directory.
	 * 
	 * @return the directory.
	 */
	public File getDirectory() {
		return directory;
	}

	/**
	 * Get all of the core image files in this directory.
	 * 
	 * @return the list of core image files.
	 */
	public List<CoreImage> getImages() {
		List<CoreImage> images = new ArrayList<CoreImage>();
		if (directory != null) {
			File[] files = directory.listFiles(FILTER);
			if (files != null) {
				for (File file : files) {
					CoreImage cif = parseImage(file, config);
					if (cif != null) {
						images.add(cif);
					}
				}
			}
		}
		return images;
	}

	/**
	 * Loads the configuration data from the directory.
	 */
	public void loadConfig() {
		if (directory != null) {
			File configFile = new File(directory, CONFIG_FILE);
			if (configFile.exists() && configFile.canRead()) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(configFile);
					config.load(fis);
				} catch (FileNotFoundException e) {
					// ignore
				} catch (IOException e) {
					// ignore
				} finally {
					if (fis != null) {
						try {
							fis.close();
						} catch (IOException ioe) {
							// ignore
						}
					}
				}
			}
		}
	}

	/**
	 * Stores the configuration data from the directory.
	 */
	public void saveConfig() {
		if (directory != null) {
			File configFile = new File(directory, CONFIG_FILE);
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(configFile);
				config.store(fos, null);
			} catch (FileNotFoundException e) {
				// ignore
			} catch (IOException e) {
				// ignore
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException ioe) {
						// ignore
					}
				}
			}
		}
	}

	/**
	 * Sets the directory.
	 * 
	 * @param directory
	 *            the directory.
	 */
	public void setDirectory(final File directory) {
		this.directory = directory;
		loadConfig();
	}

	/**
	 * Return the directory name.
	 */

	@Override
	public String toString() {
		if (directory == null) {
			return "< none >";
		} else {
			if (config == null || config.getTrack() == null || config.getTrack().equals("")) {
				return directory.getAbsolutePath();
			} else {
				return config.getTrack();
			}
		}
	}
}
