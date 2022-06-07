package corelyzer.data;

import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;

public class CoreSectionTie {
    SectionTiePoint a, b;
    boolean show;

    // tie: SceneGraph tie ID
    public CoreSectionTie(final int tie) {
        createTiePointA(tie);
        createTiePointB(tie);
        show = SceneGraph.getSectionTieShow(tie);
    }
    
    public SectionTiePoint getTiePointA() { return a; }
    public SectionTiePoint getTiePointB() { return b; }

    private void createTiePointA(int tie) {
        final int a_track_id = SceneGraph.getSectionTieATrack(tie);
        Session a_session = CoreGraph.getInstance().getTrackSession(a_track_id);
        TrackSceneNode a_track = a_session.getTrackSceneNodeWithTrackId(a_track_id);
        String a_section_name = SceneGraph.getSectionTieASectionName(tie);
        float[] a_pos = SceneGraph.getSectionTieAPosition(tie);
        String a_desc = SceneGraph.getSectionTieADescription(tie);
        this.a = new SectionTiePoint(a_track.getName(), a_section_name, a_pos[0], a_pos[1], a_desc);
    }

    private void createTiePointB(int tie) {
        final int b_track_id = SceneGraph.getSectionTieBTrack(tie);
        Session b_session = CoreGraph.getInstance().getTrackSession(b_track_id);
        TrackSceneNode b_track = b_session.getTrackSceneNodeWithTrackId(b_track_id);
        String b_section_name = SceneGraph.getSectionTieBSectionName(tie);
        float[] b_pos = SceneGraph.getSectionTieBPosition(tie);
        String b_desc = SceneGraph.getSectionTieBDescription(tie);
        this.b = new SectionTiePoint(b_track.getName(), b_section_name, b_pos[0], b_pos[1], b_desc);
    }
}


