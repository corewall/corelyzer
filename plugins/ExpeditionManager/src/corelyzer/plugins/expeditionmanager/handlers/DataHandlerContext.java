package corelyzer.plugins.expeditionmanager.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import corelyzer.plugin.freedraw.FreedrawManager;

/**
 * Contains the context for the data handlers.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DataHandlerContext {
    private final FreedrawManager freedraw;
    private final ExecutorService ioPool;
    private final JLabel status;
    private final ImageIcon statusIcon = new ImageIcon(getClass().getResource(
            "/corelyzer/plugins/expeditionmanager/ui/resources/progress.gif"));
    private final List<String> jobs;

    /**
     * Create a new DataHandlerContext.
     * 
     * @param freedraw
     *            the freedraw manager.
     */
    public DataHandlerContext(final FreedrawManager freedraw,
            final JLabel status) {
        this.freedraw = freedraw;
        this.status = status;

        // multiple threads for IO jobs
        ioPool = Executors.newFixedThreadPool(Runtime.getRuntime()
                .availableProcessors() * 5);
        jobs = Collections.synchronizedList(new ArrayList<String>());
    }

    /**
     * Gets the freedraw manager associated with this context
     */
    public FreedrawManager getFreedrawManager() {
        return freedraw;
    }

    /**
     * Signal a job as completed.
     * 
     * @param job
     *            the job.
     */
    protected void jobCompleted() {
        jobs.remove(0);
        updateStatus();
    }

    /**
     * Submit IO jobs.
     * 
     * @param job
     *            the job to run.
     */
    public void submitIOJob(final Job job) {
        jobs.add(job.getName());
        job.setContext(this);
        ioPool.submit(job);
        updateStatus();
    }

    /**
     * Submit a job to be run in the UI thread.
     * 
     * @param job
     *            the job to run.
     */
    public void submitUIJob(final Job job) {
        jobs.add(job.getName());
        job.setContext(this);
        SwingUtilities.invokeLater(job);
        updateStatus();
    }

    /**
     * Submit a job to be run in the UI thread and wait for the job.
     * 
     * @param job
     *            the job to run.
     */
    public void submitUIJobAndWait(final Job job) {
        jobs.add(job.getName());
        job.setContext(this);
        try {
            SwingUtilities.invokeAndWait(job);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        } catch (InvocationTargetException e) {
            e.printStackTrace(System.err);
        }
        updateStatus();
    }

    private void updateStatus() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (jobs.size() == 0) {
                    status.setIcon(null);
                    status.setText("");
                } else {
                    status.setIcon(statusIcon);
                    status.setText(jobs.get(0) + " (" + jobs.size()
                            + " remaining)");
                }
            }
        });
    }
}
