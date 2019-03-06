// @flow

import type { TableWithFilterValueType } from "../standard-query-editor/types";

import { isEmpty } from "../common/helpers";

export const tablesHaveActiveFilter = (tables: TableWithFilterValueType[]) =>
  tables.some(table => tableHasActiveFilters(table));

export const tableHasActiveFilters = (table: TableWithFilterValueType) =>
  (table.selects && table.selects.some(select => !!select.selected)) ||
  (table.filters &&
    table.filters.some(
      filter => !isEmpty(filter.value) && filter.value !== filter.defaultValue
    ));

export const resetAllFiltersInTables = (tables: TableWithFilterValueType[]) => {
  return (tables || []).map(table => {
    const selects = table.selects
      ? table.selects.map(select => ({
          ...select,
          selected: false
        }))
      : null;

    const filters = table.filters
      ? table.filters.map(filter => ({
          ...filter,
          value: filter.defaultValue,
          formattedValue: undefined
        }))
      : null;

    return {
      ...table,
      filters,
      selects,
      exclude: false
    };
  });
};
