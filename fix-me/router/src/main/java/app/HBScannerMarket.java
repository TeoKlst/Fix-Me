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
            String hBType = null;
            while (true) {
                String dINString = dIn.readLine();
                Thread.sleep(250);
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
                    if (message[i].startsWith("560=")) {
                        hBType = message[i].substring(4);
                    }
                }
                if ("2".equals(hBType)) {
                    Server.mapHBMarket.put(marketRouteID, seconds);
                }
                System.out.println("\nHB Market =>" + Server.mapHBMarket.keySet());
                System.out.println("AL Market =>" + Server.mapMarket.keySet());
                System.out.println("\nHB Broker =>" + Server.mapHBBroker.keySet());
                System.out.println("AL Broker =>" + Server.mapBroker.keySet());
            }
        } catch(IOException e) {
            System.out.println("Oops: " + e.getMessage());
        } catch(Exception e) {
            System.out.println("HeartBeat Server exception " + e.getMessage());
        }
    }
}