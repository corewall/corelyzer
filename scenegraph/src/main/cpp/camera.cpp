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

#include "camera.h"
#include "matrix.h"
#include <vector>
#include <math.h>

//========================================================================
std::vector< Camera > camvec;

//========================================================================
bool is_camera(int i)
{
    if( i < 0 || i >= camvec.size() ) return false;
    return camvec[i].valid;
}

//========================================================================
int create_camera()
{
    // see if there is already an existing invalid camera
    for( int i = 0; i < camvec.size(); i++)
    {
        if( camvec[i].valid == false ) 
        {
            camvec[i].valid = true;
            make_identity( camvec[i].m );
            camvec[i].pos[0] = camvec[i].pos[1] = camvec[i].pos[2] = 0.0f;
            return i;
        }
    }
    
    Camera cam;
    make_identity( cam.m );
    cam.pos[0] = cam.pos[1] = cam.pos[2] = 0.0f;
    cam.valid = true;
    camvec.push_back(cam);
    return camvec.size() - 1;
}

//========================================================================
void free_camera(int cam)
{
    if( !is_camera(cam)) return;
    camvec[cam].valid = false;
}

void free_all_camera()
{
	camvec.clear();
}
//========================================================================
int num_cameras()
{
    return camvec.size();
}

//========================================================================
void position_camera( int id, float x, float y, float z)
{
    if( !is_camera(id) ) return;

    camvec[id].pos[0] = x;
    camvec[id].pos[1] = y;
    camvec[id].pos[2] = z;
}

//========================================================================
void orient_camera( int id, float p, float y, float r)
{
    if(!is_camera(id)) return;
    GLfloat *m = camvec[id].m;
    GLfloat a[16];
    GLfloat b[16];
    make_identity(m);
    make_identity(a);
    make_identity(b);

    GLfloat cr = cosf(r);
    GLfloat sr = sinf(r);
    GLfloat cp = cosf(p);
    GLfloat sp = sinf(p);
    GLfloat cy = cosf(y);
    GLfloat sy = sinf(y);

    // yaw then pitch then roll
    b[5]  = cp;
    b[6]  = sp;
    b[9]  = -sp;
    b[10] = cp;
    a[0]  = cy;
    a[2]  = -sy;
    a[8]  = sy;
    a[10] = cy;

    mul_matrix(m,a,b);
    copy_matrix(a,m);
    make_identity(b);

    b[0] = cr;
    b[1] = sr;
    b[4] = -sr;
    b[5] = cr;

    mul_matrix(m,a,b);
}

//========================================================================
void orient_camera(int id, float* mat)
{
    if(!is_camera(id)) return;
    if( !mat ) return;
    
    for(int i = 0; i < 16; i++)
        camvec[id].m[i] = mat[i];
}

//========================================================================
void get_camera_position(int id, float *x, float *y, float *z)
{
    if(!is_camera(id)) return;
    if( !x || !y || !z ) return;
    
    *x = camvec[id].pos[0];
    *y = camvec[id].pos[1];
    *z = camvec[id].pos[2];
}

//========================================================================
void get_camera_orientation(int id, float* r)
{
    if(!is_camera(id)) return;
    if(!r) return;
    for(int i = 0; i < 16; i++)
        r[i] = camvec[id].m[i];
}

//------------------------------------
//========================================================================
void translate_camera( int id, float x, float y, float z)
{
    if(!is_camera(id)) return;
    // orient vector based on our rotation matrix
    float tx, ty, tz;
    GLfloat *m = camvec[id].m;
    tx = (m[0] * x) + (m[4] * y) + (m[8] * z);
    ty = (m[1] * x) + (m[5] * y) + (m[9] * z);
    tz = (m[2] * x) + (m[6] * y) + (m[10] * z);

    // add our new vector to our position vector
    camvec[id].pos[0] += tx;
    camvec[id].pos[1] += ty;
    camvec[id].pos[2] += tz;
}

//========================================================================
void turn_camera( int id, float p, float y, float r)
{
    if(!is_camera(id)) return;
    GLfloat *m = camvec[id].m;
    GLfloat a[16];
    GLfloat b[16];
 
    make_identity(b);
    make_identity(a);

    GLfloat cr = cosf(r);
    GLfloat sr = sinf(r);
    GLfloat cp = cosf(p);
    GLfloat sp = sinf(p);
    GLfloat cy = cosf(y);
    GLfloat sy = sinf(y);

    // yaw then pitch then roll
    GLfloat temp[16];

    a[0]  = cy;
    a[2]  = -sy;
    a[8]  = sy;
    a[10] = cy;
    
    b[5]  = cp;
    b[6]  = sp;
    b[9]  = -sp;
    b[10] = cp;
    
    mul_matrix(temp,a,b);
    copy_matrix(a,temp);
    make_identity(b);
    
    b[0] = cr;
    b[1] = sr;
    b[4] = -sr;
    b[5] = cr;
    
    mul_matrix(temp,a,b);
    copy_matrix(a,m);
    mul_matrix(m,a,temp);
}

//========================================================================
// make sure by accident it these can't happen more than once
void apply_camera_matrix(int id)
{
    if(!is_camera(id)) return;
    /*
    gluLookAt( camvec[id].pos[0], 
               camvec[id].pos[1], 
               camvec[id].pos[2],
               camvec[id].pos[0] + camvec[id].m[8], 
               camvec[id].pos[1] + camvec[id].m[9], 
               camvec[id].pos[2] + camvec[id].m[10],
               camvec[id].m[4], 
               camvec[id].m[5], 
               camvec[id].m[6]);
    */
    glTranslatef( -camvec[id].pos[0], -camvec[id].pos[1], -camvec[id].pos[2]);
    glMultMatrixf( camvec[id].m );
}

//========================================================================
void unapply_camera_matrix(int id)
{
}

