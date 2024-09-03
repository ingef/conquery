package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.function.Function;

/**
 * Printers handle transformation from {@link com.bakdata.conquery.models.query.results.EntityResult} to the respective renderers "native" representation.
 *  
 * @param <T> The intermediate representation of the type we are printing.
 */
@FunctionalInterface
public interface Printer<T> extends Function<T, Object> {
	Object apply(T value);
}
