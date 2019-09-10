package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Calendar;

public class HBScannerMarket extends Thread {
    private Socket socket;

    public HBScannerMarket(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            HBTimeOutMarket hbTimeOutMarket = new HBTimeOutMarket();
            hbTimeOutMarket.start();
            String marketRouteID = null;
            while (true) {
                String dINString = dIn.readLine();
                Calendar cal = Calendar.getInstance();
                int seconds = cal.get(Calendar.SECOND);
                if (dINString == null) {
                    break;
                }

                String[] message = dINString.split("\\|");
                
                for (int i=0; i < message.length; i++) {
                    if (message[i].startsWith("554=")) {
                        marketRouteID = message[i].substring(4);
                    }
                }
                Server.mapHBMarket.put(marketRouteID, seconds);
                System.out.println("HBMarket Map" + Server.mapHBMarket);
            }
        } catch(IOException e) {
            System.out.println("Oops: " + e.getMessage());
        } catch(Exception e) {
            System.out.println("HeartBeat Server exception " + e.getMessage());
        }
    }
}