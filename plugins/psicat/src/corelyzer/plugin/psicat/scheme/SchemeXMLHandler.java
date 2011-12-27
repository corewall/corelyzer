package corelyzer.plugin.psicat.scheme;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX XML handler for loading scheme XML data.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class SchemeXMLHandler extends DefaultHandler {
    public static final String SCHEME_TAG = "scheme"; //$NON-NLS-1$
    public static final String ENTRY_TAG = "entry"; //$NON-NLS-1$
    public static final String KEYWORDS_TAG = "keywords"; //$NON-NLS-1$
    public static final String PROPERTY_TAG = "property"; //$NON-NLS-1$
    public static final String NAME_ATTR = "name"; //$NON-NLS-1$
    public static final String SCOPE_ATTR = "scope"; //$NON-NLS-1$

    private transient final SchemeManager schemeManager;
    private transient final List<String> parsed;

    private transient SchemeEntry current = null;
    private transient String scheme;
    private transient String scope;
    private transient String currentProperty = null;
    private transient StringBuffer buffer;

    /**
     * Create a new SchemeXMLHandler.
     * 
     * @param schemeManager
     *            the scheme manager.
     */
    public SchemeXMLHandler(final SchemeManager schemeManager) {
        super();
        this.schemeManager = schemeManager;
        parsed = new ArrayList<String>();
        buffer = new StringBuffer();
    }

    /**
     * Create a new SchemeXMLHandler.
     * 
     * @param scheme
     *            the scheme.
     * @param scope
     *            the scope.
     * @param schemeManager
     *            the scheme manager.
     */
    public SchemeXMLHandler(final String scheme, final String scope,
            final SchemeManager schemeManager) {
        super();
        this.scheme = scheme;
        this.scope = scope;
        this.schemeManager = schemeManager;
        parsed = new ArrayList<String>();
        parsed.add(scheme + ":" + scope);
        buffer = new StringBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        final StringBuffer temp = new StringBuffer();
        for (int i = start; i < start + length; i++) {
            temp.append(ch[i]);
        }
        buffer.append(temp.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        if (SchemeXMLHandler.ENTRY_TAG.equals(qName) && (current != null)) {
            schemeManager.addSchemeEntry(scheme, scope, current);
            current = null;
        } else if (SchemeXMLHandler.KEYWORDS_TAG.equals(qName)
                && (current != null)) {
            final String[] split = buffer.toString().split(","); //$NON-NLS-1$
            for (int i = 0; i < split.length; i++) {
                split[i] = split[i].trim().toLowerCase();
            }
            current.setKeywords(split);
        } else if (SchemeXMLHandler.PROPERTY_TAG.equals(qName)
                && (current != null)) {
            current.getMap().put(currentProperty, buffer.toString());
        }
    }

    /**
     * Gets the list of parsed schemes and scopes.
     * 
     * @return the list of parsed schemes and scopes.
     */
    public String[] getParsed() {
        return parsed.toArray(new String[parsed.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
            throws SAXException {
        if (SchemeXMLHandler.SCHEME_TAG.equals(qName)) {
            scheme = attributes.getValue(SchemeXMLHandler.NAME_ATTR);
            scope = attributes.getValue(SchemeXMLHandler.SCOPE_ATTR);
            parsed.add(scheme + ":" + scope);
        } else if (SchemeXMLHandler.ENTRY_TAG.equals(qName)) {
            current = new SchemeEntry();
            current.setScheme(scheme);
            current.setScope(scope);
        } else if (SchemeXMLHandler.KEYWORDS_TAG.equals(qName)
                && (current != null)) {
            buffer = new StringBuffer();
        } else if (SchemeXMLHandler.PROPERTY_TAG.equals(qName)
                && (current != null)) {
            currentProperty = attributes.getValue(SchemeXMLHandler.NAME_ATTR);
            buffer = new StringBuffer();
        }
    }
}