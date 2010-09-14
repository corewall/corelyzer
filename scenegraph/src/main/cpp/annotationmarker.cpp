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

#include "annotationmarker.h"
#include "fontsys.h"
#include <math.h>
#include <vector>

extern void read_png(const char* pngfilename, int* w, int* h, GLenum* format, 
                     char*& pixels);

#define UNSELCTED 0
#define SELECTED  1
#define PI        3.14159

float quarterPI = 0.25*PI;
float threeQuarterPI = 0.75*PI;
float boundary [] = { quarterPI,  threeQuarterPI,
                     -quarterPI, -threeQuarterPI};

typedef struct MarkerTexture_s {
    GLuint texId;
    int    refcount;
    char*  texName;
} MarkerTexture;

std::vector< MarkerTexture >        marker_texvec;
std::vector< AnnotationMarkerType > marker_typevec;

// Annotation size parameter, bigger DPI, bigger size
static float markerScale  = DEFAULT_MARKER_SCALE;
// static float _markerScale = DEFAULT_MARKER_SCALE;
// static bool  markerAutoScale = true;
//==========================================================================
bool is_marker_texture(int id)
{
    if( id < 0 || id > marker_texvec.size() - 1) return false;
    return (marker_texvec[id].texId != 0);
}

//==========================================================================
// assume texture is in png file
int alloc_marker_texture(const char* name)
{
    if(!name) return -1;

    // see if we already have it, if we do increment refcount and return
    // index
    int i;
    int match = -1;
    int firstEmpty = -1;
    for( i = 0; i < marker_texvec.size(); i++)
    {
        if( !is_marker_texture(i))
        {
            if( firstEmpty < 0)
            {
                firstEmpty = i;
            }
        }
        else if(!strcmp(name,marker_texvec[i].texName) )
        {
            match = i;
            i = marker_texvec.size();
        }
    }

    // if no match then make it... try to reuse empty slots

    if( match < 0 )
    {

        MarkerTexture mt;
        int w, h;
        GLenum format;
        char* pixels = NULL;
            
        read_png( name, &w, &h, &format, pixels);
        if( !pixels ) {
// #ifdef DEBUG
            printf("\n---- ERROR! Allocate texture %s failed ----\n", name);
// #endif
            return -1;
        }
        
        GLuint id = 0;
        glGenTextures(1, &id );
#ifdef DEBUG
        printf("glGenTextures created %d\n", id);
#endif
        mt.texId = id;
        glBindTexture(GL_TEXTURE_2D, mt.texId);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, format, w, h, 0, format,
                     GL_UNSIGNED_BYTE, pixels);

        delete [] pixels;
        
        mt.texName = new char[strlen(name) + 1];
        strcpy(mt.texName, name);

        // try to use the first empty
        if( firstEmpty < 0)
        {
            // need to make a new one
            mt.refcount = 1;
            marker_texvec.push_back(mt);
            return marker_texvec.size() - 1;
        }
        else
        {
            if(marker_texvec[firstEmpty].texName)
                delete [] marker_texvec[firstEmpty].texName;

            marker_texvec[firstEmpty].texId    = mt.texId;
            marker_texvec[firstEmpty].texName  = mt.texName;
            marker_texvec[firstEmpty].refcount = 1;
            return firstEmpty;
        }
    }
    else
    {
        return match;
    }
    
}

//==========================================================================
void free_marker_texture(int id)
{
    // see if this is a marker texture, if it is, decrement refcount
    // if refcount == 0 dealloc texture object
    if( !is_marker_texture(id)) return;

    marker_texvec[id].refcount--;

    if( marker_texvec[id].refcount <= 0)
    {
        glDeleteTextures(1,&(marker_texvec[id].texId) );
        if( marker_texvec[id].texName)
            delete [] marker_texvec[id].texName;
        marker_texvec[id].texId = 0;
    }

}

