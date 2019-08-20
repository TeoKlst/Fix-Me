package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class BrokerHBReceiver extends Thread{
  private Socket socket;

  public BrokerHBReceiver(Socket socket) {
      this.socket = socket;
  }

  @Override
  public void run() {
      try {
          BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          PrintWriter dOut = new PrintWriter(socket.getOutputStream(), true);

          while (true) {
              String echoString = dIn.readLine();
              if (echoString == null) {
                  break;
              }
              else if (echoString.equals("heartbeat")) {
                System.out.println("-√v^√v^√❤ Received-");
                dOut.println(BrokerAccount.brokerRouteID);
              }
              else
                System.out.println("Server Noise:" + echoString);
          }
      } catch(IOException e) {
          System.out.println("Oops, HeartBeat Error: " + e.getMessage());
      } finally {
          try {
              socket.close();
          } catch(IOException e) {}
      }
  }
}