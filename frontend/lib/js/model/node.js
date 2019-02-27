// @flow

import type {
  ConceptQueryNodeType,
  TableWithFilterValueType
} from "../standard-query-editor/types";

import { tablesHaveActiveFilter } from "./table";

export const nodeHasActiveFilters = (
  node: ConceptQueryNodeType,
  tables: TableWithFilterValueType[] = node.tables
) =>
  node.excludeTimestamps ||
  node.includeSubnodes ||
  nodeHasActiveTableFilters(node, tables) ||
  nodeHasExludedTable(node, tables);

export const nodeHasActiveTableFilters = (
  node: ConceptQueryNodeType,
  tables: TableWithFilterValueType[] = node.tables
) => {
  if (!tables) return false;

  return tablesHaveActiveFilter(tables);
};

export const nodeHasExludedTable = (
  node: ConceptQueryNodeType,
  tables: TableWithFilterValueType[] = node.tables
) => {
  if (!tables) return false;

  return tables.some(table => table.exclude);
};
