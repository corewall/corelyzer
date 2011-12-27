package corelyzer.plugins.expeditionmanager.data.filters;

import java.net.URL;

/**
 * A URL filter that ANDs two other filters.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class AndURLFilter implements URLFilter {
    private final URLFilter filter1;
    private final URLFilter filter2;

    /**
     * Create a new AndURLFilter.
     * 
     * @param filter1
     *            the first filter.
     * @param filter2
     *            the seconf filter.
     */
    public AndURLFilter(final URLFilter filter1, final URLFilter filter2) {
        this.filter1 = filter1;
        this.filter2 = filter2;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept(final URL url) {
        return filter1.accept(url) && filter2.accept(url);
    }
}
