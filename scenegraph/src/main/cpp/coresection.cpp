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
  

#include "coresection.h"
#include "graph.h"
#include "trackscene.h"
#include "freedraw.h"
#include "fontsys.h"
#include <math.h>

#if defined(__APPLE__) || defined(MACOSX)
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#else
#include <GL/glu.h>
#endif

#include "textureresource_ex.h"

int coresection_marker[NUM_CORE_MARKERS];
static bool showSectionText = true;

//================================================================
void create_section_model(int track, int section, CoreSection*& ptr)
{
    ptr = NULL;
    
    ptr = new CoreSection();
	ptr->g = NULL;
	ptr->dpi_x = ptr->dpi_y = 0;
	ptr->orientation = LANDSCAPE;
	ptr->rotangle = 0;
	ptr->src = -1;
    ptr->track     = track;
    ptr->section   = section;

    ptr->highlight = false;
    ptr->highlight_color = new GLfloat[3];

    // Default highlight color yellow
    ptr->highlight_color[0] = 1.0f;
    ptr->highlight_color[1] = 1.0f;
    ptr->highlight_color[2] = 1.0f;

    ptr->px        = 0.0f;
    ptr->py        = 0.0f;
    ptr->draw_vert_line = false;
    ptr->vert_line_x = -1.0f;
    ptr->movable = true;
	ptr->graphMovable = false;
	ptr->name = NULL;
	ptr->width = 0;
	ptr->height = 0;
	ptr->depth = 0;
	ptr->graph_offset = 0;

    ptr->intervalTop = 0;
    ptr->intervalBottom = ptr->width;

    ptr->parentTrack   = -1;
    ptr->parentSection = -1;
}

//================================================================
void add_section_image(int track, int section, int src, float rot, 
                          CoreSection*& ptr)
{
	if( !is_section_model( get_scene_track(track), section) ) return;
    ptr = get_track_section( get_scene_track(track), section);
    if( !ptr) return;

    create_model_grid(src, ptr->g);
    ptr->dpi_x     = get_texset_src_dpi_x(src);
    ptr->dpi_y     = get_texset_src_dpi_y(src);
    ptr->rotangle  = rot;
    ptr->src       = src;
    ptr->track     = track;
    ptr->section   = section;
    ptr->highlight = false;
    ptr->draw_vert_line = false;
    ptr->vert_line_x = -1.0f;
    ptr->movable   = true;
    inc_texset_ref_count(src);
	
	// in cm
	ptr->width = (float)get_texset_src_width(src) * CM_PER_INCH / ptr->dpi_x;
	ptr->height = (float)get_texset_src_height(src) * CM_PER_INCH / ptr->dpi_y;

    ptr->intervalTop = 0;
	ptr->intervalBottom = (ptr->width > ptr->height) ? ptr->width : ptr->height;

#ifdef DEBUG
    printf("---> [DEBUG] texset_w, texset_h : %.3f, %.3f, %.3f, %.3f, %.3f\n",
           (float)get_texset_src_width(src), (float)get_texset_src_height(src),
           ptr->dpi_x, ptr->dpi_y, CM_PER_INCH);

    printf("---> [DEBUG] Add a core section of (w, h) cm = (%.3f, %.3f)\n",
           ptr->width, ptr->height);
#endif

}

//================================================================
void free_section_model(CoreSection* ptr)
{
    if(!ptr) return;
    if (ptr->g != NULL)
	{
	    // free the texture model when reference count reaches 0
	    if(get_texset_ref_count(ptr->src) == 1)
	    {
		    free_model_grid(ptr->g);
		}
		ptr->g = NULL;
		dec_texset_ref_count(ptr->src);
	}

	if ( ptr->highlight_color )
		delete[] ptr->highlight_color;
	if ( ptr->name )
		delete[] ptr->name;

    delete ptr;
}

//================================================================
void render_section_graphs(CoreSection* ptr, Canvas *c)
{
    for (unsigned int i = 0; i < ptr->graphvec.size(); i++)
    {
		// render graph
		render_graph( c, ptr, ptr->graphvec[i] );
    }
}

//================================================================
void render_section_markers(CoreSection* ptr, Canvas *c)
{
    glColor3f(1,1,1);
    for (unsigned int i = 0; i < ptr->annovec.size(); i++)
    {
        CoreAnnotation* ca = ptr->annovec[i];
        if(ca)
        {
            AnnotationMarker m = ca->m;

            glTranslatef( m.px, m.py, 0);
            render_marker(c, &m);
            glTranslatef( -m.px, -m.py, 0);
        }
    }
}

//================================================================
void render_section_free_draw_rects(CoreSection *ptr, Canvas *c)
{
    for (unsigned int i = 0; i < ptr->freedrawvec.size(); i++)
    {
        render_free_draw( ptr->freedrawvec[i] );
    } 
}

