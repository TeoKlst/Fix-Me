package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Calendar;

public class HBScannerBroker extends Thread {
    private Socket socket;

    public HBScannerBroker(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            HBTimeOutBroker hbTimeOutBroker = new HBTimeOutBroker();
            hbTimeOutBroker.start();
            String brokerRouteID = null;
            String hBType = null;
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
                        brokerRouteID = message[i].substring(4);
                    }
                    if (message[i].startsWith("560=")) {
                        hBType = message[i].substring(4);
                    }
                }
                if ("1".equals(hBType)) {
                    Server.mapHBBroker.put(brokerRouteID, seconds);
                }
                // System.out.println("\nHB Broker =>" + Server.mapHBBroker.keySet());
                // System.out.println("AL Broker =>" + Server.mapBroker.keySet());
            }
        } catch(IOException e) {
            System.out.println("Oops: " + e.getMessage());
        } catch(Exception e) {
            System.out.println("HeartBeat Server exception " + e.getMessage());
        }
    }
}