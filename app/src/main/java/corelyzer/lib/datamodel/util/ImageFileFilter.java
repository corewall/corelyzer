/**
 * 
 */
package corelyzer.lib.datamodel.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * A file fileter for images.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ImageFileFilter implements FilenameFilter {

	public boolean accept(final File dir, final String name) {
		String l = name.toLowerCase();
		return l.endsWith(".bmp") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png") || l.endsWith(".tiff");
	}
}