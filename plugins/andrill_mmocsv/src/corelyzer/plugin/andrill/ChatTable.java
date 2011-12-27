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

package corelyzer.plugin.andrill;

import corelyzer.data.ChatGroup;
import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.TableSorter;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

class ChatTableModel extends AbstractTableModel {
    /**
     *  Columns: depths, last_modified, visibility, group
     */
    final int colCount = 4;

    Vector<SessionChat> chatVec;

    public TableModelListener checkListener;

    String s;
    Float f;
    Boolean b;
    Date d;

    public ChatTableModel() {
        chatVec = new Vector<SessionChat>();

        d = new Date();
        s = new String("");
        f = new Float(0);
        b = new Boolean(false);

        checkListener = null;
    }

    public ChatTableModel(Vector<SessionChat> cvec) {
        this();

        chatVec = cvec;
    }

    public int getRowCount() { return chatVec.size(); }

    public int getColumnCount() { return colCount; }

    public Object getValueAt(int row, int col) {
        if(col >= this.getColumnCount() || col < 0) return null;
        if(row >= this.getRowCount() || row < 0) return null;

        switch(col) {
            case 0:  // visibility
                return chatVec.elementAt(row).visibility;

            case 1:  // depth
                return chatVec.elementAt(row).xpos_m;

            case 2:  // last modified
                SimpleDateFormat format = new SimpleDateFormat(
                        "MM/dd/yyy 'at' HH:mm:ss z");

                if(chatVec.elementAt(row).lastModified != -1) {
                    return format.format(chatVec.elementAt(row).lastModified);
                } else {
                    return format.format(0);
                }

            case 3:  // group
            {
                return ChatGroup.getGroupName(chatVec.elementAt(row).group);
            }
        }

        return "Empty";
    }

    public Class getColumnClass(int col) {
        switch(col) {
            case 0: // visibility
                return b.getClass();

            case 1: // depth
                return f.getClass();

            case 2: // last_modified
                return s.getClass();

            case 3: // group
                return s.getClass();
        }

        return s.getClass();
    }

    public boolean isCellEditable(int row, int col) {
        if( chatVec.elementAt(row) == null ) return false;

        // TODO if the core image is not loaded, then
        // visibility will not have immediate visual cue
        if( col == 0) {
//            corelyzer.plugin.andrill.SessionChat sc = chatVec.elementAt(row);
//
//            if( (sc.track == -1) || (sc.local_sectionid == -1) ||
//                (sc.local == -1) )
//            {
//                return false;
//            } else {
//                return true;
//            }
            return true;
        } else {
            return false;
        }
    }

    public void setValueAt(Object value, int row, int col) {
        if(col >= this.getColumnCount() || col < 0) return;
        if(row >= this.getRowCount() || row < 0) return;

        if( chatVec.elementAt(row) == null) return;

        if( col == 0 ) {  // toggle visibility flag
            chatVec.elementAt(row).visibility =
                    !chatVec.elementAt(row).visibility;

            if( checkListener != null)
            {
                checkListener.tableChanged(
                    new TableModelEvent(this, row, row, 0) );
            }
        }

    }
}

//---------------------------------------------------------------------------//

// public class ChatTable extends JTable implements ListSelectionListener {
public class ChatTable extends JTable {

    private ChatTableModel model;
    private TableSorter sorter;
    private ChatCheckListener checkListener;

    public ChatTable(Vector<SessionChat> chatVec,
                     CorelyzerSessionClientPlugin p)
    {
        model  = new ChatTableModel( chatVec );

        sorter = new TableSorter();
        sorter.setTableModel(model);
        sorter.setTableHeader(this.getTableHeader());
        setModel(sorter);
        // setModel(model);

        setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
        this.setHeaderTitles();

        this.checkListener = new ChatCheckListener(p, chatVec);
        model.checkListener = this.checkListener;

        setDragEnabled(true);
        setColumnSelectionAllowed(false);
        setShowHorizontalLines(true);
        getTableHeader().setReorderingAllowed(true);

        //-- Double click action handler
        addMouseListener(new TableMouseListener(sorter, model.chatVec));
    }

    public void setHeaderTitles() {
        getColumnModel().getColumn(0).setHeaderValue("Show");
        getColumnModel().getColumn(1).setHeaderValue("Depth(m)");
        getColumnModel().getColumn(2).setHeaderValue("Last Modify");
        getColumnModel().getColumn(3).setHeaderValue("Group");
    }

    public void setSize( int w, int h) {
        super.setSize( w, h);
        getColumnModel().getColumn(0).setPreferredWidth(25);
    }

    public void setPreferredSize(Dimension d) {
        super.setPreferredSize(d);

        getColumnModel().getColumn(0).setPreferredWidth(10);
        getColumnModel().getColumn(1).setPreferredWidth(10);
        getColumnModel().getColumn(2).setPreferredWidth(d.width - 90);
        getColumnModel().getColumn(3).setPreferredWidth(70);
    }

}

//---------------------------------------------------------------------------//
/** Handle action inside the table */
class ChatCheckListener implements TableModelListener {
    private CorelyzerSessionClientPlugin plugin;
    private Vector< SessionChat > chatVec;

    public ChatCheckListener(CorelyzerSessionClientPlugin p,
                             Vector< SessionChat > cvec)
    {
        plugin = p;
        chatVec = cvec;
    }

    public void tableChanged(TableModelEvent e) {

        // visibility toggle
        SessionChat sc = chatVec.elementAt(e.getFirstRow());

        if( (sc.track == -1) || (sc.local_sectionid == -1) ||
            (sc.local == -1) )
        {
            System.out.println(
                    "Load this chat's track and section image first!");
        } else {
            SceneGraph.setCoreSectionMarkerVisibility(sc.track,
                                                      sc.local_sectionid,
                                                      sc.local, sc.visibility);

            CorelyzerApp.getApp().updateGLWindows();

        }

    }
}

// Listener to handle mouse interactions in the table
class TableMouseListener extends MouseAdapter {

    private TableSorter sorter;
    private Vector< SessionChat > chatVec;

    public TableMouseListener(TableSorter s, Vector< SessionChat > scvec) {
        super();

        sorter  = s;
        chatVec = scvec;
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            JTable target = (JTable) e.getSource();
            int row = sorter.modelIndex(target.getSelectedRow());
            // int row = target.getSelectedRow();

            SessionChat sc = this.chatVec.elementAt(row);

            float px = sc.xpos_m;

            SceneGraph.lock();
            {
                float canvas_dpix = SceneGraph.getCanvasDPIX(0);
                float w_x = canvas_dpix * px * 100.0f / 2.54f;
                float w_y;

                if( sc.local_sectionid < 0) {
                    w_y = SceneGraph.getSceneCenterY();
                } else {
                    w_y = SceneGraph.getSectionYPos(sc.track,
                                                    sc.local_sectionid);
                }

                SceneGraph.positionScene(w_x, w_y);
            }
            SceneGraph.unlock();
            CorelyzerApp.getApp().updateGLWindows();
        }
    }
}
