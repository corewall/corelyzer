package corelyzer.data;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CorelyzerXMLDataHandler extends DefaultHandler {
	protected List<WellLogTable> tables = new ArrayList<WellLogTable>();
	protected StringBuilder buffer = new StringBuilder();
	protected WellLogTable current = null;
	protected List<List<Float>> data = new ArrayList<List<Float>>();
	protected List<List<Boolean>> valid = new ArrayList<List<Boolean>>();
	protected int index = 0;
	protected int currentDepthIndex = -1;

	@Override
	public void characters(final char[] ch, final int start, final int length) {
		for (int i = start; i < start + length; i++) {
			buffer.append(ch[i]);
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		if ("section".equals(qName)) {
			// set some values
			current.depthColumn = 0;
			current.numCols = current.columns.size();
			current.numFields = current.columns.size() - 1;
			current.colData = current.columns.toArray(new Column[current.columns.size()]);
			current.numRows = data.get(0).size();

			// copy over our column data
			for (int i = 0; i < current.columns.size(); i++) {
				List<Float> colData = data.get(i);
				Column col = current.columns.get(i);
				col.dataArray = new float[colData.size()];
				col.valid = new boolean[colData.size()];
				float max = Float.MIN_VALUE;
				float min = Float.MAX_VALUE;
				for (int j = 0; j < colData.size(); j++) {
					float f = colData.get(j);
					boolean isValid = valid.get(i).get(j);

					col.dataArray[j] = f;
					if (f > max && isValid) {
						max = f;
						col.maxIndex = j;
					}
					if (f < min && isValid) {
						min = f;
						col.minIndex = j;
					}
					col.valid[j] = valid.get(i).get(j);
				}
			}

			// set the column data
			tables.add(current);
			current = null;
			data.clear();
			valid.clear();
		} else if ("id".equals(qName)) {
			current.name = buffer.toString();
		} else if ("depth_unit".equals(qName)) {
			String unit = buffer.toString().toLowerCase();
			if (unit.equals("cm")) {
				current.depthUnit = UnitLength.CM;
			} else if (unit.equals("m")) {
				current.depthUnit = UnitLength.M;
			} else if (unit.equals("mm")) {
				current.depthUnit = UnitLength.MM;
			} else if (unit.equals("in")) {
				current.depthUnit = UnitLength.INCH;
			} else if (unit.equals("ft")) {
				current.depthUnit = UnitLength.FOOT;
			} else if (unit.equals("yd")) {
				current.depthUnit = UnitLength.YARD;
			} else {
				current.depthUnit = UnitLength.M;
			}
		} else if ("offset".equals(qName)) {
			try {
				current.depth_offset = Float.parseFloat(buffer.toString());
			} catch (NumberFormatException e) {
				current.depth_offset = 0.0f;
			}
		} else if ("top".equals(qName)) {
			try {
				current.topDepth = Float.parseFloat(buffer.toString());
			} catch (NumberFormatException e) {
				current.topDepth = -1.0f;
			}
		} else if ("depth".equals(qName)) {
			data.get(0).add(Float.parseFloat(buffer.toString()));
			valid.get(0).add(true);
			currentDepthIndex = data.get(0).size() - 1;

			// Pre-populate the data & valid array to the indices matches with
			// depth array
			// Index starts at '1' because '0' is the depth column.
			for (int i = 1; i <= current.headers.size() - 1; ++i) {
				data.get(i).add(0.0f); // default value
				valid.get(i).add(false);
			}
		} else if ("sensor".equals(qName)) {
			// Fill-in pre-allocated(by depth tag) vector
			try {
				float value = Float.parseFloat(buffer.toString().trim());

				data.get(index).set(currentDepthIndex, value);
				valid.get(index).set(currentDepthIndex, true);
			} catch (NumberFormatException nfe) {
				// Invalid as default
			}
		}
	}

	public List<WellLogTable> getTables() {
		return tables;
	}

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		if ("section".equals(qName)) {
			// create a new table
			current = new WellLogTable();

			// add our depth column
			Column depth = new Column();
			depth.setDataType(CellType.FLOAT);
			current.columns.add(depth);
			current.headers.add("section depth");

			// add a bucket for the data
			data.add(new ArrayList<Float>());
			valid.add(new ArrayList<Boolean>());

			// section "offset" attribute
			String offsetString = attributes.getValue("offset");
			float offset;

			if (offsetString == null) {
				offset = -1.0f;
			} else {
				try {
					offset = Float.parseFloat(offsetString);
				} catch (NumberFormatException e) {
					offset = -1.0f;
				}
			}
			current.setDepth_offset(offset);
		} else if ("field".equals(qName)) {
			// create a new column for the field
			Column field = new Column();
			field.setDataType(CellType.FLOAT);
			current.columns.add(field);
			current.headers.add(attributes.getValue("name"));

			// add a bucket for the data
			data.add(new ArrayList<Float>());
			valid.add(new ArrayList<Boolean>());
		} else if ("sensor".equals(qName)) {
			index = Integer.parseInt(attributes.getValue("id")) + 1;
		}

		// clear our buffer
		buffer = new StringBuilder();
	}
}
