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

#include "trackscene.h"

#include <float.h>
#include <math.h>

#include <algorithm>
#include <string>

#include "canvas.h"
#include "freedraw.h"
#include "graph.h"
#include "trackscenenode.h"

#ifdef linux
#include "string.h"  // linux
#endif

#define TRACK_GAP (10)  // inch
#define DEFAULT_DPI (72)

static float defaultTrackPositionY = 0.0f;

static bool is_remote_controlled = false;

static int selectedTie = -1; // ID of selected tie, -1 if no selection
static int mouseoverTie = -1;


static void get_scenespace_tie_points(CoreSectionTie *tie, float &ax, float &ay, float &bx, float &by);
static void prep_tie_appearance(CoreSectionTie *tie, const int tie_id);
static void create_section_tie_segments(TrackScene *ts, Canvas *c);
static void render_section_tie_offcore_segments(TrackScene *ts);
static void update_mouseover_tie(TrackScene *ts, Canvas *c);
static bool is_tie_valid(CoreSectionTie *tie);
static bool is_tie_visible(CoreSectionTie *tie);

//================================================================
std::vector<TrackScene *> trackscenevec;
static int renderMode = -1;

int create_track_scene() {
    TrackScene *ts = new TrackScene();
    ts->selectedTrack = -1;
    ts->trackvec.clear();
    ts->zorder.clear();

    for (unsigned int i = 0; i < trackscenevec.size(); i++) {
        if (trackscenevec[i] == NULL) {
            trackscenevec[i] = ts;
            return i;
        }
    }

    trackscenevec.push_back(ts);
    return trackscenevec.size() - 1;
}

//================================================================
void free_track_scene(int i) {
    if (!is_track_scene(i))
        return;
    free_all_tracks(i);
    delete trackscenevec[i];
    trackscenevec[i] = NULL;
}

//================================================================
bool is_track_scene(int i) {
    if (i < 0)
        return false;
    const int trackSceneVecSize = trackscenevec.size() - 1;
    if (i > trackSceneVecSize)
        return false;
    return (trackscenevec[i] != NULL);
}

//================================================================
bool is_track(int scene, int trackid) {
    if (!is_track_scene(scene))
        return false;
    if (trackid < 0)
        return false;
    const int trackVecSize = trackscenevec[scene]->trackvec.size() - 1;
    if (trackid > trackVecSize)
        return false;
    return (trackscenevec[scene]->trackvec[trackid] != NULL);
}

//================================================================
// Copy section tie IDs into idvec
void get_tie_ids(int scene, std::vector<int> &idvec) {
    TrackScene *ts = get_scene(scene);
    if (!ts) return;
    int count = 0;
    for (int i = 0; i < ts->tievec.size(); i++) {
        if (ts->tievec[i] != NULL) {
            idvec.push_back(i);
        }
    }
}

//================================================================
int add_tie(int scene, CoreSectionTie *tie) { // todo: associate tie with session by name as in track logic
    if (!is_track_scene(scene))
        return -1;

    TrackScene *ts = trackscenevec[scene];
    int tieId = -1;
    for (int i = 0; i < ts->tievec.size(); i++) { // use empty slot if possible
        if (ts->tievec[i] == NULL) {
            ts->tievec[i] = tie;
            tieId = i;
            break;
        }
    }
    if (tieId == -1) {
        ts->tievec.push_back(tie);
        tieId = ts->tievec.size() - 1;
    }
    return tieId;
}

//================================================================
// todo? associate tie with session by name as in track logic
// delete_tie() better name?
void remove_tie(int scene, int tieId) {
    if (tieId < 0) return;
    TrackScene *ts = get_scene(scene);
    CoreSectionTie *tie = ts->tievec[tieId];
    delete tie;
    ts->tievec[tieId] = NULL;
}

//================================================================
CoreSectionTie *get_tie(int scene, int tieId) {
    TrackScene *ts = trackscenevec[scene];
    if (!ts || tieId == -1) return NULL;
    CoreSectionTie *tie = ts->tievec[tieId];
    return tie;
}

//================================================================
void set_selected_tie(int tieId) {
    selectedTie = tieId;
}

//================================================================
int get_mouseover_tie() {
    return mouseoverTie;
}

//================================================================
bool edit_section_tie_point(int scene, float mouseX, float mouseY) {
    if (mouseoverTie != -1) {
        float ax, ay, bx, by;
        CoreSectionTie *tie = get_tie(scene, mouseoverTie);
        get_scenespace_tie_points(tie, ax, ay, bx, by);
        SectionTiePoint *fixedPoint = NULL;

        if (pt_to_pt_dist(mouseX, mouseY, ax, ay) < pt_to_pt_dist(mouseX, mouseY, bx, by)) {
            fixedPoint = tie->b;
        } else {
            fixedPoint = tie->a;
        }
        const bool canEdit = start_edit_existing_tie(mouseoverTie, fixedPoint == tie->a);
        if (!canEdit) { return false; }

        return true;
    }
    return false;
}

