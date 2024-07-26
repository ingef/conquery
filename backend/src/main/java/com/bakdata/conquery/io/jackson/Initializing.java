package com.bakdata.conquery.io.jackson;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Interface for class instances that need initialization after deserialization and value injection.
 * Let the class implement this interface and annotate the class with:
 * <pre>
 *  {@code
 *  @JsonDeserialize(converter = Initializing.Converter.class )
 *  }
 * </pre>
 * @param <T> the class to be initialized
 *
 * @implNote cannot be used on classes with a back reference. In this case make the reference managing class
 * initializable and let it initialize its children.
 */
public interface Initializing<T extends Initializing<T>> {

	T init();

	class Converter<T extends Initializing<T>> extends StdConverter<T, T> {

		@Override
		public T convert(T value) {
			value.init();
			return value;
		}
	}
}
