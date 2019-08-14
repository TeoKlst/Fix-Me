package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

// 
// MARKET TEST SERVER CRAP
// 

public class MainServer2 {

    private static String getUserNameMarket(Socket s) {
        return Integer.toString(MarketCount.marketCount);
    }

    public static void main(String[] args) throws Exception {        
        try (ServerSocket serverSocketMarket = new ServerSocket(5001)) {
            System.out.println("--Market Router Running--");
            while (true) {
                Socket socket = serverSocketMarket.accept();
                Echoer echoer = new Echoer(socket);
                echoer.start();

                //Market Saved in Hash Map
                MarketCount.marketCount = MarketCount.marketCount + 1;
                String username = getUserNameMarket(socket);
                MainServer.marketSockets.put(username, socket);
                System.out.println("Market[" + MarketCount.marketCount + "] connected");
                System.out.println(MainServer.marketSockets);

                Socket marketPort = MainServer.marketSockets.get(Integer.toString(MarketCount.marketCount));
                PrintWriter output = new PrintWriter(marketPort.getOutputStream(), true);
                output.println("You are Market: " + MarketCount.marketCount);
            }

        } catch(IOException e) {
            System.out.println("Server exception " + e.getMessage());
        }
    }
}