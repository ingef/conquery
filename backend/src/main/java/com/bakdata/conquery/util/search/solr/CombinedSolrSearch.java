package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.resources.api.ConceptsProcessor.AutoCompleteResult;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class to abstract/capsule the searchables of the filter away
 */
@AllArgsConstructor
@Slf4j
public class CombinedSolrSearch {

	private final SelectFilter<?> filter;
	private final SolrProcessor processor;
	private final SolrClient solrClient;
	private final String queryTemplate;

	public long getTotal() {

		String searchables = buildFilterQuery();

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


	/**
	 * Creates a filter query (which is cached by solr) for the subset of documents originating from the searchables related to this query.
	 *
	 * @return Query string that is a group of the searchable ids for the {@link CombinedSolrSearch#filter}.
	 */
	private @NotNull String buildFilterQuery() {
		List<Search<FrontendValue>> searches = processor.getSearchesFor(filter);
		String searchables = searches.stream()
									 .map(SolrSearch.class::cast)
									 .map(SolrSearch::getSearchable)
									 .map(Searchable::getId)
									 .map(Id::toString)
									 .map(ClientUtils::escapeQueryChars)
									 .collect(Collectors.joining(" ", "%s:(".formatted(SolrFrontendValue.Fields.searchable_s), ")"));
		return searchables;
	}

	/**
	 * <a href="https://lucene.apache.org/core/10_1_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Wildcard_Searches">Query syntax reference</a>
	 */
	public AutoCompleteResult topItems(String text, Integer start, @Nullable Integer limit) {

		String term = text;

		if (StringUtils.isBlank(term)) {
			// Fallback to wild card if search term is blank search for everything
			term = "_text_:*";
		}
		else {
			term = Arrays.stream(term.split("\\s"))
						 // Skip blanks
						 .filter(Predicate.not(String::isBlank))
						 // Escape
						 .map(ClientUtils::escapeQueryChars)
						 // Wildcard regex each term (maybe combine with fuzzy search)
						 .map(queryTemplate::formatted)
						 .collect(Collectors.joining(" AND "));
		}

		return sendQuery(term, start, limit);
	}

	private @NotNull AutoCompleteResult sendQuery(String queryString, Integer start, @org.jetbrains.annotations.Nullable Integer limit) {
		String filterQuery = buildFilterQuery();
		SolrQuery query = buildSolrQuery(filterQuery, queryString, start, limit);
		log.info("Query [{}] created: {}", queryString.hashCode(), URLDecoder.decode(String.valueOf(query), StandardCharsets.UTF_8));

		try {
			QueryResponse response = solrClient.query(query);
			List<SolrFrontendValue> beans = response.getBeans(SolrFrontendValue.class);

			long numFound = response.getResults().getNumFound();
			log.info("Query [{}] Found: {} | Collected: {} | QTime: {} | ElapsedTime: {}", queryString.hashCode(), numFound, beans.size(), response.getQTime(), response.getElapsedTime());

			List<FrontendValue> values = beans.stream().map(SolrFrontendValue::toFrontendValue).toList();
			return new AutoCompleteResult(values, numFound);

		}
		catch (SolrServerException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static @NotNull SolrQuery buildSolrQuery(String filterQuery, String queryString, Integer start, @org.jetbrains.annotations.Nullable Integer limit) {
		SolrQuery query = new SolrQuery(queryString);
		query.addFilterQuery(filterQuery);
		query.addField(SolrFrontendValue.Fields.value_s);
		query.addField(SolrFrontendValue.Fields.label_t);
		query.addField(SolrFrontendValue.Fields.optionValue_s);
		query.setStart(start);
		query.setRows(limit);

		// Collapse the results with equal "value" field. Only the one with the highest score remains.
		// This only works if solr is not sharded (or collapsing documents are on the same shard)
		query.addFilterQuery("{!collapse field=%s min=%s}".formatted(SolrFrontendValue.Fields.value_s, SolrFrontendValue.Fields.sourcePriority_i));

		return query;
	}

	public AutoCompleteResult topItemsExact(String text, Integer start, @Nullable Integer limit) {
		String term = text;

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

		return sendQuery(collect, start, limit);
	}
}
