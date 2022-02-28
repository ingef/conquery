package com.bakdata.conquery.resources.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.search.TrieSearch;
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

								if (Strings.isNullOrEmpty(searchTerm)) {
									return listAllValues(filter);
								}

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

		// search in the full text engine
		Set<String> openSearchTerms = new HashSet<>(searchTerms);

		final Namespace namespace = namespaces.get(filter.getDataset().getId());

		List<FEValue> out = new ArrayList<>();

		for (String reference : filter.getSearchReferences()) {
			final List<FEValue> searchResult = namespace.getFilterSearch()
														.getSearchFor(reference).findExact(openSearchTerms, Integer.MAX_VALUE);

			searchResult.forEach(result -> openSearchTerms.remove(result.getValue()));

			out.addAll(searchResult);
		}

		return new ResolvedConceptsResult(
				null,
				new ResolvedFilterResult(
						filter.getConnector().getId(),
						filter.getId(),
						out
				),
				openSearchTerms
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


		try {
			log.trace("Searching for for the term `{}`. (Page = {}, Items = {})", text, pageNumber, itemsPerPage);

			List<FEValue> fullResult = searchCache.get(Pair.of(filter, text));

			int startIncl = Math.min(itemsPerPage * pageNumber, fullResult.size());
			int endExcl = Math.min(startIncl + itemsPerPage, fullResult.size());

			log.trace("Preparing subresult for search term `{}` in the index range [{}-{})", text, startIncl, endExcl);

			return new AutoCompleteResult(fullResult.subList(startIncl, endExcl), fullResult.size());
		}
		catch (ExecutionException e) {
			log.warn("Failed to search for \"{}\".", text, (Throwable) (log.isTraceEnabled() ? e : null));
			return new AutoCompleteResult(Collections.emptyList(), 0);
		}


	}


	private List<FEValue> listAllValues(AbstractSelectFilter<?> filter) {
		final Namespace namespace = namespaces.get(filter.getDataset().getId());

		List<FEValue> out = new ArrayList<>();

		for (String reference : filter.getSearchReferences()) {
			final TrieSearch<FEValue> search = namespace.getFilterSearch().getSearchFor(reference);
			out.addAll(search.listItems());
		}

		return out;
	}

	/**
	 * Autocompletion for search terms. For values of {@link AbstractSelectFilter<?>}.
	 * Is used by the serach cache to load missing items
	 */
	private List<FEValue> autocompleteTextFilter(AbstractSelectFilter<?> filter, String text) {
		final Namespace namespace = namespaces.get(filter.getDataset().getId());

		List<FEValue> out = new ArrayList<>();

		for (String reference : filter.getSearchReferences()) {

			List<FEValue> result = createSourceSearchResult(
					namespace.getFilterSearch().getSearchFor(reference),
					Collections.singletonList(text),
					OptionalInt.empty()
			);

			out.addAll(result);
		}

		return out;
	}

	/**
	 * Do a search with the supplied values.
	 */
	private static List<FEValue> createSourceSearchResult(TrieSearch<FEValue> search, Collection<String> values, OptionalInt numberOfTopItems) {
		if (search == null) {
			return Collections.emptyList();
		}

		// Quicksearch can split and also schedule for us.
		List<FEValue> result = search.findItems(values, numberOfTopItems.orElse(Integer.MAX_VALUE));

		if (numberOfTopItems.isEmpty() && result.size() == Integer.MAX_VALUE) {
			//TODO This looks odd, do we really expect QuickSearch to allocate that huge of a list for us?
			log.warn("The quick search returned the maximum number of results ({}) which probably means not all possible results are returned.", Integer.MAX_VALUE);
		}

		return result;
	}

	public ResolvedConceptsResult resolveConceptElements(TreeConcept concept, List<String> conceptCodes) {

		final List<ConceptElementId<?>> resolvedCodes = new ArrayList<>();
		final List<String> unknownCodes = new ArrayList<>();

		for (String conceptCode : conceptCodes) {
			try {
				ConceptTreeChild child = concept.findMostSpecificChild(conceptCode, new CalculatedValue<>(Collections::emptyMap));

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
		private Collection<FEValue> value;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public static class ResolvedConceptsResult {
		private List<ConceptElementId<?>> resolvedConcepts;
		private ResolvedFilterResult resolvedFilter;
		private Collection<String> unknownCodes;
	}
}
