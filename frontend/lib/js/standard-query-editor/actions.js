// @flow

import { type Dispatch }               from 'redux-thunk';

import api                             from '../api';

import { uploadConceptListModalOpen }  from '../upload-concept-list-modal/actions';

import {
  defaultSuccess,
  defaultError
} from '../common/actions';

import {
  DROP_AND_NODE,
  LOAD_FILES_START,
  LOAD_FILES_SUCCESS,
  LOAD_FILES_ERROR,
  DROP_OR_NODE,
  DELETE_NODE,
  DELETE_GROUP,
  TOGGLE_EXCLUDE_GROUP,
  LOAD_QUERY,
  CLEAR_QUERY,
  EXPAND_PREVIOUS_QUERY,
  SELECT_NODE_FOR_EDITING,
  DESELECT_NODE,
  TOGGLE_TABLE,
  SET_FILTER_VALUE,
  RESET_ALL_FILTERS,
  SWITCH_FILTER_MODE,
  TOGGLE_TIMESTAMPS,
  LOAD_FILTER_SUGGESTIONS_START,
  LOAD_FILTER_SUGGESTIONS_SUCCESS,
  LOAD_FILTER_SUGGESTIONS_ERROR,
} from './actionTypes';


export const dropAndNode = (item, dateRange) => ({
  type: DROP_AND_NODE,
  payload: { item, dateRange }
});

export const loadFilesStart = () =>
  ({ type: LOAD_FILES_START });
export const loadFilesSuccess = (res) =>
  defaultSuccess(LOAD_FILES_SUCCESS, res);
export const loadFilesError = (err) =>
  defaultError(LOAD_FILES_ERROR, err);

const validateConceptListFile = (file) => {
  return file.type === "text/plain";
};

const readConceptListFile = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader();

  reader.onload = (evt) => resolve(evt.target.result);
  reader.onerror = (err) => reject(err);

  reader.readAsText(file);
});

const parseConceptListFile = (fileContents) => {
  return fileContents.split('\n')
    .map(row => row.trim())
    .filter(row => row.length > 0);
};

export const dropConceptListFile = (item, queryContext = {}) => {
  return (dispatch) => {
    dispatch(loadFilesStart());

    // Ignore all dropped files except the first
    const file = item.files[0];

    if (!validateConceptListFile(file))
      return dispatch(loadFilesError(new Error("Invalid concept list file")));

    return readConceptListFile(file).then(
      r => {
        const conceptCodes = parseConceptListFile(r);

        if (conceptCodes.length)
          return dispatch([
            loadFilesSuccess(),
            uploadConceptListModalOpen({
              fileName: file.name,
              conceptCodes,
              queryContext
            })
          ]);

        return dispatch(loadFilesError(new Error('An empty file was dropped')));
      },
      e => dispatch(loadFilesError(e))
    );
  }
};

export const dropOrConceptListFile = (item, andIdx) => dropConceptListFile(item, { andIdx });

export const dropOrNode = (item, andIdx) => ({
  type: DROP_OR_NODE,
  payload: { item, andIdx }
});

export const deleteNode = (andIdx, orIdx) => ({
  type: DELETE_NODE,
  payload: { andIdx, orIdx }
});

export const deleteGroup = (andIdx, orIdx) => ({
  type: DELETE_GROUP,
  payload: { andIdx, orIdx }
});

export const toggleExcludeGroup = (andIdx) => ({
  type: TOGGLE_EXCLUDE_GROUP,
  payload: { andIdx }
});

export const loadQuery = (query) => ({
  type: LOAD_QUERY,
  payload: { query }
});

export const clearQuery = () => ({ type: CLEAR_QUERY });

export const expandPreviousQuery = (rootConcepts, groups) => ({
  type: EXPAND_PREVIOUS_QUERY,
  payload: { rootConcepts, groups }
});

export const selectNodeForEditing = (andIdx, orIdx) => ({
  type: SELECT_NODE_FOR_EDITING,
  payload: { andIdx, orIdx }
});

export const deselectNode = () => ({ type: DESELECT_NODE });

export const toggleTable = (tableIdx, isExcluded) => ({
  type: TOGGLE_TABLE,
  payload: { tableIdx, isExcluded }
});

export const setFilterValue = (tableIdx, filterIdx, value) => ({
  type: SET_FILTER_VALUE,
  payload: { tableIdx, filterIdx, value }
});

export const resetAllFilters = (andIdx, orIdx) => ({
  type: RESET_ALL_FILTERS,
  payload: { andIdx, orIdx }
});

export const switchFilterMode = (tableIdx, filterIdx, mode) => ({
  type: SWITCH_FILTER_MODE,
  payload: { tableIdx, filterIdx, mode }
});

export const toggleTimestamps = (isExcluded) => ({
  type: TOGGLE_TIMESTAMPS,
  payload: { isExcluded }
});

export const loadFilterSuggestionsStart = (tableIdx, conceptId, filterIdx, prefix) => ({
  type: LOAD_FILTER_SUGGESTIONS_START,
  payload: { tableIdx, conceptId, filterIdx, prefix }
});

export const loadFilterSuggestionsSuccess = (suggestions, tableIdx, filterIdx) => ({
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
  },
});

export const loadFilterSuggestions =
  (datasetId, tableIdx, tableId, conceptId, filterIdx, filterId, prefix) => {
    return (dispatch: Dispatch) => {
      dispatch(loadFilterSuggestionsStart(tableIdx, conceptId, filterIdx, prefix));

      return api.postPrefixForSuggestions(datasetId, conceptId, tableId, filterId, prefix)
        .then(
          r => dispatch(loadFilterSuggestionsSuccess(r, tableIdx, filterIdx)),
          e => dispatch(loadFilterSuggestionsError(e, tableIdx, filterIdx))
        );
    };
  }
