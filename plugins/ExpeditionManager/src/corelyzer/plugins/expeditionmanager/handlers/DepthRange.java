package corelyzer.plugins.expeditionmanager.handlers;

/**
 * A class to hold the depth range.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DepthRange {
    private double top, bottom;

    /**
     * Create a new DepthRange.
     */
    public DepthRange() {
        top = 0.0;
        bottom = 0.0;
    }

    /**
     * Create a new DepthRange.
     * 
     * @param top
     *            the top depth.
     * @param bottom
     *            the bottom depth.
     */
    public DepthRange(final double top, final double bottom) {
        this.top = top;
        this.bottom = bottom;
    }

    /**
     * @return the bottom
     */
    public double getBottom() {
        return bottom;
    }

    /**
     * @return the top
     */
    public double getTop() {
        return top;
    }

    public boolean intersects(final double t, final double b) {
        return ((t >= top) && (t <= bottom)) || ((b >= top) && (b <= bottom))
                || ((t <= top) && (b >= bottom));
    }

    /**
     * @param bottom
     *            the bottom to set
     */
    public void setBottom(final double bottom) {
        this.bottom = bottom;
    }

    /**
     * @param top
     *            the top to set
     */
    public void setTop(final double top) {
        this.top = top;
    }
}
