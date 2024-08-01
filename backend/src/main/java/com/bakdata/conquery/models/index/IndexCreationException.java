package com.bakdata.conquery.models.index;

public class IndexCreationException extends Exception {

	public IndexCreationException(IndexKey<?> key, Throwable cause) {
		super(String.format("Unable to build index from index configuration: %s)", key), cause);
	}
}
