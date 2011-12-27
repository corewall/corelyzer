/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2008 Julian Yu-Chung Chen
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
package corelyzer.plugin.correlator;

import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.plugin.freedraw.FreedrawPlugin;

import javax.swing.*;

// public class CorrelatorPlugin extends CorelyzerPlugin {
public class CorrelatorPlugin extends FreedrawPlugin {
    private static CorrelatorPlugin correlatorPlugin = null;

    CorrelatorDialog view;

    public static CorrelatorPlugin getCorrelatorPlugin() {
        return correlatorPlugin;
    }

    public CorrelatorPlugin() {
        super();
        correlatorPlugin = this;
    }

    public boolean start() {//(Component component) {
        // create our frame
    	view = new CorrelatorDialog();
        view.getController().setPluginId(pluginId);
        view.pack();
        view.setSize(640, 480);
        view.setLocationRelativeTo(null);
        view.setAlwaysOnTop(true);

        return true;
    }

    // public void fini() {
    //     view = null;
    // }

    public void processEvent(CorelyzerPluginEvent event) {
    }

    public JFrame getFrame() {
        return view;
    }
}
