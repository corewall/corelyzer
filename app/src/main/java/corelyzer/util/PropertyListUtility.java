package corelyzer.util;

import java.io.File;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

// TODO add "<dict>" to allow nested property list
public class PropertyListUtility {

	public static String defaultPlistHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!-- !DOCTYPE plist "
			+ "PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" " + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\" -->\n" + "<plist version=\"1.0\">\n";
	public static String defaultPlistFooter = "</plist>";

	public static String defaultDictHeader = "<dict>\n";
	public static String defaultDictFooter = "</dict>\n";

	// Generate <dict></dict> block string from a Hashtable<String, String)
	static public String generateDictStringFromHashtable(final Hashtable<String, String> aTable) {
		String result = defaultDictHeader;

		Enumeration<String> e = aTable.keys();
		while (e.hasMoreElements()) {
			String aKey = e.nextElement();
			String aValue = aTable.get(aKey);
			result += "\t<key>" + aKey + "</key>\n";
			result += "\t<string>" + aValue + "</string>\n";
		}

		result += defaultDictFooter;

		return result;
	}

	static public Hashtable<String, String> generateHashtableFromDictNode(final Element rootDict) {
		Hashtable<String, String> aTable = new Hashtable<String, String>();

		NodeList allKeys = rootDict.getElementsByTagName("key");
		NodeList allStrings = rootDict.getElementsByTagName("string");

		for (int i = 0; i < allKeys.getLength(); i++) {
			if (!(allKeys.item(i) instanceof Element)) {
				continue;
			}

			Element e = (Element) allKeys.item(i);
			String aKey = e.getTextContent();

			String aValue = "";
			if (i >= 0 && i < allStrings.getLength()) {
				if (!(allStrings.item(i) instanceof Element)) {
					continue;
				}

				Element ev = (Element) allStrings.item(i);
				aValue = ev.getTextContent();
			}

			aTable.put(aKey, aValue);
		}

		return aTable;
	}

	static public Hashtable<String, String> generateHashtableFromFile(final File f) {
		Hashtable<String, String> aTable = new Hashtable<String, String>();

		if (!f.exists()) {
			return aTable;
		}

		try {
			DOMParser parser = new DOMParser();
			parser.parse(f.getAbsolutePath());

			Document doc = parser.getDocument();
			NodeList allKeyNodes = doc.getElementsByTagName("key");
			NodeList allStringNodes = doc.getElementsByTagName("string");

			for (int i = 0; i < allKeyNodes.getLength(); i++) {
				if (!(allKeyNodes.item(i) instanceof Element)) {
					continue;
				}

				Element e = (Element) allKeyNodes.item(i);
				String aKey = e.getTextContent();

				String aValue = "";
				if (i >= 0 && i < allStringNodes.getLength()) {
					if (!(allStringNodes.item(i) instanceof Element)) {
						continue;
					}

					Element ev = (Element) allStringNodes.item(i);
					aValue = ev.getTextContent();
				}

				aTable.put(aKey, aValue);
			}

		} catch (Exception e) {
			System.err.println("---> [Exception] " + e + " file: '" + f.getAbsolutePath() + "'");
			e.printStackTrace();
		}

		return aTable;
	}

	// Generate a Hashtable<String, String> instance from a string that looks
	// like: name = julian\n city = chicago ...
	static public Hashtable<String, String> generateHashtableFromSimpleString(final String aPropertiesString) {
		Hashtable<String, String> aTable = new Hashtable<String, String>();

		String[] lines = aPropertiesString.split("\n");

		for (String aLine : lines) {
			String[] toks = aLine.split("=");

			if (toks.length == 2) {
				String aKey = toks[0].trim().toLowerCase();
				String aValue = toks[1].trim().toLowerCase();
				aTable.put(aKey, aValue);
			}
		}

		return aTable;
	}

	public static String generateHTMLTableString(final File aFile) {
		String htmlString = "<table border=\"3\">\n" + "\t    <tr aligh=\"left\" valigh=\"top\" >\n" + "\t      <td bgcolor=\"#222222\">\n"
				+ "\t\t<font color=\"#FFCC00\">\n" + "\t\tProperty\n" + "                </font>\n" + "\t      </td>\n" + "\t      <td bgcolor=\"#222222\">\n"
				+ "\t\t<font color=\"#FFCC00\">\n" + "\t\tValue\n" + "                </font>\n" + "\t      </td>" + "\t    </tr>\n";

		Hashtable<String, String> aHash = PropertyListUtility.generateHashtableFromFile(aFile);

		for (Map.Entry<String, String> s : aHash.entrySet()) {
			String prop = (String) s.getKey();
			String value = (String) s.getValue();

			htmlString += "<tr aligh=\"left\" valigh=\"top\" >\n" + "\t      <td bgcolor=\"#222222\">\n" + "\t\t<font color=\"#FFCC00\">\n" + "\t\t" + prop
					+ "\n" + "                </font>\n" + "\t      </td>\n" + "<td bgcolor=\"#222222\">\n" + "\t\t<font color=\"#FFCC00\">\n" + "\t\t" + value
					+ "\n" + "                </font>\n" + "\t      </td>" + "\t    </tr>";
		}

		htmlString += "\n</table>\n";

		return htmlString;
	}

