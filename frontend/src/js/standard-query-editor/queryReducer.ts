import { ActionType, getType } from "typesafe-actions";

import type {
  OrNodeT,
  DateRestrictionNodeT,
  NegationNodeT,
  QueryConceptNodeT,
  SavedQueryNodeT,
  SelectorT,
  TableConfigT,
  FilterConfigT,
  RangeFilterValueT,
  FilterT,
  ConceptIdT,
  SelectOptionT,
} from "../api/types";
import { Action } from "../app/actions";
import { DNDType } from "../common/constants/dndTypes";
import { isEmpty } from "../common/helpers";
import { exists } from "../common/helpers/exists";
import { getConceptsByIdsWithTablesAndSelects } from "../concept-trees/globalTreeStoreHelper";
import type { TreesT } from "../concept-trees/reducer";
import { isMultiSelectFilter, mergeFilterOptions } from "../model/filter";
import { nodeIsConceptQueryNode } from "../model/node";
import { resetSelects } from "../model/select";
import { resetTables, tableWithDefaults } from "../model/table";
import { loadQuerySuccess } from "../previous-queries/list/actions";
import {
  queryGroupModalResetAllDates,
  queryGroupModalSetDate,
} from "../query-group-modal/actions";
import { filterSuggestionToSelectOption } from "../query-node-editor/suggestionsHelper";
import { acceptQueryUploadConceptListModal } from "../query-upload-concept-list-modal/actions";
import { isMovedObject } from "../ui-components/Dropzone";

import {
  dropAndNode,
  dropOrNode,
  resetTable,
  clearQuery,
  deleteGroup,
  deleteNode,
  toggleTable,
  updateNodeLabel,
  setFilterValue,
  toggleExcludeGroup,
  loadSavedQuery,
  toggleTimestamps,
  toggleSecondaryIdExclude,
  resetAllSettings,
  addConceptToNode,
  removeConceptFromNode,
  switchFilterMode,
  setDateColumn,
  setSelects,
  setTableSelects,
  expandPreviousQuery,
  loadFilterSuggestionsSuccess,
} from "./actions";
import type {
  StandardQueryNodeT,
  QueryGroupType,
  DragItemConceptTreeNode,
  FilterWithValueType,
  DragItemQuery,
  TableWithFilterValueT,
  SelectedSelectorT,
} from "./types";

export type StandardQueryStateT = QueryGroupType[];

const initialState: StandardQueryStateT = [];

const filterItem = (item: StandardQueryNodeT): StandardQueryNodeT => {
  const baseItem = {
    dragContext: item.dragContext,
    label: item.label,
    excludeTimestamps: item.excludeTimestamps,
    excludeFromSecondaryId: item.excludeFromSecondaryId,
    loading: item.loading,
    error: item.error,
  };

  switch (item.type) {
    case DNDType.PREVIOUS_QUERY:
    case DNDType.PREVIOUS_SECONDARY_ID_QUERY:
      return {
        ...baseItem,
        type: item.type,

        id: item.id,
        query: item.query,
        tags: item.tags,
        canExpand: item.canExpand,
        availableSecondaryIds: item.availableSecondaryIds,
      };
    case DNDType.CONCEPT_TREE_NODE:
      return {
        ...baseItem,
        type: item.type,

        ids: item.ids,
        description: item.description,
        tables: item.tables,
        selects: item.selects,
        tree: item.tree,

        additionalInfos: item.additionalInfos,
        matchingEntries: item.matchingEntries,
        matchingEntities: item.matchingEntities,
        dateRange: item.dateRange,
      };
  }
};

const setGroupProperties = (
  node: StandardQueryStateT,
  andIdx: number,
  properties: Partial<QueryGroupType>,
) => {
  return [
    ...node.slice(0, andIdx),
    {
      ...node[andIdx],
      ...properties,
    },
    ...node.slice(andIdx + 1),
  ];
};

