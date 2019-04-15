// @flow
import { includes, flatmap } from "../common/helpers";
import type {
  NodeType,
  TableType,
  TreeNodeIdType
} from "../common/types/backend";

import type { TreesType } from "./reducer";

// Globally store the huge (1-5 MB) trees for read only
// - keeps the redux store free from huge data
window.categoryTrees = {};

// To make this global variable a little more sane to use,
// we only use it with the following  getters / setters

//
// RESETTER
//
export function resetAllTrees() {
  window.categoryTrees = {};
}

//
// SETTER
//
export function setTree(
  rootConcept: TreeNodeIdType,
  treeId: TreeNodeIdType,
  tree: NodeType
): void {
  // This replaces the root concept with the one loaded initially (at /concepts)
  const concepts = {
    ...tree,
    [treeId]: rootConcept
  };

  window.categoryTrees[treeId] = concepts;
}

//
// GETTER
//
export function getConceptById(conceptId?: TreeNodeIdType): ?NodeType {
  const keys: TreeNodeIdType[] = Object.keys(window.categoryTrees);

  for (let i = 0; i < keys.length; i++) {
    const concept = window.categoryTrees[keys[i]][conceptId];

    if (concept) return concept;
  }

  return null;
}

//
// GETTER including parent tables all the way to the root concept
//
const findParentConcepts = (concepts: NodeType[]): NodeType[] => {
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
  conceptIds: TreeNodeIdType[],
  rootConcepts: TreesType
): ?{
  concepts: (NodeType & { id: TreeNodeIdType })[],
  root: TreeNodeIdType,
  tables: TableType[]
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
  if (!rootConceptId) return null;

  const rootConcept = rootConcepts[rootConceptId];

  const selects = rootConcept.selects ? { selects: rootConcept.selects } : {};

  return {
    concepts,
    root: rootConceptId,
    tables: rootConcept.tables,
    ...selects
  };
};

export const hasConceptChildren = node => {
  if (!node) return false;

  const concept = getConceptById(node.ids);

  return concept && concept.children && concept.children.length > 0;
};

// .reduce(
//         (allTree, conceptKey) => {
//           const concept = window.categoryTrees[key][conceptKey];
//           const results = findConcepts(key, concept, query);

//           // console.log("GOT RESULTS");

//           return {
//             ...allTree,
//             ...(concept.children
//               ? concept.children.reduce((sum, id) => sum + results[id], 0)
//               : { [conceptKey]: doesQueryMatchNode(concept, query) }),
//             ...results
//           };
//         },
//         {}

export const search = async (query: string) => {
  const result = Object.keys(window.categoryTrees).reduce(
    (all, key) => ({
      ...all,
      ...findConcepts(key, key, window.categoryTrees[key][key], query)
    }),
    {}
  );

  return Promise.resolve({
    size: Object.keys(result).length,
    limit: 500,
    result
  });
};

const findConcepts = (treeId, nodeId, node, query) => {
  const isNodeIncluded = doesQueryMatchNode(node, query);

  const childrenResults = node.children
    ? node.children.reduce(
        (all, child) => ({
          ...all,
          ...findConcepts(
            treeId,
            child,
            window.categoryTrees[treeId][child],
            query
          )
        }),
        {}
      )
    : {};

  if (!node.children) return isNodeIncluded ? { [nodeId]: 1 } : {};

  childrenResults[nodeId] = 0;

  for (let child of node.children) {
    childrenResults[nodeId] += childrenResults[child] || 0;
  }

  if (childrenResults[nodeId] === 0) return {};

  return childrenResults;
};

const doesQueryMatchNode = (node, query) => {
  const lowerQuery = query.toLowerCase();

  return (
    node.label.toLowerCase().includes(lowerQuery) ||
    (node.description && node.description.toLowerCase().includes(lowerQuery)) ||
    (node.additionalInfos &&
      node.additionalInfos
        .map(({ value }) => value)
        .join("")
        .toLowerCase()
        .includes(lowerQuery))
  );
};

// const findConcepts = (concepts, query) => {
//   const matches = Object.keys(concepts)
//     .map(key => ({
//       id: key,
//       label: concepts[key].label,
//       description: concepts[key].description,
//       additionalInfos: concepts[key].additionalInfos
//     }))
//     .filter(co => {
//       return doesQueryMatchNode(co, query);
//     })
//     .map(({ id }) => id);

//   return [...new Set(fetchParents(concepts, matches))];
// };

// const fetchParents = (concepts, matches) => {
//   for (var ma in matches) {
//     // Updates matches as a side-effect
//     visitParentOf(matches[ma], concepts, matches);
//   }

//   return matches;
// };

// const visitParentOf = (id, concepts, matches) => {
//   const concept = concepts[id];

//   if (concept && concept.parent) {
//     matches.push(concept.parent);

//     return visitParentOf(concept.parent, concepts, matches);
//   }
// };
