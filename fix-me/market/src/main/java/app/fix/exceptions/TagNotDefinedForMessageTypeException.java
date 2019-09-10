package app.fix.exceptions;

public class TagNotDefinedForMessageTypeException extends Exception {
	public TagNotDefinedForMessageTypeException(String errorMessage) {
		super(errorMessage);
	}
}