const setElementProperties = (
  node: StandardQueryStateT,
  andIdx: number,
  orIdx: number,
  properties: Partial<StandardQueryNodeT>,
) => {
  const groupProperties = {
    elements: [
      ...node[andIdx].elements.slice(0, orIdx),
      {
        ...node[andIdx].elements[orIdx],
        ...properties,
      },
      ...node[andIdx].elements.slice(orIdx + 1),
    ] as StandardQueryNodeT[],
  };

  return setGroupProperties(node, andIdx, groupProperties);
};

const onDropAndNode = (
  state: StandardQueryStateT,
  { item }: ActionType<typeof dropAndNode>["payload"],
) => {
  const group = state[state.length - 1];
  const dateRangeOfLastGroup = group ? group.dateRange : undefined;

  const nextState: StandardQueryStateT = [
    ...state,
    {
      elements: [filterItem(item)],
      dateRange: dateRangeOfLastGroup,
    },
  ];

  return isMovedObject(item)
    ? onDeleteNode(nextState, {
        andIdx: item.dragContext.movedFromAndIdx,
        orIdx: item.dragContext.movedFromOrIdx,
      })
    : nextState;
};

const onDropOrNode = (
  state: StandardQueryStateT,
  { item, andIdx }: ActionType<typeof dropOrNode>["payload"],
) => {
  const nextState = [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      elements: [filterItem(item), ...state[andIdx].elements],
    },
    ...state.slice(andIdx + 1),
  ];

  if (!isMovedObject(item)) {
    return nextState;
  }

  return item.dragContext.movedFromAndIdx === andIdx
    ? onDeleteNode(nextState, {
        andIdx: item.dragContext.movedFromAndIdx,
        orIdx: item.dragContext.movedFromOrIdx + 1,
      })
    : onDeleteNode(nextState, {
        andIdx: item.dragContext.movedFromAndIdx,
        orIdx: item.dragContext.movedFromOrIdx,
      });
};

// Delete a single Node (concept inside a group)
const onDeleteNode = (
  state: StandardQueryStateT,
  { andIdx, orIdx }: ActionType<typeof deleteNode>["payload"],
) => {
  return [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      elements: [
        ...state[andIdx].elements.slice(0, orIdx),
        ...state[andIdx].elements.slice(orIdx + 1),
      ],
    },
    ...state.slice(andIdx + 1),
  ].filter((and) => !!and.elements && and.elements.length > 0);
};

const onDeleteGroup = (
  state: StandardQueryStateT,
  { andIdx }: ActionType<typeof deleteGroup>["payload"],
) => {
  return [...state.slice(0, andIdx), ...state.slice(andIdx + 1)];
};

const onToggleExcludeGroup = (
  state: StandardQueryStateT,
  { andIdx }: ActionType<typeof toggleExcludeGroup>["payload"],
) => {
  return [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      exclude: state[andIdx].exclude ? undefined : true,
    },
    ...state.slice(andIdx + 1),
  ];
};

const updateNodeTable = (
  state: StandardQueryStateT,
  andIdx: number,
  orIdx: number,
  tableIdx: number,
  table: TableWithFilterValueT,
) => {
  const node = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(node)) return state;

  const tables = [
    ...node.tables.slice(0, tableIdx),
    table,
    ...node.tables.slice(tableIdx + 1),
  ];

  return updateNodeTables(state, andIdx, orIdx, tables);
};

const updateNodeTables = (
  state: StandardQueryStateT,
  andIdx: number,
  orIdx: number,
  tables: TableWithFilterValueT[],
) => {
  return setElementProperties(state, andIdx, orIdx, { tables });
};

const onToggleNodeTable = (
  state: StandardQueryStateT,
  {
    andIdx,
    orIdx,
    tableIdx,
    isExcluded,
  }: ActionType<typeof toggleTable>["payload"],
) => {
  const node = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(node)) return state;

  const table = {
    ...node.tables[tableIdx],
    exclude: isExcluded,
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, table);
};

