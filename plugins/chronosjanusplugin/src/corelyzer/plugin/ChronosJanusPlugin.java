/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
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

package corelyzer.plugin;

import corelyzer.helper.SceneGraph;
import corelyzer.helper.URLRetrieval;
import corelyzer.ui.CheckBoxList;
import corelyzer.ui.CorelyzerApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

public class ChronosJanusPlugin extends CorelyzerPlugin
        implements ActionListener {
    final float DEFAULT_DPI = 254.0f;

    JFrame pluginFrame;
    JTextField siteField;
    JTextField holeField;
    JButton searchBtn;
    JButton loadBtn;
    JProgressBar progBar;
    JPanel mainpane;

    CorelyzerApp app;

    CheckBoxList queryResultList;
    Vector queryList;
    Vector cbList;

    public boolean init(Component parentUI) {

        app = CorelyzerApp.getApp();

        // build user interface
        queryList = new Vector();
        cbList = new Vector();
        this.buildUI();
        return true;
    }


    public void fini() {

        // do nothing
    }

    public void processEvent(CorelyzerPluginEvent e) {

        // do nothing
    }

    public String getMenuName() {
        return "Janus via Chronos";
    }

    public void addToPopupMenu(JPopupMenu jpm) {
        // do nothing
    }


    private void PositionWidget(JComponent p, JComponent c,
                                SpringLayout l, int x, int y) {
        l.putConstraint(SpringLayout.WEST, c, x, SpringLayout.WEST, p);
        l.putConstraint(SpringLayout.NORTH, c, y, SpringLayout.NORTH, p);
    }

    void buildUI() {
        pluginFrame = new JFrame("Chronos - Janus Plugin");
        pluginFrame.setLocation(300, 200);
        pluginFrame.setSize(400, 550);
        pluginFrame.setVisible(false);
        pluginFrame.setAlwaysOnTop(true);
        
        mainpane = new JPanel();
        mainpane.setLayout(new SpringLayout());

        // SEARCH PARAMETER WIDGETS   ---------------------------------

        JPanel panel = new JPanel(new SpringLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Search Parameters"));
        panel.setPreferredSize(new Dimension(400, 120));
        mainpane.add(panel);
        {
            JLabel label;

            label = new JLabel("Site ");
            panel.add(label);
            PositionWidget(panel, label, (SpringLayout) panel.getLayout(),
                    0, 0);

            siteField = new JTextField("");
            siteField.setPreferredSize(new Dimension(300, 20));
            panel.add(siteField);
            PositionWidget(panel, siteField, (SpringLayout) panel.getLayout(),
                    30, 5);

            searchBtn = new JButton("Search");
            searchBtn.setPreferredSize(new Dimension(300, 25));
            searchBtn.addActionListener(this);
            panel.add(searchBtn);
            PositionWidget(panel, searchBtn, (SpringLayout) panel.getLayout(),
                    30, 60);
        }
        PositionWidget(mainpane, panel, (SpringLayout) mainpane.getLayout(),
                0, 0);

        // SEARCH RESULT WIDGETS    ---------------------------------     

        JScrollPane pane = new JScrollPane();
        pane.setBorder(BorderFactory.createTitledBorder("Search Results"));
        pane.setPreferredSize(new Dimension(400, 280));
        mainpane.add(pane);
        {
            queryResultList = new CheckBoxList();
            queryResultList.setListData(cbList);
            queryResultList.setVisibleRowCount(8);
            queryResultList.setSelectionMode(
                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            pane.getViewport().setView(queryResultList);
        }
        PositionWidget(mainpane, pane, (SpringLayout) mainpane.getLayout(),
                0, 140);

        // LOAD SELECTIONS WIDGETS  ----------------------------------

        loadBtn = new JButton("Load Selections");
        loadBtn.setPreferredSize(new Dimension(380, 25));
        loadBtn.addActionListener(this);
        mainpane.add(loadBtn);
        PositionWidget(mainpane, loadBtn, (SpringLayout) mainpane.getLayout(),
                5, 435);

        progBar = new JProgressBar(0, 1);
        progBar.setPreferredSize(new Dimension(380, 25));
        progBar.setStringPainted(true);
        progBar.setString("");
        mainpane.add(progBar);
        PositionWidget(mainpane, progBar, (SpringLayout) mainpane.getLayout(),
                5, 470);
        pluginFrame.getContentPane().add(mainpane);
    }

    public JFrame getFrame() {
        return pluginFrame;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == searchBtn) {
            Runnable query = new Runnable() {
                public void run() {
                    runQuery();
                }
            };

            (new Thread(query)).start();
        } else if (e.getSource() == loadBtn) {
            Runnable load = new Runnable() {
                public void run() {
                    loadImages();
                }
            };

            (new Thread(load)).start();
        }
    }

    public void runQuery() {
        if (siteField.getText() == null)
            return;
        if (siteField.getText().equals(""))
            return;

        System.out.println("Site: " + siteField.getText());

        URL remote;
        URLConnection uc;
        InputStreamReader isr;
        BufferedReader br;
        String query;

        query = "";
        query = query + "http://services.chronos.org/xqe/public"
                + "/iodp.janus.core-images?callback=displayNexus&"
                + "site=" + siteField.getText()
                //+ "&hole=" + holeField.getText() 
                + "&serializeAs=tsv"
                //+ "&filter=curated_length,ImageURL"
                + "&noHeader=true";

        System.out.println("Query to Send: " + query);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progBar.setString("Getting Results");
                progBar.paintImmediately(0, 0, progBar.getWidth(),
                        progBar.getHeight());                
            }
        });

        // run the query and pull in the data to system.out
        try {
            remote = new URL(query);
            uc = remote.openConnection();
            isr = new InputStreamReader(uc.getInputStream());
            br = new BufferedReader(isr);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    queryList.clear();
                    cbList.clear();
                    queryResultList.updateUI();
                }
            });

            // Line format:
            // Leg	Site	Hole	Core	Section Number	SECTION_ID	Section Type	Core Type	Curated Length	Liner Length	MBSF	FORMAT	DPI	Image URL

            String line;
            // float depth = 0.0f;
            while ((line = br.readLine()) != null) {
                String [] tok = line.split("\t");

                String loc = tok[tok.length - 1];
                float depth = Float.parseFloat(tok[10]);

                final ResultEntry re = new ResultEntry(loc, depth);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        //noinspection unchecked
                        queryList.addElement(re);
                        //noinspection unchecked
                        cbList.addElement(new JCheckBox(re.toString()));

                        queryResultList.updateUI();                        
                    }
                });
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progBar.setString("");                
            }
        });
    }

    public void loadImages() {
        try {
            CorelyzerApp app = CorelyzerApp.getApp();
            final int [] indices = queryResultList.getCheckedIndices();
            if (indices.length <= 0) return;

            System.out.println("Selected " + indices.length + " cores\n");

            float canvas_dpix = SceneGraph.getCanvasDPIX(0);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progBar.setMinimum(0);
                    progBar.setMaximum(indices.length);
                    //progBar.setIndeterminate(true);
                }
            });

            for (int i = 0; i < indices.length; i++) {
                final ResultEntry r = (ResultEntry) queryList.elementAt(indices[i]);
                File f = new File(app.getDownloadDirectoryPath() +
                        r.toString() + ".jpg");

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progBar.setString("Retrieving " + r.toString() + ".jpg");
                        mainpane.paintImmediately(0, 0, mainpane.getWidth(),
                                mainpane.getHeight());                        
                    }
                });

                if (!f.exists()) {
                    // we need to download it from url
                    if (!URLRetrieval.retrieveLocalCopy(
                            r.location, app.getDownloadDirectoryPath() +
                            r.toString() + ".jpg")) {
                        if (JOptionPane.showConfirmDialog(pluginFrame,
                                "Failed to download image file.\n" +
                                        r.location + "\n" +
                                        "Do you want to try it again?",
                                "Image download failed",
                                JOptionPane.YES_NO_OPTION)
                                == JOptionPane.YES_OPTION) {
                            loadImages(); // FIXME reloading over&over
                            return;
                        }

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                progBar.setIndeterminate(false);
                                progBar.setString("");
                                progBar.setValue(0);
                            }
                        });

                        return;
                    } else {
                        f = new File(app.getDownloadDirectoryPath() +
                                r.toString() + ".jpg");
                    }
                }

                System.out.println("SUCCESS");

                // Put Core Catchers on separate track
                boolean isCC = r.toString().endsWith("_cc");
                String trackName;
                int trackId;

                if(isCC) {
                    trackName = siteField.getText() + "-cc";
                } else {
                    trackName = siteField.getText() + "-cores";
                }

                trackId = app.createTrack(trackName);
                System.out.println("Track id is: " + trackId + ", " + isCC);

                // manupilate trackList to desired track
                app.selectTrackByNativeTrackID(trackId);

                SceneGraph.bringTrackToFront(trackId);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progBar.setString("Loading " + r.toString() + ".jpg");
                        mainpane.paintImmediately(0, 0, mainpane.getWidth(),
                                mainpane.getHeight());                        
                    }
                });

                // System.out.println("Calling load: " + f);
                int imgid = app.loadImage(f, r.location);
                if (imgid > -1) {
                    // correct the angle and DPI of the image
                    SceneGraph.setSectionOrientation(trackId, imgid, SceneGraph.PORTRAIT);
                    SceneGraph.setSectionDPI(trackId, imgid, DEFAULT_DPI, DEFAULT_DPI);
                    SceneGraph.bringSectionToFront(trackId, imgid);

                    float px = r.depth * 100.0f / 2.54f * canvas_dpix;
                    SceneGraph.positionSection(trackId, imgid, px, 0);

                    // update the display
                    app.updateGLWindows();
                }

                final int idx = i;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progBar.setValue(idx);
                        mainpane.paintImmediately(0, 0, mainpane.getWidth(),
                                mainpane.getHeight());
                    }
                });
            }

            System.out.println("");

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progBar.setIndeterminate(false);
                    progBar.setString("");
                    progBar.setValue(0);
                }
            });

            // Update the Display
            app.updateGLWindows();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ResultEntry {

        public String location;
        public float depth;

        public String toString() {
            String[] tok = location.split("/");
            tok[tok.length - 1] = tok[tok.length - 1].replace(".jpg", "");
            return tok[tok.length - 1];
        }


        public ResultEntry(String loc, float d) {
            location = loc;
            depth = d;
        }

    }

    public static void main(String[] args) {
        ChronosJanusPlugin plugin = new ChronosJanusPlugin();
        plugin.init(null);
        plugin.getFrame().setVisible(true);
    }

}


