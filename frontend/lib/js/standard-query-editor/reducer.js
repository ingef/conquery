// @flow

import T from 'i18n-react';

import {
  getConceptsByIdsWithTables
} from '../category-trees/globalTreeStoreHelper';

import {
  isEmpty,
  stripObject
} from '../common/helpers';

import {
  type DateRangeType
} from '../common/types/backend'

import {
  resetAllFiltersInTables
} from '../model/table';

import {
  QUERY_GROUP_MODAL_SET_MIN_DATE,
  QUERY_GROUP_MODAL_SET_MAX_DATE,
  QUERY_GROUP_MODAL_RESET_ALL_DATES,
} from '../query-group-modal/actionTypes';

import {
  LOAD_PREVIOUS_QUERY_START,
  LOAD_PREVIOUS_QUERY_SUCCESS,
  LOAD_PREVIOUS_QUERY_ERROR,
  RENAME_PREVIOUS_QUERY_SUCCESS,
} from '../previous-queries/list/actionTypes';

import {
  UPLOAD_CONCEPT_LIST_MODAL_ACCEPT,
  type UploadConceptListModalResultType
} from '../upload-concept-list-modal/actionTypes'

import {
  INTEGER_RANGE
} from '../form-components';

import type { StateType } from '../query-runner/reducer';

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
  RESET_ALL_FILTERS,
  SWITCH_FILTER_MODE,
  TOGGLE_TIMESTAMPS,
  LOAD_FILTER_SUGGESTIONS_START,
  LOAD_FILTER_SUGGESTIONS_SUCCESS,
  LOAD_FILTER_SUGGESTIONS_ERROR,
  SET_RESOLVED_FILTER_VALUES,
} from './actionTypes';

import type
{
  QueryNodeType,
  QueryGroupType,
  StandardQueryType,
  DraggedNodeType,
  DraggedQueryType
} from './types';


const initialState: StandardQueryType = [];


const filterItem = (item: DraggedNodeType | DraggedQueryType): QueryNodeType => {
  // This sort of mapping might be a problem when adding new optional properties to
  // either Nodes or Queries: Flow won't complain when we omit those optional
  // properties here. But we can't use a spread operator either...

  if (item.isPreviousQuery)
    return {
      label: item.label,
      excludeTimestamps: item.excludeTimestamps,
      loading: item.loading,
      error: item.error,

      id: item.id,
      // eslint-disable-next-line no-use-before-define
      query: item.query,
      isPreviousQuery: item.isPreviousQuery,
    };
  else
    return {
      ids: item.ids,
      tables: item.tables,
      tree: item.tree,

      label: item.label,
      excludeTimestamps: item.excludeTimestamps,
      loading: item.loading,
      error: item.error,

      isPreviousQuery: item.isPreviousQuery,
    }
};

const setGroupProperties = (node, andIdx, properties) => {
  return [
    ...node.slice(0, andIdx),
    {
      ...node[andIdx],
      ...properties
    },
    ...node.slice(andIdx + 1)
  ];
};

const setElementProperties = (node, andIdx, orIdx, properties) => {
  const groupProperties = {
    elements: [
      ...node[andIdx].elements.slice(0, orIdx),
      {
        ...node[andIdx].elements[orIdx],
        ...properties
      },
      ...node[andIdx].elements.slice(orIdx + 1)
    ]
  };

  return setGroupProperties(node, andIdx, groupProperties);
};

const setAllElementsProperties = (node, properties) => {
  return node.map(group => ({
    ...group,
    elements: group.elements.map(element => ({
      ...element,
      ...properties
    }))
  }));
};

const dropAndNode = (
  state,
  action: {
    payload: {
      item: DraggedNodeType | DraggedQueryType,
      dateRange?: DateRangeType
    }
  }
) => {
  const group = state[state.length - 1];
  const dateRangeOfLastGroup = (group ? group.dateRange : null);
  const {item, dateRange = dateRangeOfLastGroup} = action.payload;

  const nextState = [
    ...state,
    {
      elements: [filterItem(item)],
      dateRange: dateRange
    },
  ];

  return item.moved
    ? deleteNode(nextState, { payload: { andIdx: item.andIdx, orIdx: item.orIdx } })
    : nextState;
};

