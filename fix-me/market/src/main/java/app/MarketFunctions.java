package app;

public class MarketFunctions {

    public static void assignRouteServiceID(String value) {
        String[] parts = value.split("-");
        MarketAccount.marketRouteID = Integer.parseInt(parts[0]);
        MarketAccount.marketServiceID = Integer.parseInt(parts[1]);
    }

    public static int getMarketItemAmount(int itemID) {
        int amount = 0;
        if (itemID == 1)
            amount = MarketAccount.silver;
        else if (itemID == 2)
            amount = MarketAccount.gold;
        else if (itemID == 3)
            amount = MarketAccount.platinum;
        else if (itemID == 4)
            amount = MarketAccount.fuel;
        else if (itemID == 5)
            amount = MarketAccount.bitcoin;
        return amount;
    }

    public static String brokerPurchaseCheck(String value) {
        Boolean checkPass = true;
        String ret;

        String[] message = value.split("\\|");
        int itemID = 0;
        int purchaseAmount = 0;
        // TODO Check if market has enough money to send to broker
        int purchasePrice = 0;

        for (int i=0; i < message.length; i++) {
            if (message[i].startsWith("100=")) {
                itemID = Integer.parseInt(message[i].substring(3));
            }
            if (message[i].startsWith("101=")) {
                purchaseAmount = Integer.parseInt(message[i].substring(3));
            }
            if (message[i].startsWith("102=")) {
                purchasePrice = Integer.parseInt(message[i].substring(3));
            }
        }

        if (itemID > 5 || itemID < 0)
            checkPass = false;
        else if (purchaseAmount > getMarketItemAmount(itemID) || purchaseAmount < 0)
            checkPass = false;
        if (checkPass)
            return ret = brokerPurchaseExecuted(value);
        else
            return ret = brokerPurchaseRejected(value);
    }

    public static String brokerPurchaseExecuted(String value) {

        String[] message = value.split("\\|");
        int itemID = 0;
        int purchaseAmount = 0;
        int purchasePrice = 0;
        String brokerID = "";

        for (int i=0; i < message.length; i++) {
            if (message[i].startsWith("100=")) {
                itemID = Integer.parseInt(message[i].substring(3));
            }
            if (message[i].startsWith("101=")) {
                purchaseAmount = Integer.parseInt(message[i].substring(3));
            }
            if (message[i].startsWith("102=")) {
                purchasePrice = Integer.parseInt(message[i].substring(3));
            }
            if (message[i].startsWith("554=")) {
                brokerID = message[i].substring(3);
            }
        }

        if (itemID == 1)
            MarketAccount.silver -= purchaseAmount;
        else if (itemID == 2)
            MarketAccount.gold -= purchaseAmount;
        else if (itemID == 3)
            MarketAccount.platinum -= purchaseAmount;
        else if (itemID == 4)
            MarketAccount.fuel -= purchaseAmount;
        else if (itemID == 5)
            MarketAccount.bitcoin -= purchaseAmount;

        MarketAccount.capital += purchasePrice;

        String ret = "4" + "-" + brokerID + "-" + "1";
        return ret;
    }


    public static String brokerPurchaseRejected(String value) {
        String[] parts = value.split("-");
        String brokerID = parts[5];
        String ret = "5" + "-" + brokerID;
        return ret;
    }

    public static String brokerSaleCheck(String value) {
        Boolean checkPass = true;
        String ret;

        String[] parts = value.split("-");
        int marketID = Integer.parseInt(parts[1]);
        int itemID = Integer.parseInt(parts[2]);
        int sellAmount = Integer.parseInt(parts[3]);
        int sellPrice = Integer.parseInt(parts[4]);
        int marketCapital = MarketAccount.capital;

        if (sellPrice > marketCapital)
            checkPass = false;
        else if (itemID > 5 || itemID < 0)
            checkPass = false;
        if (checkPass)
            return ret = brokerSaleExecuted(value);
        else
            return ret = brokerSaleRejected(value);
    }

    public static String  brokerSaleExecuted(String value) {
        String[] parts = value.split("-");
        int itemID = Integer.parseInt(parts[2]);
        int sellAmount = Integer.parseInt(parts[3]);
        int sellPrice = Integer.parseInt(parts[4]);
        String brokerID = parts[5];

        if (itemID == 1)
            MarketAccount.silver += sellAmount;
        else if (itemID == 2)
            MarketAccount.gold += sellAmount;
        else if (itemID == 3)
            MarketAccount.platinum += sellAmount;
        else if (itemID == 4)
            MarketAccount.fuel += sellAmount;
        else if (itemID == 5)
            MarketAccount.bitcoin += sellAmount;

        MarketAccount.capital -= sellPrice;
        
        String ret = "4" + "-" + brokerID + "-" + "2";
        return ret;
    }

    public static String brokerSaleRejected(String value) {
        String[] parts = value.split("-");
        String brokerID = parts[5];
        String ret = "5" + "-" + brokerID;
        return ret;
    }

    public static String marketQuery(String value) {
        String[] parts = value.split("-");
        int brokerID = Integer.parseInt(parts[2]);

        String ret = "7" + "-" + MarketAccount.marketRouteID  + "-" + brokerID +
        "-" + MarketAccount.silver + "-" + MarketAccount.gold + "-" + MarketAccount.platinum +
        "-" + MarketAccount.fuel + "-" + MarketAccount.bitcoin + "-" + MarketAccount.capital;
        return ret;
    }
    
}