package com.bakdata.conquery.models.datasets.concepts;

import com.bakdata.conquery.models.query.InternalFilterSearch;
import com.bakdata.conquery.util.search.internal.TrieSearch;
import jakarta.validation.constraints.Min;

/**
 * @implNote This class is tightly coupled with {@link InternalFilterSearch} and {@link com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter}.
 * <p>
 * Searchable classes describe how a search should be constructed, and provide the values with getSearchValues.
 */
public interface Searchable {

	String getSearchHandle();

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
