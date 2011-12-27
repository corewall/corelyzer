package corelyzer.plugin.andrill;

import corelyzer.data.ChatGroup;
import corelyzer.data.MarkerType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class DataManager {

    private static Vector< SessionSection > coreImages;
    private static Vector< SessionDataset > datasets;

    private static boolean initialized = false;
    private static String copy;
    
    //==================================================================
    public static void init() {
        coreImages = new Vector< SessionSection >();
        datasets = new Vector< SessionDataset >();
        
        if( System.getProperty("os.name").contains("Windows") )
            copy = new String("cmd.exe /c copy ");
        else
            copy = new String("cp ");

    }

    //==================================================================
    public static void addSection(String name, float depth, float length) {

        // make sure section isn't already there
        for( int i = 0; i < coreImages.size(); i++) {
            if( coreImages.elementAt(i).name.equals(name))
                return;
        }

        SessionSection ss = new SessionSection(name, depth, length);
        coreImages.add(ss);

        ss.globalId = coreImages.size() - 1;
    }

    //==================================================================
    public static void addDataset(String name) {

        SessionDataset sd = new SessionDataset(name);
        datasets.add( sd );

        sd.globalId = datasets.size() - 1;
    }

    public static void addDataset(SessionDataset sd) {
        datasets.add(sd);
        sd.globalId = datasets.size() - 1;
    }

    //==================================================================
    public static void setSplitCoreURL(String sectionName, String url) {

        for( int i = 0; i < coreImages.size(); i++) {
            SessionSection ss = coreImages.elementAt(i);
            if( ss == null ) 
                continue;

            if( ss.name.equals(sectionName) ) {
                ss.splitCoreURL = url;
                return;
            }
        }
    }

    //==================================================================
    public static void setWholeCoreURL(String sectionName, String url) {

        for( int i = 0; i < coreImages.size(); i++) {
            SessionSection ss = coreImages.elementAt(i);
            if( ss == null ) 
                continue;

            if( ss.name.equals(sectionName) ) {
                ss.wholeCoreURL = url;
                return;
            }
        }
    }

    //==================================================================
    public static void setSplitCoreDPI(String sectionName,
                                       float dpi_x, float dpi_y)
    {

        for( int i = 0; i < coreImages.size(); i++) {
            SessionSection ss = coreImages.elementAt(i);
            if( ss == null ) 
                continue;

            if( ss.name.equals(sectionName) ) {
                ss.splitDPIX = dpi_x;
                ss.splitDPIY = dpi_y;
                return;
            }
        }
    }

    //==================================================================
    public static void setWholeCoreDPI(String sectionName, float dpi) {

        for( int i = 0; i < coreImages.size(); i++) {
            SessionSection ss = coreImages.elementAt(i);
            if( ss == null ) 
                continue;

            if( ss.name.equals(sectionName) ) {
                ss.wholeDPI = dpi;
                return;
            }
        }
    }

    //==================================================================
    public static int getNumSections() {
        return coreImages.size();
    }

    //==================================================================
    public static int getSectionID(String sectionName) {

        for( int i = 0; i < coreImages.size(); i++) {
            SessionSection ss = coreImages.elementAt(i);
            if( ss == null ) 
                continue;

            if( ss.name.equals(sectionName) ) {
                return i;
            }
        }

        return -1;
    }

    //==================================================================
    public static String getSectionName(int id) {

        if( id < 0 || id >= coreImages.size())
            return null;

        return coreImages.elementAt(id).name;
    }

    //==================================================================
    public static String getSplitCoreURL(int id) {

        if( id < 0 || id >= coreImages.size())
            return null;

        return coreImages.elementAt(id).splitCoreURL;
    }


    //==================================================================
    public static String getWholeCoreURL(int id) {

        if( id < 0 || id >= coreImages.size())
            return null;

        return coreImages.elementAt(id).wholeCoreURL;
    }

    //==================================================================
    public static float getSectionDepth(int id){

        if( id < 0 || id >= coreImages.size())
            return 0.0f;

        return coreImages.elementAt(id).depth;

    }

    //==================================================================
    public static float getSectionLength(int id) {

        if( id < 0 || id >= coreImages.size())
            return 0.0f;

        return coreImages.elementAt(id).length;

    }


    //==================================================================
    public static float getSplitCoreDPIX(int id) {

        if( id < 0 || id >= coreImages.size())
            return 0.0f;

        return coreImages.elementAt(id).splitDPIX;

    }

    public static float getSplitCoreDPIY(int id) {

        if( id < 0 || id >= coreImages.size())
            return 0.0f;

        return coreImages.elementAt(id).splitDPIY;

    }

    //==================================================================
    public static float getWholeCoreDPI(int id) {

        if( id < 0 || id >= coreImages.size())
            return 0.0f;

        return coreImages.elementAt(id).wholeDPI;

    }

    //==================================================================
    public static int getNumSplitChats( int id) {

        if( id < 0 || id >= coreImages.size())
            return -1;

        return coreImages.elementAt(id).splitChat.size();
    }

    //==================================================================
    public static int getNumWholeChats( int id) {

        if( id < 0 || id >= coreImages.size())
            return -1;

        return coreImages.elementAt(id).wholeChat.size();
    }

    //==================================================================
    public static String newChat(int usrid, int secid, int split, int groupid,
                                 int typeid, float depthX, float depthY, long created)
    {

        if( secid < 0 || secid >= coreImages.size())
            return null;

        int entrynum = -1;
        SessionChat sc = null;

        if( split == 0 ) {  // splitcore chat
            // FIXME sc = new corelyzer.plugin.andrill.SessionChat( 0, id, 0, depth );
            sc = new SessionChat( 0, secid, 0, groupid, typeid,
                                  depthX, depthY, created );

            coreImages.elementAt(secid).splitChat.add(sc);
            entrynum = coreImages.elementAt(secid).splitChat.size() - 1;
            CorelyzerSessionServer server = CorelyzerSessionServer.getServer();

            String secname = coreImages.elementAt(secid).name;
            secname = secname.replace(' ', '_');
            String url = new String(server.getAnnotationRootURL() + "/" + 
                                    secname +
                                    ".split_" + entrynum + ".html");
            sc.url = url;
            sc.localfile = new String(server.getAnnotationRootDIR() + "/" 
                                      + secname +
                ".split_" + entrynum + ".html");
            sc.userID = usrid;

            // Obtain chat file's lastModified date
            File file = new File(sc.localfile);
            if(file.exists()) {
                sc.lastModified = file.lastModified();
            } else {
                sc.lastModified = sc.created;
            }

            System.out.println("NEW CHAT AT URL: " + url );
            System.out.println("LOCAL FILE: " + sc.localfile );
            return url;
        }
        else if(split == 1) { // wholecore chat
            // FIXME sc = new corelyzer.plugin.andrill.SessionChat( 0, id, 0, depth );
            sc = new SessionChat( 1, secid, 0, groupid, typeid,
                                  depthX, depthY, created );

            coreImages.elementAt(secid).wholeChat.add(sc);
            entrynum = coreImages.elementAt(secid).wholeChat.size() - 1;
            CorelyzerSessionServer server = CorelyzerSessionServer.getServer();

            String secname = coreImages.elementAt(secid).name;
            secname = secname.replace(' ', '_');
            String url = new String(server.getAnnotationRootURL() + "/" 
                                    + secname +
                                    ".whole_" + entrynum + ".html");
            sc.url = url;
            sc.localfile = new String(server.getAnnotationRootDIR() + "/" 
                                      + secname +
                                      ".whole_" + entrynum + ".html");
            sc.userID = usrid;

            // Obtain chat file's lastModified date
            File file = new File(sc.localfile);
            if(file.exists()) {
                sc.lastModified = file.lastModified();
            } else {
                sc.lastModified = sc.created;
            }

            System.out.println("NEW CHAT AT URL: " + url );
            System.out.println("LOCAL FILE: " + sc.localfile );
            return url;
        } else {
            System.err.println("Unsupported chat target! " + split);
            return null;
        }
    }

    //==================================================================
    public static int getChatEntryByURL( int split, int secId, String url) {
        if( secId < 0 || secId >= coreImages.size() )
            return -1;

        SessionSection ss = coreImages.elementAt(secId);

        for( int i = 0; i < ss.splitChat.size(); i++) {

            SessionChat sc;
            if( split == 0 )
                sc = ss.splitChat.elementAt(i);
            else
                sc = ss.wholeChat.elementAt(i);

            if( sc.url.equals( url ) )
                return i;
        }

        return -1;
    }


    //==================================================================
    public static int getChatEntryByDepth( int split, int secId, float depth) {
        if( secId < 0 || secId >= coreImages.size() )
            return -1;

        SessionSection ss = coreImages.elementAt(secId);

        for( int i = 0; i < ss.splitChat.size(); i++) {

            SessionChat sc;
            if( split == 0 )
                sc = ss.splitChat.elementAt(i);
            else
                sc = ss.wholeChat.elementAt(i);

            System.out.println("Comparing depth: " + depth + " to " + 
                               sc.xpos_m);
            if( sc.xpos_m == depth )
                return i;
        }

        return -1;
    }

    //==================================================================
    public static int getChatCreator( int split, int secId, int chatId ) {
        if( secId < 0 || secId >= coreImages.size() )
            return -1;

        SessionSection ss = coreImages.elementAt(secId);
        
        if( split == 0 ) {
            if( chatId < 0 || chatId >= ss.splitChat.size() )
                return -1;

            return ss.splitChat.elementAt(chatId).userID;

        } 
        else {
            if( chatId < 0 || chatId >= ss.wholeChat.size() )
                return -1;

            return ss.wholeChat.elementAt(chatId).userID;
        }
    }

    //==================================================================
    public static float getChatDepthX( int split, int secId, int chatId ) {
        if( secId < 0 || secId >= coreImages.size() )
            return -1;

        SessionSection ss = coreImages.elementAt(secId);
        
        if( split == 0 ) {
            if( chatId < 0 || chatId >= ss.splitChat.size() )
                return -1;

            return ss.splitChat.elementAt(chatId).xpos_m;

        } 
        else {
            if( chatId < 0 || chatId >= ss.wholeChat.size() )
                return -1;

            return ss.wholeChat.elementAt(chatId).xpos_m;
        }

    }

    //==================================================================
    public static float getChatDepthY( int split, int secId, int chatId ) {
        if( secId < 0 || secId >= coreImages.size() )
            return -1;

        SessionSection ss = coreImages.elementAt(secId);
        
        if( split == 0 ) {
            if( chatId < 0 || chatId >= ss.splitChat.size() )
                return -1;

            return ss.splitChat.elementAt(chatId).ypos_m;

        } 
        else {
            if( chatId < 0 || chatId >= ss.wholeChat.size() )
                return -1;

            return ss.wholeChat.elementAt(chatId).ypos_m;
        }

    }

    //==================================================================
    public static long getChatCreated( int split, int secId, int chatId ) {
        if( secId < 0 || secId >= coreImages.size() )
            return -1;

        SessionSection ss = coreImages.elementAt(secId);

        if( split == 0 ) {
            if( chatId < 0 || chatId >= ss.splitChat.size() )
                return -1;

            return ss.splitChat.elementAt(chatId).created;

        }
        else {
            if( chatId < 0 || chatId >= ss.wholeChat.size() )
                return -1;

            return ss.wholeChat.elementAt(chatId).created;
        }
    }

    //==================================================================
    public static long getChatLastModified( int split, int secId, int chatId )
    {
        if( secId < 0 || secId >= coreImages.size() )
            return -1;

        SessionSection ss = coreImages.elementAt(secId);

        if( split == 0 ) {
            if( chatId < 0 || chatId >= ss.splitChat.size() )
                return -1;

            return ss.splitChat.elementAt(chatId).lastModified;

        }
        else {
            if( chatId < 0 || chatId >= ss.wholeChat.size() )
                return -1;

            return ss.wholeChat.elementAt(chatId).lastModified;
        }
    }

    //==================================================================
    public static void setChatLastModified( int split, int secId, int chatId,
                                            long modified)
    {
        if( secId < 0 || secId >= coreImages.size() ) return;

        SessionSection ss = coreImages.elementAt(secId);

        if( split == 0 ) { // splitcore
            if( chatId < 0 || chatId >= ss.splitChat.size() ) return;

            ss.splitChat.elementAt(chatId).lastModified = modified;
        }
        else if ( split == 1 ) {  // wholecore
            if( chatId < 0 || chatId >= ss.wholeChat.size() ) return;

            ss.wholeChat.elementAt(chatId).lastModified = modified;
        }
        else { // more targets that chat attachted to possible.
            return;
        }
    }

    //==================================================================
    public static String getChatLocal( int split, int secId, int chatId) {
        if( secId < 0 || secId >= coreImages.size() )
            return null;

        SessionSection ss = coreImages.elementAt(secId);
        
        if( split == 0 ) {
            if( chatId < 0 || chatId >= ss.splitChat.size() )
                return null;

            return ss.splitChat.elementAt(chatId).localfile;

        } 
        else {
            if( chatId < 0 || chatId >= ss.wholeChat.size() )
                return null;

            return ss.wholeChat.elementAt(chatId).localfile;
        }

    }

    //==================================================================
    public static String getChatURL( int split, int secId, int chatId ) {
        if( secId < 0 || secId >= coreImages.size() )
            return null;

        SessionSection ss = coreImages.elementAt(secId);
        
        if( split == 0 ) {
            if( chatId < 0 || chatId >= ss.splitChat.size() )
                return null;

            return ss.splitChat.elementAt(chatId).url;

        } 
        else {
            if( chatId < 0 || chatId >= ss.wholeChat.size() )
                return null;

            return ss.wholeChat.elementAt(chatId).url;
        }

    }

    //==================================================================
    public static int getChatGroup( int split, int secId, int chatId ) {
        if( secId < 0 || secId >= coreImages.size() )
          return ChatGroup.DEFAULT;

        SessionSection ss = coreImages.elementAt(secId);

        if( split == 0 ) {
            if( chatId < 0 || chatId >= ss.splitChat.size() ) {
                return ChatGroup.DEFAULT;
            }

            return ss.splitChat.elementAt(chatId).group;
        }
        else {
            if( chatId < 0 || chatId >= ss.wholeChat.size() ) {
                return ChatGroup.DEFAULT;
            }

            return ss.wholeChat.elementAt(chatId).group;
        }

    }

    //==================================================================
    public static int getMarkerType( int split, int secId, int chatId ) {
        if( secId < 0 || secId >= coreImages.size() )
          return MarkerType.CORE_DEFAULT_MARKER;

        SessionSection ss = coreImages.elementAt(secId);

        if( split == 0 ) {
            if( chatId < 0 || chatId >= ss.splitChat.size() ) {
                return MarkerType.CORE_DEFAULT_MARKER;
            }

            return ss.splitChat.elementAt(chatId).type;
        }
        else {
            if( chatId < 0 || chatId >= ss.wholeChat.size() ) {
                return MarkerType.CORE_DEFAULT_MARKER;
            }

            return ss.wholeChat.elementAt(chatId).type;
        }

    }

    //==================================================================
    public static String appendChatEntry( int split, int secId, int chatId,
                                          String entry, String name ) {
        System.gc();

        String text = new String("");
        try {
            
            String local = getChatLocal( split, secId, chatId );
            System.out.println("APPENDING TO CHAT ON FILE: " + local);
            if( local == null )
                return null;
            
            File f = new File(local);
            if( f.exists() ) {
                BufferedReader br = new BufferedReader( new FileReader(local));
                String line;
                while( (line = br.readLine()) != null) {
                    text += line;
                }
                br.close();
                br = null;
            } 
            else {
                String secname = coreImages.elementAt(secId).name;
                String url;
                if( split == 0 ) {
                    secname += " - split core ";
                    url = coreImages.elementAt(secId).splitCoreURL;
                }
                else {
                    secname += " - whole core ";
                    url = coreImages.elementAt(secId).wholeCoreURL;
                }

                String header = "<h3><a href=\"" + url 
                               + "\">" + secname + "</a> at hole depth: " +
                               getChatDepthX( split, secId, chatId ) + " m</h3>"
                               + "<br>\n";               
                text = new String("<html>\n\t<head>\n\n</head>\n\t<body>" +
                                  "\n" + header + "<hr>\t</body>\n</html>\n");
            }
            
            // modify entry!
            long modified = System.currentTimeMillis();

            Date now = new Date(modified);
            SimpleDateFormat format = new SimpleDateFormat(
                "MM/dd/yyy 'at' hh:mm:ss z");
            String time = format.format(now);

            int entryheaderend = entry.indexOf("<br>");
            String newEntryHeader = new String("\n<table bgcolor=\"#ffffff\"" +
                                               " width=\"100%\" border=\"0\">" +
                                               "<tr><td><b>On " + time
                                               + " " + name + 
                                               " wrote:</b><br>");

            String newentrytext = newEntryHeader + 
                entry.substring( entryheaderend, entry.length());
                                               
            // System.out.println("NEW ENTRY: " + entry );

            String before, end;
            int bodyend = text.indexOf("<hr>") + 4;
            before = text.substring(0,bodyend);
            end = text.substring(bodyend, text.length());

            String newtext = before + newentrytext + end;

//            System.out.println("NEW TEXT: " + newtext );
            
            FileWriter fw = new FileWriter(local + ".temp");
            fw.write( newtext, 0, newtext.length());
            fw.flush();
            fw.close();
            fw = null;
            newtext = null;
            text = null;

            // update chat's lastModified field
            DataManager.setChatLastModified(split, secId, chatId, modified);
            
            // james modification: fix windows cmd runtime to copy file
            if( System.getProperty("os.name").contains("Windows") ) {
                String tempStr = local.replace('/', '\\');
                System.out.println("copying chat file: "+tempStr);
                Runtime.getRuntime().exec( copy + tempStr + ".temp " + tempStr );
            }
            else
                Runtime.getRuntime().exec( copy + local + ".temp " + local );
                
            System.gc();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
        return entry;
    }

    //==================================================================
    public static int getDatasetID(String name) {

        for( int i = 0; i < datasets.size(); i++) {
            if( datasets.elementAt(i) == null)
                continue;

            if( datasets.elementAt(i).name.equals(name))
                return i;
        }

        return -1;
    }

    //==================================================================
    public static int getNumDatasets() {
        return datasets.size(); 
    }

    //==================================================================
    public static SessionDataset getDataset(int id) {
        return datasets.elementAt(id);
    }

    //==================================================================
    public static SessionTable getTableForSection( int setId, String secname){
        SessionDataset sd = datasets.elementAt(setId);
        for( int i = 0; i < sd.tables.size(); i++) {
            // FIXME if( sd.tables.elementAt(i).name.equals(secname))
            String serverTableName = sd.tables.elementAt(i).name;
            if( secname.contains(serverTableName) )            
                return sd.tables.elementAt(i);
        }

        return null;
    }

    //==================================================================
    public static boolean hasSection(String sectionName) {

        for( int i = 0; i < coreImages.size(); i++) {
            SessionSection ss = coreImages.elementAt(i);

            if( ss == null )
                continue;

            if( ss.name.equals(sectionName) ) {
                return true;
            }
        }

        return false;
    }

    //==================================================================
    public static void setWholeCoreStartDepth(String sectionName, float start) {

        for( int i = 0; i < coreImages.size(); i++) {
            SessionSection ss = coreImages.elementAt(i);

            if( ss == null )
                continue;

            if( ss.name.equals(sectionName) ) {
                ss.depth = start; // TODO need to verify ssDS
                return;
            }
        }

    }

    public static void setWholeCoreLength(String sectionName, float length) {

        for( int i = 0; i < coreImages.size(); i++) {
            SessionSection ss = coreImages.elementAt(i);

            if( ss == null )
                continue;

            if( ss.name.equals(sectionName) ) {
                ss.length = length; // TODO need to verify ssDS
                return;
            }
        }

    }

    // int i is sequence index, not sectionId
    public static SessionSection getSessionSection(int i) {
        if( (i>=0) && (i<coreImages.size()) ) {
            return coreImages.elementAt(i);
        } else {
            return null;
        }
    }
}
