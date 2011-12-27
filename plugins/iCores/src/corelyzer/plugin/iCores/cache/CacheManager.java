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
import corelyzer.plugin.iCores.ui.ICoreFrame;
import corelyzer.util.FileUtility;
import corelyzer.util.PropertyListUtility;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

public class CacheManager implements ICacheManager {
    static CacheManager cacheMgr;
    
    static String sp = System.getProperty("file.separator");
    static String CONF_FILE = "icores_cache.plist";
    static String BAK_FILE  = CacheManager.CONF_FILE + "-bak";

    CRPreferences prefs;
    String cacheDir;
    boolean isInited = false;

    // FIXMe replace these 2 threads with a Executor Threadpool
    ImageLoadThread     imgLoadThread;
    DownloadThread      downloadThread;

    // background thread for refresh feeds
    CacheRefreshThread  refreshThread;

    // FIXME need a FIXED size hashtable for recycling disk entries
    Hashtable<String, CacheEntry> mapping;

    public static CacheManager initWithPreference(CRPreferences p) {
        cacheMgr = new CacheManager(p);
        return cacheMgr;
    }

    public static CacheManager getCacheManager() {
        return cacheMgr;
    }
       
    public CacheManager() {
        this(null);
    }

    public CacheManager(CRPreferences p) {
        prefs = (p == null) ? (new CRPreferences()) : p;

        cacheDir  = prefs.cache_Directory;
        cacheMgr = this;
    }

    public void init() {
        if(!isInited) {
            File confFile = new File(prefs.config_Directory + sp + CONF_FILE);

            if(confFile.exists()) {
                mapping = initCacheWithPersistentStorage(confFile);
            } else {
                mapping = new Hashtable<String, CacheEntry>();
            }

            System.out.println("---> [INFO] Init cacheManager has '" +
                    mapping.size() + "' entries.");

            // Init & start entries refresh thread
            refreshThread = new CacheRefreshThread(this);
            refreshThread.start();

            // FIXME replace these 2 threads with A Executor Task Thread Pool
            downloadThread = new DownloadThread(this);
            downloadThread.start();

            imgLoadThread = new ImageLoadThread(this);
            imgLoadThread.start();
            //
            
            isInited = true;
        }
    }

    public void finish() {
        if(!isInited) return;

        // Stop threads
        if(refreshThread != null) {
            refreshThread.setKeepRunning(false);
            refreshThread.interrupt();
            refreshThread = null;
        }

        if(imgLoadThread != null) {
            imgLoadThread.setKeepRunning(false);
            imgLoadThread.interrupt();
            refreshThread = null;
        }

        if(downloadThread != null) {
            downloadThread.setKeepRunning(false);
            downloadThread.interrupt();
            refreshThread = null;
        }

        // Write things out to cacheMgr persistent file storage
        File aFile = new File(prefs.config_Directory + sp + CONF_FILE);

        if(aFile.exists()) {
            // make a backup in case something goes wrong in writing out
            String bakFile = prefs.config_Directory + sp + BAK_FILE;
            FileUtility.copyFile(aFile.getAbsolutePath(), bakFile);
        }
        
        System.out.println("---> " + (new Date()) + " Writing out to: " +
                aFile.getAbsolutePath());
        
        try {
            FileWriter fw = new FileWriter(aFile);
            String aString = PropertyListUtility.defaultPlistHeader;
            aString += PropertyListUtility.defaultDictHeader;

            fw.write(aString, 0, aString.length());

            for (Map.Entry<String, CacheEntry> entry : mapping.entrySet()) {
                System.out.println("---> Writing " + entry.getKey());

                aString = "<key>" + entry.getKey() + "</key>\n";
                aString += PropertyListUtility.generateDictStringFromHashtable(
                                entry.getValue().getDictionary());
                
                fw.write(aString, 0, aString.length());
            }

            aString = PropertyListUtility.defaultDictFooter;
            aString += PropertyListUtility.defaultPlistFooter;

            fw.write(aString, 0, aString.length());
            fw.close();
        } catch (Exception e) {
            System.err.println("---> [EXCEPTION] Writing file '" + aFile
                    + "' failed");
        }
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }
    
    public void add(String url, CacheEntry entry) {
        mapping.put(url, entry);
    }

    public void remove(String url) {
        if(!mapping.containsKey(url)) return;

        CacheEntry anEntry = mapping.get(url);
        String local = anEntry.getLocal();
        String type  = anEntry.getType();

        // Remove file on the harddrive
        String filePath = prefs.cache_Directory + sp + type + sp + local;
        File aFile = new File(filePath);
        if(aFile.exists()) {
            boolean isDeleted = aFile.delete();
            System.out.println("---> [INFO] Deletion result: '" + isDeleted +
                    "'");
        }

        mapping.remove(url);
    }

    public void set(String url, CacheEntry entry) // for update
    {
        mapping.put(url, entry);
    }

    public CacheEntry fetch(String url) {
        return mapping.get(url);
    }

    public boolean hasItem(String url) {
        return mapping.containsKey(url);
    }

