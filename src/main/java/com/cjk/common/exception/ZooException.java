package com.cjk.common.exception;

public class ZooException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ZooException() {
		super();
	}

	public ZooException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ZooException(String message, Throwable cause) {
		super(message, cause);
	}

	public ZooException(String message) {
		super(message);
	}

	public ZooException(Throwable cause) {
		super(cause);
	}

}
