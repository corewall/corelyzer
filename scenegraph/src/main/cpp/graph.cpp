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

#include "canvas.h"
#include "graph.h"
#include "textureresource_ex.h"
#include "trackscene.h"

#if defined(__APPLE__) || defined(MACOSX)
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#else
#include <GL/glu.h>
#endif

#include "fontsys.h"
#include <util.h>

//======================================================================
std::vector< Graph* > graphvec;
static bool isCollapse = false;

static float graphScaleLimit = 1000.0f;
static float graphScale  = DEFAULT_GRAPH_SCALE;
// static float _graphScale = DEFAULT_GRAPH_SCALE;
// static bool  graphAutoScale = true;
//======================================================================
bool is_graph(int gid)
{
	if(gid < 0 || gid > graphvec.size() - 1) return false;
    return graphvec[gid] != NULL;
}

//======================================================================
int add_line_graph_to_section(int track, int section,
                          int dataset, int table, int field)
{
    if(!is_dataset(dataset))
    {
        printf("Cannot find dataset(%d)\n", dataset);

        return -1;
    }

    if(!is_table(dataset, table))
    {
        printf("Cannot find table(%d, %d)\n", dataset, table);

        return -1;
    }

    if( field < 0 )
    {
        printf("Cannot find field(%d)\n", field);

        return -1;
    }

    if( !is_track(track) )
    {
        printf("Cannot find track(%d)\n", track);
        return -1;
    }

    if( !is_section_model( get_scene_track(track), section) )
    {
        printf("Cannot find section(%d, %d)\n", track, section);
        return -1;
    }

    CoreSection* cs = get_track_section( get_scene_track(track), section);
    if(!cs)
    {
        printf("CoreSection(native) is null\n");
        return -1;
    }
	
    int gid = locate_graph(track, section, dataset, table, field);

    if( is_graph(gid) ) {
		graphvec[gid]->show = true;
        return gid; 
    }
    
    // create a new graph
    Graph* g = new Graph();
    g->track   = track;
    g->section = section;

    g->dataset = dataset;
    g->table   = table;
    g->field   = field;
    g->show    = true;
	g->graphonly = false;
    g->label   = NULL;

    g->r       = 1.0f;
    g->g       = 1.0f;
    g->b       = 1.0f;

    g->h       = DEFAULT_GRAPH_HEIGHT;

	// calc width in case that coresection only have graph
	// depth here is inch
	int rows = get_table_height( dataset, table );
	float depthunitscale = get_table_depthunitscale(dataset, table);
	float depth = get_table_row_depth( dataset, table, rows-1);
	g->w = (depth * depthunitscale * INCH_PER_CM);
	if (cs->src == -1) {
		cs->width = g->w * CM_PER_INCH;			// cm
		cs->height = DEFAULT_SECTION_HEIGHT;	// cm
	}
	// end of calc width

    g->min     = get_field_min(dataset, table, field);
    g->max     = get_field_max(dataset, table, field);

    g->orig_min = get_field_min(dataset, table, field);
    g->orig_max = get_field_max(dataset, table, field);
	
	g->type	   = GRAPH_LINE;

    // find the first open spot and use it
    for( int i = 0; i < graphvec.size(); i++)
    {
        if( graphvec[i] == NULL)
        {
            graphvec[i] = g;
            // put the graph in the sections graphvec
            cs->graphvec.push_back(i);
            set_graph_slot( i, cs->graphvec.size() - 1);

            return i;
        }
    }

    graphvec.push_back(g);

    // put the graph in the sections graphvec
    cs->graphvec.push_back( graphvec.size() - 1);
    g->slot = cs->graphvec.size() - 1;

    return (graphvec.size() - 1);
}

