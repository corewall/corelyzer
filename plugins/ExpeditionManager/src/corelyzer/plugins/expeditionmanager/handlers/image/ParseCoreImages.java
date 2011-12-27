package corelyzer.plugins.expeditionmanager.handlers.image;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.sun.syndication.feed.module.core.CoreModule;
import com.sun.syndication.feed.module.image.ImageModule;
import com.sun.syndication.feed.synd.SyndEntry;

import corelyzer.helper.SceneGraph;
import corelyzer.lib.datamodel.CoreImage;
import corelyzer.lib.datamodel.CoreImageConfiguration;
import corelyzer.lib.datamodel.CoreImageDirectory;
import corelyzer.plugins.expeditionmanager.data.Resource;
import corelyzer.plugins.expeditionmanager.handlers.DepthRange;
import corelyzer.plugins.expeditionmanager.handlers.IDataHandler;
import corelyzer.plugins.expeditionmanager.handlers.Job;
import corelyzer.plugins.expeditionmanager.util.FileUtils;
import corelyzer.services.cache.CacheService;

/**
 * Parse CoreImages from a list of Resources.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ParseCoreImages extends Job {
    private final Map<URL, CoreImage> loaded;
    private final IDataHandler dataHandler;
    private final Map<File, CoreImageConfiguration> configCache;

    /**
     * Create a new ParseCoreImages job.
     */
    public ParseCoreImages(final IDataHandler dataHandler,
            final Map<URL, CoreImage> loaded) {
        super("Parse core images...");
        this.dataHandler = dataHandler;
        this.loaded = loaded;
        configCache = new HashMap<File, CoreImageConfiguration>();
    }

    /**
     * Parse a CoreImage from a feed entry.
     * 
     * @param entry
     *            the entry.
     * @return a CoreImage or null.
     */
    private CoreImage coerceImageFromFeedEntry(final SyndEntry entry) {
        // get our modules
        CoreModule coreModule = (CoreModule) entry
                .getModule(CoreModule.CORE_URI);
        ImageModule imageModule = (ImageModule) entry
                .getModule(ImageModule.IMAGE_URI);
        CoreImageConfiguration config = new CoreImageConfiguration();
        config.load(dataHandler.getProperties());

        // create a new CoreImage if both modules are present
        CoreImage image = null;
        if ((coreModule != null) && (imageModule != null)) {
            try {
                image = new CoreImage(new URL(entry.getLink()), coreModule
                        .getDepth(), coreModule.getLength(), imageModule
                        .getDPIX(), imageModule.getDPIY(), imageModule
                        .getOrientation(), config);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return image;
    }

    /**
     * Coerce a CoreImage from a file.
     * 
     * @param content
     *            the File.
     * @return a CoreImage or null.
     */
    private CoreImage coerceImageFromFile(final File content) {
        return CoreImageDirectory.parseImage(content, getConfig(content));
    }

    private CoreImage coerceImageFromResource(final Resource resource) {
        Object content = resource.getContent();
        if ((content != null) && (content instanceof CoreImage)) {
            return (CoreImage) content;
        } else if ((content != null) && (content instanceof SyndEntry)) {
            return coerceImageFromFeedEntry((SyndEntry) content);
        } else if ((content != null) && (content instanceof File)) {
            return coerceImageFromFile((File) content);
        } else {
            return coerceImageFromURL(resource.getURL());
        }
    }

    /**
     * Coerce a CoreImage from a URL. This route is *very* slow since we need to
     * cache the URL contents locally before we can try to coerce a CoreImage
     * from it.
     * 
     * @param url
     *            the URL.
     * @return a CoreImage or null.
     */
    private CoreImage coerceImageFromURL(final URL url) {
        // check if this can be converted to a File
        File file = FileUtils.getFile(url);
        if (file != null) {
            return coerceImageFromFile(file);
        }

        // create our core image configuration
        CoreImageConfiguration config = new CoreImageConfiguration();
        config.load(dataHandler.getProperties());

        // get our cache service
        CacheService cache = CacheService.getService();
        CoreImage result = null;
        try {
            // parse a core image from the cached content
            if (cache.isCached(url)) {
                result = CoreImageDirectory.parseImage(cache.get(url).get(),
                        config);
            } else {
                result = CoreImageDirectory.parseImage(cache.put(url).get(),
                        config);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Parse the core images.
     */
    @Override
    protected void execute() {
        DepthRange range = dataHandler.getVisibleRange();

        // update the scene to show the visible range
        float rangeWidth = (float) (range.getBottom() - range.getTop())
                * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0);
        float screenWidth = SceneGraph.getCanvasWidth(0);

        // show range
        SceneGraph.lock();
        SceneGraph.scaleScene(rangeWidth / screenWidth);
        SceneGraph
                .positionScene(
                        (float) (range.getTop() + (range.getBottom() - range
                                .getTop()) / 2)
                                * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0),
                        0);
        SceneGraph.unlock();

        // start adding resources
        for (Resource resource : dataHandler.getDataStore().getContents()) {
            CoreImage ci = coerceImageFromResource(resource);
            if ((ci != null)
                    && range.intersects(ci.getDepth(), ci.getDepth()
                            + ci.getLength()) && !loaded.containsValue(ci)) {
                // add it to our loaded list
                loaded.put(resource.getURL(), ci);

                // fire a loading job
                dataHandler.getContext().submitIOJob(new LoadCoreImage(ci));
            }
        }
    }

    private CoreImageConfiguration getConfig(final File file) {
        File parent = file.getParentFile();
        if (configCache.containsKey(parent)) {
            return configCache.get(parent);
        } else {
            CoreImageDirectory dir = new CoreImageDirectory(parent);
            CoreImageConfiguration config = dir.getConfig();
            config.load(dataHandler.getProperties());
            configCache.put(parent, config);
            return config;
        }
    }

    /**
     * System job.
     */
    @Override
    public double getDepth() {
        return SYSTEM_JOB;
    }
}
