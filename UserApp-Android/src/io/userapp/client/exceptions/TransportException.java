package io.userapp.client.exceptions;

public class TransportException extends UserAppException {

	public TransportException(String message, Exception innerException) {
		super(message, innerException);
	}
	
}
