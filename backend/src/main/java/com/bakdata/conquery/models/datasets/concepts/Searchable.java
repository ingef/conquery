package com.bakdata.conquery.models.datasets.concepts;

import java.util.List;
import java.util.stream.Stream;

import javax.validation.constraints.Min;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @implNote This class is tightly coupled with {@link com.bakdata.conquery.apiv1.FilterSearch} and {@link com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter}.
 *
 * Searchable classes describe how a search should be constructed, and provide the values with getSearchValues.
 *
 */
public interface Searchable {
	/**
	 * All available {@link FEValue}s for searching.
	 */
	Stream<FEValue> getSearchValues(CSVConfig config, NamespaceStorage storage);

	/**
	 * The actual Searchables to use, if there is potential for deduplication/pooling.
	 * @implSpec The order of objects returned is used to also sort search results from different sources.
	 */
	@JsonIgnore
	default List<Searchable> getSearchReferences() {
		//Hopefully the only candidate will be Column
		return List.of(this);
	}

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
