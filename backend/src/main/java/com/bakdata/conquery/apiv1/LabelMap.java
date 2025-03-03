package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.search.SearchConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.InternalFilterSearch;
import com.bakdata.conquery.util.search.Search;
import com.google.common.collect.BiMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

@Getter
@RequiredArgsConstructor
@Slf4j
@EqualsAndHashCode
public class LabelMap implements Searchable<FrontendValue> {

	private final FilterId id;
	@Delegate
	private final BiMap<String, String> delegate;
	private final int minSuffixLength;
	private final boolean generateSearchSuffixes;

	@Override
	public Search<FrontendValue> createSearch(SearchConfig config) {

		final Search<FrontendValue> search = config.createSearch(this);

		final List<FrontendValue> collected = delegate.entrySet().stream()
													  .map(entry -> new FrontendValue(entry.getKey(), entry.getValue()))
													  .toList();

		if (log.isTraceEnabled()) {
			log.trace("Labels for {}: `{}`", getId(), collected.stream().map(FrontendValue::toString).collect(Collectors.toList()));
		}

		StopWatch timer = StopWatch.createStarted();
		log.trace("START-SELECT ADDING_ITEMS for {}", getId());

		collected.forEach(feValue -> search.addItem(feValue, InternalFilterSearch.extractKeywords(feValue)));

		log.trace("DONE-SELECT ADDING_ITEMS for {} in {}", getId(), timer);

		timer.reset();
		log.trace("START-SELECT SHRINKING for {}", getId());

		search.finalizeSearch();

		log.trace("DONE-SELECT SHRINKING for {} in {}", getId(), timer);

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
