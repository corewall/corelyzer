/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2008 Julian Yu-Chung Chen
 * Electronic Visualization Laboratory, University of Illinois at Chicago
 *
 * This software is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along
 * with this software; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Questions or comments about CoreWall should be directed to
 * cavern@evl.uic.edu
 *
 *****************************************************************************/
package corelyzer.plugin.correlator;

import corelyzer.data.*;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.data.graph.Tuple;
import corelyzer.helper.SceneGraph;
import corelyzer.helper.URLRetrieval;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.StringUtility;

import javax.swing.*;
import java.io.*;
import java.util.Vector;


public class CorrelatorController {
    CorrelatorDialog view;
    int pluginId;

    public CorrelatorController() {
        super();
    }

    public CorrelatorController(CorrelatorDialog dlg) {
        this();
        view = dlg;
    }

    public void onOK() {
        this.updateInputs();

        // printout data
        Correlator.printoutData();

        if(view.getDebugInfoCheckBox().isSelected()) {
            Correlator.debugPrintOut();
        }

        // dealloc
        Correlator.finish();
    }

    public void onPlot() {
        this.updateInputs();

        String [] holes = Correlator.getHoleNames();
        System.out.println("---> Ploting " + holes.length + " holes");

        //noinspection ConstantConditions
        if(holes != null) {
            for(int i=0; i<holes.length; ++i) {
                String holeName = holes[i];
                System.out.println("---> Plotting hole: " + holeName);

                float [] tuples = Correlator.getHoleData(i);
                if(tuples != null) {
                    // Pack tuples array into linegraph model                    
                    Vector<Tuple> dVTuples = new Vector<Tuple>(tuples.length/2);
                    for(int idx=0; idx<tuples.length; idx+=2) {
                        Tuple t = new Tuple(tuples[idx], tuples[idx+1]);
                        dVTuples.add(t);
                    }

                    float valueMin = 0.0f;
                    float valueMax = 0.0f;
                    for(int idx = 0; idx < dVTuples.size(); idx++) {
                        Tuple t = dVTuples.elementAt(idx);

                        if(idx == 0) {
                            valueMin = t.getValue();
                            valueMax = t.getValue();
                        }

                        if(valueMin > t.getValue()) {
                            valueMin = t.getValue();
                        }

                        if(valueMax < t.getValue()) {
                            valueMax = t.getValue();
                        }
                    }

                    System.out.println("Value Min/Max: " + valueMin + ", " + valueMax);
                    System.out.println("Depth Min/Max: " +
                            dVTuples.firstElement().getDepth() + ", " +
                            dVTuples.lastElement().getDepth());

                    CorelyzerApp app = CorelyzerApp.getApp();
                    if(app != null) {
                        // Prepare data
                        String datasetName = "Correlator-leg-site-hole[" +
                                holeName + "]";
                        String tableName = "Hole-" + holeName;
                        String fieldName = view.getTypeCB().getSelectedItem().
                                toString();
                        int numOfFields = view.getTypeCB().getModel().getSize();

                        System.out.println("---> Field: " + fieldName +
                                "valueMin/valueMax: " +
                                valueMin + ", " + valueMax);

                        int dsId = SceneGraph.addDataset(datasetName);
                        if(dsId < 0) {
                            Correlator.finish();
                            return;
                        }
                        int tableId = SceneGraph.addTable(dsId, tableName);

                        CoreGraph cg = CoreGraph.getInstance();
                        WellLogDataSet ds;
                        WellLogTable   table;
                        boolean isDatasetExist =
                                cg.getCurrentSession().hasDataset(datasetName); 
                        if(isDatasetExist) {
                            ds = cg.getCurrentSession().getDataset(datasetName);
                            table = ds.getTable(tableName);

                            // FIXME
                            SceneGraph.setTableHeightAndFieldCount(dsId,
                                    tableId, dVTuples.size(), numOfFields);

                            // depth using meter in tuples
                            SceneGraph.setTableDepthUnitScale(dsId, tableId,
                                    100); // meter -> cm
                            // FIXME
                        } else { // newly create, just once
                            ds = new WellLogDataSet(datasetName);
                            ds.setId(dsId);

                            table = new WellLogTable(tableName, numOfFields);
                            table.addHeader("section depth"); // column 0
                            ds.addTable(table);

                            SceneGraph.setTableHeightAndFieldCount(dsId,
                                    tableId, dVTuples.size(), numOfFields);

                            // depth using meter in tuples
                            SceneGraph.setTableDepthUnitScale(dsId, tableId,
                                    100); // meter -> cm
                        }
                        table.addHeader(fieldName); // starts from index 1
                        cg.addDataset(cg.getCurrentSession(), ds);

                        System.out.println("---> Insert values in table " +
                                tableName);
                        float depth_offset = 0.0f;
                        int fieldIdx = view.getTypeCB().getSelectedIndex();

                        for(int r=0; r<dVTuples.size(); ++r) {
                            Tuple t = dVTuples.get(r);

                            if(r == 0) {
                                depth_offset = t.getDepth();

                                // TODO same for all fields?
                                table.setDepth_offset(depth_offset);
                                // table.setFieldMinMax(fieldIdx, valueMin, valueMax);
                            }

                            float depth = t.getDepth() - depth_offset;
                            float value = t.getValue();

                            SceneGraph.setTableCell(dsId, tableId, fieldIdx,
                                                    r, true,
                                                    depth, value);
                        }

                        SceneGraph.setFieldMinMax(dsId, tableId, fieldIdx,
                                valueMin, valueMax);

                        // Prepare graph
                        // create new track and sections for the dataset
                        int trackId = app.createTrack(datasetName);
                        if(trackId < 0) {
                            Correlator.finish();
                            return;
                        }
                        
                        int numOfSecs = SceneGraph.getNumSections(trackId);
                        int secId = -1;
                        TrackSceneNode tnode = null;

                        if(isDatasetExist) {
                            for(TrackSceneNode t : cg.getCurrentSession().getTrackSceneNodes()) {
                                if(t.getName().equals(datasetName)) {
                                    tnode = t;
                                }
                            }

                            if(tnode == null) {
                                tnode = cg.getCurrentTrack();
                            }

                            for(CoreSection cs : tnode.getCoreSections()) {
                                if(cs.getName().equals(tableName)) {
                                    secId = cs.getId();
                                }
                            }

                            if(secId == -1) {
                                secId = SceneGraph.addSectionToTrack(trackId, numOfSecs);
                                // offset section
                                SceneGraph.positionSection(trackId, secId,
                                        depth_offset * 100.0f / 2.54f *
                                                SceneGraph.getCanvasDPIX(0), 0);

                                CoreSection sec = new CoreSection(tableName, secId);
                                tnode = cg.getCurrentTrack();
                                tnode.addCoreSection(sec);
                                cg.notifyListeners();

                                sec.setDepth(
                                        SceneGraph.getSectionDepth(trackId, secId));                                
                            }

                            float sectionLength = SceneGraph.getSectionLength(trackId, secId);
                            System.out.println("---> Section Length is: " + sectionLength); // FIXME
                        } else {
                            secId = SceneGraph.addSectionToTrack(trackId,
                                    numOfSecs);
                            SceneGraph.setSectionName(trackId, secId, tableName);

                            // offset section
                            SceneGraph.positionSection(trackId, secId,
                                    depth_offset * 100.0f / 2.54f *
                                            SceneGraph.getCanvasDPIX(0), 0);

                            CoreSection sec = new CoreSection(tableName, secId);
                            tnode = cg.getCurrentTrack();
                            tnode.addCoreSection(sec);
                            cg.notifyListeners();

                            sec.setDepth(
                                    SceneGraph.getSectionDepth(trackId, secId));
                        }

                        // New graph in C
                        System.out.println("---> [DEBUG] Add graph: " +
                                "trackId: " + trackId + ", " +
                                "secId: "   + secId + ", " +
                                "tableId: " + tableId + ", " +
                                "fieldIdx: " + fieldIdx);
                        
                        int gid = SceneGraph.addLineGraphToSection(
                                trackId, secId, dsId, tableId, fieldIdx);

                        System.out.println("---> [DEBUG] GraphId: " + gid);

                        if(gid == -1) {
                            System.err.println("Create graph failed!");

                            Correlator.finish();
                            return;
                        }

                        SceneGraph.setLineGraphRange(gid, valueMin, valueMax);
                        SceneGraph.setLineGraphLabel(gid,
                                view.getTypeCB().getSelectedItem().toString());

                        // new graph in Java
                        CoreSectionGraph csg = new CoreSectionGraph(
                                dsId, tableId, 0, gid, tnode);
                        csg.setName(tableName);
                        tnode.addCoreSectionGraph(csg, secId, gid);

                        app.updateGLWindows();
                    }
                } else {
                    System.err.println(holeName + " has no data tuples");
                }

                System.out.println("---------");
            }
        }
                    
        Correlator.finish();
    }

