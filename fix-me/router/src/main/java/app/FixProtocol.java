package app;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class FixProtocol {
    private String     username;
    private String     password;
    private String     test;

    public FixProtocol(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public String       checksumGenerator(String messageInput) {
        //Replace | with the ascii value of 1 (a non-printable character) to ensure the correct byte size
        String message = messageInput.replace('|', '\u0001');
        //Put the message in ascii byte value into a byte array
        byte[] messageBytes = message.getBytes(StandardCharsets.US_ASCII);
        //Iterate through byte array and add each bytes value to get the total byte size of the message
        int total = 0;
        for (int i = 0; i < message.length(); i++) {
            total += messageBytes[i];
        }
        //Modulous by 256 to get what the checksum value should be
        Integer checksumResult = total % 256;

        //For testing. To delete.
        System.out.println("Size of message in bytes: " + total);
        System.out.println("Checksum: " + checksumResult);
        //

        //Get's the correct padding
        String checksumStr = "000".substring(checksumResult.toString().length()) + checksumResult.toString();

        //To delete
        System.out.println("Checksum String: " + checksumStr);
        //

        return checksumStr;
    }
    
    public boolean      fixMessageValidator(String input) {
        // Reference: https://gigi.nullneuron.net/gigilabs/calculating-the-checksum-of-a-fix-message/
        
        //Use to test:
        /*
        * FixProtocol fixProtocol = new FixProtocol();
        * fixProtocol.fixMessageValidator("8=FIX.4.1|9=61|35=A|34=1|49=EXEC|52=20121105-23:24:06|56=BANZAI|98=0|108=30|10=003|");
        */

        //Separates the checksum from the message
        String[] values = input.split("\\|10=");

        //Make sure that there are only 2 strings: the input and the checksum
        if (values.length != 2) {
            System.out.println("Split failed");
            return false;
        }
        //Add the pipe back to the first string to end the input
        values[0] += '|';
        
        String checksumStr = checksumGenerator(values[0]);

        if (!checksumStr.equals(values[1].substring(0,3))) {
            return false;
        }
        System.out.println("String valid");
        return true;
    }

    public String       logonMessage(HashMap<String, String> object) {
        StringBuilder body = new StringBuilder();

        /* Define a message encryption scheme
         * Valid value is "0" = NONE+OTHER (encryption is not used)
        */
        body.append("98=0|");

        /*
         * Heartbeat interval in seconds.
         * The Heartbeat monitors the status of the communication link and identifies when the last of a string of messages was not received.
         * When either end of the FIX connection has not sent any data for HeartBtInt seconds, it will transmit a Heartbeat message.
         * 
         * Value is set in the 'config.properties' file (client side) as 'SERVER.POLLING.INTERVAL'
         * 30 seconds is default interval value. If HeartBtInt is set to 0 no heartbeat message is required.
         * 
         */
        if (object.containsKey("heartbeat")) {
            body.append("108=" + object.get("heartbeat") + "|");
        } else {
            body.append("108=" + "60" + "|");
        }
        /*
         * Each FIX message has a unique sequence number (MsgSecNum (34) tag) - https://kb.b2bits.com/display/B2BITS/Sequence+number+handling
         * Sequence numbers are initialized at the start of the FIX session starting at 1 (one) and increment through the session
         * 
         * All sides of FIX session should have sequence numbers reset.
         * Valid value is "Y" = Yes (reset)
         * 
         */
        if (object.containsKey("resetSeqNum") && object.get("resetSeqNum").equals("true")) {
            body.append("141=Y|");
        }

        /*
         * The numeric User ID.
         * User is linked to SenderCompID (#49) value (the user's organisation)
         */
        body.append("553=" + this.username + "|");

        /*
         * User password.
         */
        body.append("554=" + this.password + "|");

        String header = ConstructHeader();

        String message = header + body.toString() + checksumGenerator(header + body.toString()) + "|";

        System.out.println("Message : " + message);

        return message;
    }

    /*
     * The header is the first part of the FIX message and it is composed of the following fields:
     * 
     * BeginString -> defines the FIX protocol version e.g.FIX4.4
     * BodyLength -> states the length of the message in characters, excluding the BeginString, the BodyLength and the Trailer fields
     * MsgType -> defines the message type, so that the receiver knows how to parse the body
     * SenderComID
     * TargetCompID -> the target of our message
     * SenderSubID -> the trader login
     * MsgSeqNum -> the sequence number of the message. Needs to be increased for each message sent in the same session 
     * Sending Time -> the time of message transmission
     */

    public String ConstructHeader() {
        StringBuilder header = new StringBuilder();

        //Protocol version. Always unencrypted, must be first field in message.
        header.append("8=FIX4.4|");
        return null;
    }
}