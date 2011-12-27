package corelyzer.plugin.directoryviewer;

import java.awt.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;

import corelyzer.plugin.CorelyzerPlugin;
import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.plugin.directoryviewer.ui.DirectoryViewerFrame;

/**
 * The DirectoryViewer plugin.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DirectoryViewerPlugin extends CorelyzerPlugin {
    private static DirectoryViewerPlugin THIS = null;

    /**
     * Get the shared instance of this plugin.
     * 
     * @return the schared instance.
     */
    public static DirectoryViewerPlugin getDefault() {
        return THIS;
    }

    /**
     * Added to run the plugin outside of Corelyzer.
     * 
     * @param args
     *            not used.
     */
    public static void main(final String[] args) {
        DirectoryViewerPlugin plugin = new DirectoryViewerPlugin();
        plugin.init(null); // hack
        JFrame frame = plugin.getFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setAlwaysOnTop(false);
        frame.setVisible(true);
    }

    private DirectoryViewerFrame pluginFrame = null;

    private ExecutorService threadPool = Executors.newFixedThreadPool(2);

    /**
     * Keep a reference to ourselves.
     */
    public DirectoryViewerPlugin() {
        THIS = this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fini() {
        threadPool.shutdownNow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JFrame getFrame() {
        return pluginFrame;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMenuName() {
        return "Directory Viewer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean init(final Component parentUI) {
        pluginFrame = new DirectoryViewerFrame();
        pluginFrame.pack();
        pluginFrame.setLocationRelativeTo(null);

        // try setting the frame to be always on top
        try {
            pluginFrame.setAlwaysOnTop(true);
        } catch (Exception e) {
            // this may fail under some situations, so catch the runtime
            // exception
            e.printStackTrace();
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processEvent(final CorelyzerPluginEvent e) {
        // don't need this
    }

    /**
     * Submit a job to be run.
     * 
     * @param job
     *            the job to be run.
     */
    public void submitJob(final Runnable job) {
        threadPool.execute(job);
    }
}
