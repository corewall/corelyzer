package corelyzer.plugin.iCores.data;

import java.util.Date;

public class SubscribeEntry {
    private boolean isSubscribed;
    private boolean isDownloaded;
    private boolean isUpdated;
    private float size;
    private String name;
    private String url;
    private Date lastModified;

    public SubscribeEntry() {
        name = "unknown entry name";
        url = "invalid url";
        lastModified = new Date();
    }

    public SubscribeEntry(String n, String u) {
        this();
        name = n;
        url = u;
    }

    public String toString() {
        return name;
    }
    
    // autogen getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isSubscribed() {
        return isSubscribed;
    }

    public void setSubscribed(boolean subscribed) {
        isSubscribed = subscribed;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void setUpdated(boolean updated) {
        isUpdated = updated;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }
}
