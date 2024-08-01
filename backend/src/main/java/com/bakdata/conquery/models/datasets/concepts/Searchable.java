package com.bakdata.conquery.models.datasets.concepts;

import java.util.List;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;

/**
 * @implNote This class is tightly coupled with {@link FilterSearch} and {@link com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter}.
 * <p>
 * Searchable classes describe how a search should be constructed, and provide the values with getSearchValues.
 */
public interface Searchable {

	/**
	 * All available {@link FrontendValue}s for searching in a {@link TrieSearch}.
	 */
	TrieSearch<FrontendValue> createTrieSearch(IndexConfig config);

	/**
	 * Parameter used in the construction of {@link com.bakdata.conquery.util.search.TrieSearch}, defining the shortest suffix to create.
	 * Ignored if isGenerateSuffixes is true.
	 */
	@Min(0)
	int getMinSuffixLength();

	/**
	 * If true, the underlying {@link com.bakdata.conquery.util.search.TrieSearch} will not generate any suffixes. This can help reduce pressure on memory.
	 */
	boolean isGenerateSuffixes();

	/**
	 * Feature to disable search on a column completely. This has no benefit to the users, but can be used to reduce strain on memory and time spent indexing large columns.
	 */
	boolean isSearchDisabled();
}
