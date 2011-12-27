package corelyzer.plugin.iCores.data;

public class ImageEntry extends SubscribeEntry {
    private String format;
    private String sourceURL;
    private float dpi_x;
    private float dpi_y;

    public ImageEntry() {
        super();
    }

    public ImageEntry(String aFormat, String aURL, float dpix, float dpiy) {
        format = aFormat;
        sourceURL = aURL;
        this.dpi_x = dpix;
        this.dpi_y = dpiy;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getSourceURL() {
        return sourceURL;
    }

    public void setSourceURL(String sourceURL) {
        this.sourceURL = sourceURL;
    }

    public float getDpi_x() {
        return dpi_x;
    }

    public void setDpi_x(float dpi_x) {
        this.dpi_x = dpi_x;
    }

    public float getDpi_y() {
        return dpi_y;
    }

    public void setDpi_y(float dpi_y) {
        this.dpi_y = dpi_y;
    }
}
