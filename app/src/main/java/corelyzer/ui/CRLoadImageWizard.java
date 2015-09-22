package corelyzer.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.*;
import java.io.File;
import java.util.Collections;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import net.miginfocom.swing.MigLayout;

import corelyzer.data.*;
import corelyzer.data.coregraph.*;
import corelyzer.graphics.SceneGraph;
import corelyzer.util.FileUtility;

// This "wizard" attempts to simplify the section image loading process. It allows multiple
// tracks to be created and populated in a single transaction. It makes best guesses as to
// where selected image files should be added to existing tracks (if possible) based on their
// names. Users can then reorder these images as they wish before loading occurs.

public class CRLoadImageWizard extends JDialog {
	public static void main(final String[] args) {
		File testFile = new File("/Users/bgrivna/Documents/Corelyzer/Core Repository/GLAD6/images/GLAD6-BOS04-3C-1H-1.BMP");
		Vector<File> testFileVec = new Vector<File>();
		testFileVec.add( testFile );
		CRLoadImageWizard dialog = new CRLoadImageWizard(null, testFileVec);
		dialog.pack();
		dialog.setVisible(true);
	}

	private JPanel contentPane, activePane;
	private JButton nextButton, previousButton, finishButton, cancelButton;
	private SectionListPane sectionListPane;
	private ImagePropertiesPane imagePropertiesPane;
	private boolean firstOpenOfPropertiesPane = true;
	private TrackSectionListModel trackSectionModel;
	private Vector<File> newFiles;
	
	public static String UNRECOGNIZED_SECTIONS_TRACK = "Unrecognized Sections";

	public CRLoadImageWizard(final Frame owner, final Vector<File> newFiles) {
		super(owner);
		this.newFiles = newFiles;
		
		loadTrackData();
		setupUI();
		updateUI();
	}

	// add panel and buttons for active pane
	private void updateUI()
	{
		contentPane.removeAll();
		contentPane.add( activePane, "growy" );
		
		if ( activePane.equals( sectionListPane ))
		{
			setTitle("Arrange New Sections");
			contentPane.add(nextButton, "split 2, align right, aligny bottom");
			contentPane.add(cancelButton, "aligny bottom");
			getRootPane().setDefaultButton( nextButton );
		}
		else
		{
			setTitle("Section Image Properties");
			contentPane.add(previousButton, "split 3, align right");
			contentPane.add(finishButton);
			contentPane.add(cancelButton);
			getRootPane().setDefaultButton( finishButton );
		}

		pack();
		repaint();
	}
	
	// For each new section, set its ImageProperties.dpix, .dpiy, and .orientation
	// fields so its length and depth can be calculated in ImagePropertiesPane.
	private void setNewSectionsDPIAndOrientation()
	{
		final float dpix = sectionListPane.getDPIX();
		final float dpiy = sectionListPane.getDPIY();
		final String orientation = sectionListPane.getOrientation();
		
		for ( Vector<TrackSectionListElement> track : trackSectionModel.getTrackSectionVector() )
		{
			for ( int secIndex = 1; secIndex < track.size(); secIndex++ )
			{
				TrackSectionListElement section = track.elementAt( secIndex );
				if ( section.isNew() )
				{
					section.getImageProperties().dpix = dpix;
					section.getImageProperties().dpiy = dpiy;
					section.getImageProperties().orientation = orientation;
				}
			}
		}
	}
	
	private void onNext()
	{
		if ( !sectionListPane.validateDPIFields( this ))
			return;
		
		if ( firstOpenOfPropertiesPane )
		{
			// User may modify values in properties pane, then return to previous pane -
			// Make sure we don't overwrite potential edits by initializing again!
			setNewSectionsDPIAndOrientation();
			imagePropertiesPane.updateSectionProperties();
			firstOpenOfPropertiesPane = false;
		}

		activePane = imagePropertiesPane;

		updateUI();
	}
		
	private void onPrevious()
	{
		imagePropertiesPane.saveSectionProperties();
		activePane = sectionListPane;
		updateUI();
	}
	
	private void onFinish() { 
		imagePropertiesPane.saveSectionProperties();
		Runnable loading = new Runnable() {
			public void run() {
				onConfirmLoad();
			}
		};
		new Thread(loading).start();
		
		dispose();
	}
	
