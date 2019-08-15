package app;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AsyncRouter {

    private final static int OUTPUT_THREADS = 5;

    private ServerSocket socketBroker;
    private Runnable accepterBroker;
    private ServerSocket socketMarket;
    private Runnable accepterMarket;

    private Set<Socket> groupBroker;
    private Set<Socket> groupMarket;

    private ExecutorService outputService;

    public AsyncRouter(int portBroker, int portMarket)throws IOException {

        if (portBroker == portMarket) {
            throw new IllegalArgumentException("Ports can't be equal");
        }

        groupBroker = Collections.synchronizedSet(new HashSet<Socket>());
        groupMarket = Collections.synchronizedSet(new HashSet<Socket>());

        socketBroker = new ServerSocket(portBroker);
        socketMarket = new ServerSocket(portMarket);

        // accepterBroker = new ConnAccepter(socketBroker, groupBroker, groupMarket);
        accepterMarket = new ConnAccepter(socketMarket, groupMarket, groupBroker);
        accepterBroker = new ConnAccepter(socketBroker, groupBroker, groupMarket);

        Thread tBroker = new Thread(accepterBroker, "Group Broker");
        Thread tMarket = new Thread(accepterMarket, "Group Market");

        outputService = Executors.newFixedThreadPool(OUTPUT_THREADS);

        tBroker.start();
        tMarket.start();
    }

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

            while (true) {
                newsock = null;
                try {
                    newsock = sock.accept();
                    System.err.println("Connection from " + newsock + " for " + Thread.currentThread().getName());
                } catch(IOException e) {
                    System.err.println("Bad accept in " + Thread.currentThread().getName());
                }
                if (newsock != null) {
                    try {
                        newreader = new SocketInputReader(newsock, mygroup, othergroup);
                        mygroup.add(newsock);
                        Thread t = new Thread(newreader, newsock.toString());
                        t.start();
                    } catch(IOException e2) {
                        System.err.println("Error on reader startup in " + Thread.currentThread().getName());
                    }
                }
            }
        }
    }


    class SocketInputReader implements Runnable {
        private Socket mysock;
        private Set<Socket> mygroup, outputgroup;
        private BufferedReader reader;

        SocketInputReader(Socket s, Set<Socket>mg, Set<Socket>og) throws IOException {
            InputStream is = s.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));
            // BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            mygroup = mg;
            outputgroup = og;
            // PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
        }

        public void run() {
            String line;
            Runnable opx;
            Iterator<Socket> it;
            try {
                while (true) {
                    line = reader.readLine();
                    if (line == null) break;
                    line = line + "\n";
                    for (it = outputgroup.iterator(); it.hasNext();) {
                        opx = new OutputAction(line.getBytes(),it.next());
                        outputService.submit(opx);
                    } 
                }
            } catch (IOException e) { }
            finally {
                System.err.println("Lost connection (" + Thread.currentThread().getName() + ")");
                mygroup.remove(mysock);
                try { reader.close(); } catch (Exception e1) { }
                try { mysock.close(); } catch (Exception e2) { }
            }
        }
    }

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
                groupBroker.remove(sock);
                groupMarket.remove(sock);
            }
        }
    }

    public static void main(String[] args) {
        int portBroker = 5000;
        int portMarket = 5001;

        AsyncRouter router;
        try {router = new AsyncRouter(portBroker, portMarket);
        } catch(IOException e) {
            System.err.println("Could not start server: " + e);
        }

    }
}