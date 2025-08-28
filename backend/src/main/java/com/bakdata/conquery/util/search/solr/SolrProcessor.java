package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.LabelMap;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.search.solr.FilterValueConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.index.FrontendValueIndex;
import com.bakdata.conquery.models.index.FrontendValueIndexKey;
import com.bakdata.conquery.models.index.IndexCreationException;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.UpdateFilterSearchJob;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.ConceptsProcessor.AutoCompleteResult;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.google.common.base.Stopwatch;
import com.google.common.collect.BiMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;

/**
 * Entrypoint for searches that are handled by solr.
 * Each dataset has its own {@link SolrProcessor} and a corresponding collection.
 */
@Slf4j
@RequiredArgsConstructor
public class SolrProcessor implements SearchProcessor, Managed {

	@NonNull
	private final Supplier<SolrClient> solrSearchClientFactory;

	@NonNull
	private final Supplier<SolrClient> solrIndexClientFactory;

	/**
	 * Currently unused
	 */
	@SuppressWarnings("unused")
	private final Duration commitWithin;

	private final FilterValueConfig filterValueConfig;

	private final Map<String, FilterValueIndexer> indexers = new ConcurrentHashMap<>();

	private SolrClient solrSearchClient;

	private SolrClient solrIndexClient;

	/**
	 * Single threaded runtime for the chunk submitter.
	 * This is mainly used to decouple mina threads from the solr client in order to prevent blocking and to convert between {@link com.bakdata.conquery.models.messages.namespaces.specific.RegisterColumnValues}'s
	 * and solr chunk sizes.
	 */
	private final ExecutorService chunkDecoupleExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
																		  .setNameFormat("solr-submitter-%d")
																		  .setDaemon(true)
																		  .build());

	@Override
	public void start() throws Exception {
		refreshClients();
		log.info("Started solr search processor for {}", solrSearchClient.getDefaultCollection());
	}

	/**
	 * {@link SolrClient}s might fall into an error state, from which they cannot recover.
	 * This method recreated the clients.
	 */
	private synchronized void refreshClients() {
		if (solrSearchClient != null) {
			try {
				solrSearchClient.close();
			} catch (Exception e) {
				log.warn("Failed to close solr search client", e);
			}
		}
		if (solrIndexClient != null) {
			try {
				solrIndexClient.close();
			} catch (Exception e) {
				log.warn("Failed to close solr index client", e);
			}
		}

		solrSearchClient = solrSearchClientFactory.get();
		solrIndexClient = solrIndexClientFactory.get();

	}

	@Override
	public void stop() throws Exception {
		log.info("Stopping solr search processor for {}", solrSearchClient.getDefaultCollection());
		List<Runnable> runnables = chunkDecoupleExecutor.shutdownNow();
		log.debug("Cancelling {} runnables for solr", runnables.size());
		solrSearchClient.close();
		solrIndexClient.close();
	}

	@Override
	public void clearSearch() {

		try (SolrClient solrClient = solrSearchClientFactory.get()) {
			log.info("Clearing collection: {}", solrClient.getDefaultCollection());
			solrClient.deleteByQuery("*:*");
		} catch (SolrServerException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Job createUpdateFilterSearchJob(NamespaceStorage storage, Consumer<Set<Column>> columnsConsumer) {
		refreshClients();
		indexers.clear();
		return new UpdateFilterSearchJob(storage, this, columnsConsumer);
	}

	@Override
	public void registerValues(Searchable searchable, Collection<String> values) {

		FilterValueIndexer indexer = getIndexerFor(searchable);

		indexer.registerValuesRaw(values);
	}

	/**
	 * Helper to build referable names for search sources which may allow abstraction.
	 * E.g. if column names are used across multiple tables and holds the same set of values, we may want to create only a single document in solr not one for every column.
	 * @param searchable The searchable whose name is created.
	 * @return the name for the searchable
	 */
	private String buildNameForSearchable(Searchable searchable) {

		String name = searchable instanceof Column column
					  ? getNameFromColumn(column)
					  : searchable.getSearchHandle();

		name = ClientUtils.escapeQueryChars(name);

		return name;
	}

	private String getNameFromColumn(Column column) {
		String columnGroup = filterValueConfig.getColumnGroup(column);
		if (columnGroup == null) {
			return column.getSearchHandle();
		}
		log.trace("Mapping column {} to search group {}", column.getId(), columnGroup);
		return "shared_column_" + columnGroup;
	}

	/**
	 * Higher priority for lower number
	 * @param searchable The searchable entity that is map to a priority
	 * @return The priority
	 */
	static int getFilterValueSourcePriority(Searchable searchable) {
		return switch (searchable) {
			case SolrEmptySeachable ignore -> 0;
			case LabelMap ignore -> 1;
			case FilterTemplate ignore -> 2;
			case Column ignore -> 3;
			default -> Integer.MAX_VALUE;
		};
	}

	/*package*/ FilterValueIndexer getIndexerFor(Searchable searchable) {
		String nameForSearchable = buildNameForSearchable(searchable);
		int sourcePriority = getFilterValueSourcePriority(searchable);

		return indexers.computeIfAbsent(nameForSearchable,
										searchRef -> new FilterValueIndexer(sourcePriority,
																			new ChunkSubmitter(nameForSearchable,
																							   solrIndexClient,
																							   filterValueConfig.getUpdateChunkSize(),
																							   chunkDecoupleExecutor
																			)
										)
		);
	}

	@Override
	public void finalizeSearch(Searchable searchable) {
		String nameForSearchable = buildNameForSearchable(searchable);
		log.info("Finalizing Search for {}", searchable);
		FilterValueIndexer indexer = indexers.get(nameForSearchable);

		if (indexer == null) {
			log.info("Skipping finalization of {}, because it does not exist", searchable);
			return;
		}

		indexer.finalizeSearch();
	}

	public AutoCompleteResult topItems(SelectFilter<?> filter, String text, Integer start, Integer limit) {
		FilterValueSearch filterValueSearch = new FilterValueSearch(filter, this, solrSearchClient, filterValueConfig);

		return filterValueSearch.topItems(text, start, limit);
	}

	@Override
	public void indexManagerResidingSearches(Set<Searchable> managerSearchables, AtomicBoolean cancelledState, ProgressReporter progressReporter) throws InterruptedException {

		// Index an empty result for all searches
		indexEmptyLabel();

		progressReporter.setMax(managerSearchables.size());
		progressReporter.start();
		// Most computations are cheap but data intensive: we fork here to use as many cores as possible.
		try(final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)) {

			final Map<Searchable, Search<FrontendValue>> searchCache = new ConcurrentHashMap<>();
			for (Searchable searchable : managerSearchables) {
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

						final Search<FrontendValue> search = getIndexerFor(searchable);


						searchCache.put(searchable, search);

						log.debug(
								"DONE collecting entries for `{}`, within {}",
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

	private void indexEmptyLabel() {
		Search<FrontendValue> search = getIndexerFor(SolrEmptySeachable.INSTANCE);
		search.addItem(new FrontendValue("", filterValueConfig.getEmptyLabel()), List.of(""));

		search.finalizeSearch();
	}

	private void indexLabelMap(Searchable searchable, LabelMap labelMap) {
		Search<FrontendValue> search = getIndexerFor(searchable);

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

		collected.forEach(feValue -> search.addItem(feValue, SearchProcessor.extractKeywords(feValue)));

		log.trace("DONE ADDING_ITEMS for {} in {}", id, timer);

		timer.reset();
		log.trace("BEGIN commit for {}", id);

		search.finalizeSearch();

		log.trace("DONE commit for {} in {}", id, timer);
	}

	/**
	 * @implNote We use the {@link com.bakdata.conquery.models.index.IndexService} here because it can efficiently and asynchronously parse the CSVs referenced by the template.
	 * The resulting {@link com.bakdata.conquery.models.index.Index} is not used by a {@link com.bakdata.conquery.models.index.InternToExternMapper}. That's why
	 * {@link FilterValueIndexer} does not implement {@link Search#findExact(String, int)}
	 */
	private void indexFilterTemplate(Searchable searchable, FilterTemplate temp) {
		final URI resolvedURI = temp.getResolvedUri();
		log.trace("Resolved filter template reference url for search '{}': {}", temp.getId(), resolvedURI);

		try {
			final FrontendValueIndex ignore = temp.getIndexService().getIndex(new FrontendValueIndexKey(
					resolvedURI,
					temp.getColumnValue(),
					temp.getValue(),
					temp.getOptionValue(),
					() -> getIndexerFor(searchable)
			));
		}
		catch (IndexCreationException e) {
			throw new RuntimeException(e);
		}
	}

	public Collection<FrontendValue> findExact(SelectFilter<?> filter, String searchTerm) {

		return findExact(filter, List.of(searchTerm)).resolved();
	}

	@Override
	public ConceptsProcessor.ExactFilterValueResult findExact(SelectFilter<?> filter, List<String> searchTerms) {
		FilterValueSearch filterValueSearch = new FilterValueSearch(filter, this, solrSearchClient, filterValueConfig);

		return filterValueSearch.exact(searchTerms);
	}

	@Override
	public AutoCompleteResult query(SelectFilter<?> filter, String maybeText, int itemsPerPage, int pageNumber) {

		int start = itemsPerPage * pageNumber;
		return topItems(filter, maybeText, start, itemsPerPage);
	}

	@Override
	public Job createFinalizeFilterSearchJob(Namespace namespace, Set<ColumnId> columns) {
		return new FinalizeColumnValuesIndexJob(columns, this);
	}

	/**
	 * Public only for tests purposes, to ensure everything is commited.
	 */
	public void explicitCommit() throws SolrServerException, IOException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info("BEGIN explicit commit to core/collection {}", solrIndexClient.getDefaultCollection());
		solrIndexClient.commit();
		// Depending on the collection size optimize takes a long time, longer than our client timeout, so we don't wait here.
		solrIndexClient.optimize(false, false);
		log.info("DONE explicit commit to core/collection (optimize might still be pending) {} in {}", solrIndexClient.getDefaultCollection(), stopwatch);

	}

	@RequiredArgsConstructor
	public static class FinalizeColumnValuesIndexJob extends Job {

		private final Collection<ColumnId> columns;
		private final SolrProcessor solrProcessor;

		@Override
		public void execute() throws Exception {
			getProgressReporter().setMax(columns.size() + 1 /* final commit */);
			try {

				Stopwatch stopwatch = Stopwatch.createStarted();
				try(ExecutorService executorService = Executors.newFixedThreadPool(4)) {
					for (ColumnId columnId : columns) {
						executorService.submit(() -> {
							solrProcessor.finalizeSearch(columnId.resolve());
							getProgressReporter().report(1);
						});
					}
				}
				solrProcessor.explicitCommit();
				getProgressReporter().report(1);
				log.info("Finished commit on collection {} in {}", solrProcessor.solrIndexClient.getDefaultCollection(), stopwatch);
			} catch (Exception e) {
				log.error("Unable to issue explicit commit on collection {}", solrProcessor.solrIndexClient.getDefaultCollection(), e);
			}
		}

		@Override
		public String getLabel() {
			return "FinalizeColumnValuesIndexJob on %d columns".formatted(columns.size());
		}
	}
}
