package corelyzer.plugins.expeditionmanager.data;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import corelyzer.services.cache.CacheService;

/**
 * An implementation of the DataStore class for a zip file.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ZipFileDataStore extends AbstractDataStore {

    /**
     * {@inheritDoc}
     */
    public List<Resource> getContents() {
        // our contents
        List<Resource> contents = new ArrayList<Resource>();

        // convert the URL to a File
        CacheService service = CacheService.getService();
        try {
            File file = service.put(getPath()).get();

            // open the file as a zip file
            ZipFile zip = new ZipFile(file);

            // list the entries
            for (ZipEntry entry : Collections.list(zip.entries())) {
                if (!entry.isDirectory()) {
                    URL url = new URL("jar:file:" + file.getAbsolutePath()
                            + "!/" + entry.getName());
                    ;
                    if (url != null) {
                        contents.add(new Resource(url, null));
                    }
                }
            }
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return contents;
    }
}
