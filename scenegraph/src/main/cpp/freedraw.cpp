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

#include "freedraw.h"
#include "canvas.h"
#include "trackscene.h"
#include "trackscenenode.h"
#include "coresection.h"
#include "textureresource_ex.h"
#include <vector>
#include <stdio.h>
#include <stdlib.h>

std::vector< PluginFreeDrawRectangle* > freedrawvec;

//==========================================================================
int create_free_draw_rectangle(int plugin, int track, int section,
                               float x, float y,
                               float w, float h)
{
    PluginFreeDrawRectangle *pfdr = new PluginFreeDrawRectangle();
    pfdr->plugin  = plugin;
    pfdr->track   = track;
    pfdr->section = section;
    pfdr->x       = x;
    pfdr->y       = y;
    pfdr->w       = w;
    pfdr->h       = h;
    pfdr->scale_independent = false;
    pfdr->visible = true;
    //find the first empty slot
    for( int i = 0; i < freedrawvec.size(); i++)
    {
        if( freedrawvec[i] == NULL)
        {
            freedrawvec[i] = pfdr;
            return i;
        }
    }

    freedrawvec.push_back(pfdr);
    return freedrawvec.size() - 1;
}

//==========================================================================
void free_free_draw_rectangle(int fdid)
{
    if(!is_free_draw_rectangle(fdid)) return;
    TrackSceneNode* tsn;
    CoreSection* cs;
    tsn = get_scene_track( freedrawvec[fdid]->track);
    cs = get_track_section( tsn, freedrawvec[fdid]->section );

    if( cs )
        detach_free_draw_from_section( cs, fdid );
    else if( tsn )
        detach_free_draw_from_track( tsn, fdid );
    else
        detach_free_draw_from_scene( fdid );

    delete freedrawvec[fdid];
    freedrawvec[fdid] = NULL;
}

//==========================================================================
bool is_free_draw_rectangle(int fdid)
{
//    printf("Checking if %d is a free draw rectangle\n", fdid);
    if( fdid < 0 ) return false;
//    printf("> 0\n");
    int vecsize = freedrawvec.size();
//    printf("%d\n", vecsize);
    if( fdid >= vecsize) return false;
//    printf("< %d\n", vecsize);
    return (freedrawvec[fdid] != NULL);
}

//==========================================================================
int get_free_draw_plugin(int fdid)
{
    if( !is_free_draw_rectangle(fdid)) return -1;
    return freedrawvec[fdid]->plugin;
}


//==========================================================================
int get_free_draw_track(int fdid)
{
    if( !is_free_draw_rectangle(fdid)) return -1;
    return freedrawvec[fdid]->track;
}


//==========================================================================
int get_free_draw_section(int fdid)
{
    if( !is_free_draw_rectangle(fdid)) return -1;
    return freedrawvec[fdid]->section;
}

//==========================================================================
float get_free_draw_width(int fdid)
{
    if( !is_free_draw_rectangle(fdid)) return 0.0f;
    return freedrawvec[fdid]->w;
}

//==========================================================================
float get_free_draw_height(int fdid)
{
    if( !is_free_draw_rectangle(fdid)) return 0.0f;
    return freedrawvec[fdid]->h;
}

//==========================================================================
float get_free_draw_x(int fdid)
{
    if( !is_free_draw_rectangle(fdid)) return 0.0f;
    return freedrawvec[fdid]->x;
}

//==========================================================================
float get_free_draw_y(int fdid)
{
    if( !is_free_draw_rectangle(fdid)) return 0.0f;
    return freedrawvec[fdid]->y;
}
//==========================================================================
bool is_free_draw_scale_independent( int fdid ) {
    if( !is_free_draw_rectangle(fdid))
        return false;
    return freedrawvec[fdid]->scale_independent;
}

//==========================================================================
bool is_free_draw_visible(int fdid) 
{
    if(!is_free_draw_rectangle(fdid)) return false;
    return freedrawvec[fdid]->visible;
}