//======================================================================
int remove_line_graph_from_section(int track, int section,
                                   int dataset, int table, int field)
{

	if( !is_dataset(dataset) || !is_table(dataset, table) ) return -1;

    if( field < 0 ) return -1;

    if( !is_track(track) ) return -1;

    if( !is_section_model( get_scene_track(track), section) ) return -1;
    CoreSection* cs = get_track_section( get_scene_track(track), section);
    if( !cs) return -1;

    // get the reference to the graph in the core section and remove it
    // update all the slots of all the other graphs
    int gid = locate_graph(track, section, dataset, table, field);

    if(!is_graph(gid)) return -1;

    std::vector< int >::iterator csitr = cs->graphvec.begin();

    int count = 0;
    while( csitr != cs->graphvec.end() )
    {
        if(*csitr == gid )
        {
            cs->graphvec.erase(csitr);
			break;
        }
        csitr++;
        count++;
    }

    // update slots
    int i = 0;
    for( csitr = cs->graphvec.begin();
         csitr != cs->graphvec.end();
         csitr++, i++)
    {
        set_graph_slot( *csitr, i);
    }

    if( graphvec[gid]->label )
        delete [] graphvec[gid]->label;

    if( graphvec[gid] )
        delete graphvec[gid];
    graphvec[gid] = NULL;
	
    return gid;
}

//======================================================================
int remove_line_graph_from_section(int gid)
{
    // get the reference to the graph in the core section and remove it
    // update all the slots of all the other graphs
    if(!is_graph(gid)) {
        printf("- [remove_graph] gid %d is not a graph.\n", gid);

        return -1;
    }

    CoreSection* cs = get_track_section( get_scene_track(graphvec[gid]->track),
                                         graphvec[gid]->section);
	if (cs == NULL) {
	    printf("- [remove_graph] CoreSection is NULL.\n");

		return -1;
	}

	std::vector< int >::iterator csitr = cs->graphvec.begin();

    int count = 0;
    while( csitr != cs->graphvec.end() )
    {
        if(*csitr == gid )
        {
            cs->graphvec.erase(csitr);
			break;
        }
        csitr++;
        count++;
    }

    // update slots
    int i = 0;
    for( csitr = cs->graphvec.begin();
         csitr != cs->graphvec.end();
         csitr++, i++)
    {
        set_graph_slot( *csitr, i);
    }

    if( graphvec[gid] ) {
        delete graphvec[gid];
    }
    
    graphvec[gid] = NULL;

    return gid;
}

//======================================================================
int locate_graph(int track, int section, int dataset, int table, int field)
{
    for(int i=0; i<graphvec.size(); i++) 
    {
        if( graphvec[i] )
        {
            if( (graphvec[i]->track   == track) &&
                (graphvec[i]->section == section) &&
                (graphvec[i]->field   == field) &&
                (graphvec[i]->dataset == dataset) &&
                (graphvec[i]->table   == table) )
            {
                return i;
            }    
        }
    }

    return -1;
}

//======================================================================

void render_minmax_labels(Canvas * c, CoreSection* cs, int gid)
{
    Graph* g = graphvec[gid];
    if(!g) return;

    Box* b = get_graph_box(c, cs, gid);

    float min = g->min;
    float max = g->max;
    
    char * minLabel = new char[64];
    char * maxLabel = new char[64];

    sprintf(minLabel, "Min: %.2f", min);
    sprintf(maxLabel, "Max: %.2f", max);

    float angle = get_horizontal_depth() ? 0.0f : -90.0f;
    float v_shift_max = -(b->h -0.5) * c->dpi_y;
    float h_shift = 0.0f;
    float scale = 0.5 * getGraphScale();

    if(isCollapse)
    {
        h_shift = 5 * 40 * get_graph_slot(gid);
    }

    glColor3f(1.0f, 1.0f, 1.0f);
    // min
    glPushMatrix();
    {
        glTranslatef(h_shift, 0, 0);

        glRotatef(angle, 0, 0, 1);
        glScalef(scale, scale, 1);
        render_string(minLabel, 0, strlen(minLabel)-1);    
    }
    glPopMatrix();

    // max
    glPushMatrix();
    {
        glTranslatef(h_shift, v_shift_max, 0);    

        glRotatef(angle, 0, 0, 1);
        glScalef(scale, scale, 1);
        render_string(maxLabel, 0, strlen(maxLabel)-1);
    }
    glPopMatrix();
}

