package app;

import java.util.concurrent.ThreadLocalRandom;

public class BrokerAccount {
    public static int brokerServiceID;
    public static int brokerRouteID;
    public static String[] parts;

    private static int min = 0;
    private static int max = 100;
    
    private static int randomNumberGenerate() {
        int randomNum = ThreadLocalRandom.current().nextInt(min, max);
        return (randomNum);
    }

    public static int accountSilver = randomNumberGenerate();
    public static int accountGold = randomNumberGenerate();
    public static int accountPlatinum = randomNumberGenerate();
    public static int accountFuel = randomNumberGenerate();
    public static int accountBitcoin = randomNumberGenerate();
    public static int capital = 10000;
}