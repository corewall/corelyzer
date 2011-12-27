package corelyzer.plugins.expeditionmanager.data.filters;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * A URL filter based on UNIX filename globs.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class GlobFilter implements URLFilter {
    private final Pattern pattern;

    /**
     * Create a new GlobFilter.
     * 
     * @param glob
     *            the glob string.
     */
    public GlobFilter(final String glob) {
        pattern = Pattern.compile(glob(glob));
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept(final URL url) {
        return (pattern.matcher(url.toExternalForm())).matches();
    }

    private String glob(final String wild) {
        StringBuffer buffer = new StringBuffer();

        char[] chars = wild.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            if (chars[i] == '*') {
                buffer.append(".*");
            } else if (chars[i] == '?') {
                buffer.append(".");
            } else if ("+()^$.{}[]|\\".indexOf(chars[i]) != -1) {
                buffer.append('\\').append(chars[i]);
            } else {
                buffer.append(chars[i]);
            }
        }

        return buffer.toString();
    }
}
