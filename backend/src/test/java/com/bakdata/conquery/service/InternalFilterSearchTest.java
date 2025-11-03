package com.bakdata.conquery.service;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.config.search.InternalSearchConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SingleSelectFilter;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.util.TestNamespacedStorageProvider;
import com.bakdata.conquery.util.extensions.NamespaceStorageExtension;
import com.bakdata.conquery.util.search.internal.InternalFilterSearch;
import com.google.common.collect.ImmutableBiMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class InternalFilterSearchTest {

	@RegisterExtension
	private static final NamespaceStorageExtension NAMESPACE_STORAGE_EXTENSION = new NamespaceStorageExtension();
	private static final NamespacedStorage NAMESPACED_STORAGE = NAMESPACE_STORAGE_EXTENSION.getStorage();
	public static final NamespacedStorageProvider STORAGE_PROVIDER = new TestNamespacedStorageProvider(NAMESPACED_STORAGE);

	@Test
	public void totals() throws Exception {
		final InternalSearchConfig searchConfig = new InternalSearchConfig();
		InternalFilterSearch search = new InternalFilterSearch(searchConfig);

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
			search.addSearches(Map.of(searchable, searchConfig.createSearch(searchable)));
		});

		search.registerValues(column, List.of(
				"a",
				"bb",
				"cc",
				"mm"
		));
		search.finalizeSearch(column);

		assertThat(search.getTotal(filter)).isEqualTo(5);
	}

	@Test
	public void totalsEmptyFiler() throws Exception {
		final InternalSearchConfig searchConfig = new InternalSearchConfig();
		InternalFilterSearch search = new InternalFilterSearch(searchConfig);

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
			search.addSearches(Map.of(searchable, searchConfig.createSearch(searchable)));
		});
		search.finalizeSearch(column);

		assertThat(search.getTotal(filter)).isEqualTo(0);
	}
}
