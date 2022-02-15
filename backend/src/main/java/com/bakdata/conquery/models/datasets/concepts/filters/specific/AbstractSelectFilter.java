package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.apiv1.FilterSearchItem;
import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.search.QuickSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.univocity.parsers.common.IterableResult;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true, value = {"values"}) //TODO this is a hotfix because we cannot reimport this late
public abstract class AbstractSelectFilter<FE_TYPE> extends SingleColumnFilter<FE_TYPE> {

	/**
	 * user given mapping from the values in the CSVs to shown labels
	 */
	protected BiMap<String, String> labels = ImmutableBiMap.of();


	@JsonIgnore
	protected final transient QuickSearch<FilterSearchItem> sourceSearch = new QuickSearch.QuickSearchBuilder()
			.withUnmatchedPolicy(QuickSearch.UnmatchedPolicy.IGNORE)
			.withMergePolicy(QuickSearch.MergePolicy.UNION)
			.withKeywordMatchScorer(FilterSearch.FilterSearchType.CONTAINS::score)
			.build();

	@JsonIgnore
	private final int maximumSize;
	@JsonIgnore
	private final FEFilterType filterType;

	private FilterTemplate template;


	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	public FilterSearch.FilterSearchType searchType = FilterSearch.FilterSearchType.EXACT;

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setTemplate(getTemplate());
		f.setType(filterType);


		f.setOptions(
				labels.entrySet().stream()
					  .map(entry -> new FEValue(entry.getValue(), entry.getKey()))
					  .collect(Collectors.toList())
		);
	}


	private final Set<String> bag = new HashSet<>();

	/**
	 * Adds an item to the FilterSearch associating it with containing words.
	 * <p>
	 * The item is not added, if we've already collected an item with the same {@link FilterSearchItem#getValue()}.
	 */
	private void addSearchItem(FilterSearchItem item) {
		if (bag.contains(item.getValue())) {
			return;
		}

		getSourceSearch().addItem(item, item.getValue());

		// If templateValues is empty, we can assume that label is not a template.
		if (item.getTemplateValues().isEmpty()) {
			getSourceSearch().addItem(item, item.getLabel());
		}
		else {
			item.getTemplateValues().values()
				.forEach(value -> getSourceSearch().addItem(item, value));
		}

		bag.add(item.getValue());
	}


	public void initializeSourceSearch(CSVConfig parserConfig, NamespacedStorage storage) {
		sourceSearch.clear(); //TODO might not be necessary
		bag.clear();

		collectTemplateSearchItems(parserConfig);
		collectLabeledSearchItems(getLabels());

		collectRawSearchItems(storage);
	}

	private void collectLabeledSearchItems(Map<String, String> labels) {
		labels.forEach((value, label) -> {
			// This means we've already registered the value by a template.

			final FilterSearchItem item = new FilterSearchItem();
			item.setLabel(label);
			item.setValue(value);
			item.setOptionValue(value);

			addSearchItem(item);
		});

	}

	/**
	 * Collect search results based on provided CSV with prepopulated values.
	 */
	private void collectTemplateSearchItems(CSVConfig parserConfig) {

		if (getTemplate() == null) {
			return;
		}

		List<String> templateColumns = new ArrayList<>(template.getColumns());
		templateColumns.add(template.getColumnValue());


		log.info("BEGIN Processing template {}", getTemplate());
		final long time = System.currentTimeMillis();

		final CsvParser parser = parserConfig.createParser();

		try {
			File file = new File(template.getFilePath());

			final IterableResult<Record, ParsingContext> records = parser.iterateRecords(file);


			for (Record row : records) {

				final String rowId = row.getString(template.getColumnValue());

				FilterSearchItem item = new FilterSearchItem();
				item.setLabel(template.getValue());

				item.setOptionValue(template.getOptionValue());

				item.setValue(rowId);

				for (String column : templateColumns) {
					final String value = row.getString(column);

					item.getTemplateValues().put(column, value);
				}

				addSearchItem(item);
			}

			final long duration = System.currentTimeMillis() - time;

			log.info("DONE Processing reference for {} in {} ms ({} Items in {} Lines)",
					 getTemplate(), duration, getSourceSearch().getStats().getItems(), records.getContext().currentLine()
			);

		}
		finally {
			parser.stopParsing();
		}
	}

	/**
	 * Collect search Items from raw data.
	 */
	private void collectRawSearchItems(NamespacedStorage storage) {


		for (Import imp : storage.getAllImports()) {
			if (!imp.getTable().equals(getConnector().getTable())) {
				continue;
			}

			for (String value : ((StringStore) getColumn().getTypeFor(imp))) {
				final FilterSearchItem item = new FilterSearchItem();
				item.setLabel(value);
				item.setValue(value);
				item.setOptionValue(value);

				addSearchItem(item);
			}
		}
	}
}
