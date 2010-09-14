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
 
#include "trackscenenode.h"
#include "textureresource_ex.h"
#include "graph.h"
#include "freedraw.h"
#include "canvas.h"

//================================================================
void update_track_dimensions(TrackSceneNode* t);

//================================================================
void create_track(const char* sessionName, const char* trackName, TrackSceneNode*& ptr)
{
    TrackSceneNode* t = new TrackSceneNode();

    // Session name
    t->sessionName = new char[strlen(sessionName) + 1];
    strcpy(t->sessionName, sessionName);

    // Track name
    t->name = new char[strlen(trackName) + 1];
    strcpy(t->name, trackName);

    t->highlight = false;
    t->show = true;   
    t->selectedSection = -1;
    t->modelvec.clear();
    t->zorder.clear();
    t->px = 0;
    t->py = 0;
    t->w  = 0;
    t->h  = 0;
	t->nextPos = 0.0f;
	t->movable = false; // only in depth direction

    t->highlight_color = new GLfloat[3];
    t->highlight_color[0] = 1.0f;
    t->highlight_color[1] = 1.0f;
    t->highlight_color[2] = 0.0f;

    ptr = t;
}

//================================================================
int append_model( TrackSceneNode* t, CoreSection* c)
{
    if( !t || !c ) return -1;
    int pos = -1;
    
    for( int i = 0; i < t->modelvec.size(); ++i)
    {
        if( t->modelvec[i] == NULL )
        {
            t->modelvec[i] = c;
            
            // make sure we don't have more than one i in zorder
            if( t->zorder.size() == 0)
            {
                t->zorder.push_back(i);
                return i;
            }
            
            bool found = false;
            for( int k = 0; k < t->zorder.size(); ++k)
            {
                if( t->zorder[k] == i )
                {
                    if(!found)
                        found = true;
                    else
                        t->zorder[k] = -1;
                }
            }

            if(!found)
            {
                t->zorder.push_back(i);
                bring_model_front(t,i);
            }

            update_track_dimensions(t);
            return i;
        }
    }

    t->modelvec.push_back(c);
    t->zorder.push_back( t->modelvec.size() - 1);
    update_track_dimensions(t);

    return t->modelvec.size() - 1;
}

//================================================================
void free_track(TrackSceneNode* t)
{
    if( t )
    {
        free_all_models(t);
        delete t;
    }
}

//================================================================
void free_all_models(TrackSceneNode* t)
{
    if(!t) return;
    for( int i = 0; i < t->modelvec.size(); ++i)
    {
        free_section_model(t->modelvec[i]);
        t->modelvec[i] = NULL;
    }

    t->modelvec.clear();
    t->zorder.clear();

    delete [] t->sessionName;
    delete [] t->name;
    delete [] t->highlight_color;
}

//================================================================
void free_model(TrackSceneNode* t, int section)
{
    if(!is_section_model(t,section)) return;
    free_section_model( t->modelvec[section]);
    t->modelvec[section] = NULL;
}

//================================================================
bool is_section_model(TrackSceneNode* t, int section)
{
    if(!t) return false;
    if(section < 0 || section > t->modelvec.size() - 1) return false;
    return (t->modelvec[section] != NULL);
}

//================================================================
void highlight_model(TrackSceneNode* t, int section, bool highlight)
{
    if(!is_section_model(t,section)) return;
    t->modelvec[section]->highlight = highlight;
}

void setShow(TrackSceneNode* t, bool isShow)
{
    if(t)
    {
        t->show = isShow;
    }
}

//================================================================
void bring_model_front(TrackSceneNode* t, int section)
{
    if(!is_section_model(t,section)) return;
    
    // find it in the zorder
   
    int i;
    int z = -1;
    for( i = 0; i < t->zorder.size(); ++i)
    {
        if( t->zorder[i] == section )
        {
            z = i;
            i = t->zorder.size();
        }
    }

    // never found!!!

    if( z < 0 )
    {
        printf("NEVER FOUND SECTION %d\n", section);
        t->zorder.push_back(section);
        return;
    }

    for( i = z; i > 0; --i)
        t->zorder[i] = t->zorder[i - 1];

    t->zorder[0] = section;
}

//================================================================
void render_track(TrackSceneNode* t, Canvas* c)
{
#ifdef DEBUG
    printf("\n\t--- Render Track ---\n");
#endif

    if( !t || !c ) 
    {
#ifdef DEBUG
        printf("Track or Canvas is NULL!\n");
#endif
        return;
    }

    if(!t->show) return;

    // move our canvas
    float x, y, z;

#ifdef DEBUG
    get_camera_position(c->camera, &x, &y, &z);
    printf("Track b4: Camera position %.2f %.2f\n", x, y);
    printf("Track translation: %.2f %.2f\n", -t->px, -t->py);
#endif

    // orig: translate_camera( c->camera, -t->px, -t->py, 0);
    if(get_horizontal_depth()) {
        translate_camera( c->camera, -t->px, -t->py, 0);
    } else {
        translate_camera( c->camera, t->py, -t->px, 0);
    }

    glTranslatef( t->px, t->py, 0);

#ifdef DEBUG
    get_camera_position( c->camera, &x, &y, &z);
    printf("Track after: Camera position %.2f, %.2f\n", x, y);
    printf("Track has zorder of size %d\n", t->zorder.size());
#endif

    int i;
    // draw back to front
    glPushMatrix();
    for( i = t->zorder.size() - 1; i > - 1; --i)
    {
#ifdef DEBUG
        printf("Rendering Core Section %d\n", t->zorder[i]);
#endif

        if( t->zorder[i] >= 0 && t->modelvec[ t->zorder[i] ] != NULL)
            render_section_model( t->modelvec[ t->zorder[i] ], c);
    }
    glPopMatrix();

    // draw plugin free draw rectangles, scale so x,y,w,h are in meters
    glPushMatrix();
    float scale = 1.0 / c->dpi_x * CM_PER_INCH / 100.0f;
    float indep_scale = c->w / c->w0;
    glScalef(1.0 /scale,1.0 /scale,1.0 /scale);
    for( i = 0; i < t->freedrawvec.size(); i++)
    {
        if( is_free_draw_scale_independent(t->freedrawvec[i]))
        {
            glScalef( indep_scale, indep_scale, 1.0f);
            render_free_draw(t->freedrawvec[i]);
            glScalef( 1.0 / indep_scale, 1.0 / indep_scale, 1.0f);
        }
        else
        {
            render_free_draw(t->freedrawvec[i]);

        }
    }

    glPopMatrix();

    glTranslatef( -t->px, -t->py, 0);

    // orig: translate_camera( c->camera, t->px, t->py, 0);
    if(get_horizontal_depth()) {
        translate_camera( c->camera, t->px, t->py, 0);
    } else {
        translate_camera( c->camera, -t->py, t->px, 0);
    }
}

