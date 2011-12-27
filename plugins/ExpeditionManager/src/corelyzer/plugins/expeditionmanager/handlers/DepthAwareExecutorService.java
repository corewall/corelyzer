package corelyzer.plugins.expeditionmanager.handlers;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A depth-aware ExecutorService.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DepthAwareExecutorService extends ThreadPoolExecutor {

    /**
     * Prioritizes jobs based on their depth.
     * 
     * @author Josh Reed (jareed@andrill.org)
     */
    private static class DepthJobComparator implements Comparator<Job> {
        /**
         * {@inheritDoc}
         */
        public int compare(final Job o1, final Job o2) {
            return Double.compare(o1.getDepth(), o2.getDepth());
        }
    }

    /**
     * Create a new DepthAwareExecutorService;
     */
    public DepthAwareExecutorService() {
        super(1, 100, 10, TimeUnit.SECONDS, new PriorityBlockingQueue(10,
                new DepthJobComparator()));
    }
}
