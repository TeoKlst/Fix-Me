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
	private String		userID;
	private int			msgSeqNum;

	public				FixProtocol(String userID) {
		this.userID = userID;
		this.msgSeqNum = 0;
	}
   
	//General checks
		public static boolean	isNumeric(String str) {
			try {
				double d = Double.parseDouble(str);
			} catch(NumberFormatException nfe) {
				return false;
			}
			return true;
		}
	
		public static boolean	isInteger(String s) {
		   try {
			   Integer.parseInt(s);
		   } catch(NumberFormatException e) {
			   return false;
		   } catch(NullPointerException e) {
			   return false;
		   }
		   return true;
	   }

	//Receiving Messages
		public int				validateMessage(String fixMessage) {
			try {
				checksumValidator(fixMessage);
			} catch (InvalidChecksumException e) {
				return -1; 																										//-1 means bad checksum
			}
			try {
				msgLengthValidator(fixMessage);

			}catch (InvalidMsgLengthException me) {
				return -2; 																										// -2 means bad message length
			}
			return 1;
		}

		public String			receiveMessage(String messageInput) {
			int ret = validateMessage(messageInput);
			String rejectMessage = null;

			if (ret == -1) {
				int msgSqnNum = -1;																								//Reject through the checksum - first get the msgSequence number
				String[] message = messageInput.split("\\|");
				for (int i=0; i < message.length; i++) {
					if (message[i].startsWith("34=") && isNumeric(message[i].substring(3)) && isInteger(message[i].substring(3))) {
						msgSqnNum = Integer.parseInt(message[i].substring(3));
					}
				}
				if (msgSqnNum < 1) {
					msgSqnNum = 1;
				}
				try {
					rejectMessage = RejectMessage(msgSqnNum, 99, getMsgSeqNum(messageInput),"InvalidCheckSum");
				} catch (InvalidMsgTypeException e) {
					e.printStackTrace();
				}
			} else if (ret == -2) {
				int msgSqnNum = -1;																								//Reject through the msgLength - first get the msgSequence number
				String[] message = messageInput.split("\\|");
				for (int i=0; i < message.length; i++) {
					if (message[i].startsWith("34=") && isNumeric(message[i].substring(3)) && isInteger(message[i].substring(3))) {
						msgSqnNum = Integer.parseInt(message[i].substring(3));
					}
				}
				if (msgSqnNum < 1) {
					msgSqnNum = 1;
				}
				try {
					rejectMessage = RejectMessage(msgSqnNum, 99, getMsgSeqNum(messageInput),"InvalidMsgLength");
				} catch (InvalidMsgTypeException e) {
					e.printStackTrace();
				}
			} else if (ret == 1) {
				return null;																									//Message valid
			}
			return rejectMessage;
		}

	//Validation Checks
		public boolean			checksumValidator(String input) throws InvalidChecksumException {
			// Reference: https://gigi.nullneuron.net/gigilabs/calculating-the-checksum-of-a-fix-message/
			
			String[] values = input.split("\\|10=");																			//Separates the checksum from the message

			if (values.length != 2) {																							//Make sure that there are only 2 strings: the input and the checksum
				throw new InvalidChecksumException("Invalid Checksum");
			}
			
			values[0] += '|';																									//Add the pipe back to the first string to end the input
			
			String checksumStr = checksumGenerator(values[0]);

			if (!checksumStr.equals(values[1].substring(0,3))) {
				throw new InvalidChecksumException("Invalid Checksum");
			}
			return true;
		}

		public boolean			msgLengthValidator(String messageInput) throws InvalidMsgLengthException {
			int msgLength = -1;

			if (!messageInput.contains("|9=")) {
				throw new InvalidMsgLengthException("Incorrect Message Length");
			}
			int msgIndexStart = messageInput.indexOf("|9=") + 3;
			while (messageInput.charAt(msgIndexStart) != '|') { msgIndexStart++;}												//Get the starting index of the message length flag to get the length
			msgIndexStart++;
			
			int msgIndexEnd = messageInput.indexOf("|10=");																		//Get the end index of the message to calculate length
			msgIndexEnd++;

			String innerMessage = messageInput.substring(msgIndexStart, msgIndexEnd);											//Get message length from indexes

			
			String[] message = messageInput.split("\\|");																		//Get the given message length
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("9=") && 
					isNumeric(message[i].substring(2)) && isInteger(message[i].substring(2))) {
					msgLength = Integer.parseInt(message[i].substring(2));
				}
			}
			if (msgLength < 0 || msgLength != innerMessage.length()) {
				throw new InvalidMsgLengthException("Incorrect Message Length");
			}
			return true;
		}

	//Default Generators
		public String			checksumGenerator(String messageInput) {
			
			String message = messageInput.replace('|', '\u0001');																//Replace | with the ascii value of 1 (a non-printable character) to ensure the correct byte size
			
			byte[] messageBytes = message.getBytes(StandardCharsets.US_ASCII);													//Put the message in ascii byte value into a byte array
			
			int total = 0;																										//Iterate through byte array and add each bytes value to get the total byte size of the message
			for (int i = 0; i < message.length(); i++) {
				total += messageBytes[i];
			}
			Integer checksumResult = total % 256;																				//Modulus by 256 to get what the checksum value should be

			String checksumStr = "000".substring(checksumResult.toString().length()) + checksumResult.toString();				//Get the correct padding

			return checksumStr;
		}
	
		public String			constructHeader(String bodyMessage, String type, int msgSeqNumber) {
			//Protocol Version|length|Message Type|Message Sequence Number|Date|

			StringBuilder header = new StringBuilder();

			header.append("8=FIX4.4|");																							//Protocol version. Always unencrypted, must be first field in message.

			StringBuilder message = new StringBuilder();

			message.append("35=" + type + "|");																					//Message type. Always unencrypted, must be the third field in message.

			// this.msgSeqNum++;																								//Message Sequence Number. Starts at 1
			// message.append("34=" + this.msgSeqNum + "|");
			message.append("34=" + msgSeqNumber + "|");

			DateFormat df = new SimpleDateFormat("yyyyMMddHH:mm:ss");
			df.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date date = new Date();
			message.append("52=" + df.format(date) + "|");																		//Time of message transmission (always expressed in UTC (Universal Time Coordinated), also known as 'GMT'))
			
			int length = message.length() + bodyMessage.length();
			header.append("9=" + length + "|");																					//Message body length. Always unencrypted, must be second field in message.
			header.append(message);

			return header.toString();
		}

	//Session Related Message Generation
		public String			logonMessage(int heartbeat) {
		   //Encryption|UserID|Heartbeat|resetSeqNum|

		   StringBuilder body = new StringBuilder();
   
		   body.append("98=0|");																								//Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
   
		   body.append("553=" + this.userID + "|");																				//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
   
		   
		   if (heartbeat > 0) {																									//Heartbeat interval in seconds.
			   body.append("108=" + heartbeat + "|");
		   } else {
			   body.append("108=" + "120" + "|");
		   }

		   //TODO
		   //To be taken out
		   body.append("141=Y|");																								//Each FIX message has a unique sequence number (MsgSeqNum (34) tag) - https://kb.b2bits.com/display/B2BITS/Sequence+number+handling
		   this.msgSeqNum = 0;																									//Sequence numbers are initialized at the start of the FIX session starting at 1 (one) and increment through the session
		  
		   this.msgSeqNum++;																									//Message Sequence Number. Starts at 1
		   String header = constructHeader(body.toString(), "A", this.msgSeqNum); 												//Logon = "A"
   
		   String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
   
		   return message;
	   }

		public String			logoutMessage() {
			//Encryption|UserID|

			StringBuilder body = new StringBuilder();
			
			body.append("98=0|");																								//Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)

			body.append("553=" + this.userID + "|");																			//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)

			this.msgSeqNum++;																									//Message Sequence Number. Starts at 1
			String header = constructHeader(body.toString(), "5", this.msgSeqNum); 												//Logout = "5"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

		public String			heartBeatMessage() {
			//Encryption|UserID|Heartbeat|resetSeqNum

			StringBuilder body = new StringBuilder();

			body.append("98=0|");																								//Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
			
			body.append("553=" + this.userID + "|");																			//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)

			this.msgSeqNum++;
			String header = constructHeader(body.toString(), "0", this.msgSeqNum); 												//Heartbeat = "0"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
		
		public String			heartBeatMessage(int marketRouteID) {
			StringBuilder body = new StringBuilder();
	
			body.append("98=0|");																								//Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
			
			body.append("553=" + this.userID + "|");																			//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)

			body.append("554=" + marketRouteID + "|");
	
			body.append("560=" + "2" + "|"); 																					//HB Type = 2 Market
	
			this.msgSeqNum++;
			String header = constructHeader(body.toString(), "0", this.msgSeqNum); 												//Heartbeat = "0"
	
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
	 
	//Buy
		public String			buyOrderMessage(String marketID, String itemID, String purchaseAmount,
														String purchasePrice, String brokerRouteID) {
			StringBuilder body = new StringBuilder();

			body.append("98=0|");																								//Encryption

			body.append("553=" + this.userID + "|");																			//UserID

			body.append("554=" + brokerRouteID + "|"); 																			//Need to remove this one somehow, only one ID

			body.append("54=1|");																								//Side <54> = 1 to buy

			body.append("100=" + itemID + "|"); 																				//Instrument -> Product<460> -> Type of product

			body.append("101=" + purchaseAmount + "|"); 																		//Quantity<53>

			body.append("44=" + purchasePrice + "|"); 																			//Price<44>

			body.append("49=" + marketID + "|");																				//SenderCompID <49>

			this.msgSeqNum++;
			String header = constructHeader(body.toString(), "D", this.msgSeqNum); 												//New Order - Single = "D"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

		public String			PurchaseMessage(String marketID, String itemID, String purchaseAmount,
												String purchasePrice, String brokerRouteID) {
			StringBuilder body = new StringBuilder();

			body.append("553=" + this.userID + "|");

			body.append("554=" + brokerRouteID + "|");

			body.append("100=" + itemID + "|");

			body.append("101=" + purchaseAmount + "|");

			body.append("102=" + purchasePrice + "|");

			body.append("103=" + marketID + "|");

			this.msgSeqNum++;
			String header = constructHeader(body.toString(), "1", this.msgSeqNum); 																//Purchase = "1"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
	
		public String			PurchaseMessageSuccess(String brokerRouteID, int refSeqNum, String confirmationType) {
			StringBuilder body = new StringBuilder();
	
			body.append("553=" + this.userID + "|");
	
			body.append("554=" + brokerRouteID + "|");
	
			body.append("655=" + confirmationType + "|"); 																		//confirmationType Purchase Success Type - 1
	
			String header = constructHeader(body.toString(), "AK", refSeqNum); 													//Confirmation = "AK"
	
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
	
			return message;
		}
	
		public String			PurchaseMessageFail(String brokerRouteID, int refSeqNum) {
			StringBuilder body = new StringBuilder();
	
			body.append("553=" + this.userID + "|");
	
			body.append("554=" + brokerRouteID + "|");
	
			String header = constructHeader(body.toString(), "4", refSeqNum); 													//Reject = "4"
	
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
	
			return message;
		}
	

	//Sell
		public String			sellOrderMessage(String marketID, String itemID, String purchaseAmount,
													String purchasePrice, String brokerRouteID) {

			StringBuilder body = new StringBuilder();

			body.append("98=0|");																									//Encryption

			body.append("553=" + this.userID + "|");																				//UserID

			body.append("554=" + brokerRouteID + "|"); 																				//Need to remove this one somehow, only one ID

			body.append("54=2|");																									//Side <54> = 2 to sell

			body.append("100=" + itemID + "|"); 																					//Instrument -> Product<460> -> Type of product

			body.append("101=" + purchaseAmount + "|"); 																			//Quantity<53>

			body.append("44=" + purchasePrice + "|"); 																				//Price<44>

			body.append("49=" + marketID + "|");																					//SenderCompID <49>

			this.msgSeqNum++;
			String header = constructHeader(body.toString(), "D", this.msgSeqNum); 													//New Order - Single = "D"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

		public String			SaleMessage(String marketID, String itemID, String purchaseAmount,
			String purchasePrice, String brokerRouteID) {
			StringBuilder body = new StringBuilder();

			// Append to body

			this.msgSeqNum++;
			String header = constructHeader(body.toString(), "2", this.msgSeqNum); 																//Sale = "2"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
		
		public String			SaleMessageSuccess(String brokerRouteID, int refSeqNum, String confirmationType) {
			StringBuilder body = new StringBuilder();
	
			body.append("553=" + this.userID + "|");
	
			body.append("554=" + brokerRouteID + "|");
	
			body.append("655=" + confirmationType + "|");																		//confirmationType Purchase Success Type - 1
	
			String header = constructHeader(body.toString(), "AK", refSeqNum); 															//Confirmation = "AK"
	
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
	
			return message;
		}
	
		public String			SaleMessageFail(String brokerRouteID, int refSeqNum) {
			StringBuilder body = new StringBuilder();
	
			body.append("553=" + this.userID + "|");
	
			body.append("554=" + brokerRouteID + "|");
	
			String header = constructHeader(body.toString(), "4", refSeqNum); 																//Reject = "4"
	
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
	
			return message;
		}
	
	//List Markets
		public String			listMarketsRequest(String brokerRouteID) {
			//<Header MsgType=M>|Encryption|UserID|<TAIL>
			// itemID, Sell_Price, Sell_Amount, MarketID.
			StringBuilder body = new StringBuilder();
			
			body.append("98=0|");																								//Encryption
			
			body.append("553=" + this.userID + "|");																			//UserID

			body.append("554=" + brokerRouteID + "|"); 																			//Need to remove this one somehow, only one ID

			this.msgSeqNum++;
			String header = constructHeader(body.toString(), "M", this.msgSeqNum); 																//List status request (list markets) - Single = "M"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
	
		public String			listMarketsResponse(String marketsList, int refSeqNum) {
			//<Header MsgType=N>Encryption|Text<58>|Tail|
			// itemID, Sell_Price, Sell_Amount, MarketID.
			StringBuilder body = new StringBuilder();
			
			body.append("98=0|");																								//Encryption
			
			body.append("58=" + marketsList + "|");																				//Text

			String header = constructHeader(body.toString(), "N", refSeqNum); 																//List status request (list markets) - Single = "M"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
		
		// public String			ListMarketReturn(int refSeqNum) {
		// 	StringBuilder body = new StringBuilder();
	
		// 	body.append("553=" + this.userID + "|");
	
		// 	body.append("600=" + Server.mapHBMarket.keySet() + "|");
	
		// 	String header = constructHeader(body.toString(), "60", refSeqNum); 															//ListMarkets = "60"
	
		// 	String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
	
		// 	return message;
		// }
	
	//List Market Goods
		public String			marketsDataRequest(String brokerRouteID, String marketDataReqID, String marketID) {
			// <Header MsgTyp=Y>|Encryption|UserID|MDReqID<262>|SenderCompId<49>|Tail   										Market Data Request (From broker) https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Request_V_.html
			// itemID, Sell_Price, Sell_Amount, MarketID.
			StringBuilder body = new StringBuilder();
			
			body.append("98=0|");																								//Encryption
			
			body.append("553=" + this.userID + "|");																			//UserID
			
			body.append("554=" + brokerRouteID + "|");																			//Need to remove this one somehow, only one ID
			
			body.append("262=" + marketDataReqID + "|");																		//MarketDataReq ID
			
			body.append("49=" + marketID + "|");																				//MarketID - SenderCompID <49>

			this.msgSeqNum++;
			String header = constructHeader(body.toString(), "V", this.msgSeqNum); 																//Market Data Request - Single = "V"
				
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
	
		public String			marketsDataResponse(String brokerID, int refSeqNum, String marketDataReqID, String marketData) {
			// <Header MsgTyp=W>|Encryption|UserID|MDReqID<262>|Text<58>|SenderCompID|Tail										Market Data Request Snapshot/Full Refresh (FromMarket)   https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Snapshot_Full_Refresh_W_.html
			// itemID, Sell_Price, Sell_Amount, MarketID.
			StringBuilder body = new StringBuilder();

			body.append("98=0|");																								//Encryption

			body.append("553=" + brokerID + "|");																				//UserID (who the market is sending to)

			// body.append("45=" + refSeqNum + "|");																				//Reference to the Message Sequence Number that was rejected

			body.append("262=" + marketDataReqID + "|");																		//MarketDataReq ID

			body.append("58=" + marketData + "|");																				//Text

			body.append("49=" + this.userID + "|");																				//SenderCompID <49>

			String header = constructHeader(body.toString(), "W", refSeqNum); 																//Market Data Request Snapshot/Full Refresh = "W"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

		public String			marketsDataReject(String brokerID, int refSeqNum, String marketDataReqID, String marketData) {
			// <Header MsgTyp=Y>|UserID|MDReqID<262>|SenderCompID|Tail															Market Data Request Reject (From Market) https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Request_Reject_Y_.html
			// itemID, Sell_Price, Sell_Amount, MarketID.
			StringBuilder body = new StringBuilder();
			
			body.append("98=0|");																								//Encryption
			
			body.append("553=" + brokerID + "|");																				//UserID (who the market is sending to)
			
			// body.append("45=" + refSeqNum + "|");																				//Reference to the Message Sequence Number that was rejected

			body.append("262=" + marketDataReqID + "|");																		//MarketDataReq ID

			body.append("49=" + this.userID + "|");																				//SenderCompID <49>

			String header = constructHeader(body.toString(), "Y", refSeqNum); 																//Market Data Request Snapshot/Full Refresh = "Y"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

		public String			MarketQueryReturn(String brokerRouteID, int refSeqNum, int marketID, int silver, int gold,
												int platinum, int fuel, int bitcoin, int capital){
			StringBuilder body = new StringBuilder();

			body.append("553=" + this.userID + "|");

			body.append("554=" + brokerRouteID + "|");
			
			// body.append("45=" + refSeqNum + "|");																				//Reference to the Message Sequence Number that was rejected

			body.append("103=" + marketID + "|");

			body.append("104=" + silver + "," + gold + "," + platinum + "," + fuel + "," + bitcoin + "," + capital + "|");

			String header = constructHeader(body.toString(), "7", refSeqNum); 																//MarketQueryReturn = "6"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

	//Other Messages
		public String			RejectMessage(int refSeqNum, int brokerRouteID, int sessionRejectReason, String text) {
			//Encryption|UserID|RefSeqNum|sessionRejectReason|Text
			
			StringBuilder body = new StringBuilder();

			body.append("98=0|");																								//Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)

			// body.append("553=" + this.userID + "|");																			//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
			body.append("553=" + brokerRouteID + "|");																			//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
			
			// body.append("45=" + refSeqNum + "|");																				//Reference to the Message Sequence Number that was rejected

			if ((sessionRejectReason >= 0 && sessionRejectReason <= 17)) {														//Setting the sessionRejectionReason value, as well as adding text to explain further
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

			String header = constructHeader(body.toString(), "3", refSeqNum); 																//Reject = "3"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

		public String			NullMarket(int refSeqNum, int brokerRouteID) {
			// Error Non-Existent Market
			StringBuilder body = new StringBuilder();
	
			// body.append("553=" + this.userID + "|");
			body.append("553=" + brokerRouteID + "|");

			// body.append("45=" + refSeqNum + "|");	

			String header = constructHeader(body.toString(), "91", refSeqNum); 															//Non-Existent Market = "91"
	
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
	
			return message;
		}
	
		public String			RejectMessageNumFormat(int refSeqNum, int sessionRejectReason, String text, int brokerRouteID) {
			//Encryption|UserID|RefSeqNum|sessionRejectReason|Text

			StringBuilder body = new StringBuilder();

			body.append("98=0|");																								//Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)

			body.append("553=" + this.userID + "|");																			//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)

			body.append("554=" + brokerRouteID + "|");

			// body.append("45=" + refSeqNum + "|");																				//Reference to the Message Sequence Number that was rejected
			
			if ((sessionRejectReason >= 0 && sessionRejectReason <= 17)) {														//Setting the sessionRejectionReason value, as well as adding text to explain further
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

			String header = constructHeader(body.toString(), "3", refSeqNum); 																//Reject = "3"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

	//Getters
		public String			getMsgType(String messageInput) throws InvalidMsgTypeException {
			String msgType = null;
			if (messageInput == null || !messageInput.contains("|35=")) {
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

		public String			getHBType(String messageInput) throws InvalidMsgTypeException {
			String msgType = null;
			if (!messageInput.contains("|560=")) {
				throw new InvalidMsgTypeException("Invalid  Heart Beat");
			}
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("560=")) {
					msgType =message[i].substring(4);
				}
			}
			if (msgType == null) {
				throw new InvalidMsgTypeException("Invalid Heart Beat");
			}
			return msgType;
		}

		public String			getMarketRouteID(String messageInput) throws InvalidMsgTypeException {
			String msgRouteID = null;
			if (!messageInput.contains("|103=")) {
				throw new InvalidMsgTypeException("Invalid RouteID Market");
			}
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("103=")) {
					msgRouteID =message[i].substring(4);
				}
			}
			if (msgRouteID == null) {
				throw new InvalidMsgTypeException("Invalid Message Type");
			}
			return msgRouteID;
		}

		public String			getRouteID(String messageInput) throws InvalidMsgTypeException {
			String msgType = null;
			if (!messageInput.contains("|554=")) {
				// TODO To be able to distinguish if message is required to check for broker or market routeID and give appropriate error return
				throw new InvalidMsgTypeException("Invalid RouteID (Broker/Market) Type");
			}
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("554=")) {
					msgType =message[i].substring(4);
				}
			}
			if (msgType == null || !isNumeric(msgType) || !isInteger(msgType)) {
				throw new InvalidMsgTypeException("Invalid Message Type");
			}
			return msgType;
		}

		public int			getMsgSeqNum(String messageInput) throws InvalidMsgTypeException {
			String msgSeqNum = null;
			if (!messageInput.contains("|34=")) {
				// TODO To be able to distinguish if message is required to check for broker or market routeID and give appropriate error return
				throw new InvalidMsgTypeException("Invalid Message Sequence Number");
			}
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("34=")) {
					msgSeqNum =message[i].substring(3);
				}
			}
			if (msgSeqNum == null || !isNumeric(msgSeqNum) || !isInteger(msgSeqNum)) {
				throw new InvalidMsgTypeException("Invalid Message Sequence Number");
			}
			return Integer.parseInt(msgSeqNum);
		}

}

// public class FixProtocol {
//         // https://www.onixs.biz/fix-dictionary/4.4/msgs_by_category.html
//         private String     userID;
//         private int         msgSeqNum;
    
//         public FixProtocol(String userID) {
//             this.userID = userID;
//             this.msgSeqNum = 0;
//         }
    
//         public static boolean 	isNumeric(String str) {
//             try {
//                 double d = Double.parseDouble(str);
//             } catch(NumberFormatException nfe) {
//                 return false;
//             }
//             return true;
//         }
    
//         public static boolean   isInteger(String s) {
//             try {
//                 Integer.parseInt(s);
//             } catch(NumberFormatException e) {
//                 return false;
//             } catch(NullPointerException e) {
//                 return false;
//             }
//             // only got here if we didn't return false
//             return true;
//         }
        
//         public String           checksumGenerator(String messageInput) {
//             //Replace | with the ascii value of 1 (a non-printable character) to ensure the correct byte size
//             String message = messageInput.replace('|', '\u0001');
//             //Put the message in ascii byte value into a byte array
//             byte[] messageBytes = message.getBytes(StandardCharsets.US_ASCII);
//             //Iterate through byte array and add each bytes value to get the total byte size of the message
//             int total = 0;
//             for (int i = 0; i < message.length(); i++) {
//                 total += messageBytes[i];
//             }
//             //Modulus by 256 to get what the checksum value should be
//             Integer checksumResult = total % 256;
    
//             //Get's the correct padding
//             String checksumStr = "000".substring(checksumResult.toString().length()) + checksumResult.toString();
    
//             return checksumStr;
//         }
        
//         public boolean          checksumValidator(String input) throws InvalidChecksumException {
//             // Reference: https://gigi.nullneuron.net/gigilabs/calculating-the-checksum-of-a-fix-message/
//             //Separates the checksum from the message
//             String[] values = input.split("\\|10=");
    
//             //Make sure that there are only 2 strings: the input and the checksum
//             if (values.length != 2) {
//                 throw new InvalidChecksumException("Invalid Checksum");
//             }
//             //Add the pipe back to the first string to end the input
//             values[0] += '|';
            
//             String checksumStr = checksumGenerator(values[0]);
    
//             if (!checksumStr.equals(values[1].substring(0,3))) {
//                 throw new InvalidChecksumException("Invalid Checksum");
//     //			return false;
//             }
//             return true;
//         }
    
//         public boolean          msgLengthValidator(String messageInput) throws InvalidMsgLengthException {
//             int msgLength = -1;
    
//             //Get the start index of the message to get length
//             if (!messageInput.contains("|9=")) {
//                 throw new InvalidMsgLengthException("Incorrect Message Length");
//             }
//             int msgIndexStart = messageInput.indexOf("|9=") + 3;
//             while (messageInput.charAt(msgIndexStart) != '|') { msgIndexStart++;}   //Gets to the message index to calculate the message length from
//             msgIndexStart++;
    
//             //Get the end index of the message to calculate length
//             int msgIndexEnd = messageInput.indexOf("|10=");
//             msgIndexEnd++;
    
//             //Get message to get length from
//             String innerMessage = messageInput.substring(msgIndexStart, msgIndexEnd);
    
//             //Get the given message length
//             String[] message = messageInput.split("\\|");
//             for (int i=0; i < message.length; i++) {
//                 if (message[i].startsWith("9=") && isNumeric(message[i].substring(2)) && isInteger(message[i].substring(2))) {
//                     msgLength = Integer.parseInt(message[i].substring(2));
//                 }
//             }
//             if (msgLength < 0 || msgLength != innerMessage.length()) {
//                 throw new InvalidMsgLengthException("Incorrect Message Length");
//             }
//             return true;
//         }
    
//         //Encryption|UserID|Heartbeat|resetSeqNum|
//         public String           logonMessage(int heartbeat) {
//             StringBuilder body = new StringBuilder();
    
//             /* 
//              * Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
//              */
//             body.append("98=0|");
    
//             /*
//              * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
//              */
//             body.append("553=" + this.userID + "|");
    
//             /*
//              * Heartbeat interval in seconds.
//              * The Heartbeat monitors the status of the communication link and identifies when the last of a string of messages was not received.
//              * When either end of the FIX connection has not sent any data for HeartBtInt seconds, it will transmit a Heartbeat message.
//              * 
//              * Value is set in the 'config.properties' file (client side) as 'SERVER.POLLING.INTERVAL'
//              * 30 seconds is default interval value. If HeartBtInt is set to 0 no heartbeat message is required.
//              * 
//              */
//             if (heartbeat > 0) {
//                 body.append("108=" + heartbeat + "|");
//             } else {
//                 body.append("108=" + "120" + "|");
//             }
    
//             /*
//              * Each FIX message has a unique sequence number (MsgSecNum (34) tag) - https://kb.b2bits.com/display/B2BITS/Sequence+number+handling
//              * Sequence numbers are initialized at the start of the FIX session starting at 1 (one) and increment through the session
//              * 
//              * All sides of FIX session should have sequence numbers reset.
//              * Valid value is "Y" = Yes (reset)
//              * 
//              */
//             body.append("141=Y|");
//             this.msgSeqNum = 0;
           
    
//             String header = constructHeader(body.toString(), "A"); //Logon = "A"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//         }
    
//             // Get Broker/Market Route ID
//             public String		    getRouteID(String messageInput) throws InvalidMsgTypeException {
//                 String msgType = null;
//                 if (!messageInput.contains("|554=")) {
//                     // TODO To be able to distinguish if message is required to check for broker or market routeID and give appropriate error return
//                     throw new InvalidMsgTypeException("Invalid RouteID (Broker/Market) Type");
//                 }
//                 String[] message = messageInput.split("\\|");
//                 for (int i=0; i < message.length; i++) {
//                    if (message[i].startsWith("554=")) {
//                        msgType =message[i].substring(3);
//                    }
//                }
//                if (msgType == null) {
//                    throw new InvalidMsgTypeException("Invalid Message Type");
//                }
//                 return msgType;
//            }
    
//             // Purchase Message Builder
//             public String           PurchaseMessage(String marketID, String itemID, String purchaseAmount,
//                                                     String purchasePrice, String brokerRouteID) {
//                 StringBuilder body = new StringBuilder();
    
//                 body.append("553=" + this.userID + "|");
    
//                 body.append("554=" + brokerRouteID + "|");
    
//                 body.append("100=" + itemID + "|");
    
//                 body.append("101=" + purchaseAmount + "|");
    
//                 body.append("102=" + purchasePrice + "|");
    
//                 body.append("103=" + marketID + "|");
    
//                 String header = constructHeader(body.toString(), "1"); //Purchase = "1"
    
//                 String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//                 return message;
//             }
        
//             // Sale Message Builder
//             public String           SaleMessage(String marketID, String itemID, String purchaseAmount,
//                                                 String purchasePrice, String brokerRouteID) {
//                 StringBuilder body = new StringBuilder();
    
//                 // Append to body
    
//                 String header = constructHeader(body.toString(), "2"); //Sale = "2"
    
//                 String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//                 return message;
//             }
    
//         //Encryption|UserID|
//         public String           logoutMessage() {
//             StringBuilder body = new StringBuilder();
    
//             /* 
//              * Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
//              */
//             body.append("98=0|");
    
//             /*
//              * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
//              */
//             body.append("553=" + this.userID + "|");
    
//             String header = constructHeader(body.toString(), "5"); //Logout = "5"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//         }
    
//         //Encryption|UserID|RefSeqNum|sessionRejectReason|Text
//         public String           RejectMessage(int refSeqNum, int sessionRejectReason, String text) {
//             StringBuilder body = new StringBuilder();
    
//             /* 
//              * Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
//              */
//             body.append("98=0|");
    
//             /*
//              * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
//              */
//             body.append("553=" + this.userID + "|");
    
//             /*
//              * Reference to the Message Sequence Number that was rejected
//              */
//             body.append("45=" + refSeqNum + "|");
    
//             /*
//              * Setting the sessionRejectionReason value, as well as adding text to explain further
//              */
//             if ((sessionRejectReason >= 0 && sessionRejectReason <= 17)) {
//                 body.append("373=" + sessionRejectReason + "|");
//             } else if (sessionRejectReason == 99) {
//                 body.append("373=" + sessionRejectReason + "|");
//             } else {
//                 System.out.println("Invalid rejection value entered.");
//                 return null;
//             }
//             if (text != null && !text.isEmpty()) {
//                 body.append("58=" + text + "|");
//             }
    
//             String header = constructHeader(body.toString(), "3"); //Reject = "3"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//         }

//        //Protocol Version|length|Message Type|Message Sequence Number|Date|
//        public String            constructHeader(String bodyMessage, String type) {
//             StringBuilder header = new StringBuilder();
    
//             //Protocol version. Always unencrypted, must be first field in message.
//             header.append("8=FIX4.4|");
    
//             StringBuilder message = new StringBuilder();
    
//             //Message type. Always unencrypted, must be the third field in message.
//             message.append("35=" + type + "|");
    
//             //Message Sequence Number
//             this.msgSeqNum++;       //Message sequence number starts at 1
//             message.append("34=" + this.msgSeqNum + "|");
    
//             //Time of message transmission (always expressed in UTC (Universal Time Coordinated), also known as 'GMT'))
//             DateFormat df = new SimpleDateFormat("yyyyMMddHH:mm:ss");
//             df.setTimeZone(TimeZone.getTimeZone("GMT"));
//             Date date = new Date();
//             message.append("52=" + df.format(date) + "|");
    
//             //Message body length. Always unencrypted, must be second field in message.
//             int length = message.length() + bodyMessage.length();
//             header.append("9=" + length + "|");
//             header.append(message);
    
//             return header.toString();
//         }
       
//        public int               validateMessage(String fixMessage) {
//             try {
//                 checksumValidator(fixMessage);
//             } catch (InvalidChecksumException e) {
//                 return -1; //-1 means bad checksum
//             }
//             try {
//                 msgLengthValidator(fixMessage);
    
//             }catch (InvalidMsgLengthException me) {
//                 return -2; // -2 means bad message length
//             }
//            return 1;
//        }
    
//        public String 		    receiveMessage(String messageInput) {
//             int ret = validateMessage(messageInput);
//            String rejectMessage = null;
    
//             if (ret == -1) {
//                 //Checksum invalid
//                 int msgSqnNum = -1;
//                 //Reject through the checksum - first get the msgSequence number
//                 String[] message = messageInput.split("\\|");
//                 for (int i=0; i < message.length; i++) {
//                     if (message[i].startsWith("34=") && isNumeric(message[i].substring(3)) && isInteger(message[i].substring(3))) {
//                         msgSqnNum = Integer.parseInt(message[i].substring(3));
//                     }
//                 }
//                 if (msgSqnNum < 1) {
//                     msgSqnNum = 1;
//                 }
//                 rejectMessage = RejectMessage(msgSqnNum, 99, "InvalidCheckSum");
//             } else if (ret == -2) {
//                 int msgSqnNum = -1;
//                 //Reject through the msgLength - first get the msgSequence number
//                 String[] message = messageInput.split("\\|");
//                 for (int i=0; i < message.length; i++) {
//                     if (message[i].startsWith("34=") && isNumeric(message[i].substring(3)) && isInteger(message[i].substring(3))) {
//                         msgSqnNum = Integer.parseInt(message[i].substring(3));
//                     }
//                 }
//                 if (msgSqnNum < 1) {
//                     msgSqnNum = 1;
//                 }
//                 rejectMessage = RejectMessage(msgSqnNum, 99, "InvalidMsgLength");
//             } else if (ret == 1) {
//                 //Message valid
//                 return null;
//             }
//             return rejectMessage;
//        }
    
//        public String		    getMsgType(String messageInput) throws InvalidMsgTypeException {
//             // System.out.println("Message contains |35=: " + messageInput.contains("|35="));
//             String msgType = null;
//             if (messageInput == null || !messageInput.contains("|35=")) {
//                 throw new InvalidMsgTypeException("Invalid Message Type");
//             }
//             String[] message = messageInput.split("\\|");
//             for (int i=0; i < message.length; i++) {
//                 if (message[i].startsWith("35=")) {
//                     msgType =message[i].substring(3);
//                 }
//             }
//             if (msgType == null) {
//                 throw new InvalidMsgTypeException("Invalid Message Type");
//             }
//                 return msgType;
//         }
       
//        public String       buyOrderMessage(String marketID, String itemID, String purchaseAmount,
//                                                     String purchasePrice, String brokerRouteID) {
//             StringBuilder body = new StringBuilder();
    
//             //Encryption
//             body.append("98=0|");
    
//             //UserID
//             body.append("553=" + this.userID + "|");
    
//             body.append("554=" + brokerRouteID + "|"); //Need to remove this one somehow, only one ID
    
//             //Side <54> = 1 to buy
//             body.append("54=1|");
    
//             //Instrument -> Product<460> -> Type of product
//             body.append("100=" + itemID + "|"); //To fix
    
//             body.append("101=" + purchaseAmount + "|"); //Quantity<53>
    
//             body.append("44=" + purchasePrice + "|"); //Price<44>
    
//             body.append("49=" + marketID + "|");//SenderCompID <49>
    
//             String header = constructHeader(body.toString(), "D"); //New Order - Single = "D"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//        }
       
//         // Sale Message Builder
//         public String           sellOrderMessage(String marketID, String itemID, String purchaseAmount,
//                     String purchasePrice, String brokerRouteID) {
    
//             // itemID, Sell_Price, Sell_Amount, MarketID.
//             StringBuilder body = new StringBuilder();
    
//              //Encryption
//              body.append("98=0|");
    
//              //UserID
//              body.append("553=" + this.userID + "|");
     
//              body.append("554=" + brokerRouteID + "|"); //Need to remove this one somehow, only one ID
     
//              //Side <54> = 2 to sell
//              body.append("54=2|");
     
//              //Instrument -> Product<460> -> Type of product
//              body.append("100=" + itemID + "|"); //To fix
     
//              body.append("101=" + purchaseAmount + "|"); //Quantity<53>
     
//              body.append("44=" + purchasePrice + "|"); //Price<44>
     
//              body.append("49=" + marketID + "|");//SenderCompID <49>
    
//             String header = constructHeader(body.toString(), "D"); //New Order - Single = "D"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//         }
       
//         //List Markets (List status request)
//         //<Header MsgType=M>|Encryption|UserID|<TAIL>
//         //ListID<66>?
//         public String           listMarketsRequest(String brokerRouteID) {
//             // itemID, Sell_Price, Sell_Amount, MarketID.
//             StringBuilder body = new StringBuilder();
    
//             //Encryption
//             body.append("98=0|");
    
//             //UserID
//             body.append("553=" + this.userID + "|");
    
//             body.append("554=" + brokerRouteID + "|"); //Need to remove this one somehow, only one ID
    
//             String header = constructHeader(body.toString(), "M"); //List status request (list markets) - Single = "M"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//     }

//     // Purchase Message Builder
//     public String           PurchaseMessageSuccess(String brokerRouteID, String confirmationType) {
//         StringBuilder body = new StringBuilder();

//         body.append("553=" + this.userID + "|");

//         body.append("554=" + brokerRouteID + "|");

//         body.append("655=" + confirmationType + "|"); //confirmationType Purchase Success Type - 1

//         String header = constructHeader(body.toString(), "AK"); //Confirmation = "AK"

//         String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

//         return message;
//     }

//     public String           PurchaseMessageFail(String brokerRouteID) {
//         StringBuilder body = new StringBuilder();

//         body.append("553=" + this.userID + "|");

//         body.append("554=" + brokerRouteID + "|");

//         String header = constructHeader(body.toString(), "4"); //Reject = "4"

//         String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

//         return message;
//     }

//     // Sale Message Builder
//     public String           SaleMessageSuccess(String brokerRouteID, String confirmationType) {
//         StringBuilder body = new StringBuilder();

//         body.append("553=" + this.userID + "|");

//         body.append("554=" + brokerRouteID + "|");

//         body.append("655=" + confirmationType + "|"); //confirmationType Purchase Success Type - 1

//         String header = constructHeader(body.toString(), "AK"); //Confirmation = "AK"

//         String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

//         return message;
//     }

//     public String           SaleMessageFail(String brokerRouteID) {
//         StringBuilder body = new StringBuilder();

//         body.append("553=" + this.userID + "|");

//         body.append("554=" + brokerRouteID + "|");

//         String header = constructHeader(body.toString(), "4"); //Reject = "4"

//         String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

//         return message;
//     }

//     public String           MarketQueryReturn(String brokerRouteID, int marketID, int silver, int gold,
//                                             int platinum, int fuel, int bitcoin, int capital){
//         StringBuilder body = new StringBuilder();

//         body.append("553=" + this.userID + "|");

//         body.append("554=" + brokerRouteID + "|");

//         body.append("103=" + marketID + "|");

//         body.append("104=" + silver + "," + gold + "," + platinum + "," + fuel + "," + bitcoin + "," + capital + "|");

//         String header = constructHeader(body.toString(), "7"); //MarketQueryReturn = "6"

//         String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

//         return message;
//     }

//     //Encryption|UserID|Heartbeat|resetSeqNum
//     public String           heartBeatMessage(int marketRouteID) {
//         StringBuilder body = new StringBuilder();

//         /* 
//          * Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
//          */
//         body.append("98=0|");

//         /*
//          * The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
//          */
//         body.append("553=" + this.userID + "|");

//         body.append("554=" + marketRouteID + "|");

//         body.append("560=" + "2" + "|"); //HB Type = 2 Market

//         String header = constructHeader(body.toString(), "0"); //Heartbeat = "0"

//         String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";


//         return message;
//     }


    
//         //list markets (ListStatus<N> -> Answer)
//         //<Header MsgType=N>Encryption|Text<58>|Tail|
//         //ListID<66>?
//         public String           listMarketsResponse(String marketsList) {
//             // itemID, Sell_Price, Sell_Amount, MarketID.
//             StringBuilder body = new StringBuilder();
    
//             //Encryption
//             body.append("98=0|");
    
//             //Text
//             body.append("58=" + marketsList + "|");
    
//             String header = constructHeader(body.toString(), "N"); //List status request (list markets) - Single = "M"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//         }
    
//         //list market goods
//         //Market Data Request (From broker)
//         // https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Request_V_.html
//         // <Header MsgTyp=Y>|Encryption|UserID|MDReqID<262>|SenderCompId<49>|Tail
//         // MDReqID -> unique request ID
//         //Could have instrument? Not sure how
//         public String           marketsDataRequest(String brokerRouteID, String marketDataReqID, String marketID) {
//             // itemID, Sell_Price, Sell_Amount, MarketID.
//             StringBuilder body = new StringBuilder();
    
//             //Encryption
//             body.append("98=0|");
            
//             //UserID
//             body.append("553=" + this.userID + "|");
            
//             body.append("554=" + brokerRouteID + "|"); //Need to remove this one somehow, only one ID
            
//             //MarketDataReq ID
//             body.append("262=" + marketDataReqID + "|");
            
//             //MarketID
//             body.append("49=" + marketID + "|");//SenderCompID <49>
//             // //Text
//             // body.append("58=" + marketsList + "|");
    
//             String header = constructHeader(body.toString(), "V"); //Market Data Request - Single = "V"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//         }
    
//         //Market Data Request Snapshot/Full Refresh (FromMarket)
//         // https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Snapshot_Full_Refresh_W_.html
//         // <Header MsgTyp=W>|Encryption|UserID|MDReqID<262>|Text<58>|SenderCompID|Tail
//         public String           marketsDataResponse(String brokerID, String marketDataReqID, String marketData) {
    
//             // itemID, Sell_Price, Sell_Amount, MarketID.
//             StringBuilder body = new StringBuilder();
    
//              //Encryption
//              body.append("98=0|");
    
//              //UserID (who the market is sending to)
//              body.append("553=" + brokerID + "|");
     
//              //MarketDataReq ID
//             body.append("262=" + marketDataReqID + "|");
    
//             //Text
//             body.append("58=" + marketData + "|");
     
//             body.append("49=" + this.userID + "|");//SenderCompID <49>
    
//             String header = constructHeader(body.toString(), "W"); //Market Data Request Snapshot/Full Refresh = "W"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//         }
       
//         //Market Data Request Reject (From Market)
//         // https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Request_Reject_Y_.html
//         // <Header MsgTyp=Y>|UserID|MDReqID<262>|SenderCompID|Tail
//         public String           marketsDataReject(String brokerID, String marketDataReqID, String marketData) {
    
//             // itemID, Sell_Price, Sell_Amount, MarketID.
//             StringBuilder body = new StringBuilder();
    
//              //Encryption
//              body.append("98=0|");
    
//              //UserID (who the market is sending to)
//              body.append("553=" + brokerID + "|");
     
//              //MarketDataReq ID
//             body.append("262=" + marketDataReqID + "|");
     
//             body.append("49=" + this.userID + "|");//SenderCompID <49>
    
//             String header = constructHeader(body.toString(), "Y"); //Market Data Request Snapshot/Full Refresh = "Y"
    
//             String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
    
//             return message;
//         }

// }