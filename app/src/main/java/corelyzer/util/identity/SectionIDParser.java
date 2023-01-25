// SectionIDParserFactory.java
// Copyright (c) CSDCO, LacCore
// April 23 2020
//
// Parsers for established core section ID naming conventions, including a default
// backup parser for unrecognized core section IDs.

package corelyzer.util.identity;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SectionIDParser {
    Pattern pattern = null;
    String name = null;
    protected SectionIDParser() { }

    public boolean matches(final String secid) {
        return pattern.matcher(secid).matches();
    }
    public String getFullHoleID(final String secid) {
        Matcher m = pattern.matcher(secid);
        return m.matches() ? m.group("hole") : null;
    }
    public String getName() { return name; }
}


class LacCoreSectionParser extends SectionIDParser {
    public LacCoreSectionParser() {
        super();
        name = "LacCore";
        final String EXP = "[A-Z0-9]+";
        // 8/16/2021 brg: LAKEYEAR component is now optional
        final String LAKEYEAR = "([A-Z0-9]+?[0-9]{2}-)?"; // Lake: 1+ alphanumeric, Year: exactly 2 numbers
        final String SITE = "[0-9]+";
        final String HOLE = "[A-Z]+";
        final String SITEHOLE = SITE + HOLE;
        final String CORE = "[0-9]+";
        final String TOOL = "([A-Z]|BX|HS|MC|MK|RP)";
        final String CORETOOL = CORE + TOOL;
        final String SECTION = "([0-9]{1,2}|CC)";
        final String HALF = "(-[AW]|-WR)?"; // optional
        final String SUFFIX = "(_.+)?"; // optional

        // parentheses are capturing group for full hole/track ID
        final String laccorePattern = "(?<hole>" + EXP + "-" + LAKEYEAR + SITEHOLE + ")-" + CORETOOL + "-" + SECTION + HALF + SUFFIX;
        pattern = Pattern.compile(laccorePattern);
    }
}

class IODPSectionParser extends SectionIDParser {
    public IODPSectionParser() {
        super();
        name = "IODP";
        final String EXP = "[0-9]+[A-Z]*";
        final String SITE = "U?[0-9]+";
        final String HOLE = "([A-Z]+|\\*)";
        final String CORE = "[0-9]+";
        final String TOOL = "[A-Z]+";
        final String SECTION = "([0-9]+|CC)";
        final String HALF = "(-[AW]|-WR)?"; // optional
        final String SUFFIX = "(_.+)?"; // optional
        
        // parentheses are capturing group for full hole/track ID
        final String iodpPattern = "(?<hole>" + EXP + "-" + SITE + HOLE + ")-" + CORE + TOOL + "-" + SECTION + HALF + SUFFIX;
        pattern = Pattern.compile(iodpPattern);
    }
}

class ICDPSectionParser extends SectionIDParser {
    public ICDPSectionParser() {
        super();
        name = "ICDP";
        final String EXP = "[0-9]+";
        final String SITE = "[0-9]+";
        final String HOLE = "[A-Z]";
        final String CORE = "[0-9]+";
        final String SECTION = "[0-9]+";
        final String HALF = "(_[AW]|_WR)?"; // optional
        final String SUFFIX = "(_.+)?"; // optional

        // parentheses are capturing group for full hole/track ID
        final String icdpPattern = "(?<hole>" + EXP + "_" + SITE + "_" + HOLE + ")_" + CORE + "_" + SECTION + HALF + SUFFIX;
        pattern = Pattern.compile(icdpPattern);
    }
}

// Last resort when a section name doesn't match any known naming conventions.
// Split on - or _ if present.  Make no other attempt to split components e.g. on
// alpha and numeric boundaries. If input string doesn't contain - or _, return
// entire string.
class DefaultSectionParser extends SectionIDParser {
    public DefaultSectionParser() {
        super();
        name = "Default";
        pattern = Pattern.compile("([a-zA-Z0-9]+[_-])+([a-zA-Z0-9]+)");
    }

    @Override
    public boolean matches(String secid) {
        return true;
    }

    // Best we can do is to guess meaning of components, assuming the section
    // ID ends in core (if two components), or core and section (if 3+ components).
    @Override
    public String getFullHoleID(final String secid) {
        String result = null;
		StringTokenizer tokenizer = new StringTokenizer(secid, "-_");
        final int tokenCount = tokenizer.countTokens();
        if (tokenCount > 1 && pattern.matcher(secid).matches()) {
            if (tokenCount == 2 || tokenCount == 3) {
                // assume first component is the hole followed by core (count == 2),
                // or core and section (count == 3)
                result = tokenizer.nextToken();
            } else if (tokenCount > 3) {
                // assume last two groups are core and section, take everything prior
                result = "";
                for (int tidx = 0; tidx < tokenCount - 2; tidx++) {
                    result += tokenizer.nextToken();
                    if (tidx < tokenCount - 3) {
                        // for all but the last component of the full hole ID, restore the
                        // delimiter from the original string (StringTokenizer removes it)
                        final String delimiter = secid.substring(result.length(), result.length() + 1);
                        result += delimiter;
                    }
                }
            }
        } else { // can't parse anything, return entire string
            result = secid;
        }
        return result;
    }
}