const setNodeFilterProperties = (
  state: StandardQueryStateT,
  {
    andIdx,
    orIdx,
    tableIdx,
    filterIdx,
  }: { andIdx: number; orIdx: number; tableIdx: number; filterIdx: number },
  properties: Partial<FilterWithValueType>,
) => {
  const nodeFromState = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(nodeFromState)) return state;

  const table = nodeFromState.tables[tableIdx];
  const { filters } = table;

  const filter = filters[filterIdx];

  const newTable: TableWithFilterValueT = {
    ...table,
    filters: [
      ...filters.slice(0, filterIdx),
      {
        ...filter,
        ...properties,
      },
      ...filters.slice(filterIdx + 1),
    ] as FilterWithValueType[],
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, newTable);
};

const setNodeFilterValue = (
  state: StandardQueryStateT,
  payload: ActionType<typeof setFilterValue>["payload"],
) => {
  return setNodeFilterProperties(state, payload, { value: payload.value });
};

const setNodeTableSelects = (
  state: StandardQueryStateT,
  {
    andIdx,
    orIdx,
    tableIdx,
    value,
  }: ActionType<typeof setTableSelects>["payload"],
) => {
  const node = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(node)) return state;

  const table = node.tables[tableIdx];
  const { selects } = table;

  // value contains the selects that have now been selected
  const newTable = {
    ...table,
    selects: selects.map((select) => ({
      ...select,
      selected:
        !!value &&
        !!value.find((selectedValue) => selectedValue.value === select.id),
    })),
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, newTable);
};

const setNodeTableDateColumn = (
  state: StandardQueryStateT,
  {
    andIdx,
    orIdx,
    tableIdx,
    value,
  }: ActionType<typeof setDateColumn>["payload"],
) => {
  const node = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(node)) return state;

  const table = node.tables[tableIdx];
  const { dateColumn } = table;

  if (!dateColumn) return state;

  const newTable: TableWithFilterValueT = {
    ...table,
    dateColumn: {
      ...dateColumn,
      value,
    },
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, newTable);
};

const setNodeSelects = (
  state: StandardQueryStateT,
  { andIdx, orIdx, value }: ActionType<typeof setSelects>["payload"],
) => {
  const node = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(node)) return state;

  return setElementProperties(state, andIdx, orIdx, {
    selects: node.selects.map((select) => ({
      ...select,
      selected:
        !!value &&
        !!value.find((selectedValue) => selectedValue.value === select.id),
    })),
  });
};

const switchNodeFilterMode = (
  state: StandardQueryStateT,
  {
    andIdx,
    orIdx,
    tableIdx,
    filterIdx,
    mode,
  }: ActionType<typeof switchFilterMode>["payload"],
) => {
  return setNodeFilterProperties(
    state,
    { andIdx, orIdx, tableIdx, filterIdx },
    {
      mode,
      value: null,
    },
  );
};

const resetNodeAllSettings = (
  state: StandardQueryStateT,
  { andIdx, orIdx, config }: ActionType<typeof resetAllSettings>["payload"],
) => {
  const node = state[andIdx].elements[orIdx];

  const newState = setElementProperties(state, andIdx, orIdx, {
    excludeFromSecondaryId: false,
    excludeTimestamps: false,
    selects: nodeIsConceptQueryNode(node)
      ? resetSelects(node.selects, config)
      : [],
  });

  if (!nodeIsConceptQueryNode(node)) return newState;

  const tables = resetTables(node.tables, config);

  return updateNodeTables(newState, andIdx, orIdx, tables);
};

const resetNodeTable = (
  state: StandardQueryStateT,
  { andIdx, orIdx, tableIdx, config }: ActionType<typeof resetTable>["payload"],
) => {
  const node = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(node)) return state;

  const table = node.tables[tableIdx];

  if (!table) return state;

  return updateNodeTable(
    state,
    andIdx,
    orIdx,
    tableIdx,
    tableWithDefaults(config)(table),
  );
};

