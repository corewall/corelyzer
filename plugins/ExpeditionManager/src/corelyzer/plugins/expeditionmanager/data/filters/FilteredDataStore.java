package corelyzer.plugins.expeditionmanager.data.filters;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import corelyzer.plugins.expeditionmanager.Expedition;
import corelyzer.plugins.expeditionmanager.data.IDataStore;
import corelyzer.plugins.expeditionmanager.data.Resource;

/**
 * An implementation of the IDataStore interface that provides a filtered view
 * of a data store.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class FilteredDataStore implements IDataStore {
    private final IDataStore dataStore;
    private final URLFilter filter;

    /**
     * Create a new AbstractFilteredDataStore.
     * 
     * @param dataStore
     *            the dataStore.
     */
    public FilteredDataStore(final IDataStore dataStore, final URLFilter filter) {
        this.dataStore = dataStore;
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     */
    public String getCategory() {
        return dataStore.getCategory();
    }

    /**
     * {@inheritDoc}
     */
    public List<Resource> getContents() {
        List<Resource> filtered = new ArrayList<Resource>();
        for (Resource resource : dataStore.getContents()) {
            if (filter.accept(resource.getURL())) {
                filtered.add(resource);
            }
        }
        return filtered;
    }

    /**
     * Gets the underlying data store.
     * 
     * @return the underlying data store.
     */
    protected IDataStore getDataStore() {
        return dataStore;
    }

    /**
     * {@inheritDoc}
     */
    public Expedition getExpedition() {
        return dataStore.getExpedition();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return dataStore.getName();
    }

    /**
     * {@inheritDoc}
     */
    public URL getPath() {
        return dataStore.getPath();
    }

    /**
     * {@inheritDoc}
     */
    public void setCategory(final String category) {
        dataStore.setCategory(category);
    }

    /**
     * {@inheritDoc}
     */
    public void setExpedition(final Expedition expedition) {
        dataStore.setExpedition(expedition);
    }

    /**
     * {@inheritDoc}
     */
    public void setName(final String name) {
        dataStore.setName(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setPath(final URL path) {
        dataStore.setPath(path);
    }
}
