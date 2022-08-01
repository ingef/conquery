package com.bakdata.conquery.resources.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.apiv1.frontend.FEList;
import com.bakdata.conquery.apiv1.frontend.FERoot;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.FrontEndConceptBuilder;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.search.Cursor;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterators;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@Slf4j
@RequiredArgsConstructor
public class ConceptsProcessor {

	private final DatasetRegistry namespaces;
	private final Validator validator;

	private final LoadingCache<Concept<?>, FEList>
			nodeCache =
			CacheBuilder.newBuilder().softValues().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<>() {
				@Override
				public FEList load(Concept<?> concept) {
					return FrontEndConceptBuilder.createTreeMap(concept);
				}
			});

	/**
	 * Cache of all search results on SelectFilters.
	 */
	private final LoadingCache<Pair<SelectFilter<?>, String>, List<FEValue>> searchResults = CacheBuilder.newBuilder().softValues().build(new CacheLoader<>() {

		@Override
		public List<FEValue> load(Pair<SelectFilter<?>, String> filterAndSearch) {
			String searchTerm = filterAndSearch.getValue();
			SelectFilter<?> filter = filterAndSearch.getKey();

			log.trace("Calculating a new search cache for the term \"{}\" on filter[{}]", searchTerm, filter.getId());

			return autocompleteTextFilter(filter, searchTerm);
		}

	});

	/**
	 * Container class to pair number of available values and Cursor for those values.
	 */
	@Value
	private static class CursorAndLength {
		private final Cursor<FEValue> values;
		private final long size;
	}

	/**
	 * Cache of raw listing of values on a filter.
	 * We use Cursor here to reduce strain on memory and increase response time.
	 */
	private final LoadingCache<SelectFilter<?>, CursorAndLength> listResults = CacheBuilder.newBuilder().softValues().build(new CacheLoader<>() {
		@Override
		public CursorAndLength load(SelectFilter<?> filter) {
			log.debug("Creating cursor for `{}`", filter.getId());
			return new CursorAndLength(listAllValues(filter), countAllValues(filter));
		}

	});


	public FERoot getRoot(NamespaceStorage storage, Subject subject) {

		final FERoot root = FrontEndConceptBuilder.createRoot(storage, subject);

		// Report Violation
		ValidatorHelper.createViolationsString(validator.validate(root), log.isTraceEnabled()).ifPresent(log::warn);

		return root;
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
						 .sorted(Comparator.comparing(Dataset::getWeight).thenComparing(Dataset::getLabel))
						 .map(d -> new IdLabel<>(d.getId(), d.getLabel()))
						 .collect(Collectors.toList());
	}

	public Stream<ConnectorId> getEntityPreviewDefaultConnectors(Dataset dataset) {
		return namespaces.get(dataset.getId())
						 .getStorage()
						 .getAllConcepts()
						 .stream()
						 .map(Concept::getConnectors)
						 .flatMap(Collection::stream)
						 .filter(Connector::isDefaultForEntityPreview)
						 .map(Identifiable::getId);
	}

	/**
	 * Search for all search terms at once, with stricter scoring.
	 * The user will upload a file and expect only well-corresponding resolutions.
	 */
	public ResolvedConceptsResult resolveFilterValues(SelectFilter<?> filter, List<String> searchTerms) {

		// search in the full text engine
		final Set<String> openSearchTerms = new HashSet<>(searchTerms);

		final Namespace namespace = namespaces.get(filter.getDataset().getId());

		final List<FEValue> out = new ArrayList<>();

		for (TrieSearch<FEValue> search : namespace.getFilterSearch().getSearchesFor(filter)) {
			for (Iterator<String> iterator = openSearchTerms.iterator(); iterator.hasNext(); ) {

				final String searchTerm = iterator.next();
				final List<FEValue> results = search.findExact(List.of(searchTerm), Integer.MAX_VALUE);

				if (results.isEmpty()) {
					continue;
				}

				iterator.remove();
				out.addAll(results);
			}
		}

		return new ResolvedConceptsResult(null, new ResolvedFilterResult(filter.getConnector().getId(), filter.getId(), out), openSearchTerms);
	}

	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class AutoCompleteResult {
		private final List<FEValue> values;
		private final long total;
	}

	public AutoCompleteResult autocompleteTextFilter(SelectFilter<?> filter, Optional<String> maybeText, OptionalInt pageNumberOpt, OptionalInt itemsPerPageOpt) {
		final int pageNumber = pageNumberOpt.orElse(0);
		final int itemsPerPage = itemsPerPageOpt.orElse(50);

		Preconditions.checkArgument(pageNumber >= 0, "Page number must be 0 or a positive integer.");
		Preconditions.checkArgument(itemsPerPage > 1, "Must at least have one item per page.");

		log.trace("Searching for for  `{}` in `{}`. (Page = {}, Items = {})", maybeText, filter.getId(), pageNumber, itemsPerPage);

		final int startIncl = itemsPerPage * pageNumber;
		final int endExcl = startIncl + itemsPerPage;

		try {

			// If we have none or a blank query string we list all values.
			if (maybeText.isEmpty() || maybeText.get().isBlank()) {
				final CursorAndLength cursorAndLength = listResults.get(filter);
				final Cursor<FEValue> cursor = cursorAndLength.getValues();

				return new AutoCompleteResult(cursor.get(startIncl, endExcl), cursorAndLength.getSize());
			}

			final List<FEValue> fullResult = searchResults.get(Pair.of(filter, maybeText.get()));

			if (startIncl >= fullResult.size()) {
				return new AutoCompleteResult(Collections.emptyList(), fullResult.size());
			}

			return new AutoCompleteResult(fullResult.subList(startIncl, Math.min(fullResult.size(), endExcl)), fullResult.size());
		}
		catch (ExecutionException e) {
			log.warn("Failed to search for \"{}\".", maybeText, (Throwable) (log.isTraceEnabled() ? e : null));
			return new AutoCompleteResult(Collections.emptyList(), 0);
		}
	}


	private Cursor<FEValue> listAllValues(SelectFilter<?> filter) {
		final Namespace namespace = namespaces.get(filter.getDataset().getId());
		/*
		Don't worry, I am as confused as you are!
		For some reason, flatMapped streams in conjunction with distinct will be evaluated full before further operation.
		This in turn causes initial loads of this endpoint to extremely slow. By instead using iterators we have uglier code but enforce laziness.

		See: https://stackoverflow.com/questions/61114380/java-streams-buffering-huge-streams
		 */

		final Iterator<FEValue>
				iterators =
				Iterators.concat(Iterators.transform(namespace.getFilterSearch().getSearchesFor(filter).iterator(), TrieSearch::iterator));

		// Use Set to accomplish distinct values
		final Set<FEValue> seen = new HashSet<>();

		return new Cursor<>(Iterators.filter(iterators, seen::add));
	}

	private long countAllValues(SelectFilter<?> filter) {
		final Namespace namespace = namespaces.get(filter.getDataset().getId());


		return namespace.getFilterSearch().getTotal(filter);
	}

	/**
	 * Autocompletion for search terms. For values of {@link SelectFilter <?>}.
	 * Is used by the serach cache to load missing items
	 */
	private List<FEValue> autocompleteTextFilter(SelectFilter<?> filter, String text) {
		final Namespace namespace = namespaces.get(filter.getDataset().getId());

		// Note that FEValues is equals/hashcode only on value:
		// The different sources might contain duplicate FEValue#values which we want to avoid as
		// they are already sorted in terms of information weight by getSearchesFor

		// Also note: currently we are still issuing large search requests, but much smaller allocations at once, and querying only when the past is not sufficient
		return namespace.getFilterSearch()
						.getSearchesFor(filter)
						.stream()
						.map(search -> createSourceSearchResult(search, Collections.singletonList(text), OptionalInt.empty()))
						.flatMap(Collection::stream)
						.distinct()
						.collect(Collectors.toList());


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

		final Set<ConceptElementId<?>> resolvedCodes = new HashSet<>();
		final Set<String> unknownCodes = new HashSet<>();

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
		private Set<ConceptElementId<?>> resolvedConcepts;
		private ResolvedFilterResult resolvedFilter;
		private Collection<String> unknownCodes;
	}
}
