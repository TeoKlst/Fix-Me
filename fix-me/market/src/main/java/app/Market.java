package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// A market has a list of instruments that can be traded.
// When orders are received from brokers the market tries to execute it. If the execution is successfully,
// it updates the internal instrument list and sends the broker an Executed message. If the order can’t be
// met, the market sends a Rejected message.
// The rules by which a market executes orders can be complex and you can play with
// them. This is why you build the simulator. Some simple rules that you need to respect
// is that an order can’t be executed if the instrument is not traded on the market or if the
// demanded quantity is not available (in case of Buy orders).

class Market {
    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", 5001)) {
            //-Starts Market HeartBeat
            MarketHBSender marketHBSender = new MarketHBSender(socket);
            marketHBSender.start();

            BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter dOut = new PrintWriter(socket.getOutputStream(), true);
            
            String echoString;
            String response;

            String savedServerResponse = dIn.readLine();
            MarketFunctions.assignRouteServiceID(savedServerResponse);
            System.out.println("--Market Connected--\n" + 
            "Market[" + MarketAccount.marketRouteID + "]" + " ServiceID => " + MarketAccount.marketServiceID);

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

            marketHBSender.interrupt();

        } catch(IOException e) {
            System.out.println("Market Error: " + e.getMessage());
        }
    }
}