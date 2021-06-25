package com.bakdata.conquery.models.datasets.concepts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEList;
import com.bakdata.conquery.apiv1.frontend.FENode;
import com.bakdata.conquery.apiv1.frontend.FERoot;
import com.bakdata.conquery.apiv1.frontend.FESecondaryId;
import com.bakdata.conquery.apiv1.frontend.FESelect;
import com.bakdata.conquery.apiv1.frontend.FETable;
import com.bakdata.conquery.apiv1.frontend.FEValidityDate;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.StructureNodeId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class constructs the concept tree as it is presented to the front end.
 */
@AllArgsConstructor
@Slf4j
public class FrontEndConceptBuilder {

	public static FERoot createRoot(NamespaceStorage storage, User user) {

		FERoot root = new FERoot();
		Map<IId<?>, FENode> roots = root.getConcepts();
		
		List<? extends Concept<?>> allConcepts = new ArrayList<>(storage.getAllConcepts());
		// Remove any hidden concepts
		allConcepts.removeIf(Concept::isHidden);
		
		if(allConcepts.isEmpty()) {
			log.warn("There are no displayable concepts in the dataset {}", storage.getDataset().getId());
		}

		// Submit all permissions to Shiro
		boolean[] isPermitted = user.isPermitted(allConcepts, Ability.READ);
		
		for (int i = 0; i<allConcepts.size(); i++) {
			if(isPermitted[i]) {
				roots.put(allConcepts.get(i).getId(), createCTRoot(allConcepts.get(i), storage.getStructure()));
			}
		}
		if(roots.isEmpty()) {
			log.warn("No concepts could be collected for {} on dataset {}. The user is possibly lacking the permission to use them.", user.getId(), storage.getDataset().getId());
		} else {
			log.trace("Collected {} concepts for {} on dataset {}.", roots.size(), user.getId(), storage.getDataset().getId());
		}
		//add the structure tree
		for(StructureNode sn : storage.getStructure()) {
			FENode node = createStructureNode(sn, roots);
			if(node == null) {
				log.trace("Did not create a structure node entry for {}. Contained no concepts.", sn.getId());
				continue;
			}
			roots.put(sn.getId(), node);
		}
		//add all secondary IDs
		root.getSecondaryIds()
			.addAll(
					storage.getSecondaryIds().stream()
						   .map(sid -> new FESecondaryId(sid.getId().toString(), sid.getLabel(), sid.getDescription()))
						   .collect(Collectors.toSet())
			);

		return root;
	}

	private static FENode createCTRoot(Concept<?> c, StructureNode[] structureNodes) {

		MatchingStats matchingStats = c.getMatchingStats();

		StructureNodeId structureParent = Arrays
			.stream(structureNodes)
			.filter(sn->sn.getContainedRoots().contains(c.getId()))
			.findAny()
			.map(StructureNode::getId)
			.orElse(null);

		FENode n = FENode.builder()
				.active(true)
				.description(c.getDescription())
				.label(c.getLabel())
				.additionalInfos(c.getAdditionalInfos())
				.matchingEntries(matchingStats.countEvents())
				.dateRange(matchingStats.spanEvents() != null ? matchingStats.spanEvents().toSimpleRange() : null)
				.detailsAvailable(Boolean.TRUE)
				.codeListResolvable(c.countElements() > 1)
				.parent(structureParent)
				.selects(c
					.getSelects()
					.stream()
					.map(FrontEndConceptBuilder::createSelect)
					.collect(Collectors.toList())
				)
				.tables(c
						.getConnectors()
						.stream()
						.map(FrontEndConceptBuilder::createTable)
						.collect(Collectors.toList())
				)
				.build();
		
		if(c instanceof ConceptTreeNode) {
			ConceptTreeNode<?> tree = (ConceptTreeNode<?>)c;
			if(tree.getChildren()!=null) {
				n.setChildren(
					tree
						.getChildren()
						.stream()
						.map(ConceptTreeChild::getId)
						.toArray(ConceptTreeChildId[]::new)
				);
			}
		}
		return n;
	}

