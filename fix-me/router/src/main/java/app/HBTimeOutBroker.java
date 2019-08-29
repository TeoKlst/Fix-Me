package app;

import java.util.Calendar;
import java.util.ConcurrentModificationException;

public class HBTimeOutBroker extends Thread {
    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(5000);
                Calendar cal = Calendar.getInstance();
                int seconds = cal.get(Calendar.SECOND);
                String index = "base";
                int i = 0;

                for (String key : Server.mapHBBroker.keySet()) {
                    int val = Server.mapHBBroker.get(key) < seconds ? seconds - Server.mapHBBroker.get(key) : Server.mapHBBroker.get(key) - seconds;
                    if (val > 5) {
                        index = "," + key;
                    }
                    else {}
                }
                String parts[] = index.split(",");
                while (i != parts.length) {
                    Server.mapHBBroker.remove(parts[i]);
                    Server.mapBroker.remove(parts[i]);
                    i++;
                }
            }
        } catch(ConcurrentModificationException e) {
            System.out.println("HB Modification exception " + e.getMessage());
        } catch (InterruptedException e) {
			System.out.println("HB Broker Interrupted " + e.getMessage());
		}
    }
}