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

#include "cache.h"
#include "camera.h"
#include "common.h"
#include "fontsys.h"
#include "freedraw.h"
#include "graph.h"
#include "trackscene.h"

#ifdef linux
#include "string.h"
#endif

#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include <iostream>
#include <string>
#include <vector>

// Stifle deprecation warnings on macOS for GLU f'ns e.g. gluOrtho2D,
// suggesting use of GLKMatrix4MakeOrtho(), which is 1) macOS-specific
// and 2) a non-trivial change.
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

using namespace std;

extern void read_png(const char *pngfilename, int *w, int *h, GLenum *format,
                     char *&pixels);

vector<Canvas> canvasvec;
static int number_of_rows = 1;
static int number_of_cols = 1;

extern int default_track_scene;
int current_canvas = -1;

void run_through_scene_graph(Canvas *c);
void render_crossHair(Canvas *c);
void render_grid(Canvas *c);
void render_scale(Canvas *c);

bool s3tc_available = false;
bool checked_extensions = false;

// Background color
static float bgcolor[3] = {0.1f, 0.1f, 0.1f};
static bool hasCrossHair = true;
static char *crossHairLabel = NULL;

#if defined(WIN32) || defined(_WIN32)
PFNGLCOMPRESSEDTEXIMAGE2DARBPROC glCompressedTexImage2D = NULL;
PFNGLCOMPRESSEDTEXSUBIMAGE2DARBPROC glCompressedTexSubImage2D = NULL;
#endif

static bool horizontalDepth = true;
static bool showOrigin = true;

// Tie depth used for FineTune operation
static bool showTieDepth = false;
static float tieDepth = 0.0f;

// frames per second
static float framesPerSecond = 0.0f;

//====================================================================
void load_gl_extensions() {
#if defined(__APPLE__) || defined(linux)
    return;
#else
    glCompressedTexImage2D = (PFNGLCOMPRESSEDTEXIMAGE2DARBPROC)  // commmented for linux...already defined in gl.h
        glGetProcAddress("glCompressedTexImage2DARB");
    glCompressedTexSubImage2D = (PFNGLCOMPRESSEDTEXSUBIMAGE2DARBPROC)
        glGetProcAddress("glCompressedTexSubImage2DARB");

#ifdef DEBUG
    printf("glCompressedTexImage2D found at %d\n", glCompressedTexImage2D);
    printf("glCompressedTexSubImage2D found at %d\n", glCompressedTexSubImage2D);
#endif

#endif
}

//====================================================================
int create_canvas(float x, float y, float w, float h, float dpix, float dpiy) {
    printf("\n\t--- Create Canvas called ---\n");

    Canvas c;
    c.x = x;
    c.y = y;
    c.w = (float)w;
    c.h = (float)h;
    c.w0 = c.w;
    c.h0 = c.h;
    c.dpi_x = dpix;
    c.dpi_y = dpiy;
    c.sx = 1.0f;
    c.sy = 1.0f;
    c.valid = true;

    // initially all canvases orient horizontally and do not draw scales
    c.camera = create_camera();

    c.bottomRow = false;
    c.firstColumn = false;

    c.cross_core_scale = false;
    c.horizontal = true;
    // canvas grid
    c.grid = false;
    c.r = 0.8f;
    c.g = 0.8f;
    c.b = 0.8f;
    c.grid_type = GRID_BASIC;
    c.grid_size = 10.0f;
    c.grid_thickness = 1;

    position_camera(c.camera, x, y, 0.0);
    orient_camera(c.camera, 0.0f, 0.0f, 0);

    c.nummeasurepoint = (MEASURE_READY);
    c.measurepoint[0] = c.measurepoint[1] = 0.0f;
    c.measurepoint[2] = c.measurepoint[3] = 0.0f;
    c.mode = CANVAS_NORMAL;

#ifdef DEBUG
    float cx, cy, cz;
    get_camera_position(c.camera, &cx, &cy, &cz);
    printf("Camera %d made with position, %.2f, %.2f, %.2f\n",
           c.camera, cx, cy, cz);
#endif

    // find the next invalid camera spot
    unsigned int i;
    for (i = 0; i < canvasvec.size(); i++) {
        if (canvasvec[i].valid == false) {
            canvasvec[i] = c;
            canvasvec[i].valid = true;
#ifdef DEBUG
            printf("Canvas %d made with camera %d\n", i, c.camera);
#endif
            return i;
        }
    }

    canvasvec.push_back(c);

#ifdef DEBUG
    printf("Canvas %d made with camera %d\n", i, c.camera);
#endif

    return canvasvec.size() - 1;
}

//====================================================================
void free_canvas(int canvas) {
    if (!is_canvas(canvas))
        return;
    canvasvec[canvas].valid = false;
    free_camera(canvasvec[canvas].camera);
}

void free_all_canvas() {
    canvasvec.clear();
    free_all_camera();
    free_all_texsets(false);
}

//====================================================================
int num_canvases() {
    return canvasvec.size();
}

//====================================================================
int get_current_canvas() {
    return current_canvas;
}

//====================================================================
static int *logo_texture_info = NULL;
static int *remoteLogo_texture_info = NULL;
int *load_logo(const char *logoPath) {
    char *pixels = NULL;
    int w, h;
    GLenum format;

    read_png(logoPath, &w, &h, &format, pixels);

    if (!pixels) {
        printf("\n---- Error! Allocate logo texture %s failed ----\n",
               logoPath);
        return NULL;
    }

    GLuint id = 0;
    glGenTextures(1, &id);

    glBindTexture(GL_TEXTURE_2D, id);
    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    glTexImage2D(GL_TEXTURE_2D, 0, format, w, h, 0, format,
                 GL_UNSIGNED_BYTE, pixels);

    delete[] pixels;

    int *texture_info = (int *)malloc(sizeof(int) * 3);
    texture_info[0] = id;
    texture_info[1] = w;
    texture_info[2] = h;

    return texture_info;
}

void render_logo(int id) {
    // make sure canvas and camera are valid
    if (!is_canvas(id)) {
        return;
    }

    // load image file if it's not ready yet
    if (logo_texture_info == NULL) {
        const char *logoPath = "./resources/core_logo.png";

        logo_texture_info = load_logo(logoPath);
        if (!logo_texture_info) {
            printf("---> [WARN] Cannot load logo texture: %s\n", logoPath);
            return;
        }
    }

    Canvas *c = &(canvasvec[id]);

    int logoTextureId = logo_texture_info[0];
    float logo_w = (float)logo_texture_info[1];
    float logo_h = (float)logo_texture_info[2];

    glPushMatrix();
    {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluOrtho2D(0, c->w0, c->h0, 0);

        glPushMatrix();
        {
            glEnable(GL_BLEND);
            glColor3f(1.0, 1.0, 1.0);

            float scale = 0.7f;
            float padding = 20.0f;

            glBindTexture(GL_TEXTURE_2D, logoTextureId);
            glTranslatef((c->w0 - logo_w * scale - padding),
                         (c->h0 - logo_h * scale - padding),
                         0.0);
            glScalef(scale, scale, 0.0);
            glBegin(GL_QUADS);
            {
                glTexCoord2f(0.0, 0.0);
                glVertex3f(0.0, 0.0, 0.0);
                glTexCoord2f(0.0, 1.0);
                glVertex3f(0.0, logo_h, 0.0);
                glTexCoord2f(1.0, 1.0);
                glVertex3f(logo_w, logo_h, 0.0);
                glTexCoord2f(1.0, 0.0);
                glVertex3f(logo_w, 0.0, 0.0);
            }
            glEnd();
            glDisable(GL_BLEND);
        }
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }
    glPopMatrix();
}

