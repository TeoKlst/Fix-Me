package app.fix;

import app.fix.exceptions.InvalidChecksumException;
import app.fix.exceptions.InvalidMsgLengthException;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class FixProtocol {
    // https://www.onixs.biz/fix-dictionary/4.4/msgs_by_category.html
    private String     userID;
    private int         msgSeqNum;

    public FixProtocol(String userID) {
        this.userID = userID;
        this.msgSeqNum = 0;
    }

    public static boolean 	isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch(NumberFormatException nfe) {
			return false;
		}
		return true;
	}

    public static boolean   isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch(NumberFormatException e) {
			return false;
		} catch(NullPointerException e) {
			return false;
		}
		// only got here if we didn't return false
		return true;
    }
    
    public String           checksumGenerator(String messageInput) {
        //Replace | with the ascii value of 1 (a non-printable character) to ensure the correct byte size
        String message = messageInput.replace('|', '\u0001');
        //Put the message in ascii byte value into a byte array
        byte[] messageBytes = message.getBytes(StandardCharsets.US_ASCII);
        //Iterate through byte array and add each bytes value to get the total byte size of the message
        int total = 0;
        for (int i = 0; i < message.length(); i++) {
            total += messageBytes[i];
        }
        //Modulus by 256 to get what the checksum value should be
        Integer checksumResult = total % 256;

        //Get's the correct padding
        String checksumStr = "000".substring(checksumResult.toString().length()) + checksumResult.toString();

        return checksumStr;
    }
    
    public boolean          checksumValidator(String input) throws InvalidChecksumException {
        // Reference: https://gigi.nullneuron.net/gigilabs/calculating-the-checksum-of-a-fix-message/
        //Separates the checksum from the message
        String[] values = input.split("\\|10=");

        //Make sure that there are only 2 strings: the input and the checksum
        if (values.length != 2) {
			throw new InvalidChecksumException("Invalid Checksum");
        }
        //Add the pipe back to the first string to end the input
        values[0] += '|';
        
        String checksumStr = checksumGenerator(values[0]);

        if (!checksumStr.equals(values[1].substring(0,3))) {
            throw new InvalidChecksumException("Invalid Checksum");
//			return false;
		}
        return true;
    }

    public boolean          msgLengthValidator(String messageInput) throws InvalidMsgLengthException {
        int msgLength = -1;

        //Get the start index of the message to get length
		if (!messageInput.contains("|9=")) {
			throw new InvalidMsgLengthException("Incorrect Message Length");
		}
        int msgIndexStart = messageInput.indexOf("|9=") + 3;
        while (messageInput.charAt(msgIndexStart) != '|') { msgIndexStart++;}   //Gets to the message index to calculate the message length from
        msgIndexStart++;

        //Get the end index of the message to calculate length
        int msgIndexEnd = messageInput.indexOf("|10=");
        msgIndexEnd++;

        //Get message to get length from
        String innerMessage = messageInput.substring(msgIndexStart, msgIndexEnd);

        //Get the given message length
        String[] message = messageInput.split("\\|");
        for (int i=0; i < message.length; i++) {
            if (message[i].startsWith("9=") && isNumeric(message[i].substring(2)) && isInteger(message[i].substring(2))) {
                msgLength = Integer.parseInt(message[i].substring(2));
            }
        }
        if (msgLength < 0 || msgLength != innerMessage.length()) {
            throw new InvalidMsgLengthException("Incorrect Message Length");
        }
        return true;
    }

    //Encryption|UserID|Heartbeat|resetSeqNum|
    public String           logonMessage(int heartbeat) {
        StringBuilder body = new StringBuilder();

        /* 
         * Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
         */
        body.append("98=0|");

        /*
         * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
         */
        body.append("553=" + this.userID + "|");

        /*
         * Heartbeat interval in seconds.
         * The Heartbeat monitors the status of the communication link and identifies when the last of a string of messages was not received.
         * When either end of the FIX connection has not sent any data for HeartBtInt seconds, it will transmit a Heartbeat message.
         * 
         * Value is set in the 'config.properties' file (client side) as 'SERVER.POLLING.INTERVAL'
         * 30 seconds is default interval value. If HeartBtInt is set to 0 no heartbeat message is required.
         * 
         */
        if (heartbeat > 0) {
            body.append("108=" + heartbeat + "|");
        } else {
            body.append("108=" + "120" + "|");
        }

        /*
         * Each FIX message has a unique sequence number (MsgSecNum (34) tag) - https://kb.b2bits.com/display/B2BITS/Sequence+number+handling
         * Sequence numbers are initialized at the start of the FIX session starting at 1 (one) and increment through the session
         * 
         * All sides of FIX session should have sequence numbers reset.
         * Valid value is "Y" = Yes (reset)
         * 
         */
        body.append("141=Y|");
        this.msgSeqNum = 0;
       

        String header = constructHeader(body.toString(), "A"); //Logon = "A"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

    //Encryption|UserID|
	public String       logoutMessage() {
        StringBuilder body = new StringBuilder();

        /* 
         * Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
         */
        body.append("98=0|");

        /*
         * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
         */
        body.append("553=" + this.userID + "|");

        String header = constructHeader(body.toString(), "5"); //Logout = "5"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

    //Encryption|UserID|Heartbeat|resetSeqNum
    public String       heartBeatMessage() {
        StringBuilder body = new StringBuilder();

        /* 
         * Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
         */
        body.append("98=0|");

        /*
         * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
         */
        body.append("553=" + this.userID + "|");

        String header = constructHeader(body.toString(), "0"); //Heartbeat = "0"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";


        return message;
    }

    //Encryption|UserID|RefSeqNum|sessionRejectReason|Text
    public String       RejectMessage(int refSeqNum, int sessionRejectReason, String text) {
        StringBuilder body = new StringBuilder();

        /* 
         * Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
         */
        body.append("98=0|");

        /*
         * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
         */
        body.append("553=" + this.userID + "|");

        /*
         * Reference to the Message Sequence Number that was rejected
         */
        body.append("45=" + refSeqNum + "|");

        /*
         * Setting the sessionRejectionReason value, as well as adding text to explain further
         */
        if ((sessionRejectReason >= 0 && sessionRejectReason <= 17)) {
            body.append("373=" + sessionRejectReason + "|");
        } else if (sessionRejectReason == 99) {
            body.append("373=" + sessionRejectReason + "|");
        } else {
            System.out.println("Invalid rejection value entered.");
            return null;
        }
        if (text != null && !text.isEmpty()) {
            body.append("58=" + text + "|");
        }

        String header = constructHeader(body.toString(), "3"); //Reject = "3"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

   //Protocol Version|length|Message Type|Message Sequence Number|Date|
   public String            constructHeader(String bodyMessage, String type) {
        StringBuilder header = new StringBuilder();

        //Protocol version. Always unencrypted, must be first field in message.
        header.append("8=FIX4.4|");

        StringBuilder message = new StringBuilder();

        //Message type. Always unencrypted, must be the third field in message.
        message.append("35=" + type + "|");

        //Message Sequence Number
        this.msgSeqNum++;       //Message sequence number starts at 1
        message.append("34=" + this.msgSeqNum + "|");

        //Time of message transmission (always expressed in UTC (Universal Time Coordinated), also known as 'GMT'))
        DateFormat df = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = new Date();
        System.out.println(df.format(date));
        message.append("52=" + df.format(date) + "|");

        //Message body length. Always unencrypted, must be second field in message.
        int length = message.length() + bodyMessage.length();
        header.append("9=" + length + "|");
        header.append(message);

        return header.toString();
    }
   
   public int          validateMessage(String fixMessage) {
    	try {
            checksumValidator(fixMessage);
		} catch (InvalidChecksumException e) {
			return -1; //-1 means bad checksum
		}
    	try {
			msgLengthValidator(fixMessage);

		}catch (InvalidMsgLengthException me) {
			return -2; // -2 means bad message length
		}
	   return 1;
   }

   public void 		receiveMessage(String messageInput) {
		int ret = validateMessage(messageInput);
		if (ret == -1) {
			System.out.println("Checksum invalid");
			int msgSqnNum = -1;
			//Reject through the checksum - first get the msgSequence number
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("34=") && isNumeric(message[i].substring(3)) && isInteger(message[i].substring(3))) {
					msgSqnNum = Integer.parseInt(message[i].substring(3));
				}
			}
			if (msgSqnNum < 1) {
				msgSqnNum = 1;
			}
			String rejectMessage = RejectMessage(msgSqnNum, 99, "InvalidCheckSum");
			System.out.println("Reject message: " + rejectMessage);
		} else if (ret == -2) {
			System.out.println("Message Length invalid");
//			int msgSqnNum = -1;
//			//Reject through the msgLength - first get the msgSequence number
//			String[] message = messageInput.split("\\|");
//			for (int i=0; i < message.length; i++) {
//				if (message[i].startsWith("34=") && isNumeric(message[i].substring(3)) && isInteger(message[i].substring(3))) {
//					msgSqnNum = Integer.parseInt(message[i].substring(3));
//				}
//			}
//			if (msgSqnNum < 1) {
//				msgSqnNum = 1;
//			}
//			String rejectMessage = RejectMessage(msgSqnNum, 99, "InvalidCheckSum");
//			System.out.println("Reject message: " + rejectMessage);
		} else if (ret == 1) {
			System.out.println("Message valid");
		}
   }
   
   
   
   
   
   
   
   
   
   
   
    //Encryption|Heartbeat|resetSeqNum|UserID|              INCORRECT DO NOT USE
    public String       orderMessage(HashMap<String, String> object) {
        // https://www.onixs.biz/fix-dictionary/4.4/msgType_D_68.html
        StringBuilder body = new StringBuilder();

        //Message encryption scheme. "0" = NONE+OTHER (encryption is not used)
        body.append("98=0|");

        //Heartbeat interval in seconds.
        if (object.containsKey("heartbeat")) {
            body.append("108=" + object.get("heartbeat") + "|");
        } else {
            body.append("108=" + "120" + "|");
        }

        /*
         * Each FIX message has a unique sequence number (MsgSecNum (34) tag) - https://kb.b2bits.com/display/B2BITS/Sequence+number+handling
         * Sequence numbers are initialized at the start of the FIX session starting at 1 (one) and increment through the session
         * 
         * All sides of FIX session should have sequence numbers reset.
         * Valid value is "Y" = Yes (reset)
         */
        if (object.containsKey("resetSeqNum") && object.get("resetSeqNum").equals("true")) {
            body.append("141=Y|");
            this.msgSeqNum = 0;
        } else {
            body.append("141=N|");
        }

        /*
         * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
         */
        body.append("553=" + this.userID + "|");

        String header = constructHeader(body.toString(), "A"); //Logon = "A"

        String message = header + body.toString() + checksumGenerator(header + body.toString()) + "|";

        System.out.println("Message : " + message);

        return message;
    }
}