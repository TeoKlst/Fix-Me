package app.fix.exceptions;

public class InvalidMsgLengthException extends Exception {
	public InvalidMsgLengthException(String errorMessage) {
		super(errorMessage);
	}
}
