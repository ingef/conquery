package com.bakdata.conquery.sql.conversion;

import java.util.List;
import java.util.Optional;

import lombok.Getter;

/**
 * Converts an input to a result with an applicable converter.
 *
 * @param <C> type that can be converted
 * @param <R> type of the result
 * @param <X> context of the convertible
 * @see Converter
 */
public abstract class Conversions<C, R, X extends Context> {

	@Getter
	private final List<? extends Converter<? extends C, R, X>> converters;

	protected Conversions(List<? extends Converter<? extends C, R, X>> converters) {
		this.converters = converters;
	}

	public R convert(C node, X context) {
		R converted = null;
		for (Converter<? extends C, R, X> converter : converters) {
			Optional<R> maybeConverted = converter.tryConvert(node, context);
			if (maybeConverted.isPresent()) {
				if (converted == null) {
					converted = maybeConverted.get();
				}
				else {
					throw new IllegalStateException("Multiple Converters for %s".formatted(node));
				}
			}
		}

		if (converted == null) {
			throw new IllegalStateException("No converter found for %s".formatted(node));
		}

		return converted;
	}

}
