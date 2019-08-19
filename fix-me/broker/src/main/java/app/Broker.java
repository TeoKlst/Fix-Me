package app;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// The Broker will send two types of messages:

// • Buy. - An order where the broker wants to buy an instrument
// • Sell. - An order where the broker want to sell an instrument

// and will receive from the market messages of the following types:

// • Exeuted - when the order was accepted by the market and the action succeeded
// • Rejected - when the order could not be met

class Broker {
    public static void main(String[] args) throws Exception {
        // new Socket("localhost", 5001) <- should also work with that string
        try (Socket socket = new Socket("127.0.0.1", 5000)) {
            BufferedReader echoes = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter stringToEcho = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            String echoString;
            String response;

            //-Reading output from server and saving it
            //-Error of fatal close, string left null which faults echoer on server side
            BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String savedServerResponse = dIn.readLine();
            System.out.println("--Broker Connected--\n" + savedServerResponse);
            BrokerAccount.brokerCoverID = 1;
            
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
                    sbMessage.append(echoString);
                    //-Sends message to echoer
                    stringToEcho.println(sbMessage.toString());
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
                    sbMessage.append(echoString);
                    //if (Successfull)
                    //  Transfers value from broker --> market
                    BrokerFunctions.brokerSellSuccess(sbMessage.toString());
                    //-Sends message to echoer
                    stringToEcho.println(sbMessage.toString());
                }
                else if (echoString.equals("listm")) {
                    brokerMessageType = "3";
                    stringToEcho.println(brokerMessageType);
                }
                else if (echoString.equals("listg")) {
                    System.out.println("__/Your Account/__" + "\nSilver: " + BrokerAccount.accountSilver + 
                    "\nGold: " + BrokerAccount.accountGold+ "\nPlatinum: " + BrokerAccount.accountPlatinum + 
                    "\nFuel: " + BrokerAccount.accountFuel + "\nBitcoin: " + BrokerAccount.accountBitcoin + 
                    "\nCapital :" + BrokerAccount.capital);
                    stringToEcho.println(echoString);
                }
                else {
                    //-Prints from echoer what has been written
                    stringToEcho.println(echoString);
                }
                if (!echoString.equals("exit")) {
                    response = echoes.readLine();
                    if (response.equals("Purchase Successful"))
                        BrokerFunctions.brokerBuySuccess(sbMessage.toString());
                    if (response.equals("Sale Successful"))
                        BrokerFunctions.brokerSellSuccess(sbMessage.toString());
                    else
                        System.out.println(response);
                }
            } while (!echoString.equals("exit"));

            scanner.close();
            System.out.println("Connection Closed");

        } catch(IOException e) {
            System.out.println("Broker Error: " + e.getMessage());
        }
    }
}