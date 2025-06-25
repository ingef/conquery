package com.bakdata.conquery.resources.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.validation.Validator;

import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.apiv1.frontend.FrontendList;
import com.bakdata.conquery.apiv1.frontend.FrontendPreviewConfig;
import com.bakdata.conquery.apiv1.frontend.FrontendRoot;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.FrontEndConceptBuilder;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConceptsProcessor {

	private final DatasetRegistry<? extends Namespace> namespaces;
	private final Validator validator;

	private final ConqueryConfig config;

	@Getter(lazy = true)
	private final FrontEndConceptBuilder frontEndConceptBuilder = new FrontEndConceptBuilder(getConfig());

	private final LoadingCache<Concept<?>, FrontendList> nodeCache =
			CacheBuilder.newBuilder().softValues().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<>() {
				@Override
				public FrontendList load(Concept<?> concept) {
					return getFrontEndConceptBuilder().createTreeMap(concept);
				}
			});


	public FrontendRoot getRoot(NamespaceStorage storage, Subject subject, boolean showHidden) {

		final FrontendRoot root = getFrontEndConceptBuilder().createRoot(storage, subject, showHidden);

		// Report Violation
		ValidatorHelper.createViolationsString(validator.validate(root), log.isTraceEnabled()).ifPresent(log::warn);

		return root;
	}

	public FrontendList getNode(Concept<?> concept) {
		try {
			return nodeCache.get(concept);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("failed to create frontend node for " + concept, e);
		}
	}

	public Stream<IdLabel<DatasetId>> getDatasets(Subject subject) {
		return namespaces.getAllDatasets()
						 .filter(d -> subject.isPermitted(d, Ability.READ))
						 .map(DatasetId::resolve)
						 .sorted(Comparator.comparing(Dataset::getWeight).thenComparing(Dataset::getLabel))
						 .map(d -> new IdLabel<>(d.getId(), d.getLabel()));
	}

	public FrontendPreviewConfig getEntityPreviewFrontendConfig(DatasetId dataset) {
		final Namespace namespace = namespaces.get(dataset);
		final PreviewConfig previewConfig = namespace.getPreviewConfig();

		// Connectors only act as bridge to table for the fronted, but also provide ConceptColumnT semantic

		return new FrontendPreviewConfig(
				previewConfig.getAllConnectors()
							 .stream()
							 .map(id -> new FrontendPreviewConfig.Labelled(id.toString(), id.resolve().getResolvedTable().getLabel()))
							 .collect(Collectors.toSet()),

				previewConfig.getDefaultConnectors()
							 .stream()
							 .map(id -> new FrontendPreviewConfig.Labelled(id.toString(), id.resolve().getResolvedTable().getLabel()))
							 .collect(Collectors.toSet()),
				previewConfig.getSearchFilters(),
				previewConfig.resolveSearchConcept()
		);
	}

	/**
	 * Search for all search terms at once, with stricter scoring.
	 * The user will upload a file and expect only well-corresponding resolutions.
	 */
	public ResolvedFilterValues resolveFilterValues(FilterId filterId, List<String> searchTerms) {
		SelectFilter<?> filter = (SelectFilter<?>) filterId.resolve();

		// search in the full text engine
		final Set<String> openSearchTerms = new HashSet<>(searchTerms);

		final Namespace namespace = namespaces.get(filter.getDataset());

		final List<FrontendValue> out = new ArrayList<>();


		SearchProcessor filterSearch = namespace.getFilterSearch();

		for (final Iterator<String> iterator = openSearchTerms.iterator(); iterator.hasNext(); ) {

			final String searchTerm = iterator.next();
			final List<FrontendValue> results = filterSearch.findExact(filter, searchTerm);

			if (results.isEmpty()) {
				continue;
			}

			iterator.remove();
			out.addAll(results);
		}

		final ConnectorId connectorId = filter.getConnector().getId();

		return new ResolvedFilterValues(new ResolvedFilterResult(connectorId, filter.getId().toString(), out), openSearchTerms);
	}

	public AutoCompleteResult autocompleteTextFilter(
			FilterId filterId,
			String maybeText,
			OptionalInt pageNumberOpt,
			OptionalInt itemsPerPageOpt
	) {
		final int pageNumber = pageNumberOpt.orElse(0);
		final int itemsPerPage = itemsPerPageOpt.orElse(50);

		Preconditions.checkArgument(pageNumber >= 0, "Page number must be 0 or a positive integer.");
		Preconditions.checkArgument(itemsPerPage > 1, "Must at least have one item per page.");

		final SelectFilter<?> filter = (SelectFilter<?>) filterId.resolve();

		log.trace("Searching for for  `{}` in `{}`. (Page = {}, Items = {})", maybeText, filterId, pageNumber, itemsPerPage);

		return namespaces.get(filter.getDataset()).getFilterSearch().query(filter, maybeText, itemsPerPage, pageNumber);
	}

	public ResolvedConceptsResult resolveConceptElements(TreeConcept concept, List<String> conceptCodes) {

		final Set<ConceptElementId<?>> resolvedCodes = new HashSet<>();
		final Set<String> unknownCodes = new HashSet<>();

		for (String conceptCode : conceptCodes) {
			try {
				final ConceptElement<?> child = concept.findMostSpecificChild(conceptCode, new CalculatedValue<>(Collections::emptyMap));

				if (child != null) {
					resolvedCodes.add(child.getId());
				}
				else {
					unknownCodes.add(conceptCode);
				}
			}
			catch (ConceptConfigurationException e) {
				log.error("Error while trying to resolve `{}`", conceptCode, e);
			}
		}
		return new ResolvedConceptsResult(resolvedCodes, unknownCodes);
	}

	public record AutoCompleteResult(List<FrontendValue> values, long total) {
	}

	public record ResolvedFilterResult(ConnectorId tableId, String filterId, Collection<FrontendValue> value) {
		//TODO FK filterId as Id causes issues with IdUtil createParser, should investigate this
	}

	public record ResolvedFilterValues(ResolvedFilterResult resolvedFilter, Collection<String> unknownCodes) {

	}

	@Data
	public static final class ResolvedConceptsResult {
		private final Set<ConceptElementId<?>> resolvedConcepts;
		private final Collection<String> unknownCodes;


	}
}