//================================================================
/** deprecated
void render_section_vert_line(CoreSection *ptr, Canvas *c)
{
    if( !ptr->draw_vert_line || ptr->vert_line_x < 0 )
    {
#ifdef DEBUG
        printf("NOT DRAWING VERT LINE!!! %f\n", ptr->vert_line_x);
#endif
        return;
    }
    float h, dpiy;
    if( ptr->rotangle == 0.0f)
    {
        h = get_texset_src_width(ptr->src);
        if( ptr->vert_line_x / c->dpi_x * ptr->dpi_x > h) 
        {
            return;
        }
        h = get_texset_src_height(ptr->src);
        dpiy = ptr->dpi_y;
    }
    else if( ptr->rotangle == 90.0f)
    {
        h = get_texset_src_height(ptr->src);
        if( ptr->vert_line_x / c->dpi_y * ptr->dpi_y > h)
        {
            return;
        }
        h = get_texset_src_width(ptr->src);
        dpiy = ptr->dpi_x;
    }

    // determine how much vertical space to cover
    glEnable(GL_LINE_STIPPLE);
    glDisable(GL_TEXTURE_2D);
    glColor3f(1,1,1);
    glLineStipple(5,0x8888);

    float vy = 0.0f;
    for( int i = 0; i < ptr->graphvec.size(); i++)
    {
        Box* b = get_graph_box(ptr,ptr->graphvec[i]);
        if( vy > b->y )
            vy = b->y;
        delete b;
    }

    glBegin(GL_LINES);
    {
        glVertex3f( ptr->vert_line_x * ptr->dpi_x / c->dpi_x, h , 0.0f);
        glVertex3f( ptr->vert_line_x * ptr->dpi_x / c->dpi_x, vy * dpiy, 0.0f);
    }
    glEnd();
    glDisable(GL_LINE_STIPPLE);
    glEnable(GL_TEXTURE_2D);
}
*/

// Depth-culling core sections
bool outsideView(const CoreSection* ptr, const Canvas *c)
{
    if( !ptr || !c )
    {
        return true;
    }

    // determine coverage area
    float x, y, z;
    get_camera_position(c->camera, &x, &y, &z);

    float cl = x;                  // left
    float cr = x + c->coverage_x;  // right
    float cu = y;                  // up
    float cd = y + c->coverage_y;  // down

    // Coverage in meters
    // depth(x)-direction
    float depthStart = (2.54f * cl) / (c->dpi_x * 100.0f);
    float depthEnd   = (2.54f * cr) / (c->dpi_x * 100.0f);

    // (y)-direction
    float wStart     = (2.54f * cu) / (c->dpi_y * 100.0f);
    float wEnd       = (2.54f * cd) / (c->dpi_y * 100.0f);

    // Core section information
    float length = ptr->orientation ? ptr->height : ptr->width;
    float width  = ptr->orientation ? ptr->width  : ptr->height;

    float sectionStart = ptr->depth / 100.0f;
    float sectionEnd   = (ptr->depth + length) / 100.0f;

    float yStart = (2.54f * ptr->py) / (c->dpi_y * 100.0f);
    float yEnd   = yStart + (width / 100.0f);

    // Swap comparing orientation if different depth orientation
    float xSmall = get_horizontal_depth() ? depthStart : wStart;
    float xBig   = get_horizontal_depth() ? depthEnd   : wEnd;

#ifdef DEBUG
    printf("- Depth coverage in coresection depths: %.4f - %.4f\n", depthStart, depthEnd);
    printf("  - Section depth, width, height: %.4f, %.4f, %.4f\n", ptr->depth/100.0f, ptr->width/100.0f, ptr->height/100.0f);

    printf("- Width coverage in coresection widths: %.4f - %.4f\n", wStart, wEnd);
    printf("  - Section yStart, yEnd: %.4f, %.4f\n", yStart, yEnd);
#endif

    // Comparing depth(x)-direction
    if( (sectionStart > xBig) && (sectionEnd > xBig) )
    {
        return true;
    }
    else if( (sectionStart < xSmall) && (sectionEnd < xSmall) )
    {
        return true;
    }

    // Y-direction: only if both yStart & yEnd < than coverageYMin return true
    //              Greater than need to consider graphs & gaps
    //              Also need to know Track's py
    /*
    float ySmall = get_horizontal_depth() ? wStart : -depthEnd;
    float yBig   = get_horizontal_depth() ? wEnd   : -depthStart;

    if( (yStart < ySmall) && (yEnd < ySmall) )
    {
        return true;
    }
    */

    return false;
}

