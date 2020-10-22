package com.bakdata.conquery.io.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public interface Injectable {

	public default ObjectReader injectInto(ObjectReader reader) {
		if(reader.getInjectableValues() instanceof MutableInjectableValues) {
			return reader.with(
				inject(
					((MutableInjectableValues)reader.getInjectableValues()).copy()
				)
			);
		}
		return reader
			.with(
				inject(new MutableInjectableValues())
			);
	}
	
	public default ObjectMapper injectInto(ObjectMapper mapper) {
		if(mapper.getInjectableValues() instanceof MutableInjectableValues) {
			return mapper.copy()
				.setInjectableValues(
					inject(
						((MutableInjectableValues)mapper.getInjectableValues()).copy()
					)
				);
		}
		return mapper
			.copy()
			.setInjectableValues(
				inject(new MutableInjectableValues())
			);
	}

	public MutableInjectableValues inject(MutableInjectableValues values);
}
