package app;

import app.fix.FixProtocol;
import app.fix.exceptions.InvalidChecksumException;
import app.fix.exceptions.InvalidMsgLengthException;

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

    public Server(int portA, int portB) throws IOException {

        mapBroker = new HashMap<String,Socket>();
        mapMarket = new HashMap<String,Socket>();

        socketBroker = new ServerSocket(portA);
        socketMarket = new ServerSocket(portB);

        bS = new BrokerSocket(socketBroker);
        mS = new MarketSocket(socketMarket);

        Thread tb = new Thread(bS);
        Thread tm = new Thread(mS);
        tb.start();
        tm.start();
    }

    private static String getUserNameBroker(Socket s) {
        return Integer.toString(BrokerCount.brokerCount);
    }

    private static String getUserNameMarket(Socket s) {
        return Integer.toString(MarketCount.marketCount);
    }

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
                    MessageProcessing messageProcessing = new MessageProcessing(socket);
                    messageProcessing.start();

                    //-Broker Saved in Hash Map
                    BrokerCount.brokerCount = BrokerCount.brokerCount + 1;
                    String username = getUserNameBroker(socket);
                    mapBroker.put(username, socket);
                    System.out.println("Broker[" + BrokerCount.brokerCount + "] connected");
                    System.out.println("Saved Brokers => " + mapBroker);

                    //-Send message to broker
                    // Socket brokerPort = mapBroker.get(Integer.toString(BrokerCount.brokerCount));
                    // PrintWriter output = new PrintWriter(brokerPort.getOutputStream(), true);
                    // output.println("You are broker: " + BrokerCount.brokerCount);
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
                    MessageProcessing messageProcessing = new MessageProcessing(socket);
                    messageProcessing.start();

                    //-Market Saved in Hash Map
                    MarketCount.marketCount = MarketCount.marketCount + 1;
                    String username = getUserNameMarket(socket);
                    mapMarket.put(username, socket);
                    System.out.println("Market[" + MarketCount.marketCount + "] connected");
                    System.out.println("Saved Markets => " + mapMarket);

                    //-Send message to market
                    // Socket brokerPort = mapMarket.get(Integer.toString(BrokerCount.brokerCount));
                    // PrintWriter output = new PrintWriter(brokerPort.getOutputStream(), true);
                    // output.println("You are market: " + MarketCount.marketCount);
                } catch(Exception e) {
                    System.out.println("Market Server exception " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args)
    {
        FixProtocol fixProtocol = new FixProtocol("000001");
        String logonMessage = fixProtocol.logonMessage(120);
        System.out.println("Logon message: " + logonMessage);
        if (fixProtocol.validateMessage(logonMessage) == -1) {
            System.out.println("Checksum invalid");
        } else if (fixProtocol.validateMessage(logonMessage) == -2) {
            System.out.println("Message Length invalid");
        } else if (fixProtocol.validateMessage(logonMessage) == 1) {
            System.out.println("Checksum valid");
            System.out.println("Message Length valid");
        }

        if (fixProtocol.validateMessage("8=FIX4.4|9=60|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|10=227|") == -1) {
            System.out.println("Checksum invalid");
        } else if (fixProtocol.validateMessage("8=FIX4.4|9=60|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|10=227|") == -2) {
            System.out.println("Message Length invalid");
        } else if (fixProtocol.validateMessage("8=FIX4.4|9=60|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|10=227|") == 1) {
            System.out.println("Checksum valid");
            System.out.println("Message Length valid");
        }

        if (fixProtocol.validateMessage("8=FIX4.4|9=61|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|10=228|") == -1) {
            System.out.println("Checksum invalid");
        } else if (fixProtocol.validateMessage("8=FIX4.4|9=61|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|10=228|") == -2) {
            System.out.println("Message Length invalid");
        } else if (fixProtocol.validateMessage("8=FIX4.4|9=61|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|10=228|") == 1) {
            System.out.println("Checksum valid");
            System.out.println("Message Length valid");
        }

//        int portA = 5000;
//        int portB = 5001;
//
//        Server server;
//        try {
//            server = new Server(portA, portB);
//        } catch(IOException ie) {
//            System.err.println("Could not start server: " + ie);
//        }
    }
} 