package com.bakdata.conquery.util.search.solr;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.LabelMap;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.search.solr.FilterValueConfig;
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
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;

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

@Slf4j
@RequiredArgsConstructor
public class SolrProcessor implements SearchProcessor, Managed {

	@NonNull
	private final Supplier<SolrClient> solrSearchClientFactory;

	@NonNull
	private final Supplier<SolrClient> solrIndexClientFactory;

	private final Duration commitWithin;

	private final int updateChunkSize;

	private final FilterValueConfig filterValueConfig;

	private final Map<String, Search<FrontendValue>> searches = new ConcurrentHashMap<>();

	private SolrClient solrSearchClient;

	private SolrClient solrIndexClient;

	@Override
	public void start() throws Exception {
		solrSearchClient = solrSearchClientFactory.get();
		solrIndexClient = solrIndexClientFactory.get();
	}

	@Override
	public void stop() throws Exception {
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
		return new UpdateFilterSearchJob(storage, this, columnsConsumer);
	}

	@Override
	public void registerValues(Searchable searchable, Collection<String> values) {

		FilterValueIndexer search = (FilterValueIndexer) getSearchFor(searchable);

		search.registerValuesRaw(values);
	}

	@Override
	public long getTotal(SelectFilter<?> filter) {
		FilterValueSearch filterValueSearch = new FilterValueSearch(filter, this, solrSearchClient, filterValueConfig);
		return filterValueSearch.getTotal();
	}


	/**
	 * Helper to build referable names for search sources which may allow abstraction.
	 * E.g. if column names are used across multiple tables and holds the same set of values, we may want to create only a single document in solr not one for every column.
	 * @param searchable The searchable whose name is created.
	 * @return the name vor the searchable
	 */
	private String buildNameForSearchable(Searchable searchable) {
		String name = switch (searchable) {
			case Column column -> {

				String columnGroup = filterValueConfig.getColumnGroup(column);
				if (columnGroup == null) {
					yield column.getSearchHandle();
				}
				log.trace("Mapping column {} to search group {}", column.getId(), columnGroup);
				yield "shared_column_" + columnGroup;
			}
			default -> searchable.getSearchHandle();
		};

		name = ClientUtils.escapeQueryChars(name);

		return name;
	}

	static int getSourcePriority(Searchable searchable) {
		return switch (searchable) {
			case SolrEmptySeachable ignore -> 0;
			case LabelMap ignore -> 1;
			case FilterTemplate ignore -> 2;
			case Column ignore -> 3;
			default -> Integer.MAX_VALUE;
		};
	}

	/*package*/ Search<FrontendValue> getSearchFor(Searchable searchable) {
		String nameForSearchable = buildNameForSearchable(searchable);
		int sourcePriority = getSourcePriority(searchable);
		return searches.computeIfAbsent(nameForSearchable, searchRef -> new FilterValueIndexer(solrIndexClient, nameForSearchable, sourcePriority, commitWithin, updateChunkSize));
	}

	@Override
	public void finalizeSearch(Searchable searchable) {
		String nameForSearchable = buildNameForSearchable(searchable);
		log.info("Finalizing Search for {}", searchable);
		Search<FrontendValue> frontendValueSearch = searches.get(nameForSearchable);

		if (frontendValueSearch == null) {
			log.info("Skipping finalization of {}, because it does not exist", searchable);
			return;
		}

		frontendValueSearch.finalizeSearch();
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

						final Search<FrontendValue> search = getSearchFor(searchable);


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
		Search<FrontendValue> search = getSearchFor(SolrEmptySeachable.INSTANCE);
		search.addItem(new FrontendValue("", filterValueConfig.getEmptyLabel()), List.of(""));

		search.finalizeSearch();
	}

	private void indexLabelMap(Searchable searchable, LabelMap labelMap) {
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

	private void indexFilterTemplate(Searchable searchable, FilterTemplate temp) {
		final URI resolvedURI = temp.getResolvedUri();
		log.trace("Resolved filter template reference url for search '{}': {}", temp.getId(), resolvedURI);

		try {
			final FrontendValueIndex ignore = temp.getIndexService().getIndex(new FrontendValueIndexKey(
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
		FilterValueSearch filterValueSearch = new FilterValueSearch(filter, this, solrSearchClient, filterValueConfig);

		return filterValueSearch.topItemsExact(searchTerm, 0, 10).values();

	}

	@Override
	public AutoCompleteResult query(SelectFilter<?> searchable, String maybeText, int itemsPerPage, int pageNumber) {

		int start = itemsPerPage * pageNumber;
		return topItems(searchable, maybeText, start, itemsPerPage);
	}

	/**
	 * Intended for tests only to ensure everything is commited.
	 */
	public void explicitCommit() throws SolrServerException, IOException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info("BEGIN explicit commit to core/collection {}", solrSearchClient.getDefaultCollection());
		solrSearchClient.commit();
		log.info("DONE explicit commit to core/collection {} in {}", solrSearchClient.getDefaultCollection(), stopwatch);

	}
}