    public void applyAffine() {
        applyAffineWithLibCore();
        // applyAffineWithJava();
    }

    private void applyAffineWithLibCore() {
        updateInputs(); // including correlator init

        String [] holes = Correlator.getHoleNames();
        System.out.println("---> Processing " + holes.length + " holes");

        //noinspection ConstantConditions
        if( (holes != null) && (holes.length > 0) ) {
            int leg  = Correlator.getLeg(0);
            int site = Correlator.getSite(0);
            String sessionName = "Session-" + leg + "-" + site;

            CorelyzerApp app = CorelyzerApp.getApp();
            if(app != null) {
                // Create Corelyzer Session                
                app.getController().createSession(sessionName);
            }

            for(int i=0; i<holes.length; ++i) {
                int legNumber   = Correlator.getLeg(i);
                int siteNumber  = Correlator.getSite(i);

                String holeName = holes[i];
                System.out.println("---> Leg: " + legNumber +
                                     ", Site: " + siteNumber +
                                     ", Hole: " + holeName);

                String [] cores = Correlator.getCoreNames(i);
                if(cores != null) {
                    for(int j=0; j<cores.length; ++j) {
                        System.out.println("Core: " + holeName + ", " + cores[j]);
                        String coreName = cores[j];
                        String coreType = String.valueOf(
                                Correlator.getCoreType(i, j)).toLowerCase();

                        // Create Corelyzer track
                        int trackId = -1;
                        if(app != null) {
                            String trackName = "Hole-" + holeName + "-" +
                                    coreName;
                            trackId = app.getController().createTrack(trackName);                            
                        }

                        String [] sections = Correlator.getSectionNames(i, j);
                        if(sections != null) {
                            for(int k=0; k<sections.length; ++k) {
                                String section = (sections[k] == null) ?
                                        "null" : sections[k];

                                float[] interval =
                                        Correlator.getSectionInterval(i, j, k);

                                if( (interval != null) &&
                                    (interval.length >= 2) ) {
                                    String sectionName =
                                            siteNumber + holeName.toLowerCase()
                                            + "_" + StringUtility.expandNums(
                                                    coreName, 3) + coreType +
                                                    "_" +
                                                    StringUtility.expandNums(
                                                            section, 2);

                                    String urlPrefix = "http://iodp.tamu.edu/publications/";
                                    String imageURLStr =
                                            urlPrefix + legNumber +
                                            "_IR/VOLUME/CORES/JPEG/" +
                                            siteNumber + holeName + "/" +
                                            siteNumber + holeName.toLowerCase()+
                                            "_" + StringUtility.expandNums(coreName, 3) +
                                            String.valueOf(coreType).toLowerCase() + "/" +
                                            sectionName + ".jpg";

                                    System.out.println(
                                            "---> [INFO] [Check/Download/Load] Section ["
                                            +  sectionName + "][" + imageURLStr
                                            + "] intervale: "
                                            + interval[0] + ", " + interval[1]);

                                    // TODO
                                    // Section name: sectionName
                                    // Download URL: imageURLStr
                                    // Interval:     interval[2]

                                    // Create Corelyzer section
                                    if(app != null) {
                                        String sp = System.getProperty("file.separator");
                                        String source = "IODP";
                                        String aFilePath =
                                                app.getDownloadDirectoryPath() +
                                                sp + source + source + sp +
                                                sessionName +
                                                (new File(imageURLStr)).
                                                        getName();

                                        System.out.println("Local file should be: " +
                                                aFilePath);

                                        File aFile = new File(aFilePath);
                                        if(!aFile.exists()) {
                                            String localFileStr =
                                                    URLRetrieval.retrieveLocalCopy(
                                                    imageURLStr, source,
                                                    sessionName);

                                            if(localFileStr == null) continue;
                                        }

                                        File imgFile = new File(aFilePath);
                                        if (imgFile.exists()) {
                                            int secId = app.loadImage(imgFile,
                                                    imageURLStr);

                                            if (trackId != -1 && secId != -1) {
                                                // FIXME
                                                float depthInPx = interval[0] * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0);
                                                SceneGraph.setSectionOrientation(trackId, secId, SceneGraph.PORTRAIT);
                                                SceneGraph.positionSection(trackId, secId, depthInPx, 0.0f);
                                            }
                                        } else {
                                            System.err.println("---> [WARN] Cannot get " + aFilePath + "; continue");
                                        }
                                    }
                                }
                            } // section-loop
                        }
                    } // core loop
                }
            } // hole loop
        }

