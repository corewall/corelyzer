package corelyzer.plugin.psicat.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import corelyzer.plugin.psicat.scheme.SchemeManager;

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
            File f = new File(".");
            System.out.println("Current dir: " + f.getAbsolutePath());
            File file = new File(name);
            System.out.println("Looking for file " + name);
            if (file.exists()) {
                System.out.println("Exists!");
                urls.add(new URL("file:" + file.getAbsolutePath()));
            } else {
                System.out.println("File doesn't exist, sad.");
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

    public static void main(final String[] args) {
        SchemeManager.getInstance();
    }

    private ResourceUtils() {
        // not intended to be instantiated
    }
}