const dropOrNode = (
  state,
  action: {
    payload: {
      item: DraggedNodeType | DraggedQueryType,
      andIdx: number
    }
  }
) => {
  const { item, andIdx } = action.payload;

  const nextState = [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      elements: [
        filterItem(item),
        ...state[andIdx].elements,
      ]
    },
    ...state.slice(andIdx + 1)
  ];

  return item.moved
    ? item.andIdx === andIdx
      ? deleteNode(nextState, { payload: { andIdx: item.andIdx, orIdx: item.orIdx + 1 } })
      : deleteNode(nextState, { payload: { andIdx: item.andIdx, orIdx: item.orIdx } })
    : nextState;
};

// Delete a single Node (concept inside a group)
const deleteNode = (state, action: { payload: { andIdx: number, orIdx: number } }) => {
  const { andIdx, orIdx } = action.payload;

  return [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      elements: [
        ...state[andIdx].elements.slice(0, orIdx),
        ...state[andIdx].elements.slice(orIdx + 1),
      ]
    },
    ...state.slice(andIdx + 1)
  ].filter(and => !!and.elements && and.elements.length > 0);
};

const deleteGroup = (state, action) => {
  const { andIdx } = action.payload;

  return [
    ...state.slice(0, andIdx),
    ...state.slice(andIdx + 1)
  ];
};

const toggleExcludeGroup = (state, action) => {
  const { andIdx } = action.payload;

  return [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      exclude: state[andIdx].exclude ? undefined : true
    },
    ...state.slice(andIdx + 1)
  ];
};

const loadQuery = (state, action) => {
  // In case there is no query, keep state the same
  if (!action.payload.query) return state;

  return action.payload.query;
};

const updateNodeTable = (state, andIdx, orIdx, tableIdx, table) => {
  const node = state[andIdx].elements[orIdx];
  const tables = [
    ...node.tables.slice(0, tableIdx),
    table,
    ...node.tables.slice(tableIdx + 1),
  ];

  return updateNodeTables(state, andIdx, orIdx, tables);
};

const updateNodeTables = (state, andIdx, orIdx, tables) => {
  return setElementProperties(state, andIdx, orIdx, { tables });
};

const toggleNodeTable = (state, action) => {
  const { tableIdx, isExcluded } = action.payload;

  const nodePosition = selectEditedNode(state);
  if (!nodePosition) return state;

  const {andIdx, orIdx} = nodePosition;
  const node = state[andIdx].elements[orIdx];
  const table = {
    ...node.tables[tableIdx],
    exclude: isExcluded
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, table);
};

const selectEditedNode = (state) => {
  const selectedNodes = state
    .reduce((acc, group, andIdx) =>
      [...acc, ...group.elements.map((element, orIdx) => ({andIdx, orIdx, element}))], [])
    .filter(({element}) => element.isEditing)
    .map(({andIdx, orIdx}) => ({andIdx, orIdx}));

  return selectedNodes.length ? selectedNodes[0] : null;
}

const setNodeFilterProperties = (state, action, obj) => {
  const { tableIdx, filterIdx } = action.payload;

  const node = selectEditedNode(state);
  if (!node) return state;

  const { andIdx, orIdx } = node;
  const table = state[andIdx].elements[orIdx].tables[tableIdx];
  const { filters } = table;

  if (!filters) return state;

  const filter = filters[filterIdx];

  // Go through the keys and set them to undefined if they're empty values
  // or empty objects
  const properties = stripObject(obj);

  if ('options' in properties) {
    // Options are only updated in the context of autocompletion.
    // In this case we don't want to replace the existing options but update
    // them with the new list, removing duplicates
    const previousOptions = filter.options || [];
    // The properties object contains an 'options' key, but its value might
    // be undefined (because of stripObject above)
    const newOptions = (properties.options || []);
    properties.options = newOptions
      .concat(previousOptions)
      .reduce(
        (options, currentOption) =>
          options.find(x => x.value === currentOption.value)
            ? options
            : [...options, currentOption],
        []
      );
  }

  if (properties.value === undefined && filter.defaultValue)
      properties.value = filter.defaultValue;

  const newTable = {
    ...table,
    filters: [
      ...filters.slice(0, filterIdx),
      {
        ...filter,
        ...properties
      },
      ...filters.slice(filterIdx + 1),
    ]
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, newTable);
};

