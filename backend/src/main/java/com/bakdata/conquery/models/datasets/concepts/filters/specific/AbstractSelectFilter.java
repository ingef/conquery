package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.apiv1.FilterSearchItem;
import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.search.QuickSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Sets;
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
	protected Set<String> values = new HashSet<>();
	@JsonIgnore
	protected transient QuickSearch<FilterSearchItem> sourceSearch = new QuickSearch.QuickSearchBuilder()
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

		if (values == null || values.isEmpty()) {
			return;
		}

		if (maximumSize != -1 && values.size() > maximumSize) {
			log.trace("Too many possible values ({} of {} in Filter[{}]). Upgrading to BigMultiSelect", values.size(), maximumSize, getId());
			f.setType(FEFilterType.BIG_MULTI_SELECT);
		}

		if (filterType != FEFilterType.BIG_MULTI_SELECT) {
			f.setOptions(
					values
							.stream()
							.map(v -> new FEValue(getLabelFor(v), v))
							.collect(Collectors.toList())
			);
		}
	}

	@Override
	public void addImport(Import imp) {
		if (values == null) {
			values = new HashSet<>();
		}

		final ColumnStore store = getColumn().getTypeFor(imp);

		values.addAll(Sets.newHashSet(((StringStore) store).iterator()));
	}

	public String getLabelFor(String value) {
		return labels.getOrDefault(value, value);
	}

	public String getValueFor(String label) {
		String value = labels.inverse().get(label);
		if (value == null) {
			if (values.contains(label)) {
				return label;
			}
		}
		return null;
	}


	public void initializeSourceSearch(CSVConfig parserConfig) {
		sourceSearch.clear(); //TODO might not be necessary

		Set<String> bag = new HashSet<>();

		if(getTemplate() != null) {
			collectTemplateSearchItems(parserConfig, bag);
		}

		collectRawSearchItems(getValues(), bag);
	}

	private void collectTemplateSearchItems(CSVConfig parserConfig, Set<String> items) {

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

				if (items.contains(rowId)) {
					log.error("Duplicate reference for value `{}`", rowId);
				}

				FilterSearchItem item = new FilterSearchItem();
				item.setLabel(template.getValue());

				item.setOptionValue(template.getOptionValue());

				item.setValue(rowId);

				for (String column : templateColumns) {
					final String value = row.getString(column);

					item.getTemplateValues().put(column, value);

					// A bit odd, but we eagerly register the mutable record for all columns,
					// this saves us iterating twice over the record's columns
					getSourceSearch().addItem(item, value);
				}

				items.add(rowId);
			}

			final long duration = System.currentTimeMillis() - time;

			log.info("DONE Processing reference for {} in {} ms ({} Items in {} Lines)",
					 getTemplate(), duration, getSourceSearch().getStats().getItems(), records.getContext().currentLine()
			);

		}
		catch (Exception e) {
			// TODO what do?
		}
		finally {
			parser.stopParsing();
		}
	}

	private void collectRawSearchItems(Set<String> values, Set<String> items) {

		for (String value : values) {
			// This means we've already registered the value by a template.
			if(items.contains(value)){
				continue;
			}

			final FilterSearchItem item = new FilterSearchItem();
			item.setLabel(value);
			item.setValue(value);
			item.setOptionValue(value);

			getSourceSearch().addItem(item, value);
		}
	}
}