//==========================================================================
int register_marker_type(const char* typeName)
{
    if(!typeName) return -1;

    // time to insert a new one
    AnnotationMarkerType amt;
    amt.typeName = new char[strlen(typeName) + 1];
    strcpy( amt.typeName, typeName);
    amt.tex = -1;
    amt.valid = true;

    // find an unused marker type entry
    int i;
    for( i = 0; i < marker_typevec.size(); ++i)
    {
        if( !marker_typevec[i].valid )
        {
            // use it!
            if( marker_typevec[i].typeName )
                delete [] marker_typevec[i].typeName;
            
            marker_typevec[i].typeName = amt.typeName;
            marker_typevec[i].tex      = amt.tex;
            marker_typevec[i].valid    = amt.valid;
            return i;
        }
    }

    marker_typevec.push_back(amt);
    return marker_typevec.size() - 1;

}

//==========================================================================
void unregister_marker_type(int type)
{
    if(!is_marker_type(type)) return;
    marker_typevec[type].valid = false;
    if( marker_typevec[type].typeName )
        delete [] marker_typevec[type].typeName;
    marker_typevec[type].tex = -1;
}

//==========================================================================
void alloc_marker_type_resource( int type, const char* pngTextureFilename)
{
    if(!is_marker_type(type)) return;
    if(!pngTextureFilename)   return;
    
    marker_typevec[type].tex = alloc_marker_texture(pngTextureFilename);
#ifdef DEBUG
    printf("Marker Type %d Alloc Resource %s -> got tex id %d\n",type,
           pngTextureFilename,marker_typevec[type].tex);
#endif
    if(is_marker_texture(marker_typevec[type].tex))
    {
#ifdef DEBUG
        printf("OpenGL Tex Id: %d\n", 
               marker_texvec[ marker_typevec[type].tex].texId);
#endif
    }
}

//==========================================================================
int copy_marker_type(int type, const char* newTypeName)
{
    if(!is_marker_type(type)) return -1;

    int id = register_marker_type( newTypeName );

    if( id >= 0 ) 
    {
        marker_typevec[id].tex   = marker_typevec[type].tex;
        marker_typevec[id].valid = true;
    }

    return id;
}

//==========================================================================
void free_marker_type_resource(int type)
{
    if(!is_marker_type(type)) return;

    free_marker_texture( marker_typevec[type].tex );

    marker_typevec[type].tex = -1;
}

//==========================================================================
bool marker_type_resource_allocated(int type)
{
    if(!is_marker_type(type)) return false;
    return is_marker_texture(marker_typevec[type].tex);
}

//==========================================================================
bool is_marker_type(int type)
{
    if( type < 0 || type > marker_typevec.size() - 1) return false;
    return marker_typevec[type].valid;
}

