package corelyzer.plugin.iCores;

import corelyzer.plugin.CorelyzerPlugin;
import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.plugin.iCores.ui.ICoreFrame;

import javax.swing.*;
import java.awt.*;

/**
 * The iCores plugin class.
 */
public class ICoresPlugin extends CorelyzerPlugin {

	/**
	 * Added to run the plugin outside of Corelyzer.
	 *
	 * @param args not used.
	 */
    public static void main(String[] args) {
    	ICoresPlugin plugin = new ICoresPlugin();
    	plugin.init(null);
    	JFrame frame = plugin.getFrame();
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	frame.setSize(500, 500);
    	frame.setAlwaysOnTop(true);
    	frame.setVisible(true);
    }

    // our plugin's frame
    private ICoreFrame pluginFrame;

    /**
     * {@inheritDoc}
     */
    @Override
	public void fini() {
        pluginFrame.terminate();
        pluginFrame = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public JFrame getFrame() {
        return pluginFrame;
    }

    /**
     * Initialize our plugin frame.
     */
    @Override
	public boolean init(Component component) {
        // create our frame
    	pluginFrame = new ICoreFrame();
        pluginFrame.pack();
        pluginFrame.setSize(800, 400);
        pluginFrame.setLocationRelativeTo(null);
        pluginFrame.setAlwaysOnTop(true);

        this.setWantToReplaceMainFrame(true);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void processEvent(CorelyzerPluginEvent corelyzerPluginEvent) {
    	// do nothing
    }
}
