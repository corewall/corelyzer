package corelyzer.plugin.iCores.data;

public class SectionEntry extends SubscribeEntry {
    private float startInterval;
    private float length;

    private ImageEntry image;

    public SectionEntry() {
        super();
    }

    public SectionEntry(float aStartInterval, float aLength, String aName,
                        String aURL)
    {
        this();
        startInterval = aStartInterval;
        length = aLength;

        this.setName(aName);
        this.setUrl(aURL);
    }

    public SectionEntry(ImageEntry aImage,
                        float aStartInterval, float aLength, String aName,
                        String aURL)
    {
        this(aStartInterval, aLength, aName, aURL);
        image = aImage;
    }

    public float getStartInterval() {
        return startInterval;
    }

    public void setStartInterval(float startInterval) {
        this.startInterval = startInterval;
    }

    public float getLength() {
        return length;
    }

    public void setLength(float length) {
        this.length = length;
    }

    public ImageEntry getImage() {
        return image;
    }

    public void setImage(ImageEntry image) {
        this.image = image;
    }
}
