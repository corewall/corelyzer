package corelyzer.plugin.andrill;

import java.util.Vector;

public class SessionSection implements Comparable {
    public int    splitId;
    public int    wholeId;

    public boolean subscribedSplit;
    public boolean subscribedWhole;

    public int    globalId;

    public String name;
    public String splitCoreURL;
    public String wholeCoreURL;

    public float depth;
    public float length;

    public float splitDPIX;
    public float splitDPIY;
    public float wholeDPI;
    
    public Vector< SessionChat > splitChat;
    public Vector< SessionChat > wholeChat;

    public int compareTo(Object i) {
        SessionSection o = (SessionSection) i;
        if( depth < o.depth )
            return -1;
        if( depth > o.depth )
            return 1;
        return 0;
    }

    public SessionSection(String sectionName, float depth, float length) {
        splitId = -1;
        wholeId = -1;
        globalId = -1;
        name = sectionName;
        this.depth = depth;
        this.length = length;

        splitDPIX = -1;
        splitDPIY = -1;
        wholeDPI  = -1;

        subscribedSplit = false;
        subscribedWhole = false;

        splitChat = new Vector< SessionChat >();
        wholeChat = new Vector< SessionChat >();
    }

}