void render_source_arrow(CoreSection * ptr, Canvas * c)
{
    // render an arrow from the source center to ptr center
    if( !ptr || !c )
    {
        return;
    }

    TrackSceneNode * myTrack = get_scene_track(0, ptr->track);
    if(!myTrack)
    {
        printf("Null myTrack\n");
        return;
    }

    // defaultTrackScene:0
	TrackSceneNode * srcTrack = get_scene_track(0, ptr->parentTrack);
    CoreSection * srcCoreSection = get_track_section(srcTrack, ptr->parentSection);

    if(!srcCoreSection)
    {
        return;
    }

    // convert to canvas space (dot)
    float imageWidth  = c->dpi_x * (get_texset_src_width(ptr->src) / ptr->dpi_x);
    float imageHeight = c->dpi_y * (get_texset_src_height(ptr->src) / ptr->dpi_y);

    if(ptr->orientation)
    {
        float tmp = imageWidth;
        imageWidth = imageHeight;
        imageHeight = tmp;
    }

    // find coords in scene space
    float middle = c->dpi_x * (ptr->intervalTop + ptr->intervalBottom)/(2.0f * 2.54f);
    float fromX = (srcTrack->px + srcCoreSection->px + middle) - (myTrack->px + ptr->px);
    float fromY = (srcTrack->py + srcCoreSection->py) - (myTrack->py + ptr->py) + imageHeight;
    
    float toX   = middle;
    float toY   = 0.0f;

    int signX = 1;
    int signY = 1;

    // relative track position
    if((myTrack->py - imageHeight) < srcTrack->py)
    {
        fromY = fromY - imageHeight;
        toY   = imageHeight;

        signY = -1;
    }

    // the arrow coords
    float pi = 3.14f;
    float vecLength = sqrt((fromX-toX)*(fromX-toX) + (fromY-toY)*(fromY-toY));
    float theta = acos(fabs(fromX - toX)/vecLength);

    // relative X
    if(toX > fromX)
    {
        signX  = -1;
    }
    float l = 50.0f;

    float x0 = toX + signX * l * cos(theta - signX * pi/6.0f);
    float y0 = toY - l * sin(theta - signX * pi/6.0f);
    
    float x1 = toX + signX * l * cos(theta + signX * pi/6.0f);
    float y1 = toY - l * sin(theta + signX * pi/6.0f);

    // highlighting sub-box coordinates in source image
    float deX = c->dpi_x * fabs(ptr->intervalBottom - ptr->intervalTop) / (2.0f * 2.54f);
    float aX = fromX - deX;
    float aY = fromY - signY * imageHeight;
    float bX = fromX + deX;
    float bY = fromY - signY * imageHeight;
    float cX = fromX + deX;
    float cY = fromY;
    float dX = fromX - deX;
    float dY = fromY;

    // draw
    glPushMatrix();
    {
        glBindTexture(GL_TEXTURE_2D, 0);
        glLineWidth(2.0);
        glColor3f(0.0, 1.0, 0.0);

        glPushMatrix();
        {
            if(signY == -1)
            {
                glTranslatef(0,  imageHeight, 0);
                glScalef(1, (float)signY, 1);
                glTranslatef(0, -imageHeight, 0);
            }

            // arrow head
            glBegin(GL_POLYGON);
            {
                glVertex2f(toX, toY);
                glVertex2f(x0, y0);
                glVertex2d(x1, y1);
            }
            glEnd();
        }
        glPopMatrix();

        // indication line
        glLineStipple(1, 0xAAAA);
        glEnable(GL_LINE_STIPPLE);    
        glBegin(GL_LINES);
        {
            glVertex2f(fromX, fromY);
            glVertex2f(toX, toY);
        }
        glEnd();

        // source sub-section box highlight
        if(ptr->highlight)
        {
            glBegin(GL_LINE_LOOP);
            {
                glVertex2f(aX, aY);
                glVertex2f(bX, bY);
                glVertex2f(cX, cY);
                glVertex2f(dX, dY);
            }
            glEnd();
        }

        glDisable(GL_LINE_STIPPLE);
    }
    glPopMatrix();
}

//================================================================
void render_section_model(CoreSection* ptr, Canvas *c)
{
    if(outsideView(ptr, c))
    {
        return;
    }

    // todo
    // printf("- Render mode: %d\n", get_render_mode());    

    // To have antialias lines
    glEnable(GL_LINE_SMOOTH);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE) ;

    if( !ptr || !c ) return;
    
    // determine coverage area
    
    float origcoverage_x = c->coverage_x;
    float origcoverage_y = c->coverage_y;

    // scale our coverage
    
    if( ptr->dpi_x == 0) ptr->dpi_x = 150.0;
    if( ptr->dpi_y == 0) ptr->dpi_y = 150.0;

    float scale[2]; 
    scale[0] = c->dpi_x / ptr->dpi_x;
    scale[1] = c->dpi_y / ptr->dpi_y;

    c->coverage_x      /= scale[0];
    c->coverage_y      /= scale[1];
    c->half_coverage_x  = 0.5f * c->coverage_x;
    c->half_coverage_y  = 0.5f * c->coverage_y;

    float x, y, z, r;
#ifdef DEBUG
    get_camera_position(c->camera, &x, &y, &z);
    printf("@CoreSection: Camera Position B4 %.2f, %.2f\n", x, y);
#endif    

    // Question: 2 opposite translation? why?
    // Answer:   one for camera, and the other for drawing the core images(using GL)

    // apply to Camera numbers
    if(get_horizontal_depth()) {
        translate_camera(c->camera, -ptr->px, -ptr->py, 0);
    } else {
        translate_camera(c->camera, -ptr->py, -ptr->px, 0);
    }

    // Apply to CoreSection GL related rendering modelview matrix
    glTranslatef(ptr->px, ptr->py, 0);

    float tx, ty;
    get_camera_position( c->camera, &x, &y, &z);

#ifdef DEBUG
    printf("@CoreSection: Camera Position After %.2f, %.2f\n", x, y);
    printf("@CoreSection: Translation %.2f, %.2f\n", ptr->px, ptr->py);
