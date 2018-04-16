// @flow
import SearchApi, { INDEX_MODES }         from 'js-worker-search'

import { includes }                       from '../common/helpers';
import type {
  NodeType,
  TableType,
  TreeNodeIdType
}                                         from '../common/types/backend';

import type { TreesType }                 from './reducer';

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
};

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
    [treeId]: rootConcept,
  };

  window.categoryTrees[treeId] = concepts;
};

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
}

export const getConceptsByIdsWithTables = (
  conceptIds: TreeNodeIdType[],
  rootConcepts: TreesType
) : ?{
  concepts: (NodeType & {id: TreeNodeIdType})[],
  root: TreeNodeIdType,
  tables: TableType[]
} => {
  const concepts = conceptIds.map(c => {
    const concept = getConceptById(c);

    return concept !== null ? {...concept, id: c} : null;
  }).filter(c => !!c);

  if (concepts.length !== conceptIds.length) return null;

  const parentConceptIds = findParentConcepts(concepts).map(
    c => c.id.toString() // toString so we can find them by object keys
  );

  const parentConceptsWithTables = Object.keys(rootConcepts)
    .filter(id =>
      includes(parentConceptIds, id) &&
      !!rootConcepts[id].tables
    )
    .map(id => ({id, concept: rootConcepts[id]}));

  // There should only be one exact root node that has table information
  // If it's more or less than one, something went wrong
  if (parentConceptsWithTables.length !== 1) return null;

  return {
    concepts,
    root: parentConceptsWithTables[0].id,
    tables: parentConceptsWithTables[0].concept.tables
  };
}

export const SEARCH_API = new SearchApi({ indexMode: INDEX_MODES.PREFIXES })

export const createTreeSearchIndex = (tree: any) => {
  Object.keys(tree).forEach(key => {
    const node = tree[key];
    const label = node.label || '';
    const description = node.description || '';
    const additionalInfos = node.additionalInfos
      ? node.additionalInfos.map(({key, value}) => `${key} ${value}`).join('')
      : '';
    SEARCH_API.indexDocument(key, label);
    SEARCH_API.indexDocument(key, description);
    SEARCH_API.indexDocument(key, additionalInfos);
  })
}
