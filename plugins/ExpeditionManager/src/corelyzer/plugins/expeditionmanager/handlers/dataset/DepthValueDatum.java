package corelyzer.plugins.expeditionmanager.handlers.dataset;

/**
 * A simple class to hold depth-registered point data.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DepthValueDatum<E> {
    public double depth;
    public E value;

    public DepthValueDatum() {
    }

    public DepthValueDatum(final double depth, final E value) {
        this.depth = depth;
        this.value = value;
    }
}