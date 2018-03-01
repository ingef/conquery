// @flow

import type { ElementType }       from '../standard-query-editor/types';

import { tablesHaveActiveFilter } from './table';

export const nodeHasActiveFilters = (node, tables = node.tables) =>
  node.excludeTimestamps ||
    nodeHasActiveTableFilters(node, tables) ||
    nodeHasExludedTable(node, tables);

export const nodeHasActiveTableFilters = (node: ElementType, tables = node.tables) => {
  if (!tables) return false;
  return tablesHaveActiveFilter(tables);
};

export const nodeHasExludedTable = (node: ElementType, tables = node.tables) => {
  if (!tables) return false;
  return tables.some(table => table.exclude);
}