const setNodeFilterValue = (state, action) => {
  const { value, formattedValue, options } = action.payload;

  return setNodeFilterProperties(state, action, { value, formattedValue, options });
};

const switchNodeFilterMode = (state, action) => {
  const { mode } = action.payload;

  return setNodeFilterProperties(state, action, {
    mode,
    value: null,
    formattedValue: null,
  });
};

const resetNodeAllFilters = (state, action) => {
  const nodeIdx = selectEditedNode(state);
  if (!nodeIdx) return state;

  const { andIdx, orIdx } = nodeIdx;
  const node = state[andIdx].elements[orIdx];

  const newState = setElementProperties(state, andIdx, orIdx, { excludeTimestamps: false });

  if (!node.tables) return newState;

  const tables = resetAllFiltersInTables(node.tables);
  return updateNodeTables(newState, andIdx, orIdx, tables);
};

const setGroupDate = (state, action, minOrMax) => {
  const { andIdx, date } = action.payload;

  // Calculate next daterange
  const tmpDateRange = {
    ...state[andIdx].dateRange,
    [minOrMax]: date
  };
  // Make sure it has either min or max set, otherwise "delete" the key
  // by setting to undefined
  const dateRange = (tmpDateRange.min || tmpDateRange.max)
    ? tmpDateRange
    : undefined;

  return setGroupProperties(state, andIdx, { dateRange });
};

const resetGroupDates = (state, action) => {
  const { andIdx } = action.payload;

  return setGroupProperties(state, andIdx, { dateRange: null });
};

// Merges filter values from `table` into declared filters from `savedTable`
//
// `savedTable` may define filters, but it won't have any filter values,
// since `savedTables` comes from a `savedConcept` in a `categoryTree`. Such a
// `savedConcept` is never modified and only declares possible filters.
// Since `table` comes from a previous query, it may have set filter values
// if so, we will need to merge them in.
const mergeFiltersFromSavedConcept = (savedTable, table) => {
  if (!table || !table.filters) return savedTable.filters || null;

  if (!savedTable.filters) return null;

  return table.filters.map(filter => {
    const tableFilter = savedTable.filters.find(f => f.id === filter.id) || {};
    const mode = tableFilter.type === INTEGER_RANGE
      ? tableFilter.value && !isEmpty(tableFilter.value.exact)
        ? { mode: 'exact' }
        : { mode: 'range' }
      : {}

    return {
      ...filter,
      ...tableFilter, // => this one may contain a "value" property
      ...mode
    };
  })
}

// Look for tables in the already savedConcept. If they were not included in the
// respective query concept, exclude them.
// Also, apply all necessary filters
const mergeTablesFromSavedConcept = (savedConcept, concept) => {
  return savedConcept.tables
    ? savedConcept.tables.map(savedTable => {
        // Find corresponding table in previous queryObject
        const table = concept.tables.find(t => t.id === savedTable.id);
        const filters = mergeFiltersFromSavedConcept(savedTable, table);

        return {
          ...savedTable,
          exclude: !table,
          filters
        };
      })
   : [];
};

