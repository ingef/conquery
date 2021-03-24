import { getConceptsByIdsWithTablesAndSelects } from "../concept-trees/globalTreeStoreHelper";

import { isEmpty, objectWithoutKey } from "../common/helpers";
import { exists } from "../common/helpers/exists";

import type {
  AndQueryT,
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
} from "../api/types";

import { resetAllFiltersInTables } from "../model/table";
import { selectsWithDefaults } from "../model/select";

import {
  QUERY_GROUP_MODAL_SET_DATE,
  QUERY_GROUP_MODAL_RESET_ALL_DATES,
} from "../query-group-modal/actionTypes";

import {
  LOAD_PREVIOUS_QUERY_START,
  LOAD_PREVIOUS_QUERY_SUCCESS,
  LOAD_PREVIOUS_QUERY_ERROR,
  RENAME_PREVIOUS_QUERY_SUCCESS,
} from "../previous-queries/list/actionTypes";

import { MODAL_ACCEPT as QUERY_UPLOAD_CONCEPT_LIST_MODAL_ACCEPT } from "../query-upload-concept-list-modal/actionTypes";

import type { TreesT } from "../concept-trees/reducer";

import {
  DROP_AND_NODE,
  DROP_OR_NODE,
  DELETE_NODE,
  DELETE_GROUP,
  TOGGLE_EXCLUDE_GROUP,
  LOAD_QUERY,
  CLEAR_QUERY,
  EXPAND_PREVIOUS_QUERY,
  SELECT_NODE_FOR_EDITING,
  DESELECT_NODE,
  UPDATE_NODE_LABEL,
  ADD_CONCEPT_TO_NODE,
  REMOVE_CONCEPT_FROM_NODE,
  TOGGLE_TABLE,
  SET_FILTER_VALUE,
  SET_TABLE_SELECTS,
  SET_SELECTS,
  RESET_ALL_FILTERS,
  SWITCH_FILTER_MODE,
  TOGGLE_TIMESTAMPS,
  TOGGLE_SECONDARY_ID_EXCLUDE,
  LOAD_FILTER_SUGGESTIONS_START,
  LOAD_FILTER_SUGGESTIONS_SUCCESS,
  LOAD_FILTER_SUGGESTIONS_ERROR,
  SET_DATE_COLUMN,
} from "./actionTypes";

import type {
  QueryNodeType,
  DraggedNodeType,
  DraggedQueryType,
  QueryGroupType,
} from "./types";
import { isMultiSelectFilter } from "../model/filter";

export type StandardQueryStateT = QueryGroupType[];

const initialState: StandardQueryStateT = [];

