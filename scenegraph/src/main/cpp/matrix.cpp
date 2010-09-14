#include "matrix.h"

// identity matrix in row-major is same as in column-major... :)
const GLfloat opengl_identity[16] = 
                             { 1.0f, 0.0f, 0.0f, 0.0f,   // first column
                               0.0f, 1.0f, 0.0f, 0.0f,   // second column
                               0.0f, 0.0f, 1.0f, 0.0f,   // third column
                               0.0f, 0.0f, 0.0f, 1.0f};  // fourth column

//------------------------------------
void make_identity(GLfloat *m)
{
    if( !m ) return;
    for(int i = 0; i < 16; i++)
        m[i] = opengl_identity[i];
}

//------------------------------------
void copy_matrix(GLfloat *dst, GLfloat* src)
{
    if( !dst || !src ) return;
    for( int i = 0; i < 16; i++)
        dst[i] = src[i];
}

//------------------------------------
// a * b -> d
void mul_matrix(GLfloat *d, GLfloat *a, GLfloat* b)
{

    for( int c = 0; c < 4; c++)
    {
        for( int r = 0; r < 4; r++)
        {
            d[c * 4 + r] = 
                (a[r     ] * b[4 * c    ]) +
                (a[4  + r] * b[4 * c + 1]) +
                (a[8  + r] * b[4 * c + 2]) +
                (a[12 + r] * b[4 * c + 3]) ;
        }
    }
}

//------------------------------------
void print_matrix(GLfloat *m)
{
    printf("Matrix Print Out --\n");
    printf(" %f, %f, %f, %f,\n", m[0], m[4], m[8], m[12]);
    printf(" %f, %f, %f, %f,\n", m[1], m[5], m[9], m[13]);
    printf(" %f, %f, %f, %f,\n", m[2], m[6], m[10], m[14]);
    printf(" %f, %f, %f, %f;\n", m[3], m[7], m[11], m[15]);
}

