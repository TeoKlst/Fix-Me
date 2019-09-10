package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;

import app.fix.FixProtocol;
import app.fix.exceptions.InvalidMsgTypeException;

public class MessageProcessing extends Thread {

	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost:3306/fixme";

	//  Database credentials
	static final String USER = "java";
	static final String PASS = "123";

    private Socket socket;

    public MessageProcessing(Socket socket) {
        this.socket = socket;
    }

    // ERROR
    private static FixProtocol fixProtocol = new FixProtocol(Integer.toString(1));

    @Override
    public void run() {

		Connection conn = null;
		Statement stmt = null;
		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			stmt = conn.createStatement();
		} catch(Exception e) {
			e.printStackTrace();
		}
        
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                String echoString = input.readLine();
                try {
                    String sql = "INSERT INTO fixmessages (Message) " +
                                "VALUES ('" + echoString + "')";
                    stmt.executeUpdate(sql);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                if (echoString == null) {
                //- ⬆ echoString == null Break; Helps Prevent null pointer exception when Broker or Market close unexpectedly
                    break;
                }

                if (echoString.equals("exit")) {
                    break;
                }

                /*
                //Transfers HB to broker server
                else if (echoStringParts[0].equals("HBB")) {
                    Socket hbPort = Server.mapBroker.get("0");
                    output = new PrintWriter(hbPort.getOutputStream(), true);
                    output.println(echoString);
                }
                //Transfers HB to market server
                else if (echoStringParts[0].equals("HBM")) {
                    Socket mbPort = Server.mapMarket.get("0");
                    output = new PrintWriter(mbPort.getOutputStream(), true);
                    output.println(echoString);
                }
                */

/////////////////////////////////////////////////////////////////////////////////////////////////////////
                output.println("Received message: " + echoString);

                String rejectMessage = fixProtocol.receiveMessage(echoString);
                if (rejectMessage != null) {
                    //Send rejectMessage
                    Socket brokerPort = Server.mapBroker.get(fixProtocol.getRouteID(echoString));
                    output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println("Reject message: " + rejectMessage);
                    return;
                }
                try {
                    String type = fixProtocol.getMsgType(echoString);
                    if (type.equals("A") || type.equals("5") || type.equals("0")) {
                        output.println("Message for Router");
                        //Message for router
                        if (fixProtocol.getHBType(echoString).equals("1")) {
                            Socket brokerPort = Server.mapBroker.get("0");
                            output = new PrintWriter(brokerPort.getOutputStream(), true);
                            output.println(echoString);
                        }
                        String test = fixProtocol.getHBType(echoString);
                        if (fixProtocol.getHBType(echoString).equals("2")) {
                            Socket marketPort = Server.mapMarket.get("0");
                            output = new PrintWriter(marketPort.getOutputStream(), true);
                            output.println(echoString);
                        }
                    } 
                    else if (type.equals("1") || type.equals("2") || type.equals("3") || type.equals("AK") || type.equals("D")
                            || type.equals("6") || type.equals("7") || type.equals("60") || type.equals("4")) {
                        //Send through message to broker
                        if (type.equals("60")) {
                            Socket brokerPort = Server.mapBroker.get(fixProtocol.getRouteID(echoString));
                            output = new PrintWriter(brokerPort.getOutputStream(), true);
                            output.println(fixProtocol.ListMarketReturn());
                        }
                        if (type.equals("AK") || type.equals("3") || type.equals("7")) {
                            Socket brokerPort = Server.mapBroker.get(fixProtocol.getRouteID(echoString));
                            output = new PrintWriter(brokerPort.getOutputStream(), true);
                            output.println(echoString);
                        }
                        //Send through message to market
                        if (type.equals("D") || type.equals("1") || type.equals("2") || type.equals("6")) {
                            Socket marketPort = Server.mapMarket.get(fixProtocol.getMarketRouteID(echoString));
                            if (marketPort == null) {
                                Socket brokerPort = Server.mapBroker.get(fixProtocol.getRouteID(echoString));
                                output = new PrintWriter(brokerPort.getOutputStream(), true);
                                output.println(fixProtocol.NullMarket());
                            }
                            else {
                                output = new PrintWriter(marketPort.getOutputStream(), true);
                                output.println(echoString);
                            }
                        }
                    } else {
                        throw new InvalidMsgTypeException("No valid type sent through");
                    }
                } catch (InvalidMsgTypeException mte) {
                    System.out.println("Invalid message exception found");
                    int msgSqnNum = -1;
                    //Reject through the msgLength - first get the msgSequence number
                    String[] message = echoString.split("\\|");
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
                    Socket brokerPort = Server.mapBroker.get(fixProtocol.getRouteID(echoString));
                    output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println("Reject message: " + rejectMessage);
                }
            }
/////////////////////////////////////////////////////////////////////////////////////////////////////////            
        } catch(IOException | InvalidMsgTypeException e) {
            System.out.println("Oops: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch(IOException e) {}
            try{
				if(stmt!=null)
					stmt.close();
			}catch(SQLException se){
			}
			try{
				if(conn!=null)
					conn.close();
			}catch(SQLException se){
                se.printStackTrace();
            }
        }
    }
}