void render_remote_control_icon(int id) {
    if (!is_canvas(id)) {
        return;
    }

    // figure out canvas IDs that need to render the remote control logo
    int workCanvasId = -1;
    bool acrossTwoCanvases = false;
    if ((number_of_cols % 2) == 0)  // even number of columns: 2 canvas need to render
    {
        workCanvasId = (number_of_rows * number_of_cols - number_of_cols / 2);
        acrossTwoCanvases = true;
    } else  // odd number of columns: 1 canvas need to render
    {
        workCanvasId = ((2 * number_of_rows - 1) * number_of_cols - 1) / 2;
        acrossTwoCanvases = false;
    }

    if ((id == workCanvasId) ||
        (acrossTwoCanvases && (id == (workCanvasId - 1)))) {
        if (remoteLogo_texture_info == NULL) {
            const char *remotePath = "./resources/RemoteControl-Billboard.png";

            remoteLogo_texture_info = load_logo(remotePath);
            if (!remoteLogo_texture_info) {
                printf("---> [WARN] Cannot load remote logo texture: %s\n", remotePath);
                return;
            }
        }

        Canvas *c = &(canvasvec[id]);
        int logoTextureId = remoteLogo_texture_info[0];
        float logo_w = (float)remoteLogo_texture_info[1];
        float logo_h = (float)remoteLogo_texture_info[2];

        // Translate properly depends on canvas ID
        float scale = 0.4f;
        float x_translate, y_translate;

        if (acrossTwoCanvases) {
            if (id == workCanvasId)  // right
            {
                x_translate = -logo_w * scale / 2;
                y_translate = c->h0 - logo_h * scale - 0.6f * logo_h * scale;
            } else  // left
            {
                x_translate = c->w0 - (logo_w * scale / 2);
                y_translate = c->h0 - logo_h * scale - 0.6f * logo_h * scale;
            }
        } else {
            x_translate = c->w0 / 2 - logo_w * scale / 2;
            y_translate = c->h0 - logo_h * scale - 0.6f * logo_h * scale;
        }

        glPushMatrix();
        {
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            gluOrtho2D(0, c->w0, c->h0, 0);

            glPushMatrix();
            {
                glEnable(GL_BLEND);
                glColor3f(1.0, 1.0, 1.0);
                glBindTexture(GL_TEXTURE_2D, logoTextureId);

                glTranslatef(x_translate, y_translate, 0.0);
                glScalef(scale, scale, 0.0);
                glBegin(GL_QUADS);
                {
                    glTexCoord2f(0.0, 0.0);
                    glVertex3f(0.0, 0.0, 0.0);
                    glTexCoord2f(0.0, 1.0);
                    glVertex3f(0.0, logo_h, 0.0);
                    glTexCoord2f(1.0, 1.0);
                    glVertex3f(logo_w, logo_h, 0.0);
                    glTexCoord2f(1.0, 0.0);
                    glVertex3f(logo_w, 0.0, 0.0);
                }
                glEnd();
                glDisable(GL_BLEND);
            }
            glPopMatrix();
            glMatrixMode(GL_MODELVIEW);
        }
        glPopMatrix();
    }
}

void render_onscreen(Canvas *c, int id) {
    float x, y, z;
    get_camera_position(c->camera, &x, &y, &z);

    // draw guid grid if enabled
    if (c->grid) {
        glPushMatrix();
        {
            glTranslatef(-x, -y, 0);
            render_grid(c);
        }
        glPopMatrix();
    }

    // draw the scale on the bottom or first column if enabled
    if (get_horizontal_depth()) {
        if (c->bottomRow) {
            glPushMatrix();
            {
                glTranslatef(-x, -y, 0);
                render_scale(c);
            }
            glPopMatrix();
        }
    } else {
        if (c->firstColumn) {
            glPushMatrix();
            {
                glTranslatef(-x, -y, 0);
                render_scale(c);
            }
            glPopMatrix();
        }
    }

    // draw cross hair with coordinate
    if (has_crosshair()) {
        glPushMatrix();
        {
            glTranslatef(-x, -y, 0);
            render_crossHair(c);
        }
        glPopMatrix();
    }

    // Places to put onscreen billboards
    if (id == (canvasvec.size() - 1)) {
        glPushMatrix();
        {
            render_logo(id);
        }
        glPopMatrix();
    }

    // Render remote control icon if connected
    if (get_remote_controlled()) {
        render_remote_control_icon(id);
    }
}

