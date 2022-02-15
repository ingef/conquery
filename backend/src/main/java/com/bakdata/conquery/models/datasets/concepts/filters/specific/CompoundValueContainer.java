package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface CompoundValueContainer {

	default void resolve(QueryResolveContext context) {
	}

	;

}