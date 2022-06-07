package corelyzer.data;

public class SectionTiePoint {
    public String track; // SceneGraph track name
    public String section; // SceneGraph section name
    public float x, y; // core-relative position in cm
    public String desc;

    public SectionTiePoint(String track, String section, float x, float y, String desc) {
        this.track = track;
        this.section = section;
        this.x = x;
        this.y = y;
        this.desc = desc;
    }
}