	// create UI components, they'll be added to the content pane in updateUI()
	private void setupUI()
	{
		sectionListPane = new SectionListPane(trackSectionModel);
		imagePropertiesPane = new ImagePropertiesPane(trackSectionModel);

		contentPane = new JPanel( new MigLayout( "filly, wrap 1", "[]15[]", "[c,grow 100,fill][c,grow 0,fill]"));
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dispose();
			}
		});
		
		nextButton = new JButton("Next >");
		nextButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				onNext();
			}
		});
		
		previousButton = new JButton("< Previous");
		previousButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				onPrevious();
			}
		});
		
		finishButton = new JButton("Finish");
		finishButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				onFinish();
			}
		});
		
		setContentPane( contentPane );
		
		activePane = sectionListPane; // set initial pane in preparation for updateUI()
	}
	
	private TrackSceneNode createTrack(final Session session, final String trackName)
	{
		// we'll stifle duplication in GUI
		TrackSceneNode newTrack = null;
		int newTrackId = SceneGraph.addTrack( session.getName(), trackName );
		if ( newTrackId >= 0 )
		{
			// create the node and add it to the listing of tracks
			newTrack = new TrackSceneNode( trackName, newTrackId );
			CoreGraph.getInstance().addTrack( session, newTrack );
		}

		return newTrack;
	}
	
	private void loadTrackData() {
		SortedMap<String, TreeSet<String>> map = new TreeMap<String, TreeSet<String>>(new AlphanumComparator.StringAlphanumComparator());
		SortedMap<String, ImagePropertyTable.ImageProperties> imageProps = new TreeMap<String, ImagePropertyTable.ImageProperties>();
		Vector<File> dupSecs = new Vector<File>();
		for (Session s : CoreGraph.getInstance().getSessions()) {
			for (TrackSceneNode tsn : s.getTrackSceneNodes()) {
				map.put(tsn.getName(), new TreeSet<String>());
				for (CoreSection cs : tsn.getCoreSections()) {
					map.get(tsn.getName()).add(cs.getName());
					
					// gather image properties here, while we have access to TrackSceneNode and CoreSection's IDs
					imageProps.put(cs.getName(), getSectionProperties( tsn.getId(), cs.getId()));
					
					// note files whose name matches an existing section
					for (File f : newFiles) {
						if (cs.getName().equals(FileUtility.stripExtension(f.getName()))) {
							dupSecs.add(f);
						}
					}
				}
			}
		}
		
		if (newFiles == null) return;
		for (File f : dupSecs) { newFiles.remove(f); } // remove duplicate files		

		Vector<File> unrec = new Vector<File>(); // track unrecognized sections
		for (File curFile : newFiles) {
			final String secName = FileUtility.stripExtension(curFile.getName());
			final String fullTrackID = FileUtility.parseFullTrackID(secName);
			if (fullTrackID != null) {
				String trackKey = getTrackKey(map, fullTrackID);
				if (trackKey != null) {
					map.get(trackKey).add(secName + "*");
				} else {
					TreeSet<String> newSet = new TreeSet<String>(new AlphanumComparator.StringAlphanumComparator());
					newSet.add(secName + "*");
					map.put(fullTrackID + "*", newSet);
				}
			} else {
				unrec.add(curFile);
			}
		}

		Vector<Vector<TrackSectionListElement>> tsVec = new Vector<Vector<TrackSectionListElement>>();
		for (String key : map.keySet()) {
			//System.out.println(key + ": ");
			
			final boolean isNew = key.endsWith("*");
			final String listKey = isNew ? key.substring(0, key.length() - 1) : key; // strip off asterisk 
			Vector<TrackSectionListElement> track = new Vector<TrackSectionListElement>();
			TrackSectionListElement trackElt = new TrackSectionListElement(listKey, isNew, true);
			track.add(trackElt);
			for (String val : map.get(key)) {
				//System.out.println("   " + val);
				final boolean newSec = val.endsWith("*");
				if (newSec) { val = val.substring(0, val.length() - 1);	}
				
				TrackSectionListElement newElt = new TrackSectionListElement(val, newSec, false, findSourceFile(val));
				if (!newSec) { // set image properties for existing sections
					newElt.setImageProperties(imageProps.get(val));
				}
				track.add(newElt);
			}
			
			tsVec.add(track);
		}
		
		Vector<TrackSectionListElement> unrecVec = new Vector<TrackSectionListElement>();
		unrecVec.add(new TrackSectionListElement(UNRECOGNIZED_SECTIONS_TRACK, false /* prevent renaming */, true));
		for (File f : unrec) {
			unrecVec.add(new TrackSectionListElement(FileUtility.stripExtension(f.getName()), true, false, f));
		}
		
		// add unrecognized sections vector if it contains any sections.
		if ( unrecVec.size() > 1 ) {
			tsVec.add(unrecVec);
			JOptionPane.showMessageDialog( this, "One or more images could not be auto-sorted. They have been added to the Unrecognized Sections" +
					"\nlist and should be moved. Images remaining in Unrecognized Sections will not be loaded." );
		}
		
		trackSectionModel = new TrackSectionListModel(tsVec);
	}
	
	private String getTrackKey(Map map, String trackID) {
		if (map.containsKey(trackID)) return trackID;
		if (map.containsKey(trackID + "*")) return trackID + "*";
		return null;		
	}
	
	private File findSourceFile(String secName) {
		File srcFile = null;
		for (File f : newFiles) {
			if (f.getName().startsWith(secName)) {
				srcFile = f;
				break;
			}
		}
		return srcFile;
	}
	
	private void onConfirmLoad()
	{
		int curProgressValue = 1;
		JProgressBar progress = CorelyzerApp.getApp().getProgressUI();
		progress.setString("Loading Images");
		progress.setMaximum( trackSectionModel.getNewSectionCount() + 1 );
		progress.setValue( curProgressValue );
		
		final Vector<Vector<TrackSectionListElement>> tsVec = trackSectionModel.getTrackSectionVector();
		
		Session session = CoreGraph.getInstance().getCurrentSession();
		
		for (Vector<TrackSectionListElement> trackVec : tsVec)
		{
			TrackSceneNode curTrack = null;
			final TrackSectionListElement trackElt = trackVec.elementAt( 0 );
			if ( trackElt.isNewTrack() )
			{
				// never load sections left in Unrecognized Sections track
				if ( trackElt.getName().equals( UNRECOGNIZED_SECTIONS_TRACK ))
					continue;
				
				curTrack = createTrack( session, trackElt.getName() );
			}
			else {
				curTrack = session.getTrackSceneNodeWithName(trackElt.getName());
			}
			if ( curTrack != null )
			{
				for ( int eltIndex = 1; eltIndex < trackVec.size(); eltIndex++ )
				{
					final TrackSectionListElement sectionElt = trackVec.elementAt( eltIndex );
					if ( sectionElt.isNew() )
					{						
						File imageFile = sectionElt.getImageFile();
						if ( imageFile != null ) {
							// add section to track
							progress.setString("Loading " + imageFile.getName() );
							
							final ImagePropertyTable.ImageProperties imageProps = sectionElt.getImageProperties();
							boolean isVertical = imageProps.orientation.equals("Vertical");
							//System.out.println(imageFile.getName() + " depth pixels = " + 
							//		SceneGraph.getImageDepthPix( imageFile.toString(), isVertical ));
							
							final int insertIndex = eltIndex - 1;
							final int sectionId = FileUtility.loadImageFile( imageFile, null, sectionElt.getName(), curTrack, insertIndex );
							if ( sectionId != -1 )
							{
								FileUtility.setSectionImageProperties(curTrack, sectionElt.getName(), sectionId,
										imageProps.length, imageProps.depth,
										imageProps.dpix, imageProps.dpiy, imageProps.orientation);
							}
							progress.setValue(++curProgressValue);
						} else {
							System.out.println("New section has null imageFile");
						}
					}
					else
					{
						// if sections were inserted, need to adjust existing sections' depth
						final int sectionId = curTrack.getCoreSection(sectionElt.getName()).getId();
						final float oldDepthInCM = SceneGraph.getSectionDepth( curTrack.getId(), sectionId );
						final float newDepthInCM = sectionElt.getImageProperties().depth * 100.0f; // convert m to cm
						final float depthChangeInPix = ( newDepthInCM - oldDepthInCM ) * SceneGraph.getCanvasDPIX( 0 ) * ( 1.0f / 2.54f );
						if ( Math.abs( depthChangeInPix ) > 0.0f )
							SceneGraph.moveSection( curTrack.getId(), sectionId, depthChangeInPix, 0.0f );
					}
				}
			}
			else {
				System.out.println("Couldn't create/load track");
			}
		}
		
		progress.setString("Section image loading complete");
		progress.setValue(0);
	}
	
	private void initializeSectionImageProperties()
	{
		for ( Vector<TrackSectionListElement> track : trackSectionModel.getTrackSectionVector() )
		{
			float curDepth = 0.0f;
			for ( int secIndex = 1; secIndex < track.size(); secIndex++ )
			{
				TrackSectionListElement section = track.elementAt( secIndex );
				if ( !section.isNew() ) {
					curDepth = section.getImageProperties().depth + section.getImageProperties().length;
				} else {
					// new section, default length and depth
					section.getImageProperties().depth = curDepth;
					section.getImageProperties().length = 1.5f; // meters
					
					section.getImageProperties().orientation = sectionListPane.getOrientation();
					section.getImageProperties().dpix = sectionListPane.getDPIX();
					section.getImageProperties().dpiy = sectionListPane.getDPIY();

					// attempt to determine section's actual length
					File imageFile = section.getImageFile();
					if ( imageFile != null ) {
						final boolean isVertical = section.getImageProperties().orientation.equals("Vertical");
						final float depthDPI = isVertical ? section.getImageProperties().dpiy :
							section.getImageProperties().dpix;
						final int lengthInPix = SceneGraph.getImageDepthPix( imageFile.toString(), isVertical );
						section.getImageProperties().length = (( lengthInPix / depthDPI ) * 2.54f ) / 100.0f;
					} else {
						System.out.println("New section has null imageFile");
					}
					
					curDepth += section.getImageProperties().length;
					
					// if necessary, push subsequent pre-existing sections deeper to create space
					// TODO: only push if there isn't sufficient space for the new core to be
					// added without overlapping.
					boolean firstSubSec = true;
					float depthOffset = 0.0f;
					for ( int subSecIndex = secIndex + 1; subSecIndex < track.size(); subSecIndex++ ) {
						TrackSectionListElement subSection = track.elementAt( subSecIndex );
						if ( !subSection.isNew() )
						{
							if ( firstSubSec )
							{
								depthOffset = curDepth - subSection.getImageProperties().depth;
								subSection.getImageProperties().depth = curDepth;
								firstSubSec = false;
							}
							else
							{
								subSection.getImageProperties().depth += depthOffset;
							}
						}
					}
				}
			}
		}
	}
	
	private ImagePropertyTable.ImageProperties getSectionProperties( final int trackId, final int sectionId )
	{
		ImagePropertyTable.ImageProperties props = new ImagePropertyTable.ImageProperties();
		// use getImageIDForSection to determine whether section is legitimate
		if ( SceneGraph.getImageIdForSection( trackId, sectionId ) != -1 )
		{
			props.depth = SceneGraph.getSectionDepth( trackId, sectionId ) / 100.0f; // convert cm depth to m
			props.length = SceneGraph.getSectionLength( trackId, sectionId ) / 100.0f; // convert cm length to m
			props.dpix = SceneGraph.getSectionDPIX( trackId, sectionId );
			props.dpiy = SceneGraph.getSectionDPIY( trackId, sectionId );
			props.orientation = SceneGraph.getSectionOrientation( trackId, sectionId ) ? "Vertical" : "Horizontal";
		}
		
		return props;
	}
}


