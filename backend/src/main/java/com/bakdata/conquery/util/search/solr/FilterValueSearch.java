package com.bakdata.conquery.util.search.solr;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.search.solr.FilterValueConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.resources.api.ConceptsProcessor.AutoCompleteResult;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to abstract/capsule the searchables of the filter away
 */
@AllArgsConstructor
@Slf4j
public class FilterValueSearch {

	private final SelectFilter<?> filter;
	private final SolrProcessor processor;
	private final SolrClient solrClient;
	private final FilterValueConfig filterValueConfig;

	public long getTotal() {

		String searchables = buildFilterQuery(true);

		// We query all documents that reference the searchables of the filter
		SolrQuery query = new SolrQuery(searchables);

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

	public List<FilterValueIndexer> getSearchesFor(SelectFilter<?> searchable, boolean withEmptySource) {
		List<Searchable> searchReferences = searchable.getSearchReferences();

		if (withEmptySource) {
			// Patchup searchables
			searchReferences.add(SolrEmptySeachable.INSTANCE);
		}

		return searchReferences.stream().map(processor::getIndexerFor).toList();
	}


	/**
	 * Creates a filter query (which is cached by solr) for the subset of documents originating from the searchables related to this query.
	 *
	 * @param withEmptySource Also include the special empty value source in the results, which allows in conquery to filter for empty fields.
	 * @return Query string that is a group of the searchable ids for the {@link FilterValueSearch#filter}.
	 */
	private @NotNull String buildFilterQuery(boolean withEmptySource) {
		List<FilterValueIndexer> indexers = getSearchesFor(filter, withEmptySource);
		return indexers.stream()
				.map(FilterValueIndexer::getSearchable)
				// The name of the searchable was already escaped at the creation of SolrSearch
				.collect(Collectors.joining(" ", "%s:(".formatted(SolrFrontendValue.Fields.searchable_s), ")"));
	}

	/**
	 * <a href="https://lucene.apache.org/core/10_1_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Wildcard_Searches">Query syntax reference</a>
	 */
	public AutoCompleteResult topItems(String text, Integer start, @Nullable Integer limit) {

		if (StringUtils.isBlank(text)) {
			// Fallback to wild card if search term is blank search for everything
			text = "_text_:*";

			return sendQuery(text, start, limit, true, true);
		}
		else {
			text = Arrays.stream(text.split("\\s"))
						 // Skip blanks
						 .filter(Predicate.not(String::isBlank))
						 // Escape
						 .map(ClientUtils::escapeQueryChars)
						 //
						 .map((term) -> {
							 Map<String, String> valuesMap = Map.of("term", term);
							 StringSubstitutor sub = new StringSubstitutor(valuesMap);
							 return sub.replace(filterValueConfig.getQueryTemplate());
						 })
						 .collect(Collectors.joining(" AND "));
			return sendQuery(text, start, limit, false, false);
		}
	}

	private @NotNull AutoCompleteResult sendQuery(String queryString, Integer start, @org.jetbrains.annotations.Nullable Integer limit, boolean withEmptySource, boolean sort) {
		String filterQuery = buildFilterQuery(withEmptySource);
		SolrQuery query = buildSolrQuery(filterQuery, queryString, start, limit, sort);
		String decodedQuery = URLDecoder.decode(String.valueOf(query), StandardCharsets.UTF_8);
		int queryHash = decodedQuery.hashCode();
		log.info("Query [{}] created: {}", queryHash, decodedQuery);

		try {
			QueryResponse response = solrClient.query(query);
			List<SolrFrontendValue> beans = response.getBeans(SolrFrontendValue.class);

			long numFound = response.getResults().getNumFound();
			log.info("Query [{}] Found: {} | Collected: {} | QTime: {} | ElapsedTime: {}", queryHash, numFound, beans.size(), response.getQTime(), response.getElapsedTime());

			List<FrontendValue> values = beans.stream().map(SolrFrontendValue::toFrontendValue).toList();
			return new AutoCompleteResult(values, numFound);

		}
		catch (SolrServerException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private @NotNull SolrQuery buildSolrQuery(String filterQuery, String queryString, Integer start, @org.jetbrains.annotations.Nullable Integer limit, boolean sort) {
		SolrQuery query = new SolrQuery(queryString);
		query.addFilterQuery(filterQuery);
		query.addField(SolrFrontendValue.Fields.value_s);
		query.addField(SolrFrontendValue.Fields.label_t);
		query.addField(SolrFrontendValue.Fields.optionValue_s);
		query.setStart(start);
		query.setRows(limit);

		if (sort) {
			query.setSort(SolrQuery.SortClause.asc(SolrFrontendValue.Fields.sourcePriority_i));
			query.addSort(SolrQuery.SortClause.asc(filterValueConfig.getDefaultSearchSortField()));
		}

		// Collapse the results with equal "value" field. Only the one with the highest score remains.
		// This only works if solr is not sharded (or collapsing documents are on the same shard)
		// We set 'nullPolicy=expand' so we do not suppress the empty label entry
		query.addFilterQuery("{!collapse field=%s min=%s nullPolicy=expand}".formatted(SolrFrontendValue.Fields.value_s, SolrFrontendValue.Fields.sourcePriority_i));

		return query;
	}

	public AutoCompleteResult topItemsExact(String term, Integer start, @Nullable Integer limit) {

		if (StringUtils.isBlank(term)) {
			return new AutoCompleteResult(List.of(), 0);
		}
		// Escape user input
		term = ClientUtils.escapeQueryChars(term);

		final String finalTerm = term;

		String collect = Stream.of(
									   SolrFrontendValue.Fields.value_s,
									   SolrFrontendValue.Fields.label_t
							   )
							   .map(field -> "%s:\"%s\"".formatted(field, finalTerm))
							   .collect(Collectors.joining(" OR ", "(", ")"));

		return sendQuery(collect, start, limit, true, false);
	}
}
