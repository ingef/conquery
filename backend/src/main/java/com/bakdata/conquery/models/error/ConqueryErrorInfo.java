package com.bakdata.conquery.models.error;

import java.util.Comparator;
import java.util.UUID;

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

	SimpleErrorInfo asPlain();

	
	/**
	 * Method to check if two errors are basically the same, by not checking the id and the context (which possibly checks on hashcode basis).
	 */
	default boolean equalsRegardingCodeAndMessage(ConqueryErrorInfo other) {
		return Comparator
			.comparing(ConqueryErrorInfo::getCode)
			.thenComparing(ConqueryErrorInfo::getMessage)
			.compare(this, other) == 0;
	}
}
