// @flow

import type { TableType } from '../standard-query-editor/types';

import { isEmpty }        from '../common/helpers';

export const tablesHaveActiveFilter = (tables: TableType[]) =>
  tables.some(table => tableHasActiveFilters(table));

export const tableHasActiveFilters = (table: TableType) =>
    table.filters &&
    table.filters.some(filter => !isEmpty(filter.value) && filter.value !== filter.defaultValue);

export const resetAllFiltersInTables = (tables: TableType[]) => {
  return (tables || []).map(table => {
    const filters = table.filters
      ? table.filters.map((filter) => ({
          ...filter,
          value: filter.defaultValue,
          formattedValue: undefined
        }))
      : null;

    return {
      ...table,
      filters,
      exclude: false,
    };
  });
}
