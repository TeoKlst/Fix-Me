package app;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


/**
 * <P>
 * This Java network programming example shows how to
 * write a server that handles client connections on two 
 * different sockets: A and B.  Clients that connect
 * to socket A are called Group A clients, and those for
 * socket B are Group B clients.  Each line of text received
 * from any A client is sent to all B clients, and each
 * line received from a B client is copied to all A clients.
 * The port numbers for the A and B sockets must be supplied
 * on the command line.
 * </P><P>
 * This example shows how to use java.net.ServerSocket,
 * java.net.Socket, various java.io classes, java.util.Set,
 * java.util.concurrent.ExecutorService, java.lang.Thread,
 * and java.lang.Runnable.
 * </P><P>
 * This code requires JDK 1.5 (J2SE 5.0) or later.
 */

public class TwoPortServer {
    // output thread count
    private final static int OUTPUT_THREADS = 5;

    // members to support the two server sockets
    private ServerSocket socketA;
    private Runnable accepterA;
    private ServerSocket socketB;
    private Runnable accepterB;

    // members to hold the two groups of clients
    private Set<Socket> groupA;
    private Set<Socket> groupB;

    // members to support the output thread pool
    private ExecutorService outputService;

    /**
     * Constructor that creates a TwoPortServer for
     * two designated ports.  If either port is
     * not usable, or if any other errors occur,
     * an IOException is thrown.
     */
    public TwoPortServer(int portA, int portB) 
        throws IOException
    {
        if (portA == portB)
            throw new IllegalArgumentException("Ports can't be equal");
    
        groupA = Collections.synchronizedSet(new HashSet<Socket>());
        groupB = Collections.synchronizedSet(new HashSet<Socket>());

        socketA = new ServerSocket(portA);
        socketB = new ServerSocket(portB);

        accepterA = new ConnAccepter(socketA, groupA, groupB);
        accepterB = new ConnAccepter(socketB, groupB, groupA);

        Thread tA = new Thread(accepterA, "Group A");
        Thread tB = new Thread(accepterB, "Group B");

        outputService = Executors.newFixedThreadPool(OUTPUT_THREADS);

        tA.start();
        tB.start();
    }

    /**
     * Each ConnAccepter object runs in a thread, and handles
     * accepting connection for one ServerSocket.  Each time
     * it accepts a connection, it create a SocketInputReader
     * object for the newly connected Socket, and kicks off
     * a separate Thread for reading input from that Socket.
     */
    class ConnAccepter implements Runnable {
        private ServerSocket sock;
        private Set<Socket> mygroup, othergroup;

        ConnAccepter(ServerSocket s, Set<Socket> g1, Set<Socket> g2) {
            sock = s; 
            mygroup = g1; othergroup = g2;
        }

        public void run() {
            Socket newsock;
            Runnable newreader;
            while(true) {
                newsock = null;
                try {
                    newsock = sock.accept();
                    System.err.println("Connection from " + newsock +
                                       " for " + 
                                       Thread.currentThread().getName());
                } catch (IOException e) {
                    System.err.println("Bad accept in " + 
                         Thread.currentThread().getName());
                }
                if (newsock != null) {
                    try {
                        newreader = new SocketInputReader(newsock,
                                                  mygroup,othergroup);
                        mygroup.add(newsock);
                        Thread t = new Thread(newreader,newsock.toString());
                        t.start();
                    } catch (IOException e2) {
                        System.err.println("Error on reader startup in " +
                         Thread.currentThread().getName());
                    }
                }
            }
        }
    }

    /**
     * Each SocketInputReader object handles reading lines of
     * text from a Socket, using a BufferedReader.  For each
     * line of text, an OutputAction object is created for
     * each member of the outputgroup, and each OutputAction
     * object is given to the outputService to be handled.
     */
    class SocketInputReader implements Runnable {
        private Socket mysock;
        private Set<Socket> mygroup, outputgroup;
        private BufferedReader reader;

        SocketInputReader(Socket s, Set<Socket>mg, Set<Socket> og) 
            throws IOException
        {
            InputStream is = s.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));
            mygroup = mg;
            outputgroup = og;
        }

        public void run() {
            String line;
            Runnable opx;
            Iterator<Socket> it;
            try {
                while(true) {
                    line = reader.readLine();
                    if (line == null) break; // end of input
                    line = line + "\n";
                    for(it = outputgroup.iterator(); it.hasNext(); ) {
                        opx = new OutputAction(line.getBytes(),it.next());
                        outputService.submit(opx);
                    }
                }
            }
            catch (IOException e) { }
            finally {
                System.err.println("Lost connection (" +
                                   Thread.currentThread().getName() +
                                   ")");
                mygroup.remove(mysock);
                try { reader.close(); } catch (Exception e1) { }
                try { mysock.close(); } catch (Exception e2) { }
            }
        }
    }

    /**
     * An OutputAction object represents a single chunk of data
     * to be written to a particular Socket.  The socket and
     * data are bundled up in the way so that the output action
     * can be managed by a ExecutorService (thread pool).
     */
    public class OutputAction implements Runnable {
        private byte[] data;
        private Socket sock;
        public OutputAction(byte [] d, Socket s) {
            data = d; sock = s;
        }
        public void run() {
            try {
                sock.getOutputStream().write(data);
            }
            catch(IOException e) {
                groupA.remove(sock);
                groupB.remove(sock);
            }
        }
    }

    
    /**
     * Main method, for accepting command line arguments.
     * 
     * To use this program, give it two command line arguments,
     * two different integers between 1025 and 65535.  It will
     * open TCP sockets on the two ports and wait for connections.
     */
    public static void main(String [] args) {
        /*
        if (args.length != 2) {
            System.err.println("Usage: java TwoPortServer port1 port2");
            System.exit(0);
        }

        int portA = Integer.parseInt(args[0]);
        int portB = Integer.parseInt(args[1]);
        */

        int portA = 5000;
        int portB = 5001;

        TwoPortServer server;
        try {
            server = new TwoPortServer(portA, portB);
        } catch(IOException ie) {
            System.err.println("Could not start server: " + ie);
        }
    }

}