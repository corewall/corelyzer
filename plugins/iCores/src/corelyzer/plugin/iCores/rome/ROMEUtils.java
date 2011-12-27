package corelyzer.plugin.iCores.rome;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.feed.synd.SyndLinkImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * Some useful ROME utility methods.
 *
 * @author Josh Reed (jareed@andrill.org)
 */
public class ROMEUtils {
	/**
	 * Date format.
	 */
	public static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Double foramt.
	 */
	public static final DecimalFormat DOUBLE = new DecimalFormat("0.00");

	/**
	 * Creates a category with the specified name and taxonomy URI.
	 *
	 * @param name
	 *            the name.
	 * @param taxonomy
	 *            the taxonomy URI.
	 * @return the SyndCategory.
	 */
	public static SyndCategory createCategory(String name, String taxonomy) {
		SyndCategory category = new SyndCategoryImpl();
		category.setName(name);
		category.setTaxonomyUri(taxonomy);
		return category;
	}

	/**
	 * Crreates a link with the specified parameters.
	 *
	 * @param href
	 *            the href.
	 * @param rel
	 *            the rel.
	 * @param title
	 *            the title.
	 * @param type
	 *            the type.
	 * @return the link.
	 */
	public static SyndLink createLink(String href, String rel, String title, String type) {
		SyndLink link = new SyndLinkImpl();
		link.setHref(href);
		link.setRel(rel);
		link.setTitle(title);
		link.setType(type);
		return link;
	}

	/**
	 * Reads a feed from the specified URL.
	 *
	 * @param url
	 *            the URL.
	 * @return the parsed SyndFeed or null.
	 */
	public static SyndFeed readFeed(String url) {
		SyndFeedInput input = new SyndFeedInput();
		try {
            SyndFeed feed = input.build(new InputStreamReader(new URL(url).openStream()));
			
            if (feed.getLink() == null) {
                // NOTE: it looks like ROME doesn't currently parse feed links that are
            	// 	rel="self" or if there are multiple links for Atom 1.0.
            	//  This has been reported (https://rome.dev.java.net/issues/show_bug.cgi?id=64)
            	//  and hopefully will be fixed in the next version.
                feed.setLink(url);
            }

            // System.out.println("---> [DEBUG] ROMEUtils' url is: " + feed.getLink());
            return feed;
		} catch (IllegalArgumentException e) {
			System.err.println("IllegalArgumentException when parsing feed '" + url + "':" + e.getMessage());
		} catch (MalformedURLException e) {
			System.err.println("MalformedURLException when parsing feed '" + url + "'");
		} catch (FeedException e) {
			System.err.println("FeedException when parsing feed '" + url + "':" + e.getMessage() + " " + e.getCause().getMessage() + e.getCause().getCause().getMessage());
		} catch (IOException e) {
			System.err.println("IOException when parsing feed '" + url + "'");
		}
		return null;
	}

	/**
	 * Write out a feed.
	 *
	 * @param feed
	 *            the feed to write.
	 * @param filename
	 *            the filename.
	 */
	public static void writeFeed(SyndFeed feed, String filename) {
		Writer writer = null;
		try {
			writer = new FileWriter(filename);
			SyndFeedOutput output = new SyndFeedOutput();
			output.output(feed, writer);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try { writer.close(); } catch (IOException ioe) { /* do nothing */ }
			}
		}
	}

	private ROMEUtils() {
		// this class is not intended to be instantiated
	}
}
