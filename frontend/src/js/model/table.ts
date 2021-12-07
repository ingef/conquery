import type { TableT } from "../api/types";
import { compose } from "../common/helpers";
import { exists } from "../common/helpers/exists";
import type { TableWithFilterValueT } from "../standard-query-editor/types";

import { filtersWithDefaults, filterValueDiffersFromDefault } from "./filter";
import { objectHasSelectedSelects, selectsWithDefaults } from "./select";

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

export const resetAllFiltersInTables = (tables: TableWithFilterValueT[]) => {
  if (!tables) return [];

  return tablesWithDefaults(tables);
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

const tableWithDefaultFilters = (table: TableT) => ({
  ...table,
  filters: filtersWithDefaults(table.filters),
});

const tableWithDefaultSelects = (table: TableT) => ({
  ...table,
  selects: selectsWithDefaults(table.selects),
});

export const tableWithDefaults = (table: TableT): TableT =>
  compose(
    tableWithDefaultDateColumn,
    tableWithDefaultSelects,
    tableWithDefaultFilters,
  )({
    ...table,
    exclude: false,
  });

export const tablesWithDefaults = (tables?: TableT[]) =>
  tables ? tables.map(tableWithDefaults) : [];
