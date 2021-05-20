package corelyzer.plugin.psicat.scheme;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import corelyzer.plugin.psicat.util.ResourceUtils;

/**
 * Manages schemes, such as lithology schemes.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class SchemeManager {
    private static SchemeManager SINGLETON = null;

    /**
     * Gets the shared instance of the SchemeManager.
     * 
     * @return the shared instance.
     */
    public static SchemeManager getInstance() {
        if (SINGLETON == null) {
            SINGLETON = new SchemeManager();
        }
        return SINGLETON;
    }

    private final transient Map<String, Map<String, List<SchemeEntry>>> schemes;
    private final transient Map<String, Map<String, SchemeEntry>> cache;
    private final transient Map<String, URL> sources;

    /**
     * Create a new SchemeManager.
     */
    private SchemeManager() {
        schemes = new HashMap<String, Map<String, List<SchemeEntry>>>();
        cache = new HashMap<String, Map<String, SchemeEntry>>();
        sources = new HashMap<String, URL>();

        initSchemes();
    }

    /**
     * {@inheritDoc}
     */
    public void addSchemeEntry(final String scheme, final String scope,
            final SchemeEntry entry) {
        // create the scheme if it didn't exist
        if (!schemes.containsKey(scheme)) {
            schemes.put(scheme, new HashMap<String, List<SchemeEntry>>());
        }

        // create the scope if it didn't exist
        if (!schemes.get(scheme).containsKey(scope)) {
            schemes.get(scheme).put(scope, new ArrayList<SchemeEntry>());
        }

        // add the entry
        schemes.get(scheme).get(scope).add(entry);
    }

    /**
     * {@inheritDoc}
     */
    public SchemeEntry findSchemeEntry(final String scheme, final String scope,
            final String[] keywords) {
        if (schemes.containsKey(scheme)
                && schemes.get(scheme).containsKey(scope)) {
            // make sure our cache exists
            final String cacheKey = scheme + ":" + scope;
            if (!cache.containsKey(cacheKey)) {
                cache.put(cacheKey, new HashMap<String, SchemeEntry>());
            }

            // prepare our keywords and cache key
            final StringBuffer key = new StringBuffer();
            for (int i = 0; i < keywords.length; i++) {
                keywords[i] = keywords[i].trim().toLowerCase();
                key.append(keywords[i] + ":");
            }

            // if it's cached, return it
            if (cache.get(cacheKey).containsKey(key.toString())) {
                return cache.get(cacheKey).get(key.toString());
            } else {
                SchemeEntry match = null;
                final List<SchemeEntry> entries = schemes.get(scheme)
                        .get(scope);
                double score = 0.0;
                for (final SchemeEntry entry : entries) {
                    final double newScore = score(entry, keywords);
                    if (newScore > score) {
                        match = entry;
                        score = newScore;
                    }
                }
                cache.get(cacheKey).put(key.toString(), match);
                return match;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public SchemeEntry[] getSchemeMembers(final String scheme,
            final String scope) {
        if (schemes.containsKey(scheme)
                && schemes.get(scheme).containsKey(scope)) {
            final List<SchemeEntry> entries = schemes.get(scheme).get(scope);
            return entries.toArray(new SchemeEntry[entries.size()]);
        }
        return new SchemeEntry[0];
    }

    /**
     * {@inheritDoc}
     */
    public String[] getScopesForScheme(final String scheme) {
        if (schemes.containsKey(scheme)) {
            final Map<String, List<SchemeEntry>> scopeMap = schemes.get(scheme);
            return scopeMap.keySet().toArray(new String[scopeMap.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Gets the source of the specified scheme and scope.
     * 
     * @param scheme
     *            the scheme.
     * @param scope
     *            the scope.
     * @return
     */
    public URL getSource(final String scheme, final String scope) {
        return sources.get(scheme + ":" + scope);
    }

    private void initSchemes() {
        // discover and load any schemes
        for (URL url : ResourceUtils.getResources("scheme.xml")) {
            SchemeXMLHandler handler = new SchemeXMLHandler(this);
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            try {
                factory.newSAXParser().parse(url.openStream(), handler);
                for (String key : handler.getParsed()) {
                    System.out.println("key = " + key);
                    sources.put(key, url);
                }
            } catch (final SAXException e) {
                System.err.println("Error loading scheme " + url);
                e.printStackTrace(System.err);
            } catch (final IOException e) {
                System.err.println("Error loading scheme " + url);
                e.printStackTrace(System.err);
            } catch (final ParserConfigurationException e) {
                System.err.println("Error loading scheme " + url);
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeSchemeEntry(final String scheme, final String scope,
            final SchemeEntry entry) {
        if (schemes.containsKey(scheme)
                && schemes.get(scheme).containsKey(scope)) {
            final List<SchemeEntry> entries = schemes.get(scheme).get(scope);
            for (final Iterator<SchemeEntry> entryIter = entries.iterator(); entryIter
                    .hasNext();) {
                if (SchemeUtils.matches(entry.getKeywords(), entryIter.next()
                        .getKeywords())) {
                    entryIter.remove();
                }
            }
        }
    }

    private double score(final SchemeEntry entry, final String[] keywords) {
        // need to implement some caching to speed this up
        double score = 0.0;
        for (final String keyword : keywords) {
            for (final String other : entry.getKeywords()) { // already lower
                // case
                if (keyword.equals(other)) {
                    score += 1.0; // if it is an exact match, it gets +1.0
                    break;
                }
            }
        }

        // if it was an exact match, give a bonus
        if ((keywords.length == entry.getKeywords().length)
                && (score == keywords.length)) {
            score += 1.0;
        }
        return score;
    }
}
