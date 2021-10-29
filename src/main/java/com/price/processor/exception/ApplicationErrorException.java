package com.price.processor.exception;

public class ApplicationErrorException extends Exception {
	public ApplicationErrorException(String message) {
		super(message);
	}

	public ApplicationErrorException(String message, Throwable cause) {
		super(message, cause);
	}
}
