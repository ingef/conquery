package com.bakdata.conquery.models.datasets.concepts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendList;
import com.bakdata.conquery.apiv1.frontend.FrontendNode;
import com.bakdata.conquery.apiv1.frontend.FrontendResultType;
import com.bakdata.conquery.apiv1.frontend.FrontendRoot;
import com.bakdata.conquery.apiv1.frontend.FrontendSecondaryId;
import com.bakdata.conquery.apiv1.frontend.FrontendSelect;
import com.bakdata.conquery.apiv1.frontend.FrontendTable;
import com.bakdata.conquery.apiv1.frontend.FrontendValidityDate;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.StructureNodeId;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * This class constructs the concept tree as it is presented to the front end.
 */
@Data
@Slf4j
public class FrontEndConceptBuilder {

	private final ConqueryConfig conqueryConfig;

	public FrontendRoot createRoot(NamespaceStorage storage, Subject subject) {

		final FrontendRoot root = new FrontendRoot();
		final Map<Id<?, ?>, FrontendNode> roots = root.getConcepts();

		final List<? extends Concept<?>> allConcepts =
				storage.getAllConcepts()
					   // Remove any hidden concepts
					   .filter(Predicate.not(Concept::isHidden))
					   .toList();

		if (allConcepts.isEmpty()) {
			log.warn("There are no displayable concepts in the dataset {}", storage.getDataset().getId());
		}

		// Submit all permissions to Shiro
		final boolean[] isPermitted = subject.isPermitted(allConcepts, Ability.READ);

		for (int i = 0; i < allConcepts.size(); i++) {
			if (!isPermitted[i]) {
				continue;
			}

			Concept<?> concept = allConcepts.get(i);
			roots.put(concept.getId(), createConceptRoot(concept, storage.getStructure()));
		}
		if (roots.isEmpty()) {
			log.warn("No concepts could be collected for {} on dataset {}. The subject is possibly lacking the permission to use them.",
					 subject.getId(),
					 storage.getDataset()
							.getId()
			);
		}
		else {
			log.trace("Collected {} concepts for {} on dataset {}.", roots.size(), subject.getId(), storage.getDataset().getId());
		}
		//add the structure tree
		for (StructureNode sn : storage.getStructure()) {
			insertStructureNode(sn, roots);
		}
		//add all secondary IDs
		try (Stream<SecondaryIdDescription> secondaryIds = storage.getSecondaryIds()) {
			root.getSecondaryIds()
				.addAll(secondaryIds
								.filter(sid -> !sid.isHidden())
								.map(sid -> new FrontendSecondaryId(sid.getId().toString(), sid.getLabel(), sid.getDescription()))
								.collect(Collectors.toSet()));
		}

		return root;
	}

	private FrontendNode createConceptRoot(Concept<?> concept, StructureNode[] structureNodes) {

		final StructureNodeId
				structureParent =
				Arrays.stream(structureNodes)
					  .flatMap(StructureNode::stream)
					  .filter(sn -> sn.getContainedRoots().contains(concept.getId()))
					  .findAny()
					  .map(StructureNode::getId)
					  .orElse(null);

		final MatchingStats matchingStats = concept.getMatchingStats();
		boolean hasStats = matchingStats != null;

		final FrontendNode node =
				FrontendNode.builder()
							.active(true)
							.description(concept.getDescription())
							.label(concept.getLabel())
							.additionalInfos(concept.getAdditionalInfos())
							.matchingEntries(hasStats ? matchingStats.countEvents() : 0)
							.matchingEntities(hasStats ? matchingStats.countEntities() : 0)
							.dateRange(hasStats && matchingStats.spanEvents() != null ? matchingStats.spanEvents().toSimpleRange() : null)
							.detailsAvailable(Boolean.TRUE)
							.codeListResolvable(concept.countElements() > 1)
							.parent(structureParent)
							.excludeFromTimeAggregation(concept.isDefaultExcludeFromTimeAggregation() || concept.getConnectors()
																												.stream()
																												.map(Connector::getValidityDates)
																												.flatMap(Collection::stream)
																												.findAny()
																												.isEmpty())
							.selects(concept.getSelects().stream().map(this::createSelect).collect(Collectors.toList()))
							.tables(concept.getConnectors().stream().map(this::createTable).collect(Collectors.toList()))
							.build();

		if (concept.getChildren() != null) {
			node.setChildren(concept.getChildren().stream()
									.map(ConceptTreeChild::getId)
									.toArray(ConceptTreeChildId[]::new));
		}
		return node;
	}

