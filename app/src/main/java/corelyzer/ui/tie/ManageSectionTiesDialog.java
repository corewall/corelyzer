package corelyzer.ui.tie;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;

import java.io.FileWriter;
import java.io.IOException;

import java.text.DecimalFormat;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import java.util.Comparator;
import java.util.ArrayList;

import corelyzer.data.CoreSectionTieType;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.FileUtility;

import com.opencsv.CSVWriter;
import net.miginfocom.swing.MigLayout;

public class ManageSectionTiesDialog extends JDialog {
    private JTable tieTable;
    private ArrayList<TieData> ties = new ArrayList<TieData>();
    private JButton editButton;
    private JButton reverseButton;
    private JButton deleteButton;
    private JButton exportButton;
    private JButton closeButton;

    private static ManageSectionTiesDialog dialogSingleton = null;
    public static ManageSectionTiesDialog getDialog() {
        if (dialogSingleton == null) {
            dialogSingleton = new ManageSectionTiesDialog();
        } else {
            dialogSingleton.updateTieData();
        }
        return dialogSingleton;
    }

    private ManageSectionTiesDialog() {
        super();
        setupUI();
        updateTieData();
        setAlwaysOnTop(true);
    }

    public void selectTie(int selectTieId) {
        for (int tie_idx = 0; tie_idx < ties.size(); tie_idx++) {
            if (ties.get(tie_idx).id == selectTieId) {
                tieTable.setRowSelectionInterval(tie_idx, tie_idx);
                tieTable.scrollRectToVisible(tieTable.getCellRect(tie_idx, 0, true));
                break;
            }
        }
    }

    private void setupUI() {
        setTitle("Manage section ties");
        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("insets 5", "[grow]", "[grow][][][]"));
        
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
        buttonPanel.setLayout(new MigLayout("insets 5", "", ""));