void render_canvas(int id) {
    const int startRenderTime = clock();

    // check opengl extensions if we haven't already
    if (checked_extensions == false) {
        printf("CHECKING EXTENSIONS\n");
        fflush(stdout);
        const GLubyte *ext_str = glGetString(GL_EXTENSIONS);
        if (strstr((const char *)ext_str, "GL_EXT_texture_compression_s3tc")) {
            printf("S3TC AVAILABLE!\n");
            s3tc_available = true;
        } else {
            printf("S3TC NOT AVAILABLE!\n");
            printf("- GL_EXTENSIONS: %s\n", ext_str);
        }

        printf("DONE CHECKING EXTENSIONS\n");
        load_gl_extensions();
        checked_extensions = true;
    }

    // make sure canvas and camera are valid
    if (!is_canvas(id)) {
#ifdef DEBUG
        printf("render_canvas: not valid canvas id %d\n", id);
#endif
        return;
    }

    Canvas *c = &(canvasvec[id]);
    if (!is_camera(c->camera)) {
#ifdef DEBUG
        printf("render_canvas: not valid camera id %d\n", c->camera);
#endif
        return;
    }

    current_canvas = id;

#ifdef DEBUG
    printf("\n\t--- Render Canvas ---\n");
    printf("Canvas %d, Camera %d\n", id, c->camera);
#endif

    // clear the canvas and setup modelview_projection_matrix

    glClearColor(bgcolor[0], bgcolor[1], bgcolor[2], 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    gluOrtho2D(0, float(c->w), float(c->h), 0);

    glGetDoublev(GL_PROJECTION_MATRIX, c->projMatrix);
    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();

    glEnable(GL_TEXTURE_2D);

    // keep dpi copy

    float dpix, dpiy;
    float x, y, z;

    dpix = c->dpi_x;
    dpiy = c->dpi_y;
    get_camera_position(c->camera, &x, &y, &z);

#ifdef DEBUG
    printf("Camera Position (%.2f, %.2f, %.2f)\n", x, y, z);
#endif

    glPushMatrix();
    {
        //--
        // apply camera position and orientation
        // roll about z if not horizontal and switch dpi values so things
        // stay proper size both ways

        if (!c->horizontal) {
            glRotatef(90, 0, 0, 1);
            glTranslatef(-y, x, 0);
        } else {
            glTranslatef(-x, -y, 0);
        }

        if (showOrigin) {
            // draw red/yellow crosshair at origin
            glBindTexture(GL_TEXTURE_2D, 0);
            glLineWidth(5);
            glBegin(GL_LINES);
            {
                glColor3f(1, 0, 0);
                glVertex2f(-500, 0);
                glVertex2f(500, 0);

                glColor3f(1, 1, 0);
                glVertex2f(0, 500);
                glVertex2f(0, -500);
            }
            glEnd();
            glLineWidth(1);

            // draw canvas label and ID # in Debug mode only
            if (getDebug()) {
                char *label = new char[16];
                sprintf(label, "Canvas: %d", id);
                glColor3f(1, 1, 1);
                render_string(label, 0, strlen(label) - 1);
                delete[] label;
            }
        }

        // draw tie depth line if enabled
        if (showTieDepth) {
            float depthInPixels = (tieDepth * 100.0f / 2.54f) * dpix;
            float bigger = (c->coverage_x > c->coverage_y) ? c->coverage_x : c->coverage_y;

            glBindTexture(GL_TEXTURE_2D, 0);
            glLineWidth(5);
            glColor3f(1, 0, 0);

            glBegin(GL_LINES);
            {
                glVertex2f(depthInPixels, -(bigger + y));
                glVertex2f(depthInPixels, bigger + y);
            }
            glEnd();
            glLineWidth(1);
        }

        // run through scene graph
        run_through_scene_graph(c);
    }
    glPopMatrix();

    render_onscreen(c, id);

    // make sure dpi values are back to the right values
    position_camera(c->camera, x, y, z);
    c->dpi_x = dpix;
    c->dpi_y = dpiy;

    // done
#ifdef DEBUG
    printf("exiting render_canvas\n");
#endif

    current_canvas = -1;

    const int timeDelta = clock() - startRenderTime;
    if (timeDelta > 0)
        update_fps(timeDelta);

}  // end render_canvas

//====================================================================
bool is_canvas(int canvas) {
    if (canvas < 0)
        return false;
    const int canvasVecSize = canvasvec.size();
    if (canvas >= canvasVecSize)
        return false;
    return canvasvec[canvas].valid;
}

//====================================================================
void get_canvas_position(int id, float *x, float *y) {
    if (!is_canvas(id))
        return;
    if (!x || !y)
        return;

    *x = canvasvec[id].x;
    *y = canvasvec[id].y;
}

//====================================================================
void get_canvas_dimensions(int id, float *w, float *h) {
    if (!is_canvas(id))
        return;
    if (!w || !h)
        return;

    *w = canvasvec[id].w;
    *h = canvasvec[id].h;
}

//====================================================================
void get_canvas_dpi(int id, float *dpix, float *dpiy) {
    if (!is_canvas(id))
        return;
    if (!dpix || !dpiy)
        return;

    *dpix = canvasvec[id].dpi_x;
    *dpiy = canvasvec[id].dpi_y;
}

//====================================================================
int get_canvas_camera(int id) {
    if (!is_canvas(id))
        return -1;
    return canvasvec[id].camera;
}

//====================================================================
void get_canvas_scale(int id, float *sx, float *sy) {
    if (!is_canvas(id))
        return;
    if (!sx || !sy)
        return;
    *sx = canvasvec[id].sx;
    *sy = canvasvec[id].sy;
}

//====================================================================
float get_canvas_width(int id) {
    if (!is_canvas(id))
        return 0.0f;
    return canvasvec[id].w;
}

//====================================================================
float get_canvas_orig_width(int id) {
    if (!is_canvas(id))
        return 0.0f;
    return canvasvec[id].w0;
}

//====================================================================
float get_canvas_height(int id) {
    if (!is_canvas(id))
        return 0.0f;
    return canvasvec[id].h;
}

//====================================================================
float get_canvas_orig_height(int id) {
    if (!is_canvas(id))
        return 0.0f;
    return canvasvec[id].h0;
}

//====================================================================
void set_canvas_position(int id, float x, float y) {
    if (!is_canvas(id))
        return;
    canvasvec[id].x = x;
    canvasvec[id].y = y;
}

//====================================================================
void set_canvas_dimensions(int id, float w, float h) {
    if (!is_canvas(id))
        return;
    canvasvec[id].w = w;
    canvasvec[id].h = h;
}

//====================================================================
void set_canvas_dpi(int id, float dpix, float dpiy) {
    if (!is_canvas(id))
        return;
    canvasvec[id].dpi_x = dpix;
    canvasvec[id].dpi_y = dpiy;
}

//====================================================================
void set_canvas_scale(int id, float sx, float sy) {
    if (!is_canvas(id))
        return;
    canvasvec[id].sx = sx;
    canvasvec[id].sy = sy;
}

//====================================================================
void set_canvas_mouse(int id, float mx, float my) {
    //if(!is_canvas(id)) return;
    canvasvec[id].mouseX = mx;
    canvasvec[id].mouseY = my;
}

float get_canvas_mouseX(int id) {
    if (!is_canvas(id))
        return 0.0f;

    return canvasvec[id].mouseX;
}

float get_canvas_mouseY(int id) {
    if (!is_canvas(id))
        return 0.0f;

    return canvasvec[id].mouseY;
}

//====================================================================
int add_canvas_measurepoint(int id, float x, float y) {
    //if(!is_canvas(id)) return;

    if (canvasvec[id].nummeasurepoint == (MEASURE_NOT)) {
        return -1;
    } else if (canvasvec[id].nummeasurepoint == (MEASURE_ONE_POINT)) {
        canvasvec[id].nummeasurepoint = (MEASURE_TWO_POINT);
        canvasvec[id].measurepoint[2] = x;
        canvasvec[id].measurepoint[3] = y;
        return 2;
    } else {
        canvasvec[id].nummeasurepoint = (MEASURE_ONE_POINT);
        canvasvec[id].measurepoint[0] = x;
        canvasvec[id].measurepoint[1] = y;
        return 1;
    }
}

//====================================================================
void set_canvas_measurepoint(int id, float x1, float y1, float x2, float y2) {
    // parameter coordinate is absolute scale (CM)
    // so need to convert it to scene
    float dpi = canvasvec[id].horizontal ? canvasvec[id].dpi_x : canvasvec[id].dpi_y;

    canvasvec[id].nummeasurepoint = (MEASURE_TWO_POINT);
    canvasvec[id].measurepoint[0] = x1 * dpi / CM_PER_INCH;
    canvasvec[id].measurepoint[1] = y1 * dpi / CM_PER_INCH;
    canvasvec[id].measurepoint[2] = x2 * dpi / CM_PER_INCH;
    canvasvec[id].measurepoint[3] = y2 * dpi / CM_PER_INCH;
}

//====================================================================
void set_canvas_measurepointnumber(int id, int num) {
    //if(!is_canvas(id)) return;

    canvasvec[id].nummeasurepoint = num;
}

//====================================================================
void set_canvas_mode(int id, int imode) {
    canvasvec[id].mode = (float)imode;
    canvasvec[id].nummeasurepoint = (MEASURE_READY);
}

//====================================================================
void orient_canvas_vertically(int id) {
    if (!is_canvas(id))
        return;
    canvasvec[id].horizontal = false;
}

//====================================================================
void orient_canvas_horizontally(int id) {
    if (!is_canvas(id))
        return;
    canvasvec[id].horizontal = true;
}

//====================================================================
void set_canvas_bottom_row(int id, bool flag) {
    if (!is_canvas(id))
        return;
    canvasvec[id].bottomRow = flag;
}

void set_canvas_first_column(int id, bool flag) {
    if (!is_canvas(id))
        return;
    canvasvec[id].firstColumn = flag;
}

//====================================================================
void set_canvas_draw_cross_core_scale(int id, bool flag) {
    if (!is_canvas(id))
        return;
    canvasvec[id].cross_core_scale = flag;
}

//====================================================================
void set_canvas_draw_grid(int id, bool flag) {
    if (!is_canvas(id))
        return;
    canvasvec[id].grid = flag;
}

//====================================================================
void set_canvas_grid_color(int id, float r, float g, float b) {
    if (!is_canvas(id))
        return;
    canvasvec[id].r = r;
    canvasvec[id].g = g;
    canvasvec[id].b = b;
}

//====================================================================
void set_canvas_grid_size(int id, float size) {
    if (!is_canvas(id))
        return;
    canvasvec[id].grid_size = size;
}

//====================================================================
void set_canvas_grid_thickness(int id, int thick) {
    if (!is_canvas(id))
        return;
    canvasvec[id].grid_thickness = thick;
}

//====================================================================
void set_canvas_grid_type(int id, int type) {
    if (!is_canvas(id))
        return;
    canvasvec[id].grid_type = type;
}

//====================================================================
// Camera position (x, y, z)
void render_crossHair(Canvas *c) {
    if (!c) {
        return;
    }

    float x, y, z;
    get_camera_position(c->camera, &x, &y, &z);

    int hairs = 0;
    glLineWidth(1);
    glBindTexture(GL_TEXTURE_2D, 0);
    glColor3f(1, 1, 1);

    float bigger = (c->coverage_x > c->coverage_y) ? c->coverage_x : c->coverage_y;

    if (c->mouseX > x && c->mouseX < c->coverage_x + x) {
        // draw vertical hair
        hairs++;
        glBegin(GL_LINES);
        {
            glVertex2f(c->mouseX, y);  // vertical
            glVertex2f(c->mouseX, bigger + y);
        }
        glEnd();
    }

    if (c->mouseY > y && c->mouseY < c->coverage_y + y) {
        // draw horizontal hair
        hairs++;
        glBegin(GL_LINES);
        {
            glVertex2f(x, c->mouseY);  // horizontal
            glVertex2f(bigger + x, c->mouseY);
        }
        glEnd();
    }

    // if canvas draws two hairs then need to draw coordinate too
    // possibly have issue on close border
    if (hairs == 2) {
        // draw coordinate
        glPushMatrix();
        {
            glTranslatef(c->mouseX, c->mouseY, 0);
            float scale = c->horizontal ? c->w / c->w0 / 1.5f : c->h / c->h0 / 1.5f;
            glScalef(scale, scale, 1.0);
            glTranslatef(10, -10, 0);
            float dpi = get_horizontal_depth() ? c->dpi_x : c->dpi_y;
            float pixels = (get_horizontal_depth()) ? c->mouseX : c->mouseY;
            float mousedepth = (CM_PER_INCH * pixels) / (dpi * 100);  // meter

            char *coordbuf = (char *)malloc(sizeof(char) * 256);
            {
                if (crossHairLabel == NULL) {
                    sprintf(coordbuf, "%.2fm", mousedepth);
                } else {
                    sprintf(coordbuf, "%.2fm - %s", mousedepth, crossHairLabel);
                }

                render_string_shadowed(coordbuf, 0, strlen(coordbuf) - 1);
            }
            free(coordbuf);
        }
        glPopMatrix();
    }
}

void render_grid(Canvas *c) {
    float x, y, z;
    get_camera_position(c->camera, &x, &y, &z);

    float rangeX, rangeY, incrementX, incrementY;
    float gxstart, gxend, gystart, gyend;

    // how many cm are we?
    rangeX = (c->w / c->dpi_x) * CM_PER_INCH;
    rangeY = (c->h / c->dpi_y) * CM_PER_INCH;

    // screen space increment of 1 cm
    incrementX = (1.0f / c->dpi_x) * CM_PER_INCH;
    incrementY = (1.0f / c->dpi_y) * CM_PER_INCH;

    // determine start and end in current units
    float interval_scale = c->grid_size;

    gxstart = floorf(x * incrementX / interval_scale);
    gxend = gxstart + ceilf(rangeX / interval_scale);
    gystart = floorf(y * incrementY / interval_scale);
    gyend = gystart + ceilf(rangeY / interval_scale);

    float k, gx0, gx1, gy0, gy1;
    int count = 0;
    gx0 = y + c->h;
    gx1 = y;
    gy0 = x;
    gy1 = x + c->w;

    glDisable(GL_TEXTURE_2D);
    glLineWidth((float)c->grid_thickness);
    glColor3f(c->r, c->g, c->b);

    // draw grid
    switch (c->grid_type) {
        case GRID_BASIC: {
            // vertical lines
            glPushMatrix();
            glScalef(interval_scale / incrementX, 1.0, 1.0);
            glBegin(GL_LINES);
            for (k = gxstart; k <= gxend; ++k) {
                glVertex2f(k, gx0);
                glVertex2f(k, gx1);
            }
            glEnd();
            glPopMatrix();

            // horizontal lines
            glPushMatrix();
            glScalef(1.0, interval_scale / incrementY, 1.0);
            glBegin(GL_LINES);
            for (k = gystart; k <= gyend; ++k) {
                glVertex2f(gy0, k);
                glVertex2f(gy1, k);
            }
            glEnd();
            glPopMatrix();
        } break;
        case GRID_HORIZON: {
            // horizontal lines
            glPushMatrix();
            glScalef(1.0, interval_scale / incrementY, 1.0);
            glBegin(GL_LINES);
            for (k = gystart; k <= gyend; ++k) {
                glVertex2f(gy0, k);
                glVertex2f(gy1, k);
            }
            glEnd();
            glPopMatrix();
        } break;
        case GRID_VERTICAL: {
            // vertical lines
            glPushMatrix();
            glScalef(interval_scale / incrementX, 1.0, 1.0);
            glBegin(GL_LINES);
            for (k = gxstart; k <= gxend; ++k) {
                glVertex2f(k, gx0);
                glVertex2f(k, gx1);
            }
            glEnd();
            glPopMatrix();
        } break;
        case GRID_POINT: {
            // horizontal lines
            glPointSize(c->grid_thickness * 2.0f);
            glPushMatrix();
            glScalef(interval_scale / incrementX, interval_scale / incrementY, 1.0);
            glBegin(GL_POINTS);
            for (k = gystart; k <= gyend; ++k)
                for (float l = gxstart; l <= gxend; ++l)
                    glVertex2f(l, k);
            glEnd();
            glPopMatrix();
        } break;
        case GRID_CPOINT: {
            // horizontal lines
            float halfx = c->grid_size / (interval_scale / incrementX);
            float halfy = c->grid_size / (interval_scale / incrementY);
            glPushMatrix();
            glScalef(interval_scale / incrementX, interval_scale / incrementY, 1.0);
            glBegin(GL_LINES);
            for (k = gystart; k <= gyend; ++k)
                for (float l = gxstart; l <= gxend; ++l) {
                    // vertical
                    glVertex2f(l, k + halfy);
                    glVertex2f(l, k - halfy);
                    // horizontal
                    glVertex2f(l + halfx, k);
                    glVertex2f(l - halfx, k);
                }
            glEnd();
            glPopMatrix();
        } break;
        default:
            break;
    }
}

// utility method to draw a marker at 'depth' mbsf showing it's 'age'
void render_age(float depth, float age) {
    // todo render a age model marker along with depth
}

void render_scale(Canvas *c) {
    float x, y, z;
    get_camera_position(c->camera, &x, &y, &z);

    // if vertical then draw on left side of canvas, else draw along
    // the bottom of the canvas

    float range, increment, translate, coverage;
    float dpi, scale;
    float tickstart, tickend;
    int tick_threshold;
    float text_vscale = 1.0f;
    if (c->horizontal) {
        text_vscale = c->h / c->h0;
        coverage = c->w;
        translate = x;
        dpi = c->dpi_x;
        scale = c->w / c->w0;

        // some numover of ticks per unit of screen space
        //  let's say ~ 1 tick per 5 cm

        tick_threshold = (int)(c->w / (scale * dpi) * CM_PER_INCH / 5.0f);
    } else {
        text_vscale = c->w / c->w0;
        coverage = c->h;
        translate = y;
        dpi = c->dpi_y;
        scale = c->h / c->h0;

        // some numover of ticks per unit of screen space
        //  let's say ~ 1 tick per 5 cm

        tick_threshold = (int)(c->h / (scale * dpi) * CM_PER_INCH / 5.0f);
    }

#ifdef DEBUG
    printf("Tick Threshold %d\n", tick_threshold);
#endif
    // how many mm are we?
    range = (coverage / dpi) * (CM_PER_INCH * 10.0f);

    // screen space increment of 1 cm
    increment = (1.0f / dpi) * CM_PER_INCH;

#ifdef DEBUG
    printf("%f mm\n", range);
#endif

    string units("mm");
    float interval_scale = .1f;

    if (range > tick_threshold)  // up to centimeters
    {
        range *= 0.1f;
        interval_scale *= 10.0f;  // increment is cm
        units = "cm";
    }

    if (range > tick_threshold)  // up to decimeters
    {
        range *= 0.1f;
        interval_scale *= 10.0f;  // increment is cm
        units = "dm";
    }

    if (range > tick_threshold)  // up to meters
    {
        range *= 0.1f;
        interval_scale *= 10.0f;  // increment is cm
        units = "m";
    }

    if (range > tick_threshold)  //up to deka meters
    {
        range *= 0.1f;
        interval_scale *= 10.0f;  // increment is cm
        units = "dam";
    }

    if (range > tick_threshold)  //up to hecto meters
    {
        range *= 0.1f;
        interval_scale *= 10.0f;  // increment is cm
        units = "hm";
    }

    if (range > tick_threshold)  //up to kilometers
    {
        range *= 0.1f;
        interval_scale *= 10.0f;  // increment is cm
        units = "km";
    }

#ifdef DEBUG
    printf("Range %f %s\n", range, units.c_str());
#endif

    // determine start and end in current units

    tickstart = floorf(translate * increment / interval_scale);
    tickend = tickstart + ceilf(range);

#ifdef DEBUG
    printf("increment %f mm, coverage %f\n", increment, coverage);
    printf("tickstart %f %s, tickend %f %s\n", tickstart, units.c_str(),
           tickend, units.c_str());
#endif

    // determine how much to scale the values when displaying
    float value_scale = 1.0f;

    if (units == "km") {
        value_scale = 1000.0f;
        units = "m";
    } else if (units == "hm") {
        value_scale = 100.0f;
        units = "m";
    } else if (units == "dam") {
        value_scale = 10.0f;
        units = "m";
    } else if (units == "dm") {
        value_scale = 10.0f;
        units = "cm";
    }

    // draw tick marks for every increment starting from tickstart
    int bufferSize = 128;
    char *buf = (char *)malloc(sizeof(char) * bufferSize);

    glBindTexture(GL_TEXTURE_2D, 0);
    glPushMatrix();
    {
        float k;
        float tickScaleFactor = interval_scale / increment;  // to k

        float r0;  // tickBottom y_coord@horizontal, tickStart x_coord@vert
        float r1;  // tickTop y_coord@horzontal, tickEnd x_coord@vert

        if (c->horizontal) {
            glScalef(tickScaleFactor, 1.0, 1.0);

            r0 = y + c->h;
            r1 = r0 - (50 * scale);
        } else {
            glScalef(1.0, tickScaleFactor, 1.0);

            r0 = x;
            r1 = r0 + (50 * scale);
        }

        // For tick labels: restore scale for text
        float text_hscale = text_vscale / tickScaleFactor;

        // Draw shadowed ticks, values and units
        for (k = tickstart; k <= tickend; ++k) {
            if (get_horizontal_depth()) {
                // draw outlined background first
                glLineWidth(5);
                glColor3f(0, 0, 0);
                glBegin(GL_LINES);
                {
                    glVertex2f(k, r0);
                    glVertex2f(k, r1);
                }
                glEnd();

                // draw tick lines
                glLineWidth(3);
                glColor3f(0.6f, 0.6f, 0.6f);
                glBegin(GL_LINES);
                {
                    glVertex2f(k, r0);
                    glVertex2f(k, r1);
                }
                glEnd();

                // Label
                memset(buf, 0, bufferSize);
                sprintf(buf, "%.2f %s", k * value_scale, units.c_str());
                int len = strlen(buf);

                float t_hscale = text_hscale;
                float t_vscale = text_vscale;

                if (len >= 10) {
                    t_hscale = text_hscale * 0.8f;
                    t_vscale = text_vscale * 0.8f;
                }

                glPushMatrix();
                {
                    float tx = get_horizontal_depth() ? k : r1;
                    float ty = get_horizontal_depth() ? r1 : k;

                    glTranslatef(tx, ty, 0);
                    glScalef(t_hscale, t_vscale, 1.0);

                    float color[3];
                    color[0] = color[1] = color[2] = 0.6f;
                    render_string_shadowed(buf, 0, len - 1, color, 2);
                }
                glPopMatrix();
            } else {
                glLineWidth(5);
                glColor3f(0, 0, 0);
                glBegin(GL_LINES);
                {
                    glVertex2f(r0, k);
                    glVertex2f(r1, k);
                }
                glEnd();

                // draw tick lines
                glLineWidth(3);
                glColor3f(0.6f, 0.6f, 0.6f);
                glBegin(GL_LINES);
                {
                    glVertex2f(r0, k);
                    glVertex2f(r1, k);
                }
                glEnd();

                // Label
                memset(buf, 0, bufferSize);
                sprintf(buf, "%.2f %s", k * value_scale, units.c_str());
                int len = strlen(buf);

                float t_hscale = text_hscale;
                float t_vscale = text_vscale;

                if (len >= 10) {
                    t_hscale = text_hscale * 0.8f;
                    t_vscale = text_vscale * 0.8f;
                }

                glPushMatrix();
                {
                    float tx = get_horizontal_depth() ? k : r1;
                    float ty = get_horizontal_depth() ? r1 : k;

                    glTranslatef(tx, ty, 0);
                    glScalef(text_vscale, t_hscale, 1.0);

                    float color[3];
                    color[0] = color[1] = color[2] = 0.6f;
                    render_string_shadowed(buf, 0, len - 1, color, 2);
                }
                glPopMatrix();
            }
        }
    }
    glPopMatrix();
    free(buf);

    // Display cache memory, FPS and other debug information
    if (getDebug()) {
        glPushMatrix();
        {
            char *buf = (char *)malloc(sizeof(char) * 128);
            {
                glTranslatef(x, y, 0);
                glScalef(scale, scale, 1.0);

                memset(buf, 0, 128);
                sprintf(buf, "DEBUG MODE: Type 'D' (case sensitive) to toggle.");
                glTranslatef(0, CHAR_HEIGHT / 2, 0);
                render_scaled_string(buf, 0, strlen(buf) - 1, 0.5f);

                const int texmem_usage_kb = get_cur_texmem_usage() / 1024;
                const float texmem_usage_pct = 100.0f * (get_cur_texmem_usage() / (float)(get_max_texmem_usage()));
                sprintf(buf, "Image Cache: %d KB (%.1f%% full)", texmem_usage_kb, texmem_usage_pct);
                glTranslatef(0, CHAR_HEIGHT / 2, 0);
                render_scaled_string(buf, 0, strlen(buf) - 1, 0.5f);

                sprintf(buf, "%.1f frames/sec", get_fps());
                glTranslatef(0, CHAR_HEIGHT / 2, 0);
                render_scaled_string(buf, 0, strlen(buf) - 1, 0.5f);
            }
            free(buf);
        }
        glPopMatrix();
    }
}

void run_through_scene_graph(Canvas *c) {
    if (!c) {
        return;
    }

#ifdef DEBUG
    else
        printf("Running thru scene graph\n");
#endif

    float x, y, z;
    get_camera_position(c->camera, &x, &y, &z);

    glPushMatrix();
    {
        // scale the world according to canvas scale
        // glScalef( c->sx, c->sy, 1.0f);

        // if( c->horizontal )
        {
            c->coverage_x = c->w;
            c->coverage_y = c->h;
            c->half_coverage_x = c->coverage_x * 0.5f;
            c->half_coverage_y = c->coverage_y * 0.5f;
        }
        /* else
        {
            c->coverage_x      = c->h;
            c->coverage_y      = c->w;
            c->half_coverage_x = c->coverage_x * .5;
            c->half_coverage_y = c->coverage_y * .5;
        } */

#ifdef DEBUG
        printf("Canvas has coverage of (%.2f, %.2f)\n", c->coverage_x,
               c->coverage_y);
#endif

        // draw the track scene
        render_track_scene(default_track_scene, c);

        // draw the general annotation scene
    }
    glPopMatrix();

    // draw measuring stuff
    // how many point we have?
    if (c->mode == CANVAS_MEASURE)  // 1: Measure mode
    {
        glPushMatrix();
        {
            if (!get_horizontal_depth()) {
                glRotatef(-90, 0, 0, 1);
            }

            glEnable(GL_BLEND);
            render_measure_mode(c);
            glDisable(GL_BLEND);
        }
        glPopMatrix();
    } else if (c->mode == CANVAS_CLAST)  // 3: Clast mode
    {
        glPushMatrix();
        {
            if (!get_horizontal_depth()) {
                glRotatef(-90, 0, 0, 1);
            }

            glEnable(GL_BLEND);
            render_clast_mode(c);
            glDisable(GL_BLEND);
        }
        glPopMatrix();
    } else if (c->mode == CANVAS_CUT)  // 4: Cut mode
    {
        glPushMatrix();
        {
            if (!get_horizontal_depth()) {
                glRotatef(-90, 0, 0, 1);
            }

            glEnable(GL_BLEND);
            {
                render_cut_mode(c);
            }
            glDisable(GL_BLEND);
        }
        glPopMatrix();
    }
}


bool is_s3tc_available() {
    return s3tc_available;
}

void set_clast_mode(int id, int mode) {
    canvasvec[id].clastMode = mode;
}

void set_clast_1st_point(int id, float x, float y) {
    canvasvec[id].clastPoint1.x = x;
    canvasvec[id].clastPoint1.y = y;

    set_clast_mode(id, CLAST_HAS_1ST_POINT);
}

void set_clast_2nd_point(int id, float x, float y) {
    canvasvec[id].clastPoint2.x = x;
    canvasvec[id].clastPoint2.y = y;

    set_clast_mode(id, CLAST_RECT_DEFINED);
}

void render_clast_mode(Canvas *c) {
    float x1 = get_horizontal_depth() ? c->clastPoint1.x : -c->clastPoint1.y;
    float y1 = get_horizontal_depth() ? c->clastPoint1.y : c->clastPoint1.x;

    c->clastPoint2.x = c->mouseX;
    c->clastPoint2.y = c->mouseY;

    float x2 = get_horizontal_depth() ? c->clastPoint2.x : c->clastPoint2.x;
    float y2 = get_horizontal_depth() ? c->clastPoint2.y : c->clastPoint2.y;

    if (c->clastMode == CLAST_HAS_1ST_POINT) {
        // Bounding box
        glLineWidth(1.0);
        glColor4f(0.549f, 0.549f, 1.0f, 0.8f);
        glBegin(GL_QUADS);
        {
            glVertex2f(x1, y1);
            glVertex2f(x2, y1);
            glVertex2f(x2, y2);
            glVertex2f(x1, y2);
        }
        glEnd();

        float scale = c->horizontal ? c->w / c->w0 / 2 : c->h / c->h0 / 2;
        float dpi = c->horizontal ? c->dpi_x : c->dpi_y;

        int bufferSize = 128;
        char *coordbuf = (char *)malloc(sizeof(char) * bufferSize);
        memset(coordbuf, 0, bufferSize);

        glColor4f(1, 1, 1, 1);
        // Upper left Corner
        glPushMatrix();
        {
            glTranslatef(x1, y1, 0);
            glScalef(scale, scale, 1.0);
            glTranslatef(12, -10, 0);
            float xx = x1 / dpi * CM_PER_INCH;
            float yy = y1 / dpi * CM_PER_INCH;
            sprintf(coordbuf, "(%.2f, %.2f)", xx, yy);
            render_string_shadowed(coordbuf, 0, strlen(coordbuf) - 1);
        }
        glPopMatrix();

        // Center showing width and height
        glPushMatrix();
        {
            float centerX = (x1 + x2) / 2;
            float centerY = (y1 + y2) / 2;
            float width = fabs(x1 - x2);
            float height = fabs(y1 - y2);

            glTranslatef(centerX, centerY, 0);
            glScalef(scale, scale, 1.0);

            glTranslatef(12, -10, 0);
            float xx = width / dpi * CM_PER_INCH;
            float yy = height / dpi * CM_PER_INCH;
            sprintf(coordbuf, "(W: %.2f, H: %.2f)cm", xx, yy);
            render_string_shadowed(coordbuf, 0, strlen(coordbuf) - 1);
        }
        glPopMatrix();

        free(coordbuf);
    }
}

void render_cut_mode(Canvas *c) {
    float x1 = get_horizontal_depth() ? c->clastPoint1.x : -c->clastPoint1.y;
    float y1 = get_horizontal_depth() ? c->clastPoint1.y : c->clastPoint1.x;

    c->clastPoint2.x = c->mouseX;
    c->clastPoint2.y = c->mouseY;

    float x2 = get_horizontal_depth() ? c->clastPoint2.x : c->clastPoint2.x;
    float y2 = get_horizontal_depth() ? c->clastPoint2.y : c->clastPoint2.y;

    if (c->clastMode == CLAST_HAS_1ST_POINT) {
        // Bounding box
        glLineWidth(1.0);
        glColor4f(0.549f, 0.549f, 1.0f, 0.8f);
        glBegin(GL_QUADS);
        {
            glVertex2f(x1, y1);
            glVertex2f(x2, y1);
            glVertex2f(x2, y2);
            glVertex2f(x1, y2);
        }
        glEnd();

        float scale = c->horizontal ? c->w / c->w0 / 2 : c->h / c->h0 / 2;
        float dpi = c->horizontal ? c->dpi_x : c->dpi_y;

        int bufferSize = 128;
        char *coordbuf = (char *)malloc(sizeof(char) * bufferSize);
        memset(coordbuf, 0, bufferSize);

        glColor4f(1, 1, 1, 1);
        // Upper left Corner
        glPushMatrix();
        {
            glTranslatef(x1, y1, 0);
            glScalef(scale, scale, 1.0);
            glTranslatef(12, -10, 0);
            float xx = x1 / dpi * CM_PER_INCH;
            float yy = y1 / dpi * CM_PER_INCH;
            sprintf(coordbuf, "(%.2f, %.2f)", xx, yy);
            render_string_shadowed(coordbuf, 0, strlen(coordbuf) - 1);
        }
        glPopMatrix();

        // Center showing width and height
        glPushMatrix();
        {
            float centerX = (x1 + x2) / 2;
            float centerY = (y1 + y2) / 2;
            float width = fabs(x1 - x2);
            float height = fabs(y1 - y2);

            glTranslatef(centerX, centerY, 0);
            glScalef(scale, scale, 1.0);

            glTranslatef(12, -10, 0);
            float xx = width / dpi * CM_PER_INCH;
            float yy = height / dpi * CM_PER_INCH;
            sprintf(coordbuf, "(W: %.2f, H: %.2f)cm", xx, yy);
            render_string_shadowed(coordbuf, 0, strlen(coordbuf) - 1);
        }
        glPopMatrix();

        free(coordbuf);
    }
}

void render_measure_mode(Canvas *c) {
    glBindTexture(GL_TEXTURE_2D, 0);

    switch (c->nummeasurepoint) {
        case (MEASURE_NOT):  // not in measuring mode
            break;
        case (MEASURE_READY):  // ready mode, show some guide message "click the first point"
            break;
        case (MEASURE_ONE_POINT):  // got one point
        {
            // draw line from the first point to current mouse pos
            // test shadow
            glLineWidth(3);
            glColor3f(0, 0, 0);
            glBegin(GL_LINES);
            {
                glVertex2f(c->measurepoint[0], c->measurepoint[1]);
                glVertex2f(c->mouseX, c->mouseY);
            }
            glEnd();
            glLineWidth(1.5);
            glColor3f(1, 1, 1);
            glBegin(GL_LINES);
            {
                glVertex2f(c->measurepoint[0], c->measurepoint[1]);
                glVertex2f(c->mouseX, c->mouseY);
            }
            glEnd();

            // draw misc text
            glTranslatef(c->measurepoint[0], c->measurepoint[1], 0);
            float scale = c->horizontal ? c->w / c->w0 / 2 : c->h / c->h0 / 2;
            glScalef(scale, scale, 1.0);
            glTranslatef(12, -10, 0);
            float dpi = c->horizontal ? c->dpi_x : c->dpi_y;
            float xx = c->measurepoint[0] / dpi * CM_PER_INCH;
            float yy = c->measurepoint[1] / dpi * CM_PER_INCH;

            // char coordbuf[128];
            int bufferSize = 128;
            char *coordbuf = (char *)malloc(sizeof(char) * bufferSize);
            {
                memset(coordbuf, 0, bufferSize);
                sprintf(coordbuf, "(%.2f, %.2f)", xx, yy);
                render_string_shadowed(coordbuf, 0, strlen(coordbuf) - 1);
            }
            free(coordbuf);

            glTranslatef(-12, 10, 0);

            // draw end point
            glColor3f(1, 0, 0);
            draw_solid_square(0, 0, 20);

            glScalef(1.0f / scale, 1.0f / scale, 1.0f);
            glTranslatef(-c->measurepoint[0], -c->measurepoint[1], 0);
        } break;
        case (MEASURE_TWO_POINT):  // two complete point sets
        {
            // draw line from the first point to current mouse pos
            glLineWidth(3);
            glColor3f(0, 0, 0);
            glBegin(GL_LINES);
            {
                glVertex2f(c->measurepoint[0], c->measurepoint[1]);
                glVertex2f(c->measurepoint[2], c->measurepoint[3]);
            }
            glEnd();
            glLineWidth(1.5);
            glColor3f(0.8f, 0.8f, 0.8f);
            glBegin(GL_LINES);
            {
                glVertex2f(c->measurepoint[0], c->measurepoint[1]);
                glVertex2f(c->measurepoint[2], c->measurepoint[3]);
            }
            glEnd();

            // draw misc text
            // the first end point coord
            glTranslatef(c->measurepoint[0], c->measurepoint[1], 0);
            float scale = c->horizontal ? c->w / c->w0 / 2 : c->h / c->h0 / 2;
            glScalef(scale, scale, 1.0);
            glTranslatef(12, -10, 0);
            float dpi = c->horizontal ? c->dpi_x : c->dpi_y;
            float xx1 = c->measurepoint[0] / dpi * CM_PER_INCH;
            float yy1 = c->measurepoint[1] / dpi * CM_PER_INCH;

            // char coordbuf[128];
            int bufferSize = 128;
            char *coordbuf = (char *)malloc(sizeof(char) * bufferSize);
            memset(coordbuf, 0, bufferSize);

            sprintf(coordbuf, "(%.2f, %.2f)", xx1, yy1);
            glColor3f(1, 1, 1);
            render_string_shadowed(coordbuf, 0, strlen(coordbuf) - 1);

            glTranslatef(-12, 10, 0);

            // draw end point
            glColor3f(1, 0, 0);
            draw_solid_square(0, 0, 20);

            glScalef(1.0f / scale, 1.0f / scale, 1.0f);
            glTranslatef(-c->measurepoint[0], -c->measurepoint[1], 0);

            // the second end point coord
            glColor3f(1, 1, 1);
            glTranslatef(c->measurepoint[2], c->measurepoint[3], 0);
            glScalef(scale, scale, 1.0);
            glTranslatef(12, -10, 0);
            float xx2 = c->measurepoint[2] / dpi * CM_PER_INCH;
            float yy2 = c->measurepoint[3] / dpi * CM_PER_INCH;
            sprintf(coordbuf, "(%.2f, %.2f)", xx2, yy2);
            render_string_shadowed(coordbuf, 0, strlen(coordbuf) - 1);
            glTranslatef(-12, 10, 0);

            // draw end point
            glColor3f(1, 0, 0);
            draw_solid_square(0, 0, 20);

            // distance info
            float dist;
            dist = (xx1 - xx2) * (xx1 - xx2) + (yy1 - yy2) * (yy1 - yy2);
            dist = sqrtf(dist);
            glTranslatef(12, 40, 0);
            glColor3f(1, 1, 1);
            sprintf(coordbuf, "Distance: %.2f cm", dist);
            render_string_shadowed(coordbuf, 0, strlen(coordbuf) - 1);
            glTranslatef(-12, -40, 0);
            glScalef(1.0f / scale, 1.0f / scale, 1.0f);
            glTranslatef(-c->measurepoint[2], -c->measurepoint[3], 0);

            free(coordbuf);

            // distance in center
            /*
                glColor3f( 1, 1, 1);
                float dist, mx, my;
                dist = (xx1 - xx2) * (xx1 - xx2) + (yy1 - yy2) * (yy1 - yy2);
                dist = sqrtf(dist);
                mx = (c->measurepoint[0] + c->measurepoint[2])/2.0f;
                my = (c->measurepoint[1] + c->measurepoint[3])/2.0f;
                glTranslatef(mx, my, 0);
                glScalef( scale, scale, 1.0);
                glTranslatef(10, 20, 0);
                sprintf(coordbuf, "Distance: %.2f cm", dist);
                render_string_shadowed(coordbuf, 0, strlen(coordbuf)-1);
                glTranslatef(-10, -20, 0);
                glScalef( 1.0 / scale, 1.0 / scale, 1.0);
                glTranslatef( -mx, -my, 0);
                */
        } break;
    }  // end of switch
}

void set_bgcolor(const float *aColor) {
    bgcolor[0] = aColor[0];
    bgcolor[1] = aColor[1];
    bgcolor[2] = aColor[2];
}

float *get_bgcolor() {
    return bgcolor;
}

void set_crosshair(bool b) {
    hasCrossHair = b;
}

bool has_crosshair() {
    return hasCrossHair;
}

bool get_horizontal_depth() {
    return horizontalDepth;
}

void set_horizontal_depth(bool b) {
    horizontalDepth = b;

    for (vector<Canvas>::iterator it = canvasvec.begin();
         it != canvasvec.end();
         ++it) {
        it->horizontal = b;

        /*
        float t = it->x;
        it->x = it->y;
        it->y = t;
        printf("AHHHH %.2f, %.2f\n", it->x, it->y);
        */

        // swap(it->coverage_x, it->coverage_y);
        // swap(it->half_coverage_x, it->half_coverage_y);
        // swap(it->dpi_x, it->dpi_y);
    }
}

bool is_show_origin() {
    return showOrigin;
}

void set_show_origin(bool b) {
    showOrigin = b;
}

void set_crosshair_label(char *label) {
    if (crossHairLabel != NULL) {
        // free(crossHairLabel);
        delete[] crossHairLabel;
        crossHairLabel = NULL;
    }

    if (label != NULL) {
        crossHairLabel = new char[128];
        strcpy(crossHairLabel, label);
    }
}

void set_canvas_rows_and_columns(int nrow, int ncols) {
    number_of_rows = nrow;
    number_of_cols = ncols;
}

void setTieDepth(bool isEnabled, float depth) {
    showTieDepth = isEnabled;
    tieDepth = depth;
}

float get_fps() {
    return framesPerSecond;
}

// Track and average the last five render times for a pseudo frames/second.
// Time is tracked separately for each canvas, add times until a complete
// render of all canvases is complete, then update FPS.
void update_fps(const int lastRenderTime) {
    static const int NUM_RENDERS = 5;
    static int lastRenders[NUM_RENDERS] = {0, 0, 0, 0, 0};
    static int canvasRenders = 0, totalRenders = 0;

    if (canvasRenders == 0)
        lastRenders[totalRenders % NUM_RENDERS] = 0;
    lastRenders[totalRenders % NUM_RENDERS] += lastRenderTime;
    canvasRenders++;

    if (canvasRenders % num_canvases() == 0) {
        int totalTime = 0;
        for (int i = 0; i < NUM_RENDERS; i++) {
            totalTime += lastRenders[i];
        }
        const float avgSecPerFrame = (totalTime / (float)NUM_RENDERS) / CLOCKS_PER_SEC;
        //if (avgSecPerFrame > 0.0f)
        framesPerSecond = 1.0f / avgSecPerFrame;

        canvasRenders = 0;
        totalRenders++;
    }
}