package corelyzer.data;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class ImagePropertyTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6605238316599523670L;
	final public static String HORIZONTAL = "Horizontal";
	final public static String VERTICAL = "Vertical";

	public Vector<String> filepathVec;
	public Vector<String> fileNameVec;
	Vector<String> orientationVec;
	Vector<Float> lengthVec;
	Vector<Float> dpixVec;
	Vector<Float> dpiyVec;
	Vector<Float> depthVec;

	// Vector<Dimension> imgSize;

	public ImagePropertyTableModel() {
		filepathVec = new Vector<String>();
		fileNameVec = new Vector<String>();
		orientationVec = new Vector<String>();
		lengthVec = new Vector<Float>();
		dpixVec = new Vector<Float>();
		dpiyVec = new Vector<Float>();
		depthVec = new Vector<Float>();
		// imgSize = new Vector<Dimension>();
	}

	@Override
	public Class<?> getColumnClass(final int col) {
		return getValueAt(0, col).getClass();
	}

	public int getColumnCount() {
		return 6;
	}

	public int getRowCount() {
		return fileNameVec.size();
	}

	public Object getValueAt(final int row, final int col) {
		if (col >= 6 || col < 0) {
			return null;
		}
		if (row >= getRowCount() || row < 0) {
			return null;
		}

		switch (col) {
			case 0:
				return fileNameVec.elementAt(row);

			case 1:
				return orientationVec.elementAt(row);

			case 2:
				return lengthVec.elementAt(row);

			case 3:
				return dpixVec.elementAt(row);

			case 4:
				return dpiyVec.elementAt(row);

			case 5:
				return depthVec.elementAt(row);

			default:
				return null;
		}
	}

	@Override
	public boolean isCellEditable(final int row, final int col) {
		return col != 0;
	}

	@Override
	public void setValueAt(final Object value, final int row, final int col) {
		switch (col) {
			case 0:
				fileNameVec.setElementAt(value.toString(), row);
				break;

			case 1:
				orientationVec.setElementAt(value.toString(), row);
				break;

			case 2:
				lengthVec.setElementAt(Float.parseFloat(value.toString()), row);
				break;

			case 3:
				dpixVec.setElementAt(Float.parseFloat(value.toString()), row);
				fireTableDataChanged();
				break;

			case 4:
				dpiyVec.setElementAt(Float.parseFloat(value.toString()), row);
				fireTableDataChanged();
				break;

			case 5:
				depthVec.setElementAt(Float.parseFloat(value.toString()), row);
				break;
		}

	}

}
