package corelyzer.plugin.andrill;

import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

public class CorelyzerSessionBatchAdminClient {

    // Processing results
    final int SUCCESS            = 0;
    final int CANNOT_CONNECT     = 1;
    final int CANNOT_LOGIN       = 2;

    final int PARSE_DATA_SUCCESS = 3;
    final int PARSE_DATA_ERROR   = 4;

    final int PARSE_IMG_SUCCESS  = 5;
    final int PARSE_IMG_ERROR    = 6;

    Socket socket;
    SharedMiceClientThread mouseThread;
    DataInputStream dis;
    DataOutputStream dos;
    String server;
    boolean closeConnection;
    boolean retryLogin;

    String username;
    String password;

    Vector<SessionSection> sectionVec;
    SessionSection[] sarray;

    String singlePixelImageUrl =
            "http://www.evl.uic.edu/cavern/corewall/blah.jpg";

    boolean mode; // false: image or true: data
    String input_file;

    //==========================================
    public CorelyzerSessionBatchAdminClient(String s) {
        this.server = s;
        username = null;
        password = null;
        sectionVec = new Vector<SessionSection>();
    }

    public CorelyzerSessionBatchAdminClient(String [] args) {
        this.server     = args[0];
        this.username   = args[1];
        this.password   = args[2];
        this.mode       = args[3].equalsIgnoreCase("data");
        this.input_file = args[4];

        sectionVec = new Vector<SessionSection>(); // FIXME need this?
    }

