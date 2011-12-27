package corelyzer.plugin.psicat.scheme;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the ISchemeEntry interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class SchemeEntry {
    private String scheme = null;
    private String scope = null;
    private String[] keywords = new String[0];
    private final transient Map<String, String> map = new HashMap<String, String>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SchemeEntry other = (SchemeEntry) obj;
        if (!Arrays.equals(keywords, other.keywords)) {
            return false;
        }
        if (scheme == null) {
            if (other.scheme != null) {
                return false;
            }
        } else if (!scheme.equals(other.scheme)) {
            return false;
        }
        if (scope == null) {
            if (other.scope != null) {
                return false;
            }
        } else if (!scope.equals(other.scope)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getKeywords() {
        return keywords;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getMap() {
        return map;
    }

    /**
     * {@inheritDoc}
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * {@inheritDoc}
     */
    public String getScope() {
        return scope;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + Arrays.hashCode(keywords);
        result = PRIME * result + ((scheme == null) ? 0 : scheme.hashCode());
        result = PRIME * result + ((scope == null) ? 0 : scope.hashCode());
        return result;
    }

    /**
     * Set the keywords.
     * 
     * @param keywords
     *            the keywords.
     */
    public void setKeywords(final String[] keywords) {
        this.keywords = keywords;
    }

    /**
     * Set the scheme.
     * 
     * @param scheme
     *            the scheme.
     */
    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    /**
     * Set the scope.
     * 
     * @param scope
     *            the scope.
     */
    public void setScope(final String scope) {
        this.scope = scope;
    }
}
