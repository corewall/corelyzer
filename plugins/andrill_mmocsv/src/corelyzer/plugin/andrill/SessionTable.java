package corelyzer.plugin.andrill;

import java.util.Date;
import java.util.Vector;

public class SessionTable {

    public int id;
    public int globalId;
    
    public Date date;
    public String name;
    public int section;
    public String localfile;

    public Vector< float[] >   values;
    public Vector< boolean[] > valids;
    public int[]  gids;

    public SessionTable(String n) {
        this.name = n;
        values = new Vector< float[] >();
        valids = new Vector< boolean[] >();
    }
};
