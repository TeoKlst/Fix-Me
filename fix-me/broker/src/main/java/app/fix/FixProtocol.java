package app.fix;

import app.fix.exceptions.InvalidChecksumException;
import app.fix.exceptions.InvalidMsgLengthException;
import app.fix.exceptions.InvalidMsgTypeException;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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

        // Get Broker/Market Route ID
        public String		    getRouteID(String messageInput) throws InvalidMsgTypeException {
            String msgType = null;
            if (!messageInput.contains("|554=")) {
                // TODO To be able to distinguish if message is required to check for broker or market routeID and give appropriate error return
                throw new InvalidMsgTypeException("Invalid RouteID (Broker/Market) Type");
            }
            String[] message = messageInput.split("\\|");
            for (int i=0; i < message.length; i++) {
               if (message[i].startsWith("554=")) {
                   msgType =message[i].substring(3);
               }
           }
           if (msgType == null) {
               throw new InvalidMsgTypeException("Invalid Message Type");
           }
            return msgType;
       }

        // Get Sale/Purchase success status
        public String		    getTransactionState(String messageInput) throws InvalidMsgTypeException {
        String msgType = null;
        if (!messageInput.contains("|655=")) {
            throw new InvalidMsgTypeException("Invalid Purchase/Sale State");
        }
        String[] message = messageInput.split("\\|");
        for (int i=0; i < message.length; i++) {
            if (message[i].startsWith("655=")) {
                msgType =message[i].substring(4);
            }
        }
        if (msgType == null) {
            throw new InvalidMsgTypeException("Invalid Message Type");
        }
        return msgType;
    }


    // Purchase Message Builder
    public String           PurchaseMessage(String marketID, String itemID, String purchaseAmount,
                                            String purchasePrice, String brokerRouteID) {
        StringBuilder body = new StringBuilder();

        body.append("553=" + this.userID + "|");

        body.append("554=" + brokerRouteID + "|");

        body.append("100=" + itemID + "|");

        body.append("101=" + purchaseAmount + "|");

        body.append("102=" + purchasePrice + "|");

        body.append("103=" + marketID + "|");

        String header = constructHeader(body.toString(), "1"); //Purchase = "1"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }
    
    // Sale Message Builder
    public String           SaleMessage(String marketID, String itemID, String saleAmount,
                                        String salePrice, String brokerRouteID) {
        StringBuilder body = new StringBuilder();

        body.append("553=" + this.userID + "|");

        body.append("554=" + brokerRouteID + "|");

        body.append("100=" + itemID + "|");

        body.append("101=" + saleAmount + "|");

        body.append("102=" + salePrice + "|");

        body.append("103=" + marketID + "|");

        String header = constructHeader(body.toString(), "2"); //Sale = "2"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

    // Default No Type Message Builder
    public String           DefaultNoType(int brokerRouteID) {
        StringBuilder body = new StringBuilder();

        body.append("553=" + this.userID + "|");

        body.append("554=" + brokerRouteID + "|");

        String header = constructHeader(body.toString(), "404"); //Default = "404"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

    // ListMarkets Message Builder
    public String           ListMarket(int brokerRouteID) {
        StringBuilder body = new StringBuilder();

        body.append("553=" + this.userID + "|");

        body.append("554=" + brokerRouteID + "|");

        String header = constructHeader(body.toString(), "60"); //ListMarkets = "60"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

    // MarketQuery Message Builder
    public String           MarketQuery(String marketID, String brokerRouteID) {
        StringBuilder body = new StringBuilder();

        body.append("553=" + this.userID + "|");

        body.append("554=" + brokerRouteID + "|");

        body.append("103=" + marketID + "|");

        String header = constructHeader(body.toString(), "6"); //MarketQuery = "6"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

    //Encryption|UserID|
	public String           logoutMessage() {
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
    public String           heartBeatMessage() {
        StringBuilder body = new StringBuilder();

        /* 
         * Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
         */
        body.append("98=0|");

        /*
         * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
         */
        body.append("553=" + this.userID + "|");

        body.append("560=" + "1" + "|"); //HB Type = 1 Broker

        String header = constructHeader(body.toString(), "0"); //Heartbeat = "0"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";


        return message;
    }

    //Encryption|UserID|RefSeqNum|sessionRejectReason|Text
    public String           RejectMessage(int refSeqNum, int sessionRejectReason, String text) {
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
        message.append("52=" + df.format(date) + "|");

        //Message body length. Always unencrypted, must be second field in message.
        int length = message.length() + bodyMessage.length();
        header.append("9=" + length + "|");
        header.append(message);

        return header.toString();
    }
   
   public int               validateMessage(String fixMessage) {
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

   public String 		    receiveMessage(String messageInput) {
		int ret = validateMessage(messageInput);
	   String rejectMessage = null;

		if (ret == -1) {
			//Checksum invalid
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
			rejectMessage = RejectMessage(msgSqnNum, 99, "InvalidCheckSum");
		} else if (ret == -2) {
			int msgSqnNum = -1;
			//Reject through the msgLength - first get the msgSequence number
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("34=") && isNumeric(message[i].substring(3)) && isInteger(message[i].substring(3))) {
					msgSqnNum = Integer.parseInt(message[i].substring(3));
				}
			}
			if (msgSqnNum < 1) {
				msgSqnNum = 1;
			}
			rejectMessage = RejectMessage(msgSqnNum, 99, "InvalidMsgLength");
		} else if (ret == 1) {
			//Message valid
			return null;
		}
		return rejectMessage;
   }

   public String		    getMsgType(String messageInput) throws InvalidMsgTypeException {
		String msgType = null;
		if (!messageInput.contains("|35=")) {
			throw new InvalidMsgTypeException("Invalid Message Type");
		}
		String[] message = messageInput.split("\\|");
		for (int i=0; i < message.length; i++) {
		   if (message[i].startsWith("35=")) {
			   msgType =message[i].substring(3);
		   }
	   }
	   if (msgType == null) {
		   throw new InvalidMsgTypeException("Invalid Message Type");
	   }
    	return msgType;
   }
   
   public String       buyOrderMessage(String marketID, String itemID, String purchaseAmount,
                                                String purchasePrice, String brokerRouteID) {
        StringBuilder body = new StringBuilder();

        //Encryption
        body.append("98=0|");

        //UserID
        body.append("553=" + this.userID + "|");

        body.append("554=" + brokerRouteID + "|"); //Need to remove this one somehow, only one ID

        //Side <54> = 1 to buy
        body.append("54=1|");

        //Instrument -> Product<460> -> Type of product
        body.append("100=" + itemID + "|"); //To fix

        body.append("101=" + purchaseAmount + "|"); //Quantity<53>

        body.append("44=" + purchasePrice + "|"); //Price<44>

        body.append("49=" + marketID + "|");//SenderCompID <49>

        String header = constructHeader(body.toString(), "D"); //New Order - Single = "D"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
   }
   
    // Sale Message Builder
    public String           sellOrderMessage(String marketID, String itemID, String purchaseAmount,
                String purchasePrice, String brokerRouteID) {

        // itemID, Sell_Price, Sell_Amount, MarketID.
        StringBuilder body = new StringBuilder();

         //Encryption
         body.append("98=0|");

         //UserID
         body.append("553=" + this.userID + "|");
 
         body.append("554=" + brokerRouteID + "|"); //Need to remove this one somehow, only one ID
 
         //Side <54> = 2 to sell
         body.append("54=2|");
 
         //Instrument -> Product<460> -> Type of product
         body.append("100=" + itemID + "|"); //To fix
 
         body.append("101=" + purchaseAmount + "|"); //Quantity<53>
 
         body.append("44=" + purchasePrice + "|"); //Price<44>
 
         body.append("49=" + marketID + "|");//SenderCompID <49>

        String header = constructHeader(body.toString(), "D"); //New Order - Single = "D"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }
   
    //List Markets (List status request)
    //<Header MsgType=M>|Encryption|UserID|<TAIL>
    //ListID<66>?
    public String           listMarketsRequest(String brokerRouteID) {
        // itemID, Sell_Price, Sell_Amount, MarketID.
        StringBuilder body = new StringBuilder();

        //Encryption
        body.append("98=0|");

        //UserID
        body.append("553=" + this.userID + "|");

        body.append("554=" + brokerRouteID + "|"); //Need to remove this one somehow, only one ID

        String header = constructHeader(body.toString(), "M"); //List status request (list markets) - Single = "M"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

    //list markets (ListStatus<N> -> Answer)
    //<Header MsgType=N>Encryption|Text<58>|Tail|
    //ListID<66>?
    public String           listMarketsResponse(String marketsList) {
        // itemID, Sell_Price, Sell_Amount, MarketID.
        StringBuilder body = new StringBuilder();

        //Encryption
        body.append("98=0|");

        //Text
        body.append("58=" + marketsList + "|");

        String header = constructHeader(body.toString(), "N"); //List status request (list markets) - Single = "M"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

    //list market goods
    //Market Data Request (From broker)
    // https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Request_V_.html
    // <Header MsgTyp=Y>|Encryption|UserID|MDReqID<262>|SenderCompId<49>|Tail
    // MDReqID -> unique request ID
    //Could have instrument? Not sure how
    public String           marketsDataRequest(String brokerRouteID, String marketDataReqID, String marketID) {
        // itemID, Sell_Price, Sell_Amount, MarketID.
        StringBuilder body = new StringBuilder();

        //Encryption
        body.append("98=0|");
        
        //UserID
        body.append("553=" + this.userID + "|");
        
        body.append("554=" + brokerRouteID + "|"); //Need to remove this one somehow, only one ID
        
        //MarketDataReq ID
        body.append("262=" + marketDataReqID + "|");
        
        //MarketID
        body.append("49=" + marketID + "|");//SenderCompID <49>
        // //Text
        // body.append("58=" + marketsList + "|");

        String header = constructHeader(body.toString(), "N"); //List status request (list markets) - Single = "M"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }

    //Market Data Request Snapshot/Full Refresh (FromMarket)
    // https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Snapshot_Full_Refresh_W_.html
    // <Header MsgTyp=W>|Encryption|UserID|MDReqID<262>|Text<58>|SenderCompID|Tail
    public String           marketsDataResponse(String brokerID, String marketDataReqID, String marketData) {

        // itemID, Sell_Price, Sell_Amount, MarketID.
        StringBuilder body = new StringBuilder();

         //Encryption
         body.append("98=0|");

         //UserID (who the market is sending to)
         body.append("553=" + brokerID + "|");
 
         //MarketDataReq ID
        body.append("262=" + marketDataReqID + "|");

        //Text
        body.append("58=" + marketData + "|");
 
        body.append("49=" + this.userID + "|");//SenderCompID <49>

        String header = constructHeader(body.toString(), "W"); //Market Data Request Snapshot/Full Refresh = "W"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }
   
    //Market Data Request Reject (From Market)
    // https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Request_Reject_Y_.html
    // <Header MsgTyp=Y>|UserID|MDReqID<262>|SenderCompID|Tail
    public String           marketsDataReject(String brokerID, String marketDataReqID, String marketData) {

        // itemID, Sell_Price, Sell_Amount, MarketID.
        StringBuilder body = new StringBuilder();

         //Encryption
         body.append("98=0|");

         //UserID (who the market is sending to)
         body.append("553=" + brokerID + "|");
 
         //MarketDataReq ID
        body.append("262=" + marketDataReqID + "|");
 
        body.append("49=" + this.userID + "|");//SenderCompID <49>

        String header = constructHeader(body.toString(), "Y"); //Market Data Request Snapshot/Full Refresh = "Y"

        String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

        return message;
    }
}