// @flow

import type { TableWithFilterValueType } from "../standard-query-editor/types";

import { isEmpty, compose } from "../common/helpers";

import { objectHasSelectedSelects, selectsWithDefaults } from "./select";
import { filtersWithDefaults } from "./filter";

export const tablesHaveActiveFilter = (tables: TableWithFilterValueType[]) =>
  tables.some(table => tableHasActiveFilters(table));

export const tableHasActiveFilters = (table: TableWithFilterValueType) =>
  objectHasSelectedSelects(table) ||
  tableHasNonDefaultDateColumn(table) ||
  (table.filters &&
    table.filters.some(
      filter => !isEmpty(filter.value) && filter.value !== filter.defaultValue
    ));

const tableHasNonDefaultDateColumn = (table: TableWithFilterValueType) =>
  !!table.dateColumn &&
  !!table.dateColumn.options &&
  table.dateColumn.options.length > 0 &&
  table.dateColumn.value !== table.dateColumn.options[0].value;

export function tableIsDisabled(
  table: TableWithFilterValueType,
  disabledTables: string[]
) {
  return disabledTables.some(
    tableName => table.id.toLowerCase().indexOf(tableName.toLowerCase()) !== -1
  );
}

export const resetAllFiltersInTables = (tables: TableWithFilterValueType[]) => {
  if (!tables) return [];

  return tablesWithDefaults(tables);
};

const tableWithDefaultDateColumn = table => {
  return {
    ...table,
    dateColumn:
      !!table.dateColumn &&
      !!table.dateColumn.options &&
      table.dateColumn.options.length > 0
        ? { ...table.dateColumn, value: table.dateColumn.options[0].value }
        : null
  };
};

const tableWithDefaultFilters = table => ({
  ...table,
  filters: filtersWithDefaults(table.filters)
});

const tableWithDefaultSelects = table => ({
  ...table,
  selects: selectsWithDefaults(table.selects)
});

const tableWithDefaults = table =>
  compose(
    tableWithDefaultDateColumn,
    tableWithDefaultSelects,
    tableWithDefaultFilters
  )({
    ...table,
    exclude: false
  });

export const tablesWithDefaults = tables =>
  tables ? tables.map(tableWithDefaults) : null;