void render_label(Canvas* c, CoreSection* cs, int gid)
{
    Graph* g = graphvec[gid];

    if(!g || !g->label) return;

	Box* b = get_graph_box(c, cs, gid); // return values' in 'inch'

    // min/max values
    // float min = g->min;
    // float max = g->max;

    // char * minLabel = new char[64];
    // char * maxLabel = new char[64];
    // sprintf(minLabel, "Min: %.2f", min);
    // sprintf(maxLabel, "Max: %.2f", max);

    char * label = new char[256];
    sprintf(label, "%s (min: %.2f - max: %.2f)", g->label, g->min, g->max);

    int len = strlen(label);
    // int   len = (real_len <= 5) ? real_len : 5;

    float shiftv = b->h * c->dpi_y;
    float shifth = (len) * 24 * get_graph_slot(gid);

    // float scaleh = 0.5f * getGraphScale();
    // float scalev = 0.5f * getGraphScale();
    float scaleh = 1.5f;
    float scalev = 1.5f;

    glPushMatrix();
    {
        if(isCollapse)
        {
            glColor3f(g->r, g->g, g->b);
            glTranslatef(shifth, -shiftv, 0.0f);
        }
        else
        {
            glColor3f(1.0f, 1.0f, 0.0f);
            glTranslatef( 0.0f, -shiftv, 0.0f );
        }

        if(!get_horizontal_depth()) // if vertical depth
        {
            glTranslatef(50, 0, 0);
            glRotatef(-90, 0, 0, 1);
        }

        glScalef(scaleh, scalev, 1.0f);
        render_string( label, 0, len - 1 );
	}
	glPopMatrix();
}