const filterItem = (
  item: DraggedNodeType | DraggedQueryType
): QueryNodeType => {
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

const setGroupProperties = (node, andIdx, properties) => {
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
  properties: Partial<QueryNodeType>
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

const setAllElementsProperties = (node, properties) => {
  return node.map((group) => ({
    ...group,
    elements: group.elements.map((element) => ({
      ...element,
      ...properties,
    })),
  }));
};

const dropAndNode = (
  state: StandardQueryStateT,
  action: {
    payload: {
      item: DraggedNodeType | DraggedQueryType;
    };
  }
) => {
  const group = state[state.length - 1];
  const dateRangeOfLastGroup = group ? group.dateRange : null;
  const { item } = action.payload;

  const nextState = [
    ...state,
    {
      elements: [filterItem(item)],
      dateRange: dateRangeOfLastGroup,
    },
  ];

  return item.moved
    ? deleteNode(nextState, {
        payload: { andIdx: item.andIdx, orIdx: item.orIdx },
      })
    : nextState;
};

const dropOrNode = (
  state: StandardQueryStateT,
  action: {
    payload: {
      item: DraggedNodeType | DraggedQueryType;
      andIdx: number;
    };
  }
) => {
  const { item, andIdx } = action.payload;

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
      ? deleteNode(nextState, {
          payload: { andIdx: item.andIdx, orIdx: item.orIdx + 1 },
        })
      : deleteNode(nextState, {
          payload: { andIdx: item.andIdx, orIdx: item.orIdx },
        })
    : nextState;
};

// Delete a single Node (concept inside a group)
const deleteNode = (
  state: StandardQueryStateT,
  action: { payload: { andIdx: number; orIdx: number } }
) => {
  const { andIdx, orIdx } = action.payload;

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

const deleteGroup = (state: StandardQueryStateT, action: any) => {
  const { andIdx } = action.payload;

  return [...state.slice(0, andIdx), ...state.slice(andIdx + 1)];
};

const toggleExcludeGroup = (state: StandardQueryStateT, action: any) => {
  const { andIdx } = action.payload;

  return [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      exclude: state[andIdx].exclude ? undefined : true,
    },
    ...state.slice(andIdx + 1),
  ];
};

const loadQuery = (state: StandardQueryStateT, action: any) => {
  // In case there is no query, keep state the same
  if (!action.payload.query) return state;

  return action.payload.query;
};

const updateNodeTable = (
  state: StandardQueryStateT,
  andIdx: number,
  orIdx: number,
  tableIdx: number,
  table
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
  tables
) => {
  return setElementProperties(state, andIdx, orIdx, { tables });
};

const toggleNodeTable = (state: StandardQueryStateT, action: any) => {
  const { tableIdx, isExcluded } = action.payload;

  const nodePosition = selectEditedNodePosition(state);
  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;
  const node = state[andIdx].elements[orIdx];
  const table = {
    ...node.tables[tableIdx],
    exclude: isExcluded,
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, table);
};

const selectEditedNodePosition = (state: StandardQueryStateT) => {
  for (let andIdx = 0; andIdx < state.length; andIdx++) {
    for (let orIdx = 0; orIdx < state[andIdx].elements.length; orIdx++) {
      const node = state[andIdx].elements[orIdx];

      if (node.isEditing) {
        return { andIdx, orIdx };
      }
    }
  }

  return null;
};

const setNodeFilterProperties = (state, action, properties) => {
  const { tableIdx, filterIdx } = action.payload;

  const node = selectEditedNodePosition(state);

  if (!node) return state;

  const { andIdx, orIdx } = node;
  const table = state[andIdx].elements[orIdx].tables[tableIdx];
  const { filters } = table;

  if (!filters) return state;

  const filter = filters[filterIdx];

  const newTable = {
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

const setNodeFilterValue = (state: StandardQueryStateT, action: any) => {
  const { value } = action.payload;

  return setNodeFilterProperties(state, action, { value });
};

const setNodeTableSelects = (state: StandardQueryStateT, action: any) => {
  const { tableIdx, value } = action.payload;
  const { andIdx, orIdx } = selectEditedNodePosition(state);
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

const setNodeTableDateColumn = (state: StandardQueryStateT, action: any) => {
  const { tableIdx, value } = action.payload;
  const { andIdx, orIdx } = selectEditedNodePosition(state);
  const table = state[andIdx].elements[orIdx].tables[tableIdx];
  const { dateColumn } = table;

  // value contains the selects that have now been selected
  const newTable = {
    ...table,
    dateColumn: {
      ...dateColumn,
      value,
    },
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, newTable);
};

const setNodeSelects = (state: StandardQueryStateT, action: any) => {
  const { value } = action.payload;
  const { andIdx, orIdx } = selectEditedNodePosition(state);
  const { selects } = state[andIdx].elements[orIdx];

  return setElementProperties(state, andIdx, orIdx, {
    selects: selects.map((select) => ({
      ...select,
      selected:
        !!value &&
        !!value.find((selectedValue) => selectedValue.value === select.id),
    })),
  });
};

const switchNodeFilterMode = (state: StandardQueryStateT, action: any) => {
  const { mode } = action.payload;

  return setNodeFilterProperties(state, action, {
    mode,
    value: null,
  });
};

const resetNodeAllFilters = (state: StandardQueryStateT, action: any) => {
  const nodeIdx = selectEditedNodePosition(state);
  if (!nodeIdx) return state;

  const { andIdx, orIdx } = nodeIdx;
  const node = state[andIdx].elements[orIdx];

  const newState = setElementProperties(state, andIdx, orIdx, {
    excludeTimestamps: false,
    selects: selectsWithDefaults(node.selects),
  });

  if (!node.tables) return newState;

  const tables = resetAllFiltersInTables(node.tables);

  return updateNodeTables(newState, andIdx, orIdx, tables);
};

const setGroupDate = (state: StandardQueryStateT, action: any) => {
  const { andIdx, date } = action.payload;

  return setGroupProperties(state, andIdx, { dateRange: date });
};

const resetGroupDates = (state: StandardQueryStateT, action: any) => {
  const { andIdx } = action.payload;

  return setGroupProperties(state, andIdx, { dateRange: null });
};

const isRangeFilterConfig = (
  filter: FilterConfigT
): filter is {
  filter: FilterIdT;
  value: RangeFilterValueT;
  type: "INTEGER_RANGE" | "REAL_RANGE" | "MONEY_RANGE";
} =>
  filter.type === "INTEGER_RANGE" ||
  filter.type === "REAL_RANGE" ||
  filter.type === "MONEY_RANGE";

const isMultiSelectFilterConfig = (
  filter: FilterConfigT
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
  table?: TableConfigT
) => {
  if (!table || !table.filters) return savedTable.filters || null;

  if (!savedTable.filters) return null;

  return savedTable.filters.map((filter) => {
    // TODO: Improve the api and don't use `.filter`, but `.id` or `.filterId`
    const matchingFilter = table.filters!.find((f) => f.filter === filter.id);

    if (!matchingFilter) {
      return filter;
    }

    if (isRangeFilterConfig(matchingFilter)) {
      const filterDetails =
        matchingFilter.value &&
        !isEmpty(matchingFilter.value.min) &&
        !isEmpty(matchingFilter.value.max) &&
        matchingFilter.value.min === matchingFilter.value.max
          ? { mode: "exact", value: { exact: matchingFilter.value.min } }
          : { mode: "range", value: matchingFilter.value };

      return { ...filter, ...filterDetails };
    }

    if (isMultiSelectFilterConfig(matchingFilter)) {
      const filterDetails = {
        ...matchingFilter,
        type: filter.type, // matchingFilter.type is sometimes wrongly saying MULTI_SELECT
        value: matchingFilter.value
          .map((val) => {
            if (!isMultiSelectFilter(filter)) {
              console.error(
                `Filter: ${filter} is not a multi-select filter, even though its matching filter was: ${matchingFilter}`
              );
              return val;
            } else {
              // There is the possibility, that we have a BIG_MULTI_SELECT that loads options async.
              // Then filter.options would be empty and we wouldn't find it
              return filter.options.find((op) => op.value === val);
            }
          })
          .filter(exists),
        // For BIG MULTI SELECT only, to be able to load all non-loaded options form the defaultValue later
        defaultValue: matchingFilter.value.filter((val) => {
          if (!isMultiSelectFilter(filter)) {
            console.error(
              `Filter: ${filter} is not a multi-select filter, even though its matching filter was: ${matchingFilter}`
            );
            return false;
          }

          return !exists(filter.options.find((opt) => opt.value === val));
        }),
      };

      return { ...filter, ...filterDetails };
    }

    return { ...filter, ...matchingFilter };
  });
};

const mergeSelects = (
  savedSelects?: SelectorT[],
  conceptOrTable?: QueryConceptNodeT | TableT
) => {
  if (!conceptOrTable || !conceptOrTable.selects) {
    return savedSelects || null;
  }

  if (!savedSelects) return null;

  return savedSelects.map((select) => {
    const selectedSelect = conceptOrTable.selects.find(
      (id) => id === select.id
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
          (t) => t.id === savedTable.connectorId
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
  { tables, selects }: { tables: TableT[]; selects: SelectorT[] }
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
  expandErrorMessage: string
) => {
  switch (node.type) {
    case "OR":
      return {
        type: "OR",
        elements: node.children.map((c) =>
          expandNode(rootConcepts, c, expandErrorMessage)
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
        node.ids
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
const expandPreviousQuery = (action: {
  payload: {
    rootConcepts: TreesT;
    query: AndQueryT;
    expandErrorMessage: string;
  };
}) => {
  const { rootConcepts, query, expandErrorMessage } = action.payload;

  return query.root.children.map((child) =>
    expandNode(rootConcepts, child, expandErrorMessage)
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
            concept.isPreviousQuery && concept.id === action.payload.queryId
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
  attributes: any
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

const loadPreviousQueryStart = (state: StandardQueryStateT, action: any) => {
  return updatePreviousQueries(state, action, { loading: true });
};
const loadPreviousQuerySuccess = (state: StandardQueryStateT, action: any) => {
  const label = action.payload.data.label
    ? { label: action.payload.data.label }
    : {};

  return updatePreviousQueries(state, action, {
    ...label,
    id: action.payload.data.id,
    loading: false,
    query: action.payload.data.query,
    canExpand: action.payload.data.canExpand,
    availableSecondaryIds: action.payload.data.availableSecondaryIds,
  });
};
const loadPreviousQueryError = (state: StandardQueryStateT, action: any) => {
  return updatePreviousQueries(state, action, {
    loading: false,
    error: action.payload.message,
  });
};
const renamePreviousQuery = (state: StandardQueryStateT, action: any) => {
  return updatePreviousQueries(state, action, {
    loading: false,
    label: action.payload.label,
  });
};

function getPositionFromActionOrEditedNode(
  state: StandardQueryStateT,
  action: any
) {
  const { andIdx, orIdx } = action.payload;

  if (exists(andIdx) && exists(orIdx)) {
    return { andIdx, orIdx };
  }

  return selectEditedNodePosition(state);
}

const toggleTimestamps = (state: StandardQueryStateT, action: any) => {
  const { andIdx, orIdx } = getPositionFromActionOrEditedNode(state, action);

  return setElementProperties(state, andIdx, orIdx, {
    excludeTimestamps: !state[andIdx].elements[orIdx].excludeTimestamps,
  });
};

const toggleSecondaryIdExclude = (state: StandardQueryStateT, action: any) => {
  const { andIdx, orIdx } = getPositionFromActionOrEditedNode(state, action);

  return setElementProperties(state, andIdx, orIdx, {
    excludeFromSecondaryIdQuery: !state[andIdx].elements[orIdx]
      .excludeFromSecondaryIdQuery,
  });
};

const loadFilterSuggestionsStart = (state: StandardQueryStateT, action: any) =>
  setNodeFilterProperties(state, action, { isLoading: true });

const loadFilterSuggestionsSuccess = (
  state: StandardQueryStateT,
  action: any
) => {
  // When [] comes back from the API, don't touch the current options
  if (!action.payload.data || action.payload.data.length === 0)
    return setNodeFilterProperties(state, action, { isLoading: false });

  return setNodeFilterProperties(state, action, {
    isLoading: false,
    options: action.payload.data,
  });
};

const loadFilterSuggestionsError = (state: StandardQueryStateT, action: any) =>
  setNodeFilterProperties(state, action, { isLoading: false });

const createQueryNodeFromConceptListUploadResult = (
  label,
  rootConcepts,
  resolvedConcepts
): DraggedNodeType => {
  const lookupResult = getConceptsByIdsWithTablesAndSelects(
    rootConcepts,
    resolvedConcepts
  );

  return lookupResult
    ? {
        label,
        ids: resolvedConcepts,
        tables: lookupResult.tables,
        selects: lookupResult.selects,
        tree: lookupResult.root,
      }
    : null;
};

const insertUploadedConceptList = (state: StandardQueryStateT, action: any) => {
  const { label, rootConcepts, resolvedConcepts, andIdx } = action.payload;

  const queryElement = createQueryNodeFromConceptListUploadResult(
    label,
    rootConcepts,
    resolvedConcepts
  );

  if (!queryElement) return state;

  return andIdx === null
    ? dropAndNode(state, {
        payload: { item: queryElement },
      })
    : dropOrNode(state, {
        payload: { andIdx, item: queryElement },
      });
};

const selectNodeForEditing = (
  state: StandardQueryStateT,
  { payload: { andIdx, orIdx } }: any
) => {
  return setElementProperties(state, andIdx, orIdx, { isEditing: true });
};

const updateNodeLabel = (state: StandardQueryStateT, action: any) => {
  const node = selectEditedNodePosition(state);

  if (!node) return state;

  const { andIdx, orIdx } = node;

  return setElementProperties(state, andIdx, orIdx, {
    label: action.payload.label,
  });
};

const addConceptToNode = (state: StandardQueryStateT, action: any) => {
  const nodePosition = selectEditedNodePosition(state);

  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;
  const node = state[andIdx].elements[orIdx];

  return setElementProperties(state, andIdx, orIdx, {
    ids: [...action.payload.concept.ids, ...node.ids],
  });
};

const removeConceptFromNode = (state: StandardQueryStateT, action: any) => {
  const nodePosition = selectEditedNodePosition(state);

  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;
  const node = state[andIdx].elements[orIdx];

  return setElementProperties(state, andIdx, orIdx, {
    ids: node.ids.filter((id) => id !== action.payload.conceptId),
  });
};

// -----------------------------
// TODO: Figure out, whether we ever want to
//       include subnodes in the reguar query editor
//       => If we do, use this method, if we don't remove it
// -----------------------------
//
// const toggleIncludeSubnodes = (state: StateType, action: Object) => {
//   const { includeSubnodes } = action.payload;

//   const nodePosition = selectEditedNodePosition(state);

//   if (!nodePosition) return state;

//   const { andIdx, orIdx } = nodePosition;
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
  action: any
): StandardQueryStateT => {
  switch (action.type) {
    case DROP_AND_NODE:
      return dropAndNode(state, action);
    case DROP_OR_NODE:
      return dropOrNode(state, action);
    case DELETE_NODE:
      return deleteNode(state, action);
    case DELETE_GROUP:
      return deleteGroup(state, action);
    case TOGGLE_EXCLUDE_GROUP:
      return toggleExcludeGroup(state, action);
    case LOAD_QUERY:
      return loadQuery(state, action);
    case CLEAR_QUERY:
      return initialState;
    case SELECT_NODE_FOR_EDITING:
      return selectNodeForEditing(state, action);
    case DESELECT_NODE:
      return setAllElementsProperties(state, { isEditing: false });
    case UPDATE_NODE_LABEL:
      return updateNodeLabel(state, action);
    case ADD_CONCEPT_TO_NODE:
      return addConceptToNode(state, action);
    case REMOVE_CONCEPT_FROM_NODE:
      return removeConceptFromNode(state, action);
    case TOGGLE_TABLE:
      return toggleNodeTable(state, action);
    case SET_FILTER_VALUE:
      return setNodeFilterValue(state, action);
    case SET_TABLE_SELECTS:
      return setNodeTableSelects(state, action);
    case SET_SELECTS:
      return setNodeSelects(state, action);
    case RESET_ALL_FILTERS:
      return resetNodeAllFilters(state, action);
    case SWITCH_FILTER_MODE:
      return switchNodeFilterMode(state, action);
    case TOGGLE_TIMESTAMPS:
      return toggleTimestamps(state, action);
    case TOGGLE_SECONDARY_ID_EXCLUDE:
      return toggleSecondaryIdExclude(state, action);
    case QUERY_GROUP_MODAL_SET_DATE:
      return setGroupDate(state, action);
    case QUERY_GROUP_MODAL_RESET_ALL_DATES:
      return resetGroupDates(state, action);
    case EXPAND_PREVIOUS_QUERY:
      return expandPreviousQuery(action);
    case LOAD_PREVIOUS_QUERY_START:
      return loadPreviousQueryStart(state, action);
    case LOAD_PREVIOUS_QUERY_SUCCESS:
      return loadPreviousQuerySuccess(state, action);
    case LOAD_PREVIOUS_QUERY_ERROR:
      return loadPreviousQueryError(state, action);
    case RENAME_PREVIOUS_QUERY_SUCCESS:
      return renamePreviousQuery(state, action);
    case LOAD_FILTER_SUGGESTIONS_START:
      return loadFilterSuggestionsStart(state, action);
    case LOAD_FILTER_SUGGESTIONS_SUCCESS:
      return loadFilterSuggestionsSuccess(state, action);
    case LOAD_FILTER_SUGGESTIONS_ERROR:
      return loadFilterSuggestionsError(state, action);
    case QUERY_UPLOAD_CONCEPT_LIST_MODAL_ACCEPT:
      return insertUploadedConceptList(state, action);
    case SET_DATE_COLUMN:
      return setNodeTableDateColumn(state, action);
    default:
      return state;
  }
};

export default query;
