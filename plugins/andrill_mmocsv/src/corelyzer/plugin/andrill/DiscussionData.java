package corelyzer.plugin.andrill;

public class DiscussionData {

    public String filename;
    public int    freeDrawId;
    public int    trackId;
    public int    sectionId;
    public int    globalId;
   
    public DiscussionData(int freedraw, int track, int section, int id,
                          String file) {
        filename = file;
        freeDrawId = freedraw;
        trackId = track;
        sectionId = section;
        globalId = id;
    }
}
