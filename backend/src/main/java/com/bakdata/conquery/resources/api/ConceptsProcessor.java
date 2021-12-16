package com.bakdata.conquery.resources.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.apiv1.FilterSearchItem;
import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.apiv1.frontend.FEList;
import com.bakdata.conquery.apiv1.frontend.FERoot;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.FrontEndConceptBuilder;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.search.QuickSearch;
import com.bakdata.conquery.util.search.SearchScorer;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@Slf4j
@RequiredArgsConstructor
public class ConceptsProcessor {

	private final DatasetRegistry namespaces;

	private final LoadingCache<Concept<?>, FEList> nodeCache =
			CacheBuilder.newBuilder()
						.softValues()
						.expireAfterWrite(10, TimeUnit.MINUTES)
						.build(new CacheLoader<>() {
							@Override
							public FEList load(Concept<?> concept) {
								return FrontEndConceptBuilder.createTreeMap(concept);
							}
						});

	private final LoadingCache<Pair<AbstractSelectFilter<?>, String>, List<FEValue>> searchCache =
			CacheBuilder.newBuilder()
						.softValues()
						.build(new CacheLoader<>() {

							@Override
							public List<FEValue> load(Pair<AbstractSelectFilter<?>, String> filterAndSearch) {
								String searchTerm = filterAndSearch.getValue();
								AbstractSelectFilter<?> filter = filterAndSearch.getKey();

								log.trace("Calculating a new search cache for the term \"{}\" on filter[{}]", searchTerm, filter.getId());

								final List<FEValue> result = autocompleteTextFilter(filter, searchTerm);

								log.debug("Got {} results for {}", result.size(), filterAndSearch);

								return result;
							}

						});

	public FERoot getRoot(NamespaceStorage storage, Subject subject) {

		return FrontEndConceptBuilder.createRoot(storage, subject);
	}

	public FEList getNode(Concept<?> concept) {
		try {
			return nodeCache.get(concept);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("failed to create frontend node for " + concept, e);
		}
	}

	public List<IdLabel<DatasetId>> getDatasets(Subject subject) {
		return namespaces.getAllDatasets()
						 .stream()
						 .filter(d -> subject.isPermitted(d, Ability.READ))
						 .sorted(Comparator.comparing(Dataset::getWeight)
										   .thenComparing(Dataset::getLabel))
						 .map(d -> new IdLabel<>(d.getId(), d.getLabel()))
						 .collect(Collectors.toList());
	}

	/**
	 * Search for all search terms at once, with stricter scoring.
	 * The user will upload a file and expect only well-corresponding resolutions.
	 */
	public ResolvedConceptsResult resolveFilterValues(AbstractSelectFilter<?> filter, List<String> searchTerms) {

		//search in the full text engine
		Set<String> searchResult = createSourceSearchResult(filter.getSourceSearch(), searchTerms, OptionalInt.empty(), filter.getSearchType()::score)
				.stream()
				.map(FEValue::getValue)
				.collect(Collectors.toSet());

		Set<String> openSearchTerms = new HashSet<>(searchTerms);
		openSearchTerms.removeAll(searchResult);

		// Iterate over all unresolved search terms. Gather all that match labels into searchResults. Keep the unresolvable ones.
		for (Iterator<String> it = openSearchTerms.iterator(); it.hasNext(); ) {
			String searchTerm = it.next();
			// Test if any of the values occurs directly in the filter's values or their labels (for when we don't have a provided file).
			if (filter.getValues().contains(searchTerm)) {
				searchResult.add(searchTerm);
				it.remove();
			}
			else {
				String matchingValue = filter.getLabels().inverse().get(searchTerm);
				if (matchingValue != null) {
					searchResult.add(matchingValue);
					it.remove();
				}
			}
		}

		return new ResolvedConceptsResult(
				null,
				new ResolvedFilterResult(
						filter.getConnector().getId(),
						filter.getId(),
						searchResult
								.stream()
								.map(v -> new FEValue(filter.getLabelFor(v), v))
								.collect(Collectors.toList())
				),
				new ArrayList<>(openSearchTerms)
		);
	}

	@Data
	public static class AutoCompleteResult {
		private final List<FEValue> values;
		private final long total;
	}

