package corelyzer.data;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

/**
 * A class to specifically extend the JTable. This was developed due to
 * interaction limitations with the corelyzer.ui.CheckBoxList. Unlike the
 * corelyzer.ui.CheckBoxList, when a user clicks on the label next to a
 * checkbox, the checkbox will not toggle on or off. The only way to toggle a
 * checkbox on or off is to actually click on the checkbox.
 */
public class GraphSectionPropsTable extends JTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5528482465862859451L;
	public GraphSectionPropsTableModel model;

	public GraphSectionPropsTable() {
		model = new GraphSectionPropsTableModel();
		setModel(model);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		getColumnModel().getColumn(0).setHeaderValue("Show");
		getColumnModel().getColumn(1).setHeaderValue("Field");
		getColumnModel().getColumn(2).setHeaderValue("Color");
		getColumnModel().getColumn(3).setHeaderValue("Data Range");
	}

	public void addCheckEventListener(final TableModelListener l) {
		//model.checklistener = l;
	}

	/** Add another selectable entry into the table, with the associated label */
	public void addRow(final boolean b, final String label, final Color c, final String range) {
		model.checkVec.add(Boolean.valueOf(b));
		model.labelVec.add(label);
		model.colorVec.add(c);
		model.valueRangeVec.add(range);
	}

	/** Clears all entries in the table and calles repaint. */
	public void clearTable() {
		model.checkVec.clear();
		model.labelVec.clear();
		model.colorVec.clear();
		model.valueRangeVec.clear();
		repaint();
	}

	@Override
	public int getRowCount() {
		return model.labelVec.size();
	}

	public String getRowLabel(final int row) {
		return model.labelVec.elementAt(row);
	}

	public boolean isRowChecked(final int row) {
		return model.checkVec.elementAt(row).booleanValue();
	}

	@Override
	public void setPreferredSize(final Dimension d) {
		super.setPreferredSize(d);
		getColumnModel().getColumn(0).setPreferredWidth(50);
		getColumnModel().getColumn(1).setPreferredWidth(200);
		getColumnModel().getColumn(2).setPreferredWidth(50);
		getColumnModel().getColumn(3).setPreferredWidth(d.width - 300);
	}

	@Override
	public void setSize(final int width, final int height) {
		super.setSize(width, height);
		getColumnModel().getColumn(0).setPreferredWidth(50);
		getColumnModel().getColumn(1).setPreferredWidth(200);
		getColumnModel().getColumn(2).setPreferredWidth(50);
		getColumnModel().getColumn(3).setPreferredWidth(width - 300);
	}
}

/** Used as the table model for a GraphSectionPropsTable */
class GraphSectionPropsTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2887647344943761211L;
	/** A vector of booleans to keep track of what has been checked */
	public Vector<Boolean> checkVec;
	/** A vector of strings for the labels */
	public Vector<String> labelVec;
	public Vector<Color> colorVec;
	public Vector<String> valueRangeVec;

	//public TableModelListener checklistener;

	public GraphSectionPropsTableModel() {
		checkVec = new Vector<Boolean>();
		labelVec = new Vector<String>();
		colorVec = new Vector<Color>();
		valueRangeVec = new Vector<String>();
	}

	@Override
	public Class<?> getColumnClass(final int col) {
		return getValueAt(0, col).getClass();
	}

	/** Returns the number of colums in the table model, currently 3. */

	public int getColumnCount() {
		return 4;
	}

	/** Returns the number of rows in the table model */

	public int getRowCount() {
		return checkVec.size();
	}

	/**
	 * Returns the object if the row is valid and the col >= 0 and < 4.
	 * Otherwise it returns null.
	 */

	public Object getValueAt(final int row, final int col) {
		if (col >= 4 || col < 0) {
			return null;
		}
		if (row >= getRowCount() || row < 0) {
			return null;
		}
		if (col == 0) {
			return checkVec.elementAt(row);
		}
		if (col == 1) {
			return labelVec.elementAt(row);
		}
		if (col == 2) {
			return colorVec.elementAt(row);
		}
		if (col == 3) {
			return valueRangeVec.elementAt(row);
		}
		return null;
	}

	/** Returns true if the cell is in column zero, where the checkbox resides */

	@Override
	public boolean isCellEditable(final int row, final int col) {
		return ( col == 0 || col == 2 );
	}

	/**
	 * Set the checkbox in column zero to true or false, or set the label text
	 * next to the check box with a string.
	 */

	@Override
	public void setValueAt(final Object value, final int row, final int col) {
		if (col == 0) {
			if (checkVec.elementAt(row).booleanValue()) {
				checkVec.setElementAt(Boolean.valueOf(false), row);
			} else {
				checkVec.setElementAt(Boolean.valueOf(true), row);
			}
			this.fireTableCellUpdated( row, col );
		}
		if (col == 1) {
			labelVec.setElementAt(value.toString(), row);
		}
		if (col == 2) {
			colorVec.setElementAt((Color)value, row);
			this.fireTableCellUpdated( row, col );
		}
		if (col == 3) {
			valueRangeVec.setElementAt(value.toString(), row);
		}
	}
}
