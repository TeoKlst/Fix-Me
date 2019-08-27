package app.fix.exceptions;

public class InvalidMsgTypeException extends Exception {
	public InvalidMsgTypeException(String errorMessage) {
		super(errorMessage);
	}
}
