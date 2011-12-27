package corelyzer.plugins.expeditionmanager.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import corelyzer.plugins.expeditionmanager.util.FileUtils;

/**
 * An implementation of the IDataStore interface that reads an index file of
 * URLs.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class IndexFileDataStore extends AbstractDataStore {

    /**
     * {@inheritDoc}
     */
    public List<Resource> getContents() {
        List<Resource> contents = new ArrayList<Resource>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getPath()
                    .openStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                try {
                    if (line.indexOf(':') > -1) {
                        contents.add(new Resource(new URL(line), null));
                    } else {
                        contents.add(new Resource(new URL(getPath(), line),
                                null));
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtils.silentClose(br);
        }
        return contents;
    }
}
