// @flow

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

export const nodeIsDisabled = (
  node: ConceptQueryNodeType,
  disallowedConceptIds: string[]
) =>
  !!node.ids &&
  disallowedConceptIds.some(id =>
    node.ids.some(conceptId => conceptId.indexOf(id.toLowerCase()) !== -1)
  );

export const nodeIsEnabled = (
  node: ConceptQueryNodeType,
  allowedConceptIds: string[]
) =>
  !!node.ids &&
  allowedConceptIds.some(id =>
    node.ids.every(conceptId => conceptId.indexOf(id.toLowerCase()) !== -1)
  );
