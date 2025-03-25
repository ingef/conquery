package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
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

	public long getTotal() {

		String searchables = combineSearchables(false);

		// We query all documents that reference the searchables of the filter
		SolrQuery query = new SolrQuery("%s:%s".formatted(SolrFrontendValue.Fields.searchable_s, searchables));

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
	 *
	 * @param boost Boost filter search references by order (first gets highest boost)
	 * @return Query substring that is a group of the searchable ids with optional boosting
	 */
	private @NotNull String combineSearchables(boolean boost) {
		List<Search<FrontendValue>> searches = processor.getSearchesFor(filter);
		final AtomicInteger boostIndex = new AtomicInteger(1);
		String searchables = searches.stream()
									 .map(SolrSearch.class::cast)
									 .map(SolrSearch::getSearchable)
									 .map(Searchable::getId)
									 .map(Id::toString)
									 .map(ClientUtils::escapeQueryChars)
									 /* Apply boost (^) if flagged https://lucene.apache.org/core/2_9_4/queryparsersyntax.html
									 	The boost is between (0,1]. The first item gets 1 boost (default) like the search term
									  */
									 .map(boost ? term -> "%s^%.2f".formatted(term, 1f / boostIndex.getAndIncrement()) : Function.identity())
									 .collect(Collectors.joining(" ", "(", ")"));
		return searchables;
	}

	/**
	 * <a href="https://lucene.apache.org/core/10_1_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Wildcard_Searches">Query syntax reference</a>
	 */
	public AutoCompleteResult topItems(String text, Integer start, @Nullable Integer limit) {
		String searchables = combineSearchables( true);

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
						 .map("/.*%s.*/"::formatted)
						 .collect(Collectors.joining(" "));
		}


		String queryString = "%s:%s ".formatted(SolrFrontendValue.Fields.searchable_s, searchables) + " AND "
							 + term;

		return sendQuery(queryString, start, limit);
	}

	private @NotNull AutoCompleteResult sendQuery(String queryString, Integer start, @org.jetbrains.annotations.Nullable Integer limit) {
		log.info("Query [{}] created: {}", queryString.hashCode(), queryString);
		SolrQuery query = new SolrQuery(queryString);
		query.addField(SolrFrontendValue.Fields.value_s_lower);
		query.addField(SolrFrontendValue.Fields.label_ws);
		query.addField(SolrFrontendValue.Fields.optionValue_s);
		query.setStart(start);
		query.setRows(limit);

		try {
			QueryResponse response = solrClient.query(query);
			List<SolrFrontendValue> beans = response.getBeans(SolrFrontendValue.class);

			long numFound = response.getResults().getNumFound();
			log.info("Query [{}] Found: {} | Collected: {}", queryString.hashCode(), numFound, beans.size());

			List<FrontendValue> values = beans.stream().map(SolrFrontendValue::toFrontendValue).toList();
			return new AutoCompleteResult(values, numFound);

		}
		catch (SolrServerException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public AutoCompleteResult topItemsExact(String text, Integer start, @Nullable Integer limit) {
		String searchables = combineSearchables( true);

		String term = text;

		if (StringUtils.isBlank(term)) {
			return new AutoCompleteResult(List.of(), 0);
		}
		// Escape user input
		term = ClientUtils.escapeQueryChars(term);

		final String finalTerm = term;

		StringBuilder queryStringBuilder = new StringBuilder("%s:%s ".formatted(SolrFrontendValue.Fields.searchable_s, searchables));
		String collect = Stream.of(
									   SolrFrontendValue.Fields.value_s_lower,
									   SolrFrontendValue.Fields.label_ws
							   )
							   .map(field -> "%s:\"%s\"".formatted(field, finalTerm))
							   .collect(Collectors.joining(" OR ", " AND (", ")"));

		queryStringBuilder.append(collect);
		String queryString = queryStringBuilder.toString();

		return sendQuery(queryString, start, limit);
	}
}
