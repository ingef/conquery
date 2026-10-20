package com.bakdata.conquery.sql.translate.conversion;

import java.util.List;
import java.util.Optional;

import lombok.Getter;

/** Dispatches a value to exactly one converter supporting its runtime type. */
public final class ConversionDispatcher<C, R, X> {

	@Getter
	private final List<? extends Converter<? extends C, R, X>> converters;

	public ConversionDispatcher(List<? extends Converter<? extends C, R, X>> converters) {
		this.converters = List.copyOf(converters);
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