    //==========================================
    public void run() {
        try {

            BufferedReader bir = new BufferedReader(
                new InputStreamReader(System.in));

            closeConnection = false;
            socket = new Socket(server,11999);
            dos = new DataOutputStream( socket.getOutputStream() );
            dis = new DataInputStream( socket.getInputStream() );

            if( !socket.isConnected() )
                return;

            System.out.println("--- CLIENT CONNECTED ---");
            int i = 0;

            closeConnection = true;

            if( login() )
            {
                closeConnection = false;
                System.out.println("--- ADMIN LOGGED IN ---");
            }

            while( !closeConnection && socket.isConnected() )
            {
                String line = bir.readLine();
                String[] toks = line.split(" ");
                if( toks[0].equals("new-section")) {
                    processNewSection(toks);
                }
                else if( toks[0].equals("new-split-core")) {
                    processNewSplitCore(toks);
                }
                else if( toks[0].equals("new-whole-core")) {
                    processNewWholeCore(toks);
                }
                else if( toks[0].equals("logout")) {
                    System.exit(1);
                }
                else if( toks[0].equals("server-shutdown")) {
                    dos.writeByte( ClientRequest.SHUTDOWN_SERVER);
                    dos.flush();
                    System.out.println("NOTIFIED SERVER TO SHUTDOWN..PLEASE "
                                       + " WAIT");
                    System.out.flush();
                    /// FIXME sleep(1000);
                    System.exit(1);
                }
                else if( toks[0].equals("list-sections")) {
                    processListSections();
                }
                else if( toks[0].equals("list-split-cores")) {
                    processListSplitCores(toks);
                }
                else if( toks[0].equals("list-whole-cores")) {
                    processListWholeCores(toks);
                }
                else if( toks[0].equals("list-datasets")) {
                    processListDatasets();
                }
                else if( toks[0].equals("list-tables")) {
                    processListTables(toks);
                }
                else if( toks[0].equals("list-table-data")) {
                    processListTableData(toks);
                }
                else if( toks[0].equals("new-dataset")) {
                    processNewDataset(toks);
                }
                else if( toks[0].equals("run-backup")) {
                    dos.writeByte( ClientRequest.RUN_BACKUP );
                    dos.flush();
                }
                else if( toks[0].equals("run-script")) {
                    if( toks.length < 2 ) {
                        System.out.println("Please give a script file");
                    }
                    else{
                        runScript(toks[1]);
                    }
                }
                else if( toks[0].equals("help")) {
                    System.out.println("Commands list: ---------");
                    System.out.println("new-section");
                    System.out.println("new-split-core");
                    System.out.println("new-whole-core");
                    System.out.println("new-dataset");
                    System.out.println("list-sections");
                    System.out.println("list-datasets");
                    System.out.println("list-tables");
                    System.out.println("list-table-data");
                    System.out.println("run-backup");
                    System.out.println("run-script");
                    System.out.println("logout");
                    System.out.println("server-shutdown");
                    System.out.println("new-missing-section");
                    System.out.println("new-missing-split-core");
                }
                else if( toks[0].equals("new-missing-section") ) {
                    processMissingSection(toks);
                }
                else if( toks[0].equals("new-missing-split-core") ) {
                    processMissingSplitCore(toks);
                }
                else {
                    System.out.println(toks[0] + " is an unknown command.");
                    System.out.println("Type 'help' for list of commands.");
                }
            }

            System.out.println("--- ADMIN CLIENT SHUTTING DOWN---");
            shutdownConnection();
            socket.close();

        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    //=========================================
    private void runScript(String file) {
        int linenum = 0;
        String line = new String("");
        try {
            BufferedReader br = new BufferedReader( new FileReader(file));
            while( (line = br.readLine()) != null) {

                String[] toks = line.split(" ");
                if( toks[0].equals("new-section")) {
                    if(!processNewSection(toks)) throw new Exception();
                }
                else if( toks[0].equals("new-split-core")) {
                    if(!processNewSplitCore(toks))throw new Exception();
                }
                else if( toks[0].equals("new-whole-core")) {
                    if(!processNewWholeCore(toks))throw new Exception();
                }
                else if( toks[0].equals("logout")) {
                    System.exit(1);
                }
                else if( toks[0].equals("server-shutdown")) {
                    dos.writeByte( ClientRequest.SHUTDOWN_SERVER);
                    dos.flush();
                    System.out.println("NOTIFIED SERVER TO SHUTDOWN..PLEASE "
                                       + " WAIT");
                    System.out.flush();
                    /// FIXME sleep(1000);
                    System.exit(1);
                }
                else if( toks[0].equals("list-sections")) {
                    if(!processListSections()) throw new Exception();
                }
                else if( toks[0].equals("list-split-cores")) {
                    if(!processListSplitCores(toks))throw new Exception();
                }
                else if( toks[0].equals("list-whole-cores")) {
                    if(!processListWholeCores(toks))throw new Exception();
                }
                else if( toks[0].equals("list-datasets")) {
                    if(!processListDatasets()) throw new Exception();
                }
                else if( toks[0].equals("list-tables")) {
                    if(!processListTables(toks)) throw new Exception();
                }
                else if( toks[0].equals("list-table-data")) {
                    if(!processListTableData(toks)) throw new Exception();
                }
                else if( toks[0].equals("new-dataset")) {
                    if(!processNewDataset(toks))throw new Exception();
                }
                else if( toks[0].equals("run-backup")) {
                    dos.writeByte( ClientRequest.RUN_BACKUP );
                    dos.flush();
                }
                else if( toks[0].equals("run-script")) {
                    if( toks.length < 2 ) {
                        System.out.println("Please give a script file");
                    }
                    else{
                        runScript(toks[1]);
                    }
                }
                else if( toks[0].equals("help")) {
                    System.out.println("Commands list: ---------");
                    System.out.println("new-section");
                    System.out.println("new-split-core");
                    System.out.println("new-whole-core");
                    System.out.println("new-dataset");
                    System.out.println("list-sections");
                    System.out.println("list-datasets");
                    System.out.println("list-tables");
                    System.out.println("list-table-data");
                    System.out.println("run-backup");
                    System.out.println("run-script");
                    System.out.println("logout");
                    System.out.println("server-shutdown");
                    System.out.println("new-missing-section");
                    System.out.println("new-missing-split-core");

                }
                else if( toks[0].equals("new-missing-section") ) {
                    processMissingSection(toks);
                }
                else if( toks[0].equals("new-missing-split-core") ) {
                    processMissingSplitCore(toks);
                }
                else {
                    System.out.println(toks[0] + " is an unknown command.");
                    System.out.println("Type 'help' for list of commands.");
                }
            }

            linenum++;

        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Error in file: " + file + " on line number "
                               + linenum + ": \n" + line );
        }

    }

    //==========================================
    private boolean login() {
        System.out.println("--- Attempting to login as [" + username + "] ---");

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
                    int id = dis.readInt();
                    if (id == 0)
                        return true;

                    dos.writeByte(ClientRequest.LOGOUT);
                    dos.flush();
                    socket.close();
                    System.out.println("FAILED TO LOGIN AS ADMIN");
                    System.exit(1);
                case ServerResponse.USER_PASS_INVALID:
                    System.out.println("INVALID NAME/PASSWORD");
                    socket.close();
                    System.exit(1);
                case ServerResponse.USER_ALREADY_LOGGED_IN:
                    System.out.println("ADMIN IS ALREADY LOGGED IN!!!");
                    socket.close();
                    System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    //==========================================
    private byte registerName(String s) {
        try {
            byte[] utf8chars = s.getBytes("UTF-8");
            dos.writeByte( ClientRequest.REGISTER_USER );
            dos.writeInt( utf8chars.length );
            dos.write( utf8chars, 0, utf8chars.length );
            dos.flush();
            
            if( !waitForResponse() ) 
                return ServerResponse.SERVER_UNAVAILABLE;

            return dis.readByte();
        } catch (Exception e) {
            return ServerResponse.SERVER_UNAVAILABLE;
        }
    }

    //==========================================
    private void shutdownConnection() {
        try{
            dos.writeByte( ClientRequest.LOGOUT );
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //==========================================
    private boolean waitForResponse() {
        try {
            while( dis.available() <= 0 );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //==========================================
    public void beginShutdown() {
        closeConnection = true;
    }

    //======================================================
    private boolean processNewSection(String[] toks) {
        if( toks.length < 4 )
        {
            System.out.println("Error: Not enough arguments.");
            System.out.println("Usage: new-section <section name> " +
                               "<depth (mbsf) > <length (m) >");
            System.out.println("Please try again.");
            return false;
        }

        float depth, length;

        try {
            depth = Float.parseFloat(toks[2]);
        }
        catch( Exception e) {
            System.out.println("Error: Expected real number, received " +
                               toks[2]);
            System.out.println("Usage: new-section <section name> " +
                               "<depth (mbsf) > <length (m) >");
            System.out.println("Please try again.");
            return false;
        }

        try {
            length = Float.parseFloat(toks[3]);
        }
        catch( Exception e) {
            System.out.println("Error: Expected real number, received " +
                               toks[3]);
            System.out.println("Usage: new-section <section name> " +
                               "<depth (mbsf) > <length (m) >");
            System.out.println("Please try again.");
            return false;
        }

        System.out.println("Notifying server of existence of new " +
                           "core section: " + toks[1] );
        
        byte[] buf;
        try {
            buf = toks[1].getBytes("UTF-8");
        }
        catch( Exception e) {
            System.out.println("Error: Unable to convert section name to "
                               + " UTF-8 encoding.");
            e.printStackTrace();
            return false;
        }
                
        try {
            dos.writeByte( ClientRequest.NEW_CORE_SECTION );
            dos.writeFloat(depth);
            dos.writeFloat(length);
            dos.writeInt( buf.length );
            dos.write( buf, 0, buf.length );
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't send message");
            return false;
        }

        System.out.print("Waiting for server response: " );
        
        try {
            byte ret = dis.readByte();
            
            if( ret  == ServerResponse.SECTION_RECEIVED )
                System.out.println("SUCCESS");
            else
                System.out.println("FAILED .. please check server print-out");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't get a response!" +
                               " Please check server.");
        }

        return true;
    }

    //=========================================
    // MODIFICATION: need to pass depth info in, it can be used whn
    //               creating a new section is needed
    // Usage: new-split-core <image url> <section name>
    //                       <start depth> <end depth> <dpi_x> [<dpi_y>]
    //
    private boolean processNewSplitCore(String[] toks) {
        if( toks.length < 6 )
        {
            System.out.println("Error: Not enough arguments.");
            System.out.println("Usage: new-split-core <image url> " +
                               " <section name> <start depth> <end depth>" +
                               " <image dpi_x> [<image dpi_y>]");
            System.out.println("Please try again.");
            return false;
        }

        float dpi_x, dpi_y;
        float start_depth, end_depth, length;

        try {
            start_depth = Float.parseFloat(toks[3]);
            end_depth   = Float.parseFloat(toks[4]);
            length = end_depth - start_depth;

            dpi_x = Float.parseFloat(toks[5]);
            dpi_y = (toks.length == 7) ? Float.parseFloat(toks[6]) : dpi_x;
        } catch( Exception e) {
            System.out.println("Error: Expected real number, received " +
                               toks[2]);
            System.out.println("Usage: new-split-core <image url> " +
                               " <section name> <start depth> <end depth>" +
                               " <dpi_x> [<dpi_y>]");
            System.out.println("Please try again.");
            return false;
        }

        String splitCoreSectionName = toks[2];
        byte[] name;
        byte[] url;

        try {
            name = splitCoreSectionName.getBytes("UTF-8");
        }
        catch( Exception e) {
            System.out.println("Error: Unable to convert section name to "
                               + " UTF-8 encoding.");
            e.printStackTrace();
            return false;
        }

        try {
            new URL( toks[1] );
            url = toks[1].getBytes("UTF-8");
        } catch ( MalformedURLException e) {
            System.out.println( toks[1] + " is a malformed URL.");
            System.out.println("Usage: new-split-core <image url> " +
                               " <section name> <start_depth> <end_depth>" +
                               " <dpi_x> [<dpi_y>]");
            System.out.println("Please try again.");
            return false;
        } catch ( Exception e) {
            System.out.println("Error: Unable to convert section url to "
                               + " UTF-8 encoding.");
            e.printStackTrace();
            return false;
        }
        
        // TODO check whether the "section" label exists
        //---- query whether the section name already exist
        System.out.println("Query server if section " + name +
                           " already exists.");

        try {
            dos.writeByte( ClientRequest.HAS_SECTION );
            dos.writeInt( name.length );
            dos.write( name, 0, name.length );
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't send message");
            return false;
        }

        System.out.println("Waiting for server response" );

        try {
            byte ret = dis.readByte();

            if( ret  == ServerResponse.HAVE_NO_SECTION ) {
                System.out.println("Server has no such section");
                System.out.println("Will create one for this split core");

                String [] newSecToks = { "new-section", splitCoreSectionName,
                                         toks[3], String.valueOf(length) };

                System.out.println("Creating a new section for splitcore: " +
                                   splitCoreSectionName);
                this.processNewSection(newSecToks);
            } else if( ret == ServerResponse.HAVE_SECTION ) {
                System.out.println("Server already has such section, continue");
            }
            else
                System.out.println("FAILED .. please check server print-out");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't get a response!" +
                               " Please check server.");
        }

        //---------------------------------------------------------------------

        System.out.println("Notifying server of existence of new " +
                           "split-core section: " + toks[1] );

        try {
            dos.writeByte( ClientRequest.NEW_SPLIT_CORE );
            dos.writeFloat(dpi_x);
            dos.writeFloat(dpi_y);
            dos.writeInt( name.length );
            dos.write( name, 0, name.length );
            dos.writeInt( url.length );
            dos.write( url, 0, url.length );
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't send message");
            return false;
        }

        System.out.print("Waiting for server response: " );
        
        try {
            byte ret = dis.readByte();
            
            if( ret  == ServerResponse.SPLIT_CORE_RECEIVED )
                System.out.println("SUCCESS");
            else
                System.out.println("FAILED .. please check server print-out");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't get a response!" +
                               " Please check server.");
        }

        return true;
        
    }

    //=========================================
    // old:
    // new-whole-core <image url>  <section name> <image dpi>
    // new:
    // new-whole-core <image url> <given name>
    //                <start depth> <end depth> <dpi>
    private boolean processNewWholeCore(String[] toks) {
        if( toks.length < 6 )
        {
            System.out.println("Error: Not enough arguments.");
            System.out.println("Usage: new-whole-core <image url>" +
                               " <section name> <start depth> <end depth>" +
                               " <image dpi>");
            System.out.println("Please try again.");
            return false;
        }

        // section name
        String wholeCoreSectionName = toks[2] + "-" + toks[3] + "-whole";
        byte[] name;

        try {
            name = toks[2].getBytes("UTF-8");
        } catch( Exception e) {
            System.out.println("Error: Unable to convert section name to "
                               + " UTF-8 encoding.");
            e.printStackTrace();
            return false;
        }

        // Image URL
        byte[] url;

        try {
            new URL( toks[1] );
            url = toks[1].getBytes("UTF-8");
        } catch ( MalformedURLException e) {
            System.out.println( toks[1] + " is a malformed URL.");
            System.out.println("Usage: new-whole-core <image url>" +
                               " <section name> <start depth> <end depth>" +
                               " <image dpi>");
            System.out.println("Please try again.");
            return false;
        } catch ( Exception e) {
            System.out.println("Error: Unable to convert section url to "
                               + " UTF-8 encoding.");
            e.printStackTrace();
            return false;
        }

        // start_depth, end_depth, dpi
        float dpi, start_depth, end_depth, length;

        try {
            start_depth = Float.parseFloat(toks[3]);
        }
        catch( Exception e) {
            System.out.println("Error: Expected real number, received " +
                               toks[3]);
            System.out.println("Usage: new-whole-core <image url>" +
                               " <section name> <start depth> <end depth>" +
                               " <image dpi>");
            System.out.println("Please try again.");
            return false;
        }

        try {
            end_depth = Float.parseFloat(toks[4]);
        }
        catch( Exception e) {
            System.out.println("Error: Expected real number, received " +
                               toks[4]);
            System.out.println("Usage: new-whole-core <image url>" +
                               " <section name> <start depth> <end depth>" +
                               " <image dpi>");
            System.out.println("Please try again.");
            return false;
        }

        try {
            dpi = Float.parseFloat(toks[5]);
        }
        catch( Exception e) {
            System.out.println("Error: Expected real number, received " +
                               toks[5]);
            System.out.println("Usage: new-whole-core <image url>" +
                               " <section name> <start depth> <end depth>" +
                               " <image dpi>");
            System.out.println("Please try again.");
            return false;
        }

        length = end_depth - start_depth;

        //---- query whether the section name already exist
        System.out.println("Query server if section " + name +
                           " already exists.");

        try {
            dos.writeByte( ClientRequest.HAS_SECTION );
            dos.writeInt( name.length );
            dos.write( name, 0, name.length );
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't send message");
            return false;
        }

        System.out.print("Waiting for server response: " );

        try {
            byte ret = dis.readByte();

            if( ret  == ServerResponse.HAVE_NO_SECTION ) {
                System.out.println("Server has no such section");
                System.out.println("Will create one for this whole core");

                // create a new section for this whole core

                try {
                    name = wholeCoreSectionName.getBytes("UTF-8");
                }
                catch( Exception e) {
                    System.out.println("Error: Unable to convert whole core " +
                                       "section name to UTF-8 encoding.");
                    e.printStackTrace();
                    return false;
                }

                String [] newSecToks = { "new-section", wholeCoreSectionName,
                                         toks[3], String.valueOf(length) };

                this.processNewSection(newSecToks);

            } else if( ret == ServerResponse.HAVE_SECTION ) {
                System.out.println("Server already has such section, continue");
            }
            else
                System.out.println("FAILED .. please check server print-out");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't get a response!" +
                               " Please check server.");
        }

        //---------------------------------------------------------------------

        System.out.println("Notifying server of existence of new " +
                           "whole-core section: " + toks[1] );

        try {
            dos.writeByte( ClientRequest.NEW_WHOLE_CORE );
            dos.writeFloat(dpi);
            dos.writeInt( name.length );
            dos.write( name, 0, name.length );
            dos.writeInt( url.length );
            dos.write( url, 0, url.length );

            dos.writeFloat(start_depth);
            dos.writeFloat(length);
            
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't send message");
            return false;
        }

        System.out.print("Waiting for server response: " );
        
        try {
            byte ret = dis.readByte();
            
            if( ret  == ServerResponse.WHOLE_CORE_RECEIVED )
                System.out.println("SUCCESS");
            else
                System.out.println("FAILED .. please check server print-out");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR!! Couldn't get a response!" +
                               " Please check server.");
        }

        return true;

    }

    //=========================================
    private boolean processListSplitCores(String[] toks) {
        try {
            dos.writeByte( ClientRequest.LIST_SPLIT_CORES );
            dos.flush();

            while( dis.readByte() != ServerResponse.SPLIT_CORE_LIST_DONE ) {

                int    len;
                byte[] buf;
                String name, url;
                float  depth, length, dpi;
                int    globalId;

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

                System.out.println(globalId + ": " + name + ", " + depth + 
                                   " (mbsf), " + length + " (m), " + dpi + 
                                   " (DPI), " + url );  
                url = null;
                buf = null;
                name = null;

                System.gc();
            }

        } catch (Exception e) {
            System.out.print("UNKNOWN ERROR: ");
            e.printStackTrace();
            System.exit(1);
        }

        return true;
    }

    //=========================================
    private boolean processListWholeCores(String[] toks) {
        try {
            dos.writeByte( ClientRequest.LIST_WHOLE_CORES );
            dos.flush();

            while( dis.readByte() != ServerResponse.WHOLE_CORE_LIST_DONE ) {

                int    len;
                byte[] buf;
                String name, url;
                float  depth, length, dpi;
                int    globalId;


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

                System.out.println(globalId + ": " + name + ", " + depth + 
                                   " (mbsf), " + length + " (m), " + dpi + 
                                   " (DPI), " + url );  

                url = null;
                buf = null;
                name = null;
                System.gc();
            }

        } catch (Exception e) {
            System.out.print("UNKNOWN ERROR: ");
            e.printStackTrace();
            System.exit(1);
        }

        return true;
    }

    //========================================
    private boolean processListSections() {
        try {

            sectionVec.clear();

            dos.writeByte(ClientRequest.LIST_SECTIONS);
            dos.flush();

            byte response;
            while( (response = dis.readByte()) != 
                   ServerResponse.SECTION_LIST_DONE) {
                int len;
                byte[] buf;
                String name;
                float depth, length;
                int gid;
                
                gid = dis.readInt();
                depth = dis.readFloat();
                length = dis.readFloat();
                len = dis.readInt();
                buf = new byte[len];
                // dis.read( buf, 0, len);
                dis.readFully(buf);
                name = new String(buf,"UTF-8");

                SessionSection ss;
                ss = new SessionSection( name, depth, length );

                sectionVec.add( ss );

                ss = null;
                name = null;
                buf = null;
            }


            // sort the sections according the depth
            sarray = new SessionSection[sectionVec.size()];

            int i;
            for( i = 0; i < sectionVec.size(); i++) {
                sarray[i] = sectionVec.elementAt(i);
            }

            Arrays.sort( sarray );

            for( i = 0; i < sarray.length; i++) {
                System.out.println("" + i + ": " + sarray[i].name
                                   + " @ " + sarray[i].depth +
                                   " for " + sarray[i].length );
            }

        } catch( Exception e) {
            e.printStackTrace();
            System.out.println("FAILED TO GET SECTION LISTING!!");
            System.out.println("PLEASE RETRY!");
            System.exit(1);
        }


        return true;

    }

    //=========================================
    private boolean processListDatasets() {
        try {
            dos.writeByte(ClientRequest.LIST_DATASETS);
            dos.flush();
            if( dis.readByte() != ServerResponse.DATASET_LIST )
            {
                System.out.println("ERROR getting dataset list");
                return false;
            }
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
                System.out.println("Dataset: " + sdname + " with fields: ");
                
                fcount = dis.readInt();
                for( int l = 0; l < fcount; l++) {
                    len = dis.readInt();
                    buf = new byte[len];
                    // dis.read(buf,0,len);
                    dis.readFully(buf);
                    sdname = new String(buf,"UTF-8");
                    System.out.print("\t\t" + sdname);
                    float min,max;
                    min = dis.readFloat();
                    max = dis.readFloat();
                    System.out.println(" value range [ " + min + ", " + max 
                                       + " ]");
                    buf = null;
                }
                
                sdname = null;
                buf = null;

                System.out.println("");
                System.gc();
            }

        }
        catch (Exception e) {
            System.out.println("ERROR: ");
            e.printStackTrace();
            System.exit(1);
        }

        return true;
    }

