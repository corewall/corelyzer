package corelyzer.plugin.andrill;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class CorelyzerSessionServer {

    private ServerSocket server;
    private boolean      listening;
    private String copy;
    private LinkedList<ClientConnectionThread> clientThreads;

    private Semaphore sectionListFileLock;
    private Semaphore splitCoreListFileLock;
    private Semaphore wholeCoreListFileLock;
    private Semaphore discussionListFileLock;
    private Semaphore datasetListFileLock;

    private static final String listDir = "./lists/";
    private static final String dataDir = "./dataset/";

    private static final String sectionListTempFile = listDir +
        "global_section_listing.tab.temp";
    private static final String sectionListFile =  listDir +
        "global_section_listing.tab";

    private static final String splitCoreListTempFile = listDir +
        "global_splitCore_listing.tab.temp";
    private static final String splitCoreListFile = listDir +
        "global_splitCore_listing.tab";

    private static final String wholeCoreListTempFile = listDir +
        "global_wholeCore_listing.tab.temp";
    private static final String wholeCoreListFile = listDir +
        "global_wholeCore_listing.tab";

    private static final String datasetListTempFile = listDir +
        "global_dataset_listing.tab.temp";
    private static final String datasetListFile = listDir +
        "global_dataset_listing.tab";
    
    private static final String discussionListTempFile = listDir +
        "global_chat_listing.tab.temp";
    private static final String discussionListFile = listDir +
        "global_chat_listing.tab";
    

    private static SharedMiceThread mouseThread;
    private static CorelyzerSessionServer        sessionServer;

    public  static HashMap< Integer, ClientConnectionThread>
        userIdThreadTable;

    private String annotationRootURL;
    private String annotationRootDIR;
    private String annotationBackupDIR;

    private long   lastBackup;
    private Date   lastBackupDate;
    private boolean backingup;
    private boolean backupenabled;
    
    private CorelyzerSessionLog sessionLogger;

    //===============
    CorelyzerSessionServer() {
        File f = new File("server-settings.txt");
        if( !f.exists() ) {
            System.out.println("NEED server-settings.txt file!");
            System.exit(0);
        }

        File fListDir = new File(listDir);
        File fDataDir = new File(dataDir);

        if(!fListDir.exists()) {
            fListDir.mkdir();
        }

        if(!fDataDir.exists()) {
            fDataDir.mkdir();
        }

        backingup = false;
        backupenabled = false;

        try {
            BufferedReader br = new BufferedReader( new FileReader(f));
            annotationRootURL = br.readLine();
            if( annotationRootURL == null ) throw new Exception();
            annotationRootDIR = br.readLine();
            if( annotationRootDIR == null ) throw new Exception();
            annotationBackupDIR = br.readLine();
            if( annotationBackupDIR == null ) throw new Exception();

            lastBackup = Long.parseLong( br.readLine() );
            lastBackupDate = new Date(lastBackup);
            System.out.println("Last Backup: " + lastBackupDate);
            
            // optional backup
            String backupoption = br.readLine();
            if (backupoption.equals("backupenabled"))
                backupenabled = true;
            else if (backupoption.equals("backupdisabled"))
                backupenabled = false;

        } catch (Exception e) {
            System.out.println("Failed to load server settings. Please check"
                               + " server-settings.txt to make sure it is ok."
                );
            System.exit(0);
        }

        if( System.getProperty("os.name").contains("Windows") )
            copy = new String("cmd.exe /c copy ");
        else
            copy = new String("cp ");

        DataManager.init();
        sessionServer = this;
        System.out.println("---SYSTEM STARTING---");
        listening = true;

        discussionListFileLock = new Semaphore(1,true);
        wholeCoreListFileLock  = new Semaphore(1,true);
        splitCoreListFileLock  = new Semaphore(1,true);
        sectionListFileLock    = new Semaphore(1,true);
        datasetListFileLock    = new Semaphore(1,true);

        userIdThreadTable = new HashMap< Integer, ClientConnectionThread>();
        // client linked list
        clientThreads = new LinkedList<ClientConnectionThread>();

        // rebuild the session
        buildSession();

        try {
            // TCP server socket
            server = new ServerSocket(11999);
            System.out.println("Server Listening to port 11999.");

            mouseThread = new SharedMiceThread(11998);
            System.out.println("Mouse Pos Thread on port 11998.");
            
            sessionLogger = new CorelyzerSessionLog();
            sessionLogger.start();

            mouseThread.start();
            runServerLoop();

        }
        catch( Exception e ) {
            e.printStackTrace();
        }
        
    }

    //===============
    public void shutdown() {
        listening = false;
        mouseThread.shutdown();
    }

    //===============
    public static CorelyzerSessionServer getServer(){
        return sessionServer;
    }

    //===============
    public void freeClient(Object o) {
        try {
            clientThreads.remove(o);
        }
        catch( Exception e) {
            e.printStackTrace();
        }
    }

    //===============
    public int getNumClients() {
        return clientThreads.size();
    }

    //===============
    public ClientConnectionThread getConnectionThreadByUserID(int id) {
        for( int i = 0; i < clientThreads.size(); i++) {
            ClientConnectionThread cct = clientThreads.get(i);
            if(cct.getClientData().getUserID() == id)
                return cct;
        }
        return null;
    }

    //===============
    public ClientConnectionThread getConnectionThread(int index) {
        try {
            return clientThreads.get(index);
        }
        catch( Exception e ) {
            return null;
        }
    }

    //===============
    private void runServerLoop() {
        try {
            server.setSoTimeout(10000);
            System.out.println("\tSYSTEM RUNNING & LISTENING");
            
            // log: STARTUP
            writeLog(CorelyzerSessionLog.STARTUP, "system", "server started");
            
            while( listening ) {
                Socket s;
                try {
                    s = server.accept();
                    System.out.println(
                        "Connection accepted. Creating Thread.");
                    clientThreads.add( new ClientConnectionThread(
                                       s, clientThreads.size()) );
                    clientThreads.get( clientThreads.size() - 1).start();
                    // log
                    writeLog(CorelyzerSessionLog.CONNECTIONOPEN,
                                    "system", "connection created: \t" +
                                    clientThreads.get( clientThreads.size() - 1).getClientIP() );
                }
                catch( SocketTimeoutException e ) {

                    // has one day passed up?
                    if( System.currentTimeMillis() - lastBackup > 76400000 
                        && !backingup)
                    {
                        backingup = true;

                        try {
                            if (backupenabled)
                                runBackupProcess();
                                
                            lastBackup = System.currentTimeMillis();
                            FileWriter fw = new FileWriter(
                                "server-settings.txt.temp");
                            fw.write( annotationRootURL, 0, 
                                      annotationRootURL.length());
                            fw.write('\n');
                            fw.write( annotationRootDIR, 0,
                                      annotationRootDIR.length());
                            fw.write('\n');
                            fw.write( annotationBackupDIR, 0,
                                      annotationBackupDIR.length());
                            
                            fw.write('\n');
                            String last = new String("" + lastBackup);
                            fw.write( last, 0, last.length());
                            
                            fw.write('\n');
                            String backupoption = new String("");
                            if (backupenabled)
                                backupoption = backupoption + "backupenabled";
                            else
                                backupoption = backupoption + "backupdisabled";
                            fw.write(backupoption, 0, backupoption.length());
                            
                            fw.flush();
                            fw.close();
                            Runtime.getRuntime().exec(
                                copy + "server-settings.txt.temp " +
                                "server-settings.txt");
                            System.out.println("FINISHED BACKING SETTINGS");
                        } catch( Exception ex) {
                            ex.printStackTrace();
                            System.out.println(
                                "FAILED TO BACKUP AUTOMATICALLY");
                        }
                        
                        backingup = false;
                    }
                }
            }
        
            System.out.println("--- SHUTTING DOWN ---");
            System.out.println("\tCLOSING CONNECTIONS");
            // close connections
            for( int i = 0; i < clientThreads.size(); i++) {
                clientThreads.get(i).setCloseConnection();
            }
            
            System.out.println("\tWAITING FOR CLIENT THREADS TO FINISH");
            // join the clientThreads
            for( int i = 0; i < clientThreads.size(); i++) {
                clientThreads.get(i).join();
            }
            
            server.close();
            
            // log: SHUTDOWN
            writeLog(CorelyzerSessionLog.SHUTDOWN, "system", "server shut down");
            sessionLogger.stop();
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    //=======================
    public void runBackupProcess() {
        try {
            System.out.println("NEED TO RUN BACKUP OPERATIONS");
            lastBackup = System.currentTimeMillis();
            Calendar c = Calendar.getInstance();
            String dir = new String("backup_");
            dir = dir + c.get(Calendar.DAY_OF_MONTH) + "_"
                + c.get(Calendar.MONTH) + "_" + 
                c.get(Calendar.YEAR) + "/";
            File f = new File(".");
            File[] kids = f.listFiles();
            String mkdir;
            if( System.getProperty("os.name").contains("Windows") )
                mkdir = new String("cmd.exe /c mkdir ");
            else
                mkdir = new String("mkdir ");

            //Runtime.getRuntime().exec("mkdir " + dir);
            Runtime.getRuntime().exec(mkdir + dir);
            int i;
            for( i = 0; i < kids.length; i++) {
                if( kids[i].getName().contains(".tab"))
                {
                    Runtime.getRuntime().exec(
                        copy + " " + kids[i].getName() +
                        " " + dir + "/" + kids[i].getName());
                }
            }
            
            // backup annotations
            dir = annotationBackupDIR + "/" + dir;
            f = new File(annotationRootDIR);
            kids = f.listFiles();
            //Runtime.getRuntime().exec("mkdir " + dir);
            Runtime.getRuntime().exec(mkdir + dir);
            for( i = 0; i < kids.length; i++) {
                System.out.print("Backup " + kids[i].getName() + "???");
                if( kids[i].getName().contains(".htm"))
                {
                    System.out.println(" - YES");
                    Runtime.getRuntime().exec( copy + " " + annotationRootDIR 
                                               + "/" + kids[i].getName() +
                                               " " + dir + "/" +
                                               kids[i].getName());
                }
                else
                    System.out.println(" - NO");
            }

        } catch( Exception e) {
            e.printStackTrace();
            System.out.println("FAILED TO MAKE BACKUP COPIES");
        }
        
        System.out.println("FINISHED BACKING DATAFILES");

    }

    //=======================
    public static void main(String[] args) {
        new CorelyzerSessionServer();
    }


    //======================
    public void addUserToTable(Integer id, ClientConnectionThread thread) {
        try {
            userIdThreadTable.put( id, thread);
            System.out.println("ADDED USER " + id );
        } catch (Exception e ) {
            e.printStackTrace();
        }
    }

    //======================    
    public void removeUserFromTable(Integer id) {
        try {
            userIdThreadTable.remove(id);
            System.out.println("REMOVED USER " + id);
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    //=====================
    public ClientConnectionThread lookupUserTable(Integer id) {
        try {
            System.out.println("LOOKING UP USER " + id);
            ClientConnectionThread cct = userIdThreadTable.get(id);
            return cct;
        } catch( Exception e ) {
            return null;
        }
    }

    //=====================
    public void appendSectionListFile(int secId) {
        try {

            sectionListFileLock.acquire();

            File f = new File( sectionListTempFile );
            FileWriter fw = new FileWriter( f, true );

            String name = DataManager.getSectionName(secId);
            String depth = new String("") + DataManager.getSectionDepth(secId);
            String len = new String("") + DataManager.getSectionLength(secId);
            
            String buf;

            buf = new String("" + secId);
            fw.write(buf,0,buf.length());
            fw.write('\t');

            buf = name;
            fw.write(buf,0,buf.length());
            fw.write('\t');
            
            buf = depth;
            fw.write(buf,0,buf.length());
            fw.write('\t');
            
            buf = len;
            fw.write(buf,0,buf.length());
            fw.write('\n');
            fw.flush();
            fw.close();


            Runtime.getRuntime().exec( copy + sectionListTempFile + " " + 
                                       sectionListFile );

            sectionListFileLock.release();

        } catch ( Exception e ) {
            sectionListFileLock.release();
            e.printStackTrace();
            System.out.println("FAILED TO SAVE CHANGES TO SECTION LIST FILE");
        }

    }

    //=====================
    public void appendSplitCoreListFile(int secId) {
        try {
            splitCoreListFileLock.acquire();

            String url = DataManager.getSplitCoreURL(secId);
            String dpi_x = String.valueOf(DataManager.getSplitCoreDPIX(secId));
            String dpi_y = String.valueOf(DataManager.getSplitCoreDPIY(secId));
            
            String buf;

            File f = new File( splitCoreListTempFile );
            FileWriter fw = new FileWriter( f, true );

            buf = new String("" + secId);
            fw.write( buf, 0, buf.length() );
            fw.write('\t');

            buf = url;
            fw.write( buf, 0, buf.length() );
            fw.write('\t');

            buf = dpi_x;
            fw.write( buf, 0, buf.length() );
            fw.write('\t');

            buf = dpi_y;
            fw.write( buf, 0, buf.length() );
            fw.write('\n');
            fw.flush();
            fw.close();

            Runtime.getRuntime().exec( copy + splitCoreListTempFile + " " 
                                       + splitCoreListFile );

            splitCoreListFileLock.release();

        } catch ( Exception e ) {
            splitCoreListFileLock.release();
            e.printStackTrace();
            System.out.println("FAILED TO SAVE CHANGES TO SECTION LIST FILE");
        }

    }


    //=====================
    public void appendWholeCoreListFile(int secId) {
        try {
            wholeCoreListFileLock.acquire();

            String url = DataManager.getWholeCoreURL(secId);
            String dpi = new String("") + DataManager.getWholeCoreDPI(secId);
            
            String buf;

            File f = new File( wholeCoreListTempFile );
            FileWriter fw = new FileWriter( f, true );

            buf = new String("" + secId);
            fw.write( buf, 0, buf.length() );
            fw.write('\t');

            buf = url;
            fw.write( buf, 0, buf.length() );
            fw.write('\t');

            buf = dpi;
            fw.write( buf, 0, buf.length() );
            fw.write('\n');
            fw.flush();
            fw.close();

            Runtime.getRuntime().exec( copy + wholeCoreListTempFile + " " 
                                       + wholeCoreListFile );

            wholeCoreListFileLock.release();

        } catch ( Exception e ) {
            wholeCoreListFileLock.release();
            e.printStackTrace();
            System.out.println("FAILED TO SAVE CHANGES TO SECTION LIST FILE");
        }
    }

    //==================================================
    public void appendDatasetListFile(int setId) {
        SessionDataset sd = DataManager.getDataset(setId);
        if( sd == null ) return;

        try {
            datasetListFileLock.acquire();
            FileWriter fw = new FileWriter( datasetListTempFile, true);
            
            String buf;
            buf = sd.name;

            fw.write( buf, 0, buf.length() );
            fw.write('\t');

            buf = new String("" + sd.fields.size() );
            fw.write( buf, 0, buf.length() );
            fw.write('\t');

            for( int i = 0; i < sd.fields.size(); i++) {
                buf = sd.fields.elementAt(i);
                fw.write(buf,0,buf.length());
                fw.write('\t');
                buf = new String("" + sd.mins.elementAt(i));
                fw.write(buf,0,buf.length());
                fw.write('\t');
                buf = new String("" + sd.maxs.elementAt(i));
                fw.write(buf,0,buf.length());
                fw.write('\t');
            }

            fw.write('\n');
            fw.flush();
            fw.close();

            Runtime.getRuntime().exec( copy + " " + datasetListTempFile +
                                       " " + datasetListFile );

            datasetListFileLock.release();
        } catch( Exception e ) {
            datasetListFileLock.release();
            e.printStackTrace();
            System.out.println("FAILED TO SAVE CHANGES TO DATASET LIST FILE");
        }

    }

    //===================================================
    public String getAnnotationRootURL() { return annotationRootURL; }
    
    //===================================================
    public String getAnnotationRootDIR() { return annotationRootDIR; }

    //===================================================
    public void appendChatListFile(int split, int secId, int chatId) {

        String url = DataManager.getChatURL( split, secId, chatId );
        String local = DataManager.getChatLocal( split, secId, chatId);
        int    uid = DataManager.getChatCreator( split, secId, chatId );
        int groupid= DataManager.getChatGroup( split, secId, chatId);
        int typeid= DataManager.getMarkerType( split, secId, chatId);
        float  posx = DataManager.getChatDepthX( split, secId, chatId );
        float  posy = DataManager.getChatDepthY( split, secId, chatId );
        long   created = DataManager.getChatCreated( split, secId, chatId );

        if( url == null )
            return;
        
        try {
            discussionListFileLock.acquire();
            FileWriter fw = new FileWriter( discussionListTempFile, true);

            String buf;

            buf = new String("" + secId);
            fw.write( buf, 0, buf.length() );
            fw.write("\t");

            buf = new String("" + split);
            fw.write( buf, 0, buf.length() );
            fw.write("\t");

            buf = new String("" + chatId);
            fw.write( buf, 0, buf.length() );
            fw.write("\t");

            buf = new String("" + uid );
            fw.write( buf, 0, buf.length() );
            fw.write("\t");

            buf = new String("" + groupid );
            fw.write( buf, 0, buf.length() );
            fw.write("\t");

            buf = new String("" + typeid );         // new addition: marker type
            fw.write( buf, 0, buf.length() );
            fw.write("\t");

            buf = new String("" + posx );
            fw.write( buf, 0, buf.length() );
            fw.write("\t");

            buf = new String("" + posy );           // new addition: y depth
            fw.write( buf, 0, buf.length() );
            fw.write("\t");

            buf = new String("" + created);
            fw.write( buf, 0, buf.length() );
            fw.write("\t");

            fw.write( local, 0, local.length() );
            fw.write("\t");

            fw.write( url, 0, url.length() );
            fw.write("\n");
            fw.flush();
            fw.close();

            //Runtime.getRuntime().exec( copy + " " + discussionListTempFile +
            Runtime.getRuntime().exec( copy + discussionListTempFile +
                                       " " + discussionListFile );

            discussionListFileLock.release();

        } catch (Exception e) {
            discussionListFileLock.release();
            e.printStackTrace();
            System.out.println("FAILED TO SAVE CHANGES TO CHAT LIST FILE");
        }
    }
    
    //===================================================
    public void writeLog(byte type, String user, String message) {
        sessionLogger.writeLog(type, user, message);
    }
    
    //===================================================
    private void buildSession() {
        try {
            // sections list
            File f = new File( sectionListFile );
            if( f.exists() == false )
                return;

            BufferedReader br = new BufferedReader( new FileReader(f));
            
            String line;
            while( (line = br.readLine()) != null ) {
                String[] toks = line.split("\t");
                if( toks.length != 4 )
                    continue;
                float depth = Float.parseFloat( toks[2] );
                float length = Float.parseFloat( toks[3] );
                DataManager.addSection( toks[1], depth, length );
            }
            
            br.close();

            // split core list
            f = new File( splitCoreListFile );
            if( f.exists() )
                loadSplitCores(f);

            // whole core list
            f = new File( wholeCoreListFile );
            if( f.exists() )
                loadWholeCores(f);

            f = new File( datasetListFile );
            if( f.exists() )
                loadDatasetInfo(f);

            f = new File( discussionListFile );
            if( f.exists() )
                loadChatInfo(f);

        } catch ( Exception e ) {
            System.out.println("ERROR: Couldn't build the session. " +
                               " Please make sure the data files are correct");
            System.exit(1);
        }
    }

    
    //================================================
    private void loadSplitCores(File f) {
        try {
            BufferedReader br = new BufferedReader( new FileReader(f));
            String line;
            while( (line = br.readLine()) != null) {
                String[] toks = line.split("\t");
                if( toks.length != 4 )
                    continue;

                int secid = Integer.parseInt( toks[0] );
                float dpi_x = Float.parseFloat( toks[2] );
                float dpi_y = Float.parseFloat( toks[3] );

                String name = DataManager.getSectionName( secid );
                if( name == null )
                    continue;
                
                DataManager.setSplitCoreURL( name, toks[1] );
                DataManager.setSplitCoreDPI( name, dpi_x, dpi_y );                
            }

        } catch( Exception e) {
            System.out.println("Had to stop reading split core listing!");
            e.printStackTrace();
        }
    }


    //================================================
    private void loadWholeCores(File f) {
        try {
            BufferedReader br = new BufferedReader( new FileReader(f));
            String line;
            while( (line = br.readLine()) != null) {
                String[] toks = line.split("\t");
                if( toks.length != 3 )
                    continue;

                int secid = Integer.parseInt( toks[0] );
                float dpi = Float.parseFloat( toks[2] );

                String name = DataManager.getSectionName( secid );
                if( name == null )
                    continue;
                
                DataManager.setWholeCoreURL( name, toks[1] );
                DataManager.setWholeCoreDPI( name, dpi );
            }

        } catch( Exception e) {
            System.out.println("Had to stop reading whole core listing!");
            e.printStackTrace();
        }
    }

    //===================================================
    private void loadDatasetInfo(File f) {
        try {
            BufferedReader br = new BufferedReader( new FileReader(f));
            String line;
            while( (line = br.readLine()) != null ) {
                String[] toks;
                toks = line.split("\t");
                SessionDataset sd = new SessionDataset(toks[0]);
                int fields = Integer.parseInt( toks[1] );
                int i;
                for( i = 0; i < fields; i++) {
                    sd.fields.add( toks[ i * 3 + 2] );
                    sd.mins.add( new Float( toks[ i * 3 + 3 ] ) );
                    sd.maxs.add( new Float( toks[ i * 3 + 4 ] ) );
                }

                System.out.println("Adding Dataset " + sd.name +
                                   " with tables");
                for( i = 0; i < DataManager.getNumSections(); i++) {
                    String secname = DataManager.getSectionName(i);
                    SessionTable st = new SessionTable(secname);
                    st.localfile = dataDir + "/dataset." + secname + "." +
                                   sd.name + ".tab";
                    System.out.println("Table file: " + st.localfile );
                    File tablefile;
                    tablefile = new File( st.localfile );
                    if( tablefile.exists() ) {
                        sd.tables.add( st );
                    }
                }

                DataManager.addDataset( sd );
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Had to stop reading dataset info listing!");
            e.printStackTrace();
        }
    }

    //===================================================
    private void loadChatInfo(File f) {

        try {
            BufferedReader br = new BufferedReader( new FileReader(f));
            String line;
            while( (line = br.readLine()) != null) {
                String[] toks;
                toks = line.split("\t");
                
                int secid;
                int split;
                int chatid;
                int userid;
                int groupid;
                int typeid;
                float posx;
                float posy;
                long created;
                String local;
                String url;

                // check the version of chat list file
                if (toks.length != 11) {    // previous version
                    secid   = Integer.parseInt( toks[0] );
                    split   = Integer.parseInt( toks[1] );
                    chatid  = Integer.parseInt( toks[2] );
                    userid  = Integer.parseInt( toks[3] );
                    groupid = 0;
                    typeid  = 0;
                    posx     = Float.parseFloat( toks[4] );
                    posy     = 0;
                    created = -1;
                    local   = toks[5];
                    url     = toks[6];                    
                }
                else {                      // new version
                    secid   = Integer.parseInt( toks[0] );
                    split   = Integer.parseInt( toks[1] );
                    chatid  = Integer.parseInt( toks[2] );
                    userid  = Integer.parseInt( toks[3] );
                    groupid = Integer.parseInt( toks[4] );
                    typeid  = Integer.parseInt( toks[5] );
                    posx     = Float.parseFloat( toks[6] );
                    posy     = Float.parseFloat( toks[7] );
                    created = Long.parseLong(   toks[8] );
                    local   = toks[9];
                    url     = toks[10];
                }
                
                toks = null;

                DataManager.newChat( userid, secid, split, groupid, typeid, 
                                     posx, posy, created );
            }

            br.close();

        } catch (Exception e) {

        }
    }

    //===================================================
    // Dump all current splitcore listing to file
    public void dumpAllSplitCoreListFile() {
        try {
            splitCoreListFileLock.acquire();

            File f = new File(splitCoreListTempFile);
            FileWriter fw = new FileWriter(f, false); // notice, overwrite!

            for(int i=0; i < DataManager.getNumSections(); i++) {
                SessionSection ss = DataManager.getSessionSection(i);

                if(ss == null) continue;

                int secId = i;
                String url = DataManager.getSplitCoreURL(secId);

                if( (url != null) && (!url.equals("")) ) {
                    String dpi_x =
                            String.valueOf(DataManager.getSplitCoreDPIX(secId));
                    String dpi_y =
                            String.valueOf(DataManager.getSplitCoreDPIY(secId));

                    String buf;

                    buf = new String("" + secId);
                    fw.write(buf, 0, buf.length());
                    fw.write('\t');

                    buf = url;
                    fw.write(buf, 0, buf.length());
                    fw.write('\t');

                    buf = dpi_x;
                    fw.write(buf, 0, buf.length());
                    fw.write('\t');

                    buf = dpi_y;
                    fw.write(buf, 0, buf.length());
                    fw.write('\n');
                }

            }

            fw.flush();
            fw.close();

            Runtime.getRuntime().exec( copy + splitCoreListTempFile + " "
                                       + splitCoreListFile );

            splitCoreListFileLock.release();

        } catch ( Exception e ) {
            splitCoreListFileLock.release();
            e.printStackTrace();
            System.out.println("FAILED TO SAVE CHANGES TO SECTION LIST FILE");
        }

    }

    //=====================
    public void dumpAllWholeCoreListFile() {
        try {
            wholeCoreListFileLock.acquire();

            File f = new File( wholeCoreListTempFile );
            FileWriter fw = new FileWriter( f, false ); // notice, overwrite!

            for (int i = 0; i < DataManager.getNumSections(); i++) {
                SessionSection ss = DataManager.getSessionSection(i);

                if(ss == null) continue;

                int secId = i;
                String url = DataManager.getWholeCoreURL(secId);

                if( (url != null) && (!url.equals("")) ) {
                    String dpi =
                            new String("") + DataManager.getWholeCoreDPI(secId);

                    String buf;


                    buf = new String("" + secId);
                    fw.write(buf, 0, buf.length());
                    fw.write('\t');

                    buf = url;
                    fw.write(buf, 0, buf.length());
                    fw.write('\t');

                    buf = dpi;
                    fw.write(buf, 0, buf.length());
                    fw.write('\n');
                }
            }
            
            fw.flush();
            fw.close();

            Runtime.getRuntime().exec( copy + wholeCoreListTempFile + " "
                                       + wholeCoreListFile );

            wholeCoreListFileLock.release();

        } catch ( Exception e ) {
            wholeCoreListFileLock.release();
            e.printStackTrace();
            System.out.println("FAILED TO SAVE CHANGES TO SECTION LIST FILE");
        }
    }

}


