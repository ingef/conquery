package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.univocity.parsers.csv.CsvParser;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;

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
					  .map(entry -> new com.bakdata.conquery.apiv1.frontend.FEValue(entry.getKey(), entry.getValue()))
					  .collect(Collectors.toList())
		);
	}


	private static String decideColumnReference(Column column) {

		if (column.getSharedDictionary() != null) {
			return column.getSharedDictionary();
		}

		if (column.getSecondaryId() != null) {
			return column.getSecondaryId().getId().toString();
		}

		return column.getId().toString();
	}

	@JsonIgnore
	public List<String> getSearchReferences() {
		final List<String> references = new ArrayList<>(3);

		if (getTemplate() != null) {
			references.add(getTemplate().getFilePath());
		}

		references.add(getId().toString());
		references.add(decideColumnReference(getColumn()));

		return references;
	}

	@RequiredArgsConstructor
	@Data
	public abstract static class SourceSearchTask {
		private final String sourceId;
		private final String targetId;

		public abstract Stream<FEValue> values();

		public static class FromLabels extends SourceSearchTask {
			private final Map<String, String> labels;

			public FromLabels(AbstractSelectFilter<?> filter, Map<String, String> labels) {
				super(filter.getId().toString(), filter.getId().toString());
				this.labels = labels;
			}

			@Override
			public Stream<FEValue> values() {
				return labels.entrySet().stream()
							 .map(entry -> new FEValue(entry.getKey(), entry.getValue()))
							 .onClose(() -> log.debug("DONE processing {} labels for {}", labels.size(), getSourceId()));
			}
		}

		public static class FromTemplate extends SourceSearchTask {

			private final CSVConfig parserConfig;
			private final FilterTemplate template;

			public FromTemplate(CSVConfig parserConfig, FilterTemplate template) {
				super(template.getFilePath(), template.getFilePath());

				this.parserConfig = parserConfig;
				this.template = template;
			}

			@Override
			public Stream<FEValue> values() {
				final CsvParser parser = parserConfig.createParser();
				// It is likely that multiple Filters reference the same file+config. However we want to ensure it is read only once to avoid wasting computation.
				// We use Streams below to ensure a completely transparent lazy execution of parsing reference files
				return Stream.of(new File(template.getFilePath()))
							 .map(parser::iterateRecords)
							 // Univocity parser does not support streams, so we create one manually using their spliterator.
							 .flatMap(iter -> StreamSupport.stream(iter.spliterator(), false))
							 .map(row -> {
								 final StringSubstitutor substitutor = new StringSubstitutor(row::getString, "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

								 final String rowId = row.getString(template.getColumnValue());

								 final String label = substitutor.replace(template.getValue());
								 final String optionValue = substitutor.replace(template.getOptionValue());

								 // TODO log the line and give feedback to suppliers of reference
								 if (rowId == null || label == null) {
									 return null;
								 }

								 return new FEValue(rowId, label, optionValue);
							 })
							 .filter(Objects::nonNull)
							 .distinct();
			}
		}

		@Getter
		public static class FromColumn extends SourceSearchTask {
			private final List<Import> imports;
			private final Column column;

			public FromColumn(List<Import> imports, Column column, String targetId) {
				super(column.getId().toString(), targetId);
				this.imports = imports;
				this.column = column;
			}

			@Override
			public Stream<FEValue> values() {
				return imports.stream()
							  .flatMap(imp -> StreamSupport.stream(((StringStore) getColumn().getTypeFor(imp)).spliterator(), false))
							  .map(value -> new FEValue(value, value))
							  .onClose(() -> log.debug("DONE processing values for {}", getColumn().getId()));
			}
		}


	}

	public List<SourceSearchTask> collectSourceSearchTasks(CSVConfig parserConfig, NamespacedStorage storage) {
		List<SourceSearchTask> out = new ArrayList<>(3);

		// Collect data from csv template
		if (getTemplate() != null) {
			out.add(new SourceSearchTask.FromTemplate(parserConfig, getTemplate()));
		}


		// Collect data from labels
		if (!getLabels().isEmpty()) {
			out.add(new SourceSearchTask.FromLabels(this, getLabels()));
		}

		// Collect data from raw underlying data, try to unify among columns if at all possible (either via SharedDict or SecondaryId)
		{
			String id = decideColumnReference(getColumn());

			final List<Import> imports = getConnector().getTable().findImports(storage).collect(Collectors.toList());

			out.add(new SourceSearchTask.FromColumn(imports, getColumn(), id));
		}

		return out;
	}
}
