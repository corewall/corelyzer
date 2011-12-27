package corelyzer.plugin.andrill;

import java.io.DataOutputStream;

public class GlobalEventBroadcastThread extends Thread {

    //============================================
    GlobalEventBroadcastThread() {
    }

    public void run() {

    }

    //============================================
    public void addConnection(int id) {
        ClientConnectionThread cct = 
            CorelyzerSessionServer.getServer().getConnectionThreadByUserID(id);
        if( cct == null ) 
            return;
    }

    //==========================================
    public static void broadcastUserLoggedInEvent(int userid, String name) {
        CorelyzerSessionServer server = CorelyzerSessionServer.getServer();
        for( int i = 0; i < server.getNumClients(); i++) {
            ClientConnectionThread cct = server.getConnectionThread(i);
            ClientData cd = cct.getClientData();

            try {

                if( cd.getUserID() == 0 )
                    continue;

                if( cd.getUserID() == userid) 
                    continue;
                
                cct.acquireDOS();

                DataOutputStream dos = cct.getOutStream();

                byte[] str = name.getBytes("UTF-8");
                
                dos.writeByte( ServerResponse.USER_LOGGED_IN );
                dos.writeInt( userid );
                dos.writeInt( str.length );
                dos.write( str, 0, str.length );
                dos.flush();
                
                cct.releaseDOS();
            }
            catch (Exception e ) {
                cct.releaseDOS();
                System.out.println("SERVER FAILED TO BROADCAST LOGIN EVENT TO"
                                   + " AN EXISTING USER");
            }
        }
    }

    //=============================================
    public static void broadcastUserLoggedOutEvent(int userid) {
        CorelyzerSessionServer server = CorelyzerSessionServer.getServer();
        for( int i = 0; i < server.getNumClients(); i++) {
            ClientConnectionThread cct = server.getConnectionThread(i);
            ClientData cd = cct.getClientData();
            try {
                

                if( cd.getUserID() == 0 )
                    continue;

                cct.acquireDOS();

                DataOutputStream dos = cct.getOutStream();

                System.out.println("Notifying User " + cd.getUserID() + 
                                   " that User " + userid + " logged out");
                
                dos.writeByte( ServerResponse.USER_LOGGED_OUT );
                dos.writeInt( userid );
                dos.flush();
                
                cct.releaseDOS();
            }
            catch ( Exception e ) {
                cct.releaseDOS();
                System.out.println("COULDN'T NOTIFY A USER LOGGED OUT TO "
                                   + " ANOTHER USER");
            }
        }

    }

    //=============================================
    public static void broadcastTrackCreated(int userid, byte[] name) {
        CorelyzerSessionServer server = CorelyzerSessionServer.getServer();

        // probably want to place new track in to persistant system

        for( int i = 0; i < server.getNumClients(); i++) {
            try {
                ClientConnectionThread cct = server.getConnectionThread(i);
                ClientData cd = cct.getClientData();

                if( cd.getUserID() == 0 )
                    continue;

                if( cd.getUserID() == userid) 
                    continue;

                cct.notifyTrackCreated( userid, name );
            }
            catch (Exception e ) {
                System.out.println("SERVER FAILED TO BROADCAST TRACK CREATED "
                                   + "TO AN EXISTING USER");
            }
        }
    }

    //=============================================
    public static void broadcastNewSection(int secId) {
        String name = DataManager.getSectionName(secId);
        float depth = DataManager.getSectionDepth(secId);
        float length = DataManager.getSectionLength(secId);

        if( name == null)
            return;

        System.out.println("Broadcasting section: " + name + 
                           " at depth: " + depth + " with length: " +
                           length);

        byte[] nbuf;

        try {
            nbuf = name.getBytes("UTF-8");
        } catch( Exception e) {
            System.out.println("COULDN'T CONVERT TO UTF-8!! WTF!!");
            e.printStackTrace();
            return;
        }

        CorelyzerSessionServer server = CorelyzerSessionServer.getServer();

        // place section in persistant system
        server.appendSectionListFile(secId);

        // notify existing users

        for( int i = 0; i < server.getNumClients(); i++) {
            ClientConnectionThread cct = server.getConnectionThread(i);
            ClientData cd = cct.getClientData();
            try {

                if( cd.getUserID() == 0 )
                    continue;

                cct.acquireDOS();

                DataOutputStream dos = cct.getOutStream();

                dos.writeByte  (ServerResponse.NEW_SECTION);
                dos.writeInt   (secId);
                dos.writeFloat (depth);
                dos.writeFloat (length);
                dos.writeInt   (nbuf.length);
                dos.write      (nbuf, 0 ,nbuf.length);
                dos.flush();

                cct.releaseDOS();
            }
            catch (Exception e ) {
                cct.releaseDOS();
                System.out.println("SERVER FAILED TO BROADCAST TRACK CREATED "
                                   + "TO AN EXISTING USER");
            }
        }

    }

