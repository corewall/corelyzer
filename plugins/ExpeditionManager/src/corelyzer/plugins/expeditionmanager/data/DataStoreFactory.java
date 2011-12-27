package corelyzer.plugins.expeditionmanager.data;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import corelyzer.plugins.expeditionmanager.Expedition;

/**
 * A factory class for creating IDataStore objects.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DataStoreFactory {
    private final static Map<String, Class<? extends IDataStore>> PROVIDERS = new HashMap<String, Class<? extends IDataStore>>();
    static {
        registerProvider("url", URLDataStore.class);
        registerProvider("file", FileDataStore.class);
        registerProvider("zip", ZipFileDataStore.class);
        registerProvider("index", IndexFileDataStore.class);
        registerProvider("feed", FeedDataStore.class);
    }

    /**
     * Create a data store.
     * 
     * @param expedition
     *            the expedition.
     * @param path
     *            the path.
     * @param name
     *            the name.
     * @param category
     *            the category.
     * @param type
     *            the type.
     * @return a configured DataStore.
     */
    public static IDataStore create(final Expedition expedition,
            final String path, final String name, final String category,
            final String type) {
        // get our data store provider class
        Class<? extends IDataStore> providerClass = PROVIDERS.get(type);
        if (providerClass == null) {
            return null;
        }

        // create an instance of the data store
        IDataStore store = null;
        try {
            store = providerClass.newInstance();
            store.setExpedition(expedition);
            store.setName(name);
            store.setCategory(category);

            // resolve the URL
            try {
                URL url;
                if (path.indexOf(':') > -1) {
                    // try absolute
                    url = new URL(path);
                } else {
                    // try relative
                    url = new URL(expedition.getRoot(), path);
                }
                store.setPath(url);
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return store;
    }

    /**
     * Registers a DataStore provider with the factory.
     * 
     * @param type
     *            the type of data store.
     * @param provider
     *            the DataStore provider class.
     */
    public static void registerProvider(final String type,
            final Class<? extends AbstractDataStore> provider) {
        PROVIDERS.put(type, provider);
    }

    private DataStoreFactory() {
        // not intended to be instantiated
    }
}
