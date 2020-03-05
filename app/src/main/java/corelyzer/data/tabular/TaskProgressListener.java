package corelyzer.data.tabular;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

// listener for potentially long-running tasks
public class TaskProgressListener implements PropertyChangeListener {
	SwingWorker<?,?> task;
	String progressFormatString;
	ProgressMonitor monitor;
	public TaskProgressListener(SwingWorker<?,?> task, String progressFormatString, ProgressMonitor monitor) {
		this.task = task;
		this.progressFormatString = progressFormatString;
		this.monitor = monitor;
	}
	public void propertyChange(PropertyChangeEvent evt) {
        // System.out.println("Property change: " + evt.toString());
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			monitor.setProgress(progress);
			String message = String.format(progressFormatString, progress);
			monitor.setNote(message);
			if (monitor.isCanceled() || task.isDone()) {
				if (monitor.isCanceled()) {
					task.cancel(true);
				}
			}
        }
        if (task.isDone()) {
            monitor.close();
        }
    }
}