	public static String generateHTMLTableString(final Hashtable<String, String> aHash) {
		String htmlString = "<table border=\"3\">\n" + "\t    <tr aligh=\"left\" valigh=\"top\" >\n" + "\t      <td bgcolor=\"#222222\">\n"
				+ "\t\t<font color=\"#FFCC00\">\n" + "\t\tProperty\n" + "                </font>\n" + "\t      </td>\n" + "\t      <td bgcolor=\"#222222\">\n"
				+ "\t\t<font color=\"#FFCC00\">\n" + "\t\tValue\n" + "                </font>\n" + "\t      </td>" + "\t    </tr>\n";

		for (Map.Entry<String, String> s : aHash.entrySet()) {
			String prop = (String) s.getKey();
			String value = (String) s.getValue();

			htmlString += "<tr aligh=\"left\" valigh=\"top\" >\n" + "\t      <td>\n" + "\t\t\n" + "\t\t" + prop + "\n" + "\n" + "\t      </td>\n" + "<td>\n"
					+ "\t\t\n" + "\t\t" + value + "\n" + "\n" + "\t      </td>" + "\t    </tr>";
		}

		htmlString += "\n</table>\n";

		return htmlString;
	}

	public static String generateHTMLTableString(final Hashtable<String, String> aHash, final Vector<String> orderedKeys) {
		String htmlString = "<table border=\"3\">\n" + "\t    <tr aligh=\"left\" valigh=\"top\" >\n" + "\t      <td bgcolor=\"#222222\">\n"
				+ "\t\t<font color=\"#FFCC00\">\n" + "\t\tProperty\n" + "                </font>\n" + "\t      </td>\n" + "\t      <td bgcolor=\"#222222\">\n"
				+ "\t\t<font color=\"#FFCC00\">\n" + "\t\tValue\n" + "                </font>\n" + "\t      </td>" + "\t    </tr>\n";

		for (String key : orderedKeys) {
			String value = aHash.get(key);

			htmlString += "<tr aligh=\"left\" valigh=\"top\" >\n" + "\t      <td>\n" + "\t\t\n" + "\t\t" + StringUtility.capitalizeHeadingCharacter(key)
					+ "\n\n" + "\t      </td>\n" + "<td>\n" + "\t\t\n" + "\t\t" + value + "\n" + "\n" + "\t      </td>" + "\t    </tr>";
		}

		htmlString += "\n</table>\n";

		return htmlString;
	}

	// Generate a plist content as a string from a Hashtable<String, String>
	static public String generateOuputStringFromHashtable(final Hashtable<String, String> aTable) {
		String result = defaultPlistHeader;

		result += generateDictStringFromHashtable(aTable);
		result += defaultPlistFooter;

		return result;
	}

	public static void main(final String[] args) {
		// Generate a simple hashtable
		Hashtable<String, String> aHash = new Hashtable<String, String>();
		aHash.put("corename", "andrill_001");
		aHash.put("depth", "1287.0");
		aHash.put("URL", "faraway");
		aHash.put("date", "2007-07-12");

		File propertyListFile = new File("/tmp/propList.xml");

		// Go & back
		System.out.println("----- PropertyListUtility Testing -----");
		System.out.println("---> Generating property list:");
		String propList = generateOuputStringFromHashtable(aHash);
		System.out.println("" + propList);

		boolean isOK = saveHashtableToProperListFile(aHash, propertyListFile);

		if (!isOK) {
			System.err.println("---> [ERROR] Writing failed, stops here");
			System.exit(1);
		}

		System.out.println("<--- Generate hash from propety list file:");
		Hashtable<String, String> bHash = generateHashtableFromFile(propertyListFile);

		Enumeration<String> e = bHash.keys();
		while (e.hasMoreElements()) {
			String aKey = e.nextElement();
			String aValue = bHash.get(aKey);
			System.out.println("'" + aKey + "', '" + aValue + "'");
		}
		System.out.println("------------------------------------------");
	}

	static public boolean saveHashtableToProperListFile(final Hashtable<String, String> aTable, final File aFile) {

		String fileContent = generateOuputStringFromHashtable(aTable);

		try {
			FileWriter fw = new FileWriter(aFile);
			fw.write(fileContent, 0, fileContent.length());
			fw.close();
		} catch (Exception e) {
			System.err.println("---> [EXCEPTION] Writing file '" + aFile + "' failed");
			return false;
		}

		return true;
	}

}
