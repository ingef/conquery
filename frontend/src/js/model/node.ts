import type { ConceptQueryNodeType } from "../standard-query-editor/types";

import { tablesHaveActiveFilter } from "./table";
import { objectHasSelectedSelects } from "./select";

export const nodeHasActiveFilters = (node: ConceptQueryNodeType) =>
  node.excludeTimestamps ||
  node.includeSubnodes ||
  objectHasSelectedSelects(node) ||
  nodeHasActiveTableFilters(node) ||
  nodeHasExludedTable(node);

export const nodeHasActiveTableFilters = (node: ConceptQueryNodeType) => {
  if (!node.tables) return false;

  return tablesHaveActiveFilter(node.tables);
};

export const nodeHasExludedTable = (node: ConceptQueryNodeType) => {
  if (!node.tables) return false;

  return node.tables.some(table => table.exclude);
};

export function nodeIsInvalid(
  node: ConceptQueryNodeType,
  blacklistedConceptIds?: string[],
  whitelistedConceptIds?: string[]
) {
  return (
    (!!whitelistedConceptIds &&
      !nodeIsWhitelisted(node, whitelistedConceptIds)) ||
    (!!blacklistedConceptIds && nodeIsBlacklisted(node, blacklistedConceptIds))
  );
}

export function nodeIsBlacklisted(
  node: ConceptQueryNodeType,
  blacklistedConceptIds: string[]
) {
  return (
    !!node.ids &&
    blacklistedConceptIds.some(id =>
      node.ids.some(conceptId => conceptId.indexOf(id.toLowerCase()) !== -1)
    )
  );
}

export function nodeIsWhitelisted(
  node: ConceptQueryNodeType,
  whitelistedConceptIds: string[]
) {
  return (
    !!node.ids &&
    whitelistedConceptIds.some(id =>
      node.ids.every(conceptId => conceptId.indexOf(id.toLowerCase()) !== -1)
    )
  );
}
