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

#ifndef CORELYZER_FONT_SYS_H
#define CORELYZER_FONT_SYS_H

#define CHAR_HEIGHT       48
#define CHAR_FIXED_WIDTH  32
#define LINE_VERT_ADVANCE 52


int         queue_font_to_load  (const char*);
void        set_current_font    (int font);
bool        is_font             (int font);

int         get_num_fonts       ();
int         get_current_font    ();
const char* get_font_name       ();
int         get_char_escapement (char c);

void        render_string       (const char* str, int start, int end);
void        render_string_label (const char* str, int start, int end);
void        render_string_shadowed (const char* str, int start, int end, 
									float* color=NULL, float offset = 3.0f);
void        render_string_outlined (const char* str, int start, int end);

#endif
