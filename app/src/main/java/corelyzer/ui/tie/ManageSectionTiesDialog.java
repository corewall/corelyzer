package corelyzer.ui.tie;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import net.miginfocom.swing.MigLayout;

public class ManageSectionTiesDialog extends JFrame {
    private JList<TieData> tieList;
    private DefaultListModel<TieData> ties = new DefaultListModel<TieData>();
    private JButton showHideButton;
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

        contentPane.setLayout(new MigLayout("", "[grow]", ""));
        tieList = new JList<TieData>(ties);
        JScrollPane tieListScroll = new JScrollPane(tieList);
        contentPane.add(tieListScroll, "wmin 300, hmin 100, hmax 120, wrap");

        tieList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    updateShowHideButton();
                }
            }
        });

        showHideButton = new JButton("Show");
        showHideButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (tieList.getSelectedIndex() == -1) return;
                TieData tie = tieList.getSelectedValue();
                SceneGraph.setSectionTieShow(tie.id, !tie.show);
                CorelyzerApp.getApp().updateGLWindows();
                tie.show = !tie.show;
                updateShowHideButton();
            }
        });
        contentPane.add(showHideButton);

        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final int sel_idx = tieList.getSelectedIndex();
                TieData tie = tieList.getSelectedValue();
                ties.removeElement(tie);
                final int new_sel = ties.size() > 0 ? Math.min(ties.size()-1, sel_idx) : -1;
                tieList.setSelectedIndex(new_sel);
                tieList.updateUI();
                SceneGraph.deleteSectionTie(tie.id);
                CorelyzerApp.getApp().updateGLWindows();
            }
        });
        contentPane.add(deleteButton);

        closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setVisible(false);
            }
        });
        contentPane.add(closeButton);
        
        pack();
        updateShowHideButton();
    }

    private void updateShowHideButton() {
        final boolean enable = tieList.getSelectedIndex() != -1;
        showHideButton.setEnabled(enable);   
        deleteButton.setEnabled(enable);
        if (!enable) return;

        TieData cur = tieList.getSelectedValue();
        showHideButton.setText(cur.show ? "Hide" : "Show");
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