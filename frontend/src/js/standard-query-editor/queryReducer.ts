import { ActionType, getType } from "typesafe-actions";

import type {
  TableT,
  OrNodeT,
  DateRestrictionNodeT,
  NegationNodeT,
  QueryConceptNodeT,
  SavedQueryNodeT,
  SelectorT,
  TableConfigT,
  FilterConfigT,
  RangeFilterValueT,
  FilterIdT,
  ConceptIdT,
} from "../api/types";
import { Action } from "../app/actions";
import { isEmpty, objectWithoutKey } from "../common/helpers";
import { exists } from "../common/helpers/exists";
import { getConceptsByIdsWithTablesAndSelects } from "../concept-trees/globalTreeStoreHelper";
import type { TreesT } from "../concept-trees/reducer";
import { isMultiSelectFilter } from "../model/filter";
import { nodeIsConceptQueryNode } from "../model/node";
import { selectsWithDefaults } from "../model/select";
import { resetAllFiltersInTables, tableWithDefaults } from "../model/table";
import { loadQuery, renameQuery } from "../previous-queries/list/actions";
import {
  queryGroupModalResetAllDates,
  queryGroupModalSetDate,
} from "../query-group-modal/actions";
import { acceptQueryUploadConceptListModal } from "../query-upload-concept-list-modal/actions";

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
  resetAllFilters,
  addConceptToNode,
  removeConceptFromNode,
  switchFilterMode,
  setDateColumn,
  setSelects,
  setTableSelects,
  expandPreviousQuery,
  loadFilterSuggestions,
} from "./actions";
import type {
  StandardQueryNodeT,
  DragItemQuery,
  QueryGroupType,
  DragItemNode,
  DragItemConceptTreeNode,
  FilterWithValueType,
} from "./types";

export type StandardQueryStateT = QueryGroupType[];

const initialState: StandardQueryStateT = [];

