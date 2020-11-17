package com.bakdata.conquery.io.result.arrow;

import java.util.Objects;

@FunctionalInterface
public interface RowConsumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(int rowNumber, Object[] row);

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default RowConsumer andThen(RowConsumer after) {
        Objects.requireNonNull(after);
        return (n, r) -> { accept(n, r); after.accept(n, r); };
    }

}
