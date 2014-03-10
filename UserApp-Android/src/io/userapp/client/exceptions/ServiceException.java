package io.userapp.client.exceptions;

public class ServiceException extends UserAppException {
	
	private String errorCode;
	
	public ServiceException(String errorCode, String message) {
	   super(message);
	   this.errorCode = errorCode;
	}
	
	public String getErrorCode() {
		return this.errorCode;
	}
	
}