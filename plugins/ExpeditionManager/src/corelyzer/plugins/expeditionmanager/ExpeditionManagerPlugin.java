package corelyzer.plugins.expeditionmanager;

import java.net.Authenticator;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import corelyzer.plugin.freedraw.FreedrawPlugin;
import corelyzer.plugins.expeditionmanager.ui.ExpeditionManagerFrame;
import corelyzer.plugins.expeditionmanager.util.URLAuthenticator;

/**
 * A plugin for managing the images and data of an expedition.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ExpeditionManagerPlugin extends FreedrawPlugin {
    private static ExpeditionManagerPlugin THIS = null;

    /**
     * Gets the shared instance of this plugin.
     * 
     * @return the shared instance.
     */
    public static ExpeditionManagerPlugin getDefault() {
        return THIS;
    }

    public static void main(final String[] args) {
        final ExpeditionManagerPlugin plugin = new ExpeditionManagerPlugin();
        plugin.start();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = plugin.getFrame();
                frame.setVisible(true);
            }
        });
    }

    private ExpeditionManagerFrame frame;

    /**
     * Create our Expedition Manager plugin.
     */
    public ExpeditionManagerPlugin() {
        THIS = this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JFrame getFrame() {
        if (frame == null) {
            frame = new ExpeditionManagerFrame();
            frame.setLocationRelativeTo(null);
            frame.setAlwaysOnTop(true);
        }
        return frame;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMenuName() {
        return "Expedition Manager";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean start() {
        // init our authenticator
        Authenticator.setDefault(new URLAuthenticator());

        return true;
    }
}
