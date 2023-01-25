// SectionIDParserFactory.java
// Copyright (c) CSDCO, LacCore
// April 23 2020
//
// Factory to provide an appropriate parser for a given core section ID String.


package corelyzer.util.identity;

import java.util.Vector;

public class SectionIDParserFactory {
    static Vector<SectionIDParser> parsers;
    static {
        parsers = new Vector<SectionIDParser>();
        parsers.add(new LacCoreSectionParser());
        parsers.add(new IODPSectionParser());
        parsers.add(new ICDPSectionParser());
        parsers.add(new DefaultSectionParser());
    }

    // Return the first SectionIDParser that matches secid.
    public static SectionIDParser getMatchingParser(final String secid) {
        SectionIDParser result = null;
        for (SectionIDParser p : parsers) {
            if (p.matches(secid)) {
                result = p;
                break;
            }
        }
        return result;
    }

    public static void clearParsers() { parsers.clear(); }
    public static void addParser(SectionIDParser parser) { parsers.add(parser); }
    public static void removeParser(SectionIDParser parser) { parsers.remove(parser); }
}
