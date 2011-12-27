package corelyzer.plugins.expeditionmanager.handlers.psicat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import corelyzer.plugins.expeditionmanager.data.Resource;
import corelyzer.plugins.expeditionmanager.data.filters.FilteredDataStore;
import corelyzer.plugins.expeditionmanager.data.filters.GlobFilter;
import corelyzer.plugins.expeditionmanager.handlers.AbstractDataHandler;
import corelyzer.plugins.expeditionmanager.handlers.DepthRange;
import corelyzer.plugins.expeditionmanager.handlers.Job;
import corelyzer.plugins.expeditionmanager.handlers.dataset.CSVReader;
import corelyzer.plugins.expeditionmanager.util.scheme.SchemeEntry;
import corelyzer.plugins.expeditionmanager.util.scheme.SchemeManager;
import corelyzer.plugins.expeditionmanager.util.scheme.SchemeUtils;

/**
 * Renders the lithology strip from PSICAT.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class LithologyHandler extends AbstractDataHandler {
    private class ParseIntervals extends Job {
        public ParseIntervals() {
            super("Parsing PSICAT data");
        }

        @Override
        protected void execute() {
            // get our data store and visible range
            FilteredDataStore filtered = new FilteredDataStore(getDataStore(),
                    new GlobFilter("*lithology.tsv"));

            // set up our range
            DepthRange range = getVisibleRange();

            // clear our intervals
            intervals.clear();

            // dispose our freedraw
            if (freedraw != null) {
                freedraw.dispose();
                freedraw = null;
            }

            // parse our data
            for (Resource resource : filtered.getContents()) {
                try {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(resource.getURL()
                                    .openStream()));
                    CSVReader csv = new CSVReader(br, '\t');
                    String[] row;
                    Interval i;
                    while ((row = csv.nextRow()) != null) {
                        i = parseInterval(row);
                        if ((i != null) && range.intersects(i.top, i.bot)) {
                            intervals.add(i);

                            if (i.top < top) {
                                top = (float) i.top;
                            }

                            if (i.bot > bot) {
                                bot = (float) i.bot;
                            }
                        }
                    }
                    csv.close();
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }

            // create our freedraw
            freedraw = new LithologyFreedraw(intervals, getContext()
                    .getFreedrawManager(), top, bot);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getDepth() {
            return SYSTEM_JOB;
        }

        private Interval parseInterval(final String[] row) {
            SchemeManager schemes = SchemeManager.getInstance();
            String scope = schemes.getScopesForScheme("lithology")[0];
            Interval interval;
            if (row.length >= 2) { // only continue if got two depths
                interval = new Interval();
                try {
                    // parse our depths
                    interval.top = Double.parseDouble(row[0]);
                    interval.bot = Double.parseDouble(row[1]);

                    // create our arrays
                    interval.ratios = new double[row.length - 2];
                    interval.entries = new SchemeEntry[row.length - 2];

                    // parse the ratios and entries
                    for (int i = 2; i < row.length; i++) {
                        String[] split = row[i].split(":");
                        if (split.length == 2) {
                            interval.ratios[i - 2] = Double
                                    .parseDouble(split[0]);
                            SchemeEntry entry = schemes.findSchemeEntry(
                                    "lithology", scope, SchemeUtils
                                            .stringToArray(split[1]));
                            interval.entries[i - 2] = entry;
                        } else {
                            interval.ratios[i - 2] = 0.0;
                            interval.entries[i - 2] = null;
                        }
                    }
                    return interval;
                } catch (NumberFormatException nfe) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    private final List<Interval> intervals;
    private LithologyFreedraw freedraw = null;
    private float top = Float.MAX_VALUE;
    private float bot = Float.MIN_VALUE;

    /**
     * Create a new LithologyHandler.
     */
    public LithologyHandler() {
        intervals = Collections.synchronizedList(new ArrayList<Interval>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disable() {
        // clear our intervals
        intervals.clear();

        // dispose our freedraw
        if (freedraw != null) {
            freedraw.dispose();
            freedraw = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enable() {
        top = Float.MAX_VALUE;
        bot = Float.MIN_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void render(final DepthRange range) {
        getContext().submitIOJob(new ParseIntervals());
    }
}
