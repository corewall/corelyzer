package corelyzer.plugins.expeditionmanager.data;

import java.net.URL;

import corelyzer.plugins.expeditionmanager.Expedition;

/**
 * An abstract implementation of the IDataStore interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class AbstractDataStore implements IDataStore {
    private Expedition expedition;
    private URL path;
    private String name;
    private String category;

    /**
     * Create a new AbstractDataStore.
     */
    protected AbstractDataStore() {
        // default constructor
    }

    /**
     * Create a new AbstractDataStore.
     * 
     * @param expedition
     *            the expedition.
     * @param path
     *            the path.
     * @param name
     *            the name.
     * @param category
     *            the category.
     */
    protected AbstractDataStore(final Expedition expedition, final URL path,
            final String name, final String category) {
        this.expedition = expedition;
        this.path = path;
        this.name = name;
        this.category = category;
    }

    /*
     * (non-Javadoc)
     * 
     * @see corelyzer.plugins.expeditionmanager.data.IDataStore#getCategory()
     */
    public String getCategory() {
        return category;
    }

    /*
     * (non-Javadoc)
     * 
     * @see corelyzer.plugins.expeditionmanager.data.IDataStore#getExpedition()
     */
    public Expedition getExpedition() {
        return expedition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see corelyzer.plugins.expeditionmanager.data.IDataStore#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see corelyzer.plugins.expeditionmanager.data.IDataStore#getPath()
     */
    public URL getPath() {
        return path;
    }

    /*
     * (non-Javadoc)
     * 
     * @see corelyzer.plugins.expeditionmanager.data.IDataStore#setCategory(java.lang.String)
     */
    public void setCategory(final String category) {
        this.category = category;
    }

    /*
     * (non-Javadoc)
     * 
     * @see corelyzer.plugins.expeditionmanager.data.IDataStore#setExpedition(corelyzer.plugins.expeditionmanager.data.Expedition)
     */
    public void setExpedition(final Expedition expedition) {
        this.expedition = expedition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see corelyzer.plugins.expeditionmanager.data.IDataStore#setName(java.lang.String)
     */
    public void setName(final String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see corelyzer.plugins.expeditionmanager.data.IDataStore#setPath(java.net.URL)
     */
    public void setPath(final URL path) {
        this.path = path;
    }
}
