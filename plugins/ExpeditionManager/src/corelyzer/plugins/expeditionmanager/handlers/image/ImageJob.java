package corelyzer.plugins.expeditionmanager.handlers.image;

import corelyzer.lib.datamodel.CoreImage;
import corelyzer.plugins.expeditionmanager.handlers.Job;

/**
 * A base class for image-related jobs.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class ImageJob extends Job {
    protected final CoreImage image;

    /**
     * Create a new ImageJob.
     * 
     * @param cif
     *            the core image file.
     */
    public ImageJob(final String name, final CoreImage cif) {
        super(name);
        image = cif;
    }

    /**
     * Gets the section name of the core image file.
     * 
     * @return the section name.
     */
    protected String getSectionName() {
        String filename = image.getURL().getPath();
        return filename.substring(filename.lastIndexOf('/') + 1, filename
                .lastIndexOf('.'));
    }

    /**
     * Gets the name of the track the core image file belongs in.
     * 
     * @return the track name .
     */
    protected String getTrackName() {
        String trackName = image.getConfiguration().getTrack();
        if ((trackName == null) || trackName.equals("")) {
            trackName = image.getURL().getPath();
            trackName = trackName.substring(0, trackName.lastIndexOf('/'));
        }
        return trackName;
    }
}
