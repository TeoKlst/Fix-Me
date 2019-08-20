package app;

import java.io.PrintWriter;
import java.net.Socket;

// public class HeartBeat extends Thread{

//     public static Boolean hearbeatmessage = true;
  
//     Socket clientSocket = new Socket();
//     PrintWriter dOut = new PrintWriter(clientSocket.getOutputStream(), true);

//     public void receiver(Socket clientsocket) {
//         clientSocket = clientsocket;
//         dOut = new PrintWriter(clientSocket.getOutputStream(), true);
//     }
  
//     public void run(){
//       while(true) {
//         if(heartbeatmessage) {
//           thread.sleep(10000);
//           dOut.println("heartbeat");
//         }
//       }
//     }
// }

// import java.net.*;
// import java.io.*;

// public class HeartBeat extends Thread
// {// sends a heartbeat message to the multicast group every 60 seconds
//     public HeartBeat (MulticastSocket Csock, InetAddress maddr, int port, Players ulist)
//     { 
//         this.ulist = ulist;
//         this.Csock = Csock;
//         this.maddr= maddr;
//         this.port = port;
//     }
    
//     public static Players ulist;
//     public static MulticastSocket Csock;
//     public static InetAddress maddr;
//     public static int port;
//     private DatagramPacket hbMsg ;
//     static private long TmHB = 60000;  //heartbeat frequency in milliseconds
    
//     public void run(){
//         // setup the hb datagram packet then run forever
//         // setup the line to ignore the loopback we want to get it too
//         String line = "5|";
        
//         hbMsg = new DatagramPacket(line.getBytes(), line.length(), maddr, port);
                                                   
//         // continually loop and send this packet every TmHB seconds
//         while (true) {
//             try {
//                 Csock.send(hbMsg);
//                 sleep(TmHB);
//               } catch (IOException e) {
//                 System.err.println("Server can't send heartbeat");
//                 System.exit(-1);
//             } catch (InterruptedException e){}
//         }
//     }// end run
    
//       static public void  doHbCheck()
//    {// checks the HB status of all users and send leave if > 5
//         int hb;
//         // first increment the hb of all
//         ulist.incHB();        
//         // now keep removing all over 5
//         // checkHB() returns the ip+port token or null if done
//         String test = ulist.checkHB();
//         while (test !=null){
//                 // send a leave message for this client 
// 	            String line = "3|"+test; //leave flag plus user
// 	            DatagramPacket lvMsg = new DatagramPacket(line.getBytes(),
//                                    line.length(),
//                                    maddr,
//                                    port);
//                 try{
//                     Csock.send(lvMsg);
//                 }
//                 catch (IOException e) {} 
//                 test=ulist.checkHB();
//             }
//    }
   
// }// end class