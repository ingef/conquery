package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
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

@Slf4j
@RequiredArgsConstructor
public class SolrSearch extends Search<FrontendValue> {

	public static final int UPDATE_CHUNK_SIZE = 1000;
	private final SolrClient solrClient;

	@Getter
	private final Searchable<?> searchable;

	private final Duration commitWithin;

	/**
	 * Buffer docs to send larger update chunks (size {@link SolrSearch#UPDATE_CHUNK_SIZE}).
	 */
	private final LinkedList<SolrFrontendValue> openDocs = new LinkedList<>();


	@Override
	public void finalizeSearch() {
		if (openDocs.isEmpty()) {
			return;
		}

		// Commit what is left
		log.info("Commiting the last {} documents of {}", openDocs.size(), searchable);
		registerValues(openDocs);
		openDocs.clear();
	}

	@Override
	public long calculateSize() {

		// We query all documents that reference the searchables of the filter
		SolrQuery query = new SolrQuery("%s:%s".formatted(SolrFrontendValue.Fields.searchable_s, searchable.getId()));

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
		throw new UnsupportedOperationException("Must not be used. Use " + CombinedSolrSearch.class);
	}

	@Override
	public Iterator<FrontendValue> iterator() {
		throw new UnsupportedOperationException("Must not be used. Use " + CombinedSolrSearch.class);
	}

	@Override
	public void addItem(FrontendValue feValue, List<String> _keywords) {
		if (feValue.getValue().isEmpty()) {
			log.warn("Skip indexing of {}, because its 'value' is empty.", feValue);
			return;
		}
		SolrFrontendValue solrFrontendValue = new SolrFrontendValue(searchable, feValue);

		openDocs.add(solrFrontendValue);

		// We chunk here for performance.
		// Too many small document request cause a lot of overhead.
		// A too large chunk slows request submission and solr.
		while (openDocs.size() >= UPDATE_CHUNK_SIZE) {
			log.trace("Adding {} documents for {}", openDocs.size(), searchable.getId());
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
														   .map(value -> new SolrFrontendValue(searchable, value, value, null))
														   .toList();

		registerValues(solrFrontendValues);
	}

	public void registerValues(Collection<SolrFrontendValue> solrFrontendValues) {
		try {
			Stopwatch stopwatch = Stopwatch.createStarted();
			log.info("BEGIN registering {} values to {} for {} {}", solrFrontendValues.size(), solrClient.getDefaultCollection(), searchable.getClass().getSimpleName(), searchable.getId());
			solrClient.addBeans(solrFrontendValues, getCommitWithinMs());
			log.info("DONE registering {} values to {} for {} {} in {}", solrFrontendValues.size(), solrClient.getDefaultCollection(), searchable.getClass().getSimpleName(), searchable.getId(), stopwatch);
		}
		catch (SolrServerException | IOException e) {
			throw new IllegalStateException("Unable to register values for searchable '%s'".formatted(searchable), e);
		}
	}
}
