package com.bakdata.conquery.models.exceptions;

/**
 * This exception if thrown if there is any kind of error in one of the configuration files.
 */
public class JSONException extends Exception {

	private static final long serialVersionUID = 1L;

	public JSONException(String message, Throwable cause) {
		super(message, cause);
	}

	public JSONException(String message) {
		super(message);
	}

}
