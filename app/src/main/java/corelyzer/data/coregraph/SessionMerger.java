package corelyzer.data.coregraph;

import java.util.Vector;

import corelyzer.data.*;
import corelyzer.graphics.SceneGraph;


public class SessionMerger {
    public static boolean mergeSessions(CoreGraph cg, Vector<Session> sessionsToMerge, Session destSession) {
        if (!canMergeSessions(sessionsToMerge)) { return false; }

        for (int i = 0; i < sessionsToMerge.size(); i++) {
            Session curSession = sessionsToMerge.get(i);
            for (TrackSceneNode track : curSession.getTrackSceneNodes()) {
                destSession.addTrack(track);
                SceneGraph.setTrackSessionName(track.getId(), destSession.getName());
            }
            for (WellLogDataSet ds : curSession.getDatasets()) {
                destSession.addDataset(ds);
            }
            curSession.clearTracks();
            curSession.clearDatasets();
            cg.removeSession(curSession);
        }
        cg.setCurrentSession(destSession);
        // TODO: Update session state file to avoid possibility of overwriting a merged
        // session's state file?    

        return true;
    }

    private static boolean canMergeSessions(Vector<Session> sessions) {
        // check all sessions for duplicated track and dataset names
        Vector<String> trackNames = new Vector<String>();
        Vector<String> datasetNames = new Vector<String>();
        for (Session s : sessions) {
            for (TrackSceneNode tsn : s.getTrackSceneNodes()) {
                if (!trackNames.contains(tsn.getName())) {
                    trackNames.add(tsn.getName());
                } else {
                    System.out.println("Duplicate track name " + tsn.getName() + " in session " + s.getName());
                    return false;
                }
            }
            for (WellLogDataSet d : s.getDatasets()) {
                if (!datasetNames.contains(d.getSourceFilename())) {
                    datasetNames.add(d.getSourceFilename());
                } else {
                    System.out.println("Duplicate dataset name " + d.getSourceFilename() + " in session " + s.getName());
                    return false;
                }
            }
        }
        return true;
    }
}
