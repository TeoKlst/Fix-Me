package app;

import java.net.Socket;

public class MarketHBSender extends Thread {
    private Socket socket;
  
    public MarketHBSender(Socket socket) {
        this.socket = socket;
    }

}