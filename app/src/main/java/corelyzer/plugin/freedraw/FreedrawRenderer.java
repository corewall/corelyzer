package corelyzer.plugin.freedraw;

/**
 * Defines the interface for a freedraw rectangle.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface FreedrawRenderer {

	/**
	 * Called with the freedraw rectangle is disposed.
	 */
	void dispose();

	/**
	 * Render this freedraw rectangle.
	 * 
	 * @param context
	 *            the freedraw context.
	 */
	void render(FreedrawContext context);
}