const setGroupDate = (
  state: StandardQueryStateT,
  { andIdx, date }: ActionType<typeof queryGroupModalSetDate>["payload"],
) => {
  return setGroupProperties(state, andIdx, { dateRange: date });
};

const resetGroupDates = (
  state: StandardQueryStateT,
  { andIdx }: ActionType<typeof queryGroupModalResetAllDates>["payload"],
) => {
  return setGroupProperties(state, andIdx, { dateRange: undefined });
};

const isRangeFilterConfig = (
  filter: FilterConfigT,
): filter is {
  filter: FilterT["id"];
  value: RangeFilterValueT;
  type: "INTEGER_RANGE" | "REAL_RANGE" | "MONEY_RANGE";
} =>
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
) => {
  if (!table || !table.filters) return savedTable.filters || null;

  if (!savedTable.filters) return null;

  return savedTable.filters.map((savedFilter) => {
    // TODO: Improve the api and don't use `.filter`, but `.id` or `.filterId`
    const matchingFilter = table.filters!.find(
      (f) => f.filter === savedFilter.id,
    );

    if (!matchingFilter) {
      return savedFilter;
    }

    if (isRangeFilterConfig(matchingFilter)) {
      const filterDetails =
        matchingFilter.value &&
        !isEmpty(matchingFilter.value.min) &&
        !isEmpty(matchingFilter.value.max) &&
        matchingFilter.value.min === matchingFilter.value.max
          ? { mode: "exact", value: { exact: matchingFilter.value.min } }
          : { mode: "range", value: matchingFilter.value };

      return { ...savedFilter, ...filterDetails };
    }

    if (isMultiSelectFilterConfig(matchingFilter)) {
      const filterDetails = {
        ...matchingFilter,
        type: savedFilter.type, // matchingFilter.type is sometimes wrongly saying MULTI_SELECT
        value: matchingFilter.value
          .map((val) => {
            if (!isMultiSelectFilter(savedFilter)) {
              console.error(
                `Filter: ${savedFilter} is not a multi-select filter, even though its matching filter was: ${matchingFilter}`,
              );
              return val;
            } else {
              // There is the possibility, that we have a BIG_MULTI_SELECT that loads options async.
              // Then filter.options would be empty and we wouldn't find it
              return savedFilter.options.find((op) => op.value === val) || val;
            }
          })
          .filter(exists),
        // For BIG MULTI SELECT only, to be able to load all non-loaded options from the defaultValue later
        defaultValue: matchingFilter.value.filter((val) => {
          if (!isMultiSelectFilter(savedFilter)) {
            console.error(
              `Filter: ${savedFilter} is not a multi-select filter, even though its matching filter was: ${matchingFilter}`,
            );
            return false;
          }

          return !exists(savedFilter.options.find((opt) => opt.value === val));
        }),
      };

      return { ...savedFilter, ...filterDetails };
    }

    return { ...savedFilter, ...matchingFilter };
  });
};

const mergeSelects = (
  savedSelects?: SelectorT[],
  conceptOrTable?: QueryConceptNodeT | TableConfigT,
) => {
  if (!conceptOrTable || !conceptOrTable.selects) {
    return savedSelects || null;
  }

  if (!savedSelects) return null;

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
  return savedTables
    ? savedTables.map((savedTable) => {
        // Find corresponding table in previous queryObject
        // TODO: Disentangle id / connectorId mixing
        const table = concept.tables.find(
          (t) => t.id === savedTable.connectorId,
        );
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
      })
    : [];
};

// Look for tables in the already savedConcept. If they were not included in the
// respective query concept, exclude them.
// Also, apply all necessary filters
const mergeFromSavedConceptIntoNode = (
  node: QueryConceptNodeT,
  {
    tables,
    selects,
  }: { tables: TableWithFilterValueT[]; selects: SelectedSelectorT[] },
) => {
  return {
    selects: mergeSelects(selects, node),
    tables: mergeTables(tables, node),
  };
};

