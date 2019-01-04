package com.bakdata.conquery.apiv1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.ContentTreeResources.SearchResult;
import com.bakdata.conquery.models.api.description.FENode;
import com.bakdata.conquery.models.api.description.FERoot;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.FrontEndConceptBuilder;
import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.concepts.filters.Filter;
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
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.ResourceUtil;
import com.zigurs.karlis.utils.search.QuickSearch;

import io.dropwizard.auth.Auth;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContentTreeProcessor {

	private ConceptSearch conceptSearch;
	private Namespaces namespaces;
	private ResourceUtil dsUtil;

	public ContentTreeProcessor(Namespaces namespaces) {
		this.namespaces = namespaces;
		this.conceptSearch = new ConceptSearch(namespaces.getAllDatasets());
		this.dsUtil = new ResourceUtil(namespaces);
		FilterSearch.init(namespaces.getAllDatasets());
	}

	public FERoot getRoot(Dataset dataset) {
		return FrontEndConceptBuilder.createRoot(dataset);
	}

	public List<FEValue> autocompleteTextFilter(@Auth User user, Dataset dataset, Table table, Filter filter, String text) {
		List<FEValue> result = new LinkedList<>();

		BigMultiSelectFilter tf = (BigMultiSelectFilter) filter;

		QuickSearch<FilterSearchItem> search = tf.getSourceSearch();
		if (search != null) {
			result = createSourceSearchResult(search, text);
		}

		// TODO 
//		List<String> res = meta.getAutoCompleteSuggestions(dataset, tf.getColumn(), text); 
//              if(res != null) result.addAll(res.stream().map(v->new FEValue(tf.getRealLabels().get(v), v)).collect(Collectors.toList()));
//                dataset.get
		return result;
	}

	public Map<ConceptElementId<?>, FENode> getNode(@Auth User user, Dataset dataset, IId id) {
		Map<ConceptId, Map<ConceptElementId<?>, FENode>> ctRoots = FrontEndConceptBuilder
			.createTreeMap(dataset.getConcepts());
		return ctRoots.get(id);
	}

	public List<IdLabel> getDatasets(User user) {
		return namespaces.getAllDatasets().stream()
			//                        .filter(d -> user.isPermitted(new IdentifiableInstancePermission(user.getId(), AccessType.READ, d.getId())))
			.map(d -> new IdLabel(d.getLabel(), d.getId().toString()))
			.collect(Collectors.toList());
	}

	public ResolvedConceptsResult resolve(User user, Dataset dataset, ConceptElement conceptElement,
		List<String> conceptCodes) {

		List<String> resolvedCodes = new ArrayList<>(), unknownCodes = new ArrayList<>();

		if (conceptElement.getConcept() instanceof TreeConcept) {
			TreeConcept tree = (TreeConcept) conceptElement.getConcept();

			for (String conceptCode : conceptCodes) {
				ConceptTreeChild child;
				try {
					child = tree.findMostSpecificChild(conceptCode, new CalculatedValue<>(() -> new HashMap<>()));
					if (child != null) {
						resolvedCodes.add(child.getId().toString());
					} else {
						unknownCodes.add(conceptCode);
					}
				} catch (ConceptConfigurationException e) {
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
						} else {
							unknownCodes.add(conceptCode);
						}
					}

					List<FEValue> filterValues = new LinkedList<>();
					QuickSearch<FilterSearchItem> search = selectFilter.getSourceSearch();
					if (search != null) {
						filterValues.addAll(createSourceSearchResult(search, conceptCodes.toArray(new String[conceptCodes.size()])));
					}

					List<String> toRemove = filterValues.stream().map(v -> v.getValue()).collect(Collectors.toList());
					filterValues.addAll(resolvedCodes.stream().filter(v -> !toRemove.contains(v))
						.map(v -> new FEValue(selectFilter.getRealLabels().get(v), v))
						.collect(Collectors.toList()));

					return new ResolvedConceptsResult(null, new ResolvedFilterResult(connector.getId().toString(),
						selectFilter.getId().toString(), filterValues), unknownCodes);
				}
			}
		}
		return new ResolvedConceptsResult(null, null, conceptCodes);
	}

	private List<FEValue> createSourceSearchResult(QuickSearch<FilterSearchItem> search, String... values) {
		List<FilterSearchItem> result = new LinkedList<>();
		for (String value : values) {
			result.addAll(search.findItems(value, 10));
		}

		return result.stream().map(
			item -> new FEValue(item.getLabel(), item.getValue(), item.getTemplateValues(), item.getOptionValue()))
			.collect(Collectors.toList());
	}

	private ResolvedFilter createResolvedFilter(Filter<?> filter) {
		ResolvedFilter result = new ResolvedFilter();

		if (filter instanceof BigMultiSelectFilter) {
			BigMultiSelectFilter bmsf = (BigMultiSelectFilter) filter;
			result.setColumn(bmsf.getColumn());
			result.setRealLabels(bmsf.getRealLabels());
		} else {
			MultiSelectFilter msf = (MultiSelectFilter) filter;
			result.setColumn(msf.getColumn());
			result.setRealLabels(msf.getRealLabels());
		}

		return result;
	}

	public ResolvedConceptsResult resolveFilterValues(@Auth User user, Dataset dataset, Table table, Filter filter, List<String> values) {
		BigMultiSelectFilter tf = (BigMultiSelectFilter) filter;

		List<FEValue> filterValues = new LinkedList<>();
		QuickSearch<FilterSearchItem> search = tf.getSourceSearch();
		if (search != null) {
			filterValues.addAll(createSourceSearchResult(search, values.toArray(new String[values.size()])));
		}

		ResolvedFilter rf = createResolvedFilter(filter);

		/*
		 * FIXME not yet supported
		 */
//		              @SqlQuery("SELECT value FROM <dataset.schema>.<column.dictName> WHERE value IN (SELECT * FROM UNNEST(:values));"
//                )
//            List<String> resolveFilterValues = meta.resolveFilterValues(dataset, rf.getColumn(), values.toArray(new String[values.size()]));
//           
//            if (resolveFilterValues != null) {
//		List<String> toRemove = filterValues.stream().map(v -> v.getValue()).collect(Collectors.toList());
//		filterValues.addAll(resolveFilterValues.stream()
//			.filter(v -> !toRemove.contains(v))
//			.map(v -> new FEValue(rf.getRealLabels().get(v), v))
//			.collect(Collectors.toList()));
//                values.removeAll(filterValues.stream().map(v -> v.getValue()).collect(Collectors.toList()));
//	    }
		return new ResolvedConceptsResult(null, new ResolvedFilterResult(table.getId().getTable(), filter.getId().toString(), filterValues), values);
	}

	public SearchResult search(@Auth User user, Dataset dataset, String query, int limit) {
		List<String> items = conceptSearch.findItems(dataset.getId(), query);
		List<String> result = items.stream().limit(limit).collect(Collectors.toList());

		return new SearchResult(result, result.size(), limit, items.size());

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
	}
}
