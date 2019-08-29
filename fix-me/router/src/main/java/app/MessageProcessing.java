package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MessageProcessing extends Thread {
    private Socket socket;

    public MessageProcessing(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                String echoString = input.readLine();
                if (echoString == null) {
                //- ⬆ echoString == null Break; Helps Prevent null pointer exception when Broker or Market close unexpectedly
                    break;
                }

                //Check if message follows fix notation from broker/market here
                //echoString is a message(buy/sell/accept buy/reject buy/ etc.) from either broker or market
                Server.readMessage(echoString);
                // 

                //Message gets split up here to be passed to specific statements
                //that will either send it to broker or market 
                String[] echoStringParts = echoString.split("-");
                //

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
        }
    }
}
