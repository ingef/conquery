package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.identifiable.ids.Id;
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

		// TODO put this in an extra class
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
		final AtomicInteger boostIndex = new AtomicInteger(searches.size());
		String searchables = searches.stream()
									 .map(SolrSearch.class::cast)
									 .map(SolrSearch::getSearchable)
									 .map(Searchable::getId)
									 .map(Id::toString)
									 .map(ClientUtils::escapeQueryChars)
									 // Apply boost (^) if flagged https://lucene.apache.org/core/2_9_4/queryparsersyntax.html
									 .map(boost ? term -> "%s^%d".formatted(term, boostIndex.getAndDecrement()) : Function.identity())
									 .collect(Collectors.joining(" ", "(", ")"));
		return searchables;
	}


	public List<FrontendValue> topItems(String text, @Nullable Integer limit, boolean fuzzy) {
		String searchables = combineSearchables( true);

		String term = text;

		if (StringUtils.isBlank(term)) {
			// Fallback to wild card if search term is blank search for everything
			term = "*";
		} else {
			// Escape user input
			term = ClientUtils.escapeQueryChars(term);
		}

		if (fuzzy) {
			term = term + "~";
		}
		final String finalTerm = term;

		StringBuilder queryStringBuilder = new StringBuilder("%s:%s ".formatted(SolrFrontendValue.Fields.searchable_s, searchables));
		String collect = Stream.of(
									   SolrFrontendValue.Fields.value_s_lower,
									   SolrFrontendValue.Fields.label_ws
							   )
							   .map(field -> "%s:%s".formatted(field, finalTerm))
							   .collect(Collectors.joining(" OR ", " AND (", ")"));

		queryStringBuilder.append(collect);
		String queryString = queryStringBuilder.toString();

		log.info("Query [{}] created: {}", queryString.hashCode(), queryString);
		SolrQuery query = new SolrQuery(queryString);
		query.addField(SolrFrontendValue.Fields.value_s_lower);
		query.addField(SolrFrontendValue.Fields.label_ws);
		query.addField(SolrFrontendValue.Fields.optionValue_s);
		query.setRows(limit);

		try {
			QueryResponse response = solrClient.query(query);
			List<SolrFrontendValue> beans = response.getBeans(SolrFrontendValue.class);

			log.info("Query [{}] collected {} documents", queryString.hashCode(), beans.size());

			return beans.stream().map(SolrFrontendValue::toFrontendValue).toList();

		}
		catch (SolrServerException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
