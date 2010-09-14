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

#include <string>
#include "trackscene.h"
#include "trackscenenode.h"
#include "freedraw.h"

#define TRACK_GAP   (10) // inch
#define DEFAULT_DPI (72)

static float defaultTrackPositionY = 0.0f;

static bool is_remote_controlled = false;

//================================================================
std::vector< TrackScene* > trackscenevec;
static int renderMode = -1;

int create_track_scene()
{
   TrackScene* ts = new TrackScene();
   ts->selectedTrack = -1;
   ts->trackvec.clear();
   ts->zorder.clear();

   int i;
   for( i = 0; i < trackscenevec.size(); i++)
   {
        if( trackscenevec[i] == NULL )
        {
            trackscenevec[i] = ts;
            return i;
        }
   }

   trackscenevec.push_back(ts);
   return trackscenevec.size() - 1;
}

//================================================================
void free_track_scene(int i)
{
    if(!is_track_scene(i)) return;
    free_all_tracks(i);
    delete trackscenevec[i];
    trackscenevec[i] = NULL;
}

//================================================================
bool is_track_scene(int i)
{
    if( i < 0 || i > trackscenevec.size() - 1) return false;
    return (trackscenevec[i] != NULL );
}

//================================================================
bool is_track(int scene, int trackid)
{
    if( !is_track_scene(scene)) return false;
    if( trackid < 0 || trackid > trackscenevec[scene]->trackvec.size() - 1)
        return false;
    return (trackscenevec[scene]->trackvec[trackid] != NULL );
}

//================================================================
int append_track(int scene, const char* sessionName, const char* trackName)
{
#ifdef DEBUG
    if(!is_track_scene(scene))
    {
        printf("%d is not a Track Scene\n", scene);
        return -1;
    }
#else
    if(!is_track_scene(scene)) return -1;
#endif

    TrackSceneNode* t = NULL;
    create_track(sessionName, trackName, t);

#ifdef DEBUG
    if( !t )
    {
        printf("Could not create track object\n");
        return -1;
    }
#else
    if( !t ) return -1;
#endif

    TrackScene* ts = trackscenevec[scene];

    // Tracks with the same 'HOLE' should have the same 't->py'
    // Track name: <hole>_<core>
    bool foundTheSameHole = false;
    float holePy = defaultTrackPositionY;

    char * newTrackName = (char*) malloc(sizeof(char) * (strlen(trackName) + 1));
    strcpy(newTrackName, trackName);
    char * newHoleStr = strtok(newTrackName, "_");

    // try to reuse a track vec slot
    for(int i = 0; i < ts->trackvec.size(); ++i)
    {
        if( ts->trackvec[i] == NULL)
        {
            ts->trackvec[i] = t;

            // make sure that we don't have more than one i in zorder
            if( ts->zorder.size() == 0)
            {
                ts->zorder.push_back(i);
                return i;
            }

            bool found = false;
            for( int k = 0; k < ts->zorder.size(); ++k)
            {
                if( ts->zorder[k] == i)
                {
                    if(!found)
                    {
                        found = true;
                    }
                    else
                    {
                        ts->zorder[k] = -1;
                    }
                }
            }

            if(!found)
            {
                ts->zorder.push_back(i);
                bring_track_front(scene,i);
            }

            // increament default track posY
            t->py = holePy;
            defaultTrackPositionY = (foundTheSameHole) ?
                            (defaultTrackPositionY) :
                            (defaultTrackPositionY -= (TRACK_GAP * DEFAULT_DPI));

            return i;
        }
        else // try to find if track name with the same HOLE component exists
        {
            if(!foundTheSameHole)
            {
                char *thisTrackName = (char *) malloc( sizeof(char) * (strlen(ts->trackvec[i]->name)+1) );
                strcpy(thisTrackName, ts->trackvec[i]->name);
                char * thisHoleStr = strtok(thisTrackName, "_");

                // found it, with compare HOLE string
                if(0 == strcmp(newHoleStr, thisHoleStr))
                {
                    holePy = ts->trackvec[i]->py;
                    foundTheSameHole = true;
                }

                free(thisTrackName);
            }
        }
    }

    t->py = holePy;
    defaultTrackPositionY = (foundTheSameHole) ?
                            (defaultTrackPositionY) :
                            (defaultTrackPositionY -= (TRACK_GAP * DEFAULT_DPI));
    free(newTrackName);

    // no empty slots    
    ts->trackvec.push_back(t);
    ts->zorder.push_back(ts->trackvec.size() -1);
    bring_track_front(scene,ts->trackvec.size() -1);

    return (ts->trackvec.size() - 1);
}

