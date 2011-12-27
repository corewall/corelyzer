package corelyzer.plugins.expeditionmanager.handlers.dataset;

import java.awt.geom.Rectangle2D;
import java.util.List;

import corelyzer.plugin.freedraw.FreedrawManager;
import corelyzer.plugins.expeditionmanager.ui.AbstractMovableFreedraw;

/**
 * An abstract class for rendering depth-value data.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class DepthValueDataRenderer<E> extends AbstractMovableFreedraw {

    protected List<DepthValueDatum<E>> data;

    /**
     * Create a new DepthValueDataRenderer.
     * 
     * @param manager
     *            the manager.
     * @param track
     *            the track.
     * @param bounds
     *            the bounds.
     */
    public DepthValueDataRenderer(final FreedrawManager manager,
            final String track, final Rectangle2D bounds) {
        super(manager, track, bounds);
    }

    /**
     * Sets the data to render.
     * 
     * @param data
     *            the data.
     */
    public void setData(final List<DepthValueDatum<E>> data) {
        this.data = data;
    }
}
