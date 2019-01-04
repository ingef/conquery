package com.bakdata.conquery.models.exceptions;

/**
 * This exception if thrown if there is any kind of error in one of the configuration files.
 */
public class ConfigurationException extends Exception {

	private static final long serialVersionUID = 1L;

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigurationException(String message) {
		super(message);
	}

}
