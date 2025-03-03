package com.bakdata.conquery.models.datasets.concepts;

import jakarta.validation.constraints.Min;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.search.SearchConfig;
import com.bakdata.conquery.models.index.IndexCreationException;
import com.bakdata.conquery.models.query.InternalFilterSearch;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.internal.TrieSearch;

/**
 * @implNote This class is tightly coupled with {@link InternalFilterSearch} and {@link com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter}.
 * <p>
 * Searchable classes describe how a search should be constructed, and provide the values with getSearchValues.
 */
public interface Searchable<T> {

	/**
	 * All available {@link FrontendValue}s for searching in a {@link TrieSearch}.
	 * TODO unwind this as it should be handled by the respective search config
	 */
	Search<T> createSearch(SearchConfig config) throws IndexCreationException;

	/**
	 * Parameter used in the construction of {@link TrieSearch}, defining the shortest suffix to create.
	 * Ignored if isGenerateSuffixes is true.
	 */
	@Min(0)
	int getMinSuffixLength();

	/**
	 * If true, the underlying {@link TrieSearch} will not generate any suffixes. This can help reduce pressure on memory.
	 */
	boolean isGenerateSuffixes();

	/**
	 * Feature to disable search on a column completely. This has no benefit to the users, but can be used to reduce strain on memory and time spent indexing large columns.
	 */
	boolean isSearchDisabled();
}
