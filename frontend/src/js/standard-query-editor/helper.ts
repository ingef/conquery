import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { nodeIsConceptQueryNode } from "../model/node";

import type { ConceptQueryNodeType, StandardQueryNodeT } from "./types";

export function getRootNodeLabel(node: StandardQueryNodeT) {
  if (!nodeIsConceptQueryNode(node) || !node.ids || !node.tree) return null;

  const nodeIsRootNode = node.ids.includes(node.tree);
  const root = getConceptById(node.tree);

  if (nodeIsRootNode) {
    const noRootOrSameLabel = !root || root.label === node.label;

    return noRootOrSameLabel ? null : root.label;
  }

  return root ? root.label : null;
}

export function isLabelPristine(node: ConceptQueryNodeType) {
  if (node.ids.length === 0) {
    return false;
  }

  const storedConcept = getConceptById(node.ids[0]);

  return !!storedConcept && storedConcept.label === node.label;
}