class SectionListPane extends JPanel implements ListSelectionListener {
	private JButton renameButton, deleteButton, moveUpButton, moveDownButton, moveToTrackButton, newButton;
	private JScrollPane tslScrollPane;
	private JComboBox orientationComboBox;
	private JTextField dpiXField, dpiYField;
	private JList trackSectionList;
	private TrackSectionListModel trackSectionModel;
	
	public SectionListPane( TrackSectionListModel trackSectionModel )
	{
		super( new MigLayout( "wrap 2, fillx",  "[]10[]", "[][c, grow 0]15[][c, grow 0][c, grow 100]" ));
		
		this.trackSectionModel = trackSectionModel;
		setupUI();
		
		trackSectionList.setModel( trackSectionModel );
	}
	
	// The lone ListSelectionListener interface method
	public void valueChanged(ListSelectionEvent e)
	{
		// This is called twice when the selection changes, first with getValueIsAdjusting()
		// returning true, second time false. No idea why, but there's no need to do things twice.
		if ( !e.getValueIsAdjusting() )
		{
			final int[] selectedIndices = trackSectionList.getSelectedIndices();
			
			int trackCount = 0, newTrackCount = 0, newSectionCount = 0, oldSectionCount = 0;
			for ( int curSelIndex : selectedIndices )
			{
				TrackSectionListElement tsle = (TrackSectionListElement)trackSectionModel.getElementAt( curSelIndex );
				if ( tsle.isTrack() ) {
					trackCount++; // count each track
					if ( tsle.isNew() )
						newTrackCount++; // of those tracks, how many are new?
				} else if (	tsle.isNew() ) {
					newSectionCount++;
				} else {
					oldSectionCount++;
				}
			}
				
			boolean deletableTrack = false;
			if  ( trackCount == 1 && newSectionCount + oldSectionCount == 0 )
			{
				final int curSelIndex = selectedIndices[0];
				TrackSectionListElement tsle = (TrackSectionListElement)trackSectionModel.getElementAt( curSelIndex );
				if ( tsle.isNewTrack() )
				{
					// empty tracks can be deleted
					if ( curSelIndex == trackSectionModel.getSize() - 1 ) {
						// if current track is the last item in the list, it contains no sections
						deletableTrack = true;
					} else {
						// if next element is a track, current track contains no sections
						final TrackSectionListElement nextElt = (TrackSectionListElement)trackSectionModel.getElementAt( curSelIndex + 1 );
						if ( nextElt.isTrack() )
							deletableTrack = true;
					}
				}
			}
			
			// Only newly added sections can be moved up/down and deleted
			enableSectionButtons( newSectionCount > 0 && trackCount + oldSectionCount == 0 );
			
			// Only a single new track can be renamed at a time
			enableTrackButtons( trackCount == 1 && newTrackCount == 1 && newSectionCount + oldSectionCount == 0);
			
			// Only new sections and empty tracks can be deleted
			enableDeleteButton(( newSectionCount > 0 && oldSectionCount + trackCount == 0 ) || deletableTrack );
		}
	}
	
