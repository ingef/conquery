package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.LabelMap;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.index.FrontendValueIndex;
import com.bakdata.conquery.models.index.FrontendValueIndexKey;
import com.bakdata.conquery.models.index.IndexCreationException;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.UpdateFilterSearchJob;
import com.bakdata.conquery.models.query.InternalFilterSearch;
import com.bakdata.conquery.resources.api.ConceptsProcessor.AutoCompleteResult;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.google.common.base.Stopwatch;
import com.google.common.collect.BiMap;
import com.google.common.collect.Sets;
import io.dropwizard.util.Duration;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

@Slf4j
@RequiredArgsConstructor
public class SolrProcessor implements SearchProcessor {

	@NonNull
	private final SolrClient solrClient;

	private final Duration commitWithin;

	private final int updateChunkSize;

	private final String queryTemplate;

	private final Map<Searchable<FrontendValue>, Search<FrontendValue>> searches = new ConcurrentHashMap<>();
	@Override
	public void clearSearch() {
		try {
			log.info("Clearing collection: {}", solrClient.getDefaultCollection());
			solrClient.deleteByQuery("*:*");
		}
		catch (SolrServerException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Job createUpdateFilterSearchJob(NamespaceStorage storage, Consumer<Set<Column>> columnsConsumer) {
		return new UpdateFilterSearchJob(storage, this, columnsConsumer);
	}

	@Override
	public void registerValues(Searchable<FrontendValue> searchable, Collection<String> values) {

		SolrSearch search = (SolrSearch) getSearchFor(searchable);

		search.registerValuesRaw(values);
	}

	@Override
	public long getTotal(SelectFilter<?> filter) {
		CombinedSolrSearch combinedSolrSearch = new CombinedSolrSearch(filter, this, solrClient, queryTemplate);

		return combinedSolrSearch.getTotal();
	}


	@Override
	public List<Search<FrontendValue>> getSearchesFor(SelectFilter<?> searchable) {
		List<Searchable<FrontendValue>> searchReferences = searchable.getSearchReferences();
		return searchReferences.stream().map(this::getSearchFor).toList();
	}

	private Search<FrontendValue> getSearchFor(Searchable<FrontendValue> searchable) {
		return searches.computeIfAbsent(searchable, searchRef -> new SolrSearch(solrClient, searchRef, commitWithin, updateChunkSize));
	}
	@Override
	public void finalizeSearch(Searchable<FrontendValue> searchable) {
		log.info("Finalizing Search for {}", searchable);
		searches.get(searchable).finalizeSearch();
	}

	public AutoCompleteResult topItems(SelectFilter<?> filter, String text, Integer start, Integer limit) {
		CombinedSolrSearch combinedSolrSearch = new CombinedSolrSearch(filter, this, solrClient, queryTemplate);

		return combinedSolrSearch.topItems(text, start, limit);
	}

	@Override
	public void indexManagerResidingSearches(Set<Searchable<FrontendValue>> managerSearchables, AtomicBoolean cancelledState, ProgressReporter progressReporter) throws InterruptedException {

		progressReporter.setMax(managerSearchables.size());
		// Most computations are cheap but data intensive: we fork here to use as many cores as possible.
		try(final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)) {

			final Map<Searchable<FrontendValue>, Search<FrontendValue>> searchCache = new ConcurrentHashMap<>();
			for (Searchable<FrontendValue> searchable : managerSearchables) {
				if (searchable instanceof Column) {
					throw new IllegalStateException("Columns should have been grouped out previously");
				}

				service.submit(() -> {

					final StopWatch watch = StopWatch.createStarted();

					log.info("BEGIN collecting entries for `{}`", searchable);

					try {
						if (searchable instanceof FilterTemplate temp) {
							indexFilterTemplate(searchable, temp);
						}
						else if (searchable instanceof LabelMap labelMap) {
							indexLabelMap(searchable, labelMap);
						}
						else {
							log.error("Unsupported searchable of type {}, skipping: {}", searchable.getClass(), searchable);
							return;
						}

						final Search<FrontendValue> search = getSearchFor(searchable);


						searchCache.put(searchable, search);

						log.debug(
								"DONE collecting {} entries for `{}`, within {}",
								search.calculateSize(),
								searchable,
								watch
						);
					}
					catch (Exception e) {
						log.error("Failed to create search for {}", searchable, e);
					}
					finally {
						progressReporter.report(1);
					}

				});
			}

			service.shutdown();


			while (!service.awaitTermination(1, TimeUnit.MINUTES)) {
				if (cancelledState.get()) {
					log.info("This job got canceled");
					service.shutdownNow();
					return;
				}
				log.debug("Still waiting for {} to finish.", Sets.difference(managerSearchables, searchCache.keySet()));
			}
			searchCache.values().forEach(Search::finalizeSearch);
		}

	}

	private void indexLabelMap(Searchable<FrontendValue> searchable, LabelMap labelMap) {
		Search<FrontendValue> search = getSearchFor(searchable);

		BiMap<String, String> delegate = labelMap.getDelegate();
		FilterId id = labelMap.getId();

		final List<FrontendValue> collected = delegate.entrySet().stream()
													  .map(entry -> new FrontendValue(entry.getKey(), entry.getValue()))
													  .toList();

		if (log.isTraceEnabled()) {
			log.trace("Labels for {}: `{}`", id, collected.stream().map(FrontendValue::toString).collect(Collectors.toList()));
		}

		StopWatch timer = StopWatch.createStarted();
		log.trace("BEGIN ADDING_ITEMS for {}", id);

		collected.forEach(feValue -> search.addItem(feValue, InternalFilterSearch.extractKeywords(feValue)));

		log.trace("DONE ADDING_ITEMS for {} in {}", id, timer);

		timer.reset();
		log.trace("BEGIN commit for {}", id);

		search.finalizeSearch();

		log.trace("DONE commit for {} in {}", id, timer);
	}

	private void indexFilterTemplate(Searchable<FrontendValue> searchable, FilterTemplate temp) {
		final URI resolvedURI = temp.getResolvedUri();
		log.trace("Resolved filter template reference url for search '{}': {}", temp.getId(), resolvedURI);

		try {
			final FrontendValueIndex search_ = temp.getIndexService().getIndex(new FrontendValueIndexKey(
					resolvedURI,
					temp.getColumnValue(),
					temp.getValue(),
					temp.getOptionValue(),
					() -> getSearchFor(searchable)
			));
		}
		catch (IndexCreationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<FrontendValue> findExact(SelectFilter<?> filter, String searchTerm) {
		CombinedSolrSearch combinedSolrSearch = new CombinedSolrSearch(filter, this, solrClient, queryTemplate);

		return combinedSolrSearch.topItemsExact(searchTerm, 0, 10).values();

	}

	@Override
	public AutoCompleteResult query(SelectFilter<?> searchable, Optional<String> maybeText, int itemsPerPage, int pageNumber) {

		int start = itemsPerPage * pageNumber;
		return topItems(searchable, maybeText.orElse(null), start, itemsPerPage);
	}

	/**
	 * Intended for tests only to ensure everything is commited.
	 */
	public void explicitCommit() throws SolrServerException, IOException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info("BEGIN explicit commit to core/collection {}", solrClient.getDefaultCollection());
		solrClient.commit();
		log.info("DONE explicit commit to core/collection {} in {}", solrClient.getDefaultCollection(), stopwatch);

	}
}
