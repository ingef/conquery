import {
  DateRestrictionNodeT,
  FilterConfigT,
  FilterT,
  NegationNodeT,
  OrNodeT,
  QueryConceptNodeT,
  RangeFilterValueT,
  SavedQueryNodeT,
  SelectFilterValueT,
  SelectOptionT,
  SelectorT,
  TableConfigT,
} from "../api/types";
import { DNDType } from "../common/constants/dndTypes";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import { getConceptsByIdsWithTablesAndSelects } from "../concept-trees/globalTreeStoreHelper";
import { TreesT } from "../concept-trees/reducer";

import {
  BigMultiSelectFilterWithValueType,
  DragItemConceptTreeNode,
  DragItemQuery,
  FilterWithValueType,
  MultiSelectFilterWithValueType,
  QueryGroupType,
  RangeFilterWithValueType,
  SelectedSelectorT,
  SelectFilterWithValueType,
  TableWithFilterValueT,
} from "./types";

interface RangeFilterConfig {
  filter: FilterT["id"];
  value: RangeFilterValueT;
  type: "INTEGER_RANGE" | "REAL_RANGE" | "MONEY_RANGE";
}
const isRangeFilterConfig = (
  filter: FilterConfigT,
): filter is RangeFilterConfig =>
  filter.type === "INTEGER_RANGE" ||
  filter.type === "REAL_RANGE" ||
  filter.type === "MONEY_RANGE";

const isMultiSelectFilterConfig = (
  filter: FilterConfigT,
): filter is {
  filter: FilterT["id"];
  value: FilterT["id"][];
  type: "MULTI_SELECT" | "BIG_MULTI_SELECT";
} =>
  (filter.type === "MULTI_SELECT" || filter.type === "BIG_MULTI_SELECT") &&
  filter.value instanceof Array;

const mergeRangeFilter = (
  savedFilter: RangeFilterWithValueType,
  matchingFilter: RangeFilterConfig,
): RangeFilterWithValueType => {
  const filterDetails =
    matchingFilter.value &&
    !isEmpty(matchingFilter.value.min) &&
    !isEmpty(matchingFilter.value.max) &&
    matchingFilter.value.min === matchingFilter.value.max
      ? {
          mode: "exact" as const,
          value: { exact: matchingFilter.value.min },
        }
      : { mode: "range" as const, value: matchingFilter.value };

  return {
    ...(savedFilter as RangeFilterWithValueType),
    ...filterDetails,
  };
};

const mergeMultiSelectFilter = ({
  savedFilter,
  matchingFilter,
}:
  | {
      savedFilter: MultiSelectFilterWithValueType;
      matchingFilter: {
        filter: FilterT["id"];
        value: string[];
        type: "MULTI_SELECT";
      };
    }
  | {
      savedFilter: BigMultiSelectFilterWithValueType;
      matchingFilter: {
        filter: FilterT["id"];
        value: string[];
        type: "BIG_MULTI_SELECT";
      };
    }): MultiSelectFilterWithValueType | BigMultiSelectFilterWithValueType => {
  const fixedFilterType = {
    type: savedFilter.type as typeof matchingFilter["type"], // matchingFilter.type is sometimes wrongly saying MULTI_SELECT
  };

  const basicFilter = {
    ...savedFilter,
    ...matchingFilter,
    ...fixedFilterType,
  };

  if (matchingFilter.value.length === 0) {
    return { ...basicFilter, value: [] };
  }

  const hasOptions = savedFilter.options.length > 0;
  const isMultiSelect = savedFilter.type === "MULTI_SELECT";

  if (isMultiSelect && hasOptions) {
    return {
      ...basicFilter,
      value: matchingFilter.value
        .map((val) => savedFilter.options.find((op) => op.value === val))
        .filter(exists),
    };
  } else {
    return {
      ...basicFilter,
      // This actually is a string[], which will be used to resolve the filter values
      // in useLoadBigMultiSelectValues (see actions)
      // TODO: Figure out how to actually use string[] as a type here, without having to adjust
      // the entire type hierarchy upwards
      value: matchingFilter.value as unknown as SelectOptionT[],
    };
  }
};

// Merges filter values from `table` into declared filters from `savedTable`
//
// `savedTable` may define filters, but it won't have any filter values,
// since `savedTables` comes from a `savedConcept` in a `conceptTree`. Such a
// `savedConcept` is never modified and only declares possible filters.
// Since `table` comes from a previous query, it may have set filter values
// if so, we will need to merge them in.
const mergeFiltersFromSavedConcept = (
  savedTable: TableWithFilterValueT,
  table?: TableConfigT,
): FilterWithValueType[] => {
  if (!table || !table.filters) return savedTable.filters;

  return savedTable.filters.map((savedFilter): FilterWithValueType => {
    // TODO: Improve the api and don't use `.filter`, but `.id` or `.filterId`
    const matchingFilter = table.filters!.find(
      (f) => f.filter === savedFilter.id,
    );

    if (!matchingFilter) {
      return savedFilter;
    }

    if (isRangeFilterConfig(matchingFilter)) {
      return mergeRangeFilter(
        savedFilter as RangeFilterWithValueType,
        matchingFilter,
      );
    }

    if (isMultiSelectFilterConfig(matchingFilter)) {
      return mergeMultiSelectFilter({
        savedFilter,
        matchingFilter,
      } as
        | {
            savedFilter: MultiSelectFilterWithValueType;
            matchingFilter: {
              filter: string;
              type: "MULTI_SELECT";
              value: string[];
            };
          }
        | {
            savedFilter: BigMultiSelectFilterWithValueType;
            matchingFilter: {
              filter: string;
              type: "BIG_MULTI_SELECT";
              value: string[];
            };
          });
    }

    const selectFilter: SelectFilterWithValueType = {
      ...(savedFilter as SelectFilterWithValueType),
      ...(matchingFilter as {
        filter: FilterT["id"]; // TODO: Rename this: "id"
        type: "SELECT";
        value: SelectFilterValueT;
      }),
    };
    return selectFilter;
  });
};

