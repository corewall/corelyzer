/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen,
 * Sangyoon Lee, Electronic Visualization Laboratory, University of Illinois 
 * at Chicago
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

// #include "shaders.h"
#include "common.h"
#include "corelyzer_graphics_SceneGraph.h"
#include "annotationmarker.h"
#include "canvas.h"
#include "trackscene.h"
#include "textureresource_ex.h"
#include "coresection.h"
#include "model.h"
#include "dataset.h"
#include "graph.h"
#include "fontsys.h"
#include "freedraw.h"

#include <vector>
#include <iostream>
#include <math.h>

using namespace std;

//=====================================================================
int   default_track_scene = -1;
float center_x = 0.0f;
float center_y = 0.0f;

float get_scene_center_x();
float get_scene_center_y();
void  translate_scene_center(float dx, float dy);
void  update_center_point();
void scale_scene( float ds );
char* default_block_dir = NULL;
int duplicateSection(int trackId, int sectionId, int newTrackId);

int PickedTrack    = -1;
int PickedSection  = -1;
int PickedGraph    = -1;
int PickedMarker   = -1;
int PickedFreeDraw = -1;

bool MeasureMode	= false;
int  MeasurePoints  = 0;

void perform_pick(int canvas, float x, float y);

#define TRACK        0
#define SECTION      1
#define SECTION_ANNO 2
#define GRAPH        3
#define GRAPH_ANNO   4

#define DEFAULT_FONT_FILE "resources/arial.ttf"

int   selected_objects[5];

bool markers_initialized = false;

// For separation of image_block_generation and textureset_insertion
typedef struct {
    char *imageFilename;
    MultiLevelTextureSetEX* texset;    
} TextureSet;

// Is there a good cross platform solution? Or I can only do that in Java?
// A semaphore in scenegraph.java: access with texturelock using calls:
// imageLock(); and imageUnlock();
static std::vector<TextureSet*> tVec;

// for highlight track and section
static int prevPickedTrack   = -1;
static int prevPickedSection = -1;

// for limit zooming levels
#define MIN_SCALE (0.02)
#define MAX_SCALE (4000)
static float allScale = 1.0f;

// Levels of texture blocks to generate
#define LEVELS (3)

//************************** JNI FUNCTIONS ********************************//

#ifdef __cplusplus
extern "C" {
#endif

// james addition
/*
 * Class:     SceneGraph
 * Method:    setMode
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setMode
(JNIEnv *jenv, jclass jcls, jint mode) {

	// for each canvas, reset measure point number
    for( int id = 0; id < num_canvases(); ++id)
    {
        if( !is_canvas(id) ) continue;
		set_canvas_mode(id, mode);
	}
}

/*
 * Class:     SceneGraph
 * Method:    addMeasurePoint
 * Signature: (FF)V
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_addMeasurePoint
(JNIEnv *jenv, jclass jcls, jfloat x, jfloat y){
    
	// for each canvas, add measure point
	int npoint = 0;
    for( int id = 0; id < num_canvases(); ++id)
    {
        if( !is_canvas(id) ) continue;
		npoint = add_canvas_measurepoint(id, x, y);
	}

	return npoint;

}

/*
 * Class:     SceneGraph
 * Method:    setMeasurePoint
 * Signature: (FFFF)I
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setMeasurePoint
(JNIEnv *jenv, jclass jcls, jfloat x1, jfloat y1, jfloat x2, jfloat y2){

	// for each canvas, set measure point
    for( int id = 0; id < num_canvases(); ++id)
    {
        if( !is_canvas(id) ) continue;
		set_canvas_measurepoint(id, x1, y1, x2, y2);
	}
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    addClastPoint1
 * Signature: (FF)I
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_addClastPoint1
  (JNIEnv * aEnv, jclass aClass, jfloat x_pos, jfloat y_pos)
{
	// for each canvas, add 1st clast point
    for( int id = 0; id < num_canvases(); ++id)
    {
        if( !is_canvas(id) ) continue;
		set_clast_1st_point(id, x_pos, y_pos);
	}
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    addClastPoint2
 * Signature: (FF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_addClastPoint2
  (JNIEnv * aEnv, jclass aClass, jfloat x_pos, jfloat y_pos)
{
	// for each canvas, add 2nd clast point
    for( int id = 0; id < num_canvases(); ++id)
    {
        if( !is_canvas(id) ) continue;
		set_clast_2nd_point(id, x_pos, y_pos);
	}
}

/*
 * Class:     SceneGraph
 * Method:    startUp
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_startUp
  (JNIEnv *jenv, jclass jcls){

    printf("\n--- SceneGraph startUp called ---\n");
    if( default_track_scene < 0)
        default_track_scene = create_track_scene();    

    if( default_track_scene >= 0)
    {
        printf("Default Track Scene Made & Bound: %d \n", default_track_scene);
        bind_scene(default_track_scene);
    }
    else
        printf("Unable to make Default Track Scene\n");

    // set the default block directory to imgblocks
    default_block_dir = (char*) malloc( strlen("imgblocks/") + 1);
    strcpy( default_block_dir, "imgblocks/");

    set_current_jnienv(jenv);
    set_current_font( queue_font_to_load(DEFAULT_FONT_FILE));

#ifdef DEBUG    
    printf( "\n--- Dataset init ---\n" );
    printf( "\n--- Init dsVecSize is: [%d] ---\n", num_datasets() );
#endif

    // init_shaders();
}

/*
 * Class:     SceneGraph
 * Method:    closeDown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_closeDown
  (JNIEnv *jenv, jclass jcls){
    if( default_block_dir )
        free(default_block_dir);

    printf("\n--- SceneGraph closeDown called ---\n");
    printf("Freeing Default Track Scene\n");
    free_track_scene(default_track_scene);
    default_track_scene = -1;
    printf("Freeing all texture resources\n");
  //  free_all_texsets(true);

  /*  // Free up section annotation markers
    if (markers_initialized)
    {
        printf("\n---- Free up section annotation markers ----\n");
        free_section_annotation_markers();
        markers_initialized = false;
    }
  */
}

/*
 * Class:     SceneGraph
 * Method:    setTexBlockDirectory
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTexBlockDirectory
  (JNIEnv *jenv, jclass cls, jstring abspath)
{
    int i;
    i = jenv->GetStringLength(abspath);
    if( i <= 1) return;

    if( default_block_dir ) delete [] default_block_dir;
    default_block_dir = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(abspath,0,i,default_block_dir);
    printf("Default Tex Block Directory Now: %s\n", default_block_dir);
}


/*
 * Class:     SceneGraph
 * Method:    getImageName
 * Signature: (V)C
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getTexBlockDirectory
  (JNIEnv *jenv, jclass jcls){
    return jenv->NewStringUTF(default_block_dir);
}

/*
 * Class:     SceneGraph
 * Method:    panScene
 * Signature: (FF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_panScene
  (JNIEnv *jenv, jclass jcls, jfloat dx, jfloat dy){
    
#ifdef DEBUG
    printf("\n--- SceneGraph panScene called ---\n");
    printf("Incoming delta %f, %f\n", dx, dy);
#endif
    translate_scene_center(dx,dy);
}

/*
 * Class:     SceneGraph
 * Method:    scaleScene
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_scaleScene
(JNIEnv *jenv, jclass jcls, jfloat ds)
{
	scale_scene( ds );
}
	
/*
 * Class:     SceneGraph
 * Method:    positionScene
 * Signature: (FF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_positionScene
  (JNIEnv *jenv, jclass jcls, jfloat x, jfloat y){

    float dx, dy;

    dx = get_scene_center_x();
    dy = get_scene_center_y();
    
    dx = x - dx;
    dy = y - dy;
    
    translate_scene_center(dx, dy);

    // position mouse to stay where they are in screen space
    for( int id = 0; id < num_canvases(); ++id)
    {
        if( !is_canvas(id) ) continue;

        float mx = get_canvas_mouseX(id) + dx;
        float my = get_canvas_mouseY(id) + dy;
        
		set_canvas_mouse(id, mx, my);
	}
}

/*
 * Class:     SceneGraph
 * Method:    positionMouse
 * Signature: (FF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_positionMouse
  (JNIEnv *jenv, jclass jcls, jfloat x, jfloat y){

    for( int id = 0; id < num_canvases(); ++id)
    {
        if( !is_canvas(id) ) continue;
		set_canvas_mouse(id, x, y);
	}
}

/*
 * Class:     SceneGraph
 * Method:    setSceneScale
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSceneScale
  (JNIEnv *jenv, jclass jcls, jfloat s){
    
}

/*
 * Class:     SceneGraph
 * Method:    getSceneCenterX
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSceneCenterX
  (JNIEnv *jenv, jclass jcls){
    
    return get_scene_center_x();
}

/*
 * Class:     SceneGraph
 * Method:    getSceneCenterY
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSceneCenterY
  (JNIEnv *jenv, jclass jcls){

    return get_scene_center_y();
}


/*
 * Class:     SceneGraph
 * Method:    genCanvas
 * Signature: (IIII)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_genCanvas
  (JNIEnv *jenv, jclass jcls, jfloat x, jfloat y, jint width, jint height,
   jfloat dpix, jfloat dpiy){

    int id = create_canvas( x, y, (float) width, (float) height, dpix, dpiy);
    if( id >= 0)
        update_center_point();
    return id;
}

/*
 * Class:     SceneGraph
 * Method:    numCanvases
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_numCanvases
  (JNIEnv *jenv, jclass jcls){
    
    return num_canvases();
}

/*
 * Class:     SceneGraph
 * Method:    destroyCanvases
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_destroyCanvases
  (JNIEnv *jenv, jclass jcls){

    int i;
    for( i = 0; i < num_canvases(); ++i)
        free_canvas(i);
	//free_all_canvas();
	update_center_point();
}


/*
 * Class:     SceneGraph
 * Method:    debugKey
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_debugKey
	(JNIEnv *jenv, jclass jcls, jint keyId)
{
	// 8/16/2012 brg: Leaving around as mechanism to pass keystrokes to
	// scenegraph for debugging purposes.
}

	
/*
 * Class:     SceneGraph
 * Method:    render
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_render
  (JNIEnv *jenv, jclass jcls, jint canvas_id)
{
    set_current_jnienv(jenv);

    if (!markers_initialized)
    {
        printf("Initializing markers\n");
        init_section_annotation_markers();
        markers_initialized = true;
    }

    render_canvas(canvas_id);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setRenderMode
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setRenderMode
  (JNIEnv * jenv, jclass jclass, jint renderMode)
{
    set_render_mode(renderMode);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getRenderMode
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getRenderMode
  (JNIEnv *, jclass)
{
    return get_render_mode();  
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setCanvasBottomRow
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCanvasBottomRow
  (JNIEnv * jenv, jclass jcls, jint canvas, jboolean flag)
{
    if( flag )
        set_canvas_bottom_row(canvas, true);
    else
        set_canvas_bottom_row(canvas, false);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setCanvasRowcAndColumn
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCanvasRowcAndColumn
  (JNIEnv * jenv, jclass jcls, jint nrows, jint ncols)
{
    set_canvas_rows_and_columns(nrows, ncols);
}


/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setCanvasFirstColumn
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCanvasFirstColumn
  (JNIEnv * jenv, jclass jcls, jint canvas, jboolean flag)
{
    if( flag )
        set_canvas_first_column(canvas, true);
    else
        set_canvas_first_column(canvas, false);
}


/*
 * Class:     SceneGraph
 * Method:    markCanvasDrawCrossCoreScale
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_enableCanvasCrossCoreScale
  (JNIEnv *env, jclass jcls, jint canvas, jboolean flag){
    
    if( flag )
        set_canvas_draw_cross_core_scale(canvas, true);
    else
        set_canvas_draw_cross_core_scale(canvas, false);
}

/*
 * Class:     SceneGraph
 * Method:    enableCanvasGrid
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_enableCanvasGrid
(JNIEnv *env, jclass jcls, jboolean flag){

	for( int i = 0; i < num_canvases(); ++i) {
		if( flag )
			set_canvas_draw_grid(i, true);
		else
			set_canvas_draw_grid(i, false);
	}
}

/*
 * Class:     SceneGraph
 * Method:    setCanvasGridColor
 * Signature: (IFFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCanvasGridColor
(JNIEnv *env, jclass jcls, jfloat r, jfloat g, jfloat b){

	for( int i = 0; i < num_canvases(); ++i)
		set_canvas_grid_color(i, r, g, b);
}

/*
 * Class:     SceneGraph
 * Method:    setCanvasGridSize
 * Signature: (IF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCanvasGridSize
(JNIEnv *env, jclass jcls, jfloat size){

	for( int i = 0; i < num_canvases(); ++i)
		set_canvas_grid_size(i, size);

}

/*
 * Class:     SceneGraph
 * Method:    setCanvasGridThickness
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCanvasGridThickness
(JNIEnv *env, jclass jcls, jint thick){

	for( int i = 0; i < num_canvases(); ++i)
		set_canvas_grid_thickness(i, thick);

}

/*
 * Class:     SceneGraph
 * Method:    setCanvasGridType
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCanvasGridType
(JNIEnv *env , jclass jcls, jint type){

	for( int i = 0; i < num_canvases(); ++i)
		set_canvas_grid_type(i, type);
}

/*
 * Class:     SceneGraph
 * Method:    orientSceneVertical
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_orientSceneVertical
  (JNIEnv *jenv, jclass jcls, jboolean flag) {
    
    int i;

    for( i = 0; i < num_canvases(); i++)
    {
        /* tell each one to align the scene vertically */
        if( flag )
            orient_canvas_vertically(i);
        else
            orient_canvas_horizontally(i);
    }
}