//================================================================
void free_all_tracks(int scene)
{
    if(!is_track_scene(scene)) return;
    for( int i = 0; i < trackscenevec[scene]->trackvec.size(); ++i)
    {
        free_track(trackscenevec[scene]->trackvec[i]);
        trackscenevec[scene]->trackvec[i] = NULL;
    }

    trackscenevec[scene]->trackvec.clear();
    trackscenevec[scene]->zorder.clear();
}

//================================================================
void free_track(int scene, int trackid)
{
    if(!is_track(scene,trackid)) return;
    free_track(trackscenevec[scene]->trackvec[trackid]);
    
	trackscenevec[scene]->trackvec[trackid] = NULL;
}

//================================================================
void free_track_section_model(int scene, int trackid, int sectionid)
{
    if(!is_track(scene,trackid)) return;
    free_model(trackscenevec[scene]->trackvec[trackid], sectionid);
}

//================================================================
int get_scene_track(int scene, const char* name)
{
    if(!is_track_scene(scene)) return -1;
    for( int i = 0; i < trackscenevec[scene]->trackvec.size(); ++i)
    {
        if( trackscenevec[scene]->trackvec[i] == NULL) continue;
        
        if(!strcmp(name,trackscenevec[scene]->trackvec[i]->name))
            return i;
    }

    return -1;
}

//================================================================
int num_tracks(int scene)
{
    if(!is_track_scene(scene)) return -1;
    return trackscenevec[scene]->trackvec.size();
}

//================================================================
void highlight_track( int scene, int trackid, bool h)
{
    if(!is_track(scene,trackid)) return;
    trackscenevec[scene]->trackvec[trackid]->highlight = h;
}

//================================================================
void bring_track_front(int scene, int trackid)
{
    if(!is_track(scene,trackid)) return;
    if( trackscenevec[scene]->zorder.size() <= 0 )
    {
        trackscenevec[scene]->zorder.push_back(trackid);
        return;
    }

    // find the track in the zorder, and bring it to the front
    
    int i;
    int z = -1;
    for( i = trackscenevec[scene]->zorder.size() - 1; i >= 0; --i)
    {
        if( trackscenevec[scene]->zorder[i] == trackid)
        {
            z = i;
            i = -1;
        }
    }

    // never found!!!

    if( z < 0 )
    {
        trackscenevec[scene]->zorder.push_back(trackid);
        return;
    }
    
    for( i = z; i > 0; --i)
        trackscenevec[scene]->zorder[i] = trackscenevec[scene]->zorder[i - 1];

    trackscenevec[scene]->zorder[0] = trackid;
}

//================================================================
void attach_free_draw_to_scene(int scene, int fdid)
{
    if(!is_track_scene(scene)) return;
    if(!is_free_draw_rectangle(fdid)) return;
    TrackScene* ts = trackscenevec[scene];
    for( int i = 0; i < ts->freedrawvec.size(); i++)
    {
        if( ts->freedrawvec[i] == -1)
        {
            ts->freedrawvec[i] = fdid;
            return;
        }
    }


    trackscenevec[scene]->freedrawvec.push_back(fdid);
}

//================================================================
void detach_free_draw_from_scene(int scene, int fdid)
{
    if(!is_track_scene(scene)) return;
    if(!is_free_draw_rectangle(fdid)) return;
    TrackScene* ts = trackscenevec[scene];
    for( int i = 0; i < ts->freedrawvec.size(); i++) 
    {
        if( ts->freedrawvec[i] == fdid)
        {
            ts->freedrawvec[i] = -1;
            i = ts->freedrawvec.size();
        }
    }
}

