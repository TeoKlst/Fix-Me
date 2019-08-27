package app;

import app.fix.FixProtocol;
import app.fix.exceptions.InvalidMsgTypeException;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;
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

    public static Map<String, Integer> mapHBBroker;
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

    class HeartBeatScanner extends Thread {
        private Socket socket;

        public HeartBeatScanner(Socket socket) {
            this.socket = socket;
        }

		public void run() {
            try {
                BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // HBTimeOut hbTimeOut = new HBTimeOut();
                // hbTimeOut.start();

                while (true) {
                    String echoString = dIn.readLine();
                    Calendar cal = Calendar.getInstance();
                    int seconds = cal.get(Calendar.SECOND);
                    if (echoString == null) {
                        break;
                    }
                    String[] echoStringParts = echoString.split("-");
                    if (echoStringParts[0].equals("HB")) {

                        mapHBBroker.put(echoStringParts[1], seconds);
                        
                        System.out.println("-√v^√v^√❤ Received-" + echoStringParts[1]);
                        System.out.println( "Seconds in current minute = " + seconds);
                        System.out.println(mapHBBroker);
                    }
                }
            } catch(IOException e) {
                System.out.println("Oops: " + e.getMessage());
            } catch(Exception e) {
                System.out.println("HeartBeat Server exception " + e.getMessage());
            }
		}
    }

    class BrokerSocket implements Runnable {
        private ServerSocket socketB;

        BrokerSocket(ServerSocket sB) {
            socketB = sB;
        }

        public void run() {

            Socket hbSocket;
            try {
                hbSocket = new Socket("127.0.0.1", 5000);
                HeartBeatScanner heartBeatScanner = new HeartBeatScanner(hbSocket);
                heartBeatScanner.start();
            } catch (UnknownHostException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
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
                    
                    //-Count added Broker(Avoid nulls with brokerHB) 
                    LinkCounter.countBroker();
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