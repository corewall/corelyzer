package corelyzer.plugins.expeditionmanager.handlers.psicat;

import corelyzer.plugins.expeditionmanager.util.scheme.SchemeEntry;

/**
 * A simple class to hold our lithology data.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class Interval {
    public double top = -1.0;
    public double bot = -1.0;
    public double[] ratios = new double[0];
    public SchemeEntry[] entries = new SchemeEntry[0];

    @Override
    public Interval clone() {
        Interval clone = new Interval();
        clone.top = top;
        clone.bot = bot;
        clone.ratios = new double[ratios.length];
        clone.entries = new SchemeEntry[entries.length];
        for (int j = 0; j < clone.ratios.length; j++) {
            clone.ratios[j] = ratios[j];
            clone.entries[j] = entries[j];
        }
        return clone;
    }
}