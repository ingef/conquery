package com.bakdata.conquery.io.jackson;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.base.Throwables;

/**
 * Interface for class instances that need initialization after deserialization and value injection.
 * Let the class implement this interface and annotate the class with:
 * <pre>
 *  {@code
 *  @JsonDeserialize(converter = Initializing.Converter.class )
 *  }
 * </pre>
 * @param <T>
 * @implNote Every class that inherits from an initializing class needs to define its own Converter.
 * Otherwise, the class is parsed as the super class, and its overrides are not called.
 */
public interface Initializing {

	void init() throws Exception;

	class Converter<T extends Initializing> extends StdConverter<T, T> {

		@Override
		public T convert(T value) {
			try {
				value.init();
				return value;
			}
			catch (Exception e) {
				Throwables.throwIfUnchecked(e);
				throw new IllegalStateException("Could not initialize %s".formatted(value), e);
			}
		}
	}
}
