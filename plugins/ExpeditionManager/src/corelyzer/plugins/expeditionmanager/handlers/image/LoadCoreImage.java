package corelyzer.plugins.expeditionmanager.handlers.image;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionImage;
import corelyzer.data.TrackSceneNode;
import corelyzer.helper.SceneGraph;
import corelyzer.lib.datamodel.CoreImage;
import corelyzer.lib.datamodel.Image;
import corelyzer.plugins.expeditionmanager.util.SceneGraphUtil;
import corelyzer.services.cache.CacheService;
import corelyzer.ui.CorelyzerApp;

/**
 * Loads a CoreImage into Corelyzer.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class LoadCoreImage extends ImageJob {
    private static Semaphore lock = new Semaphore(1, true);

    /**
     * Create a new LoadCoreImage with the specified CoreImage.
     * 
     * @param image
     *            the CoreImage to load.
     */
    public LoadCoreImage(final CoreImage image) {
        super("Loading image @ " + image.getDepth(), image);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute() {
        // get our cache service
        CacheService cache = CacheService.getService();

        // get our local file
        File file = null;
        try {
            if (!cache.isCached(image.getURL())) {
                file = cache.put(image.getURL()).get();
            } else {
                file = cache.get(image.getURL()).get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return;
        }

        try {
            lock.acquire();

            // texturize the image
            if (SceneGraph.genTextureBlocks(file.getAbsolutePath()) == -1) {
                return;
            }
        

        // display the image
        // lock the SceneGraph
            SceneGraph.lock();
            // get our track id
            int trackId = SceneGraphUtil.getTrack(getTrackName(), true);

            // create a new section in the track
            int secCnt = SceneGraph.getNumSections(trackId);
            int sectionId = SceneGraph.addSectionToTrack(trackId, secCnt);

            // give the section a name
            String name = getSectionName();
            SceneGraph.setSectionName(trackId, sectionId, name);

            // load our image
            int imageId = SceneGraph.loadImage(file.getAbsolutePath());
            SceneGraph.setImageURL(imageId, image.getURL().toExternalForm());

            // associate or image and our section
            SceneGraph.addSectionImageToTrack(trackId, sectionId, imageId);

            // set the section properties
            boolean center = Boolean.parseBoolean(image.getConfiguration()
                    .getProperty("center", "false"));
            if (center) {
                SceneGraph.positionSection(trackId, sectionId, (float) (image
                        .getDepth() - image.getLength() / 2)
                        * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0), 0);
            } else {
                SceneGraph.positionSection(trackId, sectionId, (float) image
                        .getDepth()
                        * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0), 0);
            }
            if (image.getOrientation().equals(Image.HORIZONTAL)) {
                SceneGraph.setSectionDPI(trackId, sectionId, (float) image
                        .getDPIX(), (float) image.getDPIY());
            } else {
                SceneGraph.setSectionDPI(trackId, sectionId, (float) image
                        .getDPIY(), (float) image.getDPIX());
                // SceneGraph.rotateSection(trackId, sectionId, 90.0f);
                SceneGraph.setSectionOrientation(trackId, sectionId, SceneGraph.PORTRAIT);
            }

            SceneGraph.markSectionImmovable(trackId, sectionId, true);

            // update the UI lists
            CorelyzerApp app = CorelyzerApp.getApp();

            // get our track
            TrackSceneNode track = (TrackSceneNode) app.getTrackListModel()
                    .get(trackId);

            // add our section
            CoreSection section = new CoreSection(name, sectionId);
            track.addCoreSection(section);

            // add our image
            CoreSectionImage image = new CoreSectionImage(track, file
                    .getAbsolutePath(), sectionId);
            track.addChild(image, sectionId, imageId);
            track.Update();

            app.getSectionListModel().addElement(name);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            SceneGraph.unlock();
            lock.release();
        }

        CorelyzerApp.getApp().updateGLWindows();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDepth() {
        return image.getDepth();
    }
}