//================================================================
int append_track(int scene, const char *sessionName, const char *trackName) {
#ifdef DEBUG
    if (!is_track_scene(scene)) {
        printf("%d is not a Track Scene\n", scene);
        return -1;
    }
#else
    if (!is_track_scene(scene))
        return -1;
#endif

    TrackSceneNode *t = NULL;
    create_track(sessionName, trackName, t);

#ifdef DEBUG
    if (!t) {
        printf("Could not create track object\n");
        return -1;
    }
#else
    if (!t)
        return -1;
#endif

    TrackScene *ts = trackscenevec[scene];

    // Tracks with the same 'HOLE' should have the same 't->py'
    // Track name: <hole>_<core>
    bool foundTheSameHole = false;
    float holePy = defaultTrackPositionY;

    char *newTrackName = (char *)malloc(sizeof(char) * (strlen(trackName) + 1));
    strcpy(newTrackName, trackName);
    char *newHoleStr = strtok(newTrackName, "_");

    // try to reuse a track vec slot
    for (unsigned int i = 0; i < ts->trackvec.size(); ++i) {
        if (ts->trackvec[i] == NULL) {
            ts->trackvec[i] = t;

            // make sure that we don't have more than one i in zorder
            if (ts->zorder.size() == 0) {
                ts->zorder.push_back(i);
                return i;
            }

            bool found = false;
            for (unsigned int k = 0; k < ts->zorder.size(); ++k) {
                if (ts->zorder[k] == i) {
                    if (!found) {
                        found = true;
                    } else {
                        ts->zorder[k] = -1;
                    }
                }
            }

            if (!found) {
                ts->zorder.push_back(i);
                bring_track_front(scene, i);
            }

            // increament default track posY
            t->py = holePy;
            defaultTrackPositionY = (foundTheSameHole) ? (defaultTrackPositionY) : (defaultTrackPositionY -= (TRACK_GAP * DEFAULT_DPI));

            return i;
        } else  // try to find if track name with the same HOLE component exists
        {
            if (!foundTheSameHole) {
                char *thisTrackName = (char *)malloc(sizeof(char) * (strlen(ts->trackvec[i]->name) + 1));
                strcpy(thisTrackName, ts->trackvec[i]->name);
                char *thisHoleStr = strtok(thisTrackName, "_");

                // found it, with compare HOLE string
                if (0 == strcmp(newHoleStr, thisHoleStr)) {
                    holePy = ts->trackvec[i]->py;
                    foundTheSameHole = true;
                }

                free(thisTrackName);
            }
        }
    }

    t->py = holePy;
    defaultTrackPositionY = (foundTheSameHole) ? (defaultTrackPositionY) : (defaultTrackPositionY -= (TRACK_GAP * DEFAULT_DPI));
    free(newTrackName);

    // no empty slots
    ts->trackvec.push_back(t);
    ts->zorder.push_back(ts->trackvec.size() - 1);
    bring_track_front(scene, ts->trackvec.size() - 1);

    return (ts->trackvec.size() - 1);
}

//================================================================
void free_all_tracks(int scene) {
    if (!is_track_scene(scene))
        return;
    for (unsigned int i = 0; i < trackscenevec[scene]->trackvec.size(); ++i) {
        free_track(trackscenevec[scene]->trackvec[i]);
        trackscenevec[scene]->trackvec[i] = NULL;
    }

    trackscenevec[scene]->trackvec.clear();
    trackscenevec[scene]->zorder.clear();
}

//================================================================
void free_track(int scene, int trackid) {
    if (!is_track(scene, trackid))
        return;
    free_track(trackscenevec[scene]->trackvec[trackid]);

    trackscenevec[scene]->trackvec[trackid] = NULL;
}

//================================================================
void free_track_section_model(int scene, int trackid, int sectionid) {
    if (!is_track(scene, trackid))
        return;
    free_model(trackscenevec[scene]->trackvec[trackid], sectionid);
    free_associated_ties(scene, trackid, sectionid);
}

//================================================================
// Delete ties with at least one endpoint on this section
void free_associated_ties(int scene, int trackId, int sectionId) {
    for (int tidx = 0; tidx < trackscenevec[scene]->tievec.size(); tidx++) {
        CoreSectionTie *tie = trackscenevec[scene]->tievec[tidx];
        if (!tie) { continue; }
        if (tie->isOnTrack(trackId) && tie->isOnSection(sectionId)) {
            remove_tie(scene, tidx);
        }
    }
}

//================================================================
// Delete all ties that originate from track being deleted
void delete_section_ties_on_track(int scene, int trackId) {
    TrackSceneNode *track = get_scene_track(trackId);
    if (!track) return;
    std::vector<int> ties_to_delete = std::vector<int>();
    for (int sidx = 0; sidx < track->modelvec.size(); sidx++) {
        CoreSection *sec = track->modelvec[sidx];
        if (!sec) continue;
        for (int tidx = 0; tidx < trackscenevec[scene]->tievec.size(); tidx++) {
            CoreSectionTie *tie = trackscenevec[scene]->tievec[tidx];
            if (!tie) continue;
            if (tie->isOnTrack(trackId)) {
                ties_to_delete.push_back(tidx);
            }
        }
    }

    for (int i = 0; i < ties_to_delete.size(); i++) {
        remove_tie(scene, ties_to_delete[i]);
    }
}

//================================================================
int get_scene_track(int scene, const char *name) {
    if (!is_track_scene(scene))
        return -1;
    for (unsigned int i = 0; i < trackscenevec[scene]->trackvec.size(); ++i) {
        if (trackscenevec[scene]->trackvec[i] == NULL)
            continue;

        if (!strcmp(name, trackscenevec[scene]->trackvec[i]->name))
            return i;
    }

    return -1;
}

//================================================================
int num_tracks(int scene) {
    if (!is_track_scene(scene))
        return -1;
    return trackscenevec[scene]->trackvec.size();
}

//================================================================
void highlight_track(int scene, int trackid, bool h) {
    if (!is_track(scene, trackid))
        return;
    trackscenevec[scene]->trackvec[trackid]->highlight = h;
}

