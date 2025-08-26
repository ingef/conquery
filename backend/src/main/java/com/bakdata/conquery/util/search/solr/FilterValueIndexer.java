package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.messages.namespaces.specific.RegisterColumnValues;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import com.google.common.base.Stopwatch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;


/**
 * Helper class to index {@link com.bakdata.conquery.apiv1.query.concept.filter.FilterValue}s
 */
@Slf4j
@RequiredArgsConstructor
public class FilterValueIndexer extends Search<FrontendValue> {

	private final SolrClient solrClient;

	@Getter
	private final String searchable;
	private final int sourcePriority;

	public final int updateChunkSize;

	/**
	 * Current chunk that is filled with column values through {@link RegisterColumnValues} -- only a single thread/job accesses it at a time.
	 * A filled chunk is inserted into {@link FilterValueIndexer#openChunks} and is concurrently extracted by {@link FilterValueIndexer#submitter}
	 */
	private Collection<SolrFrontendValue> openChunk = new ArrayList<>();

	/**
	 * Buffer docs to send repartitioned update chunks.
	 */
	private final Queue<Collection<SolrFrontendValue>> openChunks = new ConcurrentLinkedQueue<>();


	/**
	 * Keep track of empty value
	 */
	private boolean seenEmpty = false;

	/**
	 * We keep track of values that we send to solr to lower network traffic.
	 * Because we receive values individually from the shards, a value is probably seen multiple times.
	 */
	private final Set<String> seenValues = ConcurrentHashMap.newKeySet();

	private Throwable clientError = null;

	/**
	 * Single threaded runtime for the chunk submitter.
	 */
	private final ExecutorService executor;

	/**
	 * Asynchronous submitter for solr docs.
	 */
	private CompletableFuture<Void> submitter = CompletableFuture.completedFuture(null);


	@Override
	public void finalizeSearch() {
		if (openChunk.isEmpty() && openChunks.isEmpty()) {
			return;
		}

		openChunks.add(openChunk);

		// Commit what is left
		log.trace("Commiting the last {} documents of {}", openChunk.size(), searchable);
		openChunk = new ArrayList<>();
		submitChunk().join();
		openChunks.clear();
		seenValues.clear();
	}

	@Override
	public long calculateSize() {

		// We query all documents that reference the searchables of the filter
		SolrQuery query = new SolrQuery("%s:%s".formatted(SolrFrontendValue.Fields.searchable_s, searchable));

		// Set rows to 0 because we don't want actual results, we are only interested in the total number
		query.setRows(0);

		try {
			QueryResponse response = solrClient.query(query);
			return response.getResults().getNumFound();
		}
		catch (SolrServerException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<FrontendValue> findExact(String searchTerm, int maxValue) {
		throw new UnsupportedOperationException("Must not be used. Use " + FilterValueSearch.class);
	}

	@Override
	public Iterator<FrontendValue> iterator() {
		throw new UnsupportedOperationException("Must not be used. Use " + FilterValueSearch.class);
	}

	@Override
	public void addItem(FrontendValue feValue, List<String> _keywords) {

		SolrFrontendValue solrFrontendValue = new SolrFrontendValue(searchable, sourcePriority, feValue);

		insertIntoChunk(solrFrontendValue);

	}

	private void insertIntoChunk(SolrFrontendValue solrFrontendValue) {

		if (solrFrontendValue.value_s.isEmpty()) {
			if (seenEmpty) {
				log.trace("Skip indexing of {} for {}, because its 'value' is empty and was already added.", solrFrontendValue, searchable);
				return;
			}
			seenEmpty = true;

		}

		if (!seenValues.add(solrFrontendValue.value_s)) {
			log.trace("Skip indexing of {} for {}, because its 'value' has already been submitted to solr.", solrFrontendValue, searchable);
			return;
		}

		openChunk.add(solrFrontendValue);

		if (openChunk.size() >= updateChunkSize) {
			openChunks.add(openChunk);
			openChunk = new ArrayList<>(updateChunkSize);
		}
	}

	private synchronized CompletableFuture<Void> submitChunk() {
		submitter =  submitter.thenRunAsync(
				() -> {
					if (openChunks.isEmpty()) {
						return;
					}

					// We chunk here for performance.
					// Too many small document request cause a lot of overhead.
					// A too large chunk slows request submission and solr.
					do  {
						int chunksCount = openChunks.size();

						Collection<SolrFrontendValue> chunk = openChunks.poll();
						log.debug("Adding {} (of currently ca. {}) documents for {}", chunk.size(), chunksCount*updateChunkSize, searchable);
						registerValues(chunk);
					} while (!openChunks.isEmpty());
				},
				executor
		);

		return submitter;
	}

	public void registerValuesRaw(Collection<String> values) {
		// Convert values and prepare chunks
		values.stream()
			   .filter(Objects::nonNull)
			   .filter(Predicate.not(String::isBlank))
			   .map(value -> new SolrFrontendValue(searchable, sourcePriority, value, null, null))
			   .forEach(this::insertIntoChunk);

		// Check if there is already a submitter working on pending docs
		if (submitter.isDone()) {
			// Start submitting so we can start free pending documents
			submitChunk();
		}
	}

	private void registerValues(Collection<SolrFrontendValue> solrFrontendValues) {
		if (clientError != null) {
			log.error("Cannot index value for {}, because client had an error previously", searchable);
			return;
		}

		if (solrFrontendValues.isEmpty()) {
			// Avoid "BaseHttpSolrClient$RemoteSolrException: ... missing content stream" on empty collection
			return;
		}

		try {
			Stopwatch stopwatch = Stopwatch.createStarted();
			log.trace("BEGIN registering {} values to {} for {}", solrFrontendValues.size(), solrClient.getDefaultCollection(), searchable);
			solrClient.addBeans(solrFrontendValues); // do not commit yet
			log.trace("DONE registering {} values to {} for {} in {}", solrFrontendValues.size(), solrClient.getDefaultCollection(), searchable, stopwatch);

		}
		catch (SolrServerException | IOException e) {
			clientError = e;
			try {
				solrClient.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			throw new IllegalStateException("Unable to register values for searchable '%s'".formatted(searchable), e);
		}
	}
}
