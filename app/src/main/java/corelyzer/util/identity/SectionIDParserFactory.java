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

    // tests
    public static void main(String[] args) {
        String[] laccore_ids = {
            // valid IDs
            "1001-MCC11-1A-5P-1", // exp can be numeric
            "OGDP-119-1X-1G-5", // Lake code can be entirely numeric
            "OGDP-LAKE119-1X-1G-5", // Lake code can end in a number, so must allow 2+ digits
            "OGDP-A11-1P-1H-1",
            "OGDP-OLD14-1A-1G-1", // no archive/working/whole-round
            "OGDP-OLD14-1A-1G-1-A", // archive
            "OGDP-OLD14-1A-1G-1-W", // working
            "OGDP-OLD14-1A-1G-1-WR", // whole-round
            "OGDP-OLD14-1A-1G-CC-A", // core catcher
            "OGDP-OLD14-1A-1G-CC", // core catcher no half
            "OGDP-OLD14-1A-1G-7-A", // 7th section
            "OGDP-OLD14-1A-1G-99-A", // max valid section is 99
            "GLAD9-PET06-1A-31MC-1", // valid two-letter tool code
            "OGDP-OLD14-99ZZ-999Q-99-A", // crazy but technically valid site, hole, core, and section counts
            "OGDP-OLD14-10X-1P-7_f", // minimal suffix
            "OGDP-OLD14-10X-1Q-CC-A_f11_bunchofotherstuff@#%", // long but still valid suffix
            // invalid IDs
            "FOO-22-1A-1C-1", // LakeYear code must be at least three characters
            "BAR-SUP1-1A-1C-1", // Year component must be at least two numbers
            "BAZ-SU112-1A-8-1", // Core must include tool code
            "GLAD9-PET06-1A-31MX-1", // invalid two-letter tool code
            "OGDP-OLD14-1A-1G-1-X" // invalid half A/W/WR
        };
        String[] iodp_ids = { // all valid IODP IDs
            "318-U1357A-1H-1",
            "318-U1357A-1H-1-A",
            "318-U1357ZZ-99H-CC-W",
            "100-625C-1H-1",
            "48-399*-1R-3-WR",
            "362T-U1473A-93R-4",
            "334XA-U1356Q-10Z-CC-WR_suffix"
        };
        String[] icdp_ids = {
            // valid IDs
            "5054_1_A_1_1",
            "5054_30_Z_1_1_W",
            "5054_2_C_999_7_A", // huge core
            "5054_2_C_3_999_WR", // huge section
            "5054_2_C_3_7_suffix", // suffix, no half
            "5054_2_C_3_7_W_suffix", // half and suffix
            "1_2_D_10_2_A_suffix_quite_long",
            // invalid - max of 26 holes per site (i.e. max hole is 'Z')
            "333_1_AA_1_1"
        };
        String[] default_ids = {
            // valid IDs
            "1-1",
            "1A-1b-1CXZ",
            "a-b-C",
            "D1-E20-F300-G999",
            "D_E_F_G_",
            "d1_e12_xx_1000_cow",
            "EXP10-SITE9-HOLE5_CORE3_SEC2",
            "A_B-C_D-E_F-G",
            // invalid IDs
            "1A", // must be at least two components separated by - or _
            "1-B--C" // only a single - or _ can separate each pair of components
        };

        Vector<String[]> idLists = new Vector<String[]>();
        // idLists.add(laccore_ids);
        // idLists.add(iodp_ids);
        // idLists.add(icdp_ids);
        idLists.add(default_ids);
        for (String[] ids : idLists) {
            SectionIDParser p = SectionIDParserFactory.getMatchingParser(ids[0]);
            System.out.println("Using " + p.getName() + " parser.");
            for (int i = 0; i < ids.length; i++) {
                final boolean matches = p.matches(ids[i]);
                System.out.println(ids[i] + ": " + matches);
                if (matches) {
                    System.out.println("Hole/Track component: " + p.getFullHoleID(ids[i]));
                }            
            }
        }
    }
}
