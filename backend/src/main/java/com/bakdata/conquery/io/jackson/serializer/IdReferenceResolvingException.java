package com.bakdata.conquery.io.jackson.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Marker class for exceptions in id resolving.
 */
public class IdReferenceResolvingException extends InvalidFormatException {
	private static final long serialVersionUID = 1L;

	public IdReferenceResolvingException(JsonParser p, String msg, String value, Class<?> targetType) {
		super(p, msg, value, targetType);
	}
}