//================================================================
void bring_track_front(int scene, int trackid) {
    if (!is_track(scene, trackid))
        return;
    if (trackscenevec[scene]->zorder.size() <= 0) {
        trackscenevec[scene]->zorder.push_back(trackid);
        return;
    }

    // find the track in the zorder, and bring it to the front

    int i;
    int z = -1;
    for (i = trackscenevec[scene]->zorder.size() - 1; i >= 0; --i) {
        if (trackscenevec[scene]->zorder[i] == trackid) {
            z = i;
            i = -1;
        }
    }

    // never found!!!

    if (z < 0) {
        trackscenevec[scene]->zorder.push_back(trackid);
        return;
    }

    for (i = z; i > 0; --i)
        trackscenevec[scene]->zorder[i] = trackscenevec[scene]->zorder[i - 1];

    trackscenevec[scene]->zorder[0] = trackid;
}

//================================================================
void attach_free_draw_to_scene(int scene, int fdid) {
    if (!is_track_scene(scene))
        return;
    if (!is_free_draw_rectangle(fdid))
        return;
    TrackScene *ts = trackscenevec[scene];
    for (unsigned int i = 0; i < ts->freedrawvec.size(); i++) {
        if (ts->freedrawvec[i] == -1) {
            ts->freedrawvec[i] = fdid;
            return;
        }
    }

    trackscenevec[scene]->freedrawvec.push_back(fdid);
}

//================================================================
void detach_free_draw_from_scene(int scene, int fdid) {
    if (!is_track_scene(scene))
        return;
    if (!is_free_draw_rectangle(fdid))
        return;
    TrackScene *ts = trackscenevec[scene];
    for (unsigned int i = 0; i < ts->freedrawvec.size(); i++) {
        if (ts->freedrawvec[i] == fdid) {
            ts->freedrawvec[i] = -1;
            i = ts->freedrawvec.size();
        }
    }
}

//================================================================
void render_track_scene(int id, Canvas *c) {
#ifdef DEBUG
    printf("\n--- Rendering Track Scene %d ---\n", id);
#endif

    if (!is_track_scene(id)) {
#ifdef DEBUG
        printf("%d is not a Track Scene\n", id);
#endif
        return;
    }

    if (c == NULL) {
#ifdef DEBUG
        printf("Canvas is NULL\n");
#endif
        return;
    }

    TrackScene *ts = trackscenevec[id];
#ifdef DEBUG
    printf("Track Scene address at 0x%x\n", ts);
#endif

    if (!ts)
        return;

    update_mouseover_tie(ts, c);
    create_section_tie_segments(ts, c);
    render_section_tie_offcore_segments(ts);

#ifdef DEBUG
    printf("Going to go through zorder of size %d\n", ts->zorder.size());
#endif

    // render back to front
    int i;
    for (i = ts->zorder.size() - 1; i > -1; --i) {
#ifdef DEBUG
        printf("Rendering Track %d\n", ts->zorder[i]);
        float x, y, z;
        get_camera_position(c->camera, &x, &y, &z);
        printf("Camera position %.2f, %.2f\n", x, y);
#endif
        if (ts->zorder[i] > -1 && ts->trackvec[ts->zorder[i]] != NULL) {
            glPushMatrix();
            {
                render_track(ts->trackvec[ts->zorder[i]], c);
            }
            glPopMatrix();
        }
    }

    render_section_tie_oncore_segments(ts, c);
    render_in_progress_tie(c);

    // draw plugin free draw rectangles, scale so x,y,w,h are in meters
    glPushMatrix();
    {
        float scale = 1.0f / c->dpi_x * CM_PER_INCH / 100.0f;
        glScalef(1.0f / scale, 1.0f / scale, 1.0f / scale);
        for (unsigned int i = 0; i < ts->freedrawvec.size(); i++) {
            //        printf("Rendering pfdr %d\n", ts->freedrawvec[i]);
            render_free_draw(ts->freedrawvec[i]);
        }
    }
    glPopMatrix();
}

//================================================================
void render_arrowhead(float fromX, float fromY, float toX, float toY, float size) {
    const float pi = 3.14159f;
    float theta = atan2(toY - fromY, toX - fromX);
    float x0 = toX - size * cos(theta - pi/6);
    float y0 = toY - size * sin(theta - pi/6);
    float x1 = toX - size * cos(theta + pi/6);
    float y1 = toY - size * sin(theta + pi/6);
    glBegin(GL_POLYGON);
    {
        glVertex2f(toX, toY);
        glVertex2f(x0, y0);
        glVertex2f(x1, y1);
    }
    glEnd();
}

//================================================================
// Set tie color based on tie type
static void set_tie_color(SectionTieType type) {
    if (type == VISUAL) {
        glColor3f(0, 1, 0);
    } else if (type == DATA) {
        glColor3f(0, 0, 1);
    } else if (type == SPLICE) {
        glColor3f(1, 0, 0);
    } else {
        printf("Unexpected tie type %d\n", type);
    }
}

//================================================================
// client is responsible for handling vertical line i.e. undefined slope
float get_slope(float ax, float ay, float bx, float by) {
    return (by - ay) / (bx - ax);
}

