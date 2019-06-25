package com.bakdata.conquery.apiv1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.ArrayUtils;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.api.description.FEList;
import com.bakdata.conquery.models.api.description.FERoot;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.FrontEndConceptBuilder;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.concepts.filters.specific.BigMultiSelectFilter;
import com.bakdata.conquery.models.concepts.filters.specific.MultiSelectFilter;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConceptConnector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.CalculatedValue;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zigurs.karlis.utils.search.QuickSearch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContentTreeProcessor {

	private Namespaces namespaces;
	private LoadingCache<Concept<?>, FEList> nodeCache = CacheBuilder.newBuilder()
		.softValues()
		.expireAfterWrite(10, TimeUnit.MINUTES)
		.build(new CacheLoader<Concept<?>, FEList>() {
			@Override
			public FEList load(Concept<?> concept) throws Exception {
				return FrontEndConceptBuilder.createTreeMap(concept);
			};
		});
		
	public ContentTreeProcessor(Namespaces namespaces) {
		this.namespaces = namespaces;
		FilterSearch.init(namespaces.getAllDatasets());
	}

	public FERoot getRoot(NamespaceStorage storage) {

		return FrontEndConceptBuilder.createRoot(storage);
	}

	public List<FEValue> autocompleteTextFilter(Dataset dataset, Table table, Filter filter, String text) {
		List<FEValue> result = new LinkedList<>();

		BigMultiSelectFilter tf = (BigMultiSelectFilter) filter;

		QuickSearch<FilterSearchItem> search = tf.getSourceSearch();
		if (search != null) {
			result = createSourceSearchResult(tf.getSourceSearch(), text);
		}
		
		if (tf.getRealLabels() != null) {
			result.addAll(tf.getRealLabels().entrySet().stream()
				.filter(r -> r.getValue().equalsIgnoreCase(text))
				.map(r -> new FEValue(r.getKey(), r.getValue())).collect(Collectors.toList()));
		}

		// see https://github.com/bakdata/conquery/issues/235
		return result;
	}

	public FEList getNode(Concept<?> concept) {
		try {
			return nodeCache.get(concept);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("failed to create frontend node for "+concept, e);
		}
	}
	
	public List<IdLabel> getDatasets(User user) {
		return namespaces
			.getAllDatasets()
			.stream()
			.filter(d -> user.isPermitted(new DatasetPermission(user.getId(), Ability.READ.asSet(), d.getId())))
			.map(d -> new IdLabel(d.getLabel(), d.getId().toString()))
			.collect(Collectors.toList());
	}

	public ResolvedConceptsResult resolve(Dataset dataset, ConceptElement conceptElement, List<String> conceptCodes) {
		List<String> resolvedCodes = new ArrayList<>(), unknownCodes = new ArrayList<>();

		if (conceptElement.getConcept() instanceof TreeConcept) {
			TreeConcept tree = (TreeConcept) conceptElement.getConcept();

			for (String conceptCode : conceptCodes) {
				ConceptTreeChild child;
				try {
					child = tree.findMostSpecificChild(conceptCode, new CalculatedValue<>(() -> new HashMap<>()));
					if (child != null) {
						resolvedCodes.add(child.getId().toString());
					}
					else {
						unknownCodes.add(conceptCode);
					}
				}
				catch (ConceptConfigurationException e) {
					log.error("", e);
				}
			}
			return new ResolvedConceptsResult(resolvedCodes, null, unknownCodes);
		}

		if (conceptElement.getConcept() instanceof VirtualConcept) {
			VirtualConcept virtualConcept = (VirtualConcept) conceptElement.getConcept();

			for (VirtualConceptConnector connector : virtualConcept.getConnectors()) {
				// A virtual concept by definition has only one concept connector
				if (connector.getFilter() instanceof AbstractSelectFilter) {
					AbstractSelectFilter<?> selectFilter = (AbstractSelectFilter<?>) connector.getFilter();
					for (String conceptCode : conceptCodes) {
						String resolved = selectFilter.resolveValueToRealValue(conceptCode);
						if (resolved != null) {
							resolvedCodes.add(resolved);
						}
						else {
							unknownCodes.add(conceptCode);
						}
					}

					List<FEValue> filterValues = new LinkedList<>();
					QuickSearch<FilterSearchItem> search = selectFilter.getSourceSearch();
					if (search != null) {
						filterValues.addAll(createSourceSearchResult(search, conceptCodes.toArray(ArrayUtils.EMPTY_STRING_ARRAY)));
					}

					List<String> toRemove = filterValues.stream().map(v -> v.getValue()).collect(Collectors.toList());
					filterValues
						.addAll(
							resolvedCodes
								.stream()
								.filter(v -> !toRemove.contains(v))
								.map(v -> new FEValue(selectFilter.getRealLabels().get(v), v))
								.collect(Collectors.toList()));

					return new ResolvedConceptsResult(
						null,
						new ResolvedFilterResult(connector.getId().toString(), selectFilter.getId().toString(), filterValues),
						unknownCodes);
				}
			}
		}
		return new ResolvedConceptsResult(null, null, conceptCodes);
	}

	private List<FEValue> createSourceSearchResult(QuickSearch<FilterSearchItem> search, String... values) {
		List<FilterSearchItem> result = new LinkedList<>();
		for (String value : values) {
			result.addAll(search.findItems(value, 50));
		}
		
		return result
			.stream()
			.map(item -> new FEValue(item.getLabel(), item.getValue(), item.getTemplateValues(), item.getOptionValue()))
			.collect(Collectors.toList());
	}

	private ResolvedFilter createResolvedFilter(Filter<?> filter) {
		ResolvedFilter result = new ResolvedFilter();

		if (filter instanceof BigMultiSelectFilter) {
			BigMultiSelectFilter bmsf = (BigMultiSelectFilter) filter;
			result.setColumn(bmsf.getColumn());
			result.setRealLabels(bmsf.getRealLabels());
			result.setSourceSearch(bmsf.getSourceSearch());
		} else if (filter instanceof MultiSelectFilter) {
			MultiSelectFilter msf = (MultiSelectFilter) filter;
			result.setColumn(msf.getColumn());
			result.setRealLabels(msf.getRealLabels());
			result.setSourceSearch(msf.getSourceSearch());
		} else {
			try {
				throw new WebApplicationException(String.format("Could not resolved Filter values for this Type. Filter: %s", filter.getName()));
			} catch (WebApplicationException ex) {
				log.error(ex.getMessage());
			}
		}
		

		return result;
	}

	public ResolvedConceptsResult resolveFilterValues(Dataset dataset, Table table, Filter filter, List<String> values) {
		ResolvedFilter rf = createResolvedFilter(filter);

		List<FEValue> filterValues = new LinkedList<>();
		QuickSearch<FilterSearchItem> search = rf.getSourceSearch();
		if (search != null) {
			filterValues.addAll(createSourceSearchResult(search, values.toArray(new String[values.size()])));
		}
		
		if (rf.getRealLabels() != null) {
			List<String> resolveFilterValues = new ArrayList<>(rf.getRealLabels().values());
			List<String> toRemove = filterValues.stream().map(v -> v.getValue()).collect(Collectors.toList());
			resolveFilterValues.removeIf(fv -> !toRemove.contains(fv) && !values.contains(fv));
			filterValues = resolveFilterValues.stream().map(v -> new FEValue(rf.getRealLabels().get(v), v)).collect(Collectors.toList());
			values.removeAll(resolveFilterValues);
		}

		// see https://github.com/bakdata/conquery/issues/251
		return new ResolvedConceptsResult(
			null,
			new ResolvedFilterResult(table.getId().getTable(), filter.getId().toString(), filterValues),
			values);
	}

	@Getter
	@Setter
	@AllArgsConstructor
	public static class ResolvedConceptsResult {

		private List<String> resolvedConcepts;
		private ResolvedFilterResult resolvedFilter;
		private List<String> unknownCodes;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	public static class ResolvedFilterResult {

		private String tableId;
		private String filterId;
		private List<FEValue> value;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	private class ResolvedFilter {

		private Column column;
		private Map<String, String> realLabels;
		private QuickSearch sourceSearch;
	}
}
