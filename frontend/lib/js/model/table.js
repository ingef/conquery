// @flow

import type { TableWithFilterValueType } from "../standard-query-editor/types";

import { MULTI_SELECT, BIG_MULTI_SELECT } from "../form-components/filterTypes";

import { isEmpty } from "../common/helpers";

import { objectHasSelectedSelects } from "./select";

export const tablesHaveActiveFilter = (tables: TableWithFilterValueType[]) =>
  tables.some(table => tableHasActiveFilters(table));

export const tableHasActiveFilters = (table: TableWithFilterValueType) =>
  objectHasSelectedSelects(table) ||
  (table.filters &&
    table.filters.some(
      filter => !isEmpty(filter.value) && filter.value !== filter.defaultValue
    ));

export function tableIsDisabled(
  table: TableWithFilterValueType,
  disabledTables: string[]
) {
  return disabledTables.some(
    tableName => table.id.toLowerCase().indexOf(tableName.toLowerCase()) !== -1
  );
}

export const resetAllFiltersInTables = (tables: TableWithFilterValueType[]) => {
  return (tables || []).map(table => {
    const selects = table.selects
      ? table.selects.map(select => ({
          ...select,
          selected: false
        }))
      : null;

    const filters = table.filters
      ? table.filters.map(filter => {
          switch (filter.type) {
            case MULTI_SELECT:
            case BIG_MULTI_SELECT:
              return {
                ...filter,
                value: filter.defaultValue || []
              };
            default:
              return {
                ...filter,
                value: filter.defaultValue || null
              };
          }
        })
      : null;

    return {
      ...table,
      filters,
      selects,
      exclude: false
    };
  });
};
