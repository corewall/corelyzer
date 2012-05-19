package corelyzer.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

import net.miginfocom.swing.MigLayout;

import corelyzer.data.*;
import corelyzer.data.coregraph.*;
import corelyzer.graphics.SceneGraph;
import corelyzer.util.FileUtility;

// This dialog attempts to simplify the section image loading process. It allows multiple
// tracks to be created and populated in a single transaction. It makes best guesses as to
// where selected image files should be added to existing tracks (if possible) based on their
// names. Users can then reorder these images as they wish before loading occurs.

public class CRAutoLoadImageDialog extends JDialog implements ListSelectionListener {
	public static void main(final String[] args) {
		CRAutoLoadImageDialog dialog = new CRAutoLoadImageDialog(null, null);
		dialog.pack();
		dialog.setVisible(true);
		
		System.exit(0);
	}

	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JButton renameButton;
	private JButton deleteButton;
	private JButton moveUpButton, moveDownButton;
	private JButton newButton;
	private JButton imagePropsButton;
	private JScrollPane tslScrollPane;
	private JList trackSectionList;
	private TrackSectionListModel trackSectionModel;
	private Vector<File> newFiles;
	
	private static String UNRECOGNIZED_SECTIONS_TRACK = "Unrecognized Sections";

	public CRAutoLoadImageDialog(final Frame owner, final Vector<File> newFiles) {
		super(owner);
		this.newFiles = newFiles;
		
		setupUI();

		loadTrackData();
	}