	public String getOrientation()
	{
		String orientationStr = ( orientationComboBox.getSelectedIndex() == 0 ? "Horizontal" : "Vertical" );
		return orientationStr;
	}
	public float getDPIX() { return Float.parseFloat( dpiXField.getText() ); }
	public float getDPIY() { return Float.parseFloat( dpiYField.getText() ); }
	public boolean validateDPIFields( final JDialog owner )
	{
		final String dpix = dpiXField.getText();
		final String dpiy = dpiYField.getText();
		
		if ( dpix.length() == 0 || dpiy.length() == 0 )
		{
			JOptionPane.showMessageDialog( owner, "Please enter a value in both DPI fields." );
			return false;
		}
		
		try {
			Float.parseFloat( dpix );
			Float.parseFloat( dpiy );
		} catch ( NumberFormatException nfe ) {
			JOptionPane.showMessageDialog( owner, "Invalid DPI value: " + nfe.getMessage() );
			return false;
		}
		
		return true;
	}
	
	private void doRenameTrack()
	{
		String newTrackName = JOptionPane.showInputDialog( this, "Please enter new track name", "[new name]" );
		if ( newTrackName != null )
		{
			trackSectionModel.renameTrack( trackSectionList.getSelectedIndex(), newTrackName );
			trackSectionList.repaint();
		}
	}
	
