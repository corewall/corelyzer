/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
 * Electronic Visualization Laboratory, University of Illinois at Chicago
 *
 * This software is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along
 * with this software; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Questions or comments about CoreWall should be directed to 
 * cavern@evl.uic.edu
 *
 *****************************************************************************/

package corelyzer.graphics;

/**
 * The Java interface to native scene graph.
 * 
 */

import java.util.concurrent.Semaphore;

public class SceneGraph {
	public static boolean LANDSCAPE = false;
	public static boolean PORTRAIT = true;

	public static boolean HORIZONTAL = true;
	public static boolean VERTICAL = false;

	static {
		// brg 10/15/2020: uncomment to launch from VSCode on Windows
		// System.loadLibrary("pthreadVC2");
		// System.loadLibrary("zlib1");
		// System.loadLibrary("libpng13");
		// System.loadLibrary("tiff");

		System.loadLibrary("scenegraph");
	}

	// private static ReentrantLock lock = new ReentrantLock();
	private static Semaphore scenelock = new Semaphore(1, true);
	private static Semaphore texturelock = new Semaphore(1, true);

	// james addition
	// public variables for multiple glcanvas: james addition
	public static int focusedTrack = -1;

	public static int focusedTrackSection = -1;

	public static int focusedMarker = -1;

	/**
	 * @return id of the free draw rectangle selected.
	 */
	public native static int accessPickedFreeDraw();

	/**
	 * @return id of graph within the picked track, section combination at last
	 *         performPick call
	 */
	public native static int accessPickedGraph();

	/**
	 * @return id of marker selected. If accessPickedGraph return -1 then it is
	 *         a marker of the picked section, otherwise it is a marker of the
	 *         last picked graph.
	 */
	public native static int accessPickedMarker();

	/**
	 * @return id of section within the picked track at last performPick call
	 */
	public native static int accessPickedSection();

	/**
	 * @return id of track found at last performPick call
	 */
	public native static int accessPickedTrack();

	public native static void addClastPoint1(float x, float y);

	public native static void addClastPoint2(float x, float y);

	/**
	 * Add a dataset file to Corelyzer runtime.
	 * 
	 * @param name
	 *            File path of the input dataset file
	 * @return Dataset ID
	 */
	public native static int addDataset(String name);

	/**
	 * Associate a graph to a core section. The graph is an association between
	 * (Track, Section) and (Dataset, table, field).
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param dataset
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param field
	 *            Field ID
	 * @return corelyzer.data.Graph ID
	 */
	public native static int addLineGraphToSection(int track, int section, int dataset, int table, int field);

	public native static int addMeasurePoint(float x, float y);

	/**
	 * Adds a new field to a given table. This is incase new data exists for
	 * whatever reason and a new column should be made.
	 */
	public native static boolean addNewFieldToTable(int set, int table, String label);

	// ----- Library system methods

	/**
	 * Associate section image to a track
	 */
	public native static int addSectionImageToTrack(int track, int section, int image);

	/**
	 * Create section to a track
	 */
	public native static int addSectionToTrack(int track, int section);

	/**
	 * Add a data table to Corelyzer runtime
	 * 
	 * @param set
	 *            Dataset ID that this table is added to
	 * @param name
	 *            Name of the table
	 * @return corelyzer.data.Table ID
	 */
	public native static int addTable(int set, String name);

	/**
	 * @param name
	 *            the name to give to the track
	 * @return if it fails -1, otherwise a non-negative id
	 */
	public native static int addTrack(String session, String name);

	// ----- Scene methods

	/**
	 * Bring a core section to front.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 */
	public native static void bringSectionToFront(int track, int section);

	/**
     */
	public native static void bringTrackToFront(int track);

	/**
	 * Performs deallocation of public native side resources
	 */
	public native static void closeDown();

	/**
	 * Create a core section marker associated with a position in a core section
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Section ID
	 * @param type
	 *            Type of the marker
	 * @param xpos
	 *            X position within the core section
	 * @return Marker ID
	 */
	public native static int createCoreSectionMarker(int track, int section, int group, int type, float xpos, float ypos);

	// ---- PLUGIN DRAWN RECTANGLE METHODS ----//
	/**
	 * @param x
	 *            Horizontal positon, equivalent to meters depth
	 * @param y
	 *            Vertical position, unit in meters
	 * @param w
	 *            Horiztonal span, unit in meters
	 * @param h
	 *            Vertical span, unit in meters
	 */
	public native static int createFreeDrawRectangle(int pluginid, float x, float y, float w, float h);

	/**
	 * @param y
	 *            Vertical position with respect to image, unit in pixels
	 * @param h
	 *            Vertical span, unit in pixels
	 */
	public native static int createFreeDrawRectangleForSection(int pluginid, int track, int section, float y, float h);

