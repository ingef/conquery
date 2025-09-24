package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;


/**
 * Helper class to index {@link com.bakdata.conquery.apiv1.query.concept.filter.FilterValue}s in Solr.
 *
 * @implNote we implement {@link Search} because we use the {@link com.bakdata.conquery.models.index.IndexService} to parse and index external mappings from
 * {@link FilterTemplate}s.
 */
@Slf4j
@RequiredArgsConstructor
public class FilterValueIndexer extends Search<FrontendValue> {

	private final int sourcePriority;

	private final ChunkSubmitter chunkSubmitter;


	/**
	 * Keep track of empty value
	 */
	private boolean seenEmpty = false;

	/**
	 * We keep track of values that we send to solr to lower network traffic.
	 * Because we receive values individually from the shards, a value is probably seen multiple times.
	 */
	private final Set<String> seenValues = ConcurrentHashMap.newKeySet();

	public String getSearchable() {
		return chunkSubmitter.getSearchable();
	}


	@Override
	public void finalizeSearch() {
		chunkSubmitter.finalizeSubmit();
		seenValues.clear();
	}

	@Override
	public long calculateSize() {


		// We query all documents that reference the searchables of the filter
		SolrQuery query = new SolrQuery("%s:%s".formatted(SolrFrontendValue.Fields.searchable_s, getSearchable()));

		// Set rows to 0 because we don't want actual results, we are only interested in the total number
		query.setRows(0);

		try {
			QueryResponse response = chunkSubmitter.getSolrClient().query(query);
			return response.getResults().getNumFound();
		}
		catch (SolrServerException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @implNote This is not used, because this indexer only imports data and is not used by a {@link InternToExternMapper} to retrieve
	 * the data. Data retrieval is done by {@link FilterValueSearch} which combines multiple {@link Searchable}s of a
	 * {@link Filter}.
	 */
	@Override
	public List<FrontendValue> findExact(String searchTerm, int maxValue) {
		throw new UnsupportedOperationException("Must not be used. Use " + FilterValueSearch.class);
	}


	@Override
	public void addItem(FrontendValue feValue, List<String> _keywords) {

		SolrFrontendValue solrFrontendValue = new SolrFrontendValue(getSearchable(), sourcePriority, feValue);

		insertIntoChunk(solrFrontendValue);

	}

	private void insertIntoChunk(SolrFrontendValue solrFrontendValue) {

		if (solrFrontendValue.value_s.isEmpty()) {
			if (seenEmpty) {
				log.trace("Skip indexing of {} for {}, because its 'value' is empty and was already added.", solrFrontendValue, getSearchable());
				return;
			}
			seenEmpty = true;

		}

		if (!seenValues.add(solrFrontendValue.value_s)) {
			log.trace("Skip indexing of {} for {}, because its 'value' has already been submitted to solr.", solrFrontendValue, getSearchable());
			return;
		}

		chunkSubmitter.insertIntoChunk(solrFrontendValue);
	}

	/**
	 * @implNote Call this non concurrently
	 * @param values String values that are converted to SolrDocs
	 */
	public void registerValuesRaw(Collection<String> values) {
		// Convert values and prepare chunks
		values.stream()
			   .filter(Objects::nonNull)
			   .filter(Predicate.not(String::isBlank))
			   .map(value -> new SolrFrontendValue(getSearchable(), sourcePriority, value, null, null))
			   .forEach(this::insertIntoChunk);
	}
}