	private int findTrack(String trackName)
	{
		int result = -1;
		final int eltCount = trackSectionModel.getSize();
		for (int index = 0; index < eltCount; index++)
		{
			TrackSectionListElement tsle = (TrackSectionListElement)trackSectionModel.getElementAt(index);
			if (tsle.isTrack()) {
				if (tsle.getName().equals(trackName)) {
					result = index;
					break;
				}
			}
		}
		return result;
	}
	
	private String selectTrack()
	{
		// build list of track names (gross, but the easiest way to spin through elements...)
		Vector<Object> trackNames = new Vector<Object>(); 
		final int eltCount = trackSectionModel.getSize();
		for (int index = 0; index < eltCount; index++) {
			TrackSectionListElement tsle = (TrackSectionListElement)trackSectionModel.getElementAt(index);
			if (tsle.isTrack())
				trackNames.add(tsle.getName());
		}
		
		Object[] tnArray = trackNames.toArray(); 
		String result = (String)JOptionPane.showInputDialog(this, "Select a destination track:", "Select Track",
				JOptionPane.PLAIN_MESSAGE, null, tnArray, tnArray[0]);
		
		return result;
	}
	
	private void enableSectionButtons( final boolean enable ) {
		moveUpButton.setEnabled( enable );
		moveDownButton.setEnabled( enable );
		moveToTrackButton.setEnabled( enable );
	}
	
	private void enableTrackButtons( final boolean enable ) { renameButton.setEnabled( enable ); }
	private void enableDeleteButton( final boolean enable ) { deleteButton.setEnabled( enable ); }
	