const expandNode = (
  rootConcepts: TreesT,
  node:
    | NegationNodeT
    | DateRestrictionNodeT
    | OrNodeT
    | QueryConceptNodeT
    | SavedQueryNodeT,
  expandErrorMessage: string,
): any /* This FN returns a QueryGroupType in the end  */ => {
  switch (node.type) {
    case "OR":
      // The assumption is that the OR node is always the root of the tree.
      return {
        elements: node.children.map((c) =>
          expandNode(rootConcepts, c, expandErrorMessage),
        ),
      };
    case "SAVED_QUERY":
      return {
        ...node,
        type: DNDType.PREVIOUS_QUERY,
        id: node.query,
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
    default:
      const lookupResult = getConceptsByIdsWithTablesAndSelects(
        rootConcepts,
        node.ids,
        { useDefaults: false },
      );

      if (!lookupResult)
        return {
          ...node,
          type: DNDType.CONCEPT_TREE_NODE,
          error: expandErrorMessage,
        };

      const { tables, selects } = mergeFromSavedConceptIntoNode(node, {
        tables: lookupResult.tables,
        selects: lookupResult.selects || [],
      });
      const label = node.label || lookupResult.concepts[0].label;
      const description = lookupResult.concepts[0].description;

      return {
        ...node,
        type: DNDType.CONCEPT_TREE_NODE,
        label,
        description,
        tables,
        selects,
        excludeTimestamps: node.excludeFromTimeAggregation,
        excludeFromSecondaryId: node.excludeFromSecondaryId,
        tree: lookupResult.root,
      };
  }
};

// Completely override all groups in the editor with the previous groups, but
// a) merge elements with concept data from concept trees (esp. "tables")
// b) load nested previous queries contained in that query,
//    so they can also be expanded
const onExpandPreviousQuery = ({
  rootConcepts,
  query,
  expandErrorMessage,
}: ActionType<typeof expandPreviousQuery>["payload"]): StandardQueryStateT => {
  return query.root.children.map((child) =>
    expandNode(rootConcepts, child, expandErrorMessage),
  );
};

const findPreviousQueries = (state: StandardQueryStateT, queryId: string) => {
  // Find all nodes that are previous queries and have the correct id
  return state.flatMap((group, andIdx) => {
    return group.elements
      .map((concept, orIdx) => [concept, orIdx] as [StandardQueryNodeT, number])
      .filter((item): item is [DragItemQuery, number] => {
        const [concept] = item;
        return (
          (concept.type === DNDType.PREVIOUS_QUERY ||
            concept.type === DNDType.PREVIOUS_SECONDARY_ID_QUERY) &&
          concept.id === queryId
        );
      })
      .map(([concept, orIdx]) => ({
        andIdx,
        orIdx,
        node: concept,
      }));
  });
};

const updatePreviousQueries = (
  state: StandardQueryStateT,
  action: { payload: { id: string } },
  attributes: Partial<DragItemQuery>,
) => {
  const queries = findPreviousQueries(state, action.payload.id);

  return queries.reduce((nextState, query) => {
    const { node, andIdx, orIdx } = query;

    return [
      ...nextState.slice(0, andIdx),
      {
        ...nextState[andIdx],
        elements: [
          ...nextState[andIdx].elements.slice(0, orIdx),
          {
            ...node,
            ...attributes,
          },
          ...nextState[andIdx].elements.slice(orIdx + 1),
        ],
      },
      ...nextState.slice(andIdx + 1),
    ];
  }, state);
};

const loadPreviousQuerySuccess = (
  state: StandardQueryStateT,
  action: ActionType<typeof loadQuerySuccess>,
) => {
  const { data } = action.payload;

  const maybeLabel = data.label ? { label: data.label } : {};

  return updatePreviousQueries(state, action, {
    ...maybeLabel,
    id: data.id,
    loading: false,
    query: data.query,
    canExpand: data.canExpand,
    availableSecondaryIds: data.availableSecondaryIds,
  });
};

const onToggleTimestamps = (
  state: StandardQueryStateT,
  { andIdx, orIdx }: ActionType<typeof toggleTimestamps>["payload"],
) => {
  return setElementProperties(state, andIdx, orIdx, {
    excludeTimestamps: !state[andIdx].elements[orIdx].excludeTimestamps,
  });
};

const onToggleSecondaryIdExclude = (
  state: StandardQueryStateT,
  { andIdx, orIdx }: ActionType<typeof toggleSecondaryIdExclude>["payload"],
) => {
  return setElementProperties(state, andIdx, orIdx, {
    excludeFromSecondaryId:
      !state[andIdx].elements[orIdx].excludeFromSecondaryId,
  });
};

const mergeStandardQueryFilterOptions = (
  state: StandardQueryStateT,
  {
    andIdx,
    orIdx,
    tableIdx,
    filterIdx,
  }: { andIdx: number; orIdx: number; tableIdx: number; filterIdx: number },
  newOptions: SelectOptionT[],
) => {
  // -------------
  // A bit verbose, just to get the filter, maybe extract to a fn?
  const nodeFromState = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(nodeFromState)) return null;

  const table = nodeFromState.tables[tableIdx];
  const { filters } = table;

  const filter = filters[filterIdx];
  // -------------

  return mergeFilterOptions(filter, newOptions);
};