    public ImageLoadThread getImgLoadThread() {
        return imgLoadThread;
    }

    public void setImgLoadThread(ImageLoadThread imgLoadThread) {
        this.imgLoadThread = imgLoadThread;
    }

    public DownloadThread getDownloadThread() {
        return downloadThread;
    }

    public void setDownloadThread(DownloadThread downloadThread) {
        this.downloadThread = downloadThread;
    }

    public CacheRefreshThread getRefreshThread() {
        return refreshThread;
    }

    public void setRefreshThread(CacheRefreshThread refreshThread) {
        this.refreshThread = refreshThread;
    }

    private Hashtable<String, CacheEntry> initCacheWithPersistentStorage(
            File confFile)
    {
        if( !confFile.exists() || (confFile.length() == 0) ) {
            File bakConfFile = new File(prefs.config_Directory + sp + BAK_FILE);

            System.out.println(
                    "---> ConfFile doesn't exist or be size 0, seek backup "
                            + bakConfFile.getAbsolutePath());

            if(bakConfFile.exists()) {
                System.out.println("---> [INFO] Init with bakuped config file");
                confFile = bakConfFile;
            } else {
                return (new Hashtable<String, CacheEntry>());
            }
        }

        Hashtable<String, CacheEntry> aHash =
                new Hashtable<String, CacheEntry>();

        try {
            // Read back persistent storage is it exists
            System.out.println("---> Init cacheManager: parsing " +
                    confFile.getAbsolutePath());

            DOMParser parser = new DOMParser();
            parser.parse(confFile.getAbsolutePath());

            Document doc = parser.getDocument();
            Element docElement = doc.getDocumentElement();

            NodeList list = docElement.getChildNodes();
            Element topLevelDict = null;
            for(int i=0; i<list.getLength(); i++) {
                if( !(list.item(i) instanceof Element) ) continue;

                Element e = (Element) list.item(i);
                if(e.getTagName().equalsIgnoreCase("dict")) {
                    topLevelDict = e;
                    break;
                }
            }

            if(topLevelDict == null) return aHash;
            list = topLevelDict.getChildNodes();

            String theKey = null;
            for(int i=0; i<list.getLength(); i++) {
                if(!(list.item(i) instanceof Element)) {
                    continue;
                }

                Element e = (Element) list.item(i);
                String tagname  = e.getTagName();

                if(tagname.equalsIgnoreCase("key")) {
                    theKey = e.getTextContent();
                } else if(tagname.equalsIgnoreCase("dict")) {
                    // Create CacheEntry with the incoming dict
                    if(theKey == null || theKey.equalsIgnoreCase("")) {
                        System.out.println(
                               "---> No key before reaching dict? just ignore");
                        continue;
                    }

                    Hashtable<String, String> cacheHash =
                            PropertyListUtility.generateHashtableFromDictNode(e);

                    CacheEntry entry =
                            CacheEntry.initWithHashtable(cacheHash);

                    // FIXME what about CacheEntry's observer?
                    Runnable observer = ICoreFrame.getIcoreFrame().
                            getSubscriptionNode(entry.getRemote());
                    if(observer == null) {
                        System.out.println("=== WHAT!!! WHY? ===");
                    }
                    entry.setObserver(observer);

                    String localPath = cacheDir + sp + entry.getType() + sp +
                            entry.getLocal();
                    File localFile = new File(localPath);
                    if(localFile.exists()) {
                        entry.setReady(true);
                    } else {
                        System.out.println("---> entry is not ready");
                    }

                    aHash.put(theKey, entry);
                } else {
                    System.out.println(
                            "---> Not match <key><dict> ? but continue");
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("---> [ERROR] Cannot find file '" + confFile +
                    "'");
        } catch (IOException e) {
            System.err.println("---> [ERROR] Cannot read file '" + confFile +
                    "'");
        } catch (SAXException e) {
            System.err.println("---> [ERROR] SAXException: '" + confFile + "'");
        }

        return aHash;
    }

    public CRPreferences getPrefs() {
        return prefs;
    }

    public static void main(String[] args) {
        // Some standalone testing...
        
        // start cacheManager
        ICacheManager mgr = new CacheManager();
        mgr.init();
        
        String [] feeds = {"http://www.corewall.org/",
                           "http://www.evl.uic.edu/",
                           "http://www.andrill.org/",
                           "http://corewalldb.evl.uic.edu/"
                          };

        // insert few items
        for(String feed : feeds) {
            CacheEntry entry = new CacheEntry(feed, "feed"); // FIXME
            mgr.add(feed, entry);
            entry = mgr.fetch(feed);

            // show some status info
            System.out.println("----");
            System.out.println("remote: '" + entry.remote + "'");
            System.out.println("local: '" + entry.local + "'");
            System.out.println("createTime: '" + entry.createTime + "'");
            System.out.println("lastUpdateTime: '" + entry.lastUpdateTime + "'");
            System.out.println("size: '" + entry.size + "'");
            System.out.println("----");
        }

        try {
            // keep cachemanager alive for awhile
            Thread.sleep(5 * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // stop cacheManager
        mgr.finish();
    }
}
