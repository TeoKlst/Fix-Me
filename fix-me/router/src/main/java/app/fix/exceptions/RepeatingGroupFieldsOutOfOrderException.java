package app.fix.exceptions;

public class RepeatingGroupFieldsOutOfOrderException extends Exception {
	public RepeatingGroupFieldsOutOfOrderException(String errorMessage) {
		super(errorMessage);
	}
}
