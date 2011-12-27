package corelyzer.plugin.directoryviewer.util;

import java.io.File;

import corelyzer.data.CoreSection;
import corelyzer.data.CoreSectionImage;
import corelyzer.data.TrackSceneNode;
import corelyzer.helper.SceneGraph;
import corelyzer.lib.datamodel.CoreImageFile;
import corelyzer.lib.datamodel.Image;
import corelyzer.ui.CorelyzerApp;

/**
 * Adds a CoreImageFile to Corelyzer.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class AddCoreImageFileTask implements Runnable {
    private final CoreImageFile cif;

    /**
     * Create a new AddCoreImageFileTask.
     * 
     * @param cif
     *            the core image file.
     */
    public AddCoreImageFileTask(final CoreImageFile cif) {
        this.cif = cif;
    }

    private int getTrack() {
        String trackName = cif.getConfig().getTrack();
        if ((trackName == null) || trackName.equals("")) {
            trackName = cif.getFile().getParentFile().getName();
        }

        int trackId = SceneGraph.getTrackIDByName(trackName);
        if (trackId == -1) {
            trackId = CorelyzerApp.getApp().createTrack(trackName);
        }
        return trackId;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        // our file object
        File file = cif.getFile();

        // lock
        SceneGraph.lock();

        // get our track id
        int trackId = getTrack();

        // create a new section in the track
        int secCnt = SceneGraph.getNumSections(trackId);
        int sectionId = SceneGraph.addSectionToTrack(trackId, secCnt);

        // give the section a name
        String name = file.getName().substring(0,
                file.getName().lastIndexOf('.'));
        SceneGraph.setSectionName(trackId, sectionId, name);

        // load our image
        int imageId = SceneGraph.loadImage(file.getAbsolutePath());
        SceneGraph.setImageURL(imageId, "file:"
                + cif.getFile().getAbsolutePath());

        // associate or image and our section
        SceneGraph.addSectionImageToTrack(trackId, sectionId, imageId);

        // set the section properties
        SceneGraph.positionSection(trackId, sectionId, (float) cif.getDepth()
                * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0), 0);
        if (cif.getOrientation().equals(Image.HORIZONTAL)) {
            SceneGraph.setSectionDPI(trackId, sectionId, (float) cif.getDPIX(),
                    (float) cif.getDPIY());
        } else {
            SceneGraph.setSectionDPI(trackId, sectionId, (float) cif.getDPIY(),
                    (float) cif.getDPIX());
            // SceneGraph.rotateSection(trackId, sectionId, 90.0f);
            SceneGraph.setSectionOrientation(trackId, sectionId, SceneGraph.PORTRAIT);
        }

        SceneGraph.positionScene((float) cif.getDepth() * 100.0f / 2.54f
                * SceneGraph.getCanvasDPIX(0), 0);
        SceneGraph.unlock();

        // update the UI lists
        CorelyzerApp app = CorelyzerApp.getApp();

        // get our track
        TrackSceneNode track = (TrackSceneNode) app.getTrackListModel().get(
                trackId);

        // add our section
        CoreSection section = new CoreSection(name, sectionId);
        track.addCoreSection(section);

        // add our image
        CoreSectionImage image = new CoreSectionImage(track, file
                .getAbsolutePath(), sectionId);
        track.addChild(image, sectionId, imageId);
        track.Update();

        app.getSectionListModel().addElement(name);
        CorelyzerApp.getApp().updateGLWindows();
    }
}
