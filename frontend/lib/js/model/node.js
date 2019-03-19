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