	public void valueChanged(ListSelectionEvent e)
	{
		// This is called twice when the selection changes, first with getValueIsAdjusting()
		// returning true, second time false. No idea why, but there's no need to do things twice.
		if ( !e.getValueIsAdjusting() )
		{
			// assume single index for now
			//final int curSelIndex = trackSectionList.getSelectedIndex();
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
				if ( tsle.isTrack() && tsle.isNew() )
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

	private void loadTrackData()
	{
		if ( newFiles == null )
			return;
		
		Vector<Vector<TrackSectionListElement>> tsVec = new Vector<Vector<TrackSectionListElement>>();
		Vector<String> apparentTrack = new Vector<String>(); // one per vector (each corresponds to a track) in tsVec
		Vector<String> trackNameVec = new Vector<String>();
		Vector<String> sectionNameVec = new Vector<String>();
		
		// build vector of vectors of sections, one vector per track
		CoreGraph cg = CoreGraph.getInstance();
		for ( Session s : cg.getSessions() )
		{
			for ( TrackSceneNode tsn : s.getTrackSceneNodes() )
			{
				Vector<TrackSectionListElement> trackVec = new Vector<TrackSectionListElement>();
				boolean firstSection = true;
				trackNameVec.add( tsn.getName() );
				
				// add entry for empty track to keep apparentTrack indices aligned with tracks
				if ( tsn.getNumCores() == 0 )
					apparentTrack.add( tsn.getName() );
				
				for ( CoreSection cs : tsn.getCoreSections() )
				{
					if ( firstSection ) {
						// make best guess of track name based on name of first section in each
						// track: thus the Nth element of apparentTrack corresponds to the
						// Nth element (a track) in tsVec.
						final String fullTrackID = FileUtility.parseFullTrackID( cs.getName() );
						if ( fullTrackID == null ) {
							// couldn't parse track out of filename
							apparentTrack.add( null );
						} else {
							apparentTrack.add( fullTrackID );
						}
						firstSection = false;
					}
					trackVec.add( new TrackSectionListElement( cs.getName(), false ));
					sectionNameVec.add( cs.getName() );
				}
				
				tsVec.add( trackVec );
			}
		}
		
		// If any loaded files are duplicates of existing sections, remove them
		Vector<File> cleanedNewFiles = (Vector<File>)newFiles.clone();
		for ( File f : newFiles )
		{
			final String strippedFilename = FileUtility.stripExtension( f.getName() );
			if ( sectionNameVec.indexOf( strippedFilename ) != -1 ) {
				System.out.println( "ignoring apparent duplicate section " + strippedFilename );
				cleanedNewFiles.remove( f );
			}
		}
		
		// For each new file, attempt to find a matching track based on SIP: add if found, else, create new vec.
		// Sections whose names can't be parsed properly are added to an "unrecognized" track so the user can
		// position them manually if desired.
		Vector<TrackSectionListElement> unrecognizedSectionsVec = new Vector<TrackSectionListElement>();
		unrecognizedSectionsVec.add( new TrackSectionListElement( UNRECOGNIZED_SECTIONS_TRACK, true, true ));
		for ( File f : cleanedNewFiles )
		{
			final String strippedFilename = FileUtility.stripExtension( f.getName() );
			final String fullTrackID = FileUtility.parseFullTrackID( strippedFilename ); 
			if ( fullTrackID == null )
			{
				unrecognizedSectionsVec.add( new TrackSectionListElement( strippedFilename, true ));
				continue;
			}
			
			int matchingTrackIndex = -1;
			if (( matchingTrackIndex = apparentTrack.indexOf( fullTrackID )) != -1 )
			{
				tsVec.get( matchingTrackIndex ).add( new TrackSectionListElement( strippedFilename, true ));
				Collections.sort( tsVec.get( matchingTrackIndex ), new AlphanumComparator.TSLEAlphanumComparator() );
			}
			else
			{
				Vector<TrackSectionListElement> newTrack = new Vector<TrackSectionListElement>();
				newTrack.add( new TrackSectionListElement( strippedFilename, true ));
				tsVec.add( newTrack );
				apparentTrack.add( fullTrackID );
			}
		}
		
		// now that they won't disrupt sorting, add track elements
		int oldTrackIndex = 0, newTrackIndex = 1;
		for ( Vector<TrackSectionListElement> trackVec : tsVec )
		{
			if ( oldTrackIndex < trackNameVec.size() ) {
				trackVec.insertElementAt( new TrackSectionListElement( trackNameVec.elementAt( oldTrackIndex ), false, true ), 0 );
				oldTrackIndex++;
			} else {
				String newTrackName = FileUtility.parseFullTrackID( trackVec.elementAt( 0 ).getName() );
				if ( newTrackName == null )
					newTrackName = "New Track" + newTrackIndex++;
				trackVec.insertElementAt( new TrackSectionListElement( newTrackName, true, true ), 0 );
			}
		}
		
		// finally, add unrecognized sections vector if it contains any sections (remember that the first element
		// represents the track).
		if ( unrecognizedSectionsVec.size() > 1 )
			tsVec.add( unrecognizedSectionsVec );
		
		trackSectionModel = new TrackSectionListModel( tsVec );
		trackSectionList.setModel( trackSectionModel );
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
	
	private void enableSectionButtons( final boolean enable ) {
		moveUpButton.setEnabled( enable );
		moveDownButton.setEnabled( enable );
	}
	
	private void enableTrackButtons( final boolean enable ) { renameButton.setEnabled( enable ); }
	private void enableDeleteButton( final boolean enable ) { deleteButton.setEnabled( enable ); }
	
	private void setupUI() {
		contentPane = new JPanel(new MigLayout("wrap 2, fillx"));
		
		JLabel iconExplainLabel = new JLabel("Indicates loaded section or newly-created track");
		iconExplainLabel.setIcon( new ImageIcon( "resources/icons/newCircle.gif" ));
		contentPane.add( iconExplainLabel, "span 2, align left" );

		trackSectionList = new JList();
		trackSectionList.setCellRenderer(new TrackSectionListCellRenderer());
		trackSectionList.addListSelectionListener( this );
		
		tslScrollPane = new JScrollPane();
		tslScrollPane.setViewportView(trackSectionList);
		contentPane.add(tslScrollPane, "width 250::, height 400::, growx");
		
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
		contentPane.add(moveUpButton, "split 5, flowy");
		
		moveDownButton = new JButton("Move Down");
		moveDownButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// the array of selected indices is guaranteed to be in ascending order
				int[] selectedIndices = trackSectionList.getSelectedIndices();
				for ( int i = selectedIndices.length - 1; i >= 0; i-- ) {
					final boolean moved = trackSectionModel.moveItemDown( selectedIndices[i] );
					
					// When moving down, we try move the bottommost item first. If it can't be moved,
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
		contentPane.add(moveDownButton);

		deleteButton = new JButton("Delete");
		deleteButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				final int[] selectedIndices = trackSectionList.getSelectedIndices();
				
				for ( int i = 0; i < selectedIndices.length; i++ ) {
					// When we delete the first element (selectedIndices is guarnateed to be in
					// ascending order), the remaining indices need to be decremented, but it's
					// simpler to just delete the same index for each selected list element!
					trackSectionModel.deleteItem( selectedIndices[0] );
				}
				trackSectionList.repaint();
			}
		});
		contentPane.add(deleteButton);
		
		renameButton = new JButton("Rename Track");
		renameButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doRenameTrack();
			}
		});
		contentPane.add(renameButton);
		
		newButton = new JButton("New Track");
		newButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				trackSectionModel.newTrack();
				trackSectionList.repaint();
			}
		});
		contentPane.add(newButton);
				
		imagePropsButton = new JButton("Set Image Properties...");
		imagePropsButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onSetImageProperties();
			}
		});
		contentPane.add(imagePropsButton, "gapy 10, span 2, split 3, align left");
		
		buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				onCancel();
			}
		});
		contentPane.add(buttonCancel, "gapleft 30");
		
		buttonOK = new JButton("OK");
		buttonOK.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Runnable loading = new Runnable() {
					public void run() {
						onConfirmLoad();
					}
				};
				new Thread(loading).start();
				dispose();
			}
		});
		contentPane.add(buttonOK);
		
		setTitle("Auto Load Images");
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);
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
	
	private void onSetImageProperties()
	{
		// create vector of new sections and hand off to image properties dialog
		Vector<TrackSectionListElement> newSections = new Vector<TrackSectionListElement>();
		for ( Vector<TrackSectionListElement> track : trackSectionModel.getTrackSectionVector() ) {
			for ( TrackSectionListElement section : track ) {
				if ( !section.isTrack() && section.isNew() ) {
					newSections.add( section );
				}
			}
		}
		
		CRAutoLoadImagePropDialog propsDialog = new CRAutoLoadImagePropDialog( this, newSections );
		propsDialog.setVisible(true);
	}
	
	private void onCancel() { dispose(); }
	
	private void onConfirmLoad()
	{
		int curProgressValue = 1;
		JProgressBar progress = CorelyzerApp.getApp().getProgressUI();
		progress.setString("Loading Images");
		progress.setMaximum( trackSectionModel.getNewSectionCount() + 1 );
		progress.setValue( curProgressValue );
		
		final Vector<Vector<TrackSectionListElement>> tsVec = trackSectionModel.getTrackSectionVector();
		
		Session session = CoreGraph.getInstance().getCurrentSession();
		
		for ( int tsVecIndex = 0; tsVecIndex < tsVec.size(); tsVecIndex++ )
		{
			Vector<TrackSectionListElement> trackVec = tsVec.elementAt( tsVecIndex );
			TrackSceneNode curTrack = null;
			final TrackSectionListElement trackElt = trackVec.elementAt( 0 ); 
			if ( trackElt.isTrack() && trackElt.isNew() )
			{
				// never load sections left in Unrecognized Sections track
				if ( trackElt.getName().equals( UNRECOGNIZED_SECTIONS_TRACK ))
					continue;
				
				curTrack = createTrack( session, trackElt.getName() );
			}
			else
				curTrack = session.getTrackSceneNodeWithIndex( tsVecIndex );
			if ( curTrack != null )
			{
				for ( int eltIndex = 1; eltIndex < trackVec.size(); eltIndex++ )
				{
					final TrackSectionListElement sectionElt = trackVec.elementAt( eltIndex );
					if ( sectionElt.isNew() )
					{
						// now map section name in list back to original file, yes this is gross FIXME
						final String sectionName = sectionElt.getName();
						File originalFile = null;
						for ( File curFile : newFiles ) {
							if ( FileUtility.stripExtension( curFile.getName() ).equals( sectionName )) {
								originalFile = curFile;
								break;
							}
						}
						
						if ( originalFile != null ) {
							// add section to track
							progress.setString("Loading " + originalFile.getName() );
							final ImagePropertyTable.ImageProperties imageProps = sectionElt.getImageProperties();
							final int insertIndex = eltIndex - 1;
							final int sectionId = FileUtility.loadImageFile( originalFile, null, sectionName, curTrack, insertIndex );
							if ( sectionId != -1 )
							{
								FileUtility.setSectionImageProperties(curTrack, sectionId, imageProps.length, imageProps.depth,
										imageProps.dpix, imageProps.dpiy, imageProps.orientation);
							}
							progress.setValue(++curProgressValue);
						} else {
							System.out.println("Couldn't map section " + sectionName + " back to loaded file");
						}
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
	
	public int getNewSectionCount()
	{
		int count = 0;
		for ( Vector<TrackSectionListElement> trackVec : tsVec ) {
			for ( TrackSectionListElement tsle : trackVec ) {
				if ( !tsle.isTrack() && tsle.isNew() )
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

// Indicate newly-added tracks and sections with an icon. Wrap tracks in text
// so they're easily distinguished from sections.
class TrackSectionListCellRenderer extends DefaultListCellRenderer {
	public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected,
			final boolean cellHasFocus)
	{
		JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		TrackSectionListElement tsle = (TrackSectionListElement)value;
		if ( tsle.isNew() )
		{
			label.setIcon(new ImageIcon("resources/icons/newCircle.gif"));
		}
		if ( tsle.isTrack() )
		{
			final String trackName = "[Track: " + tsle.getName() + "]";
			label.setText( trackName );
			if ( !isSelected )
				label.setBackground( new Color( 245, 245, 220 )); // Beige
		}
		
		return label;
	}
}
