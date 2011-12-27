package corelyzer.plugin.andrill;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Date;
import java.util.Vector;

public class SessionDataset extends DefaultMutableTreeNode {
    public int    id;
    public int    globalId;

    public Date   date;
    public String name;
    public String filename;

    Vector< SessionTable > tables;
    Vector< String > fields;
    Vector< Float > mins;
    Vector< Float > maxs;
    Vector< Float > userMins;
    Vector< Float > userMaxs;
    float[][] colors;
    int[]     types;
    boolean[] display;

    public SessionDataset(String n) {
        super( n, true );
        tables = new Vector< SessionTable >();
        name = new String(n);
        date = new Date(System.currentTimeMillis());
        fields = new Vector< String >();
        mins = new Vector< Float >();
        maxs = new Vector< Float >();
        userMins = new Vector< Float >();
        userMaxs = new Vector< Float >();
    }

    public String toString() {
        return name;
    }

    public String getName() { return name; }

    public void addField(String f) {
        fields.add(f);
        add( new DefaultMutableTreeNode( f, false ) );
    }
}
