package corelyzer.plugins.expeditionmanager.ui;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionImage;
import corelyzer.data.TrackSceneNode;
import corelyzer.helper.SceneGraph;
import corelyzer.plugin.freedraw.FreedrawManager;
import corelyzer.plugin.freedraw.FreedrawRenderer;
import corelyzer.plugins.expeditionmanager.util.FileUtils;
import corelyzer.plugins.expeditionmanager.util.SceneGraphUtil;
import corelyzer.ui.CorelyzerApp;

/**
 * An abstract class to help with the creation of movable freedraw figures for a
 * particular track.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class AbstractMovableFreedraw implements FreedrawRenderer {
    protected static final double HANDLE_SIZE = 0.0;
    // the size of our handle image
    private static final int HANDLE_WIDTH = 32;
    private static final int HANDLE_HEIGHT = 32;

    // our fields
    protected final FreedrawManager manager;
    protected final String track;
    protected final Rectangle2D bounds;

    // our various ids
    protected int trackId = -1;
    protected int sectionId = -1;
    protected int handleId = -1;
    protected int freedrawId = -1;

    /**
     * Create a new AbstractMovableFreedraw.
     * 
     * @param manager
     *            the freedraw manager.
     * @param track
     *            the track.
     * @param bounds
     *            the bounds.
     */
    public AbstractMovableFreedraw(final FreedrawManager manager,
            final String track, final Rectangle2D bounds) {
        this.manager = manager;
        this.track = track;
        this.bounds = bounds;

        // initialize things
        initialize();
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        if (freedrawId != -1) {
            // remove our handle image
            try {
                SceneGraph.lock();
                SceneGraph.destroyFreeDrawRectangle(freedrawId);
                SceneGraph.removeSectionImageFromTrack(trackId, sectionId);
            } finally {
                SceneGraph.unlock();
            }
        }

        // remove the track
        if (trackId != -1) {
            CorelyzerApp.getApp().deleteTrack(track);
        }

        // reset everything
        handleId = -1;
        freedrawId = -1;
        sectionId = -1;
        trackId = -1;
    }

    private void initialize() {
        File handle = null;
        String name = null;
        try {
            // lock the scene graph
            SceneGraph.lock();

            // create our track
            if (trackId == -1) {
                trackId = SceneGraphUtil.getTrack(track, true);
            }

            // create a section
            int secCnt = SceneGraph.getNumSections(trackId);
            int sectionId = SceneGraph.addSectionToTrack(trackId, secCnt);

            // give the section a name
            name = track;
            SceneGraph.setSectionName(trackId, sectionId, name);

            // copy our dummy image to an actual file
            try {
                handle = File.createTempFile("handle_", ".bmp");
                FileUtils
                        .copy(
                                getClass()
                                        .getResourceAsStream(
                                                "/corelyzer/plugins/expeditionmanager/ui/resources/handle.bmp"),
                                new FileOutputStream(handle));
            } catch (FileNotFoundException e) {
                e.printStackTrace(System.err);
                return;
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return;
            }

            // load our handle image
            SceneGraph.genTextureBlocks(handle.getAbsolutePath());
            handleId = SceneGraph.loadImage(handle.getAbsolutePath());
            SceneGraph
                    .setImageURL(handleId, "file:" + handle.getAbsolutePath());

            // associate our handle and our section
            SceneGraph.addSectionImageToTrack(trackId, sectionId, handleId);

            // position the section
            float top = (float) bounds.getMinX() * 100.0f / 2.54f
                    * SceneGraph.getCanvasDPIX(0);
            SceneGraph.positionSection(trackId, sectionId, top, 0.0f);

            float dpiX = (float) (HANDLE_WIDTH / (bounds.getWidth() * 100.0f / 2.54f));
            float dpiY = (float) (HANDLE_HEIGHT / (bounds.getHeight() * 100.0f / 2.54f));
            SceneGraph.setSectionDPI(trackId, sectionId, dpiX, dpiY);

            // push to the back
            SceneGraph.pushSectionToEnd(trackId, sectionId);
        } finally {
            SceneGraph.unlock();
        }
        
     // create our freedraw and associate it with our track
        freedrawId = manager.createFreedrawForTrack(this, trackId,
                (float) bounds.getMinX(), (float) bounds.getMinY(),
                (float) bounds.getWidth(), (float) bounds.getHeight());

        // get our track scene node
        TrackSceneNode track = (TrackSceneNode) CorelyzerApp.getApp()
                .getTrackListModel().get(trackId);

        // add our handle
        CoreSection section = new CoreSection(name, sectionId);
        track.addCoreSection(section);

        // add our image
        CoreSectionImage image = new CoreSectionImage(track, handle
                .getAbsolutePath(), sectionId);
        track.addChild(image, sectionId, handleId);
        track.Update();

        CorelyzerApp.getApp().getSectionListModel().addElement(name);
    }
}
