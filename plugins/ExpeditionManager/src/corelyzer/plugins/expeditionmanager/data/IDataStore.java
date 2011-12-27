package corelyzer.plugins.expeditionmanager.data;

import java.net.URL;
import java.util.List;

import corelyzer.plugins.expeditionmanager.Expedition;

/**
 * Defines the interface for a data store.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface IDataStore {

    /**
     * Gets the category of this data location.
     * 
     * @return the category
     */
    public abstract String getCategory();

    /**
     * Get the contents of this data location.
     * 
     * @return the list of Resources.
     */
    public abstract List<Resource> getContents();

    /**
     * Gets the expedition this data location is associated with.
     * 
     * @return the expedition
     */
    public abstract Expedition getExpedition();

    /**
     * Gets the name of this data location.
     * 
     * @return the name
     */
    public abstract String getName();

    /**
     * Gets the path of this data location.
     * 
     * @return the path
     */
    public abstract URL getPath();

    /**
     * Sets the category of this location.
     * 
     * @param category
     *            the category.
     */
    public abstract void setCategory(final String category);

    /**
     * Sets the expedition of this location.
     * 
     * @param expedition
     *            the expedition.
     */
    public abstract void setExpedition(final Expedition expedition);

    /**
     * Sets the name of this location.
     * 
     * @param name
     *            the name.
     */
    public abstract void setName(final String name);

    /**
     * Sets the path of this location.
     * 
     * @param path
     *            the path.
     */
    public abstract void setPath(final URL path);

}
