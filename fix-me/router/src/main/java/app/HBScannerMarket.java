package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Calendar;

public class HBScannerMarket extends Thread{
    private Socket socket;

    public HBScannerMarket(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            HBTimeOutMarket hbTimeOutMarket = new HBTimeOutMarket();
            hbTimeOutMarket.start();

            while (true) {
                String echoString = dIn.readLine();
                Calendar cal = Calendar.getInstance();
                int seconds = cal.get(Calendar.SECOND);
                if (echoString == null) {
                    break;
                }
                String[] echoStringParts = echoString.split("-");
                if (echoStringParts[0].equals("HBM")) {
                    Server.mapHBMarket.put(echoStringParts[1], seconds);
                    // System.out.println("-√v^√v^√❤ Received-" + echoStringParts[1]);
                    // System.out.println( "Seconds in current minute = " + seconds);
                    System.out.println("::::: Market HB Map ::::: " + Server.mapHBMarket);
                    System.out.println(":::::  MNormal Map  ::::: " + Server.mapMarket);
                }
            }
        } catch(IOException e) {
            System.out.println("Oops: " + e.getMessage());
        } catch(Exception e) {
            System.out.println("HeartBeat Server exception " + e.getMessage());
        }
    }
}