package com.bakdata.conquery.apiv1;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConcept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.google.common.base.Stopwatch;
import com.zigurs.karlis.utils.search.QuickSearch;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides the methods of QuickSearch.
 */
@Slf4j
public class ConceptSearch {

	private Map<IId<?>, QuickSearch<String>> searchMap;

	public ConceptSearch(Collection<Dataset> datasets) {
		this.searchMap = datasets.stream()
			.collect(Collectors.toMap(
				Dataset::getId,
				ds -> addConceptElement(ds)));
	}

	/**
	 * Add {@link ConceptElement} attributes (Label, Description, additional Infos) to QuickSearch context.
	 *
	 * @param dataset
	 */
	private QuickSearch<String> addConceptElement(Dataset dataset) {
		Stopwatch watch = Stopwatch.createStarted();

		String datasetName = dataset.getId().toString();

		QuickSearch<String> qs = new QuickSearch.QuickSearchBuilder()
			.withUnmatchedPolicy(QuickSearch.UnmatchedPolicy.IGNORE)
			.withMergePolicy(QuickSearch.MergePolicy.INTERSECTION)
			.withParallelProcessing()
			.build();

		List<ConceptElement<?>> elements = dataset.getConcepts().stream()
			.filter(TreeConcept.class::isInstance)
			.map(TreeConcept.class::cast)
			.map(c -> c.getAllChildren().values())
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
		
		elements.forEach((ele) -> {
			String key = ele.getId().toString();

			qs.addItem(key, ele.getLabel());
			qs.addItem(key, ele.getDescription());
			qs.addItem(key, ele
				.getAdditionalInfos()
				.stream()
				.map(Objects::toString)
				.collect(Collectors.joining())
			);
		});
		
		dataset.getConcepts().stream()
			.filter(VirtualConcept.class::isInstance)
			.map(VirtualConcept.class::cast)
			.forEach(c -> {
				String key = c.getId().toString();
				qs.addItem(key, c.getLabel());
				qs.addItem(key, c.getName());
				qs.addItem(key, c.getDescription());
			});

		log.info("{} ConceptElements collected from '{}' Dataset in {}", elements.size(), datasetName, watch.stop());
		return qs;
	}

	/**
	 * Retrieve top (Integer.MAX_VALUE) items matching the supplied search string.
	 *
	 * @param datasetId HierarchicalName of Dataset
	 * @param query raw search string
	 * @return list (possibly empty) containing up to n top search results
	 */
	public List<String> findItems(DatasetId datasetId, String query) {
		return searchMap.get(datasetId).findItems(query, Integer.MAX_VALUE);
	}
}
