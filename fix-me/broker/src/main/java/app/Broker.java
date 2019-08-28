package app;

import app.fix.FixProtocol;
import app.fix.exceptions.InvalidMsgTypeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// The Broker will send two types of messages:

// • Buy. - An order where the broker wants to buy an instrument
// • Sell. - An order where the broker want to sell an instrument

// and will receive from the market messages of the following types:

// • Executed - when the order was accepted by the market and the action succeeded
// • Rejected - when the order could not be met

class Broker {
    private static FixProtocol fixProtocol;

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

    public static void main(String[] args) throws Exception {
        // new Socket("localhost", 5001) <- should also work with that string
        try (Socket socket = new Socket("127.0.0.1", 5000)) {
            //-Starts Broker HeartBeat
            // BrokerHBSender brokerHBSender = new BrokerHBSender(socket);
            // brokerHBSender.start();

            BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter dOut = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            String echoString;
            String response;

            String savedServerResponse = dIn.readLine();
            BrokerFunctions.assignRouteServiceID(savedServerResponse);
            System.out.println("--Broker Connected--\n" + 
            "You are Broker[" + BrokerAccount.brokerRouteID + "]" + " ServiceID => " + BrokerAccount.brokerServiceID);
            fixProtocol = new FixProtocol("" + BrokerAccount.brokerServiceID);
            
            do {
                StringBuilder sbMessage = new StringBuilder();
                String brokerMessageType = "0";

                System.out.println("Buy, Sell, List Markets or Display your goods:");
                echoString = scanner.nextLine().toLowerCase();
                readMessage(echoString);

                if (echoString.equals("buy")) {
                    //-Type of message ID
                    brokerMessageType = "1";
                    sbMessage.append(brokerMessageType + "-");
                    System.out.println("Choose Market ID:");
                    echoString = scanner.nextLine().toLowerCase();
                    sbMessage.append(echoString + "-");
                    System.out.println("Choose Item ID to purchase:");
                    echoString = scanner.nextLine().toLowerCase();
                    sbMessage.append(echoString + "-");
                    System.out.println("Choose purchase Amount:");
                    echoString = scanner.nextLine().toLowerCase();
                    sbMessage.append(echoString + "-");
                    System.out.println("Choose purchase Price:");
                    echoString = scanner.nextLine().toLowerCase();
                    sbMessage.append(echoString + "-");
                    sbMessage.append(BrokerAccount.brokerRouteID);
                    if (BrokerFunctions.brokerPurchaseValidate(sbMessage.toString())) {
                        //-Sends message to echoer
                        dOut.println(sbMessage.toString());
                    }
                    else
                        dOut.println("Purchase: Account amount error");
                }
                else if (echoString.equals("sell")) {
                    brokerMessageType = "2";
                    sbMessage.append(brokerMessageType + "-");
                    System.out.println("Choose Market ID:");
                    echoString = scanner.nextLine().toLowerCase();
                    sbMessage.append(echoString + "-");
                    System.out.println("Choose Item ID to sell:");
                    echoString = scanner.nextLine().toLowerCase();
                    sbMessage.append(echoString + "-");
                    System.out.println("Choose sale Amount:");
                    echoString = scanner.nextLine().toLowerCase();
                    sbMessage.append(echoString + "-");
                    System.out.println("Choose sale Price:");
                    echoString = scanner.nextLine().toLowerCase();
                    sbMessage.append(echoString + "-");
                    sbMessage.append(BrokerAccount.brokerRouteID);
                    if (BrokerFunctions.brokerSaleValidate(sbMessage.toString())) {
                        //-Sends message to echoer
                        dOut.println(sbMessage.toString());
                    }
                    else
                        dOut.println("Sale: Account amount error");
                }
                else if (echoString.equals("listm")) {
                    brokerMessageType = "3";
                    sbMessage.append(brokerMessageType + "-");
                    sbMessage.append(BrokerAccount.brokerRouteID);
                    dOut.println(sbMessage);
                }
                else if (echoString.equals("listg")) {
                    BrokerFunctions.brokerGetDataBroker();
                    dOut.println(echoString);
                }
                else if (echoString.equals("listmg")) {
                    System.out.println("Choose Market ID to view (its) goods:");
                    echoString = scanner.nextLine().toLowerCase();
                    brokerMessageType = "6";
                    sbMessage.append(brokerMessageType + "-");
                    sbMessage.append(echoString + "-");
                    sbMessage.append(BrokerAccount.brokerRouteID);
                    dOut.println(sbMessage);
                }
                else {
                    dOut.println(echoString);
                }
                if (!echoString.equals("exit")) {
                    response = dIn.readLine();
                    String[] echoStringParts = response.split("-");
                    if (echoStringParts[0].equals("4")) {
                        if (echoStringParts[2].equals("1"))
                            BrokerFunctions.brokerBuySuccess(sbMessage.toString());
                        if (echoStringParts[2].equals("2"))
                            BrokerFunctions.brokerSellSuccess(sbMessage.toString());
                        System.out.println("Transaction Successful");
                    }
                    else if (echoStringParts[0].equals("5"))
                        System.out.println("Transaction Failed");
                    else if (echoStringParts[0].equals("7"))
                        BrokerFunctions.brokerReceiveDataMarket(response);
                    else
                        System.out.println(response);
                }
            } while (!echoString.equals("exit"));

            // brokerHBSender.interrupt();
            scanner.close();
            System.out.println("Connection Closed");

        } catch(IOException e) {
            System.out.println("Broker Error: " + e.getMessage());
        }
    }
}