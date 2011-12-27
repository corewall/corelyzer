package corelyzer.plugin.psicat.scheme;

/**
 * Some various Scheme-related utility methods.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public final class SchemeUtils {

    /**
     * The name of the keywords property.
     */
    public static final String KEYWORDS_PROP = "keywords"; //$NON-NLS-1$

    /**
     * Adds a keyword to a list of keywords if it isn't already in the list.
     * 
     * @param keywords
     *            the list of keywords.
     * @param keyword
     *            the keyword to add.
     * @return the list of keywords with the new keyword add.
     */
    public static String[] addKeyword(final String[] keywords,
            final String keyword) {
        // add the keyword if it didn't already exist
        if (!SchemeUtils.containsKeyword(keywords, keyword)) {
            final String[] newKeywords = new String[keywords.length + 1];
            for (int i = 0; i < keywords.length; i++) {
                newKeywords[i] = keywords[i];
            }
            newKeywords[keywords.length] = keyword;
            return newKeywords;
        } else {
            return keywords;
        }
    }

    /**
     * Converts an array of keywords to a String.
     * 
     * @param keywords
     *            the keywords.
     * @return the String representation of the keywords.
     */
    public static String arrayToString(final String[] keywords) {
        final StringBuffer buffer = new StringBuffer();
        for (int i = 0; (keywords != null) && (i < keywords.length); i++) {
            if (i > 0) {
                buffer.append(',');
            }
            buffer.append(keywords[i]);
        }
        return buffer.toString();
    }

    /**
     * Checks whether a list of keywords contains a specific keyword.
     * 
     * @param keywords
     *            the list of keywords.
     * @param keyword
     *            the keyword.
     * @return true if the keyword is in the list, false otherwise.
     */
    public static boolean containsKeyword(final String[] keywords,
            final String keyword) {
        for (final String existing : keywords) {
            if (existing.equalsIgnoreCase(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares two sets of keywords to see if they match.
     * 
     * @param keywords1
     *            keyword set one.
     * @param keywords2
     *            keyword set two.
     * @return true if they match, false otherwise.
     */
    public static boolean matches(final String[] keywords1,
            final String[] keywords2) {
        // didn't match if the keyword arrays aren't the same
        if (keywords1.length != keywords2.length) {
            return false;
        }

        // check all of the keywords
        for (final String keyword : keywords1) {
            boolean found = false;
            for (final String other : keywords2) {
                found |= keyword.trim().equalsIgnoreCase(other.trim());
            }

            // bail early if a keyword wasn't found
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes a keyword from a list of keywords.
     * 
     * @param keywords
     *            the list of keywords.
     * @param keyword
     *            the keyword to remove.
     * @return the list of keywords with the keyword removed.
     */
    public static String[] removeKeyword(final String[] keywords,
            final String keyword) {
        if (SchemeUtils.containsKeyword(keywords, keyword)) {
            final String[] newKeywords = new String[keywords.length - 1];
            int i = 0;
            for (final String foo : keywords) {
                if (!foo.equalsIgnoreCase(keyword)) {
                    newKeywords[i] = foo;
                    i++;
                }
            }
            return newKeywords;
        } else {
            return keywords;
        }
    }

    /**
     * Converts a String of keywords to an array.
     * 
     * @param keywords
     *            the keywords.
     * @return the array of keywords.
     */
    public static String[] stringToArray(final String keywords) {
        if (keywords == null) {
            return new String[0];
        }
        final String[] split = keywords.split(","); //$NON-NLS-1$
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].trim();
        }
        return split;
    }

    private SchemeUtils() {
        // this class is not intended to be instantiated
    }
}
