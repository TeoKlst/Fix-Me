package app;

import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

public class HBTimeOutMarket extends Thread{

    public static void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        Calendar cal = Calendar.getInstance();
        int seconds = cal.get(Calendar.SECOND);
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            int keyValue = (Integer) pair.getValue();
            int val = keyValue < seconds ? seconds - keyValue : keyValue - seconds;
            if (val > 5)
                it.remove(); // avoids ConcurrentModificationException
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(5000);
                Calendar cal = Calendar.getInstance();
                int seconds = cal.get(Calendar.SECOND);

                String index = "base";
                int i = 0;

                // printMap(Server.mapHBMarket);

                for (String key : Server.mapHBMarket.keySet()) {
                    int val = Server.mapHBMarket.get(key) < seconds ? seconds - Server.mapHBMarket.get(key) : Server.mapHBMarket.get(key) - seconds;
                    if (val > 5) {
                        index = "," + key;
                    }
                    else {}
                }
                String parts[] = index.split(",");
                while (i != parts.length) {
                    Server.mapHBMarket.remove(parts[i]);
                    Server.mapMarket.remove(parts[i]);
                    i++;
                }
            }
        } catch(ConcurrentModificationException e) {
            System.out.println("HB Modification exception " + e.getMessage());
        } catch (InterruptedException e) {
			System.out.println("HB Interrupted " + e.getMessage());
		}
    }
}