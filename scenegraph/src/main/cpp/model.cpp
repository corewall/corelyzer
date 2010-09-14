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
 

#include "model.h"
#include "textureresource_ex.h"
#include "common.h"

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <stack>

//======================================================================
void create_model_grid( int src, ModelGridsLOD*& m)
{    
    if( !is_texset(src) ) 
    {
        m = NULL;
        return;
    }

    // allocate memory

    m       = new ModelGridsLOD();
    m->grid = new ModelNode*[get_texset_num_levels(src)];
    m->cols = new int[get_texset_num_levels(src)];
    m->rows = new int[get_texset_num_levels(src)];
    m->lod  = 0;
    m->src  = src;

    // build grids at all levels

    for( int l = 0; l < get_texset_num_levels(src); ++l)
    {
        int cols, rows; 
        float scale;

        cols  = get_texset_num_cols(src,l);
        rows  = get_texset_num_rows(src,l);
        scale = get_texset_scale(src,l);
        m->grid[l] = new ModelNode[ cols * rows ];
        m->cols[l] = cols;
        m->rows[l] = rows;

        for( int r = 0; r < rows; ++r)
        {
            for(int c = 0; c < cols; ++c)
            {
                texBlock* tb = get_tex_block(src,l,c,r);
                int index = r * cols + c;

                m->grid[l][index].w   = (float) tb->dataW;
                m->grid[l][index].h   = (float) tb->dataH;
                m->grid[l][index].x   = (float) tb->imgx;
                m->grid[l][index].y   = (float) tb->imgy;
                m->grid[l][index].col = c;
                m->grid[l][index].row = r;

                m->grid[l][index].tex_crd[NW_CORNER][TEX_S] = 0.0f;
                m->grid[l][index].tex_crd[NW_CORNER][TEX_T] = 0.0f;
                m->grid[l][index].tex_crd[SE_CORNER][TEX_S] =
                    (((float) tb->dataW) * scale) / ((float) tb->texW);
                m->grid[l][index].tex_crd[SE_CORNER][TEX_T] =
                    (((float) tb->dataH) * scale) / ((float) tb->texH);
#ifdef DEBUG
                printf("Data size %f x %f -> Coords from 0,0 to %f, %f\n",
                       m->grid[l][index].w, m->grid[l][index].h,
                       m->grid[l][index].tex_crd[SE_CORNER][TEX_S],
                       m->grid[l][index].tex_crd[SE_CORNER][TEX_T]);
#endif
            } 
        } // end for each row and column
    } // end for each level
    
}

//======================================================================
// TODO: more to delete: need to free texBlock*?
void free_model_grid(ModelGridsLOD* m)
{
    if(!m) return;

    if(m->cols) {
        delete [] m->cols;
    }

    if(m->rows) {
        delete [] m->rows;
    }

    for(int i = 0; i < get_texset_num_levels(m->src); ++i)
    {
        if(m->grid[i]) {
            delete [] m->grid[i];
        }
    }

    delete m;
}

//======================================================================
void set_model_grid_lod(ModelGridsLOD* m, int lod)
{
    if(!m) return;
    if( lod < 0 || lod > get_texset_num_levels(m->src) - 1) return;
    m->lod = lod;
}
//======================================================================
int get_model_grid_lod(ModelGridsLOD* m)
{
    if(!m) return -1;
    return m->lod;
}

//======================================================================
typedef struct ModelDrawStackNode_s {
    int  col;
    int  row;
    int  span_x;
    int  span_y;
    char next_subgrid; // value between NW, NE, SE, and SW_CORNER
} ModelDrawStackNode;

