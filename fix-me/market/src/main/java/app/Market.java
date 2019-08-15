package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// import router.BrokerCount;

// A market has a list of instruments that can be traded.
// When orders are received from brokers the market tries to execute it. If the execution is successfull,
// it updates the internal instrument list and sends the broker an Executed message. If the order can’t be
// met, the market sends a Rejected message.
// The rules by which a market executes orders can be complex and you can play with
// them. This is why you build the simulator. Some simple rules that you need to respect
// is that an order can’t be executed if the instrument is not traded on the market or if the
// demanded quantity is not available (in case of Buy orders).

class Market {
    public static void main(String[] args) throws Exception {
        // new Socket("localhost", 5001) <- should also work with "localhost" string
        try (Socket socket = new Socket("127.0.0.1", 5001)) {
            BufferedReader echoes = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter stringToEcho = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
            
            String echoString;
            String response;

            // BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // String savedServerResponse = dIn.readLine();
            // System.out.println("--Market Connected--\n" + savedServerResponse);
            System.out.println("--Market Connected--");
            do {
                response = echoes.readLine();
                // Continual loop looking for input
                // Runs purchase with its check function
                // Sends Back if transaction was successful
                echoString = response;
                // echoString = echoes.readLine();
                /*
                GET MESSAGE FROM SERVER

                if (Message == Purchase -> Purchase Func Called)
                    MarketFunctions.brokerPurchaseCheck(echoString);
                else if (Message == Sale -> Sale Func Called)
                    MarketFunctions.brokerSaleCheck(echoString);
                */
                
                stringToEcho.println(echoString);
                if (!echoString.equals("exit")) {
                    response = echoes.readLine();
                    System.out.println(response);
                }
            } while (!echoString.equals("exit"));

            scanner.close();

        } catch(IOException e) {
            System.out.println("Market Error: " + e.getMessage());
        }
    }
}