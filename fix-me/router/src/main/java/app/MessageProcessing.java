package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import app.fix.FixProtocol;
import app.fix.exceptions.InvalidMsgTypeException;

public class MessageProcessing extends Thread {
    private Socket socket;

    public MessageProcessing(Socket socket) {
        this.socket = socket;
    }

    // ERROR
    private static FixProtocol fixProtocol;
    fixProtocol = new FixProtocol(Integer.toString(BrokerAccount.brokerServiceID));

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

                String rejectMessage = fixProtocol.receiveMessage(echoString);
                if (rejectMessage != null) {
                    //Send rejectMessage
                    Socket brokerPort = Server.mapBroker.get(fixProtocol.getRouteID(echoString));
                    output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println("Reject message: " + rejectMessage);
                    return;
                }
                try {
                    // TODO Deny user to send to messages to HB readers => "0"
                    String type = fixProtocol.getMsgType(echoString);
                    if (type.equals("A") || type.equals("5") || type.equals("0")) {
                        //Message for router
                        System.out.println("Message for Router");
                    } else if (type.equals("1") || type.equals("2") || type.equals("3") || type.equals("AK") || type.equals("D")) {
                        //Send through message to broker
                        if (type.equals("AK") || type.equals("3")) {
                            Socket brokerPort = Server.mapBroker.get(fixProtocol.getRouteID(echoString));
                            output = new PrintWriter(brokerPort.getOutputStream(), true);
                            output.println(echoString);
                        }
                        //Send through message to market
                        // Get market route id
                        if (type.equals("D") || type.equals("1") || type.equals("2")) {
                            Socket marketPort = Server.mapMarket.get(fixProtocol.getMarketRouteID(echoString));
                            output = new PrintWriter(marketPort.getOutputStream(), true);
                            output.println(echoString);
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

                /*
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
            */
            
        } catch(IOException | InvalidMsgTypeException e) {
            System.out.println("Oops: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch(IOException e) {}
        }
    }
}
