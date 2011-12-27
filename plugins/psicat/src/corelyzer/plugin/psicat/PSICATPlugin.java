package corelyzer.plugin.psicat;

import java.awt.Component;

import javax.swing.JFrame;

import corelyzer.plugin.freedraw.FreedrawPlugin;
import corelyzer.plugin.psicat.ui.PSICATFrame;

/**
 * This plugin displays data exported from PSICAT.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class PSICATPlugin extends FreedrawPlugin {
    private static PSICATPlugin THIS = null;

    /**
     * Gets the shared instance of this plugin.
     * 
     * @return the shared instance.
     */
    public static PSICATPlugin getDefault() {
        return THIS;
    }

    protected Component parent = null;

    // our frame
    private PSICATFrame frame = null;

    /**
     * Create our PSICATPlugin.
     */
    public PSICATPlugin() {
        THIS = this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JFrame getFrame() {
        return frame;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMenuName() {
        return "PSICAT Data Plugin";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean start() {
        // create our frame
        frame = new PSICATFrame();
        frame.pack();
        frame.setLocationRelativeTo(null);

        // try setting the frame to be always on top
        try {
            frame.setAlwaysOnTop(true);
        } catch (Exception e) {
            // ignore
        }

        return true;
    }
}