//================================================================
void render_track_scene(int id, Canvas* c)
{
#ifdef DEBUG
    printf("\n--- Rendering Track Scene %d ---\n", id);
#endif

    if(!is_track_scene(id))
    {
#ifdef DEBUG
        printf("%d is not a Track Scene\n", id);
#endif
        return;
    }
    
    if( c == NULL )
    {
#ifdef DEBUG
        printf("Canvas is NULL\n");
#endif
        return;
    }

    TrackScene* ts = trackscenevec[id];
#ifdef DEBUG
    printf("Track Scene address at 0x%x\n", ts);
#endif

    if(!ts) return;

#ifdef DEBUG
    printf("Going to go through zorder of size %d\n", ts->zorder.size());
#endif

    // render back to front
    int i;
    for( i = ts->zorder.size() - 1; i > -1; --i)
    {
#ifdef DEBUG
        printf("Rendering Track %d\n", ts->zorder[i]);
        float x, y, z;
        get_camera_position(c->camera,&x,&y,&z);
        printf("Camera position %.2f, %.2f\n", x, y);
#endif
        if( ts->zorder[i] > -1 && ts->trackvec[ ts->zorder[i] ] != NULL)
        {
            glPushMatrix();
            {
                render_track( ts->trackvec[ ts->zorder[i] ], c);
            }
            glPopMatrix();
        }
    }

    // draw plugin free draw rectangles, scale so x,y,w,h are in meters
    glPushMatrix();
    {
        float scale = 1.0 / c->dpi_x * CM_PER_INCH / 100.0f;
        glScalef(1.0/scale,1.0/scale,1.0/scale);
        for( i = 0; i < ts->freedrawvec.size(); i++)
        {
    //        printf("Rendering pfdr %d\n", ts->freedrawvec[i]);
            render_free_draw(ts->freedrawvec[i]);
        }
    }
    glPopMatrix();

}

//================================================================
TrackScene* get_scene(int scene)
{
    if(!is_track_scene(scene)) return NULL;
    return trackscenevec[scene];
}

//================================================================
TrackSceneNode* get_scene_track(int scene, int trackid)
{
    if(!is_track(scene,trackid)) return NULL;
    return trackscenevec[scene]->trackvec[trackid];
}

//================================================================
int get_scene_track_zorder_length(int scene)
{
    if(!is_track_scene(scene)) return 0;
    return trackscenevec[scene]->zorder.size();
}

//================================================================
void get_scene_track_zorder(int scene, int* order)
{
    if(!is_track_scene(scene)) return;
    if(!order) return;
    
    for(int i = 0; i < trackscenevec[scene]->zorder.size(); ++i)
    {
        order[i] = trackscenevec[scene]->zorder[i];
    }
}

//================================================================
int BoundScene = -1;
//================================================================
void bind_scene(int scene)
{
    if(!is_track_scene(scene)) 
    {
        BoundScene = -1;
        return;
    }

    BoundScene = scene;
}

//================================================================
bool is_scene_bound()
{
    return BoundScene != -1;
}

//================================================================
bool is_track(int trackid)
{
    return is_track(BoundScene,trackid);
}

//================================================================
int append_track(const char* sessionName, const char* trackName)
{
    if(!is_scene_bound()) return -1;

#ifdef DEBUG
    if(!is_track_scene(BoundScene)) 
    {
        printf("%d is not a Track Scene\n", BoundScene);
        return -1;
    }
#else
    if(!is_track_scene(BoundScene)) return -1;
#endif

    TrackSceneNode* t = NULL;
    create_track(sessionName, trackName, t);

#ifdef DEBUG
    if( !t )
    {
        printf("Could not create track object\n");
        return -1;
    }
#else
    if( !t ) return -1;
#endif

    TrackScene* ts = trackscenevec[BoundScene];

    // try to reuse a track vec slot
    int pos = -1;
    for( int i = 0; i < ts->trackvec.size(); ++i)
    {
        if( ts->trackvec[i] == NULL)
        {
            ts->trackvec[i] = t;

            // make sure that we don't have more than one i in zorder
            if( ts->zorder.size() == 0)
            {
                ts->zorder.push_back(i);
                return i;
            }

            bool found = false;
            for( int k = 0; k < ts->zorder.size(); ++k)
            {
                if( ts->zorder[k] == i)
                {
                    if(!found)
                        found = true;
                    else
                        ts->zorder[k] = -1;
                }
            }

            if(!found)
            {
                ts->zorder.push_back(i);
                bring_track_front(BoundScene,i);
            }

            return i;
        }
    }
    
    // no empty slots
    
    ts->trackvec.push_back(t);
    ts->zorder.push_back(ts->trackvec.size() -1);
    bring_track_front(BoundScene,ts->trackvec.size() -1);
    return ts->trackvec.size() - 1;
}

