package com.bakdata.conquery.io.result.arrow;

@FunctionalInterface
public interface RowConsumer {

	/**
	 * Performs this operation on the given argument.
	 *
	 * @param rowNumber current row number
	 * @param value object to be manipulated
	 */
	void accept(int rowNumber, Object value);

}
