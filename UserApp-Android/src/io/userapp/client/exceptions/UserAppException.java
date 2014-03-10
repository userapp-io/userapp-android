package io.userapp.client.exceptions;

public class UserAppException extends Exception {
	
	public UserAppException(String message) {
	   super(message);
	}
	
	public UserAppException(String message, Exception cause) {
	   super(message, cause);
	}
	
}