#endif

    // take into account the scale of the image and modify our camera position

    tx = x / scale[0];
    ty = y / scale[1];

#ifdef DEBUG
    printf("Scaled camera translation: %.2f %.2f\n", tx, ty);
#endif
    
    position_camera(c->camera, tx, ty, z); // will affect model range comparision

    // scale our object

#ifdef DEBUG
    printf("Section DPI %f, %f\n", ptr->dpi_x, ptr->dpi_y);
    printf("Scaling %f, %f\n", scale[0], scale[1]);
#endif

	
	// Draw Section Image
	// check section has section image or not
	if (ptr->g)
	{
		glScalef( scale[0], scale[1], 1.0 );

		glPushMatrix();  // PUSHING MATRIX BEFORE RENDER MODEL
		{
            // define visible interval additional clipping
            float scaleFactorX = ptr->dpi_x / CM_PER_INCH;
            float topPx    = ptr->intervalTop * scaleFactorX;
            float bottomPx = ptr->intervalBottom * scaleFactorX;

		    float centroidShiftX;
		    float centroidShiftY;

		    if(ptr->orientation == PORTRAIT)
		    {
		        centroidShiftX = (topPx + bottomPx)/2.0f;
		        centroidShiftY = get_texset_src_width(ptr->src)/2.0f;
            }
            else
            {
                centroidShiftX = (topPx + bottomPx)/2.0f;
                centroidShiftY = get_texset_src_height(ptr->src)/2.0f;
            }

            // real rotation (relative to the center of the visible image/model)
		    glTranslatef(centroidShiftX, centroidShiftY, 0);
		    glRotatef(ptr->rotangle, 0, 0, 1);
		    glTranslatef(-centroidShiftX, -centroidShiftY, 0);
		    {
                float camx, camy, camz;
                float covw, covh;
                camx = tx;
                camy = ty;
                camz = 0;
                covw = c->coverage_x;
                covh = c->coverage_y;

                // do rotations and modify what the coverage and camera positions are
                // as necessary
                if(ptr->orientation == PORTRAIT) // portrait, need a rotation & translation
                {
                    float t, w;
                    w = (float)get_texset_src_width(ptr->src);

                    // swap coverage dimensions
                    t = c->coverage_x;
                    c->coverage_x = c->coverage_y;
                    c->coverage_y = t;
                    r = -90;

                    // position the camera properly for render_model to work right
                    // for 90 degrees CC there's some funny translating
                    float nx = -camy - c->coverage_x + w;

                    position_camera(c->camera,
                                    nx,
                                    camx,
                                    camz);

#ifdef DEBUG
                    // turn_camera(c->camera, 0, 3.14/2, 0);\

                    float imgW = (float)get_texset_src_width(ptr->src);
                    float imgH = (float)get_texset_src_height(ptr->src);

                    printf("imgW: %.2f imgH: %.2f\n", imgW, imgH);
                    printf("scale: %.2f %.2f\n", scale[0], scale[1]);
#endif

                    if(!get_horizontal_depth()) {
                        translate_camera(c->camera,
                                         (float)-get_texset_src_width(ptr->src), // up-down
                                         (float)get_texset_src_width(ptr->src), // left-right
                                         0);
                    }

                    glTranslatef(0, (float)get_texset_src_width(ptr->src), 0);
                    glRotatef(r, 0, 0, 1);

                    // define visible interval clipping planes
                    // Clipping based on visible interval settings
                    GLdouble topEqn[4]    = {0.0,  1.0, 0.0, -topPx+1};
                    GLdouble bottomEqn[4] = {0.0, -1.0, 0.0, bottomPx+1};

                    glClipPlane(GL_CLIP_PLANE0, topEqn);
                    glClipPlane(GL_CLIP_PLANE1, bottomEqn);
                }
                else // landscape
                {
                    // define visible interval clipping planes                
                    // Clipping based on visible interval settings
                    GLdouble topEqn[4]    = { 1.0, 0.0, 0.0, -topPx+1};
                    GLdouble bottomEqn[4] = {-1.0, 0.0, 0.0, bottomPx+1};

                    glClipPlane(GL_CLIP_PLANE0, topEqn);
                    glClipPlane(GL_CLIP_PLANE1, bottomEqn);
                }

                // make sure our base color is white
                float opacity = (ptr->highlight) ? 0.9f : 1.0f;
                glColor4f(1, 1, 1, opacity);

                //determine LOD based on the DPI of the image,
                //and the DPI of the screen

                int true_level = (int) floor(
                    logf(1.0f / scale[0] * (c->w / c->w0)) / logf(2.0));

                //for every factor of 5 we switch to a lower detail

                int numlevels = get_texset_num_levels( ptr->g->src );
                int lod = numlevels;

                do
                {
                    lod--;
                } while(
                    (lod > 0) &&
                    (get_texset_level_in_pyramid(ptr->g->src, lod) > true_level)
                );

                set_model_grid_lod(ptr->g, lod);

        #ifdef DEBUG
                printf("Assigning LOD of %d\n", lod);
                printf("ModelGrid set to LOD of %d\n", get_model_grid_lod(ptr->g));
        #endif

                // maybe render the interval bracket bars
                
                glEnable(GL_CLIP_PLANE0);
                glEnable(GL_CLIP_PLANE1);
                {
                    render_model_grid(ptr->g, c);
                    render_highlight(ptr, c);
                }
                glDisable(GL_CLIP_PLANE0);
                glDisable(GL_CLIP_PLANE1);

                // undo camera operations
                position_camera( c->camera, tx, ty, z);
                c->coverage_x = covw;
                c->coverage_y = covh;
			}
		}
		glPopMatrix(); // POPPING MATRIX PUSHED PRIOR TO RENDER MODEL
	
		// scale back to orig DPI scale
		glScalef( 1.0f / scale[0], 1.0f / scale[1], 1.0f );

	}	// end of drawing section image
	else
	{
		// here we don't have section image, so need to draw dummy rect
		// assume that we have graph in the section.
		glBindTexture(GL_TEXTURE_2D, 0);
		glLineWidth(BORDER_LINE_WIDTH);
		
		float lEnd = 0;
		float rEnd = lEnd + ptr->width * INCH_PER_CM * c->dpi_x;
		float thick = DEFAULT_SECTION_HEIGHT * INCH_PER_CM * c->dpi_y; // 10 cm dummy core
		// solid dummy section
		glColor4f(0.2f, 0.2f, 0.2f, 0.2f);
		glBegin(GL_QUADS);
		{
			glVertex2f(lEnd, 0);		// left upper
			glVertex2f(lEnd, thick);	// left lower
			glVertex2f(rEnd, thick);	// right lower
			glVertex2f(rEnd, 0);		// right upper
		}
		glEnd();

		// outline of dummy section
		glColor4f(0.4f, 0.4f, 0.4f, 1.0f);
		glBegin(GL_LINE_LOOP);
		{
			glVertex2f(lEnd, 0);		// left upper
			glVertex2f(lEnd, thick);	// left lower
			glVertex2f(rEnd, thick);	// right lower
			glVertex2f(rEnd, 0);		// right upper
		}
		glEnd();

		// Draw section name on this dummy
		if (ptr->name && showSectionText) {
			float gapx = thick * 0.3f;
			glColor3f(1.0f, 1.0f, 1.0f);
			glTranslatef(gapx, gapx * 2, 0);
			glScalef(2, 2, 1.0);
			render_string_shadowed(ptr->name, 0, strlen(ptr->name) - 1);
			glScalef(0.5f, 0.5f, 1.0f);
			glTranslatef(-gapx, -gapx * 2, 0);
		}

	}

    // Section Marker, scaling using marker images' DPI
    // instead of core section image's DPI
    // Put inside render_marker() method
    render_section_markers( ptr, c );
    render_section_free_draw_rects( ptr, c );

    // Draw source section arrow if it's in another track
    if((ptr->parentTrack) != -1 && (ptr->parentSection != -1))
    {
        if(ptr->parentTrack != ptr->track)
        {
            // if(ptr->highlight)
            {
                render_source_arrow(ptr, c);                    
            }
        }
    }

    // scale back to canvas/coresection DPI scaling
    glScalef( scale[0], scale[1], 1.0f );

    // set our area of coverage, scale and position back to normal
    c->coverage_x      = origcoverage_x;
    c->coverage_y      = origcoverage_y;
    c->half_coverage_x = origcoverage_x / 2.0f;
    c->half_coverage_y = origcoverage_y / 2.0f;

    glScalef( 1.0f / scale[0], 1.0f / scale[1], 1.0f );

    position_camera( c->camera, x, y, z);
    // turn_camera(c->camera, 0, -3.14/2, 0);

    // Render graphs, without core section image's DPI scale
    // Call after camera info are restored.
    render_section_graphs(ptr, c);

    glTranslatef( -ptr->px, -ptr->py, 0);

    // orig: translate_camera(c->camera, ptr->px, ptr->py, 0);
    if(get_horizontal_depth()) {
        translate_camera(c->camera, ptr->px, ptr->py, 0);
    } else {
        translate_camera(c->camera, ptr->py, ptr->px, 0);
    }
}

