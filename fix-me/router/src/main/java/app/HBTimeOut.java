package app;

import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

public class HBTimeOut extends Thread{

    public static void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids ConcurrentModificationException
        }
    }

    // need to set it to only to run every 7 seconds

    @Override
    public void run() {
        try {
            while (true) {
                Calendar cal = Calendar.getInstance();
                int seconds = cal.get(Calendar.SECOND);

                for (String key : Server.mapHBBroker.keySet()) {
                    int val = Server.mapHBBroker.get(key) < seconds ? seconds - Server.mapHBBroker.get(key) : Server.mapHBBroker.get(key) - seconds;
                    if (val <= 7) {
                        Server.mapHBBroker.put(key, seconds);
                    }
                    else {
                        // Server.mapHBBroker.remove(key);
                        System.out.println("<=Removed from server=>" + Server.mapBroker.get(key));
                        // Server.mapBroker.remove(key);
                    }
                }
            }
        } catch(ConcurrentModificationException e) {
            System.out.println("HB Modification exception " + e.getMessage());
        }
    }
}