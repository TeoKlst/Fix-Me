package app;

import java.net.Socket;

public class LinkCounter {

        public static int brokerCount = 0;
        public static int marketCount = 0;
        
        public static int serviceID = 100000;

        public static String getBrokerRouteID(Socket s) {
            return Integer.toString(brokerCount);
        }

        public static int countBroker() {
            return brokerCount++;
        }

        public static String getMarketRouteID(Socket s) {
            return Integer.toString(marketCount);
        }

        public static int countMarket() {
            return marketCount++;
        }
    
        public static int generateServiceID() {
            serviceID ++;
            return serviceID;
        }
    
}