/*
 * Class:     SceneGraph
 * Method:    getCanvasPositionX
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCanvasPositionX
  (JNIEnv *jenv, jclass jcls, jint canvas){

    float x, y, z;
    int camera;
    if(!is_canvas(canvas)) return 0.0;
    camera = get_canvas_camera(canvas);
    if(!is_camera(camera)) return 0.0;
    get_camera_position(camera,&x,&y,&z);

    return x;

}

/*
 * Class:     SceneGraph
 * Method:    getCanvasPositionY
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCanvasPositionY
  (JNIEnv *jenv, jclass jcls, jint canvas){

    float x, y, z;
    int camera;
    if(!is_canvas(canvas)) return 0.0;
    camera = get_canvas_camera(canvas);
    if(!is_camera(camera)) return 0.0;
    get_camera_position(camera,&x,&y,&z);

    return y;

}

/*
 * Class:     SceneGraph
 * Method:    getCanvasWidth
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCanvasWidth
  (JNIEnv *jenv, jclass jcls, jint canvas){

    float w, h;
    if(!is_canvas(canvas)) return 0.0;
    
    get_canvas_dimensions(canvas,&w,&h);

    return w;
}

/*
 * Class:     SceneGraph
 * Method:    getCanvasHeight
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCanvasHeight
  (JNIEnv *jenv, jclass jcls, jint canvas){

    float w, h;
    if(!is_canvas(canvas)) return 0.0;
    
    get_canvas_dimensions(canvas,&w,&h);

    return h;

}

/*
 * Class:     SceneGraph
 * Method:    getCanvasDPIX
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCanvasDPIX
  (JNIEnv *jenv, jclass jcls, jint canvas){
    
    float dpix, dpiy;

    if(!is_canvas(canvas)) return 0.0;

    get_canvas_dpi(canvas,&dpix,&dpiy);

    return dpix;
}

/*
 * Class:     SceneGraph
 * Method:    getCanvasDPIY
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCanvasDPIY
  (JNIEnv *jenv, jclass jcls, jint canvas){

    float dpix, dpiy;

    if(!is_canvas(canvas)) return 0.0;

    get_canvas_dpi(canvas,&dpix,&dpiy);

    return dpiy;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    addTrack
 * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_addTrack
  (JNIEnv * jenv, jclass jcls, jstring jSessionName, jstring jTrackName)
{
    char* sessionName;
    char* trackName;
    int length = -1;

    // convert name to UTF-8, call createTrack and return index
    length = jenv->GetStringLength(jSessionName);
    sessionName = (char*) malloc(length * sizeof(char) + 1);
    jenv->GetStringUTFRegion(jSessionName, 0, length, sessionName);

    length = jenv->GetStringLength(jTrackName);
    trackName = (char*) malloc(length * sizeof(char) + 1);
    jenv->GetStringUTFRegion(jTrackName, 0, length, trackName);

    int i = append_track(default_track_scene, sessionName, trackName);

    free(sessionName);
    free(trackName);

    return i;
}

/*
 * Class:     SceneGraph
 * Method:    deleteTrack
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_deleteTrack
  (JNIEnv *jenv, jclass jcls, jint track){

    // printf("\n--- Delete Track called ---\n");
    free_track( default_track_scene, track);

#ifdef DEBUG
    printf("Freed Track %d in Track Scene %d\n", track, default_track_scene);
#endif

}

/*
 * Class:     SceneGraph
 * Method:    highlightTrack
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTrackHighlight
  (JNIEnv *jenv, jclass jcls, jint track, jboolean isOn)
{
    int i;
    TrackSceneNode *tsn;

    // make sure to unhilight all tracks
    /*
    for( i = 0; i < num_tracks(default_track_scene); ++i)
    {
        tsn = get_scene_track(default_track_scene,i);
        if(!tsn)
        {
            continue;
        }
        else
        {
            tsn->highlight = false;
            set_crosshair_label(NULL);            
        }
    } */

    // highlight the track we want

    tsn = get_scene_track(default_track_scene, track);
    if(!tsn) return;

    tsn->highlight = isOn;

    /*
    char *label = new char[128];
    sprintf(label, "track [%s]", tsn->name);
    set_crosshair_label(label);
    free(label);
    */
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setTrackHighlightColor
 * Signature: (IFFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTrackHighlightColor
  (JNIEnv * jenv, jclass jclass, jint trackId, jfloat r, jfloat g, jfloat b)
{
    TrackSceneNode *t = get_scene_track(default_track_scene, trackId);
    if(!t) return;

    set_track_highlight_color(t, r, g, b);
}



/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setTrackShow
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTrackShow
  (JNIEnv * jenv, jclass jcls, jint trackId, jboolean isShow)
{
    TrackSceneNode *tsn;

    tsn = get_scene_track(default_track_scene, trackId);
    if(!tsn) return;

    tsn->show = isShow;  
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getTrackShow
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_getTrackShow
  (JNIEnv * jenv, jclass jcls, jint track)
{
    TrackSceneNode *tsn;

    tsn = get_scene_track(default_track_scene, track);
    if(!tsn) return false;

    return tsn->show;      
}

/*
 * Class:     SceneGraph
 * Method:    moveTrack
 * Signature: (IFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_moveTrack
  (JNIEnv *jenv, jclass jcls, jint track, jfloat dx, jfloat dy){
    
    TrackSceneNode *tsn = get_scene_track( default_track_scene, track);
    if( !tsn ) return;

    if(tsn->movable)
    {
        tsn->px += dx;
    }

    tsn->py += dy;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    moveTrackAbsX
 * Signature: (IF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_moveTrackAbsX
  (JNIEnv * jenv, jclass jcls, jint track, jfloat absX)
{
    TrackSceneNode *tsn = get_scene_track(default_track_scene, track);
    if(!tsn) return;

    tsn->px = absX;
}


/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    moveTrackAbsY
 * Signature: (IF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_moveTrackAbsY
  (JNIEnv * jenv, jclass jcls, jint track, jfloat absY)
{
    TrackSceneNode *tsn = get_scene_track(default_track_scene, track);
    if(!tsn) return;

    tsn->py = absY;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    renameTrack
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_renameTrack
  (JNIEnv * jenv, jclass jclass, jint trackId, jstring newName) {
    TrackSceneNode *t = get_scene_track(default_track_scene, trackId);
    if(!t) return;

    char* nbuf;
    int   i;

    i = jenv->GetStringLength(newName);

    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(newName, 0, i, nbuf);

    delete [] t->name;
    
    t->name = new char[strlen(nbuf) + 1];
    strcpy(t->name, nbuf);

    free(nbuf);
}


/*
 * Class:     SceneGraph
 * Method:    getImageName
 * Signature: (I)C
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getTrackName
  (JNIEnv *jenv, jclass jcls, jint trackId){
    TrackSceneNode* tsn = get_scene_track(default_track_scene,trackId);
    if( !tsn )
        return jenv->NewStringUTF(NULL);
    else
        return jenv->NewStringUTF(tsn->name);
}

/*
 * Class:     SceneGraph
 * Method:    getTrackXPos
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getTrackXPos
  (JNIEnv *jenv, jclass jcls, jint trackId)
{
    if(!is_track(default_track_scene,trackId)) return 0.0;
    
    return get_scene_track(default_track_scene,trackId)->px;
}

/*
 * Class:     SceneGraph
 * Method:    getTrackYPos
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getTrackYPos
  (JNIEnv *jenv, jclass jcls, jint trackId)
{
    if(!is_track(default_track_scene,trackId)) return 0.0;
    
    return get_scene_track(default_track_scene,trackId)->py;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setTrackXPos
 * Signature: (IF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTrackXPos
  (JNIEnv * jenv, jclass jcls, jint trackId, jfloat xpos)
{
    if(!is_track(default_track_scene,trackId)) return;

    TrackSceneNode *t = get_scene_track(default_track_scene, trackId);
    if(t)
    {
        t->px = xpos;
    }
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setTrackYPos
 * Signature: (IF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTrackYPos
  (JNIEnv * jenv, jclass jcls, jint trackId, jfloat ypos)
{
    if(!is_track(default_track_scene,trackId)) return;

    TrackSceneNode *t = get_scene_track(default_track_scene, trackId);
    if(t)
    {
        t->py = ypos;    
    }
}


/*
 * Class:     SceneGraph
 * Method:    getNumSections
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getNumSections
  (JNIEnv *jenv, jclass jcls, jint trackId)
{
    if(!is_track(default_track_scene,trackId)) return 0;
    
    return get_track_section_zorder_length(
        get_scene_track(default_track_scene,trackId));
}

/*
 * Class:     SceneGraph
 * Method:    bringTrackToFront
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_bringTrackToFront
  (JNIEnv *jenv, jclass jcls, jint trackId)
{
    if(!is_track(default_track_scene,trackId)) return;

    // update the zorder
    bring_track_front( default_track_scene, trackId);
}


/*
 * Class:     SceneGraph
 * Method:    getTrackIDByName
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getTrackIDByName
  (JNIEnv *jenv, jclass jcls, jstring jSessionName, jstring jTrackName)
{
    char *sessionName;
    char *trackName;

    int length = jenv->GetStringLength(jSessionName);
    sessionName = (char*) malloc(length * sizeof(char) + 1);
    jenv->GetStringUTFRegion(jSessionName, 0, length, sessionName);

    length = jenv->GetStringLength(jTrackName);
    trackName = (char*) malloc(length * sizeof(char) + 1);
    jenv->GetStringUTFRegion(jTrackName, 0, length, trackName);

    for(int i = 0; i < num_tracks(default_track_scene); i++)
    {
        TrackSceneNode *tsn = get_scene_track(default_track_scene, i);

        if( !tsn ) {
            continue;
        }

        // Also compare "session name" to make tracks, session aware?
        if( !strcmp(sessionName, tsn->sessionName)
            && !strcmp(trackName, tsn->name)
          )
        {
            free(sessionName);
            free(trackName);
            
            return i;
        }
    }

    free(sessionName);
    free(trackName);
    
    return -1;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionIDByName
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getSectionIDByName
  (JNIEnv * jenv, jclass jcls, jint trackId, jstring name)
{
    TrackSceneNode *t = get_scene_track(default_track_scene, trackId);
    if(!t) return -1;

    int i;
    char* nbuf;

    i = jenv->GetStringLength(name);
    nbuf = (char*) malloc(i*sizeof(char) + 1);
    jenv->GetStringUTFRegion(name, 0, i, nbuf);

    for(i = 0; i < t->modelvec.size(); i++)
    {
        CoreSection* cs = t->modelvec[i];

        if(!cs) continue;

        if(get_section_name(cs) == NULL) continue;

        if(!strcmp(nbuf, cs->name))
        {
            free(nbuf);
            return i;
        }
    }

    free(nbuf);
    return -1;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionIDFromURL
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getSectionIDFromURL
  (JNIEnv * jenv, jclass jcls, jint trackId, jstring urlString)
{
    TrackSceneNode *t = get_scene_track(default_track_scene, trackId);
    if(!t) return -1;

    int i;
    char* nbuf;

    i = jenv->GetStringLength(urlString);
    nbuf = (char*) malloc(i*sizeof(char) + 1);
    jenv->GetStringUTFRegion(urlString, 0, i, nbuf);

    for(i = 0; i < t->modelvec.size(); i++)
    {
        CoreSection* cs = t->modelvec[i];

        if(!cs) continue;

        // Has image
        if(is_texset(cs->src))
        {
            char* imageURL = get_texset_url(cs->src);
            if(imageURL)
            {
                if(!strcmp(nbuf, imageURL))
                {
                    free(nbuf);
                    return i;
                }            
            }
        }
    }

    free(nbuf);
    return -1;
}



/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    loadImage
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_loadImage
  (JNIEnv * jenv, jclass jcls, jstring name)
{
    int i;
    char* nbuf;

    i = jenv->GetStringLength(name);
    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(name, 0, i, nbuf);

    // Search the tVec for common filename
    for(int j=0; j<tVec.size(); j++)
    {
        TextureSet* ts = tVec[j];
        if(ts == NULL) continue;

        if(strcmp(ts->imageFilename, nbuf) == 0)
        {
#ifdef DEBUG
            printf("---> [INFO] Hit  %d. file: %s\n", j, nbuf);
#endif
            int k = insert_texset(ts->texset);

            // remove ts from tVec;
            free(ts->imageFilename);
            free(ts);
            tVec[j] = NULL;            

            free(nbuf);
            return k;
        }
        else
        {
#ifdef DEBUG
            printf("---> [INFO] Miss %d, file: %s\n", j, nbuf);
#endif
        }
    }

    printf("---> [INFO] No texset available for image: %s\n", nbuf);

    free(nbuf);
    return -1;
}

/*
 * Class:     SceneGraph
 * Method:    genTextureBlocks
 * Signature: (C)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_genTextureBlocks
  (JNIEnv *jenv, jclass jcls, jstring name)
{
	// 4/17/2012 brg: File verification error codes, corresponding to codes in
	// CorelyzerAppController.java. Names are more readable than arbitrary integers!
	// Ideally we'd report a more meaningful error (from the appropriate library) in
	// the case of FILE_READ_ERROR.
	const int FILE_READ_ERROR = -1;
	const int FILE_DOES_NOT_EXIST = -2;
	const int FILE_IS_EMPTY = -3;
	
    // get the name and based on file extension, call the proper load function
    const int fileNameLen = jenv->GetStringLength(name);
    char *fileName = (char*) malloc(fileNameLen * sizeof(char) + 1);
    jenv->GetStringUTFRegion(name, 0, fileNameLen, fileName);

    // fixme printf("---> [INFO] GenTextureBlocks() %s\n", nbuf);
    // fixme 1. Unicode path,
    
    // Verify file exists
    FILE *f = fopen(fileName, "r");
    if (!f)
    {
        free(fileName);
        return FILE_DOES_NOT_EXIST;
    }
	
	// 4/9/2012 brg: Verify file contains data (empty files cause a crash)
	if ( fgetc(f) == EOF )
	{
		free(fileName);
		fclose(f);
		return FILE_IS_EMPTY;
	}
    fclose(f);

    // TextureSet* ts = (TextureSet*) malloc(sizeof(TextureSet));
    TextureSet* ts = new TextureSet();

    if( strstr(fileName,".JPEG") || strstr(fileName,".jpeg") ||
        strstr(fileName,".JPG")  || strstr(fileName,".jpg") )
    {
        ts->texset = create_texset_from_jpeg(fileName, LEVELS);
    }
    else if( strstr(fileName,".PNG") || strstr(fileName,".png"))
    {
        ts->texset = create_texset_from_png(fileName, LEVELS);
    }
    else if( strstr(fileName,".BMP") || strstr(fileName,".bmp"))
    {
        ts->texset = create_texset_from_bmp(fileName, LEVELS);
    }
    else if( strstr(fileName,".TIFF") || strstr(fileName,".tiff") ||
             strstr(fileName,".TIF")  || strstr(fileName,".tif") )
    {
        ts->texset = create_texset_from_tiff(fileName, LEVELS);
    }
    else if(strstr(fileName, ".jp2"))
    {
        printf("---> [TODO] Loading JPEG2000: %s\n", fileName);
        ts->texset = create_texset_from_jp2k(fileName, LEVELS);
    }
    else
    {
        printf("Could not load image %s, unsupported format\n", fileName);
        free(fileName);
        return FILE_READ_ERROR;
    }
	
	if (ts->texset == NULL)
	{
		free(fileName);
		return FILE_READ_ERROR;
	}

    ts->imageFilename = (char*) malloc(fileNameLen * sizeof(char) + 1);
    strcpy(ts->imageFilename, fileName);    
    free(fileName);

    // find a place to insert
    for (int i = 0; i < tVec.size(); i++)
    {
        if(tVec[i] == NULL)
        {
            tVec[i] = ts;
            return 1;
        }
    }
    tVec.push_back(ts);

    return 1;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    genTextureBlocksToDirectory
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_genTextureBlocksToDirectory
  (JNIEnv * jenv, jclass jcls, jstring inputFileNameString, jstring outputDirectoryString)
{
    int input_length, output_length;
    char* inputFileName;
    char* outputDirectory;

    // get the name and based on file extension, call the proper load function

    input_length = jenv->GetStringLength(inputFileNameString);
    inputFileName = (char*) malloc(input_length * sizeof(char) + 1);
    jenv->GetStringUTFRegion(inputFileNameString, 0, input_length, inputFileName);
    
    output_length = jenv->GetStringLength(outputDirectoryString);
    outputDirectory = (char *) malloc(output_length * sizeof(char) + 1);
    jenv->GetStringUTFRegion(outputDirectoryString, 0, output_length, outputDirectory);

    // assign block dir
    if( default_block_dir )
    {
        free(default_block_dir);
    }

    default_block_dir = (char*) malloc(output_length * sizeof(char) + 1);
    strcpy(default_block_dir, outputDirectory);
    free(outputDirectory);

    // 2. non-exist path
    FILE *f = fopen(inputFileName, "r");
    if(!f)
    {
        free(inputFileName);
        return false;
    }

    // fixme 1. Unicode path,
    fclose(f);

    // TextureSet* ts = new TextureSet();
    MultiLevelTextureSetEX* texset;

    if( strstr(inputFileName,".JPEG") || strstr(inputFileName,".jpeg") ||
        strstr(inputFileName,".JPG")  || strstr(inputFileName,".jpg") )
    {
        texset = create_texset_from_jpeg(inputFileName, LEVELS);

        if(texset == NULL)
        {
            free(inputFileName);
            return false;
        }
    }
    else if( strstr(inputFileName,".PNG") || strstr(inputFileName,".png"))
    {
        texset = create_texset_from_png(inputFileName, LEVELS);

        if(texset == NULL)
        {
            free(inputFileName);
            return false;
        }
    }
    else if( strstr(inputFileName,".BMP") || strstr(inputFileName,".bmp"))
    {
        texset = create_texset_from_bmp(inputFileName, LEVELS);

        if(texset == NULL)
        {
            free(inputFileName);
            return false;
        }
    }
    else if( strstr(inputFileName,".TIFF") || strstr(inputFileName,".tiff") ||
             strstr(inputFileName,".TIF")  || strstr(inputFileName,".tif") )
    {
        texset = create_texset_from_tiff(inputFileName, LEVELS);

        if(texset == NULL)
        {
            free(inputFileName);
            return false;
        }
    }
    else if(strstr(inputFileName, ".jp2"))
    {
        printf("---> [TODO] Loading JPEG2000: %s\n", inputFileName);
        texset = create_texset_from_jp2k(inputFileName, LEVELS);

        if(texset == NULL)
        {
            free(inputFileName);
            return false;
        }
    }
    else
    {
        printf("Could not load image %s, unsupported format\n", inputFileName);
        free(inputFileName);

        return false;
    }
    free(inputFileName);

    // release
    if(texset != NULL)
    {
        delete_texset(texset);

        // free(texset);
        delete texset;
    }

    return true;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    duplicateSection
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_duplicateSection
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId)
{
    return duplicateSection(trackId, sectionId, trackId);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    duplicateSectionToAnotherTrack
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_duplicateSectionToAnotherTrack
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId, jint newTrackId)
{
    return duplicateSection(trackId, sectionId, newTrackId);
}

/*
 * Class:     SceneGraph
 * Method:    setImageURL
 * Signature: (IC)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setImageURL
  (JNIEnv *jenv, jclass jcls, jint imageId, jstring url){

    int i;
    char* nbuf;

    if(!is_texset(imageId)) return;

    // get the name and based on file extension, call the proper load function

    i = jenv->GetStringLength(url);
    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(url,0,i,nbuf);

    set_texset_url(imageId,nbuf);
    
    free(nbuf);

}

/*
 * Class:     SceneGraph
 * Method:    getImageName
 * Signature: (I)C
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getImageName
  (JNIEnv *jenv, jclass jcls, jint imageId){
    return jenv->NewStringUTF(get_texset_name(imageId));
}

/*
 * Class:     SceneGraph
 * Method:    getImageURL
 * Signature: (I)C
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getImageURL
  (JNIEnv *jenv, jclass jcls, jint imageId){
    return jenv->NewStringUTF(get_texset_url(imageId));
}

/*
 * Class:     SceneGraph
 * Method:    getImageDPIX
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getImageDPIX
  (JNIEnv *jenv, jclass jcls, jint imageId){
    
    if( !is_texset(imageId)) return 0.0f;
    return get_texset_src_dpi_x(imageId);
}

/*
 * Class:     SceneGraph
 * Method:    getImageDPIY
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getImageDPIY
  (JNIEnv *jenv, jclass jcls, jint imageId){

    if( !is_texset(imageId)) return 0.0f;
    return get_texset_src_dpi_y(imageId);
}


/*
 * Class:     SceneGraph
 * Method:    getImageWidth
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getImageWidth
  (JNIEnv *jenv, jclass jcls, jint imageId){

    if( !is_texset(imageId)) return 0;
    return get_texset_src_width(imageId);
}
	

/*
 * Class:     SceneGraph
 * Method:    getImageDepthPix
 * Signature: (Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getImageDepthPix
(JNIEnv *jenv, jclass jcls, jstring imageFilename, jboolean isVertical)
{
	const int fileNameLen = jenv->GetStringLength( imageFilename );
    char *fileName = (char*) malloc( fileNameLen * sizeof(char) + 1 );
    jenv->GetStringUTFRegion( imageFilename, 0, fileNameLen, fileName );
	
	return get_image_depth_pix( fileName, isVertical );
}


/*
 * Class:     SceneGraph
 * Method:    getImageHeight
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getImageHeight
  (JNIEnv *jenv, jclass jcls, jint imageId){

    if( !is_texset(imageId)) return 0;
    return get_texset_src_height(imageId);
}

/*
 * Class:     SceneGraph
 * Method:    addSectionToTrack
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_addSectionToTrack // fixme
(JNIEnv *jenv, jclass jcls, jint track, jint sec)
{
    CoreSection* section;

    // fixme printf("\n--- Add Section To Track called ---\n");

    if(!is_track(default_track_scene,track))
    {
        printf("ERROR: %d is not a track\n", (int)track);
        return -1;
    }

    create_section_model( track, sec, section);

    if(!section)
    {
        printf("ERROR: Could not create a section model\n");
        return -1;
    }
	
	// update x position
	TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(track);
	section->px = t->nextPos;

    return append_model( get_scene_track(default_track_scene,track), section);

}

/*
 * Class:     SceneGraph
 * Method:    setSectionName
 * Signature: (IILjava/lang/String;)I
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionName
(JNIEnv *jenv, jclass jcls, jint track, jint sec, jstring name){

	TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track( default_track_scene, track);
    cs = get_track_section(t,sec);
    
    if(!cs) return;

	char* nbuf;
    int   i;
    i = jenv->GetStringLength(name);
    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(name, 0, i, nbuf);

	set_section_name(cs, nbuf);
	free(nbuf);

}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionName
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getSectionName
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId)
{
	TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, trackId);
    cs = get_track_section(t, sectionId);

    if(!cs) return NULL;

    return jenv->NewStringUTF(get_section_name(cs));
}

/*
 * Class:     SceneGraph
 * Method:    addSectionImageToTrack
 * Signature: (II)V
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_addSectionImageToTrack
  (JNIEnv *jenv, jclass jcls, jint track, jint sec, jint image)
{
    
    
    CoreSection* section;

    // fixme printf("\n--- Add Section Image To Track called ---\n");

    if(!is_track(default_track_scene,track))
    {
        printf("ERROR: %d is not a track\n", (int)track);
        return -1;
    }

    // fixme printf("\n-- after is_track? --\n");

    if(!is_texset(image))
    {
        printf("ERROR: %d is not an image\n", (int)image);
        return -1;
    }

    // fixme printf("\n-- after is_texset --\n");

    add_section_image( track, sec, image, 0, section);

    // fixme printf("\n-- after add_section_image --\n");

    if(!section)
    {
        printf("ERROR: Could not make a section model from image %d\n", (int)image);
        return -1;
    }

    // fixme printf("\n-- before return: %d --\n", sec);

    //return append_model( get_scene_track(default_track_scene,track), section);
	
	// we know that coresection model already added before this point
	// so, just return that coresection id here
	return sec;
}

/*
 * Class:     SceneGraph
 * Method:    removeSectionImageFromTrack
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_removeSectionImageFromTrack
  (JNIEnv *jenv, jclass jcls, jint track, jint section){

	printf("\n--- Delete Section called ---\n");
	free_track_section_model(default_track_scene, track, section);
}

/*
 * Class:     SceneGraph
 * Method:    highlightSection
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_highlightSection
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jboolean isOn)
{
	TrackSceneNode *t;
    CoreSection *cs;

    t  = get_scene_track(default_track_scene, track);
    if(!t) return -1;

    cs = get_track_section(t, section);
    if(!cs) return -1;

    cs->highlight = isOn;

    // set crosshair label
    // char *label = new char[128];
    int bufferSize = 128;
    char * label = (char *) malloc(sizeof(char) * bufferSize);
    {
        sprintf(label, "section [%s]", get_section_name(cs));
        set_crosshair_label(label);
    }
    free(label);

    // loop through track to un-highlight other sections
    for(int i=0; i<t->modelvec.size(); ++i)
    {
        if( (t->modelvec[i] != NULL) && (t->modelvec[i] != cs) )
        {
            t->modelvec[i]->highlight = false;
        }
    }

    // Un-highlight previous selected section
    t  = get_scene_track(default_track_scene, prevPickedTrack);
    cs = (t) ? get_track_section(t, prevPickedSection) : NULL;
    if(cs)
    {
        cs->highlight = false;
        set_crosshair_label(NULL);
    }

    prevPickedTrack = track;
    prevPickedSection = section;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setSectionHighlightColor
 * Signature: (IIFFF)I
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionHighlightColor
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId, jfloat r, jfloat g, jfloat b)
{
	TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, trackId);
    cs = get_track_section(t, sectionId);

    if(!cs) {
        return;
    }

    set_section_highlight_color(cs, r, g, b);    
}

/*
 * Class:     SceneGraph
 * Method:    moveSection
 * Signature: (IIFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_moveSection
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jfloat dx, jfloat dy){
	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track);
    cs = get_track_section(t,section);
    
	if(!cs) return;
	if(!cs->movable) return;

#ifdef DEBUG
    printf("Updating section position from %f, %f to %f, %f\n",
           cs->px, cs->py, cs->px + dx, cs->py + dy);
#endif

	cs->px += dx;
    cs->py += dy;

    // Allow negative depth, due to some core images might have extra core cap
    // length before the real cores.
    // if( cs->px < 0 )
    // {
    //     cs->px = 0;
    // }

	// update section depth var
	float cdpix, cdpiy;
	get_canvas_dpi(0,&cdpix,&cdpiy);
	cs->depth = cs->px *  CM_PER_INCH / cdpix;		// cm

}

/*
 * Class:     SceneGraph
 * Method:    moveSectionGraph
 * Signature: (IIFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_moveSectionGraph
(JNIEnv *jenv, jclass jcls, jint track, jint section, jfloat dx, jfloat dy){

	TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track( default_track_scene, track);
    cs = get_track_section(t,section);
    
    if(!cs) return;
    if(!cs->graphMovable) return;

#ifdef DEBUG
    printf("Updating section position from %f, %f to %f, %f\n",
           cs->px, cs->py, cs->px + dx, cs->py + dy);
#endif

	// if this section has only graph, then do same logic as moveSection func
	// else, then adjust graph offset only
	if (cs->src == -1)
	{
		cs->px += dx;
		cs->py += dy;
		if( cs->px < 0 ) 
			cs->px = 0;
		
		// update section depth var
		float cdpix, cdpiy;
		get_canvas_dpi(0,&cdpix,&cdpiy);
		cs->depth = cs->px *  CM_PER_INCH / cdpix;		// cm
		
	}
	else
		cs->graph_offset += dx;
	
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setGraphScale
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setGraphScale
  (JNIEnv * jenv, jclass jcls, jfloat scale)
{
    setGraphScale(scale);  
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setMarkerScale
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setMarkerScale
  (JNIEnv * jenv, jclass jcls, jfloat scale)
{
    setMarkerScale(scale);  
}

/*
 * Class:     SceneGraph
 * Method:    positionSection
 * Signature: (IIFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_positionSection
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jfloat x, jfloat y){

    // x and y is pixel unit

	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track);
    if(!t) return;

    cs = get_track_section(t,section);
    if(!cs) return;
    if(!cs->movable) return;

    cs->px = x;
    cs->py = y;

    // Allow negative depth, due to some core images might have extra core cap
    // if( cs->px < 0)
    // {
    //     cs->px = 0;
    // }

	// update section depth var
	float cdpix, cdpiy;
	get_canvas_dpi(0,&cdpix,&cdpiy);
	cs->depth = cs->px *  CM_PER_INCH / cdpix;		// cm
}

/*
 * Class:     SceneGraph
 * Method:    getSectionDepth
 * Signature: (II)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionDepth
(JNIEnv *jenv, jclass jcls, jint track, jint section){

	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track);
    if(!t) return 0;

    cs = get_track_section(t,section);
    if(!cs) return 0;

	return cs->depth;
	
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionParentTrackId
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getSectionParentTrackId
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId)
{
	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, trackId);
    if(!t) return -1;

    cs = get_track_section(t,sectionId);
    if(!cs) return -1;

	return get_section_parentTrackId(cs);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionParentSectionId
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getSectionParentSectionId
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId)
{
	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, trackId);
    if(!t) return -1;

    cs = get_track_section(t,sectionId);
    if(!cs) return -1;

	return get_section_parentSectionId(cs);  
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setSectionParentIds
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionParentIds
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId, jint srcTrackId, jint srcSectionId)
{
	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, trackId);
    if(!t) return;

    cs = get_track_section(t,sectionId);
    if(!cs) return;

    set_section_parentTrackId(cs, srcTrackId);
    set_section_parentSectionId(cs, srcSectionId);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionLength
 * Signature: (II)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionLength
  (JNIEnv * jenv, jclass jcls, jint track, jint section)
{
	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track);
    if(!t) return 0;

    cs = get_track_section(t,section);
    if(!cs) return 0;

	return cs->width;

}
/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionHeight
 * Signature: (II)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionHeight
  (JNIEnv * jenv, jclass jcls, jint track, jint section)
{

	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track);
    if(!t) return 0;

    cs = get_track_section(t,section);
    if(!cs) return 0;

	return cs->height;

}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionWidth
 * Signature: (II)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionWidth
  (JNIEnv * jenv, jclass jcls, jint track, jint section)
{

	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track);
    if(!t) return 0;

    cs = get_track_section(t,section);
    if(!cs) return 0;

	return cs->width;

}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionIntervalTop
 * Signature: (II)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionIntervalTop
  (JNIEnv * jenv, jclass jcls, jint track, jint section)
{
	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track(default_track_scene, track);
    if(!t) return 0;

    cs = get_track_section(t, section);
    if(!cs) return 0;

	return cs->intervalTop;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionIntervalBottom
 * Signature: (II)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionIntervalBottom
  (JNIEnv * jenv, jclass jcls, jint track, jint section)
{
	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track(default_track_scene, track);
    if(!t) return 0;

    cs = get_track_section(t, section);
    if(!cs) return 0;

	return cs->intervalBottom;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setSectionIntervalTop
 * Signature: (IIF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionIntervalTop
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jfloat top)
{
	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track(default_track_scene, track);
    if(!t) return;

    cs = get_track_section(t, section);
    if(!cs) return;

    cs->intervalTop = (top < 0) ? 0 : top;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setSectionIntervalBottom
 * Signature: (IIF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionIntervalBottom
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jfloat bottom)
{
	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track(default_track_scene, track);
    if(!t) return;

    cs = get_track_section(t, section);
    if(!cs) return;

    // horizontal length on screen
    float h_length = (cs->orientation) ? cs->height : cs->width;
	cs->intervalBottom = (bottom > h_length) ? h_length : bottom;
}

/*
 * Class:     SceneGraph
 * Method:    getSectionGraphOffset
 * Signature: (II)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionGraphOffset
(JNIEnv * jenv, jclass jcls, jint track, jint section){

	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track);
    if(!t) return 0;

    cs = get_track_section(t,section);
    if(!cs) return 0;

	float cdpix, cdpiy;
	get_canvas_dpi(0,&cdpix,&cdpiy);

	return cs->graph_offset * CM_PER_INCH / cdpix;

}

/*
 * Class:     SceneGraph
 * Method:    setSectionGraphOffset
 * Signature: (IIF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionGraphOffset
(JNIEnv *jenv, jclass jcls, jint track, jint section, jfloat offset){
	
	// offset is cm scale

	TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track);
    if(!t) return;

    cs = get_track_section(t,section);
    if(!cs) return;

	float cdpix, cdpiy;
	get_canvas_dpi(0,&cdpix,&cdpiy);
	
	// graph_offset is pixel, so need to convert cm to pixel
	cs->graph_offset = offset * cdpix * INCH_PER_CM;
	
	// printf("graph offset is set: %f\n", cs->graph_offset);
}

/*
 * Class:     SceneGraph
 * Method:    getSectionSourceImage
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getSectionSourceImage
  (JNIEnv *jenv, jclass jcls, jint track, jint section){

    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track( default_track_scene, track);
    cs = get_track_section(t,section);
    
    if(!cs) return -1;

    return cs->src;

}
/*
 * Class:     SceneGraph
 * Method:    getSectionDPIX
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionDPIX
  (JNIEnv *jenv, jclass jcls, jint track, jint section){

    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs) return -1.0f;

    return cs->dpi_x;
}

/*
 * Class:     SceneGraph
 * Method:    getSectionDPIY
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionDPIY
  (JNIEnv *jenv, jclass jcls, jint track, jint section){

    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs) return -1.0f;

    return cs->dpi_y;
}

// private function for update CoreSection metadata // FIXME mmm...
void updateCoreSectionMeta(CoreSection *cs)
{
    // in cm
    float horiDPI = (cs->orientation) ? cs->dpi_y : cs->dpi_x;
    float vertDPI = (cs->orientation) ? cs->dpi_x : cs->dpi_y;

	cs->width  = (float)get_texset_src_width(cs->src) * CM_PER_INCH / horiDPI;
	cs->height = (float)get_texset_src_height(cs->src)* CM_PER_INCH / vertDPI;

	cs->intervalTop = 0;
	cs->intervalBottom = (cs->width > cs->height) ? cs->width : cs->height;
}

/*
 * Class:     SceneGraph
 * Method:    setSectionDPI
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionDPI
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jfloat dpix, 
   jfloat dpiy) {
    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs)
    {
        printf("ERROR: Track %d\tSection %d\n", (int)track, (int)section);
        return;
    }

    cs->dpi_x = dpix;
    cs->dpi_y = dpiy;

    updateCoreSectionMeta(cs);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setSectionOrientation
 * Signature: (IIZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionOrientation
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId, jboolean isPortrait)
{
    TrackSceneNode* tsn;
    CoreSection* cs;

    tsn = get_scene_track(default_track_scene, trackId);

    if(!tsn) return;
    cs = get_track_section(tsn, sectionId);

    if(!cs) return;

    cs->orientation = isPortrait;

    updateCoreSectionMeta(cs);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getSectionOrientation
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_getSectionOrientation
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId)
{
    TrackSceneNode* tsn;
    CoreSection* cs;

    tsn = get_scene_track(default_track_scene,trackId);
    if(!tsn) return 0.0f;

    cs  = get_track_section( tsn, sectionId);
    if(!cs) return 0.0f;

    return cs->orientation;
}

/*
 * Class:     SceneGraph
 * Method:    rotateSection
 * Signature: (IIF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_rotateSection
  (JNIEnv *jenv, jclass jcls, jint trackId, jint sectionId, jfloat angle)
{
    TrackSceneNode* tsn;
    CoreSection* cs;

    // angle = float(int(floor(angle / 90.0f) * 90.0f) % 360);
    tsn = get_scene_track(default_track_scene,trackId);

    if(!tsn) return;
    cs = get_track_section(tsn, sectionId);

    if(!cs) return;
    cs->rotangle = angle;
}

/*
 * Class:     SceneGraph
 * Method:    getSectionRotation
 * Signature: (II)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionRotation
  (JNIEnv *jenv, jclass jcls, jint trackId, jint sectionId){

    TrackSceneNode* tsn;
    CoreSection* cs;

    tsn = get_scene_track(default_track_scene,trackId);
    if(!tsn) return 0.0f;
    cs  = get_track_section( tsn, sectionId);
    if(!cs) return 0.0f;

    return cs->rotangle;
    
}

/*
 * Class:     SceneGraph
 * Method:    getImageIdForSection
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getImageIdForSection
  (JNIEnv *jenv, jclass jcls, jint track, jint section){
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs) return -1;

    return cs->src;

}

/*
 * Class:     SceneGraph
 * Method:    getSectionXPos
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionXPos
  (JNIEnv *jenv, jclass jcls, jint track, jint section){

    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs) return -1.0f;

    return cs->px;
}

/*
 * Class:     SceneGraph
 * Method:    getSectionYPos
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getSectionYPos
  (JNIEnv *jenv, jclass jcls, jint track, jint section){

    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs) return -1.0f;

    return cs->py;
}

 /*
 * Class:     SceneGraph
 * Method:    bringSectionToFront
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_bringSectionToFront
  (JNIEnv *jenv, jclass jcls, jint trackId, jint sectionId)
{
    TrackSceneNode* tsn;
    tsn = get_scene_track( default_track_scene, trackId);
    if( !tsn ) return;

    if(!is_section_model( tsn, sectionId)) return;

    bring_model_front( tsn, sectionId );
}

 /*
 * Class:     SceneGraph
 * Method:    pushSectionToEnd
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_pushSectionToEnd
  (JNIEnv *jenv, jclass jcls, jint trackId, jint sectionId)
{
    TrackSceneNode* tsn;
    tsn = get_scene_track(default_track_scene, trackId);
#ifdef DEBUG
    printf("pushing section to end called for track %d, sec %d\n",
           trackId, sectionId);
#endif
    if( !tsn ) return;
    if( !is_section_model( tsn, sectionId )) return;

    push_section_to_end( tsn, sectionId );
}

/*
 * Class:     SceneGraph
 * Method:    setSectionMovable
 * Signature: (IIB)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionMovable
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jboolean flag)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs) return;
  
    cs->movable = flag;
}

/*
 * Class:     SceneGraph
 * Method:    isSectionMovable
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_isSectionMovable
  (JNIEnv *jenv, jclass jcls, jint track, jint section)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs) return false;

    return cs->movable;
}

/*
 * Class:     SceneGraph
 * Method:    setSectionGraphMovable
 * Signature: (IIB)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setSectionGraphMovable
   (JNIEnv *jenv, jclass jcls, jint track, jint section, jboolean flag)
{
	TrackSceneNode* t;
	CoreSection* cs;
	t  = get_scene_track(default_track_scene, track);
	cs = get_track_section(t,section);
	if(!cs) return;
	
	cs->graphMovable = flag;
}

/*
 * Class:     SceneGraph
 * Method:    isSectionGraphMovable
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_isSectionGraphMovable
   (JNIEnv *jenv, jclass jcls, jint track, jint section)
{
	TrackSceneNode* t;
	CoreSection* cs;
	t  = get_scene_track(default_track_scene, track);
	cs = get_track_section(t,section);
	if(!cs) return false;
	
	return cs->graphMovable;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    markTrackMovable
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTrackMovable
  (JNIEnv * jenv, jclass jcls, jint trackId, jboolean flag)
{
    TrackSceneNode* t;
    t = get_scene_track(default_track_scene, trackId);

    if(t) t->movable = flag;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    isTrackMovable
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_isTrackMovable
  (JNIEnv * jenv, jclass jcls, jint trackId)
{
    TrackSceneNode* t = get_scene_track(default_track_scene, trackId);
    if(t) return t->movable;
    else true;
}

/*
 * Class:     SceneGraph
 * Method:    addDataset
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_addDataset
  (JNIEnv *jenv, jclass jcls, jstring name){

    char* nbuf;
    int   i;

    printf("\n--- addDataset called ----\n");

    i = jenv->GetStringLength(name);
    nbuf = (char *) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(name, 0, i, nbuf);

#ifdef DEBUG
    printf("Converting DatasetName to UTF-8\n");
    printf("String length %d\n", i);
    printf("String in UTF-8 %s\n", nbuf);
#endif
    
    i = create_dataset(nbuf);

    free(nbuf);
    
    return i;

}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    deleteDataset
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_deleteDataset
  (JNIEnv * jenv, jclass jcls, jint datasetId)
{
    printf("\n--- free native dataset ---\n");

    if(is_dataset(datasetId))
    {
        free_dataset(datasetId);
    }
}


/*
 * Class:     SceneGraph
 * Method:    setDatasetURL
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setDatasetURL
  (JNIEnv *jenv, jclass jcls, jint set, jstring url){

    char* nbuf;
    int   i;

    if(!is_dataset(set)) return;
    i = jenv->GetStringLength(url);
    nbuf = (char *) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(url, 0, i, nbuf);
    
    set_dataset_url(set,nbuf);

    free(nbuf);
}

/*
 * Class:     SceneGraph
 * Method:    getDatasetURL
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getDatasetURL
  (JNIEnv *jenv, jclass jcls, jint set){
    return jenv->NewStringUTF(get_dataset_url(set));
}

/*
 * Class:     SceneGraph
 * Method:    getDatasetName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getDatasetName
  (JNIEnv *jenv, jclass jcls, jint set){
    return jenv->NewStringUTF(get_dataset_name(set));
}


/*
 * Class:     SceneGraph
 * Method:    addTable
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_addTable
  (JNIEnv * jenv, jclass jcls, jint jset, jstring jName){

    char* nbuf;
    int i;

    if(!is_dataset(jset)) return -1;

    i = jenv->GetStringLength(jName);
    nbuf = (char *) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(jName, 0, i, nbuf);

    i = create_table(jset,nbuf);

    free(nbuf);
    
    return i;

}

/*
 * Class:     SceneGraph
 * Method:    setTableHeight
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTableHeight
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable, jint jheight)
{

    int h = set_table_height(jset,jtable,jheight);

#ifdef DEBUG
    printf("\n--- setTableHeight: [%d] ---\n", h);
#endif

}

/*
 * Class:     SceneGraph
 * Method:    setTableFieldCount
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTableFieldCount
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable, jint jcount)
{

    int c = set_table_field_count(jset,jtable,jcount);
#ifdef DEBUG
    printf("\n--- setTableFieldCount: [%d] ---\n", c);
#endif

}


/*
 * Class:     SceneGraph
 * Method:    addNewFieldToTable
 * Signature: (IILjava/lang/String;)B
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_addNewFieldToTable
  (JNIEnv *jenv, jclass jcls, jint set, jint table, jstring label)
{
    int i;
    char* nbuf;
    i = jenv->GetStringLength(label);
    if( i <= 1 ) return false;

    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(label,0,i,nbuf);

    bool result = add_new_field_to_table(set,table,nbuf);
    free(nbuf);

    return result; 
}

/*
 * Class:     SceneGraph
 * Method:    setTableHeightAndFileCount
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTableHeightAndFieldCount
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable,
  jint jheight, jint jcount)
{
#ifdef DEBUG
    printf("\n--- Enter setTableHeightAndFieldCount ---\n");
#endif

    int h, c;
    h = set_table_height(jset,jtable,jheight);
    c = set_table_field_count(jset,jtable,jcount);

#ifdef DEBUG
    printf("\n--- Set[%d], Table[%d] has [%d] rows and [%d] fields ---\n",
           jset, jtable, h, c);
    printf("--- Table Init done ---\n");
#endif

}

/*
 * Class:     SceneGraph
 * Method:    setTableCell
 * Signature: (IIIIF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTableCell
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable,
  jint jfield, jint jrow, jboolean jvalid, jfloat jdepth, jfloat jvalue)
{
#ifdef DEBUG
    printf("\n--- Put value[%4.4f] depth[%4.4f] Valid[%d] in row[%d] \
        field[%d] in table[%d] of dataset[%d] ---\n",
        jvalue, jdepth, jvalid, jrow, jfield, jtable, jset);
#endif

    set_table_cell(jset,jtable,jfield,jrow,jvalue);
    if( jvalid )
        set_table_cell_valid(jset,jtable,jfield,jrow,true);
    else
        set_table_cell_valid(jset,jtable,jfield,jrow,false);

    set_table_row_depth(jset,jtable,jrow,jdepth);
}

/*
 * Class:     SceneGraph
 * Method:    setFieldMinMax
 * Signature: (IIIFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setFieldMinMax
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable, jint jfield, 
   jfloat jmin, jfloat jmax)
{
#ifdef DEBUG
    printf("\n--- Setting dataset:%d, table:%d, field:%d minmax value",
           jset, jtable, jfield);
    printf(" to: %f, %f ---\n", jmin, jmax);
#endif

    set_field_min_max(jset, jtable, jfield, jmin, jmax);
}

/*
 * Class:     SceneGraph
 * Method:    getTableHeight
 * Signature: (II)V
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getTableHeight
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable){

    return get_table_height(jset,jtable);
}

/*
 * Class:     SceneGraph
 * Method:    getTableFields
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getTableFields
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable)
{
    return get_table_field_count(jset,jtable);
}

/*
 * Class:     SceneGraph
 * Method:    isTableCellValid
 * Signature: (IIII)I
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_isTableCellValid
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable, jint jfield, jint jrow)
{
    return is_table_cell_valid(jset,jtable,jfield,jrow);
}

/*
 * Class:     SceneGraph
 * Method:    getTableCell
 * Signature: (IIII)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getTableCell
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable, jint jfield, jint jrow)
{
    return get_table_cell(jset,jtable,jfield,jrow);
}

/*
 * Class:     SceneGraph
 * Method:    getTableCellDepth
 * Signature: (IIII)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getTableCellDepth
  (JNIEnv * jenv, jclass jcls, jint jset, jint jtable, jint jfield, jint jrow)
{
    return get_table_row_depth(jset,jtable,jrow);
}

/*
 * Class:     SceneGraph
 * Method:    getNumberOfTables
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getNumberOfTables
  (JNIEnv * jenv, jclass jcls, jint jset)
{
    return num_tables(jset);
}

/*
 * Class:     SceneGraph
 * Method:    getTableName
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getTableName
  (JNIEnv *jenv, jclass jcls, jint set, jint table)
{
    return jenv->NewStringUTF(get_table_name(set,table));
    
}

/*
 * Class:     SceneGraph
 * Method:    getFieldName
 * Signature: (III)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getFieldName
  (JNIEnv *jenv, jclass jcls, jint set, jint table, jint field)
{
    return jenv->NewStringUTF(get_field_name(set,table,field));

}

/*
 * Class:     SceneGraph
 * Method:    setTableDepthUnitScale
 * Signature: (IIF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTableDepthUnitScale
(JNIEnv * jenv, jclass jcls, jint jdataset, jint jtable, jfloat jscale)
{
    if(!is_dataset(jdataset)) return;

    set_table_depthunitscale(jdataset, jtable, jscale);
}

/*
 * Class:     SceneGraph
 * Method:    getTableDepthUnitScale
 * Signature: (II)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getTableDepthUnitScale
  (JNIEnv * jenv, jclass jcls, jint jdataset, jint jtable)
{
    if(!is_dataset(jdataset)) return -1.0;

    return get_table_depthunitscale(jdataset, jtable);
}

/*
 * Class:     SceneGraph
 * Method:    addLineGraphToSection
 * Signature: (IIIII)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_addLineGraphToSection
  (JNIEnv * jenv, jclass jcls, jint jtrack, jint jsection, jint jdataset,
  jint jtable, jint jfield)
{

    int gid = add_line_graph_to_section(jtrack, jsection, 
                                        jdataset, jtable, jfield);

    // if we have a line graph, move the graph's y position to be
    // on top of the other graphs
    if( gid != -1) move_graph_to_top(gid);

    if( gid == -1 )
    {
        // fixme
        printf("\nERROR: addLineGraph Fail!\n");
        printf("trackId: %d, sectionId: %d, datasetId: %d, tableId: %d, fieldId: %d\n",
                (int)jtrack, (int)jsection, (int)jdataset, (int)jtable, (int)jfield);
    }

    return gid;
}

/*
 * Class:     SceneGraph
 * Method:    removeLineGraphFromSection
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_removeLineGraphFromSection
  (JNIEnv * jenv, jclass jcls, jint graphid)
{
    int gid = remove_line_graph_from_section(graphid);

    if( gid == -1 )
    {
        printf("\nERROR: removeLineGraph Fail! graphId: %d\n", (int)graphid);
    }

    return gid;
}

/*
 * Class:     SceneGraph
 * Method:    setLineGraphColor
 * Signature: (IIIFFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setLineGraphColor
  (JNIEnv * jenv, jclass jcls, jint graphid, jfloat jr, jfloat jg, jfloat jb)
{
    set_line_graph_color(graphid, jr, jg, jb);    
}

/*
 * Class:     SceneGraph
 * Method:    setLineGraphType
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setLineGraphType
(JNIEnv *jenv, jclass jcls, jint graphid, jint type)
{
	set_line_graph_type(graphid, type);
}

/*
 * Class:     SceneGraph
 * Method:    setLineGraphRange
 * Signature: (IIIFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setLineGraphRange
  (JNIEnv * jenv, jclass jcls, jint graphid, jfloat jmin, jfloat jmax)
{
    set_line_graph_range(graphid, jmin, jmax);
}
	
/*
 * Class:     SceneGraph
 * Method:    setLineGraphExcludeRange
 * Signature: (IIIFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setLineGraphExcludeRange
(JNIEnv * jenv, jclass jcls, jint graphid, jfloat jmin, jfloat jmax)
{
	set_line_graph_exclude_range(graphid, jmin, jmax);
}
	
/*
 * Class:     SceneGraph
 * Method:    setLineGraphExcludeStyle
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setLineGraphExcludeStyle
(JNIEnv * jenv, jclass jcls, jint graphid, jint jstyle)
{
	set_line_graph_exclude_style(graphid, jstyle);
}

/*
 * Class:     SceneGraph
 * Method:    setLineGraphLabel
 * Signature: (IIILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setLineGraphLabel
  (JNIEnv * jenv, jclass jcls, jint graphid,  jstring jlabel)
{

    char* nbuf;
    int   i;

    i = jenv->GetStringLength(jlabel);
    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(jlabel, 0, i, nbuf);
    set_line_graph_label(graphid, nbuf);

    if( nbuf != NULL )
    {
        free(nbuf);
    }
}

/*
 * Class:     SceneGraph
 * Method:    isLineGraphShown
 * Signature: (III)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_isLineGraphShown
  (JNIEnv * jenv, jclass jcls, jint graphid)
{
    return is_line_graph_shown(graphid);
}

/*
 * Class:     SceneGraph
 * Method:    getNumGraphsForSection
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getNumGraphsForSection
  (JNIEnv *jenv, jclass jcls, jint track, jint section)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs) return -1;
    return cs->graphvec.size();
}

/*
 * Class:     SceneGraph
 * Method:    getGraphID
 * Signature: (IIIII)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getGraphID
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint dataset,
   jint table, jint field)
{
    return locate_graph(track,section,dataset,table,field);
}

// set/get whether to collapse the graphs
/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setGraphsCollapse
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setGraphsCollapse
  (JNIEnv * jenv, jclass jcls, jboolean isCollapse)
{
    setCollapse(isCollapse);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getGraphsCollapse
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_getGraphsCollapse
  (JNIEnv * jenv, jclass jcls)
{
    return ifCollapse();
}

/*
 * Class:     SceneGraph
 * Method:    getGraphIDFromSectionSlot
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getGraphIDFromSectionSlot
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint slot)
{
    TrackSceneNode *tsn;
    CoreSection *cs;

    tsn = get_scene_track(default_track_scene, track);
    cs  = get_track_section( tsn, section);

    if( cs == NULL ) return -1;

    return get_graph_from_section_slot(cs,slot);
}

/*
 * Class:     SceneGraph
 * Method:    getDatasetReference
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getDatasetReference
  (JNIEnv * jenv, jclass jcls, jint gid)
{
    return get_data_set_index(gid);
}

/*
 * Class:     SceneGraph
 * Method:    getTableReference
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getTableReference
  (JNIEnv * jenv, jclass jcls, jint gid)
{
    return get_table_index(gid);
}

/*
 * Class:     SceneGraph
 * Method:    getFieldReference
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getFieldReference
  (JNIEnv * jenv, jclass jcls, jint gid)
{
    return get_field_index(gid);
}

/*
 * Class:     SceneGraph
 * Method:    getTrackReference
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getTrackReference
  (JNIEnv * jenv, jclass jcls, jint gid)
{
    return get_track_index(gid);
}

/*
 * Class:     SceneGraph
 * Method:    getSectionReference
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getSectionReference
  (JNIEnv * jenv, jclass jcls, jint gid)
{
    return get_section_index(gid);
}
/*
 * Class:     SceneGraph
 * Method:    getGraphMax
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getGraphMax
  (JNIEnv * jenv, jclass jcls, jint gid){
    return get_max(gid);
}

/*
 * Class:     SceneGraph
 * Method:    getGraphMin
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getGraphMin
  (JNIEnv *jenv, jclass jcls, jint graphid){
    return get_min(graphid);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getGraphOrigMax
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getGraphOrigMax
  (JNIEnv * jenv, jclass jcls, jint graphid)
{
    return get_graph_orig_max(graphid);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getGraphOrigMin
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getGraphOrigMin
  (JNIEnv * jenv, jclass jcls, jint graphid)
{
    return get_graph_orig_min(graphid);  
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getGraphExcludeMin
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getGraphExcludeMin
(JNIEnv * jenv, jclass jcls, jint graphid)
{
	return get_graph_exclude_min(graphid);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getGraphExcludeMax
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getGraphExcludeMax
(JNIEnv * jenv, jclass jcls, jint graphid)
{
	return get_graph_exclude_max(graphid);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getGraphExcludeStyle
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getGraphExcludeStyle
(JNIEnv * jenv, jclass jcls, jint graphid)
{
	return get_graph_exclude_style(graphid);
}
	
	
/*
 * Class:     SceneGraph
 * Method:    getGraphSlot
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getGraphSlot
  (JNIEnv *jenv, jclass jcls, jint graphId){
    return get_graph_slot(graphId);
}

/*
 * Class:     SceneGraph
 * Method:    getLineGraphColorComponent
 * Signature: (IIII)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getLineGraphColorComponent
  (JNIEnv * jenv, jclass jcls, jint graphid, jint jcomponent)
{
    return get_line_graph_color_component( graphid, jcomponent);
}

/*
 * Class:     SceneGraph
 * Method:    getLineGraphType
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getLineGraphType
  (JNIEnv * jenv, jclass jcls, jint graphid)
{
	return get_line_graph_type( graphid );
}
	
/*
 * Class:     SceneGraph
 * Method:    findGraph
 * Signature: (I)II
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_findGraphByField
  (JNIEnv * jenv, jclass jcls, jint datasetid, jint fieldid )
{
	return find_graph_by_field( datasetid, fieldid );
}

/*
 * Class:     SceneGraph
 * Method:    pickForTrack
 * Signature: (IFF)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_pickForTrack
  (JNIEnv *jenv, jclass jcls, jint canvas, jfloat x, jfloat y){

    // go through zorder of scene's tracks from front to back
    int zlen, *order;
    int i;
    TrackSceneNode* tsn;
    float cdpix, cdpiy;
    
    if(!is_canvas(canvas)) return -1;
    get_canvas_dpi(canvas,&cdpix,&cdpiy);

    zlen = get_scene_track_zorder_length(default_track_scene);
    if( zlen <= 0 ) return -1;

#ifdef DEBUG
    printf("Going through zorder of length %d\n", zlen);
#endif

    order = (int*) malloc( sizeof(int) * zlen);
    get_scene_track_zorder( default_track_scene, order);

    for( i = 0; i < zlen; ++i)
    {
#ifdef DEBUG
        printf("Checking if %d is a track in track scene\n", order[i]);
#endif
        if(!is_track(default_track_scene, order[i])) continue;

        tsn = get_scene_track( default_track_scene, order[i] );
        
        // bounding box intersection test
        printf("Comparing %f, %f to bound (%f, %f) to (%f, %f)\n",
               x, y, tsn->px, tsn->py, tsn->px + (tsn->w * cdpix),
               tsn->py + (tsn->h * cdpiy));

        if( x < tsn->px ) continue;
        if( y < tsn->py ) continue;
        if( x > tsn->px + (tsn->w * cdpix) ) continue;
        if( y > tsn->py + (tsn->h * cdpiy) ) continue;

        // found it!
        free(order);
        return order[i];
    }
    
    // none found

    free(order);
    return -1;
}

/*
 * Class:     SceneGraph
 * Method:    pickForSection
 * Signature: (IIFF)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_pickForSection
  (JNIEnv *jenv, jclass jcls, jint canvas, jint track, jfloat x, jfloat y){

    int zlen, *order;
    int i;
    TrackSceneNode* tsn;
    CoreSection* cs;
    float cdpix, cdpiy;

    if(!is_canvas(canvas)) return -1;
    get_canvas_dpi(canvas,&cdpix,&cdpiy);

    if(!is_track( default_track_scene, track)) return -1;

    tsn  = get_scene_track( default_track_scene, track);
    zlen = get_track_section_zorder_length(tsn);
    if( zlen <= 0 ) return -1;

    x   -= tsn->px;
    y   -= tsn->py;

    order = (int*) malloc( sizeof(int) * zlen);

    get_track_section_zorder(tsn,order);

    // bounding box test
    for( i = 0; i < zlen; ++i)
    {
        float w, h;

        if(!is_section_model(tsn,order[i])) continue;

        cs = get_track_section(tsn,order[i]);
        w  = get_texset_src_width(cs->src);
        h  = get_texset_src_height(cs->src);
        w  /= cs->dpi_x;
        h  /= cs->dpi_y;
        w  *= cdpix;
        h  *= cdpiy;

        if(cs->orientation == PORTRAIT)
        {
            float t;
            t = w;
            w = h;
            h = t;
        }

        if( x < cs->px ) continue;
        if( x < cs->py ) continue;
        if( x > cs->px + w ) continue;
        if( y > cs->py + h ) continue;

        // found it
        free(order);
        return order[i];
    }

    // none found

    free(order);
    return -1;
}


/*
 * Class:     SceneGraph
 * Method:    enableVerticalLineFromGraph
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_enableVerticalLineFromGraph
  (JNIEnv *jenv, jclass jcls, jint graph)
{
    TrackSceneNode* tsn;
    CoreSection* cs;
    int csid;
    int tsnid;
    csid = get_section_index(graph);
    tsnid = get_track_index(graph);
    tsn = get_scene_track( default_track_scene, tsnid );
    cs = get_track_section( tsn, csid );
    if( cs == NULL ) return;
    set_section_draw_vert_line( cs, true, 0.0f);
//    printf("Enabled vert line!\n");
}

/*
 * Class:     SceneGraph
 * Method:    disableVerticalLineFromGraph
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_disableVerticalLineFromGraph
  (JNIEnv *jenv, jclass jcls, jint graph)
{
    TrackSceneNode* tsn;
    CoreSection* cs;
    int csid;
    int tsnid;
    csid = get_section_index(graph);
    tsnid = get_track_index(graph);
    tsn = get_scene_track( default_track_scene, tsnid );
    cs = get_track_section( tsn, csid );
    if( cs == NULL ) return;
    set_section_draw_vert_line( cs, false, -1.0f );
//    printf("Disabled vert line!!\n");
}

/*
 * Class:     SceneGraph
 * Method:    setVerticalLineFromGraphXPosition
 * Signature: (IF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setVerticalLineFromGraphXPosition
  (JNIEnv *jenv, jclass jcls, jint graph, jfloat x)
{
    TrackSceneNode* tsn;
    CoreSection* cs;
    int csid;
    int tsnid;
    csid = get_section_index(graph);
    tsnid = get_track_index(graph);
    tsn = get_scene_track( default_track_scene, tsnid );
    cs = get_track_section( tsn, csid );
    if( cs == NULL ) return;
    set_section_draw_vert_line( cs, true, x);
}

/*
 * Class:     SceneGraph
 * Method:    performPick
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_performPick
  (JNIEnv *jenv, jclass jcls, jint canvas, jfloat x, jfloat y) {

    perform_pick(canvas,x,y);
}

/*
 * Class:     SceneGraph
 * Method:    hitMarker
 * Signature: (IIIFF)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_hitMarker
  (JNIEnv * jenv, jclass jcls, jint canvas, jint track, jint section, jint marker,
  jfloat x, jfloat y) {
	
    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track );
    cs = get_track_section( t, section );
	
	bool hit = false;
	
    float cdpix, cdpiy;

    if(!get_horizontal_depth()) {
        float tVar = x;
        x = y;
        y = -tVar;
    }

	// adjust track position
    x = x - t->px;
    y = y - t->py;

    if(!is_canvas(canvas)) return hit;
    get_canvas_dpi(canvas,&cdpix,&cdpiy);

    if( cs && is_section_annotation(cs, marker) )
    {
        CoreAnnotation *ca = cs->annovec[marker];

		// find which handle mouse hit
		/// float sx, sy;
		/// sx = cdpix / DEFAULT_MARKER_DPI_X;
		/// sy = cdpiy / DEFAULT_MARKER_DPI_Y;
		float mx, my, mw, mh;

		switch (ca->m.type) {
			case CORE_POINT_MARKER:
				{
					// two possible handle
					// 1. annotation icon
					mx = ca->m.px;
					my = ca->m.py;
					mw = ca->m.w * getMarkerScale();
					mh = ca->m.h * getMarkerScale();
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.px);
						ca->m.hit1 = &(ca->m.py);
						break;
					}
					// 2: point
					mw *= 0.5f;
					mh *= 0.5f;
					mx = ca->m.depthX - mw/2.0f;
					my = ca->m.depthY - mh/2.0f;
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.depthX);
						ca->m.hit1 = &(ca->m.depthY);
						break;
					}
				}
				break;
			case CORE_SPAN_MARKER:
				{
					// 1. annotation icon
					mx = ca->m.px;
					my = ca->m.py;
					mw = ca->m.w * getMarkerScale();
					mh = ca->m.h * getMarkerScale();
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.px);
						ca->m.hit1 = &(ca->m.py);
						break;
					}
					// left span
					mw = ca->m.w * getMarkerScale() / 6.0f;
					mh = ca->m.w * getMarkerScale() / 6.0f;
					mx = ca->m.markerVt[0] - mw/2.0f;
					my = ca->m.markerVt[1] - mh/2.0f;
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.markerVt[0]);
						ca->m.hit1 = &(ca->m.markerVt[4]);	// dummy point since not moving y in span
						break;
					}
					// right span
					mx = ca->m.markerVt[2] - mw/2.0f;
					my = ca->m.markerVt[3] - mh/2.0f;
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.markerVt[2]);
						ca->m.hit1 = &(ca->m.markerVt[4]);	// dummy point since not moving y in span
						break;
					}

				}
				break;
			case CORE_OUTLINE_MARKER:
				{
					// 1. annotation icon
					mx = ca->m.px;
					my = ca->m.py;
					mw = ca->m.w * getMarkerScale();
					mh = ca->m.h * getMarkerScale();
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.px);
						ca->m.hit1 = &(ca->m.py);
						break;
					}

					// outline center
					mw = ca->m.w * getMarkerScale() / 4.0f;
					mh = ca->m.w * getMarkerScale() / 4.0f;
					mx = ca->m.depthX - mw/2.0f;
					my = ca->m.depthY - mh/2.0f;
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.depthX);
						ca->m.hit1 = &(ca->m.depthY);
						break;
					}
					// top handle
					mw = ca->m.w * getMarkerScale() / 6.0f;
					mh = ca->m.w * getMarkerScale() / 6.0f;
					mx = (ca->m.markerVt[0]+ca->m.markerVt[2])/2.0f - mw/2.0f;
					my = ca->m.markerVt[1] - mh/2.0f;
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.markerVt[4]);	// dummy point
						ca->m.hit1 = &(ca->m.markerVt[1]);
						break;
					}
					// bottom handle
					my = ca->m.markerVt[3] - mh/2.0f;
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.markerVt[4]);	// dummy point
						ca->m.hit1 = &(ca->m.markerVt[3]);	// change bottom y
						break;
					}
					// left handle
					mx = ca->m.markerVt[0] - mw/2.0f;
					my = (ca->m.markerVt[1] + ca->m.markerVt[3])/2.0f - mh/2.0f;
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.markerVt[0]);
						ca->m.hit1 = &(ca->m.markerVt[4]);	// dummy point
						break;
					}
					// right handle
					mx = ca->m.markerVt[2] - mw/2.0f;
					if( 	y >= my 
						 && y <= my + mh
						 && x >= mx + cs->px 
						 && x <= mx + mw + cs->px)
					{
                        hit = true;
						ca->m.hit0 = &(ca->m.markerVt[2]);
						ca->m.hit1 = &(ca->m.markerVt[4]);	// dummy point
						break;
					}

				}
				break;
		}

    }

	return hit;
}

/*
 * Class:     SceneGraph
 * Method:    manipulateMarker
 * Signature: (IIIFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_manipulateMarker
  (JNIEnv *jenv, jclass jcls, jint canvas, jint track, jint section, jint marker,
  jfloat dx, jfloat dy)
{
    if(!get_horizontal_depth()) {
        float tVar = dx;
        dx = dy;
        dy = -tVar;
    }

    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track );
    cs = get_track_section( t, section );

    if( cs && is_section_annotation(cs, marker) )
    {
		CoreAnnotation *ca = cs->annovec[marker];

		// first need to chech whether new result is within the section.
		// otherwise ingnore this manipulation
		// adjust track position
		/*
		float cdpix, cdpiy;
		get_canvas_dpi(canvas,&cdpix,&cdpiy);
		
		
        // check the angle
		float w;
        if(cs->orientation == PORTRAIT)
        {
			w = get_texset_src_height(cs->src);
			w  /= cs->dpi_y;
			w  *= cdpiy;
        }
		else {
			w  = get_texset_src_width(cs->src);
			w  /= cs->dpi_x;
			w  *= cdpix;
		}
		
		float result = *(ca->m.hit0) + dx;

		if (result < 0 || result > w)
			return;
	    */
		
		// change marker handle position
		*(ca->m.hit0) += dx;
		*(ca->m.hit1) += dy;

		// need some more work...
		// when move span, need to change depthX
		// when move outline, need to sync other vtx and depth
		switch (ca->m.type) {
			case CORE_SPAN_MARKER:
				// update depthX. no matter which handle moves
				ca->m.depthX = (ca->m.markerVt[0] + ca->m.markerVt[2]) / 2.0f;
				break;
			case CORE_OUTLINE_MARKER:
				{
					// additional update
					// when center moves
					if (ca->m.hit0 == &(ca->m.depthX) )
					{
						// update markerVt
						ca->m.markerVt[0] += dx;
						ca->m.markerVt[2] += dx;
						ca->m.markerVt[1] += dy;
						ca->m.markerVt[3] += dy;
					}
					// when one of outlines moves
					else
					{
						// update depthX, depthY
						ca->m.depthX = (ca->m.markerVt[0] + ca->m.markerVt[2])/2.0f;
						ca->m.depthY = (ca->m.markerVt[1] + ca->m.markerVt[3])/2.0f;
					}
				}
				break;
		}
	}
}