	/**
	 * @param x
	 *            Horizontal positon with respect to track, equivalent to meters
	 *            depth
	 * @param y
	 *            Vertical position with respect to track, unit in meters
	 * @param w
	 *            Horiztonal span, unit in meters
	 * @param h
	 *            Vertical span, unit in meters
	 */
	public native static int createFreeDrawRectangleForTrack(int pluginid, int track, float x, float y, float w, float h);

	// section tie f'ns
	public native static int[] getSectionTieIds();
	public native static void createSectionTie(float x, float y, int trackId, int sectionId);
	public native static int finishSectionTie(float x, float y, int trackId, int sectionId);
	public native static void setSectionTieSourceDescription(int tieId, String desc);
	public native static String getSectionTieSourceDescription(int tieId);
	public native static void setSectionTieDestinationDescription(int tieId, String desc);
	public native static String getSectionTieDestinationDescription(int tieId);
	public native static boolean getSectionTieShow(int tieId);
	public native static void setSectionTieShow(int tieId, boolean show);

	// ----- Canvas methods

	public native static void deleteDataset(int datasetId);

	/**
	 * @param track
	 *            the id of the track to delete
	 */
	public native static void deleteTrack(int track);

	/**
	 * Destroy the current set of canvas objects. This is meant to be used in
	 * case the display layout is incorrect.
	 */
	public native static void destroyCanvases();

	public native static void destroyFreeDrawRectangle(int freeDrawRect);

	/**
	 * Access method. Disable the vertical graph aligning dash-line.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 */
	public native static void disableVerticalLineFromGraph(int graphId);

	public native static int duplicateSection(int trackId, int sectionId);

	public native static int duplicateSectionToAnotherTrack(int trackId, int sectionId, int newTrackId);

	/**
     *
     */
	public native static void enableCanvasCrossCoreScale(int canvas, boolean flg);

	/**
	 * Flag a canvas to draw the grid quide line or not.
	 * 
	 * @param flg
	 *            true/false
	 */
	public native static void enableCanvasGrid(boolean flg);

	/**
	 * Access method. Enable the vertical graph aligning dash-line.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 */
	public native static void enableVerticalLineFromGraph(int graphId);

	/**
	 * Return first found graph (displayed or not) with matching dataset and field
	 * 
	 * @param datasetId
	 *            Dataset ID
	 * @param fieldId
	 *            Field ID
	 * @return ID of matching graph if found, otherwise -1
	 */
	public native static int findGraphByField( int datasetId, int fieldId );
	
	/**
	 * Create a Public Native Side Canvas with initial parameters
	 * 
	 * @param x
	 *            x-position in the desktop area
	 * @param y
	 *            y-position in the desktop area
	 * @param width
	 *            width of canvas
	 * @param height
	 *            height of canvas
	 * @param dpix
	 *            horizontal dpi of the canvas
	 * @param dpiy
	 *            vertical dpi of the canvas
	 * @return the id of the canvas
	 */
	public native static int genCanvas(float x, float y, int width, int height, float dpix, float dpiy);

	public native static int genTextureBlocks(String name);

	public native static boolean genTextureBlocksToDirectory(String inputFileName, String outputDirectoryName);

	public native static float[] getBackgroundColor();

	/**
	 * @param canvas
	 *            id of the canvas
	 * @return the horizontal dpi of the canvas object
	 */
	public native static float getCanvasDPIX(int canvas);

	/**
	 * @param canvas
	 *            id of the canvas
	 * @return the vertical dpi of the canvas object
	 */
	public native static float getCanvasDPIY(int canvas);

	/**
	 * @param canvas
	 *            id of the canvas
	 * @return the vertical area of coverage of the canvas in world space
	 */
	public native static float getCanvasHeight(int canvas);

	/**
	 * @param canvas
	 *            id of the canvas
	 * @return the x-position of the canvas in world space coordinates
	 */
	public native static float getCanvasPositionX(int canvas);

	/**
	 * @param canvas
	 *            id of the canvas
	 * @return the y-position of the canvas in world space coordinates
	 */
	public native static float getCanvasPositionY(int canvas);

	/**
	 * @param canvas
	 *            id of the canvas
	 * @return the horizontal area of coverage of the canvas in world space
	 */
	public native static float getCanvasWidth(int canvas);

	/**
	 * Get an annotation marker's group
	 */
	public native static int getCoreSectionMarkerGroup(int track, int section, int marker);

	/**
	 * Get marker's icon x position. (px)
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's icon X position
	 */
	public native static float getCoreSectionMarkerIconXPos(int track, int section, int marker);

	// ----- Track Methods

	/**
	 * Get marker's icon y position. (py)
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's icon Y position
	 */
	public native static float getCoreSectionMarkerIconYPos(int track, int section, int marker);

	/**
	 * Access method. Get the marker's local image resource filepath.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's local image resource filepath
	 */
	public native static String getCoreSectionMarkerLocal(int track, int section, int marker);

	public native static String getCoreSectionMarkerRelationText(int track, int section, int marker);

