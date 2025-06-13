package com.bakdata.conquery.models.config.search;


import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import jakarta.validation.constraints.Min;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.LabelMap;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.index.FrontendValueIndex;
import com.bakdata.conquery.models.index.FrontendValueIndexKey;
import com.bakdata.conquery.models.index.IndexCreationException;
import com.bakdata.conquery.models.query.InternalFilterSearch;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.bakdata.conquery.util.search.internal.TrieSearch;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.common.collect.BiMap;
import io.dropwizard.core.setup.Environment;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

@Data
@CPSType(id = "INTERNAL", base = SearchConfig.class)
@Slf4j
public class InternalSearchConfig implements SearchConfig {

	@JsonAlias("searchSuffixLength")
	@Min(0)
	private int ngramLength = 3;

	@Nullable
	private String searchSplitChars = "(),;.:\"'/";

	@NotNull
	private String emptyLabel = "No Value";



	@Override
	public SearchProcessor createSearchProcessor(Environment environment, DatasetId id) {
		return new InternalFilterSearch(this);
	}


	public TrieSearch<FrontendValue> createSearch(Searchable searchable) {
		if (searchable instanceof FilterTemplate temp) {

			return getFilterTemplateSearch(temp);
		}

		if (searchable instanceof LabelMap labelMap) {
			return getLabelMapSearch(labelMap);
		}

		return new TrieSearch<>(searchable.isGenerateSuffixes() ? getNgramLength() : Integer.MAX_VALUE, getSearchSplitChars());

	}

	private TrieSearch<FrontendValue> getLabelMapSearch(LabelMap labelMap) {
		TrieSearch<FrontendValue> search = new TrieSearch<>(labelMap.isGenerateSuffixes() ? getNgramLength() : Integer.MAX_VALUE, getSearchSplitChars());

		BiMap<String, String> delegate = labelMap.getDelegate();
		FilterId id = labelMap.getId();

		final List<FrontendValue> collected = delegate.entrySet().stream()
													  .map(entry -> new FrontendValue(entry.getKey(), entry.getValue()))
													  .toList();

		if (log.isTraceEnabled()) {
			log.trace("Labels for {}: `{}`", id, collected.stream().map(FrontendValue::toString).collect(Collectors.toList()));
		}

		StopWatch timer = StopWatch.createStarted();
		log.trace("START-SELECT ADDING_ITEMS for {}", id);

		collected.forEach(feValue -> search.addItem(feValue, InternalFilterSearch.extractKeywords(feValue)));

		log.trace("DONE-SELECT ADDING_ITEMS for {} in {}", id, timer);

		timer.reset();
		log.trace("START-SELECT SHRINKING for {}", id);

		search.finalizeSearch();

		log.trace("DONE-SELECT SHRINKING for {} in {}", id, timer);

		return search;
	}

	private TrieSearch<FrontendValue> getFilterTemplateSearch(FilterTemplate temp) {
		final URI resolvedURI = temp.getResolvedUri();
		log.trace("Resolved filter template reference url for search '{}': {}", temp.getId(), resolvedURI);

		final FrontendValueIndex search;
		try {
			search = temp.getIndexService().getIndex(new FrontendValueIndexKey(
					resolvedURI,
					temp.getColumnValue(),
					temp.getValue(),
					temp.getOptionValue(),
					() -> new TrieSearch<>(
							temp.isGenerateSuffixes() ? temp.getMinSuffixLength() : Integer.MAX_VALUE,
							getSearchSplitChars()
					)
			));
		}
		catch (IndexCreationException e) {
			throw new RuntimeException(e);
		}

		return (TrieSearch<FrontendValue>) search.getDelegate();
	}
}
