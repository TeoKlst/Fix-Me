package app;

public class BrokerFunctions {

    public static void brokerBuySuccess(String value) {
        String[] parts = value.split("-");
        int marketID = Integer.parseInt(parts[1]);
        int itemID = Integer.parseInt(parts[2]);
        int purchaseAmount = Integer.parseInt(parts[3]);
        int purchasePrice = Integer.parseInt(parts[4]);
        
        if (itemID == 1)
            BrokerAccount.accountSilver += purchaseAmount;
        else if (itemID == 2)
            BrokerAccount.accountGold += purchaseAmount;
        else if (itemID == 3)
            BrokerAccount.accountPlatinum += purchaseAmount;
        else if (itemID == 4)
            BrokerAccount.accountFuel += purchaseAmount;
        else if (itemID == 5)
            BrokerAccount.accountBitcoin += purchaseAmount;
            
        BrokerAccount.capital -= purchasePrice;
    }

    public static void brokerSellSuccess(String value) {
        String[] parts = value.split("-");
        int marketID = Integer.parseInt(parts[1]);
        int itemID = Integer.parseInt(parts[2]);
        int purchaseAmount = Integer.parseInt(parts[3]);
        int purchasePrice = Integer.parseInt(parts[4]);
        
        if (itemID == 1)
            BrokerAccount.accountSilver -= purchaseAmount;
        else if (itemID == 2)
            BrokerAccount.accountGold -= purchaseAmount;
        else if (itemID == 3)
            BrokerAccount.accountPlatinum -= purchaseAmount;
        else if (itemID == 4)
            BrokerAccount.accountFuel -= purchaseAmount;
        else if (itemID == 5)
            BrokerAccount.accountBitcoin -= purchaseAmount;

        BrokerAccount.capital += purchasePrice;
    }
}