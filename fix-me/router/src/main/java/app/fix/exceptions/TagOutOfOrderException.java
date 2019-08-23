package app.fix.exceptions;

public class TagOutOfOrderException extends Exception {
	public TagOutOfOrderException(String errorMessage) {
		super(errorMessage);
	}
}