void render_graph(Canvas* c, CoreSection* cs, int gid)
{
    if( !is_graph(gid) )
        return;

    Graph* g = graphvec[gid];

    // now unitscale is relative to 'cm'
    float depthunitscale = get_table_depthunitscale(g->dataset, g->table);

	// Get camera coverage corners
    float x, y, z;
    get_camera_position(c->camera, &x, &y, &z);

    float cl = x;                  // left
    float cr = x + c->coverage_x;  // right
    float cu = y;                  // up
    float cd = y + c->coverage_y;  // down

    float scale = c->w / c->w0;
    int stride = 1; // todo adjust according to Z for striding value array

    float depthStart = (2.54f*cl) / (c->dpi_x * 100.0f);
    float depthEnd   = (2.54f*cr) / (c->dpi_x * 100.0f);

    /*
    printf("- [DEBUG] Coverage bounds: %.2f, %.2f, %.2f, %.2f\n", cl, cr, cu, cd);
    printf("-         - Canvas scale: %.2f\n", scale);
    printf("-         - Coverage: %.2f, %.2f\n", c->coverage_x, c->coverage_y);
    printf("-         - DPI: %.2f %.2f\n", c->dpi_x, c->dpi_y);
    printf("-         - Depth:  %.2f - %.2f\n", depthStart, depthEnd);
    */
    
    if(g->show) 
    {
        float fieldRange = (g->max - g->min);
        float fieldMed = (g->max + g->min)/2.0;
        float y_scale = (g->h * getGraphScale()) / (fieldRange * INCH_PER_CM);

        // fit curve into border
        float y_curv_adjust = (getGraphScale() * g->h * c->dpi_y) / 2.0f;
            
        // this means it's 1x1 pixel image for missing purpose
        // no need to rotate it no matter what
        if( (cs->dpi_y == 0.25) && (cs->dpi_x < 1.0f) )
        {
            cs->orientation = LANDSCAPE;
        }

        // TODO Graph Culling
        // TODO Stride the graph values when camera Z increases
        // TODO Pack the data into vertex array

        //  save modelview, projection matrix for
        //  scissor box's window coordinates
        GLdouble mvmat[16];
        GLdouble prjmat[16];
        GLint    viewport[4];

        glGetDoublev(GL_MODELVIEW_MATRIX, mvmat);
        glGetDoublev(GL_PROJECTION_MATRIX, prjmat);
        glGetIntegerv(GL_VIEWPORT, viewport);

        //Box* b = get_graph_box(cs, gid); // return values' in 'inch'
		Box* b = get_graph_box(c, cs, gid); // return values' in 'inch'
		
        if(!b) return;
		
		// graph offset translation in pixel
		glTranslatef(cs->graph_offset, 0.0, 0.0);

        // render border
        glPushMatrix();
        {
            glTranslatef( 0.0, (b->y + b->h) * c->dpi_y, 0.0 );

            // Leave 2 pixels spacing
            render_border(b->w * c->dpi_x + 2, b->h * c->dpi_y + 2);
            render_label(c, cs, gid);
            // render_minmax_labels(c, cs, gid);
        }
        glPopMatrix();
		
#ifdef DEBUG
        printf("\n--- Graph Box Info: %f %f %f %f\n", b->x, b->y, b->w, b->h);
#endif
        // the real graph itself
        glPushMatrix();
        {
            //---- Build clipping scissor box
            // Notice: scissor box input values need to be in
            //         window coordinate
            // w1, w2: lower_left and upper_right point coordinate
            //         in window coordinate
            // p1, p2: lower_left and upper_right point coordinate
            //         in object coordinate
            GLdouble w1[3];
            GLdouble w2[3];
            
            GLint p1, p2;
            
            if(get_horizontal_depth()) {
                // the lower left corner in window space
                p1 = gluProject(b->x * c->dpi_x,
                                (b->y + b->h) * c->dpi_y,
                                0,
                                mvmat, prjmat, viewport,
                                w1, w1+1, w1+2);

                // the upper right corner
                p2 = gluProject((b->w + b->x) * c->dpi_x,
                                b->y * c->dpi_y,
                                0,
                                mvmat, prjmat, viewport,
                                w2, w2+1, w2+2);            
            } else {
                // the lower left corner in window space            
                p1 = gluProject((b->x + b->w) * c->dpi_x,
                                (b->y + b->h) * c->dpi_y,
                                0,
                                mvmat, prjmat, viewport,
                                w1, w1+1, w1+2);

                // the upper right corner
                p2 = gluProject(b->x * c->dpi_x,
                                b->y * c->dpi_y,
                                0,
                                mvmat, prjmat, viewport,
                                w2, w2+1, w2+2);                        
            }

            if( p1 == GL_FALSE || p2 == GL_FALSE )
            {
                glPopMatrix();
                delete b;
                return;
            }

            glPushAttrib( GL_SCISSOR_BIT | GL_CURRENT_BIT );
            glEnable( GL_SCISSOR_TEST );

            if(w1[1] >= c->h0) {
                glScissor(0, 0, 0, 0);
            } else {
                glScissor( int(w1[0]), int(w1[1]),
                           int(w2[0] - w1[0]),
                           int(w2[1] - w1[1]) + 2 ); // 2 more pixels to leave some space
            }
            
#ifdef DEBUG
            printf("Scissoring from %d, %d by %d x %d\n", int(w1[0]),
                   int(w1[1]), int(w2[0] - w1[0]), int(w2[1] - w1[1]));
#endif
            
            // translate wiggle to fit inside border box
            glTranslatef(0, (b->y + b->h) * c->dpi_y,  0);
            glTranslatef(0, -y_curv_adjust, 0);

            // flip +/-, and scale to canvas
            glScalef(c->dpi_x, y_scale, 1);
            glScalef(1, -1, 1);

            // move the middle of the wiggle to x-axis
            glTranslatef(0, -fieldMed * INCH_PER_CM * c->dpi_y, 0);
            
            glBindTexture(GL_TEXTURE_2D, 0);
            glColor3f(g->r, g->g, g->b);

			if (g->type == GRAPH_LINE)
			{
                // Have antialias lines
                glEnable(GL_LINE_SMOOTH);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE);
			    glLineWidth(3.0f);

				glBegin(GL_LINE_STRIP);
				{
					int rows = get_table_height( g->dataset, g->table );

                    // float prevDepth;
                    // float prev_y;

                    bool isInGLBlock = true; // Whether inside a glBegin-glEnd block
                    int numberOfVerticesInStrip = 0; // For segments with only one vertex
                    float pre_x_coord = 0.0f;
                    float pre_y_coord = 0.0f;
                    
					for(int r = 0; r < rows; r += stride) 
					{
					    // Get data tuple
						float depth, value;
						bool isValid;
						
						depth = get_table_row_depth( g->dataset, g->table, r);
						value = get_table_cell( g->dataset, g->table, g->field, r);
						isValid = is_table_cell_valid(g->dataset, g->table, g->field, r);

                        if(isValid)
                        {
                            if(!isInGLBlock)
                            {
                                glBegin(GL_LINE_STRIP);
                                isInGLBlock = true;
                            }

                            float x_coord = (depth * depthunitscale * INCH_PER_CM);
                            float y_coord = (value * INCH_PER_CM) * c->dpi_y;

						    glVertex2f(x_coord, y_coord);

                            pre_x_coord = x_coord;
                            pre_y_coord = y_coord;

						    numberOfVerticesInStrip++;                                                                            
                        }
                        else
                        {
                            if(isInGLBlock)
                            {
                                if(numberOfVerticesInStrip == 1)
                                {
                                    // Add a fake point to show it on screen.
                                    float x_coord = pre_x_coord - 0.01f;
                                    float y_coord = pre_y_coord;
                                    
                                    glVertex2f(x_coord, y_coord);
                                }

                                glEnd();
                                isInGLBlock = false;

                                numberOfVerticesInStrip = 0;
                            }
                        }

                        /* Check whether the plot is out of a section range
                        int direction = isOutside(prevDepth, depth, depthStart, depthEnd);
                        if(r == 0)
                        {
                            direction = 0;
                        }

	                    if(direction == 0) // within coverage
	                    {
                            // unitscale is relative to 'cm'
                            float x_coord = (depth * depthunitscale * INCH_PER_CM);
                            float y_coord = (value * INCH_PER_CM) * c->dpi_y;

						    glVertex2f(x_coord, y_coord);                            
                        }
                        else if(direction = -1) // before coverage depth range
                        {
                            prevDepth = depth;
                            
                            continue;
						}
						else if(direction = 1) // after coverage depth range
						{
                            break;
                        }

						prevDepth = depth;
						// prev_y = y_coord;
						*/
					}
				}
				glEnd();
				glLineWidth(1.0f);
			}
			else if (g->type == GRAPH_POINT)
			{
				glPointSize(2);
				glBegin(GL_POINTS);
				{
					int rows = get_table_height( g->dataset, g->table );
	                
					for(int r = 0; r < rows; r++)
					{
						float depth, value;
						depth = get_table_row_depth( g->dataset, g->table, r);
						value = get_table_cell( g->dataset, g->table, g->field, r);

						bool isValid = is_table_cell_valid(g->dataset, g->table, g->field, r);
						if(!isValid)
						{
						    continue;
                        }

						// unitscale is relative to 'cm'
						float x_coord = (depth * depthunitscale * INCH_PER_CM);
						float y_coord = (value * INCH_PER_CM) * c->dpi_y;
	                    
						glVertex2f(x_coord, y_coord);
					}
				}
				glEnd();
			}
			else if (g->type == GRAPH_CPOINT)	// small cross point style
			{
				// cross size is 0.4 cm
				//float halfx = 0.2f * depthunitscale * INCH_PER_CM;
				float halfx = 0.2f * INCH_PER_CM;
				float halfy = 0.2f * INCH_PER_CM * c->dpi_y / y_scale;
				glLineWidth(1.0f);
				// printf("graph depthunitscale: %f\n", depthunitscale);
				glBegin(GL_LINES);
				{
					int rows = get_table_height( g->dataset, g->table );
	                
					for(int r = 0; r < rows; r++) 
					{
						float depth, value;
						depth = get_table_row_depth( g->dataset, g->table, r);
						value = get_table_cell( g->dataset, g->table, g->field, r);

						bool isValid = is_table_cell_valid(g->dataset, g->table, g->field, r);
						if(!isValid)
						{
						    continue;
                        }

						// unitscale is relative to 'cm'
						float x_coord = (depth * depthunitscale * INCH_PER_CM);
						float y_coord = (value * INCH_PER_CM) * c->dpi_y;
	                    
						// vertical segment
						glVertex2f(x_coord, y_coord+halfy);
						glVertex2f(x_coord, y_coord-halfy);
						
						// horizontal segment
						glVertex2f(x_coord+halfx, y_coord);
						glVertex2f(x_coord-halfx, y_coord);
					}
				}
				glEnd();
			}
            
            glDisable( GL_SCISSOR_TEST );
            glPopAttrib();

        }
        glPopMatrix();
        delete b;

		// restore graph offset translation
		glTranslatef(-cs->graph_offset, 0.0, 0.0);
    }

}


