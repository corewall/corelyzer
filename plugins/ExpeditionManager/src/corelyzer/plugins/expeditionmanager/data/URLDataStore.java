package corelyzer.plugins.expeditionmanager.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple data store that just returns a URL.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class URLDataStore extends AbstractDataStore {

    /**
     * {@inheritDoc}
     */
    public List<Resource> getContents() {
        List<Resource> contents = new ArrayList<Resource>(1);
        contents.add(new Resource(getPath(), getPath()));
        return contents;
    }
}
