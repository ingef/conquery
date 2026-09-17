package com.bakdata.conquery.io.result.arrow;

@FunctionalInterface
public interface RowConsumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(int rowNumber, Object value);

}
