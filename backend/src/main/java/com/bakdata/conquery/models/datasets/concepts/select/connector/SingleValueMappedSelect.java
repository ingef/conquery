package com.bakdata.conquery.models.datasets.concepts.select.connector;

import java.io.IOException;

import com.bakdata.conquery.models.index.InternToExternMapping;

public interface SingleValueMappedSelect {

	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SingleValueMappedSelect.class);

	InternToExternMapping getMapping();


	default String doTransformValue(Object intern) {
		if (!(intern instanceof String)) {
			throw new IllegalStateException(String.format("Expected a String got %s (Type: %s)", intern, intern.getClass().getName()));
		}

		try {
			return getMapping().external(String.valueOf(intern));
		}
		catch (IOException e) {
			log.warn("Error resolving {} to external", intern, e);
		}
		// If mapping was not successful return nothing
		return null;
	}

}
