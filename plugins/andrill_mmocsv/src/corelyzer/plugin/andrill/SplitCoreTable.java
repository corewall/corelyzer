package corelyzer.plugin.andrill;

import corelyzer.graphics.SceneGraph;
import corelyzer.ui.CorelyzerApp;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

class SplitCoreTableModel extends AbstractTableModel {

    public Vector< SessionSection > sectionVec;
    String s;
    Float f;
    Boolean b;
    String available;
    String notAvailable;

    public TableModelListener checklistener;

    public SplitCoreTableModel(Vector<SessionSection> svec) {
        available = new String("Yes");
        notAvailable = new String("No");

        sectionVec = svec;
        s = new String("");
        f = new Float(0);
        b = new Boolean(false);
        checklistener = null;
    }

    public int getRowCount() { return sectionVec.size(); }

    public int getColumnCount() { return 4; }

    public Object getValueAt(int row, int col) {
        if( sectionVec.elementAt(row) == null)
            return new String("N/A");

        if(col == 0)
            return sectionVec.elementAt(row).name;

        if(col == 1)
            return Float.valueOf(sectionVec.elementAt(row).depth);

        if(col == 2)
            return Boolean.valueOf(sectionVec.elementAt(row).subscribedSplit);

        if(col == 3)
        {
            if( sectionVec.elementAt(row).splitDPIX <= 0 ||
                sectionVec.elementAt(row).splitDPIY <= 0
              )
                return notAvailable;
            else
                return available;
        }

        return new String("");
    }

    public Class getColumnClass(int col) {
        if( col == 0)
            return s.getClass();
        if( col == 1)
            return f.getClass();
        if( col == 2)
            return b.getClass();
        return s.getClass();
    }

    public boolean isCellEditable(int row, int col) {
        if( sectionVec.elementAt(row) == null)
            return false;
        if( col == 2 ) {
            if( /*sectionVec.elementAt(row).subscribedSplit == true || */
                sectionVec.elementAt(row).splitDPIX <= 0 ||
                sectionVec.elementAt(row).splitDPIY <= 0
              )
                return false;
            
            return true;
        }

        return false;
    }

    public void setValueAt(Object value, int row, int col) {
        if( sectionVec.elementAt(row) == null)
            return;

        if( col == 2 ) {
            System.out.println("received setvalueat in splitcoretablemodel");
            if( sectionVec.elementAt(row).subscribedSplit == false)
            {
                sectionVec.elementAt(row).subscribedSplit = true;
                if( checklistener != null)
                {
                    checklistener.tableChanged(
                        new TableModelEvent(this,row,row,2, TableModelEvent.INSERT) );
                }
            }
            else
            {
                // this is the remove request
                sectionVec.elementAt(row).subscribedSplit = false;
                if( checklistener != null)
                {
                    checklistener.tableChanged(
                        new TableModelEvent(this,row,row,2, TableModelEvent.DELETE) );
                }
            }
        }
    }
}

//---------------------------------------------------------------------------//


public class SplitCoreTable extends JTable {

    public SplitCoreTableModel model;
    private SplitCoreCheckListener listener;

    
    public SplitCoreTable(Vector< SessionSection > sectionVec,
                          CorelyzerSessionClientPlugin p) {

        model = new SplitCoreTableModel( sectionVec );
        setModel(model);

        setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
        
        getColumnModel().getColumn(0).setHeaderValue("Section Name");
        getColumnModel().getColumn(1).setHeaderValue("Depth (m)");
        getColumnModel().getColumn(2).setHeaderValue("Display");
        getColumnModel().getColumn(3).setHeaderValue("Avail.");

        listener = new SplitCoreCheckListener(p, sectionVec);

        model.checklistener = listener;

        setDragEnabled(false);
        setColumnSelectionAllowed(false);
        setShowHorizontalLines(true);
        getTableHeader().setReorderingAllowed(false);

        addMouseListener(
            new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        JTable target = (JTable) e.getSource();
                        int row = target.getSelectedRow();
                        // int row = target.getSelectedRow();

                        SplitCoreTableModel model =
                                (SplitCoreTableModel)target.getModel();
                        SessionSection ss = model.sectionVec.elementAt(row);

                        float px = ss.depth;

                        SceneGraph.lock();
                        {
                            float canvas_dpix = SceneGraph.getCanvasDPIX(0);
                            float w_x = canvas_dpix * px * 100.0f / 2.54f;
                            float w_y;

                            if(ss.splitId < 0) {
                                w_y = SceneGraph.getSceneCenterY();
                            } else {
                                w_y = SceneGraph.getSectionYPos(0, ss.splitId);
                            }

                            SceneGraph.positionScene(w_x, w_y);
                        }
                        SceneGraph.unlock();
                        CorelyzerApp.getApp().updateGLWindows();
                    }
                }
            }

        );
    }

    public void setSize( int w, int h) {
        super.setSize( w, h);
        getColumnModel().getColumn(0).setPreferredWidth(150);
    }

    public void setPreferredSize(Dimension d) {
        super.setPreferredSize(d);
        getColumnModel().getColumn(3).setPreferredWidth(30);
        getColumnModel().getColumn(2).setPreferredWidth(60);
        getColumnModel().getColumn(1).setPreferredWidth(75);
        getColumnModel().getColumn(0).setPreferredWidth(d.width - 180);
    }

}

class SplitCoreCheckListener implements TableModelListener {
    private CorelyzerSessionClientPlugin plugin;
    private Vector< SessionSection > sectionVec;

    public SplitCoreCheckListener(CorelyzerSessionClientPlugin p,
                                  Vector< SessionSection > secvec) {
        plugin = p;
        sectionVec = secvec;
    }

    public void tableChanged( TableModelEvent e) {
        // check wether this is display or remove
        if (e.getType() == TableModelEvent.INSERT) {
            plugin.requestSplitCore(e.getFirstRow());
        }
        else {
            // remove section from current scene
            // this only affect on client side
            plugin.deleteSplitCore(e.getFirstRow());
        }
    }

}
