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

import corelyzer.helper.URLRetrieval;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/** A thread to download things other than feeds */
public class DownloadThread extends Thread {
    CacheManager cacheMgr;
    boolean keepRunning;

    LinkedList<CacheEntry> queue;

    public DownloadThread() {
        super();
        queue = new LinkedList<CacheEntry>();
    }

    public DownloadThread(CacheManager mgr) {
        this();
        cacheMgr = mgr;
        keepRunning = true;
    }

    public void start() {
        System.out.println("---> [INFO] DownloadThread starts.");
        super.start();            
    }

    public void run() {
        while(keepRunning) {
            try {
                if(queue.isEmpty()) {
                    Thread.sleep(3000);
                } else {
                    CacheEntry entry; // TODO use entry.retrys

                    while( (entry = queue.removeFirst()) != null ) {
                        if (!entry.isReady) {
                            int retCode = download(entry);

                            if (retCode != -1) {
                                System.out.println("---> [INFO] '" +
                                        entry.getRemote() + "' downloaded.");

                                if (entry.createTime == null) {
                                    entry.createTime = new Date();
                                }
                                entry.lastUpdateTime = new Date();
                                entry.setReady(true);

                                queue.remove(entry);
                            } else {
                                System.out.println(
                                        "---> [WARN] Incomplete download '" +
                                                entry.getRemote() +
                                                "', need to try again later...");
                                // queue.add(entry);
                            }
                        }
                    }

                    sleep(2000);
                }
            } catch (NoSuchElementException e) {
                System.out.println("---> [WARNING] No such element exception");
            } catch (InterruptedException e) {
                System.err.println(
                        "---> [EXCEPTION] DownloadThread Interrupted: " + e);                
            }
        }
    }

    public void add(CacheEntry entry) {
        queue.add(entry);
    }

    private int download(CacheEntry entry) {
        // TODO need to have some visual cue in the UI, like 'download list panel'
        if(entry.type.equalsIgnoreCase("feed")) {
            System.out.println("---> Ignore if cacheEntry is feed");
            return -1;
        }

        System.out.println("---> [INFO] Download entry:'" + entry.remote + "'");

        String sp = System.getProperty("file.separator");
        String type = entry.getType();
        String url = entry.getRemote();
        String typeDir;
        // if(type.equalsIgnoreCase("image") || type.equalsIgnoreCase("dataset")) {
        if( type.toLowerCase().contains("image") ||
            type.toLowerCase().contains("dataset") ) {
            typeDir = cacheMgr.getCacheDir() + sp + "downloads"; // FIXME 
        } else {
            typeDir = cacheMgr.getCacheDir() + sp + type;
        }

        // create type dir
        File fDir = new File(typeDir);
        if(!fDir.exists()) {
            fDir.mkdir();
        }

        String local = entry.getLocal();
        local = typeDir + sp + local;

        // TODO create 'project_dir' or 'feedname(hole)_dir'

        int downloadSize;
        try {
            downloadSize = URLRetrieval.retrieveLocalCopyWithLength(url, local,
                    "username", "password");
        } catch (IOException e) {
            System.out.println("---> [Exception] DownloadThread:136 " + e);
            downloadSize = -1;
        }

        return downloadSize;
    }

    public boolean isKeepRunning() {
        return keepRunning;
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }
}
