package corelyzer.plugin.andrill;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CorelyzerSessionLog {

    private String      logFileName     = null;
    private PrintWriter pw              = null;
    private SimpleDateFormat timeFormat = null;
    private SimpleDateFormat dateFormat = null;
    private String[]    typeStringArr   = null;
    private static final int numTypes   = 15;
    
    // LOG TYPES
    public static final byte START          = 0;
    public static final byte STOP           = 1;
    public static final byte CONNECTIONOPEN = 2;
    public static final byte CONNECTIONCLOSE= 3;
    public static final byte LOGIN          = 4;
    public static final byte LOGOUT         = 5;
    public static final byte SECTION_NEW    = 6;
    public static final byte GRAPH_NEW      = 7;
    public static final byte SPLITCORE      = 8;
    public static final byte WHOLECORE      = 9;
    public static final byte CHAT_NEW       = 10;
    public static final byte CHAT_ENTRY     = 11;
    public static final byte IMAGE          = 12;
    public static final byte STARTUP        = 13;
    public static final byte SHUTDOWN       = 14;
    
    //============================================
    CorelyzerSessionLog() {
        // set time stamp format
        timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        
        // log file name
        logFileName = new String("log/");
        dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date now = new Date(System.currentTimeMillis());
        logFileName = logFileName + dateFormat.format(now) + ".log";
        
        // build type string array
        typeStringArr = new String[numTypes];
        typeStringArr[START]            = new String("LOG_START");
        typeStringArr[STOP]             = new String("LOG_STOP");
        typeStringArr[CONNECTIONOPEN]   = new String("CONNEC_OPEN");
        typeStringArr[CONNECTIONCLOSE]  = new String("CONNEC_CLOSE");
        typeStringArr[LOGIN]            = new String("LOGIN");
        typeStringArr[LOGOUT]           = new String("LOGOUT");
        typeStringArr[SECTION_NEW]      = new String("SECTION_NEW");
        typeStringArr[GRAPH_NEW]        = new String("GRAPH_NEW");
        typeStringArr[SPLITCORE]        = new String("SPLITCORE");
        typeStringArr[WHOLECORE]        = new String("WHOLECORE");
        typeStringArr[CHAT_NEW]         = new String("CHAT_NEW");
        typeStringArr[CHAT_ENTRY]       = new String("CHAT_ENTRY");
        typeStringArr[IMAGE]            = new String("IAMGE");
        typeStringArr[STARTUP]          = new String("STARTUP");
        typeStringArr[SHUTDOWN]         = new String("SHUTDOWN");
    }
    
    //============================================
    public void start() {
        // check existence of log file
        boolean bNewFile = true;
        File dir = new File("log");
        
        if (dir.exists() == false) {
            dir.mkdir();
        }
        else {
            File testFile = new File(logFileName);
            if (testFile.exists())
                bNewFile = false;
        }
    
        if (pw != null) {
            pw.close();
            pw = null;
        }

        try {
            pw = new PrintWriter(new FileWriter(logFileName, true));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    
        // print log file header information
        if (bNewFile) {
            this.writeln("CoreWall Log File ");
            this.writeln();
            this.writeln("--------------------------------------------------------------------------------");
            this.writeln("TYPE            TIME                    USER        LOG");
            this.writeln("--------------------------------------------------------------------------------");
        }
        this.writeType( "START");
        this.writeTime();
        this.writeUser("system");
        this.writeln("logging started");
    }
    
    //============================================
    public void stop() {
        if (pw == null)
            return;
    
        this.writeLog(STOP, "system", "logging stopped");
        pw.close();
        pw = null;
    }
    
    //============================================
    public void createNewLogFile() {
        try {
            pw = new PrintWriter(new FileWriter(logFileName, true));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    
        this.writeln("CoreWall Session Log File ");
        this.writeln();
        this.writeln("--------------------------------------------------------------------------------");
        this.writeln("TYPE            TIME                    USER        LOG");
        this.writeln("--------------------------------------------------------------------------------");
    }
    
    //============================================
    public void checkDate() {
        String currDate = new String("log/");
        Date now = new Date(System.currentTimeMillis());
        currDate = currDate + dateFormat.format(now) + ".log";
        if (!currDate.equals(logFileName)) {
            pw.close();
            logFileName = currDate;
            this.createNewLogFile();
        }
    }
    
    //============================================
    public void writeLog(byte type, String user, String message) {
        
        checkDate();
        
        if (type > numTypes)
            return;
        
        this.writeType(typeStringArr[type]);
        this.writeTime();
        this.writeUser(user);
        this.writeln(message);
    }
    
    //============================================
    public void writeln(String s) {
        pw.println(s);
        pw.flush();
    }
    
    //============================================
    public void writeln() {
        pw.println();
        pw.flush();
    }
    
    //============================================
    public void writeType(String type) {
        // 12 character space and tab delim
        pw.printf("%1$-12s\t", type);
    }
    
    //============================================
    public void writeTime() {
        // 20 character space and tab delim
        Date now = new Date(System.currentTimeMillis());
        pw.printf("%1$-20s\t", timeFormat.format(now));
    }
    
    //============================================
    public void writeUser(String user){
        // 10 character space and tab delim
        pw.printf("%1$-10s\t", user);
    }
}
