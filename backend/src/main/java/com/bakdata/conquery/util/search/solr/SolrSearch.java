package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;

@Slf4j
@RequiredArgsConstructor
public class SolrSearch extends Search<FrontendValue> {

	// TODO use extra client for indexing
	private final SolrClient solrClient;

	@Getter
	private final Searchable<?> searchable;


	@Override
	public void finalizeSearch() {

	}

	@Override
	public long calculateSize() {
		return 0;
	}

	@Override
	public List<FrontendValue> findExact(String searchTerm, int maxValue) {
		return find(List.of(searchTerm), maxValue, false);
	}

	public List<FrontendValue> find(Collection<String> searchTerms, int maxValue, boolean fuzzy) {
		// Escape user input
		String termsEscaped = searchTerms.stream()
										 .filter(Objects::nonNull)
										 .map(ClientUtils::escapeQueryChars)
										 .map(fuzzy ? "%s~"::formatted : Function.identity())
										 .collect(Collectors.joining(" ", "(", ")"));

		StringBuilder queryStringBuilder = new StringBuilder("%s:%s ".formatted(SolrFrontendValue.Fields.searchable_s, searchable.getId()));
		String collect = Stream.of(
									  SolrFrontendValue.Fields.value_s_lower,
									  SolrFrontendValue.Fields.label_ws
							  )
							   .map(field -> "%s:%s".formatted(field, termsEscaped))
							   .collect(Collectors.joining(" OR ", " AND (", ")"));

		queryStringBuilder.append(collect);
		String queryString = queryStringBuilder.toString();

		log.info("Query [{}] created: {}", queryString.hashCode(), queryString);
		SolrQuery query = new SolrQuery(queryString);
		query.addField(SolrFrontendValue.Fields.value_s_lower);
		query.addField(SolrFrontendValue.Fields.label_ws);
		query.addField(SolrFrontendValue.Fields.optionValue_s);
		query.setRows(maxValue);

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

	@Override
	public Iterator<FrontendValue> iterator() {
		return null;
	}

	@Override
	public void addItem(FrontendValue feValue, List<String> _keywords) {
		SolrFrontendValue solrFrontendValue = new SolrFrontendValue(searchable, feValue);

		try {
			solrClient.addBean(solrFrontendValue);
		}
		catch (IOException | SolrServerException e) {
			throw new RuntimeException(e);
		}

	}

	public void registerValues(Collection<String> values) {
		try {

			// Use searchable's id to for collection.
			solrClient.addBeans(values.stream().map(value -> new SolrFrontendValue(searchable, value, value, null)).iterator());
			solrClient.commit();
		}
		catch (SolrServerException | IOException e) {
			throw new IllegalStateException("Unable to register values for searchable '%s'".formatted(searchable), e);
		}
	}
}
