package com.bakdata.conquery.sql.conversion;

import java.util.List;

import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.google.common.collect.MoreCollectors;

/**
 * Converts an input to a result with an applicable converter.
 *
 * @param <C> type that can be converted
 * @param <R> type of the result
 * @see Converter
 */
public abstract class ConverterService<C, R> {

	private final List<? extends Converter<? extends C, R>> converters;

	protected ConverterService(List<? extends Converter<? extends C, R>> converters) {
		this.converters = converters;
	}

	public R convert(C selectNode, ConversionContext context) {
		return converters.stream()
						 .flatMap(converter -> converter.tryConvert(selectNode, context).stream())
						 .collect(MoreCollectors.onlyElement());
	}

}