        Correlator.finish();
    }

    private Vector<String[]> processAffineXML(File aFile){
        Vector<String[]> ret = new Vector<String[]>();
        // TODO
        System.out.println("TODO reading affine table in XML format");

        JOptionPane.showMessageDialog(view, "TODO");

        return ret;
    }

    private Vector<String[]> processAffinePlainText(File aFile) {
        Vector<String[]> ret = new Vector<String[]>();

        System.out.println("---> [INFO] Reading plain text affine table");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(aFile));

            String line;
            while( (line = reader.readLine()) != null ) {
                String [] toks = line.split("\t");

                for(int i=0; i<toks.length; ++i) {
                    toks[i] = toks[i].trim();
                }

                ret.add(toks);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }    

    private void applyAffineWithJava() {
        String urlPrefix = "http://iodp.tamu.edu/publications/";
        
        System.out.println("---> Apply Affine table ...");
        String affineTablePath = view.getAffineField().getText();

        File affTableFile = new File(affineTablePath);
        if(!affTableFile.exists()) {
            return;
        }

        Vector<String[]> cores = null;
        if(affineTablePath.endsWith("xml")) {
            cores = processAffineXML(affTableFile);
        } else if(affineTablePath.endsWith("table")) {
            cores = processAffinePlainText(affTableFile);
        } else {
            System.err.println("---> Unknown affine table file format: " +
                    affineTablePath);
        }

        if(cores == null) {
            System.err.println("---> Cannot get mapping from input file: " +
                    affineTablePath);

            return;
        }
        
        if(cores.size() == 0) {
            System.out.println("---> [INFO] Zero core entries, just return");
            return;
        }

        System.out.println("---> Working on " + cores.size() + " cores");

        for(String [] toks : cores) { // loop through each core, find sections
            if(toks.length < 10) {
                System.err.println("---> Skip, # of toks: " + toks.length);
                continue;
            }
            
            // [0]leg, [1]site, [2]hole#, [3]core#, [4]coretype,
            // [5]shift,
            // [6]applied only(Y) by previous one(N) [7] null
            // and [8]topDepth & [9]bottomDepth
            // Notice: 2 tabs between Y/N and topDepth columns

            // sample core section image url:
            //   http://iodp.tamu.edu/publications/
            //          207_IR/VOLUME/CORES/JPEG/1258A/1258a_001r/
            //          1258a_001r_01.jpg
            String sectionNamePrefix = toks[1] + toks[2].toLowerCase()
                    + "_" + StringUtility.expandNums(toks[3], 3)
                    + toks[4].toLowerCase() + "_";

            String imageURLStrPrefix = urlPrefix + toks[0] +
                    "_IR/VOLUME/CORES/JPEG/" + toks[1] + toks[2] + "/" +
                    toks[1] + toks[2].toLowerCase() + "_" +
                    StringUtility.expandNums(toks[3], 3) +
                    toks[4].toLowerCase() + "/" + sectionNamePrefix;

            float depth;
            try {
                depth = Float.valueOf(toks[8]);
            } catch(NumberFormatException exception) {
                System.err.println("Number format exception");
                continue;
            }

            // AWwww! section# are in rawdata?!
            for(Integer i : this.getSectionsIds(toks)) {
                String sectionNumber = StringUtility.expandNums(
                        String.valueOf(i), 2);

                String sectionName = sectionNamePrefix + sectionNumber;
                String imageURLStr = imageURLStrPrefix + sectionNumber + ".jpg";

                float withInCoreOffset = 0.0f; // TODO
                float sectionDepth = depth + withInCoreOffset;

                System.out.println("Check/Download/Load\t" + sectionName +
                        "\t@\t" + sectionDepth);

                // TODO 1st find if the images are in current scenegraph
                //      if not, put into download queue

                /*
                int [] ids = SceneGraph.locateImage(sectionName);

                if( (ids != null) && (ids.length >= 2) ) {
                    // apply shifts
                    SceneGraph.positionSection(ids[0], ids[1], sectionDepth, 0.0f);
                } else {
                    Job job = new Job(DOWNLOAD+LOAD+Depth, imageURLStr, sectionDepth);
                    CRDispatcher.submit(job);
                }
                */

            } // end of section-loop
        } // end of core-loop
    }

    private Integer [] getSectionsIds(final String [] coreTokens) {
        Vector<Integer> ret = new Vector<Integer>();
        ListModel model = view.getDatafileList().getModel();

        int numCols = 5;
        String coreStr = "";
        for(int i=0; i<numCols; i++) {
            coreStr += coreTokens[i];
        }

        // loop through all files, FIXME maybe not necessary
        for(int i=0; i<model.getSize(); i++) {
            String s = model.getElementAt(i).toString();
            File dataFile = new File(s);

            if(dataFile.exists()) {
                // System.out.println("---> [INFO] Process data file: " + s);

                try {
                    BufferedReader reader = new BufferedReader(
                            new FileReader(dataFile));

                    String line;
                    while( (line = reader.readLine()) != null ) {
                        String [] toks = line.split("\t");

                        for(int j=0; j<toks.length; ++j) {
                            toks[j] = toks[j].trim();
                        }

                        String dataCoreStr = "";
                        for(int j=0; j<numCols; j++) {
                            dataCoreStr += toks[j];
                        }

                        if(coreStr.equals(dataCoreStr)) {
                            try {
                                int sectionNumber = Integer.valueOf(toks[5]);

                                if(!ret.contains(sectionNumber)) {
                                    ret.addElement(sectionNumber);    
                                }
                            } catch (NumberFormatException exception) {
                                System.out.println("---> Not a number, ignore " + dataCoreStr);
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return ret.toArray(new Integer[ret.size()]);
    }

    private void updateInputs() {
        int coreFormat = view.getFormatCB().getSelectedIndex();
        int coreType = view.getTypeCB().getSelectedIndex();

        String affinePath = view.getAffineField().getText();
        String splicePath = view.getSpliceField().getText();

        boolean initResult = Correlator.init();
        System.out.println("[Correlator] Initialization result: " + initResult);

        // Set format & type
        Correlator.setCoreFormat(coreFormat);
        Correlator.setCoreType(coreType);

        // Load data
        DefaultListModel model = (DefaultListModel)
                view.getDatafileList().getModel();

        for(int i=0; i<model.getSize(); ++i) {
            String path = (String) model.getElementAt(i);
            Correlator.loadData(path);
        }

        // Set affine and splice tables
        Correlator.loadAffineTable(affinePath);
        Correlator.loadSpliceTable(splicePath);

        // execute
        Correlator.updateData();

        boolean hasSplice = !view.getSpliceField().getText().equals("");
        Correlator.execute(hasSplice);
    }

    private void applyFilters() {
        // TODO apply filters
        // Correlator.enableCulling();
        // Correlator.enableDecimation();
        // Correlator.enableGaussian();
    }

    public void setPluginId(int pluginId) {
        this.pluginId = pluginId;
    }
}