//======================================================================
void render_border(float width, float height)
{
    glBindTexture(GL_TEXTURE_2D, 0);
    glLineWidth(BORDER_LINE_WIDTH);
    glColor3f(BORDER_R, BORDER_G, BORDER_B);

    glBegin(GL_LINE_STRIP);
    {
        glVertex2f( 0, -height );
        glVertex2f( 0, 0 );
        glVertex2f( width, 0 );
    }
    glEnd();

    glLineStipple(1, 0x0101);
    glEnable(GL_LINE_STIPPLE);
    glBegin(GL_LINE_STRIP);
    {
        glVertex2f(0, -height);
        glVertex2f(width, -height);
        glVertex2f(width, 0);
    }
    glEnd();    
    glDisable(GL_LINE_STIPPLE);
}

//==================================================================
std::vector< int > match_my_graph_id(int trackId, int sectionId)
{
    std::vector< int > gids;

    int i;
    for(i=0; i<graphvec.size(); i++) {
        if( (graphvec[i]->track   == trackId) &&
            (graphvec[i]->section == sectionId) )
        {
            gids.push_back(i);
        }
    }

    return gids;
}


//======================================================================
int get_data_set_index(int gid)
{
    if(!is_graph(gid)) return -1;
    return graphvec[gid]->dataset;
}

