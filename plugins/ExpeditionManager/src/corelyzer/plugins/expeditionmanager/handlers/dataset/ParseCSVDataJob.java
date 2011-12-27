/**
 * 
 */
package corelyzer.plugins.expeditionmanager.handlers.dataset;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import corelyzer.plugins.expeditionmanager.data.IDataStore;
import corelyzer.plugins.expeditionmanager.data.Resource;
import corelyzer.plugins.expeditionmanager.data.filters.FilteredDataStore;
import corelyzer.plugins.expeditionmanager.data.filters.RegexFilter;
import corelyzer.plugins.expeditionmanager.handlers.DepthRange;
import corelyzer.plugins.expeditionmanager.handlers.Job;

/**
 * Defines the interface for a parser of character-separated data.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class ParseCSVDataJob<E> extends Job {
    private IDataStore dataStore;
    private Properties properties;
    private DepthRange range;
    private DepthValueDataRenderer<E> renderer;

    /**
     * Creates a new ParseCSVDataJob.
     * 
     * @param dataStore
     *            the data store.
     * @param properties
     *            the properties.
     * @param renderer
     *            the renderer.
     */
    public ParseCSVDataJob(final IDataStore dataStore,
            final Properties properties, final DepthRange range,
            final DepthValueDataRenderer<E> renderer) {
        super("Parsing data in " + dataStore.getName());
        this.dataStore = dataStore;
        this.properties = properties;
        this.range = range;
        this.renderer = renderer;
    }

    /**
     * Convert the depth and string into a datum.
     * 
     * @param depth
     *            the depth.
     * @param value
     *            the value.
     * @return the created datum or null.
     */
    protected abstract DepthValueDatum<E> createDatum(double depth, String value);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute() {
        // get our data store
        IDataStore ds;
        String filter = properties.getProperty("filter");
        if (filter == null) {
            ds = dataStore;
        } else {
            ds = new FilteredDataStore(dataStore, new RegexFilter(filter));
        }

        // the extents of the data
        double top = Double.MAX_VALUE;
        double base = Double.MIN_VALUE;

        // get our columns
        int depthCol = getDepthColumn();
        int dataCol = getDataColumn();

        // parse all of the data in the data store
        List<DepthValueDatum<E>> data = new ArrayList<DepthValueDatum<E>>();
        for (Resource resource : ds.getContents()) {
            try {
                CSVReader reader = new CSVReader(new InputStreamReader(resource
                        .getURL().openStream()), getSeparator());

                String[] next;
                double depth;
                DepthValueDatum<E> datum;
                while ((next = reader.nextRow()) != null) {
                    try {
                        if (depthCol < next.length) {
                            depth = Double.parseDouble(next[depthCol]);
                            if (range.intersects(depth, depth)) {
                                top = Math.min(top, depth);
                                base = Math.max(base, depth);

                                if (dataCol < next.length) {
                                    datum = createDatum(depth, next[dataCol]);
                                    if (datum != null) {
                                        data.add(datum);
                                    }
                                }
                            }
                        }
                    } catch (final NumberFormatException nfe) {
                        // invalid depth, so skip
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        // parsed everything, so set the data on the renderer
        renderer.setData(data);
    }

    private int getDataColumn() {
        return Integer.parseInt(properties.getProperty("data-column", "1"));
    }

    private int getDepthColumn() {
        return Integer.parseInt(properties.getProperty("depth-column", "0"));
    }

    /**
     * Gets the separator.
     * 
     * @return the separator.
     */
    private char getSeparator() {
        final String sep = properties.getProperty("separator");
        if ((sep == null) || sep.equals("")) { //$NON-NLS-1$
            return '\t';
        } else if (sep.equals("\\\\")) { //$NON-NLS-1$
            return '\\';
        } else if (sep.equals("\\t")) { //$NON-NLS-1$
            return '\t';
        } else if (sep.equals("\\r")) { //$NON-NLS-1$
            return '\r';
        } else if (sep.equals("\\n")) { //$NON-NLS-1$
            return '\n';
        } else if (sep.equals("\\b")) { //$NON-NLS-1$
            return '\b';
        } else if (sep.equals("\\f")) { //$NON-NLS-1$
            return '\f';
        } else if (sep.equals("\\\"")) { //$NON-NLS-1$
            return '\"';
        } else if (sep.equals("\\\'")) { //$NON-NLS-1$
            return '\'';
        } else {
            return sep.charAt(0);
        }
    }
}
