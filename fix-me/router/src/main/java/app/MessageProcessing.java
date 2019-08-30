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
                String[] echoStringParts = echoString.split("-");
                if (echoString.equals("exit")) {
                    break;
                }
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
                //-Buy from market || Sell to market
                else if (echoStringParts[0].equals("1") || echoStringParts[0].equals("2")) {
                    Socket marketPort = Server.mapMarket.get(echoStringParts[1]);
                    if (marketPort != null) {
                        if (echoStringParts[1].equals("0")) {
                            Socket brokerPort = Server.mapMarket.get(echoStringParts[5]);
                            output = new PrintWriter(brokerPort.getOutputStream(), true);
                            output.println("Market Find  Error");
                        }
                        else {
                            output = new PrintWriter(marketPort.getOutputStream(), true);
                            output.println(echoString);
                        }
                    }
                    else {
                        Socket brokerPort = Server.mapBroker.get(echoStringParts[5]);
                        output = new PrintWriter(brokerPort.getOutputStream(), true);
                        output.println("Market Find Error");
                    }
                }
                //-List Markets
                else if (echoStringParts[0].equals("3")) {
                    Socket brokerPort = Server.mapBroker.get(echoStringParts[1]);
                    output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println("Available Market ID's => " + (Server.mapHBMarket.keySet()));
                }
                //-Purchase || Sale Executed
                else if (echoStringParts[0].equals("4")) {
                    Socket marketPort = Server.mapBroker.get(echoStringParts[1]);
                    output = new PrintWriter(marketPort.getOutputStream(), true);
                    output.println(echoString);
                }
                //-Purchase || Sale Rejected
                else if (echoStringParts[0].equals("5")) {
                    Socket brokerPort = Server.mapBroker.get(echoStringParts[1]);
                    output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println(echoString);
                }
                //-List Market Goods Query
                else if (echoStringParts[0].equals("6")) {
                    Socket marketPort = Server.mapMarket.get(echoStringParts[1]);
                    if (marketPort != null) {
                        if(echoStringParts[1].equals("0")) {
                            Socket brokerPort = Server.mapMarket.get(echoStringParts[2]);
                            output = new PrintWriter(brokerPort.getOutputStream(), true);
                            output.println("Market Find  Error");
                        }
                        else {
                            output = new PrintWriter(marketPort.getOutputStream(), true);
                            output.println(echoString);
                        }
                    }
                    else {
                        Socket brokerPort = Server.mapMarket.get(echoStringParts[2]);
                        output = new PrintWriter(brokerPort.getOutputStream(), true);
                        output.println("Market Find  Error");
                    }
                }
                //-List Market Goods Data Return
                else if (echoStringParts[0].equals("7")) {
                    Socket marketPort = Server.mapBroker.get(echoStringParts[2]);
                    output = new PrintWriter(marketPort.getOutputStream(), true);
                    output.println(echoString);
                }
                else {
                    output = new PrintWriter(socket.getOutputStream(), true);
                    output.println(echoString);
                }
            }
        } catch(IOException e) {
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
