package com.bakdata.conquery.models.config.search;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM )
public interface SearchConfig {
	<T extends Comparable<T>> Search<T> createSearch(Searchable<T> searchable);

	SearchProcessor createSearchProcessor();
}
