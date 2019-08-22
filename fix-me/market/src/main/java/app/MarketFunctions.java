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

        String[] parts = value.split("-");
        int marketID = Integer.parseInt(parts[1]);
        int itemID = Integer.parseInt(parts[2]);
        int purchaseAmount = Integer.parseInt(parts[3]);
        int purchasePrice = Integer.parseInt(parts[4]);

        if (purchaseAmount > getMarketItemAmount(itemID) && purchaseAmount < getMarketItemAmount(itemID))
            checkPass = false;
        else if (itemID > 5 || itemID < 0)
            checkPass = false;
        if (checkPass)
            return ret = brokerPurchaseExecuted(value);
        else
            return ret = brokerPurchaseRejected(value);
    }

    public static String brokerPurchaseExecuted(String value) {
        String[] parts = value.split("-");
        int itemID = Integer.parseInt(parts[2]);
        int purchaseAmount = Integer.parseInt(parts[3]);
        int purchasePrice = Integer.parseInt(parts[4]);
        String brokerID = parts[5];

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
        String ret = "5" + brokerID;
        return ret;
    }

    public static String brokerSaleCheck(String value) {
        Boolean checkPass = true;
        String ret;

        String[] parts = value.split("-");
        int marketID = Integer.parseInt(parts[1]);
        int itemID = Integer.parseInt(parts[2]);
        int purchaseAmount = Integer.parseInt(parts[3]);
        int purchasePrice = Integer.parseInt(parts[4]);

        if (purchaseAmount > getMarketItemAmount(itemID) || purchaseAmount < getMarketItemAmount(itemID))
            checkPass = false;
        else if (itemID > 5 || itemID < 0)
            checkPass = false;
        if (checkPass)
            return ret = brokerSaleExecuted(itemID, purchaseAmount, purchasePrice);
        else
            return ret = brokerSaleRejected();
    }

    public static String  brokerSaleExecuted(int itemID, int purchaseAmount, int purchasePrice) {
        String ret = "4";

        if (itemID == 1)
            MarketAccount.silver += purchaseAmount;
        else if (itemID == 2)
            MarketAccount.gold += purchaseAmount;
        else if (itemID == 3)
            MarketAccount.platinum += purchaseAmount;
        else if (itemID == 4)
            MarketAccount.fuel += purchaseAmount;
        else if (itemID == 5)
            MarketAccount.bitcoin += purchaseAmount;

        MarketAccount.capital -= purchasePrice;
        
        return ret;
    }

    public static String brokerSaleRejected() {
        String ret = "5";
        return ret;
    }
    
}