	/**
	 * StructureNodes can be nested and the frontend needs them plain.
	 * This method puts the given {@link StructureNode} and its children into the given root.
	 * If the node references only instances not contained in the given roots element, it is skipped.
	 * This method calls itself recursively.
	 *
	 * @param structureNode the node to process (and its children)
	 * @param roots         the map where the given and child nodes are inserted into.
	 */
	private void insertStructureNode(StructureNode structureNode, Map<Id<?, ?>, FrontendNode> roots) {
		final List<ConceptId> contained = new ArrayList<>();
		for (ConceptId id : structureNode.getContainedRoots()) {
			if (!roots.containsKey(id)) {
				log.trace("Concept from structure node can not be found: {}", id);
				continue;
			}
			contained.add(id);
		}

		if (contained.isEmpty() && structureNode.getChildren().isEmpty()) {
			log.trace("Did not create a structure node entry for {}. Contained no concepts.", structureNode.getId());
			return;
		}

		// Add Children to root
		for (StructureNode n : structureNode.getChildren()) {
			insertStructureNode(n, roots);
		}

		FrontendNode currentNode =
				FrontendNode.builder()
							.active(false)
							.description(structureNode.getDescription())
							.label(structureNode.getLabel())
							.detailsAvailable(Boolean.FALSE)
							.codeListResolvable(false)
							.additionalInfos(structureNode.getAdditionalInfos())
							.parent(structureNode.getParent() == null ? null : structureNode.getParent().getId())
							.children(Stream.concat(structureNode.getChildren().stream().map(Identifiable::getId), contained.stream()).toArray(NamespacedId[]::new))
							.build();

		roots.put(structureNode.getId(), currentNode);
	}

	public FrontendSelect createSelect(Select select) {
		return FrontendSelect.builder()
							 .id(select.getId())
							 .label(select.getLabel())
							 .description(select.getDescription())
							 .resultType(FrontendResultType.from(select.getResultType()))
							 .isDefault(select.isDefault())
							 .build();
	}

	public FrontendTable createTable(Connector con) {
		final FrontendTable
				result =
				FrontendTable.builder()
							 .id(con.getTableId())
							 .connectorId(con.getId())
							 .label(con.getLabel())
							 .isDefault(con.isDefault())
							 .filters(con.collectAllFilters().stream().map(this::createFilter).collect(Collectors.toList()))
							 .selects(con.getSelects().stream().map(this::createSelect).collect(Collectors.toList()))
							 .supportedSecondaryIds(Arrays.stream(con.getResolvedTable().getColumns())
														  .map(Column::getSecondaryId)
														  .filter(Objects::nonNull)
														  .collect(Collectors.toSet()))
							 .build();

		if (!con.getValidityDates().isEmpty()) {
			result.setDateColumn(new FrontendValidityDate(con.getValidityDatesDescription(), null,
														  con.getValidityDates().stream()
															 .map(vd -> new FrontendValue(vd.getId().toString(), vd.getLabel()))
															 .collect(Collectors.toList())
			));

			if (!result.getDateColumn().getOptions().isEmpty()) {
				result.getDateColumn().setDefaultValue(result.getDateColumn().getOptions().getFirst().getValue());
			}
		}

		return result;
	}

	public FrontendFilterConfiguration.Top createFilter(Filter<?> filter) {
		try {
			return filter.createFrontendConfig(conqueryConfig);
		}
		catch (ConceptConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

	private FrontendNode createCTNode(ConceptElement<?> ce) {
		final MatchingStats matchingStats = ce.getMatchingStats();
		FrontendNode.FrontendNodeBuilder nodeBuilder =
				FrontendNode.builder()
							.active(null)
							.description(ce.getDescription())
							.label(ce.getLabel())
							.additionalInfos(ce.getAdditionalInfos())
							.matchingEntries(matchingStats != null ? matchingStats.countEvents() : 0)
							.matchingEntities(matchingStats != null ? matchingStats.countEntities() : 0)
							.dateRange(matchingStats != null && matchingStats.spanEvents() != null
									   ? matchingStats.spanEvents().toSimpleRange()
									   : null);


		if (ce instanceof Concept<?> concept) {
			final boolean anyValidityDates = concept.getConnectors()
													.stream()
													.map(Connector::getValidityDates)
													.flatMap(Collection::stream)
													.findAny()
													.isEmpty();

			nodeBuilder = nodeBuilder.excludeFromTimeAggregation(concept.isDefaultExcludeFromTimeAggregation() || anyValidityDates);
		}

		if (ce.getChildren() != null) {
			nodeBuilder = nodeBuilder.children(ce.getChildren().stream().map(ConceptElement::getId).toArray(ConceptTreeChildId[]::new));
		}
		if (ce.getParent() != null) {
			nodeBuilder = nodeBuilder.parent(ce.getParent().getId());
		}

		return nodeBuilder.build();
	}

	public FrontendList createTreeMap(Concept<?> concept) {
		final FrontendList map = new FrontendList();
		fillTreeMap(concept, map);
		return map;
	}

	private void fillTreeMap(ConceptElement<?> ce, FrontendList map) {
		map.add(ce.getId(), createCTNode(ce));
		if (ce.getChildren() != null) {
			for (ConceptTreeChild c : ce.getChildren()) {
				fillTreeMap(c, map);
			}
		}
	}
}
