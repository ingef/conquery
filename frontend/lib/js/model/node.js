// @flow

import type { ConceptQueryNodeType } from "../standard-query-editor/types";

import { tablesHaveActiveFilter } from "./table";

export const nodeHasActiveFilters = (node: ConceptQueryNodeType) =>
  node.excludeTimestamps ||
  node.includeSubnodes ||
  nodeHasSelectedSelects(node) ||
  nodeHasActiveTableFilters(node) ||
  nodeHasExludedTable(node);

export const nodeHasSelectedSelects = (node: ConceptQueryNodeType) => {
  if (!node.selects) return false;

  return node.selects.some(select => select.selected);
};

export const nodeHasActiveTableFilters = (node: ConceptQueryNodeType) => {
  if (!node.tables) return false;

  return tablesHaveActiveFilter(node.tables);
};

export const nodeHasExludedTable = (node: ConceptQueryNodeType) => {
  if (!node.tables) return false;

  return node.tables.some(table => table.exclude);
};
