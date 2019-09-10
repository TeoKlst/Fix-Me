package app.fix.exceptions;

public class RequiredTagMissingException extends Exception {
	public RequiredTagMissingException(String errorMessage) {
		super(errorMessage);
	}
}
