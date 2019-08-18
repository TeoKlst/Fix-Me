package app;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;

public class ClientTest {
    public static void main(String[] args) {
        try {
            Socket s=new Socket("127.0.0.1", 5000);
            InputStreamReader streamReader=new InputStreamReader(s.getInputStream());
            BufferedReader reader=new BufferedReader(streamReader);
            String text=reader.readLine();
            System.out.println(text);
            reader.close();
        } catch(Exception e) {
            System.out.println("Error");
        }
    }
}