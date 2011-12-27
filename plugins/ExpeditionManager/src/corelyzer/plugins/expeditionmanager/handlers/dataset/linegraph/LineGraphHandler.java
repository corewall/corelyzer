package corelyzer.plugins.expeditionmanager.handlers.dataset.linegraph;

import java.awt.geom.Rectangle2D;

import corelyzer.plugins.expeditionmanager.handlers.AbstractDataHandler;
import corelyzer.plugins.expeditionmanager.handlers.DepthRange;
import corelyzer.plugins.expeditionmanager.handlers.dataset.DepthValueDatum;
import corelyzer.plugins.expeditionmanager.handlers.dataset.ParseCSVDataJob;

/**
 * Renders a line graph from some data.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class LineGraphHandler extends AbstractDataHandler {
    private class ParseDataJob extends ParseCSVDataJob<Double> {
        /**
         * Parse our CSV data.
         */
        public ParseDataJob() {
            super(getDataStore(), getProperties(), getVisibleRange(), renderer);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DepthValueDatum<Double> createDatum(final double depth,
                final String value) {
            try {
                double number = Double.parseDouble(value);
                return new DepthValueDatum<Double>(depth, number);
            } catch (NumberFormatException nfe) {
                return new DepthValueDatum<Double>(depth, Double.NaN);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getDepth() {
            return SYSTEM_JOB;
        }
    }

    private LineGraphRenderer renderer;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void disable() {
        if (renderer != null) {
            renderer.dispose();
            renderer = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void enable() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void render(final DepthRange range) {
        // dispose our renderer
        if (renderer != null) {
            renderer.dispose();
            renderer = null;
        }

        // re-create our renderer
        double top = getVisibleRange().getTop();
        double width = getVisibleRange().getBottom() - top;

        renderer = new LineGraphRenderer(getContext().getFreedrawManager(),
                getDataStore().getName(), new Rectangle2D.Double(top, 0.0,
                        width, 0.15));
        getContext().submitIOJob(new ParseDataJob());
    }
}