//================================================================
void init_section_annotation_markers()
{
    for( int i = 0; i < NUM_CORE_MARKERS; i++)
        coresection_marker[i] = -1;

    // REGISTER THE MARKER TYPE HERE
    
    coresection_marker[ CORE_POINT_MARKER   ] = 
        register_marker_type("Core Section Point Annotation Marker");
    coresection_marker[ CORE_SPAN_MARKER    ] =
        register_marker_type("Core Section Span Annotation Marker");
    coresection_marker[ CORE_OUTLINE_MARKER ] =
        register_marker_type("Core Section Outline Annotation Marker");

    // ALLOCATE THE MARKER RESOURCE HERE

    alloc_marker_type_resource(coresection_marker[ CORE_POINT_MARKER ],
                               "resources/coresection_point_marker.png");
    alloc_marker_type_resource(coresection_marker[ CORE_SPAN_MARKER ],
                               "resources/coresection_span_marker.png");
    alloc_marker_type_resource(coresection_marker[ CORE_OUTLINE_MARKER ],
                               "resources/coresection_outline_marker.png");

    // for clast & sample
    coresection_marker[ CORE_CLAST_MARKER ] =
        register_marker_type("Core Clast Marker");
    coresection_marker[ CORE_SAMPLE_MARKER ] =
        register_marker_type("Core Sample Requet Marker");

    alloc_marker_type_resource(coresection_marker[ CORE_CLAST_MARKER ],
                               "resources/coresection_clast_marker.png");
    alloc_marker_type_resource(coresection_marker[ CORE_SAMPLE_MARKER ],
                               "resources/coresection_sample_marker.png");

    // for defaultPropertyValue and Freeform markers
    coresection_marker[ CORE_PV_MARKER ] =
        register_marker_type("Core PropertyValues Marker");
    coresection_marker[ CORE_FREEFORM_MARKER ] =
        register_marker_type("Freeform Marker");

    alloc_marker_type_resource(coresection_marker[ CORE_PV_MARKER ],
                               "resources/coresection_pv_marker.png");
    alloc_marker_type_resource(coresection_marker[ CORE_FREEFORM_MARKER ],
                               "resources/coresection_freeform_marker.png");
}