    //=========================================
    private boolean processListTables(String[] toks) {
        try {
            if( toks.length < 2 ) {
                System.out.println("Not enough arguments");
                System.out.println("list-tables <dataset name>");
                return false;
            }

            byte[] buf;
            buf = toks[1].getBytes("UTF-8");

            dos.writeByte(ClientRequest.LIST_TABLES);
            dos.writeInt( buf.length );
            dos.write(buf, 0, buf.length);
            dos.flush();

            if( dis.readByte() != ServerResponse.TABLE_LIST ) {
                System.out.println(toks[1] + " not recognized as a dataset");
                return true;
            }

            int len = dis.readInt();
            buf = new byte[len];
            // dis.read( buf, 0, len );
            dis.readFully(buf);
            int ntables = dis.readInt();
            System.out.println("");

            for( int i = 0; i < ntables; i++) {
                len = dis.readInt();
                buf = new byte[len];
                String tablename;
                // dis.read( buf, 0, len);
                dis.readFully(buf);
                tablename = new String( buf, "UTF-8");
                System.out.println("Table name: " + tablename);
                tablename = null;
                System.gc();
            }

        } catch( Exception e) {
            System.out.println("ERROR:");
            e.printStackTrace();
            System.exit(1);
        }

        return true;
    }

    //=========================================
    private boolean processListTableData(String[] toks) {
        if( toks.length < 3 ) {
            System.out.println("Not enough arguments.");
            System.out.println("list-table-data <set name> <table name>");
            return false;
        }

        try {
            byte[] sbuf = toks[1].getBytes("UTF-8");
            byte[] tbuf = toks[2].getBytes("UTF-8");

            dos.writeByte( ClientRequest.TABLE_DATA );
            dos.writeInt( sbuf.length );
            dos.write(sbuf,0,sbuf.length);
            dos.writeInt( tbuf.length );
            dos.write(tbuf,0,tbuf.length);
            dos.flush();

            if( dis.readByte() == ServerResponse.TABLE_DATA_ERR )
            {
                System.out.println("Either set: " + toks[1] + " or table " +
                                   toks[2] + " don't exist.");
                return true; 
            }
            
            int len;
            len = dis.readInt();
            sbuf = new byte[len];
            // dis.read( sbuf, 0, len);
            dis.readFully(sbuf);
            len = dis.readInt();
            tbuf = new byte[len];
            // dis.read( tbuf, 0, len);
            dis.readFully(tbuf);

            String setname, tablename;
            setname = new String(sbuf,"UTF-8");
            tablename = new String(tbuf,"UTF-8");

            System.out.println("Set: " + setname + " Table: " + tablename);
           
            setname = null;
            tablename = null;
 
            int nfields = dis.readInt();
            int nrows   = dis.readInt();


            System.out.println("Expecting " + nrows + " rows with " + nfields
                               + " values each.");
            for( int r = 0; r < nrows; r++) {
                for( int f = 0; f < nfields; f++) {
                    boolean b;
                    float   v;
                    b = dis.readBoolean();
                    v = dis.readFloat();
                    if( b ) {
                        System.out.print("" + v + "\t");
                    }
                    else
                    {
                        System.out.print("N/A\t");
                    }
                }
                System.out.println("");
            }
            
            
            System.gc();

        } catch (Exception e) {
            System.out.println("ERROR:");
            e.printStackTrace();
            System.exit(1);
        }

        return true;
    }

