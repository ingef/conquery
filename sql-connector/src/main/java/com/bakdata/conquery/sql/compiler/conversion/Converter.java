package com.bakdata.conquery.sql.compiler.conversion;

import java.util.Optional;

/**
 * Converts values of one supported runtime type using an explicit compilation context.
 *
 * <p>This small SPI is independent of query-framework classes so connector extensions can contribute converters for
 * resolved model operations.</p>
 *
 * @param <C> supported input type
 * @param <R> conversion result type
 * @param <X> conversion context type
 */
public interface Converter<C, R, X> {

	default <I> Optional<R> tryConvert(I input, X context) {
		if (getConversionClass().isInstance(input)) {
			return Optional.ofNullable(convert(getConversionClass().cast(input), context));
		}
		return Optional.empty();
	}

	Class<? extends C> getConversionClass();

	R convert(C input, X context);
}
