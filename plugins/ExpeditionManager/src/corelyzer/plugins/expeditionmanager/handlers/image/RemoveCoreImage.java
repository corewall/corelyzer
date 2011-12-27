package corelyzer.plugins.expeditionmanager.handlers.image;

import corelyzer.data.CoreSection;
import corelyzer.data.TrackSceneNode;
import corelyzer.helper.SceneGraph;
import corelyzer.lib.datamodel.CoreImage;
import corelyzer.plugins.expeditionmanager.util.SceneGraphUtil;
import corelyzer.ui.CorelyzerApp;

/**
 * Removes a CoreImage from Corelyzer.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class RemoveCoreImage extends ImageJob {

    /**
     * Create a new RemoveCoreImage with the specified CoreImage.
     * 
     * @param image
     *            the CoreImage to remove.
     */
    public RemoveCoreImage(final CoreImage image) {
        super("Removing image @ " + image.getDepth(), image);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute() {
        // get our track id
        int trackId = SceneGraphUtil.getTrack(getTrackName(), false);
        if (trackId != -1) {
            // get the section name
            String name = getSectionName();

            // get our track scene node
            TrackSceneNode track = (TrackSceneNode) CorelyzerApp.getApp()
                    .getTrackListModel().get(trackId);
            CoreSection cs = track.getCoreSection(name);

            // delete the section if we found it
            if (cs != null) {
                // remove it from the track
                CorelyzerApp.getApp().getSectionListModel().removeElement(name);
                track.removeCoreSection(cs);

                SceneGraph.lock();
                SceneGraph.removeSectionImageFromTrack(trackId, cs.getId());
                SceneGraph.unlock();
            }
        }
        CorelyzerApp.getApp().updateGLWindows();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDepth() {
        return SYSTEM_JOB;
    }
}
