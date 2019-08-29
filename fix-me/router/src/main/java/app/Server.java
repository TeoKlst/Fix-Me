package app;

import java.io.PrintWriter;
import java.io.IOException;
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

    //-Heart-Beat Broker
    public static Map<String, Integer> mapHBBroker;
    //-Heart-Beat Market
    public static Map<String, Integer> mapHBMarket;

    public Server(int portA, int portB) throws IOException {

        mapBroker = new HashMap<String,Socket>();
        mapMarket = new HashMap<String,Socket>();

        mapHBBroker = new HashMap<String,Integer>();
        mapHBMarket = new HashMap<String,Integer>();

        socketBroker = new ServerSocket(portA);
        socketMarket = new ServerSocket(portB);

        bS = new BrokerSocket(socketBroker);
        mS = new MarketSocket(socketMarket);

        Thread tb = new Thread(bS);
        Thread tm = new Thread(mS);
        tb.start();
        tm.start();
    }

    class BrokerSocket implements Runnable {
        private ServerSocket socketB;

        BrokerSocket(ServerSocket sB) {
            socketB = sB;
        }

        public void run() {

            try {
                Socket hbSocket;
                hbSocket = new Socket("127.0.0.1", 5000);
                HBScannerBroker hbScannerBroker = new HBScannerBroker(hbSocket);
                hbScannerBroker.start();
                System.out.println(" -Broker HBScanner Running-");
            } catch (IOException e1) {
                System.out.println("Broker HBSocket exception " + e1.getMessage());
            }

            System.out.println("--Broker Router Running--");

            while(true) {
                try {
                    Socket socket = socketB.accept();
                    MessageProcessing messageProcessing = new MessageProcessing(socket);
                    messageProcessing.start();

                    //-Broker Saved in Hash Map
                    int serviceID = LinkCounter.generateServiceID();
                    String routeID = LinkCounter.getBrokerRouteID(socket);
                    mapBroker.put(routeID, socket);
                    System.out.println("Broker[" + LinkCounter.brokerCount + "] connected");
                    System.out.println("Saved Brokers => " + mapBroker);

                    //-Send message to broker
                    Socket brokerPort = mapBroker.get(Integer.toString(LinkCounter.brokerCount));
                    PrintWriter output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println(LinkCounter.brokerCount + "-" + serviceID);

                    LinkCounter.countBroker();
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

            try {
                Socket hbSocket;
                hbSocket = new Socket("127.0.0.1", 5001);
                HBScannerMarket hbScannerMarket = new HBScannerMarket(hbSocket);
                hbScannerMarket.start();
                System.out.println(" -Market HBScanner Running-");
            } catch (IOException e1) {
                System.out.println("Market HBSocket exception " + e1.getMessage());
            }

            System.out.println("--Market Router Running--");

            while(true) {
                try {
                    Socket socket = socketM.accept();
                    MessageProcessing messageProcessing = new MessageProcessing(socket);
                    messageProcessing.start();

                    //-Market Saved in Hash Map
                    int serviceID = LinkCounter.generateServiceID();
                    String routeID = LinkCounter.getMarketRouteID(socket);
                    mapMarket.put(routeID, socket);
                    System.out.println("Market[" + LinkCounter.marketCount + "] connected");
                    System.out.println("Saved Markets => " + mapMarket);

                    //-Send message to market
                    Socket marketPort = mapMarket.get(Integer.toString(LinkCounter.marketCount));
                    PrintWriter output = new PrintWriter(marketPort.getOutputStream(), true);
                    output.println(LinkCounter.marketCount + "-" + serviceID);

                    LinkCounter.countMarket();
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