    //=========================================
    private boolean processNewDataset(String[] toks) {
        if( toks.length < 3 ) {
            System.out.println("Not enough arguments.");
            System.out.println("Usage: new-dataset <dataset file> " +
                               " <new dataset name>");
            System.out.println("NOTE: Datafile should be tab delimited " +
                               "with first row as headers, and depth on " +
                               " the first column.  Blank entries assumed " +
                               " to be invalid data. ");
            return false;
        }

        File f = new File(toks[1]);
        if( !f.exists() ) {
            System.out.println("Datafile " + toks[1] + " does not exist.");
            return false;
        }

        if( !processListSections() )
            return false;

        int i;
        for( i = 0; i < sectionVec.size(); i++) {
            System.out.println("" + i + ": " + sectionVec.elementAt(i).name
                               + " @ " + sectionVec.elementAt(i).depth +
                               " for " + sectionVec.elementAt(i).length);
        }

        String setname = toks[2];
        SessionDataset sd = new SessionDataset(setname);
        String[] headers = new String[0];

        try {

            BufferedReader br = new BufferedReader( new FileReader(f));
            String line = br.readLine();
            String[] kens = line.split("\t");
            if(!kens[0].toLowerCase().contains("depth")) {
                System.out.println("First column not depth!! Fix datafile.");
                return false;
            }

            // determine headers
            headers = kens;

            SessionTable t = null;
            int currentSectionID = 0;
            // int k = 0; // corelyzer.plugin.andrill.SessionSection array sarray[] index
            // boolean procfile = true;

            // process each row of data in the file if possible
            //-- while( (line = br.readLine()) != null && procfile ) {
            while( (line = br.readLine()) != null ) {
                kens = line.split("\t");

                // init value[] & valid[]
                float[] value = new float[headers.length];
                boolean[] valid = new boolean[headers.length];

                for( i = 0; i < valid.length; i++) {
                    valid[i] = false;
                }

                // fill in value[] & valid[] is there's a value
                for( i = 0; i < kens.length; i++) {
                    if( kens[i].equals("") )
                        continue;
                    value[i] = Float.parseFloat(kens[i]);
                    valid[i] = true;
                }

                //-------------------------------------------------------------

                // Comparing row's depth value to all sections(starting from
                // index currentSectionID) ranges to
                // determine which table should this row belongs to
                boolean found = false;
                for(int k = currentSectionID; k<sarray.length && (!found); k++)
                {

                    if( value[0] >= sarray[k].depth &&
                        value[0] <= sarray[k].depth + sarray[k].length)
                    {
                        if ( t == null ) {
                            System.out.println("Create new corelyzer.plugin.andrill.SessionTable: " +
                                               sarray[k].name);

                            t = new SessionTable( sarray[k].name );
                            t.section = k;
                        }

                        t.values.add( value );
                        t.valids.add( valid );

                        currentSectionID = k;
                        found = true;
                    } else {
                        if( t != null ) {
                            sd.tables.add(t);
                            t = null;
                        }
                    }
                }

                /*
                if( value[0] >= sarray[k].depth &&
                    value[0] <= sarray[k].depth + sarray[k].length) {

                    if( t == null ) {
                        System.out.println("corelyzer.plugin.andrill.SessionTable: " + sarray[k].name);
                        t = new corelyzer.plugin.andrill.SessionTable( sarray[k].name );
                        t.section = k;
                    }

                    t.values.add( value );
                    t.valids.add( valid );
                }
                else {
                    // need to find next section
                    if( t != null )
                        sd.tables.add(t);

                    boolean secfound = false;
                    while( !secfound && k < sarray.length - 1) {
                        k++;
                        if( value[0] >= sarray[k].depth &&
                            value[0] <= sarray[k].depth + sarray[k].length) {
                            secfound = true;
                        }
                    }

                    if( secfound == false)
                        procfile = false;
                    else
                    {
                        System.out.println("corelyzer.plugin.andrill.SessionTable: " + sarray[k].name);
                        t = new corelyzer.plugin.andrill.SessionTable( sarray[k].name );
                        t.section = k;
                        t.values.add( value);
                        t.valids.add( valid);
                    }
                }
                */
            } // end of while loop through lines/rows

            if( t != null ) {
                sd.tables.add(t);
            }
        } catch( Exception e) {
            System.out.println("FAILED. File may not be readable, or no"
                + " sections may exist.  Please check file permissions and "
                + " try to run list-sections." );
            System.gc();
            System.exit(1);
        }

        //---------------------------------------------------------------------

        // send the new dataset to the server as a set of datasets
        // one per sessiontable
        try {

            SessionTable[] tables = new SessionTable[ sd.tables.size() ];
            dos.writeByte( ClientRequest.NEW_DATASET);
            byte[] buf = sd.name.getBytes("UTF-8");
            dos.writeInt( buf.length );
            dos.write(buf,0,buf.length);
            dos.writeInt( headers.length );

            for( int l = 0; l < headers.length; l++) {
                buf = headers[l].getBytes("UTF-8");
                dos.writeInt( buf.length );
                dos.write( buf, 0, buf.length );
            }
            dos.flush();

            System.out.println("Referencing against " + sd.tables.size() +
                               " tables!");

            for( int k = 0; k < sd.tables.size(); k++) {
                System.out.print(".");
                tables[k] = sd.tables.elementAt(k);
                String tablename = sarray[ tables[k].section ].name;

                buf = tablename.getBytes("UTF-8");
                dos.writeByte( ClientRequest.NEW_TABLE );
                dos.writeInt( sarray[ tables[k].section ].globalId );
                dos.writeInt( buf.length );
                dos.write( buf, 0, buf.length );

                for( i = 0; i < tables[k].valids.size(); i++) {

                    dos.writeByte( ClientRequest.NEW_ROW );
                    float[] value = tables[k].values.elementAt(i);
                    boolean[] valid = tables[k].valids.elementAt(i);

                    value[0] -= sarray[ tables[k].section ].depth;

                    for( int l = 0; l < headers.length; l++) {
                        dos.writeFloat( value[l] );
                        dos.writeBoolean( valid[l] );
                    }
                }

                dos.writeByte( ClientRequest.END_TABLE );
                dos.flush();
            }

            dos.writeByte(ClientRequest.END_DATASET);
            dos.flush();

            if( dis.readByte() == ServerResponse.NEW_DATASET_AVAILABLE ){
                System.out.println("SUCCESS");
            }
            else
            {
                System.out.println("ERROR OCCURRED. Check Server Output");
            }

        } catch ( Exception e ) {
            e.printStackTrace();
            System.out.println("FAILED to send whole dataset over to server!");
            System.out.println("Please try again, old data will be" +
                               " overwritten.");
            System.exit(1);
        }

        System.gc();
        return true;
    }

