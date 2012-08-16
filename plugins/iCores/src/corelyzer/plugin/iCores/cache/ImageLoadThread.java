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
package corelyzer.plugin.iCores.cache;

import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;

import java.io.File;
import java.util.LinkedList;

public class ImageLoadThread extends Thread {
    CacheManager cacheMgr;
    LinkedList<ImageLoadEntry> queue;
    boolean keepRunning;
    
    public ImageLoadThread() {
        super();
        queue = new LinkedList<ImageLoadEntry>();
    }

    public ImageLoadThread(CacheManager mgr) {
        this();
        cacheMgr = mgr;
        keepRunning = true;
    }

    public void start() {
        System.out.println("---> [INFO] ImageLoadThread starts");
        super.start();
    }

    public void run() {
        while(keepRunning) {
            try {
                if(queue.isEmpty()) {
                    Thread.sleep(3000);
                    continue;
                }

                ImageLoadEntry entry;
                while( (entry = queue.getFirst()) != null ) {
                    if(entry.cache.isReady()) {
                        // Load image, remove from the loading queue
                        if(loadImage(entry)) {
                            queue.remove(entry);
                        }
                    } else {
                        System.out.println(
                                "---> [INFO] The item is not ready yet, " +
                                        "sleep 10sec and try again.");
                        sleep(10000);
                    }
                }
                
                sleep(5000);
            } catch (Exception e) {
                System.err.println("---> [EXCEPTION] Interrupted: " + e);
                e.printStackTrace();
            }
        }
    }

    private boolean loadImage(ImageLoadEntry entry) {
        System.out.println("---> [INFO] In loadImage()");

        if(!entry.cache.isReady()) {
            System.out.println("---> [INFO] CacheEntry is not ready yet.");
            return false;
        }

        String sp = System.getProperty("file.separator");
        String type = entry.cache.getType();
        String url  = entry.cache.getRemote();
        String localFilename = entry.cache.getLocal();
        // FIXME
        localFilename = cacheMgr.getCacheDir() + sp + "downloads" + sp +
                localFilename;

        File imageFile = new File(localFilename);
        if(imageFile.exists()) {

            CorelyzerApp app = CorelyzerApp.getApp();
            if(app == null) {
                System.out.println("---> [INFO] There's no Corelyzer " +
                        "visualization context, will return 'true' but " +
                        "image not loaded.");
                System.out.println("---> [SIM]  Loading local filename: '" +
                        imageFile + "'");
                return true;
            }
                        
            System.out.println("---> [INFO] Really Load image '" + imageFile + "' with properties.");

            CorelyzerApp.getApp().setSelectedTrack(entry.trackId);
            int secId = CorelyzerApp.getApp().loadImage(imageFile, url);

            SceneGraph.lock();
            {
                SceneGraph.setSectionOrientation(entry.trackId, secId,
                        entry.isPortrait);
                SceneGraph.rotateSection(entry.trackId, secId, entry.rotation);
                SceneGraph.positionSection(entry.trackId, secId,
                                           entry.depth * 100.0f / 2.54f *
                                           SceneGraph.getCanvasDPIX(0),0);
                SceneGraph.setSectionDPI(entry.trackId, secId,
                                         entry.dpi_x, entry.dpi_y);
                SceneGraph.bringSectionToFront(entry.trackId, secId);
                SceneGraph.setSectionMovable(entry.trackId, secId, false);

                // TODO append annotations if there's any
            }
            SceneGraph.unlock();

            return true;
        } else {
            System.err.println("---> [ERROR] '" + localFilename +
                    "' doesn't exist!");
            return false;
        }
    }

    public void add(ImageLoadEntry entry) {
        queue.add(entry);
    }

    public boolean isKeepRunning() {
        return keepRunning;
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }
}

