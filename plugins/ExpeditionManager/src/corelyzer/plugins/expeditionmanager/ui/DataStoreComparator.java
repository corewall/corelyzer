package corelyzer.plugins.expeditionmanager.ui;

import java.util.Comparator;

import corelyzer.plugins.expeditionmanager.data.IDataStore;

/**
 * A comparator for IDataStore.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DataStoreComparator implements Comparator<IDataStore> {

    /**
     * {@inheritDoc}
     */
    public int compare(final IDataStore o1, final IDataStore o2) {
        int categories = o1.getCategory().compareTo(o2.getCategory());
        if (categories == 0) {
            return o1.getName().compareTo(o2.getName());
        } else {
            return categories;
        }
    }
}