/*
 * Class:     SceneGraph
 * Method:    accesPickedTrack
 * Signature: (V)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_accessPickedTrack
  (JNIEnv *jenv, jclass jcls) {

    return PickedTrack;
}

/*
 * Class:     SceneGraph
 * Method:    accesPickedSection
 * Signature: (V)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_accessPickedSection
  (JNIEnv *jenv, jclass jcls) {
    return PickedSection;
}

/*
 * Class:     SceneGraph
 * Method:    accesPickedGraph
 * Signature: (V)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_accessPickedGraph
  (JNIEnv *jenv, jclass jcls) {
    return PickedGraph;
}

/*
 * Class:     SceneGraph
 * Method:    accesPickedMarker
 * Signature: (V)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_accessPickedMarker
  (JNIEnv *jenv, jclass jcls) {
    return PickedMarker;
}


/*
 * Class:     SceneGraph
 * Method:    accessPickedFreeDraw
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_accessPickedFreeDraw
  (JNIEnv *jenv, jclass jcls) {
    return PickedFreeDraw;
}

/*
 * Class:     SceneGraph
 * Method:    createCoreSectionMarker
 * Signature: (IIIF)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_createCoreSectionMarker
  (JNIEnv *jenv, jclass jcls, jint track, jint section, 
	jint group, jint type, jfloat xpos, jfloat ypos)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);

    if(!cs) return -1;

    float x, y;
    x = xpos - (t->px + cs->px);
	y = ypos - (t->py + cs->py);

    printf("\n---- createCoreSectionMarker: %f ----\n", x);
    return create_section_annotation(cs,group,type,x,y);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    removeCoreSectionMarker
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_removeCoreSectionMarker
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track(default_track_scene, track);
    if(!t) return;

    cs = get_track_section(t,section);
    if(!cs) return;

    if(!is_section_annotation(cs,marker)) return;

    free_section_annotation(cs, marker);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    isCoreSectionMarker
 * Signature: (III)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_isCoreSectionMarker
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track(default_track_scene, track);
    if(!t) return false;

    cs = get_track_section(t,section);
    if(!cs) return false;


    return is_section_annotation(cs, marker);
}

/*
 * Class:     SceneGraph
 * Method:    setCoreSectionMarkerURL
 * Signature: (IIILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCoreSectionMarkerURL
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker, 
   jstring url){

    TrackSceneNode* t;
    CoreSection* cs;
    char* nbuf;
    int   i;

    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);

    if(!cs) return;
    if(!is_section_annotation(cs,marker)) return;

    i = jenv->GetStringLength(url);
    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(url, 0, i, nbuf);

    set_marker_url( &(cs->annovec[marker]->m), nbuf);

    free(nbuf);
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerURL
 * Signature: (III)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerURL
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker) {

    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if( cs && is_section_annotation(cs,marker))
    {
        return jenv->NewStringUTF(get_marker_url(&(cs->annovec[marker]->m)));
    }
    else
    {
        return jenv->NewStringUTF(NULL);
    }
}

/*
 * Class:     SceneGraph
 * Method:    getNumCoreSectionMarkers
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getNumCoreSectionMarkers
  (JNIEnv *jenv, jclass jcls, jint track, jint section)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if(!cs) return 0;
    return cs->annovec.size();
}

/*
 * Class:     SceneGraph
 * Method:    setCoreSectionMarkerLocal
 * Signature: (IIILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCoreSectionMarkerLocal
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker, 
   jstring filename)
{
    TrackSceneNode* t;
    CoreSection* cs;
    int i;
    char* nbuf;

    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    i = jenv->GetStringLength(filename);
    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(filename, 0, i, nbuf);

    if( cs && is_section_annotation(cs,marker))
    {
        set_marker_local_file( &(cs->annovec[marker]->m), nbuf);
    }

    free(nbuf);
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerLocal
 * Signature: (III)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerLocal
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    if( cs && is_section_annotation(cs,marker))
    {
        return jenv->NewStringUTF(get_marker_local_file(
                                      &(cs->annovec[marker]->m)));
    }
    else
    {
        return jenv->NewStringUTF(NULL);
    }
}

/*
 * Class:     SceneGraph
 * Method:    setCoreSectionMarkerVertex
 * Signature: (IIIFFFFFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCoreSectionMarkerVertex
(JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker, 
 jfloat ax, jfloat ay, jfloat v0, jfloat v1, jfloat v2, jfloat v3)
{
    // Notice: floats here are relative to the beginning of the core section
    TrackSceneNode* t;
    CoreSection* cs;
    int i;
    char* nbuf;

    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);

	if( cs && is_section_annotation(cs,marker)) {
        set_marker_vertex( &(cs->annovec[marker]->m), 
							ax, ay, v0, v1, v2, v3);
	}
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerXPos
 * Signature: (III)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerXPos
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    return get_section_annotation_x(cs,marker);
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerYPos
 * Signature: (III)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerYPos
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    return get_section_annotation_y(cs,marker);
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerIconXPos
 * Signature: (III)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerIconXPos
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    return get_section_annotation_icon_x(cs,marker);
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerIconYPos
 * Signature: (III)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerIconYPos
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    return get_section_annotation_icon_y(cs,marker);
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerV0
 * Signature: (III)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerV0
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    return get_section_annotation_vt0(cs,marker);
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerV1
 * Signature: (III)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerV1
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    return get_section_annotation_vt1(cs,marker);
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerV2
 * Signature: (III)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerV2
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    return get_section_annotation_vt2(cs,marker);
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerV3
 * Signature: (III)F
 */