//==========================================================================
// Auto adjust connection line
// connector from marker to the middle of span
float * determineConnectionLines(AnnotationMarker* m)
{
    float * ends = new float[4];

    // Marker Icon center
    float centerX = (m->px + m->w * getMarkerScale() / 2.0f);
    float centerY = (m->py + m->h * getMarkerScale() / 2.0f);

    // Target center
    float targetX;
    float targetY;

    switch(m->type) {
        case CORE_POINT_MARKER:
            targetX = m->depthX;
            targetY = m->depthY;
            
            ends[0] = targetX;
            ends[1] = targetY;
            break;

        case CORE_SPAN_MARKER:
            targetX = (m->markerVt[0] + m->markerVt[2]) / 2.0f;
            targetY = m->markerVt[1];

            ends[0] = targetX;
            ends[1] = targetY;
            break;

        case CORE_OUTLINE_MARKER:
            targetX = (m->markerVt[0] + m->markerVt[2])/2.0f;
            targetY = (m->markerVt[1] + m->markerVt[3])/2.0f;
            // endpoints decided later
            break;
    }

    float angle = atan2((centerY - targetY), (centerX - targetX));

    // determine which quadrant is the icon, notice that +Y is going down 
    if(angle >= boundary[2] && angle < boundary[0]) { // right
        // icon end
        ends[2] = m->px;
        ends[3] = m->py + (m->h * getMarkerScale() / 2.0f);

        // target end, only handles outline-type
        if(m->type == CORE_OUTLINE_MARKER) {
            ends[0] = m->markerVt[2];
            ends[1] = (m->markerVt[1] + m->markerVt[3])/2.0f;
        }
    } else if(angle >= boundary[0] && angle < boundary[1]) { // bottom
        // icon end
        ends[2] = m->px + (m->w * getMarkerScale() / 2.0f);
        ends[3] = m->py;

        // target end, only handles outline-type
        if(m->type == CORE_OUTLINE_MARKER) {
            ends[0] = (m->markerVt[0] + m->markerVt[2]) / 2.0f;
            ends[1] = m->markerVt[3];
        }
    } else if(angle >= boundary[3] && angle < boundary[2]) { // top
        // icon end
        ends[2] = m->px + (m->w * getMarkerScale() / 2.0f);
        ends[3] = m->py + (m->h * getMarkerScale());

        // target end, only handles outline-type
        if(m->type == CORE_OUTLINE_MARKER) {
            ends[0] = (m->markerVt[0] + m->markerVt[2]) / 2.0f;
            ends[1] = m->markerVt[1];
        }
    } else if(angle >= boundary[1] || angle < boundary[3]) { // left
        // icon end
        ends[2] = m->px + (m->w * getMarkerScale());
        ends[3] = m->py + (m->h * getMarkerScale() / 2.0f);

        // target end, only handles outline-type
        if(m->type == CORE_OUTLINE_MARKER) {
            ends[0] = m->markerVt[0];
            ends[1] = (m->markerVt[1] + m->markerVt[3]) / 2.0f;
        }
    } else {
        printf("---> [INFO] Else Ouch %.4f\n", angle);
        ends[0] = targetX;
        ends[1] = m->markerVt[1];
        ends[2] = m->px;
        ends[3] = m->py + (m->h * getMarkerScale());        
    }

    return ends;
}

