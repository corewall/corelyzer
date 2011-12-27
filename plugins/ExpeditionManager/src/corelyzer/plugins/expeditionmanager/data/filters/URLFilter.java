package corelyzer.plugins.expeditionmanager.data.filters;

import java.net.URL;

/**
 * Defines the interface for a URL filter.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface URLFilter {

    /**
     * Check whether the specified should be filter.
     * 
     * @param url
     *            the URL.
     * @return true if the the URL should be included, false otherwise.
     */
    boolean accept(URL url);
}