JNIEXPORT jfloat JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerV3
  (JNIEnv *jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    return get_section_annotation_vt3(cs,marker);
}

/*
 * Class:     SceneGraph
 * Method:    setCoreSectionMarkerVisibility
 * Signature: (IIIZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCoreSectionMarkerVisibility
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker, 
   jboolean vis)
{

    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);

    if( cs && is_section_annotation(cs,marker))
    {
        set_marker_visibility( &(cs->annovec[marker]->m), vis );
    }
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerVisibility
 * Signature: (III)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerVisibility
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker)
{

    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track );
    cs = get_track_section( t, section );

    if( cs && is_section_annotation(cs, marker) )
    {
        return get_marker_visibility( &(cs->annovec[marker]->m) );
    }
    else
    {
        return true;
    } 
}

/*
 * Class:     SceneGraph
 * Method:    setCoreSectionMarkerGroup
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCoreSectionMarkerGroup
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker, 
   jint groupid)
{

    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track );
    cs = get_track_section( t, section );

    if( cs && is_section_annotation(cs, marker) )
    {
        set_marker_group( &(cs->annovec[marker]->m), groupid );
    }
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerGroup
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerGroup
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker)
{

    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track );
    cs = get_track_section( t, section );

    if( cs && is_section_annotation(cs, marker) )
    {
        return get_marker_group( &(cs->annovec[marker]->m) );
    }
    else
    {
        return -1;
    }
}

/*
 * Class:     SceneGraph
 * Method:    setCoreSectionMarkerType
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCoreSectionMarkerType
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker, 
   jint typeId)
{
    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track );
    cs = get_track_section( t, section );

    if( cs && is_section_annotation(cs, marker) )
    {
        set_marker_type( &(cs->annovec[marker]->m), typeId );
    }
}

/*
 * Class:     SceneGraph
 * Method:    getCoreSectionMarkerType
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerType
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker)
{

    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track );
    cs = get_track_section( t, section );

    if( cs && is_section_annotation(cs, marker) )
    {
        return get_marker_type( &(cs->annovec[marker]->m) );
    }
    else
    {
        return -1;
    }
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setCoreSectionMarkerText
 * Signature: (IIILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCoreSectionMarkerText
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker, jstring aText)
{
    TrackSceneNode* t;
    CoreSection* cs;
    char* nbuf;
    int   i;

    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);

    if(!cs) return;
    if(!is_section_annotation(cs,marker)) return;

    i = jenv->GetStringLength(aText);
    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(aText, 0, i, nbuf);

    set_marker_label( &(cs->annovec[marker]->m), nbuf);
    free(nbuf);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setCoreSectionMarkerRelationText
 * Signature: (IIILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCoreSectionMarkerRelationText
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker, jstring aText)
{
    TrackSceneNode* t;
    CoreSection* cs;
    char* nbuf;
    int   i;

    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);

    if(!cs) return;
    if(!is_section_annotation(cs,marker)) return;

    i = jenv->GetStringLength(aText);
    nbuf = (char*) malloc(i*sizeof(char)+1);
    jenv->GetStringUTFRegion(aText, 0, i, nbuf);

    set_marker_relation_label( &(cs->annovec[marker]->m), nbuf);
    free(nbuf);    
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getCoreSectionMarkerText
 * Signature: (III)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerText
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);

    if( cs && is_section_annotation(cs,marker))
    {
        return jenv->NewStringUTF(get_marker_label(&(cs->annovec[marker]->m)));
    }
    else
    {
        return jenv->NewStringUTF(NULL);
    }
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getCoreSectionMarkerRelationText
 * Signature: (III)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_corelyzer_graphics_SceneGraph_getCoreSectionMarkerRelationText
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker)
{
    TrackSceneNode* t;
    CoreSection* cs;
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);

    if( cs && is_section_annotation(cs,marker))
    {
        return jenv->NewStringUTF(get_marker_relation_label(&(cs->annovec[marker]->m)));
    }
    else
    {
        return jenv->NewStringUTF(NULL);
    }
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    isDuplicateAnnotation
 * Signature: (IIFFFFFFFFLjava/lang/String;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_isDuplicateAnnotation
  (JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId, jfloat x, jfloat y, jfloat v0, jfloat v1, jfloat v2, jfloat v3,
  jstring jUrlString, jstring jLocalString)
{
    TrackSceneNode* t;
    CoreSection* cs;
    char* urlString;
    char* localString;
    int   i;

    t  = get_scene_track(default_track_scene, trackId);
    cs = get_track_section(t, sectionId);

    if(!cs) { return false; }

    i = jenv->GetStringLength(jUrlString);
    urlString = (char*) malloc(i * sizeof(char) + 1);
    jenv->GetStringUTFRegion(jUrlString, 0, i, urlString);

    i = jenv->GetStringLength(jLocalString);
    localString = (char*) malloc(i * sizeof(char) + 1);
    jenv->GetStringUTFRegion(jLocalString, 0, i, localString);

    // loop through CoreSection's annotation to detect duplicates
    bool hasDuplicate = doesCoreSectionHasDuplicateWithInfo(cs, x, y, v0, v1, v2, v3, urlString, localString);

    free(urlString);
    free(localString);

    return hasDuplicate;
}

/*
 * Class:     SceneGraph
 * Method:    setCoreSectionMarkerFocus
 * Signature: (IIIZ)I
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCoreSectionMarkerFocus
  (JNIEnv * jenv, jclass jcls, jint track, jint section, jint marker, 
  jboolean flag)
{
    TrackSceneNode* t;
    CoreSection* cs;

    t  = get_scene_track( default_track_scene, track );
    cs = get_track_section( t, section );

    if( cs && is_section_annotation(cs, marker) )
    {
        set_marker_focus( &(cs->annovec[marker]->m), flag );
    }
}

/*
 * Class:     SceneGraph
 * Method:    createFreeDrawRectangle
 * Signature: (IFFFF)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_createFreeDrawRectangle
  (JNIEnv *jenv, jclass jcls, jint plugin, jfloat x, jfloat y, 
   jfloat w, jfloat h){
    
    int pfdr = create_free_draw_rectangle(plugin,-1,-1,x,y,w,h);
    attach_free_draw_to_scene(default_track_scene,pfdr);
    return pfdr;
}

/*
 * Class:     SceneGraph
 * Method:    createFreeDrawRectangleForTrack
 * Signature: (IIFFFF)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_createFreeDrawRectangleForTrack
  (JNIEnv *jenv, jclass jcls, jint plugin, jint track, 
   jfloat x, jfloat y, jfloat w, jfloat h)
{
    int pfdr = create_free_draw_rectangle(plugin,track,-1,x,y,w,h);
    attach_free_draw_to_track(get_scene_track(track),pfdr);
    return pfdr;
}

/*
 * Class:     SceneGraph
 * Method:    createFreeDrawRectangleForSection
 * Signature: (IIIFF)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_createFreeDrawRectangleForSection
  (JNIEnv *jenv, jclass jcls, jint plugin, jint track, jint section, 
   jfloat y, jfloat h){
    
    int pfdr_id;
    TrackSceneNode* t;
    CoreSection* cs;
    
    t  = get_scene_track(default_track_scene, track);
    cs = get_track_section(t,section);
    
    if( !cs ) return -1;

    pfdr_id = create_free_draw_rectangle(plugin,track,section,0,y,0,h);
    attach_free_draw_to_section(cs,pfdr_id);
    return pfdr_id;
}


/*
 * Class:     SceneGraph
 * Method:    repositionFreeDrawRectangle
 * Signature: (IFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_repositionFreeDrawRectangle
  (JNIEnv *jenv, jclass jcls, jint fdid, jfloat x, jfloat y)
{
    set_free_draw_x(fdid, x );
    set_free_draw_y(fdid, y ); 
}

/*
 * Class:     SceneGraph
 * Method:    moveFreeDrawRectangle
 * Signature: (IFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_moveFreeDrawRectangle
  (JNIEnv *jenv, jclass jcls, jint fdid, jfloat x, jfloat y) 
{
    if( !is_free_draw_rectangle(fdid)) return;
    x = x + get_free_draw_x(fdid);
    y = y + get_free_draw_y(fdid);
    set_free_draw_x(fdid,x);
    set_free_draw_y(fdid,y);
}


/*
 * Class:     SceneGraph
 * Method:    destroyFreeDrawRectangle
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_destroyFreeDrawRectangle
  (JNIEnv *jenv, jclass jcls, jint fdid)
{
    free_free_draw_rectangle(fdid);
}

/*
 * Class:     SceneGraph
 * Method:    markFreeDrawScaleIndependent
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_markFreeDrawScaleIndependent
  (JNIEnv *jenv, jclass jcls, jint fdid, jboolean flag) 
{
    set_free_draw_scale_independence(fdid,flag);
}

/*
 * Class:     SceneGraph
 * Method:    setFreeDrawVisiblity
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setFreeDrawVisiblity
  (JNIEnv *jenv, jclass jcls, jint fdid, jboolean flag)
{
    set_free_draw_visibility(fdid,flag);
}

/*
 * Class:     SceneGraph
 * Method:    getFreeDrawPluginID
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_corelyzer_graphics_SceneGraph_getFreeDrawPluginID
  (JNIEnv *jenv, jclass jcls, jint fdid)
{
    return get_free_draw_plugin(fdid);
}

#ifdef __cplusplus
}
#endif

//************************ END JNI FUNCTIONS ********************************//