const onLoadFilterSuggestionsSuccess = (
  state: StandardQueryStateT,
  { data, ...rest }: ActionType<typeof loadFilterSuggestionsSuccess>["payload"],
) => {
  const newOptions: SelectOptionT[] = data.values.map(
    filterSuggestionToSelectOption,
  );

  const options =
    rest.page === 0
      ? newOptions
      : mergeStandardQueryFilterOptions(state, rest, newOptions);

  if (!exists(options)) return state;

  return setNodeFilterProperties(state, rest, {
    options,
    total: data.total,
  });
};

const createQueryNodeFromConceptListUploadResult = (
  label: string,
  rootConcepts: TreesT,
  resolvedConcepts: ConceptIdT[],
): DragItemConceptTreeNode | null => {
  const lookupResult = getConceptsByIdsWithTablesAndSelects(
    rootConcepts,
    resolvedConcepts,
    { useDefaults: true },
  );

  return lookupResult
    ? {
        type: DNDType.CONCEPT_TREE_NODE,
        dragContext: {
          height: 0,
          width: 0,
        },
        label,
        ids: resolvedConcepts,
        tables: lookupResult.tables,
        selects: lookupResult.selects || [],
        tree: lookupResult.root,
        matchingEntities: 0,
        matchingEntries: 0,
      }
    : null;
};

const insertUploadedConceptList = (
  state: StandardQueryStateT,
  {
    label,
    rootConcepts,
    resolvedConcepts,
    andIdx,
  }: ActionType<typeof acceptQueryUploadConceptListModal>["payload"],
) => {
  const queryElement = createQueryNodeFromConceptListUploadResult(
    label,
    rootConcepts,
    resolvedConcepts,
  );

  if (!queryElement) return state;

  return andIdx === null
    ? onDropAndNode(state, {
        item: queryElement,
      })
    : onDropOrNode(state, {
        andIdx,
        item: queryElement,
      });
};

const onUpdateNodeLabel = (
  state: StandardQueryStateT,
  { andIdx, orIdx, label }: ActionType<typeof updateNodeLabel>["payload"],
) => {
  return setElementProperties(state, andIdx, orIdx, {
    label,
  });
};

const onAddConceptToNode = (
  state: StandardQueryStateT,
  { andIdx, orIdx, concept }: ActionType<typeof addConceptToNode>["payload"],
) => {
  const node = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(node)) return state;

  return setElementProperties(state, andIdx, orIdx, {
    ids: [...concept.ids, ...node.ids],
  });
};

