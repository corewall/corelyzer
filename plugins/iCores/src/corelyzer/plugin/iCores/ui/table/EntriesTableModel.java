package corelyzer.plugin.iCores.ui.table;

import corelyzer.plugin.iCores.data.SectionEntry;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.util.Vector;

public class EntriesTableModel extends AbstractTableModel {

    private int numberOfFields = 7;
    private Vector<Boolean> downloadedVec;
    private Vector<String>  nameVec;
    private Vector<Float>   startDepthVec;
    private Vector<Float>   lengthVec;
    private Vector<Float>   dpixVec;
    private Vector<Float>   dpiyVec;
    private Vector<String>  urlVec;

    private TableModelListener checklistener;

    public EntriesTableModel() {
        downloadedVec = new Vector<Boolean>();
        nameVec       = new Vector<String>();
        startDepthVec = new Vector<Float>();
        lengthVec     = new Vector<Float>();
        dpixVec       = new Vector<Float>();
        dpiyVec       = new Vector<Float>();
        urlVec        = new Vector<String>();        
    }

    public int getRowCount() {
        return downloadedVec.size();
    }

    public int getColumnCount() {
        return this.numberOfFields;
    }

    public Object getValueAt(int row, int col) {
        if (col >= this.numberOfFields || col < 0) return null;
        if (row >= getRowCount() || row < 0) return null;

        if (col == 0) return downloadedVec.elementAt(row);
        if (col == 1) return nameVec.elementAt(row);
        if (col == 2) return startDepthVec.elementAt(row);
        if (col == 3) return lengthVec.elementAt(row);
        if (col == 4) return dpixVec.elementAt(row);
        if (col == 5) return dpiyVec.elementAt(row);
        if (col == 6) return urlVec.elementAt(row);

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
            downloadedVec.setElementAt(!downloadedVec.elementAt(row), row);

            if (checklistener != null) {
                checklistener.tableChanged(
                        new TableModelEvent(this, row, row, 0));
            }
        }

        if (col == 1) {
            nameVec.setElementAt(value.toString(), row);
        }

        if (col == 2) {
            startDepthVec.setElementAt(Float.parseFloat(value.toString()), row);
        }

        if (col == 3) {
            lengthVec.setElementAt(Float.parseFloat(value.toString()), row);
        }

        if (col == 4) {
            dpixVec.setElementAt(Float.parseFloat(value.toString()), row);
        }

        if (col == 5) {
            dpiyVec.setElementAt(Float.parseFloat(value.toString()), row);
        }

        if (col == 6) {
            urlVec.setElementAt(value.toString(), row);            
        }
    }

    public void addElement(SectionEntry e) {
        boolean downloaded = false; // TODO check with local repo
        downloadedVec.add(downloaded);
        nameVec.add(e.getName());
        startDepthVec.add(e.getStartInterval());
        lengthVec.add(e.getLength());
        urlVec.add(e.getUrl());
        dpixVec.add(e.getImage().getDpi_x());
        dpiyVec.add(e.getImage().getDpi_y());
    }

    public void clear() {
        this.downloadedVec.clear();
        this.nameVec.clear();
        this.startDepthVec.clear();
        this.lengthVec.clear();
        this.urlVec.clear();
        this.dpixVec.clear();
        this.dpiyVec.clear();
    }

}
