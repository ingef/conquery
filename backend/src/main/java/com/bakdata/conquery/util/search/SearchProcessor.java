package com.bakdata.conquery.util.search;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import io.dropwizard.lifecycle.Managed;

import javax.annotation.CheckForNull;

public interface SearchProcessor extends Managed {
	void clearSearch();

	Job createUpdateFilterSearchJob(NamespaceStorage storage, Consumer<Set<Column>> columnsConsumer);

	void registerValues(Searchable searchable, Collection<String> values);

	long getTotal(SelectFilter<?> filter);

	void finalizeSearch(Searchable searchable);

	void indexManagerResidingSearches(Set<Searchable> managerSearchables, AtomicBoolean cancelledState, ProgressReporter progressReporter) throws InterruptedException;

	List<FrontendValue> findExact(SelectFilter<?> filter, String searchTerm);

	ConceptsProcessor.AutoCompleteResult query(SelectFilter<?> searchable, @CheckForNull String maybeText, int itemsPerPage, int pageNumber);
}