//================================================================
// Find point where (ax,ay)->(bx,by) intersects the edges of the core
// section sectionId in track trackId. Return intersection point in
// (outX, outY) parameters.
void calc_core_edge_intersection(
    Canvas *c, const int trackId, const int sectionId,
    float ax, float ay, float bx, float by,
    float m, // slope
    float b, // y-intercept
    float &outX, float &outY) {
    TrackSceneNode *t = get_scene_track(trackId);
    CoreSection *cs = get_track_section(t, sectionId);

    // Edge names based on section appearance in horizontal depth orientation.
    // leftEdge is the top of the physical core, rightEdge is the bottom.
    const float topEdge = t->py;
    const float botEdge = t->py + (cs->height * INCH_PER_CM * c->dpi_x);
    const float leftEdge = cs->px + (cs->intervalTop * (INCH_PER_CM * c->dpi_x));
    const float rightEdge = cs->px + (cs->intervalBottom * INCH_PER_CM * c->dpi_x);

    // does (ax,ay)->(bx,by) intersect top or bottom edge?
    float topBotX, topBotY;
    if (ay > by) {
        topBotX = (topEdge - b)/m;
        topBotY = topEdge;
    } else {
        topBotX = (botEdge - b)/m;
        topBotY = botEdge;
    }

    // left or right edge?
    float leftRightX, leftRightY;
    if (ax > bx) {
        leftRightX = leftEdge;
        leftRightY = m * leftEdge + b;
    } else {
        leftRightX = rightEdge;
        leftRightY = m * rightEdge + b;
    }

    // of the top/bot and left/right candidate points, only one should
    // fall within the core section's rectangle
    if (topBotX >= leftEdge && topBotX <= rightEdge) {
        outX = topBotX;
        outY = topBotY;
    } else {
        outX = leftRightX;
        outY = leftRightY;
    }
}

//================================================================
// Prepare scenespace points for drawing tie line segments.
static void create_section_tie_segments(TrackScene *ts, Canvas *c) {
    for (int tidx = 0; tidx < ts->tievec.size(); tidx++) {
        CoreSectionTie *tie = ts->tievec[tidx];
        if (!is_tie_valid(tie) || !is_tie_visible(tie)) {
            continue;
        }

        float ax, ay, bx, by;
        get_scenespace_tie_points(tie, ax, ay, bx, by);

        // Single-section tie. A single line segment is required.
        if (tie->isSingleSection()) {
            tie->drawData->setPointA(ax, ay);
            tie->drawData->setPointB(bx, by);
            continue;
        }

        // Two-section tie. Create points defining three line segments:
        // A -> edge of tied section (A')
        // B -> edge of tied section (B')
        // A' -> B'
        if (ax == bx) { // handle vertical line (undefined slope)
            TrackSceneNode *aTrack = get_scene_track(tie->a->trackId);
            CoreSection *aCore = get_track_section(aTrack, tie->a->sectionId);
            TrackSceneNode *bTrack = get_scene_track(tie->b->trackId);
            CoreSection *bCore = get_track_section(bTrack, tie->b->sectionId);
            float aEdge = aTrack->py;
            float bEdge = bTrack->py;
            if (ay < by) {
                aEdge += aCore->height * INCH_PER_CM * c->dpi_x;
            } else {
                bEdge += bCore->height * INCH_PER_CM * c->dpi_x;
            }
            tie->drawData->setPointA(ax, ay);
            tie->drawData->setPointB(bx, by);
            tie->drawData->setPointAEdge(ax, aEdge);
            tie->drawData->setPointBEdge(bx, bEdge);
        } else {
            const float m = get_slope(ax, ay, bx, by);
            const float b = ay - (m * ax); // y-intercept

            float a_intX, a_intY, b_intX, b_intY;
            calc_core_edge_intersection(c, tie->a->trackId, tie->a->sectionId, ax, ay, bx, by, m, b, a_intX, a_intY);
            calc_core_edge_intersection(c, tie->b->trackId, tie->b->sectionId, bx, by, ax, ay, m, b, b_intX, b_intY);
            tie->drawData->setPointA(ax, ay);
            tie->drawData->setPointB(bx, by);
            tie->drawData->setPointAEdge(a_intX, a_intY);
            tie->drawData->setPointBEdge(b_intX, b_intY);
        }
    }
}

//================================================================
// Of the ties within 5 pixels of the mouse cursor, set mouseoverTie
// to the ID of the tie closest to the cursor.
static void update_mouseover_tie(TrackScene *ts, Canvas *c) {
    int minDistTie = -1;
    float minDist = 1000.0f;
    for (int tidx = 0; tidx < ts->tievec.size(); tidx++) {
        CoreSectionTie *tie = ts->tievec[tidx];
        if (!is_tie_valid(tie) || !is_tie_visible(tie)) {
            continue;
        }

        float ax, ay, bx, by;
        tie->a->toSceneSpace(ax, ay);
        tie->b->toSceneSpace(bx, by);
        const float ssDist = get_horizontal_depth() ? pt_to_line_dist(c->mouseX, c->mouseY, ax, ay, bx, by) : pt_to_line_dist(c->mouseY, -c->mouseX, ax, ay, bx, by);
        const float pixDist = ssDist / (get_canvas_width(0) / get_canvas_orig_width(0));
        if (pixDist <= TIE_SELECT_DIST_PIX) {
            if (pixDist < minDist) {
                minDist = pixDist;
                minDistTie = tidx;
            }
        }
    }
    mouseoverTie = minDistTie;
}

//================================================================
static bool is_tie_visible(CoreSectionTie *tie) {
    TrackSceneNode *trackA = get_scene_track(tie->a->trackId);
    TrackSceneNode *trackB = get_scene_track(tie->b->trackId);
    if (!trackA || !trackB) {
        return false;
    }
    return tie->show && trackA->show && trackB->show;
}

//================================================================
static bool is_tie_valid(CoreSectionTie *tie) {
    return tie && tie->valid();
}