    //=============================================
    public static void broadcastSplitCoreAvailable(int secId) {
        String name = DataManager.getSectionName(secId);
        String url = DataManager.getSplitCoreURL(secId);
        float depth = DataManager.getSectionDepth(secId);
        float length = DataManager.getSectionLength(secId);
        float dpi_x = DataManager.getSplitCoreDPIX(secId);
        float dpi_y = DataManager.getSplitCoreDPIY(secId);

        if( name == null || url == null)
            return;

        System.out.println("Broadcasting Split core: " + name + 
                           " at depth: " + depth + " with length: " +
                           length +
                           " and dpi_x: " + dpi_x +
                           " dpi_y:" + dpi_y + " at URL: " + url);
        byte[] nbuf;
        byte[] ubuf;

        try {
            nbuf = name.getBytes("UTF-8");
            ubuf = url.getBytes("UTF-8");
        } catch( Exception e) {
            System.out.println("COULDN'T CONVERT TO UTF-8!! WTF!!");
            e.printStackTrace();
            return;
        }

        CorelyzerSessionServer server = CorelyzerSessionServer.getServer();

        // place new track in to persistant system
        // FIXME server.appendSplitCoreListFile(secId);
        server.dumpAllSplitCoreListFile();

        // notify existing users

        for( int i = 0; i < server.getNumClients(); i++) {
            ClientConnectionThread cct = server.getConnectionThread(i);
            ClientData cd = cct.getClientData();
            try {

                if( cd.getUserID() == 0 )
                    continue;

                cct.acquireDOS();

                DataOutputStream dos = cct.getOutStream();

                dos.writeByte  (ServerResponse.SPLIT_CORE_AVAILABLE);
                dos.writeInt   (secId);
                dos.writeFloat (depth);
                dos.writeFloat (length);
                dos.writeFloat (dpi_x);
                dos.writeFloat (dpi_y);
                dos.writeInt   (nbuf.length);
                dos.write      (nbuf, 0 ,nbuf.length);
                dos.writeInt   (ubuf.length);
                dos.write      (ubuf, 0, ubuf.length);
                dos.flush();

                cct.releaseDOS();
            }
            catch (Exception e ) {
                cct.releaseDOS();
                System.out.println("SERVER FAILED TO BROADCAST TRACK CREATED "
                                   + "TO AN EXISTING USER");
            }
        }
        
    }

    //=============================================
    public static void broadcastWholeCoreAvailable(int secId) {
        String name = DataManager.getSectionName(secId);
        String url = DataManager.getWholeCoreURL(secId);
        float depth = DataManager.getSectionDepth(secId);
        float length = DataManager.getSectionLength(secId);
        float dpi = DataManager.getWholeCoreDPI(secId);

        if( name == null || url == null)
            return;

        byte[] nbuf;
        byte[] ubuf;

        try {
            nbuf = name.getBytes("UTF-8");
            ubuf = url.getBytes("UTF-8");
        } catch( Exception e) {
            System.out.println("COULDN'T CONVERT TO UTF-8!! WTF!!");
            e.printStackTrace();
            return;
        }

        CorelyzerSessionServer server = CorelyzerSessionServer.getServer();

        // place new track in to persistant system
        // FIXME server.appendWholeCoreListFile(secId);
        server.dumpAllWholeCoreListFile();

        // notify existing users

        for( int i = 0; i < server.getNumClients(); i++) {
            ClientConnectionThread cct = server.getConnectionThread(i);
            ClientData cd = cct.getClientData();

            try {

                if( cd.getUserID() == 0 )
                    continue;

                cct.acquireDOS();

                DataOutputStream dos = cct.getOutStream();

                dos.writeByte  (ServerResponse.WHOLE_CORE_AVAILABLE);
                dos.writeInt   (secId);
                dos.writeFloat (depth);
                dos.writeFloat (length);
                dos.writeFloat (dpi);
                dos.writeInt   (nbuf.length);
                dos.write      (nbuf, 0 ,nbuf.length);
                dos.writeInt   (ubuf.length);
                dos.write      (ubuf, 0, ubuf.length);
                dos.flush();


                cct.releaseDOS();
            }
            catch (Exception e ) {
                cct.releaseDOS();
                System.out.println("SERVER FAILED TO BROADCAST TRACK CREATED "
                                   + "TO AN EXISTING USER");
            }
        }
        
    }

