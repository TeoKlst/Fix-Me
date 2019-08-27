package app;

import app.fix.FixProtocol;
import app.fix.exceptions.InvalidMsgTypeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// A market has a list of instruments that can be traded.
// When orders are received from brokers the market tries to execute it. If the execution is successfull,
// it updates the internal instrument list and sends the broker an Executed message. If the order can’t be
// met, the market sends a Rejected message.
// The rules by which a market executes orders can be complex and you can play with
// them. This is why you build the simulator. Some simple rules that you need to respect
// is that an order can’t be executed if the instrument is not traded on the market or if the
// demanded quantity is not available (in case of Buy orders).

class Market {
    private static FixProtocol      fixProtocol;

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
        // new Socket("localhost", 5001) <- should also work with "localhost" string
        try (Socket socket = new Socket("127.0.0.1", 5001)) {
            BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter dOut = new PrintWriter(socket.getOutputStream(), true);
            
            String echoString;
            String response;

            //-Reading output from server and saving it
            String savedServerResponse = dIn.readLine();
            MarketFunctions.assignRouteServiceID(savedServerResponse);
            System.out.println("--Market Connected--\n" + 
            "Market[" + MarketAccount.marketRouteID + "]" + " ServiceID => " + MarketAccount.marketServiceID);
            fixProtocol = new FixProtocol("" + MarketAccount.marketServiceID);
            do {
                response = null;
                // Thread.sleep(1000);
                echoString = dIn.readLine();
                String[] echoStringParts = echoString.split("-");
                System.out.println(echoString);
                
                if (echoStringParts[0].equals("1"))
                    response = MarketFunctions.brokerPurchaseCheck(echoString);
                else if (echoStringParts[0].equals("2"))
                    response = MarketFunctions.brokerSaleCheck(echoString);
                else if (echoStringParts[0].equals("6"))
                    response = MarketFunctions.marketQuery(echoString);
                else
                    response = "Market Command Error";
                dOut.println(response);   
            } while (!echoString.equals("exit"));

        } catch(IOException e) {
            System.out.println("Market Error: " + e.getMessage());
        }
    }
}