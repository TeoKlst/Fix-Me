package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class MarketHBSender extends Thread {
    private Socket socket;

    public MarketHBSender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            PrintWriter dOut = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                Thread.sleep(3000);
                dOut.println(Market.fixProtocol.heartBeatMessage(MarketAccount.marketRouteID));
                }
        } catch (IOException e) {
            System.out.println("Oops, MarketHeartBeat Send Error: " + e.getMessage());
        } catch (InterruptedException te) {
            System.out.println("Market HeartBeat Stopped");
            // System.out.println("Oops, BrokerHeartBeat ThreadSleep Error: " + te.getMessage());
        } finally {
            try {
                socket.close();
            } catch(IOException e) {}
        }
    }
}