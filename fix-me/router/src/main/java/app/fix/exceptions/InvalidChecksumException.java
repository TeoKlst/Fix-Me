package app.fix.exceptions;

public class InvalidChecksumException extends Exception {
	public InvalidChecksumException(String errorMessage) {
		super(errorMessage);
	}
}
