package corelyzer.ui.tie;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import java.util.Vector;

import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import net.miginfocom.swing.MigLayout;

public class ManageSectionTiesDialog extends JFrame {
    private JTable tieTable;
    private Vector<TieData> ties = new Vector<TieData>();
    private JButton deleteButton;
    private JButton closeButton;

    public ManageSectionTiesDialog() {
        super();
        addDummyTies();
        setupUI();
    }

    public ManageSectionTiesDialog(int[] tieIds) {
        super();
        gatherTieData(tieIds);
        setupUI();
    }

    private void setupUI() {
        setTitle("Manage section ties");
        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[grow]", "[grow][]"));
        
        // tie table
        tieTable = new TieTable(new TieTableModel(ties));
        tieTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    TieData tie = ties.get(tieTable.getSelectedRow());
                    SceneGraph.setSelectedTie(tie.id);
                    updateButtons();
                    CorelyzerApp.getApp().updateGLWindows();
                }
            }
        });

        tieTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                final int row = e.getFirstRow();
                TieData tie = ties.get(row);
                SceneGraph.setSectionTieShow(tie.id, tie.show);
                CorelyzerApp.getApp().updateGLWindows();
            }
        });

        JScrollPane tableScroll = new JScrollPane(tieTable);
        contentPane.add(tableScroll, "wmin 300, hmin 100, wrap, grow");

        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final int row = tieTable.getSelectedRow();
                TieData tie = ties.get(row);
                ties.removeElement(tie);
                SceneGraph.deleteSectionTie(tie.id);
                final int new_sel = ties.size() > 0 ? Math.min(ties.size()-1, row) : -1;
                if (new_sel != -1) { // select another row if possible
                    tieTable.setRowSelectionInterval(new_sel, new_sel);
                    TieData selectTie = ties.get(tieTable.getSelectedRow());
                    SceneGraph.setSelectedTie(selectTie.id);
                }
                tieTable.updateUI();
                CorelyzerApp.getApp().updateGLWindows();
            }
        });
        contentPane.add(deleteButton, "split 2");

        closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                SceneGraph.setSelectedTie(-1);
                CorelyzerApp.getApp().updateGLWindows();
                setVisible(false);
            }
        });
        contentPane.add(closeButton);
        
        pack();
        updateButtons();
    }

    private void updateButtons() {
        final boolean enable = tieTable.getSelectedRow() != -1;
        deleteButton.setEnabled(enable);
        if (!enable) return;
    }
    
    private void gatherTieData(int[] tieIds) {
        for (int i = 0; i < tieIds.length; i++) {
            final int id = tieIds[i];
            final boolean show = SceneGraph.getSectionTieShow(id);
            final String srcDesc = SceneGraph.getSectionTieSourceDescription(id);
            final String destDesc = SceneGraph.getSectionTieDestinationDescription(id);
            ties.add(i, new TieData(id, show, srcDesc, destDesc));
        }
    }

    // dummy ties for testing
    private void addDummyTies() {
        for (int i = 0; i < 10; i++) {
            ties.add(i, new TieData(i+1, i % 2 == 0 ? true : false, "source desc", "dest desc"));
        }
    }

    public static void main(String[] args) {
        ManageSectionTiesDialog dlg = new ManageSectionTiesDialog();
        dlg.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        dlg.setVisible(true);
    }
}

class TieData {
    public int id;
    public boolean show;
    public String srcDesc, destDesc;
    public TieData(int id, boolean show, String srcDesc, String destDesc) {
        this.id = id;
        this.show = show;
        this.srcDesc = srcDesc;
        this.destDesc = destDesc;
    }

    public String toString() {
        return "ID: " + id + " Source: " + srcDesc + " Dest: " + destDesc;
    }
}


// Subclassed JTable to override column width handling
class TieTable extends JTable {
    TieTable(TableModel model) {
        super(model);
    }

    final int showWidth = 50;

	@Override
	public void setPreferredSize(final Dimension d) {
		super.setPreferredSize(d);
        setWidths(d.width);
	}

	@Override
	public void setSize(final int width, final int height) {
		super.setSize(width, height);
        setWidths(width);
	}

    private void setWidths(int width) {
		getColumnModel().getColumn(0).setPreferredWidth(showWidth);
        final int half = Math.round((width - showWidth) / 2.0f);
		getColumnModel().getColumn(1).setPreferredWidth(half);
		getColumnModel().getColumn(2).setPreferredWidth(half);
    }
}


// Display and handle checkboxes in "Show" column
class TieTableModel extends AbstractTableModel {
    Vector<TieData> ties;
    TieTableModel(Vector<TieData> ties) {
        super();
        this.ties = ties;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "Show";
        } else if (columnIndex == 1) {
            return "Source Description";
        } else {
            return "Dest Description";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Boolean.class;
        }
        return String.class;
    }

	@Override
	public boolean isCellEditable(final int row, final int col) { return col == 0; }

	@Override
	public void setValueAt(final Object value, final int row, final int col) {
        TieData tie = ties.get(row);
		if (col == 0) {
            tie.show = (boolean)value;
			this.fireTableCellUpdated(row, col);
		}
	}

    @Override public int getColumnCount() { return 3; }
    @Override public int getRowCount() { return ties.size(); }
    @Override public Object getValueAt(final int row, final int col) { 
        TieData t = ties.get(row);
        if (col == 0) {
            return Boolean.valueOf(t.show);
        } else if (col == 1) {
            return t.srcDesc;
        } else { // col == 2
            return t.destDesc;
        }
    }
}