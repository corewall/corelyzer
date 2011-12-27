package corelyzer.plugin.andrill;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class ClientConnectionThread extends Thread {
    private Socket  socket;
    private boolean closeConnection;
    private DataOutputStream dos;
    private DataInputStream dis;
    private int id;
    private ClientData data;
    private boolean loggingIn;
    private Semaphore commSemaphore;

    private static ReentrantLock usersFileLock;
    private static String usersFile = new String("users.txt");

    private static final String dataDir = "./dataset/";
    
    //============================================
    public ClientConnectionThread(Socket s, int id) {
        super("ClientConnectionThread");
        socket = s;
        closeConnection = false;
        this.id = id;
        data = new ClientData();
        data.setUserID(-1);
        commSemaphore = new Semaphore(1,true);
    }

    //============================================
    public InetAddress getClientIP() {
        return socket.getInetAddress();
    }

    //============================================
    public void acquireDOS() {
        try {
            commSemaphore.acquire();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    //============================================
    public void releaseDOS() {
        try {
            commSemaphore.release();
        } catch ( Exception e ) {
            e.printStackTrace(); 
        }
    }

    //============================================
    public Socket getSocket() {
        return socket; 
    }

    //============================================
    public void run() {
        try {

            dos = new DataOutputStream( socket.getOutputStream() );
            dis = new DataInputStream( socket.getInputStream() );

            int i = 0;
            System.out.println("Thread Working...");
            if( closeConnection ) System.out.println("boo");
            else System.out.println("yay");

            socket.setSoTimeout(0);

            loggingIn = true;

            System.out.println("blah");
            while( !closeConnection )
            {
                if( dis == null ) 
                {
                    System.out.println("NULL DIS!!!!");
                    closeConnection = true;
                    continue;
                }

                try {
                    byte msg = dis.readByte();
                    System.out.println("MSG: " + msg );
                    switch( msg ) {
                    case ClientRequest.REGISTER_USER:
                        processRegistration();
                        break;
                    case ClientRequest.LOGIN:
                        loggingIn = true;
                        processLogin();
                        if( data.getUserID() == 0) 
                            socket.setSoTimeout(0);
                        break;
                    case ClientRequest.LOGOUT:
                        System.out.println("RECEIVED LOGOUT");
                        processLogout();
                        break;
                    case ClientRequest.SHUTDOWN_SERVER:
                        CorelyzerSessionServer.getServer().shutdown();
                        closeConnection = true;
                        System.out.println("SERVER TOLD TO SHUTDOWN!!!!");
                        break;
                    case ClientRequest.HEART_BEAT:
                        System.out.println("SERVER RECEIVED HEARTBEAT!!!");
                        updateHeartbeat();
                        break;
                    case ClientRequest.NEW_CORE_SECTION:
                        System.out.println("NEW CORE SECTION EXISTS!" +
                                           " PROCESSING");
                        updateSessionSections();
                        break;
                    case ClientRequest.NEW_SPLIT_CORE:
                        System.out.println("NEW SPLIT CORE EXISTS!");
                        updateSessionSplitCores();
                        System.out.println("PROCESSED!!");
                        break;
                    case ClientRequest.NEW_WHOLE_CORE:
                        System.out.println("NEW WHOLE CORE EXISTS!");
                        updateSessionWholeCores();
                        break;
                    case ClientRequest.NEW_DATASET:
                        receiveDataset();
                        break;
                    case ClientRequest.NEW_CHAT:
                        receiveNewChat();
                        break;
                    case ClientRequest.NEW_CHAT_ENTRY:
                        receiveNewChatEntry();
                        break;
                    case ClientRequest.LIST_SECTIONS:
                        sendSectionListing();
                        dos.writeByte(ServerResponse.SECTION_LIST_DONE);
                        dos.flush();
                        break;
                    case ClientRequest.LIST_SPLIT_CORES:
                        returnSplitCoreList();
                        break;
                    case ClientRequest.LIST_WHOLE_CORES:
                        returnWholeCoreList();
                        break;
                    case ClientRequest.LIST_DATASETS:
                        returnDatasetList();
                        break;
                    case ClientRequest.LIST_TABLES:
                        returnDatasetTablesList();
                        break;
                    case ClientRequest.TABLE_DATA:
                        returnDatasetTableData();
                        break;
                    case ClientRequest.GET_SPLIT_CORE_DATA:
                        sendSplitCoreImageData();
                        break;
                    case ClientRequest.GET_WHOLE_CORE_DATA:
                        sendWholeCoreImageData();
                        break;
                    case ClientRequest.RELAY_LOAD_SECTION:
                        relayLoadSection();
                        break;
                    case ClientRequest.RELAY_START_GRAPHS:
                        relayStartGraph();
                        break;
                    case ClientRequest.RELAY_MAKE_GRAPH:
                        relayMakeGraph();
                        break;
                    case ClientRequest.RUN_BACKUP:
                        CorelyzerSessionServer.getServer().runBackupProcess();
                        break;
                    case ClientRequest.HAS_SECTION:
                        queryHaveSection();
                        break;
                    }
                }
                catch( SocketException se ) {
                    closeConnection = true;
                    continue;
                }
                catch( SocketTimeoutException ste) {

                    if( data.getUserID() == 0)
                    {
                        try {
                            socket.setSoTimeout(0);
                        }
                        catch( Exception e) {
                        }
                        continue;
                    }

                    System.out.println("Caught TCP Timeout Exception!");
                    // send out a heart beat request if the last heartbeat
                    // is within 1 minute, otherwise do a disconnect
                    if( loggingIn )
                    {
                        System.out.println("STILL LOGGING IN");
                        continue;
                    }
                    else if( System.currentTimeMillis() - 
                             data.getLastHeartBeat()
                        > 60000 ) 
                    {
                        closeConnection = true;
                        continue;
                    }

                    System.out.println("SENDING HEARTBEAT REQUEST");
                    dos.writeByte( ServerResponse.HEART_BEAT_REQUEST );
                    dos.flush();
                }
                catch( EOFException eof) {
                    // probably socket closing time!
                    System.out.println("EOF EXCEPTION!!!!!");
                    closeConnection = true;
                    eof.printStackTrace();
                }
                catch( Exception e ) {
                    e.printStackTrace();
                }
                
            }
            
            // CLOSING CONNECTION!!!!!!
            System.out.println("NOTIFIED TO CLOSE CONNECTION!!! CLOSING");
            GlobalEventBroadcastThread.broadcastUserLoggedOutEvent(
                data.getUserID());
            
            CorelyzerSessionServer server = CorelyzerSessionServer.getServer();
            server.freeClient(this);
            server.writeLog(CorelyzerSessionLog.CONNECTIONCLOSE,
                            "system", "connection closed: \t"+getClientIP());
            
            // free up the entry in the has table
            CorelyzerSessionServer.getServer().removeUserFromTable(
                new Integer(data.getUserID()) );
            
            socket.close();
        }
        catch( Exception e ) {
            e.printStackTrace();
            try {

                GlobalEventBroadcastThread.broadcastUserLoggedOutEvent(
                    data.getUserID());

                CorelyzerSessionServer.getServer().freeClient(this);
                // free up the entry in the has table
                CorelyzerSessionServer.getServer().removeUserFromTable(
                    new Integer(data.getUserID()) );

                socket.close();

            } catch( Exception ee) {
                // do nothing
            }
        }

        System.out.println("\t\tCLOSING NETWORK THREAD!");
    }

     //============================================
    private void updateHeartbeat() {
        data.setLastHeartBeat( System.currentTimeMillis());   
    }

    //============================================
    public void setCloseConnection() {
        System.out.println("Set Close Connection CALLED!!!");
        CorelyzerSessionServer.getServer().freeClient(this);
        
        // log: CONNECTIONCLOSE
        CorelyzerSessionServer.getServer().
            writeLog(CorelyzerSessionLog.CONNECTIONCLOSE,
                     "system", 
                     "connection closed: \t" + getClientIP());
                     
        closeConnection = true;
    }

    //============================================
    private void relayLoadSection() {
        acquireDOS();
        try {
            int secType = dis.readInt();
            int secId   = dis.readInt();
            
            dos.writeByte( ServerResponse.RELAY_LOAD_SECTION );
            dos.writeInt( secType );
            dos.writeInt( secId );
            dos.flush();
            
            // log: IMAGE
            if (secType == 0) {     // splic core
                CorelyzerSessionServer.getServer().
                    writeLog(CorelyzerSessionLog.IMAGE,
                            data.getName(), 
                            "split core image downloaded: \t" + 
                            DataManager.getSplitCoreURL(secId));
            }
            else {                  // whole core
                CorelyzerSessionServer.getServer().
                    writeLog(CorelyzerSessionLog.IMAGE,
                            data.getName(), 
                            "whole core image downloaded: \t" + 
                            DataManager.getWholeCoreURL(secId));

            }
            
        } catch (Exception e) {

        }
        releaseDOS();
    }

    //============================================
    private void relayStartGraph() {
        acquireDOS();
        
        try {
            int field = dis.readInt();
            int len = dis.readInt();
            byte[] buf = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);

            dos.writeByte( ServerResponse.RELAY_START_GRAPHS );
            dos.writeInt(field);
            dos.writeInt(len);
            dos.write( buf, 0, len );
            dos.flush();

        } catch (Exception e) {

        }

        releaseDOS();
    }

    //============================================
    private void relayMakeGraph() {
        acquireDOS();
        try {
            byte[] sbuf;
            byte[] tbuf;
            int slen, tlen;
            int field;
            String sname, tname;

            slen = dis.readInt();
            sbuf = new byte[slen];
            // dis.read( sbuf, 0, slen );
            dis.readFully(sbuf);
            sname = new String(sbuf,"UTF-8");

            tlen = dis.readInt();
            tbuf = new byte[tlen];
            // dis.read( tbuf, 0, tlen );
            dis.readFully(tbuf);
            tname = new String(tbuf,"UTF-8");

            field = dis.readInt();

            System.out.println("Relaying make graph of set: " + sname + 
                               " with table: " + tname + " using field: " +
                               field);

            sbuf = null;
            tbuf = null;

            sbuf = sname.getBytes("UTF-8");
            tbuf = tname.getBytes("UTF-8");
            slen = sbuf.length;
            tlen = tbuf.length;

            System.out.println("SBUF LEN: " + sbuf.length + " TBUF LEN: " +
                               tbuf.length );

            dos.writeByte( ServerResponse.RELAY_MAKE_GRAPH );
            dos.writeInt ( sbuf.length );
            dos.write    ( sbuf, 0, sbuf.length );
            dos.writeInt ( tbuf.length );
            dos.write    ( tbuf, 0, tbuf.length );
            dos.writeInt ( field );
            dos.flush();
            
            // log: GRAPH_NEW
            CorelyzerSessionServer.getServer().
                writeLog(CorelyzerSessionLog.GRAPH_NEW, data.getName(), 
                         "make graph: \t" + sname + "\t" + tname + "\t" + field);

        } catch (Exception e) {

        }
        releaseDOS();
        
    }

    //============================================
    private void processRegistration() {
        try {

            int len;
            byte[] data;
            String name, realname,pass;
            
            len = dis.readInt();
            data = new byte[len];
            // dis.read( data, 0, len);
            dis.readFully(data);
            realname = new String( data, "UTF-8");

            len = dis.readInt();
            data = new byte[len];
            // dis.read( data, 0, len);
            dis.readFully(data);
            name = new String( data, "UTF-8");
            
            len = dis.readInt();
            data = new byte[len];
            // dis.read(data, 0, len);
            dis.readFully(data);
            pass = new String( data, "UTF-8");

            // unique names only!
            int userid = getUserID(name);

            if( userid >= 0 )
            {
                dos.writeByte( ServerResponse.USER_PASS_INVALID );
                dos.flush();
            }
            else
            {
                userid = addNewUser(name,realname,pass);

                if( userid >= 0 )
                    dos.writeByte( ServerResponse.USER_REGISTERED );
                else
                    dos.writeByte( ServerResponse.SERVER_UNAVAILABLE );

                dos.flush();
            }                

        } catch( Exception e ) {
            e.printStackTrace(); 
        }
    }

    //==============================================
    private void processLogin() {
        try {
            System.out.println("Processing Login");
            int len;
            byte[] str;
            String name, realname, pass;
            long starttime;

            len = dis.readInt();
            str = new byte[len];
            // dis.read( str, 0, len);
            dis.readFully(str);
            name = new String( str, "UTF-8");
            System.out.println("NAME: " + name);
            
            System.out.println("Name: " + name);

            len = dis.readInt();
            str = new byte[len];
            // dis.read(str, 0, len);
            dis.readFully(str);
            pass = new String( str, "UTF-8");
            System.out.println("PASS: " + pass);

            starttime = dis.readLong();

            int userid = getUserID(name,pass);

            System.out.println("USER ID: " + userid );

            if( userid < 0 )
            {
                dos.writeByte( ServerResponse.USER_PASS_INVALID );
                dos.flush();
                return;
            }

            if( isUserLoggedIn(userid))
            {
                System.out.println("USER ALREADY LOGGED IN!!!");
                dos.writeByte( ServerResponse.USER_ALREADY_LOGGED_IN );
                dos.flush();
                return;
            }


            realname = getUserRealName(userid);

            System.out.println("Sending Message User Has Logged in");
            dos.writeByte( ServerResponse.USER_LOGGED_IN );
            dos.writeInt( userid );
            dos.flush();

            loggingIn = false;

            CorelyzerSessionServer server = 
                CorelyzerSessionServer.getServer();


            if( userid != 0 ) {
                sendSectionListing();
                sendSplitCoreListing();
                sendWholeCoreListing();
                sendDatasetListing();
                sendChatListing();
                dos.writeByte( ServerResponse.UPDATE_LOCAL_SECTION_LIST);
                dos.flush();
            }

            // send out current set of logged in users
            System.out.println("Num Collaborators: " + 
                               server.getNumClients());

            int i;

/*
            for( i = 0; i < server.getNumClients() && userid != 0; i++) {

                ClientConnectionThread cct = 
                    server.getConnectionThread(i);
                if( cct == null ) 
                    continue;
                if( cct.data.isLoggedIn() == false )
                    continue;

                dos.writeByte( corelyzer.plugin.andrill.ServerResponse.USER_LOGGED_IN );
                dos.writeInt( cct.data.getUserID() );
                String friendName;
                friendName = cct.data.getName();
                byte[] sendstr;
                sendstr = friendName.getBytes("UTF-8");
                dos.writeInt( sendstr.length );
                dos.write( sendstr, 0, sendstr.length );
                dos.flush();
                    
            }
*/

            // set user data
            data.setLastHeartBeat(System.currentTimeMillis());
            data.setLoggedIn(true);
            data.setName(name);
            data.setRealName(realname);
            data.setUserID( userid );
            data.setState( ClientState.IDLE );
            data.setXY( 0.0f, 0.0f );

            //GlobalEventBroadcastThread.broadcastUserLoggedInEvent(
            //    userid, name);

            // add to the userID Thread hashtable
            server.addUserToTable( new Integer(userid), this);
            
            // log
            server.writeLog(CorelyzerSessionLog.LOGIN, 
                            name, "user logged in: \t" + getClientIP());

        } catch( Exception e ) {
            e.printStackTrace(); 
        }
    }
    
    //==============================================
    private void processLogout() {
        data.setLoggedIn(false);
        CorelyzerSessionServer server = 
                CorelyzerSessionServer.getServer();

        server.freeClient(this);
        closeConnection = true;
        GlobalEventBroadcastThread.broadcastUserLoggedOutEvent(data.getUserID());
        System.out.println("Closing Thread...");
        server.writeLog(CorelyzerSessionLog.LOGOUT, 
                        data.getName(), "user logged out");
        
    }

    //======================================================
    private void sendSectionListing() {
        // send the sections listing, split and whole
        for( int i = 0; i < DataManager.getNumSections(); i++)
        {
            String name;
            float depth, length;
            byte[] nbuf;

            name = DataManager.getSectionName(i);
            depth = DataManager.getSectionDepth(i);
            length = DataManager.getSectionLength(i);

            if( name == null)
                continue;

            try {
                nbuf = name.getBytes("UTF-8");
            } catch( Exception e) {
                System.out.println("COULDN'T CONVERT TO UTF-8!! WTF!!");
                e.printStackTrace();
                continue;
            }                
                
            try {
                dos.writeByte  (ServerResponse.NEW_SECTION);
                dos.writeInt   (i);
                dos.writeFloat (depth);
                dos.writeFloat (length);
                dos.writeInt   (nbuf.length);
                dos.write      (nbuf, 0 ,nbuf.length);
                dos.flush();
            }
            catch (Exception e ) {

                System.out.println("SERVER FAILED TO BROADCAST TRACK " + 
                                   " CREATED TO AN EXISTING USER");
            }
        }

    }

    //======================================================
    private void sendSplitCoreListing() {
        // send the sections listing, split and whole
        for( int i = 0; i < DataManager.getNumSections(); i++)
        {
            String name, url;
            float depth, length, dpi_x, dpi_y;
            byte[] nbuf;
            byte[] ubuf;

            name = DataManager.getSectionName(i);
            depth = DataManager.getSectionDepth(i);
            length = DataManager.getSectionLength(i);

            url = DataManager.getSplitCoreURL(i);
            dpi_x = DataManager.getSplitCoreDPIX(i);
            dpi_y = DataManager.getSplitCoreDPIY(i);

            if( name == null || url == null)
                continue;

            try {
                nbuf = name.getBytes("UTF-8");
                ubuf = url.getBytes("UTF-8");
            } catch( Exception e) {
                System.out.println("COULDN'T CONVERT TO UTF-8!! WTF!!");
                e.printStackTrace();
                continue;
            }                
                
            try {
                dos.writeByte  (ServerResponse.SPLIT_CORE_AVAILABLE);
                dos.writeInt   (i);
                dos.writeFloat (depth);
                dos.writeFloat (length);
                dos.writeFloat (dpi_x);
                dos.writeFloat (dpi_y);
                dos.writeInt   (nbuf.length);
                dos.write      (nbuf, 0 ,nbuf.length);
                dos.writeInt   (ubuf.length);
                dos.write      (ubuf, 0, ubuf.length);
                dos.flush();
            }
            catch (Exception e ) {

                System.out.println("SERVER FAILED TO BROADCAST TRACK " + 
                                   " CREATED TO AN EXISTING USER");
            }
        }

        try {
            dos.writeByte( ServerResponse.SPLIT_CORE_LIST_DONE );
        } catch (Exception e) {
            System.out.println("FAILED TO SEND SPLIT CORE LIST DONE!");
        }

    }

    //=======================================================
    private void sendWholeCoreListing() {
        // send the sections listing, split and whole
        for( int i = 0; i < DataManager.getNumSections(); i++)
        {
            String name, url;
            float depth, length, dpi;
            byte[] nbuf;
            byte[] ubuf;

            name = DataManager.getSectionName(i);
            depth = DataManager.getSectionDepth(i);
            length = DataManager.getSectionLength(i);

            url = DataManager.getWholeCoreURL(i);
            dpi = DataManager.getWholeCoreDPI(i);

            if( name == null || url == null)
                continue;

            try {
                nbuf = name.getBytes("UTF-8");
                ubuf = url.getBytes("UTF-8");
            } catch( Exception e) {
                System.out.println("COULDN'T CONVERT TO UTF-8!! WTF!!");
                e.printStackTrace();
                continue;
            }                
                
            try {
                dos.writeByte  (ServerResponse.WHOLE_CORE_AVAILABLE);
                dos.writeInt   (i);
                dos.writeFloat (depth);
                dos.writeFloat (length);
                dos.writeFloat (dpi);
                dos.writeInt   (nbuf.length);
                dos.write      (nbuf, 0 ,nbuf.length);
                dos.writeInt   (ubuf.length);
                dos.write      (ubuf, 0, ubuf.length);
                dos.flush();
            }
            catch (Exception e ) {

                System.out.println("SERVER FAILED TO BROADCAST TRACK " + 
                                   " CREATED TO AN EXISTING USER");
            }
        }

        try {
            dos.writeByte( ServerResponse.WHOLE_CORE_LIST_DONE );
        } catch (Exception e) {
            System.out.println("FAILED TO SEND WHOLE CORE LIST DONE!");
        }
    }

    //=======================================================
    private void sendDatasetListing() {
        try {
            dos.writeByte( ServerResponse.DATASET_LIST );
            int nsets = DataManager.getNumDatasets();
            dos.writeInt(nsets);
            dos.flush();
            
            for( int k = 0; k < nsets; k++) {
                
                SessionDataset sd = DataManager.getDataset(k);
                byte[] buf = sd.name.getBytes("UTF-8");
                dos.writeInt(buf.length);
                dos.write(buf,0,buf.length);
                int fcount = sd.fields.size();
                dos.writeInt(fcount);
                
                int l;
                for( l = 0; l < fcount; l++) {
                    buf = sd.fields.elementAt(l).getBytes("UTF-8");
                    dos.writeInt(buf.length);
                    dos.write(buf,0,buf.length);
                    dos.writeFloat( sd.mins.elementAt(l).floatValue());
                    dos.writeFloat( sd.maxs.elementAt(l).floatValue());
                }

            }
        
            dos.flush();
        } catch (Exception e) {
            System.out.println("Error sending out dataset list!");
        }
    }

    //=======================================================
    private void sendDatasetTableListing() {
        try {
            System.out.println("Getting dataset string");
            int len = dis.readInt();
            byte[] buf = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            String setname = new String(buf,"UTF-8");
            System.out.println("Set name to search for: " + setname );

            int id = DataManager.getDatasetID(setname);
            System.out.println("Set id: " + id );
            if( id < 0 ) 
            {
                dos.writeByte( ServerResponse.TABLE_LIST_ERR);
                dos.flush();
                return;
            }

            SessionDataset sd = DataManager.getDataset(id);

            System.out.println("Num Tables: " + sd.tables.size() );

            dos.writeByte( ServerResponse.TABLE_LIST );
            buf = setname.getBytes("UTF-8");
            dos.writeInt( buf.length );
            dos.write   ( buf, 0, buf.length );
            dos.writeInt( sd.tables.size() );

            for( int i = 0; i < sd.tables.size(); i++) {
                buf = sd.tables.elementAt(i).name.getBytes("UTF-8");
                dos.writeInt( buf.length );
                dos.write( buf, 0, buf.length );
            }
            
            dos.flush();

        } catch (Exception e) { 
            System.out.println("Error Sending out dataset table list!");
        }
    }

    //=======================================================
    private void sendDatasetTableData() {
        try {
            System.out.println("Getting dataset string");
            int len = dis.readInt();
            byte[] buf = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            String setname = new String(buf,"UTF-8");
            
            len = dis.readInt();
            buf = new byte[len];
            // dis.read(buf,0,len);
            dis.readFully(buf);
            
            String tablename = new String(buf,"UTF-8");
            System.out.println("Set name to search for: " + setname 
                + " with table: " + tablename);

            int setid = DataManager.getDatasetID(setname);
            if( setid < 0 ) {
                dos.writeByte( ServerResponse.TABLE_DATA_ERR);
                dos.flush();
                return;
            }

            SessionTable st = DataManager.getTableForSection( setid, 
                                                              tablename);

            if( st == null ) {
                dos.writeByte(ServerResponse.TABLE_DATA_ERR);
                dos.flush();
                return;
            }

            // open the file, read the data and send it away

            File f = new File( st.localfile );
            if( !f.exists() ) {
                dos.writeByte(ServerResponse.TABLE_DATA_ERR);
                dos.flush();
                return;
            }

            BufferedReader br = new BufferedReader( new FileReader(f) );
            String line = br.readLine();

            if( line == null ) {
                dos.writeByte(ServerResponse.TABLE_DATA_ERR);
                dos.flush();
                return;
            }


            String[] toks = line.split("\t");
            int nfields = toks.length;

            Vector< boolean[] > valids = new Vector< boolean[] >();
            Vector< float[] >   values = new Vector< float[] >();

            boolean[] bs;
            float[]   vs;

            dos.writeByte( ServerResponse.TABLE_DATA);
            buf = setname.getBytes("UTF-8");
            dos.writeInt( buf.length );
            dos.write   ( buf, 0, buf.length );
            buf = tablename.getBytes("UTF-8");
            dos.writeInt( buf.length );
            dos.write   ( buf, 0, buf.length );

            while( (line = br.readLine()) != null ) {

                toks = line.split("\t");
                bs = new boolean[nfields];
                vs = new float[nfields];

                for( int i = 0; i < nfields; i++) {
                    if( toks[i * 2].contains("v"))
                        bs[i] = true;
                    else
                        bs[i] = false;
                    vs[i] = Float.parseFloat( toks[i * 2 + 1] );
                    System.out.print("," + bs[i] + " : " + vs[i] );
                }

                System.out.println("");
                valids.add(bs);
                values.add(vs);
            }

            int rows = values.size();

            dos.writeInt( nfields );
            dos.writeInt( values.size() );

            for( int r = 0; r < rows; r++) {
                bs = valids.elementAt(r);
                vs = values.elementAt(r);
                for( int i = 0; i < nfields; i++) {
                    dos.writeBoolean( bs[i] );
                    dos.writeFloat( vs[i] );
                }
            }

            dos.flush();

        } catch( Exception e) {
            System.out.println("Error Sending table data!");
        }


    }

    //=======================================================
    private void sendChatListing() {
        System.out.print("Sending Chat Listing...");
        try {

            for( int i = 0; i < DataManager.getNumSections(); i++) {
                int k;
                for( k = 0; k < DataManager.getNumSplitChats(i); k++) {
                    String url;
                    int uid;
                    int group, type;
                    float posx, posy;
                    long created;
                    long lastModified;
                    byte[] ubuf;

                    url = DataManager.getChatURL( 0, i, k );
                    uid = DataManager.getChatCreator( 0, i, k);
                    group = DataManager.getChatGroup( 0, i, k);
                    type = DataManager.getMarkerType( 0, i, k);
                    posx = DataManager.getChatDepthX( 0, i, k );
                    posy = DataManager.getChatDepthY( 0, i, k );
                    created = DataManager.getChatCreated( 0, i, k);
                    lastModified = DataManager.getChatLastModified( 0, i, k );
                    ubuf = url.getBytes("UTF-8");

                    dos.writeByte( ServerResponse.NEW_CHAT );
                    dos.writeInt( 0 );   //split
                    dos.writeInt( i );   //sectionid
                    dos.writeInt( k );   //globalid
                    dos.writeInt( uid ); //uid
                    dos.writeInt( group ); //groupid
                    dos.writeInt( type );  //typeid
                    dos.writeFloat( posx ); // x_pos
                    dos.writeFloat( posy ); // y_pos
                    dos.writeLong( created );
                    dos.writeLong( lastModified );
                    dos.writeInt( ubuf.length );
                    dos.write( ubuf, 0, ubuf.length );
                    dos.flush();

                }

                for( k = 0; k < DataManager.getNumWholeChats(i); k++) {
                    String url;
                    int uid;
                    int group, type;
                    float posx, posy;
                    long created;
                    long lastModified;
                    byte[] ubuf;

                    url = DataManager.getChatURL( 1, i, k );
                    uid = DataManager.getChatCreator( 1, i, k);
                    group = DataManager.getChatGroup( 1, i, k);
                    type = DataManager.getMarkerType( 1, i, k);
                    posx = DataManager.getChatDepthX( 1, i, k );
                    posy = DataManager.getChatDepthY( 1, i, k );
                    created = DataManager.getChatCreated( 1, i, k );
                    lastModified = DataManager.getChatLastModified( 1, i, k );
                    ubuf = url.getBytes("UTF-8");

                    dos.writeByte( ServerResponse.NEW_CHAT );
                    dos.writeInt( 1 ); //split
                    dos.writeInt( i ); //sectionid
                    dos.writeInt( k ); //globalid
                    dos.writeInt( uid ); //uid
                    dos.writeInt( group );  //groupid
                    dos.writeInt( type );   //typeid
                    dos.writeFloat( posx );  // x_pos
                    dos.writeFloat( posy );  // y_pos
                    dos.writeLong( created );
                    dos.writeLong( lastModified );
                    dos.writeInt( ubuf.length );
                    dos.write( ubuf, 0, ubuf.length );
                    dos.flush();

                }
            }

        } catch (Exception e) {

        }
        System.out.print("Done\n");
    }

    //=======================================================
    private int getUserID(String name) {
        try {
            File f = new File( usersFile );
            BufferedReader br = new BufferedReader(new FileReader(f));
            boolean found = false;
            String n, r, p;
            int i = 0;

            while( (n = br.readLine()) != null && !found )
            {
                r = br.readLine();
                p = br.readLine();
                if( n.equals(name))
                {
                    found = true;
                }
                else
                    i++;
            }
            
            br.close();

            if( !found ) 
                return -1;
            else
                return i;

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    //==========================================================
    private int getUserID(String name, String pass) {
        try {
            File f = new File( usersFile );
            BufferedReader br = new BufferedReader(new FileReader(f));
            boolean found = false;
            String n, r, p;
            int i = 0;

            while( (n = br.readLine()) != null && !found )
            {
                r = br.readLine();
                p = br.readLine();
                if( n.equals(name) && p.equals(pass))
                {
                    found = true;
                }
                else
                    i++;
            }
            
            br.close();

            if( !found ) 
                return -1;
            else
                return i;

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    //==========================================================
    private String getUserRealName(int id) {
        try {
            File f = new File( usersFile );
            BufferedReader br = new BufferedReader(new FileReader(f));
            String n, r, p;
            int i = 0;

            while( (n = br.readLine()) != null)
            {
                r = br.readLine();
                p = br.readLine();
                if( i == id) {
                    br.close();
                    return r;
                }
                
                i++;
            }
            
            br.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //=================================================
    private int addNewUser(String name, String realname, String pass) {
        try {
            usersFileLock.lock();
            FileWriter fw = new FileWriter(usersFile,true);
            fw.write( name + "\n", 0, name.length() + 1);
            fw.write( realname + "\n", 0, realname.length() + 1);
            fw.write( pass + "\n", 0, pass.length() + 1);
            fw.close();
            usersFileLock.unlock();
            return getUserID(name,pass);

        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    //=================================================
    private boolean isUserLoggedIn(int id) {
        if( CorelyzerSessionServer.getServer().getConnectionThreadByUserID(id) 
            != null) 
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    //================================================
    public ClientData getClientData() { return data; }

    //================================================
    public DataOutputStream getOutStream() { return dos; }

    //================================================
    public void notifyTrackCreated(int userid, byte[] name) {
        try {

            commSemaphore.acquire();

            dos.writeByte( ServerResponse.TRACK_CREATED );
            dos.writeInt( userid );
            dos.writeInt( name.length );
            dos.write( name, 0, name.length );

            commSemaphore.release();

        }
        catch( Exception e ) {
            System.out.println("FAILED TO SEND TO CLIENT... ADD TO QUEUE " +
                               "TO SEND OUT LATER");
        }
    }

    //================================================
    private void updateSessionTracks() {
        try {
            int len;
            byte[] name;
            len = dis.readInt();
            name = new byte[len];
            // dis.read( name, 0, len );
            dis.readFully(name);

            System.out.println("Track Name: " + new String(name,"UTF-8") );

            GlobalEventBroadcastThread.broadcastTrackCreated( data.getUserID(),
                                                              name );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //================================================
    public void updateSessionSections() {
        try {

            float depth = dis.readFloat();
            float length = dis.readFloat();

            int len;
            byte[] buf;

            len = dis.readInt();
            buf = new byte[len];
            // dis.read( buf, 0 , len);
            dis.readFully(buf);

            String secName = new String(buf, "UTF-8");
            DataManager.addSection( secName, depth, length );

            
            dos.writeByte( ServerResponse.SECTION_RECEIVED );
            dos.flush();

            GlobalEventBroadcastThread.broadcastNewSection(
                DataManager.getSectionID(secName) );

            System.out.println("RECEIVED AND ADDED NEW SECTION!");
            
            // log
            CorelyzerSessionServer.getServer().
                writeLog(CorelyzerSessionLog.SECTION_NEW,
                         data.getName(), 
                         "added new section: \t" + secName + "\t" + depth);

            try {


            } catch (Exception e) {
                
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                dos.writeByte( ServerResponse.SECTION_ERR );
                dos.flush();
            } catch ( Exception ee ) {
                System.out.println("CANNOT WRITE OUT!!!!!");
                ee.printStackTrace();
            }
        }
    }

    //================================================
    public void updateSessionSplitCores() {
        try {
            float dpi_x = dis.readFloat();
            float dpi_y = dis.readFloat();
            
            int len;
            byte[] buf;

            len = dis.readInt();
            buf = new byte[len];
            // dis.read( buf, 0 , len);
            dis.readFully(buf);
            
            String secname = new String(buf,"UTF-8");

            len = dis.readInt();
            buf = new byte[len];
            // dis.read(buf,0,len);
            dis.readFully(buf);

            String url = new String(buf,"UTF-8");

            DataManager.setSplitCoreURL(secname,url);
            DataManager.setSplitCoreDPI(secname, dpi_x, dpi_y);

            dos.writeByte( ServerResponse.SPLIT_CORE_RECEIVED );
            dos.flush();

            GlobalEventBroadcastThread.broadcastSplitCoreAvailable(
                DataManager.getSectionID(secname));
            
            // log
            CorelyzerSessionServer.getServer().
                writeLog(CorelyzerSessionLog.SPLITCORE,
                         data.getName(), 
                         "new split core: \t" + secname + "\t" + url);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                dos.writeByte( ServerResponse.SECTION_ERR );
                dos.flush();
            } catch ( Exception ee ) {
                System.out.println("CANNOT WRITE OUT!!!!!");
                ee.printStackTrace();
            }
        }
        finally {
                        
        }
    }

    //================================================
    public void receiveDataset() {
        try {
            int len;
            byte[] buf;

            len = dis.readInt();
            buf = new byte[len];
            // dis.read(buf,0,len);
            dis.readFully(buf);
            String sdname = new String(buf,"UTF-8");

            boolean writeNewEntry = true;

            int id = DataManager.getDatasetID(sdname);
            SessionDataset sd;
            if( id >= 0 )
            {
                sd = DataManager.getDataset(id);
                writeNewEntry = false;
            }
            else
            {
                sd = new SessionDataset(sdname);
                DataManager.addDataset( sd );
                id = DataManager.getDatasetID(sdname);
            }

            int fieldcount = dis.readInt();
            String[] fields = new String[fieldcount];
            
            for( int l = 0; l < fieldcount; l++) {
                len = dis.readInt();
                buf = new byte[len];
                // dis.read(buf,0,len);
                dis.readFully(buf);
                fields[l] = new String(buf,"UTF-8");
                if( !sd.fields.contains( fields[l] ) )
                    sd.fields.add( fields[l] );
                sd.mins.add(null);
                sd.maxs.add(null);
            }

            
            while( dis.readByte() != ClientRequest.END_DATASET ) {

                SessionTable st;

                int secid = dis.readInt();
                len = dis.readInt();
                buf = new byte[len];
                // dis.read(buf,0,len);
                dis.readFully(buf);
                String name = new String(buf,"UTF-8");

                st = DataManager.getTableForSection( id, name );

                if( st == null ) {
                    st = new SessionTable(name);
                    sd.tables.add(st);
                }

                Vector< float[] > data;
                Vector< boolean[] > valids;
                
                data = new Vector< float[] >();
                valids = new Vector< boolean[] >();
                
                
                while( dis.readByte() != ClientRequest.END_TABLE ) {
                    float[] row;
                    boolean[] valid;
                    row = new float[fieldcount];
                    valid = new boolean[fieldcount];

                    for( int k = 0; k < fieldcount; k++) {
                        row[k] = dis.readFloat();
                        valid[k] = dis.readBoolean();

                        // check min/max
                        if( valid[k] ) {

                            if( sd.mins.elementAt(k) == null ) {
                                sd.mins.setElementAt(new Float(row[k]), k );
                            }
                            else if( sd.mins.elementAt(k).floatValue() > row[k])
                            { 
                                sd.mins.setElementAt(new Float(row[k]), k);
                            }

                            if( sd.maxs.elementAt(k) == null ) {
                                sd.maxs.setElementAt(new Float(row[k]), k );
                            }
                            else if( sd.maxs.elementAt(k).floatValue() < row[k])
                            { 
                                sd.maxs.setElementAt(new Float(row[k]), k);
                            }

                        }
                    }
                    
                    valids.add(valid);
                    data.add(row);
                } // end for each row

                st.localfile = dataDir + "/dataset." + name + "." + sd.name +
                               ".tab";
                FileWriter fw = new FileWriter(st.localfile + ".temp");

                int l;
                
                for( l = 0; l < fieldcount; l++) {
                    fw.write( fields[l], 0, fields[l].length());
                    fw.write('\t');
                    System.out.print(fields[l] + "\t");
                }
                System.out.println("");
                fw.write('\n');
                
                for( l = 0; l < data.size(); l++) {
                    for( int k = 0; k < fieldcount; k++) {
                        
                        float[] row;
                        boolean[] valid;
                        
                        row = data.elementAt(l);
                        valid = valids.elementAt(l);
                        
                        if( valid[k] ) {
                            System.out.print("v\t");
                            fw.write('v');
                            fw.write('\t');
                            String value;
                            value = new String("" + row[k]);
                            fw.write(value,0,value.length());
                            fw.write('\t');
                        }
                        else {
                            System.out.print("i\t\t");
                            fw.write('i');
                            fw.write('\t');
                            fw.write('0');
                            fw.write('\t');
                        }
                        
                    }
                    
                    System.out.println("");
                    fw.write('\n');
                }

                fw.flush();
                fw.close();

                String copy;
                if( System.getProperty("os.name").contains("Windows"))
                    copy = new String("cmd.exe /c copy ");
                else
                    copy = new String("cp ");
                
                Runtime.getRuntime().exec( copy + " " + st.localfile + ".temp"
                                           + " " + st.localfile );

            } // end for each table
            
            if( writeNewEntry ) {
                
                CorelyzerSessionServer.getServer().appendDatasetListFile(
                    DataManager.getDatasetID( sd.name ));
            }

            dos.write( ServerResponse.NEW_DATASET_AVAILABLE );
            dos.flush();

            GlobalEventBroadcastThread.broadcastNewDatasetAvailable( sd );

        } catch( Exception e) {

            System.out.println("ERROR: " );
            e.printStackTrace();

            try {
                dos.write( ServerResponse.DATASET_ERR );
                dos.flush();
            }  catch (Exception ex) {
                System.out.println("COULDN'T NOTIFY USER OF DATASET LOAD ERR");
            }

        }
    }

    //================================================
    public void receiveNewChat() {
        try {

            int tid = dis.readInt();
            int sid = dis.readInt();
            int groupid = dis.readInt();
            int typeid = dis.readInt();
            float px = dis.readFloat();
            float py = dis.readFloat();
            long now = System.currentTimeMillis();

            // boolean split;
            int split;
            String url;
            
            if( tid == 0 )
                split = 0;
            else if( tid == 1)
                split = 1;
            else
                split = -1;

            url = DataManager.newChat( data.getUserID(), sid, split, 
                                        groupid, typeid, px, py, now );

            if( url == null )
                return;

            int entry = DataManager.getChatEntryByURL( tid, sid, url );

            GlobalEventBroadcastThread.broadcastNewChat( tid, sid, entry );
            CorelyzerSessionServer server = CorelyzerSessionServer.getServer();
            server.appendChatListFile( tid, sid, entry );
            
            // log: CHAT_NEW
            server.writeLog(CorelyzerSessionLog.CHAT_NEW, 
                            data.getName(), "new chat: \t" + 
                            DataManager.getSectionName(sid) + 
                            "\t" + url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //================================================
    public void receiveNewChatEntry() {
        try {

            System.gc();
            System.out.println("NEW CHAT ENTRY EVENT!");
            int tid = dis.readInt();
            int sid = dis.readInt();
            int len;
            byte[] buf;
            String url, entry;
            float pos = dis.readFloat();

            len = dis.readInt();
            buf = new byte[len];
            // dis.read(buf,0,len);
            dis.readFully(buf);
            url = new String(buf, "UTF-8");
            buf = null;


            len = dis.readInt();
            buf = new byte[len];
            // dis.read(buf,0,len);
            dis.readFully(buf);
            entry = new String(buf,"UTF-8");
            buf = null;

            // System.out.println("NEW CHAT ENTRY: " + entry );

            int entryid = DataManager.getChatEntryByURL( tid, sid, url );
            if( entryid == -1 )
            {
                System.out.println("Couldn't find chat html by URL: " +
                                   url );
                System.out.println("Searching by depth: " + pos );
                // must be a first... search by depth, if still no match,
                // then return
                entryid = DataManager.getChatEntryByDepth( tid, sid, pos);
                if( entryid == -1)
                    return;
            }

            System.out.println("Found chat entry!!!" + entryid );
            DataManager.appendChatEntry( tid, sid, entryid, entry, 
                                         data.getRealName() );
            GlobalEventBroadcastThread.broadcastNewChatEntry( tid, sid, 
                                                              entryid );
            
            // log: CHAT_ENTRY
            CorelyzerSessionServer.getServer().
                    writeLog(CorelyzerSessionLog.CHAT_ENTRY, 
                             data.getName(), "new chat entry: \t" + 
                             DataManager.getSectionName(sid) + 
                             "\t" + url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //================================================
    public void updateSessionWholeCores() {
        try {
            float dpi = dis.readFloat();

            int len;
            byte[] buf;

            len = dis.readInt();
            buf = new byte[len];
            // dis.read( buf, 0 , len);
            dis.readFully(buf);
            
            String secname = new String(buf,"UTF-8");

            len = dis.readInt();
            buf = new byte[len];
            // dis.read(buf,0,len);
            dis.readFully(buf);

            String url = new String(buf,"UTF-8");

            float start_depth = dis.readFloat();
            float length      = dis.readFloat();

            DataManager.setWholeCoreURL(secname,url);
            DataManager.setWholeCoreDPI(secname,dpi);
            DataManager.setWholeCoreStartDepth(secname, start_depth);
            DataManager.setWholeCoreLength(secname, length);

            dos.writeByte( ServerResponse.WHOLE_CORE_RECEIVED );
            dos.flush();

            GlobalEventBroadcastThread.broadcastWholeCoreAvailable(
                DataManager.getSectionID(secname));
            
            // log: WHOLECORE
            CorelyzerSessionServer.getServer().
                writeLog(CorelyzerSessionLog.WHOLECORE,
                         data.getName(), 
                         "new whole core: \t" + secname + "\t" + url);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                dos.writeByte( ServerResponse.SECTION_ERR );
                dos.flush();
            } catch ( Exception ee ) {
                System.out.println("CANNOT WRITE OUT!!!!!");
                ee.printStackTrace();
            }
        }

    }

    //================================================
    public void returnSplitCoreList() {
        acquireDOS();
        sendSplitCoreListing();
        releaseDOS();
    }

    //================================================
    public void returnWholeCoreList() {
        acquireDOS();
        sendWholeCoreListing();
        releaseDOS();
    }

    //================================================
    public void sendSplitCoreImageData() {

    }

    //================================================
    public void sendWholeCoreImageData() {

    }

    //================================================
    public void returnDatasetList() {
        acquireDOS();
        sendDatasetListing();
        releaseDOS();
    }

    //================================================
    public void returnDatasetTablesList() {
        acquireDOS();
        sendDatasetTableListing();
        releaseDOS();
    }

    //================================================
    public void returnDatasetTableData() {
        acquireDOS();
        sendDatasetTableData();
        releaseDOS();
    }

    //================================================
    public void queryHaveSection() {
        try {
            int len;
            byte[] buf;

            len = dis.readInt();
            buf = new byte[len];
            // dis.read( buf, 0 , len);
            dis.readFully(buf);

            String secname = new String(buf,"UTF-8");

            // check whether the server has the 'secname' section
            if( DataManager.hasSection(secname) ) {
                dos.writeByte( ServerResponse.HAVE_SECTION );
                dos.flush();
            } else {
                dos.writeByte( ServerResponse.HAVE_NO_SECTION );
                dos.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();

            try {
                dos.writeByte( ServerResponse.SECTION_ERR );
                dos.flush();
            } catch ( Exception ee ) {
                System.out.println("CANNOT WRITE OUT!!!!!");
                ee.printStackTrace();
            }
        }
    }
}