//======================================================================
int get_table_index(int gid)
{
    if(!is_graph(gid)) return -1;
    return graphvec[gid]->table;
}

//======================================================================
int get_field_index(int gid)
{
     if(!is_graph(gid)) return -1;
    return graphvec[gid]->field;
}

//======================================================================
int get_graph_slot(int gid)
{
    if(!is_graph(gid)) return -1;
    return graphvec[gid]->slot;
}

//======================================================================
int get_track_index(int gid)
{
    if(!is_graph(gid)) return -1;
    return graphvec[gid]->track;
}

//======================================================================
int get_section_index(int gid)
{
    if(!is_graph(gid)) return -1;
    return graphvec[gid]->section;
}

//======================================================================
// view (scaling)
float get_min(int gid)
{
    if(!is_graph(gid)) return 0.0;
    return graphvec[gid]->min;
}

float get_max(int gid)
{
    if(!is_graph(gid)) return 0.0;
    return graphvec[gid]->max;
}

// data (original field data)
float get_graph_orig_max (int gid)
{
    if(!is_graph(gid)) return 0.0;
    return graphvec[gid]->orig_max;
}

float get_graph_orig_min (int gid)
{
    if(!is_graph(gid)) return 0.0;
    return graphvec[gid]->orig_min;
}

//======================================================================
void set_min(int gid, float min)
{
    if(!is_graph(gid)) return;
    graphvec[gid]->min = min;
}

