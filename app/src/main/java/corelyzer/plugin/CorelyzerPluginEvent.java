package corelyzer.plugin;

/**
 * Each event has a description string that is tab delimited. Following is the
 * protocol of descriptions of currently supported event: <br>
 * <br>
 * TRACK_CREATED:<br>
 * "track name"<br>
 * <br>
 * TRACK_MOVED:<br>
 * <br>
 * "track id"\t"dx inches"\t"dy inches"<br>
 * <br>
 * IMAGE_LOADED:<br>
 * <br>
 * "image filename"\t"image url"<br>
 * <br>
 * SECTION_CREATED:<br>
 * <br>
 * "track id"\t"source image id"\t"x pos inches"\t"y pos inches"<br>
 * <br>
 * MOUSE_MOTION:<br>
 * <br>
 * "x pos inches"\t"y pos inches"<br>
 * <br>
 * SECTION_MOVED:<br>
 * <br>
 * "track id"\t"section id"\t"dx inches"\t"dy inches"<br>
 * <br>
 * FREE_DRAW_SELECTED:<br>
 * <br>
 * This only goes to the plugin that created the free draw rectangle<br>
 * "freedraw id"<br>
 * <br>
 * NEW_ANNOTATION:<br>
 * <br>
 * "track id"\t"section id"\t"x-pos inches from section top"<br>
 * <br>
 * EDIT_ANNOTATION:<br>
 * <br>
 * "track id"\t"section id"\t"annotation id"<br>
 * <br>
 * NEW_ANNOTATION_ENTRY:<br>
 * <br>
 * "track id"\t"section id"\t"annotation id"\t"Annotation String..."<br>
 * <br>
 */

public class CorelyzerPluginEvent {

	private final int eventID; // the kind of event
	private final String description; // description of the event

	public static final int NUMBER_OF_EVENTS = 19;

	public static final int MOUSE_MOTION = 0;
	public static final int CLICK_ON_SECTION = 1;
	public static final int ADD_ANNOTATION = 2;
	public static final int SECTION_MOVED = 3;
	public static final int TRACK_MOVED = 4;
	public static final int IMAGE_LOADED = 5;
	public static final int SECTION_CREATED = 6;
	public static final int DATAFILE_LOADED = 7;
	public static final int GRAPH_CREATED = 8;
	public static final int GRAPH_REMOVED = 9;
	public static final int TRACK_CREATED = 10;
	public static final int SECTION_DPI_CHANGE = 11;
	public static final int IMAGE_ROTATED = 12;
	public static final int FREE_DRAW_SELECTED = 13;

	public static final int NEW_ANNOTATION = 14;
	public static final int NEW_ANNOTATION_ENTRY = 15;
	public static final int EDIT_ANNOTATION = 16;

	public static final int TRACK_REMOVED = 17;
	public static final int SECTION_REMOVED = 18;

	public CorelyzerPluginEvent(final int id, final String desc) {
		eventID = id;
		description = desc;
	}

	public String getDescription() {
		return new String(description);
	}

	public int getID() {
		return eventID;
	}
}
