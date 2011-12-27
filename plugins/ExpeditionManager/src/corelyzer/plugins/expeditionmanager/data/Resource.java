package corelyzer.plugins.expeditionmanager.data;

import java.net.URL;

/**
 * Defines the interface for a resource.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class Resource {
    private final URL url;
    private final Object content;

    /**
     * Create a new resource with the specified URL and resource.
     * 
     * @param url
     *            the URL.
     * @param content
     *            the content object.
     */
    public Resource(final URL url, final Object content) {
        this.url = url;
        this.content = content;
    }

    /**
     * Gets the content object for this resource.
     * 
     * @return the content object or null.
     */
    public Object getContent() {
        return content;
    }

    /**
     * Gets the URL for this resource.
     * 
     * @return the URL.
     */
    public URL getURL() {
        return url;
    }
}
