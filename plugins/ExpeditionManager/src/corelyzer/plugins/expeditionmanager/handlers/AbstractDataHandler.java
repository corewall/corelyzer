package corelyzer.plugins.expeditionmanager.handlers;

import java.util.Properties;

import corelyzer.plugins.expeditionmanager.data.IDataStore;

/**
 * An abstract implementation of the IDataHandler interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class AbstractDataHandler implements IDataHandler {
    private DataHandlerContext context;
    private IDataStore dataStore;
    private Properties properties;
    private DepthRange range;
    private boolean enabled = false;

    /**
     * Disable this data handler.
     */
    protected abstract void disable();

    /**
     * Enable this data handler.
     */
    protected abstract void enable();

    /**
     * {@inheritDoc}
     */
    public DataHandlerContext getContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     */
    public IDataStore getDataStore() {
        return dataStore;
    }

    /**
     * {@inheritDoc}
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     */
    public DepthRange getVisibleRange() {
        return range;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Renders the visible range.
     * 
     * @param range
     *            the range.
     */
    protected abstract void render(DepthRange range);

    /**
     * {@inheritDoc}
     */
    public void setContext(final DataHandlerContext context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public void setDataStore(final IDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * {@inheritDoc}
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            enable();
            if (getVisibleRange() != null) {
                render(getVisibleRange());
            }
        } else {
            disable();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setProperties(final Properties properties) {
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     */
    public void setVisibleRange(final DepthRange range) {
        this.range = range;
        if (isEnabled()) {
            render(range);
        }
    }
}
