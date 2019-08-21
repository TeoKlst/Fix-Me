package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// All messages will respect the FIX notation.
// All messages will start with the ID assigned by the router and will be ended by the checksum.
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
                if (echoString == null) {
                    break;
                }
                String[] echoStringParts = echoString.split("-");
                //- java.lang.NullPointerException on this-line ⬆ when I close terminal without writing exit on broker
                //- echoString == null Break; Helps Prevent null pointer exception when Broker or Market close unexpectedly
                if (echoString.equals("exit")) {
                    break;
                }
                //Transfers HB to server
                else if (echoStringParts[0].equals("HB")) {
                    Socket hbPort = Server.mapBroker.get("0");
                    output = new PrintWriter(hbPort.getOutputStream(), true);
                    output.println(echoString);
                }
                //-Buy from market
                else if (echoStringParts[0].equals("1")) {
                    Socket marketPort = Server.mapMarket.get(echoStringParts[1]);
                    output = new PrintWriter(marketPort.getOutputStream(), true);
                    output.println(echoString);
                }
                //-Sell from market
                else if (echoStringParts[0].equals("2")) {
                    output.println("Message from Broker -> Sale Market => Data: " + echoString);
                }
                //-List Markets
                else if (echoString.equals("3")) {
                    output.println("Available Market ID's => " + Server.mapMarket.keySet());
                }
                else if (echoStringParts[0].equals("4")) {
                    Socket marketPort = Server.mapBroker.get(Integer.toString(1));
                    output = new PrintWriter(marketPort.getOutputStream(), true);
                    output.println("Purchase Successful");
                }
                else if (echoStringParts[0].equals("5")) {
                    Socket brokerPort = Server.mapBroker.get(Integer.toString(1));
                    output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println("Purchase Failed");
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
