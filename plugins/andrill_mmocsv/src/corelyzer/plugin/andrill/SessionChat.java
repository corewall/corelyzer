package corelyzer.plugin.andrill;

import corelyzer.data.ChatGroup;
import corelyzer.data.MarkerType;

public class SessionChat {
    public int    global; // global chat id
    public int    local;  // local  chat id

    public int    section;// global section id
    public int    local_sectionid; // local section id

    public int    track;   // local

    public int    userID;
    public int    group;  // enumeration in ChatGroup
    public int    type;

    public float  xpos_m; // x position in meters from section's x
    public float  ypos_m;

    public long   created;       // when the chat is created
    public long   lastModified;  // the last modified time

    public String url;
    public String localfile;

    public boolean visibility;
    public String name;

    public SessionChat(int trackId, int sectionId, int localId, float posx, float posy) {
        track  = trackId;
        section = sectionId;
        local  = localId;
        xpos_m = posx;
        ypos_m = posy;
        global = -1;
        url = null;

        created      = -1;
        lastModified = -1;

        group  = ChatGroup.UNDEFINED;
        type   = MarkerType.CORE_POINT_MARKER;
        visibility = true;
    }

    public SessionChat(int trackId, int sectionId, int localId, int grp, int marker,
                       float posx, float posy)
    {
        this(trackId, sectionId, localId, posx, posy);
        this.group = grp;
        this.type = marker;
    }

    public SessionChat(int trackId, int sectionId, int localId, int grp, int marker,
                       float posx, float posy, long create)
    {
        this(trackId, sectionId, localId, grp, marker, posx, posy);
        this.created = create;
    }
}