        editButton = new JButton("Edit...");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doEditTie();
            }
        });
        buttonPanel.add(editButton);

        reverseButton = new JButton("Reverse Direction");
        reverseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doReverseTie();   
            }
        });
        buttonPanel.add(reverseButton);

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
                final ArrayList<TieData> tiesToDelete = new ArrayList<TieData>();
                for (int rowIdx : rows) { tiesToDelete.add(ties.get(rowIdx)); }
                for (TieData tie : tiesToDelete) {
                    ties.remove(tie);
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
        buttonPanel.add(deleteButton);
        
        exportButton = new JButton("Export Ties to CSV...");
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doExport();
            }
        });
        buttonPanel.add(exportButton);
        
        contentPane.add(buttonPanel, "grow, wrap");
        contentPane.add(new JSeparator(), "grow, wrap");

        closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                SceneGraph.deselectAllSectionTies();
                CorelyzerApp.getApp().updateGLWindows();
                tieTable.clearSelection();
                TieTable.preferredWidth = tableScroll.getWidth();
                TieTable.preferredHeight = tableScroll.getHeight();
                setVisible(false);
            }
        });
        contentPane.add(closeButton, "align right");
        
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

    private void doReverseTie() {
        TieData tie = ties.get(tieTable.getSelectedRow());
        SceneGraph.reverseSectionTieDirection(tie.id);
        TieData updatedTie = getTieData(tie.id);
        ties.set(tieTable.getSelectedRow(), updatedTie);
        tieTable.updateUI();
        CorelyzerApp.getApp().updateGLWindows();
    }

    private void updateButtons() {
        final boolean hasSelection = tieTable.getSelectedRow() != -1;
        final boolean isMultiple = tieTable.getSelectedRows().length > 1;
        editButton.setEnabled(hasSelection && !isMultiple);
        reverseButton.setEnabled(hasSelection && ties.get(tieTable.getSelectedRow()).type == CoreSectionTieType.SPLICE);
        deleteButton.setEnabled(hasSelection);
        exportButton.setEnabled(tieTable.getRowCount() > 0);
    }

    private TieData getTieData(int tieId) {
        final CoreSectionTieType type = CoreSectionTieType.fromInt(SceneGraph.getSectionTieType(tieId));
        final boolean show = SceneGraph.getSectionTieShow(tieId);
        final String aDesc = SceneGraph.getSectionTieADescription(tieId);
        final String bDesc = SceneGraph.getSectionTieBDescription(tieId);
        float[] aPos = SceneGraph.getSectionTieAPosition(tieId);
        float[] bPos = SceneGraph.getSectionTieBPosition(tieId);
        final float ax = aPos[0] / SceneGraph.getCanvasDPIX(0) * 2.54f;
        final float bx = bPos[0] / SceneGraph.getCanvasDPIX(0) * 2.54f;
        final String aSec = SceneGraph.getSectionTieASectionName(tieId);
        final String bSec = SceneGraph.getSectionTieBSectionName(tieId);
        final int aTrackId = SceneGraph.getSectionTieATrack(tieId);
        final int aSectionId = SceneGraph.getSectionTieASection(tieId);
        final int bTrackId = SceneGraph.getSectionTieBTrack(tieId);
        final int bSectionId = SceneGraph.getSectionTieBSection(tieId);
        final float aSectionDepth = SceneGraph.getSectionDepth(aTrackId, aSectionId);
        final float bSectionDepth = SceneGraph.getSectionDepth(bTrackId, bSectionId);
        final float aTotalDepth = (ax + aSectionDepth) / 100.0f;
        final float bTotalDepth = (bx + bSectionDepth) / 100.0f;

        return new TieData(tieId, type, show, aDesc, bDesc, aSec, bSec, ax, bx, aTotalDepth, bTotalDepth);
    }

    public void updateTieData() {
        int[] tieIds = SceneGraph.getSectionTieIds();
        ties.clear();
        for (int i = 0; i < tieIds.length; i++) {
            TieData tieData = getTieData(tieIds[i]);
            ties.add(i, tieData);
        }
        // sort by total depth ascending
        ties.sort(new Comparator<TieData>() {
            public int compare(TieData td1, TieData td2) {
                if (td1.aTotalDepth == td2.aTotalDepth) {
                    return 0;
                } else {
                    return td1.aTotalDepth < td2.aTotalDepth ? -1 : 1;
                }
            }
        });
    }    

    public void gatherTieData(int[] tieIds) {
        ties.clear();
        for (int i = 0; i < tieIds.length; i++) {
            TieData tieData = getTieData(tieIds[i]);
            ties.add(i, tieData);
        }
        // sort by total depth ascending
        ties.sort(new Comparator<TieData>() {
            public int compare(TieData td1, TieData td2) {
                if (td1.aTotalDepth == td2.aTotalDepth) {
                    return 0;
                } else {
                    return td1.aTotalDepth < td2.aTotalDepth ? -1 : 1;
                }
            }
        });
    }

    private void doExport() {
        String exportFile = FileUtility.selectASingleFile(this, "Export Tie Data", "csv", FileUtility.SAVE);
        if (exportFile != null) {
            try {
                CSVWriter writer = new CSVWriter(new FileWriter(exportFile));
                String[] headers = { "Tie Type", "Z Section", "Z Section Depth (cm)", "Z Description", "Z' Section", "Z' Section Depth (cm)", "Z' Description", "Z Total Depth (m)" };
                writer.writeNext(headers);
                for (TieData td : ties) {
                    String[] row = { td.type.toString(), td.aSectionID, DepthFormats.SECTION_DEPTH_FORMAT.format(td.aSectionDepth), td.aDesc, td.bSectionID, DepthFormats.SECTION_DEPTH_FORMAT.format(td.bSectionDepth), td.bDesc, DepthFormats.TOTAL_DEPTH_FORMAT.format(td.aTotalDepth) };
                    writer.writeNext(row);
                }
                writer.close();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Tie data export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

class DepthFormats {
    public static final DecimalFormat SECTION_DEPTH_FORMAT = new DecimalFormat("#.#");
    public static final DecimalFormat TOTAL_DEPTH_FORMAT = new DecimalFormat("#.##");
}

class TieData {
    public int id;
    public CoreSectionTieType type;
    public boolean show;
    public String aDesc, bDesc;
    public String aSectionID, bSectionID; // section name
    public float aSectionDepth, bSectionDepth; // section depth (cm)
    public float aTotalDepth, bTotalDepth; // total depth (m)
    public TieData(int id, CoreSectionTieType type, boolean show, String aDesc, String bDesc, String aSectionID, String bSectionID, float aSectionDepth, float bSectionDepth, float aTotalDepth, float bTotalDepth) {
        this.id = id;
        this.type = type;
        this.show = show;
        this.aDesc = aDesc;
        this.bDesc = bDesc;
        this.aSectionID = aSectionID;
        this.bSectionID = bSectionID;
        this.aSectionDepth = aSectionDepth;
        this.bSectionDepth = bSectionDepth;
        this.aTotalDepth = aTotalDepth;
        this.bTotalDepth = bTotalDepth;
    }

    public String toString() {
        return "ID: " + id + " A: " + aDesc + " B: " + bDesc;
    }
}


// Subclassed JTable to override column width handling
class TieTable extends JTable {
    TieTable(TableModel model) {
        super(model);
    }

    public static int preferredWidth = 600;
    public static int preferredHeight = 200;

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
        return new Dimension(preferredWidth, preferredHeight);
    }

    private void setWidths(int width) {
		getColumnModel().getColumn(0).setPreferredWidth(40);
        getColumnModel().getColumn(1).setPreferredWidth(50);
        getColumnModel().getColumn(2).setPreferredWidth(150);
        getColumnModel().getColumn(3).setPreferredWidth(50);
        getColumnModel().getColumn(4).setPreferredWidth(150);
        getColumnModel().getColumn(5).setPreferredWidth(50);
        getColumnModel().getColumn(6).setPreferredWidth(50);
    }
}


// Display and handle checkboxes in "Show" column
class TieTableModel extends AbstractTableModel {
    ArrayList<TieData> ties;
    TieTableModel(ArrayList<TieData> ties) {
        super();
        this.ties = ties;
    }

    private final String[] columnNames = { "Show", "Type", "Z", "Z Section Depth (cm)", "Z'", "Z' Section Depth (cm)", "Z Total Depth (m)" };

    @Override
    public String getColumnName(int columnIndex) { return columnNames[columnIndex]; }

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

    @Override public int getColumnCount() { return 7; }
    @Override public int getRowCount() { return ties.size(); }
    @Override public Object getValueAt(final int row, final int col) { 
        TieData t = ties.get(row);
        if (col == 0) {
            return Boolean.valueOf(t.show);
        } else if (col == 1) {
            return t.type.toString();
        } else if (col == 2) {
            return t.aSectionID;
        } else if (col == 3) {
            return DepthFormats.SECTION_DEPTH_FORMAT.format(t.aSectionDepth);
        } else if (col == 4) {
            return t.bSectionID;
        } else if (col == 5) {
            return DepthFormats.SECTION_DEPTH_FORMAT.format(t.bSectionDepth);
        } else {
            return DepthFormats.TOTAL_DEPTH_FORMAT.format(t.aTotalDepth);
        }
    }
}