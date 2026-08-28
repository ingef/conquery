package com.bakdata.conquery.models.exceptions;


/**
 * This exception if thrown if any value could not be parsed as intended.
 */
public class ParsingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParsingException(String message) {
		super(message);
	}

	public static ParsingException of(String value, String format) {
		return new ParsingException("Failed to parse '" + value + "' as " + format);
	}

	public static ParsingException of(String value, String format, Throwable cause) {
		return new ParsingException("Failed to parse '" + value + "' as " + format, cause);
	}
}
