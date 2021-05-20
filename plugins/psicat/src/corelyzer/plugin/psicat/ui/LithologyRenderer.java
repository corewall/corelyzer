package corelyzer.plugin.psicat.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import corelyzer.graphics.SceneGraph;
import corelyzer.plugin.freedraw.FreedrawContext;
import corelyzer.plugin.freedraw.FreedrawRenderer;
import corelyzer.plugin.psicat.scheme.SchemeEntry;
import corelyzer.plugin.psicat.scheme.SchemeManager;
import corelyzer.plugin.psicat.scheme.SchemeUtils;
import corelyzer.plugin.psicat.util.CSVReader;

/**
 * Renders the lithology data from PSICAT.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class LithologyRenderer implements FreedrawRenderer {
    /**
     * A simple class to hold our lithology data.
     * 
     * @author Josh Reed (jareed@andrill.org)
     */
    private static class Interval {
        double top = -1.0;
        double bot = -1.0;
        double[] ratios = new double[0];
        SchemeEntry[] entries = new SchemeEntry[0];

        @Override
        public Interval clone() {
            Interval clone = new Interval();
            clone.top = top;
            clone.bot = bot;
            clone.ratios = new double[ratios.length];
            clone.entries = new SchemeEntry[entries.length];
            for (int j = 0; j < clone.ratios.length; j++) {
                clone.ratios[j] = ratios[j];
                clone.entries[j] = entries[j];
            }
            return clone;
        }
    }

    /**
     * The name of our lithology data file.
     */
    public static final String LITHOLOGY_DATA = "lithology.tsv";

    /**
     * The name of the scheme.
     */
    private static final String SCHEME = "lithology";

    // our fields
    private final File file;
    private float top = Float.MAX_VALUE;
    private float bot = Float.MIN_VALUE;
    private List<Interval> intervals;
    private SchemeManager schemes;

    // caches
    private Map<SchemeEntry, float[]> colorCache = new HashMap<SchemeEntry, float[]>();
    private Map<SchemeEntry, Texture> textureCache = new HashMap<SchemeEntry, Texture>();

    /**
     * Create a new LithologyRenderer.
     * 
     * @param file
     *            the file.
     */
    public LithologyRenderer(final File file) {
        this.file = file;
    }

    /**
     * Do nothing...
     */
    public void dispose() {
        // do nothing
        textureCache.clear();
    }

    /**
     * Gets the bottom depth of the lithology data.
     * 
     * @return the bottom depth.
     */
    public float getBottom() {
        return bot;
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

    private Texture getTexture(final SchemeEntry entry, final GL2 gl) {
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
            texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
            texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
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
     * Gets the top depth of the lithology data.
     * 
     * @return the top depth.
     */
    public float getTop() {
        return top;
    }

    /**
     * Parse the lithology data.
     * 
     * @return true if there was lithology data.
     */
    public boolean parse() {
        // get and initialize our scheme manager
        schemes = SchemeManager.getInstance();

        // create a list to hold our intervals
        intervals = new ArrayList<Interval>();

        // parse the lithology data
        try {
            // get our zip file
            ZipFile zip = new ZipFile(file);

            // get the entry in the zip file
            ZipEntry entry = zip.getEntry(LITHOLOGY_DATA);
            if (entry == null) {
                return false;
            }

            // read in our data and create some structure from it
            BufferedReader br = new BufferedReader(new InputStreamReader(zip
                    .getInputStream(entry)));
            CSVReader csv = new CSVReader(br, '\t');
            String[] row;
            Interval i;
            while ((row = csv.nextRow()) != null) {
                i = parseInterval(row);
                if (i != null) {
                    intervals.add(i);

                    if (i.top < top) {
                        top = (float) i.top;
                    }

                    if (i.bot > bot) {
                        bot = (float) i.bot;
                    }
                }
            }
            csv.close();

            return true;
        } catch (ZipException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Interval parseInterval(final String[] row) {
        String scope = schemes.getScopesForScheme(SCHEME)[0];
        Interval interval;
        if (row.length >= 2) { // only continue if got two depths
            interval = new Interval();
            try {
                // parse our depths
                interval.top = Double.parseDouble(row[0]);
                interval.bot = Double.parseDouble(row[1]);

                // create our arrays
                interval.ratios = new double[row.length - 2];
                interval.entries = new SchemeEntry[row.length - 2];

                // parse the ratios and entries
                for (int i = 2; i < row.length; i++) {
                    String[] split = row[i].split(":");
                    if (split.length == 2) {
                        interval.ratios[i - 2] = Double.parseDouble(split[0]);
                        SchemeEntry entry = schemes.findSchemeEntry(SCHEME,
                                scope, SchemeUtils.stringToArray(split[1]));
                        interval.entries[i - 2] = entry;
                    } else {
                        interval.ratios[i - 2] = 0.0;
                        interval.entries[i - 2] = null;
                    }
                }
                return interval;
            } catch (NumberFormatException nfe) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void render(final FreedrawContext context) {
        // set our gl parameters
        GL2 gl = context.getGl();

        gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);

        boolean textureEnabled = gl.glIsEnabled(GL2.GL_TEXTURE_2D);
        if (!textureEnabled) {
            gl.glEnable(GL2.GL_TEXTURE_2D);
        }
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_BLEND);
        gl.glEnable(GL2.GL_BLEND);

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

        gl.glDisable(GL2.GL_BLEND);

        if (!textureEnabled) {
            gl.glDisable(GL2.GL_TEXTURE_2D);
        }

        gl.glPopAttrib();
    }

    private void renderBorder(final FreedrawContext context,
            final Interval interval) {
        GL2 gl = context.getGl();

        float x = (float) interval.top;
        float y = context.getY();
        float w = (float) (interval.bot - interval.top);
        float h = context.getH();

        gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE);

        gl.glLineWidth(1);
        gl.glColor4f(1, 1, 1, 1);
        gl.glBegin(GL2.GL_LINE_STRIP);
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
        // get our GL2
        GL2 gl = context.getGl();

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
            Texture t = getTexture(entry, gl);
            if (t == null) {
                // just draw a colored box
                gl.glBegin(GL2.GL_QUADS);
                {
                    gl.glVertex3f(x, y, 0);
                    gl.glVertex3f(x, y + h, 0);
                    gl.glVertex3f(x + w, y + h, 0);
                    gl.glVertex3f(x + w, y, 0);
                }
                gl.glEnd();
            } else {
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);
                t.bind(gl);
                // render texture
                gl.glBegin(GL2.GL_QUADS);
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
