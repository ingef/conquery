package com.bakdata.conquery.sql.conversion;

import java.util.List;

import com.google.common.collect.MoreCollectors;

/**
 * Converts an input to a result with an applicable converter.
 *
 * @param <C> type that can be converted
 * @param <R> type of the result
 * @param <X> context of the convertible
 * @see Converter
 */
public abstract class Conversions<C, R, X> {

	private final List<? extends Converter<? extends C, R, X>> converters;

	protected Conversions(List<? extends Converter<? extends C, R, X>> converters) {
		this.converters = converters;
	}

	public R convert(C selectNode, X context) {
		return converters.stream()
						 .flatMap(converter -> converter.tryConvert(selectNode, context).stream())
						 .collect(MoreCollectors.onlyElement());
	}

}
