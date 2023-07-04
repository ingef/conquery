package com.bakdata.conquery.sql.conversion;

import java.util.Optional;

import com.bakdata.conquery.sql.conversion.context.ConversionContext;

/**
 * A converter converts an input into a result object if the input matches the conversion class.
 *
 * @param <C> type that can be converted
 * @param <R> type of the result
 */
public interface Converter<C, R> {

	default <I> Optional<R> tryConvert(I input, ConversionContext context) {
		if (getConversionClass().isInstance(input)) {
			return Optional.ofNullable(convert(getConversionClass().cast(input), context));
		}
		return Optional.empty();
	}

	Class<C> getConversionClass();

	R convert(final C convert, final ConversionContext context);

}