const mergeSelects = (
  savedSelects: SelectorT[],
  conceptOrTable?: QueryConceptNodeT | TableConfigT,
) => {
  if (!conceptOrTable || !conceptOrTable.selects) {
    return savedSelects;
  }

  return savedSelects.map((select) => {
    const selectedSelect = (conceptOrTable.selects || []).find(
      (id) => id === select.id,
    );

    return { ...select, selected: !!selectedSelect };
  });
};

const mergeDateColumn = (
  savedTable: TableWithFilterValueT,
  table?: TableConfigT,
) => {
  if (!table || !table.dateColumn || !savedTable.dateColumn)
    return savedTable.dateColumn;

  return {
    ...savedTable.dateColumn,
    value: table.dateColumn.value,
  };
};

const mergeTables = (
  savedTables: TableWithFilterValueT[],
  concept: QueryConceptNodeT,
) => {
  return savedTables.map((savedTable) => {
    // Find corresponding table in previous queryObject
    // TODO: Disentangle id / connectorId mixing
    const table = concept.tables.find((t) => t.id === savedTable.connectorId);
    const filters = mergeFiltersFromSavedConcept(savedTable, table);
    const selects = mergeSelects(savedTable.selects, table);
    const dateColumn = mergeDateColumn(savedTable, table);

    return {
      ...savedTable,
      exclude: !table,
      filters,
      selects,
      dateColumn,
    };
  });
};

// Look for tables in the already savedConcept. If they were not included in the
// respective query concept, exclude them.
// Also, apply all necessary filters
const mergeFromSavedConceptIntoNode = (
  node: QueryConceptNodeT,
  {
    tables,
    selects,
  }: {
    tables: TableWithFilterValueT[];
    selects: SelectedSelectorT[];
  },
) => {
  return {
    selects: mergeSelects(selects, node),
    tables: mergeTables(tables, node),
  };
};

const expandQueryNode = (
  rootConcepts: TreesT,
  node: QueryConceptNodeT | SavedQueryNodeT,
  expandErrorMessage: string,
) => {
  if (node.type === "SAVED_QUERY") {
    const queryNode: DragItemQuery = {
      ...node,
      query: undefined,
      dragContext: { width: 0, height: 0 },
      label: "", // TODO: DOUBLE CHECK
      tags: [],
      type: DNDType.PREVIOUS_QUERY,
      id: node.query,
    };

    return queryNode;
  }
  // node.type === "CONCEPT"

  const lookupResult = getConceptsByIdsWithTablesAndSelects(
    rootConcepts,
    node.ids,
    { useDefaults: false },
  );

  if (!lookupResult) {
    const errorNode: DragItemConceptTreeNode = {
      ...node,
      matchingEntities: 0,
      matchingEntries: 0,
      tables: [],
      selects: [],
      tree: "",
      label: "",
      dragContext: { width: 0, height: 0 },
      type: DNDType.CONCEPT_TREE_NODE,
      error: expandErrorMessage,
    };

    return errorNode;
  }

  const { tables, selects } = mergeFromSavedConceptIntoNode(node, {
    tables: lookupResult.tables,
    selects: lookupResult.selects || [],
  });
  const label = node.label || lookupResult.concepts[0].label;
  const description = lookupResult.concepts[0].description;

  const conceptNode: DragItemConceptTreeNode = {
    ...node,
    dragContext: { width: 0, height: 0 },
    additionalInfos: lookupResult.concepts[0].additionalInfos,
    matchingEntities: lookupResult.concepts[0].matchingEntities,
    matchingEntries: lookupResult.concepts[0].matchingEntries,
    type: DNDType.CONCEPT_TREE_NODE,
    label,
    description,
    tables,
    selects,
    excludeTimestamps: node.excludeFromTimeAggregation,
    tree: lookupResult.root,
  };

  return conceptNode;
};

export const expandNode = (
  rootConcepts: TreesT,
  node: NegationNodeT | DateRestrictionNodeT | OrNodeT,
  expandErrorMessage: string,
): QueryGroupType => {
  switch (node.type) {
    // The assumption is that the OR node always contains query nodes (query or concept)
    case "OR":
      return {
        elements: node.children.map((c) =>
          expandQueryNode(rootConcepts, c, expandErrorMessage),
        ),
      };
    case "DATE_RESTRICTION":
      return {
        dateRange: node.dateRange,
        ...expandNode(rootConcepts, node.child, expandErrorMessage),
      };
    case "NEGATION":
      return {
        exclude: true,
        ...expandNode(rootConcepts, node.child, expandErrorMessage),
      };
  }
};
