package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.models.query.QueryResolveContext;

public interface QueryContextResolvable {

	default void resolve(QueryResolveContext context) {
	}

}