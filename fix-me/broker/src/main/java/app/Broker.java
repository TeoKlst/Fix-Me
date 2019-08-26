package app;

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
            
            do {
                StringBuilder sbMessage = new StringBuilder();
                String brokerMessageType = "0";

                System.out.println("Buy, Sell, List Markets or Display your goods:");
                echoString = scanner.nextLine().toLowerCase();

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
                else if (echoString.equals("listm") || echoString.equals("list markets")) {
                    brokerMessageType = "3";
                    sbMessage.append(brokerMessageType + "-");
                    sbMessage.append(BrokerAccount.brokerRouteID);
                    dOut.println(sbMessage);
                }
                else if (echoString.equals("listg")|| echoString.equals("list goods")) {
                    System.out.println("__/Your Account/__" + "\nSilver: " + BrokerAccount.accountSilver + 
                    "\nGold: " + BrokerAccount.accountGold+ "\nPlatinum: " + BrokerAccount.accountPlatinum + 
                    "\nFuel: " + BrokerAccount.accountFuel + "\nBitcoin: " + BrokerAccount.accountBitcoin + 
                    "\nCapital :" + BrokerAccount.capital);
                    stringToEcho.println(echoString);
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