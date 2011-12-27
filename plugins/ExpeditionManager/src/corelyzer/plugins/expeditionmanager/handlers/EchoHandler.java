package corelyzer.plugins.expeditionmanager.handlers;

import corelyzer.plugins.expeditionmanager.data.Resource;

/**
 * Simply echos the contents of a particular data store for debugging purposes.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class EchoHandler extends AbstractDataHandler {

    @Override
    protected void disable() {
        // do nothing
    }

    @Override
    protected void enable() {
        // do nothing
    }

    @Override
    protected void render(final DepthRange range) {
        System.out.println("Contents of [" + getDataStore().getName() + "]:");
        for (Resource resource : getDataStore().getContents()) {
            System.out.println("\t" + resource.getURL() + " ["
                    + resource.getContent() + "]");
        }
    }
}
