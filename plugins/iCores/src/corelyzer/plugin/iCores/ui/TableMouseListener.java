/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2007 Julian Yu-Chung Chen
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
package corelyzer.plugin.iCores.ui;

import corelyzer.plugin.iCores.cache.CacheEntry;
import corelyzer.plugin.iCores.cache.CacheManager;
import corelyzer.plugin.iCores.cache.ImageLoadEntry;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.CRUtility;
import corelyzer.util.FileUtility;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

public class TableMouseListener implements MouseListener {
    JTable table;

    public TableMouseListener() {
        super();
    }

    public TableMouseListener(JTable aTable) {
        this();
        table = aTable;
    }

    public void mouseClicked(MouseEvent event) {
        if(table == null) return;

        Point p = event.getPoint();
        // int row = table.rowAtPoint(p);
        int [] rows = table.getSelectedRows();

        // int col = table.convertColumnIndexToModel(table.columnAtPoint(p));
        TableModel model = table.getModel();
        
        if (event.isPopupTrigger()) {
        	JPopupMenu menu = popupMenu(model, rows);
    		menu.show(table, p.x, p.y);
        } else {
        	// JOSH: delete this?
        	switch(event.getButton()) {
	        	case MouseEvent.BUTTON1:
	        		// String val = model.getValueAt(row, col).toString();
	        		// System.out.println(
	        		//         "---> [INFO] Mouse clicked r:" + row + " col:" + col
	        		//                + " value: '" + val + "'");
	        		if(event.getClickCount() == 2) {
	        			// TODO double click
	        			System.out.println("---> [INFO] Double-click");
	        		}
	
	        		break;
	
	        	case MouseEvent.BUTTON2:
	        		break;
	
	        	case MouseEvent.BUTTON3:
	        		// right click, should bring up a popupmenu
	        		JPopupMenu menu = popupMenu(model, rows);
	        		menu.show(table, p.x, p.y);
	        		break;
        	}
        }

    }

    private CacheEntry download(String url, String type) {
        System.out.println("---> [INFO] Choose to download '" + url + "'");

        CacheManager cacheMgr = CacheManager.getCacheManager();
        if(cacheMgr == null) {
            JOptionPane.showMessageDialog(ICoreFrame.getIcoreFrame(),
                    "No CacheManager Instance available");
            return null;
        }

        CacheEntry entry;
        if( cacheMgr.hasItem(url) &&
            ((entry = cacheMgr.fetch(url)) != null)
          )
        {
            if(entry.isReady()) {
                // Re-download?
                String mesg = "The Cache already has the " + type +
                        ".\nDo you want to download image '" + url +
                        "' anyway?";
                int answer = JOptionPane.showConfirmDialog(null, mesg);

                if(answer == JOptionPane.NO_OPTION) {
                    return entry;
                }
            } else {
                System.out.println(
                        "---> [INFO] NotReady, put into download thread");
            }
        } else {
            // New entry, ask refresh thread do this right away
            System.out.println("---> Create a new CacheEntry");

            entry = new CacheEntry(url, type);
            cacheMgr.add(url, entry);
        }

        System.out.println("---> Schedule download to " +
                "download Thread");
        cacheMgr.getDownloadThread().add(entry);

        return entry;
    }

    private void loadDataset(String url) {
        // TODO
        CacheManager cacheMgr = CacheManager.getCacheManager();

        if(cacheMgr == null) {
            JOptionPane.showMessageDialog(ICoreFrame.getIcoreFrame(),
                    "No CacheManager Instance available");
            return;
        }

        CacheEntry entry = cacheMgr.fetch(url);
        if(entry == null) {
            entry = download(url, "dataset");
        }

        CorelyzerApp app = CorelyzerApp.getApp();
        if(app == null) {
            JOptionPane.showMessageDialog(ICoreFrame.getIcoreFrame(),
                    "Cannot find Corelyzer rendering screens\n" +
                            "The selected dataset is not loaded.");
            return;
        }

        String sp = System.getProperty("file.separator");
        String datasetFullPath = CacheManager.getCacheManager().getCacheDir() +
                sp + "downloads" + sp + entry.getLocal();
        File dataFile = new File(datasetFullPath);

        if(dataFile.exists()) {
            app.loadData(dataFile);
            // init display dataset?
        } else {
            System.out.println("---> [INFO] Awww, no, still downloading...");
            /*
            Executor e = app.getExecutor(type);
            if(e != null) {
                e.add(aDataSetJob);
            } else {
                System.err.println("---> Unknown type: " + type);
            }
            */
            // cacheMgr.getImgLoadThread().add(e);
        }
    }

