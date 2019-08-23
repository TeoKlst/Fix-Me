package app.fix.exceptions;

public class TagSpecifiedWithoutValueException extends Exception {
	public TagSpecifiedWithoutValueException(String errorMessage) {
		super(errorMessage);
	}
}
