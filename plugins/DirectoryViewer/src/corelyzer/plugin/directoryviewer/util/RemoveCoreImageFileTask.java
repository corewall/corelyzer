package corelyzer.plugin.directoryviewer.util;

import java.io.File;

import corelyzer.data.CoreSection;
import corelyzer.data.TrackSceneNode;
import corelyzer.helper.SceneGraph;
import corelyzer.lib.datamodel.CoreImageFile;
import corelyzer.ui.CorelyzerApp;

/**
 * Removes a CoreImageFile from Corelyzer.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class RemoveCoreImageFileTask implements Runnable {
    private final CoreImageFile cif;

    /**
     * Create a new RemoveCoreImageFileTask.
     * 
     * @param cif
     *            the core image file.
     */
    public RemoveCoreImageFileTask(final CoreImageFile cif) {
        System.out.println("RemoveCoreImageFileTask "
                + cif.getFile().getAbsolutePath());
        this.cif = cif;
    }

    private int getTrack() {
        String trackName = cif.getConfig().getTrack();
        if ((trackName == null) || trackName.equals("")) {
            trackName = cif.getFile().getParentFile().getName();
        }

        return SceneGraph.getTrackIDByName(trackName);
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        // our file object
        File file = cif.getFile();

        // get our track id
        int trackId = getTrack();
        if (trackId != -1) {
            // get the section name
            String name = file.getName().substring(0,
                    file.getName().lastIndexOf('.'));

            // get our track scene node
            TrackSceneNode track = (TrackSceneNode) CorelyzerApp.getApp()
                    .getTrackListModel().get(trackId);
            CoreSection cs = track.getCoreSection(name);

            // delete the section if we found it
            if (cs != null) {
                CorelyzerApp.getApp().deleteSection(trackId, cs.getId());
            }
        }

        CorelyzerApp.getApp().updateGLWindows();
    }
}
