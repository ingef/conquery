import { ThunkDispatch } from "redux-thunk";

import api from "../api";
import type { DatasetIdT } from "../api/types";

import { defaultSuccess, defaultError } from "../common/actions";
import { loadPreviousQuery } from "../previous-queries/list/actions";

import type {
  DraggedNodeType,
  DraggedQueryType,
  PreviousQueryQueryNodeType,
} from "./types";
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
  SET_SELECTS,
  SET_TABLE_SELECTS,
  RESET_ALL_FILTERS,
  SWITCH_FILTER_MODE,
  TOGGLE_TIMESTAMPS,
  LOAD_FILTER_SUGGESTIONS_START,
  LOAD_FILTER_SUGGESTIONS_SUCCESS,
  LOAD_FILTER_SUGGESTIONS_ERROR,
  SET_DATE_COLUMN,
  SET_SELECTED_SECONDARY_ID,
} from "./actionTypes";
import { TreesT } from "../concept-trees/reducer";

export const dropAndNode = (item: DraggedNodeType | DraggedQueryType) => ({
  type: DROP_AND_NODE,
  payload: { item },
});

export const dropOrNode = (
  item: DraggedNodeType | DraggedQueryType,
  andIdx: number
) => ({
  type: DROP_OR_NODE,
  payload: { item, andIdx },
});

export const deleteNode = (andIdx: number, orIdx: number) => ({
  type: DELETE_NODE,
  payload: { andIdx, orIdx },
});

export const deleteGroup = (andIdx: number, orIdx: number) => ({
  type: DELETE_GROUP,
  payload: { andIdx, orIdx },
});

export const toggleExcludeGroup = (andIdx: number) => ({
  type: TOGGLE_EXCLUDE_GROUP,
  payload: { andIdx },
});

export const loadQuery = (query) => ({
  type: LOAD_QUERY,
  payload: { query },
});

export const clearQuery = () => ({ type: CLEAR_QUERY });

const findPreviousQueryIds = (node, queries = []) => {
  switch (node.type) {
    case "SAVED_QUERY":
      return [...queries, node.query];
    case "NEGATION":
    case "DATE_RESTRICTION":
      return findPreviousQueryIds(node.child, queries);
    case "AND":
    case "OR":
      return [
        ...queries,
        ...node.children.flatMap((child) => findPreviousQueryIds(child, [])),
      ];
    default:
      return queries;
  }
};

/*
  1) Expands previous query in the editor
  2) Triggers a load for all nested queries
*/
export const expandPreviousQuery = (
  datasetId: DatasetIdT,
  rootConcepts: TreesT,
  query: PreviousQueryQueryNodeType
) => {
  if (!query.root || query.root.type !== "AND") {
    throw new Error("Cant expand query, because root is not AND");
  }

  const nestedPreviousQueryIds = findPreviousQueryIds(query.root);

  return [
    {
      type: EXPAND_PREVIOUS_QUERY,
      payload: { rootConcepts, query },
    },
    ...nestedPreviousQueryIds.map((queryId) =>
      loadPreviousQuery(datasetId, queryId)
    ),
  ];
};

export const selectNodeForEditing = (andIdx: number, orIdx: number) => ({
  type: SELECT_NODE_FOR_EDITING,
  payload: { andIdx, orIdx },
});

export const deselectNode = () => ({ type: DESELECT_NODE });

export const updateNodeLabel = (label) => ({
  type: UPDATE_NODE_LABEL,
  payload: { label },
});
export const addConceptToNode = (concept) => ({
  type: ADD_CONCEPT_TO_NODE,
  payload: { concept },
});
export const removeConceptFromNode = (conceptId) => ({
  type: REMOVE_CONCEPT_FROM_NODE,
  payload: { conceptId },
});

export const toggleTable = (tableIdx, isExcluded) => ({
  type: TOGGLE_TABLE,
  payload: { tableIdx, isExcluded },
});

export const setFilterValue = (tableIdx, filterIdx, value) => ({
  type: SET_FILTER_VALUE,
  payload: { tableIdx, filterIdx, value },
});

export const setTableSelects = (tableIdx, value) => ({
  type: SET_TABLE_SELECTS,
  payload: { tableIdx, value },
});
export const setSelects = (value) => ({
  type: SET_SELECTS,
  payload: { value },
});

export const setDateColumn = (tableIdx, value) => ({
  type: SET_DATE_COLUMN,
  payload: { tableIdx, value },
});

export const resetAllFilters = (andIdx: number, orIdx: number) => ({
  type: RESET_ALL_FILTERS,
  payload: { andIdx, orIdx },
});

export const switchFilterMode = (tableIdx, filterIdx, mode) => ({
  type: SWITCH_FILTER_MODE,
  payload: { tableIdx, filterIdx, mode },
});

export const toggleTimestamps = (andIdx, orIdx) => ({
  type: TOGGLE_TIMESTAMPS,
  payload: { andIdx, orIdx },
});

export const loadFilterSuggestionsStart = (tableIdx, filterIdx) => ({
  type: LOAD_FILTER_SUGGESTIONS_START,
  payload: { tableIdx, filterIdx },
});

export const loadFilterSuggestionsSuccess = (
  suggestions,
  tableIdx,
  filterIdx
) =>
  defaultSuccess(LOAD_FILTER_SUGGESTIONS_SUCCESS, suggestions, {
    tableIdx,
    filterIdx,
  });

export const loadFilterSuggestionsError = (error, tableIdx, filterIdx) =>
  defaultError(LOAD_FILTER_SUGGESTIONS_ERROR, error, { tableIdx, filterIdx });

export const loadFilterSuggestions = (
  datasetId,
  conceptId,
  tableId,
  filterId,
  prefix,
  tableIdx,
  filterIdx
) => {
  return (dispatch: ThunkDispatch) => {
    dispatch(loadFilterSuggestionsStart(tableIdx, filterIdx));

    return api
      .postPrefixForSuggestions(datasetId, conceptId, tableId, filterId, prefix)
      .then(
        (r) => dispatch(loadFilterSuggestionsSuccess(r, tableIdx, filterIdx)),
        (e) => dispatch(loadFilterSuggestionsError(e, tableIdx, filterIdx))
      );
  };
};

export const setSelectedSecondaryId = (secondaryId: string | null) => {
  return {
    type: SET_SELECTED_SECONDARY_ID,
    payload: { secondaryId },
  };
};
