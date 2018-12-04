package com.bakdata.conquery.models.exceptions;

public class QueryTranslationException extends QueryExecutionException {

	private static final long serialVersionUID = 1L;

	public QueryTranslationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public QueryTranslationException(String message, Throwable cause) {
		super(message, cause);
	}

	public QueryTranslationException(String message) {
		super(message);
	}

	public QueryTranslationException(Throwable cause) {
		super(cause);
	}

}
