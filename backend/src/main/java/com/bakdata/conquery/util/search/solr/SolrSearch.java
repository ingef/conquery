package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

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
		throw new UnsupportedOperationException("Must not be used. Use " + CombinedSolrSearch.class);
	}

	@Override
	public Iterator<FrontendValue> iterator() {
		throw new UnsupportedOperationException("Must not be used. Use " + CombinedSolrSearch.class);
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
