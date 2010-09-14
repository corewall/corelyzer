package corelyzer.data;

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

/**
 * A class to specifically extend the JTable. This was developed due to
 * interaction limitations with the corelyzer.ui.CheckBoxList. Unlike the
 * corelyzer.ui.CheckBoxList, when a user clicks on the label next to a
 * checkbox, the checkbox will not toggle on or off. The only way to toggle a
 * checkbox on or off is to actually click on the checkbox.
 */
public class CheckBoxTable extends JTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5528482465862859451L;
	public CheckBoxTableModel model;

	public CheckBoxTable() {
		model = new CheckBoxTableModel();
		setModel(model);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		getColumnModel().getColumn(0).setHeaderValue("Show");
		getColumnModel().getColumn(1).setHeaderValue("Field Name");
	}

	public void addCheckEventListener(final TableModelListener l) {
		model.checklistener = l;
	}

	/** Add another selectable entry into the table, with the associated label */
	public void addRow(final boolean b, final String label) {
		model.checkVec.add(new Boolean(b));
		model.labelVec.add(label);
	}

	/** Clears all entries in the table and calles repaint. */
	public void clearTable() {
		model.checkVec.clear();
		model.labelVec.clear();
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
		getColumnModel().getColumn(0).setPreferredWidth(60);
		getColumnModel().getColumn(1).setPreferredWidth(d.width - 60);
	}

	@Override
	public void setSize(final int width, final int height) {
		super.setSize(width, height);
		getColumnModel().getColumn(0).setPreferredWidth(60);
		getColumnModel().getColumn(1).setPreferredWidth(width - 60);
	}
}

/** Used as the table model for a CheckBoxTable */
class CheckBoxTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2887647344943761211L;
	/** A vector of booleans to keep track of what has been checked */
	public Vector<Boolean> checkVec;
	/** A vector of strings for the labels */
	public Vector<String> labelVec;
	public TableModelListener checklistener;

	public CheckBoxTableModel() {
		checkVec = new Vector<Boolean>();
		labelVec = new Vector<String>();
	}

	@Override
	public Class getColumnClass(final int col) {
		return getValueAt(0, col).getClass();
	}

	/** Always returns two. */

	public int getColumnCount() {
		return 2;
	}

	/** Returns the number of rows in the table model */

	public int getRowCount() {
		return checkVec.size();
	}

	/**
	 * Returns the object if the row is valid and the col >= 0 and < 2.
	 * Otherwise it returns null.
	 */

	public Object getValueAt(final int row, final int col) {
		if (col >= 2 || col < 0) {
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
		return null;
	}

	/** Returns true if the cell is in column zero, where the checkbox resides */

	@Override
	public boolean isCellEditable(final int row, final int col) {
		if (col == 0) {
			return true;
		}
		return false;
	}

	/**
	 * Set the checkbox in column zero to true or false, or set the label text
	 * next to the check box with a string.
	 */

	@Override
	public void setValueAt(final Object value, final int row, final int col) {
		if (col == 0) {
			if (checkVec.elementAt(row).booleanValue()) {
				checkVec.setElementAt(new Boolean(false), row);
			} else {
				checkVec.setElementAt(new Boolean(true), row);
			}
			if (checklistener != null) {
				checklistener.tableChanged(new TableModelEvent(this, row, row, 0));
			}
		}
		if (col == 1) {
			labelVec.setElementAt(value.toString(), row);
		}
	}
}
