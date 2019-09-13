package app;

import app.fix.FixProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class Broker {
    public static FixProtocol fixProtocol;

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", 5000)) {

            fixProtocol = new FixProtocol(Integer.toString(BrokerAccount.brokerServiceID));

            BrokerHBSender brokerHBSender = new BrokerHBSender(socket);
            brokerHBSender.start();

            BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter dOut = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            String echoString;
            String response;

            String savedServerResponse = dIn.readLine();
            BrokerFunctions.assignRouteServiceID(savedServerResponse);
            System.out.println("--Broker Connected--\n" +
            "You are Broker[" + BrokerAccount.brokerRouteID + "]" + " ServiceID => " + BrokerAccount.brokerServiceID);

            do {
                String fixMessage = null;

                System.out.println("Buy, Sell, List Markets or Display your goods:");
                echoString = scanner.nextLine().toLowerCase();

                if (echoString.equals("buy")) {
                    System.out.println("Choose Market ID:");
                    String marketID = scanner.nextLine().toLowerCase();
                    System.out.println("Choose Item ID to purchase:");
                    String itemID = scanner.nextLine().toLowerCase();
                    System.out.println("Choose purchase Amount:");
                    String purchaseAmount = scanner.nextLine().toLowerCase();
                    System.out.println("Choose purchase Price:");
                    String purchasePrice = scanner.nextLine().toLowerCase();
                    String brokerRouteID = Integer.toString(BrokerAccount.brokerRouteID);
                    fixMessage = fixProtocol.PurchaseMessage(marketID, itemID, purchaseAmount, purchasePrice, brokerRouteID);
                    if (BrokerFunctions.brokerPurchaseValidate(purchasePrice, marketID)) {
                        dOut.println(fixMessage);
                    }
                    else {
                        echoString = "error_1";
                        System.out.println("ERROR: account purchase input");
                    }
                }
                else if (echoString.equals("sell")) {
                    System.out.println("Choose Market ID:");
                    String marketID = scanner.nextLine().toLowerCase();
                    System.out.println("Choose Item ID to sell:");
                    String itemID = scanner.nextLine().toLowerCase();
                    System.out.println("Choose sale Amount:");
                    String saleAmount = scanner.nextLine().toLowerCase();
                    System.out.println("Choose sale Price:");
                    String salePrice = scanner.nextLine().toLowerCase();
                    String brokerRouteID = Integer.toString(BrokerAccount.brokerRouteID);
                    fixMessage = fixProtocol.SaleMessage(marketID, itemID, saleAmount, salePrice, brokerRouteID);
                    if (BrokerFunctions.brokerSaleValidate(saleAmount, itemID, marketID)) {
                        dOut.println(fixMessage);
                    }
                    else {
                        echoString = "error_2";
                        System.out.println("ERROR: account sale input");
                    }
                }
                else if (echoString.equals("listm")) {
                    fixMessage = fixProtocol.ListMarket(BrokerAccount.brokerRouteID);
                    dOut.println(fixMessage);
                }
                else if (echoString.equals("listg") || echoString.equals("list goods")) {
                    BrokerFunctions.brokerGetDataBroker();
                }
                // Not allowed to query market 0
                else if (echoString.equals("listmg") || echoString.equals("list market goods")) {
                    System.out.println("Choose Market ID to view (its) goods:");
                    String marketID = scanner.nextLine().toLowerCase();
                    String brokerRouteID = Integer.toString(BrokerAccount.brokerRouteID);
                    fixMessage = fixProtocol.MarketQuery(marketID, brokerRouteID);
                    dOut.println(fixMessage);
                } else if (echoString.equals("logon")) {
                    fixMessage = fixProtocol.logonMessage(120);
                    dOut.println(fixMessage);
                }
                else {
                    dOut.println(fixProtocol.DefaultNoType(BrokerAccount.brokerRouteID));
                }
                if (!echoString.equals("exit")) {
                    if (!echoString.equals("listg") && !echoString.equals("list goods")
                        && !echoString.equals("error_1") && !echoString.equals("error_2")) {
                        response = dIn.readLine();
                        Thread.sleep(1000);
                        String responseType = fixProtocol.getMsgType(response);
                        if (responseType.equals("AK")) {
                            if (fixProtocol.getTransactionState(response).equals("1")) {
                                BrokerFunctions.brokerBuySuccess(fixMessage);
                                System.out.println("Purchase Successful");
                            }
                            if (fixProtocol.getTransactionState(response).equals("2")) {
                                BrokerFunctions.brokerSellSuccess(fixMessage);
                                System.out.println("Sale Successful");
                            }
                        }
                        else if (responseType.equals("4"))
                            System.out.println("Transaction Failed");
                        else if (responseType.equals("7"))
                            BrokerFunctions.brokerReceiveDataMarket(response);
                        else if (responseType.equals("91"))
                            System.out.println("ERROR: Market does not exist!");
                        else if (responseType.equals("60"))
                            BrokerFunctions.getMarketList(response);
                        else
                            System.out.println(response);
                    }
                }
            } while (!echoString.equals("exit"));

            brokerHBSender.interrupt();
            scanner.close();
            System.out.println("Connection Closed");

        } catch(IOException e) {
            System.out.println("Broker Error: " + e.getMessage());
        }
    }
}