	public AutoCompleteResult autocompleteTextFilter(AbstractSelectFilter<?> filter, Optional<String> maybeText, OptionalInt pageNumberOpt, OptionalInt itemsPerPageOpt) {
		final int pageNumber = pageNumberOpt.orElse(0);
		final int itemsPerPage = itemsPerPageOpt.orElse(50);

		final String text = maybeText.orElse("");

		Preconditions.checkArgument(pageNumber >= 0, "Page number must be 0 or a positive integer.");
		Preconditions.checkArgument(itemsPerPage > 1, "Must at least have one item per page.");

		log.trace("Searching for for the term \"{}\". (Page = {}, Items = {})", text, pageNumber, itemsPerPage);

		List<FEValue> fullResult = null;
		try {
			fullResult = searchCache.get(Pair.of(filter, text));
		}
		catch (ExecutionException e) {
			log.warn("Failed to search for \"{}\".", text, (Throwable) (log.isTraceEnabled() ? e : null));
			return new AutoCompleteResult(Collections.emptyList(), 0);
		}

		int startIncl = Math.min(itemsPerPage * pageNumber, fullResult.size());
		int endExcl = Math.min(startIncl + itemsPerPage, fullResult.size());

		log.trace("Preparing subresult for search term \"{}\" in the index range [{}-{})", text, startIncl, endExcl);

		return new AutoCompleteResult(fullResult.subList(startIncl, endExcl), fullResult.size());
	}

	/**
	 * Autocompletion for search terms. For values of {@link AbstractSelectFilter<?>}.
	 * Is used by the serach cache to load missing items
	 */
	private static List<FEValue> autocompleteTextFilter(AbstractSelectFilter<?> filter, String text) {
		if (Strings.isNullOrEmpty(text)) {
			// If no text provided, we just list them
			// Filter might not have a source search (since none might be defined).

			//TODO unify these code paths, they are quite the mess, maybe also create source search for key-value also

			final Stream<FEValue> fromSearch =
					filter.getSourceSearch() == null
					? Stream.empty()
					: filter.getSourceSearch().listItems()
							.stream()
							.map(item -> new FEValue(item.getLabel(), item.getValue(), item.getTemplateValues(), item.getOptionValue()));


			final Stream<FEValue> fromLabels = filter.getLabels().entrySet().stream().map(entry -> new FEValue(entry.getValue(), entry.getKey()));

			return Stream.concat(fromLabels, fromSearch)
					.sorted()
						 .collect(Collectors.toList());
		}

		List<FEValue> result = new LinkedList<>();

		QuickSearch<FilterSearchItem> search = filter.getSourceSearch();

		if (search != null) {
			result = createSourceSearchResult(
					filter.getSourceSearch(),
					Collections.singletonList(text),
					OptionalInt.empty(),
					FilterSearch.FilterSearchType.CONTAINS::score
			);
		}

		String value = filter.getValueFor(text);
		if (value != null) {
			result.add(new FEValue(text, value));
		}

		return result;
	}

	/**
	 * Do a search with the supplied values.
	 */
	private static List<FEValue> createSourceSearchResult(QuickSearch<FilterSearchItem> search, Collection<String> values, OptionalInt numberOfTopItems, SearchScorer scorer) {
		if (search == null) {
			return new ArrayList<>();
		}

		// Quicksearch can split and also schedule for us.
		List<FilterSearchItem> result = search.findItems(String.join(" ", values), numberOfTopItems.orElse(Integer.MAX_VALUE), scorer);

		if (numberOfTopItems.isEmpty() && result.size() == Integer.MAX_VALUE) {
			log.warn("The quick search returned the maximum number of results ({}) which probably means not all possible results are returned.", Integer.MAX_VALUE);
		}

		return result
				.stream()
				.map(item -> new FEValue(item.getLabel(), item.getValue(), item.getTemplateValues(), item.getOptionValue()))
				.collect(Collectors.toList());
	}

	public ResolvedConceptsResult resolveConceptElements(TreeConcept concept, List<String> conceptCodes) {
		List<ConceptElementId<?>> resolvedCodes = new ArrayList<>();
		List<String> unknownCodes = new ArrayList<>();

		if (concept == null) {
			return new ResolvedConceptsResult(null, null, conceptCodes);
		}

		for (String conceptCode : conceptCodes) {
			ConceptTreeChild child;
			try {
				child = concept.findMostSpecificChild(conceptCode, new CalculatedValue<>(Collections::emptyMap));
				if (child != null) {
					resolvedCodes.add(child.getId());
				}
				else {
					unknownCodes.add(conceptCode);
				}
			}
			catch (ConceptConfigurationException e) {
				log.error("Error while trying to resolve " + conceptCode, e);
			}
		}
		return new ResolvedConceptsResult(resolvedCodes, null, unknownCodes);
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public static class ResolvedFilterResult {
		private ConnectorId tableId;
		private FilterId filterId;
		private List<FEValue> value;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public static class ResolvedConceptsResult {
		private List<ConceptElementId<?>> resolvedConcepts;
		private ResolvedFilterResult resolvedFilter;
		private List<String> unknownCodes;
	}
}
