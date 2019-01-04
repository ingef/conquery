package com.bakdata.conquery.models.exceptions;

public class QueryExecutionException extends Exception {

	private static final long serialVersionUID = 1L;

	public QueryExecutionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public QueryExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public QueryExecutionException(String message) {
		super(message);
	}

	public QueryExecutionException(Throwable cause) {
		super(cause);
	}

}