void render_model_grid(ModelGridsLOD* m, Canvas* c)
{

#ifdef DEBUG
    printf("\n\t--- Render Model Grid ---\n");
    printf("Rendering ModelGridsLOD at address 0x%x\n", m);
#endif

    if(!m || !c)
    {
        printf("ERROR: ModelGridsLOD or Canvas don't exist!\n");
        return;
    }

    std::stack< ModelDrawStackNode > dstack;
    
    int l    = m->lod;
    int cols = m->cols[l];
    int rows = m->rows[l];
    ModelNode* grid = m->grid[l];

    float x, y, z;
    get_camera_position( c->camera, &x, &y, &z);

#ifdef DEBUG
    printf("Camera position %.2f, %.2f\n", x, y);
    printf("Camera LR %.2f, %.2f\n", (x+c->coverage_x), (y+c->coverage_y));
    printf("Camera coverage: %.2f, %.2f\n", c->coverage_x, c->coverage_y);
#endif

    float cl = x;                  // left
    float cr = x + c->coverage_x;  // right
    float cu = y;                  // up
    float cd = y + c->coverage_y;  // down

#ifdef DEBUG
        printf("== Orig Cam: (%.2f, %.2f) - (%.2f %.2f)\n",
               cl, cu, cr, cd);
#endif

    // setup stack

    ModelDrawStackNode snode;
    snode.col    = cols - 1;
    snode.row    = rows - 1;
    snode.span_x = cols - 1;
    snode.span_y = rows - 1;
    snode.next_subgrid = NW_CORNER;

    dstack.push( snode );

    // run through iterative quadtree like algorithm

    glEnable(GL_TEXTURE_2D);
    // glColor3f(1,1,1);

    while( dstack.size() > 0)
    {
        // make sure that the space between upper left most node to 
        // lower right node of sub grid intersects the camera space
        // otherwise pop the dstack and continue loop
        
        float ulx, uly, llx, lly; // upper left x,y and lower left x,y
        int ulc, ulr, llc, llr;   // uper left col, row and lower left col,row

        ulc = dstack.top().col - dstack.top().span_x;
        ulr = dstack.top().row - dstack.top().span_y;
        llc = dstack.top().col;
        llr = dstack.top().row;

        ModelNode* n;
        n   = &(grid[ (ulr * cols) + ulc ]);
        ulx = n->x;
        uly = n->y;
        n   = &(grid[ (llr * cols) + llc ]);
        llx = n->x + n->w;
        lly = n->y + n->h;

        // check horizontal or vertical depth
        // notice comparision should occure in model's origin/space/coord sys
        float _cl, _cu, _cr, _cd;
        if(get_horizontal_depth())
        {
            _cl = cl;
            _cu = cu;
            _cr = cr;
            _cd = cd;
        } else {
            _cl =  cu;
            _cu = -cr;
            _cr =  cd;
            _cd = -cl;
        }

#ifdef DEBUG
        printf("Checking If Nodes (%d, %d) to (%d, %d) intersect\n",
               ulc, ulr, llc, llr);
#endif

        if(ulx > _cr || uly > _cd || llx < _cl || lly < _cu)
        {
#ifdef DEBUG
            printf("\tModel Area of (%.2f,%.2f) to (%.2f,%.2f) SKIPPED!\n",
                   ulx, uly, llx, lly);
            printf("\tCamera space covering (%.2f,%.2f) to (%.2f,%.2f)\n",
                   _cl, _cu, _cr, _cd);
#endif
            dstack.pop();
            continue;
        }
#ifdef DEBUG
        else
        {
            printf("\tModel Area of (%.2f,%.2f) to (%.2f,%.2f) OK!\n",
                   ulx,uly,llx,lly);
            printf("\tCamera space covering (%.2f,%.2f) to (%.2f,%.2f)\n",
                   _cl, _cu, _cr, _cd);
        }
#endif

        // is the span zero?
        if( dstack.top().span_x == 0 && dstack.top().span_y == 0)
        {
            // draw
            n = &(grid[ (dstack.top().row * cols ) + dstack.top().col]);
            dstack.pop();

            if(!n)
                continue;

#ifdef DEBUG
            printf("\t!!! Drawing node at col: %d, row %d !!!\n", n->col, 
                   n->row);
            printf("\t!!! Starts at (%.2f, %.2f) to (%.2f, %.2f) !!!\n",
                   n->x, n->y, n->x + n->w, n->y + n->h);
#endif
            
            bind_texblock( m->src, l, n->col, n->row);

            float *nwtc = &(n->tex_crd[0][0]);
            float *setc = &(n->tex_crd[1][0]);

            glBegin(GL_QUADS);
            {
                glTexCoord2f( nwtc[TEX_S], nwtc[TEX_T]);
                glVertex2f( n->x,        n->y        );

                glTexCoord2f( nwtc[TEX_S], setc[TEX_T]);
                glVertex2f( n->x,        n->y + n->h );

                glTexCoord2f( setc[TEX_S], setc[TEX_T]);
                glVertex2f( n->x + n->w, n->y + n->h );

                glTexCoord2f( setc[TEX_S], nwtc[TEX_T]);
                glVertex2f( n->x + n->w, n->y        );
            }
            glEnd();

            continue;
        }

        
        switch( dstack.top().next_subgrid )
        {
        case NW_CORNER:
            dstack.top().next_subgrid = NE_CORNER;
            snode.span_x = dstack.top().span_x / 2;
            snode.span_y = dstack.top().span_y / 2;
            snode.col = dstack.top().col - int(ceil(dstack.top().span_x * .5));
            snode.row = dstack.top().row - int(ceil(dstack.top().span_y * .5));
            snode.next_subgrid = NW_CORNER;
            
#ifdef DEBUG
            printf("NW CORNER: Pushing node (%d, %d) onto stack\n",
                   snode.col, snode.row);
#endif
            dstack.push(snode);

            break;
        case NE_CORNER:
            dstack.top().next_subgrid = SE_CORNER;
            snode.span_x = dstack.top().span_x / 2;
            snode.span_y = dstack.top().span_y / 2;
            snode.col = dstack.top().col;
            snode.row = dstack.top().row - int(ceil(dstack.top().span_y * .5));
            snode.next_subgrid = NW_CORNER;

#ifdef DEBUG
            printf("NE CORNER: Pushing node (%d, %d) onto stack\n",
                   snode.col, snode.row);
#endif
            dstack.push(snode);

            break;
        case SE_CORNER:
            dstack.top().next_subgrid = SW_CORNER;
            snode.span_x = dstack.top().span_x / 2;
            snode.span_y = dstack.top().span_y / 2;
            snode.col = dstack.top().col;
            snode.row = dstack.top().row;
            snode.next_subgrid = NW_CORNER;

#ifdef DEBUG
            printf("SE CORNER: Pushing node (%d, %d) onto stack\n",
                   snode.col, snode.row);
#endif
            dstack.push(snode);

            break;
        case SW_CORNER:
            dstack.top().next_subgrid = UNDEF_CORNER;
            snode.span_x = dstack.top().span_x / 2;
            snode.span_y = dstack.top().span_y / 2;
            snode.col = dstack.top().col - int(ceil(dstack.top().span_x * .5));
            snode.row = dstack.top().row;
            snode.next_subgrid = NW_CORNER;

#ifdef DEBUG
            printf("SW CORNER: Pushing node (%d, %d) onto stack\n",
                   snode.col, snode.row);
#endif
            dstack.push(snode);

            break;
        case UNDEF_CORNER:
        default:
            dstack.pop();
            break;
        } // end switch to process next sub grid or not
        
    } // end while stack not empty

    // go throught the whole grid and draw the outline of the blocks
    if(getDebug()) {
        int i, j;

        glBindTexture(GL_TEXTURE_2D, 0);
        glColor3f(1,0,0);
        glBegin(GL_LINES);

        for( i = 0; i < rows; ++i)
        {
            for( j = 0; j < cols; ++j)
            {
                int id = (i * cols) + j;
                glVertex2f( grid[id].x + grid[id].w, grid[id].y);
                glVertex2f( grid[id].x,              grid[id].y);

                glVertex2f( grid[id].x,              grid[id].y);
                glVertex2f( grid[id].x,              grid[id].y + grid[id].h);

                glVertex2f( grid[id].x,              grid[id].y + grid[id].h);
                glVertex2f( grid[id].x + grid[id].w, grid[id].y + grid[id].h);

                glVertex2f( grid[id].x + grid[id].w, grid[id].y + grid[id].h);
                glVertex2f( grid[id].x + grid[id].w, grid[id].y);
            }

        }

        glEnd();
    }
}



