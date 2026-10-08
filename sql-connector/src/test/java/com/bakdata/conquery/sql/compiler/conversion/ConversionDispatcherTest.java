package com.bakdata.conquery.sql.compiler.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ConversionDispatcherTest {

	@Test
	void shouldDispatchToConverterSupportingRuntimeType() {
		ConversionDispatcher<Number, String, String> dispatcher = new ConversionDispatcher<>(List.of(
				converter(Integer.class, (value, prefix) -> prefix + value)
		));

		assertEquals("value=42", dispatcher.convert(42, "value="));
	}

	@Test
	void shouldRejectMissingConverter() {
		ConversionDispatcher<Number, String, Void> dispatcher = new ConversionDispatcher<>(List.of(
				converter(Integer.class, (value, context) -> value.toString())
		));

		assertThrows(IllegalStateException.class, () -> dispatcher.convert(1.5, null));
	}

	@Test
	void shouldRejectAmbiguousConverters() {
		ConversionDispatcher<Number, String, Void> dispatcher = new ConversionDispatcher<>(List.of(
				converter(Number.class, (value, context) -> value.toString()),
				converter(Integer.class, (value, context) -> value.toString())
		));

		assertThrows(IllegalStateException.class, () -> dispatcher.convert(42, null));
	}

	@Test
	void shouldDefensivelyCopyConverters() {
		List<Converter<? extends Number, String, Void>> converters = new ArrayList<>();
		converters.add(converter(Integer.class, (value, context) -> value.toString()));
		ConversionDispatcher<Number, String, Void> dispatcher = new ConversionDispatcher<>(converters);

		converters.clear();

		assertEquals("42", dispatcher.convert(42, null));
		assertThrows(UnsupportedOperationException.class, () -> dispatcher.getConverters().clear());
	}

	private static <C extends Number, X> Converter<C, String, X> converter(
			Class<C> conversionClass,
			Conversion<C, X> conversion
	) {
		return new Converter<>() {
			@Override
			public Class<C> getConversionClass() {
				return conversionClass;
			}

			@Override
			public String convert(C input, X context) {
				return conversion.convert(input, context);
			}
		};
	}

	@FunctionalInterface
	private interface Conversion<C, X> {

		String convert(C input, X context);
	}
}
