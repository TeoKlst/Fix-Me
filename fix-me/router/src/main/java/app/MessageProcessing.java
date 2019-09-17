package app;

import app.fix.FixProtocol;
import app.fix.exceptions.InvalidMsgTypeException;

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

    private static FixProtocol fixProtocol = new FixProtocol(Integer.toString(1));

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                String echoString = input.readLine();
                if (echoString == null) {
                    System.out.println("---Message Processor Null Break---");
                    break;
                }

                if (echoString.equals("exit")) {
                    break;
                }

                String rejectMessage = fixProtocol.receiveMessage(echoString);
                if (rejectMessage != null) {
                    //Send rejectMessage
                    Socket brokerPort = Server.mapBroker.get(fixProtocol.getRouteID(echoString));
                    output = new PrintWriter(brokerPort.getOutputStream(), true);
                    System.out.println("Reject message: " + rejectMessage);
                    return;
                }
                try {
                    String type = fixProtocol.getMsgType(echoString);
                    if (type.equals("A") || type.equals("5") || type.equals("0")) {
                        //Send through message to heartBeatScanners
                        if (fixProtocol.getHBType(echoString).equals("1")) {
                            Socket brokerPort = Server.mapBroker.get("0");
                            output = new PrintWriter(brokerPort.getOutputStream(), true);
                            output.println(echoString);
                        }
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
                            try {
                                output.println(fixProtocol.ListMarketReturn(fixProtocol.getMsgSeqNum(echoString)));
                            } catch (InvalidMsgTypeException e) {
                                e.printStackTrace();
                            }
                        }
                        if (type.equals("AK") || type.equals("3") || type.equals("7") || type.equals("4")) {
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
								try {
									output.println(fixProtocol.NullMarket(fixProtocol.getMsgSeqNum(echoString), Integer.parseInt(fixProtocol.getRouteID(echoString))));
								} catch (InvalidMsgTypeException e) {
									e.printStackTrace();
								}
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
                    try {
						rejectMessage = fixProtocol.RejectMessage(msgSqnNum, 11,fixProtocol.getMsgSeqNum(echoString), "InvalidMsgType");
					} catch (InvalidMsgTypeException e) {
                    	e.printStackTrace();
					}

                    //Send reject message
                    Socket brokerPort = Server.mapBroker.get(fixProtocol.getRouteID(echoString));
                    output = new PrintWriter(brokerPort.getOutputStream(), true);
                    System.out.println("Reject message: " + rejectMessage);
                }
            }         
        } catch(IOException | InvalidMsgTypeException e) {
            System.out.println("Oops: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch(IOException e) {}
        }
    }
}