//================================================================
// For each tie, draw the line segments within the tied cores.
void render_section_tie_oncore_segments(TrackScene *ts, Canvas *c) {
    glDisable(GL_TEXTURE_2D); // enabled textures affect point/line color

    for (int tidx = 0; tidx < ts->tievec.size(); tidx++) {
        CoreSectionTie *tie = ts->tievec[tidx];
        if (!is_tie_valid(tie) || !is_tie_visible(tie)) {
            continue;
        }

        prep_tie_appearance(tie, tidx);
        float ax, ay, bx, by;
        tie->drawData->getPointA(ax, ay);
        tie->drawData->getPointB(bx, by);
        if (tie->isSingleSection()) {
            glBegin(GL_LINES);
            glVertex2f(ax, ay);
            glVertex2f(bx, by);
            glEnd();
        } else {
            float aix, aiy, bix, biy;
            tie->drawData->getPointAEdge(aix, aiy);
            tie->drawData->getPointBEdge(bix, biy);
            glBegin(GL_LINES);
            glVertex2f(ax, ay);
            glVertex2f(aix, aiy);
            glVertex2f(bx, by);
            glVertex2f(bix, biy);
            glEnd();
        }
    }

    glEnable(GL_TEXTURE_2D);
}

//================================================================
// For each tie, draw the line segment between edges of the tied cores.
static void render_section_tie_offcore_segments(TrackScene *ts) {
    glDisable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    for (int tidx = 0; tidx < ts->tievec.size(); tidx++) {
        CoreSectionTie *tie = ts->tievec[tidx];
        if (!is_tie_valid(tie) || !is_tie_visible(tie)) {
            continue;
        }     

        if (!tie->isSingleSection()) {
            prep_tie_appearance(tie, tidx);
            glBegin(GL_LINES);
            float aix, aiy, bix, biy;
            tie->drawData->getPointAEdge(aix, aiy);
            tie->drawData->getPointBEdge(bix, biy);
            glVertex2f(aix, aiy);
            glVertex2f(bix, biy);
            glEnd();
        }
    }
    glDisable(GL_BLEND);
    glEnable(GL_TEXTURE_2D);
}


//================================================================
static void get_scenespace_tie_points(CoreSectionTie *tie, float &ax, float &ay, float &bx, float &by) {
    tie->a->toSceneSpace(ax, ay);
    tie->b->toSceneSpace(bx, by);
}

//================================================================
// set tie line width and color based on its type and selected/mouseover status
static void prep_tie_appearance(CoreSectionTie *tie, const int tie_id) {
    if (tie_id == mouseoverTie || tie_id == selectedTie) {
        glLineWidth(5);
    } else {
        glLineWidth(1);
    }
    if (tie_id == selectedTie) {
        glColor3f(1,1,0);
    } else {
        set_tie_color(tie->getType());
    }
}

//================================================================
void render_in_progress_tie(Canvas *c) {
    SectionTiePoint *tp = get_edit_tie_fixed_point();
    if (tp) {
        float ax, ay;
        tp->toSceneSpace(ax, ay);
        set_tie_color(get_edit_tie_type());
        glBegin(GL_LINES);
        {
            glVertex3f(ax, ay, 0.0f);
            const float mx = get_horizontal_depth() ? c->mouseX : c->mouseY;
            const float my = get_horizontal_depth() ? c->mouseY : -c->mouseX;
            glVertex3f(mx, my, 0.0f);
        }
        glEnd();
    }
}

//================================================================
TrackScene *get_scene(int scene) {
    if (!is_track_scene(scene))
        return NULL;
    return trackscenevec[scene];
}

//================================================================
TrackSceneNode *get_scene_track(int scene, int trackid) {
    if (!is_track(scene, trackid))
        return NULL;
    return trackscenevec[scene]->trackvec[trackid];
}

//================================================================
int get_scene_track_zorder_length(int scene) {
    if (!is_track_scene(scene))
        return 0;
    return trackscenevec[scene]->zorder.size();
}

//================================================================
void get_scene_track_zorder(int scene, int *order) {
    if (!is_track_scene(scene))
        return;
    if (!order)
        return;

    for (unsigned int i = 0; i < trackscenevec[scene]->zorder.size(); ++i) {
        order[i] = trackscenevec[scene]->zorder[i];
    }
}

//================================================================
int BoundScene = -1;
//================================================================
void bind_scene(int scene) {
    if (!is_track_scene(scene)) {
        BoundScene = -1;
        return;
    }

    BoundScene = scene;
}

//================================================================
bool is_scene_bound() {
    return BoundScene != -1;
}

//================================================================
bool is_track(int trackid) {
    return is_track(BoundScene, trackid);
}

//================================================================
int append_track(const char *sessionName, const char *trackName) {
    if (!is_scene_bound())
        return -1;

#ifdef DEBUG
    if (!is_track_scene(BoundScene)) {
        printf("%d is not a Track Scene\n", BoundScene);
        return -1;
    }
#else
    if (!is_track_scene(BoundScene))
        return -1;
#endif

    TrackSceneNode *t = NULL;
    create_track(sessionName, trackName, t);

#ifdef DEBUG
    if (!t) {
        printf("Could not create track object\n");
        return -1;
    }
#else
    if (!t)
        return -1;
#endif

    TrackScene *ts = trackscenevec[BoundScene];

    // try to reuse a track vec slot
    int pos = -1;
    for (unsigned int i = 0; i < ts->trackvec.size(); ++i) {
        if (ts->trackvec[i] == NULL) {
            ts->trackvec[i] = t;

            // make sure that we don't have more than one i in zorder
            if (ts->zorder.size() == 0) {
                ts->zorder.push_back(i);
                return i;
            }

            bool found = false;
            for (unsigned int k = 0; k < ts->zorder.size(); ++k) {
                if (ts->zorder[k] == i) {
                    if (!found)
                        found = true;
                    else
                        ts->zorder[k] = -1;
                }
            }

            if (!found) {
                ts->zorder.push_back(i);
                bring_track_front(BoundScene, i);
            }

            return i;
        }
    }

    // no empty slots

    ts->trackvec.push_back(t);
    ts->zorder.push_back(ts->trackvec.size() - 1);
    bring_track_front(BoundScene, ts->trackvec.size() - 1);
    return ts->trackvec.size() - 1;
}

