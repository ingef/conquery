package com.bakdata.conquery.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SingleSelectFilter;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.FilterSearch;
import com.google.common.collect.ImmutableBiMap;
import org.junit.jupiter.api.Test;

public class FilterSearchTest {

	@Test
	public void totals() {
		final IndexConfig indexConfig = new IndexConfig();
		FilterSearch search = new FilterSearch(indexConfig);

		// Column Searchable
		SelectFilter<String> filter = new SingleSelectFilter();
		ConceptTreeConnector connector = new ConceptTreeConnector();
		TreeConcept concept = new TreeConcept();
		Column column = new Column();
		Table table = new Table();
		Dataset dataset = new Dataset("test_dataset");

		table.setName("test_table");
		table.setDataset(dataset.getId());
		concept.setDataset(dataset.getId());
		concept.setName("test_concept");
		concept.setConnectors(List.of(connector));
		connector.setName("test_connector");
		connector.setFilters(List.of(filter));
		connector.setConcept(concept);
		column.setTable(table);
		column.setName("test_column");
		filter.setColumn(column.getId());
		filter.setConnector(connector);


		// Map Searchable
		filter.setLabels(ImmutableBiMap.of(
				"mm", "MM",
				"nn", "NN"
		));

		// Register
		filter.getSearchReferences().forEach(searchable -> {
			search.addSearches(Map.of(searchable, searchable.createTrieSearch(indexConfig)));
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
	public void totalsEmptyFiler() {
		final IndexConfig indexConfig = new IndexConfig();
		FilterSearch search = new FilterSearch(indexConfig);

		// Column Searchable
		SelectFilter<String> filter = new SingleSelectFilter();
		ConceptTreeConnector connector = new ConceptTreeConnector();
		TreeConcept concept = new TreeConcept();
		Column column = new Column();
		Table table = new Table();
		Dataset dataset = new Dataset("test_dataset");

		table.setName("test_table");
		table.setDataset(dataset.getId());
		concept.setDataset(dataset.getId());
		concept.setName("test_concept");
		concept.setConnectors(List.of(connector));
		connector.setName("test_connector");
		connector.setFilters(List.of(filter));
		connector.setConcept(concept);
		column.setTable(table);
		column.setName("test_column");
		column.setSearchDisabled(true);
		filter.setColumn(column.getId());
		filter.setConnector(connector);

		// Register
		filter.getSearchReferences().forEach(searchable -> {
			search.addSearches(Map.of(searchable, searchable.createTrieSearch(indexConfig)));
		});
		search.shrinkSearch(column);

		assertThat(search.getTotal(filter)).isEqualTo(0);
	}
}
