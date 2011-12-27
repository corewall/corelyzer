package corelyzer.plugin.andrill;

import java.util.Date;

public class SessionAnnotation {
    public int    local;
    public int    global;
    public int    userID;
    public int    sectionID;
    public Date   date;
    public Date   lastModified;
    public String name;
    public String srcURL;
    public float  xpos_m; // x position in meters from section's x
    public float  ypos_m; // y position in meters from section's x


    public SessionAnnotation(int localId, int globalId) {
        local  = localId;
        global = globalId;
    }
}
