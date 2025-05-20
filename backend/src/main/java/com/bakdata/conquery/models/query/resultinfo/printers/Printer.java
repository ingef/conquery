package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Printers handle transformation from {@link com.bakdata.conquery.models.query.results.EntityResult} to the respective renderers "native" representation.
 *  
 * @param <T> The intermediate representation of the type we are printing.
 */
@FunctionalInterface
@SuppressWarnings("raw")
public interface Printer<T> extends Function<T, Object> {
	Object apply(@NotNull T value);

	@NotNull
	default <V> Printer<T> andThen(@NotNull Printer<V> after) {
		return (T t) -> after.apply((V) apply(t));
	}
}