	public native static String getCoreSectionMarkerText(int track, int section, int marker);

	/**
	 * Get an annotation marker's type
	 */
	public native static int getCoreSectionMarkerType(int track, int section, int marker);

	/**
	 * Access method. Get the marker's image resouce URL.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's image resource URL
	 */
	public native static String getCoreSectionMarkerURL(int track, int section, int marker);

	/**
	 * Get marker's drawing vetex0
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's drawing vetex0
	 */
	public native static float getCoreSectionMarkerV0(int track, int section, int marker);

	/**
	 * Get marker's drawing vetex1
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's drawing vetex1
	 */
	public native static float getCoreSectionMarkerV1(int track, int section, int marker);

	/**
	 * Get marker's drawing vetex2
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's drawing vetex2
	 */
	public native static float getCoreSectionMarkerV2(int track, int section, int marker);

	/**
	 * Get marker's drawing vetex3
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's drawing vetex3
	 */
	public native static float getCoreSectionMarkerV3(int track, int section, int marker);

	/**
	 * Get an annotation marker's visibility
	 */
	public native static boolean getCoreSectionMarkerVisibility(int track, int section, int marker);

	/**
	 * Get marker's x position. (depthX)
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's X position
	 */
	public native static float getCoreSectionMarkerXPos(int track, int section, int marker);

	/**
	 * Get marker's y position. (depthY)
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @return Marker's Y position
	 */
	public native static float getCoreSectionMarkerYPos(int track, int section, int marker);

	/**
	 * Access method. Get the name of a dataset in Corelyzer runtime
	 * 
	 * @param set
	 *            Dataset ID
	 * @return Name of the dataset
	 */
	public native static String getDatasetName(int set);

	/**
	 * Access method. Get the dataset ID where the graph's data is from.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 * @return Dataset ID
	 */
	public native static int getDatasetReference(int graphId);

	/**
	 * Access method. Get the URL of a dataset in Corelyzer runtime.
	 * 
	 * @param set
	 *            Dataset ID
	 */
	public native static String getDatasetURL(int set);

	public native static boolean getDebug();
	
	public native static void debugKey( int keyId );

	public native static boolean getDepthOrientation();

	/**
	 * Access method. Get the name of a field.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param field
	 *            Field ID
	 * @return Name of the field
	 */
	public native static String getFieldName(int set, int table, int field);

	/**
	 * Access method. Get the field ID where the graph's data is from.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 * @return Field ID
	 */
	public native static int getFieldReference(int graphId);

	// ----- Image Methods

	public native static int getFreeDrawPluginID(int freeDrawRect);

	/**
	 * Access method. Get the graph ID about a given data field.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Section ID
	 * @param dataset
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param field
	 *            Field ID
	 * @return corelyzer.data.Graph ID
	 */
	public native static int getGraphID(int track, int section, int dataset, int table, int field);

	/**
	 * Access method. Get the graph ID in a slot.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param slot
	 *            Slot ID
	 * @return corelyzer.data.Graph ID
	 */
	public native static int getGraphIDFromSectionSlot(int track, int section, int slot);

	/**
	 * Access method. Get the graph's viewing range maximum value.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 * @return Maximum value of the graph viewing range
	 */
	public native static float getGraphMax(int graphId);

	/**
	 * Access method. Get the graph's viewing range minimum value.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 * @return Minimum value of the graph viewing range
	 */
	public native static float getGraphMin(int graphId);

	public native static float getGraphOrigMax(int graphId);

	public native static float getGraphOrigMin(int graphId);

	public native static float getGraphExcludeMin(int graphId);

	public native static float getGraphExcludeMax(int graphId);
	
	public native static int getGraphExcludeStyle(int graphId);
	
	public native static boolean getGraphsCollapse();

	/**
	 * Access method. Get the slot ID hosting the graph.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 * @return Slot ID
	 */
	public native static int getGraphSlot(int graphId);

	/**
	 * Access method. Get specified core section image's X DPI(dot per inch)
	 * 
	 * @param image
	 *            Core section image ID
	 * @return Image's X DPI
	 */
	public native static float getImageDPIX(int image);

	/**
	 * Access method. Get specified core section image's Y DPI(dot per inch)
	 * 
	 * @param image
	 *            Core section image ID
	 * @return Image's Y DPI
	 */
	public native static float getImageDPIY(int image);

	// ----- Section Methods

	/**
	 * Access method. Get specified core section image's height
	 * 
	 * @param image
	 *            Core section image ID
	 * @return Image's height
	 */
	public native static int getImageHeight(int image);

	/**
	 * Access method. Get the image ID for a specified core section (Duplicate
	 * with `getSectionSourceImage'?)
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @return Image ID
	 */
	public native static int getImageIdForSection(int track, int section);

	/**
	 * Access method. Get specified core section image's name
	 * 
	 * @param image
	 *            Core section image ID
	 */
	public native static String getImageName(int image);

