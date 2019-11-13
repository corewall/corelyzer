package corelyzer.ui;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

// 4/17/2012 brg: Corelyzer's two primary windows, the toolbar and mainFrame, are set to
// alwaysOnTop(true) so they're never obscured by the workspace. However, this behavior persists
// even when external applications are activated, to the general annoyance of all. This class
// attempts to show/hide the toolbar and mainFrame when Corelyzer is activated/deactivated by
// considering the triggering event's opposite window. There's probably a more robust way to
// do this, but it seems to work well as is.

class PaletteVisibilityManager extends WindowAdapter
{
	private boolean isSuspended = false;
	
	public void setSuspended(final boolean suspend) { isSuspended = suspend; }
	
	public void windowActivated(WindowEvent e) {
		// System.out.println("windowActivated " + e.getWindow().getName());
		if ( isSuspended )
			return;
		
		boolean showWindows = false;
		try {
			if ( e.getOppositeWindow() == null ) // non-Corelyzer window was deactivated
				showWindows = true;
		} catch (NullPointerException npe) {
			System.out.println(npe.toString());
		}
		
		if ( showWindows )
		{
			final boolean showMainFrame = CorelyzerApp.getApp().getToolFrame().isAppFrameSelected();
			
			JFrame mainFrame = CorelyzerApp.getApp().getMainFrame();
			JFrame toolbar = CorelyzerApp.getApp().getToolFrame();
			// final String exstate = Integer.toString(mainFrame.getExtendedState());
			// System.out.println("mainFrame state = " + exstate);

			if (!mainFrame.isVisible() && showMainFrame)
				mainFrame.setVisible(true);
			if (!toolbar.isVisible())
				toolbar.setVisible(true);
		}
	}
	
	public void windowDeactivated(WindowEvent e) {
		if ( isSuspended )
			return;
		
		boolean hideWindows = false;
		try {
			// String windowName = e.getWindow().getName();
			if (e.getOppositeWindow() == null) { // non-Corelyzer window was activated
				// System.out.println("windowDeactivated, hiding " + windowName);
				hideWindows = true;
			} else {
				// System.out.println("windowDeactivated, not hiding " + windowName);
			}
		} catch (NullPointerException npe) {
			System.out.println(npe.toString());
		}
		
		if ( hideWindows )
		{
			JFrame mainFrame = CorelyzerApp.getApp().getMainFrame();
			JFrame toolbar = CorelyzerApp.getApp().getToolFrame();

			// 11/13/2019 brg: On Mac, minimizing first triggers a deactivate
			// event followed by an iconify. In Java 8, calling setVisible(false)
			// causes an iconified window in the Dock to vanish entirely, never to
			// return. We only want to setVisible(false) if the window is only being
			// deactivated and *not* iconified, but we know nothing of future iconification
			// at deactivate time. Thus we invoke this logic later, at which point we
			// know if the window was iconified or not. If not, call setVisible(false)
			// to achieve original purpose of PaletteVisibilityManager (hiding Corelyzer's
			// always on top windows when another app's window is activated), otherwise
			// do nothing.
			// Debugging and resolving this bug was a real treat, so I've left debugging
			// code commented in the event it returns.
			if (mainFrame.isVisible()) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JFrame mf = CorelyzerApp.getApp().getMainFrame();
						if (mf.getExtendedState() != Frame.ICONIFIED) {
							// System.out.println("Not iconified, hiding");
							mf.setVisible(false);
						} else {
							// System.out.println("Iconified, not hiding");
						}
					}
				});
				// mainFrame.setVisible(false);
			}
			if (toolbar.isVisible())
				toolbar.setVisible(false);
		}
	}

	// public void windowIconified(WindowEvent e) {
	// 	System.out.println("windowIconified: " + e.getWindow().getName());
	// }

	// public void windowDeiconified(WindowEvent e) {
	// 	System.out.println("windowDeiconified: " + e.getWindow().getName());
	// }

	// public void windowStateChanged(WindowEvent e) {
	// 	System.out.println(e.getWindow().getName() + " state changed from " + Integer.toString(e.getOldState()) + " to " + Integer.toString(e.getNewState()));
	// 	if (e.getNewState() == WindowEvent.WINDOW_ICONIFIED) {
	// 		System.out.println("iconified");
	// 	}
	// }
}