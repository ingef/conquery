import type { ConceptIdT, ConceptT, GetConceptResponseT } from "../api/types";
import { exists } from "../common/helpers/exists";
import { NodeResetConfig, nodeIsElement } from "../model/node";
import { resetSelects } from "../model/select";
import { resetTables } from "../model/table";
import type {
  DragItemConceptTreeNode,
  SelectedSelectorT,
  TableWithFilterValueT,
} from "../standard-query-editor/types";

import type { TreesT } from "./reducer";
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
  rootConcept: ConceptT,
  treeId: ConceptIdT,
  tree: GetConceptResponseT,
): void {
  // This replaces the root concept with the one loaded initially (at /concepts)
  const concepts: Record<string, ConceptT> = {
    ...tree,
    [treeId]: rootConcept,
  };

  window.conceptTrees[treeId] = concepts;
}

//
// GETTER
//
export function getConceptById(
  conceptId: ConceptIdT,
  rootId?: ConceptIdT,
): ConceptT | undefined {
  if (rootId) {
    return window.conceptTrees[rootId]?.[conceptId];
  }

  const conceptTreeRootIds: ConceptIdT[] = Object.keys(window.conceptTrees);

  for (const rootId of conceptTreeRootIds) {
    const concept = window.conceptTrees[rootId][conceptId];

    if (concept) return concept;
  }

  return undefined;
}

//
// GETTER including parent tables all the way to the root concept
//
const findParentConcepts = (
  concepts: { concept: ConceptT; id: ConceptIdT }[],
): { concept: ConceptT; id: ConceptIdT }[] => {
  const firstConcept = concepts[0].concept;
  const parentId = nodeIsElement(firstConcept) ? firstConcept.parent : null;

  if (!parentId) return concepts;

  const parentConcept = getConceptById(parentId);

  if (!parentConcept) return concepts;

  // Will return a list like
  // [rootConcept, childConcept, grandChildConcept, grandGrandChildConcept, ...]
  return findParentConcepts([
    { concept: parentConcept, id: parentId },
    ...concepts,
  ]);
};

const findRootConceptIdFromConceptIds = (
  rootConcepts: TreesT,
  conceptIds: ConceptIdT[],
) => {
  // Note: The implicit assumption is that all of the passed conceptIds have the same root.
  // because otherwise they wouldn't have landed in the node.ids array in the first place
  const conceptsWithIds: {
    concept: ConceptT;
    id: ConceptIdT;
  }[] = conceptIds
    .map((id) => ({ id, concept: getConceptById(id) }))
    .filter((d): d is { concept: ConceptT; id: string } => !!d.concept);

  if (conceptsWithIds.length !== conceptIds.length) return null;

  const parentConceptIds = findParentConcepts(conceptsWithIds).map(
    (c) => c.id.toString(), // toString so we can find them by object keys
  );

  return Object.keys(rootConcepts).find(
    (id) => parentConceptIds.includes(id) && nodeIsElement(rootConcepts[id]),
  );
};

interface ConceptsByIds {
  concepts: ConceptT[];
  root: ConceptIdT;
  tables: TableWithFilterValueT[];
  selects?: SelectedSelectorT[];
}

export const getConceptsByIdsWithTablesAndSelects = (
  rootConcepts: TreesT,
  conceptIds: ConceptIdT[],
  resetConfig: NodeResetConfig,
): ConceptsByIds | null => {
  const rootConceptId = findRootConceptIdFromConceptIds(
    rootConcepts,
    conceptIds,
  );

  // There should only be one exact root node that has table information
  // If it's more or less than one, something went wrong
  if (!rootConceptId) {
    console.error(`No root concept ID found for ${conceptIds}`);
    return null;
  }

  const rootConcept = rootConcepts[rootConceptId];

  if (!nodeIsElement(rootConcept)) {
    console.error(`Only struct root concept ID found for ${conceptIds}`);
    return null;
  }

  const selects = rootConcept.selects
    ? { selects: resetSelects(rootConcept.selects, resetConfig) }
    : {};

  return {
    concepts: conceptIds.map((id) => getConceptById(id)).filter(exists),
    root: rootConceptId,
    tables: rootConcept.tables
      ? resetTables(rootConcept.tables, resetConfig)
      : [],
    ...selects,
  };
};

export const hasConceptChildren = (node: DragItemConceptTreeNode): boolean => {
  if (!node) return false;

  const concept = getConceptById(node.ids[0]);

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
  const formattedTrees = Object.keys(trees).reduce<
    Record<string, Record<string, ConceptT>>
  >((all, key) => {
    all[key] = { [key]: trees[key] };

    return all;
  }, {});
  const combinedTrees = Object.assign({}, formattedTrees, window.conceptTrees);

  const result = Object.keys(combinedTrees)
    .filter((key) => !combinedTrees[key].parent)
    .reduce<Record<ConceptIdT, number>>(
      (all, key) => ({
        ...all,
        ...findConcepts(
          combinedTrees,
          key,
          key,
          combinedTrees[key][key],
          lowerQuery,
        ),
      }),
      {},
    );

  return result;
};
