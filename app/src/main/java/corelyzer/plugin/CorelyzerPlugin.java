package corelyzer.plugin;

import java.awt.Component;
import java.util.Vector;

//import javax.media.opengl.GL2;
import com.jogamp.opengl.GL2;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;

import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.CorelyzerGLCanvas;

/**
 * <p>
 * This is the base class for all Corelyzer Plugins. Each plugin, once
 * successfully initialized, will automatically recieve a plugin id. Plugins are
 * required to implement four methods:<br>
 * <center>init, fini, processEvent and getFrame</center><br>
 * </p>
 * <p>
 * It is assumed that all plugins will have their own window frame. Upon
 * application startup all plugins will be called to initialize via the init
 * method. Similarly upon application shutdown, fini will be called, so that the
 * plugin can free whatever resources it may need to.
 * </p>
 * <p>
 * The processEvent method is a way for the main Corelyzer system to communicate
 * with the plugin. For instance, if a plugin registers to recieve particular
 * events, the processEvent method will be the method where Corelyzer will relay
 * the event to the plugin.
 * </p>
 * <p>
 * There is also another method that the plugin can override if it so desires,
 * the renderRectangle method. This method is called when a FreeDraw area,
 * requested by the plugin to the corelyzer.helper.SceneGraph, is in need of
 * being drawing into. This method allows a plugin to use the JOGL API to render
 * objects which ever way the plugin desires, within the bounds of the FreeDraw
 * area. There are two versions of this method, one that can be overriden and
 * one that is final (not overridable). The final version is so that the
 * corelyzer.helper.SceneGraph can make sure there is a place for the system to
 * call to have the FreeDraw area rendered to. It also takes care of retrieving
 * some internal data for the plugin developer to use.
 * </p>
 */

public abstract class CorelyzerPlugin {

	/** The system assigned plugin id number. */
	public int pluginId;

	boolean wantToReplaceMainFrame = false;

	/**
     */
	public CorelyzerPlugin() {

	}

	public void addToPopupMenu(final JPopupMenu jpm) {
		return;
	}

	/**
	 * A method needed to let the plugin free resources.
	 */
	public abstract void fini();

	/**
	 * An abstract method to implement by all plugins. The desired functionality
	 * is to return the JFrame that the plugin uses for it's own GUI.
	 */
	public abstract JFrame getFrame();

	/**
	 * An overridable method to let the Corelyzer system use a specific string
	 * for the menu item to access this plugin.
	 */
	public String getMenuName() {
		return "Unknown Plugin";
	}

	public final int getPluginID() {
		return pluginId;
	}

	/**
	 * A method needed to initialize the plugin.
	 */
	public abstract boolean init(Component parentUI);

	public boolean isWantToReplaceMainFrame() {
		return wantToReplaceMainFrame;
	}

	/**
	 * A method to recieve and process events from the Corelyzer system.
	 */
	public abstract void processEvent(CorelyzerPluginEvent e);

	/**
	 * Called by the non-overridable version of this method. This is the
	 * overridable version of the method. the GL object is the same as you find
	 * in JOGL. Canvas, track and section are ids to the current objects related
	 * to the FreeDraw area that needs to be filled in. Either or both track and
	 * section can be -1.
	 */
	public void renderRectangle(final GL2 gl, final int freeDraw, final int canvas, final int track, final int section, final float x, final float y,
			final float w, final float h, final float scale) {

		// draw a rectangle filling white quad
		gl.glPushAttrib(GL2.GL_CURRENT_BIT | GL2.GL_ENABLE_BIT);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glColor3f(1, 1, 1);
		gl.glBegin(GL2.GL_QUADS);
		{
			gl.glVertex3f(x, y, 0);
			gl.glVertex3f(x, y + h, 0);
			gl.glVertex3f(x + w, y + h, 0);
			gl.glVertex3f(x + w, y, 0);
		}
		gl.glEnd();
		gl.glPopAttrib();
	}

	/**
	 * Used by the corelyzer.helper.SceneGraph system to forward rendering
	 * control to the plugin for a FreeDraw area.
	 */
	final public void renderRectangle(final int freeDraw, final int canvas, final int track, final int section, final float x, final float y, final float w,
			final float h, final float scale) {

		Vector canvasVec = CorelyzerApp.getApp().getCanvasVec();
		if (canvas < 0 || canvas > canvasVec.size() - 1) {
			return;
		}

		CorelyzerGLCanvas cglc = (CorelyzerGLCanvas) canvasVec.elementAt(canvas);
		if (cglc == null) {
			return;
		}

		renderRectangle(cglc.getCanvas().getGL().getGL2(), freeDraw, canvas, track, section, x, y, w, h, scale);
	}

	public void setWantToReplaceMainFrame(final boolean wantToReplaceMainFrame) {
		this.wantToReplaceMainFrame = wantToReplaceMainFrame;
	}
}
