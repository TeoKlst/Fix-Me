package app.fix.exceptions;

public class IncorrectDataFormatException extends Exception {
	public IncorrectDataFormatException(String errorMessage) {
		super(errorMessage);
	}
}
