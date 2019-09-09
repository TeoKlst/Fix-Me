package app.fix.exceptions;

public class InvalidTagNumberException extends Exception{
	public InvalidTagNumberException(String errorMessage) {
			super(errorMessage);
		}
}
