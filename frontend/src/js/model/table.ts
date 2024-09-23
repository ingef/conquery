import type { TableT } from "../api/types";
import { compose } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import type { TableWithFilterValueT } from "../standard-query-editor/types";

import {
  filterIsEmpty,
  filterValueDiffersFromDefault,
  filtersHaveValues,
  resetFilters,
} from "./filter";
import type { NodeResetConfig } from "./node";
import { objectHasNonDefaultSelects, resetSelects } from "./select";

export const tableIsEditable = (table: TableWithFilterValueT) =>
  (!!table.filters && table.filters.length > 0) ||
  (!!table.selects && table.selects.length > 0) ||
  (!!table.dateColumn && table.dateColumn.options.length > 0);

export const tablesHaveEmptySettings = (tables: TableWithFilterValueT[]) =>
  tables.every(tableHasEmptySettings);

export const tablesHaveNonDefaultSettings = (tables: TableWithFilterValueT[]) =>
  tables.some(tableHasNonDefaultSettings);

export const tableHasEmptySettings = (table: TableWithFilterValueT) => {
  return (
    (!table.selects || table.selects.every((select) => !select.selected)) &&
    !tableHasNonDefaultDateColumn(table) &&
    (!table.filters || table.filters.every(filterIsEmpty))
  );
};

export const tableHasFilterValues = (table: TableWithFilterValueT) =>
  !!table.filters && filtersHaveValues(table.filters);

export const tablesHaveFilterValues = (tables: TableWithFilterValueT[]) =>
  tables.some(tableHasFilterValues);

export const tableHasNonDefaultSettings = (table: TableWithFilterValueT) => {
  const activeSelects = objectHasNonDefaultSelects(table);
  const activeDateColumn = tableHasNonDefaultDateColumn(table);
  const activeFilters =
    table.filters && table.filters.some(filterValueDiffersFromDefault);

  return activeSelects || activeDateColumn || activeFilters;
};

const tableHasNonDefaultDateColumn = (table: TableWithFilterValueT) =>
  exists(table.dateColumn) &&
  table.dateColumn.options.length > 0 &&
  (exists(table.dateColumn.defaultValue)
    ? table.dateColumn.value !== table.dateColumn.defaultValue
    : table.dateColumn.value !== table.dateColumn.options[0].value);

export function tableIsIncludedInIds(
  table: TableWithFilterValueT,
  tableIds: string[],
) {
  return tableIds.some(
    (id) => table.id.toLowerCase().indexOf(id.toLowerCase()) !== -1,
  );
}

export function tableIsDisabled(
  table: TableWithFilterValueT,
  blocklistedTables?: string[],
  allowlistedTables?: string[],
) {
  return (
    (!!allowlistedTables && !tableIsIncludedInIds(table, allowlistedTables)) ||
    (!!blocklistedTables && tableIsIncludedInIds(table, blocklistedTables))
  );
}

const tableWithDefaultDateColumn = (
  table: TableWithFilterValueT,
): TableWithFilterValueT => {
  return {
    ...table,
    dateColumn:
      !!table.dateColumn && table.dateColumn.options.length > 0
        ? {
            ...table.dateColumn,
            value: table.dateColumn.options[0].value as string,
          }
        : undefined,
  };
};

const tableWithDefaultFilters =
  (config: NodeResetConfig) =>
  (table: TableWithFilterValueT): TableWithFilterValueT => ({
    ...table,
    filters: resetFilters(table.filters, config),
  });

const tableWithDefaultSelects =
  (config: NodeResetConfig = {}) =>
  (table: TableWithFilterValueT): TableWithFilterValueT => ({
    ...table,
    selects: resetSelects(table.selects, config),
  });

export const tableWithDefaults =
  (config: NodeResetConfig) =>
  (table: TableWithFilterValueT | TableT): TableWithFilterValueT =>
    compose(
      tableWithDefaultDateColumn,
      tableWithDefaultSelects(config),
      tableWithDefaultFilters(config),
    )({
      ...table,
      exclude: config.useDefaults ? !table.default : table.exclude,
    } as TableWithFilterValueT);

export const resetTables = (
  tables: TableT[] | TableWithFilterValueT[],
  config: NodeResetConfig = {},
) => tables.map(tableWithDefaults(config));