	private void setupUI() {
		JLabel separatorLabel = new JLabel("Section Image Properties");
		separatorLabel.setForeground( new Color( 0, 70, 213 ));
		JSeparator separator = new JSeparator();
		this.add( separatorLabel, "span 2, split 2" );
		this.add( separator, "gapleft rel, growx, wrap" );
		
		dpiXField = new JTextField();
		dpiYField = new JTextField();
		orientationComboBox = new JComboBox();
		final DefaultComboBoxModel orientationModel = new DefaultComboBoxModel();
		orientationModel.addElement("Horizontal");
		orientationModel.addElement("Vertical");
		orientationComboBox.setModel(orientationModel);
		
		// if DPI Y field is empty, copy DPI X input over since DPI X/Y are usually the same
		dpiXField.addFocusListener( new FocusAdapter() {
			public void focusLost( FocusEvent e ) {
				if ( dpiYField.getText().length() == 0 ) {
					dpiYField.setText( dpiXField.getText() );
				}
			}
		});
		
		this.add( new JLabel("DPI X:"), "span 2, split 6");
		this.add(dpiXField, "gap rel, growx");
		this.add( new JLabel("DPI Y:"), "gap unrel");
		this.add(dpiYField, "gap rel, growx");
		this.add( new JLabel("Orientation:"), "gap unrel");
		this.add(orientationComboBox, "gap rel");
		
		JLabel arragementLabel = new JLabel("Section Arrangement");
		arragementLabel.setForeground( new Color( 0, 70, 213 ));
		JSeparator arrSeparator = new JSeparator();
		this.add( arragementLabel, "span 2, split 2" );
		this.add( arrSeparator, "gapleft rel, growx, wrap" );
		
		JLabel iconExplainLabel = new JLabel("Indicates loaded section or newly-created track");
		iconExplainLabel.setIcon( new ImageIcon( "resources/icons/newCircle.gif" ));
		this.add( iconExplainLabel, "span 2, align left" );

		trackSectionList = new JList();
		trackSectionList.setCellRenderer(new TrackSectionListCellRenderer());
		trackSectionList.addListSelectionListener( this );
		
		tslScrollPane = new JScrollPane();
		tslScrollPane.setViewportView(trackSectionList);
		this.add(tslScrollPane, "width 250::, height 200:400:, growx, growy");
		
		moveUpButton = new JButton("Move Up");
		moveUpButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// the array of selected indices is guaranteed to be in ascending order
				int[] selectedIndices = trackSectionList.getSelectedIndices();
				for ( int i = 0; i < selectedIndices.length; i++ ) {
					final boolean moved = trackSectionModel.moveItemUp( selectedIndices[i] );
					
					// When moving up, we try to move the uppermost item first. If it can't be
					// moved, nothing beneath it can be moved either.
					if ( !moved )
						return;
				}

				// if we make it here, all items were moved successfully
				for ( int i = 0; i < selectedIndices.length; i++ ) {
					selectedIndices[i]--;
				}
				trackSectionList.setSelectedIndices( selectedIndices );
				trackSectionList.repaint();
			}
		});
		this.add(moveUpButton, "split 6, flowy, growx");
		
		moveDownButton = new JButton("Move Down");
		moveDownButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// the array of selected indices is guaranteed to be in ascending order
				int[] selectedIndices = trackSectionList.getSelectedIndices();
				for ( int i = selectedIndices.length - 1; i >= 0; i-- ) {
					final boolean moved = trackSectionModel.moveItemDown( selectedIndices[i] );
					
					// When moving down, try moving the bottom-most item first. If it can't be moved,
					// nothing above it can be moved either.
					if ( !moved )
						return;
				}
				
				for ( int i = 0; i < selectedIndices.length; i++ ) {
					selectedIndices[i]++;
				}
				trackSectionList.setSelectedIndices( selectedIndices );
				trackSectionList.repaint();
			}
		});
		this.add(moveDownButton, "growx");
		
		final JPanel thisPanel = this;
		moveToTrackButton = new JButton("Move to Track...");
		moveToTrackButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				final String destTrack = selectTrack();
				int destTrackIndex = -1;
				int[] selectedIndices = trackSectionList.getSelectedIndices();
				int[] offsets = new int[selectedIndices.length];
				for (int i = 0; i < selectedIndices.length; i++) {
					destTrackIndex = findTrack(destTrack);
					if (destTrackIndex == -1)
						return;

					// adjust indices if element moved downward in list
					offsets[i] = trackSectionModel.moveToTrack(selectedIndices[i], destTrackIndex);
					if (destTrackIndex > selectedIndices[i]) {
						offsets[i]--;
						for (int j = i + 1; j < selectedIndices.length; j++)
							selectedIndices[j]--;
					}
				}
				for (int k = 0; k < offsets.length; k++) { offsets[k] += destTrackIndex; }
				trackSectionList.setSelectedIndices(offsets);
				trackSectionList.repaint();
			}
		});
		this.add(moveToTrackButton, "growx");

		deleteButton = new JButton("Delete");
		deleteButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				final int[] selectedIndices = trackSectionList.getSelectedIndices();
				
				for ( int i = 0; i < selectedIndices.length; i++ ) {
					trackSectionModel.deleteItem( selectedIndices[i] );
					
					// adjust subsequent indices to account for the just-deleted item
					for ( int j = i + 1; j < selectedIndices.length; j++ )
						selectedIndices[j]--;
				}
				trackSectionList.repaint();
			}
		});
		this.add(deleteButton, "growx");
		
		renameButton = new JButton("Rename Track");
		renameButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doRenameTrack();
			}
		});
		this.add(renameButton, "growx");
		
		newButton = new JButton("New Track");
		newButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				trackSectionModel.newTrack();
				trackSectionList.repaint();
			}
		});
		this.add(newButton, "growx");
	}
}

class ImagePropertiesPane extends JPanel implements TableModelListener {
	
	ImagePropertyTable imageTable;
	BatchInputPanel batchPanel;
	TrackSectionListModel trackSectionModel;

	// Stores references only to newly-added sections in the trackSectionModel - simplifies
	// saving data from imageTable back to trackSectionModel.
	Vector<TrackSectionListElement> newSections;
	
	public ImagePropertiesPane( TrackSectionListModel trackSectionModel )
	{
		super( new MigLayout( "wrap 1, fillx" ));
		setupUI();
		
		newSections = new Vector<TrackSectionListElement>();
		this.trackSectionModel = trackSectionModel;
	}
		
	// load properties into table
	private void loadSectionProperties()
	{
		newSections.clear();
		imageTable.clearTable();

		for ( Vector<TrackSectionListElement> track : trackSectionModel.getTrackSectionVector() )
		{
			for ( int secIndex = 1; secIndex < track.size(); secIndex++ )
			{
				TrackSectionListElement section = track.elementAt( secIndex );
				if ( section.isNew() )
				{
					ImagePropertyTable.ImageProperties props = section.getImageProperties();
					imageTable.addImageAndProperties( section.getName(), props.orientation, props.length, props.dpix, props.dpiy, props.depth );
					newSections.add( section );
				}
			}
		}
	}
	
