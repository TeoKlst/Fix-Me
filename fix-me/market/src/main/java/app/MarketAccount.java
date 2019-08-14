package app;

import java.util.concurrent.ThreadLocalRandom;

public class MarketAccount {
    public static int marketID;

    private static int min = 0;
    private static int max = 100;
    
    private static int randomNumberGenerate() {
        int randomNum = ThreadLocalRandom.current().nextInt(min, max);
        return (randomNum);
    }

    public static int silver = randomNumberGenerate();
    public static int gold = randomNumberGenerate();
    public static int platinum = randomNumberGenerate();
    public static int fuel = randomNumberGenerate();
    public static int bitcoin = randomNumberGenerate();
    public static int capital = 10000;
}