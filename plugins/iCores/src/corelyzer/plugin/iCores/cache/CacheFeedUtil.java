package corelyzer.plugin.iCores.cache;

import com.sun.syndication.feed.synd.SyndFeed;
import corelyzer.plugin.iCores.ui.tree.FeedTreeNode;
import corelyzer.util.ROMEUtils;

import java.io.File;
import java.net.MalformedURLException;

public class CacheFeedUtil {

    // TODO
    public static SyndFeed readFeed(ICacheManager cacheMgr,
                                    FeedTreeNode aNode) {
        String url = aNode.getUrl();

        // Local directly from local url
        if(url.toLowerCase().startsWith("file:")) {
            return ROMEUtils.readFeed(url);
        }

        if(cacheMgr == null) {
            System.out.println("---> [INFO] CacheManager is null, so null returned.");
            return null;
        }

        CacheEntry entry;
        if(cacheMgr.hasItem(url)) {
            entry = cacheMgr.fetch(url);
        } else {
            entry = new CacheEntry(url, "feed");

            // download right away
            if(!entry.doDownload(cacheMgr.getCacheDir())) {
                if(cacheMgr instanceof CacheManager) {
                    CacheManager mgr = (CacheManager) cacheMgr;
                    mgr.getDownloadThread().add(entry);
                }
            }

            cacheMgr.add(url, entry);
        }

        // FIXME
        aNode.setCacheEntry(entry);
        entry.setObserver(aNode);
        
        String sp = System.getProperty("file.separator");
        String dir  = cacheMgr.getCacheDir();
        String file = entry.getLocal();
        String type = entry.getType();
        File localFile = new File(dir + sp + type + sp + file);

        try {
            String localURL = localFile.toURL().toString();
            System.out.println("---> [INFO] Return feed with local url '" + localURL + "'");
            return ROMEUtils.readFeed(localURL);
        } catch (MalformedURLException e) {
            System.out.println("---> [EXCEPTION] MalformedURL: '" + url + "'");
            return null;
        }
    }


    public static SyndFeed readFeed(ICacheManager cacheMgr, String url) {
        // Local directly from local url
        if (url.toLowerCase().startsWith("file:")) {
            return ROMEUtils.readFeed(url);
        }
		
		if (cacheMgr == null) {
            System.out.println("---> [INFO] CacheManager is null, so null returned.");
            return null;
        }

        //CacheEntry entry = cacheMgr.fetch(url); // 12/8/2011 brg
        CacheEntry entry;
        if(cacheMgr.hasItem(url)) {
            entry = cacheMgr.fetch(url);
        } else {
            entry = new CacheEntry(url, "feed");

            // download right away
            if(!entry.doDownload(cacheMgr.getCacheDir())) {
                if(cacheMgr instanceof CacheManager) {
                    CacheManager mgr = (CacheManager) cacheMgr;
                    mgr.getDownloadThread().add(entry);
                }
            }

            cacheMgr.add(url, entry);
        }

        //CacheEntry entry; // 12/8/2011 brg - duplicated above
        //if(cacheMgr.hasItem(url)) {
        //    entry = cacheMgr.fetch(url);
        //} else {
        //    entry = new CacheEntry(url, "feed");
		//
            // download right away
        //    if(!entry.doDownload(cacheMgr.getCacheDir())) {
        //        if(cacheMgr instanceof CacheManager) {
        //            CacheManager mgr = (CacheManager) cacheMgr;
        //            mgr.getDownloadThread().add(entry);
        //        }
        //    }
		//
        //    cacheMgr.add(url, entry);
        //}

        String sp = System.getProperty("file.separator");
        String dir  = cacheMgr.getCacheDir();
        String file = entry.getLocal();
        String type = entry.getType();
        File localFile = new File(dir + sp + type + sp + file);

        try {
            String localURL = localFile.toURL().toString();
            System.out.println("---> [INFO] Return feed with local url '" + localURL + "'");
            return ROMEUtils.readFeed(localURL);
        } catch (MalformedURLException e) {
            System.out.println("---> [EXCEPTION] MalformedURL: '" + url + "'");
            return null;
        }

    }
    
}
