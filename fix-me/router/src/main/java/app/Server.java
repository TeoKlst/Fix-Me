package app;

import app.fix.FixProtocol;
import app.fix.exceptions.InvalidMsgTypeException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
	private static FixProtocol fixProtocol;

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


	    MarketSocket(ServerSocket sM) { socketM = sM; }

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

    public static void         readMessage(String input) {
    	String rejectMessage = fixProtocol.receiveMessage(input);
    	if (rejectMessage != null) {
    		//Send rejectMessage
		    System.out.println("Reject message: " + rejectMessage);
		    return;
	    }
    	try {
    		String type = fixProtocol.getMsgType(input);
    		if (type.equals("A") || type.equals("5") || type.equals("0")) {
    			//Message for router
			    System.out.println("Message for Router");
		    } else if (type.equals("3") || type.equals("AK") || type.equals("D")) {
				//Send through message to recipient
			    System.out.println("Message for recipient");
		    } else {
    			throw new InvalidMsgTypeException("No valid type sent through");
		    }
	    } catch (InvalidMsgTypeException mte) {
		    System.out.println("Invalid message exception found");
		    int msgSqnNum = -1;
		    //Reject through the msgLength - first get the msgSequence number
		    String[] message = input.split("\\|");
		    for (int i=0; i < message.length; i++) {
			    if (message[i].startsWith("34=") && fixProtocol.isNumeric(message[i].substring(3)) && fixProtocol.isInteger(message[i].substring(3))) {
				    msgSqnNum = Integer.parseInt(message[i].substring(3));
			    }
		    }
		    if (msgSqnNum < 1) {
			    msgSqnNum = 1;
		    }
		    rejectMessage = fixProtocol.RejectMessage(msgSqnNum, 11, "InvalidMsgType");
		    //Send reject message
		    System.out.println("Reject message: " + rejectMessage);

	    }
    }

    public static void main(String[] args)
    {
	    fixProtocol = new FixProtocol("000001");

	    String logonMessage = fixProtocol.logonMessage(120);
        String heartBeatMessage = fixProtocol.heartBeatMessage();
        String logoutMessage = fixProtocol.logoutMessage();
//        System.out.println("Logon message: " + logonMessage);
//        fixProtocol.receiveMessage(logonMessage);
//        fixProtocol.receiveMessage("8=FIX4.4|9=60|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|10=227|");
//		String test2 = "8=FIX4.4|9=61|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|";
//        test2 = test2 + "10=" + fixProtocol.checksumGenerator(test2) + "|";
//        System.out.println("TEST2 " + test2);
//        fixProtocol.receiveMessage(test2);
//		fixProtocol.receiveMessage("");
//		try {
//			System.out.println("Message type: " + fixProtocol.getMsgType(logonMessage));
//			System.out.println("Message type: " + fixProtocol.getMsgType(heartBeatMessage));
//			System.out.println("Message type: " + fixProtocol.getMsgType(logoutMessage));
//		} catch (InvalidMsgTypeException mte) {
//			System.out.println("Invalid message exception found");
//		}
	    readMessage(logonMessage);
	    readMessage(heartBeatMessage);
	    readMessage(logoutMessage);
	    readMessage("8=FIX4.4|9=60|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|10=227|");
	    String test2 = "8=FIX4.4|9=61|35=A|34=1|52=2019082611:45:32|98=0|553=000001|108=120|141=Y|";
	    test2 = test2 + "10=" + fixProtocol.checksumGenerator(test2) + "|";
	    readMessage(test2);
	    readMessage("");
	    readMessage("8=FIX4.4|9=77|35=3|34=4|52=2019082708:29:00|98=0|553=000001|45=1|373=99|58=InvalidCheckSum|10=248|");
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