package corelyzer.plugins.expeditionmanager.handlers.dataset.linegraph;

import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.media.opengl.GL;

import corelyzer.helper.SceneGraph;
import corelyzer.plugin.freedraw.FreedrawContext;
import corelyzer.plugin.freedraw.FreedrawManager;
import corelyzer.plugins.expeditionmanager.handlers.dataset.DepthValueDataRenderer;
import corelyzer.plugins.expeditionmanager.handlers.dataset.DepthValueDatum;

/**
 * Renders a line graph from some depth-value data.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class LineGraphRenderer extends DepthValueDataRenderer<Double> {
    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;

    /**
     * Create a new LineGraphRenderer.
     * 
     * @param manager
     *            the manager.
     * @param track
     *            the track.
     * @param bounds
     *            the bounds.
     */
    public LineGraphRenderer(final FreedrawManager manager, final String track,
            final Rectangle2D bounds) {
        super(manager, track, bounds);
    }

    /**
     * {@inheritDoc}
     */
    public void render(final FreedrawContext context) {
        // no data so bail
        if (data == null) {
            return;
        }

        // figure out our extents
        double left = Math.max(
                SceneGraph.getCanvasPositionX(0) / 72 * 2.54 / 100, bounds
                        .getMinX());
        double right = Math.min((SceneGraph.getCanvasPositionX(0) + SceneGraph
                .getCanvasWidth(0)) / 72 * 2.54 / 100, bounds.getMaxX());

        double y = context.getY();
        double h = context.getH();

        // to remember our last point
        boolean begun = false;

        // walk through the
        GL gl = context.getGl();

        // push our stack
        gl.glPushAttrib(GL.GL_ALL_ATTRIB_BITS);

        // setup the blend function
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_BLEND);
        gl.glEnable(GL.GL_BLEND);

        // render our guide lines
        gl.glLineWidth(0.5f);
        gl.glColor4f(1, 1, 1, 0.5f);
        double step = context.getH() / 4;
        gl.glBegin(GL.GL_LINES);
        {
            gl.glVertex2d(left, y + step);
            gl.glVertex2d(right, y + step);

            gl.glVertex2d(left, y + 2 * step);
            gl.glVertex2d(right, y + 2 * step);

            gl.glVertex2d(left, y + 3 * step);
            gl.glVertex2d(right, y + 3 * step);
        }

        // render the name
        gl.glEnd();

        // draw the data
        gl.glLineWidth(1);
        gl.glColor4f(1, 1, 1, 1);
        for (DepthValueDatum<Double> datum : data) {
            if ((datum.depth >= left) && (datum.depth <= right)) {
                if (Double.isNaN(datum.value)) {
                    // end our drawing
                    if (begun) {
                        gl.glEnd();
                        begun = false;
                    }
                } else {
                    if (!begun) {
                        gl.glBegin(GL.GL_LINE_STRIP);
                        begun = true;
                    }
                    gl.glVertex2d(datum.depth, y + (scale(datum.value) * h));
                }
            }
        }
        if (begun) {
            gl.glEnd();
        }

        // disable everything
        gl.glDisable(GL.GL_BLEND);
        gl.glDisable(GL.GL_TEXTURE_2D);

        // pop our stack values
        gl.glPopAttrib();
    }

    private double scale(final double value) {
        return (max - value) / (max - min);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setData(final List<DepthValueDatum<Double>> data) {
        super.setData(data);

        // figure out our extents
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        for (DepthValueDatum<Double> datum : data) {
            if (!Double.isNaN(datum.value)) {
                min = Math.min(min, datum.value);
                max = Math.max(max, datum.value);
            }
        }
    }
}
