package app.fix.exceptions;

public class UnexpectedSOHCharacterException extends Exception {
	public UnexpectedSOHCharacterException(String errorMessage) {
		super(errorMessage);
	}
}
