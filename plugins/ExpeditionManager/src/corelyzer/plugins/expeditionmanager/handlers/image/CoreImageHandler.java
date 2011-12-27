package corelyzer.plugins.expeditionmanager.handlers.image;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import corelyzer.lib.datamodel.CoreImage;
import corelyzer.plugins.expeditionmanager.handlers.AbstractDataHandler;
import corelyzer.plugins.expeditionmanager.handlers.DepthRange;

/**
 * Handles data stores of CoreImages.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreImageHandler extends AbstractDataHandler {
    private final Map<URL, CoreImage> loaded;

    /**
     * Create a new CoreImageHandler.
     */
    public CoreImageHandler() {
        loaded = new ConcurrentHashMap<URL, CoreImage>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void disable() {
        // remove all of the loaded images
        removeInvalid(new DepthRange(-1.0, -1.0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void enable() {
        // do nothing
    }

    private void removeInvalid(final DepthRange r) {
        for (Iterator<Entry<URL, CoreImage>> entryIter = loaded.entrySet()
                .iterator(); entryIter.hasNext();) {
            CoreImage img = entryIter.next().getValue();
            if (!r.intersects(img.getDepth(),
                    (img.getDepth() + img.getLength()))) {
                entryIter.remove();
                getContext().submitIOJob(new RemoveCoreImage(img));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void render(final DepthRange range) {
        // remove any images not in the current depth range
        removeInvalid(range);

        // now parse the core images and display any new ones in the range
        getContext().submitIOJob(new ParseCoreImages(this, loaded));
    }
}
