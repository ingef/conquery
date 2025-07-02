package com.bakdata.conquery.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import io.dropwizard.lifecycle.Managed;

public interface SearchProcessor extends Managed {

	/**
	 * From a given {@link FrontendValue} extract all relevant keywords.
	 */
	static List<String> extractKeywords(FrontendValue value) {
		final List<String> keywords = new ArrayList<>(3);

		keywords.add(value.getLabel());
		keywords.add(value.getValue());

		if (value.getOptionValue() != null) {
			keywords.add(value.getOptionValue());
		}

		return keywords;
	}

	/**
	 * Removes all indexed data for this dataset
	 */
	void clearSearch();

	/**
	 * Job that initiates the search on {@link FrontendValue}s
	 */
	Job createUpdateFilterSearchJob(NamespaceStorage storage, Consumer<Set<Column>> columnsConsumer);

	/**
	 * Callback for the {@link com.bakdata.conquery.models.messages.namespaces.specific.RegisterColumnValues} job to submit column values to the search engine for indexing.
	 * @param searchable Source of the values
	 * @param values Values to register
	 */
	void registerValues(Searchable searchable, Collection<String> values);


	void finalizeSearch(Searchable searchable);

	/**
	 * Callback for {@link com.bakdata.conquery.models.jobs.UpdateFilterSearchJob} to index values that are present to the manager.
	 */
	void indexManagerResidingSearches(Set<Searchable> managerSearchables, AtomicBoolean cancelledState, ProgressReporter progressReporter) throws InterruptedException;


	/**
	 * Query for an exact matching {@link FrontendValue}.
	 * Matches {@link FrontendValue#getValue()} or {@link FrontendValue#getLabel()} but case-insensitive.
	 * @param filter The filter to the resulting value must correspond to (domain of the {@link FrontendValue})
	 * @param searchTerms The exact terms to match
	 * @return A container with the exact matches and unmatched values.
	 */
	ConceptsProcessor.ExactFilterValueResult findExact(SelectFilter<?> filter, List<String> searchTerms);

	/**
	 * Query for close matches or general recommendations (empty search)
	 * @param filter The filter to the resulting value must correspond to (domain of the {@link FrontendValue})
	 * @param maybeText A search term
	 * @param itemsPerPage Pagination: items per page
	 * @param pageNumber Pagination: page (zero-based)
	 * @return Found matches
	 */
	ConceptsProcessor.AutoCompleteResult query(SelectFilter<?> filter, @CheckForNull String maybeText, int itemsPerPage, int pageNumber);

	/**
	 * Job to run after all ColumnValues are processed by the manager.
	 * It is a job, so it is definitely processed after UpdateFilterSearchJob.
	 */
	Job createFinalizeFilterSearchJob(Namespace namespace, @NotNull Set<ColumnId> columns);
}