//====================================================================
float get_scene_center_x()
{
    return center_x;
}

//====================================================================
float get_scene_center_y()
{
    return center_y;
}

//====================================================================
void translate_scene_center(float dx, float dy)
{
#ifdef DEBUG
    printf("Translating scene from %f, %f to %f, %f\n",
           center_x, center_y, center_x + dx, center_y + dy);
#endif

    /*
    bool isHorizontalDepth = get_horizontal_depth();
    if(isHorizontalDepth)
    {
        ;
    }
    else
    {
        float t = dx;
        dx = dy;
        dy = t;
    }
    */

    center_x += dx;
    center_y += dy;

    for( int i = 0; i < num_canvases(); ++i)
    {
        float cx, cy, cz;
        int camera;
        
        if(!is_canvas(i)) continue;
        camera = get_canvas_camera(i);
        if(!is_camera(camera)) continue;

        
        get_camera_position( camera, &cx, &cy, &cz);
        position_camera( camera, cx + dx, cy + dy, cz);

    }

}

//=======================================================================
void update_center_point()
{
    int i;
    float max_x = 0.0f, max_y = 0.0f;
    float min_x = 0.0f, min_y = 0.0f;

    for( i = 0; i < num_canvases(); ++i)
    {
        if(!is_canvas(i)) continue;
        float x, y, z, w, h;
        int camera;
        camera = get_canvas_camera(i);
        if(!is_camera(camera)) continue;

        get_camera_position( camera, &x, &y, &z);
        get_canvas_dimensions( i, &w, &h);

        if( x < min_x )     min_x = x;
        if( y < min_y )     min_y = y;
        if( x + w > max_x ) max_x = x + w;
        if( y + h > max_y ) max_y = y + h;
            
    }

    center_x = min_x + ( (max_x - min_x) * 0.5f);
    center_y = min_y + ( (max_y - min_y) * 0.5f); 

#ifdef DEBUG
    printf("Center Point %f, %f\n", center_x, center_y);
#endif
}