    private void loadImage(String url,
                           float depth, float dpi_x, float dpi_y,
                           String orientation, int trackId)
    {
        System.out.println("---> Schedule to Load '" + url + "' @ "
                + depth + " mbsf, with dpis: " + dpi_x + ", " + dpi_y + ", "
                + orientation);

        // Put ImageLoadEntry request into the queue with image & properties
        CacheManager cacheMgr = CacheManager.getCacheManager();

        if(cacheMgr == null) {
            JOptionPane.showMessageDialog(ICoreFrame.getIcoreFrame(),
                    "No CacheManager Instance available");
            return;
        }

        CacheEntry entry = cacheMgr.fetch(url);
        if(entry == null) {
            entry = download(url, "image");
        }

        ImageLoadEntry e = new ImageLoadEntry(entry, trackId, depth,
                                              dpi_x, dpi_y, orientation);
        cacheMgr.getImgLoadThread().add(e);
    }

    private JPopupMenu popupMenu(final TableModel model, final int [] rows) {
        // Context and actions
        JPopupMenu menu = new JPopupMenu();
        // menu.setLabel(model.getValueAt(row, 1).toString());

        JMenuItem item = new JMenuItem("Load...");
        item.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CorelyzerApp app = CorelyzerApp.getApp();
                    if(app == null) {
                        JOptionPane.showMessageDialog(
                                ICoreFrame.getIcoreFrame(),
                                "Cannot find Corelyzer rendering screens\n" +
                                        "The selected image is not loaded.");
                        return;
                    }

                    int trackId = CRUtility.getTargetTrackID(
                            app, ICoreFrame.getIcoreFrame());                    

                    for(int row : rows) {
                        String type = model.getValueAt(row, 1).toString();
                        // System.out.println("The entry row is type: '" + type +
                        //        "' in row " + row);

                        float depth  = Float.valueOf(
                                model.getValueAt(row, 3).toString());
                        float length = Float.valueOf(
                                model.getValueAt(row, 4).toString());
                        String url   = (String) model.getValueAt(row, 6);

                        if(type.toLowerCase().endsWith("image")) {
                            String dpis  = (String) model.getValueAt(row, 5);
                            float dpi_x =
                                    Float.valueOf(dpis.split(",")[0].trim());
                            float dpi_y =
                                    Float.valueOf(dpis.split(",")[1].trim());
                            String orientation =
                                    (String) model.getValueAt(row, 7);

                            // swap dpi_x & dpi_y if the image is vertical
                            if(orientation.equalsIgnoreCase("vertical")) {
                                float tmp = dpi_x;
                                dpi_x = dpi_y;
                                dpi_y = tmp;
                            }

                            loadImage(url, depth, dpi_x, dpi_y, orientation,
                                    trackId);
                        } else if(type.toLowerCase().endsWith("dataset")) {
                            System.out.println("Loading dataset");
                            loadDataset(url);
                        } else {
                            JOptionPane.showMessageDialog(
                                    ICoreFrame.getIcoreFrame(),
                                    "Unknown entry type");
                        }                        
                    }
                }
            }
        );

        menu.add(item);

        item = new JMenuItem("Download");
        item.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (int row : rows) {
                        String url = (String) model.getValueAt(row, 6);

                        String type = model.getValueAt(row, 1).toString();
                        if (type.toLowerCase().endsWith("image")) {
                            type = "image";
                        } else if (type.toLowerCase().endsWith("dataset")) {
                            type = "dataset";
                        }

                        download(url, type);
                    }
                }
            }
        );
        menu.add(item);
        
        final boolean MAC_OS_X =
            (System.getProperty("os.name").toLowerCase().startsWith(
                    "mac os x"));

        item = new JMenuItem("Remove");

        if(rows.length != 0 && rows.length <= 1) {
            int row = rows[0];

            JLabel status = (JLabel) model.getValueAt(row, 0);
            boolean hasLocal = !status.getText().equalsIgnoreCase("n/a");
            item.setEnabled(hasLocal);
        }

        item.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String mesg = "Are you sure you want to remove the\n" +
                            "selected assets and image cache blocks?";

                    Object[] options = {"Cancel", "Yes"};
                    int ans = JOptionPane.showOptionDialog(
                            ICoreFrame.getIcoreFrame(), mesg, "Confirmation",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null,
                            options, options[0]);

                    if (ans == 1) {
                        for(int row : rows) {
                            JLabel status = (JLabel) model.getValueAt(row, 0);
                            boolean isEnabled =
                                    !status.getText().equalsIgnoreCase("n/a");

                            if(isEnabled) {
                                onRemoveItem(model, row);
                            } // else just ignore
                        }
                    }
                }
            }
        );
        menu.add(item);

        boolean hasDownloaded = false;
        for(int row : rows) {
            JLabel status = (JLabel) model.getValueAt(row, 0);
            if(!status.getText().equalsIgnoreCase("n/a")) {
                hasDownloaded = true;
                break;
            }
        }

        item = new JMenuItem("Show in Finder");
        item.setEnabled(hasDownloaded && MAC_OS_X);
        item.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for(int row : rows) {
                        String cacheDir =
                                CacheManager.getCacheManager().getCacheDir();

                        String sp = System.getProperty("file.separator");

                        String fileUrl = (String) model.getValueAt(row, 6);
                        String fileProps = "downloads";
                        String[] toks = fileUrl.split("/");
                        String fileName = toks[toks.length - 1];
                        String filePath = cacheDir + sp + fileProps + sp +
                                fileName;
                        File aFile = new File(filePath);
                        if(!aFile.exists()) return;

                        FileUtility.showFileInFinder(aFile);                        
                    }
                }
            }
        );
        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("GeoRef Location");
        item.setEnabled(false);
        item.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(ICoreFrame.getIcoreFrame(),
                            "Locate it if GeoRef is available");    
                }
            }
        );
        menu.add(item);

        return menu;
    }

    private void onRemoveItem(TableModel model, int row) {
        String sp = System.getProperty("file.separator");
        String cacheDir =
                CacheManager.getCacheManager().getCacheDir();
        String fileUrl = (String) model.getValueAt(row, 6);
        String fileProps = "downloads";
        String[] toks = fileUrl.split("/");
        String fileName = toks[toks.length - 1];
        String filePath = cacheDir + sp + fileProps + sp +
                fileName;
        File aFile = new File(filePath);

        // for imageblocks dir
        int lastDotIdx = fileName.lastIndexOf(".");
        String justName = fileName.substring(0, lastDotIdx);
        String blocksDir = CacheManager.
                getCacheManager().getPrefs().
                texBlock_Directory + sp + justName;

        System.out.println("---> [INFO] To remove '" + filePath +
                "' and '" + blocksDir);

        if (aFile.exists()) {
            try {
                boolean isRemoved = aFile.delete();

                if (isRemoved) {
                    System.out.print("Success (file)!\n");

                    System.out.println("Remove: '" + blocksDir
                            + "'");

                    // Remove imageblocks
                    FileUtility.deleteDirectory(blocksDir);

                    // Remove the item from CacheManager
                    CacheManager.getCacheManager().remove(
                            fileUrl);
                } else {
                    System.out.println("Failed (file)");
                }
            } catch (Exception exp) {
                String msg = "Remove '" + filePath +
                        "' failed:\n" + exp;
                JOptionPane.showMessageDialog(
                        ICoreFrame.getIcoreFrame(), msg);
            }
        }
    }

    public void mousePressed(MouseEvent event) {
    }

    public void mouseReleased(MouseEvent event) {
    }

    public void mouseEntered(MouseEvent event) {
    }

    public void mouseExited(MouseEvent event) {
    }
}
