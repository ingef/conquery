import { getConceptById } from "../concept-trees/globalTreeStoreHelper";

import type { ConceptQueryNodeType, StandardQueryNodeT } from "./types";

export function getRootNodeLabel(node: StandardQueryNodeT) {
  if (!node.ids || !node.tree) return null;

  const nodeIsRootNode = node.ids.indexOf(node.tree) !== -1;

  if (nodeIsRootNode) return null;

  const root = getConceptById(node.tree);

  return !!root ? root.label : null;
}

export function isLabelPristine(node: ConceptQueryNodeType) {
  if (node.ids.length === 0) {
    return false;
  }

  const storedConcept = getConceptById(node.ids[0]);

  return !!storedConcept && storedConcept.label === node.label;
}
