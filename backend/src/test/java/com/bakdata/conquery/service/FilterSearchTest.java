package com.bakdata.conquery.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SingleSelectFilter;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.index.IndexCreationException;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.util.TestNamespacedStorageProvider;
import com.bakdata.conquery.util.extensions.NamespaceStorageExtension;
import com.google.common.collect.ImmutableBiMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class FilterSearchTest {

	@RegisterExtension
	private static final NamespaceStorageExtension NAMESPACE_STORAGE_EXTENSION = new NamespaceStorageExtension();
	private static final NamespacedStorage NAMESPACED_STORAGE = NAMESPACE_STORAGE_EXTENSION.getStorage();
	public static final NamespacedStorageProvider STORAGE_PROVIDER = new TestNamespacedStorageProvider(NAMESPACED_STORAGE);

	@Test
	public void totals() throws Exception {
		final IndexConfig indexConfig = new IndexConfig();
		FilterSearch search = new FilterSearch(indexConfig);

		// Column Searchable
		SelectFilter<String> filter = new SingleSelectFilter();
		ConceptTreeConnector connector = new ConceptTreeConnector();
		TreeConcept concept = new TreeConcept();
		Column column = new Column();
		Table table = new Table();
		Dataset dataset = new Dataset("test_dataset");
		dataset.setStorageProvider(STORAGE_PROVIDER);
		NAMESPACED_STORAGE.updateDataset(dataset);

		table.setName("test_table");
		table.setNamespacedStorageProvider(NAMESPACED_STORAGE);
		table.init();
		table.setColumns(new Column[]{column});
		concept.setNamespacedStorageProvider(NAMESPACED_STORAGE);
		concept.setName("test_concept");
		concept.init();
		concept.setConnectors(List.of(connector));
		connector.setName("test_connector");
		connector.setFilters(List.of(filter));
		connector.setConcept(concept);
		column.setTable(table);
		column.setName("test_column");
		table.init();
		NAMESPACED_STORAGE.addTable(table);
		filter.setColumn(column.getId());
		filter.setConnector(connector);


		// Map Searchable
		filter.setLabels(ImmutableBiMap.of(
				"mm", "MM",
				"nn", "NN"
		));

		// Register
		filter.getSearchReferences().forEach(searchable -> {
			try {
				search.addSearches(Map.of(searchable, searchable.createTrieSearch(indexConfig)));
			}
			catch (IndexCreationException e) {
				throw new RuntimeException(e);
			}
		});

		search.registerValues(column, List.of(
				"a",
				"bb",
				"cc",
				"mm"
		));
		search.shrinkSearch(column);

		assertThat(search.getTotal(filter)).isEqualTo(5);
	}

	@Test
	public void totalsEmptyFiler() throws Exception {
		final IndexConfig indexConfig = new IndexConfig();
		FilterSearch search = new FilterSearch(indexConfig);

		// Column Searchable
		SelectFilter<String> filter = new SingleSelectFilter();
		ConceptTreeConnector connector = new ConceptTreeConnector();
		TreeConcept concept = new TreeConcept();
		Column column = new Column();
		Table table = new Table();
		Dataset dataset = new Dataset("test_dataset");
		dataset.setStorageProvider(STORAGE_PROVIDER);
		NAMESPACED_STORAGE.updateDataset(dataset);

		table.setName("test_table");
		table.setNamespacedStorageProvider(NAMESPACED_STORAGE);
		table.init();
		table.setColumns(new Column[]{column});
		concept.setNamespacedStorageProvider(NAMESPACED_STORAGE);
		concept.setName("test_concept");
		concept.init();
		concept.setConnectors(List.of(connector));
		connector.setName("test_connector");
		connector.setFilters(List.of(filter));
		connector.setConcept(concept);
		column.setTable(table);
		column.setName("test_column");
		column.setSearchDisabled(true);
		table.init();
		NAMESPACED_STORAGE.addTable(table);

		filter.setColumn(column.getId());
		filter.setConnector(connector);

		// Register
		filter.getSearchReferences().forEach(searchable -> {
			try {
				search.addSearches(Map.of(searchable, searchable.createTrieSearch(indexConfig)));
			}
			catch (IndexCreationException e) {
				throw new RuntimeException(e);
			}
		});
		search.shrinkSearch(column);

		assertThat(search.getTotal(filter)).isEqualTo(0);
	}
}
