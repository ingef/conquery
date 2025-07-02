package com.bakdata.conquery.util.search.solr;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import com.google.common.base.Stopwatch;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;


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

	private final Duration commitWithin;
	public final int updateChunkSize;

	/**
	 * Buffer docs to send larger update chunks (size {@link FilterValueIndexer#updateChunkSize}).
	 */
	private final LinkedList<SolrFrontendValue> openDocs = new LinkedList<>();


	/**
	 * Keep track of empty value
	 */
	private boolean seenEmpty = false;

	/**
	 * We keep track of values that we send to solr to lower network traffic.
	 * Because we receive values individually from the shards, a value is probably seen multiple times.
	 */
	private final Set<String> seenValues = ConcurrentHashMap.newKeySet();

	Throwable clientError = null;


	@Override
	public void finalizeSearch() {
		if (openDocs.isEmpty()) {
			return;
		}

		// Commit what is left
		log.info("Commiting the last {} documents of {}", openDocs.size(), searchable);
		registerValues(openDocs);
		openDocs.clear();
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

		scheduleForIndex(solrFrontendValue);

	}

	private void scheduleForIndex(SolrFrontendValue solrFrontendValue) {

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

		openDocs.add(solrFrontendValue);

		// We chunk here for performance.
		// Too many small document request cause a lot of overhead.
		// A too large chunk slows request submission and solr.
		while (openDocs.size() >= updateChunkSize) {
			log.trace("Adding {} documents for {}", openDocs.size(), searchable);
			registerValues(openDocs);
			openDocs.clear();
		}
	}

	private int getCommitWithinMs() {
		return (int) Math.min(commitWithin.toMilliseconds(), Integer.MAX_VALUE);
	}

	public void registerValuesRaw(Collection<String> values) {
		List<SolrFrontendValue> solrFrontendValues = values.stream()
														   .filter(Objects::nonNull)
														   .filter(Predicate.not(String::isBlank))
														   .map(value -> new SolrFrontendValue(searchable, sourcePriority, value, null, null))
														   .toList();

		solrFrontendValues.forEach(this::scheduleForIndex);
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
			log.debug("BEGIN registering {} values to {} for {}", solrFrontendValues.size(), solrClient.getDefaultCollection(), searchable);
			solrClient.addBeans(solrFrontendValues, getCommitWithinMs());
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
