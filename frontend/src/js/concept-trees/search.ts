import type { ConceptIdT, ConceptT } from "../api/types";

export const doesQueryMatchNode = (node: ConceptT, query: string) => {
  return (
    node.label.toLowerCase().includes(query) ||
    (node.description && node.description.toLowerCase().includes(query)) ||
    (node.additionalInfos &&
      node.additionalInfos
        .map(({ value }) => value)
        .join("")
        .toLowerCase()
        .includes(query))
  );
};

/*
  This is a recursive algorithm to search through the trees
  It counts results and stores the count in "intermediateResult".

  The code might look a little bit "clumsy", but
  - it's been optimized to _not_ use object spread, because that slowed it down
  - it's been optimized to have a time complexity of O(n)

  trees: the structure that contains all data and allows to lookup children
  treeId: the root tree id that's being currently searched in
  nodeId: the id of the concept node, because node doesn't include it itself
  node: the current node to check for match,
    includes all information needed, eg: label, description, additionalInfos and children
  query: the search query
  intermediateResult: to avoid building new objects in every iteration, we carry
    this object through the recursion and define new properties as we go (side effects)
*/
export const findConcepts = (
  trees: Record<string, Record<string, ConceptT>>,
  treeId: string,
  nodeId: ConceptIdT,
  node: ConceptT,
  query: string,
  intermediateResult: { [key: ConceptIdT]: number } = {},
) => {
  // !node normall shouldn't happen.
  // It happens when window.conceptTrees doesn't contain a node
  // that was somewhere specified as a child.
  // TODO: Stronger contract with API on what shape of data is allowed
  if (!node) return intermediateResult;

  const isNodeIncluded = doesQueryMatchNode(node, query);

  // Early return if there are no children
  if (!node.children) {
    if (isNodeIncluded) {
      intermediateResult[nodeId] = 1;
    }

    return intermediateResult;
  }

  // Count node as 1 already, if it matches
  let sum = isNodeIncluded ? 1 : 0;

  for (let child of node.children) {
    const result = findConcepts(
      trees,
      treeId,
      child,
      trees[treeId][child],
      query,
      intermediateResult,
    );

    sum += result[child] || 0;
  }

  if (sum !== 0) {
    intermediateResult[nodeId] = sum;
  }

  // If sum === 0, node is left out from the result,
  // because it doesn't match itself and no child matches

  return intermediateResult;
};
