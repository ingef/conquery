package com.bakdata.conquery.models.concepts;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FENode;
import com.bakdata.conquery.models.api.description.FERoot;
import com.bakdata.conquery.models.api.description.FESelect;
import com.bakdata.conquery.models.api.description.FETable;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConceptConnector;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.StructureNodeId;

import lombok.AllArgsConstructor;

/**
 * This class constructs the concept tree as it is presented to the front end.
 */
@AllArgsConstructor
public class FrontEndConceptBuilder {

	public static FERoot createRoot(NamespaceStorage storage) {

		FERoot root = new FERoot();

		Map<IId<?>, FENode> roots = new LinkedHashMap<>();
		//add all real roots
		for (Concept<?> c : storage.getAllConcepts()) {
			if(!c.isHidden()) {
				roots.put(c.getId(), createCTRoot(c, storage.getStructure()));
			}
		}
		//add the structure tree
		for(StructureNode sn : storage.getStructure()) {
			roots.put(sn.getId(), createStructureNode(sn));
		}
		root.setConcepts(roots);
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
				.active(c instanceof VirtualConcept)
				.description(c.getDescription())
				.label(c.getLabel())
				.additionalInfos(c.getAdditionalInfos())
				.matchingEntries(matchingStats.countEvents())
				.dateRange(matchingStats.spanEvents() != null ? matchingStats.spanEvents().toSimpleRange() : null)
				.detailsAvailable(Boolean.TRUE)
				.codeListResolvable(
					c instanceof TreeConcept
					|| (
							c instanceof VirtualConcept
							&& ((VirtualConcept)c).getConnectors()
								.stream()
								.map(VirtualConceptConnector::getFilter)
								.anyMatch(AbstractSelectFilter.class::isInstance)
					)
				)
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

	private static FENode createStructureNode(StructureNode cn) {
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
					cn.getContainedRoots().stream()
						.toArray(IId[]::new)
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
		return FETable.builder()
			.id(con.getTable().getId())
			.connectorId(con.getId())
			.label(con.getLabel())
			.filters(con
				.collectAllFilters()
				.stream()
				.map(FrontEndConceptBuilder::createFilter)
				.collect(Collectors.toList())
			)
			.selects(con
				.getSelects()
				.stream()
				.map(FrontEndConceptBuilder::createSelect)
				.collect(Collectors.toList())
			).build();
	}

	public static FESelect createFilter(Select select) {
		return FESelect.builder()
			.label(select.getLabel())
			.id(select.getId())
			.description(select.getDescription())
			.build();
	}
	
	public static FEFilter createFilter(Filter<?> filter) {
		FEFilter f = FEFilter.builder()
			.id(filter.getId())
			.label(filter.getLabel())
			.description(filter.getDescription())
			.unit(filter.getUnit())
			.allowDropFile(filter.getAllowDropFile())
			.pattern(filter.getPattern())
			.template(filter.getTemplate())
			.build();
		try {
			filter.configureFrontend(f);
		}
		catch (ConceptConfigurationException e) {
			throw new IllegalStateException(e);
		}
		return f;
	}

	public static FESelect createSelect(Select select) {
		return FESelect
					.builder()
					.id(select.getId())
					.label(select.getLabel())
					.description(select.getDescription())
					.build();
	}

	public static Map<ConceptId, Map<ConceptElementId<?>, FENode>> createTreeMap(List<Concept<?>> concepts) {
		Map<ConceptId, Map<ConceptElementId<?>, FENode>> rootedMap = new LinkedHashMap<>();

		for (Concept<?> c : concepts) {
			Map<ConceptElementId<?>, FENode> map = new LinkedHashMap<>();
			rootedMap.put(c.getId(), map);
			fillTreeMap(c, map);
		}
		return rootedMap;
	}

	private static void fillTreeMap(ConceptElement<?> ce, Map<ConceptElementId<?>, FENode> map) {
		map.put(ce.getId(), createCTNode(ce));
		if (ce instanceof ConceptTreeNode && ((ConceptTreeNode<?>) ce).getChildren() != null) {
			for (ConceptTreeChild c : ((ConceptTreeNode<?>) ce).getChildren()) {
				fillTreeMap(c, map);
			}
		}
	}
}
