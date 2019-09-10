package app.fix.exceptions;

public class TagRepeatedException extends Exception {
	public TagRepeatedException(String errorMessage) {
		super(errorMessage);
	}
}
