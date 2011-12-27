package corelyzer.plugin.iCores.ui.table;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.util.Vector;

public class CollectionTableModel extends AbstractTableModel {
    private Vector<Boolean> checkVec;
    private Vector<String> labelVec;
    private Vector<String> urlVec;
    private TableModelListener checklistener;

    public CollectionTableModel() {
        checkVec = new Vector<Boolean>();
        labelVec = new Vector<String>();
        urlVec = new Vector<String>();
    }

    public int getRowCount() {
        return checkVec.size();
    }

    public int getColumnCount() {
        return 3;
    }

    public Object getValueAt(int row, int col) {
        if (col >= 3 || col < 0) return null;
        if (row >= getRowCount() || row < 0) return null;

        if (col == 0) return checkVec.elementAt(row);
        if (col == 1) return labelVec.elementAt(row);
        if (col == 2) return urlVec.elementAt(row);

        return null;
    }

    public Class getColumnClass(int col) {
        return getValueAt(0, col).getClass();
    }

    public boolean isCellEditable(int row, int col) {
        return (col == 0);
    }

    public void setValueAt(Object value, int row, int col) {
        if (col == 0) {
            checkVec.setElementAt(!checkVec.elementAt(row),
                    row);

            if (checklistener != null) {
                checklistener.tableChanged(
                        new TableModelEvent(this, row, row, 0));
            }
        }

        if (col == 1) {
            labelVec.setElementAt(value.toString(), row);
        }

        if (col == 2) {
            urlVec.setElementAt(value.toString(), row);
        }
    }

    public void addElement(boolean subscribed, String title, String url) {
        checkVec.add(subscribed);
        labelVec.add(title);
        urlVec.add(url);
    }

    public void clear() {
        this.checkVec.clear();
        this.labelVec.clear();
        this.urlVec.clear();
    }
}