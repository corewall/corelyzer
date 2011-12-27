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
package corelyzer.plugin.iCores.handlers;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestContext {
    private final ExecutorService ioPool;
    private final JLabel status;
    private final ImageIcon statusIcon = new ImageIcon(getClass().getResource(
            "/corelyzer/plugins/expeditionmanager/ui/resources/progress.gif")); // FIXME
    private final List<String> jobs;

    public RequestContext(final JLabel status) {
        // this.freedraw = freedraw;
        this.status = status;

        // multiple threads for IO jobs
        ioPool = Executors.newCachedThreadPool();
        jobs = Collections.synchronizedList(new ArrayList<String>());
    }

    public void jobCompleted() {
        jobs.remove(0);
        updateStatus();
    }

    private void updateStatus() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (jobs.size() == 0) {
                    status.setIcon(null);
                    status.setText("");
                } else {
                    status.setIcon(statusIcon);
                    status.setText(jobs.get(0) + " (" + jobs.size()
                            + " remaining)");
                }
            }
        });
    }
}
