#ifndef MATRIX_H
#define MATRIX_H

#if defined(WIN32) || defined(_WIN32)
#include <windows.h>
#endif

#include <stdio.h>
#include <stdlib.h>

#ifndef __APPLE__
#include <GL/gl.h>
#include <GL/glu.h>
#else
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#endif

extern const GLfloat opengl_identity[16];

void make_identity (GLfloat *m);
void copy_matrix   (GLfloat *dst, GLfloat* src);
void mul_matrix    (GLfloat *c, GLfloat* a, GLfloat* b); // a * b --> c
void print_matrix  (GLfloat *m);


#endif