void render_marker(Canvas *c, AnnotationMarker* m)
{
    if(!m) return;
    if(!is_marker_type(m->type)) return;
    if(!(m->visibility)) return;

    int tex_index = marker_typevec[m->type].tex;

	glEnable(GL_BLEND);

    // FIXME
    if(m->group == 8) {
        tex_index = marker_typevec[CORE_CLAST_MARKER].tex;
    } else if(m->group == 9) {
        tex_index = marker_typevec[CORE_SAMPLE_MARKER].tex;
    }

#ifdef DEBUG
    printf("\n---- m_info: %f, %f, %d ----\n",
           m->w * getMarkerScale(), m->h * getMarkerScale(), m->type);
    printf("\n---- tex_index: %d, %d ----\n", 
           tex_index, marker_texvec.size());
#endif

    if( !is_marker_texture( tex_index ) ) 
    {
        glBindTexture(GL_TEXTURE_2D, 0);   
		glColor3f(1,0,0);
    }
    else 
    {
        glBindTexture(GL_TEXTURE_2D, marker_texvec[tex_index].texId); 
        glTexEnvf( GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE );

        // FIXME mod color if it's in 1st 8 groups
        if( (m->group > 0) &&
            (m->group < 8)
          )
        {
            int idx = (m->group) % 8;
            glColor3fv(color_palette[idx]);
        }
        else
        {
            glColor3f(1.0f, 1.0f, 1.0f);
        }
    }

    // Draw marker icon
    glBegin(GL_QUADS);
    {
        float scale_x = c->dpi_x / DEFAULT_MARKER_DPI_X;
        float scale_y = c->dpi_y / DEFAULT_MARKER_DPI_Y;
        glScalef( scale_x, scale_y, 1.0f );
        {
            glTexCoord2f( m->ultex[0], m->lrtex[1] );
                glVertex3f( 0,    0,    0);
            glTexCoord2f( m->ultex[0], m->ultex[1] );
                glVertex3f( 0,    m->h * getMarkerScale(), 0);
            glTexCoord2f( m->lrtex[0], m->ultex[1] );
                glVertex3f( m->w * getMarkerScale(),
                            m->h * getMarkerScale(), 0);
            glTexCoord2f( m->lrtex[0], m->lrtex[1] );
                glVertex3f( m->w* getMarkerScale(), 0,    0);
        }
        glScalef( 1.0f / scale_x, 1.0f / scale_y, 1.0f );
    }
    glEnd();

    // Marker-to-Icon connection line ends
    float *lineEnds = determineConnectionLines(m);

	glLineWidth(1);
	glColor3f( 1.0f, 1.0f, 1.0f);
	glBindTexture(GL_TEXTURE_2D, 0);

	glPushMatrix();
	{
		glTranslatef( -m->px, -m->py, 0);	    
        glBegin(GL_LINES);
        {
            glVertex2f(lineEnds[0], lineEnds[1]);
            glVertex2f(lineEnds[2], lineEnds[3]);
        }
        glEnd();
    }
    glPopMatrix();

    // Labels
    if(m->label)
    {
        glPushMatrix();
        {
            glTranslatef(-130, 0, 0);
            glScalef(2.0f, 2.0f, 1.0f);
            render_string_shadowed(m->label, 0, strlen(m->label) - 1 );
        }
        glPopMatrix();
    }

    // Relationship labeling
    if(m->rlabel)
    {
        glPushMatrix();
        {
            glTranslatef(-220.0f, 0, 0);
		    glTranslatef( -m->px, -m->py, 0);	            
            glTranslatef(lineEnds[0], lineEnds[1], 0);
            glScalef(2.0f, 2.0f, 1.0f);
            render_string_shadowed(m->rlabel, 0, strlen(m->rlabel) - 1);
        }
        glPopMatrix();
    }

	// if focused,
	if (m->focused) {
		// draw annotation outline in red
		glLineWidth(1);
		glEnable(GL_BLEND);
		glColor4f( 1.0f, 0.0f, 0.0f, 0.5f);
		glBindTexture(GL_TEXTURE_2D, 0);
		glBegin(GL_QUADS);
		{
			glVertex2f( 0, 0 );
			glVertex2f( 0, m->h * getMarkerScale());
			glVertex2f( m->w * getMarkerScale(), m->h * getMarkerScale());
			glVertex2f( m->w * getMarkerScale(), 0 );
			glVertex2f( 0, 0 );
		}
		glEnd();
		glDisable(GL_BLEND);

		// draw marker guide
		glLineWidth(1);
		glColor3f( 1.0f, 1.0f, 1.0f);
		glBindTexture(GL_TEXTURE_2D, 0);
		glTranslatef( -m->px, -m->py, 0);
		switch (m->type) {
			case CORE_POINT_MARKER:
				// draw manipulator
				// manipulator on annotation icon
				draw_cross_arrow(m->px + (m->w * getMarkerScale() / 2.0f),
				                 m->py + (m->h * getMarkerScale() / 2.0f),
				                 m->w * getMarkerScale() * 0.6f);

				// manipulator on point
				draw_cross_arrow(m->depthX,
				                 m->depthY,
				                 m->w * getMarkerScale() * 0.3f);
				break;
			case CORE_SPAN_MARKER:
				glBegin(GL_LINES);
				{
					// span
					glVertex2f(m->markerVt[0], m->markerVt[1]);	// left end point
					glVertex2f(m->markerVt[2], m->markerVt[3]);	// right end point
					// left vertical
					glVertex2f(m->markerVt[0], m->markerVt[1] - (m->h * getMarkerScale() / 4.0f));
					glVertex2f(m->markerVt[0], m->markerVt[1] + (m->h * getMarkerScale() / 4.0f));
					// right vertical
					glVertex2f(m->markerVt[2], m->markerVt[3] - (m->h * getMarkerScale() / 4.0f));
					glVertex2f(m->markerVt[2], m->markerVt[3] + (m->h * getMarkerScale() / 4.0f));
				}
				glEnd();
				// draw manipulator
				// manipulator on annotation icon
				draw_cross_arrow(m->px + (m->w * getMarkerScale() / 2.0f),
				                 m->py + (m->h * getMarkerScale() / 2.0f),
				                 m->w * getMarkerScale() * 0.6f);
				// manipulator on left span
				draw_solid_square(m->markerVt[0], m->markerVt[1], m->h * getMarkerScale() / 8.0f);
				// manipulator on right span
				draw_solid_square(m->markerVt[2], m->markerVt[3], m->h * getMarkerScale() / 8.0f);
				break;
			case CORE_OUTLINE_MARKER:
				float middleX, middleY;
				middleX = (m->markerVt[0] + m->markerVt[2])/2.0f;
				middleY = (m->markerVt[1] + m->markerVt[3])/2.0f;

				// outline
				glBegin(GL_LINE_STRIP);
				{
					glVertex2f(m->markerVt[0], m->markerVt[1]);	// left upper
					glVertex2f(m->markerVt[0], m->markerVt[3]);	// left lower
					glVertex2f(m->markerVt[2], m->markerVt[3]);	// right lower
					glVertex2f(m->markerVt[2], m->markerVt[1]);	// right upper
					glVertex2f(m->markerVt[0], m->markerVt[1]);	// left upper
				}
				glEnd();
				// draw manipulator
				draw_cross_arrow(m->px + (m->w * getMarkerScale() / 2.0f),
				                 m->py + (m->h * getMarkerScale() / 2.0f),
				                 m->w * getMarkerScale() * 0.6f);
				// manipulator on center of outline
				draw_cross_arrow(middleX, middleY,
				                 m->w * getMarkerScale() * 0.3f);
				// manipulator on top handle
				draw_solid_square((m->markerVt[0] + m->markerVt[2])/2.0f,
				                  m->markerVt[1],
				                  m->h * getMarkerScale() / 8.0f);

				// manipulator on bottom handle
				draw_solid_square((m->markerVt[0] + m->markerVt[2])/2.0f,
				                   m->markerVt[3],
				                   m->h * getMarkerScale() / 8.0f);

				// manipulator on left handle
				draw_solid_square(m->markerVt[0],
				                  (m->markerVt[1] + m->markerVt[3])/2.0f,
				                  m->h * getMarkerScale() / 8.0f);

				// manipulator on right handle
				draw_solid_square(m->markerVt[2],
				                  (m->markerVt[1] + m->markerVt[3])/2.0f,
				                  m->h * getMarkerScale() / 8.0f);

				break;
		}
		glTranslatef( m->px, m->py, 0);
	}
	else
	{
		// draw annotation outline in red
		glLineWidth(1);

		// draw marker guide
		glLineWidth(1);
		glColor3f( 1.0f, 1.0f, 1.0f);
		glBindTexture(GL_TEXTURE_2D, 0);
		glTranslatef( -m->px, -m->py, 0);
		switch (m->type) {
			case CORE_POINT_MARKER:
				break;
			case CORE_SPAN_MARKER:
				glBegin(GL_LINES);
				{
					// span
					glVertex2f(m->markerVt[0], m->markerVt[1]);	// left end point
					glVertex2f(m->markerVt[2], m->markerVt[3]);	// right end point
					// left vertical
					glVertex2f(m->markerVt[0], m->markerVt[1] - m->h * getMarkerScale() / 4.0f);
					glVertex2f(m->markerVt[0], m->markerVt[1] + m->h * getMarkerScale() / 4.0f);
					// right vertical
					glVertex2f(m->markerVt[2], m->markerVt[3] - m->h * getMarkerScale() / 4.0f);
					glVertex2f(m->markerVt[2], m->markerVt[3] + m->h * getMarkerScale() / 4.0f);
				}
				glEnd();
				break;
			case CORE_OUTLINE_MARKER:
			{
				// outline
				glBegin(GL_LINE_STRIP);
				{
					glVertex2f(m->markerVt[0], m->markerVt[1]);	// left upper
					glVertex2f(m->markerVt[0], m->markerVt[3]);	// left lower
					glVertex2f(m->markerVt[2], m->markerVt[3]);	// right lower
					glVertex2f(m->markerVt[2], m->markerVt[1]);	// right upper
					glVertex2f(m->markerVt[0], m->markerVt[1]);	// left upper
				}
				glEnd();
				break;
			}
		}
		glTranslatef( m->px, m->py, 0);
	}

    glDisable(GL_BLEND);
    
    delete [] lineEnds;
}

