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
import corelyzer.plugin.iCores.ui.ICoreFrame;
import corelyzer.util.StringUtility;

import javax.swing.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;

public class CacheEntry {
    String type;
    String project; // eg. LacCore, ANDRILL, ICDP, IODP...
    String hole;    // eg. GLAD4, MIS, SMS, SAFOD1, 
    String remote;
    String local;
    Date   createTime;
    Date   lastUpdateTime;
    float  size;

    int retryCounts = 5;

    boolean isReady; // only load the entry when it's ready
    Runnable observer;

    public CacheEntry() {
        super();
    }

    // Create a entry of TYPE(t), using URL(r)
    public CacheEntry(String aURL, String aType) {
        this();

        isReady = false;
        remote = aURL;
        type   = aType;

        if(aType.equalsIgnoreCase("image") || aType.equalsIgnoreCase("dataset")) {
            try {
                URL u = new URL(remote);
                String [] toks = u.getFile().split("/");

                if(toks.length > 0) {
                    local = toks[toks.length-1];
                } else {
                    local = u.getFile();
                }
            } catch (MalformedURLException e) {
                local = StringUtility.getSHASum(remote);
            }
        } else if(aType.equalsIgnoreCase("feed")) {
            local = StringUtility.getSHASum(remote);
        } else {
            local = StringUtility.getSHASum(remote); 
        }

        this.isReady = CacheEntry.determineIfReadyInInit(local, type);
    }
    
    public static CacheEntry initWithHashtable(Hashtable<String, String> hash) {
        CacheEntry entry = new CacheEntry();

        entry.type       = hash.get("type");
        entry.remote     = hash.get("remote");
        entry.local      = hash.get("local");
        entry.createTime = new Date(Long.valueOf(hash.get("createTime")));
        entry.lastUpdateTime = new Date(
                Long.valueOf(hash.get("lastUpdateTime")));
        entry.size = Float.valueOf(hash.get("size"));

        entry.isReady = CacheEntry.determineIfReadyInInit(entry.local, entry.type);

        return entry;
    }

    private static boolean determineIfReadyInInit(String aLocal, String aType) {
        // FIXME
        String sp = System.getProperty("file.separator");
        String cacheDir = CacheManager.getCacheManager().getCacheDir();
        String typeStr  = (aType.equalsIgnoreCase("image")) ? "downloads" :
                aType;
        String filePath = cacheDir + sp + typeStr + sp + aLocal;

        return (new File(filePath)).exists();
    }

    public Hashtable<String, String> getDictionary() {
        Hashtable<String, String> dict = new Hashtable<String, String>();

        dict.put("type", type);
        dict.put("remote", remote);
        dict.put("local",  local);
        dict.put("createTime", String.valueOf(createTime.getTime()));
        dict.put("lastUpdateTime", String.valueOf(lastUpdateTime.getTime()));
        dict.put("size", String.valueOf(size));

        return dict;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public boolean doDownload(String cacheDir) {
        String sp = System.getProperty("file.separator");
        String dir = cacheDir + sp + type;
        String localFile = dir + sp + local;
        String tmpLocalFile = cacheDir + sp + "tmp" + sp + local;

        File fDir = new File(dir);
        if(!fDir.exists()) {
            boolean isMade = fDir.mkdir();

            if(!isMade) return isMade;
        }

        try {
            size = URLRetrieval.retrieveLocalCopyWithLength(remote,
                    tmpLocalFile, "username", "password");
        } catch (Exception e) {
            System.out.println(
                    "---> Exception in CacheEntry.URLRetrieval: " + e);
            // e.printStackTrace();

            String mesg = "Cannot refresh the feed: '" + remote +
                    "'\nReason: " + e;
            JOptionPane.showMessageDialog(ICoreFrame.getIcoreFrame(), mesg);

            size = -1;
        }

        if(size != -1) {            
            if((new File(tmpLocalFile)).renameTo(new File(localFile))) {
                if(createTime == null) {
                    createTime = new Date();
                }
                lastUpdateTime = new Date();

                return true;
            } else {
                System.out.println("---> Cannot move " + tmpLocalFile +
                        " to " + localFile + ", weird.");
                return false;
            }
        } else {
            return false;
        }
    }

    public Runnable getObserver() {
        return observer;
    }

    public void setObserver(Runnable observer) {
        this.observer = observer;
    }
}

