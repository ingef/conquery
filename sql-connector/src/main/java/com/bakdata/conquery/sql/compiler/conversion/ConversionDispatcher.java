package com.bakdata.conquery.sql.compiler.conversion;

import java.util.List;
import java.util.Optional;

/** Dispatches a value to exactly one converter supporting its runtime type. */
public final class ConversionDispatcher<C, R, X> {

	private final List<? extends Converter<? extends C, R, X>> converters;

	public ConversionDispatcher(List<? extends Converter<? extends C, R, X>> converters) {
		this.converters = List.copyOf(converters);
	}

	public List<? extends Converter<? extends C, R, X>> getConverters() {
		return converters;
	}

	public R convert(C input, X context) {
		R converted = null;
		for (Converter<? extends C, R, X> converter : converters) {
			Optional<R> maybeConverted = converter.tryConvert(input, context);
			if (maybeConverted.isEmpty()) {
				continue;
			}
			if (converted != null) {
				throw new IllegalStateException("Multiple converters for %s".formatted(input));
			}
			converted = maybeConverted.orElseThrow();
		}

		if (converted == null) {
			throw new IllegalStateException("No converter found for %s".formatted(input));
		}
		return converted;
	}
}
