package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.util.search.TrieSearch;
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
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

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
	private final int maximumSize;
	@JsonIgnore
	private final FEFilterType filterType;

	private FilterTemplate template;


	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setTemplate(getTemplate());
		f.setType(filterType);


		f.setOptions(
				labels.entrySet().stream()
					  .map(entry -> new com.bakdata.conquery.apiv1.frontend.FEValue(entry.getValue(), entry.getKey()))
					  .collect(Collectors.toList())
		);
	}


	/**
	 * Adds an item to the FilterSearch associating it with containing words.
	 * <p>
	 * The item is not added, if we've already collected an item with the same {@link FEValue#getValue()}.
	 */
	private List<String> extractKeywords(FEValue item) {
		return List.of(item.getValue(), item.getOptionValue(), item.getLabel());
	}

	@JsonIgnore
	public List<String> getSearchReferences() {
		final List<String> references = new ArrayList<>(3);

		if (getTemplate() != null) {
			references.add(getTemplate().getFilePath());
		}

		references.add(getId().toString());
		references.add(getColumn().getId().toString());

		return references;
	}

	public void initializeSourceSearch(CSVConfig parserConfig, NamespacedStorage storage, FilterSearch cache) {

		if (getTemplate() != null && !cache.hasSearchFor(getTemplate().getFilePath())) {
			collectTemplateSearchItems(parserConfig, cache.getSearchFor(getTemplate().getFilePath()));
		}

		final FilterId filterRefId = getId();

		if (!cache.hasSearchFor(filterRefId.toString())) {
			collectLabeledSearchItems(getLabels(), cache.getSearchFor(filterRefId.toString()));
		}

		IId<?> columnRefId = getColumn().getSecondaryId() != null ? getColumn().getSecondaryId().getId() : getColumn().getId();

		if (!cache.hasSearchFor(columnRefId.toString())) {
			// If we have a secondaryId, we can share the quicksearch for that

			collectRawSearchItems(storage, cache.getSearchFor(columnRefId.toString()));
		}
	}

	private void collectLabeledSearchItems(Map<String, String> labels, TrieSearch<FEValue> search) {
		if (labels.isEmpty()) {
			return;
		}

		log.info("BEGIN processing {} labels for {}", labels.size(), getId());

		for (Map.Entry<String, String> entry : labels.entrySet()) {
			String value = entry.getKey();
			String label = entry.getValue();

			final FEValue item = new FEValue(label, value, null);

			search.addItem(item, extractKeywords(item));
		}

		log.debug("DONE processing {} labels for {}", labels.size(), getId());
	}

	@Setter
	@Getter
	private static class MutableRecordBackedLookup implements StringLookup {
		private Record record;

		@Override
		public String lookup(String key) {
			return record.getString(key);
		}
	}

	/**
	 * Collect search results based on provided CSV with prepopulated values.
	 */
	private void collectTemplateSearchItems(CSVConfig parserConfig, TrieSearch<FEValue> search) {

		if (getTemplate() == null) {
			return;
		}

		log.info("BEGIN Processing template {}", getTemplate());
		final long time = System.currentTimeMillis();

		final CsvParser parser = parserConfig.createParser();

		final MutableRecordBackedLookup lookup = new MutableRecordBackedLookup();
		final StringSubstitutor substitutor = new StringSubstitutor(lookup, "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

		try {
			File file = new File(template.getFilePath());

			final IterableResult<Record, ParsingContext> records = parser.iterateRecords(file);


			for (Record row : records) {

				lookup.setRecord(row);

				final String rowId = row.getString(template.getColumnValue());

				//TODO render label and optionvalue!
				// TODO actually render the template; I'm just trying to estimate how much I save not using hashmaps
				final String label = substitutor.replace(template.getValue());
				final String optionValue = substitutor.replace(template.getOptionValue());

				FEValue item = new FEValue(label, rowId, optionValue);

				search.addItem(item, extractKeywords(item));
			}

			final long duration = System.currentTimeMillis() - time;

			log.info("DONE Processing reference for {} in {} ms ({} Items in {} Lines)",
					 getTemplate(), duration, search.calculateSize(), records.getContext().currentLine()
			);

		}
		finally {
			parser.stopParsing();
		}
	}


	/**
	 * Collect search Items from raw data.
	 */
	private void collectRawSearchItems(NamespacedStorage storage, TrieSearch<FEValue> search) {
		log.info("BEGIN processing values for {}", getColumn().getId());


		getConnector().getTable().findImports(storage)
					  .forEach(
							  imp -> {
								  for (String value : ((StringStore) getColumn().getTypeFor(imp))) {
									  final FEValue item = new FEValue(value, value, value);
									  search.addItem(item, extractKeywords(item));
								  }
							  }
					  );

		log.debug("DONE processing values for {} with {} Items", getColumn().getId(), search.calculateSize());
	}
}
