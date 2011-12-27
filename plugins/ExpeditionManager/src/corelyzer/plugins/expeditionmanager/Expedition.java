package corelyzer.plugins.expeditionmanager;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import corelyzer.plugins.expeditionmanager.data.IDataStore;
import corelyzer.plugins.expeditionmanager.handlers.IDataHandler;

/**
 * Represents an Expedition.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class Expedition {
    private final URL url;
    private String name;
    private URL logo;
    private URL root;
    private final Map<IDataStore, IDataHandler> stores;

    /**
     * Create a new Expedition from the specified URL.
     * 
     * @param url
     *            the URL to the Expedition descriptor.
     */
    public Expedition(final URL url) {
        this.url = url;
        stores = new HashMap<IDataStore, IDataHandler>();
    }

    /**
     * Adds a data store and handler to this expedition.
     * 
     * @param dataStore
     *            the data store.
     * @param handler
     *            the handler.
     */
    public void addDataStore(final IDataStore dataStore,
            final IDataHandler handler) {
        stores.put(dataStore, handler);
    }

    /**
     * Gets the list of configured data stores for this expedition.
     * 
     * @return the list of IDataStores.
     */
    public List<IDataStore> getDataStores() {
        return new ArrayList<IDataStore>(stores.keySet());
    }

    /**
     * Gets the handler configured for the specified data store.
     * 
     * @param dataStore
     *            the data store.
     * @return the handler.
     */
    public IDataHandler getHandler(final IDataStore dataStore) {
        return stores.get(dataStore);
    }

    /**
     * Gets the URL to the logo of this expedition.
     * 
     * @return the URL to the logo of this expedition.
     */
    public URL getLogo() {
        return logo;
    }

    /**
     * Gets the name of this expedition.
     * 
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the path of this expedition.
     * 
     * @return the path of this expedition.
     */
    protected URL getPath() {
        return url;
    }

    /**
     * Sets the root URL all other URLs are relative to.
     * 
     * @return the root URL.
     */
    public URL getRoot() {
        if (root == null) {
            return url;
        } else {
            return root;
        }
    }

    /**
     * Sets the logo for this expedition.
     * 
     * @param logo
     *            the logo.
     */
    public void setLogo(final URL logo) {
        this.logo = logo;
    }

    /**
     * Sets the name of this expedition.
     * 
     * @param name
     *            the name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the root URL all other URLs are relative to.
     * 
     * @param root
     *            the root.
     */
    public void setRoot(final URL root) {
        this.root = root;
    }
}