//=======================================================================
void perform_pick(int canvas, float _x, float _y)
{
    float x, y;

    if(get_horizontal_depth()) {
        x = _x;
        y = _y;
    } else { // rotate 90 degree to match the coordinates
        x =  _y;
        y = -_x;
    }

#ifdef DEBUG
    printf("Pick coord (_x, _y) = %.2f %.2f\n", _x, _y);
    printf("Pick coord (x, y)   = %.2f %.2f\n", x, y);
#endif

    prevPickedTrack   = PickedTrack;
    prevPickedSection = PickedSection;

    PickedTrack    = -1;
    PickedSection  = -1;
    PickedGraph    = -1;
    PickedMarker   = -1;
    PickedFreeDraw = -1;

    // go through zorder of scene's tracks from front to back
    int zlen, *order;
    int i,k,l;
    TrackSceneNode* tsn;
    float cdpix, cdpiy;
    
    if(!is_canvas(canvas)) return;
    get_canvas_dpi(canvas,&cdpix,&cdpiy);

	zlen = get_scene_track_zorder_length(default_track_scene);
    if( zlen <= 0 ) return;

    order = (int*) malloc( sizeof(int) * zlen);
    get_scene_track_zorder( default_track_scene, order);

    for( i = 0; i < zlen && PickedTrack == -1; ++i) // track
    {
        if(!is_track(default_track_scene, order[i])) continue;

        tsn = get_scene_track( default_track_scene, order[i] );

        // go through each section in track and determine they picked something
        // associated with the section (image, graph, marker?)
        int cs_zlen, *cs_order;
        float tx, ty;
        CoreSection* cs;

        cs_zlen = get_track_section_zorder_length(tsn);
		if( cs_zlen <= 0 ) continue;

        tx = x - tsn->px;
        ty = y - tsn->py;
        cs_order = (int*) malloc( sizeof(int) * cs_zlen);
        get_track_section_zorder(tsn,cs_order);

        // bounding box test
        for( k = 0; k < cs_zlen && PickedSection == -1; ++k) // section
        {
            float w, h;

            if(!is_section_model(tsn,cs_order[k])) continue;

            cs = get_track_section(tsn,cs_order[k]);

            float startPx = cs->px;
            if (cs->src == -1) // no image section
            {
                w = cs->width * INCH_PER_CM;   // inch
                w *= cdpix;                    // pixel in canvas
                h = cs->height * INCH_PER_CM;  // inch
                h *= cdpiy;                    // pixel in canvas

                if(cs->orientation == PORTRAIT)
                {
                    float t;
                    w = h;
                    h = t;
                }
            }
            else // image
            {
                w  = get_texset_src_width(cs->src);  // pixel in src width
                h  = get_texset_src_height(cs->src); // pixel in src height

                float intTop = (cs->intervalTop / 2.54) * cs->dpi_x;
                float intBot = (cs->intervalBottom / 2.54) * cs->dpi_x;
                float visibleW = intBot - intTop;

                // check the orientation
                if(cs->orientation == PORTRAIT)
                {
                    float t;
                    t = w;
                    w = h;
                    h = t;
                }

                w = visibleW;
                startPx = cs->px + (cdpix * intTop / cs->dpi_x);

                w  /= cs->dpi_x;                     // inch
                h  /= cs->dpi_y;                     // inch
                w  *= cdpix;                         // pixel in canvas
                h  *= cdpiy;                         // pixel in canvas
            }

            // check horizontal range
			// offset?
			if (cs->graph_offset == 0.0f)
			{
                if( tx < startPx ) continue;
                if( tx > (startPx + w) ) continue;
			}
			else
			{
				// adjust offset
                int min_var = startPx > startPx + cs->graph_offset ?
                              startPx + cs->graph_offset : startPx;

                int max_var = startPx + w > startPx + w + cs->graph_offset ?
                              startPx + w : startPx + w + cs->graph_offset;

                if (tx < min_var) continue;
                if (tx > max_var) continue;
			}

            // see if it's part of the actual image before checking 
            // things associated with section
            if( ty >= cs->py && ty <= cs->py + h)
            {
                // Un-highlight previous selected section
                TrackSceneNode *prev_track =
                    get_scene_track( default_track_scene, prevPickedTrack );
                    
                CoreSection *prev_section = (prev_track) ?
                    get_track_section(prev_track, prevPickedSection) : NULL;

                if(prev_section)
                {
                    prev_section->highlight = false;
                    set_crosshair_label(NULL);
                }

                // Highligh the picked section
                PickedSection = cs_order[k];
                cs->highlight = true;

                // set crosshair label
                // char *label = new char[128];
                int bufferSize = 128;
                char * label = (char *) malloc(sizeof(char) * bufferSize);
                {
                    sprintf(label, "section [%s]", get_section_name(cs));
                    set_crosshair_label(label);
                }
                free(label);

				// if marker locates inside section image?
				// TODO Check lithology markers
				for(l = 0;
					l < cs->annovec.size() && PickedMarker == -1; 
					l++)
                {
                    CoreAnnotation *ca = cs->annovec[l];
                    if(!ca) continue;
                    float mx, my, mw, mh;
                    mx = ca->m.px;
                    my = ca->m.py;
                    mw = ca->m.w * getMarkerScale();
                    mh = ca->m.h * getMarkerScale();

                    if( ty >= my
                        && ty <= my + mh
                        && tx >= mx + startPx
                        && tx <= mx + mw + startPx)
                    {
                        PickedMarker = l;
                    }
				}

                continue;
            }

            // within the horizontal bounds of a section.  
            // check for graphs
            if( ty < cs->py )
            {
                // traverse marker first and then graph
				
                // test marker
                for( l = 0;
                     l < cs->annovec.size() && PickedMarker == -1; 
                     l++)
                {
                    CoreAnnotation *ca = cs->annovec[l];
                    if(!ca) continue;
                    float mx, my, mw, mh;
                    mx = ca->m.px;
                    my = ca->m.py;
                    mw = ca->m.w * getMarkerScale();
                    mh = ca->m.h * getMarkerScale();

                    if( 	ty >= my
                                && ty <= my + mh
                                && tx >= mx + startPx
                                && tx <= mx + mw + startPx)
                    {
                        PickedMarker = l;
                    }
                }

                // test graph
                for( l = 0; 
                     l < cs->graphvec.size() && PickedGraph == -1 && 
                         PickedMarker == -1; 
                     l++)
                {
                    Box* b;
                    b = get_graph_box( cs, cs->graphvec[l] );
                    if(!b) continue;
                    // only need to check vertically
#ifdef DEBUG
                    printf("Checking graph %d\ny:%f h:%f vs. ty:%f\n",
                           l, b->y * cdpiy , b->h * cdpiy, ty);
#endif
                    if( ty >= (b->y * cdpiy) && ty <= ((b->y + b->h) * cdpiy))
                        PickedGraph = cs->graphvec[l];
                    delete b;
                }
#ifdef DEBUG
                printf("Picked Graph %d\n", PickedGraph );
#endif

                // test freedraw
                for( l = 0;
                     l < cs->freedrawvec.size() && PickedGraph == -1 && 
                         PickedMarker == -1 && PickedFreeDraw == -1;
                     l++)
                {
                    int fdid = cs->freedrawvec[l];

                    if( !is_free_draw_rectangle(fdid)) continue;

                    float mx, my, mw, mh;
                    mx = get_free_draw_x(fdid);
                    my = get_free_draw_y(fdid);
                    mw = get_free_draw_width(fdid);
                    mh = get_free_draw_height(fdid);

                    if( ty >= my
                        && ty <= my + mh
                        && tx >= mx + startPx
                        && tx <= mx + mw + startPx)
                    {
                        PickedFreeDraw = fdid;
                    }
				}
            }

            // check for Annotation Markers
            //if( ty > cs->py + h && PickedGraph == -1) 
            if( ty > cs->py + h && PickedMarker == -1) 
            {
                // TODO Check lithology markers
                for( l = 0;
                     l < cs->annovec.size() && PickedMarker == -1; 
                     l++)
                {
                    CoreAnnotation *ca = cs->annovec[l];
                    if(!ca) continue;
                    float mx, my, mw, mh;
                    mx = ca->m.px;
                    my = ca->m.py;
                    mw = ca->m.w * getMarkerScale();
                    mh = ca->m.h * getMarkerScale();

                    if( 	ty >= my
                                && ty <= my + mh
                                && tx >= mx + startPx
                                && tx <= mx + mw + startPx)
                    {
                        PickedMarker = l;
                    }
				}
			}

            if( PickedGraph != -1 || PickedMarker != -1 
                || PickedFreeDraw != -1)
            {
                // Un-highlight previous selected section
                TrackSceneNode *prev_track =
                    get_scene_track( default_track_scene, prevPickedTrack );

                CoreSection *prev_section = (prev_track) ?
                    get_track_section(prev_track, prevPickedSection) : NULL;

                if(prev_section)
                {
                    prev_section->highlight = false;
                    set_crosshair_label(NULL);                    
                }

                // Highligh the picked section
                PickedSection = cs_order[k];
                cs->highlight = true;

                // set crosshair label
                // char *label = new char[128];
                int bufferSize = 128;
                char * label = (char *) malloc(sizeof(char) * bufferSize);
                {
                    sprintf(label, "section [%s]", get_section_name(cs));
                    set_crosshair_label(label);
                }
                free(label);
            }

        } // done going through each section in track

        if( PickedSection != -1)
        {
            PickedTrack = order[i];
        }
        else 
        {
            // Didn't pick any section, unhighlight previous picked section
            TrackSceneNode *prev_track =
                get_scene_track( default_track_scene, prevPickedTrack );

            CoreSection *prev_section = (prev_track) ?
                get_track_section(prev_track, prevPickedSection) : NULL;

            if(prev_section)
            {
                prev_section->highlight = false;
                set_crosshair_label(NULL);                
            }

            // check free draws of tracks
            // convert tx, ty from dots to meters
            float fx, fy;
            fx = tx / cdpix * 2.54f / 100.0f;
            fy = ty / cdpiy * 2.54f / 100.0f;
            
            for( int l = 0;
                 l < tsn->freedrawvec.size() && PickedFreeDraw == -1;
                 l++)
            {
                int fdid = tsn->freedrawvec[l];
                
                if( !is_free_draw_rectangle(fdid)) continue;
                
                float mx, my, mw, mh;
                mx = get_free_draw_x(fdid);
                my = get_free_draw_y(fdid);
                mw = get_free_draw_width(fdid);
                mh = get_free_draw_height(fdid);

                if( ty >= my 
                    && ty <= my + mh
                    && tx >= mx 
                    && tx <= mx + mw )
                {
                    PickedFreeDraw = fdid;
                    PickedTrack = order[i];
                }
            }
            
        }

        free(cs_order);

    } // done going through each track

    free(order);
    
}