    //===============================================
    public static void broadcastNewDatasetAvailable(SessionDataset sd) {
        if( sd == null ) return;

        byte[] nbuf;
        byte[][] fbuf;

        fbuf = new byte[sd.fields.size()][];

        try{
            nbuf = sd.name.getBytes("UTF-8");
            for( int i = 0; i < sd.fields.size(); i++)
                fbuf[i] = sd.fields.elementAt(i).getBytes("UTF-8");
        } catch( Exception e) {
            System.out.println("Couldn't get strings!");
            e.printStackTrace();
            return;
        }

        CorelyzerSessionServer server = CorelyzerSessionServer.getServer();

        for( int i = 0; i < server.getNumClients(); i++) {
            ClientConnectionThread cct = server.getConnectionThread(i);
            ClientData cd = cct.getClientData();
            
            if( cd.getUserID() == 0)
                continue;

            try {
                cct.acquireDOS();
                DataOutputStream dos = cct.getOutStream();

                dos.writeByte  (ServerResponse.NEW_DATASET_AVAILABLE );
                dos.writeInt   (nbuf.length);
                dos.write      (nbuf, 0, nbuf.length);
                dos.writeInt   (sd.fields.size());

                for( int k = 0; k < sd.fields.size(); k++) {
                    dos.writeInt   ( fbuf[k].length );
                    dos.write      ( fbuf[k], 0, fbuf[k].length );
                    dos.writeFloat ( sd.mins.elementAt(k).floatValue() );
                    dos.writeFloat ( sd.maxs.elementAt(k).floatValue() );
                }
                dos.flush();

                cct.releaseDOS();
            }
            catch( Exception e) {
                cct.releaseDOS();
            }
        }
    }

    //=================================================================
    public static void broadcastNewChat( int split, int secId, int chatId) {

        String url = DataManager.getChatURL( split, secId, chatId );
        int    uid = DataManager.getChatCreator( split, secId, chatId );
        int    groupid = DataManager.getChatGroup( split, secId, chatId );
        int    typeid = DataManager.getMarkerType( split, secId, chatId );
        float  posx = DataManager.getChatDepthX( split, secId, chatId );
        float  posy = DataManager.getChatDepthY( split, secId, chatId );

        long created = DataManager.getChatCreated( split, secId, chatId );
        long modified = DataManager.getChatLastModified( split, secId, chatId );

        byte[] ubuf;
       
        try {
            ubuf = url.getBytes("UTF-8");
        } catch(Exception e) {
            System.out.println("COULN'T GET STRING!");
            e.printStackTrace();
            return;
        }

        CorelyzerSessionServer server = CorelyzerSessionServer.getServer();

        for( int i = 0; i < server.getNumClients(); i++) {
            ClientConnectionThread cct = server.getConnectionThread(i);
            ClientData cd = cct.getClientData();
            
            if( cd.getUserID() == 0)
                continue;

            try {
                cct.acquireDOS();
                DataOutputStream dos = cct.getOutStream();
                
                dos.writeByte( ServerResponse.NEW_CHAT );
                dos.writeInt ( split );
                dos.writeInt ( secId );
                dos.writeInt ( chatId );
                dos.writeInt ( uid );
                dos.writeInt ( groupid );
                dos.writeInt ( typeid );
                dos.writeFloat( posx );
                dos.writeFloat( posy );

                dos.writeLong( created );
                dos.writeLong( modified );

                dos.writeInt ( ubuf.length );
                dos.write    ( ubuf, 0, ubuf.length );

                dos.flush();

                cct.releaseDOS();
            }
            catch( Exception e) {
                cct.releaseDOS();
            }
        }
        
    }

    //=================================================================
    public static void broadcastNewChatEntry( int split, int secid, 
                                              int entryid) {

        CorelyzerSessionServer server = CorelyzerSessionServer.getServer();

        for( int i = 0; i < server.getNumClients(); i++) {
            ClientConnectionThread cct = server.getConnectionThread(i);
            ClientData cd = cct.getClientData();
            
            if( cd.getUserID() == 0)
                continue;

            try {
                cct.acquireDOS();
                DataOutputStream dos = cct.getOutStream();

                long modified = DataManager.getChatLastModified( split, secid,
                                                                 entryid );
                dos.writeByte( ServerResponse.NEW_CHAT_ENTRY );
                dos.writeInt ( split );
                dos.writeInt ( secid );
                dos.writeInt ( entryid );
                dos.writeLong( modified );

                dos.flush();

                cct.releaseDOS();
            }
            catch( Exception e) {
                cct.releaseDOS();
            }
        }
        
    }
}