	/**
	 * Access method. Get specified core section image's URL
	 * 
	 * @param image
	 *            Core section image ID
	 * @return The URL of specified core section image
	 */
	public native static String getImageURL(int image);

	/**
	 * Access method. Get specified core section image's width
	 * 
	 * @param image
	 *            Core section image ID
	 * @return Image's width
	 */
	public native static int getImageWidth(int image);

	/**
	 * Access method. Get pixel count along image's depth axis without
	 * loading pixel data
	 * 
	 * @param image
	 *            Core section image ID
	 * @return Image's width
	 */
	public native static int getImageDepthPix(String imageFilename, boolean isVertical);
	
	/**
     */
	// public native static float getLineGraphColorComponent(int track,
	// int section,
	// int field,
	// int component);
	/**
	 * Access method. Get the graph color component.
	 * 
	 * @param gid
	 *            corelyzer.data.Graph ID
	 * @param component
	 *            Color Component index 0:red, 1:green, 2:blue
	 * @return Color component value, range from 0.0 to 1.0
	 */
	public native static float getLineGraphColorComponent(int gid, int component);

	/**
	 * Access method. Get the graph type
	 * 
	 * @param gid
	 *            corelyzer.data.Graph ID
	 * @return graph type: 0, 1, 2 - line, point, crosspoint
	 */
	public native static int getLineGraphType(int gid);

	/**
	 * Access method. Get number of tables in a dataset.
	 * 
	 * @param set
	 *            Dataset ID
	 * @return Number of tables in this dataset
	 */
	public native static int getNumberOfTables(int set);

	/**
	 * Access method. Get number of markers in a core section.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @return Number of markers in this core section
	 */
	public native static int getNumCoreSectionMarkers(int track, int section);

	/**
	 * Access method. Get current number of graphs in a core section.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 */
	public native static int getNumGraphsForSection(int track, int section);

	/**
	 * Access method. Get number of core section images in a given track ID.
	 * 
	 * @param track
	 *            track ID
	 */
	public native static int getNumSections(int track);

	public native static int getRenderMode();

	/**
	 * Get the x-position of the center of the current view into the scene
	 */
	public native static float getSceneCenterX();

	/**
	 * Get the y-position of the center of the current view into the scene
	 */
	public native static float getSceneCenterY();

	/**
	 * Access method. Get the coresection depth in cm
	 * 
	 * @param track
	 *            Track ID
	 * @param sec
	 *            Core section ID
	 * 
	 * @return float mbsf(meters below sea floor)
	 */
	public native static float getSectionDepth(int track, int sec);

	/**
	 * Access method. Get the X DPI of a section.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @return X DPI
	 */
	public native static float getSectionDPIX(int track, int section);

	/**
	 * Access method. Get the Y DPI of a section.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @return Y DPI
	 */
	public native static float getSectionDPIY(int track, int section);

	/**
	 * Access method. Get the coresection graph offset in cm
	 * 
	 * @param track
	 *            Track ID
	 * @param sec
	 *            Core section ID
	 */
	public native static float getSectionGraphOffset(int track, int sec);

	public native static float getSectionHeight(int track, int sec);

	public native static int getSectionIDByName(int trackId, String sectionName);

	public native static int getSectionIDFromURL(int trackId, String url);

	public native static float getSectionIntervalBottom(int track, int sec);

	public native static float getSectionIntervalTop(int track, int sec);

	/**
	 * Return core section dimension in (cm)
	 * 
	 * Length: how long the core section is. Same as 'width' Width: how long the
	 * core section is. In horizontal direction Height: Size in vertical
	 * direction
	 * 
	 * @param track
	 *            Track ID
	 * @param sec
	 *            Core section ID
	 * @return float physical size in cm
	 * */
	public native static float getSectionLength(int track, int sec);

	public native static String getSectionName(int track, int section);

	public native static boolean getSectionOrientation(int track, int section);

	public native static int getSectionParentSectionId(int trackId, int sectionId);

	public native static int getSectionParentTrackId(int trackId, int sectionId);

	/**
	 * Access method. Get the core section ID where the graph belongs to.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 * @return Core section ID
	 */
	public native static int getSectionReference(int graphId);

	/**
	 * Access method. Get the rotation angle value of specified core section.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @return Rotation value in degree
	 */
	public native static float getSectionRotation(int track, int section);

	/**
	 * Access method. Get the image ID that is associated to a core section.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @return Image ID
	 */
	public native static int getSectionSourceImage(int track, int section);

	public native static float getSectionWidth(int track, int sec);

	/**
	 * Access method. Get core section's X position.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @return Core section's X position
	 */
	public native static float getSectionXPos(int track, int section);

	/**
	 * Access method. Get core section's Y position.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @return Core section's Y position
	 */
	public native static float getSectionYPos(int track, int section);

	public native static boolean getShowOrigin();

	public native static boolean getShowSectionText();

