package com.bakdata.conquery.resources.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.apiv1.FilterSearchItem;
import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.api.description.FEList;
import com.bakdata.conquery.models.api.description.FERoot;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.FrontEndConceptBuilder;
import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.search.QuickSearch;
import com.bakdata.conquery.util.search.SearchScorer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@RequiredArgsConstructor
public class ConceptsProcessor {

	private final Namespaces namespaces;
	private final LoadingCache<Concept<?>, FEList> nodeCache = CacheBuilder.newBuilder()
																		   .softValues()
																		   .expireAfterWrite(10, TimeUnit.MINUTES)
																		   .build(new CacheLoader<Concept<?>, FEList>() {
																			   @Override
																			   public FEList load(Concept<?> concept) throws Exception {
																				   return FrontEndConceptBuilder.createTreeMap(concept);
																			   }
																		   });

	public FERoot getRoot(NamespaceStorage storage, User user) {
		return FrontEndConceptBuilder.createRoot(storage, user);
	}

	public FEList getNode(Concept<?> concept) {
		try {
			return nodeCache.get(concept);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("failed to create frontend node for " + concept, e);
		}
	}

	public List<IdLabel> getDatasets(User user) {
		return namespaces
					   .getAllDatasets()
					   .stream()
					   .filter(d -> user.isPermitted(DatasetPermission.onInstance(Ability.READ.asSet(), d.getId())))
					   .map(d -> new IdLabel(d.getLabel(), d.getId().toString()))
					   .sorted()
					   .collect(Collectors.toList());
	}

	/**
	 * Search for all search terms at once, with stricter scoring.
	 * The user will upload a file and expect only well-corresponding resolutions.
	 *
	 * @apiNote Users expect the results to be precise. Additionally they expect all potentially resolvable values to be returned. This is used to select a large list of entries in BigMultiSelects, where users cannot be expected to review every single value.
	 */
	public ResolvedConceptsResult resolveFilterValues(AbstractSelectFilter<?> filter, List<String> searchTerms) {

		//search in the full text engine
		Set<String> searchResult = Collections.emptySet();
		QuickSearch<FilterSearchItem> search = filter.getSourceSearch();

		// If we have a QuickSearch, use that for resolving first.
		if (search != null) {
			List<FilterSearchItem> result = search.findAllItems(searchTerms, filter.getSearchType()::score);

			List<FEValue> res = result.stream()
									  .map(item -> new FEValue(item.getLabel(), item.getValue(), item.getTemplateValues(), item.getOptionValue()))
									  .collect(Collectors.toList());

			searchResult = res.stream()
							  .map(FEValue::getValue)
							  .collect(Collectors.toSet());
		}


		Set<String> openSearchTerms = new HashSet<>(searchTerms);
		openSearchTerms.removeAll(searchResult);

		// Iterate over all unresolved search terms. Gather all that match labels into searchResults. Keep the unresolvable ones.
		for (Iterator<String> it = openSearchTerms.iterator(); it.hasNext(); ) {
			String searchTerm = it.next();
			// Test if any of the values occurs directly in the filter's values or their labels (for when we don't have a provided file).
			if (filter.getValues() != null && filter.getValues().contains(searchTerm)) {
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
				openSearchTerms
		);
	}

	/**
	 * Autocompletion for search terms. For values of {@link AbstractSelectFilter<?>}.
	 */
	public List<FEValue> autocompleteTextFilter(AbstractSelectFilter<?> filter, String text) {
		List<FEValue> result = new LinkedList<>();

		QuickSearch<FilterSearchItem> search = filter.getSourceSearch();
		if (search != null) {
			result = createSourceSearchResult(filter.getSourceSearch(), Collections.singletonList(text), FilterSearch.FilterSearchType.CONTAINS::score);
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
	private List<FEValue> createSourceSearchResult(QuickSearch<FilterSearchItem> search, Collection<String> values, SearchScorer scorer) {
		if (search == null) {
			return Collections.emptyList();
		}

		List<FilterSearchItem> result = search.findItems(String.join(" ", values), 50, scorer);

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
	@NoArgsConstructor
	private class ResolvedFilter {
		private Column column;
		private Map<String, String> realLabels;
		private QuickSearch<FilterSearchItem> sourceSearch;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public static class ResolvedConceptsResult {
		private Collection<ConceptElementId<?>> resolvedConcepts;
		private ResolvedFilterResult resolvedFilter;
		private Collection<String> unknownCodes;
	}
}
