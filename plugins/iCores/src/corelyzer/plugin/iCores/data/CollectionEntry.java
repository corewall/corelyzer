package corelyzer.plugin.iCores.data;

import java.util.Vector;

public class CollectionEntry extends SubscribeEntry {
    // private String projectName;
    private Vector<SectionEntry> sectionList;

    public CollectionEntry() {
        sectionList = new Vector<SectionEntry>();
    }

    public CollectionEntry(String n, String u) {
        this();
        this.setName(n);
        this.setUrl(u);
    }

    public Vector<SectionEntry> getSectionList() {
        return sectionList;
    }

    public void setSectionList(Vector<SectionEntry> sectionList) {
        this.sectionList = sectionList;
    }

    public String toString() {
        return this.getName();
    }

    /*
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    */
}
