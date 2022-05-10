package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.models.index.InternToExternMapper;

public interface MappedSelect {

	InternToExternMapper getMapping();

	default String doTransformValue(Object intern) {
		return getMapping().external(String.valueOf(intern));
	}

}