//================================================================
void free_all_tracks()
{
    if(!is_scene_bound()) return;
    if(!is_track_scene(BoundScene)) return;
    for( int i = 0; i < trackscenevec[BoundScene]->trackvec.size(); ++i)
    {
        free_track(trackscenevec[BoundScene]->trackvec[i]);
        trackscenevec[BoundScene]->trackvec[i] = NULL;
    }

    trackscenevec[BoundScene]->trackvec.clear();
    trackscenevec[BoundScene]->zorder.clear();
}

//================================================================
void free_track(int trackid)
{
    if(!is_track(BoundScene,trackid)) return;
    free_track(trackscenevec[BoundScene]->trackvec[trackid]);
    trackscenevec[BoundScene]->trackvec[trackid] = NULL;
}

//================================================================
int get_scene_track(const char* name)
{
    if(!is_scene_bound()) return -1;
    if(!is_track_scene(BoundScene)) return -1;
    for( int i = 0; i < trackscenevec[BoundScene]->trackvec.size(); ++i)
    {
        if( trackscenevec[BoundScene]->trackvec[i] == NULL) continue;
        
        if(!strcmp(name,trackscenevec[BoundScene]->trackvec[i]->name))
            return i;
    }

    return -1;
}

//================================================================
int num_tracks()
{
    if(!is_scene_bound()) return -1;
    if(!is_track_scene(BoundScene)) return -1;
    return trackscenevec[BoundScene]->trackvec.size();
}

//================================================================
void highlight_track(int trackid, bool h)
{
    if(!is_track(BoundScene,trackid)) return;
    trackscenevec[BoundScene]->trackvec[trackid]->highlight = h;
}

//================================================================
void bring_track_front(int trackid)
{
    if(!is_scene_bound()) return;
    if(!is_track(BoundScene,trackid)) return;
    if( trackscenevec[BoundScene]->zorder.size() <= 0 )
    {
        trackscenevec[BoundScene]->zorder.push_back(trackid);
        return;
    }

    // find the track in the zorder, and bring it to the front
    
    int i;
    int z = -1;
    for( i = trackscenevec[BoundScene]->zorder.size() - 1; i >= 0; --i)
    {
        if( trackscenevec[BoundScene]->zorder[i] == trackid)
        {
            z = i;
            i = -1;
        }
    }

    // never found!!!

    if( z < 0 )
    {
        trackscenevec[BoundScene]->zorder.push_back(trackid);
        return;
    }
    
    for( i = z; i > 0; --i)
    {
        trackscenevec[BoundScene]->zorder[i] = 
            trackscenevec[BoundScene]->zorder[i - 1];
    }

    trackscenevec[BoundScene]->zorder[0] = trackid;
}

//================================================================
void attach_free_draw_to_scene(int fdid)
{
    if(!is_scene_bound()) return;
    if(!is_track_scene(BoundScene)) return;
    if(!is_free_draw_rectangle(fdid)) return;
    TrackScene* ts = trackscenevec[BoundScene];
    for( int i = 0; i < ts->freedrawvec.size(); i++)
    {
        if( ts->freedrawvec[i] == -1)
        {
            ts->freedrawvec[i] = fdid;
            return;
        }
    }

    trackscenevec[BoundScene]->freedrawvec.push_back(fdid);
}