//================================================================
void free_all_tracks() {
    if (!is_scene_bound())
        return;
    if (!is_track_scene(BoundScene))
        return;
    for (unsigned int i = 0; i < trackscenevec[BoundScene]->trackvec.size(); ++i) {
        free_track(trackscenevec[BoundScene]->trackvec[i]);
        trackscenevec[BoundScene]->trackvec[i] = NULL;
    }

    trackscenevec[BoundScene]->trackvec.clear();
    trackscenevec[BoundScene]->zorder.clear();
}

//================================================================
void free_track(int trackid) {
    if (!is_track(BoundScene, trackid))
        return;
    free_track(trackscenevec[BoundScene]->trackvec[trackid]);
    trackscenevec[BoundScene]->trackvec[trackid] = NULL;
}

//================================================================
int get_scene_track(const char *name) {
    if (!is_scene_bound())
        return -1;
    if (!is_track_scene(BoundScene))
        return -1;
    for (unsigned int i = 0; i < trackscenevec[BoundScene]->trackvec.size(); ++i) {
        if (trackscenevec[BoundScene]->trackvec[i] == NULL)
            continue;

        if (!strcmp(name, trackscenevec[BoundScene]->trackvec[i]->name))
            return i;
    }

    return -1;
}

//================================================================
int num_tracks() {
    if (!is_scene_bound())
        return -1;
    if (!is_track_scene(BoundScene))
        return -1;
    return trackscenevec[BoundScene]->trackvec.size();
}

//================================================================
void highlight_track(int trackid, bool h) {
    if (!is_track(BoundScene, trackid))
        return;
    trackscenevec[BoundScene]->trackvec[trackid]->highlight = h;
}

//================================================================
void bring_track_front(int trackid) {
    if (!is_scene_bound())
        return;
    if (!is_track(BoundScene, trackid))
        return;
    if (trackscenevec[BoundScene]->zorder.size() <= 0) {
        trackscenevec[BoundScene]->zorder.push_back(trackid);
        return;
    }

    // find the track in the zorder, and bring it to the front

    int i;
    int z = -1;
    for (i = trackscenevec[BoundScene]->zorder.size() - 1; i >= 0; --i) {
        if (trackscenevec[BoundScene]->zorder[i] == trackid) {
            z = i;
            i = -1;
        }
    }

    // never found!!!

    if (z < 0) {
        trackscenevec[BoundScene]->zorder.push_back(trackid);
        return;
    }

    for (i = z; i > 0; --i) {
        trackscenevec[BoundScene]->zorder[i] =
            trackscenevec[BoundScene]->zorder[i - 1];
    }

    trackscenevec[BoundScene]->zorder[0] = trackid;
}

//================================================================
void attach_free_draw_to_scene(int fdid) {
    if (!is_scene_bound())
        return;
    if (!is_track_scene(BoundScene))
        return;
    if (!is_free_draw_rectangle(fdid))
        return;
    TrackScene *ts = trackscenevec[BoundScene];
    for (unsigned int i = 0; i < ts->freedrawvec.size(); i++) {
        if (ts->freedrawvec[i] == -1) {
            ts->freedrawvec[i] = fdid;
            return;
        }
    }

    trackscenevec[BoundScene]->freedrawvec.push_back(fdid);
}

//================================================================
void detach_free_draw_from_scene(int fdid) {
    if (!is_scene_bound())
        return;
    if (!is_track_scene(BoundScene))
        return;
    if (!is_free_draw_rectangle(fdid))
        return;
    TrackScene *ts = trackscenevec[BoundScene];
    for (unsigned int i = 0; i < ts->freedrawvec.size(); i++) {
        if (ts->freedrawvec[i] == fdid) {
            ts->freedrawvec[i] = -1;
            i = ts->freedrawvec.size();
        }
    }
}

//================================================================
void render_track_scene(Canvas *c) {
    if (!is_scene_bound())
        return;

#ifdef DEBUG
    printf("\n--- Rendering Track Scene %d ---\n", BoundScene);
#endif

    if (!is_track_scene(BoundScene)) {
#ifdef DEBUG
        printf("%d is not a Track Scene\n", BoundScene);
#endif
        return;
    }

    if (c == NULL) {
#ifdef DEBUG
        printf("Canvas is NULL\n");
#endif
        return;
    }

    TrackScene *ts = trackscenevec[BoundScene];
#ifdef DEBUG
    printf("Track Scene address at 0x%x\n", ts);
#endif

    if (!ts)
        return;

#ifdef DEBUG
    printf("Going to go through zorder of size %d\n", ts->zorder.size());
#endif

    // render back to front
    int i;
    for (i = ts->zorder.size() - 1; i > -1; --i) {
#ifdef DEBUG
        printf("Rendering Track %d\n", ts->zorder[i]);
        float x, y, z;
        get_camera_position(c->camera, &x, &y, &z);
        printf("Camera position %.2f, %.2f\n", x, y);
#endif
        if (ts->zorder[i] > -1 && ts->trackvec[ts->zorder[i]] != NULL) {
            render_track(ts->trackvec[ts->zorder[i]], c);
        }
    }

    // draw plugin free draw rectangles, scale so x,y,w,h are in meters
    glPushMatrix();
    float scale = 1.0f / c->dpi_x * CM_PER_INCH / 100.0f;
    float indep_scale = c->w / c->w0;
    glScalef(1.0f / scale, 1.0f / scale, 1.0f / scale);
    for (unsigned int i = 0; i < ts->freedrawvec.size(); i++) {
        if (is_free_draw_scale_independent(ts->freedrawvec[i])) {
            glScalef(indep_scale, indep_scale, 1.0f);
            render_free_draw(ts->freedrawvec[i]);
            glScalef(1.0f / indep_scale, 1.0f / indep_scale, 1.0f);
        } else {
            render_free_draw(ts->freedrawvec[i]);
        }
    }
    glPopMatrix();
}

