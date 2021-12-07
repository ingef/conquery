import type { TableT } from "../api/types";
import { compose, isEmpty } from "../common/helpers";
import { exists } from "../common/helpers/exists";
import type { TableWithFilterValueT } from "../standard-query-editor/types";

import { resetFilters } from "./filter";
import type { NodeResetConfig } from "./node";
import { objectHasSelectedSelects, resetSelects } from "./select";

export const tableIsEditable = (table: TableWithFilterValueT) =>
  (!!table.filters && table.filters.length > 0) ||
  (!!table.selects && table.selects.length > 0) ||
  (!!table.dateColumn && table.dateColumn.options.length > 0);

export const tablesHaveActiveFilter = (tables: TableWithFilterValueT[]) =>
  tables.some((table) => tableHasActiveFilters(table));

export const tableHasActiveFilters = (table: TableWithFilterValueT) => {
  const activeSelects = objectHasSelectedSelects(table);
  const activeDateColumn = tableHasNonDefaultDateColumn(table);
  const activeFilters =
    table.filters &&
    table.filters.some(
      (filter) =>
        !isEmpty(filter.value) &&
        (isEmpty(filter.defaultValue) || filter.value !== filter.defaultValue),
    );

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

export const resetAllFiltersInTables = (
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
      exclude: Boolean(config.useDefaults && !table.default),
    });

export const resetTables = (tables?: TableT[], config: NodeResetConfig = {}) =>
  tables ? tables.map(tableWithDefaults(config)) : [];
