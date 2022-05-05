package com.bakdata.conquery.models.datasets.concepts;

import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @implNote This class is tightly coupled with {@link com.bakdata.conquery.apiv1.FilterSearch} and {@link com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter}.
 *
 * Searchable classes describe how a search should be constructed, and provide the values.
 *
 * getSearchReference is used for when a Searchable object opts to delegate to an underlying other Searchable:
 * Currently this is only {@link com.bakdata.conquery.models.datasets.Column}, which may delegate to its underlying {@link com.bakdata.conquery.models.datasets.SecondaryIdDescription}, to pool all values from all Columns for that SecondaryId into a single search.
 */
public interface Searchable {
	/**
	 * All available {@link FEValue}s for searching.
	 */
	Stream<FEValue> getSearchValues(CSVConfig config, NamespaceStorage storage);

	/**
	 * The actual Searchable to use, if there is potential for deduplication/pooling.
	 */
	@JsonIgnore
	default Searchable getSearchReference() {
		//Hopefully the only candidate will be Column
		return this;
	}

	/**
	 * Parameter used in the construction of {@link com.bakdata.conquery.util.search.TrieSearch}, defining the shortest suffix to create.
	 * Ignored if isGenerateSuffixes is true.
	 */
	int getMinSuffixLength();

	/**
	 * If true, the underlying {@link com.bakdata.conquery.util.search.TrieSearch} will not generate any suffixes. This can help reduce pressure on memory.
	 */
	boolean isGenerateSuffixes();
}
