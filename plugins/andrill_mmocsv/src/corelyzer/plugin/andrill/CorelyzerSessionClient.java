package corelyzer.plugin.andrill;

import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.ui.AuthDialog;
import corelyzer.ui.CorelyzerApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CorelyzerSessionClient extends Thread {

    Socket socket;
    SharedMiceClientThread mouseThread;
    IncomingEventsThread   eventThread;
    DataInputStream dis;
    DataOutputStream dos;
    String server;
    boolean closeConnection;
    boolean retryLogin;
    boolean loggedIn;

    LinkedList<CorelyzerPluginEvent> outQueue;
    Semaphore queuePermit;
    ReentrantLock dosLock;
    ReentrantLock disLock;
    
    Semaphore dataRequestWait;
    byte      dataRequestResponse;

    Semaphore logoutPermit;

    CorelyzerSessionClientPlugin plugin;
    String username;
    String password;
    String realname;

    int    userid;

    boolean isContinue = false;
    //======================================================
    public CorelyzerSessionClient(LinkedList< CorelyzerPluginEvent > queue,
                                  Semaphore permit,
                                  CorelyzerSessionClientPlugin p) {
        server = null;
        username = null;
        password = null;
        outQueue = queue;
        queuePermit = permit;
        plugin = p;
        userid = -1;
        dosLock = new ReentrantLock();
        logoutPermit = new Semaphore(1);
        loggedIn = false;
        dataRequestWait = new Semaphore(1, true);
        disLock = new ReentrantLock();
    }

    //======================================================
    public int getUserID() { return userid; }


    //======================================================
    public void setServerAddress(String s) {
        server = s;
    }

    //======================================================
    public void setUserName(String name) {
        username = name;
    }

    //======================================================
    public String getUserName() {
        return username;
    }

    //======================================================
    public String getRealName() {
        return realname;
    }

    //======================================================
    public String getPassword() {
        return password;
    }
    
    //======================================================
    public void setPassword(String psswd) {
        password = psswd;
    }

    //======================================================
    public void lockDIS() { disLock.lock(); }

    //======================================================
    public void unlockDIS() { disLock.unlock(); }

    //======================================================
    public CorelyzerSessionClientPlugin getPlugin() { return plugin; }

    //======================================================
    public void run() {
        try {
            socket.setSoTimeout(20);
            socket.setTcpNoDelay(true);

            if( !socket.isConnected() ) 
                return;

            // int i = 0;

            // start mouse postion update thread
            mouseThread = new SharedMiceClientThread(
                this, plugin, socket.getInetAddress(), 11998 );
            mouseThread.start();
            mouseThread.shutdown();

            socket.setSoTimeout(0);


            while( !closeConnection )
            {
//                System.out.print("WAITING FOR QUEUE PERMIT");
                // wait for events, release called by plugin object
                queuePermit.tryAcquire(100, TimeUnit.MILLISECONDS);
//                queuePermit.acquire();

//                System.out.println("\tQUEUE PERMIT RELEASED! " +
//                                   "WAITING FOR DOS LOCK");
                // process events
                dosLock.lock();

//                System.out.println("WORKING QuEUE");

                try {
                    // process corelyzer events
                    CorelyzerPluginEvent event;
                    while( (event = outQueue.removeFirst()) != null) {
                        switch( event.getID()) {
                        case CorelyzerPluginEvent.MOUSE_MOTION:
                            updateMousePos(event.getDescription());
                            break;
                        case CorelyzerPluginEvent.NEW_ANNOTATION:
                        }
                    }
                }
                catch( Exception e ) {
                    // whatever
                }
                
//                System.out.println("QUEUE WORK FINISHED");

                dosLock.unlock();
                // self lock
                queuePermit.tryAcquire(100, TimeUnit.MILLISECONDS);
//                queuePermit.acquire();
            }

            System.out.println("---CLIENT SHUTTING DOWN---");
            // shutdown all threads
            shutdownConnection();
            mouseThread.shutdown();
            //eventThread.shutdown();
            //    eventThread.join();

            //socket.close();
            
        }
        catch( Exception e ) {
            e.printStackTrace();

            shutdownConnection();
            mouseThread.shutdown();
//            eventThread.shutdown();
            try {
//                eventThread.join(); 
                socket.close();
            } catch (Exception ee ) {
                // WTF??
                ee.printStackTrace();
            }

        }
    }

    //======================================================
    public boolean tryLogin() {
        retryLogin = true;
        closeConnection = true;
        try {
            socket = new Socket(server,11999);
            dos = new DataOutputStream( socket.getOutputStream() );
            dis = new DataInputStream( socket.getInputStream() );
            
            while( retryLogin )
            {
                if( login() )
                {
                    retryLogin = false;
                    closeConnection = false;
                    System.out.println("--- CLIENT LOGGED IN ---");
                    eventThread = new IncomingEventsThread( this, dis );
                    eventThread.start();
                    return true;
                } else {
                    // break;
                }
            }
        } catch (Exception e ) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    //======================================================
    private boolean login() {

        if( queryUserPass() )
        {
            try {
                dos.writeByte(ClientRequest.LOGIN);
                byte[] str;

                str = username.getBytes("UTF-8");
                dos.writeInt(str.length);
                dos.write(str, 0, str.length);

                str = password.getBytes("UTF-8");
                dos.writeInt(str.length);
                dos.write(str, 0, str.length);

                dos.writeLong(System.currentTimeMillis());

                dos.flush();
                if (!waitForResponse())
                    return false;

                byte response = dis.readByte();
                switch (response) {
                    case ServerResponse.USER_LOGGED_IN:
                        userid = dis.readInt();
                        System.out.println("SERVER RESPONSE: LOGGED IN, ID: " +
                                userid);
                        if (userid == 0) {
                            queryRetry("Username or Pass Invalid!\n");
                            // retryLogin = false;
                            return false;
                        }
                        return true;
                    case ServerResponse.USER_PASS_INVALID:
                        queryRetry("Username or Pass Invalid!\nPlease " +
                                   "register with the Administrator");
                        return false;
                    case ServerResponse.USER_ALREADY_LOGGED_IN:
                        queryRetry("User of same name already logged in!");
                        return false;
                }

/*
            // process all the incoming section information
            boolean run = true;
            while( run ) {
                response = dis.readByte();
                switch( response ) {
                case corelyzer.plugin.andrill.ServerResponse.NEW_SECTION:
                    IncomingEventsThread.processNewSection(dis,this);
                    break;
                case corelyzer.plugin.andrill.ServerResponse.SPLIT_CORE_AVAILABLE:
                    IncomingEventsThread.processNewSplitCore(dis,this);
                    break;
                case corelyzer.plugin.andrill.ServerResponse.WHOLE_CORE_AVAILABLE:
                    IncomingEventsThread.processNewWholeCore(dis,this);
                    break;
                case corelyzer.plugin.andrill.ServerResponse.DATASET_LIST:
                    IncomingEventsThread.processDatasetList(dis,this);
                    break;
                case corelyzer.plugin.andrill.ServerResponse.UPDATE_LOCAL_SECTION_LIST:
                    System.out.println("FINSHED GETTING SECTION INFO!" +
                                       " UPDATING LOCAL SELECTIONS!");
                    getPlugin().determinePreloadedSections();
                    run = false;
                    break;
                }
            }
*/

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //======================================================
    private void updateMousePos(String desc) {
        String[] data = desc.split("\t");
        float x, y;
        // get positions, convert from inches to meters
        x = Float.parseFloat(data[0]);
        y = Float.parseFloat(data[1]);
        x *= 2.54f / 100.0f;
        y *= 2.54f / 100.0f;
        //System.out.println("Converted from inches to meters: " + x + "," + y);
        mouseThread.setMousePosition(x,y);
    }

    //======================================================
    public void requestUpdateSetTables(String sd) {
        try {
            dosLock.lock();
            byte[] buf = sd.getBytes("UTF-8");
            dos.writeByte( ClientRequest.LIST_TABLES);
            dos.writeInt ( buf.length );
            dos.write    ( buf, 0, buf.length );
            dos.flush();
            dosLock.unlock();
        } catch (Exception e) {
            System.out.println("Failed to request update dataset tables list");
            e.printStackTrace();
        }        
    }

    //======================================================
    public void relayStartGraphs(String sd, int field) {
        try {
            dosLock.lock();
            byte[] buf = sd.getBytes("UTF-8");
            dos.writeByte( ClientRequest.RELAY_START_GRAPHS);
            dos.writeInt ( field );
            dos.writeInt ( buf.length );
            dos.write    ( buf, 0, buf.length );
            dos.flush();
            dosLock.unlock();
        } catch (Exception e) {
            System.out.println("Failed to send start graphs relay!");
            e.printStackTrace();
        }
    }

    //======================================================
    public void relayMakeGraph(String sd, String tb, int field ) {
        try {
            dosLock.lock();
            byte[] sbuf = sd.getBytes("UTF-8");
            byte[] tbuf = tb.getBytes("UTF-8");

            dos.writeByte( ClientRequest.RELAY_MAKE_GRAPH );
            dos.writeInt ( sbuf.length );
            dos.write    ( sbuf, 0, sbuf.length );
            dos.writeInt ( tbuf.length );
            dos.write    ( tbuf, 0, tbuf.length );
            dos.writeInt ( field );
            dos.flush();

            dosLock.unlock();
        } catch (Exception e) {
            System.out.println("Failed to relay make graph message!");
            e.printStackTrace();
        }
    }

    //======================================================
    public void makeTableRequest(String setname, String tablename) {
        try {
            dosLock.lock();
            dos.writeByte( ClientRequest.TABLE_DATA );

            byte[] buf = setname.getBytes("UTF-8");
            dos.writeInt( buf.length );
            dos.write( buf, 0, buf.length );

            buf = tablename.getBytes("UTF-8");
            dos.writeInt( buf.length );
            dos.write( buf, 0, buf.length );

            dos.flush();
            dosLock.unlock();
        } catch(Exception e) {
            System.out.println("Failed to send table data request!");
            e.printStackTrace();
        }
    }

    //======================================================
    public void makeSplitCoreRequest(int id) {
        try {
            dosLock.lock();
            dos.writeByte( ClientRequest.RELAY_LOAD_SECTION );
            dos.writeInt( 0 );
            dos.writeInt( id );
            dos.flush();
            dosLock.unlock();
        } catch (Exception e) {
            System.out.println("Request split core failed!");
            e.printStackTrace();
        }
    }

    //======================================================
    public void makeWholeCoreRequest(int id) {
        try {
            dosLock.lock();
            dos.writeByte( ClientRequest.RELAY_LOAD_SECTION );
            dos.writeInt( 1 );
            dos.writeInt( id );
            dos.flush();
            dosLock.unlock();
        } catch (Exception e) {
            System.out.println("Request whole core failed!");
            e.printStackTrace();
        }
    }

    //======================================================
    // private void postTrackCreated(String name) {

    // }

    //======================================================
    public void postNewChat(int tid, int sid, int groupid, int typeid, float px, float py) {
        try {

            dosLock.lock();

            dos.writeByte( ClientRequest.NEW_CHAT );
            dos.writeInt( tid );
            dos.writeInt( sid );
            dos.writeInt( groupid );
            dos.writeInt( typeid );
            dos.writeFloat( px );
            dos.writeFloat( py );
            dos.flush();

            dosLock.unlock();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //======================================================
    public void postNewChatEntry( int split, int secId,  String url, 
                                  float depth, String entry ) {
        try {
            dosLock.lock();
            
            System.out.println("POST NEW ENTRY EVENT");
            byte[] ubuf = url.getBytes("UTF-8");
            byte[] ebuf = entry.getBytes("UTF-8");

            dos.writeByte( ClientRequest.NEW_CHAT_ENTRY );
            dos.writeInt( split );
            dos.writeInt( secId );
            dos.writeFloat( depth );
            dos.writeInt( ubuf.length );
            dos.write   ( ubuf, 0, ubuf.length );
            dos.writeInt( ebuf.length );
            dos.write   ( ebuf, 0, ebuf.length );
            dos.flush   ();

            dosLock.unlock();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //======================================================
    private void shutdownConnection() {
        try{

            System.out.println("SENDING LOGOUT MESSAGE");
            dos.writeByte( ClientRequest.LOGOUT );
            dos.flush();

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //======================================================
    public void releaseShutdownPermit() {
        try {
            logoutPermit.release();
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    //======================================================
    private boolean waitForResponse() {
        try {
            /*
            while( dis.available() <= 0 );
            return true;
            */
            
            while(true) {
                if(dis.available() > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    //======================================================
    private boolean queryUserPass() {
        final AuthDialog authDialog =
                new AuthDialog(plugin.getFrame(),
                        "Please Enter User Name and Password");

        authDialog.setUsername("");
        authDialog.setPassword("");

        authDialog.setOKActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isContinue = true;
                    authDialog.setVisible(false);
                }
            }
        );

        authDialog.setCancelActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isContinue = false;
                    authDialog.setVisible(false);
                }
            }
        );

        Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
        int loc_x = scrnsize.width / 2 -  (authDialog.getSize().width / 2);
        int loc_y = scrnsize.height / 2 - (authDialog.getSize().height / 2);
        authDialog.setLocation(loc_x, loc_y);

        authDialog.pack();
        // authDialog.setVisible(true);

        while (true)
        {
            authDialog.setVisible(true);

            if( !isContinue ) break;

            if( authDialog.getUsername().equals("") ||
                authDialog.getPassword().equals(""))
            {
                System.out.println(
                        "-- [INFO] Either username or password is empty!");
            }
            else {
                break;
            }
        }

        username = authDialog.getUsername();
        password = new String(authDialog.getPassword());

        System.out.println("User Name: " + username);
        System.out.println("Pass: XXXXXXXX");

        authDialog.dispose();

        return isContinue;
    }

    //======================================================
    private void queryRetry(String msg) {
        // boolean response = false;
        RetryDialog dlg = new RetryDialog(msg);

        Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
        int loc_x = scrnsize.width / 2 -  (dlg.getSize().width / 2);
        int loc_y = scrnsize.height / 2 - (dlg.getSize().height / 2);
        dlg.setLocation(loc_x, loc_y);
        
        dlg.setVisible(true);
        retryLogin = dlg.getResponse();
        dlg.dispose();
    }

    //======================================================
    public boolean sendHeartbeat() {
        if( closeConnection ) return false;

//        System.out.println("LOCKING WAIT");
        dosLock.lock();
        try {
            dos.writeByte( ClientRequest.HEART_BEAT);
            dos.flush();
        } catch (Exception e) {
            dosLock.unlock();
            System.out.println("FAILED TO SEND HEARTBEAT!!!");
            return false;
        }

//        System.out.println("HEARTBEAT SENT UNLOCKING");
        dosLock.unlock();

        return true;
    }

    //======================================================
    public void beginShutdown() {
        closeConnection = true;
    }
}

//*************************************************************************//
class RetryDialog extends JDialog implements ActionListener {

    private boolean response;
    JButton okbtn;

    public RetryDialog(String msg) {
        setModal(true);

        GridBagLayout gbl = new GridBagLayout();
        setLayout( gbl );
        GridBagConstraints c = new GridBagConstraints();
        JLabel label;
        
        label = new JLabel(msg);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(label,c);
        add(label);

        label = new JLabel("Retry Login?");
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(label,c);
        add(label);

        JButton btn;
        btn = new JButton("Retry");
        btn.addActionListener( this);
        okbtn = btn;

        gbl.setConstraints(btn,c);
        add(btn);

        btn = new JButton("Quit");
        btn.addActionListener(this);

        gbl.setConstraints(btn,c);
        add(btn);

        pack(); // layout();
        setSize(300,150);
        setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
        response = false;
    }


    public void actionPerformed(ActionEvent e) {
        
        if( e.getSource() == okbtn )
            response = true;

        setVisible(false);
    }

    public boolean getResponse() { return response; }

}

//*************************************************************************//
class SharedMiceClientThread extends Thread {

    private DatagramSocket mouseSocket;
    private boolean keepRunning;
    private long  lastUpdate;
    public  float mousePos[] = { 0.0f, 0.0f };
    private CorelyzerSessionClient sessionClient;
    private CorelyzerSessionClientPlugin plugin;
    private InetAddress serverAddress;

    private static final int BUF_SIZE = 20;

    public SharedMiceClientThread(CorelyzerSessionClient csc,
                                  CorelyzerSessionClientPlugin p,
                                  InetAddress address, int port) {
        try {
            sessionClient = csc;
            plugin = p;
            serverAddress = address;
            mouseSocket = new DatagramSocket(11997);
            keepRunning = true;
            Date date = new Date();
            lastUpdate = date.getTime();
        }
        catch( Exception e ) {
            keepRunning = false;
        }
    }

    public void setMousePosition(float x, float y) {
        mousePos[0] = x;
        mousePos[1] = y;
    }

    public void run() {
        byte[] inbuf = new byte[BUF_SIZE];
        DatagramPacket inpkt = new DatagramPacket( inbuf, BUF_SIZE);
        ByteArrayInputStream is = new ByteArrayInputStream(inbuf);
        DataInputStream dis = new DataInputStream(is);
        is.mark( 24 );

        byte[] outbuf; // = new byte[BUF_SIZE];
        DataOutputStream dos;
        DatagramPacket outpkt;
        try {
            mouseSocket.setSoTimeout(100);
        }
        catch( Exception e ) {
            return;
        }

        while( keepRunning ) {
            try {
                sleep(100);

                long time = System.currentTimeMillis();
//                System.out.println("Time stamp: " + time + " x,y: " + + 
//                                   mousePos[0] + ", " + mousePos[1]);
                // send out the current mouse pos with incrementing time index
                ByteArrayOutputStream baos = 
                    new ByteArrayOutputStream(BUF_SIZE);
                dos = new DataOutputStream( baos);
                dos.writeInt( sessionClient.getUserID());
                dos.writeFloat(mousePos[0]);
                dos.writeFloat(mousePos[1]);
                dos.writeLong( time );
                dos.flush();
                outbuf = baos.toByteArray();

                outpkt = new DatagramPacket( outbuf, BUF_SIZE, 
                                             serverAddress, 11998);
                mouseSocket.send(outpkt);

                // empty out the UDP socket, if the time has passed 1/10th 
                // of a second, send out the current mouse pos again, 
                // and then continue on
                try {
                    long starttime = System.currentTimeMillis();
                    while( System.currentTimeMillis() - starttime < 100 ) {
                        mouseSocket.receive(inpkt);

                        is.reset();
                        int usrid;
                        float xpos, ypos;
                        long timestamp;
                        usrid = dis.readInt();
                        xpos  = dis.readFloat();
                        ypos  = dis.readFloat();
                        timestamp = dis.readLong();
                        plugin.updateUserPosition( usrid, xpos, ypos,
                                                   timestamp);
                    }
                    
                    // update last update time
                    
                    lastUpdate = System.currentTimeMillis();
                }
                catch( Exception e ) {
                    CorelyzerApp.getApp().updateGLWindows();
                    if( mouseSocket.isClosed()) return;
                    // do nothing
//                    System.out.println("No incoming data from server");
                }

            } 
            catch( Exception e ) {
                CorelyzerApp.getApp().updateGLWindows();
                if( mouseSocket.isClosed()) return;
                // check last update time, if more than 30 seconds then
                // stop and shutdown client
                if( System.currentTimeMillis() - lastUpdate > 30000 )
                {
                    keepRunning = false;
                    sessionClient.beginShutdown();
                }
            }
        }
        
    }

    public void shutdown() {
        keepRunning = false;
        try {
            mouseSocket.close();
        }
        catch(Exception e) {
            // whatever
        }
    }


    private void processPacket( DatagramPacket pkt) {

    }

}

//*************************************************************************//
class IncomingEventsThread extends Thread {

    private DataInputStream dis;
    private CorelyzerSessionClient sessionClient;
    private boolean keepRunning;

    //===============================================================
    public IncomingEventsThread(CorelyzerSessionClient csc, 
                                DataInputStream in) {
        dis = in;
        sessionClient = csc;
        keepRunning = true;
    }

    //===============================================================
    public void run() {
        while( keepRunning ) {
            // blocking read
            byte event;
            try {
                
                sessionClient.lockDIS();
        
                event = dis.readByte();
                checkMessages(event);

                sessionClient.unlockDIS();
            }
            catch( SocketTimeoutException ste ) {
                System.out.println("Incoming data timeout...continuing");
                sessionClient.unlockDIS();
            }
            catch ( SocketException e ) {
                sessionClient.unlockDIS();
                keepRunning = false;
            }
            catch( EOFException e ) {
                sessionClient.unlockDIS();
                keepRunning = false;
            }
            catch( Exception e ) {
                sessionClient.unlockDIS();
                e.printStackTrace();
            }
        }
    }

    //===============================================================
    public boolean checkMessages(byte event) {
        boolean processed = false;

        if( event != ServerResponse.SPLIT_CORE_AVAILABLE &&
            event != ServerResponse.WHOLE_CORE_AVAILABLE &&
            event != ServerResponse.NEW_SECTION
          )
        {
            System.out.println("INCOMING EVENT FROM SERVER: " + event );
        }

        switch( event ) {
        case ServerResponse.USER_LOGGED_IN:
            System.out.println("OTHER USER LOGGED IN!!");
            processUserLoggedIn();
            processed = true;
            break;
        case ServerResponse.USER_LOGGED_OUT:
            System.out.println("OTHER USER LOGGED OUT!!");
            processUserLoggedOut();
            processed = true;
            break;
        case ServerResponse.HEART_BEAT_REQUEST:
//            System.out.println("HEARTBEAT REQUEST");
            if( !sessionClient.sendHeartbeat() )
                keepRunning = false;
            processed = true;
            break;
        case ServerResponse.TRACK_CREATED:
            System.out.println("OTHER USER CREATED A TRACK!!");
            processIncomingTrackName();
            processed = true;
            break;
        case ServerResponse.NEW_SECTION:
            processNewSection(dis,sessionClient);
            processed = true;
            break;
        case ServerResponse.SPLIT_CORE_AVAILABLE:
            processNewSplitCore(dis,sessionClient);
            processed = true;
            break;
        case ServerResponse.WHOLE_CORE_AVAILABLE:
            processNewWholeCore(dis,sessionClient);
            processed = true;
            break;
        case ServerResponse.NEW_DATASET_AVAILABLE:
            System.out.println("NEW_DATASET AVAILABLE!!");
            processNewDatasetAvailable(dis,sessionClient);
            processed = true;
        case ServerResponse.DATASET_LIST:
            processDatasetList(dis,sessionClient);
            processed = true;
            break;
        case ServerResponse.TABLE_LIST:     // let sessionClient handle
            processTableList(dis,sessionClient);
            break;
        case ServerResponse.TABLE_LIST_ERR: // let sessionClient handle
            break;
        case ServerResponse.TABLE_DATA:     // let sessionClient handle
            processTableData(dis,sessionClient);
            break;
        case ServerResponse.RELAY_LOAD_SECTION:
            processLoadSection(dis,sessionClient);
            break;
        case ServerResponse.RELAY_START_GRAPHS:
            processRelayStartGraphs(dis,sessionClient);
            break;
        case ServerResponse.RELAY_MAKE_GRAPH:
            processRelayMakeGraph(dis,sessionClient);
            break;
        case ServerResponse.TABLE_DATA_ERR: // let sessionClient handle
            break;
        case ServerResponse.NEW_CHAT:
            processNewChat(dis,sessionClient);
            break;
        case ServerResponse.NEW_CHAT_ENTRY:
            processNewChatEntry(dis,sessionClient);
            break;
        case ServerResponse.UPDATE_LOCAL_SECTION_LIST:
            sessionClient.getPlugin().determinePreloadedSections();
            processed = true;
            break;
        }

        return processed;
    }

    //===============================================================
    public void shutdown() {
        keepRunning = false;
    }

    //===============================================================
    private static void processLoadSection( 
        DataInputStream dis, CorelyzerSessionClient sessionClient) {
        try {
            int split = dis.readInt();
            int id    = dis.readInt();

            if( split == 0 )
                sessionClient.getPlugin().loadSplitCore(id);
            else
                sessionClient.getPlugin().loadWholeCore(id);

        } catch( Exception e) {
            System.err.println("Exception in processLoadSection()");
        }
    }

    //===============================================================
    private void processUserLoggedIn() {
        try {
            int id = dis.readInt();
            int len = dis.readInt();
            byte[] data = new byte[len];
            // dis.read( data, 0, len );
            dis.readFully(data);

            System.out.println("\n--Recognizing other client: " + id + "--\n");
            sessionClient.getPlugin().clientLoggedIn(
                id, new String(data,"UTF-8"));
        }
        catch( Exception e ) {
            e.printStackTrace();
        }

    }

    //===============================================================
    private void processUserLoggedOut() {
        try {
            int id = dis.readInt();
            System.out.println("\n--Recognizing client: " + id +" logged out");

            if( id == sessionClient.getUserID() ) {
                System.out.println("SELF LOGOUT");
                keepRunning = false;
                return;
            }

            System.out.println("\n--Recognizing client: " + id +" logged out");
            sessionClient.getPlugin().clientLoggedOut(id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //===============================================================
    private void processIncomingTrackName() {
        try {
            int userid = dis.readInt();
            int len = dis.readInt();
            byte[] str = new byte[len];
            // dis.read( str, 0, len );
            dis.readFully(str);
            String name = new String( str, "UTF-8");

            System.out.println("USER " + userid + " CREATED TRACK NAMED: " +
                               name );

            CorelyzerApp.getApp().createTrack(name);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    //===============================================================
    public static void processNewSection(
        DataInputStream dis, CorelyzerSessionClient sessionClient) {

        int len;
        byte[] buf;
        String name;
        float depth, length;
        int globalId;

        try {
            globalId = dis.readInt();
            depth    = dis.readFloat();
            length   = dis.readFloat();
            len      = dis.readInt();
            buf      = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            name     = new String(buf,"UTF-8");

            sessionClient.getPlugin().addNewSection( globalId, depth, length,
                                                     name);
            
        } catch( Exception e) {
            e.printStackTrace();
        }

    }


    //===============================================================
    public static void processNewSplitCore(
        DataInputStream dis, CorelyzerSessionClient sessionClient) {

        int    len;
        byte[] buf;
        String name, url;
        float  depth, length, dpi_x, dpi_y;
        int    globalId;

        try {
            globalId = dis.readInt();
            depth    = dis.readFloat();
            length   = dis.readFloat();
            dpi_x    = dis.readFloat();
            dpi_y    = dis.readFloat();
            len      = dis.readInt();
            buf      = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            name     = new String(buf,"UTF-8");
            len      = dis.readInt();
            buf      = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            url      = new String(buf,"UTF-8");

            sessionClient.getPlugin().addSplitCoreSection(globalId,depth,
                                                          length,dpi_x,dpi_y,
                                                          name,url);

        } catch( Exception e) {
            e.printStackTrace();
        }
        
    }

    //===============================================================
    public static void processNewWholeCore(
        DataInputStream dis, CorelyzerSessionClient sessionClient) {

        int    len;
        byte[] buf;
        String name, url;
        float  depth, length, dpi;
        int    globalId;

        try {
            globalId = dis.readInt();
            depth    = dis.readFloat();
            length   = dis.readFloat();
            dpi      = dis.readFloat();
            len      = dis.readInt();
            buf      = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            name     = new String(buf,"UTF-8");
            len      = dis.readInt();
            buf      = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            url      = new String(buf,"UTF-8");

            sessionClient.getPlugin().addWholeCoreSection(globalId,depth,
                                                          length,dpi,name,url);
            
        } catch( Exception e) {
            e.printStackTrace();
        }
    }

    //===============================================================
    public static void processNewDatasetAvailable(
        DataInputStream dis, CorelyzerSessionClient sessionClient) {

        try {
            
            String sdname;
            byte[] buf;
            int len;
            int fcount;
                
            len = dis.readInt();
            buf = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            sdname = new String(buf,"UTF-8");
//            System.out.println("Dataset: " + sdname + " with fields: ");
            SessionDataset sd = new SessionDataset( sdname );
            sd.removeAllChildren();

            fcount = dis.readInt();
            sd.colors = new float[fcount][];
            sd.types = new int[fcount];
            sd.display = new boolean[fcount];

            for( int l = 0; l < fcount; l++) {
                sd.colors[l] = new float[3];
                sd.colors[l][0] = sd.colors[l][1] = sd.colors[l][2] = 1.0f;
                sd.types[l] = 0;
                sd.display[l] = false;

                len = dis.readInt();
                buf = new byte[len];
                // dis.read(buf,0,len);
                dis.readFully(buf);
                sdname = new String(buf,"UTF-8");
//                System.out.print(sdname + "\t");
                sd.addField( sdname );
                    
                Float min, max;
                min = dis.readFloat();
                max = dis.readFloat();
                sd.mins.add( min );
                sd.maxs.add( max );
                sd.userMins.add( min );
                sd.userMaxs.add( max );

            }
            
            sessionClient.getPlugin().addDataset(sd);

        } catch( Exception e) {
            e.printStackTrace();
        }
    }

    //===============================================================
    public static void processDatasetList(
        DataInputStream dis, CorelyzerSessionClient sessionClient) {

        try {
            int nsets = dis.readInt();
            
            for( int k = 0; k < nsets; k++) {
                
                String sdname;
                byte[] buf;
                int len;
                int fcount;
                
                len = dis.readInt();
                buf = new byte[len];
                // dis.read( buf, 0, len );
                dis.readFully(buf);
                sdname = new String(buf,"UTF-8");
//                System.out.println("Dataset: " + sdname + " with fields: ");
                SessionDataset sd = new SessionDataset( sdname );
                sd.removeAllChildren();

                fcount = dis.readInt();
                sd.colors = new float[fcount][];
                sd.types = new int[fcount];
                sd.display = new boolean[fcount];

                for( int l = 0; l < fcount; l++) {
                    sd.colors[l] = new float[3];
                    sd.colors[l][0] = sd.colors[l][1] = sd.colors[l][2] = 1.0f;
                    sd.types[l] = 0;
                    sd.display[l] = false;

                    len = dis.readInt();
                    buf = new byte[len];
                    // dis.read(buf,0,len);
                    dis.readFully(buf);
                    sdname = new String(buf,"UTF-8");
//                    System.out.print(sdname + "\t");
                    if( l > 0 )
                        sd.addField( sdname );
                    
                    Float min, max;
                    min = dis.readFloat();
                    max = dis.readFloat();
                    sd.mins.add( min );
                    sd.maxs.add( max );
                    sd.userMins.add( min );
                    sd.userMaxs.add( max );

                }

                sessionClient.getPlugin().addDataset(sd);
            }
        }
        catch (Exception e) {
            System.out.println("ERROR: ");
            e.printStackTrace();
        }
    }

    //===============================================================
    public static void processTableList( 
        DataInputStream dis, CorelyzerSessionClient sessionClient ) {

        try {
            System.out.println("Processing table listing");
            int len;
            byte[] buf;
            String setname, tablename;
            int ntables;
            Vector< String > tnames = new Vector< String >();
        
            len = dis.readInt();
            buf = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            setname = new String( buf, "UTF-8");
            
            ntables = dis.readInt();
            for( int i = 0; i < ntables; i++) {
                len = dis.readInt();
                buf = new byte[len];
                // dis.read( buf, 0, len);
                dis.readFully(buf);

                tablename = new String(buf,"UTF-8");

                tnames.add( tablename );
            }

          sessionClient.getPlugin().updateDatasetTableList( setname, tnames);
          System.out.println("DONE WITH DATASET TABLE LIST!" + ntables + ", " +
                             tnames.size());

          System.out.println("Done calling update!");

          tnames.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //===============================================================
    public static void processTableData(
        DataInputStream dis, CorelyzerSessionClient sessionClient ) {

        System.out.println("Getting table data");

        try {

            String setname, tablename;
            int len;
            byte[] buf;

            len = dis.readInt();
            buf = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            setname = new String(buf,"UTF-8");
            
            len = dis.readInt();
            buf = new byte[len];
            // dis.read( buf, 0, len);
            dis.readFully(buf);
            tablename = new String(buf,"UTF-8");

            int nfields = dis.readInt();
            int nrows   = dis.readInt();

            Vector< boolean[] > valids = new Vector< boolean[] >();
            Vector< float[]   > values = new Vector< float[]   >();

            boolean[] bs;
            float[]   vs;

            for(int r = 0; r < nrows; r++) {
                bs = new boolean[nfields];
                vs = new float[nfields];
                for( int f = 0; f < nfields; f++) {
                    bs[f] = dis.readBoolean();
                    vs[f] = dis.readFloat();
                }
                valids.add(bs);
                values.add(vs);
            }

            sessionClient.getPlugin().createDataTable( setname, tablename,
                                                       nfields, valids,
                                                       values);
            values.clear();
            valids.clear();
        } catch (Exception e) {
            System.out.println("Couldn't get all the table data!!!");
            e.printStackTrace();
        }
    }

    //===============================================================
    public static void processRelayStartGraphs(
        DataInputStream dis, CorelyzerSessionClient sessionClient ) {
        System.out.println("Getting relay start graphs!!!");
 
        try {
            int field = dis.readInt();
            int len = dis.readInt();
            System.out.println("Field: " + field + " String Len: " + len );
            System.out.flush();
            byte[] buf = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            String setname = new String(buf,"UTF-8");
            sessionClient.getPlugin().startGraphRequests( setname, field );
            System.out.println("Started making graph requests. for set:" +
                setname );
            System.out.flush();
        } catch( Exception e) {
            System.out.println("Couldn't process relay start graphs");
            e.printStackTrace();
        }
    }

    //===============================================================
    public static void processRelayMakeGraph(
        DataInputStream dis, CorelyzerSessionClient sessionClient ) {
        try {
            int len;
            byte[] buf;
            String setname, tablename;
            int field;
            System.out.println("Getting relay make graph!!!");
            System.out.flush();
 
            len = dis.readInt();

            System.out.println("Setname length: " + len);
            System.out.flush();

            buf = new byte[len];
            // dis.read(buf,0,len);
            dis.readFully(buf);
            setname = new String(buf,"UTF-8");
            
            len = dis.readInt();
            
            System.out.println("Tablename length: " + len);
            System.out.flush();
            
            buf = new byte[len];
            // dis.read(buf,0,len);
            dis.readFully(buf);
            tablename = new String(buf,"UTF-8");
            
            field = dis.readInt();

            System.out.println("Creating graph!");
            System.out.flush();

            sessionClient.getPlugin().createGraph( setname, tablename, field);

            System.out.println("Graph should be made!");
            System.out.flush();
        } catch (Exception e) {
            System.out.println("Failed to receive make graph relay!");
            e.printStackTrace();
        }
	CorelyzerApp.getApp().updateGLWindows();
    }

    //=================================================================
    public static void processNewChat(
        DataInputStream dis, CorelyzerSessionClient sessionClient )
    {
        System.out.println("Process new chat from server...");

        // receive global split(trackid), sectionid, chat(marker)id etc.
        int split, secid, chatid;

        int len, uid, group, type;
        long created, modified;
        float depthX, depthY;
        byte[] buf;
        String url;

        try {
            split  = dis.readInt();
            secid  = dis.readInt();
            chatid = dis.readInt();
            uid = dis.readInt();
            group = dis.readInt();
            type = dis.readInt();
            depthX = dis.readFloat();
            depthY = dis.readFloat();

            created = dis.readLong();
            modified = dis.readLong();

            len = dis.readInt();
            buf = new byte[len];
            // dis.read(buf,0,len);
            dis.readFully(buf);
            url = new String(buf,"UTF-8");

            sessionClient.getPlugin().assignChatGlobalID( split,  secid,
                                                          chatid, depthX, depthY,
                                                          url, uid,
                                                          group, type,
                                                          created, modified);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //=================================================================
    public static void processNewChatEntry (
        DataInputStream dis, CorelyzerSessionClient sessionClient ) {

        try {

            int split, secid, globalId;
            long modified;

            split = dis.readInt();
            secid = dis.readInt();
            globalId = dis.readInt();
            modified = dis.readLong();

            sessionClient.getPlugin().chatUpdated( split, secid, globalId,
                                                   modified );
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
