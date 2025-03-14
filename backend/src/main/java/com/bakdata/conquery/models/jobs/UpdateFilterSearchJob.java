package com.bakdata.conquery.models.jobs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.util.search.SearchProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Job that initializes the filter search for the frontend.
 * It collects all sources of values for all filters, e.g.:
 * <ul>
 *     <li>explicit mappings in a {@link SelectFilter}</li>
 *     <li>external reference mappings</li>
 *     <li>columns of imported data which are referenced by a filter</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class UpdateFilterSearchJob extends Job {

	private final NamespaceStorage storage;

	private final SearchProcessor searchProcessor;

	private final Consumer<Set<Column>> registerColumnValuesInSearch;

	@Override
	public void execute() throws Exception {

		log.info("Clearing Search");
		searchProcessor.clearSearch();


		log.info("BEGIN loading SourceSearch");

		// collect all SelectFilters to create searches for them
		final List<SelectFilter<?>> allSelectFilters =
				getAllSelectFilters(storage);


		// Unfortunately the is no ClassToInstanceMultimap yet
		final Map<Class<?>, Set<Searchable<FrontendValue>>> collectedSearchables =
				allSelectFilters.stream()
								.map(SelectFilter::getSearchReferences)
								.flatMap(Collection::stream)
								// Group Searchables into "Columns" and other "Searchables"
								.collect(Collectors.groupingBy(s -> s instanceof Column ? Column.class : Searchable.class, Collectors.toSet()));


		log.debug("Found {} searchable Objects.", collectedSearchables.values().stream().mapToLong(Set::size).sum());

		Set<Searchable<FrontendValue>> managerSearchables = collectedSearchables.getOrDefault(Searchable.class, Collections.emptySet());


		searchProcessor.initManagerResidingSearches(managerSearchables, getCancelledState());


		// The following cast is safe
		final Set<Column> searchableColumns = (Set) collectedSearchables.getOrDefault(Column.class, Collections.emptySet());
		log.debug("Start collecting column values: {}", Arrays.toString(searchableColumns.toArray()));
		registerColumnValuesInSearch.accept(searchableColumns);

		log.info("UpdateFilterSearchJob search finished");

	}



	@NotNull
	public static List<SelectFilter<?>> getAllSelectFilters(NamespaceStorage storage) {
		try(Stream<Concept<?>> allConcepts = storage.getAllConcepts()) {
			return allConcepts
					.flatMap(c -> c.getConnectors().stream())
					.flatMap(co -> co.collectAllFilters().stream())
					.filter(SelectFilter.class::isInstance)
					.map(f -> ((SelectFilter<?>) f))
					.collect(Collectors.toList());
		}
	}

	@Override
	public String getLabel() {
		return "UpdateFilterSearchJob";
	}
}