	// save table's values to section properties
	public void saveSectionProperties()
	{
		for ( int i = 0; i < imageTable.getRowCount(); i++ ) {
			TrackSectionListElement section = newSections.elementAt( i );
			
			section.getImageProperties().orientation = (String) imageTable.model.getValueAt(i, 1);
			section.getImageProperties().length = (Float) imageTable.model.getValueAt(i, 2);
			section.getImageProperties().dpix = (Float) imageTable.model.getValueAt(i, 3);
			section.getImageProperties().dpiy = (Float) imageTable.model.getValueAt(i, 4);
			section.getImageProperties().depth = (Float) imageTable.model.getValueAt(i, 5);
		}
	}

	private void setupUI()
	{
		imageTable = new ImagePropertyTable();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView( imageTable );
		this.add( scrollPane, "height 200:400:, growx");
		
		batchPanel = new BatchInputPanel( imageTable );
		this.add( batchPanel, "growx" );
		
		imageTable.updateUI();
		imageTable.model.addTableModelListener( this );
	}
	
	public void tableChanged( TableModelEvent e )
	{
		if ( e.getType() == TableModelEvent.UPDATE )
		{
			saveSectionProperties();
			updateSectionProperties(); // rebuild table based on new DPI values
		}
	}
	
	// Calculate depth and length for new sections based on their DPI and orientation values
	// and the depth/length of existing sections in each track.
	public void updateSectionProperties()
	{
		for ( Vector<TrackSectionListElement> track : trackSectionModel.getTrackSectionVector() )
		{
			float curDepth = 0.0f;
			for ( int secIndex = 1; secIndex < track.size(); secIndex++ )
			{
				TrackSectionListElement section = track.elementAt( secIndex );
				if ( !section.isNew() ) {
					curDepth = section.getImageProperties().depth + section.getImageProperties().length;
				} else {
					// new section, default length and depth - DPI X/Y and orientation should
					// be valid values passed in from SectionListPane DPI/orientation fields
					section.getImageProperties().depth = curDepth;
					section.getImageProperties().length = 1.5f; // meters
					
					// attempt to determine section's actual length
					File imageFile = section.getImageFile();
					if ( imageFile != null ) {
						final boolean isVertical = section.getImageProperties().orientation.equals("Vertical");
						final float depthDPI = isVertical ? section.getImageProperties().dpiy :
							section.getImageProperties().dpix;
						final int lengthInPix = SceneGraph.getImageDepthPix( imageFile.toString(), isVertical );
						section.getImageProperties().length = (( lengthInPix / depthDPI ) * 2.54f ) / 100.0f;
					} else {
						System.out.println("New section has null imageFile");
					}
					
					curDepth += section.getImageProperties().length;
					
					// if necessary, push subsequent pre-existing sections deeper to create space
					// TODO: only push if there isn't sufficient space for the new core to be
					// added without overlapping.
					boolean firstSubSec = true;
					float depthOffset = 0.0f;
					for ( int subSecIndex = secIndex + 1; subSecIndex < track.size(); subSecIndex++ ) {
						TrackSectionListElement subSection = track.elementAt( subSecIndex );
						if ( !subSection.isNew() )
						{
							if ( firstSubSec )
							{
								depthOffset = curDepth - subSection.getImageProperties().depth;
								subSection.getImageProperties().depth = curDepth;
								firstSubSec = false;
							}
							else
							{
								subSection.getImageProperties().depth += depthOffset;
							}
						}
					}
				}
			}
		}
		
		loadSectionProperties();
	}
}


class TrackSectionListModel extends AbstractListModel
{
	// Each Vector<TrackSectionListElement> represents a track and the sections it includes.
	// The track is always the 0th element of the vector, followed by sections in order.
	Vector<Vector<TrackSectionListElement>> tsVec;
	int newTrackCount = 1;
	
	TrackSectionListModel(Vector<Vector<TrackSectionListElement>> tsVec) {
		this.tsVec = tsVec;
	}
	
	public Vector<Vector<TrackSectionListElement>> getTrackSectionVector()
	{
		return tsVec;
	}
	
	public void renameTrack(final int index, final String newTrackName)
	{
		Point p = getEltIndex( index );
		if ( p != null && p.y == 0 )
			tsVec.elementAt( p.x ).elementAt( p.y ).setName( newTrackName );
	}
	
	public void newTrack()
	{
		final int origSize = this.getSize();
		Vector<TrackSectionListElement> newTrack = new Vector<TrackSectionListElement>();
		final String name = "New Track" + newTrackCount++;
		newTrack.add( new TrackSectionListElement( name, true, true ));
		tsVec.add( newTrack );

		fireIntervalAdded(this, origSize, origSize);
	}
	
