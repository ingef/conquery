package com.bakdata.conquery.io.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Helper interface to build bridge implementations for {@link io.dropwizard.jackson.Jackson}.
 */
public interface Injectable {

	default ObjectReader injectIntoNew(ObjectReader reader) {
		// If is already MutableInjectable, add my values to other, else begin from scratch.
		if (reader.getInjectableValues() instanceof MutableInjectableValues) {
			return reader.with(inject(((MutableInjectableValues) reader.getInjectableValues()).copy()));
		}

		return reader.with(inject(new MutableInjectableValues()));
	}

	default ObjectMapper injectIntoNew(ObjectMapper mapper) {
		// If is already MutableInjectable, add my values to other, else begin from scratch.

		if (mapper.getInjectableValues() instanceof MutableInjectableValues) {
			return mapper.copy()
						 .setInjectableValues(inject(((MutableInjectableValues) mapper.getInjectableValues()).copy()));
		}
		// TODO unsuse
		return mapper.copy()
					 .setInjectableValues(inject(new MutableInjectableValues()));
	}

	default void injectInto(ObjectMapper mapper) {
		// If is already MutableInjectable, add my values to other, else begin from scratch.

		if (mapper.getInjectableValues() instanceof MutableInjectableValues) {
			mapper.setInjectableValues(inject(((MutableInjectableValues) mapper.getInjectableValues())));
			return;
		}
		throw new IllegalStateException("Cannot add additional injectables if the mapper does not provide MutableInjectableValues");
	}

	MutableInjectableValues inject(MutableInjectableValues values);
}
