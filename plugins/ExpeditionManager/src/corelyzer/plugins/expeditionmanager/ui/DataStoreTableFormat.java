package corelyzer.plugins.expeditionmanager.ui;

import java.util.Comparator;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;
import corelyzer.plugins.expeditionmanager.data.IDataStore;
import corelyzer.plugins.expeditionmanager.handlers.IDataHandler;

/**
 * A table format for IDataStore.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DataStoreTableFormat implements WritableTableFormat<IDataStore>,
        AdvancedTableFormat<IDataStore> {

    private static class BooleanComparator implements Comparator<Boolean> {
        public int compare(final Boolean o1, final Boolean o2) {
            return o1.compareTo(o2);
        }
    }

    private static class StringComparator implements Comparator<String> {
        public int compare(final String o1, final String o2) {
            return o1.compareTo(o2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getColumnClass(final int column) {
        switch (column) {
            case 0:
                return Boolean.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Comparator<?> getColumnComparator(final int column) {
        switch (column) {
            case 0:
                return new BooleanComparator();
            case 1:
                return new StringComparator();
            case 2:
                return new StringComparator();
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return 3;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(final int column) {
        switch (column) {
            case 0:
                return "Display";
            case 1:
                return "Name";
            case 2:
                return "Category";
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getColumnValue(final IDataStore dataStore, final int column) {
        switch (column) {
            case 0:
                return dataStore.getExpedition().getHandler(dataStore)
                        .isEnabled();
            case 1:
                return dataStore.getName();
            case 2:
                return dataStore.getCategory();
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEditable(final IDataStore dataStore, final int column) {
        return (column == 0);
    }

    /**
     * {@inheritDoc}
     */
    public IDataStore setColumnValue(final IDataStore dataStore,
            final Object value, final int column) {
        if (column == 0) {
            IDataHandler handler = dataStore.getExpedition().getHandler(
                    dataStore);
            handler.setEnabled(!handler.isEnabled());
        }
        return dataStore;
    }
}
