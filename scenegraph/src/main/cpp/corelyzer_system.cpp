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

#include "corelyzer_system.h"

//====================================================================
JNIEnv* jenv = NULL;
jobject plugin_manager_object;
jclass  plugin_manager_class;
jmethodID plugin_manager_get_plugin;

//====================================================================
void set_current_jnienv(JNIEnv* e)
{
    jenv = e;
}

//====================================================================
JNIEnv* get_current_jnienv()
{
    return jenv;
}

//====================================================================
void set_plugin_manager_object(jobject manager)
{
    plugin_manager_object = jenv->NewGlobalRef(manager);
    plugin_manager_class  = jenv->GetObjectClass(plugin_manager_object);
    plugin_manager_get_plugin = jenv->GetMethodID( plugin_manager_class,
                                                   "getPlugin",
                                                   "(I)Lcorelyzer/plugin/CorelyzerPlugin;");
}

//====================================================================
jobject get_plugin_manager_object()
{
    return plugin_manager_object;
}

//====================================================================
jclass get_plugin_manager_class()
{
    return plugin_manager_class;
}

//====================================================================
jmethodID get_plugin_manager_get_plugin()
{
    return plugin_manager_get_plugin;
}