//======================================================================
void set_max(int gid, float max)
{
    if(!is_graph(gid)) return;
    graphvec[gid]->max = max;
}

//======================================================================
void set_line_graph_color(int gid, float r, float g, float b)
{
    if(!is_graph(gid)) return;

    graphvec[gid]->r = r;
    graphvec[gid]->g = g;
    graphvec[gid]->b = b;
}

//======================================================================
void set_line_graph_range(int gid, float min, float max)
{
    if(!is_graph(gid)) return;

    set_min(gid, min);
    set_max(gid, max);
}

//======================================================================
void set_line_graph_label(int gid, char *label)
{
    if(!is_graph(gid)) return;
    if(!label) return;

    if( graphvec[gid]->label != NULL) {
        printf("Previous label: %s\n", graphvec[gid]->label );
        delete [] graphvec[gid]->label;
    }

    int length = strlen(label);
    graphvec[gid]->label = new char[length+1];
    graphvec[gid]->label[length] = '\0';
    strncpy(graphvec[gid]->label, label, length);
}

//======================================================================
void  set_line_graph_type  (int gid, int type)
{
    if(!is_graph(gid)) return;
    graphvec[gid]->type = type;
}

//======================================================================
float get_graph_height(int gid)
{
    if(!is_graph(gid)) return 0.0f;
    return (graphvec[gid]->h * getGraphScale());
}

//======================================================================
float get_line_graph_color_component(int gid, int component)
{

    if(!is_graph(gid)) return 0.0;


    float color_value;
    switch(component) {
        case 0:
            color_value = graphvec[gid]->r;
            break;

        case 1:
            color_value = graphvec[gid]->g;
            break;

        case 2:
            color_value = graphvec[gid]->b;
            break;
    }
   
    return color_value;
}

//======================================================================
int   get_line_graph_type (int gid)
{
	if(!is_graph(gid)) return 0.0;
	
	return graphvec[gid]->type;
}

//======================================================================
void set_graph_slot(int gid, int slot)
{
    if(!is_graph(gid)) return;
    graphvec[gid]->slot = slot;
}

//======================================================================
bool is_line_graph_shown(int gid)
{
    if(!is_graph(gid)) return false;
    
    return graphvec[gid]->show;
}

//======================================================================
void move_graph_to_top(int gid)
{

}

//======================================================================
int get_graph_from_section_slot(CoreSection* ptr, int slot)
{
    if(!ptr) return -1;
    if( slot < 0 || slot >= ptr->graphvec.size() ) return -1;
    if(!is_graph(ptr->graphvec[slot])) return -1;
    return ptr->graphvec[slot];
}

