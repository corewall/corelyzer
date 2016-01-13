package corelyzer.data;

import java.io.File;
//import java.util.Comparable;

import corelyzer.ui.AlphanumComparator;

public class TrackSectionListElement implements Comparable<TrackSectionListElement> {
	private File imageFile = null;  // should be non-null for new sections
	private String name = null;
	private boolean isNew = false;
	private boolean isTrack = false; // if it's not a track, it's a section
	private ImagePropertyTable.ImageProperties props = null;
	
	// default to section in two-parameter constructor
	public TrackSectionListElement(final String name, final boolean isNew) {
		this( name, isNew, false, null );
	}
	
	public TrackSectionListElement(final String name, final boolean isNew, final boolean isTrack) {
		this( name, isNew, isTrack, null );
	}

	public TrackSectionListElement(final String name, final boolean isNew, final boolean isTrack, final File imageFile) {
		this.name = name;
		this.isNew = isNew;
		this.isTrack = isTrack;
		
		if ( this.isNew )
		{
			this.imageFile = imageFile;
			props = new ImagePropertyTable.ImageProperties();
		}
	}
	
	public String getName() { return name; }
	public void setName(final String name) { this.name = name; }
	public boolean isNew() { return isNew; }
	public void setIsNew(final boolean isNew) { this.isNew = isNew; }
	public boolean isTrack() { return isTrack; }
	public void setIsTrack(final boolean isTrack) { this.isTrack = isTrack; }
	public boolean isNewTrack() { return this.isTrack && this.isNew; }
	public boolean isNewSection() { return !this.isTrack && this.isNew; }
	public void setImageFile(final File imageFile) { this.imageFile = imageFile; }
	public File getImageFile() { return imageFile; }
	
	public ImagePropertyTable.ImageProperties getImageProperties() { return props; }
	public void setImageProperties( final ImagePropertyTable.ImageProperties props ) { this.props = props; }
	
	public String toString() { return name; } // to render properly in JList
	
	public int compareTo(TrackSectionListElement elt) {
		return AlphanumComparator.compare(this.getName(), elt.getName());
	}
}
