package corelyzer.plugins.expeditionmanager.handlers;

import java.util.Properties;

import corelyzer.plugins.expeditionmanager.data.IDataStore;

/**
 * Defines the interface for a data handler.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface IDataHandler {

    /**
     * Gets the context associated with this data handler.
     * 
     * @return the context.
     */
    DataHandlerContext getContext();

    /**
     * Gets the data store for this data handler.
     */
    IDataStore getDataStore();

    /**
     * Gets the configuration properties associated with this data handler.
     * 
     * @return the properties.
     */
    Properties getProperties();

    /**
     * Gets the visible depth range.
     * 
     * @return the visible depth range.
     */
    DepthRange getVisibleRange();

    /**
     * Gets the enabled flag.
     * 
     * @return the enabled flag.
     */
    boolean isEnabled();

    /**
     * Sets the context for this data handler.
     * 
     * @param context
     *            the context.
     */
    void setContext(DataHandlerContext context);

    /**
     * Sets the data store for this data handler.
     * 
     * @param dataStore
     *            the data store.
     */
    void setDataStore(IDataStore dataStore);

    /**
     * Enables or disables this data handler.
     * 
     * @param enabled
     *            true if the data handler should be enabled, false otherwise.
     */
    void setEnabled(boolean enabled);

    /**
     * Sets the configuration properties associated with this data handler.
     */
    void setProperties(Properties properties);

    /**
     * Sets the specified depth range as visible.
     * 
     * @param range
     *            the depth range.
     */
    void setVisibleRange(DepthRange range);
}
