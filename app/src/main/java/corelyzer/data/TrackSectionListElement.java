package corelyzer.data;

public class TrackSectionListElement {
	private String name = null;
	private boolean isNew = false;
	private boolean isTrack = false;
	private ImagePropertyTable.ImageProperties props = null;
	
	// default to non-track (i.e. a section) in two parameter constructor
	public TrackSectionListElement(final String name, final boolean isNew) {
		this( name, isNew, false );
	}
	
	public TrackSectionListElement(final String name, final boolean isNew, final boolean isTrack) {
		this.name = name;
		this.isNew = isNew;
		this.isTrack = isTrack;
		
		if ( this.isNew )
			props = new ImagePropertyTable.ImageProperties();
	}
	
	public String getName() { return name; }
	public void setName(final String name) { this.name = name; }
	public boolean isNew() { return isNew; }
	public void setIsNew(final boolean isNew) { this.isNew = isNew; }
	public boolean isTrack() { return isTrack; }
	public void setIsTrack(final boolean isTrack) { this.isTrack = isTrack; }
	public ImagePropertyTable.ImageProperties getImageProperties() { return props; }
	public void setImageProperties( final ImagePropertyTable.ImageProperties props ) { this.props = props; }
	
	public String toString() { return name; } // to render properly in JList
}
