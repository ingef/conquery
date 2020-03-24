import { getConceptById } from "../concept-trees/globalTreeStoreHelper";

import type { QueryNodeType } from "./types";

export function getRootNodeLabel(node: QueryNodeType) {
  if (!node.ids || !node.tree) return null;

  const nodeIsRootNode = node.ids.indexOf(node.tree) !== -1;

  if (nodeIsRootNode) return null;

  const root = getConceptById(node.tree);

  return !!root ? root.label : null;
}
