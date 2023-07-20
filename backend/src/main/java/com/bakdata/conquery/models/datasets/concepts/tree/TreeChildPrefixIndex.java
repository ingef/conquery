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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TreeChildPrefixIndex {

	@JsonIgnore
	private final BytesTTMap valueToChildIndex;
	@JsonIgnore
	private final ConceptTreeChild[] treeChildren;

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
		if (root.getChildIndex() != null) {
			return;
		}
		synchronized (root) {
			if (root.getChildIndex() != null) {
				return;
			}

			if (root.getChildren().isEmpty()) {
				return;
			}


			final BytesTTMap valueToChildIndex = new BytesTTMap();
			// collect all prefix children that are itself children of prefix nodes
			final Map<String, ConceptTreeChild> gatheredPrefixChildren = new HashMap<>();

			final Queue<ConceptTreeChild> treeChildrenOrig = new ArrayDeque<>(root.getChildren());
			ConceptTreeChild child;

			// Iterate over all children that can be reached deterministically with prefixes
			while ((child = treeChildrenOrig.poll()) != null) {
				final CTCondition condition = child.getCondition();

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
			final List<ConceptTreeChild> gatheredChildren = new ArrayList<>();

			for (Map.Entry<String, ConceptTreeChild> entry : gatheredPrefixChildren.entrySet()) {
				final String k = entry.getKey();
				final ConceptTreeChild value = entry.getValue();

				if (valueToChildIndex.put(k.getBytes(), gatheredChildren.size()) != -1) {
					log.error("Duplicate Prefix '{}' in '{}' of '{}'", k, value, root);
				}

				gatheredChildren.add(value);
			}

			valueToChildIndex.balance();

			final ConceptTreeChild[] treeChildren = gatheredChildren.toArray(new ConceptTreeChild[0]);

			log.trace("Index below {} contains {} nodes", root.getId(), treeChildren.length);

			if (treeChildren.length == 0) {
				return;
			}

			// add lookup only if it contains any elements
			root.setChildIndex(new TreeChildPrefixIndex(valueToChildIndex, treeChildren));
		}
	}

	public ConceptTreeChild findMostSpecificChild(String stringValue) {
		final ValueNode nearestNode = valueToChildIndex.getNearestNode(stringValue.getBytes());

		if (nearestNode != null) {
			return treeChildren[nearestNode.getValue()];
		}

		return null;
	}
}
