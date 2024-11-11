package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.util.search.TrieSearch;
import com.google.common.collect.BiMap;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@ToString(onlyExplicitlyIncluded = true)
public class LabelMap implements Searchable {

	@ToString.Include
	private final FilterId id;

	@Delegate
	private final BiMap<String, String> delegate;
	private final int minSuffixLength;
	private final boolean generateSearchSuffixes;

	@Override
	public TrieSearch<FrontendValue> createTrieSearch(IndexConfig config) {

		final TrieSearch<FrontendValue> search = config.createTrieSearch(true);

		final List<FrontendValue> collected = delegate.entrySet().stream()
													  .map(entry -> new FrontendValue(entry.getKey(), entry.getValue()))
													  .collect(Collectors.toList());

		if (log.isTraceEnabled()) {
			log.trace("Labels for {}: `{}`", getId(), collected.stream().map(FrontendValue::toString).collect(Collectors.toList()));
		}

		collected.forEach(feValue -> search.addItem(feValue, FilterSearch.extractKeywords(feValue)));

		search.shrinkToFit();

		return search;
	}

	@Override
	public boolean isGenerateSuffixes() {
		return generateSearchSuffixes;
	}

	@Override
	public boolean isSearchDisabled() {
		return false;
	}
}