//================================================================
void detach_free_draw_from_scene(int fdid)
{
    if(!is_scene_bound()) return;
    if(!is_track_scene(BoundScene)) return;
    if(!is_free_draw_rectangle(fdid)) return;
    TrackScene* ts = trackscenevec[BoundScene];
    for( int i = 0; i < ts->freedrawvec.size(); i++) 
    {
        if( ts->freedrawvec[i] == fdid)
        {
            ts->freedrawvec[i] = -1;
            i = ts->freedrawvec.size();
        }
    }
}

//================================================================
void render_track_scene(Canvas* c)
{
    if(!is_scene_bound()) return;

#ifdef DEBUG
    printf("\n--- Rendering Track Scene %d ---\n", BoundScene);
#endif

    if(!is_track_scene(BoundScene))
    {
#ifdef DEBUG
        printf("%d is not a Track Scene\n", BoundScene);
#endif
        return;
    }
    
    if( c == NULL )
    {
#ifdef DEBUG
        printf("Canvas is NULL\n");
#endif
        return;
    }

    TrackScene* ts = trackscenevec[BoundScene];
#ifdef DEBUG
    printf("Track Scene address at 0x%x\n", ts);
#endif

    if(!ts) return;

#ifdef DEBUG
    printf("Going to go through zorder of size %d\n", ts->zorder.size());
#endif

    // render back to front
    int i;
    for( i = ts->zorder.size() - 1; i > -1; --i)
    {
#ifdef DEBUG
        printf("Rendering Track %d\n", ts->zorder[i]);
        float x, y, z;
        get_camera_position(c->camera,&x,&y,&z);
        printf("Camera position %.2f, %.2f\n", x, y);
#endif
        if( ts->zorder[i] > -1 && ts->trackvec[ ts->zorder[i] ] != NULL)
        {
            render_track( ts->trackvec[ ts->zorder[i] ], c);
        }
    }

    // draw plugin free draw rectangles, scale so x,y,w,h are in meters
    glPushMatrix();
    float scale = 1.0 / c->dpi_x * CM_PER_INCH / 100.0f;
    float indep_scale = c->w / c->w0;
    glScalef(1.0/scale,1.0/scale,1.0/scale);
    for( i = 0; i < ts->freedrawvec.size(); i++)
    {
        if( is_free_draw_scale_independent(ts->freedrawvec[i]))
        {
            glScalef( indep_scale, indep_scale, 1.0f);
            render_free_draw(ts->freedrawvec[i]);
            glScalef( 1.0 / indep_scale, 1.0 / indep_scale, 1.0f);
        }
        else
        {
            render_free_draw(ts->freedrawvec[i]);
        }

    }
    glPopMatrix();

}

//================================================================
TrackScene* get_scene()
{
    if(!is_scene_bound()) return NULL;
    if(!is_track_scene(BoundScene)) return NULL;
    return trackscenevec[BoundScene];
}

//================================================================
TrackSceneNode* get_scene_track(int trackid)
{
    if(!is_scene_bound()) return NULL;
    if(!is_track(BoundScene,trackid)) return NULL;
    return trackscenevec[BoundScene]->trackvec[trackid];
}

//================================================================
int get_scene_track_zorder_length()
{
    if(!is_scene_bound()) return 0;
    if(!is_track_scene(BoundScene)) return 0;
    return trackscenevec[BoundScene]->zorder.size();
}

//================================================================
void get_scene_track_zorder(int* order)
{
    if(!is_scene_bound()) return;
    if(!order) return;
    
    for(int i = 0; i < trackscenevec[BoundScene]->zorder.size(); ++i)
    {
        order[i] = trackscenevec[BoundScene]->zorder[i];
    }
}

//================================================================
float get_scene_track_nextpos (int trackid)
{
    if(!is_track(BoundScene,trackid)) return 0.0f;
    
	return trackscenevec[BoundScene]->trackvec[trackid]->nextPos;

}

void set_remote_controlled(bool b)
{
    is_remote_controlled = b;
}

bool get_remote_controlled()
{
    return is_remote_controlled;
}

void reset_default_track_ypos()
{
    defaultTrackPositionY = 0.0f;    
}

int get_render_mode()
{
    return renderMode;
}

void set_render_mode(int m)
{
    renderMode = m;
}
