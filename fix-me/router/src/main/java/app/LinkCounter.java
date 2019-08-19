package app;

public class LinkCounter {

        public static int brokerCount = 0;
        public static int marketCount = 0;
        

        public static int serviceID = 100000;

        public static int getBrokerID() {
            return brokerCount;
        }

        public static int countBroker() {
            return brokerCount++;
        }

        public static int getMarketID() {
            return marketCount;
        }
    
        public static int generateServiceID() {
            serviceID ++;
            return serviceID;
        }
    
}