//================================================================
void free_section_annotation_markers()
{
    for(int i = 0; i < NUM_CORE_MARKERS; i++)
        free_marker_type_resource( coresection_marker[i] );
}

//================================================================
int create_section_annotation(CoreSection* ptr, int group, int type,
                              float x, float y)
{
    if(!ptr) return -1;
    if(type < 0 || type >= NUM_CORE_MARKERS) return -1;

    CoreAnnotation* captr = new CoreAnnotation();
    captr->m.type = coresection_marker[ type ];
    captr->m.url  = NULL;
    captr->m.visibility = true;
    captr->m.group = group;
	captr->m.focused = false;

    // let the width and height be DEFAULT_MARKER_DPI_X * DEFAULT_MARKER_Y now,
    // units stored in image dots
    float m_gap  = GRAPH_SECTION_GAP;
    float m_dpix = DEFAULT_MARKER_DPI_X;
    float m_dpiy = DEFAULT_MARKER_DPI_Y;

    captr->m.w = m_gap * m_dpix;
    captr->m.h = m_gap * m_dpiy;

    // position the annotation marker using the depthValue and image dpi
    captr->m.px = x;
    captr->m.depthX = x;
	captr->m.depthY = y;
	if (y < 0) {
		// this is the case when we create annotation from old xml file
		// reset depthY as top of core section
		captr->m.depthY = 0;
	}
    captr->m.py = -captr->m.h * 1.5f;
    captr->sec_depth_value = x / ptr->dpi_x;

    // for now... tex coords are (0,0) on upper left to (1,1) on lower right
    captr->m.ultex[0] = 0.0f;
    captr->m.ultex[1] = 1.0f;
    captr->m.lrtex[0] = 1.0f;
    captr->m.lrtex[1] = 0.0f;

	// james addition for marker type
	switch (type) {
		case CORE_POINT_MARKER:
			captr->m.markerVt[0] = 0.0f;
			captr->m.markerVt[1] = 0.0f;
			captr->m.markerVt[2] = 0.0f;
			captr->m.markerVt[3] = 0.0f;
			captr->m.markerVt[4] = 0.0f;
			break;

		case CORE_SPAN_MARKER:
			captr->m.markerVt[0] = x - captr->m.w / 2.0f; 
			captr->m.markerVt[1] = -captr->m.h * 0.1f;
			captr->m.markerVt[2] = x + captr->m.w / 2.0f;
			captr->m.markerVt[3] = -captr->m.h * 0.1f;
			captr->m.markerVt[4] = 0.0f;
			break;

		case CORE_OUTLINE_MARKER:
			captr->m.markerVt[0] = x - captr->m.w / 2.0f;
			captr->m.markerVt[1] = y - captr->m.w / 2.0f; 
			captr->m.markerVt[2] = x + captr->m.w / 2.0f;
			captr->m.markerVt[3] = y + captr->m.w / 2.0f;
			captr->m.markerVt[4] = 0.0f;
			break;
			
		default:
			captr->m.markerVt[0] = 0.0f;
			captr->m.markerVt[1] = 0.0f;
			captr->m.markerVt[2] = 0.0f;
			captr->m.markerVt[3] = 0.0f;
			captr->m.markerVt[4] = 0.0f;
			break;
	}

    // find the first open slot, otherwise append to end
    for (unsigned int i = 0; i < ptr->annovec.size(); i++)
    {
        if (ptr->annovec[i] == NULL)
        {
            ptr->annovec[i] = captr;
            return i;
        }
    }

    ptr->annovec.push_back(captr);
    return ptr->annovec.size() - 1;
}

//================================================================
void free_section_annotation(CoreSection* ptr, int annoId)
{
    if(!is_section_annotation(ptr,annoId)) return;
    delete ptr->annovec[annoId];
    ptr->annovec[annoId] = NULL;
}

