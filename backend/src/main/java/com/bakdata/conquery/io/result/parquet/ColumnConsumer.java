package com.bakdata.conquery.io.result.parquet;

import java.util.function.BiConsumer;

import org.apache.parquet.io.api.RecordConsumer;

/**
 * A record writer which is implemented for each {@link com.bakdata.conquery.models.types.ResultType}
 * to feed the {@link RecordConsumer} the correct structure and values. Each column is given an ColumnConsumer.
 * <p>
 * Practically an implementation of a consumer know its supported type and how the given {@link Object} is
 * written to the {@link RecordConsumer}.
 */
@FunctionalInterface
public interface ColumnConsumer extends BiConsumer<RecordConsumer, Object> {

}