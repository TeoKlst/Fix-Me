package app.fix.exceptions;

public class DecryptionProblemException extends Exception {
	public  DecryptionProblemException(String errorMessage) {
		super(errorMessage);
	}
}
