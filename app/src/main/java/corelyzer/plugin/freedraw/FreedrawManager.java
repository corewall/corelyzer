package corelyzer.plugin.freedraw;

public interface FreedrawManager {

	/**
	 * Creates a freedraw rectangle that is not associated with any section or
	 * track.
	 * 
	 * @param renderer
	 *            the renderer.
	 * @param x
	 *            the x coord.
	 * @param y
	 *            the y coord.
	 * @param w
	 *            the width.
	 * @param h
	 *            the height.
	 * @return the freedraw id.
	 */
	public abstract int createFreedraw(final FreedrawRenderer renderer, final float x, final float y, final float w, final float h);

	/**
	 * Creates a freedraw rectangle that is associated with a section.
	 * 
	 * @param renderer
	 *            the renderer.
	 * @param trackId
	 *            the track id.
	 * @param sectionId
	 *            the section id.
	 * @param y
	 *            the y coord.
	 * @param h
	 *            the height.
	 * @return the freedraw id.
	 */
	public abstract int createFreedrawForSection(final FreedrawRenderer renderer, final int trackId, final int sectionId, final float y, final float h);

	/**
	 * Creates a freedraw rectangle that is associated with a track.
	 * 
	 * @param renderer
	 *            the renderer.
	 * @param trackId
	 *            the track id.
	 * @param x
	 *            the x coord.
	 * @param y
	 *            the y coord.
	 * @param w
	 *            the width.
	 * @param h
	 *            the height.
	 * @return the freedraw id.
	 */
	public abstract int createFreedrawForTrack(final FreedrawRenderer renderer, final int trackId, final float x, final float y, final float w, final float h);

	/**
	 * Destroy the specified freedraw rectangle.
	 * 
	 * @param id
	 *            the freedraw id.
	 */
	public abstract void destroyFreedraw(final int id);

}