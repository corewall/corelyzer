package corelyzer.plugins.expeditionmanager.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains some utility methods for discovering resources.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ResourceUtils {
    /**
     * Find the specified resource.
     * 
     * @param name
     *            the name of the resource.
     * 
     * @return the list of URL.
     */
    public static List<URL> getResources(final String name) {
        List<URL> urls = new ArrayList<URL>();

        try {
            // check if it is a file
            File file = new File(name);
            if (file.exists()) {
                urls.add(new URL("file:" + file.getAbsolutePath()));
            }

            // check if we can get it from our classloader
            urls.addAll(Collections.list(ResourceUtils.class.getClassLoader()
                    .getResources(name)));
        } catch (MalformedURLException mue) {
            // do nothing
        } catch (IOException e) {
            // do nothing
        }

        return urls;
    }

    private ResourceUtils() {
        // not intended to be instantiated
    }
}