	@Nullable
	private static FENode createStructureNode(StructureNode cn, Map<IId<?>, FENode> roots) {
		List<ConceptId> unstructured = new ArrayList<>();
		for(ConceptId id : cn.getContainedRoots()) {
			if(!roots.containsKey(id)) {
				log.trace("Concept from structure node can not be found: {}", id);
				continue;
			}
			unstructured.add(id);
		}
		
		if(unstructured.isEmpty()) {
			return null;
		}
		
		return FENode.builder()
			.active(false)
			.description(cn.getDescription())
			.label(cn.getLabel())
			.detailsAvailable(Boolean.FALSE)
			.codeListResolvable(false)
			.additionalInfos(cn.getAdditionalInfos())
			.parent(cn.getParent() == null ? null : cn.getParent().getId())
			.children(
				ArrayUtils.addAll(
					cn.getChildren().stream()
						.map(IdentifiableImpl::getId)
						.toArray(IId[]::new),
						unstructured.toArray(IId[]::new)
				)
			)
			.build();
	}

	private static FENode createCTNode(ConceptElement<?> ce) {
		MatchingStats matchingStats = ce.getMatchingStats();
		FENode n = FENode.builder()
				.active(null)
				.description(ce.getDescription())
				.label(ce.getLabel())
				.additionalInfos(ce.getAdditionalInfos())
				.matchingEntries(matchingStats.countEvents())
				.dateRange(matchingStats.spanEvents() != null ? matchingStats.spanEvents().toSimpleRange() : null)
				.build();
		
		if(ce instanceof ConceptTreeNode) {
			ConceptTreeNode<?> tree = (ConceptTreeNode<?>)ce;
			if(tree.getChildren()!=null) {
				n.setChildren(tree.getChildren().stream().map(IdentifiableImpl::getId).toArray(ConceptTreeChildId[]::new));
			}
			if (tree.getParent() != null) {
				n.setParent(tree.getParent().getId());
			}
		}
		return n;
	}

	public static FETable createTable(Connector con) {
		FETable result =
				FETable.builder()
					   .id(con.getTable().getId())
					   .connectorId(con.getId())
					   .label(con.getLabel())
					   .filters(
							   con.collectAllFilters()
								  .stream()
								  .map(FrontEndConceptBuilder::createFilter)
								  .collect(Collectors.toList())
					   )
					   .selects(
							   con.getSelects()
								  .stream()
								  .map(FrontEndConceptBuilder::createSelect)
								  .collect(Collectors.toList())
					   )
					   .supportedSecondaryIds(
							   Arrays.stream(con.getTable().getColumns())
									 .map(Column::getSecondaryId)
									 .filter(Objects::nonNull)
									 .map(Identifiable::getId)
									 .collect(Collectors.toSet())
					   )
					   .build();
		
		if(con.getValidityDates().size() > 1) {
			result.setDateColumn(
				new FEValidityDate(
					null,
						con
						.getValidityDates()
						.stream()
							.map(vd -> new FEValue(vd.getLabel(), vd.getId().toString()))
							.collect(Collectors.toList())
				)
			);
			
			if(!result.getDateColumn().getOptions().isEmpty()) {
				result.getDateColumn().setDefaultValue(result.getDateColumn().getOptions().get(0).getValue());
			}
		}
		
		return result;
	}

	public static FEFilter createFilter(Filter<?> filter) {
		FEFilter f = FEFilter.builder()
							 .id(filter.getId())
							 .label(filter.getLabel())
							 .description(filter.getDescription())
							 .unit(filter.getUnit())
							 .allowDropFile(filter.getAllowDropFile())
							 .pattern(filter.getPattern())
							 .build();
		try {
			filter.configureFrontend(f);
			return f;
		}
		catch (ConceptConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

	public static FESelect createSelect(Select select) {
		return FESelect
					.builder()
					.id(select.getId())
					.label(select.getLabel())
					.description(select.getDescription())
					.resultType(select.getResultType())
					.build();
	}

	public static FEList createTreeMap(Concept<?> concept) {
		FEList map = new FEList();
		fillTreeMap(concept, map);
		return map;
	}

	private static void fillTreeMap(ConceptElement<?> ce, FEList map) {
		map.add(ce.getId(), createCTNode(ce));
		if (ce instanceof ConceptTreeNode && ((ConceptTreeNode<?>) ce).getChildren() != null) {
			for (ConceptTreeChild c : ((ConceptTreeNode<?>) ce).getChildren()) {
				fillTreeMap(c, map);
			}
		}
	}
}
