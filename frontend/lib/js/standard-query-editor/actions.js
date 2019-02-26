// @flow

import { type Dispatch } from "redux-thunk";

import api from "../api";
import { type DateRangeType } from "../common/types/backend";
import { dropFiles as fileUploadDropFiles } from "../file-upload/actions";
import { conceptFilterValuesResolve } from "../upload-concept-list-modal/actions";

import type { DraggedNodeType, DraggedQueryType } from "./types";
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
  TOGGLE_INCLUDE_SUBNODES
} from "./actionTypes";

export const dropAndNode = (
  item: DraggedNodeType | DraggedQueryType,
  dateRange: ?DateRangeType
) => ({
  type: DROP_AND_NODE,
  payload: { item, dateRange }
});

export const dropOrNode = (
  item: DraggedNodeType | DraggedQueryType,
  andIdx: number
) => ({
  type: DROP_OR_NODE,
  payload: { item, andIdx }
});

export const deleteNode = (andIdx: number, orIdx: number) => ({
  type: DELETE_NODE,
  payload: { andIdx, orIdx }
});

export const deleteGroup = (andIdx: number, orIdx: number) => ({
  type: DELETE_GROUP,
  payload: { andIdx, orIdx }
});

export const toggleExcludeGroup = (andIdx: number) => ({
  type: TOGGLE_EXCLUDE_GROUP,
  payload: { andIdx }
});

export const loadQuery = query => ({
  type: LOAD_QUERY,
  payload: { query }
});

export const clearQuery = () => ({ type: CLEAR_QUERY });

export const expandPreviousQuery = (rootConcepts, groups) => ({
  type: EXPAND_PREVIOUS_QUERY,
  payload: { rootConcepts, groups }
});

export const selectNodeForEditing = (andIdx: number, orIdx: number) => ({
  type: SELECT_NODE_FOR_EDITING,
  payload: { andIdx, orIdx }
});

export const deselectNode = () => ({ type: DESELECT_NODE });

export const updateNodeLabel = label => ({ type: UPDATE_NODE_LABEL, label });
export const addConceptToNode = concept => ({
  type: ADD_CONCEPT_TO_NODE,
  concept
});
export const removeConceptFromNode = conceptId => ({
  type: REMOVE_CONCEPT_FROM_NODE,
  conceptId
});

export const toggleTable = (tableIdx, isExcluded) => ({
  type: TOGGLE_TABLE,
  payload: { tableIdx, isExcluded }
});

export const setFilterValue = (tableIdx, filterIdx, value, formattedValue) => ({
  type: SET_FILTER_VALUE,
  payload: { tableIdx, filterIdx, value, formattedValue }
});

export const resetAllFilters = (andIdx: number, orIdx: number) => ({
  type: RESET_ALL_FILTERS,
  payload: { andIdx, orIdx }
});

export const switchFilterMode = (tableIdx, filterIdx, mode) => ({
  type: SWITCH_FILTER_MODE,
  payload: { tableIdx, filterIdx, mode }
});

export const toggleTimestamps = isExcluded => ({
  type: TOGGLE_TIMESTAMPS,
  payload: { isExcluded }
});

export const toggleIncludeSubnodes = includeSubnodes => ({
  type: TOGGLE_INCLUDE_SUBNODES,
  payload: { includeSubnodes }
});

export const loadFilterSuggestionsStart = (
  tableIdx,
  conceptId,
  filterIdx,
  prefix
) => ({
  type: LOAD_FILTER_SUGGESTIONS_START,
  payload: { tableIdx, conceptId, filterIdx, prefix }
});

export const loadFilterSuggestionsSuccess = (
  suggestions,
  tableIdx,
  filterIdx
) => ({
  type: LOAD_FILTER_SUGGESTIONS_SUCCESS,
  payload: {
    suggestions,
    tableIdx,
    filterIdx
  }
});

export const loadFilterSuggestionsError = (error, tableIdx, filterIdx) => ({
  type: LOAD_FILTER_SUGGESTIONS_ERROR,
  payload: {
    message: error.message,
    ...error,
    tableIdx,
    filterIdx
  }
});

export const loadFilterSuggestions = (
  datasetId,
  tableIdx,
  tableId,
  conceptId,
  filterIdx,
  filterId,
  prefix
) => {
  return (dispatch: Dispatch) => {
    dispatch(
      loadFilterSuggestionsStart(tableIdx, conceptId, filterIdx, prefix)
    );

    return api
      .postPrefixForSuggestions(datasetId, conceptId, tableId, filterId, prefix)
      .then(
        r => dispatch(loadFilterSuggestionsSuccess(r, tableIdx, filterIdx)),
        e => dispatch(loadFilterSuggestionsError(e, tableIdx, filterIdx))
      );
  };
};

export const dropFiles = (
  datasetId,
  treeId,
  tableIdx,
  tableId,
  filterIdx,
  filterId,
  files
) =>
  fileUploadDropFiles(files, {
    parameters: {
      actionType: SET_RESOLVED_FILTER_VALUES,
      datasetId,
      treeId,
      tableIdx,
      tableId,
      filterIdx,
      filterId
    },
    callback: conceptFilterValuesResolve
  });
