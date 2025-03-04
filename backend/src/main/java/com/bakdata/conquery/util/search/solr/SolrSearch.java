package com.bakdata.conquery.util.search.solr;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.util.search.Search;
import lombok.RequiredArgsConstructor;
import org.apache.solr.client.solrj.SolrClient;

@RequiredArgsConstructor
public class SolrSearch extends Search<FrontendValue> {

	private final SolrClient solrClient;


	@Override
	public void finalizeSearch() {

	}

	@Override
	public long calculateSize() {
		return 0;
	}

	@Override
	public List<FrontendValue> findExact(Collection<String> searchTerm, int maxValue) {
		return List.of();
	}

	@Override
	public Iterator<FrontendValue> iterator() {
		return null;
	}
}