//================================================================
bool is_section_annotation(CoreSection* ptr, int annoId)
{
    if (!ptr || annoId < 0) return false;
	const int annoVecSize = ptr->annovec.size() - 1;
    if (annoId > annoVecSize) return false;
    return (ptr->annovec[annoId] != NULL);
}

//================================================================
void  set_section_annotation_focus (CoreSection* ptr, int annoId, bool value)
{
    if (!ptr || annoId < 0) return;
	const int annoVecSize = ptr->annovec.size() - 1;
    if (annoId > annoVecSize) return;
    ptr->annovec[annoId]->m.focused = value;
}

//================================================================
void set_section_name(CoreSection* ptr, const char* name)
{
	if (!ptr || !name) return;
	
	if ( ptr->name != NULL ) // release existing name
	{
		delete[] ptr->name;
	}
	
	ptr->name = new char[ strlen(name) + 1 ];
    strcpy( ptr->name, name );
}

char* get_section_name(CoreSection* ptr)
{
    if(!ptr) return NULL;

    return ptr->name;
}

//================================================================
void set_section_annotation_width( CoreSection *ptr, int annoId, float w)
{
    if(!is_section_annotation(ptr,annoId)) return;
    if(w < 0) w = 0;
    ptr->annovec[annoId]->m.w = w;
}

//================================================================
void set_section_annotation_height( CoreSection *ptr, int annoId, float h)
{
    if(!is_section_annotation(ptr,annoId)) return;
    if(h < 0) h = 0;
    ptr->annovec[annoId]->m.h = h;
}

//================================================================
void set_section_draw_vert_line( CoreSection *ptr, bool draw, float x)
{
    if(!ptr) return;
    ptr->draw_vert_line = draw;
    ptr->vert_line_x = x;
}

//================================================================
float get_section_annotation_x (CoreSection* ptr, int annoId)
{
    if(!is_section_annotation(ptr,annoId)) return 0.0f;
	return ptr->annovec[annoId]->m.depthX;
}

//================================================================
float get_section_annotation_y (CoreSection* ptr, int annoId)
{
    if(!is_section_annotation(ptr,annoId)) return 0.0f;
	return ptr->annovec[annoId]->m.depthY;
}

//================================================================
// return length in cm, where "length" is the distance along CoreSection's depth axis
float get_section_length(CoreSection* cs)
{
	float length = 0.0f;
	if (cs)
	{
		if (cs->orientation == PORTRAIT)
			length = cs->height;
		else
			length = cs->width;
	}
	return length;
}

//================================================================
float get_section_annotation_icon_x(CoreSection* ptr, int annoId)
{
    if(!is_section_annotation(ptr,annoId)) return 0.0f;
	return ptr->annovec[annoId]->m.px;
}

//================================================================
float get_section_annotation_icon_y(CoreSection* ptr, int annoId)
{
    if(!is_section_annotation(ptr,annoId)) return 0.0f;
	return ptr->annovec[annoId]->m.py;
}

//================================================================
float get_section_annotation_vt0(CoreSection* ptr, int annoId)
{
    if(!is_section_annotation(ptr,annoId)) return 0.0f;
	return ptr->annovec[annoId]->m.markerVt[0];
}

//================================================================
float get_section_annotation_vt1(CoreSection* ptr, int annoId)
{
    if(!is_section_annotation(ptr,annoId)) return 0.0f;
	return ptr->annovec[annoId]->m.markerVt[1];
}

//================================================================
float get_section_annotation_vt2(CoreSection* ptr, int annoId)
{
    if(!is_section_annotation(ptr,annoId)) return 0.0f;
	return ptr->annovec[annoId]->m.markerVt[2];
}

//================================================================
float get_section_annotation_vt3(CoreSection* ptr, int annoId)
{
    if(!is_section_annotation(ptr,annoId)) return 0.0f;
	return ptr->annovec[annoId]->m.markerVt[3];
}

//================================================================
void attach_free_draw_to_section(CoreSection* ptr, int fdid)
{
    if (!ptr) return;
    if (!is_free_draw_rectangle(fdid)) return;

    for (unsigned int i = 0; i < ptr->freedrawvec.size(); i++)
    {
        if (ptr->freedrawvec[i] == -1)
        {
            ptr->freedrawvec[i] = fdid;
            return;
        }
    }

    ptr->freedrawvec.push_back(fdid);
    set_free_draw_x(fdid, ptr->px);
    set_free_draw_width(fdid, (float)get_texset_src_width(ptr->src));
}

//================================================================
void detach_free_draw_from_section(CoreSection* ptr, int fdid)
{
    if (!ptr) return;
    if (!is_free_draw_rectangle(fdid)) return;

    for (unsigned int i = 0; i < ptr->freedrawvec.size(); i++) 
    {
        if (ptr->freedrawvec[i] == fdid)
        {
            ptr->freedrawvec[i] = -1;
            return;
        }
    }

}

//================================================================
float get_section_graph_offset(CoreSection* ptr)
{
	if(!ptr) return 0.0f;
	return ptr->graph_offset;
}

