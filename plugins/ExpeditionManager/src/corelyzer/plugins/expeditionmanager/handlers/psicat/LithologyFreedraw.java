package corelyzer.plugins.expeditionmanager.handlers.psicat;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;

import corelyzer.helper.SceneGraph;
import corelyzer.plugin.freedraw.FreedrawContext;
import corelyzer.plugin.freedraw.FreedrawManager;
import corelyzer.plugins.expeditionmanager.ui.AbstractMovableFreedraw;
import corelyzer.plugins.expeditionmanager.util.scheme.SchemeEntry;
import corelyzer.plugins.expeditionmanager.util.scheme.SchemeManager;

/**
 * Renders a set of lithologies in Corelyzer.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class LithologyFreedraw extends AbstractMovableFreedraw {
    private final List<Interval> intervals;
    private final SchemeManager schemes;
    private Map<SchemeEntry, float[]> colorCache = new HashMap<SchemeEntry, float[]>();
    private Map<SchemeEntry, Texture> textureCache = new HashMap<SchemeEntry, Texture>();

    /**
     * Create a new LithologyFreedraw.
     * 
     * @param intervals
     *            the intervals.
     */
    public LithologyFreedraw(final List<Interval> intervals,
            final FreedrawManager manager, final double top, final double bot) {
        super(manager, "Lithology", new Rectangle2D.Double(top, 0.0,
                (bot - top), 0.1));

        this.intervals = intervals;
        schemes = SchemeManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        // clear our caches
        textureCache.clear();
        colorCache.clear();

        // dispose
        super.dispose();
    }

    private float[] getColor(final SchemeEntry entry) {
        // default color
        float[] color = new float[] { 0.0f, 0.0f, 0.0f };

        // no entry, so return default
        if ((entry == null) || !entry.getMap().containsKey("color")) {
            return color;
        }

        // check the color cache
        if (colorCache.containsKey(entry)) {
            return colorCache.get(entry);
        }

        // parse our RGB values
        String[] components = entry.getMap().get("color").split(",");
        if (components.length == 3) {
            color[0] = Float.parseFloat(components[0]) / 255f;
            color[1] = Float.parseFloat(components[1]) / 255f;
            color[2] = Float.parseFloat(components[2]) / 255f;
        }

        colorCache.put(entry, color);
        return color;
    }

    private Texture getTexture(final SchemeEntry entry) {
        // no entry so return null
        if ((entry == null) || !entry.getMap().containsKey("image")) {
            return null;
        }

        // return what is cached
        if (textureCache.containsKey(entry)) {
            return textureCache.get(entry);
        }

        // get our source URL
        URL schemeSourceURL = schemes.getSource(entry.getScheme(), entry
                .getScope());

        // get our image path
        String[] split = entry.getMap().get("image").split(":");
        String imagePath = split[split.length - 1]; // last component
        if (!imagePath.startsWith("/")) {
            imagePath = "/" + imagePath;
        }

        // generate a URL to our image
        String url = schemeSourceURL.toString();
        URL imageURL = null;
        try {
            imageURL = new URL(url.substring(0, url.lastIndexOf('!') + 1)
                    + imagePath);

            // create our texture from the URL
            Texture texture = TextureIO.newTexture(imageURL, true, imagePath
                    .substring(imagePath.lastIndexOf('.') + 1));

            // set up some parameters
            texture.setTexParameteri(GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
            texture.setTexParameteri(GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
            textureCache.put(entry, texture);
            return texture;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (GLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void render(final FreedrawContext context) {
        // set our gl parameters
        GL gl = context.getGl();

        gl.glPushAttrib(GL.GL_ALL_ATTRIB_BITS);

        boolean textureEnabled = gl.glIsEnabled(GL.GL_TEXTURE_2D);
        if (!textureEnabled) {
            gl.glEnable(GL.GL_TEXTURE_2D);
        }
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_BLEND);
        gl.glEnable(GL.GL_BLEND);

        // figure out our depth extents
        double left = SceneGraph.getCanvasPositionX(0) / 72 * 2.54 / 100;
        double right = (SceneGraph.getCanvasPositionX(0) + SceneGraph
                .getCanvasWidth(0)) / 72 * 2.54 / 100;

        // only draw the intervals on the screen
        for (Interval i : intervals) {
            if (((i.top >= left) && (i.top <= right))
                    || ((i.bot >= left) && (i.bot <= right))
                    || ((i.top <= left) && (i.bot >= right))) {
                if (i.entries.length != 0) {
                    renderInterval(context, i);
                    renderBorder(context, i);
                }
            }
        }

        gl.glDisable(GL.GL_BLEND);

        if (!textureEnabled) {
            gl.glDisable(GL.GL_TEXTURE_2D);
        }

        gl.glPopAttrib();
    }

    private void renderBorder(final FreedrawContext context,
            final Interval interval) {
        GL gl = context.getGl();

        float x = (float) interval.top;
        float y = context.getY();
        float w = (float) (interval.bot - interval.top);
        float h = context.getH();

        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);

        gl.glLineWidth(1);
        gl.glColor4f(1, 1, 1, 1);
        gl.glBegin(GL.GL_LINE_STRIP);
        {
            gl.glVertex3f(x, y, 0);
            gl.glVertex3f(x, y + h, 0);
            gl.glVertex3f(x + w, y + h, 0);
            gl.glVertex3f(x + w, y, 0);
            gl.glVertex3f(x, y, 0);
        }
        gl.glEnd();
    }

    private void renderInterval(final FreedrawContext context,
            final Interval interval) {
        // get our GL
        GL gl = context.getGl();

        // init some coordinates
        float x = (float) interval.top;
        float y = context.getY();
        float w = (float) (interval.bot - interval.top);
        float step = context.getH();

        // walk through our entries
        for (int i = 0; i < interval.entries.length; i++) {
            // figure out our h based on the ratio
            float h = (float) interval.ratios[i] * context.getH();
            float r = (float) interval.ratios[i];

            SchemeEntry entry = interval.entries[i];

            // set our color
            float[] color = getColor(entry);
            gl.glColor3f(color[0], color[1], color[2]);

            // get our texture
            Texture t = getTexture(entry);
            if (t == null) {
                // just draw a colored box
                gl.glBegin(GL.GL_QUADS);
                {
                    gl.glVertex3f(x, y, 0);
                    gl.glVertex3f(x, y + h, 0);
                    gl.glVertex3f(x + w, y + h, 0);
                    gl.glVertex3f(x + w, y, 0);
                }
                gl.glEnd();
            } else {
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
                t.bind();
                // render texture
                gl.glBegin(GL.GL_QUADS);
                {
                    int blocks = (int) Math
                            .floor(((interval.bot - interval.top) / step));
                    double remainder = interval.bot
                            - (interval.top + step * blocks);
                    x = (float) interval.top;
                    w = step;
                    for (int j = 0; j < blocks; j++) {
                        gl.glTexCoord2f(0.0f, 0.0f);
                        gl.glVertex2f(x, y);
                        gl.glTexCoord2f(0.0f, r);
                        gl.glVertex2f(x, y + h);
                        gl.glTexCoord2f(1.0f, r);
                        gl.glVertex2f(x + w, y + h);
                        gl.glTexCoord2f(1.0f, 0.0f);
                        gl.glVertex2d(x + w, y);
                        x += step;
                    }

                    if (remainder > 0) {
                        w = (float) remainder;
                        gl.glTexCoord2f(0.0f, 0.0f);
                        gl.glVertex2f(x, y);
                        gl.glTexCoord2f(0.0f, r);
                        gl.glVertex2f(x, y + h);
                        gl.glTexCoord2f(w / step, r);
                        gl.glVertex2f(x + w, y + h);
                        gl.glTexCoord2f(w / step, 0.0f);
                        gl.glVertex2d(x + w, y);
                    }
                }
                gl.glEnd();
            }

            // update our y
            y += h;
        }
    }
}