//================================================================
CoreSection* get_track_section(TrackSceneNode* t, int section)
{
    if(!is_section_model(t,section)) return NULL;
    return t->modelvec[section];
}

//================================================================
int get_track_section_zorder_length(TrackSceneNode* t)
{
    if(!t) return 0;
    return t->zorder.size();
}

//================================================================
void get_track_section_zorder(TrackSceneNode* t, int* order)
{
    if(!t || !order) return;
    for( int i = 0; i < t->zorder.size(); ++i)
    {
        order[i] = t->zorder[i];
    }
}

//================================================================
void update_track_dimensions(TrackSceneNode* t)
{
    if(!t) return;
    float max_x = 0.0, max_y = 0.0;
    float min_x = 0.0, min_y = 0.0;

    for( int i = 0; i < t->zorder.size(); ++i)
    {
        CoreSection* c = t->modelvec[ t->zorder[i] ];
        if( !c ) continue;
        
        if( c->px < min_x ) min_x = c->px;
        if( c->py < min_y ) min_y = c->py;
        
        float cw, ch;
        float gw, gh;

        cw = get_texset_src_width( c->src );
        ch = get_texset_src_height( c->src );
        
        if(c->orientation == PORTRAIT)
        {
            float t;
            t  = cw;
            cw = ch;
            ch = t;
        }

//        printf("Checking image of dims %f x %f, with dpis of %f, %f\n",
//               cw,ch,c->dpi_x,c->dpi_y);

        if( cw > 0)
        {
            // scale to describe number of inches actually covered
            cw /= c->dpi_x;
            if( c->px + cw > max_x) max_x = (c->px + cw);
        }

        if( ch > 0)
        {
            // scale to describe number of inches actually covered
            ch /= c->dpi_y;
            if( c->py + cw > max_y) max_y = (c->py + ch);
        }

        for( int i = 0; i < c->graphvec.size(); i++)
        {
            Box* b = get_graph_box( c, c->graphvec[i] );
            if(!b) continue;
            delete b;
        }
    }

    t->w = max_x - min_x;
    t->h = max_y - min_y;

//    printf("Updated track dimensions %f x %f\n", t->w, t->h);
}

//================================================================
void attach_free_draw_to_track(TrackSceneNode* t, int fdid)
{
    if(!t) return;
    if(!is_free_draw_rectangle(fdid)) return;
    for( int i = 0; i < t->freedrawvec.size(); i++)
    {
        if( t->freedrawvec[i] == -1)
        {
            t->freedrawvec[i] = fdid;
            return;
        }
    }

    t->freedrawvec.push_back(fdid);

}

//================================================================
void detach_free_draw_from_track(TrackSceneNode* t, int fdid)
{
    if(!t) return;
    if(!is_free_draw_rectangle(fdid)) return;

    for( int i = 0; i < t->freedrawvec.size(); i++) 
    {
        if( t->freedrawvec[i] == fdid)
        {
            t->freedrawvec[i] = -1;
            return;
        }
    }
}

//================================================================
void push_section_to_end(TrackSceneNode* t, int section)
{
    if(!is_section_model(t,section)) return;
    float dpix, dpiy;
    get_canvas_dpi( 0, &dpix, &dpiy );

    float max_x = 0.0f;
    CoreSection* max_sec = NULL;

    for( int i = 0; i < t->modelvec.size(); i++)
    {
        if(!is_section_model(t,i)) continue;
        if( i == section ) continue;

        CoreSection* cs = get_track_section( t, i);
        if( cs->px >= max_x )
        {
            max_x = cs->px;
            max_sec = cs;
        }
    }

#ifdef DEBUG
    printf("Max x: %f, Max Sec %d\n", max_x, max_sec);
#endif

    if( max_sec == NULL ) return;

    CoreSection* sec = get_track_section( t, section );

    // do we have image in max_sec?
	if (max_sec->src != -1)
	{
		// we have section image
		sec->px = max_sec->px + 
			(get_texset_src_width(max_sec->src) * (dpix / max_sec->dpi_x));
	}
	else
	{
		// in case that the last one is graph only section
		// then, use section width instead of texture width
		sec->px = max_sec->px + max_sec->width * INCH_PER_CM * dpix;
	}
	
	// update section depth var
	sec->depth = sec->px * CM_PER_INCH / dpix;

#ifdef DEBUG
    printf("Final position for pushing to end %f\n", sec->px);
#endif

}

void set_track_highlight_color(TrackSceneNode* t, float r, float g, float b)
{
    if(!t) return;

    t->highlight_color[0] = r;
    t->highlight_color[1] = g;
    t->highlight_color[2] = b;
}
