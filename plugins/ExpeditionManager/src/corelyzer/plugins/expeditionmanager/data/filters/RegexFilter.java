package corelyzer.plugins.expeditionmanager.data.filters;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * A URL filter based on regular expressions.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class RegexFilter implements URLFilter {
    private final Pattern pattern;

    /**
     * Create a new regular expression filter.
     * 
     * @param regex
     *            the regular expression.
     */
    public RegexFilter(final String regex) {
        pattern = Pattern.compile(regex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept(final URL url) {
        return (pattern.matcher(url.toExternalForm())).matches();
    }
}
