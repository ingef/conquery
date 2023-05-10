package com.bakdata.conquery.models.datasets.concepts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendList;
import com.bakdata.conquery.apiv1.frontend.FrontendNode;
import com.bakdata.conquery.apiv1.frontend.FrontendRoot;
import com.bakdata.conquery.apiv1.frontend.FrontendSecondaryId;
import com.bakdata.conquery.apiv1.frontend.FrontendSelect;
import com.bakdata.conquery.apiv1.frontend.FrontendTable;
import com.bakdata.conquery.apiv1.frontend.FrontendValidityDate;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.StructureNodeId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class constructs the concept tree as it is presented to the front end.
 */
@AllArgsConstructor
@Slf4j
public class FrontEndConceptBuilder {

	public static FrontendRoot createRoot(NamespaceStorage storage, Subject subject) {

		final FrontendRoot root = new FrontendRoot();
		final Map<Id<?>, FrontendNode> roots = root.getConcepts();

		final List<? extends Concept<?>> allConcepts = new ArrayList<>(storage.getAllConcepts());
		// Remove any hidden concepts
		allConcepts.removeIf(Concept::isHidden);

		if (allConcepts.isEmpty()) {
			log.warn("There are no displayable concepts in the dataset {}", storage.getDataset().getId());
		}

		// Submit all permissions to Shiro
		final boolean[] isPermitted = subject.isPermitted(allConcepts, Ability.READ);

		for (int i = 0; i < allConcepts.size(); i++) {
			if (!isPermitted[i]) {
				continue;
			}

			roots.put(allConcepts.get(i).getId(), createConceptRoot(allConcepts.get(i), storage.getStructure()));
		}
		if (roots.isEmpty()) {
			log.warn("No concepts could be collected for {} on dataset {}. The subject is possibly lacking the permission to use them.", subject.getId(), storage.getDataset()
																																								 .getId());
		}
		else {
			log.trace("Collected {} concepts for {} on dataset {}.", roots.size(), subject.getId(), storage.getDataset().getId());
		}
		//add the structure tree
		for (StructureNode sn : storage.getStructure()) {
			final FrontendNode node = createStructureNode(sn, roots);
			if (node == null) {
				log.trace("Did not create a structure node entry for {}. Contained no concepts.", sn.getId());
				continue;
			}
			roots.put(sn.getId(), node);
		}
		//add all secondary IDs
		root.getSecondaryIds()
			.addAll(storage.getSecondaryIds()
						   .stream()
						   .filter(sid -> !sid.isHidden())
						   .map(sid -> new FrontendSecondaryId(sid.getId().toString(), sid.getLabel(), sid.getDescription()))
						   .collect(Collectors.toSet()));

		return root;
	}

	private static FrontendNode createConceptRoot(Concept<?> concept, StructureNode[] structureNodes) {

		final MatchingStats matchingStats = concept.getMatchingStats();

		final StructureNodeId
				structureParent =
				Arrays.stream(structureNodes).filter(sn -> sn.getContainedRoots().contains(concept.getId())).findAny().map(StructureNode::getId).orElse(null);

		final FrontendNode node =
				FrontendNode.builder()
							.active(true)
							.description(concept.getDescription())
							.label(concept.getLabel())
							.additionalInfos(concept.getAdditionalInfos())
							.matchingEntries(matchingStats != null ? matchingStats.countEvents() : 0)
							.matchingEntities(matchingStats != null ? matchingStats.countEntities() : 0)
							.dateRange(matchingStats != null && matchingStats.spanEvents() != null ? matchingStats.spanEvents().toSimpleRange() : null)
							.detailsAvailable(Boolean.TRUE)
							.codeListResolvable(concept.countElements() > 1)
							.parent(structureParent)
							.excludeFromTimeAggregation(concept.isDefaultExcludeFromTimeAggregation() || concept.getConnectors()
																												.stream()
																												.map(Connector::getValidityDates)
																												.flatMap(Collection::stream)
																												.findAny()
																												.isEmpty())
							.selects(concept.getSelects().stream().map(FrontEndConceptBuilder::createSelect).collect(Collectors.toList()))
							.tables(concept.getConnectors().stream().map(FrontEndConceptBuilder::createTable).collect(Collectors.toList()))
							.build();

		if (concept instanceof ConceptTreeNode<?> tree && tree.getChildren() != null) {
			node.setChildren(tree.getChildren().stream().map(ConceptTreeChild::getId).toArray(ConceptTreeChildId[]::new));
		}
		return node;
	}

