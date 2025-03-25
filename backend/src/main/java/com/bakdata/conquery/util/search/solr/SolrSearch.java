package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;

@Slf4j
@RequiredArgsConstructor
public class SolrSearch extends Search<FrontendValue> {

	// TODO use extra client for indexing
	private final SolrClient solrClient;

	@Getter
	private final Searchable<?> searchable;


	@Override
	public void finalizeSearch() {

		try {
			solrClient.commit();
		}
		catch (SolrServerException | IOException e) {
			throw new IllegalStateException("Unable to commit values", e);
		}
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
			List<SolrFrontendValue> solrFrontendValues = values.stream().map(value -> new SolrFrontendValue(searchable, value, value, null)).toList();
			List<String> ids = solrFrontendValues.stream().map(SolrFrontendValue::getId).toList();

			SolrDocumentList existingDocs = solrClient.getById(ids);
			Set<String> existingIds = existingDocs.stream().map(doc -> doc.getFieldValue(SolrFrontendValue.Fields.id)).map(String.class::cast).collect(Collectors.toSet());


			log.info("Received {} values, solr knows {} of them. Registering {} new documents", solrFrontendValues.size(), existingIds.size(), solrFrontendValues.size() - existingIds.size());

			solrClient.addBeans(solrFrontendValues.stream().filter(val -> !existingIds.contains(val)).iterator());
		}
		catch (SolrServerException | IOException e) {
			throw new IllegalStateException("Unable to register values for searchable '%s'".formatted(searchable), e);
		}
	}
}
