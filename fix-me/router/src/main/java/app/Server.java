package app;

import java.io.PrintWriter;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

class BrokerSocket implements Runnable {
    public void run() {
        try (ServerSocket serverSocketBroker = new ServerSocket(5000)) {
            System.out.println("--Broker Router Running--");
            while(true) {
                Socket socket = serverSocketBroker.accept();
                Echoer echoer = new Echoer(socket);
                echoer.start();
                // PrintWriter writer=new PrintWriter(sock.getOutputStream());
                // String text="Welcome to Broker Port";
                // writer.println(text);
                // writer.close();
            }
        } catch(Exception e) {
            System.out.println("Broker Server exception " + e.getMessage());
        }
    }
}

class MarketSocket implements Runnable {
    public void run() {
        try (ServerSocket serverSocketMarket = new ServerSocket(5001)) {
            while(true) {
                Socket socket = serverSocketMarket.accept();
                Echoer echoer = new Echoer(socket);
                echoer.start();
                // Socket sock=serverSock.accept();
                // PrintWriter writer=new PrintWriter(sock.getOutputStream());
                // String text="Welcome to Market Port";
                // writer.println(text);
                // writer.close();
            }
        } catch(Exception e) {
            System.out.println("Market Server exception " + e.getMessage());
        }
    }
}

public class Server
{
    public static void main(String[] args)
    {
        BrokerSocket bs = new BrokerSocket();
        MarketSocket ms = new MarketSocket();
        Thread tb=new Thread(bs);
        Thread tm=new Thread(ms);
        tb.start();
        tm.start();
    }
} 