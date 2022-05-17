package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.models.index.InternToExternMapper;

public interface MappedSelect {

	InternToExternMapper getMapping();

	default Object doTransformValue(Object intern) {
		return intern == null ? "" : getMapping().external(intern.toString());
	}

}
