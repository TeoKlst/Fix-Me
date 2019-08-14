package app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.BrokerCount;

// • Use non blocking sockets.
// • Use the java executor framework for message handling.
// • Multi-module Mmven build.
// • Implementation of a robust and performant messaging platform.
// • Accept incomming connections from multiple brokers and markets.

// Port 5000 for messages from Broker components. When a Broker establishes the
// connection the Router asigns it a unique 6 digit ID and communicates the ID to
// the Broker.

// Port 5001 for messages from Market components. When a Market establishes the
// connection the Router asigns it a unique 6 digit ID and communicates the ID to
// the Market.

// Brokers and Markets will include the assigned ID in all messages for identification
// and the Router will use the ID to create the routing table.
// Once the Router receives a message it will perform 3 steps:
// • Validate the message based on the checkshum.
// • Identify the destination in the routing table.
// • Forward the message.

// Bonus part
// Bonus points will be given if:
// • You store all transactions in a database
// • You concieve a fail-over mechanismso that ongoing transactions are restored in case one component goes down.

public class MainServer {
    /*
    public static void main(String[] args)throws Exception{
        Selector selector = Selector.open();
        int[] ports = {5000, 5001};

        for (int port : ports) {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));
            // We are only interested when accept events occur on this socket
            server.register(selector, SelectionKey.OP_ACCEPT);
        }

        while (selector.isOpen()) {
            selector.select();
            Set readyKeys = selector.selectedKeys();
            Iterator iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = (SelectionKey) iterator.next();
                if (key.isAcceptable()) {
                    SocketChannel client = ((ServerSocketChannel)key.channel()).accept();
                    // SocketChannel client = server.accept();
                    Socket socket = client.socket();
                    Echoer echoer = new Echoer(socket);
                    echoer.start();
                    // create new thread to deal with connection (closing both socket and client when done)
                }
            }
        }        
    }
    */

    /*
    public static void main(String[] args)throws Exception{
        Selector selector = Selector.open();
        int ports[] = new int[] {5000, 5001};

        // Loop through each port in the list and bind it to a ServerSocketChannel
        for (int port : ports) {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        }

        // Selection key handling proccess
        while (true) {
            selector.select();

            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey selectedKey = selectedKeys.next();

                if (selectedKey.isAcceptable()) {
                    SocketChannel socketChannel = ((ServerSocketChannel) selectedKey.channel()).accept();
                    socketChannel.configureBlocking(false);
                    // 
                    Socket socket = socketChannel.socket();
                    Echoer echoer = new Echoer(socket);
                    echoer.start();
                    // 
                    // switch (socketChannel.socket().getLocalPort()) {
                    switch (socketChannel.socket().getPort()) {
                        case 5000:
                            // handle connection for port 5000
                        case 5001:
                            // handle connection for port 5001
                            break;
                    }
                }
                else if (selectedKey.isReadable()) {
                    //yada yada yada
                }
            }
        }
    }
    */

    private static String getUserNameBroker(Socket s) {
        return Integer.toString(BrokerCount.brokerCount);
    }

    private static String getUserNameMarket(Socket s) {
        return Integer.toString(MarketCount.marketCount);
    }

    //-Broker Hash Map
    public static Map<String, Socket> brokerSockets = new HashMap<String,Socket>();
    //-Market Hash Map
    public static Map<String, Socket> marketSockets = new HashMap<String,Socket>();

    // /*
    public static void main(String[] args) throws Exception {        
        try (ServerSocket serverSocketBroker = new ServerSocket(5000)) {
            System.out.println("--Broker Router Running--");
            while (true) {
                // ----Longer Method----
                // Socket socket = serverSocket.accept();
                // Echoer echoer = new Echoer(socket);
                // echoer.start();
                // ----Longer Method----
                // new Echoer(serverSocketBroker.accept()).start();
                
                Socket socket = serverSocketBroker.accept();
                Echoer echoer = new Echoer(socket);
                echoer.start();

                //-Broker Saved in Hash Map
                BrokerCount.brokerCount = BrokerCount.brokerCount + 1;
                String username = getUserNameBroker(socket);
                brokerSockets.put(username, socket);
                System.out.println("Broker[" + BrokerCount.brokerCount + "] connected");
                System.out.println(brokerSockets);
                // brokerSockets.remove() to remove from hashmap

                //-Send message to broker
                Socket brokerPort = brokerSockets.get(Integer.toString(BrokerCount.brokerCount));
                PrintWriter output = new PrintWriter(brokerPort.getOutputStream(), true);
                output.println("You are broker: " + BrokerCount.brokerCount);
            }

        } catch(IOException e) {
            System.out.println("Server exception " + e.getMessage());
        }
    }
    // */
}