package com.bakdata.conquery.models.error;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ComparisonChain;

/**
 * Base interface for errors that should be displayed as an info in the Frontend
 */
public interface ConqueryErrorInfo {

	/**
	 * A unique id for this error to retrieve it in the logs.
	 */
	UUID getId();
	
	String getCode();
	
	String getMessage();
	
	Map<String,String> getContext();

	/**
	 * Returns a {@link ConqueryErrorInfo} POJO without the internal type information.
	 * @return
	 */
	PlainError asPlain();
	
	/**
	 * Method to check if two errors are basically the same, by not checking the id and the context (which possibly checks on hashcode basis).
	 */
	default boolean equalsRegardingCodeAndMessage(ConqueryErrorInfo other) {
		return ComparisonChain.start()
			.compare(getCode(), other.getCode())
			.compare(getMessage(), other.getMessage())
			.result() == 0;
	}
}
