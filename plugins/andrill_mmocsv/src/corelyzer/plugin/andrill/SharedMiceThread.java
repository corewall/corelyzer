package corelyzer.plugin.andrill;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;


public class SharedMiceThread extends Thread {
    private DatagramSocket mouseSocket;
    private boolean keepRunning;
    private Date date;
    private CorelyzerSessionServer server;
    private static final int BUF_SIZE = 20;
    //===================================
    public SharedMiceThread(int port) {
        try {
            mouseSocket = new DatagramSocket(port);
            keepRunning = true;
            date = new Date();
            server = CorelyzerSessionServer.getServer();
        }
        catch( Exception e) {
            keepRunning = false;
        }
    }

    //===================================
    public void run() {
        byte[] outbuf = new byte[BUF_SIZE];
        
        byte[] recvbuf = new byte[ BUF_SIZE ];
        ByteArrayInputStream is = new ByteArrayInputStream(recvbuf);
        DataInputStream dis = new DataInputStream(is);
        DatagramPacket recvpkt = new DatagramPacket( recvbuf, BUF_SIZE );

        try {
            mouseSocket.setSoTimeout(5000);
        }
        catch( Exception e ) {
            System.out.println("Couldn't set So Timeout on Mouse Thread\n");
            keepRunning = false;
        }

        while( keepRunning ) {
            try {
                // blocking wait for some input
                mouseSocket.receive(recvpkt);

                // get the user id, x & y pos and index stamp
                int usrid;
                float xpos, ypos;
                long time;
            
                is.reset();
                usrid = dis.readInt();
                xpos = dis.readFloat();
                ypos = dis.readFloat();
                time = dis.readLong();

                long curtime = System.currentTimeMillis();

                // iterate trough client linked list
                for( int i = 0; i < server.getNumClients(); i++) {
                    
                    // check last update time, if more than 1 minute 
                    // then notify 
                    // ClientConnectionThread that user is not connected
                    // Otherwise, send out packet to IP address
                    ClientConnectionThread cct;
                    ClientData cd;

                    cct = server.getConnectionThread(i);
                    cd = cct.getClientData();

                    if( cd.getUserID() == 0) continue;

                    DatagramPacket outpkt = new DatagramPacket( 
                        recvbuf, BUF_SIZE, cct.getClientIP(), 11997);
                    
                    mouseSocket.send(outpkt);
                }

            }
            catch( Exception e ) {

            }
        } // keep looping until we should stop

    }

    //===================================
    public void shutdown() {
        keepRunning = false;
        try {
            join();
        }
        catch( Exception e ) {
            // whatever
        }
    }

}