//======================================================================
Box* get_graph_box(CoreSection* cs, int gid) // return units in inch
{
    if(!is_graph(gid)) return NULL;
    if(!cs) return NULL;

    Box* b = new Box();

    // calculate graph width and height for scissor box

    // this means it's 1x1 pixel image for missing purpose
    // no need to rotate it no matter what
    if( (cs->dpi_y == 0.25) && (cs->dpi_x < 1.0f) )
    {
        cs->orientation = LANDSCAPE;
    }

    float width;
    if(cs->orientation == PORTRAIT)
    {
		if (cs->src == -1)
		{
			width = graphvec[gid]->w;
	    }
		else
		{
			width = get_texset_src_height( cs->src) / cs->dpi_y;
	    }
    }
    else
    {
		if (cs->src == -1)
		{
			width = graphvec[gid]->w;
	    }
		else
		{
			width = get_texset_src_width( cs->src) / cs->dpi_x;
		}
    }

    float y_box_adjust = 0;
    int slot = get_graph_slot(gid);
    
    for( int i = 0; i < slot; i++)
    {
        int sgid;
        sgid = cs->graphvec[i];
        y_box_adjust += GRAPH_FIELDS_GAP + get_graph_height(sgid);
    }

    b->w = width;                  // inch
    b->h = get_graph_height(gid);  // inch
    b->x = 0;

    if(isCollapse)
    {
        b->y = -(GRAPH_FIELDS_GAP + b->h);
    }
    else
    {
        b->y = -(y_box_adjust + GRAPH_FIELDS_GAP + b->h);
    }

    return b;
}
//======================================================================
Box* get_graph_box(Canvas* c, CoreSection* cs, int gid) // return units in inch
{
    if(!is_graph(gid)) return NULL;
    if(!cs) return NULL;

    Box* b = new Box();

    // calculate graph width and height for scissor box

    // this means it's 1x1 pixel image for missing purpose
    // no need to rotate it no matter what
    if( (cs->dpi_y == 0.25) && (cs->dpi_x < 1.0f) )
    {
        cs->orientation = LANDSCAPE;
    }

    float width;
	if(cs->orientation == PORTRAIT) // true: portrait
	{
		if (cs->src == -1)
		{
			width = graphvec[gid]->w;
		}
		else
		{
			width = get_texset_src_height( cs->src) / cs->dpi_y;
	    }
    }
    else
    {
		if (cs->src == -1)
		{
			width = graphvec[gid]->w;
		}
		else
		{
			width = get_texset_src_width( cs->src) / cs->dpi_x;
		}	    
    }

    float y_box_adjust = 0;
    int slot = get_graph_slot(gid);
    
    for( int i = 0; i < slot; i++)
    {
        int sgid;
        sgid = cs->graphvec[i];
        y_box_adjust += GRAPH_FIELDS_GAP + get_graph_height(sgid);
    }

    b->w = width;                  // inch
    b->h = get_graph_height(gid);  // inch
	b->x = cs->graph_offset / c->dpi_x;

    if(isCollapse)
    {
        b->y = -(GRAPH_FIELDS_GAP + b->h);
    }
    else
    {
        b->y = -(y_box_adjust + GRAPH_FIELDS_GAP + b->h);
    }

    return b;
}

//======================================================================
int isOutside(float prevDepth, float depth, float startDepth, float endDepth)
{
    // todo
    return 0;

    // check depth range
    if( (prevDepth > endDepth) && (depth > endDepth) )
    {
//         printf("[%d] %.2f %.2f\n", 1, prevDepth, depth);

        return 1;
    }
    else if( (prevDepth < startDepth)  && (depth < startDepth)  )
    {
//         printf("[%d] %.2f %.2f\n", -1, prevDepth, depth);

        return -1;
    }

//     printf("[%d] %.2f %.2f\n", 0, prevDepth, depth);
    return 0;
}

//======================================================================
bool ifCollapse()
{
    return isCollapse;
}

void setCollapse(bool aBool)
{
    isCollapse = aBool;
}

void setGraphScale(float s)
{
    graphScale *= s;
/*
    float scale = _graphScale * s;
    graphScale = scale;

    if(scale > graphScaleLimit)
    {
        _graphScale = graphScaleLimit;
    }
    else if(scale < DEFAULT_GRAPH_SCALE)
    {
        _graphScale = DEFAULT_GRAPH_SCALE;
    }
    else
    {
        _graphScale = scale;
    }
    graphScale = isGraphAutoScale() ? _graphScale : DEFAULT_GRAPH_SCALE;
*/
}

float getGraphScale()
{
    // return isGraphAutoScale() ? _graphScale : graphScale;
    return graphScale;
}

// deprecated
void  setGraphAutoScale(bool b)
{
    /*
    graphAutoScale = b;

    if(b)
    {
        graphScale = _graphScale;
    }
    else
    {
        graphScale = DEFAULT_GRAPH_SCALE;
    }
    */
}

bool  isGraphAutoScale()
{
    // return graphAutoScale;
    return false;
}
