package corelyzer.plugins.expeditionmanager.data;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import corelyzer.plugins.expeditionmanager.util.FileUtils;

/**
 * An implementation of the DataStore class for files.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class FileDataStore extends AbstractDataStore {

    private void addFiles(final URL url, final List<Resource> contents) {
        // convert the URL to a file
        File file = FileUtils.getFile(url);
        if (file == null) {
            return;
        }

        // add the file or the contents of the directory
        if (file.isFile()) {
            contents.add(new Resource(FileUtils.getURL(file), file));
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        contents.add(new Resource(FileUtils.getURL(f), f));
                    } else {
                        addFiles(FileUtils.getURL(f), contents);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Resource> getContents() {
        List<Resource> contents = new ArrayList<Resource>();
        addFiles(getPath(), contents);
        return contents;
    }
}