	/**
	 * Access method. Get the data value in a cell.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param field
	 *            Field ID
	 * @param row
	 *            Row ID
	 * @return Cell data value located by input parameters
	 */
	public native static float getTableCell(int set, int table, int field, int row);

	/**
	 * Access method. Get cell's depth value in a table.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param field
	 *            Field ID
	 * @param row
	 *            Row ID
	 * @return Cell depth value
	 */
	public native static float getTableCellDepth(int set, int table, int field, int row);

	/**
	 * Access method. Get the scale value applied as the scaling to depth
	 * values.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @return Depth scaling value
	 */
	public native static float getTableDepthUnitScale(int set, int table);

	/**
	 * Access method. Get number of fields of a table.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @return Number of fields in a table
	 */
	public native static int getTableFields(int set, int table);

	/**
	 * Access method. Get the height(number of rows) of a table.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @return Number of rows in a table
	 */
	public native static int getTableHeight(int set, int table);

	// ----- Dataset Methods

	/**
	 * Access method. Get the name of a table.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @return Name of the table
	 */
	public native static String getTableName(int set, int table);

	/**
	 * Access method. Get the table ID where the graph's data is from.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 * @return corelyzer.data.Table ID
	 */
	public native static int getTableReference(int graphId);

	public native static String getTexBlockDirectory();

	/**
     */
	public native static int getTrackIDByName(String sessionName, String trackName);

	/**
	 * Access method. Get the name of specified track ID
	 * 
	 * @param track
	 *            track ID
	 * @return track name
	 */
	public native static String getTrackName(int track);

	/**
	 * Access method. Get the track ID where the graph belongs to.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 * @return Track ID
	 */
	public native static int getTrackReference(int graphId);

	public native static boolean getTrackShow(int track);

	/**
	 * Access method. Get track's X position
	 * 
	 * @param track
	 *            track ID
	 */
	public native static float getTrackXPos(int track);

	/**
	 * Access method. Get track's Y position
	 * 
	 * @param track
	 *            track ID
	 */
	public native static float getTrackYPos(int track);

	public native static boolean hasCrossHair();

	/**
	 * Highlight a section in a track
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section image ID
	 */
	public native static int highlightSection(int track, int section, boolean isOn);
	public native static int highlightSections(int track, int[] sections);

	public static boolean hitMarker(final int canvasId, final float x, final float y) {
		return hitMarker(canvasId, focusedTrack, focusedTrackSection, focusedMarker, x, y);
	}

	public native static boolean hitMarker(int canvas, int track, int section, int marker, float x, float y);