//================================================================
/* Draw a selection outline if selected/highlighted */
void render_highlight(CoreSection* ptr, Canvas* c)
{
	if(ptr->highlight == true)
	{
        Box *bounds = new Box();
        bounds->x = 0.0f;
        bounds->y = 0.0f;
        bounds->w = (float)get_texset_src_width(ptr->src);
        bounds->h = (float)get_texset_src_height(ptr->src);

        glBindTexture(GL_TEXTURE_2D, 0);
        glLineWidth(5.0);

        // Determine highlight color
	    if(ptr->dpi_x < 254.0f || ptr->dpi_y < 254.0f)
	    {
            ptr->highlight_color[0] = 0.0f;
            ptr->highlight_color[1] = 1.0f;
            ptr->highlight_color[2] = 1.0f;
        }
        else
        {
            ptr->highlight_color[0] = 1.0f;
            ptr->highlight_color[1] = 1.0f;
            ptr->highlight_color[2] = 0.0f;
        }

        // update color if track is in different role
        TrackSceneNode *t = get_scene_track(0, ptr->track);
        if(!t)
        {
            glColor3fv(ptr->highlight_color);
        }

        if(!t->highlight_color)
        {
            if(ptr->highlight_color)
            {
                glColor3fv(ptr->highlight_color);
            }
            else
            {
                glColor3f(1.0, 1.0, 0.0);            
            }
        }
        else
        {
            // if track's highlight color is default color (1, 1, 0)
            // then respect section's own highlight color
            if(t->highlight_color[0] == 1.0f &&
               t->highlight_color[1] == 1.0f &&
               t->highlight_color[2] == 0.0f)
            {
                glColor3fv(ptr->highlight_color);
            }
            else
            {
                glColor3fv(t->highlight_color);
            }
        }

        // Turn on bloom shader
        // glUseProgram(BLOOM_SHADER);
        // glUseProgram(DEFAULT_SHADER);
        {
            glPushMatrix();
            {
                glBegin(GL_LINE_LOOP);
                {
                    glVertex2f(bounds->x, bounds->y);
                    glVertex2f(bounds->x + bounds->w, bounds->y);
                    glVertex2f(bounds->x + bounds->w, bounds->y + bounds->h);
                    glVertex2f(bounds->x, bounds->y + bounds->h);
                }
                glEnd();
            }
            glPopMatrix();

            /*
            glPushMatrix();
            {
                float x_translate = 0.0f;
                float y_translate = 0.0f;
                float midPoint = (ptr->intervalTop + ptr->intervalBottom) / 2.0f;

    			if(ptr->orientation)
    			{
                    glRotatef(90, 0, 0, 1);

                    x_translate = (midPoint / 2.54) * ptr->dpi_y;
                    y_translate = -bounds->w;
                }
                else
                {
                    x_translate = (midPoint / 2.54) * ptr->dpi_x;
                    y_translate = 0.0f;
                }
                glTranslatef(x_translate, y_translate, 0);

                float scale = 1.0f;
                if(strlen(ptr->name) >= 15)
                {
                    scale = 5.0f;
                }
                else
                {
                    scale = 8.0f;
                }
    			glScalef(scale, scale, 1.0);
	    		render_string_shadowed(ptr->name, 0, strlen(ptr->name) - 1);
            }
            glPopMatrix();
            */
        }
        // Turn off special shader program
        // glUseProgram(DEFAULT_SHADER);
    }
}

bool is_show_section_text()
{
    return showSectionText;
}

void set_show_section_text(bool b)
{
    showSectionText = b;
}

int   get_section_parentTrackId(CoreSection * ptr)
{
    if(ptr)
    {
        return ptr->parentTrack;
    }

    return -1;
}

int   get_section_parentSectionId(CoreSection * ptr)
{
    if(ptr)
    {
        return ptr->parentSection;
    }

    return -1;
}

void  set_section_parentTrackId(CoreSection * ptr, int trackId)
{
    if(ptr) ptr->parentTrack = trackId;
}

void  set_section_parentSectionId(CoreSection * ptr, int sectionId)
{
    if(ptr) ptr->parentSection = sectionId;                                        
}

void  set_section_highlight_color(CoreSection * ptr, float r, float g, float b)
{
    if(ptr)
    {
        ptr->highlight_color[0] = r;
        ptr->highlight_color[1] = g;
        ptr->highlight_color[2] = b;
    }
}

bool  doesCoreSectionHasDuplicateWithInfo(CoreSection *cs, float x, float y, float v0, float v1, float v2, float v3,
                                          const char* urlString, const char* localString)
{
    if (!cs) return false;

    for (unsigned int i = 0; i < cs->annovec.size(); i++)
    {
        CoreAnnotation* ca = cs->annovec[i];
        if (ca)
        {
            AnnotationMarker m = ca->m;

            // Do the match with area, url string and local string
            if( !strcmp((&m)->url, urlString) && !strcmp((&m)->local_file, localString)
                && (v0 == m.markerVt[0]) && (v1 == m.markerVt[1]) && (v2 == m.markerVt[2]) && (v3 == m.markerVt[3])
              )
            {
                // printf("[Duplicate Debug] %.4f\t%.4f\t%.4f\t%.4f\n", m.depthX, x, m.depthY, y);
                return true;
            }
        }
    }

    return false;
}