//=======================================================================
void scale_scene( float ds )
{
	// limit min/max zoom level
	if (ds == 0.0f)
	{
        ds = 1.0f;
    }
    
    float _allScale = allScale * ds;
	
    if(_allScale < MIN_SCALE || _allScale > MAX_SCALE)
    {
        return;
    }
    else
    {
        allScale = _allScale;
    }
	
    float cx, cy;
    float lx, ly, lz;
    float w, h;
    int id;
	
    cx = get_scene_center_x();
    cy = get_scene_center_y();
	
    // for each canvas, scale it's distance from the center point
    // and scale the dimensions of the canvas
    for( id = 0; id < num_canvases(); ++id)
    {
        int camera;
        if( !is_canvas(id) )
        {
            continue;
        }
		
        camera = get_canvas_camera(id);
		
        if( !is_camera(camera) )
        {
            continue;
        }
		
        get_camera_position(camera,&lx,&ly,&lz);
        get_canvas_dimensions(id,&w,&h);
		
        float dx, dy;
        dx = lx - cx;
        dy = ly - cy;
        dy *= ds;
        dx *= ds;
        
#ifdef DEBUG
        printf("Moving canvas %d, from %.2f, %.2f to %.2f, %.2f\n",
               id, lx, ly, dx + cx, dy + cy);
#endif
		
        position_camera(camera, dx + cx, dy + cy, lz);
        
        lx = (lx + w) - cx;
        ly = (ly + h) - cy;
        lx = lx * ds;
        ly = ly * ds;
		
#ifdef DEBUG
        printf("Resizing canvas %d, from %.2f x %.2f to %.2f x %.2f\n",
               id, w, h, lx - dx, ly - dy);
#endif
		
        set_canvas_dimensions(id, lx - dx, ly - dy);
    }
	
    // update marker scale
    // setMarkerScale(ds);
    // update graph scale
    // setGraphScale(ds);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setBackgroundColor
 * Signature: (FFF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setBackgroundColor
  (JNIEnv * jenv, jclass jcls, jfloat r, jfloat g, jfloat b)
{
    float aColor[3] = {r, g, b};
    set_bgcolor(aColor);    
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getBackgroundColor
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_corelyzer_graphics_SceneGraph_getBackgroundColor
  (JNIEnv * jenv, jclass jcls)
{
    float *aColor = get_bgcolor();

    jfloatArray retArray = jenv->NewFloatArray(3);
    jenv->SetFloatArrayRegion(retArray, 0, 3, aColor);
    
    return retArray;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setCrossHair
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setCrossHair
  (JNIEnv * jenv, jclass jcls, jboolean hasCrossHair)
{
    set_crosshair(hasCrossHair);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    hasCrossHair
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_hasCrossHair
  (JNIEnv * jenv, jclass jcls)
{
    return has_crosshair();  
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setDepthOrientation
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setDepthOrientation
  (JNIEnv * jenv, jclass jcls, jboolean orientation)
{
    set_horizontal_depth(orientation);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getDepthOrientation
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_getDepthOrientation
  (JNIEnv * jenv, jclass jcls)
{
    return get_horizontal_depth();
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setShowOrigin
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setShowOrigin
  (JNIEnv * jenv, jclass jcls, jboolean b)
{
    set_show_origin(b);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getShowOrigin
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_getShowOrigin
  (JNIEnv * jenv, jclass jcls)
{
    return is_show_origin();
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setShowSectionText
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setShowSectionText
  (JNIEnv * jenv, jclass jcls, jboolean b)
{
    set_show_section_text(b);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getShowSectionText
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_getShowSectionText
  (JNIEnv * jenv, jclass jcls)
{
    return is_show_section_text();
}

/* Duplicate a section(trackId, sectionId) to new track newTrackID */
int duplicateSection(int trackId, int sectionId, int newTrackId)
{
	TrackSceneNode * t;
	TrackSceneNode * newTrack;
    CoreSection * cs;

    t  = get_scene_track(default_track_scene, trackId);
    if(!t) return -1;

    newTrack = get_scene_track(default_track_scene, newTrackId);
    if(!t) return -1;

    cs = get_track_section(t, sectionId);
    if(!cs) return -1;

    // cs is ready, duplicate the CoreSection structure
    CoreSection* newSection = new CoreSection();
    newSection->g     = cs->g;
    newSection->dpi_x = cs->dpi_x;
    newSection->dpi_y = cs->dpi_y;
    newSection->orientation = cs->orientation;
    newSection->rotangle = cs->rotangle;
    newSection->highlight = cs->highlight;

	// 4/20/2012 brg: highlight_color wasn't being allocated, resulting in a crash when splitting cores.
	// Really, this should be resolved through a proper default constructor, along with a copy ctor. Use
	// default color for now rather than trying to copy source section's colors over.
	newSection->highlight_color = new GLfloat[3];
	newSection->highlight_color[0] = 1.0f;
    newSection->highlight_color[1] = 1.0f;
    newSection->highlight_color[2] = 1.0f;

    newSection->px = cs->px;
    newSection->py = cs->py;
    newSection->draw_vert_line = cs->draw_vert_line;
    newSection->vert_line_x = cs->vert_line_x;
    newSection->movable = cs->movable;

	newSection->name = NULL;
	set_section_name( newSection, "Split" );

    newSection->width = cs->width;
    newSection->height = cs->height;
    newSection->depth = cs->depth;
    newSection->graph_offset = cs->graph_offset;
    newSection->src = cs->src; // texture index
    inc_texset_ref_count(newSection->src);

	// put into newTrack
    newSection->track = newTrackId;
    newSection->section = append_model(newTrack, newSection);

    newSection->parentTrack = trackId;
    newSection->parentSection = sectionId;

    return newSection->section;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setRemoteControl
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setRemoteControl
  (JNIEnv * jenv, jclass jcls, jboolean b)
{
    set_remote_controlled(b);  
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setTieDepth
 * Signature: (ZF)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setTieDepth
  (JNIEnv * jenv, jclass jcls, jboolean isEnabled, jfloat depth)
{
    setTieDepth(isEnabled, depth);      
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    resetDefaultTrackYPos
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_resetDefaultTrackYPos
  (JNIEnv * jenv, jclass jcls)
{
    reset_default_track_ypos();      
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    staggerTrackSections
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_staggerTrackSections
  (JNIEnv * jenv, jclass jcls, jint trackId, jboolean stagger)
{
	stagger_track_sections(trackId, stagger);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    trackIsStaggered
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_trackIsStaggered
  (JNIEnv * jenv, jclass jcls, jint trackId)
{
    TrackSceneNode* track = get_scene_track(trackId);
	return track->staggered;
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    trimSections
 * Signature: (IIFZZ)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_trimSections
(JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId, jfloat trim,
 jboolean fromBottom, jboolean trimSelAndDeeper)
{
    trim_sections(trackId, sectionId, trim, fromBottom, trimSelAndDeeper);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    stackSections
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_stackSections
(JNIEnv * jenv, jclass jcls, jint trackId, jint sectionId)
{
    stack_sections(trackId, sectionId);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    setDebug
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_corelyzer_graphics_SceneGraph_setDebug
  (JNIEnv * jenv, jclass jcls, jboolean b)
{
    setDebug(b);
}

/*
 * Class:     corelyzer_helper_SceneGraph
 * Method:    getDebug
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_corelyzer_graphics_SceneGraph_getDebug
  (JNIEnv * jenv, jclass jcls)
{
    return getDebug();  
}
