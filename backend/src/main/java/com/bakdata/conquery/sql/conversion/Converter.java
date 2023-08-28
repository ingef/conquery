package com.bakdata.conquery.sql.conversion;

import java.util.Optional;

/**
 * A converter converts an input into a result object if the input matches the conversion class.
 *
 * @param <C> type that can be converted
 * @param <R> type of the result
 * @param <X> context of the convertible
 */
public interface Converter<C, R, X> {

	default <I> Optional<R> tryConvert(I input, X context) {
		if (getConversionClass().isInstance(input)) {
			return Optional.ofNullable(convert(getConversionClass().cast(input), context));
		}
		return Optional.empty();
	}

	Class<? extends C> getConversionClass();

	R convert(final C convert, final X context);

}