//================================================================
TrackScene *get_scene() {
    if (!is_scene_bound())
        return NULL;
    if (!is_track_scene(BoundScene))
        return NULL;
    return trackscenevec[BoundScene];
}

//================================================================
TrackSceneNode *get_scene_track(int trackid) {
    if (!is_scene_bound())
        return NULL;
    if (!is_track(BoundScene, trackid))
        return NULL;
    return trackscenevec[BoundScene]->trackvec[trackid];
}

//================================================================
int get_scene_track_zorder_length() {
    if (!is_scene_bound())
        return 0;
    if (!is_track_scene(BoundScene))
        return 0;
    return trackscenevec[BoundScene]->zorder.size();
}

//================================================================
void get_scene_track_zorder(int *order) {
    if (!is_scene_bound())
        return;
    if (!order)
        return;

    for (unsigned int i = 0; i < trackscenevec[BoundScene]->zorder.size(); ++i) {
        order[i] = trackscenevec[BoundScene]->zorder[i];
    }
}

//================================================================
float get_scene_track_nextpos(int trackid) {
    if (!is_track(BoundScene, trackid))
        return 0.0f;

    return trackscenevec[BoundScene]->trackvec[trackid]->nextPos;
}

void set_remote_controlled(bool b) {
    is_remote_controlled = b;
}

bool get_remote_controlled() {
    return is_remote_controlled;
}

void reset_default_track_ypos() {
    defaultTrackPositionY = 0.0f;
}

int get_render_mode() {
    return renderMode;
}

void set_render_mode(int m) {
    renderMode = m;
}

static bool sort_by_depth(CoreSection *cs1, CoreSection *cs2) {
    return (cs1->depth <= cs2->depth);
}

// Return (in outVec) all non-NULL elements of track->modelvec for specified track,
// whose validity should be verified by caller
static void get_clean_model_vector(const int trackid, std::vector<CoreSection *> &outVec) {
    TrackSceneNode *track = get_scene_track(trackid);

    // create vector of all non-NULL elements in track->modelvec
    std::vector<CoreSection *>::iterator modelIt;
    for (modelIt = track->modelvec.begin(); modelIt != track->modelvec.end(); modelIt++) {
        if (*modelIt != NULL)
            outVec.push_back(*modelIt);
    }
}

// Offset odd sections in specified track along non-depth axis
void stagger_track_sections(const int trackid, const bool stagger) {
    if (!is_track(trackid)) {
        printf("stagger_track_sections() failed: invalid track %d\n", trackid);
        return;
    }

    TrackSceneNode *track = get_scene_track(trackid);

    // Stagger in depth order: because track->modelvec isn't necessarily in depth order
    // (e.g. when a non-last section is deleted and re-added), need to sort ourselves.
    std::vector<CoreSection *> depthSortedVec;
    get_clean_model_vector(trackid, depthSortedVec);
    sort(depthSortedVec.begin(), depthSortedVec.end(), sort_by_depth);

    bool sectionWasOffset = false;

    if (stagger) {
        for (unsigned int i = 1; i < depthSortedVec.size(); i++) {
            if (i % 2 != 0)  // offset odd sections
            {
                CoreSection *cs = depthSortedVec[i];

                // offset by height (non-depth axis) of core section
                float dpix, dpiy;
                get_canvas_dpi(0, &dpix, &dpiy);
                const float secHeightAxisDPI = (cs->orientation == LANDSCAPE ? dpiy : dpix);
                const float secHeight = (cs->orientation == LANDSCAPE ? cs->height : cs->width);
                const float secHeightPix = secHeight * INCH_PER_CM * secHeightAxisDPI;
                const float offset = secHeightPix * -1.0f;

                cs->py += offset;
                sectionWasOffset = true;
            }
        }
    } else  // unstagger
    {
        // When unstaggering, need to consider every section in the track as the user
        // could move already-staggered sections around in such a way that a staggered
        // section is non-odd in the depth order.

        // find height of lowest section - remember that y increases as we move "down"
        // the canvas, so we're actually looking for the largest value.
        int i = 0;
        float lowHeight = FLT_MIN;
        for (unsigned int i = 0; i < depthSortedVec.size(); i++) {
            CoreSection *cs = depthSortedVec[i];
            if (cs->py > lowHeight)
                lowHeight = cs->py;
        }

        // move all sections to lowest section's height
        for (unsigned int i = 0; i < depthSortedVec.size(); i++) {
            CoreSection *cs = depthSortedVec[i];
            cs->py = lowHeight;
            sectionWasOffset = true;
        }
    }

    if (sectionWasOffset)
        track->staggered = stagger;
}