const filterItem = (
  item: DragItemNode | DragItemQuery | DragItemConceptTreeNode,
): StandardQueryNodeT => {
  // This sort of mapping might be a problem when adding new optional properties to
  // either Nodes or Queries: Flow won't complain when we omit those optional
  // properties here. But we can't use a spread operator either...
  const baseItem = {
    label: item.label,
    excludeTimestamps: item.excludeTimestamps,
    excludeFromSecondaryIdQuery: item.excludeFromSecondaryIdQuery,
    loading: item.loading,
    error: item.error,
  };

  if (item.isPreviousQuery) {
    return {
      ...baseItem,

      id: item.id,
      // eslint-disable-next-line no-use-before-define
      query: item.query,
      isPreviousQuery: item.isPreviousQuery,
      canExpand: item.canExpand,
      availableSecondaryIds: item.availableSecondaryIds,
    };
  } else {
    return {
      ...baseItem,

      ids: item.ids,
      description: item.description,
      tables: item.tables,
      selects: item.selects,
      tree: item.tree,

      additionalInfos: item.additionalInfos,
      matchingEntries: item.matchingEntries,
      dateRange: item.dateRange,

      isPreviousQuery: item.isPreviousQuery,
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
    ],
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

  return item.moved
    ? onDeleteNode(nextState, {
        andIdx: item.andIdx,
        orIdx: item.orIdx,
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

  return item.moved
    ? item.andIdx === andIdx
      ? onDeleteNode(nextState, {
          andIdx: item.andIdx,
          orIdx: item.orIdx + 1,
        })
      : onDeleteNode(nextState, {
          andIdx: item.andIdx,
          orIdx: item.orIdx,
        })
    : nextState;
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
  table: TableT,
) => {
  const node = state[andIdx].elements[orIdx];
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
  tables,
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

  if (!filters) return state;

  const filter = filters[filterIdx];

  const newTable: TableT = {
    ...table,
    filters: [
      ...filters.slice(0, filterIdx),
      {
        ...filter,
        ...properties,
      },
      ...filters.slice(filterIdx + 1),
    ],
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
  const table = state[andIdx].elements[orIdx].tables[tableIdx];
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
  const table = state[andIdx].elements[orIdx].tables[tableIdx];
  const { dateColumn } = table;

  // value contains the selects that have now been selected
  const newTable: TableT = {
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

const resetNodeAllFilters = (
  state: StandardQueryStateT,
  { andIdx, orIdx }: ActionType<typeof resetAllFilters>["payload"],
) => {
  const node = state[andIdx].elements[orIdx];

  const newState = setElementProperties(state, andIdx, orIdx, {
    excludeFromSecondaryIdQuery: false,
    excludeTimestamps: false,
    selects: nodeIsConceptQueryNode(node)
      ? selectsWithDefaults(node.selects)
      : [],
  });

  if (!nodeIsConceptQueryNode(node)) return newState;

  const tables = resetAllFiltersInTables(node.tables);

  return updateNodeTables(newState, andIdx, orIdx, tables);
};

const resetNodeTable = (
  state: StandardQueryStateT,
  { andIdx, orIdx, tableIdx }: ActionType<typeof resetTable>["payload"],
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
    tableWithDefaults(table),
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
  return setGroupProperties(state, andIdx, { dateRange: null });
};

const isRangeFilterConfig = (
  filter: FilterConfigT,
): filter is {
  filter: FilterIdT;
  value: RangeFilterValueT;
  type: "INTEGER_RANGE" | "REAL_RANGE" | "MONEY_RANGE";
} =>
  filter.type === "INTEGER_RANGE" ||
  filter.type === "REAL_RANGE" ||
  filter.type === "MONEY_RANGE";

const isMultiSelectFilterConfig = (
  filter: FilterConfigT,
): filter is {
  filter: FilterIdT;
  value: FilterIdT[];
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
  savedTable: TableT,
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
        // For BIG MULTI SELECT only, to be able to load all non-loaded options form the defaultValue later
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
  conceptOrTable?: QueryConceptNodeT | TableT,
) => {
  if (!conceptOrTable || !conceptOrTable.selects) {
    return savedSelects || null;
  }

  if (!savedSelects) return null;

  return savedSelects.map((select) => {
    const selectedSelect = conceptOrTable.selects.find(
      (id) => id === select.id,
    );

    return { ...select, selected: !!selectedSelect };
  });
};

const mergeDateColumn = (savedTable: TableT, table: TableT) => {
  if (!table || !table.dateColumn || !savedTable.dateColumn)
    return savedTable.dateColumn;

  return {
    ...savedTable.dateColumn,
    value: table.dateColumn.value,
  };
};

const mergeTables = (savedTables: TableT[], concept: QueryConceptNodeT) => {
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
  { tables, selects }: { tables: TableT[]; selects: SelectorT[] },
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
) => {
  switch (node.type) {
    case "OR":
      return {
        type: "OR",
        elements: node.children.map((c) =>
          expandNode(rootConcepts, c, expandErrorMessage),
        ),
      };
    case "SAVED_QUERY":
      return {
        ...node,
        id: node.query,
        isPreviousQuery: true,
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
      );

      if (!lookupResult)
        return {
          ...node,
          error: expandErrorMessage,
        };

      const { tables, selects } = mergeFromSavedConceptIntoNode(node, {
        tables: lookupResult.tables,
        selects: lookupResult.selects,
      });
      const label = node.label || lookupResult.concepts[0].label;
      const description = lookupResult.concepts[0].description;

      return {
        ...node,
        label,
        description,
        tables,
        selects,
        excludeTimestamps: node.excludeFromTimeAggregation,
        excludeFromSecondaryIdQuery: node.excludeFromSecondaryIdQuery,
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
}: ActionType<typeof expandPreviousQuery>["payload"]) => {
  return query.root.children.map((child) =>
    expandNode(rootConcepts, child, expandErrorMessage),
  );
};

const findPreviousQueries = (state: StandardQueryStateT, action: any) => {
  // Find all nodes that are previous queries and have the correct id
  const queries = state
    .map((group, andIdx) => {
      return group.elements
        .map((concept, orIdx) => ({ ...concept, orIdx }))
        .filter(
          (concept) =>
            concept.isPreviousQuery && concept.id === action.payload.queryId,
        )
        .map((concept) => ({
          andIdx,
          orIdx: concept.orIdx,
          node: objectWithoutKey("orIdx")(concept),
        }));
    })
    .filter((group) => group.length > 0);

  return [].concat.apply([], queries);
};

const updatePreviousQueries = (
  state: StandardQueryStateT,
  action: any,
  attributes: any,
) => {
  const queries = findPreviousQueries(state, action);

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

const loadPreviousQueryStart = (
  state: StandardQueryStateT,
  action: ActionType<typeof loadQuery.request>,
) => {
  return updatePreviousQueries(state, action, { loading: true });
};
const loadPreviousQuerySuccess = (
  state: StandardQueryStateT,
  action: ActionType<typeof loadQuery.success>,
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
const loadPreviousQueryError = (
  state: StandardQueryStateT,
  action: ActionType<typeof loadQuery.failure>,
) => {
  return updatePreviousQueries(state, action, {
    loading: false,
    error: action.payload.message,
  });
};
const onRenamePreviousQuery = (
  state: StandardQueryStateT,
  action: ActionType<typeof renameQuery.success>,
) => {
  return updatePreviousQueries(state, action, {
    loading: false,
    label: action.payload.label,
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
    excludeFromSecondaryIdQuery: !state[andIdx].elements[orIdx]
      .excludeFromSecondaryIdQuery,
  });
};

const loadFilterSuggestionsStart = (
  state: StandardQueryStateT,
  payload: ActionType<typeof loadFilterSuggestions.request>["payload"],
) => setNodeFilterProperties(state, payload, { isLoading: true });

const loadFilterSuggestionsSuccess = (
  state: StandardQueryStateT,
  {
    data,
    ...rest
  }: ActionType<typeof loadFilterSuggestions.success>["payload"],
) => {
  // When [] comes back from the API, don't touch the current options
  if (!data || data.length === 0) {
    return setNodeFilterProperties(state, rest, { isLoading: false });
  }

  return setNodeFilterProperties(state, rest, {
    isLoading: false,
    options: data,
  });
};

const loadFilterSuggestionsError = (
  state: StandardQueryStateT,
  payload: ActionType<typeof loadFilterSuggestions.failure>["payload"],
) => setNodeFilterProperties(state, payload, { isLoading: false });

const createQueryNodeFromConceptListUploadResult = (
  label: string,
  rootConcepts: TreesT,
  resolvedConcepts: ConceptIdT[],
): DragItemConceptTreeNode | null => {
  const lookupResult = getConceptsByIdsWithTablesAndSelects(
    rootConcepts,
    resolvedConcepts,
  );

  return lookupResult
    ? {
        type: "CONCEPT_TREE_NODE",
        height: 0,
        width: 0,
        label,
        ids: resolvedConcepts,
        tables: lookupResult.tables,
        selects: lookupResult.selects,
        tree: lookupResult.root,
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

// -----------------------------
// TODO: Figure out, whether we ever want to
//       include subnodes in the reguar query editor
//       => If we do, use this method, if we don't remove it
// -----------------------------
//
// const toggleIncludeSubnodes = (state: StateType, { andIdx, orIdx }: Object) => {
//   const { includeSubnodes } = action.payload;

//   const node = state[andIdx].elements[orIdx];
//   const concept = getConceptById(node.ids);

//   const childIds = [];
//   const elements = concept.children.map(childId => {
//     const child = getConceptById(childId);

//     childIds.push(childId);

//     return {
//       ids: [childId],
//       label: child.label,
//       description: child.description,
//       tables: node.tables,
//       selects: node.selects,
//       tree: node.tree
//     };
//   });

//   const groupProps = {
//     elements: [
//       ...state[andIdx].elements.slice(0, orIdx),
//       {
//         ...state[andIdx].elements[orIdx],
//         includeSubnodes
//       },
//       ...state[andIdx].elements.slice(orIdx + 1)
//     ]
//   };

//   if (includeSubnodes) {
//     groupProps.elements.push(...elements);
//   } else {
//     groupProps.elements = groupProps.elements.filter(element => {
//       return !(difference(element.ids, childIds).length === 0);
//     });
//   }

//   return setGroupProperties(state, andIdx, groupProps);
// };

// -----------------------------

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
    case getType(resetAllFilters):
      return resetNodeAllFilters(state, action.payload);
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
    case getType(loadQuery.request):
      return loadPreviousQueryStart(state, action);
    case getType(loadQuery.success):
      return loadPreviousQuerySuccess(state, action);
    case getType(loadQuery.failure):
      return loadPreviousQueryError(state, action);
    case getType(renameQuery.success):
      return onRenamePreviousQuery(state, action);
    case getType(loadFilterSuggestions.request):
      return loadFilterSuggestionsStart(state, action.payload);
    case getType(loadFilterSuggestions.success):
      return loadFilterSuggestionsSuccess(state, action.payload);
    case getType(loadFilterSuggestions.failure):
      return loadFilterSuggestionsError(state, action.payload);
    case getType(acceptQueryUploadConceptListModal):
      return insertUploadedConceptList(state, action.payload);
    case getType(setDateColumn):
      return setNodeTableDateColumn(state, action.payload);
    default:
      return state;
  }
};

export default query;