const onRemoveConceptFromNode = (
  state: StandardQueryStateT,
  {
    andIdx,
    orIdx,
    conceptId,
  }: ActionType<typeof removeConceptFromNode>["payload"],
) => {
  const node = state[andIdx].elements[orIdx];

  if (!nodeIsConceptQueryNode(node)) return state;

  return setElementProperties(state, andIdx, orIdx, {
    ids: node.ids.filter((id) => id !== conceptId),
  });
};

// Query is an array of "groups" (a AND b and c)
// where a, b, c are objects, that (can) have properites,
// like `dateRange` or `exclude`.
// But the main property is "elements" - an array of objects
// that contain at least an ID.
// An element may contain an array of tables that may
// either be excluded, or contain an array of filters with values.
//
// Example:
// [
//   {
//     elements: [
//       { id: 9, tables: [{ id: 1}] },
//       {
//         id: 10,
//         tables: [
//           { id: 213, exclude: true },
//           {
//             id: 452,
//             filters: [
//               { id: 52, type: 'INTEGER_RANGE', value: { min: 2, max: 3 } }
//               { id: 53, type: 'SELECT', value: "Some example filter value" }
//             ]
//           }
//         ]
//       }
//     ]
//   }, {
//     elements: [
//      {id: 6, tables: []}, {id: 7, tables: []}, {id: 5, tables: []}
//     ]
//   }
// ]
const query = (
  state: StandardQueryStateT = initialState,
  action: Action,
): StandardQueryStateT => {
  switch (action.type) {
    case getType(clearQuery):
      return initialState;
    case getType(dropAndNode):
      return onDropAndNode(state, action.payload);
    case getType(dropOrNode):
      return onDropOrNode(state, action.payload);
    case getType(deleteNode):
      return onDeleteNode(state, action.payload);
    case getType(deleteGroup):
      return onDeleteGroup(state, action.payload);
    case getType(toggleExcludeGroup):
      return onToggleExcludeGroup(state, action.payload);
    case getType(loadSavedQuery):
      return action.payload.query;
    case getType(updateNodeLabel):
      return onUpdateNodeLabel(state, action.payload);
    case getType(addConceptToNode):
      return onAddConceptToNode(state, action.payload);
    case getType(removeConceptFromNode):
      return onRemoveConceptFromNode(state, action.payload);
    case getType(toggleTable):
      return onToggleNodeTable(state, action.payload);
    case getType(setFilterValue):
      return setNodeFilterValue(state, action.payload);
    case getType(setTableSelects):
      return setNodeTableSelects(state, action.payload);
    case getType(setSelects):
      return setNodeSelects(state, action.payload);
    case getType(resetAllSettings):
      return resetNodeAllSettings(state, action.payload);
    case getType(resetTable):
      return resetNodeTable(state, action.payload);
    case getType(switchFilterMode):
      return switchNodeFilterMode(state, action.payload);
    case getType(toggleTimestamps):
      return onToggleTimestamps(state, action.payload);
    case getType(toggleSecondaryIdExclude):
      return onToggleSecondaryIdExclude(state, action.payload);
    case getType(queryGroupModalSetDate):
      return setGroupDate(state, action.payload);
    case getType(queryGroupModalResetAllDates):
      return resetGroupDates(state, action.payload);
    case getType(expandPreviousQuery):
      return onExpandPreviousQuery(action.payload);
    case getType(loadQuerySuccess):
      return loadPreviousQuerySuccess(state, action);
    case getType(loadFilterSuggestionsSuccess):
      return onLoadFilterSuggestionsSuccess(state, action.payload);
    case getType(acceptQueryUploadConceptListModal):
      return insertUploadedConceptList(state, action.payload);
    case getType(setDateColumn):
      return setNodeTableDateColumn(state, action.payload);
    default:
      return state;
  }
};

export default query;
