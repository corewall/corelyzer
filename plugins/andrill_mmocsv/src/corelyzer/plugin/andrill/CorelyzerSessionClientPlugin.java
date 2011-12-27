package corelyzer.plugin.andrill;

import corelyzer.data.MarkerType;
import corelyzer.helper.ExampleFileFilter;
import corelyzer.graphics.SceneGraph;
import corelyzer.helper.URLRetrieval;
import corelyzer.plugin.CorelyzerPlugin;
import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.plugin.CorelyzerPluginManager;
import corelyzer.ui.ColorChooser;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.CorelyzerGLCanvas;
import corelyzer.ui.annotation.freeform.CRAnnotationWindow;
import corelyzer.util.TableSorter;

import javax.media.opengl.GL;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class CorelyzerSessionClientPlugin extends CorelyzerPlugin
        implements ActionListener, TreeSelectionListener {

    private JFrame frame;
    private JPanel mainPanel;
    private JButton startbtn;
    private JButton logoutbtn;
    private JButton loadbtn;
    private JButton savebtn;
    private JComboBox serverfield;
    private JTabbedPane corePane;

    private JPanel splitPanel;
    private JPanel wholePanel;
    private JPanel chatsPanel;

    private JMenuItem  addDiscussion;
    private JMenuItem  editDiscussion;
    private JProgressBar statBar;

    // private ProgressDialog pdlg;

    private JTree splitCoreChats;
    private JTree wholeCoreChats;
    private JTree datasetTree;
    private JPanel fieldPropsPanel;
    private DefaultMutableTreeNode datasetTreeRoot;

    private JFormattedTextField minField;
    private JFormattedTextField maxField;
    private JButton   colorBtn;
    private JComboBox typeList;
    private JCheckBox displayCheck;
    private JCheckBox autoSplitCheck;
    private JCheckBox autoWholeCheck;
    private JButton   applyBtn;
    private ColorChooser colorchooser;

    private SessionDataset selectedSet;
    private int            selectedField;

    private SplitCoreTable sct;
    private WholeCoreTable wct;
    private ChatTable      ct;

    private JScrollPane    splitScrollPane;
    private JScrollPane    wholeScrollPane;
    private JScrollPane    chatScrollPane;
    private JScrollPane    datasetScrollPane;
    
    //------------ 

    private LinkedList<CorelyzerPluginEvent> outQueue;
    private LinkedList<ClientData> clientList;
    private Semaphore queuePermit;
    private ReentrantLock userTableLock;


    ImageLoadThread imageThread;

    private CorelyzerSessionClient csc;
    private int collabSplitCoreTrack;
    private int collabWholeCoreTrack;


    private Vector< SessionSection > sectionVec;
    private Vector< SessionDataset > datasetVec;
    private Vector< SessionChat    > chatVec;

    // split core id -> global section id
    private Hashtable< Integer, Integer > splitGlobalTable;

    // whole core id -> global section id
    private Hashtable< Integer, Integer > wholeGlobalTable;

    // freedraw id -> client data
    private Hashtable< Integer, ClientData > freeDrawUserDataTable;
    // user id -> client data
    private Hashtable< Integer, ClientData > userIdDataTable;

    // freedraw id -> discussion data
    private Hashtable< Integer, DiscussionData > freeDrawDiscussionTable;

    // marker id -> global marker id
    private Hashtable< Integer, Integer > chatGlobalTable;

    private SplitCoreTable splitTable;

    private JFileChooser chooser;
    
    //===========================================================
    public CorelyzerSessionClientPlugin() {
        collabSplitCoreTrack = -1;
        collabWholeCoreTrack = -1;

        outQueue = new LinkedList< CorelyzerPluginEvent >();
        userTableLock = new ReentrantLock();
        queuePermit = new Semaphore(1,true);

        sectionVec = new Vector< SessionSection >();
        datasetVec = new Vector<SessionDataset>();
        chatVec    = new Vector< SessionChat >();

        splitGlobalTable      = new Hashtable< Integer, Integer >();
        wholeGlobalTable      = new Hashtable< Integer, Integer >();
        chatGlobalTable       = new Hashtable< Integer, Integer >();
        freeDrawUserDataTable = new Hashtable< Integer, ClientData >();
        userIdDataTable       = new Hashtable< Integer, ClientData >();

        datasetTreeRoot = new DefaultMutableTreeNode(new String("Sets"), true);

        try {
            queuePermit.acquire();
        }
        catch( Exception e ) {
            // VERY STRANGE!!!!
        }

        String dataRoot = System.getProperty("user.home");
        chooser = new JFileChooser(dataRoot);
        chooser.setMultiSelectionEnabled(false);

        System.out.println("Session Client Plugin Created!");
    }

    //===========================================================
    private void PositionWidget( Component c, SpringLayout l, int x, int y) {
        l.putConstraint( SpringLayout.WEST, c, x , SpringLayout.WEST,
                         frame.getContentPane());
        l.putConstraint( SpringLayout.NORTH, c, y, SpringLayout.NORTH,
                         frame.getContentPane());
    }

    //===========================================================
    private void PositionWidget( Component parent, Component c, SpringLayout l,
                                 int x, int y) {
        l.putConstraint( SpringLayout.WEST, c, x , SpringLayout.WEST,
                         parent);
        l.putConstraint( SpringLayout.NORTH, c, y, SpringLayout.NORTH,
                         parent);
    }

    //===========================================================
    public boolean init( Component parentUI ) {
        // System.out.println("Plugin making UI");
        frame = new JFrame("Collaborative Corelyzer");
        frame.setAlwaysOnTop(true);
        frame.setSize(550,700);

        Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
        int loc_x = scrnsize.width/2 - (frame.getSize().width/2);
        int loc_y = scrnsize.height/2 - (frame.getSize().height/2);
        frame.setLocation(loc_x, loc_y);

        mainPanel = new JPanel();
        mainPanel.setSize( 550, 700);
        frame.add( mainPanel );

        BorderLayout layout = new BorderLayout();
        mainPanel.setLayout(layout);

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel label = new JLabel("Server: ");
        p.add(label);

        p.setBorder( BorderFactory.createTitledBorder("Server Connection"));
        serverfield = new JComboBox();
        
        File serverfile = new File("server_list.txt");
        if (serverfile.exists()) {
            try {
                BufferedReader reader =
                    new BufferedReader(new FileReader(serverfile));
                String line = "";
                while( (line = reader.readLine()) != null )
                    serverfield.addItem(line);

                reader.close();
            } catch (IOException e) {
                System.err.println("IOException in reading server list file");
            }
        }
        else {
            System.out.println("fail to load server_list.txt file");
            serverfield.addItem("data.andrill.org");
            serverfield.addItem("192.168.1.1");
            serverfield.addItem("localhost");
        }
        
        serverfield.setEditable(true);
        serverfield.setAlignmentY(Component.CENTER_ALIGNMENT);
        p.add(serverfield);

        startbtn = new JButton("Connect");
        startbtn.addActionListener(this);
        p.add(startbtn);

        logoutbtn = new JButton("Logout");
        logoutbtn.addActionListener(this);
        logoutbtn.setEnabled(false);
        p.add(logoutbtn);
        
        loadbtn = new JButton("Load Log");
        loadbtn.addActionListener(this);
        loadbtn.setEnabled(false);
        p.add(loadbtn);
        
        savebtn = new JButton("Save Log");
        savebtn.addActionListener(this);
        savebtn.setEnabled(false);
        p.add(savebtn);
        
        mainPanel.add(p, BorderLayout.NORTH);

        corePane = new JTabbedPane();
        corePane.setPreferredSize( new Dimension(400,565));
        mainPanel.add(corePane, BorderLayout.CENTER);

        createSplitCorePane();
        createWholeCorePane();
        createChatsPane();

        corePane.add( splitPanel, "Split Cores");
        corePane.add( wholePanel, "Whole Cores");
        corePane.add( chatsPanel, "Chats");

        JPanel sp = new JPanel(new GridLayout(1, 1));
        sp.setBorder(BorderFactory.createTitledBorder("Status"));
        statBar = new JProgressBar();
        statBar.setPreferredSize( new Dimension( 400, 30));
        statBar.setStringPainted(true);
        statBar.setString("");
        sp.add(statBar);
        mainPanel.add( sp, BorderLayout.SOUTH );

        // System.out.println("Plugin Making IPC Objects");
        // register for events
        // System.out.println("Getting App handle");
        CorelyzerApp app = CorelyzerApp.getApp();

        if (app != null) {
            // System.out.println("Getting Plugin handle");
            CorelyzerPluginManager pm = app.getPluginManager();

            if(pm != null) {
                buildSubscriptionPanel();
                imageThread = new ImageLoadThread(statBar, mainPanel,
                                                  sectionVec);
                imageThread.start();

                return true;
            } else {
                System.err.println("NULL MANAGER!!!!");
                return false;
            }
        } else {
            System.err.println("NULL APP!!!!");
            return false;
        }

        /*
        if( app == null ) System.out.println("NULL APP!!!!");
        System.out.println("Getting Plugin handle");
        CorelyzerPluginManager pm = app.getPluginManager();
        if( pm == null ) System.out.println("NULL MANAGER!!!!");


        buildSubscriptionPanel();

        imageThread = new ImageLoadThread(statBar, mainPanel, sectionVec);
        imageThread.start();

        return true;
        */
    }

    //===========================================================
    private void createSplitCorePane() {
        splitPanel = new JPanel(new BorderLayout());

        JScrollPane jsp = new JScrollPane();
        sct = new SplitCoreTable( sectionVec, this);
        jsp.getViewport().setView(sct);
        jsp.setPreferredSize( new Dimension(385, 250) );
        jsp.setBorder( BorderFactory.createTitledBorder("Sections"));
        splitScrollPane = jsp;
        splitPanel.add(jsp, BorderLayout.NORTH);

        datasetTree = new JTree( datasetTreeRoot );
        datasetTree.addTreeSelectionListener(this);
        jsp = new JScrollPane();
        jsp.getViewport().setView(datasetTree);
        jsp.setPreferredSize( new Dimension(385,200 ));
        datasetTree.setSize( 385, 200);
        jsp.setBorder( BorderFactory.createTitledBorder("Datasets"));
        datasetScrollPane = jsp;
        splitPanel.add(jsp, BorderLayout.CENTER);

        //-- props
        JPanel propPane = new JPanel();
        propPane.setPreferredSize( new Dimension(385, 100));
        SpringLayout sl = new SpringLayout();
        propPane.setLayout(sl);

        JLabel label = new JLabel("Min");
        propPane.add(label);
        PositionWidget( propPane, label, sl, 10, 10 ); // - 445

        DecimalFormat twosig = new DecimalFormat("#.#");
        minField = new JFormattedTextField( twosig );
        minField.setPreferredSize( new Dimension(50,25) );
        propPane.add(minField);
        PositionWidget( propPane, minField, sl, 10, 35 );

        label = new JLabel("Max");
        propPane.add(label);
        PositionWidget( propPane, label, sl, 60, 10 );

        maxField = new JFormattedTextField( twosig );
        maxField.setPreferredSize( new Dimension( 50, 25) );
        propPane.add(maxField);
        PositionWidget( propPane, maxField, sl, 65, 35);
        
        label = new JLabel("Type");
        propPane.add(label);
        PositionWidget( propPane, label, sl, 190, 10);
        
        colorBtn = new JButton("Color");
        propPane.add(colorBtn);
        PositionWidget( propPane, colorBtn, sl, 120, 35);
        
        typeList = new JComboBox();
        typeList.setEditable(false);
        typeList.addItem("Line");
        typeList.addItem("Point");
        typeList.addItem("Cross Point");
        propPane.add(typeList);
        PositionWidget( propPane, typeList, sl, 190, 35);
        
        displayCheck = new JCheckBox("Display");
        propPane.add(displayCheck);
        PositionWidget( propPane, displayCheck, sl, 300, 35 );

        autoSplitCheck = new JCheckBox("Auto Download Images");
        propPane.add(autoSplitCheck);
        PositionWidget( propPane, autoSplitCheck, sl, 10, 65);

        applyBtn = new JButton("Apply");
        propPane.add(applyBtn);
        PositionWidget( propPane, applyBtn, sl, 390, 35);
        splitPanel.add(propPane, BorderLayout.SOUTH);

        colorBtn.addActionListener(this);
        typeList.addActionListener(this);
        applyBtn.addActionListener(this);
        displayCheck.addActionListener(this);
        autoSplitCheck.addActionListener(this);

        colorchooser = new ColorChooser(frame);
        colorchooser.setVisible(false);
        colorchooser.addReturnActionListener(this);

        // just disable the graph settings
        minField.disable();
        maxField.disable();
        displayCheck.disable();
        colorBtn.disable();
        typeList.disable();
        applyBtn.disable();
        

    }

    //===========================================================
    private void createWholeCorePane() {
        wholePanel = new JPanel(new BorderLayout());

        //wholePanel.setPreferredSize( new Dimension(385, 560) );
        //SpringLayout sl = new SpringLayout();
        //wholePanel.setLayout(sl);

        wct = new WholeCoreTable( sectionVec, this);

        JScrollPane jsp = new JScrollPane();
        //jsp.setPreferredSize( new Dimension(385, 250) );
        jsp.getViewport().setView( wct );
        wholePanel.add(jsp, BorderLayout.CENTER);
        //PositionWidget( wholePanel, jsp, sl, 0, 0);
//        wct.setPreferredSize( new Dimension(385, 250) );
        //wct.setSize( 385, 250 );
        jsp.setBorder( BorderFactory.createTitledBorder("Sections"));

        wholeScrollPane = jsp;
//        wholePanel.add(wct);
//        PositionWidget( wholePanel, wct, sl, 0, 0);

        autoWholeCheck = new JCheckBox("Auto Download Images");
        wholePanel.add(autoWholeCheck, BorderLayout.SOUTH);
        //PositionWidget( wholePanel, autoWholeCheck, sl, 10, 255);
        
        autoWholeCheck.addActionListener(this);

    }

    //===========================================================
    private void createChatsPane() {
        chatsPanel = new JPanel();

        //chatsPanel.setPreferredSize( new Dimension(385,580) );
        //SpringLayout sl = new SpringLayout();
        BorderLayout bl = new BorderLayout();
        chatsPanel.setLayout(bl);

        ct = new ChatTable( chatVec, this );


        JScrollPane jsp = new JScrollPane();
        jsp.setPreferredSize( new Dimension(385, 250) );
        jsp.getViewport().setView( ct );
        chatsPanel.add(jsp, BorderLayout.CENTER);
        // PositionWidget( chatsPanel, jsp, sl, 0, 0);
        //ct.setSize( 385, 250 );
        jsp.setBorder( BorderFactory.createTitledBorder("Chats"));

        this.chatScrollPane = jsp;
    }

    //===========================================================
    public void fini() {
        try {
            System.out.println("Fini Called! Closing Net Connects!");
            if(csc != null) csc.beginShutdown();
            queuePermit.release();
            imageThread.keepRunning = false;
            //if( System.getProperty("os.name").contains("Windows") )
            //    csc.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //===========================================================
    public void processEvent(CorelyzerPluginEvent e) {
        // if we are offline, do nothing here
        if (startbtn.isEnabled())
            return;
            
        //System.out.println("Posting event! " + e.getDescription());
        int tid, sid, groupid, typeid, mid;
        String[] toks;
        String url;
        float px, py;

        switch( e.getID() ) {
        case CorelyzerPluginEvent.NEW_ANNOTATION:
            System.out.println("NEW ANNOTATION!!!: " + e.getDescription());

            SessionChat sc = null;

            try {

                toks = e.getDescription().split("\t");
                tid = Integer.parseInt( toks[0] );
                sid = Integer.parseInt( toks[1] );
                groupid = Integer.parseInt( toks[2] );
                typeid = Integer.parseInt( toks[3] );
                px = Float.parseFloat( toks[4] );
                py = Float.parseFloat( toks[5] );
                mid = CorelyzerGLCanvas.getAnnotationWindow().getMarkerId();
                px = px * 2.54f / 100.0f;
                py = py * 2.54f / 100.0f;
                
                if( tid == collabSplitCoreTrack ) {
                    tid = 0;
                    Integer s = new Integer(sid);
                    sid = splitGlobalTable.get( s ).intValue();
                    System.out.println("NEW ANNO ON GLOBAL SEC NUM: " + sid);
                    //sc = new corelyzer.plugin.andrill.SessionChat( tid, sid, mid, groupid, p );
                    sc = new SessionChat( tid, sid, mid, groupid, typeid, px, py );
                    sectionVec.elementAt(sid).splitChat.add( sc );

                    this.chatVec.add(sc);
                }
                else {
                    tid = 1;
                    Integer s = new Integer(sid);
                    sid = wholeGlobalTable.get( s ).intValue();
                    System.out.println("NEW ANNO ON GLOBAL SEC NUM: " + sid);
                    //sc = new corelyzer.plugin.andrill.SessionChat( tid, sid, mid, groupid, p );
                    sc = new SessionChat( tid, sid, mid, groupid, typeid, px, py );
                    sectionVec.elementAt(sid).wholeChat.add( sc );

                    this.chatVec.add(sc);
                }

                ((TableSorter) ct.getModel()).reSortByColumn(2);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            //csc.postNewChat( tid, sid, groupid, p );
            csc.postNewChat( tid, sid, groupid, typeid, px, py );

            return;
        case CorelyzerPluginEvent.EDIT_ANNOTATION:
            System.out.println("EDIT ANNOTATION!!!: " + e.getDescription());
            // see if we have the annotation defined globally yet???

            toks = e.getDescription().split("\t");
            tid = Integer.parseInt( toks[0] );
            sid = Integer.parseInt( toks[1] );
            mid = Integer.parseInt( toks[2] );


            if( tid < 0 || sid < 0 || mid < 0 )
                return;

            if( tid == collabSplitCoreTrack )
            {
                Integer s = new Integer(sid);
                sid = splitGlobalTable.get( s ).intValue();
                url = sectionVec.elementAt(sid).splitChat.elementAt(mid).url;
            }
            else
            {
                Integer s = new Integer(sid);
                sid = wholeGlobalTable.get( s ).intValue();
                url = sectionVec.elementAt(sid).wholeChat.elementAt(mid).url;
            }

            if( url == null )
                return;

            System.out.println("Setting URL to: " + url );

            try {
                CorelyzerGLCanvas.getAnnotationWindow().setURL( new URL(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            CorelyzerGLCanvas.getAnnotationWindow().goToBottom();

            return;
        case CorelyzerPluginEvent.NEW_ANNOTATION_ENTRY:
            System.out.println("NEW ANNOTATION ENTRY!!!:\n" +
                               e.getDescription());
            CorelyzerGLCanvas.getAnnotationWindow().setWriteLocalCopy(false);

            toks = e.getDescription().split("\t");
            tid = Integer.parseInt( toks[0] );
            sid = Integer.parseInt( toks[1] );
            mid = Integer.parseInt( toks[2] );

            if( tid < 0 || sid < 0 || mid < 0 )
                return;

            if( tid == collabSplitCoreTrack ) {
                Integer s = new Integer(sid);
                sid = splitGlobalTable.get( s ).intValue();
                url = sectionVec.elementAt(sid).splitChat.elementAt(mid).url;
                px = sectionVec.elementAt(sid).splitChat.elementAt(mid).xpos_m;
                tid = 0;
            }
            else {
                Integer s = new Integer(sid);
                sid = wholeGlobalTable.get( s ).intValue();
                url = sectionVec.elementAt(sid).wholeChat.elementAt(mid).url;
                px = sectionVec.elementAt(sid).wholeChat.elementAt(mid).xpos_m;
                tid = 0;
            }

            if( url == null )
            {
                url = new String("");
            }

            String entry = new String("");

            // make sure the splitting didn't split the entry string
            for( int i = 3; i < toks.length; i++) {
                entry = entry + "\t" + toks[i];
            }

            int last = entry.lastIndexOf("<hr>");
            entry = entry.substring(0,last) + "<br><hr>";
            System.out.println("SENDING ENTRY: " + entry );

            csc.postNewChatEntry( tid, sid, url, px, entry );
            return;
        case CorelyzerPluginEvent.SECTION_REMOVED:
            // use removed section from main app frame
            // need to update session list.
            System.out.println("SECTION REMOVED: " + e.getDescription());
            
            toks = e.getDescription().split("\t");
            tid = Integer.parseInt( toks[0] );      // native index of track
            sid = Integer.parseInt( toks[1] );      // native index of section
            
            if( tid < 0 || sid < 0)
                return;

            if( tid == collabSplitCoreTrack ) {
                // find global id
                Integer s = new Integer(sid);
                sid = splitGlobalTable.get(s).intValue();
                // set subscription
                sectionVec.elementAt(sid).subscribedSplit = false;
            }
            else {
                // find global id
                Integer s = new Integer(sid);
                sid = wholeGlobalTable.get(s).intValue();
                // set subscription
                sectionVec.elementAt(sid).subscribedWhole = false;
            }
            updateGUI();
            return;
        case CorelyzerPluginEvent.TRACK_REMOVED:
            // this should never happen!
            return;
        }

        outQueue.add( e );
        queuePermit.release();
    }

    //===========================================================
    public void renderRectangle(GL gl, int freeDraw, int canvas, int track,
                                int section,float x, float y, float w, float h,
                                float scale) {

        // determine if it's a client position free draw or a discussion thread
        // marker

        // look up client data from the freedraw -> client data table
        // to get user name

        // draw a rectangle filling white quad
        gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_ENABLE_BIT);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glColor3f(1,1,1);
        gl.glBegin(GL.GL_QUADS);
        {
            gl.glVertex3f(x  , y  , 0);
            gl.glVertex3f(x  , y+h, 0);
            gl.glVertex3f(x+w, y+h, 0);
            gl.glVertex3f(x+w, y  , 0);
        }
        gl.glEnd();
        gl.glPopAttrib();


    }

    //===========================================================
    public String getMenuName() {
        return "Collaborative Corelyzer";
    }

    //===========================================================
    public JFrame getFrame() {
        return frame;
    }

    //===========================================================
    public void updateGUI() {
        // look at inqueue, add entries depending on user preferences

        // update GUI
        frame.repaint();
    }

    //===========================================================
    private void buildSubscriptionPanel() {

    }

    //===========================================================
    public void mousePosUpdate(int userID, float x, float y) {
        if( userID == csc.getUserID())
            return;
    }

    //===========================================================
    public CorelyzerSessionClient getSessionClient() { return csc; }

    //===========================================================
    public void setSessionClient( CorelyzerSessionClient csc ) {
        this.csc = csc;
        csc.start();
    }


    //===========================================================
    public void clientLoggedIn(int userid, String name) {

        if( userid == 0 ) return;

        try {
            System.out.println("Inserting user name: " + userid + " "  + name);

            // create a free draw area
            SceneGraph.lock();
            int fdid = SceneGraph.createFreeDrawRectangle(
                getPluginID(), 0.0f, 0.0f, 0.1f, 0.1f);
            SceneGraph.unlock();

            if( fdid < 0 )
                return;


            // create the user data
            ClientData cd = new ClientData();
            cd.setUserID(userid);
            cd.setName(name);

            if( cd == null )
                return;

            // add free draw index to client data
            cd.setFreeDrawID(fdid);

            System.out.println("Free Draw Area Created");
            SceneGraph.markFreeDrawScaleIndependent( fdid, true );

            Integer fdint = new Integer(fdid);
            Integer usrint = new Integer(userid);

//            userTableLock.lock();

            // add hashtable entry for freedraw to userid
            freeDrawUserDataTable.put( fdint, cd );

            // add hashtable entry for userid to freedraw
            userIdDataTable.put( usrint, cd );

//            userTableLock.unlock();

        }
        catch( Exception e ) {
            e.printStackTrace();
            System.out.println("SYSTEM ERROR: Failed to add user " +
                               userid + " to listing.  System continuing" );
        }
    }

    //==================================================getDescription());=========
    private void clearFreeDraws() {
        Enumeration< ClientData > cdenum = freeDrawUserDataTable.elements();
        while( cdenum.hasMoreElements() ) {
            ClientData cd = cdenum.nextElement();

            SceneGraph.lock();
            SceneGraph.destroyFreeDrawRectangle( cd.getFreeDrawID() );
            SceneGraph.unlock();

        }

        freeDrawUserDataTable.clear();
        userIdDataTable.clear();
    }

    //===========================================================
    public void clientLoggedOut(int userid) {
        try {

            System.out.println("LOCKING USER TABLE");
            //           userTableLock.lock();

            // get freedraw entry id
            ClientData cd = userIdDataTable.get( new Integer(userid) );
            int fdid = cd.getFreeDrawID();
            System.out.println("USER ID " + userid + " BOUND TO FREE DRAW " +
                               fdid);

            Integer fdint = new Integer(fdid);
            Integer usrint = new Integer(userid);

            // remove hashtable entry for freedraw to user id
            freeDrawUserDataTable.remove( fdint );

            System.out.println("REMOVED FREE DRAW TO USER ID ENTRY");

            userIdDataTable.remove( usrint );

            System.out.println("REMOVED USER ID TO FREE DRAW ENTRY");

            //        userTableLock.unlock();
            // remove the freedraw area
            SceneGraph.lock();

            SceneGraph.destroyFreeDrawRectangle( fdid );

            SceneGraph.unlock();

            System.out.println("UNLOCKED USER TABLE");

            // remove the freedraw area
            SceneGraph.lock();

            SceneGraph.destroyFreeDrawRectangle( fdid );

            SceneGraph.unlock();

            System.out.println("REMOVED FREE DRAW RECTANGLE");

            CorelyzerApp.getApp().updateGLWindows();

            System.out.println("UNLOCKED FREE DRAW RECTANGLE");

        }
        catch( Exception e ) {
            // don't know what to do?????
            e.printStackTrace();
            System.out.println("SYSTEM ERROR: Client " + userid +
                               " logout did not process.  System continuing.");
        }
    }

    //******************************************************
    /** x, y are in meters. */
    public void updateUserPosition(int userid, float x, float y, long time) {
        try {
            if( userid == csc.getUserID())
            {
                return;
            }

            // lookup the userid --> freedraw id table
//            System.out.println("Updating user " + userid );


            ClientData cd = userIdDataTable.get( new Integer(userid) );
            if( cd == null )
                return;

//            System.out.println("Got Client Data");


            if( cd.getLastHeartBeat() > time )
                return;

            cd.setLastHeartBeat(time);

            // position the freedraw object
            SceneGraph.repositionFreeDrawRectangle( cd.getFreeDrawID(), x, y);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //******************************************************
    public void updateUserState(int userid, int state) {
        // look up userid -- freedraw id table
        // get client data by freedraw id --> client data

    }

    //===========================================================
    private void processStartButton() {
        // String servername = serverfield.getText();
        String servername = serverfield.getSelectedItem().toString();
        if( servername == null || servername.equals(""))
            return;
        datasetTreeRoot = new DefaultMutableTreeNode(new String("Sets"),true);
        datasetTree.setModel( new DefaultTreeModel(datasetTreeRoot) );

        csc = new CorelyzerSessionClient( outQueue, queuePermit, this);

        csc.setServerAddress(servername);
        if( csc.tryLogin() )
        {
//            pdlg = new ProgressDialog();
//            pdlg.setLabelText("Retrieving session information.");
//            pdlg.setIndeterminant();
            imageThread.setUserPass(csc.getUserName(), csc.getPassword());
            
            csc.start();
            //frame.setVisible(false);
            logoutbtn.setEnabled(true);
            startbtn.setEnabled(false);
            loadbtn.setEnabled(true);
            savebtn.setEnabled(true);
            this.serverfield.setEnabled(false);
            collabSplitCoreTrack = CorelyzerApp.getApp().createTrack(
                "PMOCSV SPLIT CORE TRACK");
            collabWholeCoreTrack = CorelyzerApp.getApp().createTrack(
                "PMOCSV WHOLE CORE TRACK");
            CorelyzerApp.getApp().setCollaborationMode(true);
            
            System.out.println("CollabTracks: split - " + collabSplitCoreTrack
                               + " | whole - " + collabWholeCoreTrack );

            imageThread.splitCoreTrack = collabSplitCoreTrack;
            imageThread.wholeCoreTrack = collabWholeCoreTrack;

            CorelyzerPluginManager pm = CorelyzerApp.getApp().
                getPluginManager();

            System.out.println("REGISTERING FOR EVENTS!!!!");
            pm.registerPluginForEventType(
                pluginId, CorelyzerPluginEvent.MOUSE_MOTION);

            pm.registerPluginForEventType(
                pluginId, CorelyzerPluginEvent.NEW_ANNOTATION );

            pm.registerPluginForEventType(
                pluginId, CorelyzerPluginEvent.EDIT_ANNOTATION );

            pm.registerPluginForEventType(
                pluginId, CorelyzerPluginEvent.NEW_ANNOTATION_ENTRY );

            pm.registerPluginForEventType(
                pluginId, CorelyzerPluginEvent.SECTION_REMOVED );

            statBar.setString("Connected");
            frame.repaint();
            
            // update server_list file
            File serverfile = new File("server_list.txt");
            boolean found = false;
            try {
                if (serverfile.exists()) {
                    BufferedReader reader =
                        new BufferedReader(new FileReader(serverfile));
                    String line = "";
                    while( (line = reader.readLine()) != null ) {
                        if (line.equals(servername)) {
                            found = true;
                            break;
                        }
                    }
                    reader.close();
                }
                
                if (!found) {
                    // append server address
                    FileWriter fw = new FileWriter(
                                        new File("server_list.txt"), true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write(servername+"\n");
                    bw.close();
                    fw.close();
                }
            
            } catch (IOException e) {
                System.err.println("IOException in append server addr");
            }
            
        }
        else {
            statBar.setString("Could not connect to server");
            frame.repaint();
        }
    }

    //===========================================================
    private void processLogoutButton() {
        try {
            /*
            if( pdlg != null )
            {
                pdlg.dispose();
                pdlg = null;
            }
            */

            System.out.println("Logout called!");
            csc.beginShutdown();
            queuePermit.release();

            clearFreeDraws();

            // clear the dataset tree
            for( int i = 0; i < datasetTreeRoot.getChildCount(); i++) {
                DefaultMutableTreeNode dmtn;
                dmtn = (DefaultMutableTreeNode) datasetTreeRoot.getChildAt(i);
                dmtn.removeAllChildren();
            }

            datasetTreeRoot.removeAllChildren();

            sectionVec.clear();
            datasetVec.clear();
            splitGlobalTable.clear();
            wholeGlobalTable.clear();
            userIdDataTable.clear();
            freeDrawUserDataTable.clear();

            datasetTree.setModel(null);

            CorelyzerPluginManager pm = CorelyzerApp.getApp()
                .getPluginManager();

            pm.unregisterPluginForEventType(
                pluginId, CorelyzerPluginEvent.MOUSE_MOTION);

            pm.unregisterPluginForEventType(
                pluginId, CorelyzerPluginEvent.NEW_ANNOTATION);

            pm.unregisterPluginForEventType(
                pluginId, CorelyzerPluginEvent.EDIT_ANNOTATION);

            pm.unregisterPluginForEventType(
                pluginId, CorelyzerPluginEvent.NEW_ANNOTATION_ENTRY);


//            csc.join();
//            frame.setVisible(false);
            startbtn.setEnabled(true);
            logoutbtn.setEnabled(false);
            loadbtn.setEnabled(false);
            savebtn.setEnabled(false);
            this.serverfield.setEnabled(true);
            frame.repaint();

            CorelyzerApp.getApp().updateGLWindows();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //===========================================================
    private void processAddDiscussion() {

        int track, section;

        track = SceneGraph.accessPickedTrack();
        section = SceneGraph.accessPickedSection();


        System.out.println("BROADCAST DISCUSSION THREAD HERE!!!");

    }

    //===========================================================
    private void processEditDiscussion() {


    }

    //===========================================================
    public void actionPerformed(ActionEvent e ) {
        if( e.getSource() == startbtn && startbtn.isEnabled() ) {
            processStartButton();
        }
        else if( e.getSource() == logoutbtn && logoutbtn.isEnabled() ) {
            processLogoutButton();
        }
        else if(e.getSource() == loadbtn && loadbtn.isEnabled() ) {
            loadSessionLog();
        }
        else if(e.getSource() == savebtn && savebtn.isEnabled() ) {
            saveSessionLog();
        }
        else if( e.getSource() == addDiscussion ) {

        }
        else if( e.getSource() == editDiscussion ) {

        }
        else if (e.getSource() == autoSplitCheck) {
            if (autoSplitCheck.isSelected()) {
                
                for( int i = 0; i < sectionVec.size(); i++) {
                    SessionSection ss = sectionVec.elementAt(i);
                    if( ss.splitDPIX > 0 && ss.splitDPIY > 0 && 
                        ss.subscribedSplit == false) {
                        ss.subscribedSplit = true;
                        requestSplitCore(i);
                    }
                }
            }
        }
        else if (e.getSource() == autoWholeCheck) {
            if (autoWholeCheck.isSelected()) {
                
                for( int i = 0; i < sectionVec.size(); i++) {
                    SessionSection ss = sectionVec.elementAt(i);
                    if( ss.wholeDPI > 0 && ss.subscribedWhole == false) {
                        ss.subscribedWhole = true;
                        requestWholeCore(i);
                    }
                }
            }
        }
        else if( e.getSource() == displayCheck ) {
            // display or not??
            if( selectedSet == null || selectedField < 0 ) {
                displayCheck.setSelected(false);
                return;
            }

            if( !selectedSet.display[selectedField] )
            {
                selectedSet.display[selectedField] = true;
                displayCheck.setSelected(true);
            }
            else
            {
                selectedSet.display[selectedField] = false;
                displayCheck.setSelected(false);
            }

        }
        else if( e.getSource() == colorBtn ) {
            // display color selection ??
            if( selectedSet == null || selectedField < 0 ) {
                return;
            }

            colorchooser.setVisible(true);
            colorchooser.setTarget(colorBtn);
            colorchooser.setColor(
                new Color( selectedSet.colors[selectedField + 1][0],
                           selectedSet.colors[selectedField + 1][1],
                           selectedSet.colors[selectedField + 1][2]) );
        }
        else if( e.getSource() == typeList ) {

            if( selectedSet == null || selectedField < 0 ) {
                return;
            }
            int t = typeList.getSelectedIndex();
            selectedSet.types[ selectedField + 1] = t;
        }
        else if( e.getSource() == colorchooser ) {

            Color c = colorchooser.getColor();

            selectedSet.colors[ selectedField + 1][0] = c.getRed() / 255.0f;
            selectedSet.colors[ selectedField + 1][1] = c.getGreen() / 255.0f;
            selectedSet.colors[ selectedField + 1][2] = c.getBlue() / 255.0f;

            frame.repaint();
        }
        else if( e.getSource() == applyBtn ) {
            System.out.println("APPLY SELECTED!!");
            try {
                if( selectedSet == null || selectedField < 0 ) {
                    return;
                }

                // get the min/max text and set it in the user min/max in
                // the set object
                try {
                    Float f = new Float( minField.getText());
                    selectedSet.userMins.setElementAt(f, selectedField +1);
                } catch ( Exception minEx) {
                }

                try {
                    Float f = new Float( maxField.getText());
                    selectedSet.userMaxs.setElementAt(f, selectedField +1);
                } catch (Exception maxEx) {
                }

                if( !selectedSet.display[selectedField] )
                {
                    // take away graphs
                    System.out.println("MARKING UNCHECKED!");
                    removeGraphs( selectedSet, selectedField );
                    System.out.println("REMOVE DATA!!!");
                }
                else {
                    // create graphs
                    System.out.println("MARKING CHECKED!");
                    requestGraphs( selectedSet, selectedField );
                    System.out.println("DISPLAY DATA!!!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            CorelyzerApp.getApp().updateGLWindows();
        }
    }

    //===========================================================
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)
            e.getPath().getLastPathComponent();

        selectedSet = null;
        selectedField = -1;

        minField.setValue(new Float(0.0f));
        maxField.setValue(new Float(0.0f));
        displayCheck.setSelected(false);
        colorBtn.setBackground( new Color(255,255,255));
        typeList.setSelectedIndex(0);

        // just disable the graph settings
        minField.disable();
        maxField.disable();
        displayCheck.disable();
        colorBtn.disable();
        typeList.disable();

        if( dmtn.getParent() == datasetTreeRoot || dmtn == datasetTreeRoot)
        {
            return;
        }

        SessionDataset sd = (SessionDataset) dmtn.getParent();
        if( sd == null )
            return;

        int field = sd.getIndex(dmtn);

        minField.enable();
        maxField.enable();
        displayCheck.enable();
        colorBtn.enable();
        typeList.enable();
        applyBtn.enable();


        minField.setValue( sd.userMins.elementAt(field +1) );
        maxField.setValue( sd.userMaxs.elementAt(field +1) );
        colorBtn.setBackground( new Color( sd.colors[field +1][0],
                                           sd.colors[field +1][1],
                                           sd.colors[field +1][2]) );
        typeList.setSelectedIndex(sd.types[field +1]);
        displayCheck.setSelected( sd.display[field] );

        selectedSet = sd;
        selectedField = field;
    }

    //===========================================================
    public void addToPopupMenu(JPopupMenu jpm) {
        return;

/*        JMenu menu = new JMenu( this.getMenuName() );

        if( SceneGraph.accessPickedFreeDraw() > -1 &&
            SceneGraph.accessPickedSection() > -1 )
        {
            addDiscussion = new JMenuItem("Create Discussion Point");
            addDiscussion.addActionListener(this);
            menu.add(addDiscussion);
        }
        else
        {
            editDiscussion = new JMenuItem("Add to Discussion Point");
            editDiscussion.addActionListener(this);
            menu.add(editDiscussion);
        }

        jpm.add(menu);
*/
    }

    //================================================================
    public void addNewSection(int globalId, float depth, float length,
                              String name) {

        // System.out.println("Section " + name + " at depth: " + depth );

        // see if we already have the section in the vector
        SessionSection ss = null;
        if( sectionVec.size() > globalId )
            ss = sectionVec.elementAt(globalId);

        if( ss == null ) {
            ss = new SessionSection(name,depth,length);
            sectionVec.insertElementAt(ss,globalId);
        }

        splitScrollPane.setViewportView( sct );
        wholeScrollPane.setViewportView( wct );
        chatScrollPane.setViewportView( ct );

        frame.repaint();
    }
    
    //================================================================
    public void addSplitCoreSection(int globalId, float depth, float length,
                                    float dpi_x, float dpi_y, String name,
                                    String url)
    {


        // System.out.println("Section " + name + " at depth: " + depth );

        // see if we already have the section in the vector
        SessionSection ss = null;
        if( sectionVec.size() > globalId )
            ss = sectionVec.elementAt(globalId);

        if( ss == null ) {
            ss = new SessionSection(name,depth,length);
            sectionVec.insertElementAt(ss,globalId);
        }

        ss.splitCoreURL = url;
        ss.subscribedSplit = false;
        ss.splitDPIX = dpi_x;
        ss.splitDPIY = dpi_y;

        splitScrollPane.setViewportView( sct );
        
        // check autodownload
        if (autoSplitCheck.isSelected() && dpi_x > 0 && dpi_y > 0) {
            ss.subscribedSplit = true;
            requestSplitCore(globalId);
        }
        
        frame.repaint();
        
    }

    //================================================================
    public void addWholeCoreSection(int globalId, float depth, float length,
                                    float dpi, String name, String url) {

        // see if we already have the section in the vector
        SessionSection ss = null;
        if( sectionVec.size() > globalId )
            ss = sectionVec.elementAt(globalId);

        if( ss == null ) {
            ss = new SessionSection(name,depth,length);
            sectionVec.insertElementAt(ss,globalId);
        }

        ss.wholeCoreURL = url;
        ss.subscribedWhole = false;
        ss.wholeDPI = dpi;

        wholeScrollPane.setViewportView( wct );

        // check autodownload
        if (autoWholeCheck.isSelected() && dpi > 0) {
            ss.subscribedWhole = true;
            requestWholeCore(globalId);
        }

        frame.repaint();
    }

    //===========================================================
    public void addDataset(SessionDataset sd) {
        // see if we already have the dataset
        datasetVec.add(sd);
        datasetTreeRoot.add(sd);

        sd.id = SceneGraph.addDataset(sd.name);

        datasetScrollPane.setViewportView( datasetTree );

        frame.repaint();
    }

    //===========================================================
    public void removeGraphs(SessionDataset sd, int field) {
        // go through each table and the fields and remove the graphs
        for( int i = 0; i < sd.tables.size(); i++) {
            SessionTable st = sd.tables.elementAt(i);
            if( st == null )
                continue;

            int gid = st.gids[field];
            if( gid == -1 )
                continue;

            SceneGraph.removeLineGraphFromSection( gid );

            st.gids[field] = -1;
        }
    }

    //===========================================================
    public void requestGraphs(SessionDataset sd, int field ) {
        csc.requestUpdateSetTables(sd.name);
        csc.relayStartGraphs(sd.name,field);
    }

    //===========================================================
    public void updateDatasetTableList(String setname,
                                       Vector< String > tnames ) {
        SessionDataset sd = null;
        int i;
        for( i = 0; i < datasetVec.size(); i++) {
            if( datasetVec.elementAt(i).name.equals(setname)) {
                sd = datasetVec.elementAt(i);
                i = datasetVec.size();
            }
        }

        if( sd == null )
            return;

        for( i = 0; i < tnames.size(); i++) {
            boolean match;
            match = false;
            for( int k = 0; k < sd.tables.size() && !match; k++) {
                SessionTable st = sd.tables.elementAt(k);
                if( st.name.equals(tnames.elementAt(i)) )
                    match = true;
            }

            if( !match ) {
                SessionTable st = new SessionTable(tnames.elementAt(i));
                st.id = -1;
                st.gids = new int[sd.fields.size()];
                for( int j = 0; j < sd.fields.size(); j++)
                    st.gids[j] = -1;
                sd.tables.add( st );
            }
        }
    }

    //===========================================================
    public void createDataTable(String setname, String tablename,
                                int fieldcount,
                                Vector< boolean[] > valids,
                                Vector< float[] > values) {
        SessionDataset sd = null;
        SessionTable st = null;
        int i;

        System.out.println("FIELD COUNT: " + fieldcount );

        for( i = 0; i < datasetVec.size(); i++) {
            if( datasetVec.elementAt(i).name.equals(setname)) {
                sd = datasetVec.elementAt(i);
                i = datasetVec.size();
            }
        }

        if( sd == null )
            return;

        boolean match;
        match = false;
        for( int k = 0; k < sd.tables.size() && !match; k++) {
            st = sd.tables.elementAt(k);
            if( st.name.equals(tablename) )
                match = true;
        }

        if( !match ) {
            st = new SessionTable(tablename);
            sd.tables.add( st );
        }


        // now that we have the table... make a table id
        st.id = SceneGraph.addTable( sd.id, tablename );
        SceneGraph.setTableDepthUnitScale( sd.id, st.id, 100.0f );
        SceneGraph.setTableHeightAndFieldCount( sd.id, st.id, values.size(),
                                                fieldcount - 1);

        for( i = 1; i < fieldcount; i++) {
            SceneGraph.setFieldMinMax( sd.id, st.id, i-1,
                                       sd.mins.elementAt(i).floatValue(),
                                       sd.maxs.elementAt(i).floatValue());
        }

        for( int r = 0; r < values.size(); r++) {
            boolean[] bs;
            float[]   vs;
            bs = valids.elementAt(r);
            vs = values.elementAt(r);
            for( i = 1; i < fieldcount; i++){
                SceneGraph.setTableCell( sd.id, st.id, i -1, r, bs[i],
                                         vs[0], vs[i]);
            }
        }

    }

    //===========================================================
    public void startGraphRequests(String setname, int field) {
        // check against the sections we have and see if the tables
        // for them are there... if not, request the table

        SessionDataset sd = null;
        int i;
        for( i = 0; i < datasetVec.size(); i++) {
            if( datasetVec.elementAt(i).name.equals(setname)) {
                sd = datasetVec.elementAt(i);
                //i = datasetVec.size();
                break;
            }
        }

        if( sd == null )
            return;

        int nsecs = SceneGraph.getNumSections( collabSplitCoreTrack );
        for( int k = 0; k < sd.tables.size(); k++) {
            SessionTable st;
            st = sd.tables.elementAt(k);
            System.out.println("graph reguest. table name: " + st.name);
            for( i = 0; i < nsecs; i++) {
                // see if the section for it is even here.. if not
                // then don't bother... if so, then request the data
                int id = SceneGraph.getImageIdForSection(
                    collabSplitCoreTrack, i);
                String srcname = SceneGraph.getImageName( id );

                // prevent possible null pointer exception
                if (srcname == null)
                    continue;
                
                if( srcname.contains(st.name) ) {
                    // ask for the table if the id < 0
                    if( st.id < 0 )
                        csc.makeTableRequest( setname, st.name );

                    csc.relayMakeGraph( setname, st.name, field );

                    i = nsecs;
                }
            }
        }
    }

    //===========================================================
    public void createGraph(String setname, String tablename, int field) {
        // do we have the data??? if not, then skip

        SessionDataset sd = null;
        SessionTable st = null;
        int i;

        for( i = 0; i < datasetVec.size(); i++) {
            if( datasetVec.elementAt(i).name.equals(setname)) {
                sd = datasetVec.elementAt(i);
                i = datasetVec.size();
            }
        }

        if( sd == null )
            return;

        boolean match;
        match = false;
        for( int k = 0; k < sd.tables.size() && !match; k++) {
            st = sd.tables.elementAt(k);

            if( tablename.contains(st.name) )
                match = true;
        }

        if( !match ) {
            return;
        }
        else {
            SceneGraph.lock();
            // match is made - find the sections that have the
            // table name as a substring
            int nsecs = SceneGraph.getNumSections( collabSplitCoreTrack );
            for( i = 0; i < nsecs; i++) {
                String srcname = SceneGraph.getImageName(
                    SceneGraph.getImageIdForSection( collabSplitCoreTrack, i));
                
                // prevent possible null pointer exception
                if (srcname == null)
                    continue;

                if( srcname.contains(tablename) ) {
                    st.gids[field] = SceneGraph.addLineGraphToSection(
                        collabSplitCoreTrack, i, sd.id, st.id, field);
                    System.out.println("Graph ID: " + st.gids[field] + " label "
                                       + sd.fields.elementAt(field));
                    SceneGraph.setLineGraphLabel( st.gids[field],
                                                  sd.fields.elementAt(field));
                    SceneGraph.setLineGraphColor( st.gids[field],
                                                  sd.colors[field+1][0],
                                                  sd.colors[field+1][1],
                                                  sd.colors[field+1][2]);
                    SceneGraph.setLineGraphType(st.gids[field],
                                                sd.types[field+1]);
                    float min, max;
                    min = sd.userMins.elementAt(field +1).floatValue();
                    max = sd.userMaxs.elementAt(field +1).floatValue();

                    SceneGraph.setLineGraphRange( st.gids[field], min, max);
                }
            }
            SceneGraph.unlock();
        }
    }

    //===========================================================
    public void requestSplitCore(int secId) {

        csc.makeSplitCoreRequest(secId);
    }

    //===========================================================
    public void deleteSplitCore(int secId) {
        // secId is global one
        // need to send local id to corelyzerApp 
        if( sectionVec.elementAt(secId) == null)
            return;
        
        SessionSection ss = sectionVec.elementAt(secId);
        CorelyzerApp.getApp().deleteSection(collabSplitCoreTrack, ss.splitId);
        ss.splitId = -1;
    }
    
    //==========================================================================
    public void loadSplitCore( int secId ) {

        System.out.println("Getting corelyzer.plugin.andrill.SessionSection");

        if( sectionVec.elementAt(secId) == null)
            return;
        SessionSection ss = sectionVec.elementAt(secId);
        String url = ss.splitCoreURL;
        if( url == null || ss.name == null )
            return;

        String ext = new ExampleFileFilter().getExtension(
            new File( url ) );

        String localcopy = CorelyzerApp.getApp().getDownloadDirectoryPath();
        if( localcopy.charAt( localcopy.length() - 1) != '/' )
            localcopy = localcopy + "/";
        localcopy = localcopy + ss.name + "-split." + ext;
        statBar.setString("Please Wait...");
        frame.repaint();

        ImageLoadRequest ilr = new ImageLoadRequest();
        ilr.url = url;
        ilr.local = localcopy;
        ilr.ss = ss;
        ilr.secId = secId;
        ilr.track = collabSplitCoreTrack;
        ilr.table = splitGlobalTable;
        ilr.split = true;
        ilr.state = 0;

        imageThread.queue.add( ilr );
        //getImage( ilr.track, ilr.secId, ilr.ss,
        //          ilr.split, ilr.table, ilr.url, ilr.local);
        ilr = null;
    }

    //===========================================================
    public void requestWholeCore(int secId) {
        csc.makeWholeCoreRequest(secId);
    }

    //===========================================================
    public void deleteWholeCore(int secId) {
        // secId is global one
        // need to send local id to corelyzerApp 
        if( sectionVec.elementAt(secId) == null)
            return;
        
        SessionSection ss = sectionVec.elementAt(secId);
        CorelyzerApp.getApp().deleteSection(collabWholeCoreTrack, ss.wholeId);
        ss.wholeId = -1;
    }

    public void loadWholeCore(int secId) {
        System.out.println("Getting corelyzer.plugin.andrill.SessionSection");

        if( sectionVec.elementAt(secId) == null)
            return;
        SessionSection ss = sectionVec.elementAt(secId);
        String url = ss.wholeCoreURL;
        if( url == null || ss.name == null )
            return;

        String ext = new ExampleFileFilter().getExtension(
            new File( url ) );

        String localcopy = CorelyzerApp.getApp().getDownloadDirectoryPath();
        if( localcopy.charAt( localcopy.length() - 1) != '/' )
            localcopy = localcopy + "/";
        localcopy = localcopy + ss.name + "-whole." + ext;
        statBar.setString("Please Wait...");
        frame.repaint();

        ImageLoadRequest ilr = new ImageLoadRequest();
        ilr.url = url;
        ilr.local = localcopy;
        ilr.ss = ss;
        ilr.secId = secId;
        ilr.track = collabWholeCoreTrack;
        ilr.table = wholeGlobalTable;
        ilr.split = false;
        ilr.state = 0;

        imageThread.queue.add( ilr );

        ilr = null;
    }

    //===================================================================
    public void assignChatGlobalID( int split, int secid, int gid, 
                                    float posx,float posy,
                                    String url, int userid,
                                    int groupid, int typeid, long created, long modified)
    {

        System.out.println("Global chat info coming in for section: " + secid
                           + " against vec size of " + sectionVec.size());

        if( secid < 0 || secid >= sectionVec.size() )
            return;

        int track;
        Vector<SessionChat> scvec;
        int localsec;
        SessionSection ss = sectionVec.elementAt(secid);

        if( split == 0 ) {  // splitcore
            scvec = sectionVec.elementAt(secid).splitChat;
            track = collabSplitCoreTrack;
            localsec = ss.splitId; // Notice! Will have valid local sectionid
                                   // only if splitcore image is already loaded

        }
        else {              // wholecore
            scvec = sectionVec.elementAt(secid).wholeChat;
            track = collabWholeCoreTrack;
            localsec = ss.wholeId; // Notice! Will have valid local sectionid
                                   // only if splitcore image is already loaded
        }

        for( int i = 0; i < scvec.size(); i++) {
            if( scvec.elementAt(i).xpos_m == posx ) {
                System.out.println("Already existing marker...");

                SessionChat chat = scvec.elementAt(i);

                chat.url = url;
                chat.global = gid;
                chat.userID = userid;

                chat.group = groupid;
                chat.type  = typeid;
                chat.created = created;
                chat.lastModified = modified;

                // physically create a local marker if not doing so yet
                if(chat.local == -1) {
                    System.out.println("Had corelyzer.plugin.andrill.SessionChat, but need to " +
                            "physically create marker in scenegraph!");
                }

                return;
            }
        }

        // never found.. must be new
        float xpos = posx * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0);
        float ypos = posy * 100.0f / 2.54f * SceneGraph.getCanvasDPIY(0);
        int mid = -1;

        // Only really create the marker in scenegraph if the section image
        // is already loaded, which means: (localsec > -1)
        if( localsec > -1 ) {  // check -1 mentioned in above notes
            mid = SceneGraph.createCoreSectionMarker(
                      track, localsec, // collabWholeCoreTrack, localsec,
                      groupid, MarkerType.CORE_POINT_MARKER, xpos, ypos);

            SceneGraph.setCoreSectionMarkerVisibility(track,
                                                      localsec, mid, true);
        }

        // at this time, 'secid' is global, 'mid' is local(could be -1)
        SessionChat sc = new SessionChat( track, secid, mid, posx, posy );
        sc.local_sectionid = localsec;
        sc.global = gid;
        sc.url = url;
        sc.userID = userid;
        sc.group = groupid;
        sc.created = created;
        sc.lastModified = modified;
        sc.visibility = true;

        if( split == 0 ) // splitcore
        {
            sectionVec.elementAt(secid).splitChat.add( sc );
        }
        else
        {
            sectionVec.elementAt(secid).wholeChat.add( sc );
        }

        this.chatVec.add(sc);
        ((TableSorter) ct.getModel()).reSortByColumn(2);

        this.chatScrollPane.setViewportView( ct );
        frame.repaint();
    }

    //===================================================================
    public void chatUpdated( int split, int secid, int globalId, long modified)
    {
        String url = null;

        if( secid < 0 || secid >= sectionVec.size() )
            return;

        int track;
        Vector< SessionChat > scvec;
        int localsec;
        SessionSection ss = sectionVec.elementAt(secid);
        SessionChat sc = null;

        if( split == 0 ) {
            scvec = ss.splitChat;
            track = collabSplitCoreTrack;
            localsec = ss.splitId;
        }
        else{
            scvec = ss.wholeChat;
            track = collabWholeCoreTrack;
            localsec = ss.wholeId;
        }


        for( int i = 0; i < scvec.size(); i++) {
            if( scvec.elementAt(i).global == globalId )
            {
                sc = scvec.elementAt(i);
                sc.lastModified = modified;
                url = sc.url;
                i = scvec.size();
            }
        }

        if( sc == null)
            return;

        CRAnnotationWindow aw = CorelyzerGLCanvas.getAnnotationWindow();
        String awurl = null;
        try {
            awurl = aw.getURL();
        } catch( Exception e) {
            System.out.println("CURRENTLY NULL URL!!!");
        }

        if( awurl == null ) {
            if( sc.url != null ) {
                try {
                    aw.setURL( new URL(sc.url) );
//                    aw.goToBottom();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            int tid = aw.getTrackId();
            int sid = aw.getSectionId();
            int mid = aw.getMarkerId();
            if( tid < 0 || sid < 0 || mid < 0 )
                return;

            SessionChat sc2 = null;

            if( tid == collabSplitCoreTrack) {
                Integer s = new Integer(sid);
                sid = splitGlobalTable.get( s ).intValue();
                sc2 = sectionVec.elementAt(sid).splitChat.elementAt(mid);
            }
            else {
                Integer s = new Integer(sid);
                sid = wholeGlobalTable.get( s ).intValue();
                sc2 = sectionVec.elementAt(sid).wholeChat.elementAt(mid);
            }

            if( sc2 == null )
                return;

            if( sc.url.equals( sc.url )) {
                try {
                    aw.setURL( new URL(sc.url) );
//                    aw.goToBottom();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        chatScrollPane.setViewportView( ct );
        chatScrollPane.updateUI();
        frame.repaint();
    }

    //===================================================================
    public void determinePreloadedSections() {
        int i;

        for( i = 0; i < SceneGraph.getNumSections( collabSplitCoreTrack ); i++)
        {

        }

        for( i = 0; i < SceneGraph.getNumSections( collabWholeCoreTrack ); i++)
        {

        }

        /*
        if( pdlg != null ) {
            pdlg.dispose();
            pdlg = null;
        }
        */
    }
    
    //===================================================================
    public void loadSessionLog() {
        // load up user's session log file
        // log file includes server addr, downloaed image list
        // check if login to server or not first
        if (!logoutbtn.isEnabled())
        {
            // user not logged in
            // show up message and return
            JOptionPane.showMessageDialog(this.frame,
                "You are not logged in!\nPlease log in first.!");
            return;
            
        }
        // choose file
        ExampleFileFilter cslFilter = new ExampleFileFilter("csl", "CSL File");

        this.chooser.setFileFilter(cslFilter);
        this.chooser.setDialogTitle("Load a Session Log file");
        int returnVal = chooser.showOpenDialog(this.frame);

        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile().getAbsoluteFile();
            System.out.println("You chose to save to file: " + selectedFile);
            loadSessionLogFile(selectedFile.getAbsolutePath());
        }

    }
    
    //===================================================================
    public void loadSessionLogFile(String filename) {
        // load up user's session log file
        // log file includes server addr, downloaed image list
        File logfile = new File(filename);
        try {
            String line = "";
            FileReader fr = new FileReader(logfile);
            BufferedReader reader = new BufferedReader(fr);
            // check this server addr first
            String serveraddr = serverfield.getSelectedItem().toString();
            line = reader.readLine();
            if (line == null) {
                // show up errro message and retrun
                JOptionPane.showMessageDialog(this.frame,
                        "Log file is empty!");
                reader.close();
                fr.close();
                return;
            }
            
            else {
                serveraddr.trim();
                if (!line.equals(serveraddr)) {
                    // this is wrong file
                    // showup message and return
                    JOptionPane.showMessageDialog(this.frame,
                        "Server address is not matched!\n" +
                        "Please load proper server log file." + 
                        "Address in log file: " + line + "\n" +
                        "Current server address: " + serveraddr);
                    reader.close();
                    fr.close();
                    return;
                }
            }
            
            // parse all lines in log file and subscribe section
            SessionSection ss;
            String [] toks;
            int index;
            while((line = reader.readLine()) != null ) {
                // index,split/whole,name
                toks = line.split(",");
                index = Integer.parseInt( toks[0] );
                // check index range
                if (index > -1 && index < sectionVec.size())
                    ss = sectionVec.elementAt(index);
                else
                    continue;
                    
                // FIXME: need to check name too? or assume that index is unique
                if (toks[1].equals("split") && ss.subscribedSplit != true) {
                    ss.subscribedSplit = true;
                    requestSplitCore(index);
                }
                else if (toks[1].equals("whole") && ss.subscribedWhole != true) {
                    ss.subscribedWhole = true;
                    requestWholeCore(index);
                }
                /*
                // FIXME: graph part is problematic
                //        since images must be loaded before start graph
                //        image download thread makes difficult
                else if (toks[1].equals("graphstart")) {
                    // we need to wait here till all section image loaded
                    // might be dangerous idea
                    while( !imageThread.queue.isEmpty())
                        ;
                }
                else if (toks[1].equals("graph")) {
                    System.out.println("** graph log line: " + toks[2]);
                    // graph log line format
                    // index, graph, dataset_name, field
                    // index is always 0 and meaningless
                    // we just need to find field in datasettree to find
                    // corelyzer.plugin.andrill.SessionDataset and int index
                    corelyzer.plugin.andrill.SessionDataset sd = null;
                    for( int i = 0; i < datasetTreeRoot.getChildCount(); i++) {
                        sd =  (corelyzer.plugin.andrill.SessionDataset) datasetTreeRoot.getChildAt(i);
                        System.out.println("** graph log line, sd name: " + sd.name);
                        if(sd.name.equals(toks[2]))
                            break;
                    }
                    if (sd != null) {
                        int filed = Integer.parseInt(toks[3]);
                        requestGraphs( sd, filed );
                    }
                }
                */
            }
            reader.close();
            fr.close();
        } catch (IOException e) {
            System.err.println("IOException in append server addr");
        }

    }

    //===================================================================
    public void saveSessionLog() {
        // save user's session log file for later use
        // check if login to server or not first
        if (!logoutbtn.isEnabled())
        {
            // user not logged in
            // show up message and return
            JOptionPane.showMessageDialog(this.frame,
                "You are not logged in!\nPlease log in first.!");
            return;
            
        }
        
        // choose file
        ExampleFileFilter cslFilter = new ExampleFileFilter("csl", "CSL File");

        this.chooser.setFileFilter(cslFilter);
        this.chooser.setDialogTitle("Save a Session Log file");
        int returnVal = chooser.showSaveDialog(this.frame);

        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile().getAbsoluteFile();
            
            // make sure it has .csl at the end
            String path = selectedFile.getAbsolutePath();
            path = path.replace('\\','/');
            String[] toks = path.split("/");
            if( !toks[ toks.length - 1].contains(".csl"))
            {
                path = path + ".csl";
                selectedFile = new File(path);
            }
            
            System.out.println("You chose to save to file: " + selectedFile);
            saveSessionLogFile(selectedFile.getAbsolutePath());
        }

    }
    
    public void saveSessionLogFile(String filename) {
        
        File logfile = new File(filename);
        try {
            String line = "";
            
            FileWriter fw = new FileWriter(logfile);
            BufferedWriter bw = new BufferedWriter(fw);
            
            // first write server addr in the first line
            line = serverfield.getSelectedItem().toString();
            bw.write(line+"\n");
            
            // scan throught all section table and write it
            SessionSection ss;
            for( int i = 0; i < sectionVec.size(); i++) {
                ss = sectionVec.elementAt(i);
                if( ss.subscribedSplit == true) {
                    // index,split,name
                    line = i + ",split," + ss.name + "\n";
                    bw.write(line);
                }
                if( ss.subscribedWhole == true) {
                    // index,whole,name
                    line = i + ",whole," + ss.name + "\n";
                    bw.write(line);
                }
            }
            bw.close();
            fw.close();
        
        } catch (IOException e) {
            System.err.println("IOException in append server addr");
        }

    }
}

//*************************************************************//
class ImageLoadRequest {
    public SessionSection ss;
    public int track;
    public int secId;
    public boolean split;
    public Hashtable< Integer, Integer > table;
    public String url;
    public String local;
    public int state;

    public ImageLoadRequest(){}

}

//*************************************************************//
class ImageLoadThread extends Thread {
    JProgressBar statBar;
    JPanel       gui;

    Vector< SessionSection > sectionVec;
    public LinkedList< ImageLoadRequest > queue;
    public boolean keepRunning;
    public int          splitCoreTrack;
    public int          wholeCoreTrack;

    // For accessing basic authenticate URLs
    private String username;
    private String password;
    
    //=================================================================
    public ImageLoadThread(JProgressBar b, JPanel panel, 
                           Vector< SessionSection > svec) {

        queue = new LinkedList< ImageLoadRequest >();
        statBar = b;
        gui = panel;
        keepRunning = true;
        sectionVec = svec;
        splitCoreTrack = -1;
        wholeCoreTrack = -1;
    }
    
    //=================================================================
    public void run() {
        while( keepRunning ) {
            try {

                if( queue.isEmpty()) {
                    sleep(2000);
                    continue;
                }
                
                // go through the whole linked list
                ImageLoadRequest ilr;
                while( (ilr = queue.getFirst()) != null) {
                    if( ilr.state == 0) {
                        ilr.state = 1;
                        System.out.println("Image Load Thread has a request!");
                        getImage( ilr.track, ilr.secId, ilr.ss,
                                  ilr.split, ilr.table, ilr.url, ilr.local);
                        ilr.state = 2;
                        ensureSectionsAtProperDepth();
                    }
                    else if( ilr.state == 1 ) 
                        continue;
                    else if( ilr.state == 2 )
                        queue.removeFirst();
                    
                    //ensureSectionsAtProperDepth();
                }

                //ensureSectionsAtProperDepth();

                sleep( 2000 );
            }
            catch( Exception e) {

            }
        }

    }

    //=================================================================
    private void ensureSectionsAtProperDepth() {

        for( int i = 0; i < sectionVec.size(); i++) {

            // check split core and whole core ids, if > -1 then
            // check positons and make sure at right depth
            SessionSection ss = sectionVec.elementAt(i);
            float p = ss.depth * 100.0f / 2.54f * SceneGraph.getCanvasDPIX(0);

            if( ss.splitId > -1 ) {
                SceneGraph.markSectionImmovable( splitCoreTrack, ss.splitId, 
                                                 false);
                SceneGraph.positionSection( splitCoreTrack, ss.splitId, p, 0);
                SceneGraph.markSectionImmovable( splitCoreTrack, ss.splitId, 
                                                 true);
            }

            if( ss.wholeId > -1 ) {
                SceneGraph.markSectionImmovable( wholeCoreTrack, ss.wholeId, 
                                                 false);
                SceneGraph.positionSection( wholeCoreTrack, ss.wholeId, p, 0);
                SceneGraph.markSectionImmovable( wholeCoreTrack, ss.wholeId, 
                                                 true);
            }

        }

        CorelyzerApp.getApp().updateGLWindows();
    }

    //=================================================================
    public void setUserPass(String u, String p) {
        username = u;
        password = p;
    }

    //=================================================================
    public void getImage(int track, int secId, SessionSection ss,
                         boolean split, Hashtable< Integer, Integer> table,
                         String url, String local )
    {
        
        System.out.println("Setting statBar");

        statBar.setMaximum(6);
        statBar.setValue(1);
        statBar.setString("Saving Image " + normalizeFilename(local, 48));
        //gui.paintImmediately( 0, 0, gui.getWidth(), gui.getHeight());
        gui.repaint();

        System.out.println("Saving Image " + local );

        File f = new File(local);

        if( !f.exists() )
        {
            boolean isDownloaded;
            try {
                isDownloaded = URLRetrieval.retrieveLocalCopy(url, local,
                        username, password);
            } catch (IOException e) {
                System.err.println("---> [Exception] " + getClass().getName() +
                        ": " + e);
                isDownloaded = false;
            }

            if(!isDownloaded)
            {
                // if we failed to download, show message & decide how to handle
                // 1. try again or 2. ignore
                if ( JOptionPane.showConfirmDialog(CorelyzerApp.getApp().
                        getMainFrame(), "Failed to download image file.\n" + 
                        local + "\n" +
                        "Do you want to try it again?",
                        "Image download failed", JOptionPane.YES_NO_OPTION)
                                   == JOptionPane.YES_OPTION)
                {
                    getImage(track, secId, ss, split, table, url, local);
                    return;
                }

                if( split )
                    ss.subscribedSplit = false;
                else
                    ss.subscribedWhole = false;
                
                statBar.setValue(0);
                statBar.setString("Failed to download image...");
                gui.repaint();

                return;
            }
        }

        statBar.setValue(2);
        f = new File( local );
        
        if( f.exists() )
        {
            statBar.setString("Loading Image, please wait...");
            gui.repaint();
            
            statBar.setValue(3);
            CorelyzerApp.getApp().setSelectedTrack(track);
            statBar.setValue(4);

            try {

                int localId = CorelyzerApp.getApp().loadImage( 
                    new File( local ), url );
                
                Integer localint, globalint;
                
                globalint = new Integer(secId);
                localint  = new Integer(localId);
                if( split ) {

                    table.put( localint, globalint );
                    ss.splitId = localId;
                    SceneGraph.lock();
                    
                    // SceneGraph.rotateSection( track,localId,90);
                    SceneGraph.setSectionOrientation(track, localId,
                            SceneGraph.PORTRAIT);
                    SceneGraph.positionSection( track, localId,
                                                ss.depth * 100.0f / 2.54f *
                                                SceneGraph.getCanvasDPIX(0),0);
                    SceneGraph.setSectionDPI( track, localId,
                                              ss.splitDPIX, ss.splitDPIY);
                    SceneGraph.bringSectionToFront( track, localId );
                    SceneGraph.markSectionImmovable( track, 
                                                     localId, true);
                    
                    // check if there are annotations to show up!
                    System.out.println("NUM CHATS: " + ss.splitChat.size());

                    for( int i = 0; i < ss.splitChat.size(); i++) {
                        SessionChat sc;
                        float px, py;
                        sc = ss.splitChat.elementAt(i);
                        px = sc.xpos_m * 100.0f / 2.54f * 
                            SceneGraph.getCanvasDPIX(0);
                        py = sc.ypos_m * 100.0f / 2.54f * 
                            SceneGraph.getCanvasDPIY(0);

                        sc.local = SceneGraph.createCoreSectionMarker(
                            track, localId, sc.group, MarkerType.CORE_POINT_MARKER, px, py );
                        sc.local_sectionid = localId;

                        SceneGraph.setCoreSectionMarkerGroup(track, localId,
                                                             sc.local,
                                                             sc.group);

                        SceneGraph.setCoreSectionMarkerVisibility(track,
                                                                  localId,
                                                                  sc.local,
                                                                  sc.visibility);

                    }

                    SceneGraph.unlock();
                }
                else {
                    table.put( localint, globalint );
                    ss.wholeId = localId;
                    
                    SceneGraph.lock();
                    
                    SceneGraph.positionSection( track, localId,
                                                ss.depth * 100.0f / 2.54f *
                                                SceneGraph.getCanvasDPIX(0),0);
                    SceneGraph.setSectionDPI( track, localId,
                                              ss.wholeDPI, ss.wholeDPI);
                    SceneGraph.bringSectionToFront( track, localId );
                    SceneGraph.markSectionImmovable( track,
                                                     localId, true);

                    // check if there are annotations to show up!
                    System.out.println("NUM CHATS: " + ss.wholeChat.size());

                    for( int i = 0; i < ss.wholeChat.size(); i++) {
                        SessionChat sc;
                        float px, py;
                        sc = ss.wholeChat.elementAt(i);
                        px = sc.xpos_m * 100.0f / 2.54f * 
                            SceneGraph.getCanvasDPIX(0);
                        py = sc.ypos_m * 100.0f / 2.54f * 
                            SceneGraph.getCanvasDPIY(0);

                        sc.local = SceneGraph.createCoreSectionMarker(
                            track, localId, sc.group, MarkerType.CORE_POINT_MARKER, px, py );
                        sc.local_sectionid = localId;

                        SceneGraph.setCoreSectionMarkerGroup(track, localId,
                                                             sc.local,
                                                             sc.group);

                        SceneGraph.setCoreSectionMarkerVisibility(track,
                                                                  localId,
                                                                  sc.local,
                                                                  sc.visibility);

                    }

                    SceneGraph.unlock();
                }

                statBar.setValue(0);
                statBar.setString("Image Downloaded");
                gui.repaint();

            } catch (Exception e) {
                System.out.println("FAILED!");
                statBar.setValue(0);
                statBar.setString("Failed to load image...");
                gui.repaint();
                e.printStackTrace();
                return;
            }
            
        }
        else
        {
            System.out.println("FAILED!");
            statBar.setValue(0);
            statBar.setString("Failed to download image...");
            gui.repaint();
        }
    }

    // Some helper functions

    /**
     * Return the extension portion of the file's name .
     *
     * @see #getExtension
     * @see javax.swing.filechooser.FileFilter#accept
     */
    private String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }

        }
        return null;
    }


    private String normalizeFilename(String filePathString, int length) {
        String filenameString = (new File(filePathString)).getName();

        if(filenameString.length() > length) {
            String extension = getExtension(new File(filePathString));
            int endIndex = (length * 2) / 3;
            filenameString = filenameString.substring(0, endIndex) +
                             "..." + extension;
        }

        return filenameString;
    }

}
