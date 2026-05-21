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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import corelyzer.data.*;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.FileUtility;
import corelyzer.util.StringUtility;
import corelyzer.util.identity.*;

import com.opencsv.CSVWriter;
import net.miginfocom.swing.MigLayout;

public class ManageSectionTiesDialog extends JDialog {
    private JTable tieTable;
    private ArrayList<TieData> ties = new ArrayList<TieData>();
    private JButton editButton;
    private JButton reverseButton;
    private JButton deleteButton;
    private JButton exportButton;
    private JButton exportSparseSpliceButton;
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

        exportSparseSpliceButton = new JButton("Export Sparse Splice CSV...");
        exportSparseSpliceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSparseSpliceExport();
            }
        });
        buttonPanel.add(exportSparseSpliceButton);
        
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
            CorelyzerApp.getApp().updateGLWindows();
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
        reverseButton.setEnabled(hasSelection && !isMultiple && ties.get(tieTable.getSelectedRow()).type == CoreSectionTieType.SPLICE);
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
        tieTable.updateUI();
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

    private void showSparseSpliceError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Sparse Splice Error", JOptionPane.WARNING_MESSAGE);
        StringUtility.setClipboard(msg); // copy error text to clipboard for clunky fixage
    }

    private TieData getSpliceStartTie(Vector<TieData> startTieCandidates) {
        TieData startTie = null;
        if (startTieCandidates.size() == 0) { // no candidates
            showSparseSpliceError("Couldn't find a starting splice section, which must have one outgoing splice tie and no incoming splice ties.");
        } else if (startTieCandidates.size() == 1) { // one candidate, yay
            final String msg = "Starting splice from " + startTieCandidates.get(0).aSectionID;
            startTie = startTieCandidates.get(0);
            JOptionPane.showMessageDialog(this, msg, "Sparse Splice", JOptionPane.WARNING_MESSAGE);
        } else if (startTieCandidates.size() > 1) { // multiple candidates, user must choose one
            String[] startTieOptions = new String[startTieCandidates.size()];
            for (int i = 0; i < startTieCandidates.size(); i++) {
                TieData td = startTieCandidates.get(i);
                startTieOptions[i] = td.aSectionID + " at " + DepthFormats.SECTION_DEPTH_FORMAT.format(td.aSectionDepth) + " cm";
            }
            
            final String msg = "Select the starting tie point for the splice.";
            String choice = (String) JOptionPane.showInputDialog(this, msg, "Select Start Tie", JOptionPane.QUESTION_MESSAGE, null, startTieOptions, startTieOptions[0]);
            if (choice != null) {
                // System.out.println("User selected: " + choice);
                final int selectedIndex = Arrays.asList(startTieOptions).indexOf(choice);
                startTie = startTieCandidates.get(selectedIndex);
            }
        }
        return startTie;
    }

    private Vector<TieData> inferSpliceSequence(TieData startTie, final HashMap<String, Vector<TieData>> outgoingTiesBySection) {
        Vector<TieData> spliceTies = new Vector<TieData>();
        spliceTies.add(startTie);

        // follow tie sequence from startTie
        boolean done = false;
        while (!done) {
            TieData cur = spliceTies.lastElement();
            Vector<TieData> v = outgoingTiesBySection.get(cur.bSectionID);

            for (TieData td : v) { // bail on an intra-section tie
                if (td.aSectionID.equals(td.bSectionID)) {
                    showSparseSpliceError("Section " + td.aSectionID + " contains a tie to itself, I can't go for that (no can do!).");
                    return null;
                }
            }

            if (v.size() == 0) {
                done = true;

                // Current section has no outgoing ties. Search downhole for a section with exactly one
                // outgoing tie. If one exists, add it to sequence and continue. If none, we're done.
                // If 2+, report a conflict.
                System.out.println("No outgoing ties for " + cur.bSectionID + ", looking downhole...");

                // Get current section's index in TrackSceneNode's list of CoreSections
                final int curTieTrackID = SceneGraph.getSectionTieBTrack(cur.id);
                TrackSceneNode track = null;
                for (Session s : CoreGraph.getInstance().getSessions()) {
                    if (s.getTrackSceneNodeWithTrackId(curTieTrackID) != null) {
                        track = s.getTrackSceneNodeWithTrackId(curTieTrackID);
                        break;
                    }
                }
                if (track == null) {
                    showSparseSpliceError("Error: Couldn't find matching track for scenegraph ID " + curTieTrackID);
                    return null;                    
                }
                
                CoreSection sec = track.getCoreSection(cur.bSectionID);
                int secIdx = track.getCoreSectionIndex(sec);
                if (secIdx == -1) {
                    showSparseSpliceError("Error: Couldn't find index of section " + cur.bSectionID + " in expected parent track " + track.getName());
                    return null;
                }

                // Search downhole sections for outgoing ties
                for (int deeperSectionIndex = secIdx + 1; deeperSectionIndex < track.getNumCores(); deeperSectionIndex++) {
                    CoreSection deeperSection = track.getCoreSection(deeperSectionIndex);
                    System.out.println("      " + deeperSection.getName());
                    if (outgoingTiesBySection.containsKey(deeperSection.getName())) {
                        Vector<TieData> deeperSectionTies = outgoingTiesBySection.get(deeperSection.getName());
                        if (deeperSectionTies.size() == 0) {
                            System.out.println("No outgoing ties, continuing");
                            continue;
                        } else if (deeperSectionTies.size() == 1) { // found a winner, add to splice ties and continue main loop
                            System.out.println("Exactly one outgoing tie, adding to splice sequence and continuing main loop");
                            spliceTies.add(deeperSectionTies.get(0));
                            done = false;
                            break;
                        } else if (deeperSectionTies.size() > 1) {
                            System.out.println("2+ outgoing ties, reporting error and bailing");
                            final String msg = "Found multiple outgoing ties in downhole section " + deeperSection.getName() + ".\nPlease resolve and try again.";
                            showSparseSpliceError(msg);
                            return null;
                        }
                    }
                }
            } else if (v.size() == 1) {
                spliceTies.add(v.get(0));
            } else if (v.size() > 1) {
                // System.out.println("ERROR: More than one outgoing tie in " + cur.bSectionID);
                final String msg = "Found multiple outgoing ties in section " + cur.bSectionID + ".\nPlease resolve and try again.";
                showSparseSpliceError(msg);
                return null;
            }
        }
        return spliceTies;
    }

    // Session could contain multiple splices...for now just try to find one.
    private Vector<TieData> createSparseSplice() {
        SectionSpliceTieAggregator sstAgg = new SectionSpliceTieAggregator(ties);
        Vector<TieData> startTieCandidates = sstAgg.getStartTies();
        TieData startTie = getSpliceStartTie(startTieCandidates);
        if (startTie == null) { return null; }

        Vector<TieData> spliceTies = inferSpliceSequence(startTie, sstAgg.outgoing);
        if (spliceTies == null) { return null; }

        System.out.println("Inferred splice tie sequence");
        for (TieData td : spliceTies) { System.out.println(td); }

        return spliceTies;
    }

    private float promptForDepth(String msg) {
        float depth = 0.0f;
        boolean valid = false;
        while (!valid) {
            String depthStr = JOptionPane.showInputDialog(this, msg);
            try {
                depth = Float.parseFloat(depthStr);
                valid = true;
            } catch (NumberFormatException e) {
                showSparseSpliceError(depthStr + " is not a number");
            }
        }
        return depth;
    }

    private TrackSceneNode getTieBTrack(final int id) {
        // Get current section's index in TrackSceneNode's list of CoreSections
        final int curTieTrackID = SceneGraph.getSectionTieBTrack(id);
        TrackSceneNode track = null;
        for (Session s : CoreGraph.getInstance().getSessions()) {
            if (s.getTrackSceneNodeWithTrackId(curTieTrackID) != null) {
                track = s.getTrackSceneNodeWithTrackId(curTieTrackID);
                break;
            }
        }
        return track;
    }

    private Vector<CoreSection> getCoreSectionsInRange(TrackSceneNode track, String startSectionID, String endSectionID) {
        Vector<CoreSection> sectionsBetween = new Vector<CoreSection>();
        CoreSection startSection = track.getCoreSection(startSectionID);
        CoreSection endSection = track.getCoreSection(endSectionID);
        if (startSection == null || endSection == null) { return null; }

        int startIdx = track.getCoreSectionIndex(startSection);
        int endIdx = track.getCoreSectionIndex(endSection);

        if (startIdx > endIdx) {
            int temp = endIdx;
            endIdx = startIdx;
            startIdx = temp;
            System.out.println("Swapping startSection and endSection indices, this should not happen!");
        }

        for (int i = startIdx; i <= endIdx && i < track.getNumCores(); i++) {
            sectionsBetween.add(track.getCoreSection(i));
        }
        
        return sectionsBetween;
    }

    // Create SparseSpliceIntervals based on inferred splice tie sequence, merging intervals from
    // the same core into a single SparseSpliceInterval.
    private Vector<SparseSpliceInterval> createSparseSpliceIntervals(Vector<TieData> spliceTies, float startDepth, float endDepth) {
        SectionIDParser parser = SectionIDParserFactory.getMatchingParser(spliceTies.get(0).aSectionID);
        Vector<SparseSpliceInterval> intervals = new Vector<SparseSpliceInterval>();
        for (int i = 0; i < spliceTies.size(); i++) {
            TieData td = spliceTies.get(i);
            TieData next_td = (i < spliceTies.size() - 1) ? spliceTies.get(i+1) : null;

            if (i == 0) { // first interval: user-provided top offset to start of first tie
                SparseSpliceInterval interval = new SparseSpliceInterval(td.aSectionID, parser, startDepth, td.aSectionDepth, "TIE");
                intervals.add(interval);
            }

            if (next_td != null) {
                String bsec = td.bSectionID; // incomingSec? inSec?
                if (td.bSectionID.equals(next_td.aSectionID)) {
                    // outgoing tie is in current section, yay!
                    SparseSpliceInterval interval = new SparseSpliceInterval(bsec, parser, td.bSectionDepth, next_td.aSectionDepth, "TIE");
                    intervals.add(interval);
                } else {
                    // outgoing tie is in a downhole section, tricky!
                    System.out.println("No outgoing tie in " + bsec + ". Next downhole outgoing tie is in " + next_td.aSectionID + ". Appending intervening sections.");
                    Vector<SparseSpliceInterval> intervalsBetween = new Vector<SparseSpliceInterval>();
                    
                    // add all intervening sections at their full length
                    TrackSceneNode track = getTieBTrack(td.id);
                    Vector<CoreSection> sectionsBetween = getCoreSectionsInRange(track, bsec, next_td.aSectionID);
                    for (int sbIdx = 0; sbIdx < sectionsBetween.size(); sbIdx++) {
                        CoreSection curSectionBetween = sectionsBetween.get(sbIdx);
                        SparseSpliceInterval intBetween = null; 
                        if (sbIdx < sectionsBetween.size() - 1) {
                            float topOffset = (sbIdx == 0) ? td.bSectionDepth : 0.0f;
                            intBetween = new SparseSpliceInterval(curSectionBetween.getName(), parser, topOffset, SparseSpliceInterval.UNKNOWN_BOTTOM_OFFSET, "APPEND");
                        } else {
                            intBetween = new SparseSpliceInterval(curSectionBetween.getName(), parser, 0.0f, next_td.aSectionDepth, "TIE");
                        }
                        intervalsBetween.add(intBetween);
                    }
                    // reduce rows in the same section to a single row
                    Vector<SparseSpliceInterval> mergedIntervals = mergeByCore(intervalsBetween);
                    intervals.addAll(mergedIntervals);
                }
            } else { // last tie
                SparseSpliceInterval interval = new SparseSpliceInterval(td.bSectionID, parser, td.bSectionDepth, endDepth, "");
                intervals.add(interval);
            }
        }
        return intervals;
    }

    private Vector<SparseSpliceInterval> mergeByCore(Vector<SparseSpliceInterval> intervals) {
        Vector<SparseSpliceInterval> result = new Vector<SparseSpliceInterval>();
        // brg 3/17/2026: Approach below is cleaner and more readable
        // int idx = 0;
        // boolean done = false;

        // Pretty sure intervals to merge can be gathered with just a for loop and no while insanity,
        // then merged and added to result after the loop by using a Vector<Vector<SparseSpliceInterval>>...
        // while (!done && idx < intervals.size()) {
        //     SparseSpliceInterval cur = intervals.get(idx);
        //     Vector<SparseSpliceInterval> commonCoreIntervals = new Vector<SparseSpliceInterval>();
        //     commonCoreIntervals.add(cur);
        //     if (idx + 1 == intervals.size()) { // last interval, we're done
        //         done = true;
        //     }

        //     for (int next_idx = idx+1; next_idx < intervals.size(); next_idx++) {
        //         SparseSpliceInterval next = intervals.get(next_idx);
        //         if (cur.coresEqual(next)) {
        //             commonCoreIntervals.add(next);
        //             idx = next_idx + 1;
        //         } else {
        //             idx = next_idx;
        //             break;
        //         }
        //     }

        //     if (commonCoreIntervals.size() == 1) {
        //         result.addAll(commonCoreIntervals);
        //     } else {
        //         // merge intervals: top members come from first interval, bottom and spliceType come from last
        //         SparseSpliceInterval first = commonCoreIntervals.firstElement();
        //         SparseSpliceInterval last = commonCoreIntervals.lastElement();
        //         SparseSpliceInterval merged = SparseSpliceInterval.merge(first, last);
        //         result.add(merged);
        //     }
        // }

        // group SparseSpliceIntervals by common core
        Vector<Vector<SparseSpliceInterval>> intervalGroups = new Vector<Vector<SparseSpliceInterval>>();
        Vector<SparseSpliceInterval> currentGroup = new Vector<SparseSpliceInterval>();
        SparseSpliceInterval prev = null;
        for (int i = 0; i < intervals.size(); i++) {
            SparseSpliceInterval cur = intervals.get(i);
            if (prev != null && !cur.coresEqual(prev)) {
                intervalGroups.add(currentGroup);
                currentGroup = new Vector<SparseSpliceInterval>();
            }
            currentGroup.add(cur);
            prev = cur;
        }
        intervalGroups.add(currentGroup);

        // add all intervals to result, merging any common core sequences
        for (Vector<SparseSpliceInterval> group : intervalGroups) {
            if (group.size() > 1) {
                SparseSpliceInterval merged = SparseSpliceInterval.merge(group.firstElement(), group.lastElement());
                result.add(merged);
            } else {
                result.add(group.firstElement());
            }
        }

        return result;
    }

    private void doSparseSpliceExport() {
        try {
            Vector<TieData> spliceTies = createSparseSplice();
            if (spliceTies == null) { return; }

            // got a confict-free tie sequence, prompt user for start and end depths of first/last sections
            float startDepth = promptForDepth("Enter the splice's starting section depth (cm):");
            float endDepth = promptForDepth("Enter the splice's ending section depth (cm):");

            Vector<SparseSpliceInterval> intervals = createSparseSpliceIntervals(spliceTies, startDepth, endDepth);

            String exportFile = FileUtility.selectASingleFile(this, "Export Sparse Splice", "csv", FileUtility.SAVE);
            if (exportFile != null) {
                try {
                    CSVWriter writer = new CSVWriter(new FileWriter(exportFile));
                    String[] headers = { "Site", "Hole", "Core", "Tool", "Top Section", "Top Offset", "Bottom Section", "Bottom Offset", "Splice Type" };
                    writer.writeNext(headers);
                    for (SparseSpliceInterval interval : intervals) { writer.writeNext(interval.toSparseSpliceRow()); }
                    writer.close();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Sparse Splice export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Sparse Splice export cancelled.", "Export Cancelled", JOptionPane.INFORMATION_MESSAGE);
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
        // return "ID: " + id + " A: " + aDesc + " B: " + bDesc;
        return aSectionID + " @ " + aSectionDepth + " -> " + bSectionID + " @ " + bSectionDepth;
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

class SparseSpliceInterval {
    public String site;
    public String hole;
    public String core;
    public String tool;
    public String topSection;
    public float topOffset;
    public String bottomSection;
    public float bottomOffset;
    public String spliceType;

    // Corelyzer can not reliably calculate a section's bottom depth due to presence of extraneous imagery
    // (color cards, labels etc) at the bottom of core images and/or inaccuracies in core image resolution.
    // For splice intervals spanning the boundary of two cores, this placeholder will be used for BottomOffset.
    // The Feldman app will use Section Summary data to replace this placeholder with the correct section bottom
    // depth when converting a Sparse Splice to a Splice Interval Table (SIT).
    public static final float UNKNOWN_BOTTOM_OFFSET = -1.0f;

    public SparseSpliceInterval(String site, String hole, String core, String tool, String topSection, float topOffset, String bottomSection, float bottomOffset, String spliceType) {
        this.site = site;
        this.hole = hole;
        this.core = core;
        this.tool = tool;
        this.topSection = topSection;
        this.topOffset = topOffset;
        this.bottomSection = bottomSection;
        this.bottomOffset = bottomOffset;
        this.spliceType = spliceType;
    }

    public SparseSpliceInterval(String sectionID, SectionIDParser parser, float topOffset, float bottomOffset, String spliceType) {
        this.site = parser.site(sectionID);
        this.hole = parser.hole(sectionID);
        this.core = parser.core(sectionID);
        this.tool = parser.tool(sectionID);
        this.topSection = parser.section(sectionID);
        this.bottomSection = parser.section(sectionID);
        this.topOffset = topOffset;
        this.bottomOffset = bottomOffset;
        this.spliceType = spliceType;
    }

    public static SparseSpliceInterval merge(SparseSpliceInterval s1, SparseSpliceInterval s2) {
        return new SparseSpliceInterval(s1.site, s1.hole, s1.core, s1.tool, s1.topSection, s1.topOffset, s2.bottomSection, s2.bottomOffset, s2.spliceType);
    }

    public boolean coresEqual(SparseSpliceInterval cmp) {
        return this.site.equals(cmp.site) && this.hole.equals(cmp.hole) && this.core.equals(cmp.core) && this.tool.equals(cmp.tool);
    }

    public String[] toSparseSpliceRow() {
        String[] row = {
            site, hole, core, tool,
            topSection, DepthFormats.SECTION_DEPTH_FORMAT.format(topOffset),
            bottomSection,
            DepthFormats.SECTION_DEPTH_FORMAT.format(bottomOffset),
            spliceType
        };
        return row;
    }
}

class SectionSpliceTieAggregator {
    public HashMap<String, Vector<TieData>> incoming;
    public HashMap<String, Vector<TieData>> outgoing;
    public HashSet<String> sectionIDs;

    public SectionSpliceTieAggregator(ArrayList<TieData> ties) {
        outgoing = new HashMap<String, Vector<TieData>>();
        incoming = new HashMap<String, Vector<TieData>>();
        sectionIDs = new HashSet<String>();

        // find all sections with 1+ ties in either direction
        for (TieData td : ties) {
            if (td.type == CoreSectionTieType.SPLICE) {
                sectionIDs.add(td.aSectionID);
                sectionIDs.add(td.bSectionID);
            }
        }

        // separate into outgoing and incoming ties keyed on section ID
        for (String sec_id : sectionIDs) {
            Vector<TieData> ogs = new Vector<TieData>();
            Vector<TieData> ics = new Vector<TieData>();
            for (TieData td : ties) {
                if (td.type == CoreSectionTieType.SPLICE) {
                    if (td.aSectionID.equals(sec_id)) { ogs.add(td); }
                    if (td.bSectionID.equals(sec_id)) { ics.add(td); }
                }
            }
            outgoing.put(sec_id, ogs);
            incoming.put(sec_id, ics);
        }
    }

    // return starting splice tie candidates sorted by total depth
    public Vector<TieData> getStartTies() {
        // find ties for sections with exactly 1 outgoing and 0 incoming ties
        Vector<TieData> spliceStartTies = new Vector<TieData>();
        for (String sec_id : sectionIDs) {
            if (outgoing.get(sec_id).size() == 1 && incoming.get(sec_id).size() == 0) {
                spliceStartTies.add(outgoing.get(sec_id).get(0));
            }
        }
        spliceStartTies.sort(new Comparator<TieData>() {
            public int compare(TieData td1, TieData td2) {
                return Double.compare(td1.aTotalDepth, td2.aTotalDepth);
            }
        });
        return spliceStartTies;
    }
}