	@Nullable
	private static FrontendNode createStructureNode(StructureNode structureNode, Map<Id<?>, FrontendNode> roots) {
		final List<ConceptId> unstructured = new ArrayList<>();
		for (ConceptId id : structureNode.getContainedRoots()) {
			if (!roots.containsKey(id)) {
				log.trace("Concept from structure node can not be found: {}", id);
				continue;
			}
			unstructured.add(id);
		}

		if (unstructured.isEmpty()) {
			return null;
		}

		return FrontendNode.builder()
						   .active(false)
						   .description(structureNode.getDescription())
						   .label(structureNode.getLabel())
						   .detailsAvailable(Boolean.FALSE)
						   .codeListResolvable(false)
						   .additionalInfos(structureNode.getAdditionalInfos())
						   .parent(structureNode.getParent() == null ? null : structureNode.getParent().getId())
						   .children(Stream.concat(structureNode.getChildren().stream().map(IdentifiableImpl::getId), unstructured.stream()).toArray(Id[]::new))
						   .build();
	}

	public static FrontendSelect createSelect(Select select) {
		return FrontendSelect.builder()
							 .id(select.getId())
							 .label(select.getLabel())
							 .description(select.getDescription())
							 .resultType(select.getResultType())
							 .isDefault(select.isDefault())
							 .build();
	}

	public static FrontendTable createTable(Connector con) {
		final FrontendTable
				result =
				FrontendTable.builder()
							 .id(con.getTable().getId())
							 .connectorId(con.getId())
							 .label(con.getLabel())
							 .isDefault(con.isDefault())
							 .filters(con.collectAllFilters().stream().map(FrontEndConceptBuilder::createFilter).collect(Collectors.toList()))
							 .selects(con.getSelects().stream().map(FrontEndConceptBuilder::createSelect).collect(Collectors.toList()))
							 .supportedSecondaryIds(Arrays.stream(con.getTable().getColumns())
														  .map(Column::getSecondaryId)
														  .filter(Objects::nonNull)
														  .map(Identifiable::getId)
														  .collect(Collectors.toSet()))
							 .build();

		if (con.getValidityDates().size() > 1) {
			result.setDateColumn(new FrontendValidityDate(con.getValidityDatesDescription(), null, con.getValidityDates()
																									  .stream()
																									  .map(vd -> new FrontendValue(vd.getId()
																																	 .toString(), vd.getLabel()))
																									  .collect(Collectors.toList())));

			if (!result.getDateColumn().getOptions().isEmpty()) {
				result.getDateColumn().setDefaultValue(result.getDateColumn().getOptions().get(0).getValue());
			}
		}

		return result;
	}

	public static FrontendFilterConfiguration.Top createFilter(Filter<?> filter) {
		try {
			return filter.createFrontendConfig();
		}
		catch (ConceptConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static FrontendNode createCTNode(ConceptElement<?> ce) {
		final MatchingStats matchingStats = ce.getMatchingStats();
		FrontendNode.FrontendNodeBuilder nodeBuilder = FrontendNode.builder()
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


		final FrontendNode n = nodeBuilder.build();


		if (ce instanceof ConceptTreeNode<?> tree) {
			if (tree.getChildren() != null) {
				n.setChildren(tree.getChildren().stream().map(IdentifiableImpl::getId).toArray(ConceptTreeChildId[]::new));
			}
			if (tree.getParent() != null) {
				n.setParent(tree.getParent().getId());
			}
		}
		return n;
	}

	public static FrontendList createTreeMap(Concept<?> concept) {
		final FrontendList map = new FrontendList();
		fillTreeMap(concept, map);
		return map;
	}

	private static void fillTreeMap(ConceptElement<?> ce, FrontendList map) {
		map.add(ce.getId(), createCTNode(ce));
		if (ce instanceof ConceptTreeNode && ((ConceptTreeNode<?>) ce).getChildren() != null) {
			for (ConceptTreeChild c : ((ConceptTreeNode<?>) ce).getChildren()) {
				fillTreeMap(c, map);
			}
		}
	}
}
