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

import corelyzer.data.CRPreferences;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** A Thread to check in 'refreshItnerval' minutes, and refresh feed(nodes) */
public class CacheRefreshThread extends Thread {
    CacheManager cacheMgr;

    File cacheDir;
    int refreshInterval = 10; // in minutes
    boolean keepRunning = true;

    Executor executor = Executors.newFixedThreadPool(10);

    public CacheRefreshThread() {
        super();
    }

    public CacheRefreshThread(CacheManager mgr) {
        this();
        cacheMgr = mgr; 
        cacheDir = new File(mgr.getCacheDir());

        if(!cacheDir.exists()) {
            System.err.println("---> [WARNING] cacheDir '" + cacheDir +
                    "' does not exist.");
            boolean isMade = cacheDir.mkdir();

            if(!isMade) {
                System.err.println("---> [ERROR] Cannot create cachedir '" +
                        cacheDir + "', the refresh will not start.");
                return;
            }
        }

        CRPreferences prefs = cacheMgr.getPrefs();
        refreshInterval = (prefs == null) ? refreshInterval :
                prefs.getRefreshInterval();
    }

    public void start() {
        System.out.println("---> [INFO] CacheRefreshThread starts");
        super.start();
    }

    public void run() {
        while(keepRunning) {
            if(!cacheMgr.mapping.isEmpty()) {
                Collection<CacheEntry> all = cacheMgr.mapping.values();

                // sequencial checking
                for(CacheEntry entry : all) {
                    // Only auto refresh feeds now
                    if(!entry.getType().equalsIgnoreCase("feed")) {
                        // System.out.println("---> [INFO] '" + entry.getRemote()
                        //         + "' is not a feed, next please");
                        continue;
                    }

                    // Just feeds, spawn a thread to let the feednode update
                    // itself

                    Runnable observer = entry.getObserver();
                    if(observer != null) {
                        System.out.println(
                                "---> [CacheRefreshThread] YES! Has an " +
                                        "observer, go!");
                        
                        executor.execute(observer);
                    } else {
                        // FIXME no observer in init, since no FeedNode initialized yet...
                        System.out.println(
                                "---> [CacheRefreshThread] NO! Feed '" + 
                                        entry.getRemote() +
                                        "' has no observer?! Ignore for now.");                        
                    }
                }
            } else {
                System.out.println("No feeds, sleep " + this.refreshInterval
                        + " minutes");
            }

            // Refresh every 10 minutes
            try {
                System.out.println("[CacheRefreshThread] Now goto sleep for " +
                        refreshInterval + " minutes");
                Thread.sleep(refreshInterval * 60 * 1000);
            } catch (InterruptedException e) {
                System.err.println(
                        "---> [EXCEPTION] CacheRefreshThread Interrupted " +
                                "for Termination" + e);
            }
        } // end of while loop        
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public boolean isKeepRunning() {
        return keepRunning;
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }
}