	public void deleteItem(final int index)
	{
		Point p = getEltIndex( index );
		if ( p != null )
		{
			final TrackSectionListElement curElt = tsVec.elementAt( p.x ).elementAt( p.y );
			
			tsVec.elementAt( p.x ).remove( p.y );

			// if deleted element is a track, remove track vector
			if ( curElt.isTrack() && tsVec.elementAt( p.x ).isEmpty() )
				tsVec.remove( p.x );
		}
		
		fireIntervalRemoved( this, index, index );
	}
	
	// Convert JList's 1-D index to a 2-D vector index/section index pair: returned
	// Point.x indicates vector index, Point.y indicates element (section) in that vector
	private Point getEltIndex(final int index)
	{
		Point result = null;
		int vecIndex = 0, curIndex = 0;
		for ( Vector<TrackSectionListElement> v : tsVec )
		{
			int lowIndex = curIndex, hiIndex = curIndex + v.size() - 1;
			if ( index >= lowIndex && index <= hiIndex )
			{
				result = new Point( vecIndex, index - lowIndex );
				break;
			}
			
			curIndex += v.size();
			vecIndex++;
		}

		return result;
	}
	
	public boolean moveItemUp(final int index)
	{
		boolean moved = false;
		
		// figure out what index maps to
		Point p = getEltIndex( index );
		if ( p.y > 1 )
		{
			// swap within vector
			TrackSectionListElement elt = tsVec.elementAt( p.x ).remove( p.y );
			tsVec.elementAt( p.x ).insertElementAt( elt, p.y - 1 );
			moved = true;
		}
		else
		{
			// append to previous vector
			if ( p.x > 0 )
			{
				TrackSectionListElement elt = tsVec.elementAt( p.x ).remove( p.y );
				tsVec.elementAt( p.x - 1 ).add( elt );
				moved = true;
			}
		}
		
		if ( moved )
			fireContentsChanged( this, index - 1, index );

		return moved;
	}
	
	public boolean moveItemDown(final int index)
	{
		boolean moved = false;
		
		// figure out what index maps to
		Point p = getEltIndex( index );
		if ( p.y == tsVec.elementAt( p.x ).size() - 1 )
		{
			// move to top of next vector if possible
			if ( p.x + 1 < tsVec.size() )
			{
				TrackSectionListElement elt = tsVec.elementAt( p.x ).remove( p.y );
				tsVec.elementAt( p.x + 1 ).insertElementAt( elt, 1 );
				moved = true;
			}
		}
		else
		{
			// swap within vector
			TrackSectionListElement elt = tsVec.elementAt( p.x ).remove( p.y );
			tsVec.elementAt( p.x ).insertElementAt( elt, p.y + 1 );
			moved = true;
		}
		
		if ( moved )
			fireContentsChanged( this, index, index + 1 );

		return moved;
	}
	
	// return index in new track (for updating selection state)
	public int moveToTrack(final int index, final int trackIndex)
	{
		final Point p = getEltIndex(index);
		final Point tp = getEltIndex(trackIndex);
		TrackSectionListElement elt = tsVec.elementAt( p.x ).remove( p.y );
		tsVec.elementAt(tp.x).add(elt);
		
		return tsVec.elementAt(tp.x).size() - 1;
	}
	
	public int getNewSectionCount()
	{
		int count = 0;
		for ( Vector<TrackSectionListElement> trackVec : tsVec ) {
			for ( TrackSectionListElement tsle : trackVec ) {
				if ( tsle.isNewSection() )
					count++;
			}
		}
		return count;
	}

	// ListModel methods
	public Object getElementAt(int index)
	{ 
		Object element = null;
		Point p = getEltIndex( index );
		if ( p != null )
			element = tsVec.elementAt( p.x ).elementAt( p.y );

		return element;
	}
	
	public int getSize()
	{ 
		int size = 0;
		for ( Vector<TrackSectionListElement> v : tsVec )
			size += v.size();
		return size;
	}
}

class TrackSectionListCellRenderer extends DefaultListCellRenderer {
	public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected,
			final boolean cellHasFocus)
	{
		JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		TrackSectionListElement tsle = (TrackSectionListElement)value;
		if ( tsle.isNew() )
		{
			// Indicate newly-added tracks and sections with an icon
			label.setIcon(new ImageIcon("resources/icons/newCircle.gif"));
		}
		
		if ( tsle.isTrack() )
		{
			// Wrap tracks in text, and add distinct background color so they're more easily
			// distinguishable from sections.
			final boolean isUnrecognizedTrack = tsle.getName().equals( CRLoadImageWizard.UNRECOGNIZED_SECTIONS_TRACK );
			
			final String trackName = isUnrecognizedTrack ? tsle.getName() : ( "[Track: " + tsle.getName() + "]" );
			label.setText( trackName );

			if ( !isSelected )
			{
				// Red for Unrecognized Sections "track", beige for regular tracks
				Color bgColor = isUnrecognizedTrack ? new Color( 255, 102, 102 ) : new Color( 245, 245, 220 );  
				label.setBackground( bgColor );	
			}
		}
		
		return label;
	}
}
