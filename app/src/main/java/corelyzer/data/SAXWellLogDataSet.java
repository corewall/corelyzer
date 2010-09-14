package corelyzer.data;

import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class SAXWellLogDataSet extends WellLogDataSet {

	public static void main(final String[] args) {
		long start = System.currentTimeMillis();
		SAXWellLogDataSet data = new SAXWellLogDataSet("/Users/jareed/Desktop/MSCL.xml");
		System.out.println(System.currentTimeMillis() - start + " ms");
		System.out.println(data.tables);

		start = System.currentTimeMillis();
		WellLogDataSet data2 = new WellLogDataSet("/Users/jareed/Desktop/MSCL.xml");
		System.out.println(System.currentTimeMillis() - start + " ms");
		System.out.println(data2.tables);
	}

	public SAXWellLogDataSet() {
		this.tables = new Vector<WellLogTable>();
	}

	/**
	 * Construct a dataset object from given dataset XML file
	 * 
	 * @param f
	 *            Filename of the dataset in XML format
	 */
	public SAXWellLogDataSet(final String f) {
		this();

		if (f == null || f.equals("")) {
			return;
		}
		this.filename = f;

		// parse with SAX
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			CorelyzerXMLDataHandler handler = new CorelyzerXMLDataHandler();
			parser.parse(this.filename, handler);
			tables.addAll(handler.getTables());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
