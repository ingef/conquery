package com.bakdata.conquery.apiv1;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.util.search.TrieSearch;
import com.univocity.parsers.csv.CsvParser;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;


@Slf4j
@NoArgsConstructor
public class FilterSearch {

	private final Map<String, TrieSearch<FEValue>> searchCache = new HashMap<>();

	public TrieSearch<FEValue> getSearchFor(String reference) {
		return searchCache.getOrDefault(reference, new TrieSearch<>());
	}

	public static interface SourceSearchTask {

		Stream<FEValue> values();

		@Data
		static class FromLabels implements SourceSearchTask {
			private final FilterId filterId;
			private final Map<String, String> labels;


			@Override
			public Stream<FEValue> values() {
				return labels.entrySet().stream()
							 .map(entry -> new FEValue(entry.getKey(), entry.getValue()))
							 .onClose(() -> log.debug("DONE processing {} labels for {}", labels.size(), getFilterId()));
			}
		}

		static class FromTemplate implements SourceSearchTask {

			private final CSVConfig parserConfig;
			private final FilterTemplate template;

			public FromTemplate(CSVConfig parserConfig, FilterTemplate template) {
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
		static class FromColumn implements SourceSearchTask {
			private final List<Import> imports;
			private final Column column;

			public FromColumn(List<Import> imports, Column column) {
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


	/**
	 * Scan all SelectFilters and submit {@link SimpleJob}s to create interactive searches for them.
	 */
	public void updateSearch(NamespaceStorage storage, JobManager jobManager, CSVConfig parser) {

		jobManager.addSlowJob(new SimpleJob("Initialize Source Search", () -> {

			log.info("BEGIN loading SourceSearch");


			final List<AbstractSelectFilter<?>> allSelectFilters =
					storage.getAllConcepts().stream()
						   .flatMap(c -> c.getConnectors().stream())
						   .flatMap(co -> co.collectAllFilters().stream())
						   .filter(AbstractSelectFilter.class::isInstance)
						   .map(f -> ((AbstractSelectFilter<?>) f))
						   .collect(Collectors.toList());


			// Generate SourceSearchTasks, then group them by their targetId (i.e. the Ids used in getSearchReferences to search for in a filter from multiple sources)
			final Map<String, List<SourceSearchTask>> suppliers = new HashMap<>();

			collectLabelTasks(allSelectFilters, suppliers);

			collectTemplateTasks(parser, allSelectFilters, suppliers);

			collectColumnTasks(storage, allSelectFilters, suppliers);

			final ExecutorService service = Executors.newCachedThreadPool();

			log.debug("Found {} search suppliers", suppliers.size());


			for (Map.Entry<String, List<SourceSearchTask>> entry : suppliers.entrySet()) {

				service.submit(() -> {
					final String id = entry.getKey();
					final long begin = System.currentTimeMillis();

					log.info("BEGIN collecting entries for `{}`", id);

					try {
						final List<SourceSearchTask> fillers = entry.getValue();

						final TrieSearch<FEValue> search = new TrieSearch<>();

						fillers.stream()
							   .flatMap(SourceSearchTask::values)
							   .distinct()
							   .forEach(item -> search.addItem(item, item.extractKeywords()));

						searchCache.put(id, search);

						log.trace("Stats for `{}`", id);

						search.shrinkToFit();

						final long end = System.currentTimeMillis();

						log.debug("DONE collecting entries for `{}`, within {} ({} Items)", id, Duration.ofMillis(end - begin), search.calculateSize());
					}
					catch (Exception e) {
						log.error("Failed to create search for {}", id, e);
					}
				});
			}

			service.shutdown();

			//TODO await properly
			service.awaitTermination(10, TimeUnit.HOURS);


			log.debug("DONE loading SourceSearch");
		}));


	}

	private void collectColumnTasks(NamespaceStorage storage, List<AbstractSelectFilter<?>> allSelectFilters, Map<String, List<SourceSearchTask>> suppliers) {
		final Set<Column> columns = allSelectFilters.stream().map(SingleColumnFilter::getColumn).collect(Collectors.toSet());

		for (Column column : columns) {
			final List<Import> imports = column.getTable().findImports(storage).collect(Collectors.toList());
			final String reference = AbstractSelectFilter.decideColumnReference(column);

			suppliers.computeIfAbsent(reference, (ignored) -> new ArrayList<>())
					 .add(new SourceSearchTask.FromColumn(imports, column));
		}
	}

	private void collectTemplateTasks(CSVConfig parser, List<AbstractSelectFilter<?>> allSelectFilters, Map<String, List<SourceSearchTask>> suppliers) {
		for (AbstractSelectFilter<?> filter : allSelectFilters) {
			if (filter.getTemplate() == null) {
				continue;
			}
			suppliers.putIfAbsent(filter.getTemplate().getFilePath(), List.of(new SourceSearchTask.FromTemplate(parser, filter.getTemplate())));
		}
	}

	private void collectLabelTasks(List<AbstractSelectFilter<?>> allSelectFilters, Map<String, List<SourceSearchTask>> suppliers) {
		for (AbstractSelectFilter<?> filter : allSelectFilters) {
			if (filter.getLabels().isEmpty()) {
				continue;
			}

			suppliers.put(filter.getId().toString(), List.of(new SourceSearchTask.FromLabels(filter.getId(), filter.getLabels())));

		}
	}
}