//==========================================================================
void set_marker_url(AnnotationMarker* m, char* url)
{
    if(!m || !url) return;
    if(m->url)
        delete [] m->url;
    m->url = new char[strlen(url) + 1];
    strcpy(m->url,url);
}

void  set_marker_label(AnnotationMarker* m, char* label)
{
    if(!m || !label) return;
    if(m->label)
        delete [] m->label;
    m->label = new char[strlen(label) + 1];
    strcpy(m->label, label);
}

//==========================================================================
char* get_marker_url(AnnotationMarker* m)
{
    if(!m) return NULL;
    return m->url;
}

char* get_marker_label(AnnotationMarker* m)
{
    if(!m) return NULL;
    return m->label;
}

void  set_marker_relation_label(AnnotationMarker* m, const char *label)
{
    if(!m || !label) return;
    if(m->rlabel)
        delete [] m->rlabel;
    m->rlabel = new char[strlen(label) + 1];
    strcpy(m->rlabel, label);
}

char* get_marker_relation_label(AnnotationMarker* m)
{
    if(!m) return NULL;
    return m->rlabel;
}

//==========================================================================
void  set_marker_local_file( AnnotationMarker* m, char* filename)
{
    if(!m || !filename) return;
    if(m->local_file)
        delete [] m->local_file;

    // m->local_file = new char[strlen(filename) + 1];
    m->local_file = (char *) malloc(sizeof(char) * (strlen(filename) + 1));
    // strncpy(m->local_file, filename, strlen(filename)+1);
    strcpy(m->local_file, filename);
}

