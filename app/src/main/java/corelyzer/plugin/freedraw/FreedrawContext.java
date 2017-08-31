package corelyzer.plugin.freedraw;

//import javax.media.opengl.GL;
import com.jogamp.opengl.GL2;

/**
 * A class to hold freedraw state.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class FreedrawContext {
	final GL2 gl;
	final int freeDraw;
	final int canvas;
	final int track;
	final int section;
	final float x;
	final float y;
	final float w;
	final float h;
	final float scale;

	/**
	 * Create a new FreedrawContext.
	 * 
	 * @param gl
	 *            the GL object.
	 * @param freeDraw
	 *            the freedraw id.
	 * @param canvas
	 *            the canvas id.
	 * @param track
	 *            the track id.
	 * @param section
	 *            the section id.
	 * @param x
	 *            the x coordinate.
	 * @param y
	 *            the y coordinate.
	 * @param w
	 *            the width.
	 * @param h
	 *            the height.
	 * @param scale
	 *            the scale.
	 */
	public FreedrawContext(final GL2 gl, final int freeDraw, final int canvas, final int track, final int section, final float x, final float y, final float w,
			final float h, final float scale) {
		this.gl = gl;
		this.freeDraw = freeDraw;
		this.canvas = canvas;
		this.track = track;
		this.section = section;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.scale = scale;
	}

	public int getCanvas() {
		return canvas;
	}

	public int getFreeDraw() {
		return freeDraw;
	}

	public GL2 getGl() {
		return gl;
	}

	public float getH() {
		return h;
	}

	public float getScale() {
		return scale;
	}

	public int getSection() {
		return section;
	}

	public int getTrack() {
		return track;
	}

	public float getW() {
		return w;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

}
