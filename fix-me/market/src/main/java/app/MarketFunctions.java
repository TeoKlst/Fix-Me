package app;

public class MarketFunctions {

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

    public static void brokerPurchaseCheck(String value) {
        Boolean checkPass = true;

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
            brokerPurchaseExecuted(itemID, purchaseAmount, purchasePrice);
        else
            brokerPurchaseRejected();
    }

    public static void brokerPurchaseExecuted(int itemID, int purchaseAmount, int purchasePrice) {
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
        //Send message to broker saying purchase was successful
    }


    public static void brokerPurchaseRejected() {
        //Send message to broker saying purchase was unsuccessful
    }

    public static void brokerSaleCheck(String value) {
        Boolean checkPass = true;

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
            brokerSaleExecuted(itemID, purchaseAmount, purchasePrice);
        else
            brokerSaleRejected();
    }

    public static void brokerSaleExecuted(int itemID, int purchaseAmount, int purchasePrice) {
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
        //Send message saying sale was successful
    }

    public static void brokerSaleRejected() {
        //Send message saying sale was unsuccessful
    }
    
}