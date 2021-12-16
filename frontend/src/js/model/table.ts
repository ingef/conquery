import type { TableT } from "../api/types";
import { compose } from "../common/helpers";
import { exists } from "../common/helpers/exists";
import type { TableWithFilterValueT } from "../standard-query-editor/types";

import {
  resetFilters,
  filterValueDiffersFromDefault,
  filterIsEmpty,
  filtersHaveValues,
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

export const resetAllTableSettings = (
  tables?: TableWithFilterValueT[],
  config: NodeResetConfig = {},
) => {
  if (!tables) return [];

  return resetTables(tables, config);
};

const tableWithDefaultDateColumn = (table: TableT): TableT => {
  return {
    ...table,
    dateColumn:
      !!table.dateColumn &&
      !!table.dateColumn.options &&
      table.dateColumn.options.length > 0
        ? {
            ...table.dateColumn,
            value: table.dateColumn.options[0].value as string,
          }
        : null,
  };
};

const tableWithDefaultFilters =
  (config: NodeResetConfig) => (table: TableT) => ({
    ...table,
    filters: resetFilters(table.filters, config),
  });

const tableWithDefaultSelects =
  (config: NodeResetConfig = {}) =>
  (table: TableT) => ({
    ...table,
    selects: resetSelects(table.selects, config),
  });

export const tableWithDefaults =
  (config: NodeResetConfig) =>
  (table: TableT): TableT =>
    compose(
      tableWithDefaultDateColumn,
      tableWithDefaultSelects(config),
      tableWithDefaultFilters(config),
    )({
      ...table,
      exclude: config.useDefaults ? !table.default : table.exclude,
    });

export const resetTables = (tables?: TableT[], config: NodeResetConfig = {}) =>
  tables ? tables.map(tableWithDefaults(config)) : [];
