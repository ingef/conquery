package com.bakdata.conquery.models.datasets.concepts;

import java.util.List;

import javax.validation.constraints.Min;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @implNote This class is tightly coupled with {@link FilterSearch} and {@link com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter}.
 * <p>
 * Searchable classes describe how a search should be constructed, and provide the values with getSearchValues.
 */
public interface Searchable {


	//TODO instead extend Identifiable properly
	public Id<?> getId();

	public Dataset getDataset();

	/**
	 * All available {@link FrontendValue}s for searching in a {@link TrieSearch}.
	 */
	List<TrieSearch<FrontendValue>> getSearches(IndexConfig config, NamespaceStorage storage);

	/**
	 * The actual Searchables to use, if there is potential for deduplication/pooling.
	 *
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
