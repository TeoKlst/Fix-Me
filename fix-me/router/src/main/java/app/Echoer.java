package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// All messages will respect the FIX notation.
// All messages will start with the ID asigned by the router and will be ended by the checksum.
// Buy and Sell messages will have the following mandatory fields:
// • Instrument
// • Quantity
// • Market
// • Price

public class Echoer extends Thread {
    private Socket socket;

    public Echoer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            // Socket brokerPort = MainServer.brokerSockets.get(Integer.toString(BrokerCount.brokerCount));
            // output = new PrintWriter(brokerPort.getOutputStream(), true);
            // output.println("You are broker: " + BrokerCount.brokerCount);

            while (true) {
                String echoString = input.readLine();
                String[] echoStringParts = echoString.split("-");
                //- java.lang.NullPointerException on this line when I close terminal withouting writing exit on broker
                if (echoStringParts[0].equals("exit")) {
                    break;
                }
                //-Buy from market
                else if (echoStringParts[0].equals("1")) {
                    // Socket brokerPort = MainServer.marketSockets.get("1");
                    // output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println("Message from Broker -> Buy Market => Data: " + echoString);
                }
                //-Sell from market
                else if (echoStringParts[0].equals("2")) {
                    // Socket brokerPort = MainServer.marketSockets.get("1");
                    // output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println("Message from Broker -> Sale Market => Data: " + echoString);
                }
                //-List Markets
                else if (echoString.equals("3")) {
                    Socket brokerPort = MainServer.brokerSockets.get("1");
                    output = new PrintWriter(brokerPort.getOutputStream(), true);
                    int key = 1;
                    while (key <= MainServer.marketSockets.size()) {
                        output.println("Market => " + MainServer.marketSockets.get(Integer.toString(key)));
                        key++;
                    }
                }
                output.println("Echo from server :" + echoString);
                // Test TKelest Branch Merge
            }
        } catch(IOException e) {
            System.out.println("Oops: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch(IOException e) {
                //-Well fuck it
            }
        }
    }
}