// Trim visible interval of selected section (sectionid), or selected section
// and all deeper sections, in specified track.
void trim_sections(const int trackid, const int sectionid, const float trim,
                   const bool fromBottom, const bool trimSelAndDeeper) {
    if (!is_track(trackid)) {
        printf("trim_sections() failed: invalid track %d\n", trackid);
        return;
    }

    if (!is_section_model(get_scene_track(trackid), sectionid)) {
        printf("trim_sections() failed: invalid section %d\n", sectionid);
        return;
    }

    // Trim in depth order: because track->modelvec isn't necessarily in depth order
    // (e.g. when a non-last section is deleted and re-added), need to sort ourselves.
    std::vector<CoreSection *> depthSortedVec;
    get_clean_model_vector(trackid, depthSortedVec);
    sort(depthSortedVec.begin(), depthSortedVec.end(), sort_by_depth);

    bool startTrimming = false;
    std::vector<CoreSection *>::iterator modelIt;
    for (modelIt = depthSortedVec.begin(); modelIt != depthSortedVec.end(); modelIt++) {
        CoreSection *cs = *modelIt;

        if (cs->section == sectionid)
            startTrimming = true;

        if (!startTrimming)
            continue;

        if (fromBottom)
            cs->intervalBottom -= trim;
        else
            cs->intervalTop += trim;

        // clamp values
        if (cs->intervalBottom > cs->width)
            cs->intervalBottom = cs->width;
        if (cs->intervalTop < 0)
            cs->intervalTop = 0;

        // enforce minimum visible interval width of .5cm
        const float visibleInterval = cs->intervalBottom - cs->intervalTop;
        const float minVisibleInterval = 0.5f;
        if (visibleInterval < minVisibleInterval) {
            const float underage = minVisibleInterval - visibleInterval;
            if (fromBottom)
                cs->intervalBottom += underage;
            else
                cs->intervalTop -= underage;
        }

        if (!trimSelAndDeeper)
            break;  // only trimming selected section, we're done
    }
}

// Starting from specified section, position all deeper sections such that
// there are no gaps between them.
void stack_sections(const int trackid, const int sectionid) {
    if (!is_track(trackid)) {
        printf("stack_sections() failed: invalid track %d\n", trackid);
        return;
    }

    if (!is_section_model(get_scene_track(trackid), sectionid)) {
        printf("trim_sections() failed: invalid section %d\n", sectionid);
        return;
    }

    TrackSceneNode *track = get_scene_track(trackid);

    // Stack in depth order: because track->modelvec isn't necessarily in depth order
    // (e.g. when a non-last section is deleted and re-added), need to sort ourselves.
    std::vector<CoreSection *> depthSortedVec;
    get_clean_model_vector(trackid, depthSortedVec);
    sort(depthSortedVec.begin(), depthSortedVec.end(), sort_by_depth);

    const int numSections = depthSortedVec.size();

    printf("Stacking %d sections of track %s\n", numSections, track->name);
    std::vector<CoreSection *>::iterator modelIt;
    bool startStacking = false;
    for (modelIt = depthSortedVec.begin(); modelIt + 1 != depthSortedVec.end(); modelIt++) {
        CoreSection *cs1 = *modelIt;
        CoreSection *cs2 = *(modelIt + 1);

        // once we've found the "base" section from which stacking should begin,
        // set a flag and commence stacking
        if (cs1->section == sectionid)
            startStacking = true;

        if (!startStacking)
            continue;

        // convert depth axis of section 1 to pixels
        float dpix, dpiy;
        get_canvas_dpi(0, &dpix, &dpiy);
        const float secDepthAxisDPI = (cs1->orientation == LANDSCAPE ? dpix : dpiy);
        const float secDepth = (cs1->orientation == LANDSCAPE ? cs1->width : cs1->height);

        // determine visible interval and use for stacking (rather than full height/width)
        const float visibleSecDepth = secDepth - cs1->intervalTop - (secDepth - cs1->intervalBottom);
        const float visibleSecDepthInPix = cs1->px + (visibleSecDepth * INCH_PER_CM * secDepthAxisDPI);

        // abut top of section 2 to end of section 1
        cs2->px -= (cs2->px - visibleSecDepthInPix);

        // adjust depth or sections can be mistakenly culled from drawing
        cs2->depth = cs2->px * CM_PER_INCH / dpix;
    }
}

//=======================================================================
// Get distance from point (px,py) to nearest point on line segment (x0,y0) -> (x1,y1).
// From https://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
float pt_to_line_dist(float px, float py, float x0, float y0, float x1, float y1) {
    const float a = px - x0;
    const float b = py - y0;
    const float c = x1 - x0;
    const float d = y1 - y0;

    const float dot = a * c + b * d;
    const float len_sq = c * c + d * d;
    float param = -1;
    if (len_sq != 0) {
        param = dot / len_sq;
    }
    float xx, yy;
    if (param < 0) { // (x0,y0) closest
        xx = x0;
        yy = y0;
    } else if (param > 1) { // (x1,y1) closest
        xx = x1;
        yy = y1;
    } else { // point (xx,yy) on segment (x0,y0) -> (x1,y1) closest
        xx = x0 + param * c;
        yy = y0 + param * d;
    }

    const float dx = px - xx;
    const float dy = py - yy;
    return sqrt(dx * dx + dy * dy);
}

// return distance between (ax,ay) and (bx,by)
float pt_to_pt_dist(float ax, float ay, float bx, float by) {
    const float dx = bx - ax;
    const float dy = by - ay;
    return sqrt(dx * dx + dy * dy);
}