// Completely override all groups in the editor with the previous groups, but
// a) merge elements with concept data from category trees (esp. "tables")
// b) load nested previous queries contained in that query,
//    so they can also be expanded
const expandPreviousQuery = (state, action: { payload: { groups: QueryGroupType[] } }) => {
  const { rootConcepts, groups } = action.payload;

  return groups.map((group) => {
    return {
      ...group,
      elements: group.elements.map((element) => {
        if (element.type === 'QUERY') {
          return {
            ...element,
            isPreviousQuery: true
          };
        } else {
          const convertConceptToConceptList = element.type === 'CONCEPT';
          const ids = element.ids || [element.id];
          const lookupResult = getConceptsByIdsWithTables(ids, rootConcepts);

          if (!lookupResult)
            return {
              ...element,
              error: T.translate('queryEditor.couldNotExpandNode')
            };

          const tables = mergeTablesFromSavedConcept(lookupResult, element);

          const label = convertConceptToConceptList
            ? lookupResult.concepts[0].label
            : element.label;

          return {
            ...element,
            label,
            ids,
            tables,
            tree: lookupResult.root
          };
        }
      })
    }
  });
};

const findPreviousQueries = (state, action) => {
  // Find all nodes that are previous queries and have the correct id
  const queries = state
    .map((group, andIdx) => {
      return group.elements
        .map((concept, orIdx) => ({ ...concept, orIdx }))
        .filter(concept => concept.isPreviousQuery && concept.id === action.payload.queryId)
        .map(concept => ({
          andIdx,
          orIdx: concept.orIdx,
          node: concept,
        }));
    })
    .filter(group => group.length > 0)

  return [].concat.apply([], queries);
};

const updatePreviousQueries = (state, action, attributes) => {
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
            ...attributes
          },
          ...nextState[andIdx].elements.slice(orIdx + 1)
        ]
      },
      ...nextState.slice(andIdx + 1)
    ];
  }, state);
};

const loadPreviousQueryStart = (state, action) => {
  return updatePreviousQueries(state, action, { loading: true });
};
const loadPreviousQuerySuccess = (state, action) => {
  const label = action.payload.data.label
    ? { label: action.payload.data.label }
    : {};

  return updatePreviousQueries(state, action, {
    ...label,
    id: action.payload.data.id,
    loading: false,
    query: action.payload.data.query
  });
};
const loadPreviousQueryError = (state, action) => {
  return updatePreviousQueries(state, action, { loading: false, error: action.payload.message });
};
const renamePreviousQuery = (state, action) => {
  return updatePreviousQueries(state, action, { loading: false, label: action.payload.label });
};

const toggleTimestamps = (state, action) => {
  const { isExcluded } = action.payload;

  const nodePosition = selectEditedNode(state);
  if (!nodePosition) return state;

  const {andIdx, orIdx} = nodePosition;

  return setElementProperties(state, andIdx, orIdx, { excludeTimestamps: isExcluded });
};

const loadFilterSuggestionsStart = (state, action) =>
  setNodeFilterProperties(state, action, { isLoading: true });

const loadFilterSuggestionsSuccess = (state, action) =>
  setNodeFilterProperties(state, action, {
    isLoading: false,
    options: action.payload.suggestions
  });

const loadFilterSuggestionsError = (state, action) =>
  setNodeFilterProperties(state, action, { isLoading: false, options: [] });

const createQueryNodeFromConceptListUploadResult = (
    result: UploadConceptListModalResultType
  ) : DraggedNodeType => {
  const { label, rootConcepts, resolutionResult } = result;

  if (resolutionResult.conceptList) {
    const lookupResult = getConceptsByIdsWithTables(resolutionResult.conceptList, rootConcepts);

    if (lookupResult)
      return {
        label,
        ids: resolutionResult.conceptList,
        tables: lookupResult.tables,
        tree: lookupResult.root
      };
  } else if (resolutionResult.filter) {
    const [conceptRoot] =
      getConceptsByIdsWithTables([resolutionResult.selectedRoot], rootConcepts).concepts;
    const resolvedTable = {
      id: resolutionResult.filter.tableId,
      filters: [{
          id: resolutionResult.filter.filterId,
          value: resolutionResult.filter.value.map(filterValue => filterValue.value),
          options: resolutionResult.filter.value,
      }]
    };
    const tables = conceptRoot.tables.map(table => ({
      ...table,
      filters: mergeFiltersFromSavedConcept(resolvedTable, table)
    }));

    return {
      label,
      ids: [conceptRoot.id],
      tables,
      tree: conceptRoot.id
    };
  }

  return {
    label: label,
    ids: [],
    tables: [],
    tree: '',
    concepts: [],

    error: T.translate('queryEditor.couldNotInsertConceptList')
  };
}