//==========================================================================
char* get_marker_local_file( AnnotationMarker* m)
{
    if(!m) return NULL;
    return m->local_file;

}

//==========================================================================
void set_marker_group( AnnotationMarker* m, int groupid )
{
    if(!m) return;
    
    m->group = groupid;
}

//==========================================================================
int get_marker_group( AnnotationMarker* m )
{
    if(!m) return -1;

    return m->group;
}

//==========================================================================
void set_marker_type( AnnotationMarker* m, int typeId )
{
    if(!m) return;
    
    m->type = typeId;
}

//==========================================================================
int get_marker_type( AnnotationMarker* m )
{
    if(!m) return -1;

    return m->type;
}

//==========================================================================
void set_marker_focus( AnnotationMarker* m, bool flag )
{
    if(!m) return;
    
    m->focused = flag;
}

//==========================================================================
void set_marker_visibility( AnnotationMarker* m, bool vis )
{
    if(!m) return;

    m->visibility = vis;
}

//==========================================================================
bool get_marker_visibility( AnnotationMarker* m )
{
    if(!m) return true;

    return m->visibility;
}

//==========================================================================
void  set_marker_position(AnnotationMarker* m, float x, float y)
{
	if(!m) return;

	if(!m->focused) return;

	m->px = x;
	m->py = y;
}

