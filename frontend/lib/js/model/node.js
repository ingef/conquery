// @flow

import type { QueryNodeType, TableType }       from '../standard-query-editor/types';

import { tablesHaveActiveFilter }            from './table';

export const nodeHasActiveFilters = (node: QueryNodeType, tables: Array<TableType> = node.tables) =>
  node.excludeTimestamps ||
    nodeHasActiveTableFilters(node, tables) ||
    nodeHasExludedTable(node, tables);

export const nodeHasActiveTableFilters = (node: QueryNodeType, tables: Array<TableType> = node.tables) => {
  if (!tables) return false;
  return tablesHaveActiveFilter(tables);
};

export const nodeHasExludedTable = (node: QueryNodeType, tables: Array<TableType> = node.tables) => {
  if (!tables) return false;
  return tables.some(table => table.exclude);
}
