package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.search.SolrConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import lombok.RequiredArgsConstructor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

@RequiredArgsConstructor
public class SolrProcessor implements SearchProcessor {

	private final SolrConfig solrConfig;

	@Override
	public void clearSearch() {

	}

	@Override
	public Job createUpdateFilterSearchJob(NamespaceStorage storage, Consumer<Set<Column>> columnsConsumer) {
		return null;
	}

	@Override
	public void registerValues(Searchable<FrontendValue> searchable, Collection<String> values) {
		SolrClient solrClient = solrConfig.getSolrClient();

		Search<FrontendValue> search = solrConfig.createSearch(searchable);


		if (searchable instanceof Identifiable<?> identifiable) {
			String searchableName = identifiable.getId().toString();
			try {

				// Use searchable's id to for collection.
				solrClient.addBeans(values.stream().map(value -> new SolrFrontendValue(searchableName, value, value, null)).iterator());
				return;
			}
			catch (SolrServerException | IOException e) {
				throw new IllegalStateException("Unable to register values for searchable '%s'".formatted(searchable), e);
			}
		}

		throw new IllegalStateException("Unable to register values for searchable '%s'. Expected an identifiable".formatted(searchable));
	}

	@Override
	public long getTotal(SelectFilter<?> filter) {
		return 0L;

	}

	@Override
	public List<Search<FrontendValue>> getSearchesFor(SelectFilter<?> searchable) {
		return List.of();
	}

	@Override
	public void finalizeSearch(Searchable<FrontendValue> searchable) {

	}

	@Override
	public List<FrontendValue> topItems(SelectFilter<?> searchable, String text) {
		return List.of();
	}

	@Override
	public void initManagerResidingSearches(Set<Searchable<FrontendValue>> managerSearchables, AtomicBoolean cancelledState) throws InterruptedException {

	}
}