//==========================================================================
void set_free_draw_width(int fdid, float w)
{
    if(!is_free_draw_rectangle(fdid)) return;
    freedrawvec[fdid]->w = w;
}

//==========================================================================
void set_free_draw_height(int fdid, float h)
{
    if(!is_free_draw_rectangle(fdid)) return;
    freedrawvec[fdid]->h = h;
}

//==========================================================================
void set_free_draw_x(int fdid, float x)
{
    if(!is_free_draw_rectangle(fdid)) return;
    freedrawvec[fdid]->x = x;
}

//==========================================================================
void set_free_draw_y(int fdid, float y)
{
    if(!is_free_draw_rectangle(fdid)) return;
    freedrawvec[fdid]->y = y;
}

//==========================================================================
void set_free_draw_scale_independence( int fdid, bool flag ) 
{
    if( !is_free_draw_rectangle(fdid))
        return;
    freedrawvec[fdid]->scale_independent = flag;
}

//==========================================================================
void set_free_draw_visibility( int fdid, bool flag) 
{
    if( !is_free_draw_rectangle(fdid)) return;
    freedrawvec[fdid]->visible = flag;
}

//==========================================================================
void render_free_draw(int fdid)
{
    try 
    {
#ifdef DEBUG
        printf("render_free_draw called %%%%%%%%%%%\n");
#endif
        if( !is_free_draw_rectangle(fdid)) return;
        if( !is_free_draw_visible(fdid)) return;

        PluginFreeDrawRectangle *pfdr = freedrawvec[fdid];

#ifdef DEBUG
        printf("PFDR Data:\nPlugin %d\nX: %f\tY: %f\n", pfdr->plugin,
               pfdr->x, pfdr->y);
        printf("W: %f\tH: %f\n", pfdr->w, pfdr->h);
#endif

        JNIEnv*   jenv = get_current_jnienv();
        if( !jenv ) return;

#ifdef DEBUG
        else printf("JNIEnv Yay\n");
#endif

        jobject   pmo  = get_plugin_manager_object();
        if( !pmo ) { printf("NULL PMO\n"); return; }

#ifdef DEBUG
        else printf("PMO Yay %d\n", pmo);
#endif

        jclass    pmc  = get_plugin_manager_class();
        if( !pmc ) { return; }

#ifdef DEBUG
        else printf("PMC Yay %d\n", pmc);
#endif

        jmethodID pmm  = get_plugin_manager_get_plugin();
        if( !pmm ) {  return; }

#ifdef DEBUG
        else printf("PMM Yay %d\n", pmm);
#endif

        jobject   cpo  = jenv->CallObjectMethod( pmo, pmm, 
                                                 pfdr->plugin);
        if( !cpo ) {  return; }

#ifdef DEBUG
        else printf("CPO Yay\n");
#endif

        jclass    cpc  = jenv->GetObjectClass(cpo);
        if( !cpc ) { return; }

#ifdef DEBUG
        else printf("CPC Yay\n");
#endif

        jmethodID cpm  = jenv->GetMethodID( cpc, "renderRectangle",
                                            "(IIIIFFFFF)V");
        if( !cpm ) { return; }
#ifdef DEBUG
        else printf("CPM Yay\n");

        printf("Calling object method!!!\n");
        fflush(stdout);
#endif
        
        int cur_canvas = get_current_canvas();

        // if the track id and section id != -1 then set the 
        // width as the the width of the section image scaled for the
        // canvas

        CoreSection *cs = get_track_section(get_scene_track(pfdr->track),
                                                pfdr->section);
        
        jenv->CallObjectMethod( cpo, cpm, fdid, cur_canvas,
                                pfdr->track, pfdr->section, 
                                pfdr->x, pfdr->y, pfdr->w, pfdr->h,
                                get_canvas_height(cur_canvas) /
                                get_canvas_orig_height(cur_canvas));
        
#ifdef DEBUG
        printf("Called Object Method!!!!\n");
#endif

    }
    catch( ... )
    {
        printf("Something very strange!\n");
    }
}

//==========================================================================
