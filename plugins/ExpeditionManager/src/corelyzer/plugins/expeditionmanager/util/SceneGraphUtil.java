package corelyzer.plugins.expeditionmanager.util;

import java.util.Collections;
import java.util.List;

import corelyzer.data.CoreSection;
import corelyzer.data.TrackSceneNode;
import corelyzer.helper.SceneGraph;
import corelyzer.ui.CorelyzerApp;

/**
 * Some utility methods for doing common tasks in the SceneGraph.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class SceneGraphUtil {

    /**
     * Get a track by name, optionally creating it if it doesn't exist.
     * 
     * @param name
     *            the name.
     * @param create
     *            the create flag.
     * @return the track id or -1 if it didn't exist and wasn't created.
     */
    public static int getTrack(final String name, final boolean create) {
        // get our track id
        int trackId = SceneGraph.getTrackIDByName(name);

        // if it didn't exist then create it
        if ((trackId == -1) && create) {
            // try to lay out the tracks so they don't overlap

            float dy = 0.0f;
            List<TrackSceneNode> tracks = (List<TrackSceneNode>) Collections
                    .list(CorelyzerApp.getApp().getTrackListModel().elements());
            for (TrackSceneNode track : tracks) {
                if (track.getNumCores() > 0) {
                    CoreSection section = track.getCoreSection(0);
                    if (SceneGraph.getSectionRotation(track.getId(), section
                            .getId()) == 0) {
                        // use the height if it is not rotated
                        dy += SceneGraph.getSectionHeight(track.getId(),
                                section.getId());
                    } else {
                        // use the width if it is rotated
                        dy += SceneGraph.getSectionWidth(track.getId(), section
                                .getId());
                    }
                }
            }

            // create our track and set it's Y position
            trackId = CorelyzerApp.getApp().createTrack(name);
            SceneGraph.moveTrack(trackId, 0f, dy * SceneGraph.getCanvasDPIY(0)
                    / 2.54f);
        }
        return trackId;
    }

    private SceneGraphUtil() {
        // not to be instantiated
    }
}
