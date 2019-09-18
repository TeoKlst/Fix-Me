package app;

import java.util.Calendar;
import java.util.ConcurrentModificationException;

public class HBTimeOutMarket extends Thread {

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(5000);
                Calendar cal = Calendar.getInstance();
                int seconds = cal.get(Calendar.SECOND);
                String index = "";
                int i = 0;

                for (String key : Server.mapHBMarket.keySet()) {
                    int val = Server.mapHBMarket.get(key) < seconds ? seconds - Server.mapHBMarket.get(key) : Server.mapHBMarket.get(key) - seconds;
                    if (val > 5) {
                        index = key + ",";
                    }
                    else {}
                }
                String parts[] = index.split(",");
                while (i != parts.length) {
                    Server.mapHBMarket.remove(parts[i]);
                    Server.mapMarket.remove(parts[i]);
                    i++;
                }
                System.out.println("\nHB Market =>" + Server.mapHBMarket.keySet());
                System.out.println("AL Market =>" + Server.mapMarket.keySet());
                System.out.println("\nHB Broker =>" + Server.mapHBBroker.keySet());
                System.out.println("AL Broker =>" + Server.mapBroker.keySet());
            }
        } catch(ConcurrentModificationException e) {
            System.out.println("HB Modification exception " + e.getMessage());
        } catch (InterruptedException e) {
			System.out.println("HB Market Interrupted " + e.getMessage());
		}
    }
}