const insertUploadedConceptList = (state, action: { data: UploadConceptListModalResultType }) => {
  const { parameters } = action.data;
  const queryElement = createQueryNodeFromConceptListUploadResult(action.data);

  if (parameters.andIdx != null)
    return dropOrNode(state, { payload: { item: queryElement, andIdx: parameters.andIdx } });

  return dropAndNode(state, { payload: { item: queryElement, dateRange: parameters.dateRange } });
};

const selectNodeForEditing = (state, {payload: { andIdx, orIdx }}) => {
  return setElementProperties(state, andIdx, orIdx, { isEditing: true });
}

const deselectNode = (state, action) => {
  return setAllElementsProperties(state, { isEditing: false });
}

const updateNodeLabel = (state, action) => {
  const node = selectEditedNode(state);
  if (!node) return state;

  const { andIdx, orIdx } = node;
  return setElementProperties(state, andIdx, orIdx, { label: action.label });
}

const addConceptToNode = (state, action) => {
  const nodePosition = selectEditedNode(state);
  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;
  const node = state[andIdx].elements[orIdx];
  return setElementProperties(state, andIdx, orIdx, {
    ids: [...action.concept.ids, ...node.ids],
  });
}

const removeConceptFromNode = (state, action) => {
  const nodePosition = selectEditedNode(state);
  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;
  const node = state[andIdx].elements[orIdx];
  return setElementProperties(state, andIdx, orIdx, {
    ids: node.ids.filter(id => id !== action.conceptId)
  });
}

const setResolvedFilterValues = (state: StateType, action: Object) => {
  const { resolutionResult, parameters } = action.data;

  return setNodeFilterValue(state, {
    payload: {
      value: resolutionResult.filter.value,
      tableIdx: parameters.tableIdx,
      filterIdx: parameters.filterIdx,
      options: resolutionResult.filter.value
    }
  });
}

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
  state: StandardQueryType = initialState,
  action: Object
): StandardQueryType => {
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
      return deselectNode(state, action);
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
    case RESET_ALL_FILTERS:
      return resetNodeAllFilters(state, action);
    case SWITCH_FILTER_MODE:
      return switchNodeFilterMode(state, action);
    case TOGGLE_TIMESTAMPS:
      return toggleTimestamps(state, action);
    case QUERY_GROUP_MODAL_SET_MIN_DATE:
      return setGroupDate(state, action, 'min');
    case QUERY_GROUP_MODAL_SET_MAX_DATE:
      return setGroupDate(state, action, 'max');
    case QUERY_GROUP_MODAL_RESET_ALL_DATES:
      return resetGroupDates(state, action);
    case EXPAND_PREVIOUS_QUERY:
      return expandPreviousQuery(state, action);
    case LOAD_PREVIOUS_QUERY_START:
      return loadPreviousQueryStart(state, action);
    case LOAD_PREVIOUS_QUERY_SUCCESS:
      return loadPreviousQuerySuccess(state, action);
    case LOAD_PREVIOUS_QUERY_ERROR:
      return loadPreviousQueryError(state, action);
    case RENAME_PREVIOUS_QUERY_SUCCESS:
      return renamePreviousQuery(state, action)
    case LOAD_FILTER_SUGGESTIONS_START:
      return loadFilterSuggestionsStart(state, action);
    case LOAD_FILTER_SUGGESTIONS_SUCCESS:
      return loadFilterSuggestionsSuccess(state, action);
    case LOAD_FILTER_SUGGESTIONS_ERROR:
      return loadFilterSuggestionsError(state, action);
    case UPLOAD_CONCEPT_LIST_MODAL_ACCEPT:
      return insertUploadedConceptList(state, action);
    case SET_RESOLVED_FILTER_VALUES:
      return setResolvedFilterValues(state, action);
    default:
      return state;
  }
};

export default query;