	public static void imageLock() {
		try {
			texturelock.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void imageUnlock() {
		try {
			texturelock.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public native static boolean isCoreSectionMarker(int track, int section, int marker);

	/**
	 * Check whether an annotation is duplicated.
	 */
	public native static boolean isDuplicateAnnotation(int trackId, int sectionId, float x, float y, float v0, float v1, float v2, float v3, String jUrlString,
			String jLocalString);

	/**
	 * Access method. Whether the graph being drawing in the scene graph.
	 * 
	 * @param gid
	 *            corelyzer.data.Graph ID
	 * @return true/false
	 */
	public native static boolean isLineGraphShown(int gid);

	public native static boolean isSectionMovable(int track, int section);
	public native static boolean isSectionGraphMovable(int track, int section);

	/**
	 * Access method. Determine if the specified cell is valid.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param field
	 *            Field ID
	 * @param row
	 *            Row ID
	 * @return true(valid)/false
	 */
	public native static boolean isTableCellValid(int set, int table, int field, int row);

	public native static boolean isTrackMovable(int track);

	/**
	 * Load the image file with input file path
	 * 
	 * @param name
	 *            File path of the image file
	 */
	public native static int loadImage(String name);

	public static void lock() {
		try {
			scenelock.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ----- corelyzer.data.Graph Methods

	public static void main(final String[] args) {
		SceneGraph.startUp();
		SceneGraph.closeDown();
	}

	/**
     */
	// public native static int removeLineGraphFromSection(int track,
	// int section,
	// int dataset,
	// int table,
	// int field);

	public static void manipulateMarker(final int canvasId, final float dx, final float dy) {
		manipulateMarker(canvasId, focusedTrack, focusedTrackSection, focusedMarker, dx, dy);

	}

	/**
     */
	// public native static void setLineGraphColor(int track, int section,
	// int field,
	// float r, float g, float b);

	public native static void manipulateMarker(int canvas, int track, int section, int marker, float dx, float dy);

	/**
	 * This will make the free draw area look the same size on the screen,
	 * independent of the current zoom/scale factor.
	 */
	public native static void markFreeDrawScaleIndependent(int freeDrawRect, boolean flag);

	/**
     */
	// public native static void setLineGraphRange(int track, int section,
	// int field, float min,
	// float max);

	/**
	 * Set flag that determines whether or not a section can be moved along depth axis
	 */
	public native static void setSectionMovable(int track, int section, boolean flag);

	/**
	 * Set flag that determines whether or not a section's graphs can be moved along depth axis
	 */
	public native static void setSectionGraphMovable(int track, int section, boolean flag);

	/**
     */
	// public native static void setLineGraphLabel(int track, int section,
	// int field, String label);

	/**
	 * Moves a free draw rectangle. If the rectangle is not attached to an image
	 * then x,y are assumed to be in meters. Otherwise x,y are in pixels
	 */
	public native static void moveFreeDrawRectangle(int freeDrawRect, float x, float y);

	/**
	 * Displace a core section image
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section image ID
	 * @param x
	 *            Displacement in X direction
	 * @param y
	 *            Displacement in Y direction
	 */
	public native static void moveSection(int track, int section, float x, float y);
	public native static void moveSections(int track, int[] sections, float x, float y);

	/**
	 * Displace a core section graph
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section image ID
	 * @param x
	 *            Displacement in X direction
	 * @param y
	 *            Displacement in Y direction
	 */
	public native static void moveSectionGraph(int track, int section, float x, float y);

	/**
     */
	// public native static boolean isLineGraphShown( int track, int section,
	// int dataset, int table,
	// int field);

	/**
	 * Displaces a given track a certain amount in world space
	 */
	public native static void moveTrack(int track, float x, float y);

	public native static void moveTrackAbsX(int track, float absX);

	public native static void moveTrackAbsY(int track, float absY);

	/**
	 * @return the number of canvas entries
	 */
	public native static int numCanvases();

	/**
	 * Access method. Get number of datasets in current Corelyzer runtime.
	 * 
	 * @return Current number of datasets
	 */
	public native static int numDatasets();

	/**
	 * Orient the scene in vertical fashion
	 * 
	 * @param flag
	 *            true/false
	 */
	public native static void orientSceneVertical(boolean flag);

	/**
	 * Makes the scene move a given values in world space
	 */
	public native static void panScene(float x, float y);

	/**
	 * Given World Space Coordinates, the selected Track, Section,
	 * corelyzer.data.Graph and Annotation Marker are determined. For the logic
	 * of a marker, you can determine that the marker is a core section marker
	 * if accessPickedGraph return -1.
	 * 
	 * @param canvas
	 *            the canvas id that is making the performPic call
	 * @param x
	 *            the worldspace x coordinate to pick at
	 * @param y
	 *            the worldspace y coordinate to pick at
	 */
	public native static void performPick(int canvas, float x, float y);

	/**
	 * Set current mouse position
	 */
	public native static void positionMouse(float x, float y);

	/**
	 * Forces the scene to position itself somewhere in world space
	 */
	public native static void positionScene(float x, float y);

	/**
	 * Move a core section image to a specific position
	 * 
	 * @param track
	 *            Track ID
	 * @param sec
	 *            Core section image ID
	 * @param x
	 *            Final position's X value
	 * @param y
	 *            Final position's Y value
	 */
	public native static void positionSection(int track, int sec, float x, float y);

	/**
	 * Push core section to the end
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 */
	public native static void pushSectionToEnd(int track, int section);

	public native static void removeCoreSectionMarker(int track, int section, int marker);

	/**
	 * Remove association of a graph from a core section
	 * 
	 * @param gid
	 *            corelyzer.data.Graph ID
	 */
	public native static int removeLineGraphFromSection(int gid);

	/**
	 * Remove section image from a track
	 */
	public native static void removeSectionImageFromTrack(int track, int section);

	public native static void renameTrack(int trackId, String newName);

	/**
	 * Render the scene at the given canvas
	 * 
	 * @param canvas
	 *            the id of the canvas to render at
	 */
	public native static void render(int canvas);

	public native static void repositionFreeDrawRectangle(int freeDrawRect, float x, float y);

	public native static void resetDefaultTrackYPos();

	// ----- Picking Methods

	/**
	 * DEPRECATED
	 */
	// public native static int pickForTrack(int canvas,float x,float y);

	/**
	 * DEPRECATED
	 */
	// public native static int pickForSection(int canvas, int track,
	// float x,float y);

	/**
	 * Rotate a specified core section image to a angle.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param angle
	 *            Rotation angle in degree
	 */
	public native static void rotateSection(int track, int section, float angle);

	/**
	 * Makes the scene scale relative to the current scale
	 */
	public native static void scaleScene(float s);

	// getter & setter of canvas background color
	public native static void setBackgroundColor(float r, float g, float b);

	public native static void setCanvasBottomRow(int canvas, boolean flag);

	public native static void setCanvasFirstColumn(int canvas, boolean flag);

	/**
	 * Access method. Set the color of grid guide line
	 * 
	 * @param r
	 *            Color's red component value. Range: 0.0f - 1.0f
	 * @param g
	 *            Color's green component value. Range: 0.0f - 1.0f
	 * @param b
	 *            Color's blue component value. Range: 0.0f - 1.0f
	 */
	public native static void setCanvasGridColor(float r, float g, float b);

	/**
	 * Access method. Set the size of grid guide line
	 * 
	 * @param size
	 *            gap between each grid lines (centimeter)
	 */
	public native static void setCanvasGridSize(float size);

	/**
	 * Access method. Set the thickness of grid guide line
	 * 
	 * @param thick
	 *            gap between each grid lines (centimeter)
	 */
	public native static void setCanvasGridThickness(int thick);

	// ----- Annotation Marker Methods
	// xpos is world space x position

	/**
	 * Access method. Set the type of grid guide
	 * 
	 * @param type
	 *            type of grid (line,point,small cross)
	 */
	public native static void setCanvasGridType(int type);

	public native static void setCanvasRowcAndColumn(int nrows, int ncols);

	public static void setCoreSectionMarkerFocus(final boolean value) {
		setCoreSectionMarkerFocus(focusedTrack, focusedTrackSection, focusedMarker, value);
		if (!value) {
			focusedTrack = focusedTrackSection = focusedMarker = -1;
		}
	}

	/**
	 * Set an annotation marker's focus
	 */
	public native static void setCoreSectionMarkerFocus(int track, int section, int marker, boolean flag);

	/**
	 * Modifies an annotation marker's group
	 */
	public native static void setCoreSectionMarkerGroup(int track, int section, int marker, int groupid);

	/**
	 * Access method. Set the marker's local image resource filepath.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @param filename
	 *            Marker's image resource local filepath
	 */
	public native static void setCoreSectionMarkerLocal(int track, int section, int marker, String filename);

	public native static void setCoreSectionMarkerRelationText(int track, int section, int marker, String text);

	public native static void setCoreSectionMarkerText(int track, int section, int marker, String text);

	/**
	 * Modifies an annotation marker's type
	 */
	public native static void setCoreSectionMarkerType(int track, int section, int marker, int typeId);

	/**
	 * Set the marker's image resource URL
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @param url
	 *            The url of the marker image resource
	 */
	public native static void setCoreSectionMarkerURL(int track, int section, int marker, String url);

	/**
	 * Access method. Set the marker's misc vertex for rendering
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param marker
	 *            Marker ID
	 * @param ax
	 *            Marker's annotation icon posx
	 * @param ay
	 *            Marker's annotation icon posy
	 * @param v0
	 *            Marker's v0
	 * @param v1
	 *            Marker's v1
	 * @param v2
	 *            Marker's v2
	 * @param v3
	 *            Marker's v3
	 */
	public native static void setCoreSectionMarkerVertex(int track, int section, int marker, float ax, float ay, float v0, float v1, float v2, float v3);

	/**
	 * Modifies an annotation marker's visibility
	 */
	public native static void setCoreSectionMarkerVisibility(int track, int section, int marker, boolean flag);

	// has crosshari?
	public native static void setCrossHair(boolean b);

	/**
	 * Access method. Set the URL of a dataset in Corelyzer runtime.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param name
	 *            The URL of dataset
	 */
	public native static void setDatasetURL(int set, String name);

	public native static void setDebug(boolean b);

	// depth orientation
	public native static void setDepthOrientation(boolean o);

	/**
	 * Set the min/max value in a field of a table.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param field
	 *            Field ID
	 * @param min
	 *            Minimum value of this field
	 * @param max
	 *            Maximum value of this field
	 */
	public native static void setFieldMinMax(int set, int table, int field, float min, float max);

	public native static void setFreeDrawVisiblity(int freeDrawRect, boolean flag);

	public native static void setGraphScale(float scale);

	public native static void setGraphsCollapse(boolean isCollapse);

	/**
	 * Access method. Set the URL of specified core section image.
	 * 
	 * @param image
	 *            Core section image
	 * @param url
	 *            Image URL in string
	 */
	public native static void setImageURL(int image, String url);

	/**
	 * Access method. Set the color of graph plotting line.
	 * 
	 * @param gid
	 *            corelyzer.data.Graph ID
	 * @param r
	 *            Color's red component value. Range: 0.0f - 1.0f
	 * @param g
	 *            Color's green component value. Range: 0.0f - 1.0f
	 * @param b
	 *            Color's blue component value. Range: 0.0f - 1.0f
	 */
	public native static void setLineGraphColor(int gid, float r, float g, float b);

	/**
	 * Access method. Set the label text of a graph
	 * 
	 * @param gid
	 *            corelyzer.data.Graph ID
	 * @param label
	 *            Label text string
	 */
	public native static void setLineGraphLabel(int gid, String label);

	/**
	 * Access method. Set the viewing range(min/max values) of a graph.
	 * 
	 * @param gid
	 *            corelyzer.data.Graph ID
	 * @param min
	 *            Minimum value of the viewing range
	 * @param max
	 *            Maximum value of the viewing range
	 */
	public native static void setLineGraphRange(int gid, float min, float max);

	// exlcuded values
	public native static void setLineGraphExcludeRange(int gid, float min, float max);
	
	public native static void setLineGraphExcludeStyle(int gid, int style);
	
	/**
	 * Access method. Set the type of graph.
	 * 
	 * @param gid
	 *            corelyzer.data.Graph ID
	 * @param type
	 *            corelyzer.data.Graph type
	 */
	public native static void setLineGraphType(int gid, int type);

	public native static void setMarkerScale(float scale);

	public native static void setMeasurePoint(float x1, float y1, float x2, float y2);

	public native static void setMode(int mode);

	// end of james addition

	// acknowledge scenegraph is remote controlled
	public native static void setRemoteControl(boolean b);

	public native static void setRenderMode(int mode);

	/**
	 * Forces a specific scale to be used
	 */
	public native static void setSceneScale(float s);

	/**
	 * Access method. Set the core section DPI.
	 * 
	 * @param track
	 *            Track ID
	 * @param section
	 *            Core section ID
	 * @param dpix
	 *            X DPI
	 * @param dpiy
	 *            Y DPI
	 */
	public native static void setSectionDPI(int track, int section, float dpix, float dpiy);

	/**
	 * Access method. Set the coresection graph offset in cm
	 * 
	 * @param track
	 *            Track ID
	 * @param sec
	 *            Core section ID
	 * @param offset
	 *            Core section graph offset in cm
	 */
	public native static void setSectionGraphOffset(int track, int sec, float offset);

	public native static void setSectionHighlightColor(int track, int section, float r, float g, float b);

	public native static void setSectionIntervalBottom(int track, int sec, float bottom);

	public native static void setSectionIntervalTop(int track, int sec, float top);

	/**
	 * Assign section name
	 */
	public native static void setSectionName(int track, int section, String name);

	// Section's orientation, 0: landscape, 1:portrait
	public native static void setSectionOrientation(int track, int section, boolean b);

	public native static void setSectionParentIds(int trackId, int sectionId, int parentTrack, int parentSection);

	// show origin?
	public native static void setShowOrigin(boolean b);

	// show section text
	public native static void setShowSectionText(boolean b);

	/**
	 * Set the value of a cell in a table.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param field
	 *            Field ID
	 * @param row
	 *            Row ID
	 * @param valid
	 *            Whether this cell is valid
	 * @param depth
	 *            Depth value of this cell
	 * @param v
	 *            Data value in this cell
	 */
	public native static void setTableCell(int set, int table, int field, int row, boolean valid, float depth, float v);

	/**
	 * Access method. Set the scale value to be applied as the scaling to depth
	 * values. The depth value should be scaled to centi-meter.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param s
	 *            Scaling
	 */
	public native static void setTableDepthUnitScale(int set, int table, float s);

	/**
	 * Set number of fields in a table.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param count
	 *            Number of fields in this table
	 */
	public native static void setTableFieldCount(int set, int table, int count);

	/**
	 * Set the height(rows) of the table.
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param height
	 *            Number of rows in this table
	 */
	public native static void setTableHeight(int set, int table, int height);

	/**
	 * Access method. Set table's height and number of fields at the same time
	 * 
	 * @param set
	 *            Dataset ID
	 * @param table
	 *            corelyzer.data.Table ID
	 * @param height
	 *            Number of rows in this table
	 * @param count
	 *            Number of fields in this table
	 */
	public native static void setTableHeightAndFieldCount(int set, int table, int height, int count);

	/**
	 * Specify which directory to use to store future image blocks
	 */
	public native static void setTexBlockDirectory(String absPath);

	public native static void setTieDepth(boolean isEnabled, float tieDepth);

	/**
     */
	public native static void setTrackHighlight(int track, boolean isOn);

	public native static void setTrackHighlightColor(int track, float r, float g, float b);

	public native static void setTrackMovable(int track, boolean flag);

	public native static void setTrackShow(int track, boolean isShow);

	public native static void setTrackXPos(int track, float xpos);

	public native static void setTrackYPos(int track, float ypos);

	/**
	 * Access method. Set the vertical dash-line to X position.
	 * 
	 * @param graphId
	 *            corelyzer.data.Graph ID
	 * @param x
	 *            X position
	 */
	public native static void setVerticalLineFromGraphXPosition(int graphId, float x);

	public native static boolean trackIsStaggered(int track);
	public native static void staggerTrackSections(int track, boolean stagger);
	public native static void trimSections(int track, int section, float trim, boolean fromBottom, boolean trimSelAndDeeper);
	public native static void stackSections(int track, int section);
	
	/**
	 * Let's the corelyzer.helper.SceneGraph library know that it's being
	 * startup and requires a plugin manager so that the
	 * corelyzer.helper.SceneGraph can send events to plugins (e.g. free draw)
	 */
	public native static void startUp();

	public static void unlock() {
		try {
			scenelock.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
