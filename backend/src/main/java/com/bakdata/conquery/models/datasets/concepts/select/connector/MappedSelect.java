package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.models.index.InternToExternMapper;

public interface MappedSelect {

	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MappedSelect.class);

	InternToExternMapper getMapping();


	default String doTransformValue(Object intern) {
		return getMapping().external(String.valueOf(intern));
	}

}