    //=========================================

    /**
     * for missing cores... or cores that can't be split, in order to still
     * see the data.
     * section naming convention: <prefix>_<start depth>-<end depth>-missing.
     *    - should take care of depth data to section data mapping...
     * example admin command: new-missing-section and001 0.0 1.5
     */
    private boolean processMissingSection(String[] toks) {
        if( toks.length < 4 )
        {
            System.out.println("Error: Not enough arguments.");
            System.out.println("Usage: new-missing-section <section prefix> " +
                               "<depth (mbsf) > <length (m) >");
            System.out.println("Example: new-missing-section and001 0.0 1.5");
            System.out.println("will new \"and001_0.0-1.5-missing\" section");
            System.out.println("Please try again.");
            return false;
        }
        else
        {
            toks[1] = toks[1] + "_" + toks[2] + "-" + toks[3] + "-missing";
            return processNewSection(toks);
        }
    }

    //=========================================

    /**
     * need to modify new split and whole core image entries so that
     * horizontal and vertical DPI can be different. in case the split core
     * image doesn't exist but data still does and you want to see the logs!
     *
     * 1. make sure a 1x1 pixel image named blah.jpg stored at the root web
     *    directory
     *    e.g. /var/www/blah.jpg --> http://localhost/blah.jpg
     *
     * 2. perform command: new-missing-split-core <missing section name>
     *
     * example admin command:
     *    new-missing-split-core and001_186.7-187.5
     */
    private boolean processMissingSplitCore(String[] toks) {
        if( toks.length < 2 )
        {
            System.out.println("Error: Not enough arguments.");
            System.out.println("Usage: new-missing-split-core " +
                               "<missing section name>");
            System.out.println("Missing section name convention example: " +
                               "and001_186.7-187.5, length is meters");
            System.out.println("Please try again.");
            return false;
        }
        else
        {
            String [] lengths   = toks[1].split("_");
            String [] start_end = lengths[1].split("-");
            float length = Float.valueOf(start_end[1]) -
                           Float.valueOf(start_end[0]);

            // Manipulate dpi, making 1 pixel equals to the length of missing
            // splitcore:
            // dpix = 1.0f / (length * 100.0f / 2.54f);
            // dpiy = 1.0 pixel / 4 inches = 0.25
            float dpi_x = 0.0254f / length;
            float dpi_y = 0.25f;

            String sectionName = toks[1];
            // String missingSectionName = toks[1] + "-missing";
            byte [] name;

            try {
                name = sectionName.getBytes("UTF-8");
            } catch( Exception e) {
                System.out.println("Error: Unable to convert section name to "
                                   + " UTF-8 encoding.");
                e.printStackTrace();
                return false;
            }

            //---- query whether the section name already exist
            System.out.println("Query server if section " + name +
                               " already exists.");

            try {
                dos.writeByte( ClientRequest.HAS_SECTION );
                dos.writeInt( name.length );
                dos.write( name, 0, name.length );
                dos.flush();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ERROR!! Couldn't send message");
                return false;
            }

            System.out.print("Waiting for server response: " );


            try {
                byte ret = dis.readByte();

                if( ret  == ServerResponse.HAVE_NO_SECTION ) {
                    System.out.println("Server has no such section");
                    System.out.println("Will create one for this whole core");

                    sectionName = sectionName + "-missing";

                    // create a new section for this missing splitcore
                    try {
                        name = sectionName.getBytes("UTF-8");
                    }
                    catch( Exception e) {
                        System.out.println("Error: Unable to convert whole core"
                                           +" section name to UTF-8 encoding.");
                        e.printStackTrace();
                        return false;
                    }

                    String [] newSecToks = { "new-section", sectionName,
                                             String.valueOf(start_end[0]),
                                             String.valueOf(length) };

                    this.processNewSection(newSecToks);

                } else if( ret == ServerResponse.HAVE_SECTION ) {
                    System.out.println("Server already has such section, " +
                                       "continue");
                }
                else
                    System.out.println("FAILED .. please check server print-out");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ERROR!! Couldn't get a response!" +
                                   " Please check server.");
            }
            //----

            String [] new_toks = {"new-split-core",
                                  singlePixelImageUrl,
                                  sectionName,
                                  String.valueOf(dpi_x),
                                  String.valueOf(dpi_y)
                                 };

            return processNewSplitCore(new_toks);
        }
    }

    //==========================================================================

    private boolean getConnected() {
        try {
            closeConnection = false;
            socket = new Socket(server,11999);
            dos = new DataOutputStream( socket.getOutputStream() );
            dis = new DataInputStream( socket.getInputStream() );

            if( !socket.isConnected() )
                return false;

            System.out.println("--- CLIENT CONNECTED ---");

            closeConnection = true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public int processBatchRequest() {
        System.out.println("--- Connecting to server: [" + server + "] ---");

        if( !getConnected() ) {
            return CANNOT_CONNECT;
        }

        if( !login()) {
            return CANNOT_LOGIN;
        }

        System.out.println("Processing...");
        System.out.println("Go through the summary report file: ["
                            + this.input_file + "]");

        int parseResult = paseLIMSSummaryReport();

        // Disconnect and leave gracefully
        System.out.println("--- Logout ---");
        shutdownConnection();

        return SUCCESS;
    }

    private int parseImageReport() {
        File f = new File(this.input_file);

        if( !f.exists() ) {
            System.out.println("Input file: " + this.input_file +
                               " does not exist.");
            return PARSE_IMG_ERROR;
        }

        try {
            BufferedReader br = new BufferedReader( new FileReader(f) );

            String line;
            String [] toks;

            while( ( line = br.readLine() )  != null ) {
                toks = line.split("\t");

                if(toks.length < 5) {
                    System.err.println("Malform line: skip");
                    continue;
                }

                String coreType = toks[0];

                String strURL = toks[1];
                String beginDepth = toks[2];
                String endDepth   = toks[3];
                String dpi_x      = toks[4];
                String dpi_y = (toks.length == 5) ? dpi_x : toks[5];

                if( coreType.contains("split") ) { // FIXME
                    String label = coreType + "_" + beginDepth + "-" + endDepth;

                    String [] tks = { "new-split-core", strURL, label,
                                       beginDepth, endDepth, dpi_x, dpi_y};

                    processNewSplitCore(tks);
                } else if( coreType.contains("whole") ) {  // FIXME
                    String label = coreType + "_" + beginDepth + "-" + endDepth;

                    String [] tks = { "new-whole-core", strURL, label,
                                      beginDepth, endDepth, dpi_x};

                    processNewWholeCore(tks);
                } else {
                    ;
                    //System.err.println("Unknown core type: ignore");
                }
            }

        } catch (Exception exp) {
            System.err.println("Exception: in parseImageReport()");
            System.gc();
            return PARSE_IMG_ERROR;
        }

        return PARSE_IMG_SUCCESS;
    }

    private int parseDataReport() {
        int retCode;
        String absFilePath = (new File(this.input_file)).getAbsolutePath();

        String datasetLabel = (new File(this.input_file)).getName();
        if( datasetLabel.equals("") ) {
            datasetLabel = "andrill_2006";
        }

        String [] toks = { "new-datset", absFilePath, datasetLabel};

        if(processNewDataset(toks)) {
            retCode = PARSE_DATA_SUCCESS;
        } else {
            retCode = PARSE_DATA_ERROR;
        }

        return retCode;
    }

    private int paseLIMSSummaryReport() {
        int retCode;

        if(this.mode) { // true:  data
            System.out.println("Parsing data summary");
            retCode = parseDataReport();
        } else {        // false: image
            System.out.println("Parsing image summary");
            retCode = parseImageReport();
        }

        return retCode;
    }

    //==========================================================================
    public static void main(String[] args) {

        if(args.length == 5) {
            CorelyzerSessionBatchAdminClient c =
                    new CorelyzerSessionBatchAdminClient(args);

            int isDone = c.processBatchRequest();

            switch(isDone) {
                case 0:
                    System.out.println("Success!");
                    break;

                case 1:
                    System.out.println("Cannot connect to server");
                    break;

                case 2:
                    System.out.println("Cannot login to server");
                    break;

                default:
                    System.out.println("Undefined result! Code: " + isDone);
            }

        } else {
            System.out.println("Usage: java corelyzer.plugin.andrill.CorelyzerSessionBatchAdminClient " +
                    "<server_ip> <username> <password> <image|data> " +
                    "<summary_report>");
        }

    }

}
