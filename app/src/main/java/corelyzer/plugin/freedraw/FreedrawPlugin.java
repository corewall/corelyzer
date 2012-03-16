package corelyzer.plugin.freedraw;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.opengl.GL2;

import corelyzer.graphics.SceneGraph;
import corelyzer.plugin.CorelyzerPlugin;
import corelyzer.plugin.CorelyzerPluginEvent;

/**
 * A base class for freedraw plugins.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class FreedrawPlugin extends CorelyzerPlugin implements FreedrawManager {
	protected Component parent = null;
	private Map<Integer, FreedrawRenderer> freedrawMap = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * corelyzer.plugin.freedraw.FreedrawManager#createFreedraw(corelyzer.plugin
	 * .freedraw.FreedrawRenderer, float, float, float, float)
	 */

	public int createFreedraw(final FreedrawRenderer renderer, final float x, final float y, final float w, final float h) {

		// create our freedraw rectangle with the SceneGraph
		SceneGraph.lock();
		int id = SceneGraph.createFreeDrawRectangle(pluginId, x, y, w, h);
		SceneGraph.unlock();

		// save our freedraw
		freedrawMap.put(id, renderer);

		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * corelyzer.plugin.freedraw.FreedrawManager#createFreedrawForSection(corelyzer
	 * .plugin.freedraw.FreedrawRenderer, int, int, float, float)
	 */

	public int createFreedrawForSection(final FreedrawRenderer renderer, final int trackId, final int sectionId, final float y, final float h) {

		// create our freedraw rectangle with the SceneGraph
		SceneGraph.lock();
		int id = SceneGraph.createFreeDrawRectangleForSection(pluginId, trackId, sectionId, y, h);
		SceneGraph.unlock();

		// save our freedraw
		freedrawMap.put(id, renderer);

		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * corelyzer.plugin.freedraw.FreedrawManager#createFreedrawForTrack(corelyzer
	 * .plugin.freedraw.FreedrawRenderer, int, float, float, float, float)
	 */

	public int createFreedrawForTrack(final FreedrawRenderer renderer, final int trackId, final float x, final float y, final float w, final float h) {

		// create our freedraw rectangle with the SceneGraph
		SceneGraph.lock();
		int id = SceneGraph.createFreeDrawRectangleForTrack(pluginId, trackId, x, y, w, h);
		SceneGraph.unlock();

		// save our freedraw
		freedrawMap.put(id, renderer);

		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see corelyzer.plugin.freedraw.FreedrawManager#destroyFreedraw(int)
	 */

	public void destroyFreedraw(final int id) {
		FreedrawRenderer f = freedrawMap.remove(id);
		if (f != null) {
			f.dispose();
			SceneGraph.lock();
			SceneGraph.destroyFreeDrawRectangle(id);
			SceneGraph.unlock();
		}
	}

	@Override
	public final void fini() {

		for (Entry<Integer, FreedrawRenderer> entry : freedrawMap.entrySet()) {
			// dispose our freedraw
			entry.getValue().dispose();

			// destroy the rectangle in the SceneGraph
			SceneGraph.lock();
			SceneGraph.destroyFreeDrawRectangle(entry.getKey());
			SceneGraph.unlock();
		}
	}

	@Override
	public final boolean init(final Component parent) {
		// save our parent
		this.parent = parent;

		// create our freedraw map
		freedrawMap = new HashMap<Integer, FreedrawRenderer>();

		// call our start method
		return start();
	}

	@Override
	public void processEvent(final CorelyzerPluginEvent e) {
		// do nothing
	}

	/**
	 * Route to the appropriate Freedraw.
	 */
	@Override
	public final void renderRectangle(final GL2 gl, final int freeDraw, final int canvas, final int track, final int section, final float x, final float y,
			final float w, final float h, final float scale) {

		// create our context to wrap our parameters
		FreedrawContext context = new FreedrawContext(gl, freeDraw, canvas, track, section, x, y, w, h, scale);

		// route the render request to the appropriate freedraw
		FreedrawRenderer f = freedrawMap.get(freeDraw);
		if (f != null) {
			f.render(context);
		}
	}

	/**
	 * Called when the plugin is started.
	 * 
	 * @return true if the plugin started properly.
	 */
	protected abstract boolean start();

	/**
	 * Called when the plugin is stopped.
	 */
	protected void stop() {
	}
}
