package com.bakdata.conquery.io.jackson;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Interface for class instances that need initialization after deserialization and value injection.
 *
 * Let the class implement this interface and annotate the class with:
 * <pre>
 *  {@code
 *  @JsonDeserialize(converter = Initializing.Converter.class )
 *  }
 * </pre>
 * @param <T>
 */
public interface Initializing<T extends Initializing<T>> {

    T init();

    class Converter<T extends Initializing<T>> extends StdConverter<T, T> {

        @Override
        public T convert(T value) {
            return value.init();
        }
    }
}
