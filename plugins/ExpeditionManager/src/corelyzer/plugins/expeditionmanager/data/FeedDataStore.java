package corelyzer.plugins.expeditionmanager.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * An implementation of the the IDataStore interface for Atom/RSS feeds.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class FeedDataStore extends AbstractDataStore {

    /**
     * {@inheritDoc}
     */
    public List<Resource> getContents() {
        List<Resource> contents = new ArrayList<Resource>();

        // parse our feed
        SyndFeedInput input = new SyndFeedInput();
        try {
            SyndFeed feed = input.build(new InputStreamReader(getPath()
                    .openStream()));

            // return the URLs of the entries as the contents
            for (SyndEntry entry : (List<SyndEntry>) feed.getEntries()) {
                String link = entry.getLink();
                try {
                    if (link != null) {
                        contents.add(new Resource(new URL(link), entry));
                    }
                } catch (MalformedURLException mue) {
                    mue.printStackTrace();
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (FeedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contents;
    }
}
