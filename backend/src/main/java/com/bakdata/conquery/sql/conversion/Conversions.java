package com.bakdata.conquery.sql.conversion;

import java.util.List;

import com.google.common.collect.MoreCollectors;
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
		return converters.stream()
						 .flatMap(converter -> converter.tryConvert(node, context).stream())
						 .collect(MoreCollectors.onlyElement());
	}

}
