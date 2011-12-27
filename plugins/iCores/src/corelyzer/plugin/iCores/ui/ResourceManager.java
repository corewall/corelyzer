/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2007 Julian Yu-Chung Chen
 * Electronic Visualization Laboratory, University of Illinois at Chicago
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
package corelyzer.plugin.iCores.ui;

import javax.swing.*;
import java.util.Vector;

public class ResourceManager {
    static ResourceManager manager;

    static public int INDICATOR = 0;
    static public int PI = 1;

    Vector<ImageIcon> icons;

    public ResourceManager() {
        super();

        icons = new Vector<ImageIcon>();
        
        loadResources();
        manager = this;
    }

    private void loadResources() {
        System.out.println("---> [INFO] Loading resources...");
        ImageIcon icon = new ImageIcon(getClass().getResource(
                  "/corelyzer/plugin/iCores/ui/resources/icons/indicator.gif"));
        icons.add(icon);

        icon = new ImageIcon(getClass().getResource(
                  "/corelyzer/plugin/iCores/ui/resources/icons/pi.png"));
        icons.add(icon);
    }

    public static ResourceManager getResourceManager() {
        return manager;
    }

    public ImageIcon getImageIcon(int index) {
        return icons.get(index);
    }
}
