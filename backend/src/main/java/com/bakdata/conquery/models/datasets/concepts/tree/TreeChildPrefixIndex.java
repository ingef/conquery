package com.bakdata.conquery.models.datasets.concepts.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.OrCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.PrefixCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.PrefixRangeCondition;
import com.bakdata.conquery.util.dict.BytesTTMap;
import com.bakdata.conquery.util.dict.ValueNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TreeChildPrefixIndex {

	@JsonIgnore
	private BytesTTMap valueToChildIndex = new BytesTTMap();
	@JsonIgnore
	private ConceptTreeChild[] treeChildren;

	public ConceptTreeChild findMostSpecificChild(String stringValue) {
		ValueNode nearestNode = valueToChildIndex.getNearestNode(stringValue.getBytes());

		if(nearestNode != null) {
			return treeChildren[nearestNode.getValue()];
		}

		return null;
	}

	/***
	 * Test if the condition maintains a prefix structure, this is necessary for creating an index.
	 * @param cond
	 * @return
	 */
	private static boolean isPrefixMaintainigCondition(CTCondition cond) {
		return cond instanceof PrefixCondition
				|| cond instanceof PrefixRangeCondition
				|| (cond instanceof OrCondition
					&& ((OrCondition) cond).getConditions().stream().allMatch(TreeChildPrefixIndex::isPrefixMaintainigCondition))
				;
	}

	public static void putIndexInto(ConceptTreeNode<?> root) {
		if(root.getChildIndex() != null) {
			return;
		}
		synchronized (root) {
			if(root.getChildIndex() != null) {
				return;
			}
			
			if(root.getChildren().isEmpty()) {
				return;
			}

			TreeChildPrefixIndex index = new TreeChildPrefixIndex();

			// collect all prefix children that are itself children of prefix nodes
			Map<String, ConceptTreeChild> gatheredPrefixChildren = new HashMap<>();

			Queue<ConceptTreeChild> treeChildrenOrig = new ArrayDeque<>(root.getChildren());
			ConceptTreeChild child;

			// Iterate over all children that can be reached deterministically with prefixes
			while ((child = treeChildrenOrig.poll()) != null) {
				CTCondition condition = child.getCondition();

				// If the Condition is not deterministic wrt to prefixes, we will not build an index over it, but start a new one from there.
				if (!isPrefixMaintainigCondition(condition)) {
					putIndexInto(child);
					continue;
				}

				treeChildrenOrig.addAll(child.getChildren());

				if (!(condition instanceof PrefixCondition)) {
					continue;
				}

				for (String prefix : ((PrefixCondition) condition).getPrefixes()) {
					// We are interested in the most specific child, therefore the deepest.
					gatheredPrefixChildren.merge(prefix, child,
						(newChild, oldChild) -> oldChild.getDepth() > newChild.getDepth() ? oldChild : newChild
					);
				}
			}

			// Insert children into index and build resolving list
			List<ConceptTreeChild> gatheredChildren = new ArrayList<>();

			for (Map.Entry<String, ConceptTreeChild> entry : gatheredPrefixChildren.entrySet()) {
				String k = entry.getKey();
				ConceptTreeChild value = entry.getValue();
				if (index.valueToChildIndex.put(k.getBytes(), gatheredChildren.size()) != -1) {
					log.error("Duplicate Prefix '{}' in '{}' of '{}'", k, value, root);
				}

				gatheredChildren.add(value);
			}

			index.valueToChildIndex.balance();

			index.treeChildren = gatheredChildren.toArray(new ConceptTreeChild[0]);

			log.trace("Index below {} contains {} nodes", root.getId(), index.treeChildren.length);

			if(index.treeChildren.length == 0) {
				return;
			}

			// add lookup only if it contains any elements
			root.setChildIndex(index);
		}
	}
}
