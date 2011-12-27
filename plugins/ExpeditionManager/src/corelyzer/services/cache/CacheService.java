package corelyzer.services.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import corelyzer.plugins.expeditionmanager.util.FileUtils;

/**
 * A service for caching URLs locally.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CacheService {
    private class IdentityFuture<T> implements Future<T> {
        private T t;

        public IdentityFuture(final T t) {
            this.t = t;
        }

        public boolean cancel(final boolean mayInterruptIfRunning) {
            return false;
        }

        public T get() throws InterruptedException, ExecutionException {
            return t;
        }

        public T get(final long timeout, final TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {
            return t;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return true;
        }
    }

    private static CacheService SINGLETON = null;
    private static String CACHE_DIR = ".ExpeditionManager";

    /**
     * Gets the shared instance of the cache service.
     * 
     * @return the shared instance of the cache service.
     */
    public static CacheService getService() {
        if (SINGLETON == null) {
            SINGLETON = new CacheService();
        }
        return SINGLETON;
    }

    private final File cacheDir;
    private final Map<URL, Future<File>> cacheMap;
    private final ExecutorService executor;

    private CacheService() {
        // set up our cache directory
        File home = new File(System.getProperty("user.home"));
        cacheDir = new File(home, CACHE_DIR);
        cacheDir.mkdirs();

        cleanupTemporaryFiles();

        // create our cache map
        cacheMap = new HashMap<URL, Future<File>>();

        // create our executor service
        executor = Executors.newCachedThreadPool();
    }

    private void cleanupTemporaryFiles() {
        // delete our temp files
        File[] files = cacheDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".tmp");
            }
        });

        // delete temporary files
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    /**
     * Gets a URL from the cache.
     * 
     * @param url
     *            the URL.
     * @return a Future with a handle to the cached file or null if not in the
     *         cache.
     */
    public synchronized Future<File> get(final URL url) {
        // check if in our map
        if (cacheMap.containsKey(url)) {
            return cacheMap.get(url);
        }

        // check if the file exists
        File cacheFile = getCacheFile(url);
        if (cacheFile.exists()) {
            IdentityFuture<File> i = new IdentityFuture<File>(cacheFile);
            cacheMap.put(url, i);
            return i;
        } else {
            // not in the cache
            return null;
        }
    }

    private File getCacheFile(final URL url) {
        // check if already a local file
        File f = FileUtils.getFile(url);
        if (f != null) {
            return f; // no need to cache local files
        }

        // not a local file, so calculate the cache file
        String hash;
        MessageDigest md5;
        try {
            // try hashing the filename to avoid invalid characters and prevent
            // conflicts
            md5 = MessageDigest.getInstance("MD5");
            md5.update(url.toExternalForm().getBytes());
            hash = new BigInteger(1, md5.digest()).toString(16)
                    + url.getFile().substring(url.getFile().lastIndexOf('.'));
        } catch (final NoSuchAlgorithmException e) {
            // no hash algorithm, so just use the filename
            hash = url.getFile();
        }
        return new File(cacheDir, hash);
    }

    /**
     * Determine if the specified URL is in the cache.
     * 
     * @param url
     *            the URL.
     * @return true if already cached, false otherwise.
     */
    public synchronized boolean isCached(final URL url) {
        return getCacheFile(url).exists();
    }

    /**
     * Puts the specified URL in the cache and returns a Future with a handle to
     * the cached file.
     * 
     * @param url
     *            the URL.
     * @return a Future with a handle to the cached file.
     */
    public synchronized Future<File> put(final URL url) {
        // remove the url from the cache and cancel the future if it is running
        Future<File> f = cacheMap.remove(url);
        if (f != null) {
            f.cancel(true);
        }

        // create our future
        Future<File> future = executor.submit(new Callable<File>() {
            public File call() throws Exception {
                File cachedFile = getCacheFile(url);
                File tempFile = new File(cachedFile.getAbsolutePath() + ".tmp");
                FileUtils
                        .copy(url.openStream(), new FileOutputStream(tempFile));
                FileUtils.moveFile(tempFile, cachedFile);
                return cachedFile;
            }
        });

        // cache it and return it
        cacheMap.put(url, future);
        return future;
    }
}
