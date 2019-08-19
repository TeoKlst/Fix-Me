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
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                String echoString = input.readLine();
                String[] echoStringParts = echoString.split("-");
                //- java.lang.NullPointerException on this-line ⬆ when I close terminal withouting writing exit on broker
                if (echoStringParts[0].equals("exit")) {
                    break;
                }
                //-Buy from market
                if (echoStringParts[0].equals("1")) {
                    output.println("Message from Broker -> Buy Market => Data: " + echoString);
                }
                //-Sell from market
                if (echoStringParts[0].equals("2")) {
                    output.println("Message from Broker -> Sale Market => Data: " + echoString);
                }
                //-List Markets
                if (echoString.equals("3")) {
                    output.println("Market => " + Server.mapMarket);
                }
                else
                    output.println("Echo from server :" + echoString);
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
