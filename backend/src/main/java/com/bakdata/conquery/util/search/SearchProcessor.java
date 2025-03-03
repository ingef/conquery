package com.bakdata.conquery.util.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.jobs.Job;

public interface SearchProcessor {
	void clearSearch();

	Job createUpdateFilterSearchJob(NamespaceStorage storage, Consumer<Set<Column>> columnsConsumer);

	void addSearches(Map<Searchable<FrontendValue>, Search<FrontendValue>> searchCache);

	void registerValues(Searchable<FrontendValue> searchable, Collection<String> values);

	long getTotal(SelectFilter<?> filter);

	List<Search<FrontendValue>> getSearchesFor(SelectFilter<?> searchable);

	void finalizeSearch(Searchable<FrontendValue> searchable);

	List<FrontendValue> topItems(SelectFilter<?> searchable, String text);
}
