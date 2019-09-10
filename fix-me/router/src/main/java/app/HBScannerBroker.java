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

            while (true) {
                String echoString = dIn.readLine();
                Calendar cal = Calendar.getInstance();
                int seconds = cal.get(Calendar.SECOND);
                if (echoString == null) {
                    break;
                }
                String[] echoStringParts = echoString.split("-");
                if (echoStringParts[0].equals("HBB")) {
                    Server.mapHBBroker.put(echoStringParts[1], seconds);
                    System.out.println("HBBroker Map=>" + Server.mapHBBroker);
                }
            }
        } catch(IOException e) {
            System.out.println("Oops: " + e.getMessage());
        } catch(Exception e) {
            System.out.println("HeartBeat Server exception " + e.getMessage());
        }
    }
}