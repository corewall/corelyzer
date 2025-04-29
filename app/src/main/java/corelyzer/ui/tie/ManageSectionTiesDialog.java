package corelyzer.ui.tie;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;

import java.text.DecimalFormat;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import java.util.Vector;

import corelyzer.data.CoreSectionTieType;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import net.miginfocom.swing.MigLayout;

public class ManageSectionTiesDialog extends JFrame {
    private JTable tieTable;
    private Vector<TieData> ties = new Vector<TieData>();
    private JButton editButton;
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
        contentPane.setLayout(new MigLayout("insets 5", "[grow]", "[grow][]"));
        
        // tie table
        tieTable = new TieTable(new TieTableModel(ties));
        tieTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    SceneGraph.deselectAllSectionTies();
                    for (int rowIdx : tieTable.getSelectedRows()) {
                        TieData tie = ties.get(rowIdx);
                        SceneGraph.selectSectionTie(tie.id, true);
                    }
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
        contentPane.add(tableScroll, "wmin 400, hmin 100, wrap, grow");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new MigLayout("insets 5", "[][][grow]", ""));

        editButton = new JButton("Edit");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doEditTie();
            }
        });
        buttonPanel.add(editButton, "align left");

        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final int[] rows = tieTable.getSelectedRows();
                if (rows.length > 1) {
                    final String msg = "Do you want to delete the selected ties?";
                    final String title = "Delete Multiple Ties?";
                    int result = JOptionPane.showConfirmDialog(tieTable.getParent(), msg, title, JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.NO_OPTION) {
                        return;
                    }
                }

                // delete ties
                final Vector<TieData> tiesToDelete = new Vector<TieData>();
                for (int rowIdx : rows) { tiesToDelete.add(ties.get(rowIdx)); }
                for (TieData tie : tiesToDelete) {
                    ties.removeElement(tie);
                    SceneGraph.deleteSectionTie(tie.id);
                }

                // select another row if possible
                final int new_sel = ties.size() > 0 ? Math.min(ties.size()-1, rows[0]) : -1;
                if (new_sel != -1) {
                    tieTable.setRowSelectionInterval(new_sel, new_sel);
                    TieData selectedTie = ties.get(tieTable.getSelectedRow());
                    SceneGraph.selectSectionTie(selectedTie.id, true);
                } else {
                    tieTable.clearSelection();
                }
                tieTable.updateUI();
                updateButtons();
                CorelyzerApp.getApp().updateGLWindows();
            }
        });
        buttonPanel.add(deleteButton, "align left");


        closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                SceneGraph.deselectAllSectionTies();
                CorelyzerApp.getApp().updateGLWindows();
                setVisible(false);
            }
        });
        buttonPanel.add(closeButton, "align right");
        contentPane.add(buttonPanel, "grow");
        
        pack();
        updateButtons();
    }

    private void doEditTie() {
        TieData tie = ties.get(tieTable.getSelectedRow());
        SectionTieDialog tieDlg = new SectionTieDialog(CorelyzerApp.getApp().getMainFrame(), tie.id, false);
        tieDlg.setModal(true);
        tieDlg.setLocationRelativeTo(this);
        tieDlg.setVisible(true);
        if (tieDlg.confirmed) {
            tie.aDesc = tieDlg.getADesc();
            tie.bDesc = tieDlg.getBDesc();
            SceneGraph.setSectionTieADescription(tie.id, tie.aDesc);
            SceneGraph.setSectionTieBDescription(tie.id, tie.bDesc);
            tie.type = tieDlg.getTieType();
            SceneGraph.setSectionTieType(tie.id, tie.type.intValue());
            tieTable.updateUI();
        }
    }

    private void updateButtons() {
        final boolean hasSelection = tieTable.getSelectedRow() != -1;
        final boolean isMultiple = tieTable.getSelectedRows().length > 1;
        editButton.setEnabled(hasSelection && !isMultiple);
        deleteButton.setEnabled(hasSelection);
    }
    
    private void gatherTieData(int[] tieIds) {
        for (int i = 0; i < tieIds.length; i++) {
            final int id = tieIds[i];
            final CoreSectionTieType type = CoreSectionTieType.fromInt(SceneGraph.getSectionTieType(id));
            final boolean show = SceneGraph.getSectionTieShow(id);
            final String aDesc = SceneGraph.getSectionTieADescription(id);
            final String bDesc = SceneGraph.getSectionTieBDescription(id);
            float[] aPos = SceneGraph.getSectionTieAPosition(id);
            float[] bPos = SceneGraph.getSectionTieBPosition(id);
            final float ax = aPos[0] / SceneGraph.getCanvasDPIX(0) * 2.54f;
            final float bx = bPos[0] / SceneGraph.getCanvasDPIX(0) * 2.54f;
            final String aSec = SceneGraph.getSectionTieASectionName(id);
            final String bSec = SceneGraph.getSectionTieBSectionName(id);
            ties.add(i, new TieData(id, type, show, aDesc, bDesc, aSec, bSec, ax, bx));
        }
    }

    // dummy ties for testing
    private void addDummyTies() {
        for (int i = 0; i < 10; i++) {
            ties.add(i, new TieData(i+1, CoreSectionTieType.NONE, i % 2 == 0 ? true : false, "source desc", "dest desc", "Section A", "Section B", 10, 20));
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
    public CoreSectionTieType type;
    public boolean show;
    public String aDesc, bDesc;
    public String aSectionID, bSectionID; // section name
    public float aSectionDepth, bSectionDepth; // section depth (cm)
    public TieData(int id, CoreSectionTieType type, boolean show, String aDesc, String bDesc, String aSectionID, String bSectionID, float aSectionDepth, float bSectionDepth) {
        this.id = id;
        this.type = type;
        this.show = show;
        this.aDesc = aDesc;
        this.bDesc = bDesc;
        this.aSectionID = aSectionID;
        this.bSectionID = bSectionID;
        this.aSectionDepth = aSectionDepth;
        this.bSectionDepth = bSectionDepth;
    }

    public String toString() {
        return "ID: " + id + " A: " + aDesc + " B: " + bDesc;
    }

    public static String makeDepthStr(final String sectionID, final float sectionDepth) {
        DecimalFormat df = new DecimalFormat("#.#");
        return sectionID + " " + df.format(sectionDepth) + "cm";
    }
}


// Subclassed JTable to override column width handling
class TieTable extends JTable {
    TieTable(TableModel model) {
        super(model);
    }

    final int showWidth = 50;
    final int typeWidth = 50;

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

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(500, 200);
    }

    private void setWidths(int width) {
		getColumnModel().getColumn(0).setPreferredWidth(showWidth);
        getColumnModel().getColumn(1).setPreferredWidth(typeWidth);
        final int half = Math.round((width - (showWidth + typeWidth)) / 2.0f);
		getColumnModel().getColumn(2).setPreferredWidth(half);
		getColumnModel().getColumn(3).setPreferredWidth(half);
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
            return "Type";
        } else if (columnIndex == 2) {
            return "Z";
        } else {
            return "Z'";
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

    @Override public int getColumnCount() { return 4; }
    @Override public int getRowCount() { return ties.size(); }
    @Override public Object getValueAt(final int row, final int col) { 
        TieData t = ties.get(row);
        if (col == 0) {
            return Boolean.valueOf(t.show);
        } else if (col == 1) {
            return t.type.toString();
        } else if (col == 2) {
            return TieData.makeDepthStr(t.aSectionID, t.aSectionDepth);
        } else { // col == 3
            return TieData.makeDepthStr(t.bSectionID, t.bSectionDepth);
        }
    }
}