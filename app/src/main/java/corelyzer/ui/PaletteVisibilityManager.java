package corelyzer.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

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
			if ( e.getOppositeWindow() == null ) // non-Corelyzer window was activated
				hideWindows = true;
		} catch (NullPointerException npe) {
			System.out.println(npe.toString());
		}
		
		if ( hideWindows )
		{
			JFrame mainFrame = CorelyzerApp.getApp().getMainFrame();
			JFrame toolbar = CorelyzerApp.getApp().getToolFrame();
			if (mainFrame.isVisible())
				mainFrame.setVisible(false);
			if (toolbar.isVisible())
				toolbar.setVisible(false);
		}
	}
}