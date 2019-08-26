// @flow
import { includes } from "../common/helpers";
import type { ConceptT, TableT, SelectorT, ConceptIdT } from "../api/types";

import type { TreesT } from "./reducer";

import { tablesWithDefaults } from "../model/table";
import { selectsWithDefaults } from "../model/select";

// Globally store the huge (1-5 MB) trees for read only
// - keeps the redux store free from huge data
window.conceptTrees = {};

// To make this global variable a little more sane to use,
// we only use it with the following  getters / setters

//
// RESETTER
//
export function resetAllTrees() {
  window.conceptTrees = {};
}

//
// SETTER
//
export function setTree(
  rootConcept: ConceptIdT,
  treeId: ConceptIdT,
  tree: ConceptT
): void {
  // This replaces the root concept with the one loaded initially (at /concepts)
  const concepts = {
    ...tree,
    [treeId]: rootConcept
  };

  window.conceptTrees[treeId] = concepts;
}

//
// GETTER
//
export function getConceptById(conceptId?: ConceptIdT): ?ConceptT {
  const keys: ConceptIdT[] = Object.keys(window.conceptTrees);

  for (let i = 0; i < keys.length; i++) {
    const concept = window.conceptTrees[keys[i]][conceptId];

    if (concept) return concept;
  }

  return null;
}

//
// GETTER including parent tables all the way to the root concept
//
const findParentConcepts = (concepts: ConceptT[]): ConceptT[] => {
  // Get parent from first concept
  const parentId = concepts[0].parent;
  const parentConcept = getConceptById(parentId);

  if (!parentConcept) return concepts;

  const parentConceptWithId = {
    ...parentConcept,
    id: parentId
  };

  // Will return a list like
  // [rootConcept, childConcept, grandChildConcept, grandGrandChildConcept, ...]
  return findParentConcepts([parentConceptWithId, ...concepts]);
};

export const getConceptsByIdsWithTablesAndSelects = (
  conceptIds: ConceptIdT[],
  rootConcepts: TreesT
): ?{
  concepts: (ConceptT & { id: ConceptIdT })[],
  root: ConceptIdT,
  tables: TableT[],
  selects?: SelectorT[]
} => {
  const concepts = conceptIds
    .map(id => ({ concept: getConceptById(id), id }))
    .filter(({ concept }) => !!concept)
    .map(({ concept, id }) => ({ ...concept, id }));

  if (concepts.length !== conceptIds.length) return null;

  const parentConceptIds = findParentConcepts(concepts).map(
    c => c.id.toString() // toString so we can find them by object keys
  );

  const rootConceptId = Object.keys(rootConcepts).find(
    id => includes(parentConceptIds, id) && !!rootConcepts[id].tables
  );

  // There should only be one exact root node that has table information
  // If it's more or less than one, something went wrong
  if (!rootConceptId) {
    console.error("No root concept ID found");
    return null;
  }

  const rootConcept = rootConcepts[rootConceptId];

  const selects = rootConcept.selects
    ? { selects: selectsWithDefaults(rootConcept.selects) }
    : {};

  return {
    concepts,
    root: rootConceptId,
    tables: tablesWithDefaults(rootConcept.tables),
    ...selects
  };
};

export const hasConceptChildren = (node: ConceptT): boolean => {
  if (!node) return false;

  const concept = getConceptById(node.ids);

  return !!concept && !!concept.children && concept.children.length > 0;
};

/*
  This is async because ... we might want to parallelize this very soon,
  as there are up to 200k concepts that need to be searched.
*/
export const search = async (query: string) => {
  const lowerQuery = query.toLowerCase();

  const result = Object.keys(window.conceptTrees).reduce(
    (all, key) => ({
      ...all,
      ...findConcepts(key, key, window.conceptTrees[key][key], lowerQuery)
    }),
    {}
  );

  return result;
};

const doesQueryMatchNode = (node, query) => {
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

  treeId: the root tree id
  nodeId: the id of the concept node, because node doesn't include it itself
  node: the current node to check for match,
    includes all information needed, eg: label, description, additionalInfos and children
  query: the search query
  intermediateResult: to avoid building new objects in every iteration, we carry
    this object through the recursion and define new properties as we go (side effects)
*/
const findConcepts = (
  treeId: string,
  nodeId: ConceptIdT,
  node: ConceptT,
  query: string,
  intermediateResult: { [ConceptIdT]: number } = {}
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
      treeId,
      child,
      window.conceptTrees[treeId][child],
      query,
      intermediateResult
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
