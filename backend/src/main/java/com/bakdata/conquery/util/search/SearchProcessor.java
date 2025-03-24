package com.bakdata.conquery.util.search;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.jobs.Job;
import io.dropwizard.lifecycle.Managed;

public interface SearchProcessor extends Managed {
	void clearSearch();

	Job createUpdateFilterSearchJob(NamespaceStorage storage, Consumer<Set<Column>> columnsConsumer);

	void registerValues(Searchable<FrontendValue> searchable, Collection<String> values);

	long getTotal(SelectFilter<?> filter);

	List<Search<FrontendValue>> getSearchesFor(SelectFilter<?> searchable);

	Iterator<FrontendValue> listAllValues(SelectFilter<?> searchable);

	void finalizeSearch(Searchable<FrontendValue> searchable);

	List<FrontendValue> topItems(SelectFilter<?> searchable, String text);

	void indexManagerResidingSearches(Set<Searchable<FrontendValue>> managerSearchables, AtomicBoolean cancelledState) throws InterruptedException;

	List<FrontendValue> findExact(SelectFilter<?> filter, String searchTerm);
}