//==========================================================================
void  draw_cross_arrow(float x, float y, float size)
{
	// for move manipulator
	// 1. draw arrow body
	// 2. draw arrow head
	// let's do super simple way
	float thick = size/4.0f; // arrow thickness: 1/10 size
	float half_t = size/12.0f;
	glLineWidth(1);		
	float half = size/2.0f;
	glEnable(GL_BLEND);
	glColor4f(0.2f, 1.0f, 0.2f, 0.6f);
	glBegin(GL_QUADS);
	{
		// horizontal arrow body
		glVertex2f(x-half, y-half_t);
		glVertex2f(x-half, y+half_t);
		glVertex2f(x+half, y+half_t);
		glVertex2f(x+half, y-half_t);
		// verticla arrow body
		glVertex2f(x-half_t, y-half);
		glVertex2f(x-half_t, y+half);
		glVertex2f(x+half_t, y+half);
		glVertex2f(x+half_t, y-half);

	}
	glEnd();
	glColor4f(0.1f, 1.0f, 0.1f, 0.8f);
	glBegin(GL_TRIANGLES);
	{
		// top
		glVertex2f( x, y-half-thick);	// Top
		glVertex2f( x-thick, y-half);	// Bottom Left
		glVertex2f( x+thick, y-half);	// Bottom Right
		// bottom
		glVertex2f( x, y+half+thick);	// Top
		glVertex2f( x+thick,y+half);	// Bottom Left
		glVertex2f( x-thick,y+half);	// Bottom Right
		// left
		glVertex2f( x-half-thick, y);	// Top
		glVertex2f( x-half,y-thick);	// Bottom Left
		glVertex2f( x-half,y+thick);	// Bottom Right
		// right
		glVertex2f( x+half+thick, y);	// Top
		glVertex2f( x+half,y+thick);	// Bottom Left
		glVertex2f( x+half,y-thick);	// Bottom Right
	}
	glEnd();
	glDisable(GL_BLEND);

}

//==========================================================================
void  draw_circle(float x, float y, float radius)
{
	glEnable(GL_LINE_SMOOTH);
	glDisable(GL_LINE_SMOOTH);
}

//==========================================================================
void  draw_rectangle(float x1, float y1, float x2, float y2)
{
	glBegin(GL_LINE_STRIP);
	{
		glVertex2f(x1, y1);	// left upper
		glVertex2f(x1, y2);	// left lower
		glVertex2f(x2, y2);	// right lower
		glVertex2f(x2, y1);	// right upper
		glVertex2f(x1, y1);	// left upper
	}
	glEnd();
}

//==========================================================================
void  draw_solid_square(float x, float y, float size)
{
	// x,y is center of square
	float half = size / 2.0f;
	glBegin(GL_QUADS);
	{
		glVertex2f(x - half, y - half);	// left upper
		glVertex2f(x - half, y + half);	// left lower
		glVertex2f(x + half, y + half);	// right lower
		glVertex2f(x + half, y - half);	// right upper
	}
	glEnd();
}

//==========================================================================
void  set_marker_vertex (AnnotationMarker* m, float ax, float ay,
											  float v0, float v1,
											  float v2, float v3)
{
    if(!m) return;

	// assigne value of vertices
	m->px = ax;
	m->py = ay;
	m->markerVt[0] = v0;
	m->markerVt[1] = v1;
	m->markerVt[2] = v2;
	m->markerVt[3] = v3;
}

void setMarkerScale(float s)
{
/*
    _markerScale *= s;
    markerScale = isMarkerAutoScale() ? _markerScale : DEFAULT_MARKER_SCALE;
*/
    markerScale *= s;
}

float getMarkerScale()
{
    return markerScale;
}

void  setMarkerAutoScale(bool b)
{
/*
    markerAutoScale = b;

    if(b)
    {
        markerScale = _markerScale;
    }
    else
    {
        markerScale = DEFAULT_MARKER_SCALE;
    }
*/
}

bool  isMarkerAutoScale()
{
//    return markerAutoScale;
    return false;
}
