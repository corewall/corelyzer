package corelyzer.plugins.expeditionmanager.handlers;

/**
 * An abstract base class for all jobs.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class Job implements Runnable {
    public static final double SYSTEM_JOB = -100.0;
    public static final double UI_JOB = -10.0;

    protected DataHandlerContext context;
    protected final String name;

    /**
     * Create a new job with the specified name.
     * 
     * @param name
     *            the name.
     */
    public Job(final String name) {
        this.name = name;
    }

    /**
     * Execute this job.
     */
    protected abstract void execute();

    /**
     * Gets the data handler context.
     * 
     * @return the data handler context.
     */
    public DataHandlerContext getContext() {
        return context;
    }

    /**
     * Gets the depth associated with this job.
     * 
     * @return the depth associated with this job.
     */
    public abstract double getDepth();

    /**
     * Gets the name of this job.
     * 
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public final void run() {
        execute();
        context.jobCompleted();
    }

    /**
     * Sets the data handler context.
     * 
     * @param context
     *            the data handler context.
     */
    public void setContext(final DataHandlerContext context) {
        this.context = context;
    }
}
