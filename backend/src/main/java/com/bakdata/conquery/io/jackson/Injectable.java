package com.bakdata.conquery.io.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Helper interface to build bridge implementations for {@link io.dropwizard.jackson.Jackson}.
 */
public interface Injectable {

	/**
	 *	See {@link Injectable#injectIntoNew(ObjectMapper)} 
	 */
	default ObjectReader injectIntoNew(ObjectReader reader) {
		// If is already MutableInjectable, add my values to other, else begin from scratch.
		if (reader.getInjectableValues() instanceof MutableInjectableValues) {
			return reader.with(inject(((MutableInjectableValues) reader.getInjectableValues()).copy()));
		}

		return reader.with(inject(new MutableInjectableValues()));
	}

	/**
	 * Creates a copy of the provided mapper and its injected values and adds the caller to the new copy 
	 * @param mapper the blueprint mapper to use which remains untouched
	 * @return a new mapper with this injected
	 */
	default ObjectMapper injectIntoNew(ObjectMapper mapper) {
		// If is already MutableInjectable, add my values to other, else begin from scratch.

		if (mapper.getInjectableValues() instanceof MutableInjectableValues) {
			return mapper.copy()
						 .setInjectableValues(inject(((MutableInjectableValues) mapper.getInjectableValues()).copy()));
		}
		// TODO unsure if overriding is expected here from the user
		return mapper.copy()
					 .setInjectableValues(inject(new MutableInjectableValues()));
	}

	/**
	 * Injects this to the provided mapper and returns the mapper
	 */
	default ObjectMapper injectInto(ObjectMapper mapper) {
		// If is already MutableInjectable, add my values to other, else begin from scratch.

		if (mapper.getInjectableValues() instanceof MutableInjectableValues) {
			mapper.setInjectableValues(inject(((MutableInjectableValues) mapper.getInjectableValues())));
			return mapper;
		}
		throw new IllegalStateException("Cannot add additional injectables if the mapper does not provide MutableInjectableValues");
	}

	MutableInjectableValues inject(MutableInjectableValues values);
}
