package corelyzer.plugins.expeditionmanager.handlers.dataset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * A very simple CSV reader.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CSVReader {

    /** The default separator to use if none is supplied to the constructor. */
    public static final char DEFAULT_SEPARATOR = ',';

    /**
     * The default quote character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    /**
     * The default line to start reading.
     */
    public static final int DEFAULT_SKIP_LINES = 0;

    /**
     * The default full line comment.
     */
    public static final String DEFAULT_FULL_LINE_COMMENT = "//"; //$NON-NLS-1$

    /**
     * The default start comment token.
     */
    public static final String DEFAULT_START_COMMENT = "/*"; //$NON-NLS-1$

    /**
     * The default end comment
     */
    public static final String DEFAULT_END_COMMENT = "*/"; //$NON-NLS-1$

    private BufferedReader br;
    private boolean hasNext = true;
    private char separator;
    private char quotechar;
    private int skipLines;
    private String fullLineComment;
    private String startComment;
    private String endComment;
    private boolean linesSkiped;

    /**
     * Constructs CSVReader using a comma for the separator.
     * 
     * @param reader
     *            the reader to an underlying CSV source.
     */
    public CSVReader(final Reader reader) {
        this(reader, CSVReader.DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVReader with supplied separator.
     * 
     * @param reader
     *            the reader to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries.
     */
    public CSVReader(final Reader reader, final char separator) {
        this(reader, separator, CSVReader.DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVReader with supplied separator and quote char.
     * 
     * @param reader
     *            the reader to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     */
    public CSVReader(final Reader reader, final char separator,
            final char quotechar) {
        this(reader, separator, quotechar, CSVReader.DEFAULT_SKIP_LINES,
                CSVReader.DEFAULT_FULL_LINE_COMMENT,
                CSVReader.DEFAULT_START_COMMENT, CSVReader.DEFAULT_END_COMMENT);
    }

    /**
     * Constructs CSVReader with supplied separator and quote char.
     * 
     * @param reader
     *            the reader to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param line
     *            the line number to skip for start reading
     */
    public CSVReader(final Reader reader, final char separator,
            final char quotechar, final int line, final String fullLineComment,
            final String startComment, final String endComment) {
        br = new BufferedReader(reader);
        this.separator = separator;
        this.quotechar = quotechar;
        skipLines = line;
        this.fullLineComment = fullLineComment;
        this.startComment = startComment;
        this.endComment = endComment;
    }

    /**
     * Reads the entire file into a List with each element being a String[] of
     * tokens.
     * 
     * @return a List of String[], with each String[] representing a line of the
     *         file.
     * 
     * @throws IOException
     *             if bad things happen during the read
     */
    public List<String[]> allRows() {
        final List<String[]> allElements = new ArrayList<String[]>();
        while (hasNext) {
            final String[] nextLineAsTokens = nextRow();
            if (nextLineAsTokens != null) {
                allElements.add(nextLineAsTokens);
            }
        }
        return allElements;

    }

    /**
     * Closes the underlying reader.
     * 
     * @throws IOException
     *             if the close fails
     */
    public void close() {
        try {
            br.close();
        } catch (final IOException ioe) { /* do nothing */
        }
    }

    /**
     * Reads the next line from the file.
     * 
     * @return the next line from the file without trailing newline
     * @throws IOException
     *             if bad things happen during the read
     */
    private String getNextLine() throws IOException {
        if (!linesSkiped) {
            for (int i = 0; i < skipLines; i++) {
                br.readLine();
            }
            linesSkiped = true;
        }

        // remove comments
        boolean inComment = false;
        String nextLine = null;
        while (nextLine == null) {
            nextLine = br.readLine();

            // is it the last line
            if (nextLine == null) {
                hasNext = false;
                return null;
            } else if (nextLine.trim().length() == 0) {
                nextLine = null;
            } else if (nextLine.startsWith(fullLineComment)) {
                nextLine = null;
            } else if (nextLine.startsWith(startComment)
                    && (nextLine.indexOf(endComment) == -1)) { // multiline
                inComment = true;
                nextLine = null;
            } else if (inComment) {
                if (nextLine.indexOf(endComment) > -1) {
                    inComment = false;
                }
                nextLine = null;
            }
        }
        return nextLine;
    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     * 
     * @return a string array with each comma-separated element as a separate
     *         entry.
     * 
     * @throws IOException
     *             if bad things happen during the read
     */
    public String[] nextRow() {
        try {
            final String nextLine = getNextLine();
            return hasNext ? parseLine(nextLine) : null;
        } catch (final IOException ioe) {
            return null;
        }
    }

    /**
     * Parses an incoming String and returns an array of elements.
     * 
     * @param nextLine
     *            the string to parse
     * @return the comma-tokenized list of elements, or null if nextLine is null
     * @throws IOException
     *             if bad things happen during the read
     */
    private String[] parseLine(String nextLine) throws IOException {
        if (nextLine == null) {
            return null;
        }

        final List<String> tokensOnThisLine = new ArrayList<String>();
        StringBuffer sb = new StringBuffer();
        boolean inQuotes = false;
        do {
            if (inQuotes) {
                // continuing a quoted section, reappend newline
                sb.append("\n"); //$NON-NLS-1$
                nextLine = getNextLine();
                if (nextLine == null) {
                    break;
                }
            }
            for (int i = 0; i < nextLine.length(); i++) {
                final char c = nextLine.charAt(i);
                if (c == quotechar) {
                    // this gets complex... the quote may end a quoted block, or
                    // escape another quote.
                    // do a 1-char lookahead:
                    if (inQuotes // we are in quotes, therefore there can be
                            // escaped quotes in here.
                            && (nextLine.length() > (i + 1)) // there is
                            // indeed
                            // another character to
                            // check.)
                            && (nextLine.charAt(i + 1) == quotechar)) { // ..and
                        // that
                        // char. is
                        // a quote
                        // also.
                        // we have two quote chars in a row == one quote char,
                        // so consume them both and
                        // put one on the token. we do *not* exit the quoted
                        // text.
                        sb.append(nextLine.charAt(i + 1));
                        i++;
                    } else {
                        inQuotes = !inQuotes;
                        // the tricky case of an embedded quote in the middle:
                        // a,bc"d"ef,g
                        if ((i > 2) // not on the begining of the line)
                                && (nextLine.charAt(i - 1) != separator) // not
                                // at
                                // the
                                // begining
                                // of an
                                // escape
                                // sequence)
                                && (nextLine.length() > (i + 1))
                                && (nextLine.charAt(i + 1) != separator)) { // not
                            // at
                            // the
                            // end
                            // of an
                            // escape
                            // sequence

                            sb.append(c);
                        }
                    }
                } else if ((c == separator) && !inQuotes) {
                    tokensOnThisLine.add(sb.toString());
                    sb = new StringBuffer(); // start work on next token
                } else {
                    sb.append(c);
                }
            }
        } while (inQuotes);
        tokensOnThisLine.add(sb.toString());
        return tokensOnThisLine.toArray(new String[0]);

    }
}
