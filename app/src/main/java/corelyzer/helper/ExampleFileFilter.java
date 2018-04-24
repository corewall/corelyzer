package corelyzer.helper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.filechooser.FileFilter;

/**
 * Code downloaded from <a
 * href="http://www.cs.cf.ac.uk/Dave/HCI/HCI_Handout_CALLER/node99.html">
 * here</a>
 * <p/>
 * A convenience implementation of FileFilter that filters out all files except
 * for those type extensions that it knows about.
 * <p/>
 * Extensions are of the type ".foo", which is typically found on Windows and
 * Unix boxes, but not on Macinthosh. Case is ignored.
 * <p/>
 * Example - create a new filter that filerts out all files but gif and jpg
 * image files:
 * <p/>
 * JFileChooser chooser = new JFileChooser(); corelyzer.helper.ExampleFileFilter
 * filter = new corelyzer.helper.ExampleFileFilter( new String{"gif", "jpg"},
 * "JPEG & GIF Images") chooser.addChoosableFileFilter(filter);
 * chooser.showOpenDialog(this);
 * 
 * @author Jeff Dinkins
 * @version 1.8 08/26/98
 */
public class ExampleFileFilter extends FileFilter implements FilenameFilter {
	private Hashtable filters = null;
	private String description = null;
	private String fullDescription = null;
	private boolean useExtensionsInDescription = true;

	/**
	 * Creates a file filter. If no filters are added, then all files are
	 * accepted.
	 * 
	 * @see #addExtension
	 */
	public ExampleFileFilter() {
		this.filters = new Hashtable();
	}

	/**
	 * Creates a file filter that accepts files with the given extension.
	 * Example: new corelyzer.helper.ExampleFileFilter("jpg");
	 * 
	 * @see #addExtension
	 */
	public ExampleFileFilter(final String extension) {
		this(extension, null);
	}

	/**
	 * Creates a file filter that accepts the given file type. Example: new
	 * corelyzer.helper.ExampleFileFilter("jpg", "JPEG Image Images");
	 * <p/>
	 * Note that the "." before the extension is not needed. If provided, it
	 * will be ignored.
	 * 
	 * @see #addExtension
	 */
	public ExampleFileFilter(final String extension, final String description) {
		this();
		if (extension != null) {
			addExtension(extension);
		}
		if (description != null) {
			setDescription(description);
		}
	}

	/**
	 * Creates a file filter from the given string array. Example: new
	 * corelyzer.helper.ExampleFileFilter(String {"gif", "jpg"});
	 * <p/>
	 * Note that the "." before the extension is not needed adn will be ignored.
	 * 
	 * @see #addExtension
	 */
	public ExampleFileFilter(final String[] filters) {
		this(filters, null);
	}

	/**
	 * Creates a file filter from the given string array and description.
	 * Example: new corelyzer.helper.ExampleFileFilter(String {"gif", "jpg"},
	 * "Gif and JPG Images");
	 * <p/>
	 * Note that the "." before the extension is not needed and will be ignored.
	 * 
	 * @see #addExtension
	 */
	public ExampleFileFilter(final String[] filters, final String description) {
		this();
		for (int i = 0; i < filters.length; i++) {
			// add filters one by one
			addExtension(filters[i]);
		}
		if (description != null) {
			setDescription(description);
		}
	}

	/**
	 * Return true if this file should be shown in the directory pane, false if
	 * it shouldn't.
	 * <p/>
	 * Files that begin with "." are ignored.
	 * 
	 * @see #getExtension
	 * @see FileFilter#accepts
	 */
	// javax.swing.FileFilter accept (used for JFileChoosers)
	@Override
	public boolean accept(final File f) {
		if (f != null) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = getExtension(f);
			if (acceptExtension(extension)) {
				return true;
			}
		}
		return false;
	}

	// java.io.FilenameFilter accept (used for FileDialog)
	@Override
	public boolean accept(final File dir, final String name) {
		final String extension = getExtension(name);
		return acceptExtension(extension);
	}
	
	private boolean acceptExtension(String ext) {
		return ext != null && filters.get(ext) != null;
	}

	/**
	 * Adds a filetype "dot" extension to filter against.
	 * <p/>
	 * For example: the following code will create a filter that filters out all
	 * files except those that end in ".jpg" and ".tif":
	 * <p/>
	 * corelyzer.helper.ExampleFileFilter filter = new
	 * corelyzer.helper.ExampleFileFilter(); filter.addExtension("jpg");
	 * filter.addExtension("tif");
	 * <p/>
	 * Note that the "." before the extension is not needed and will be ignored.
	 */
	public void addExtension(final String extension) {
		if (filters == null) {
			filters = new Hashtable(5);
		}
		filters.put(extension.toLowerCase(), this);
		fullDescription = null;
	}

	/**
	 * Returns the human readable description of this filter. For example:
	 * "JPEG and GIF Image Files (*.jpg, *.gif)"
	 * 
	 * @see setDescription
	 * @see setExtensionListInDescription
	 * @see isExtensionListInDescription
	 * @see FileFilter#getDescription
	 */

	@Override
	public String getDescription() {
		if (fullDescription == null) {
			if (description == null || isExtensionListInDescription()) {
				fullDescription = description == null ? "(" : description + " (";
				// build the description from the extension list
				Enumeration extensions = filters.keys();
				if (extensions != null) {
					fullDescription += "." + (String) extensions.nextElement();
					while (extensions.hasMoreElements()) {
						fullDescription += ", " + (String) extensions.nextElement();
					}
				}
				fullDescription += ")";
			} else {
				fullDescription = description;
			}
		}
		return fullDescription;
	}

	/**
	 * Return the extension portion of the file's name .
	 * 
	 * @see #getExtension
	 * @see FileFilter#accept
	 */
	public String getExtension(final File f) {
		if (f != null) {
			return getExtension(f.getName());
		}
		return null;
	}
	
	private String getExtension(final String filename) {
		final int i = filename.lastIndexOf('.');
		if (i > 0 && i < filename.length() - 1) {
			return filename.substring(i + 1).toLowerCase();
		}
		return null;
	}

	/**
	 * Returns whether the extension list (.jpg, .gif, etc) should show up in
	 * the human readable description.
	 * <p/>
	 * Only relevent if a description was provided in the constructor or using
	 * setDescription();
	 * 
	 * @see getDescription
	 * @see setDescription
	 * @see setExtensionListInDescription
	 */
	public boolean isExtensionListInDescription() {
		return useExtensionsInDescription;
	}

	/**
	 * Sets the human readable description of this filter. For example:
	 * filter.setDescription("Gif and JPG Images");
	 * 
	 * @see setDescription
	 * @see setExtensionListInDescription
	 * @see isExtensionListInDescription
	 */
	public void setDescription(final String description) {
		this.description = description;
		fullDescription = null;
	}

	/**
	 * Determines whether the extension list (.jpg, .gif, etc) should show up in
	 * the human readable description.
	 * <p/>
	 * Only relevent if a description was provided in the constructor or using
	 * setDescription();
	 * 
	 * @see getDescription
	 * @see setDescription
	 * @see isExtensionListInDescription
	 */
	public void setExtensionListInDescription(final boolean b) {
		useExtensionsInDescription = b;
		fullDescription = null;
	}

	/*
	 * public boolean accept(File dir, String string) { if (f != null) { if
	 * (f.isDirectory()) { return true; } String extension = getExtension(f); if
	 * (extension != null && filters.get(getExtension(f)) != null) { return
	 * true; } } return false; }
	 */
}
