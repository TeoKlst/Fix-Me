package app;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {

    //-Broker Hash Map
    public static Map<String, Socket> mapBroker;
    private ServerSocket socketBroker;
    private Runnable bS;

    //-Market Hash Map
    public static Map<String, Socket> mapMarket;
    private ServerSocket socketMarket;
    private Runnable mS;

    private ServerSocket socketHeartBeat;
    private Runnable hB;

    public Server(int portA, int portB) throws IOException {

        mapBroker = new HashMap<String,Socket>();
        mapMarket = new HashMap<String,Socket>();

        socketBroker = new ServerSocket(portA);
        socketMarket = new ServerSocket(portB);
        socketHeartBeat = new ServerSocket(4999);

        bS = new BrokerSocket(socketBroker);
        mS = new MarketSocket(socketMarket);
        hB = new Heartbeat(socketHeartBeat);

        Thread tb = new Thread(bS);
        Thread tm = new Thread(mS);
        Thread tH = new Thread(hB);
        tb.start();
        tm.start();
        tH.start();
    }

    class Heartbeat implements Runnable {
        private ServerSocket socketH;

        Heartbeat(ServerSocket sH) {
            socketH = sH;
        }
        // join a Multicast group and send the group salutations

		public void run() {
            try {
                    String msg = "Hello";
                    InetAddress group = InetAddress.getByName("127.0.0.1");
                    MulticastSocket s = new MulticastSocket(6789);
                    
                    s.joinGroup(group);
                    DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, 6789);
                    s.send(hi);
                    // get their responses!
                    byte[] buf = new byte[1000];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    s.receive(recv);

                    // OK, I'm done talking - leave the group...
                    s.leaveGroup(group);
            } catch(Exception e) {
                System.out.println("HeartBeat Server exception " + e.getMessage());
            }
		}
    }

    // TODO:CHECK HEART BEAT
    // TODO:BROKER MARKET REMOVAL ON DISCONNECT
    class BrokerSocket implements Runnable {
        private ServerSocket socketB;

        BrokerSocket(ServerSocket sB) {
            socketB = sB;
        }

        public void run() {
            System.out.println("--Broker Router Running--");
            while(true) {
                try {
                    Socket socket = socketB.accept();
                    Echoer echoer = new Echoer(socket);
                    echoer.start();

                    //-Broker Saved in Hash Map
                    LinkCounter.countBroker();
                    int serviceID = LinkCounter.generateServiceID();
                    String routeID = LinkCounter.getBrokerRouteID(socket);
                    mapBroker.put(routeID, socket);
                    System.out.println("Broker[" + LinkCounter.brokerCount + "] connected");
                    System.out.println("Saved Brokers => " + mapBroker);

                    //-Send message to broker
                    Socket brokerPort = mapBroker.get(Integer.toString(LinkCounter.brokerCount));
                    PrintWriter output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println(LinkCounter.brokerCount + "-" + serviceID);
                } catch(Exception e) {
                    System.out.println("Broker Server exception " + e.getMessage());
                }
            }
        }
    }

    class MarketSocket implements Runnable {
        private ServerSocket socketM;

        MarketSocket(ServerSocket sM) {
            socketM = sM;
        }

        public void run() {
            System.out.println("--Market Router Running--");
            while(true) {
                try {
                    Socket socket = socketM.accept();
                    Echoer echoer = new Echoer(socket);
                    echoer.start();

                    //-Market Saved in Hash Map
                    LinkCounter.countMarket();
                    int serviceID = LinkCounter.generateServiceID();
                    String routeID = LinkCounter.getMarketRouteID(socket);
                    mapMarket.put(routeID, socket);
                    System.out.println("Market[" + LinkCounter.marketCount + "] connected");
                    System.out.println("Saved Markets => " + mapMarket);

                    //-Send message to market
                    Socket marketPort = mapMarket.get(Integer.toString(LinkCounter.marketCount));
                    PrintWriter output = new PrintWriter(marketPort.getOutputStream(), true);
                    output.println(LinkCounter.marketCount + "-" + serviceID);
                } catch(Exception e) {
                    System.out.println("Market Server exception " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args)
    {
        int portA = 5000;
        int portB = 5001;

        Server server;
        try {
            server = new Server(portA, portB);
        } catch(IOException ie) {
            System.err.println("Could not start server: " + ie);
        }
    }
} 