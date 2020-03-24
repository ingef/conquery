import { includes } from "../common/helpers";
import type { ConceptT, TableT, SelectorT, ConceptIdT } from "../api/types";

import type { TreesT } from "./reducer";

import { tablesWithDefaults } from "../model/table";
import { selectsWithDefaults } from "../model/select";

import { findConcepts } from "./search";

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
export function getConceptById(conceptId?: ConceptIdT): ConceptT | null {
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

interface ConceptsByIds {
  concepts: (ConceptT & { id: ConceptIdT })[],
  root: ConceptIdT,
  tables: TableT[],
  selects?: SelectorT[]
}

export const getConceptsByIdsWithTablesAndSelects = (
  conceptIds: ConceptIdT[],
  rootConcepts: TreesT
): ConceptsByIds | null => {
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
export const globalSearch = async (trees: TreesT, query: string) => {
  const lowerQuery = query.toLowerCase();
  // `trees contains concept folders as well,
  // but they're not saved globally in window.conceptTrees, because they're not many
  //
  // We DO want to search in concept folders as well, so we'll
  // - format them to have the same "nested" structure as a single conceptTree
  // - we combine both into one object
  //
  // TODO: Refactor the state and keep both root trees as well as concept trees in a single format
  //       Then simply use that here
  const formattedTrees = Object.keys(trees).reduce((all, key) => {
    all[key] = { [key]: trees[key] };

    return all;
  }, {});
  const combinedTrees = Object.assign({}, formattedTrees, window.conceptTrees);

  const result = Object.keys(combinedTrees)
    .filter(key => !combinedTrees[key].parent)
    .reduce(
      (all, key) => ({
        ...all,
        ...findConcepts(
          combinedTrees,
          key,
          key,
          combinedTrees[key][key],
          lowerQuery
        )
      }),
      {}
    );

  return result;
};
